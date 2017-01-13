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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.co.symplectic.vivoweb.harvester.fetch.ElementsGroupCollection;
import uk.co.symplectic.vivoweb.harvester.fetch.ElementsItemKeyedCollection;
import uk.co.symplectic.vivoweb.harvester.model.ElementsGroupInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsUserInfo;
import uk.co.symplectic.vivoweb.harvester.store.ElementsRdfStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItem;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;
import java.util.Map;

public class ElementsGroupTranslateObserver extends ElementsTranslateObserver {

    private final ElementsGroupCollection groupCache;
    private final ElementsItemKeyedCollection.ItemInfo includedUsers;

    public ElementsGroupTranslateObserver(ElementsRdfStore rdfStore, String xslFilename, ElementsGroupCollection groupCache, ElementsItemKeyedCollection.ItemInfo includedUsers){
        super(rdfStore, xslFilename, StorableResourceType.RAW_GROUP, StorableResourceType.TRANSLATED_GROUP);
        if (groupCache == null) throw new NullArgumentException("groupCache");
        if (includedUsers == null) throw new NullArgumentException("includedUsers");
        this.groupCache = groupCache;
        this.includedUsers = includedUsers;
    }
    @Override
    protected void observeStoredGroup(ElementsGroupInfo info, ElementsStoredItem item) {
        Map<String, Object> extraXSLTParameters = new HashMap<String, Object>();
        extraXSLTParameters.put("groupMembers", getGroupMembershipDescription(info));
        translate(item, extraXSLTParameters);
    }

    private Document getGroupMembershipDescription(ElementsGroupInfo info){
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("groupMembers");
            doc.appendChild(rootElement);
            ElementsGroupInfo.GroupHierarchyWrapper groupDescription = groupCache.get(info.getItemId());
            for(ElementsItemId user : groupDescription.getExplicitUsers()){
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