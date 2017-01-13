/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.model;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.xml.XMLEventProcessor;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static uk.co.symplectic.elements.api.ElementsAPI.apiNS;
import static uk.co.symplectic.elements.api.ElementsAPI.atomNS;

public class ElementsGroupInfo extends ElementsItemInfo{
    public static class Extractor extends XMLEventProcessor.ItemExtractingFilter<ElementsGroupInfo>{

        public static DocumentLocation fileEntryLocation = new DocumentLocation(new QName(atomNS, "entry"), new QName(apiNS, "user-group"));
        public static DocumentLocation feedEntryLocation = new DocumentLocation(new QName(atomNS, "feed"), new QName(atomNS, "entry"), new QName(apiNS, "user-group"));

        private ElementsGroupInfo workspace;

        public Extractor(DocumentLocation location, int maximumAmountExpected){
            super(location, maximumAmountExpected);
        }

        @Override
        protected void initialiseItemExtraction(StartElement initialElement, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
            String id = initialElement.getAttributeByName(new QName("id")).getValue();
            workspace = ElementsItemInfo.createGroupItem(Integer.parseInt(id));
        }

        @Override
        protected void processEvent(XMLEvent event, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                QName name = startElement.getName();
                if (name.equals(new QName(apiNS, "name"))) {
                    XMLEvent nextEvent = readerProxy.peek();
                    if (nextEvent.isCharacters()) {
                        workspace.setName((nextEvent.asCharacters().getData()));
                    }
                }
                else if(name.equals(new QName(apiNS, "parent"))){
                    String parentId = startElement.getAttributeByName(new QName("id")).getValue();
                    workspace.setParentId(new Integer(parentId));
                }
                else if(name.equals(new QName(apiNS, "explicit-group-members"))){
                    String url = startElement.getAttributeByName(new QName("href")).getValue();
                    workspace.setMembershipFeedUrl(url);
                }
            }
        }

        @Override
        protected ElementsGroupInfo finaliseItemExtraction(EndElement finalElement, XMLEventProcessor.ReaderProxy readerProxy) {
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
    public void setParentId(Integer parentId) { this.parentId = ElementsItemId.createGroupId(parentId); }

    public static class GroupHierarchyWrapper{

        private final ElementsGroupInfo info;
        private ElementsGroupInfo.GroupHierarchyWrapper parent = null;
        private Set<ElementsGroupInfo.GroupHierarchyWrapper> children = new HashSet<ElementsGroupInfo.GroupHierarchyWrapper>();
        private Set<ElementsItemId> explicitUsers = new HashSet<ElementsItemId>();

        public GroupHierarchyWrapper(ElementsGroupInfo info) {
            if(info == null) throw new NullArgumentException("info");
            this.info = info;
        }

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
            Set<GroupHierarchyWrapper> set = new HashSet<GroupHierarchyWrapper>();
            collectChildren(set);
            return Collections.unmodifiableSet(set);
        }

        protected void collectChildren(Set<GroupHierarchyWrapper> returnSet){
            for(ElementsGroupInfo.GroupHierarchyWrapper child : children){
                child.collectChildren(returnSet);
                returnSet.add(child);
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
    }

}
