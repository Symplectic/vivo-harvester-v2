/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.elements.api;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

import java.util.*;

/**
 * Abstract class representing a query against the Elements API that will return a response containig API XML data
 * encapsulates the concepts of the "Type" of data being requested and whether the response should be returned at
 * "full" or "ref" level detail.
 */
abstract public class ElementsFeedQuery {

    //occasionally useful hack for testing.
    //public int page;

    private final boolean fullDetails;
    private final ElementsItemType itemType;

    /**
     *
     * @param fullDetails to request whether the feed contain full object details or reference level information
     */
    public ElementsFeedQuery(ElementsItemType itemType, boolean fullDetails){
        if(itemType == null) throw new NullArgumentException("itemType");
        this.itemType = itemType;
        this.fullDetails = fullDetails;
    }


    /**
     * Should the feed contain full object details
     *
     * @return true if the feed should include full object details, false if not
     */
    public boolean getFullDetails() {
        return fullDetails;
    }
    public ElementsItemType getItemType() { return itemType; }

    //Whether the query represents "deleted" items this is assumed to be false.
    //subclasses where this is true should override this  method appropriately.
    public boolean queryRepresentsDeletedItems() { return false; }


    /**
     * Call to convert this particular query into a set of URLs using the passed in builder to account for version differences.
     * Will normally just return a single url, but in the general case could be a set.
     * Leverages the abstract getUrlStrings method that concrete subclasses should implement.
     * @param apiBaseUrl the base url of the api you want to query
     * @param builder an api version specific builder that knows how to construct different types of query URL.
     * @return : a QueryIterator that will loop through all the queries that are need to be made to the API to process this FeedQuery
     */
    QueryIterator getQueryIterator(String apiBaseUrl, ElementsAPIURLBuilder builder, ElementsAPI.ProcessingOptions options) {
        Set<String> urls = getUrlStrings(apiBaseUrl, builder, options.getPerPage());
        return new QueryIterator(options, urls);
    }

    /**
     * Call to convert this particular query into (generically) a set of URLs to be fetched
     * Note: each URL in the set may need to have several pages fetched to retrieve all the data for that query.
     * uses the passed in builder to account for version differences.
     * @param apiBaseUrl the base url of the api you want to query
     * @param builder an api version specific builder that knows how to construct different types of query URL.
     * @param perPage the number of items to retrieve per page (not always used by the builder).
     * @return a set of strings that describe the distinct queries that need to be made to complete this feedquery
     *         note that each query in the set may have multiple pages that need to be fetched (this is covered by the query iterator)
     */
    protected abstract Set<String> getUrlStrings(String apiBaseUrl, ElementsAPIURLBuilder builder, int perPage);


    /**
     * Intermediate abstract class representing the concept of a query against a resource that supports the ability
     * to request only data that has changed since a known datetime.
     */
    public abstract static class DeltaCapable extends ElementsFeedQuery{
        private final Date modifiedSince;

        public Date getModifiedSince(){return modifiedSince;}

        public DeltaCapable(ElementsItemType itemType, boolean fullDetails, Date modifiedSince){
            super(itemType, fullDetails);
            this.modifiedSince = modifiedSince;
        }
    }

    /**
     * Class that abstracts over the concept of a feedQuery returning potentially multiple query urls, each of which
     * may have (generally) mutliple pages to be processed.
     * exposes a hasNext and a next operation in line with typical Java iterator patterns
     * both of these accept an ElementsFeedPagination object which should have been extracted from the previous query in the loop.
     */
    class QueryIterator{

        private final Iterator<String> queryIterator;
        private final ElementsAPI.ProcessingOptions processingOptions;

        //public QueryIterator(String... queries){ this(new HashSet<String>(Arrays.asList(queries))); }

        private QueryIterator(ElementsAPI.ProcessingOptions processingOptions, Set<String> queries){
            if(processingOptions == null) throw new NullArgumentException("processingOptions");
            if(queries == null || queries.isEmpty()) throw new IllegalArgumentException("queries must not be null or empty");
            this.queryIterator = queries.iterator();
            this.processingOptions = processingOptions;
        }

        private boolean hasNextPage(ElementsFeedPagination pagination){
            return processingOptions.getProcessAllPages() && pagination != null && pagination.getNextURL() != null;
        }

        boolean hasNext(ElementsFeedPagination pagination) {
            return hasNextPage(pagination) || queryIterator.hasNext();
        }

        String next(ElementsFeedPagination pagination){
            if(hasNextPage(pagination))
                return pagination.getNextURL();
            else if(queryIterator.hasNext())
                return queryIterator.next();
            throw new IllegalStateException("invalid use of QueryIterator");
        }
    }
}
