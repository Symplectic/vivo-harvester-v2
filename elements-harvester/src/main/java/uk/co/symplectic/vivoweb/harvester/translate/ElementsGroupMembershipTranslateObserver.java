/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.co.symplectic.vivoweb.harvester.utils.ElementsGroupCollection;
import uk.co.symplectic.vivoweb.harvester.utils.ElementsItemKeyedCollection;
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

public class ElementsGroupMembershipTranslateObserver extends ElementsTranslateObserver{

    private static final Logger log = LoggerFactory.getLogger(ElementsGroupMembershipTranslateObserver.class);
    private final ElementsGroupCollection groupCache;
    private final IncludedGroups includedGroups;

    public ElementsGroupMembershipTranslateObserver(ElementsRdfStore rdfStore, String xslFilename,
                                                    ElementsGroupCollection groupCache, IncludedGroups includedGroups){
        super(rdfStore, xslFilename, StorableResourceType.RAW_OBJECT, StorableResourceType.TRANSLATED_USER_GROUP_MEMBERSHIP);
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
                extraXSLTParameters.put("userGroups", getExtraObjectsDescription(userInfo, usersIncludedGroups));
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
        ElementsItemId.GroupId groupId = (ElementsItemId.GroupId) group.getGroupInfo().getItemId();
        if(includedGroups.getIncludedGroups().keySet().contains(groupId)) return groupId;
        return getNearestIncludedGroupWalker(group.getParent());
    }

    /**
     * Get an XML description of the groups that this user is a member of
     * filterered to only include groups that are "included" in vivo
     * Note, for any group memberships corresponding to excluded groups the system will run up the group hierarchy to find the
     * nearest parent that is included and use that.
     * @param userInfo
     * @param usersIncludedGroups
     * @return
     */
    private Document getExtraObjectsDescription(ElementsUserInfo userInfo, Set<ElementsItemId.GroupId> usersIncludedGroups) {
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
