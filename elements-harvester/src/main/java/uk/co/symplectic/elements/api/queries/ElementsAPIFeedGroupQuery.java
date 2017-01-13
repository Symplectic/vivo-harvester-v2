/**
 * Created by ajpc2_000 on 25/07/2016.
 */


package uk.co.symplectic.elements.api.queries;

import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsFeedQuery;

public class ElementsAPIFeedGroupQuery extends ElementsFeedQuery {
    public ElementsAPIFeedGroupQuery(){
        //groups resource has no concept of ref/full detail level..
        //groups resource is not paginated..
        super(false, false, 0);
    }

    @Override
    protected String getUrlString(String apiBaseUrl, ElementsAPIURLBuilder builder) {
        return builder.buildGroupQuery(apiBaseUrl, this);
    }
}
