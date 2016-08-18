/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.fetch;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.elements.api.ElementsAPIVersion;
import uk.co.symplectic.elements.api.ElementsFeedQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedGroupQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedObjectQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedRelationshipQuery;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.ElementsObjectStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoreFactory;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;
import uk.co.symplectic.xml.StAXUtils;
import uk.co.symplectic.xml.XMLEventProcessor;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class ElementsFetch {

    static abstract class DataStoringFilter<S, T extends XMLEventProcessor.ItemExtractingFilter<S>> extends XMLEventProcessor.EventFilterWrapper<T> {
        private static final int logProgressEveryN = 1000;

        private XMLEventWriter writer = null;
        private ByteArrayOutputStream dataStream = null;
        private final XMLEventFactory eventFactory = StAXUtils.getXMLEventFactory();
        private final QName rootElement;
        private int counter = 0;

        DataStoringFilter(QName rootElement, T innerFilter){
            super(innerFilter);
            this.rootElement = rootElement;
        }

        protected void postInnerItemStart(StartElement initialElement, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
            XMLOutputFactory factory = StAXUtils.getXMLOutputFactory();
            dataStream = new ByteArrayOutputStream();
            writer = factory.createXMLEventWriter(dataStream, "utf-8");
            writer.add(eventFactory.createStartDocument());
            if(rootElement != null) writer.add(eventFactory.createStartElement(rootElement, null, null));
        }

        protected void postInnerProcessEvent(XMLEvent event, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
            writer.add(event);
        }

        protected void postInnerItemEnd(EndElement finalElement, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
            if(rootElement != null) writer.add(eventFactory.createEndElement(rootElement, null));
            writer.add(eventFactory.createEndDocument());
            writer.close();
            try {
                S item = innerFilter.getExtractedItem();
                counter++;
                if(counter % logProgressEveryN == 0) {
                    log.info(MessageFormat.format("{0} items processed", counter));
                }

                processItem(item, dataStream.toByteArray());
            }
            catch(IOException e){
                throw new IllegalStateException(e);
            }
        }

        protected abstract void processItem(S item, byte[] data) throws IOException;
    }

    private static class ObjectStoreFilter<S extends ElementsItemInfo, T extends XMLEventProcessor.ItemExtractingFilter<S>> extends DataStoringFilter<S, T> {
        protected final ElementsObjectStore objectStore;
        private final StorableResourceType resourceType;
        private final boolean actuallyStoreData;

        public ObjectStoreFilter(QName rootElement, T innerFilter, ElementsObjectStore objectStore, StorableResourceType resourceType) {
            this(rootElement,innerFilter, objectStore, resourceType, true);
        }

        public ObjectStoreFilter(QName rootElement, T innerFilter, ElementsObjectStore objectStore, StorableResourceType resourceType, boolean actuallyStoreData) {
            super(rootElement, innerFilter);
            if (objectStore == null) throw new NullArgumentException("objectStore");
            if (resourceType == null) throw new NullArgumentException("resourceType");
            this.objectStore = objectStore;
            this.resourceType = resourceType;
            this.actuallyStoreData = actuallyStoreData;
        }

        @Override
        protected void processItem(S item, byte[] data) throws IOException {
            byte[] dataToStore = actuallyStoreData ? data : null;
            objectStore.storeItem(item, resourceType, dataToStore);
        }
    }

    private static class FileStoringObjectFilter extends ObjectStoreFilter<ElementsObjectInfo, ElementsObjectInfo.Extractor>{

        public static FileStoringObjectFilter create(ElementsObjectStore objectStore, boolean forDeletedObjects){
            if(forDeletedObjects)
                return new FileStoringObjectFilter(objectStore, ElementsObjectInfo.Extractor.feedDeletedEntryLocation, false);
            return new FileStoringObjectFilter(objectStore, ElementsObjectInfo.Extractor.feedEntryLocation, true);
        }

        private FileStoringObjectFilter(ElementsObjectStore objectStore, DocumentLocation loc, boolean storeData){
            super(new QName(atomNS, "entry"), new ElementsObjectInfo.Extractor(loc, 0), objectStore, StorableResourceType.RAW_OBJECT, storeData);
        }
    }

    private static class FileStoringRelationshipFilter extends ObjectStoreFilter<ElementsRelationshipInfo, ElementsRelationshipInfo.Extractor>{
        public static FileStoringRelationshipFilter create(ElementsObjectStore objectStore, boolean forDeletedObjects){
            if(forDeletedObjects)
                return new FileStoringRelationshipFilter(objectStore, ElementsRelationshipInfo.Extractor.feedDeletedEntryLocation, false);
            return new FileStoringRelationshipFilter(objectStore, ElementsRelationshipInfo.Extractor.feedEntryLocation, true);
        }

        protected FileStoringRelationshipFilter(ElementsObjectStore objectStore, DocumentLocation loc, boolean storeData){
            super(new QName(atomNS, "entry"), new ElementsRelationshipInfo.Extractor(loc, 0), objectStore, StorableResourceType.RAW_RELATIONSHIP, storeData);
        }
    }

    private static class FileStoringGroupFilter extends ObjectStoreFilter<ElementsGroupInfo, ElementsGroupInfo.Extractor> {
        FileStoringGroupFilter(ElementsObjectStore objectStore){
            super(new QName(atomNS, "entry"), new ElementsGroupInfo.Extractor(ElementsGroupInfo.Extractor.feedEntryLocation, 0),
                    objectStore, StorableResourceType.RAW_GROUP);
        }
    }


    public abstract static class FetchConfig {
        final private int itemsPerPage;
        final private boolean processAllPages;
        final private boolean getFullDetails;

        public FetchConfig(boolean getFullDetails, boolean processAllPages, int itemsPerPage){
            this.itemsPerPage = itemsPerPage;
            this.processAllPages = processAllPages;
            this.getFullDetails = getFullDetails;
        }

        public Collection<ElementsFeedQuery> getQueries(){return getQueries(getFullDetails, processAllPages, itemsPerPage);}
        protected abstract Collection<ElementsFeedQuery> getQueries(boolean fullDetails, boolean getAllPages, int perPage);
        //This should return a "new" object not the same one..
        public abstract ElementsAPI.APIResponseFilter getExtractor(ElementsObjectStore objectStore);
    }

    public static class ObjectConfig extends FetchConfig {
        //Which categories of Elements objects should be retrieved?
        final private List<ElementsObjectCategory> categoriesToHarvest = new ArrayList<ElementsObjectCategory>();
        final private String groups; //TODO: remove this entirely?

        public ObjectConfig(boolean getFullDetails, int objectsPerPage, String groups, ElementsObjectCategory... categoriesToHarvest){
            this(getFullDetails, objectsPerPage, groups, Arrays.asList(categoriesToHarvest)); }

        public ObjectConfig(boolean getFullDetails, int objectsPerPage, String groups, Collection<ElementsObjectCategory> categoriesToHarvest){
            super(getFullDetails, true, objectsPerPage);
            if (categoriesToHarvest == null)  throw new NullArgumentException("categoriesToHarvest");
            if (categoriesToHarvest.isEmpty())  throw new IllegalArgumentException("categoriesToHarvest should not be empty for an ElementsFetch.Configuration");
            this.categoriesToHarvest.addAll(categoriesToHarvest);
            this.groups = StringUtils.trimToNull(groups);
        }

        @Override
        protected Collection<ElementsFeedQuery> getQueries(boolean fullDetails, boolean getAllPages, int perPage){
            List<ElementsFeedQuery> queries = new ArrayList<ElementsFeedQuery>();
            for(ElementsObjectCategory category : categoriesToHarvest){
                queries.add(new ElementsAPIFeedObjectQuery(category, groups, fullDetails, getAllPages, perPage));
            }
            return queries;
        }

        @Override
        public ElementsAPI.APIResponseFilter getExtractor(ElementsObjectStore objectStore) {
            return new ElementsAPI.APIResponseFilter(FileStoringObjectFilter.create(objectStore, false), ElementsAPIVersion.allVersions());
        }
    }

    public static class RelationshipConfig extends FetchConfig{
        public RelationshipConfig(int relationshipsPerPage){
            super(false, true, relationshipsPerPage);
        }

        @Override
        protected Collection<ElementsFeedQuery> getQueries(boolean fullDetails, boolean getAllPages, int perPage){
            return Arrays.asList(new ElementsFeedQuery[]{new ElementsAPIFeedRelationshipQuery(fullDetails, getAllPages, perPage)});
        }

        @Override
        public ElementsAPI.APIResponseFilter getExtractor(ElementsObjectStore objectStore) {
            return new ElementsAPI.APIResponseFilter(FileStoringRelationshipFilter.create(objectStore, false), ElementsAPIVersion.allVersions());
        }
    }


    /**
     * SLF4J Logger
     */
    private static Logger log = LoggerFactory.getLogger(ElementsFetch.class);
    //the api to fetch data from
    final private ElementsAPI elementsAPI;

    //the store to put data into.
    final private ElementsObjectStore objectStore;

    public ElementsFetch(ElementsAPI api, ElementsObjectStore objectStore) {
        if (api == null) throw new NullArgumentException("api");
        if (objectStore == null) throw new NullArgumentException("objectStore");
        this.elementsAPI = api;
        this.objectStore = objectStore;
    }

    /**
     * Executes the task
     * @throws IOException error processing search
     */
    public void execute(FetchConfig config)throws IOException {
        if(config == null) throw new NullArgumentException("config");
        for (ElementsFeedQuery query : config.getQueries()) {
            if (query != null) {
                elementsAPI.executeQuery(query, config.getExtractor(objectStore));
            }
        }
    }
}

