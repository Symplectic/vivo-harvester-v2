/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api;

import java.util.Date;

abstract public class ElementsFeedQuery {

    //occasionally useful hack for testing.
    //public int page;

    private final boolean fullDetails;
    private final boolean processAllPages;
    private final int perPage;

    /**
     *
     * @param fullDetails should the feed contain full object details
     * @param processAllPages should all the pages be processed
     * @param perPage An integer, 0 or below uses the feed default
     */
    public ElementsFeedQuery(boolean fullDetails, boolean processAllPages, int perPage) {
        this.fullDetails = fullDetails;
        this.processAllPages = processAllPages;
        this.perPage = perPage;
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
     * Number of records per page in the feed
     *
     * @return An integer, 0 or below uses the feed default
     */
    public int getPerPage() {
        return perPage;
    }

    /**
     * For a query that goes over multiple pages, should all the pages be processed
     *
     * @return true to process all pages, false to only process the first
     */
    public boolean getProcessAllPages() {
        return processAllPages;
    }

    /**
     * Call to convert this particular query into a URL using the passed in builder to account for version differences.
     * @param apiBaseUrl the base url of the api you want to query
     * @param builder an api version specific builder that knows how to construct different types of query URL.
     * @return
     */
    public abstract String getUrlString(String apiBaseUrl, ElementsAPIURLBuilder builder);


    public abstract static class DeltaCapable extends ElementsFeedQuery{
        private final Date modifiedSince;

        public Date getModifiedSince(){return modifiedSince;}

        public DeltaCapable(Date modifiedSince, boolean fullDetails, boolean processAllPages, int perPage){
            super(fullDetails, processAllPages, perPage);
            this.modifiedSince = modifiedSince;
        }
    }
}
