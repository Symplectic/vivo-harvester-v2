/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.model;

public class ElementsObjectCategory extends ElementsItemType.SubType {

    public static final ElementsObjectCategory ACTIVITY          = new ElementsObjectCategory("activity", "activities");
    public static final ElementsObjectCategory EQUIPMENT         = new ElementsObjectCategory("equipment", "equipment");
    public static final ElementsObjectCategory GRANT             = new ElementsObjectCategory("grant", "grants");
    public static final ElementsObjectCategory ORG_STRUCTURE     = new ElementsObjectCategory("org-structure", "org-structures");
    public static final ElementsObjectCategory PROJECT           = new ElementsObjectCategory("project", "projects");
    public static final ElementsObjectCategory PUBLICATION       = new ElementsObjectCategory("publication", "publications");
    public static final ElementsObjectCategory USER              = new ElementsObjectCategory("user", "users");
    public static final ElementsObjectCategory TEACHING_ACTIVITY = new ElementsObjectCategory("teaching-activity", "teaching-activities");
    public static final ElementsObjectCategory IMPACT = new ElementsObjectCategory("impact", "impact-records");

    public static ElementsObjectCategory valueOf(String value) {
        return (ElementsObjectCategory) ElementsItemType.getSubType(ElementsItemType.OBJECT, value);
    }

    private ElementsObjectCategory(final String singular, final String plural) {
        super(ElementsItemType.OBJECT, singular, plural);
    }

}