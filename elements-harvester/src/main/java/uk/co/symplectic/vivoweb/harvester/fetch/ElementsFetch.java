/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.fetch;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.elements.api.ElementsAPIVersion;
import uk.co.symplectic.elements.api.ElementsFeedQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedGroupQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedObjectQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedObjectRelationshipsQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedRelationshipQuery;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemStore;
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
import java.util.*;

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
                    log.info(MessageFormat.format("{0} items processed and stored", counter));
                }

                processItem(item, dataStream.toByteArray());
            }
            catch(IOException e){
                throw new IllegalStateException(e);
            }
        }

        protected abstract void processItem(S item, byte[] data) throws IOException;
    }

    private static class ItemStoringFilter<S extends ElementsItemInfo, T extends XMLEventProcessor.ItemExtractingFilter<S>> extends DataStoringFilter<S, T> {
        protected final ElementsItemStore objectStore;

        protected ItemStoringFilter(QName rootElement, T innerFilter, ElementsItemStore objectStore) {
            super(rootElement, innerFilter);
            if (objectStore == null) throw new NullArgumentException("objectStore");
            this.objectStore = objectStore;
        }

        @Override
        protected void processItem(S item, byte[] data) throws IOException {
            objectStore.storeItem(item, getResourceType(item), data);
        }

        protected StorableResourceType getResourceType(S item){
            //wrapper to decide on the stored resource type...
            if(item == null) throw new NullArgumentException("item");
            if(item.isObjectInfo()) return StorableResourceType.RAW_OBJECT;
            if(item.isRelationshipInfo()) return StorableResourceType.RAW_RELATIONSHIP;
            if(item.isGroupInfo()) return StorableResourceType.RAW_GROUP;
            throw new IllegalStateException("Unstorable raw item");
        }
    }

    private static class ItemDeletingFilter<S extends ElementsItemInfo, T extends XMLEventProcessor.ItemExtractingFilter<S>> extends ItemStoringFilter<S,T> {

        protected ItemDeletingFilter(QName rootElement, T innerFilter, ElementsItemStore.ElementsDeletableItemStore objectStore) {
            super(rootElement, innerFilter, objectStore);
        }

        @Override
        protected void processItem(S item, byte[] data) throws IOException {
            ((ElementsItemStore.ElementsDeletableItemStore) objectStore).deleteItem(item.getItemId(), getResourceType(item));
        }
    }

    private static class FileStoringObjectFilter extends ItemStoringFilter<ElementsObjectInfo, ElementsObjectInfo.Extractor> {
        private FileStoringObjectFilter(ElementsItemStore objectStore){
            super(new QName(atomNS, "entry"), new ElementsObjectInfo.Extractor(ElementsObjectInfo.Extractor.feedEntryLocation, 0), objectStore);
        }
    }

    private static class FileDeletingObjectFilter extends ItemDeletingFilter<ElementsObjectInfo, ElementsObjectInfo.Extractor> {
        private FileDeletingObjectFilter(ElementsItemStore.ElementsDeletableItemStore objectStore){
            super(new QName(atomNS, "entry"), new ElementsObjectInfo.Extractor(ElementsObjectInfo.Extractor.feedDeletedEntryLocation, 0), objectStore);
        }
    }

    private static class FileStoringRelationshipFilter extends ItemStoringFilter<ElementsRelationshipInfo, ElementsRelationshipInfo.Extractor> {
        protected FileStoringRelationshipFilter(ElementsItemStore objectStore){
            super(new QName(atomNS, "entry"), new ElementsRelationshipInfo.Extractor(ElementsRelationshipInfo.Extractor.feedEntryLocation, 0), objectStore);
        }
    }

    private static class FileTouchingRelationshipFilter extends ItemStoringFilter<ElementsRelationshipInfo, ElementsRelationshipInfo.Extractor> {
        protected FileTouchingRelationshipFilter(ElementsItemStore.ElementsDeletableItemStore objectStore, ElementsRelationshipInfo.Extractor customExtractor){
            super(new QName(atomNS, "entry"), customExtractor, objectStore);
        }

        @Override
        protected void processItem(ElementsRelationshipInfo item, byte[] data) throws IOException {
            //TODO : improve how casting works here?
            ((ElementsItemStore.ElementsDeletableItemStore) objectStore).touchItem(item, getResourceType(item));
        }
    }

    private static class FileDeletingRelationshipFilter extends ItemDeletingFilter<ElementsRelationshipInfo, ElementsRelationshipInfo.Extractor> {
        protected FileDeletingRelationshipFilter(ElementsItemStore.ElementsDeletableItemStore objectStore){
            super(new QName(atomNS, "entry"), new ElementsRelationshipInfo.Extractor(ElementsRelationshipInfo.Extractor.feedDeletedEntryLocation, 0), objectStore);
        }
    }

    private static class FileStoringGroupFilter extends ItemStoringFilter<ElementsGroupInfo, ElementsGroupInfo.Extractor> {
        FileStoringGroupFilter(ElementsItemStore objectStore){
            super(new QName(atomNS, "entry"), new ElementsGroupInfo.Extractor(ElementsGroupInfo.Extractor.feedEntryLocation, 0), objectStore);
        }
    }

    public abstract static class FetchConfig {
        public abstract Collection<DescribedQuery> getQueries();
        //This should return a "new" object not the same one..
        //public abstract ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore);

        public abstract static class DescribedQuery{
            private final ElementsFeedQuery query;
            private final String description;

            public DescribedQuery(ElementsFeedQuery query, String description){
                if(query == null) throw new NullArgumentException("query");
                if(StringUtils.trimToNull(description) == null) throw new IllegalArgumentException("description cannot be null or empty");

                this.query = query;
                this.description = description;
            }

            public abstract ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore);
        }
    }

    public abstract static class PaginatedFeedConfig extends FetchConfig{
        final private int itemsPerPage;
        final private boolean processAllPages;
        final private boolean getFullDetails;

        public PaginatedFeedConfig(boolean getFullDetails, boolean processAllPages, int itemsPerPage){
            this.itemsPerPage = itemsPerPage;
            this.processAllPages = processAllPages;
            this.getFullDetails = getFullDetails;
        }

        @Override
        public Collection<DescribedQuery> getQueries(){return getQueries(getFullDetails, processAllPages, itemsPerPage);}

        protected abstract Collection<DescribedQuery> getQueries(boolean fullDetails, boolean getAllPages, int perPage);
    }

    public abstract static class DeltaCapableFeedConfig extends PaginatedFeedConfig{
        final private Date modifiedSince;

        protected Date getModifiedSince(){return modifiedSince; }

        public DeltaCapableFeedConfig(Date modifiedSince, boolean getFullDetails, boolean processAllPages, int itemsPerPage){
            super(getFullDetails, processAllPages, itemsPerPage);
            this.modifiedSince = modifiedSince;
        }
    }

    public static class ObjectConfig extends DeltaCapableFeedConfig {
        //Which categories of Elements objects should be retrieved?
        final private List<ElementsObjectCategory> categoriesToHarvest = new ArrayList<ElementsObjectCategory>();

        public ObjectConfig(boolean getFullDetails, int objectsPerPage, Date modifiedSince, ElementsObjectCategory... categoriesToHarvest){
            this(getFullDetails, objectsPerPage, modifiedSince, Arrays.asList(categoriesToHarvest));
        }

        public ObjectConfig(boolean getFullDetails, int objectsPerPage, Date modifiedSince, Collection<ElementsObjectCategory> categoriesToHarvest){
            super(modifiedSince, getFullDetails, true, objectsPerPage);
            if (categoriesToHarvest == null)  throw new NullArgumentException("categoriesToHarvest");
            if (categoriesToHarvest.isEmpty())  throw new IllegalArgumentException("categoriesToHarvest should not be empty for an ElementsFetch.Configuration");
            this.categoriesToHarvest.addAll(categoriesToHarvest);
        }

        @Override
        protected Collection<DescribedQuery> getQueries(boolean fullDetails, boolean getAllPages, int perPage){
            List<DescribedQuery> queries = new ArrayList<DescribedQuery>();
            for(ElementsObjectCategory category : categoriesToHarvest){
                queries.add(getQuery(category, fullDetails, getAllPages, perPage));
            }
            return queries;
        }

        protected DescribedQuery getQuery(ElementsObjectCategory category, boolean fullDetails, boolean getAllPages, int perPage) {
            ElementsAPIFeedObjectQuery currentQuery = new ElementsAPIFeedObjectQuery(category, null, getModifiedSince(), fullDetails, getAllPages, perPage);
            return new DescribedQuery(currentQuery, MessageFormat.format("Processing {0}", category.getPlural())){
                @Override
                public ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore){
                    return new ElementsAPI.APIResponseFilter(new FileStoringObjectFilter(objectStore), ElementsAPIVersion.allVersions());
                }
            };
        }
    }

    public static class DeletedObjectConfig extends ObjectConfig{
        public DeletedObjectConfig(boolean getFullDetails, int objectsPerPage, Date modifiedSince, ElementsObjectCategory... categoriesToHarvest){
            this(getFullDetails, objectsPerPage, modifiedSince, Arrays.asList(categoriesToHarvest)); }

        public DeletedObjectConfig(boolean getFullDetails, int objectsPerPage, Date modifiedSince, Collection<ElementsObjectCategory> categoriesToHarvest){
            super(getFullDetails, objectsPerPage, modifiedSince, categoriesToHarvest);
        }

        @Override
        protected DescribedQuery getQuery(ElementsObjectCategory category, boolean fullDetails, boolean getAllPages, int perPage) {
            ElementsAPIFeedObjectQuery.Deleted currentQuery = new ElementsAPIFeedObjectQuery.Deleted(category, getModifiedSince(), getAllPages, perPage);
            return new DescribedQuery(currentQuery, MessageFormat.format("Processing deleted {0}", category.getPlural())){
                @Override
                public ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore){
                    if(!(objectStore instanceof ElementsItemStore.ElementsDeletableItemStore)) throw new IllegalStateException("store must be a ElementsDeletableItemStore if processing deletions");
                    return new ElementsAPI.APIResponseFilter(new FileDeletingObjectFilter((ElementsItemStore.ElementsDeletableItemStore) objectStore), ElementsAPIVersion.allVersions());
                }
            };
        }
    }

    public static class RelationshipConfig extends DeltaCapableFeedConfig{
        public RelationshipConfig(Date modifiedSince, int relationshipsPerPage){
            super(modifiedSince, false, true, relationshipsPerPage);
        }

        @Override
        protected Collection<DescribedQuery> getQueries(boolean fullDetails, boolean getAllPages, int perPage){
            DescribedQuery query = new DescribedQuery(
                new ElementsAPIFeedRelationshipQuery(getModifiedSince(), fullDetails, getAllPages, perPage), "Processing relationships"){
                @Override
                public ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore) {
                    return new ElementsAPI.APIResponseFilter(new FileStoringRelationshipFilter(objectStore), ElementsAPIVersion.allVersions());
                }
            };
            return Arrays.asList(new DescribedQuery[]{query});
        }
    }

    public static class DeletedRelationshipConfig extends RelationshipConfig{
        public DeletedRelationshipConfig(Date deletedSince, int relationshipsPerPage){ super(deletedSince, relationshipsPerPage); }

        @Override
        protected Collection<DescribedQuery> getQueries(boolean fullDetails, boolean getAllPages, int perPage){
            DescribedQuery query = new DescribedQuery(
                    new ElementsAPIFeedRelationshipQuery.Deleted(getModifiedSince(), getAllPages, perPage),"Processing deleted relationships"){
                @Override
                public ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore) {
                    if(!(objectStore instanceof ElementsItemStore.ElementsDeletableItemStore)) throw new IllegalStateException("store must be a ElementsDeletableItemStore if processing deletions");
                    return new ElementsAPI.APIResponseFilter(new FileDeletingRelationshipFilter((ElementsItemStore.ElementsDeletableItemStore) objectStore), ElementsAPIVersion.allVersions());
                }
            };
            return Arrays.asList(new DescribedQuery[]{query});
        }
    }

    public static class ObjectsRelationshipsConfig extends PaginatedFeedConfig{

        private final Set<ElementsItemId.ObjectId> objectsToProcess = new HashSet<ElementsItemId.ObjectId>();

        public ObjectsRelationshipsConfig(Set<ElementsItemId> objectsToProcess, int relationshipsPerPage){
            super(false, true, relationshipsPerPage);
            if(objectsToProcess == null) throw new NullArgumentException("objectsToProcess");
            for(ElementsItemId object : objectsToProcess) {
                if(!(object instanceof ElementsItemId.ObjectId)) throw new IllegalArgumentException("objectsToProcess must only contain item ids representing objects");
                this.objectsToProcess.add((ElementsItemId.ObjectId) object);
            }
        }

        @Override
        protected Collection<DescribedQuery> getQueries(boolean fullDetails, boolean getAllPages, int perPage){
            List<DescribedQuery> queries = new ArrayList<DescribedQuery>();
            for(ElementsItemId.ObjectId objectId : objectsToProcess) {
                final ElementsItemId.ObjectId idForExtractor = objectId;
                DescribedQuery query = new DescribedQuery(
                        new ElementsAPIFeedObjectRelationshipsQuery(objectId, fullDetails, getAllPages, perPage),
                        MessageFormat.format("Re-processing relationships for object {0}", objectId)){
                    @Override
                    public ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore) {
                        //TODO improve how touching is handled - its pretty crudy;
                        if(!(objectStore instanceof ElementsItemStore.ElementsDeletableItemStore)) throw new IllegalStateException("store must be a ElementsDeletableItemStore if processing touches");
                        return new ElementsAPI.APIResponseFilter(new FileTouchingRelationshipFilter((ElementsItemStore.ElementsDeletableItemStore) objectStore, new IdInjectingExtractor(idForExtractor)), ElementsAPIVersion.allVersions());
                    }
                };
                queries.add(query);
            }
            return queries;
        }

        public static class IdInjectingExtractor extends ElementsRelationshipInfo.Extractor{
            private final ElementsItemId.ObjectId sourceObjectId;

            public IdInjectingExtractor(ElementsItemId.ObjectId sourceObjectId){
                super(feedEntryLocation, 0);
                if(sourceObjectId == null) throw new NullArgumentException("sourceObjectId");
                this.sourceObjectId = sourceObjectId;
            }

            @Override
            protected ElementsRelationshipInfo finaliseItemExtraction(EndElement finalElement, XMLEventProcessor.ReaderProxy readerProxy){
                ElementsRelationshipInfo info = super.finaliseItemExtraction(finalElement, readerProxy);
                info.addObjectId(sourceObjectId);
                return info;
            }
        }
    }


    public static class GroupConfig extends FetchConfig{
        @Override
        public Collection<DescribedQuery> getQueries(){
            DescribedQuery query = new DescribedQuery(new ElementsAPIFeedGroupQuery(), "Processing groups"){
                @Override
                public ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore) {
                    return new ElementsAPI.APIResponseFilter(new FileStoringGroupFilter(objectStore), ElementsAPIVersion.allVersions());
                }
            };
            return Arrays.asList(new DescribedQuery[]{query});
        }
    }

    public static class GroupMembershipConfig extends FetchConfig{
        private final int groupId;

        public GroupMembershipConfig(int groupId){ this.groupId = groupId; }

        @Override
        public Collection<DescribedQuery> getQueries(){
            DescribedQuery query = new DescribedQuery(new ElementsAPIFeedObjectQuery.GroupMembershipQuery(groupId),
                    MessageFormat.format("Processing group membership for group : {0}", groupId)){
                @Override
                public ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore) {
                    return new ElementsAPI.APIResponseFilter(new FileStoringObjectFilter(objectStore), ElementsAPIVersion.allVersions());
                }
            };
            return Arrays.asList(new DescribedQuery[]{query});
        }
    }

    /**
     * SLF4J Logger
     */
    private static Logger log = LoggerFactory.getLogger(ElementsFetch.class);
    //the api to fetch data from
    final private ElementsAPI elementsAPI;

    public ElementsFetch(ElementsAPI api) {
        if (api == null) throw new NullArgumentException("api");
        this.elementsAPI = api;
    }

    /**
     * Executes the task
     * @throws IOException error processing search
     */
    public void execute(FetchConfig config, ElementsItemStore objectStore)throws IOException {
        if(config == null) throw new NullArgumentException("config");
        if (objectStore == null) throw new NullArgumentException("objectStore");
        for (FetchConfig.DescribedQuery describedQuery : config.getQueries()) {
            if (describedQuery != null) {
                log.info(describedQuery.description);
                elementsAPI.executeQuery(describedQuery.query, describedQuery.getExtractor(objectStore));
            }
        }
    }
}

