/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api;

import uk.co.symplectic.elements.api.queries.ElementsAPIFeedGroupQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedObjectQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedRelationshipQuery;
import uk.co.symplectic.elements.api.queries.ElementsAPIFeedRelationshipTypesQuery;

import java.util.Collection;
import java.util.Set;

public interface ElementsAPIURLBuilder {
    String buildObjectFeedQuery(String endpointUrl, ElementsAPIFeedObjectQuery feedQuery, int perPage);

    String buildRelationshipFeedQuery(String endpointUrl, ElementsAPIFeedRelationshipQuery feedQuery, Set<Integer> relationshipIds);

    String buildRelationshipFeedQuery(String endpointUrl, ElementsAPIFeedRelationshipQuery feedQuery, int perPage);

    String buildGroupQuery(String endpointUrl, ElementsAPIFeedGroupQuery feedQuery);

    String buildRelationshipTypesQuery(String endpointUrl, ElementsAPIFeedRelationshipTypesQuery feedQuery);

    abstract class GenericBase implements ElementsAPIURLBuilder {
        protected String convertIntegerArrayToQueryString(Collection<Integer> integers){
            //StringUtils.join(integers, ","); simpler? - could potentially remove GenericBase then
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for(Integer anInt : integers){
                if(!first) builder.append(",");
                builder.append(anInt);
                first = false;
            }
            return builder.toString();
        }
    }
}
