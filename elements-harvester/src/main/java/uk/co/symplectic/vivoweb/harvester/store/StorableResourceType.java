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
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
//import java.util.*;

/**
 * Class (and static instances) to represent all the different types of "resource" that might be placed in a store.
 * Resources are keyed to an ElementsItemType or to a specific ElementsItemType.SubType.
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public class StorableResourceType {

    final public static StorableResourceType RAW_OBJECT = new StorableResourceType(ElementsItemType.OBJECT, "raw", "xml", true);
    final public static StorableResourceType RAW_USER_PHOTO = new StorableResourceType(ElementsObjectCategory.USER, "photo", null, false);
    final public static StorableResourceType RAW_RELATIONSHIP = new StorableResourceType(ElementsItemType.RELATIONSHIP, "raw", "xml", true);
    final public static StorableResourceType RAW_RELATIONSHIP_TYPES = new StorableResourceType(ElementsItemType.RELATIONSHIP_TYPE, "raw", "xml", true);
    final public static StorableResourceType RAW_GROUP = new StorableResourceType(ElementsItemType.GROUP, "raw", "xml", true);

    final public static StorableResourceType TRANSLATED_OBJECT = new StorableResourceType(ElementsItemType.OBJECT, "translated", "rdf", true);
    final public static StorableResourceType TRANSLATED_USER_PHOTO_DESCRIPTION = new StorableResourceType(ElementsObjectCategory.USER, "photo", "rdf", true);
    final public static StorableResourceType TRANSLATED_RELATIONSHIP = new StorableResourceType(ElementsItemType.RELATIONSHIP, "translated", "rdf", true);
    final public static StorableResourceType TRANSLATED_GROUP = new StorableResourceType(ElementsItemType.GROUP, "translated", "rdf", true);
    final public static StorableResourceType TRANSLATED_USER_GROUP_MEMBERSHIP = new StorableResourceType(ElementsObjectCategory.USER, "group-membership", "rdf", true);

    private final ResourceKey keyItem;

    //Item part of class to define structure
    private final String name;
    private final String fileExtension;
    private final boolean shouldZip;

    public ElementsItemType getKeyItemType() {
        return keyItem.getItemType();
    }
    public String getName() {
        return name;
    }
    //TODO: file extensions are complicated by the fact that raw photo data may be a variety of mime types - so this is not used at the moment.
    public String getFileExtension() {
        return fileExtension;
    }
    public boolean shouldZip() {
        return shouldZip;
    }

    public Collection<ElementsItemType.SubType> getSupportedSubTypes() {
        return keyItem.getSupportedSubTypes();
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}-{1}", getName(), keyItem.getDescriptor());
    }

    public StorableResourceType(ElementsItemType itemType, String name, String fileExtension, boolean shouldZip){
        this(new ItemTypeResourceKey(itemType), name, fileExtension, shouldZip);
    }

    public StorableResourceType(ElementsItemType.SubType subType, String name, String fileExtension, boolean shouldZip){
        this(new SubTypeResourceKey(subType), name, fileExtension, shouldZip);
    }

    private StorableResourceType(ResourceKey keyItem, String name, String fileExtension, boolean shouldZip) {
        if (keyItem == null) throw new NullArgumentException("keyItem");
        if (StringUtils.isEmpty(name) || StringUtils.isWhitespace(name))
            throw new IllegalArgumentException("argument name must not be null or empty");

        this.keyItem = keyItem;
        this.name = name;
        this.fileExtension = StringUtils.trimToNull(fileExtension);
        this.shouldZip = shouldZip;
    }

    public boolean isAppropriateForItem(ElementsItemId id) {
        return id != null && getSupportedSubTypes().contains(id.getItemSubType());
    }



    static abstract class ResourceKey{
        abstract String getDescriptor();
        abstract ElementsItemType getItemType();
        abstract Collection<ElementsItemType.SubType> getSupportedSubTypes();
    }

    static class ItemTypeResourceKey extends ResourceKey {
        private final ElementsItemType itemType;

        ItemTypeResourceKey(ElementsItemType itemType){
            if (itemType == null) throw new NullArgumentException("itemType");
            this.itemType = itemType;
        }
        @Override
        String getDescriptor() { return itemType.getName(); }
        @Override
        ElementsItemType getItemType() { return itemType; }
        @Override
        Collection<ElementsItemType.SubType> getSupportedSubTypes(){ return itemType.getSubTypes();}
    }

    static class SubTypeResourceKey extends ResourceKey {
        private final ElementsItemType.SubType subType;

        SubTypeResourceKey(ElementsItemType.SubType subType) {
            if (subType == null) throw new NullArgumentException("subType");
            this.subType = subType;
        }

        @Override
        public String getDescriptor() { return subType.getSingular(); }
        @Override
        public ElementsItemType getItemType() { return subType.getMainType(); }
        @Override
        public Collection<ElementsItemType.SubType> getSupportedSubTypes(){
            return Collections.singleton(subType);
        }
    }

}







