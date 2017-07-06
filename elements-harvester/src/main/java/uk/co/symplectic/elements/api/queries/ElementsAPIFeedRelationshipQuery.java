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
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

import java.util.*;

public class ElementsAPIFeedRelationshipQuery extends ElementsFeedQuery.DeltaCapable {

    // How many relationships to request per API request:Default of 100 used for optimal performance (see constructor chain)
    private static int defaultPerPage = 100;

    List<Integer> relationshipTypeIds = new ArrayList<Integer>();

    public ElementsAPIFeedRelationshipQuery(boolean fullDetails, Date modifiedSince, Set<ElementsItemId.RelationshipTypeId> relTypeIds) {
        super(ElementsItemType.RELATIONSHIP, fullDetails, modifiedSince);
        if(relTypeIds != null) {
            for (ElementsItemId.RelationshipTypeId id : relTypeIds) {
                relationshipTypeIds.add(id.getId());
            }
        }
    }

    public List<Integer> getRelationshipTypeIds(){ return relationshipTypeIds;}

    @Override
    protected Set<String> getUrlStrings(String apiBaseUrl, ElementsAPIURLBuilder builder, int perPage){
        return Collections.singleton(builder.buildRelationshipFeedQuery(apiBaseUrl, this, perPage));
    }

    public static class Deleted extends ElementsAPIFeedRelationshipQuery{
        public Deleted(Date deletedSince, Set<ElementsItemId.RelationshipTypeId> relTypeIds) {
            super(false, deletedSince, relTypeIds);
        }

        @Override
        public boolean queryRepresentsDeletedItems(){ return true;}
    }

    public static class IdList extends ElementsAPIFeedRelationshipQuery{
        List<Integer> relationshipIds = new ArrayList<Integer>();

        public IdList(Set<ElementsItemId.RelationshipId> ids){
            super(false, null, null);
            if(ids == null || ids.isEmpty()) throw new IllegalArgumentException("ids must not be null or empty");
            for(ElementsItemId id : ids){
                relationshipIds.add(id.getId());
            }
        }

        @Override
        public Set<String> getUrlStrings(String apiBaseUrl, ElementsAPIURLBuilder builder, int perPage){
            Set<String> queries = new HashSet<String>();
            int counter = 0;
            int maxIndex;
            do{
                //do batches in the amount set in per page..
                maxIndex = Math.min((counter+1)*perPage, relationshipIds.size());
                List<Integer> batch = relationshipIds.subList(counter*100, maxIndex);
                queries.add(builder.buildRelationshipFeedQuery(apiBaseUrl, this, new HashSet<Integer>(batch)));
                counter++;
            } while (maxIndex != relationshipIds.size());
            return queries;
        }
    }
}
