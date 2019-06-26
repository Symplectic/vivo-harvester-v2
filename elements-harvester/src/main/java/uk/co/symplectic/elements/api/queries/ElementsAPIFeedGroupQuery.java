/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.elements.api.queries;

import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsFeedQuery;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

import java.util.Collections;
import java.util.Set;

/**
 * FeedQuery representing retrieving information about user groups from within Elements.
 */
public class ElementsAPIFeedGroupQuery extends ElementsFeedQuery {
    public ElementsAPIFeedGroupQuery(){
        //groups resource has no concept of ref/full detail level..
        //groups resource is not paginated..
        super(ElementsItemType.GROUP, false);
    }

    @Override
    protected Set<String> getUrlStrings(String apiBaseUrl, ElementsAPIURLBuilder builder, int perPage) {
        return Collections.singleton(builder.buildGroupQuery(apiBaseUrl, this));
    }
}
