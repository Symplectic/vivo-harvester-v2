/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.model;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class ElementsObjectId {
    private ElementsObjectCategory category;
    private String id;

    public ElementsObjectId(ElementsObjectCategory category, String id) {
        if(category == null) throw new NullArgumentException("category");
        if(StringUtils.isEmpty(id) || StringUtils.isWhitespace(id)) throw new IllegalArgumentException("id must not be null or empty");

        this.category = category;
        this.id = id;
    }

    public ElementsObjectCategory getCategory() {
        return category;
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode(){
        return new HashCodeBuilder(17,31).append(this.getCategory()).append(this.getId()).toHashCode();
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof ElementsObjectId)) return false;
        if(obj == this) return true;

        ElementsObjectId objAsID = (ElementsObjectId) obj;
        if(!(this.getCategory().equals(objAsID.getCategory()))) return false;
        return this.getId().equals(objAsID.getId());
    }
}
