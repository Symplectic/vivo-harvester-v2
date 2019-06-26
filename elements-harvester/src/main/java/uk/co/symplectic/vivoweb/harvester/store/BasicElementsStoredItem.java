/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;

/**
 * Simple class to represent data about an Elements item held in a store.
 * Provides information about the itemId, the resourceType and access to the underlying data as a StoredData object
 *
 * This class is used when you don't already have the rich ElementsItemInfo to hand and you want to avoid extracting the
 * ElementsItemInfo from the file for the time being.
 */

@SuppressWarnings("WeakerAccess")
public class BasicElementsStoredItem {
    private ElementsItemId itemId;
    private final StorableResourceType resourceType;
    private final StoredData data;
    public BasicElementsStoredItem(ElementsItemId itemId, StorableResourceType resourceType, StoredData data) {
        if (itemId == null) throw new NullArgumentException("itemId");
        if (resourceType == null) throw new NullArgumentException("resourceType");
        if (data == null) throw new NullArgumentException("data");

        if (!resourceType.isAppropriateForItem(itemId))
            throw new IllegalArgumentException("item type does not support resourceType");

        this.itemId = itemId;
        this.resourceType = resourceType;
        this.data = data;
    }

    public ElementsItemId getItemId() { return itemId; }

    public StorableResourceType getResourceType() { return resourceType; }

    public StoredData getStoredData() { return data; }

}