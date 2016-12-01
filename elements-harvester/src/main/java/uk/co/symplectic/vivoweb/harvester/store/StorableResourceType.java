package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;

import java.text.MessageFormat;
import java.util.*;

/**
 * Created by ajpc2_000 on 08/08/2016.
 */
public class StorableResourceType {

    //Static part of class - for storage
    private static Map<ElementsItemType, List<StorableResourceType>> availableResources = new HashMap<ElementsItemType, List<StorableResourceType>>();

    public static List<StorableResourceType> getResourcesForType(ElementsItemType type) {
        return Collections.unmodifiableList(innerGetResourcesForType(type));
    }

    private static List<StorableResourceType> innerGetResourcesForType(ElementsItemType type) {
        if (type == null) throw new NullArgumentException("type");
        if (!availableResources.containsKey(type)) {
            availableResources.put(type, new ArrayList<StorableResourceType>());
        }
        return availableResources.get(type);
    }

    final public static StorableResourceType RAW_OBJECT = new StorableResourceType(ElementsItemType.OBJECT, "raw", "xml", true);
	final public static StorableResourceType RAW_USER_PHOTO = new UserRelatedResourceType("photo", null, false);
    final public static StorableResourceType RAW_RELATIONSHIP = new StorableResourceType(ElementsItemType.RELATIONSHIP, "raw", "xml", true);
    final public static StorableResourceType RAW_GROUP = new StorableResourceType(ElementsItemType.GROUP, "raw", "xml", true);

    final public static StorableResourceType TRANSLATED_OBJECT = new StorableResourceType(ElementsItemType.OBJECT, "translated", "rdf", true);
    final public static StorableResourceType TRANSLATED_USER_PHOTO_DESCRIPTION = new UserRelatedResourceType("photo", "rdf", true);
    final public static StorableResourceType TRANSLATED_RELATIONSHIP = new StorableResourceType(ElementsItemType.RELATIONSHIP, "translated", "rdf", true);
    final public static StorableResourceType TRANSLATED_GROUP = new StorableResourceType(ElementsItemType.GROUP, "translated", "rdf", true);

    //Item part of class to define structure
    private final ElementsItemType keyItemType;
    private final String name;
    private final String fileExtension;
    private final boolean shouldZip;

    public ElementsItemType getKeyItemType() {
        return keyItemType;
    }

    public String getName() {
        return name;
    }

    //TODO: file extensions are complicated by the fact that raw photo data may be a variety of mime types - so this is not implemented at the moment.
    public String getFileExtension() {
        return fileExtension;
    }

    @Override
    public String toString(){
        return MessageFormat.format("{0}-{1}", getName(), keyItemType.getName());
    }

    private StorableResourceType(ElementsItemType keyItemType, String name, String fileExtension, boolean shouldZip) {
        if (keyItemType == null) throw new NullArgumentException("keyItemType");
        if (StringUtils.isEmpty(name) || StringUtils.isWhitespace(name))
            throw new IllegalArgumentException("argument name must not be null or empty");
        this.keyItemType = keyItemType;
        this.name = name;
        this.fileExtension = StringUtils.trimToNull(fileExtension);
        this.shouldZip = shouldZip;

        StorableResourceType.innerGetResourcesForType(this.keyItemType).add(this);
    }

    public boolean isAppropriateForItem(ElementsItemId id){
        return keyItemType == id.getItemType();
    }

    public boolean shouldZip() { return shouldZip; }

    private static class UserRelatedResourceType extends StorableResourceType {
        UserRelatedResourceType(String name, String fileExtension, boolean shouldZip) {
            super(ElementsItemType.OBJECT, name, fileExtension, shouldZip);
        }

        @Override
        public boolean isAppropriateForItem(ElementsItemId id) {
            //return super.isAppropriateForItem(id) && id instanceof ElementsItemId.ObjectId && ((ElementsItemId.ObjectId) id).getCategory() == ElementsObjectCategory.USER;
            //must be an object id if is appropriate for object item type...
            return super.isAppropriateForItem(id) && id.getItemSubType() == ElementsObjectCategory.USER;
        }

        @Override
        public String toString(){
            return MessageFormat.format("{0}-{1}", getName(), "user");
        }
    }

}







