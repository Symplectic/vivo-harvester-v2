/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api.queries;

import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsFeedQuery;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;

public class ElementsAPIFeedObjectQuery extends ElementsFeedQuery {

    private ElementsObjectCategory category = null;
    private String groups = null;
    private String modifiedSince = null;
    private boolean explicitMembersOnly = false;

    public ElementsAPIFeedObjectQuery() {
        super();
    }

    public ElementsObjectCategory getCategory() {
        return category;
    }

    public String getGroups() {
        return groups;
    }

    public String getModifiedSince() {
        return modifiedSince;
    }

    public void setCategory(ElementsObjectCategory category) {
        this.category = category;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

    public void setModifiedSince(String modifiedSince) {
        this.modifiedSince = modifiedSince;
    }

    public boolean getExplicitMembersOnly() {
        return explicitMembersOnly;
    }

    public void setExplicitMembersOnly(boolean explicitMembersOnly) {
        this.explicitMembersOnly = explicitMembersOnly;
    }

    @Override
    public String getUrlString(String apiBaseUrl, ElementsAPIURLBuilder builder){
        return builder.buildObjectFeedQuery(apiBaseUrl, this);
    }
}
