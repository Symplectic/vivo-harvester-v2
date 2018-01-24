/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.model;

import org.apache.commons.lang.NullArgumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.co.symplectic.utils.xml.XMLEventProcessor;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static uk.co.symplectic.elements.api.ElementsAPI.apiNS;
import static uk.co.symplectic.elements.api.ElementsAPI.atomNS;

public class ElementsGroupInfo extends ElementsItemInfo{
    public static class Extractor extends XMLEventProcessor.ItemExtractingFilter<ElementsItemInfo>{

        private static DocumentLocation fileEntryLocation = new DocumentLocation(new QName(atomNS, "entry"), new QName(apiNS, "user-group"));
        private static DocumentLocation feedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "user-group"));

        public static Extractor getExtractor(ElementsItemInfo.ExtractionSource source, int maximumExpected){
            switch(source) {
                case FEED : return new Extractor(feedEntryLocation, maximumExpected);
                case FILE : return new Extractor(fileEntryLocation, maximumExpected);
                default : throw new IllegalStateException("invalid extractor source type requested");
            }
        }

        private ElementsGroupInfo workspace;

        protected Extractor(DocumentLocation location, int maximumAmountExpected){
            super(location, maximumAmountExpected);
        }

        @Override
        protected void initialiseItemExtraction(XMLEventProcessor.WrappedXmlEvent initialEvent) throws XMLStreamException {
            workspace = ElementsItemInfo.createGroupItem(Integer.parseInt(initialEvent.getAttribute("id")));
        }

        @Override
        protected void processEvent(XMLEventProcessor.WrappedXmlEvent event, List<QName> relativeLocation) throws XMLStreamException {
            if (event.isRelevantForExtraction()) {
                QName name = event.getName();
                if (name.equals(new QName(apiNS, "name"))) {
                    workspace.setName(event.getValueOrNull());
                }
                else if(name.equals(new QName(apiNS, "parent"))){
                    //expect the id to be present if element is - so no "has" check
                    workspace.setParentId(new Integer(event.getAttribute("id")));
                }
                else if(name.equals(new QName(apiNS, "explicit-group-members"))){
                    //expect the href to be present if element is - so no "has" check
                    workspace.setMembershipFeedUrl(event.getAttribute("href"));
                }
            }
        }

        @Override
        protected ElementsGroupInfo finaliseItemExtraction(XMLEventProcessor.WrappedXmlEvent finalEvent) {
            return workspace;
        }
    }

    private ElementsItemId.GroupId parentId;
    private String membershipFeedUrl;
    private String name;

    //package private as should only ever be constructed by create calls into superclass
    protected ElementsGroupInfo(int id) {
        super(ElementsItemId.createGroupId(id));
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMembershipFeedUrl(){ return membershipFeedUrl; }
    public void setMembershipFeedUrl(String value){ membershipFeedUrl = value; }

    public ElementsItemId.GroupId getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId == null ? null : ElementsItemId.createGroupId(parentId); }

    public static class GroupHierarchyWrapper{

        private final ElementsGroupInfo info;
        private ElementsGroupInfo.GroupHierarchyWrapper parent = null;
        private Set<ElementsGroupInfo.GroupHierarchyWrapper> children = new HashSet<ElementsGroupInfo.GroupHierarchyWrapper>();
        private Set<ElementsItemId> explicitUsers = new HashSet<ElementsItemId>();
        private String uniqueName = null;

        public GroupHierarchyWrapper(ElementsGroupInfo info) {
            if(info == null) throw new NullArgumentException("info");
            this.info = info;
        }

        public String getUniqueName(){return this.uniqueName;}

        public void setUniqueName(String value){this.uniqueName = value;}

        public ElementsGroupInfo getGroupInfo() {
            return info;
        }

        public ElementsGroupInfo.GroupHierarchyWrapper getParent() {
            return parent;
        }

        public void setParent(ElementsGroupInfo.GroupHierarchyWrapper parent) {
            this.parent = parent;
            if(parent != null) parent.children.add(this);
        }

        public Set<ElementsGroupInfo.GroupHierarchyWrapper> getChildren() {
            return Collections.unmodifiableSet(children);
        }

        public Set<ElementsGroupInfo.GroupHierarchyWrapper> getAllChildren() {
            return getAllChildren(null);
        }

        public Set<ElementsGroupInfo.GroupHierarchyWrapper> getAllChildren(GroupInclusionFilter filter) {
            Set<GroupHierarchyWrapper> set = new HashSet<GroupHierarchyWrapper>();
            collectChildren(set, filter);
            return Collections.unmodifiableSet(set);
        }

        protected void collectChildren(Set<GroupHierarchyWrapper> returnSet, GroupInclusionFilter filter){
            for(ElementsGroupInfo.GroupHierarchyWrapper child : children){
                if(filter == null || filter.includeGroup(child)) {
                    child.collectChildren(returnSet, filter);
                    returnSet.add(child);
                }
            }
        }

        public void addChildren(Set<ElementsGroupInfo.GroupHierarchyWrapper> children) {
            for(ElementsGroupInfo.GroupHierarchyWrapper child : children){
                addChild(child);
            }
        }

        public void addChild(ElementsGroupInfo.GroupHierarchyWrapper child) {
            if(child != null){
                child.parent = this;
                this.children.add(child);
            }
        }

        public Set<ElementsItemId> getExplicitUsers() {
            return Collections.unmodifiableSet(explicitUsers);
        }

        //TODO: make these collection restricted to only be able to contain users in some safe way?
        public Set<ElementsItemId> getImplicitUsers() {
            Set<ElementsItemId> set = new HashSet<ElementsItemId>();
            collectImplicitUsers(set);
            return Collections.unmodifiableSet(set);
        }

        //TODO: make these collection restricted to only be able to contain users in some safe way?
        protected void collectImplicitUsers(Set<ElementsItemId> returnSet){
            for(ElementsGroupInfo.GroupHierarchyWrapper child : children){
                child.collectImplicitUsers(returnSet);
            }
            returnSet.addAll(explicitUsers);
        }

        public void addExplicitUser(ElementsItemId.ObjectId user) {
            if(user != null && user.getItemSubType() == ElementsObjectCategory.USER) explicitUsers.add(user);
        }

        /**
         * Method to turn a hierarchy wrapper into a simple XML descriptor - useful for sending into the XSLT framework as a descriptor.
         * @param contextDocument
         * @return
         */
        public Element getXMLElementDescriptor(Document contextDocument){
            Element element = contextDocument.createElement("group");
            element.setAttribute("id", Integer.toString(getGroupInfo().getItemId().getId()));
            element.setAttribute("name", getGroupInfo().getName());
            element.setAttribute("unique-name", getUniqueName());
            return element;
        }
    }

    public static abstract class GroupInclusionFilter{
        public abstract boolean includeGroup(ElementsGroupInfo.GroupHierarchyWrapper group);
    }

}
