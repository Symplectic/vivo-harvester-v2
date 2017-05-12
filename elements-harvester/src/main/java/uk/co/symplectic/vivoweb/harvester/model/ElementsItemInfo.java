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
import uk.co.symplectic.utils.xml.XMLEventProcessor;

public abstract class ElementsItemInfo {

    public enum ExtractionSource{
        FEED,
        DELETED_FEED,
        FILE
    }

    //Static portion to deal with construction of Concrete ItemInfos
    public static ElementsObjectInfo createObjectItem(ElementsObjectCategory category, int id) {
        if (ElementsObjectCategory.USER == category) {
            return new ElementsUserInfo(id);
        }
        return new ElementsGenericObjectInfo(category, id);
    }

    public static ElementsRelationshipInfo createRelationshipItem(int id) {
        return new ElementsRelationshipInfo(id);
    }

    public static ElementsRelationshipTypeInfo createRelationshipTypeItem(int id) { return new ElementsRelationshipTypeInfo(id); }

    public static ElementsGroupInfo createGroupItem(int id) {
        return new ElementsGroupInfo(id);
    }

    public static XMLEventProcessor.ItemExtractingFilter<ElementsItemInfo> getExtractor(ElementsItemType type, ExtractionSource source, int maximumExpected) {
        switch(type){
            case OBJECT : return ElementsObjectInfo.Extractor.getExtractor(source, maximumExpected);
            case RELATIONSHIP: return ElementsRelationshipInfo.Extractor.getExtractor(source, maximumExpected);
            case GROUP : return ElementsGroupInfo.Extractor.getExtractor(source, maximumExpected);
            case RELATIONSHIP_TYPE : return ElementsRelationshipTypeInfo.Extractor.getExtractor(source, maximumExpected);
            default : throw new IllegalStateException("invalid extractor object type requested");
        }
    }

    //Main class definition
    private final ElementsItemId itemId;

    protected ElementsItemInfo(ElementsItemId itemId) {
        if (itemId == null) throw new NullArgumentException("itemId");
        this.itemId = itemId;
    }

    //methods that may need overriding in concrete subclasses
    public ElementsItemId getItemId() { return itemId; }

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

    public boolean isRelationshipTypeInfo(){
        return this instanceof ElementsRelationshipTypeInfo;
    }
    public ElementsRelationshipTypeInfo asRelationshipTypeInfo(){
        if(isRelationshipTypeInfo()) {
            return (ElementsRelationshipTypeInfo) this;
        }
        return null;
    }

    public boolean isGroupInfo(){
        return this instanceof ElementsGroupInfo;
    }
    public ElementsGroupInfo asGroupInfo(){
        if(isGroupInfo()) {
            return (ElementsGroupInfo) this;
        }
        return null;
    }

}