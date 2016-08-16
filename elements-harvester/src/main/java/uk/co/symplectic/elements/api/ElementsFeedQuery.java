/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api;

abstract public class ElementsFeedQuery {

    //occasionally useful hack for testing.
    //public int page;

    private boolean fullDetails = false;
    private boolean processAllPages = false;
    private int perPage = -1;

    public ElementsFeedQuery() {
        super();
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
     * Should the feed return full object details
     *
     * @param fullDetails true to return full details, false for links
     */
    public void setFullDetails(boolean fullDetails) {
        this.fullDetails = fullDetails;
    }

    /**
     * Number of records to return per feed page
     *
     * @param perPage 0 or negative to use feed default
     */
    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    /**
     * Should all the pages in a paginated feed be processed
     *
     * @param processAllPages true to process all pages, false to process only the first
     */
    public void setProcessAllPages(boolean processAllPages) {
        this.processAllPages = processAllPages;
    }

    /**
     * Call to convert this particular query into a URL using the passed in builder to account for version differences.
     * @param apiBaseUrl the base url of the api you want to query
     * @param builder an api version specific builder that knows how to construct different types of query URL.
     * @return
     */
    public abstract String getUrlString(String apiBaseUrl, ElementsAPIURLBuilder builder);
}
