/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.model;

import org.apache.commons.lang.NullArgumentException;

/**
 * Created by ajpc2_000 on 10/08/2016.
 */
public enum ElementsItemType {
    OBJECT("object"),
    RELATIONSHIP("relationship"),
    GROUP("group");

    private final String name;

    ElementsItemType(String name) {
        if (name == null) throw new NullArgumentException("name");
        this.name = name;
    }

    public String getName() {
        return name;
    }
}