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
package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.co.symplectic.translate.TranslationDocumentProvider;
import uk.co.symplectic.vivoweb.harvester.utils.ElementsGroupCollection;
import uk.co.symplectic.vivoweb.harvester.model.*;
import uk.co.symplectic.vivoweb.harvester.store.*;
import uk.co.symplectic.vivoweb.harvester.utils.IncludedGroups;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Specialised Concrete subclass of ElementsTranslateObserver
 * Observes RAW_OBJECT data (specifically raw user data) and converts it into TRANSLATED_USER_GROUP_MEMBERSHIP data,
 * by passing in extra information.
 * It Overrides the  observeStoredObject method and calls into the translate methods provided by the super classes
 * to perform the actual work, passing in extraXSLTParameters.
 * One parameter is a document describing the groups (from the Elements group hierarchy) that the user being processed
 * should be shown as being a member of in Vivo given the current harvester configuration:
 * (i.e based on include/exclude/excise groups as represented by the passed in groupCache & includedGroups params)
 * It sets an additional (boolean) parameter to indicate to the crosswalks that that this processing pass
 * should be attempting to create group membership information about the user being processed rather than a general pass
 *
 * Note, observeObjectDeletion is  overridden, likely never called as the group membership info is always processed in full
 * The output rdf is normally cleared down by observeTypeCleardown in ElementsStoreOutputItemObserver
 * */


public class ElementsGroupMembershipTranslateObserver extends ElementsTranslateObserver{

    private final ElementsGroupCollection groupCache;
    private final IncludedGroups includedGroups;

    public ElementsGroupMembershipTranslateObserver(ElementsRdfStore rdfStore, String xslFilename, TranslationDocumentProvider groupListDocument,
                                                    ElementsGroupCollection groupCache, IncludedGroups includedGroups){
        super(rdfStore, xslFilename, groupListDocument, StorableResourceType.RAW_OBJECT, StorableResourceType.TRANSLATED_USER_GROUP_MEMBERSHIP);
        if (groupCache == null) throw new NullArgumentException("groupCache");
        if (includedGroups == null) throw new NullArgumentException("includedGroups");
        this.groupCache = groupCache;
        this.includedGroups = includedGroups;
    }
    @Override
    protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItemInfo item) {
        if (info.getItemId().getItemSubType() == ElementsObjectCategory.USER) {
            ElementsUserInfo userInfo = (ElementsUserInfo) info;
            //if we don't think this is a complete relationships then the current code base can't process it effectively - no point translating it..
            Map<String, Object> extraXSLTParameters = new HashMap<String, Object>();
            Set<ElementsItemId.GroupId> usersIncludedGroups = getUsersIncludedGroups(userInfo);
            extraXSLTParameters.put("userGroupMembershipProcessing", true);
            if(usersIncludedGroups != null){
                extraXSLTParameters.put("userGroups", getUserGroupsDescription(userInfo, usersIncludedGroups));
            }

            translate(item, extraXSLTParameters);

        }
    }

    private Set<ElementsItemId.GroupId> getUsersIncludedGroups(ElementsUserInfo info){
        if(includedGroups.getIncludedGroups().keySet().size() == 0) return null;

        Set<ElementsItemId.GroupId> usersRawGroups = groupCache.getUsersGroups(info.getObjectId());
        if(usersRawGroups == null) return null;
        //otherwise
        Set<ElementsItemId.GroupId> groupsToReturn = new HashSet<ElementsItemId.GroupId>();
        for(ElementsItemId.GroupId groupId : usersRawGroups){
            ElementsGroupInfo.GroupHierarchyWrapper groupWrapper = groupCache.get(groupId);
            ElementsItemId.GroupId nearestIncludedGroup = getNearestIncludedGroup(groupWrapper);
            if(nearestIncludedGroup != null) groupsToReturn.add(nearestIncludedGroup);
        }
        return groupsToReturn.size() == 0 ? null : groupsToReturn;
    }

    private ElementsItemId.GroupId getNearestIncludedGroup(ElementsGroupInfo.GroupHierarchyWrapper group){
        if(group == null) return null;
        ElementsItemId.GroupId groupId = (ElementsItemId.GroupId) group.getGroupInfo().getItemId();
        //if this specific group is excised then we should not try to rewire memberships
        if(includedGroups.getExcisedGroups().keySet().contains(groupId)) return null;
        return getNearestIncludedGroupWalker(group);
    }

    private ElementsItemId.GroupId getNearestIncludedGroupWalker(ElementsGroupInfo.GroupHierarchyWrapper group){
        //note - as long as the source group of the membership is not excised then regardless of whether we pass through excised ones
        //we still want to traverse upwards though them hierarchically to find a valid parent to re-wire membership to.
        if(group == null) return null;
        ElementsItemId.GroupId groupId = (ElementsItemId.GroupId) group.getGroupInfo().getItemId();
        if(includedGroups.getIncludedGroups().keySet().contains(groupId)) return groupId;
        return getNearestIncludedGroupWalker(group.getParent());
    }

    /**
     * Get an XML description of the groups that this user is a member of
     * filtered to only include groups that are "included" in vivo
     * Note, for any group memberships corresponding to excluded groups the system will run up the group hierarchy to find the
     * nearest parent that is included and use that.
     * @param userInfo The ElementsUserInfo object you want the group description document for
     * @param usersIncludedGroups The set of groups that the user is effectively an explicit member of
     * @return A Document describing the user's groups
     */
    private Document getUserGroupsDescription(ElementsUserInfo userInfo, Set<ElementsItemId.GroupId> usersIncludedGroups) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element mainDocRootElement = doc.createElement("usersGroups");
            mainDocRootElement.setAttribute("user-id", Integer.toString(userInfo.getObjectId().getId()));
            doc.appendChild(mainDocRootElement);
            for (ElementsItemId.GroupId groupId : usersIncludedGroups) {
                ElementsGroupInfo.GroupHierarchyWrapper group = groupCache.get(groupId);
//                Element element = doc.createElement("group");
//                element.setAttribute("id", Integer.toString(groupId.getId()));
//                element.setAttribute("name", group.getGroupInfo().getName());
//                mainDocRootElement.appendChild(element);
                mainDocRootElement.appendChild(group.getXMLElementDescriptor(doc));
            }
            return doc;
        }
        catch (ParserConfigurationException pce) {
            throw new IllegalStateException(pce);
        }
    }

    @Override
    protected void observeObjectDeletion(ElementsItemId.ObjectId objectId, StorableResourceType type){
        if (objectId.getItemSubType() == ElementsObjectCategory.USER) {
            //deleting these files here is poor as the correlated change in vivo does not happen until much later.
            safelyDeleteItem(objectId, MessageFormat.format("Unable to delete user group membership rdf for user {0}", objectId.toString()));
        }
    }
}
