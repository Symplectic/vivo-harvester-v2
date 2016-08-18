/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api.queries;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsFeedQuery;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ElementsAPIFeedObjectQuery extends ElementsFeedQuery {
    // How many objects to request per API request: Default of 25 (see constructor chain) is required by 4.6 API since we request full detail for objects
    private static int defaultPerPage = 25;

    private final ElementsObjectCategory category;
    private final String groups;
    //handle as subclasses?
    private String modifiedSince = null;
    private boolean explicitMembersOnly = false;
    private boolean queryDeletedObjects = false;

    public ElementsAPIFeedObjectQuery(ElementsObjectCategory category, String groupsToInclude, boolean fullDetails, boolean processAllPages) {
        this(category, groupsToInclude, fullDetails, processAllPages, ElementsAPIFeedObjectQuery.defaultPerPage);
    }

    public ElementsAPIFeedObjectQuery(ElementsObjectCategory category, String groupsToInclude, boolean fullDetails, boolean processAllPages, int perPage) {
        super(fullDetails, processAllPages, perPage);
        if(category == null) throw new NullArgumentException("category");
        this.category = category;
        this.groups = StringUtils.trimToNull(groupsToInclude);
    }

    public ElementsObjectCategory getCategory() {
        return category;
    }

    public String getGroups() {
        return groups;
    }

    public boolean getExplicitMembersOnly() {
        return explicitMembersOnly;
    }
    public void setExplicitMembersOnly(boolean explicitMembersOnly) {
        this.explicitMembersOnly = explicitMembersOnly;
    }

    public String getModifiedSince() { return modifiedSince; }
    public void setModifiedSince(String modifiedSince) { this.modifiedSince = modifiedSince; }
    public void setModifiedSince(Date modifiedSince) { this.modifiedSince = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(modifiedSince); }

    public boolean getQueryDeletedObjects(){ return queryDeletedObjects;}
    public void setQueryDeletedObjects(boolean queryDeleted){ queryDeletedObjects = queryDeleted;}

    @Override
    public String getUrlString(String apiBaseUrl, ElementsAPIURLBuilder builder){
        return builder.buildObjectFeedQuery(apiBaseUrl, this);
    }
}
