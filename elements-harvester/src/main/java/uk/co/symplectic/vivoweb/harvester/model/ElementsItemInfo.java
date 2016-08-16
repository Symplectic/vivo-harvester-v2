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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ajpc2_000 on 10/08/2016.
 */
public abstract class ElementsItemInfo {

    //Static portion to deal with construction of Concrete ItemInfos
    public static ElementsObjectInfo createObjectItem(ElementsObjectCategory category, String id) {
        if (ElementsObjectCategory.ACTIVITY == category) {
            return new ElementsUnknownObjectInfo(category, id);
        } else if (ElementsObjectCategory.EQUIPMENT == category) {
            return new ElementsUnknownObjectInfo(category, id);
        } else if (ElementsObjectCategory.GRANT == category) {
            return new ElementsUnknownObjectInfo(category, id);
        } else if (ElementsObjectCategory.ORG_STRUCTURE == category) {
            return new ElementsUnknownObjectInfo(category, id);
        } else if (ElementsObjectCategory.TEACHING_ACTIVITY == category) {
            return new ElementsUnknownObjectInfo(category, id);
        } else if (ElementsObjectCategory.PROJECT == category) {
            return new ElementsUnknownObjectInfo(category, id);
        } else if (ElementsObjectCategory.PUBLICATION == category) {
            return new ElementsUnknownObjectInfo(category, id);
        } else if (ElementsObjectCategory.IMPACT == category) {
            return new ElementsUnknownObjectInfo(category, id);
        } else if (ElementsObjectCategory.USER == category) {
            return new ElementsUserInfo(id);
        }

        return new ElementsUnknownObjectInfo(category, id);
    }

    public static ElementsRelationshipInfo createRelationshipItem(String id) {
        return new ElementsRelationshipInfo(id);
    }

    public static Collection<String> validItemDescriptorsForType(ElementsItemType itemType){
        Set<String> validDescriptors = new HashSet<String>();
        switch(itemType){
            case OBJECT:
                for(ElementsObjectCategory category : ElementsObjectCategory.knownCategories()){
                    validDescriptors.add(category.getSingular());
                }
                break;
            default:
                validDescriptors.add(itemType.getName());
        }
        return validDescriptors;
    }

    //Main class definition
    private final ElementsItemType itemType;
    public ElementsItemType getItemType() { return itemType; }

    protected ElementsItemInfo(ElementsItemType itemType) {
        if (itemType == null) throw new NullArgumentException("itemType");
        this.itemType = itemType;
    }

    //methods that may need overriding in concrete subclasses
    public abstract String getId();

    public final String getItemDescriptor(){
        if(isObjectInfo()){
            return this.asObjectInfo().getCategory().getSingular();
        }
        return itemType.getName();
    }

    //paired is/as methods to simplify access to concrete subtypes in code.
    public boolean isObjectInfo(){
        return this instanceof ElementsObjectInfo;
    }
    public ElementsObjectInfo asObjectInfo(){
        if(isObjectInfo()) {
            return (ElementsObjectInfo) this;
        }
        return null;
    }

    public boolean isRelationshipInfo(){
        return this instanceof ElementsRelationshipInfo;
    }
    public ElementsRelationshipInfo asRelationshipInfo(){
        if(isRelationshipInfo()) {
            return (ElementsRelationshipInfo) this;
        }
        return null;
    }
}