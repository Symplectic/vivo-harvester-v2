/*
 * ******************************************************************************
 *   Copyright (c) 2019 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */

package uk.co.symplectic.elements.api.queries;

import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsFeedQuery;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

import java.util.Collections;
import java.util.Set;

/**
 * FeedQuery representing retrieving data about the "types" of relationships that can exist
 * between different categories of objects within Elements, e.g. "authorship" between users and publications.
 */
public class ElementsAPIFeedRelationshipTypesQuery extends ElementsFeedQuery {
    public ElementsAPIFeedRelationshipTypesQuery(){
        //relationship types resource has no concept of ref/full detail level..
        //relationship types resource is not paginated..
        super(ElementsItemType.RELATIONSHIP_TYPE, false);
    }

    @Override
    protected Set<String> getUrlStrings(String apiBaseUrl, ElementsAPIURLBuilder builder, int perPage) {
        return Collections.singleton(builder.buildRelationshipTypesQuery(apiBaseUrl, this));
    }
}
