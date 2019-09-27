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
package uk.co.symplectic.vivoweb.harvester.model;

/**
 * Subclass of ElementsObjectInfo to represent an Elements Object of a type you have no special need to store extra data
 * about. Exposes nothing more than the base abstract class. Used for everything except users as things stand.
 *
 * package private as should only ever be created by calls to create on ItemInfo superclass and nothing ever needs to
 * refer to it as anything except the superclass.
 */

//TODO: is this obsolete now..

class ElementsGenericObjectInfo extends ElementsObjectInfo {
    ElementsGenericObjectInfo(ElementsObjectCategory category, int id) {
        super(category, id);
    }
}
