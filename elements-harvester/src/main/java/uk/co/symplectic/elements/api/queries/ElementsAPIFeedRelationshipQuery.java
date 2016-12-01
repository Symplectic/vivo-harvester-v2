/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api.queries;

import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsFeedQuery;

import java.util.Date;

public class ElementsAPIFeedRelationshipQuery extends ElementsFeedQuery.DeltaCapable {

    // How many relationships to request per API request:Default of 100 used for optimal performance (see constructor chain)
    private static int defaultPerPage = 100;

    public ElementsAPIFeedRelationshipQuery(Date modifiedSince, boolean fullDetails, boolean processAllPages) {
        this(modifiedSince, fullDetails, processAllPages, ElementsAPIFeedRelationshipQuery.defaultPerPage);
    }


    public ElementsAPIFeedRelationshipQuery(Date modifiedSince, boolean fullDetails, boolean processAllPages, int perPage) {
        super(modifiedSince, fullDetails, processAllPages, perPage);
    }

    //TODO: make this flag properly useable instead of hard coded to true..
    public boolean getQueryDeletedObjects(){ return false;}

    @Override
    public String getUrlString(String apiBaseUrl, ElementsAPIURLBuilder builder){
        return builder.buildRelationshipFeedQuery(apiBaseUrl, this);
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
}
