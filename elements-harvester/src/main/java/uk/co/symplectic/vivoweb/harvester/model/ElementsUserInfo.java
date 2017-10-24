/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.model;

import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.utils.ImageUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ElementsUserInfo extends ElementsObjectInfo {

    private UserExtraData additionalInfo;

    //package private as should only ever be created by calls to create on ItemInfo superclass
    ElementsUserInfo(int id) {
        super(ElementsObjectCategory.USER, id);
    }

    public boolean isFullyPopulated(){
        return additionalInfo != null;
    }

    public void addExtraData(UserExtraData additionalInfo){
        this.additionalInfo = additionalInfo;
    }

    public boolean getIsCurrentStaff() {
        if(!isFullyPopulated()) throw new IllegalAccessError("Cannot access current-staff if ElementsUserInfo is not fully populated");
        return additionalInfo.isCurrentStaff;
    }

    public boolean getIsAcademic() {
        if(!isFullyPopulated()) throw new IllegalAccessError("Cannot access isAcademic if ElementsUserInfo is not fully populated");
        return additionalInfo.isAcademic;
    }

    public String getPhotoUrl() { return getPhotoUrl(ImageUtils.PhotoType.PROFILE); }

    public String getPhotoUrl(ImageUtils.PhotoType photoType) {
        if(!isFullyPopulated()) throw new IllegalAccessError("Cannot access photo-url if ElementsUserInfo is not fully populated");
        //Note: additionalInfo.photoUrl is trimmed to null in setter
        if(additionalInfo.photoUrl == null || photoType == null) return additionalInfo.photoUrl;
        switch(photoType){
            case NONE: return null;
            case ORIGINAL: return additionalInfo.photoUrl + "?type=original";
            case THUMBNAIL: return additionalInfo.photoUrl + "?type=thumbnail";
            default : return additionalInfo.photoUrl;
        }
    }

    public String getUsername() {
        if(!isFullyPopulated()) throw new IllegalAccessError("Cannot access username if ElementsUserInfo is not fully populated");
        return additionalInfo.username;
    }

    public String getProprietaryID() {
        if(!isFullyPopulated()) throw new IllegalAccessError("Cannot access proprietaryID if ElementsUserInfo is not fully populated");
        return additionalInfo.proprietaryID;
    }

    public Set<String> getLabelSchemeValues() {
        if(!isFullyPopulated()) throw new IllegalAccessError("Cannot access labelSchemeValue if ElementsUserInfo is not fully populated");
        return Collections.unmodifiableSet(additionalInfo.labelSchemeValues);
    }

    public String getGenericFieldValue() {
        if(!isFullyPopulated()) throw new IllegalAccessError("Cannot access genericFieldValue if ElementsUserInfo is not fully populated");
        return additionalInfo.genericFieldValue;
    }

    public static class UserExtraData{
        private boolean isCurrentStaff = true;
        private boolean isAcademic = true;
        private String photoUrl = null;
        private String username = null;
        private String proprietaryID = null;
        private Set<String> labelSchemeValues = new HashSet<String>();
        private String genericFieldValue = null;

        public UserExtraData setIsCurrentStaff(boolean isCurrentStaff) {
            this.isCurrentStaff = isCurrentStaff;
            return this;
        }

        public void setIsAcademic(boolean isAcademic) {
            this.isAcademic = isAcademic;
        }

        public UserExtraData setPhotoUrl(String photoUrl) {
            this.photoUrl = StringUtils.trimToNull(photoUrl);
            return this;
        }

        public UserExtraData setUsername(String username) {
            this.username = StringUtils.trimToNull(username);
            return this;
        }

        public UserExtraData setProprietaryID(String proprietaryID) {
            this.proprietaryID = StringUtils.trimToNull(proprietaryID);
            return this;
        }

        public UserExtraData addLabelSchemeValue(String labelSchemeValue) {
            String trimmedValue = StringUtils.trimToNull(labelSchemeValue);
            if(trimmedValue != null) this.labelSchemeValues.add(trimmedValue);
            return this;
        }

        public UserExtraData setGenericFieldValue(String genericFieldValue) {
            this.genericFieldValue = StringUtils.trimToNull(genericFieldValue);
            return this;
        }
    }

}
