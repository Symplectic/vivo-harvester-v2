/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api;

import org.apache.commons.lang.NullArgumentException;

import java.util.*;

abstract public class ElementsFeedQuery {

    //occasionally useful hack for testing.
    //public int page;

    private final boolean fullDetails;

    /**
     *
     * @param fullDetails to request whether the feed contain full object details or reference level information
     */
    public ElementsFeedQuery(boolean fullDetails)  {
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

    /**
     * Call to convert this particular query into a set of URLs using the passed in builder to account for version differences.
     * Will normally just return a single url, but in the general case could be a set.
     * @param apiBaseUrl the base url of the api you want to query
     * @param builder an api version specific builder that knows how to construct different types of query URL.
     * @return
     */
    public QueryIterator getQueryIterator(String apiBaseUrl, ElementsAPIURLBuilder builder, ElementsAPI.ProcessingOptions options) {
        Set<String> urls = getUrlStrings(apiBaseUrl, builder, options.getPerPage());
        return new QueryIterator(options, urls);
    }

    /**
     * Call to convert this particular query into (generically) a set of URLs to be fetched
     * Note: each URL in the set may need to have several pages fetched to retrieve all the data for that query.
     * uses the passed in builder to account for version differences.
     * @param apiBaseUrl the base url of the api you want to query
     * @param builder an api version specific builder that knows how to construct different types of query URL.
     * @param perpage the number of items to retrieve per page (not always used by the builder).
     * @return
     */
    protected abstract Set<String> getUrlStrings(String apiBaseUrl, ElementsAPIURLBuilder builder, int perPage);



    public abstract static class DeltaCapable extends ElementsFeedQuery{
        private final Date modifiedSince;

        public Date getModifiedSince(){return modifiedSince;}

        public DeltaCapable(boolean fullDetails, Date modifiedSince){
            super(fullDetails);
            this.modifiedSince = modifiedSince;
        }
    }

    public class QueryIterator{

        private final Iterator<String> queryIterator;
        private final ElementsAPI.ProcessingOptions processingOptions;

        //public QueryIterator(String... queries){ this(new HashSet<String>(Arrays.asList(queries))); }

        public QueryIterator(ElementsAPI.ProcessingOptions processingOptions, Set<String> queries){
            if(processingOptions == null) throw new NullArgumentException("processingOptions");
            if(queries == null || queries.isEmpty()) throw new IllegalArgumentException("queries must not be null or empty");
            this.queryIterator = queries.iterator();
            this.processingOptions = processingOptions;
        }

        private boolean hasNextPage(ElementsFeedPagination pagination){
            return processingOptions.getProcessAllPages() && pagination != null && pagination.getNextURL() != null;
        }

        public boolean hasNext(ElementsFeedPagination pagination) {
            return hasNextPage(pagination) || queryIterator.hasNext();
        }

        public String next(ElementsFeedPagination pagination){
            if(hasNextPage(pagination))
                return pagination.getNextURL();
            else if(queryIterator.hasNext())
                return queryIterator.next();
            throw new IllegalStateException("invalid use of QueryIterator");
        }
    }
}
