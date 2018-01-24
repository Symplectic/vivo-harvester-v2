/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.translate;

import org.apache.commons.lang.NullArgumentException;
import org.w3c.dom.Document;
import uk.co.symplectic.vivoweb.harvester.utils.ElementsGroupCollection;
import uk.co.symplectic.vivoweb.harvester.utils.ElementsItemKeyedCollection;
import uk.co.symplectic.vivoweb.harvester.model.ElementsGroupInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.store.ElementsRdfStore;
import uk.co.symplectic.vivoweb.harvester.store.ElementsStoredItemInfo;
import uk.co.symplectic.vivoweb.harvester.store.StorableResourceType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;
import java.util.Map;

public class ElementsGroupTranslateObserver extends ElementsTranslateObserver {

    private final ElementsGroupCollection groupCache;
    private final ElementsItemKeyedCollection.ItemInfo includedGroups;

    public ElementsGroupTranslateObserver(ElementsRdfStore rdfStore, String xslFilename, ElementsGroupCollection groupCache,
                                          ElementsItemKeyedCollection.ItemInfo includedGroups){
        super(rdfStore, xslFilename, StorableResourceType.RAW_GROUP, StorableResourceType.TRANSLATED_GROUP);
        if (groupCache == null) throw new NullArgumentException("groupCache");
        if (includedGroups == null) throw new NullArgumentException("includedGroups");
        this.groupCache = groupCache;
        this.includedGroups = includedGroups;
    }
    @Override
    protected void observeStoredGroup(ElementsGroupInfo info, ElementsStoredItemInfo item) {
        Map<String, Object> extraXSLTParameters = new HashMap<String, Object>();
        ElementsItemId parentId = getIncludedParentGroupId(info);
        //extraXSLTParameters.put("includedParentGroupId", parentId == null ? null : Integer.toString(parentId.getId()));
        extraXSLTParameters.put("includedParentGroup", parentId == null ? null : getParentGroupDescriptor(parentId));
        extraXSLTParameters.put("uniqueGroupName", groupCache.get(info.getItemId()).getUniqueName());
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

    private Document getParentGroupDescriptor(ElementsItemId includedParentGroupId) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            ElementsGroupInfo.GroupHierarchyWrapper group = groupCache.get(includedParentGroupId);
            doc.appendChild(group.getXMLElementDescriptor(doc));
            return doc;
        } catch (ParserConfigurationException pce) {
            throw new IllegalStateException(pce);
        }
    }
}