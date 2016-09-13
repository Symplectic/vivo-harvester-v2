/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Caching of parsed object info.
 * For now, just a simple map, and only caching "user" info
 */
public class ElementsObjectInfoCache {
    private static final Map<ElementsItemId.ObjectId, ElementsUserInfo> userInfoMap = new HashMap<ElementsItemId.ObjectId, ElementsUserInfo>();

    private ElementsObjectInfoCache() {}

    public static ElementsObjectInfo get(ElementsItemId.ObjectId id) {
        if (id == null) return null;
        return userInfoMap.get(id);
    }

    public static void put(ElementsObjectInfo info) {
        if (ElementsObjectCategory.USER == info.getCategory()) {
            //only update the cache if this is the first time we have seen it - otherwise keep the original one.
            ElementsUserInfo userInfo = (ElementsUserInfo) info;
            ElementsUserInfo currentlyStoredInfo = userInfoMap.get(userInfo.getObjectId());
            if(currentlyStoredInfo == null || (!currentlyStoredInfo.isFullyPopulated() && userInfo.isFullyPopulated())) {
                userInfoMap.put(info.getObjectId(), userInfo);
            }
        }
    }
}
