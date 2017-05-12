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
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedRelationshipTypesQuery;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemStore;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;
import uk.co.symplectic.utils.xml.StAXUtils;
import uk.co.symplectic.utils.xml.XMLEventProcessor;

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

    static abstract class DataProcessingFilter<S, T extends XMLEventProcessor.ItemExtractingFilter<S>> extends XMLEventProcessor.EventFilterWrapper<T> {

        private static final int logProgressEveryN = 1000;

        private XMLEventWriter writer = null;
        private ByteArrayOutputStream dataStream = null;
        private final XMLEventFactory eventFactory = StAXUtils.getXMLEventFactory();
        private final QName rootElement;
        private final String itemDescriptor;
        private int counter = 0;

        DataProcessingFilter(QName rootElement, T innerFilter, String itemDescriptor){
            super(innerFilter);
            this.rootElement = rootElement;
            String trimmedDescriptor = StringUtils.trimToNull(itemDescriptor);
            this.itemDescriptor = trimmedDescriptor == null ? "items" : itemDescriptor;
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
                    log.info(MessageFormat.format("{0} {1} processed and stored", counter, itemDescriptor));
                }

                processItem(item, dataStream.toByteArray());
            }
            catch(IOException e){
                throw new IllegalStateException(e);
            }
        }

        protected abstract void processItem(S item, byte[] data) throws IOException;
    }

    private static class ItemStoringFilter extends DataProcessingFilter<ElementsItemInfo, XMLEventProcessor.ItemExtractingFilter<ElementsItemInfo>> {

        protected static final QName outputRootElement = new QName(ElementsAPI.atomNS, "entry");

        private final StorableResourceType resourceType;
        private final ElementsItemStore objectStore;

        protected ElementsItemStore getObjectStore(){ return objectStore; }
        protected StorableResourceType getResourceType(){ return resourceType; }

        public ItemStoringFilter(StorableResourceType resourceType, ElementsItemStore objectStore) {
            this(outputRootElement, resourceType, ElementsItemInfo.ExtractionSource.FEED, objectStore);
        }

        protected ItemStoringFilter(QName rootElement, StorableResourceType resourceType, ElementsItemInfo.ExtractionSource source, ElementsItemStore objectStore) {
            super(rootElement, ElementsItemInfo.getExtractor(resourceType.getKeyItemType(), source, 0), resourceType.getKeyItemType().getPluralName());
            if (objectStore == null) throw new NullArgumentException("objectStore");
            this.objectStore = objectStore;
            this.resourceType = resourceType;
        }

        @Override
        protected void processItem(ElementsItemInfo item, byte[] data) throws IOException {
            getObjectStore().storeItem(item, getResourceType(), data);
        }
    }

    private static class ItemDeletingFilter extends ItemStoringFilter {
        public ItemDeletingFilter(StorableResourceType resourceType, ElementsItemStore objectStore) {
            super(outputRootElement, resourceType, ElementsItemInfo.ExtractionSource.DELETED_FEED,  objectStore);
            if(!(objectStore instanceof ElementsItemStore.ElementsDeletableItemStore))
                throw new IllegalArgumentException("objectStore must be an ElementsItemStore.ElementsDeletableItemStore when instantiating an ItemDeletingFilter");
        }

        @Override
        protected void processItem(ElementsItemInfo item, byte[] data) throws IOException {
            ((ElementsItemStore.ElementsDeletableItemStore) getObjectStore()).deleteItem(item.getItemId(), getResourceType());
        }
    }

    public abstract static class FetchConfig {
        final private boolean getFullDetails;

        public FetchConfig(boolean getFullDetails){
            this.getFullDetails = getFullDetails;
        }

        public final Collection<DescribedQuery> getQueries(){return getQueries(getFullDetails);}

        protected abstract Collection<DescribedQuery> getQueries(boolean fullDetails);
        //This should return a "new" object not the same one..
        //public abstract ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore);

        public static class DescribedQuery{
            private final ElementsFeedQuery query;
            private final String description;
            protected final StorableResourceType resourceType;

            protected DescribedQuery(ElementsFeedQuery query, StorableResourceType resourceType, String description){
                if(query == null) throw new NullArgumentException("query");
                if(StringUtils.trimToNull(description) == null) throw new IllegalArgumentException("description cannot be null or empty");
                if(resourceType == null) throw new NullArgumentException("resourceType");

                this.query = query;
                this.description = StringUtils.trimToNull(description);
                this.resourceType = resourceType;
            }

            //public abstract ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore);

            protected ElementsAPI.APIResponseFilter wrapFilterAsApiResponse(XMLEventProcessor.EventFilter filter){
                return new ElementsAPI.APIResponseFilter(filter, ElementsAPIVersion.allVersions());
            }

            protected ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore){
                return wrapFilterAsApiResponse(new ItemStoringFilter(resourceType, objectStore));
            }
        }

        public static class DescribedDeletingQuery extends DescribedQuery{
            public DescribedDeletingQuery(ElementsFeedQuery query, StorableResourceType resourceType, String description){
                super(query, resourceType, description);
            }

            protected ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore){
                return wrapFilterAsApiResponse(new ItemDeletingFilter(resourceType, objectStore));
            }
        }
    }

    public abstract static class DeltaCapableFeedConfig extends FetchConfig{
        final private Date modifiedSince;

        protected Date getModifiedSince(){return modifiedSince; }

        public DeltaCapableFeedConfig(boolean getFullDetails, Date modifiedSince){
            super(getFullDetails);
            this.modifiedSince = modifiedSince;
        }
    }

    public static class ObjectConfig extends DeltaCapableFeedConfig {
        //Which categories of Elements objects should be retrieved?
        final private List<ElementsObjectCategory> categoriesToHarvest = new ArrayList<ElementsObjectCategory>();

        public ObjectConfig(boolean getFullDetails, Date modifiedSince, ElementsObjectCategory... categoriesToHarvest){
            this(getFullDetails, modifiedSince, Arrays.asList(categoriesToHarvest));
        }

        public ObjectConfig(boolean getFullDetails, Date modifiedSince, Collection<ElementsObjectCategory> categoriesToHarvest){
            super(getFullDetails, modifiedSince);
            if (categoriesToHarvest == null)  throw new NullArgumentException("categoriesToHarvest");
            if (categoriesToHarvest.isEmpty())  throw new IllegalArgumentException("categoriesToHarvest should not be empty for an ElementsFetch.Configuration");
            this.categoriesToHarvest.addAll(categoriesToHarvest);
        }

        @Override
        protected Collection<DescribedQuery> getQueries(boolean fullDetails){
            List<DescribedQuery> queries = new ArrayList<DescribedQuery>();
            for(ElementsObjectCategory category : categoriesToHarvest){
                queries.add(getQuery(category, fullDetails));
                if(getModifiedSince() != null) queries.add(getDeletedQuery(category));
            }
            return queries;
        }

        private DescribedQuery getQuery(ElementsObjectCategory category, boolean fullDetails) {
            ElementsFeedQuery currentQuery = new ElementsAPIFeedObjectQuery(category, fullDetails, getModifiedSince());
            return new DescribedQuery(currentQuery, StorableResourceType.RAW_OBJECT, MessageFormat.format("Processing {0}", category.getPlural()));
        }

        private DescribedQuery getDeletedQuery(ElementsObjectCategory category) {
            ElementsFeedQuery currentQuery = new ElementsAPIFeedObjectQuery.Deleted(category, getModifiedSince());
            return new DescribedDeletingQuery(currentQuery, StorableResourceType.RAW_OBJECT, MessageFormat.format("Processing deleted {0}", category.getPlural()));
        }
    }

    public static class RelationshipConfig extends DeltaCapableFeedConfig{

        protected final Set<ElementsItemId.RelationshipTypeId> relationshipTypesToInclude = new HashSet<ElementsItemId.RelationshipTypeId>();

        public RelationshipConfig(Date modifiedSince, Set<ElementsItemId> relationshipTypesToInclude){
            super(false, modifiedSince);
            if(relationshipTypesToInclude != null) {
                for (ElementsItemId relationshipId : relationshipTypesToInclude) {
                    if (!(relationshipId instanceof ElementsItemId.RelationshipTypeId))
                        throw new IllegalArgumentException("relationshipTypesToInclude must only contain item ids representing relationship types");
                    this.relationshipTypesToInclude.add((ElementsItemId.RelationshipTypeId) relationshipId);
                }
            }
        }

        @Override
        protected Collection<DescribedQuery> getQueries(boolean fullDetails){
            List<DescribedQuery> queries = new ArrayList<DescribedQuery>();
            queries.add(getQuery(fullDetails));
            if(getModifiedSince() != null) queries.add(getDeletedQuery());
            return queries;
        }

        private DescribedQuery getQuery(boolean fullDetails){
            ElementsFeedQuery currentQuery = new ElementsAPIFeedRelationshipQuery(fullDetails, getModifiedSince(), relationshipTypesToInclude);
            return new DescribedQuery(currentQuery, StorableResourceType.RAW_RELATIONSHIP , "Processing relationships");
        }

        private DescribedQuery getDeletedQuery(){
            ElementsFeedQuery currentQuery = new ElementsAPIFeedRelationshipQuery.Deleted(getModifiedSince(), relationshipTypesToInclude);
            return new DescribedDeletingQuery(currentQuery, StorableResourceType.RAW_RELATIONSHIP, "Processing deleted relationships");
        }
    }

    public static class RelationshipsListConfig extends FetchConfig{

        private final List<ElementsItemId.RelationshipId> relationshipsToProcess = new ArrayList<ElementsItemId.RelationshipId>();

        public RelationshipsListConfig(Set<ElementsItemId> relationshipsToProcess){
            super(false);
            if(relationshipsToProcess == null) throw new NullArgumentException("relationshipsToProcess");
            for(ElementsItemId relationshipId : relationshipsToProcess) {
                if(!(relationshipId instanceof ElementsItemId.RelationshipId))
                    throw new IllegalArgumentException("relationshipsToProcess must only contain item ids representing relationships");
                this.relationshipsToProcess.add((ElementsItemId.RelationshipId) relationshipId);
            }
        }

        @Override
        protected Collection<DescribedQuery> getQueries(boolean fullDetails){
            String description = MessageFormat.format("Re-pulling {0} relationships for modified objects", relationshipsToProcess.size());
            DescribedQuery query = new DescribedQuery(new ElementsAPIFeedRelationshipQuery.IdList(new HashSet<ElementsItemId.RelationshipId>(relationshipsToProcess)),
                    StorableResourceType.RAW_RELATIONSHIP, description);
            return Collections.singletonList(query);
        }
    }

    public static class GroupConfig extends FetchConfig{

        public GroupConfig(){super(false);}

        @Override
        public Collection<DescribedQuery> getQueries(boolean fullDetails){
            DescribedQuery query = new DescribedQuery(new ElementsAPIFeedGroupQuery(), StorableResourceType.RAW_GROUP,"Processing groups");
            return Collections.singletonList(query);
        }
    }

    public static class GroupMembershipConfig extends FetchConfig{
        private final int groupId;

        public GroupMembershipConfig(int groupId){
            super(false);
            this.groupId = groupId;
        }

        @Override
        public Collection<DescribedQuery> getQueries(boolean fullDetails){
            DescribedQuery query = new DescribedQuery(new ElementsAPIFeedObjectQuery.GroupMembershipQuery(groupId),
                    StorableResourceType.RAW_OBJECT, MessageFormat.format("Processing group membership for group : {0}", groupId));
            return Collections.singletonList(query);
        }
    }

    public static class RelationshipTypesConfig extends FetchConfig{

        public RelationshipTypesConfig(){super(false);}

        @Override
        public Collection<DescribedQuery> getQueries(boolean fullDetails){
            DescribedQuery query = new DescribedQuery(new ElementsAPIFeedRelationshipTypesQuery(),
                    StorableResourceType.RAW_RELATIONSHIP_TYPES, "Processing relationship types");
            return Collections.singletonList(query);
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

