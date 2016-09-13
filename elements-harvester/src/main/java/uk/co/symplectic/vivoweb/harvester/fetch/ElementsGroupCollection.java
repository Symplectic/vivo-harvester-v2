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
import sun.plugin.dom.exception.InvalidStateException;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.ElementsObjectStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by ajpc2_000 on 18/08/2016.
 */
public class ElementsGroupCollection implements ElementsObjectStore {
    private ElementsGroupInfo.GroupHierarchyWrapper topLevel = null;
    private boolean membershipPopulated = false;
    private Map<Integer, ElementsGroupInfo.GroupHierarchyWrapper> groups = new HashMap<Integer, ElementsGroupInfo.GroupHierarchyWrapper>();

    public ElementsGroupInfo.GroupHierarchyWrapper constructHierarchy(){
        if(topLevel == null) {
            for (ElementsGroupInfo.GroupHierarchyWrapper group : groups.values()) {
                Integer parentId = group.getGroupInfo().getParentId();
                if (parentId != null) {
                    ElementsGroupInfo.GroupHierarchyWrapper parentGroup = groups.get(parentId.intValue());
                    group.setParent(parentGroup);
                }
            }

            //find top level group in hierarchy, error out if there is more than one
            for (ElementsGroupInfo.GroupHierarchyWrapper group : groups.values()) {
                if (group.getParent() == null) {
                    if (topLevel == null) {
                        topLevel = group;
                    } else {
                        topLevel = null;
                        throw new InvalidStateException("Invalid Group Hierarchy detected");
                    }
                }
            }
            if (topLevel == null)
                throw new InvalidStateException("Invalid Group Hierarchy detected");

            if (topLevel.getGroupInfo().getId() != 1)
                throw new InvalidStateException("Invalid Group Hierarchy detected - organisation group not topLevel");
        }
        return topLevel;
    }

    public void populateUserMembership(ElementsFetch fetcher, Set<ElementsItemId.ObjectId> systemUsers) throws IOException {
        if(topLevel == null) throw new IllegalStateException("must construct group hierarcy before populating membership");
        if(!membershipPopulated) {
            //fetch memberships from the API
            for (ElementsGroupInfo.GroupHierarchyWrapper group : groups.values()) {
                if (topLevel == group) continue; //don't process explicit memberships for the org group
                fetcher.execute(new ElementsFetch.GroupMembershipConfig(group.getGroupInfo().getItemId().getId()),new GroupMembershipStore(group));
            }

           //TODO: calculate organisation membership from user cache - requires cache to be present first, which will make other things easier too.
            Set<ElementsItemId.ObjectId> usersInNonOrgGroups = topLevel.getImplicitUsers();
            for (ElementsItemId.ObjectId userID : systemUsers) {
                if (!usersInNonOrgGroups.contains(userID))
                    topLevel.addExplicitUser(userID);
            }
            membershipPopulated = true;
        }
    }

    public ElementsGroupInfo.GroupHierarchyWrapper getGroup(int groupId){
        return groups.get(groupId);
    }

    @Override
    public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, String data) throws IOException {
        return storeItem(itemInfo, resourceType, (byte[]) null);
    }

    @Override
    public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException {
        if(!itemInfo.isGroupInfo()) throw new IllegalStateException("ElementsGroupCollection can only store Group items");
        ElementsGroupInfo groupInfo = itemInfo.asGroupInfo();
        groups.put(groupInfo.getId(), new ElementsGroupInfo.GroupHierarchyWrapper(groupInfo));
        return null;
    }

    private class GroupMembershipStore implements ElementsObjectStore{
        private final  ElementsGroupInfo.GroupHierarchyWrapper group;

        public GroupMembershipStore(ElementsGroupInfo.GroupHierarchyWrapper group){
            if(group == null) throw new NullArgumentException("group");
            this.group = group;
        }

        @Override
        public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, String data) throws IOException {
            return storeItem(itemInfo, resourceType, (byte[]) null);
        }

        @Override
        public ElementsStoredItem storeItem(ElementsItemInfo itemInfo, StorableResourceType resourceType, byte[] data) throws IOException {
            if(!itemInfo.isObjectInfo()) throw new IllegalStateException("GroupUserMembershipStore can only store Object items");
            ElementsObjectInfo objectInfo = itemInfo.asObjectInfo();
            if(!(objectInfo instanceof ElementsUserInfo)) throw new IllegalStateException("GroupUserMembershipStore can only store User items");
            group.addExplicitUser(objectInfo.getObjectId());
            return null;
        }
    }
}
