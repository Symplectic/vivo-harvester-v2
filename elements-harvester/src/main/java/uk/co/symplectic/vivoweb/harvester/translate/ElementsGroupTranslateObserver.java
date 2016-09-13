/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.co.symplectic.translate.TemplatesHolder;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.fetch.ElementsGroupCollection;
import uk.co.symplectic.vivoweb.harvester.fetch.ElementsObjectKeyedCollection;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.ElementsRdfStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.IElementsStoredItemObserver;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ajpc2_000 on 05/09/2016.
 */
public class ElementsGroupTranslateObserver extends IElementsStoredItemObserver.ElementsStoredResourceObserverAdapter {
    //TODO: fix object as static thing here
    private final TranslationService translationService = new TranslationService();
    private TemplatesHolder templatesHolder;

    private final ElementsRdfStore rdfStore;
    private final ElementsGroupCollection groupCache;
    private final ElementsObjectKeyedCollection.ObjectInfo includedUsers;

    public ElementsGroupTranslateObserver(ElementsRdfStore rdfStore, String xslFilename, ElementsGroupCollection groupCache, ElementsObjectKeyedCollection.ObjectInfo includedUsers) {
        super(StorableResourceType.RAW_GROUP);
        if (rdfStore == null) throw new NullArgumentException("rdfStore");
        if (xslFilename == null) throw new NullArgumentException("xslFilename");
        if (groupCache == null) throw new NullArgumentException("groupCache");
        if (includedUsers == null) throw new NullArgumentException("includedUsers");

        this.rdfStore = rdfStore;
        this.groupCache = groupCache;
        this.includedUsers = includedUsers;

        //TODO : is this sensible - ILLEGAL ARG instead?
        if (!StringUtils.isEmpty(xslFilename)) {
            templatesHolder = new TemplatesHolder(xslFilename);
            translationService.getConfig().setIgnoreFileNotFound(true);
            //TODO : migrate these Configuration access bits somehow?
            translationService.getConfig().addXslParameter("baseURI", Configuration.getBaseURI());
            //translationService.getConfig().addXslParameter("recordDir", Configuration.getRawOutputDir());
            translationService.getConfig().setUseFullUTF8(Configuration.getUseFullUTF8());
        }
    }

    @Override
    protected void observeStoredGroup(ElementsGroupInfo info, ElementsStoredItem item) {
        //todo: make this somehow pass along the group membership as a param to the translation
        Map<String, Object> extraXSLTParameters = new HashMap<String, Object>();
        extraXSLTParameters.put("groupMembers", getGroupMembershipDescription(info));
        translationService.translate(item, rdfStore, templatesHolder, extraXSLTParameters);
    }

    private Document getGroupMembershipDescription(ElementsGroupInfo info){
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("groupMembers");
            doc.appendChild(rootElement);
            ElementsGroupInfo.GroupHierarchyWrapper groupDescription = groupCache.getGroup(info.getId());
            for(ElementsItemId.ObjectId user : groupDescription.getExplicitUsers()){
                //if the group member is in the set of included users we need to inform the translation stage about that membership.
                if(includedUsers.keySet().contains(user)) {
                    //create an Element to reference the user we are processing.
                    ElementsUserInfo userInfo = (ElementsUserInfo) includedUsers.get(user);
                    Element userElement = doc.createElement("user");

                    //create id  and username attributes on our user Element
                    userElement.setAttribute("id", Integer.toString(userInfo.getObjectId().getId()));
                    userElement.setAttribute("username", userInfo.getUsername());

                    //add our  user Element to the root of the doc;
                    rootElement.appendChild(userElement);
                }
            }
            return doc;
        }
        catch (ParserConfigurationException pce) {
            throw new IllegalStateException(pce);
        }
    }
}