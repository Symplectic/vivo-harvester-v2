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
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItemInfo;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ElementsGroupTranslateObserver extends ElementsTranslateObserver {

    private final ElementsGroupCollection groupCache;
    private final ElementsItemKeyedCollection.ItemInfo includedUsers;
    private final ElementsItemKeyedCollection.ItemInfo includedGroups;

    public ElementsGroupTranslateObserver(ElementsRdfStore rdfStore, String xslFilename, ElementsGroupCollection groupCache,
                                          ElementsItemKeyedCollection.ItemInfo includedUsers, ElementsItemKeyedCollection.ItemInfo includedGroups){
        super(rdfStore, xslFilename, StorableResourceType.RAW_GROUP, StorableResourceType.TRANSLATED_GROUP);
        if (groupCache == null) throw new NullArgumentException("groupCache");
        if (includedUsers == null) throw new NullArgumentException("includedUsers");
        this.groupCache = groupCache;
        this.includedUsers = includedUsers;
        this.includedGroups = includedGroups;
    }
    @Override
    protected void observeStoredGroup(ElementsGroupInfo info, ElementsStoredItemInfo item) {
        Map<String, Object> extraXSLTParameters = new HashMap<String, Object>();
        extraXSLTParameters.put("groupMembers", getGroupMembershipDescription(info));
        ElementsItemId parentId = getIncludedParentGroupId(info);
        extraXSLTParameters.put("includedParentGroupId", parentId == null ? null : Integer.toString(parentId.getId()));
        translate(item, extraXSLTParameters);
    }

    private ElementsItemId getIncludedParentGroupId(ElementsGroupInfo info){
        ElementsGroupInfo.GroupHierarchyWrapper groupDescription = groupCache.get(info.getItemId());
        while(groupDescription.getParent() != null){
            ElementsGroupInfo.GroupHierarchyWrapper parentDescription = groupDescription.getParent();
            ElementsItemId parentGroupId = parentDescription.getGroupInfo().getItemId();
            if(includedGroups.keySet().contains(parentGroupId)) return parentGroupId;
            groupDescription = parentDescription;
        }
        return null;
    }

    private Document getGroupMembershipDescription(ElementsGroupInfo info){
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("groupMembers");
            doc.appendChild(rootElement);
            ElementsGroupInfo.GroupHierarchyWrapper groupDescription = groupCache.get(info.getItemId());

            Set<ElementsItemId> groupMembers = new HashSet<ElementsItemId>();

            //everyone who is an explicit member of this group is a member.
            groupMembers.addAll(groupDescription.getExplicitUsers());

            //additionally we want to include anyone who is an implicit member of this group but NOT part of an otherwise included sub group
            //start by collecting all the child groups of the current group that are NOT included.
            ElementsGroupInfo.GroupInclusionFilter filter = new ElementsGroupInfo.GroupInclusionFilter(){
                @Override
                public boolean includeGroup(ElementsGroupInfo.GroupHierarchyWrapper group){
                    //we want to process the children of the current group that are NOT going to be included.
                    return !includedGroups.keySet().contains(group.getGroupInfo().getItemId());
                }
            };

            for(ElementsGroupInfo.GroupHierarchyWrapper nonIncludedChildgroup : groupDescription.getAllChildren(filter)){
                //for all child groups of the current group that are NOT included we add their explicit users to our set..
                groupMembers.addAll(nonIncludedChildgroup.getExplicitUsers());
            }

            //go through all the users that are part of this group, or are part of the included groups set.
            for(ElementsItemId user : groupMembers){
                //if the group member is in the set of included users we nesed to inform the translation stage about that membership.
                if(includedUsers.keySet().contains(user)) {
                    //create an Element to reference the user we are processing.
                    ElementsUserInfo userInfo = (ElementsUserInfo) includedUsers.get(user);
                    Element userElement = doc.createElement("user");

                    //create id  and username attributes on our user Element
                    userElement.setAttribute("id", Integer.toString(userInfo.getObjectId().getId()));
                    userElement.setAttribute("username", userInfo.getUsername());
                    if(userInfo.getProprietaryID() != null) userElement.setAttribute("proprietary-id", userInfo.getProprietaryID());

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