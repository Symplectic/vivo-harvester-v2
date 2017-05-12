package uk.co.symplectic.vivoweb.harvester.fetch;/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

import java.util.*;

public class ElementsItemCollection {
    private final Map<ElementsItemType, Set<ElementsItemId>> mData = new HashMap<ElementsItemType, Set<ElementsItemId>>();

    public synchronized void add(ElementsItemId item) {
        if (item != null) {
            Set<ElementsItemId> itemSet = getOrCreateSetForCategory(item.getItemType());
            itemSet.add(item);
        }
    }

    public synchronized void addAll(Collection<ElementsItemId> items) {
        if (items != null) {
            for (ElementsItemId item : items) {
                add(item);
            }
        }
    }

    private Set<ElementsItemId> getOrCreateSetForCategory(ElementsItemType type) {
        Set<ElementsItemId> itemSet = mData.get(type);
        if (itemSet == null) {
            itemSet = new HashSet<ElementsItemId>();
            mData.put(type, itemSet);
        }
        return itemSet;
    }

    public synchronized void remove(ElementsItemId item) {
        if (item != null) {
            Set<ElementsItemId> itemSet = mData.get(item.getItemType());
            if (itemSet != null) {
                itemSet.remove(item);
                //if set is now empty remove it
                if (itemSet.isEmpty()) {
                    mData.remove(item.getItemType());
                }
            }
        }
    }

    public synchronized void removeAll(Collection<ElementsItemId> items) {
        if (items != null) {
            for (ElementsItemId item : items) {
                remove(item);
            }
        }
    }

    public synchronized Set<ElementsItemId> get(ElementsItemType type) {
        return Collections.unmodifiableSet(getOrCreateSetForCategory(type));
    }
}

