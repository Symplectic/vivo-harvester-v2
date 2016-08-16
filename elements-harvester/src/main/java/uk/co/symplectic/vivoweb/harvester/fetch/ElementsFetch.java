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

    private class FileStoringObjectFilter extends DataStoringFilter<ElementsObjectInfo, ElementsObjectInfo.Extractor>{
        private final ElementsObjectStore objectStore;

        FileStoringObjectFilter(ElementsObjectStore objectStore){
            super(new QName(atomNS, "entry"), new ElementsObjectInfo.Extractor(ElementsObjectInfo.Extractor.feedEntryLocation, 0));
            if(objectStore == null) throw new NullArgumentException("objectStore");
            this.objectStore = objectStore;
        }

        @Override
        protected void processItem(ElementsObjectInfo item, byte[] data) throws IOException {
            objectStore.storeItem(item, StorableResourceType.RAW_OBJECT, data);
        }
    }

    private class FileStoringRelationshipFilter extends DataStoringFilter<ElementsRelationshipInfo, ElementsRelationshipInfo.Extractor>{
        private final ElementsObjectStore objectStore;

        FileStoringRelationshipFilter(ElementsObjectStore objectStore){
            super(new QName(atomNS, "entry"), new ElementsRelationshipInfo.Extractor(ElementsRelationshipInfo.Extractor.feedEntryLocation, 0));
            if(objectStore == null) throw new NullArgumentException("objectStore");
            this.objectStore = objectStore;
        }

        @Override
        protected void processItem(ElementsRelationshipInfo item, byte[] data) throws IOException {
            objectStore.storeItem(item, StorableResourceType.RAW_RELATIONSHIP, data);
        }
    }

    public static class ObjectConfig{
        //Which categories of Elements objects should be retrieved?
        final private List<ElementsObjectCategory> categoriesToHarvest = new ArrayList<ElementsObjectCategory>();
        // How many objects to request per API request: Default of 25 (see constructor chain) is required by 4.6 API since we request full detail for objects
        final private int objectsPerPage;

        public ObjectConfig(Collection<ElementsObjectCategory> categoriesToHarvest){ this(25, categoriesToHarvest); }

        public ObjectConfig(ElementsObjectCategory... categoriesToHarvest){ this(25, categoriesToHarvest); }

        public ObjectConfig(int objectsPerPage, ElementsObjectCategory... categoriesToHarvest){ this(objectsPerPage, Arrays.asList(categoriesToHarvest)); }

        public ObjectConfig(int objectsPerPage, Collection<ElementsObjectCategory> categoriesToHarvest){
            if (categoriesToHarvest == null)  throw new NullArgumentException("categoriesToHarvest");
            if (categoriesToHarvest.isEmpty())  throw new IllegalArgumentException("categoriesToHarvest should not be empty for an ElementsFetch.Configuration");
            this.categoriesToHarvest.addAll(categoriesToHarvest);
            this.objectsPerPage =objectsPerPage;
        }
    }

    public static class RelationshipConfig {
        // How many relationships to request per API request:Default of 100 used for optimal performance (see constructor chain)
        final private int relationshipsPerPage;

        public RelationshipConfig(){ this(100); }

        public RelationshipConfig(int relationshipsPerPage){
            this.relationshipsPerPage = relationshipsPerPage;
        }
    }
    /**
     * SLF4J Logger
     */
    private static Logger log = LoggerFactory.getLogger(ElementsFetch.class);
    //the api to fetch data from
    final private ElementsAPI elementsAPI;
    //the store to place retrieved items into
    final private ElementsObjectStore objectStore;
    //whether to fetch full details or not.
    final private boolean getFullDetails;
    //Which user groups should the fetch be restricted to
    final private String groupsToHarvest;
    final private ObjectConfig objectConfig;
    final private RelationshipConfig relationshipConfig;


    public ElementsFetch(ElementsAPI api, ElementsObjectStore objectStore, ObjectConfig objectConfig, RelationshipConfig relationshipConfig){
        this(api, objectStore, objectConfig, relationshipConfig, null, true);
    }

    public ElementsFetch(ElementsAPI api, ElementsObjectStore objectStore, ObjectConfig objectConfig, RelationshipConfig relationshipConfig, String groupsToHarvest, boolean getFullDetails) {
        if (api == null) throw new NullArgumentException("api");
        if (objectStore == null) throw new NullArgumentException("objectStore");
        if(objectConfig == null && relationshipConfig == null) throw new IllegalArgumentException("One of objectConfig and relationshipConfig must not be null");

        this.elementsAPI = api;
        this.objectStore = objectStore;
        this.objectConfig = objectConfig;
        this.relationshipConfig = relationshipConfig;
        this.getFullDetails = getFullDetails;
        this.groupsToHarvest = StringUtils.trimToNull(groupsToHarvest);
    }

    /**
     * Executes the task
     * @throws IOException error processing search
     */
    public void execute()throws IOException {
        if(objectConfig != null) executeObjectFetch();
        if(relationshipConfig != null) executeRelationshipFetch();
    }

    private void executeObjectFetch() throws IOException {

        // Set up the query we are going to perform :
        // For retrieving objects, get the full record, get N objects per page and load all pages, not just one
        ElementsAPIFeedObjectQuery feedQuery = new ElementsAPIFeedObjectQuery();
        feedQuery.setFullDetails(getFullDetails);
        feedQuery.setPerPage(objectConfig.objectsPerPage);
        feedQuery.setProcessAllPages(true);
        //set the groups that should be considered - note ensure we set this to null if we have no real value.
        feedQuery.setGroups(groupsToHarvest);


        // for each category update the query to retrieve that category and execute the query putting each object retrieved into the object store.
        for (ElementsObjectCategory category : objectConfig.categoriesToHarvest) {
            if (category != null) {
                feedQuery.setCategory(category);
                FileStoringObjectFilter filter = new FileStoringObjectFilter(objectStore);
                log.info(MessageFormat.format("Retrieving Elements Category {0}", category.getSingular()));
                elementsAPI.executeQuery(feedQuery, new ElementsAPI.APIResponseFilter(filter, ElementsAPIVersion.allVersions()));
            }
        }
    }

    private void executeRelationshipFetch() throws IOException {
        //create a query to retrieve relationships, set to get N objects per page and load all pages, not just one
        ElementsAPIFeedRelationshipQuery relationshipFeedQuery = new ElementsAPIFeedRelationshipQuery();
        relationshipFeedQuery.setProcessAllPages(true);
        relationshipFeedQuery.setPerPage(relationshipConfig.relationshipsPerPage);

        //execute the query putting each relationship retrieved into the object store
        FileStoringRelationshipFilter relationshipFilter = new FileStoringRelationshipFilter(objectStore);
        log.info("Retrieving Elements Relationships");
        elementsAPI.executeQuery(relationshipFeedQuery, new ElementsAPI.APIResponseFilter(relationshipFilter, ElementsAPIVersion.allVersions()));
    }
}

