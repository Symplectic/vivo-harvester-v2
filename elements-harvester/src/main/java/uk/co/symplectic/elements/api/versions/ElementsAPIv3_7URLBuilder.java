/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api.versions;

import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedGroupQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedObjectQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedRelationshipQuery;
import uk.co.symplectic.utils.URLBuilder;

public class ElementsAPIv3_7URLBuilder extends ElementsAPIURLBuilder.GenericBase {
    @Override
    public String buildObjectFeedQuery(String endpointUrl, ElementsAPIFeedObjectQuery feedQuery) {
        URLBuilder queryUrl = new URLBuilder(endpointUrl);

        queryUrl.appendPath("objects");
        if (feedQuery.getCategory() != null) {
            queryUrl.addParam("categories", feedQuery.getCategory().getPlural());
        }

        if (feedQuery.getGroups().size() != 0) {
            queryUrl.addParam("groups", convertIntegerArrayToQueryString(feedQuery.getGroups()));

            if (feedQuery.getExplicitMembersOnly()) {
                queryUrl.addParam("group-membership", "explicit");
            }
        }

        if (feedQuery.getFullDetails()) {
            queryUrl.addParam("detail", "full");
        }

        if (feedQuery.getPerPage() > 0) {
            queryUrl.addParam("per-page", Integer.toString(feedQuery.getPerPage(), 100));
        }

        if (!StringUtils.isEmpty(feedQuery.getModifiedSince())) {
            queryUrl.addParam("modified-since", feedQuery.getModifiedSince());
        }

        return queryUrl.toString();
    }

    @Override
    public String buildRelationshipFeedQuery(String endpointUrl, ElementsAPIFeedRelationshipQuery feedQuery) {
        URLBuilder queryUrl = new URLBuilder(endpointUrl);

        queryUrl.appendPath("relationships");

        if (feedQuery.getFullDetails()) {
            queryUrl.addParam("detail", "full");
        }

        if (feedQuery.getPerPage() > 0) {
            queryUrl.addParam("per-page", Integer.toString(feedQuery.getPerPage(), 100));
        }

        return queryUrl.toString();
    }

    public String buildGroupQuery(String endpointUrl, ElementsAPIFeedGroupQuery feedQuery){
        URLBuilder queryUrl = new URLBuilder(endpointUrl);
        queryUrl.appendPath("groups");
        return queryUrl.toString();
    }
}
