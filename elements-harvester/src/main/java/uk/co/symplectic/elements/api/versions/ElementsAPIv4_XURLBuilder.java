/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api.versions;

import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedGroupQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedObjectQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedRelationshipQuery;
import uk.co.symplectic.utils.URLBuilder;
import java.text.SimpleDateFormat;
import java.util.Set;

public class ElementsAPIv4_XURLBuilder extends ElementsAPIURLBuilder.GenericBase {
    @Override

    public String buildObjectFeedQuery(String endpointUrl, ElementsAPIFeedObjectQuery feedQuery) {
        URLBuilder queryUrl = new URLBuilder(endpointUrl);

        if(feedQuery.getQueryDeletedObjects()){
            queryUrl.appendPath("deleted");
        }

        if (feedQuery.getCategory() != null) {
            queryUrl.appendPath(feedQuery.getCategory().getPlural());
        } else {
            queryUrl.appendPath("objects");
        }

        if (feedQuery.getGroups().size() != 0) {
            queryUrl.addParam("groups", convertIntegerArrayToQueryString(feedQuery.getGroups()));

            if (feedQuery.getExplicitMembersOnly()) {
                queryUrl.addParam("group-membership", "explicit");
            }
        }

        if (feedQuery.getApprovedObjectsOnly()) {
            queryUrl.addParam("ever-approved", "true");
        }

        if (feedQuery.getFullDetails()) {
            queryUrl.addParam("detail", "full");
        }

        if (feedQuery.getPerPage() > 0) {
            queryUrl.addParam("per-page", calculatePerPage(feedQuery.getPerPage(), feedQuery.getFullDetails()));
        }

        if (feedQuery.getModifiedSince() != null) {
            String modifiedSinceString = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(feedQuery.getModifiedSince());
            if(feedQuery.getQueryDeletedObjects())
                queryUrl.addParam("deleted-since", modifiedSinceString);
            else {
                queryUrl.addParam("modified-since", modifiedSinceString);
                //TODO: this needs to exist on the equivalent deleted resource to make things better (really needs to be a continuation token)
                //TODO: also same concepts need extending to relationships and deleted relationships - relationships in particular definitely need it for deleted I think.
                queryUrl.addParam("order-by", "id");
            }
            //always order by id if using modified since..

        }

        //hack in a page for testing
        //queryUrl.addParam("page", Integer.toString(feedQuery.page));

        return queryUrl.toString();
    }

    @Override
    public String buildRelationshipFeedQuery(String endpointUrl, ElementsAPIFeedRelationshipQuery feedQuery, Set<Integer> relationshipIds) {
        URLBuilder queryUrl = new URLBuilder(endpointUrl);

        queryUrl.appendPath("relationships");

        if(feedQuery.getQueryDeletedObjects()){
            queryUrl.appendPath("deleted");
        }

        if (feedQuery.getFullDetails()) {
            queryUrl.addParam("detail", "full");
        }

        if (feedQuery.getPerPage() > 0) {
            queryUrl.addParam("per-page", calculatePerPage(feedQuery.getPerPage(), feedQuery.getFullDetails()));
        }

        if(relationshipIds != null && !relationshipIds.isEmpty()){
            queryUrl.addParam("ids", convertIntegerArrayToQueryString(relationshipIds));
        }
        else if (feedQuery.getModifiedSince() != null) {
            String modifiedSinceString = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(feedQuery.getModifiedSince());
            if(feedQuery.getQueryDeletedObjects())
                queryUrl.addParam("deleted-since", modifiedSinceString);
            else {
                queryUrl.addParam("modified-since", modifiedSinceString);
                //TODO: this needs to exist on the equivalent deleted resource to make things better (really needs to be a continuation token)
                //TODO: also same concepts need extending to relationships and deleted relationships - relationships in particular definitely need it for deleted I think.
                //queryUrl.addParam("order-by", "id");
            }
            //always order by id if using modified since..

        }

        //hack in a page for testing
        //queryUrl.addParam("types", Integer.toString(83));

        return queryUrl.toString();
    }

    private String calculatePerPage(int requestedPerPage, boolean fullDetails){
        int maxPerPageAllowed = fullDetails ? 25 : 1000; //v4.6 introduced a new maximum per page of 25 for full detail
        return Integer.toString(Math.min(requestedPerPage, maxPerPageAllowed));
    }


    public String buildGroupQuery(String endpointUrl, ElementsAPIFeedGroupQuery feedQuery){
        URLBuilder queryUrl = new URLBuilder(endpointUrl);
        queryUrl.appendPath("groups");
        return queryUrl.toString();
    }
}
