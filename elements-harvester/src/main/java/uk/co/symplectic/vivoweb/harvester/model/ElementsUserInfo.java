/*
 * ******************************************************************************
 *   Copyright (c) 2019 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.model;

import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.utils.ImageUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Subclass of ElementsItemInfo to store and expose a set of data relating to an Elements Users
 * Exposes a set of data about the user: isPublic, isCurrent, isAcademic, photoUrl, username, proprietaryID.
 * Additionally exposes data about a specific label scheme or generic field.
 * The scheme and field to be looked at are specified by the static values on ElementsObjectInfo.Extractor
 *
 * All this Additional data is provided as a single update (a UserExtraData object) in the addExtraData method.
 * which is deliberately package private.
 */

//TODO: separate extractor code for users into this class?
public class ElementsUserInfo extends ElementsObjectInfo {

    private UserExtraData additionalInfo;

    //package private as should only ever be created by calls to create on ItemInfo superclass
    ElementsUserInfo(int id) {
        super(ElementsObjectCategory.USER, id);
    }

    private boolean isNotFullyPopulated(){
        return additionalInfo == null;
    }

    void addExtraData(UserExtraData additionalInfo){
        this.additionalInfo = additionalInfo;
    }

    public boolean getIsPublic() {
        if(isNotFullyPopulated()) throw new IllegalAccessError("Cannot access isPublic if ElementsUserInfo is not fully populated");
        return additionalInfo.isPublic;
    }

    public boolean getIsCurrentStaff() {
        if(isNotFullyPopulated()) throw new IllegalAccessError("Cannot access current-staff if ElementsUserInfo is not fully populated");
        return additionalInfo.isCurrentStaff;
    }

    public boolean getIsAcademic() {
        if(isNotFullyPopulated()) throw new IllegalAccessError("Cannot access isAcademic if ElementsUserInfo is not fully populated");
        return additionalInfo.isAcademic;
    }

    @SuppressWarnings("unused")
    public String getPhotoUrl() { return getPhotoUrl(ImageUtils.PhotoType.PROFILE); }

    public String getPhotoUrl(ImageUtils.PhotoType photoType) {
        if(isNotFullyPopulated()) throw new IllegalAccessError("Cannot access photo-url if ElementsUserInfo is not fully populated");
        //Note: additionalInfo.photoUrl is trimmed to null in setter
        if(additionalInfo.photoUrl == null || photoType == null) return additionalInfo.photoUrl;
        try {
            URI photoUrl = new URI(additionalInfo.photoUrl);
            //strip out parameters if any are present
            photoUrl = new URI(photoUrl.getScheme(), null , photoUrl.getHost(), photoUrl.getPort(), photoUrl.getPath(), null, null);
            switch(photoType){
                case NONE: return null;
                case ORIGINAL: return photoUrl.toString() + "?type=original";
                case THUMBNAIL: return photoUrl.toString() + "?type=thumbnail";
                default : return photoUrl.toString();
            }
        }
        catch (URISyntaxException e){
            throw new IllegalStateException(e);
        }
    }

    public String getUsername() {
        if(isNotFullyPopulated()) throw new IllegalAccessError("Cannot access username if ElementsUserInfo is not fully populated");
        return additionalInfo.username;
    }

    public String getProprietaryID() {
        if(isNotFullyPopulated()) throw new IllegalAccessError("Cannot access proprietaryID if ElementsUserInfo is not fully populated");
        return additionalInfo.proprietaryID;
    }

    public Set<String> getLabelSchemeValues() {
        if(isNotFullyPopulated()) throw new IllegalAccessError("Cannot access labelSchemeValue if ElementsUserInfo is not fully populated");
        return Collections.unmodifiableSet(additionalInfo.labelSchemeValues);
    }

    public String getGenericFieldValue() {
        if(isNotFullyPopulated()) throw new IllegalAccessError("Cannot access genericFieldValue if ElementsUserInfo is not fully populated");
        return additionalInfo.genericFieldValue;
    }

    @SuppressWarnings("UnusedReturnValue")
    static class UserExtraData{
        private boolean isPublic = true;
        private boolean isCurrentStaff = true;
        private boolean isAcademic = true;
        private String photoUrl = null;
        private String username = null;
        private String proprietaryID = null;
        private Set<String> labelSchemeValues = new HashSet<String>();
        private String genericFieldValue = null;

        UserExtraData setIsPublic(boolean isPublic) {
            this.isPublic = isPublic;
            return this;
        }

        UserExtraData setIsCurrentStaff(boolean isCurrentStaff) {
            this.isCurrentStaff = isCurrentStaff;
            return this;
        }

        void setIsAcademic(boolean isAcademic) {
            this.isAcademic = isAcademic;
        }

        UserExtraData setPhotoUrl(String photoUrl) {
            this.photoUrl = StringUtils.trimToNull(photoUrl);
            return this;
        }

        UserExtraData setUsername(String username) {
            this.username = StringUtils.trimToNull(username);
            return this;
        }

        UserExtraData setProprietaryID(String proprietaryID) {
            this.proprietaryID = StringUtils.trimToNull(proprietaryID);
            return this;
        }

        UserExtraData addLabelSchemeValue(String labelSchemeValue) {
            String trimmedValue = StringUtils.trimToNull(labelSchemeValue);
            if(trimmedValue != null) this.labelSchemeValues.add(trimmedValue);
            return this;
        }

        UserExtraData setGenericFieldValue(String genericFieldValue) {
            this.genericFieldValue = StringUtils.trimToNull(genericFieldValue);
            return this;
        }
    }

}
