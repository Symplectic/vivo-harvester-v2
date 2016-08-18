/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.translate.TemplatesHolder;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.store.*;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import java.util.Set;

public class ElementsObjectTranslateObserver extends IElementsStoredItemObserver.ElementsStoredObjectObserver {
    private ElementsRdfStore rdfStore = null;

    private boolean currentStaffOnly = true;

    private Set<ElementsObjectId> excludedUsers;

    private final TranslationService translationService = new TranslationService();
    private TemplatesHolder templatesHolder = null;

    public ElementsObjectTranslateObserver(ElementsRdfStore rdfStore, String xslFilename, boolean currentStaffOnly, Set<ElementsObjectId> excludedUsers) {
        super(StorableResourceType.RAW_OBJECT);
        if(rdfStore == null) throw new NullArgumentException("rdfStore");
        if(xslFilename == null) throw new NullArgumentException("xslFilename");

        this.rdfStore = rdfStore;

        //TODO : is this sensible - ILLEGAL ARG instead?
        if (!StringUtils.isEmpty(xslFilename)) {
            templatesHolder = new TemplatesHolder(xslFilename);
            translationService.getConfig().setIgnoreFileNotFound(true);
            //TODO : migrate these Configuration access bits somehow?
            translationService.getConfig().addXslParameter("baseURI", Configuration.getBaseURI());
            translationService.getConfig().addXslParameter("recordDir", Configuration.getRawOutputDir());
            translationService.getConfig().setUseFullUTF8(Configuration.getUseFullUTF8());
        }

        this.currentStaffOnly = currentStaffOnly;
        this.excludedUsers = excludedUsers;
    }

    protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item) {
        boolean translateObject = true;

        // TODO: This block with two conditions should be replaced with two interceptor objects...
        if (info.getCategory() == ElementsObjectCategory.USER) {
            ElementsUserInfo userInfo = (ElementsUserInfo)info;
            if (currentStaffOnly) {
                translateObject = translateObject && userInfo.getIsCurrentStaff();
            }
            if (excludedUsers != null && excludedUsers.contains(userInfo.getObjectId())) {
                translateObject = false;  //override if user is in an excluded group
            }
        }

        if (translateObject) {
            //todo: move the zip file stuff somewhere nicer?
            translationService.translate(item, rdfStore, templatesHolder, Configuration.getZipFiles());
        }
    }
}
