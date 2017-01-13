/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api.queries;

import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsFeedQuery;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import java.util.*;

public class ElementsAPIFeedRelationshipQuery extends ElementsFeedQuery.DeltaCapable {

    // How many relationships to request per API request:Default of 100 used for optimal performance (see constructor chain)
    private static int defaultPerPage = 100;

    public ElementsAPIFeedRelationshipQuery(Date modifiedSince, boolean fullDetails, boolean processAllPages) {
        this(modifiedSince, fullDetails, processAllPages, ElementsAPIFeedRelationshipQuery.defaultPerPage);
    }


    public ElementsAPIFeedRelationshipQuery(Date modifiedSince, boolean fullDetails, boolean processAllPages, int perPage) {
        super(modifiedSince, fullDetails, processAllPages, perPage);
    }

    public boolean getQueryDeletedObjects(){ return false;}

    @Override
    protected String getUrlString(String apiBaseUrl, ElementsAPIURLBuilder builder){
        return builder.buildRelationshipFeedQuery(apiBaseUrl, this, null);
    }


    public static class Deleted extends ElementsAPIFeedRelationshipQuery{
        public Deleted(Date deletedSince, boolean processAllPages) {
            this(deletedSince, processAllPages, 100);
        }

        public Deleted(Date deletedSince, boolean processAllPages, int perPage) {
            super(deletedSince, false, processAllPages, perPage);
        }

        @Override
        public boolean getQueryDeletedObjects(){ return true;}
    }

    public static class IdList extends ElementsAPIFeedRelationshipQuery{
        List<Integer> relationshipIds = new ArrayList<Integer>();

        public IdList(Set<ElementsItemId.RelationshipId> ids){
            super(null, false, true, 100);
            if(ids == null || ids.isEmpty()) throw new IllegalArgumentException("ids must not be null or empty");
            for(ElementsItemId id : ids){
                relationshipIds.add(id.getId());
            }
        }

        @Override
        public QueryIterator getQueryIterator(String apiBaseUrl, ElementsAPIURLBuilder builder){
            Set<String> queries = new HashSet<String>();

            int counter = 0;
            int maxIndex;
            do{
                //do batches in the amount set in per page..
                maxIndex = Math.min((counter+1)*getPerPage(), relationshipIds.size());
                List<Integer> batch = relationshipIds.subList(counter*100, maxIndex);
                queries.add(builder.buildRelationshipFeedQuery(apiBaseUrl, this, new HashSet<Integer>(batch)));
                counter++;
            } while (maxIndex != relationshipIds.size());
            return new QueryIterator(queries);
        }
    }
}
