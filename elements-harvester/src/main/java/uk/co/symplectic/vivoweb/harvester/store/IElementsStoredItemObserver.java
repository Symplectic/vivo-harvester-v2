/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Interface to represent the most basic concept of a Store observer.
 * defines callback methods to be implemented (and called into by an observed store) for:
 *     item added to/touched in Store
 *     item deleted from Store
 *     type cleared down in Store
 */


public interface IElementsStoredItemObserver{

    /**
     * Method to be called into if a resource is added to or touched in the observed Store.
     * @param item the affected item as an ElementsStoredItemInfo object (which includes resource type, and access to the
     *             underlying data.
     */
    void observe(ElementsStoredItemInfo item);

    /**
     * Method to be called into if a resource is deleted from the observed Store.
     * @param itemId the ElementsItemId associated with the deleted resource.
     * @param type the StorableResourceType of the resource that was deleted.
     */
    void observeDeletion(ElementsItemId itemId, StorableResourceType type);

    /**
     * Method to be called into if a resource type is cleared down entirely in the observed Store.
     * @param type the StorableResourceType of the resource that was deleted.
     * @param source the ElementsItemStore where the cleardown occurred.
     */
    void observeCleardown(StorableResourceType type, ElementsItemStore source);


    /**
     * An abstract base class for implementing IElementsStoredItemObservers that restricts the observer to only act
     * for certain resource types (as provided in the constructor).
     *
     * implements the interface methods: observe; observeDeletion and observeCleardown
     * tests that the current event is for one of the supported resourceTypes and then breaks the calls out to abstract
     * method stubs based on the affected item type:
     *
     * observeStoredObject/observeStoredRelationship/observeStoredGroup
     * observeObjectDeletion/observeRelationshipDeletion
     * observeTypeCleardown
     * Note: groups are never deleted as there is no stream of deleted data..
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    abstract class ElementsStoredResourceObserver implements IElementsStoredItemObserver {

        private final Set<StorableResourceType> resourceTypes = new HashSet<StorableResourceType>();

        protected ElementsStoredResourceObserver(StorableResourceType... resourceTypes) {
            if (resourceTypes == null) throw new NullArgumentException("resourceTypes");
            if (resourceTypes.length == 0) throw new IllegalArgumentException("resourceTypes must not be empty");
            this.resourceTypes.addAll(Arrays.asList(resourceTypes));
        }

        protected boolean supportsInputType(StorableResourceType type){
            return this.resourceTypes.contains(type);
        }

        //see interface for javadoc
        @Override
        final public void observe(ElementsStoredItemInfo item) {
            if (supportsInputType(item.getResourceType())) {

                ElementsItemType itemType = item.getResourceType().getKeyItemType();
                if(itemType == ElementsItemType.OBJECT){
                    observeStoredObject(item.getItemInfo().asObjectInfo(), item);
                }
                else if(itemType == ElementsItemType.RELATIONSHIP){
                    observeStoredRelationship(item.getItemInfo().asRelationshipInfo(), item);
                }
                else if(itemType == ElementsItemType.GROUP){
                    observeStoredGroup(item.getItemInfo().asGroupInfo(), item);
                }
                else throw new IllegalStateException("Unhandled Item Type error");
            }
        }

        //broken out calls for implementers to fill in

        /**
         * Method called into if the stored/touched item is of an appropriate type and is an Object
         * @param info the typed ElementsObjectInfo from the underlying ElementsStoredItemInfo
         * @param item the underlying ElementsStoredItemInfo
         */
        protected abstract void observeStoredObject(ElementsObjectInfo info, ElementsStoredItemInfo item);

        /**
         * Method called into if the stored/touched item is of an appropriate type and is a Relationship
         * @param info the typed ElementsRelationshipInfo from the underlying ElementsStoredItemInfo
         * @param item the underlying ElementsStoredItemInfo
         */
        protected abstract void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItemInfo item);

        /**
         * Method called into if the stored/touched item is of an appropriate type and is a Group
         * @param info the typed ElementsGroupInfo from the underlying ElementsStoredItemInfo
         * @param item the underlying ElementsStoredItemInfo
         */
        protected abstract void observeStoredGroup(ElementsGroupInfo info, ElementsStoredItemInfo item);

        //see interface for javadoc
        @Override
        final public void observeDeletion(ElementsItemId itemId, StorableResourceType type) {
            if (supportsInputType(type)) {

                ElementsItemType itemType = type.getKeyItemType();
                if(itemType == ElementsItemType.OBJECT){
                    observeObjectDeletion((ElementsItemId.ObjectId) itemId, type);
                }
                else if(itemType == ElementsItemType.RELATIONSHIP){
                    observeRelationshipDeletion((ElementsItemId.RelationshipId) itemId, type);
                }
                //groups are not relevant for deletions as they have no delta based resources for them
                else throw new IllegalStateException("Unhandled Item Type error");
            }
        }

        //broken out calls for implementers to fill in

        /**
         * Method called into if the deleted item is of an appropriate type and is an Object
         * @param objectId the id of the Elements object associated with the resource being deleted
         * @param type the StorableResourceType of the resource that was deleted.
         */
        protected abstract void observeObjectDeletion(ElementsItemId.ObjectId objectId, StorableResourceType type);

        /**
         * Method called into if the deleted item is of an appropriate type and is a Relationship
         * @param relationshipId the id of the Elements relationship associated with the resource being deleted
         * @param type the StorableResourceType of the resource that was deleted.
         */
        protected abstract void observeRelationshipDeletion(ElementsItemId.RelationshipId relationshipId, StorableResourceType type);

        //see interface for javadoc
        @Override
        final public void observeCleardown(StorableResourceType type, ElementsItemStore source){
            if(this.supportsInputType(type)) observeTypeCleardown(type, source);
        }

        /**
         * Method to be called into if the resource type being cleared down is of an appropriate type.
         * @param type the StorableResourceType of the resource that was deleted.
         * @param source the ElementsItemStore where the cleardown occurred.
         */
        protected abstract void observeTypeCleardown(StorableResourceType type, ElementsItemStore source);

    }

    /**
     * A base adapter class for implementing IElementsStoredItemObservers based on ElementsStoredResourceObserver
     * implements
     *
     * Implements all the abstract stubs from ElementsStoredResourceObserver and has them throw an "IllegalAccessError"
     * exception.
     *
     * If used as a base class you only then have to implement overrides for the methods that you actually use.
     */
    class ElementsStoredResourceObserverAdapter extends ElementsStoredResourceObserver{

        @SuppressWarnings("WeakerAccess")
        public ElementsStoredResourceObserverAdapter(StorableResourceType... resourceTypes){
            super(resourceTypes);
        }

        //addition overrides
        @Override
        protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItemInfo item){
            throw new IllegalAccessError("unexpected use of observeStoredObject");
        }
        @Override
        protected void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItemInfo item){
            throw new IllegalAccessError("unexpected use of observeStoredRelationship");
        }
        @Override
        protected void observeStoredGroup(ElementsGroupInfo info, ElementsStoredItemInfo item){
            throw new IllegalAccessError("unexpected use of observeStoredGroup");
        }

        //deletion overrides
        @Override
        protected void observeObjectDeletion(ElementsItemId.ObjectId objectId, StorableResourceType type){
            throw new IllegalAccessError("unexpected use of observeObjectDeletion");
        }
        @Override
        protected void observeRelationshipDeletion(ElementsItemId.RelationshipId relationshipId, StorableResourceType type) {
            throw new IllegalAccessError("unexpected use of observeRelationshipDeletion");
        }

        @Override
        protected void observeTypeCleardown(StorableResourceType type, ElementsItemStore source){
            throw new IllegalAccessError("unexpected use of observeTypeCleardown");
        }
    }
}








