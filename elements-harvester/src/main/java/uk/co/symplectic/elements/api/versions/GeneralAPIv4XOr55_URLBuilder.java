/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */
package uk.co.symplectic.elements.api.versions;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsAPIVersion;
import uk.co.symplectic.elements.api.ElementsFeedQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedGroupQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedObjectQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedRelationshipQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedRelationshipTypesQuery;
import uk.co.symplectic.utils.http.URLBuilder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;

public class GeneralAPIv4XOr55_URLBuilder extends ElementsAPIURLBuilder.GenericBase {

    public static GeneralAPIv4XOr55_URLBuilder get4XBuilder(){
        return new GeneralAPIv4XOr55_URLBuilder(true, false);
    }

    public static GeneralAPIv4XOr55_URLBuilder get55Builder(boolean useAffectedWhen){
        return new GeneralAPIv4XOr55_URLBuilder(false, useAffectedWhen);
    }

    public final boolean useOrderBy;
    public final boolean useAffectedWhen;

    private GeneralAPIv4XOr55_URLBuilder(boolean useOrderBy, boolean useAffectedWhen){
        this.useOrderBy = useOrderBy;
        this.useAffectedWhen = useAffectedWhen;
    }


    private String getDateTypeString(ElementsFeedQuery.DeltaCapable feedQuery){
        //if deleting it deleted-since
        if(feedQuery.queryRepresentsDeletedItems())
            return "deleted-since";
        //if using affected and its a cats query then affected-since
        if(useAffectedWhen && feedQuery instanceof ElementsAPIFeedObjectQuery)
            return "affected-since";
        //otherwise just retun modified-since
        return "modified-since";
    }

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
            queryUrl.addParam(getDateTypeString(feedQuery), modifiedSinceString);
        }

        //if we are not querying deleted objects we should order by id (regardless of whether we are doing a delta or a full pull
        if(useOrderBy &&!feedQuery.queryRepresentsDeletedItems()) {
            //TODO: this needs to exist on the equivalent deleted resource to make things better (really needs to be a continuation token)
            //TODO: also same concepts need extending to relationships and deleted relationships - relationships in particular definitely need it for deleted I think.
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
            queryUrl.addParam(getDateTypeString(feedQuery), modifiedSinceString);
        }

        //TODO: no point doing order by as only supported by {cats} resources in 4.6 and 4.9
        //5.5 does not need it
        //queryUrl.addParam("order-by", "id");

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
