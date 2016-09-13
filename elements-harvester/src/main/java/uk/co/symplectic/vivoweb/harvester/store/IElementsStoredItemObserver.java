/**
 * Created by ajpc2_000 on 02/08/2016.
 */
package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.NullArgumentException;
import uk.co.symplectic.vivoweb.harvester.model.ElementsGroupInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsRelationshipInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface IElementsStoredItemObserver{
    void observe(ElementsStoredItem item);

    abstract class ElementsStoredResourceObserver implements IElementsStoredItemObserver {
        protected final Set<StorableResourceType> resourceTypes = new HashSet<StorableResourceType>();

        protected ElementsStoredResourceObserver(StorableResourceType... resourceTypes) {
            if (resourceTypes == null) throw new NullArgumentException("resourceTypes");
            if (resourceTypes.length == 0) throw new IllegalArgumentException("resourceTypes must not be empty");
            this.resourceTypes.addAll(Arrays.asList(resourceTypes));
        }

        @Override
        final public void observe(ElementsStoredItem item) {
            if (this.resourceTypes.contains(item.getResourceType())) {

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

        protected abstract void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item);
        protected abstract void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItem item);
        protected abstract void observeStoredGroup(ElementsGroupInfo info, ElementsStoredItem item);
    }

    public class ElementsStoredResourceObserverAdapter extends ElementsStoredResourceObserver{

        public ElementsStoredResourceObserverAdapter(StorableResourceType... resourceTypes){
            super(resourceTypes);
        }
        @Override
        protected void observeStoredObject(ElementsObjectInfo info, ElementsStoredItem item){
            throw new IllegalAccessError("unexpected use of observeStoredObject");
        }
        protected void observeStoredRelationship(ElementsRelationshipInfo info, ElementsStoredItem item){
            throw new IllegalAccessError("unexpected use of observeStoredRelationship");
        }
        protected void observeStoredGroup(ElementsGroupInfo info, ElementsStoredItem item){
            throw new IllegalAccessError("unexpected use of observeStoredGroup");
        }
    }
}








