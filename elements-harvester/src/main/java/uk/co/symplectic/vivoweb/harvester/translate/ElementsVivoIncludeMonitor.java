/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.ElementsGroupInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId.ObjectId;
import uk.co.symplectic.vivoweb.harvester.fetch.ElementsObjectCollection;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsRelationshipInfo;
import uk.co.symplectic.vivoweb.harvester.store.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ElementsVivoIncludeMonitor extends IElementsStoredItemObserver.ElementsStoredResourceObserverAdapter {

    final private Set<ElementsItemId.ObjectId> includedUsers;
    final private boolean visibleLinksOnly;

    final private ElementsObjectCollection includedObjects = new ElementsObjectCollection();
    final private List<ElementsRelationshipInfo> includedRelationships = new ArrayList<ElementsRelationshipInfo>();

    public ElementsVivoIncludeMonitor(Set<ElementsItemId.ObjectId> includedUsers, boolean visibleLinksOnly) {
        super(StorableResourceType.RAW_RELATIONSHIP);
        if(includedUsers == null) throw new NullArgumentException("includedUsers");
        this.includedUsers = Collections.unmodifiableSet(includedUsers);
        this.visibleLinksOnly = visibleLinksOnly;
    }

    public ElementsObjectCollection getIncludedObjects(){ return includedObjects; }
    public List<ElementsRelationshipInfo> getIncludedRelationships(){ return includedRelationships; }

    @Override
    public void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItem item) {
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
            includedRelationships.add(info);
            for (ElementsItemId.ObjectId id : info.getObjectIds()) {
                includedObjects.add(id);
            }
        }

        //add all included users
        includedObjects.addAll(includedUsers);
    }
}
