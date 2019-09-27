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

package uk.co.symplectic.vivoweb.harvester.utils;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.fetch.ElementsFetch;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItemInfo;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A Class representing a set of Elements Groups in a map, keyed by GroupID.
 * The Groups are represented as GroupHierarchyWrapper objects which can enrich the GroupInfo with knowledge about the
 * groups position in the tree (links to parent and child objects and user membership).
 */
public class ElementsGroupCollection extends ElementsItemKeyedCollection<ElementsGroupInfo.GroupHierarchyWrapper> {
    private ElementsGroupInfo.GroupHierarchyWrapper topLevel = null;
    private boolean membershipPopulated = false;

    private Map<ElementsItemId.ObjectId, Set<ElementsItemId.GroupId>> userGroupMap = new HashMap<ElementsItemId.ObjectId, Set<ElementsItemId.GroupId>>();

    public ElementsGroupCollection(){super(new RestrictToType(ElementsItemType.GROUP));}

    /**
     * Get the top level "organisation" group - can only be called after constructHierarchy has been run.
      * @return the GroupHierarchyWrapper of the top level "organisation" group
     */
    public synchronized ElementsGroupInfo.GroupHierarchyWrapper GetTopLevel(){
        if(topLevel == null) throw new IllegalStateException("must construct group hierarchy before requesting top level");
        return topLevel;
    }

    /**
     * Once you have a GroupCollection loaded with all the Elements groups in your system this method will
     * analyse the GroupInfo's and wire the GroupHierarchyWrappers up into a tree by populating the parent object links
     * (which automatically populates the child links)
     *
     * @return the top level "organisation" group at the pinnacle of the group tree.
     */
    @SuppressWarnings("UnusedReturnValue")
    public synchronized ElementsGroupInfo.GroupHierarchyWrapper constructHierarchy(){
        if(topLevel == null) {
            for (ElementsGroupInfo.GroupHierarchyWrapper group : this.values()) {
                ElementsItemId parentGroupId = group.getGroupInfo().getParentId();
                if (parentGroupId != null) {
                    ElementsGroupInfo.GroupHierarchyWrapper parentGroup = get(parentGroupId);
                    group.setParent(parentGroup);
                }
            }

            //find top level group in hierarchy, error out if there is more than one
            for (ElementsGroupInfo.GroupHierarchyWrapper group : this.values()) {
                if (group.getParent() == null) {
                    if (topLevel == null) {
                        topLevel = group;
                    } else {
                        topLevel = null;
                        throw new IllegalStateException("Invalid Group Hierarchy detected");
                    }
                }
            }
            if (topLevel == null)
                throw new IllegalStateException("Invalid Group Hierarchy detected");

            if (topLevel.getGroupInfo().getItemId().getId() != 1)
                throw new IllegalStateException("Invalid Group Hierarchy detected - organisation group not topLevel");
        }
        return topLevel;
    }

    /**
     * method to populate the GroupHierarchyWrappers in the collection with user membership information.
     * It does this by calling the Elements API and requesting information about which users are explicit members of
     * each group. It therefore needs an ElementsFetch object to complete these queries.
     * It also accepts a set or ElementsItemId representing all the users in the Elements system.
     * This is necessary to successfully create the "explicit" users of the organisation group as the API does not return
     * these in an easily usable manner.
     * @param fetcher The ElementsFetch object to provide access to fetching group membership info from the ElementsAPI.
     * @param systemUsers A set of ElementsItemId listing all Elements users - used to establish who is an explicit member
     *                    of the organisation group (all users with no other explicit group memberships
     *                    This mimics Elements primary group behaviour.
     * @throws IOException if there are errors fetching the data.
     */
    public synchronized void populateUserMembership(ElementsFetch fetcher, Set<ElementsItemId> systemUsers) throws IOException {
        if(topLevel == null) throw new IllegalStateException("must construct group hierarchy before populating membership");
        if(!membershipPopulated) {
            //fetch memberships from the API
            for (ElementsGroupInfo.GroupHierarchyWrapper group : this.values()) {
                //take care with == here - this really is the same reference...
                if (topLevel == group) continue; //don't process explicit memberships for the org group
                fetcher.execute(new ElementsFetch.GroupMembershipConfig(group.getGroupInfo().getItemId().getId()),new GroupMembershipStore(group));
            }
            finaliseUserMembership(systemUsers);
        }
    }

    /**
     * method to populate the GroupHierarchyWrappers in the collection with user membership information.
     * It does this by populating them based on the cache of group-user membership information represented by the
     * groupUserMap parameter.
     * It also accepts a set or ElementsItemId representing all the users in the Elements system.
     * Any new users that are not part of the existing user-membership cache will be represented as members of the top
     * level organisation group
     * @param groupUserMap an memory map of raw Elements group-user membership information
     * @param systemUsers  A set of ElementsItemId listing all Elements users - used to establish who is an explicit member
     *                    of the organisation group (all users with no other explicit group memberships
     *                    This mimics Elements primary group behaviour.
     */
    public synchronized void populateUserMembership(Map<ElementsItemId, Set<ElementsItemId>> groupUserMap, Set<ElementsItemId> systemUsers) {
        if(topLevel == null) throw new IllegalStateException("must construct group hierarchy before populating membership");
        if(!membershipPopulated) {
            //fetch memberships from the API
            for (ElementsGroupInfo.GroupHierarchyWrapper group : this.values()) {
                //take care with == here - this really is the same reference...
                if (topLevel == group) continue; //don't process explicit memberships for the org group
                ElementsItemId gid = group.getGroupInfo().getItemId();
                if(groupUserMap.containsKey(gid)){
                    Set<ElementsItemId> users = groupUserMap.get(gid);
                    if(users != null){
                        for(ElementsItemId uid : users){
                            if(uid.getItemType() == ElementsItemType.OBJECT && uid.getItemSubType() == ElementsObjectCategory.USER) {
                                group.addExplicitUser((ElementsItemId.ObjectId) uid);
                                getUsersGroups((ElementsItemId.ObjectId) uid).add((ElementsItemId.GroupId) group.getGroupInfo().getItemId());
                            }
                        }
                    }
                }
            }
            finaliseUserMembership(systemUsers);
        }
    }

    public synchronized void createCanonicalNames(Set<ElementsItemId> groupIdsToPrioritise){
        Map<String, Integer> counts = new HashMap<String, Integer>();

        Set<ElementsItemId> groupIdsToProcess = new HashSet<ElementsItemId>();
        groupIdsToProcess.addAll(this.keySet());

        //do the priority ones
        for (ElementsItemId groupId : groupIdsToPrioritise){
            ElementsGroupInfo.GroupHierarchyWrapper groupWrapper = this.get(groupId);
            if(groupWrapper.getUniqueName() == null) {
                String groupName = groupWrapper.getGroupInfo().getName();
                Integer boxedCount = counts.get(groupName);
                int count = boxedCount == null ? 0 : boxedCount;
                groupWrapper.setUniqueName(count < 1 ? groupName : groupName + " (" + Integer.toString(count) + ")");
                counts.put(groupName, ++count);
            }
            groupIdsToProcess.remove(groupId);
        }

        //now do all of them
        for (ElementsItemId groupId : groupIdsToProcess){
            ElementsGroupInfo.GroupHierarchyWrapper groupWrapper = this.get(groupId);
            //skip any we have seen previously..
            if(groupWrapper.getUniqueName() == null) {
                String groupName = groupWrapper.getGroupInfo().getName();
                Integer boxedCount = counts.get(groupName.toLowerCase());
                int count = boxedCount == null ? 0 : boxedCount;
                groupWrapper.setUniqueName(count < 1 ? groupName : groupName + " (" + Integer.toString(count) + ")");
                counts.put(groupName.toLowerCase(), ++count);
            }
            //System.out.println(Integer.toString(groupWrapper.getGroupInfo().getItemId().getId()) + ": "  + groupWrapper.getUniqueName());
        }
    }

    /**
     * Internal helper method to calculate the "explicit" members of the top level organisation group
     * by removing anyone who is an explicit member of any other group from the set of all users.
     * @param systemUsers A set of ElementsItemId listing all Elements users - used to establish who is an explicit member
     *                    of the organisation group (all users with no other explicit group memberships
     *                    This mimics Elements primary group behaviour.
     */
    private synchronized void finaliseUserMembership(Set<ElementsItemId> systemUsers){
        //calculate organisation membership from passed in user cache information
        Set<ElementsItemId> usersInNonOrgGroups = topLevel.getImplicitUsers();
        for (ElementsItemId userID : systemUsers) {
            if(userID instanceof ElementsItemId.ObjectId) {
                ElementsItemId.ObjectId userObjID = (ElementsItemId.ObjectId) userID;
                if (!usersInNonOrgGroups.contains(userObjID)) {
                    topLevel.addExplicitUser(userObjID);
                    getUsersGroups(userObjID).add((ElementsItemId.GroupId) topLevel.getGroupInfo().getItemId());
                }
            }
        }
        membershipPopulated = true;
    }

    public synchronized Set<ElementsItemId.GroupId> getUsersGroups(ElementsItemId.ObjectId userID){
        if(userID == null)throw new NullArgumentException("userID");
        if(userID.getItemSubType() != ElementsObjectCategory.USER) throw new IllegalArgumentException("userID must refer to a user object");

        Set<ElementsItemId.GroupId> usersGroups = userGroupMap.get(userID);
        if(usersGroups == null){
            usersGroups = new HashSet<ElementsItemId.GroupId>();
            userGroupMap.put(userID, usersGroups);
        }
        //re-fetch it to be sure..
        return userGroupMap.get(userID);
    }


    /**
     * Overridden Method from ElementsItemKeyedCollection to make it possible for the GroupCollection
     * to be used as an ElementsItemStore when retrieving information about Elements Groups from the Elements API.
     * @param itemInfo The generic item that has been extracted and needs to be converted to a GroupHierarchyWrapper
     * @param resourceType the resource type being stored
     * @param data the raw data being stored
     * @return The GroupHierarchyWrapper representing the group that should be stored in this ElementsGroupCollection
     */
    @Override
    protected ElementsGroupInfo.GroupHierarchyWrapper getItemToStore(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) {
        if(!itemInfo.isGroupInfo()) throw new IllegalStateException("ElementsGroupCollection can only store Group items");
        ElementsGroupInfo groupInfo = itemInfo.asGroupInfo();
        return new ElementsGroupInfo.GroupHierarchyWrapper(groupInfo);
    }


    /**
     * Internal class representing the ability to target an individual GroupHierarchyWrapper object as an ElementsItemStore for
     * group membership information. storeItem will be called with a resourceType or rawObject - but all we want or need
     * is the parsed ObjectID which will represent a user.
     */
    @SuppressWarnings("WeakerAccess")
    private class GroupMembershipStore implements ElementsItemStore {
        private final  ElementsGroupInfo.GroupHierarchyWrapper group;

        public GroupMembershipStore(ElementsGroupInfo.GroupHierarchyWrapper group){
            if(group == null) throw new NullArgumentException("group");
            this.group = group;
        }

        @Override
        public ElementsStoredItemInfo storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException {
            if(!itemInfo.isObjectInfo()) throw new IllegalStateException("GroupUserMembershipStore can only store Object items");
            ElementsObjectInfo objectInfo = itemInfo.asObjectInfo();
            if(!(objectInfo instanceof ElementsUserInfo)) throw new IllegalStateException("GroupUserMembershipStore can only store User items");
            group.addExplicitUser(objectInfo.getObjectId());
            getUsersGroups(objectInfo.getObjectId()).add((ElementsItemId.GroupId) group.getGroupInfo().getItemId());
            return null;
        }
    }
}
