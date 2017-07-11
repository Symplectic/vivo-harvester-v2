/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.elements.api.queries;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.elements.api.ElementsAPIURLBuilder;
import uk.co.symplectic.elements.api.ElementsFeedQuery;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import java.util.*;

/**
 * Query representing retrieving data about a particular category of objects from Elements
 * optionally only items modified since a particular datetime.
 */
public class ElementsAPIFeedObjectQuery extends ElementsFeedQuery.DeltaCapable {

    // How many objects to request per API request: Default of 25 (see constructor chain) is required by 4.6 API since we request full detail for objects
    //private static int defaultPerPage = 25;

    private final ElementsObjectCategory category;
    private final List<Integer> groups = new ArrayList<Integer>();
    //handle as subclasses?
    //TODO: make this flag properly useable instead of hard coded to true..
    private boolean approvedObjectsOnly = true;
    private boolean explicitMembersOnly = false;

    public ElementsAPIFeedObjectQuery(ElementsObjectCategory category, boolean fullDetails, Date modifiedSince) {
        this(category, fullDetails, modifiedSince, null, false);
    }

    /**
     * protected constructor supports concept of limiting query to specific groups of users to allow for subclasses
     * that can query group membership.
     * @param category
     * @param fullDetails
     * @param modifiedSince
     * @param groupsToInclude
     * @param explicitMembersOnly
     */
    protected ElementsAPIFeedObjectQuery(ElementsObjectCategory category, boolean fullDetails, Date modifiedSince, Collection<Integer> groupsToInclude, boolean explicitMembersOnly) {
        super(ElementsItemType.OBJECT, fullDetails, modifiedSince);
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

    @Override
    protected Set<String> getUrlStrings(String apiBaseUrl, ElementsAPIURLBuilder builder, int perPage){
        return Collections.singleton(builder.buildObjectFeedQuery(apiBaseUrl, this, perPage));
    }

    //TODO: move these into the app?

    /**
     * Subclass of the ElementsAPIFeedObjectQuery representing querying items that have been deleted.
     */
    public static class Deleted extends ElementsAPIFeedObjectQuery{
        public Deleted(ElementsObjectCategory category, Date deletedSince) {
            super(category, false, deletedSince);
        }

        @Override
        public boolean queryRepresentsDeletedItems(){ return true;}
    }

    /**
     * Subclass of the ElementsAPIFeedObjectQuery querying users that are explicit members of
     * the specified user group within Elements.
     */
    public static class GroupMembershipQuery extends ElementsAPIFeedObjectQuery{
        public GroupMembershipQuery(int groupID){
            super(ElementsObjectCategory.USER, false, null, Collections.singletonList(groupID), true);
        }
    }
}
