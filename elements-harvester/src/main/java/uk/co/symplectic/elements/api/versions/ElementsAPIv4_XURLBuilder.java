/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.elements.api.versions;

import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedGroupQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedObjectQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedRelationshipQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedRelationshipTypesQuery;
import uk.co.symplectic.utils.http.URLBuilder;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;

//DEPRECATED : replaced by GeneralAPIv4XOr55_URLBuilder

/**
 * Object to perform the task of converting different FeedQueries into appropriate Elements API urls
 * This version performs the task for any Elements API running a v4.X (e.g. 4.6, 4.9) API Endpoint specification.
 */
@SuppressWarnings("unused")
public class ElementsAPIv4_XURLBuilder extends ElementsAPIURLBuilder.GenericBase {
    @Override

    public String buildObjectFeedQuery(String endpointUrl, ElementsAPIFeedObjectQuery feedQuery, int perPage) {
        URLBuilder queryUrl = new URLBuilder(endpointUrl);

        if(feedQuery.queryRepresentsDeletedItems()){
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

        if (perPage > 0) {
            queryUrl.addParam("per-page", calculatePerPage(perPage, feedQuery.getFullDetails()));
        }

        if (feedQuery.getModifiedSince() != null) {
            String modifiedSinceString = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(feedQuery.getModifiedSince());
            if(feedQuery.queryRepresentsDeletedItems())
                queryUrl.addParam("deleted-since", modifiedSinceString);
            else {
                queryUrl.addParam("modified-since", modifiedSinceString);
            }
        }

        //if we are not querying deleted objects we should order by id (regardless of whether we are doing a delta or a full pull
        if(!feedQuery.queryRepresentsDeletedItems()) {
            //WARNING: that order-by does not exist on the deleted resources means they are particularly susceptible to missing items.
            // The same issues occur for the relationships and deleted relationships resources
            // relationships in particular need it for deleted I think.
            // pagination needs to move to a continuation token in future Elements API releases
            queryUrl.addParam("order-by", "id");
        }

        //hack in a page for testing
        //queryUrl.addParam("page", Integer.toString(feedQuery.page));

        return queryUrl.toString();
    }

    private URLBuilder buildGenericRelationshipQuery(String endpointUrl, ElementsAPIFeedRelationshipQuery feedQuery, int perPage){
        URLBuilder queryUrl = new URLBuilder(endpointUrl);

        queryUrl.appendPath("relationships");

        if(feedQuery.queryRepresentsDeletedItems()){
            queryUrl.appendPath("deleted");
        }

        if (feedQuery.getFullDetails()) {
            queryUrl.addParam("detail", "full");
        }

        if (perPage > 0) {
            queryUrl.addParam("per-page", calculatePerPage(perPage, feedQuery.getFullDetails()));
        }
        return queryUrl;
    }


    @Override
    public String buildRelationshipFeedQuery(String endpointUrl, ElementsAPIFeedRelationshipQuery feedQuery, Set<Integer> relationshipIds) {
        if(relationshipIds == null || relationshipIds.size() == 0) throw new IllegalArgumentException("relationshipIds must not be null or empty");
        URLBuilder queryUrl = buildGenericRelationshipQuery(endpointUrl, feedQuery, relationshipIds.size());
        queryUrl.addParam("ids", convertIntegerArrayToQueryString(relationshipIds));
        return queryUrl.toString();
    }

    @Override
    public String buildRelationshipFeedQuery(String endpointUrl, ElementsAPIFeedRelationshipQuery feedQuery, int perPage) {
        URLBuilder queryUrl = buildGenericRelationshipQuery(endpointUrl, feedQuery, perPage);

        List<Integer> relTypeIds = feedQuery.getRelationshipTypeIds();
        if(relTypeIds != null && !relTypeIds.isEmpty()){
            queryUrl.addParam("types", convertIntegerArrayToQueryString(relTypeIds));
        }

        if (feedQuery.getModifiedSince() != null) {
            String modifiedSinceString = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(feedQuery.getModifiedSince());
            if(feedQuery.queryRepresentsDeletedItems())
                queryUrl.addParam("deleted-since", modifiedSinceString);
            else {
                queryUrl.addParam("modified-since", modifiedSinceString);
                //WARNING: order-by does not exist on the relationships and deleted relationships resources
                // pagination needs to move to a continuation token in future Elements API releases
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

    @Override
    public String buildGroupQuery(String endpointUrl, ElementsAPIFeedGroupQuery feedQuery){
        URLBuilder queryUrl = new URLBuilder(endpointUrl);
        queryUrl.appendPath("groups");
        return queryUrl.toString();
    }

    @Override
    public String buildRelationshipTypesQuery(String endpointUrl, ElementsAPIFeedRelationshipTypesQuery feedQuery){
        URLBuilder queryUrl = new URLBuilder(endpointUrl);
        queryUrl.appendPath("relationship/types");
        return queryUrl.toString();
    }
}
