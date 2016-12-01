/**
 * Created by ajpc2_000 on 02/08/2016.
 */
package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.vivoweb.harvester.model.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface IElementsStoredItemObserver{
    void observe(ElementsStoredItem item);
    void observeDeletion(ElementsItemId itemId, StorableResourceType type);
    void observeCleardown(StorableResourceType type, ElementsItemStore source);

    abstract class ElementsStoredResourceObserver implements IElementsStoredItemObserver {

        private static final Logger log = LoggerFactory.getLogger(ElementsStoredResourceObserver.class);

        protected final Set<StorableResourceType> resourceTypes = new HashSet<StorableResourceType>();

        protected ElementsStoredResourceObserver(StorableResourceType... resourceTypes) {
            if (resourceTypes == null) throw new NullArgumentException("resourceTypes");
            if (resourceTypes.length == 0) throw new IllegalArgumentException("resourceTypes must not be empty");
            this.resourceTypes.addAll(Arrays.asList(resourceTypes));
        }

        protected boolean supportsInputType(StorableResourceType type){
            return this.resourceTypes.contains(type);
        }


        @Override
        final public void observe(ElementsStoredItem item) {
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
        protected abstract void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item);
        protected abstract void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItem item);
        protected abstract void observeStoredGroup(ElementsGroupInfo info, ElementsStoredItem item);

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
        protected abstract void observeObjectDeletion(ElementsItemId.ObjectId objectId, StorableResourceType type);
        protected abstract void observeRelationshipDeletion(ElementsItemId.RelationshipId relationshipId, StorableResourceType type);

        @Override
        final public void observeCleardown(StorableResourceType type, ElementsItemStore source){
            if(this.supportsInputType(type)) observeTypeCleardown(type, source);
        }

        protected abstract void observeTypeCleardown(StorableResourceType type, ElementsItemStore source);

    }

    public class ElementsStoredResourceObserverAdapter extends ElementsStoredResourceObserver{

        public ElementsStoredResourceObserverAdapter(StorableResourceType... resourceTypes){
            super(resourceTypes);
        }

        //addition overrides
        @Override
        protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item){
            throw new IllegalAccessError("unexpected use of observeStoredObject");
        }
        @Override
        protected void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItem item){
            throw new IllegalAccessError("unexpected use of observeStoredRelationship");
        }
        @Override
        protected void observeStoredGroup(ElementsGroupInfo info, ElementsStoredItem item){
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








