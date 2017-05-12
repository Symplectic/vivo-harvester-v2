package uk.co.symplectic.elements.api;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.elements.api.versions.ElementsAPIv4_XURLBuilder;
import uk.co.symplectic.elements.api.versions.GeneralPaginationExtractingFilterFactory;
import uk.co.symplectic.utils.xml.XMLEventProcessor;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Elements API Versions know how to construct themselves from a string and know how to create a PaginationExtractingFilter that
 * will extract pagination information from any paged results returned by that version of the API.
 * To avoid concurrent requests to the API tripping over each other they need to return a "new" PaginationExtractingFilter for each request - so a factory is needed
 */
public class ElementsAPIVersion {

    /**
     * general representation of a PaginationExtractingFilter
     * concrete implementations generally found in "versions" folder
     */
    static abstract public class PaginationExtractingFilter extends XMLEventProcessor.ItemExtractingFilter<ElementsFeedPagination> {
        protected PaginationExtractingFilter(DocumentLocation location){
            super(location);
        }
    }

    /**
     * general representation of a PaginationExtractingFilterFactory as needed to construct an ElementsAPIVersion object
     * concrete implementations generally found in "versions" folder
     */
    static abstract public class PaginationExtractingFilterFactory{
        public abstract PaginationExtractingFilter createPaginationExtractor();
    }

    /**
     * static version of the filter currently used to construct all versions - this is just an optimisation
     */
    private static PaginationExtractingFilterFactory allVersionsPaginationFilterFactory = new GeneralPaginationExtractingFilterFactory();

    /**
     * Static storage to hold all possible instances of ElementsAPIVersion that are known.
     */
    private static final Map<String, ElementsAPIVersion> versionMap = new HashMap<String, ElementsAPIVersion>();

    /**
     * Instantiation of all known ElementsAPIVersion objects - one per version
     */
    public static final ElementsAPIVersion VERSION_4_6    = new ElementsAPIVersion("4.6", new ElementsAPIv4_XURLBuilder(), allVersionsPaginationFilterFactory);
    public static final ElementsAPIVersion VERSION_4_9    = new ElementsAPIVersion("4.9", new ElementsAPIv4_XURLBuilder(), allVersionsPaginationFilterFactory);
    //public static final ElementsAPIVersion VERSION_5_5    = new ElementsAPIVersion("5.5", new ElementsAPIv4_XURLBuilder(), allVersionsPaginationFilterFactory);

    /**
     * Utility method to return an Array containing all the Versions that are known
     */
    public static ElementsAPIVersion[] allVersions(){
        Collection<ElementsAPIVersion> versions = versionMap.values();
        return versions.toArray(new ElementsAPIVersion[versions.size()]);
    }

    /**
     * This particular Version's name
     */
    private final String versionName;

    /**
     * How this versions builds URL's based on a query
     */
    private final ElementsAPIURLBuilder urlBuilder;

    /**
     * A factory to generate a PaginationExtractingFilter that knows how to read pagination information from a feed returned by this version of the API
     */
    private final PaginationExtractingFilterFactory paginationExtractorFactory;

    String getVersionName() { return versionName; }
    ElementsAPIURLBuilder getUrlBuilder() { return urlBuilder; }
    PaginationExtractingFilter getPaginationExtractor() { return paginationExtractorFactory.createPaginationExtractor(); }


    /**
     * Private constructor to create a version, and place it in the map of known versions keyed by name (for easy retrieval)
     * All params are required and must not be null.
     * @param versionName : the name of the APIVersion (e.g. "4.6". "4.9", etc)
     * @param urlBuilder : a url builder suitable for formulating valid HTTP queries for this version of the API from ElementsFeedQuery objects.
     * @param paginationExtractorFactory : a paginationExtractor factory that will generate paginationExtractor objects suitable for pulling
     *                                   out pagination from this version of the API
     */
    private ElementsAPIVersion(String versionName, ElementsAPIURLBuilder urlBuilder, PaginationExtractingFilterFactory paginationExtractorFactory){
        if(StringUtils.isBlank(versionName)) throw new IllegalArgumentException("versionName must not be null or empty");
        if(urlBuilder == null) throw new NullArgumentException("urlBuilder");
        if(paginationExtractorFactory == null) throw new NullArgumentException("paginationExtractor");

        this.versionName = versionName;
        this.urlBuilder = urlBuilder;
        this.paginationExtractorFactory = paginationExtractorFactory;

        versionMap.put(this.versionName, this);
    }

    /**
     * Method to return an ElementsAPIVersion from the known list based on the passed in string
     * Throws an IllegalStateException if the string does not correspond to a known version.
     * @param value : a string describing the APIVersion (e,g "version4.9" or "v4.9" or "4.9")
     * @return : an ElementsAPIVersion matching the passed in descriptor (IllegalStateException thrown if can't be resolved)
     */
    public static ElementsAPIVersion parse(String value) {
        if(value != null) {
            String lookupValue = value.toLowerCase();
            if (lookupValue.startsWith("version")) {
                lookupValue = lookupValue.substring(7);
            } else if (lookupValue.startsWith("v")) {
                lookupValue = lookupValue.substring(1);
            }
            //strip off whitespace and and .
            lookupValue = StringUtils.stripStart(lookupValue, " .");
            lookupValue = StringUtils.stripStart(lookupValue, null);

            if (versionMap.containsKey(lookupValue)) {
                return versionMap.get(lookupValue);
            }
        }
        throw new IllegalStateException(MessageFormat.format("String \"{0}\" could not be parsed to a supported Elements API version", value == null ? "" : value));
    }
}
