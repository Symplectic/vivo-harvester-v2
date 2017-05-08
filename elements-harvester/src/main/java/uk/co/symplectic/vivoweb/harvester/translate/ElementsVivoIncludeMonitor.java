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
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.vivoweb.harvester.model.ElementsRelationshipInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.IElementsStoredItemObserver;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ElementsVivoIncludeMonitor extends IElementsStoredItemObserver.ElementsStoredResourceObserverAdapter {

    final private List<ElementsObjectCategory> includedCategories;
    final private boolean includeInvisibleLinks;
    final private Set<ElementsItemId> includedUsers;
    final private ElementsItemCollection includedItems = new ElementsItemCollection();


    public ElementsVivoIncludeMonitor(Set<ElementsItemId> includedUsers, Set<ElementsItemId> includedGroups, List<ElementsObjectCategory> includedCategories, boolean visibleLinksOnly) {
        super(StorableResourceType.RAW_RELATIONSHIP);
        if(includedUsers == null) throw new NullArgumentException("includedUsers");
        if(includedCategories == null) throw new NullArgumentException("includedCategories");

        this.includedUsers = Collections.unmodifiableSet(includedUsers);
        //make unmodifiable
        this.includedCategories = Collections.unmodifiableList(includedCategories);

        //note not!
        this.includeInvisibleLinks = !visibleLinksOnly;

        //add the included users to the included items set.
        includedItems.addAll(includedUsers);

        if(includedGroups != null){
            includedItems.addAll(includedGroups);
        }
    }

    public ElementsItemCollection getIncludedItems(){ return includedItems; }

    @Override
    public void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItem item) {
        //if invisible relationships should be included or if this relationship is visible we consider including this relationship for now.
        boolean includeRelationship = includeInvisibleLinks || info.getIsVisible();

        //if the relationship is not complete then we are not interested..
        includeRelationship = includeRelationship && info.getIsComplete();

        //optimise for early jump out wherever possible.
        if(includeRelationship) {
            //ensure we only ever make includeRelationship false once
            List<ElementsItemId.ObjectId> userIds = info.getUserIds();
            if (!userIds.isEmpty()) {
                for (ElementsItemId.ObjectId userId : userIds) {
                    includeRelationship = includeRelationship && includedUsers.contains(userId);
                }
            }
            //todo: do we want to account for only including things that were successfully translated? dodgy for the ones where the translation IS in the relationship?

            if(includeRelationship) {
                List<ElementsItemId.ObjectId> nonUserIds = info.getNonUserIds();
                if (!nonUserIds.isEmpty()) {
                    for (ElementsItemId.ObjectId nonUserId : nonUserIds) {
                        includeRelationship = includeRelationship && includedCategories.contains(nonUserId.getItemSubType());
                    }
                }

                //if we still want to include the relationship then add the objects within it into the includedItems set.
                if (includeRelationship) {
                    includedItems.add(info.getItemId());
                    for (ElementsItemId.ObjectId id : info.getObjectIds()) {
                        includedItems.add(id);
                    }
                }
            }
        }
    }
}
