/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api.queries;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsFeedQuery;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;

import java.text.SimpleDateFormat;
import java.util.*;

public class ElementsAPIFeedObjectQuery extends ElementsFeedQuery.DeltaCapable {
    // How many objects to request per API request: Default of 25 (see constructor chain) is required by 4.6 API since we request full detail for objects
    private static int defaultPerPage = 25;

    private final ElementsObjectCategory category;
    private final List<Integer> groups = new ArrayList<Integer>();
    //handle as subclasses?
    //TODO: make this flag properly useable instead of hard coded to true..
    private boolean approvedObjectsOnly = true;
    private boolean explicitMembersOnly = false;

    public ElementsAPIFeedObjectQuery(ElementsObjectCategory category, Collection<Integer> groupsToInclude, Date modifiedSince, boolean fullDetails, boolean processAllPages) {
        this(category, groupsToInclude, modifiedSince, fullDetails, processAllPages, ElementsAPIFeedObjectQuery.defaultPerPage);
    }

    public ElementsAPIFeedObjectQuery(ElementsObjectCategory category, Collection<Integer> groupsToInclude, Date modifiedSince, boolean fullDetails, boolean processAllPages, int perPage) {
        this(category, groupsToInclude, modifiedSince, fullDetails, processAllPages, perPage, false);
    }

    protected ElementsAPIFeedObjectQuery(ElementsObjectCategory category, Collection<Integer> groupsToInclude, Date modifiedSince, boolean fullDetails, boolean processAllPages, int perPage, boolean explicitMembersOnly) {
        super(modifiedSince, fullDetails, processAllPages, perPage);
        if(category == null) throw new NullArgumentException("category");
        this.category = category;
        if(groupsToInclude != null) {
            this.groups.addAll(groupsToInclude);
            this.explicitMembersOnly = explicitMembersOnly;
        }
        //if(modifiedSince != null) this.modifiedSince = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(modifiedSince);
    }

    public ElementsObjectCategory getCategory() {
        return category;
    }

    public List<Integer> getGroups() { return Collections.unmodifiableList(groups); }

    public boolean getExplicitMembersOnly() {
        return explicitMembersOnly;
    }

    public boolean getApprovedObjectsOnly() {
        return approvedObjectsOnly;
    }

    //TODO: make this flag properly useable instead of hard coded to true..
    public boolean getQueryDeletedObjects(){ return false;}

    @Override
    public String getUrlString(String apiBaseUrl, ElementsAPIURLBuilder builder){
        return builder.buildObjectFeedQuery(apiBaseUrl, this);
    }


    public static class Deleted extends ElementsAPIFeedObjectQuery{
        public Deleted(ElementsObjectCategory category, Date deletedSince, boolean processAllPages) {
            this(category, deletedSince, processAllPages, 100);
        }

        public Deleted(ElementsObjectCategory category, Date deletedSince, boolean processAllPages, int perPage) {
            super(category, null, deletedSince, false, processAllPages, perPage);
        }

        @Override
        public boolean getQueryDeletedObjects(){ return true;}
    }

    //TODO: move these into the app?
    public static class GroupMembershipQuery extends ElementsAPIFeedObjectQuery{

        //TODO: make per-page default better and move to config...
        public GroupMembershipQuery(int groupID){ this(groupID, 100); }

        public GroupMembershipQuery(int groupID, int perPage){
            super(ElementsObjectCategory.USER, Arrays.asList(groupID), null, false, true, perPage, true);
        }
    }
}
