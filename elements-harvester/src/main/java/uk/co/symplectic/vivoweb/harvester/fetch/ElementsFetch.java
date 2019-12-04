/*
 * ******************************************************************************
 *   Copyright (c) 2019 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

/**
 * Classes designed to facilitate fetching of data from an ElementsAPI object and storing them in a ElementsItemStore object.
 * The actual ElementsFetch implementation is at the very bottom of this file.
 * This class also defines some static inner classes
 *     XMLEventFilters : that are used to process the API output, break it up into entries, parse the ItemInfo and store the item's XML in a store
 *     FetchConfigs : a range of classes designed to facilitate configuring an ElementsFetch object to process a particular type of data.
 */
public class ElementsFetch {

    /**
     * General purpose XMLEventFilterWrapper designed to wrap an ItemExtractingFilter T that extracts an item of type S
     * For every entry processed by the filter. The Raw XML that was parsed to create the extracted item S is internally
     * stored (by outputting all the processed events passing through this wrapper to an XMLOutputStream) and then stored
     * in a byte array. The extracted Item S and the byte array representing the raw data that was parsed to extract S
     * are then passed to an abstract method (processItem).
     * @param <S> Type of Item to be extracted.
     * @param <T> Type of Inner Filter that extracts an Item of type S.
     */
    static abstract class DataProcessingFilter<S, T extends XMLEventProcessor.ItemExtractingFilter<S>> extends XMLEventProcessor.EventFilterWrapper<T> {

        private static final int logProgressEveryN = 1000;

        private XMLEventWriter writer = null;
        private ByteArrayOutputStream dataStream = null;
        private final XMLEventFactory eventFactory = StAXUtils.getXMLEventFactory();
        private final QName rootElement;
        private final String itemDescriptor;
        private int counter = 0;

        /**
         * package private constructor for this Abstract class.
         * @param rootElement the root XML element that should be used to wrap the XML data in the byte array that
         *                    represents each extracted item
         * @param innerFilter the innerFilter that is used to extract S from the raw data represented in the byte array
         * @param itemDescriptor a name to represent the type of extracted items S (used in logging)
         */
        DataProcessingFilter(QName rootElement, T innerFilter, String itemDescriptor){
            super(innerFilter);
            this.rootElement = rootElement;
            String trimmedDescriptor = StringUtils.trimToNull(itemDescriptor);
            this.itemDescriptor = trimmedDescriptor == null ? "items" : itemDescriptor;
        }

        @Override
        protected void postInnerItemStart(XMLEventProcessor.WrappedXmlEvent initialEvent) throws XMLStreamException {
            XMLOutputFactory factory = StAXUtils.getXMLOutputFactory();
            dataStream = new ByteArrayOutputStream();
            writer = factory.createXMLEventWriter(dataStream, "utf-8");
            writer.add(eventFactory.createStartDocument());
            if(rootElement != null) writer.add(eventFactory.createStartElement(rootElement, null, null));
        }

        @Override
        protected void postInnerProcessEvent(XMLEventProcessor.WrappedXmlEvent event, List<QName> relativeLocation) throws XMLStreamException {
            writer.add(event.getRawEvent());
        }

        @Override
        protected void postInnerItemEnd(XMLEventProcessor.WrappedXmlEvent finalEvent) throws XMLStreamException {
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

        /**
         * The abstract method called by the filter with the extracted item S and a byte array containing the raw XML data
         * that S was extracted from wrapped in the the specified rootElement
         * @param item The item (of type S) extracted by the wrapped inner filter.
         * @param data the raw XML from which the item was extracted.
         * @throws IOException if errors occur during processing.
         */
        protected abstract void processItem(S item, byte[] data) throws IOException;
    }

    /**
     * Concrete ItemProcessingFilter designed to process the xml coming out of an ElementsAPI query
     * The filter will parse each entry into an ElementsItemInfo using the relevant Extractor and
     * then pass that ItemInfo along with the raw data it was parsed from on to the specified ElementsItemStore.
     */
    @SuppressWarnings("WeakerAccess")
    private static class ItemProcessingFilter extends DataProcessingFilter<ElementsItemInfo, XMLEventProcessor.ItemExtractingFilter<ElementsItemInfo>> {

        //The default "root" element that will be used when outputting the processed data to a file (see constructor chain)
        protected static final QName outputRootElement = new QName(ElementsAPI.atomNS, "entry");

        private final StorableResourceType resourceType;
        ElementsItemInfo.ExtractionSource extractionSource;
        private final ElementsItemStore objectStore;

        protected ElementsItemStore getObjectStore(){ return objectStore; }
        protected StorableResourceType getResourceType(){ return resourceType; }

        /**
         * Constructor specifying the type of resource being stored.
         * the type of data being parsed and where to store the data.
         * The root XML element used in each stored entry will be the default value, i.e <atom:entry>.
         * @param resourceType The StorableResourceType of the data to be extracted (RAW_XXXX)
         * @param extractionSource Where the data is being extracted from (feed/deleted feed, etc)
         * @param objectStore the store where the extracted data will be stored
         */
        public ItemProcessingFilter(StorableResourceType resourceType, ElementsItemInfo.ExtractionSource extractionSource, ElementsItemStore objectStore) {
            this(outputRootElement, resourceType, extractionSource, objectStore);
        }

        /**
         * Constructor specifying the root XML Element to be used when storing each item, the type of resource being stored.
         * the type of data being parsed and where to store the data.
         * @param rootElement What "root" element should be used to wrap the processed XML data for output?
         * @param resourceType The StorableResourceType of the data to be extracted (RAW_XXXX)
         * @param extractionSource Where the data is being extracted from (feed/ deleted feed, etc)
         * @param objectStore the store where the extracted data will be stored
         */
        @SuppressWarnings("SameParameterValue")
        protected ItemProcessingFilter(QName rootElement, StorableResourceType resourceType, ElementsItemInfo.ExtractionSource extractionSource, ElementsItemStore objectStore) {
            //Note call to ElementsItemInfo.getExtractor in the super line below..
            super(rootElement, ElementsItemInfo.getExtractor(resourceType.getKeyItemType(), extractionSource, 0), resourceType.getKeyItemType().getPluralName());
            if (objectStore == null) throw new NullArgumentException("objectStore");
            //if(resourceType == null) throw new NullArgumentException("resourceType");
            if(extractionSource == null ) throw new NullArgumentException("extractionSource");
            else if(extractionSource == ElementsItemInfo.ExtractionSource.DELETED_FEED && !(objectStore instanceof ElementsItemStore.ElementsDeletableItemStore))
                throw new IllegalArgumentException("objectStore must be an ElementsItemStore.ElementsDeletableItemStore if extraction source is DELETED_FEED");

            this.objectStore = objectStore;
            this.resourceType = resourceType;
            this.extractionSource = extractionSource;
        }

        /**
         * * implementation of superclasses processItem stub that depending on the extraction source will either
         * delete or store the data against the item in the objectStore.
         */
        @Override
        protected void processItem(ElementsItemInfo item, byte[] data) throws IOException {
            if(extractionSource == ElementsItemInfo.ExtractionSource.DELETED_FEED)
                ((ElementsItemStore.ElementsDeletableItemStore) getObjectStore()).deleteItem(item.getItemId(), getResourceType());
            else
                getObjectStore().storeItem(item, getResourceType(), data);
        }
    }

    /**
     * The FetchConfig class and its various subclasses represent the concept of a set or type of data to be retrieved
     * from the ElementsAPI by the ElementsFetch class.
     * Each type of FetchConfig exposes methods that represents generically a set of DescribedQueries to be run against the API
     * along with the "Extractor" Filter that should be used to extract the ElementsItemInfo along with the raw XML data that represents that item.
     * The ElementsFetch class uses these to query the API and put the extracted data into the requested ElementsItemStore.
     */
    @SuppressWarnings("WeakerAccess")
    public abstract static class FetchConfig {
        final private boolean getFullDetails;

        /**
         * FetchConfigs generically understand whether the data being queried should be returned in "full"
         * or just "ref" detail by the ElementsAPI
         * @param getFullDetails whether to request "full" or "ref" level detail from the ElementsAPI.
         */
        public FetchConfig(boolean getFullDetails){
            this.getFullDetails = getFullDetails;
        }

        /**
         * Call to get the set of Queries to be run to complete this FetchConfig
         * Calls into corresponding abstract stub below, which is implemented by child classes
         */
        public final Collection<DescribedQuery> getQueries(){return getQueries(getFullDetails);}

        /**
         * Abstract call to specify the set of DescribedQueries to be run to complete this FetchConfig
         * this exists purely to retain complete encapsulation of the getFullDetails field.
         * This is just the abstract stub that is filled in by concrete sub classes.
         * @param fullDetails whether to request "full" or "ref" level detail from the ElementsAPI.
         * @return The set of Queries to be run to complete this FetchConfig.
         */
        protected abstract Collection<DescribedQuery> getQueries(boolean fullDetails);

        /**
         * Inner class DescribedQuery represents an ElementsFeedQuery along with a basic description to be used during logging.
         * It also incorporates the ability to generate a new Extractor object to act as the filter to process the data
         * returned by the ElementsAPI when processing this specific Query
         */
        public static class DescribedQuery{
            private final ElementsFeedQuery query;
            private final String description;

            protected DescribedQuery(ElementsFeedQuery query, String description){
                if(query == null) throw new NullArgumentException("query");
                if(StringUtils.trimToNull(description) == null) throw new IllegalArgumentException("description cannot be null or empty");

                this.query = query;
                this.description = StringUtils.trimToNull(description);
            }

            protected StorableResourceType getResourceType(){
                switch(query.getItemType()){
                    case OBJECT: return StorableResourceType.RAW_OBJECT;
                    case RELATIONSHIP: return StorableResourceType.RAW_RELATIONSHIP;
                    case GROUP: return StorableResourceType.RAW_GROUP;
                    case RELATIONSHIP_TYPE: return StorableResourceType.RAW_RELATIONSHIP_TYPES;
                    default : throw new IllegalStateException("invalid item type");
                }
            }

            protected ElementsAPI.APIResponseFilter wrapFilterAsApiResponse(XMLEventProcessor.EventFilter filter){
                return new ElementsAPI.APIResponseFilter(filter, ElementsAPIVersion.allVersions());
            }

            /**
             * Method that returns the extractor that can be used for this query.
             * This only works because currently the same extraction code is valid against every supported API endpoint.
             * If and when this changes, this will need to be broken up so that there FetchConfigs, DescribedQueries and
             * the ItemProcessingFilter and the ElementsItemInfo.getExtractor call (used in the ItemProcessingFilter constructor)
             * all know about APIVersions in a similar manner to the API Queries.
             * @param objectStore The store where the extracted data will be put
             * @return an ElementsAPI.APIResponseFilter that can be used to process an XML stream to extract data of the
             * type this query is interested in.
             */
            protected ElementsAPI.APIResponseFilter getExtractor(ElementsItemStore objectStore){
                ElementsItemInfo.ExtractionSource extractionSource = query.queryRepresentsDeletedItems() ? ElementsItemInfo.ExtractionSource.DELETED_FEED : ElementsItemInfo.ExtractionSource.FEED;
                return wrapFilterAsApiResponse(new ItemProcessingFilter(getResourceType(), extractionSource, objectStore));
            }
        }
    }

    /**
     * An interim Abstract version of a FetchConfig that represents the concept of a type of resource that is capable
     * of being queried in a delta manner (i.e. supports modified/affected since semantics)
     */
    @SuppressWarnings("WeakerAccess")
    public abstract static class DeltaCapableFeedConfig extends FetchConfig{
        final private Date modifiedSince;

        protected Date getModifiedSince(){return modifiedSince; }

        public DeltaCapableFeedConfig(boolean getFullDetails, Date modifiedSince){
            super(getFullDetails);
            this.modifiedSince = modifiedSince;
        }
    }

    /**
     * A Fetch Config that represents a set of ElementsObjectCategories to be queried
     * either in full or in a delta manner (if modified-since is not null)
     * Note that for "delta" queries the set of Described queries contains two queries per configured Elements category
     * one to fetch any new or updated objects another to fetch any deleted objects.
     */
    @SuppressWarnings("unused")
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
            return new DescribedQuery(currentQuery, MessageFormat.format("Processing {0}", category.getPlural()));
        }

        private DescribedQuery getDeletedQuery(ElementsObjectCategory category) {
            ElementsFeedQuery currentQuery = new ElementsAPIFeedObjectQuery.Deleted(category, getModifiedSince());
            return new DescribedQuery(currentQuery, MessageFormat.format("Processing deleted {0}", category.getPlural()));
        }
    }

    /**
     * A Fetch Config that represents querying all the relationships of a certain set of types
     * either in full or in a delta manner (if modified-since is not null)
     * * Note that for "delta" queries the set of Described queries contains two queries
     * one to fetch any new or updated relationships and the other to fetch any objects deleted.
     */
    public static class RelationshipConfig extends DeltaCapableFeedConfig{

        final Set<ElementsItemId.RelationshipTypeId> relationshipTypesToInclude = new HashSet<ElementsItemId.RelationshipTypeId>();

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
            return new DescribedQuery(currentQuery, "Processing relationships");
        }

        private DescribedQuery getDeletedQuery(){
            ElementsFeedQuery currentQuery = new ElementsAPIFeedRelationshipQuery.Deleted(getModifiedSince(), relationshipTypesToInclude);
            return new DescribedQuery(currentQuery, "Processing deleted relationships");
        }
    }

    /**
     * A Fetch Config that represents querying a set of specific relationships
     */
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
            DescribedQuery query = new DescribedQuery(
                    new ElementsAPIFeedRelationshipQuery.IdList(new HashSet<ElementsItemId.RelationshipId>(relationshipsToProcess)), description
            );
            return Collections.singletonList(query);
        }
    }

    /**
     * A Fetch Config that represents querying all the user groups definitions in Elements
     */
    public static class GroupConfig extends FetchConfig{

        public GroupConfig(){super(false);}

        @Override
        public Collection<DescribedQuery> getQueries(boolean fullDetails){
            DescribedQuery query = new DescribedQuery(new ElementsAPIFeedGroupQuery(),"Processing groups");
            return Collections.singletonList(query);
        }
    }

    /**
     * A Fetch Config that represents querying all the users that are members of a specific group within Elements
     */
    public static class GroupMembershipConfig extends FetchConfig{
        private final int groupId;

        public GroupMembershipConfig(int groupId){
            super(false);
            this.groupId = groupId;
        }

        @Override
        public Collection<DescribedQuery> getQueries(boolean fullDetails){
            DescribedQuery query = new DescribedQuery(new ElementsAPIFeedObjectQuery.GroupMembershipQuery(groupId),
                    MessageFormat.format("Processing group membership for group : {0}", groupId));
            return Collections.singletonList(query);
        }
    }

    /**
     * A Fetch Config that represents querying all the different types of relationship that exist within Elements
     */
    public static class RelationshipTypesConfig extends FetchConfig{

        public RelationshipTypesConfig(){super(false);}

        @Override
        public Collection<DescribedQuery> getQueries(boolean fullDetails){
            DescribedQuery query = new DescribedQuery(new ElementsAPIFeedRelationshipTypesQuery(), "Processing relationship types");
            return Collections.singletonList(query);
        }
    }

    //**********************************************************
    //Implementation of ElementsFetch class proper, begins here:
    //**********************************************************

    /**
     * SLF4J Logger
     */
    private static Logger log = LoggerFactory.getLogger(ElementsFetch.class);
    //the api to fetch data from
    final private ElementsAPI elementsAPI;

    /**
     * ElementsFetch constructor accepting the ElementsAPI object that will be the source of all data fetched.
     * @param api the ElementsAPI from which data will be fetched
     */
    public ElementsFetch(ElementsAPI api) {
        if (api == null) throw new NullArgumentException("api");
        this.elementsAPI = api;
    }

    /**
     * execute call to process the provided FetchConfig, which will retrieve the specified data from the configured
     * ElementsAPI and store (or delete) it in the provided ElementsItemStore
     * @param config the "FetchConfig" to process
     * @param objectStore where the fetched data should be stored (or deleted).
     * @throws IOException if errors occur.
     */
    @SuppressWarnings("RedundantThrows")
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

    public ElementsAPIVersion getApiVersion(){ return elementsAPI.getVersion(); }
}

