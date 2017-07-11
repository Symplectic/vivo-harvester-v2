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
//import java.util.*;

public class StorableResourceType {

    final public static StorableResourceType RAW_OBJECT = new StorableResourceType(ElementsItemType.AllObjects, "raw", "xml", true);
	final public static StorableResourceType RAW_USER_PHOTO = new StorableResourceType(ElementsObjectCategory.USER, "photo", null, false);
    final public static StorableResourceType RAW_RELATIONSHIP = new StorableResourceType(ElementsItemType.AllRelationships, "raw", "xml", true);
    final public static StorableResourceType RAW_RELATIONSHIP_TYPES = new StorableResourceType(ElementsItemType.AllRelationshipTypes, "raw", "xml", true);
    final public static StorableResourceType RAW_GROUP = new StorableResourceType(ElementsItemType.AllGroups, "raw", "xml", true);

    final public static StorableResourceType TRANSLATED_OBJECT = new StorableResourceType(ElementsItemType.AllObjects, "translated", "rdf", true);
    final public static StorableResourceType TRANSLATED_USER_PHOTO_DESCRIPTION = new StorableResourceType(ElementsObjectCategory.USER, "photo", "rdf", true);
    final public static StorableResourceType TRANSLATED_RELATIONSHIP = new StorableResourceType(ElementsItemType.AllRelationships, "translated", "rdf", true);
    final public static StorableResourceType TRANSLATED_GROUP = new StorableResourceType(ElementsItemType.AllGroups, "translated", "rdf", true);

    //Item part of class to define structure
    private final ElementsItemType.SubType keySubItemType;
    private final String name;
    private final String fileExtension;
    private final boolean shouldZip;

    public ElementsItemType getKeyItemType() { return keySubItemType.getMainType(); }
    public String getName() {
        return name;
    }

    //TODO: file extensions are complicated by the fact that raw photo data may be a variety of mime types - so this is not implemented at the moment.
    public String getFileExtension() {
        return fileExtension;
    }

    @Override
    public String toString(){
        return MessageFormat.format("{0}-{1}", getName(), keySubItemType.getSingular());
    }

    private StorableResourceType(ElementsItemType.SubType keySubItemType, String name, String fileExtension, boolean shouldZip) {
        if (keySubItemType == null) throw new NullArgumentException("keySubItemType");
        if (StringUtils.isEmpty(name) || StringUtils.isWhitespace(name))
            throw new IllegalArgumentException("argument name must not be null or empty");
        this.keySubItemType = keySubItemType;
        this.name = name;
        this.fileExtension = StringUtils.trimToNull(fileExtension);
        this.shouldZip = shouldZip;
    }

    public boolean isAppropriateForItem(ElementsItemId id){ return keySubItemType.matches(id.getItemSubType()); }

    public boolean shouldZip() { return shouldZip; }

}







