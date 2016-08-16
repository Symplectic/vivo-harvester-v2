/**
 * Created by ajpc2_000 on 25/07/2016.
 */


package uk.co.symplectic.elements.api.queries;

import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsFeedQuery;

public class ElementsAPIFeedGroupQuery extends ElementsFeedQuery {

    public boolean getFullDetails() {
        //groups resource has no concept of ref/full detail level..
        return false;
    }

    /**
     * Number of records per page in the feed
     *
     * @return An integer, 0 or below uses the feed default
     */
    public int getPerPage() {
        //groups resource is not paginated..
        return 0;
    }

    /**
     * For a query that goes over multiple pages, should all the pages be processed
     *
     * @return true to process all pages, false to only process the first
     */
    public boolean getProcessAllPages() {
        //groups resource is not paginated..
        return false;
    }

    @Override
    public String getUrlString(String apiBaseUrl, ElementsAPIURLBuilder builder){
        return builder.buildGroupQuery(apiBaseUrl, this);
    }

}
