/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.fetch;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.ElementsItemStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItemInfo;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class ElementsGroupCollection extends ElementsItemKeyedCollection<ElementsGroupInfo.GroupHierarchyWrapper> {
    private ElementsGroupInfo.GroupHierarchyWrapper topLevel = null;
    private boolean membershipPopulated = false;

    public ElementsGroupCollection(){super(new RestrictToType(ElementsItemType.GROUP));}

    public ElementsGroupInfo.GroupHierarchyWrapper GetTopLevel(){
        if(topLevel == null) throw new IllegalStateException("must construct group hierarcy before requesting top level");
        return topLevel;
    }

    public ElementsGroupInfo.GroupHierarchyWrapper constructHierarchy(){
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

    public void populateUserMembership(ElementsFetch fetcher, Set<ElementsItemId> systemUsers) throws IOException {
        if(topLevel == null) throw new IllegalStateException("must construct group hierarcy before populating membership");
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

    public void populateUserMembership(Map<ElementsItemId, Set<ElementsItemId>> groupUserMap, Set<ElementsItemId> systemUsers) throws IOException {
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
                            }
                        }
                    }
                }
            }
            finaliseUserMembership(systemUsers);
        }
    }

    private void finaliseUserMembership(Set<ElementsItemId> systemUsers){
        //calculate organisation membership from passed in user cache information
        Set<ElementsItemId> usersInNonOrgGroups = topLevel.getImplicitUsers();
        for (ElementsItemId userID : systemUsers) {
            if(userID instanceof ElementsItemId.ObjectId) {
                ElementsItemId.ObjectId userObjID = (ElementsItemId.ObjectId) userID;
                if (!usersInNonOrgGroups.contains(userObjID))
                    topLevel.addExplicitUser(userObjID);
            }
        }
        membershipPopulated = true;
    }


    @Override
    protected ElementsGroupInfo.GroupHierarchyWrapper getItemToStore(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) {
        if(!itemInfo.isGroupInfo()) throw new IllegalStateException("ElementsGroupCollection can only store Group items");
        ElementsGroupInfo groupInfo = itemInfo.asGroupInfo();
        return new ElementsGroupInfo.GroupHierarchyWrapper(groupInfo);
    }

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
            return null;
        }
    }
}
