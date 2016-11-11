/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.model;

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

    public String getPhotoUrl() {
        if(!isFullyPopulated()) throw new IllegalAccessError("Cannot access photo-url if ElementsUserInfo is not fully populated");
        return additionalInfo.photoUrl;
    }


    public String getUsername() {
        if(!isFullyPopulated()) throw new IllegalAccessError("Cannot access username if ElementsUserInfo is not fully populated");
        return additionalInfo.username;
    }

    public static class UserExtraData{
        private boolean isCurrentStaff = true;
        private boolean isAcademic = true;
        private String photoUrl = null;
        private String username = null;


        public UserExtraData setIsCurrentStaff(boolean isCurrentStaff) {
            this.isCurrentStaff = isCurrentStaff;
            return this;
        }

        public void setIsAcademic(boolean isAcademic) {
            this.isAcademic = isAcademic;
        }

        public UserExtraData setPhotoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
            return this;
        }

        public UserExtraData setUsername(String username) {
            this.username = username;
            return this;
        }


    }

}
