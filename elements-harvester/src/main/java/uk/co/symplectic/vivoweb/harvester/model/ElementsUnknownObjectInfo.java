/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.model;

public class ElementsUnknownObjectInfo extends ElementsObjectInfo {
    //package private as should only ever be created by calls to create on ItemInfo superclass
    ElementsUnknownObjectInfo(ElementsObjectCategory category, String id) {
        super(category, id);
    }
}
