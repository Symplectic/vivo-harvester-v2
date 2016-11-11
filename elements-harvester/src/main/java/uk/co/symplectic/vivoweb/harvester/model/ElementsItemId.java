/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.model;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.text.MessageFormat;

public abstract class ElementsItemId{
    private final int id;

    public static ElementsItemId.ObjectId createObjectId(ElementsObjectCategory category, int id){ return new ObjectId(category, id); }
    public static ElementsItemId.GroupId createGroupId(int id){ return new GroupId(id); }
    public static ElementsItemId.RelationshipId createRelationshipId(int id){ return new RelationshipId(id); }

    private final ElementsItemType itemType;
    public ElementsItemType getItemType() { return itemType; }

    protected ElementsItemId(ElementsItemType itemType, int id) {
        if (itemType == null) throw new NullArgumentException("itemType");
        this.itemType = itemType;
        this.id = id;
    }

    public String getItemDescriptor(){
        //requires that these names and the singular names of the object categories remain distinct for good hash code and equals behaviour
        return itemType.getName();
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode(){
        return new HashCodeBuilder(17,31).append(this.getItemDescriptor()).append(this.getId()).toHashCode();
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof ElementsItemId)) return false;
        if(obj == this) return true;

        ElementsItemId objAsID = (ElementsItemId) obj;
        if(!(this.getItemDescriptor().equals(objAsID.getItemDescriptor()))) return false;
        return this.getId() == (objAsID.getId());
    }

    @Override
    public String toString(){
        return MessageFormat.format("{0}:{1}", getItemDescriptor(), Integer.toString(id));
    }

    public static class ObjectId extends ElementsItemId {
        private final ElementsObjectCategory category;

        private ObjectId(ElementsObjectCategory category, int id) {
            super(ElementsItemType.OBJECT, id);
            if (category == null) throw new NullArgumentException("category");
            this.category = category;
        }

        public ElementsObjectCategory getCategory() { return category; }

        @Override
        public String getItemDescriptor(){return category.getSingular();}
    }

    public static class GroupId extends ElementsItemId {
        private GroupId(int id) { super(ElementsItemType.GROUP, id); }
    }

    public static class RelationshipId extends ElementsItemId {
        private RelationshipId(int id) { super(ElementsItemType.RELATIONSHIP, id); }
    }
}

