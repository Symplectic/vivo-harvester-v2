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
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.*;

public enum ElementsItemType {
    OBJECT("object"),
    RELATIONSHIP("relationship"),
    GROUP("group");

    private final String name;
    public String getName() {
        return name;
    }

    ElementsItemType(String name) {
        if (StringUtils.trimToNull(name) == null) throw new NullArgumentException("name");
        this.name = name;
    }

    private static final Map<ElementsItemType, Map<String, SubType>> singularMap = new HashMap<ElementsItemType, Map<String, SubType>>();
    private static final Map<ElementsItemType, Map<String, SubType>> pluralMap = new HashMap<ElementsItemType, Map<String, SubType>>();

    private static void addSubType(SubType subType){
        ElementsItemType mainType = subType.getMainType();
        if(!singularMap.containsKey(mainType)) singularMap.put(mainType, new HashMap<String, SubType>());
        if(!pluralMap.containsKey(mainType)) pluralMap.put(mainType, new HashMap<String, SubType>());

        if (singularMap.get(mainType).put(subType.getSingular(), subType) != null || pluralMap.get(mainType).put(subType.getPlural(), subType) != null) {
            throw new IllegalStateException(MessageFormat.format("Duplicate value given for singular / plural in ElementsItemType {0}'s SubTypes", mainType.getName()));
        }
    }

    public static SubType getSubType(ElementsItemType type, String value) {
        if (singularMap.containsKey(type) && singularMap.get(type).containsKey(value)) {
            return singularMap.get(type).get(value);
        }
        if(pluralMap.containsKey(type) && pluralMap.get(type).containsKey(value)) {
            return pluralMap.get(type).get(value);
        }
        throw new IndexOutOfBoundsException(MessageFormat.format("{0} is not a known subtype of {2}", value, type.getName()));
    }

    public static Collection<SubType> knownSubTypes (ElementsItemType type) {
        if(singularMap.containsKey(type)) {
            return Collections.unmodifiableCollection(singularMap.get(type).values());
        }
        return Collections.unmodifiableCollection(new HashSet<SubType>());
    }

    //static block to instantiate the generic subtypes encompasing all groups and all relationships
    public static SubType AllGroups = new DummySubType(ElementsItemType.GROUP);
    public static SubType AllRelationships = new DummySubType(ElementsItemType.RELATIONSHIP);

    public static class SubType{
        private final ElementsItemType mainType;
        private final String singular;
        private final String plural;

        public ElementsItemType getMainType() { return mainType; }
        public String getSingular() { return singular; }
        public String getPlural() {
            return plural;
        }

        protected SubType(ElementsItemType mainType, String singular, String plural){
            if (StringUtils.trimToNull(singular) == null) throw new NullArgumentException("singular");
            if (StringUtils.trimToNull(plural) == null) throw new NullArgumentException("plural");
            this.mainType = mainType;
            this.singular = singular;
            this.plural = plural;
            //add to parent dictionaries
            addSubType(this);
        }
    }

    private static class DummySubType extends SubType{
        DummySubType(ElementsItemType mainType){
            super(mainType, mainType.getName(), mainType.getName() + 's');
        }
    }

}