/**
 * Created by ajpc2_000 on 02/08/2016.
 */
package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsRelationshipInfo;

public interface IElementsStoredItemObserver{
    void observe(ElementsStoredItem item);

    abstract class ElementsStoredResourceObserver implements IElementsStoredItemObserver {
        protected final StorableResourceType resourceType;

        protected ElementsStoredResourceObserver(StorableResourceType resourceType) {
            if (resourceType == null) throw new NullArgumentException("resourceType");
            this.resourceType = resourceType;
        }

        @Override
        final public void observe(ElementsStoredItem item) {
            if (this.resourceType == item.getResourceType()) {
                observeResource(item);
            }
        }
        protected abstract void observeResource(ElementsStoredItem item);
    }

    abstract class ElementsStoredObjectObserver extends ElementsStoredResourceObserver{

        protected ElementsStoredObjectObserver(StorableResourceType resourceType){
            super(resourceType); //this checks input for null
            if(resourceType.getKeyItemType() != ElementsItemType.OBJECT)
                throw new IllegalArgumentException("Invalid resource type for an ElementsStoredObjectObserver");
        }

        @Override
        final protected void observeResource(ElementsStoredItem item){
            if(item.getItemInfo().isObjectInfo()){
                observeStoredObject(item.getItemInfo().asObjectInfo(), item);
            }
        }

        protected abstract void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item);
    }

    abstract class ElementsStoredRelationshipObserver extends ElementsStoredResourceObserver{

        protected ElementsStoredRelationshipObserver(StorableResourceType resourceType){
            super(resourceType); //this checks input for null
            if(resourceType.getKeyItemType() != ElementsItemType.RELATIONSHIP)
                throw new IllegalArgumentException("Invalid resource type for an ElementsStoredRelationshipObserver");
        }

        @Override
        final protected void observeResource(ElementsStoredItem item){
            if(item.getItemInfo().isRelationshipInfo()){
                observeStoredRelationship(item.getItemInfo().asRelationshipInfo(), item);
            }
        }

        protected abstract void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItem item);
    }
}








