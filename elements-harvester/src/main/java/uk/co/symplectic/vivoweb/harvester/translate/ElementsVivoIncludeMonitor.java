/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectId;
import uk.co.symplectic.vivoweb.harvester.fetch.ElementsObjectCollection;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfoCache;
import uk.co.symplectic.vivoweb.harvester.model.ElementsRelationshipInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import uk.co.symplectic.vivoweb.harvester.store.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ElementsVivoIncludeMonitor extends IElementsStoredItemObserver.ElementsStoredRelationshipObserver {

    final private Set<ElementsObjectId> excludedUsers;
    final private boolean currentStaffOnly;
    final private boolean visibleLinksOnly;

    final private ElementsObjectCollection includedObjects = new ElementsObjectCollection();
    final private List<ElementsRelationshipInfo> includedRelationships = new ArrayList<ElementsRelationshipInfo>();

    public ElementsVivoIncludeMonitor(Set<ElementsObjectId> excludedUsers, boolean currentStaffOnly, boolean visibleLinksOnly) {
        super(StorableResourceType.RAW_RELATIONSHIP);
        if(excludedUsers == null) throw new NullArgumentException("excludedUsers");
        this.excludedUsers = Collections.unmodifiableSet(excludedUsers);
        this.currentStaffOnly = currentStaffOnly;
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

        if (currentStaffOnly && includeRelationship) {
            List<String> userIds = info.getUserIds();
            if (!userIds.isEmpty()) {
                for(String userId : userIds) {
                    ElementsUserInfo userInfo = (ElementsUserInfo) ElementsObjectInfoCache.get(ElementsObjectCategory.USER, userId);
                    if (userInfo != null) {
                        //if user is not currentStaff then set to false ensure we only ever make it false once
                        includeRelationship = includeRelationship && userInfo.getIsCurrentStaff();

                        //if user is in the excluded users then set include to false;
                        includeRelationship = includeRelationship && !excludedUsers.contains(userInfo.getObjectId());
                    } else {
                        includeRelationship = false;
                    }
                }
            }
        }

        if (includeRelationship) {
            includedRelationships.add(info);
            for (ElementsObjectId id : info.getObjectIds()) {
                includedObjects.add(id);
            }
        }
    }
}
