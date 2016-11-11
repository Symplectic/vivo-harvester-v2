/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.fetch.ElementsItemCollection;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsRelationshipInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.IElementsStoredItemObserver;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ElementsVivoIncludeMonitor extends IElementsStoredItemObserver.ElementsStoredResourceObserverAdapter {

    final private boolean visibleLinksOnly;
    final private Set<ElementsItemId> includedUsers;
    final private ElementsItemCollection includedItems = new ElementsItemCollection();

    public ElementsVivoIncludeMonitor(Set<ElementsItemId> includedUsers, Set<ElementsItemId> includedGroups, boolean visibleLinksOnly) {
        super(StorableResourceType.RAW_RELATIONSHIP);
        if(includedUsers == null) throw new NullArgumentException("includedUsers");
        this.includedUsers = Collections.unmodifiableSet(includedUsers);
        this.visibleLinksOnly = visibleLinksOnly;

        //add the included users to the included items set.
        includedItems.addAll(includedUsers);

        if(includedGroups != null){
            includedItems.addAll(includedGroups);
        }
    }

    public ElementsItemCollection getIncludedItems(){ return includedItems; }

    @Override
    public void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItem item) {
        //TODO: make this look at whether translations of the relevant relationships and items exist
        boolean includeRelationship = true;

        if (visibleLinksOnly && includeRelationship) {
            //ensure we only ever make it false once
            includeRelationship = includeRelationship && info.getIsVisible();
        }

        List<ElementsItemId.ObjectId> userIds = info.getUserIds();
        if (!userIds.isEmpty()) {
            for (ElementsItemId.ObjectId userId : userIds) {
                includeRelationship = includeRelationship && includedUsers.contains(userId);
            }
        }

        if (includeRelationship) {
            includedItems.add(info.getItemId());
            for (ElementsItemId.ObjectId id : info.getObjectIds()) {
                includedItems.add(id);
            }
        }
    }
}
