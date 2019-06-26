/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.model;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.*;

/**
 * enum representing the concept of a "type" of data from Elements
 * The four main types are split into "sub-types" (see inner class below).
 * These subclasses can represent a specific sub-set of a type, or an aggregate grouping of sub-types (e.g. all-objects)
 * The SubTypes are registered in the static dictionaries here, so all extant "sub types" are always directly comparable
 * as they are the same object...
 */
public enum ElementsItemType {
    OBJECT("object"),
    RELATIONSHIP("relationship"),
    RELATIONSHIP_TYPE("relationship_type"),
    GROUP("group");

    private final String name;
    private final String pluralName;
    public String getName() { return name; }
    public String getPluralName() {
        return pluralName;
    }

    ElementsItemType(String name) { this(name, name + 's'); }

    ElementsItemType(String name, String pluralName) {
        if (StringUtils.trimToNull(name) == null) throw new NullArgumentException("name");
        if (StringUtils.trimToNull(pluralName) == null) throw new NullArgumentException("pluralName");
        this.name = name;
        this.pluralName = pluralName;
    }

    /**
     * SubType's keyed by ItemType and name (singular and plural)
     */
    private static final Map<ElementsItemType, Map<String, SubType>> singularMap = new HashMap<ElementsItemType, Map<String, SubType>>();
    private static final Map<ElementsItemType, Map<String, SubType>> pluralMap = new HashMap<ElementsItemType, Map<String, SubType>>();

    /**
     * method to register an item type in the maps above (called by constructor within SubType class)
     * @param subType the Subtype to be registered in the static type maps
     */
    private static synchronized void addSubType(SubType subType){
        ElementsItemType mainType = subType.getMainType();
        if(!singularMap.containsKey(mainType)) singularMap.put(mainType, new HashMap<String, SubType>());
        if(!pluralMap.containsKey(mainType)) pluralMap.put(mainType, new HashMap<String, SubType>());

        if (singularMap.get(mainType).put(subType.getSingular(), subType) != null || pluralMap.get(mainType).put(subType.getPlural(), subType) != null) {
            throw new IllegalStateException(MessageFormat.format("Duplicate value given for singular / plural in ElementsItemType {0}'s SubTypes", mainType.getName()));
        }
    }

    /**
     * Method to retrieve a "known" subtype based on ItemType and name from the maps
     * @param type The ElementsItemType to search for sub types.
     * @param value The name of the desired subType.
     * @return The desired type
     */
    public static SubType getSubType(ElementsItemType type, String value) {
        if (singularMap.containsKey(type) && singularMap.get(type).containsKey(value)) {
            return singularMap.get(type).get(value);
        }
        if(pluralMap.containsKey(type) && pluralMap.get(type).containsKey(value)) {
            return pluralMap.get(type).get(value);
        }
        throw new IndexOutOfBoundsException(MessageFormat.format("{0} is not a known subtype of {2}", value, type.getName()));
    }

    /**
     * Method to retrieve all the "known" sub types for a particular item type.
     * @param type The ElementsItemType to search for sub types.
     * @return Collection<SubType> of all registered sub types
     */
    public static Collection<SubType> knownSubTypes (ElementsItemType type) {
        if(singularMap.containsKey(type)) {
            return Collections.unmodifiableCollection(singularMap.get(type).values());
        }
        return Collections.unmodifiableCollection(new HashSet<SubType>());
    }

    //static block to instantiate the generic subtypes encompassing all groups and all relationships
    public static SubType AllObjects = new AggregateSubType(ElementsItemType.OBJECT, false);
    public static SubType AllGroups = new AggregateSubType(ElementsItemType.GROUP, true);
    public static SubType AllRelationships = new AggregateSubType(ElementsItemType.RELATIONSHIP, true);
    public static SubType AllRelationshipTypes = new AggregateSubType(ElementsItemType.RELATIONSHIP_TYPE, true);

    /**
     * Class representing the idea of a subtype of data (e.g. publication within object)
     * If they are "concrete" then they are "known" types which are listed by the knownSubTypes method.
     * If not concrete they might be simple abstract groupings of types that facilitate certain things via the
     * matches method.
     */
    @SuppressWarnings("WeakerAccess")
    public static class SubType{
        private final ElementsItemType mainType;
        private final String singular;
        private final String plural;
        private final boolean isConcrete;

        public ElementsItemType getMainType() { return mainType; }
        public String getSingular() { return singular; }
        public String getPlural() {
            return plural;
        }
        public boolean isConcrete() {
            return isConcrete;
        }

        @SuppressWarnings("SameParameterValue")
        protected SubType(ElementsItemType mainType, String singular, String plural){
            this(mainType, singular, plural, true);
        }

        protected SubType(ElementsItemType mainType, String singular, String plural, boolean registerAsConcrete){
            if (StringUtils.trimToNull(singular) == null) throw new NullArgumentException("singular");
            if (StringUtils.trimToNull(plural) == null) throw new NullArgumentException("plural");
            this.mainType = mainType;
            this.singular = singular;
            this.plural = plural;
            this.isConcrete = registerAsConcrete;
            //add to parent dictionaries
            if(registerAsConcrete) addSubType(this);
        }

        //Note matches is not equals..if a matches b or b matches a then it is a match.
        //if either side believes that it matches the other then it is a match..
        public boolean matches(SubType other){
            return innerMatches(other) || other.innerMatches(this);
        }

        public boolean innerMatches(SubType other){
            return this == other;
        }
    }

    /**
     * Class that can represent an aggregate grouping of subtypes
     * specifically in terms of matches behaviour..
     */
    private static class AggregateSubType extends SubType{
        AggregateSubType(ElementsItemType mainType, boolean registerAsConcrete){
            super(mainType, mainType.getName(), mainType.getPluralName(), registerAsConcrete);
        }

        @Override
        public boolean innerMatches(SubType other){
            return this.getMainType() == other.getMainType();
        }
    }

}