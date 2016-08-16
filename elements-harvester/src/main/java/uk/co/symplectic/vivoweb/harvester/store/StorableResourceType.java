package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemInfo;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemType;

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

    final public static StorableResourceType RAW_OBJECT = new StorableResourceType(ElementsItemType.OBJECT, "raw", "xml");
    final public static StorableResourceType RAW_USER_PHOTO = new StorableResourceType(ElementsItemType.OBJECT, "photo", null);
    final public static StorableResourceType RAW_RELATIONSHIP = new StorableResourceType(ElementsItemType.RELATIONSHIP, "raw", "xml");

    final public static StorableResourceType TRANSLATED_OBJECT = new StorableResourceType(ElementsItemType.OBJECT, "translated", "rdf");
    final public static StorableResourceType TRANSLATED_USER_PHOTO_DESCRIPTION = new StorableResourceType(ElementsItemType.OBJECT, "photo", "rdf");
    final public static StorableResourceType TRANSLATED_RELATIONSHIP = new StorableResourceType(ElementsItemType.RELATIONSHIP, "translated", "rdf");

    //Item part of class to define structure
    private final ElementsItemType keyItemType;
    private final String name;
    private final String fileExtension;

    public ElementsItemType getKeyItemType() {
        return keyItemType;
    }

    public String getName() {
        return name;
    }

    //TODO: file extensions are complicated by the fact that raw photo data may be a variety of mime types - this is not handled cleanly at present.
    public String getFileExtension() {
        return fileExtension;
    }

    private StorableResourceType(ElementsItemType keyItemType, String name, String fileExtension) {
        if (keyItemType == null) throw new NullArgumentException("keyItemType");
        if (StringUtils.isEmpty(name) || StringUtils.isWhitespace(name))
            throw new IllegalArgumentException("argument name must not be null or empty");
        this.keyItemType = keyItemType;
        this.name = name;
        this.fileExtension = StringUtils.trimToNull(fileExtension);

        StorableResourceType.innerGetResourcesForType(this.keyItemType).add(this);
    }

    public boolean isAppropriateForItem(ElementsItemInfo info){
        return keyItemType == info.getItemType();
    }
}







