/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.elements.api;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import uk.co.symplectic.elements.api.versions.GeneralAPIv4XOr55_URLBuilder;
import uk.co.symplectic.elements.api.versions.GeneralPaginationExtractingFilterFactory;
import uk.co.symplectic.utils.xml.XMLEventProcessor;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elements API Versions know how to construct themselves from a string and know how to create a PaginationExtractingFilter that
 * will extract pagination information from any paged results returned by that version of the API.
 * To avoid concurrent requests to the API tripping over each other they need to return a "new" PaginationExtractingFilter for each request - so a factory is needed
 */
public class ElementsAPIVersion {


    public static class VersionExtractingFilter extends XMLEventProcessor.ItemExtractingFilter<ElementsAPIVersion> {

        ElementsAPIVersion workspace = null;

        public VersionExtractingFilter() {
            super(new XMLEventProcessor.EventFilter.DocumentLocation(new QName(ElementsAPI.atomNS, "feed"), new QName(ElementsAPI.apiNS, "schema-version")));
        }

        @Override
        protected void initialiseItemExtraction(XMLEventProcessor.WrappedXmlEvent initialEvent) throws XMLStreamException {
            workspace = ElementsAPIVersion.parse(initialEvent.getRequiredValue());
        }

        @Override
        protected void processEvent(XMLEventProcessor.WrappedXmlEvent event, List<QName> relativeLocation) throws XMLStreamException {

        }

        @Override
        protected ElementsAPIVersion finaliseItemExtraction(XMLEventProcessor.WrappedXmlEvent finalEvent) {
            return workspace;
        }
    }

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
    public static final ElementsAPIVersion VERSION_4_6    = new ElementsAPIVersion("4.6", GeneralAPIv4XOr55_URLBuilder.get4XBuilder(), allVersionsPaginationFilterFactory);
    public static final ElementsAPIVersion VERSION_4_9    = new ElementsAPIVersion("4.9", GeneralAPIv4XOr55_URLBuilder.get4XBuilder(), allVersionsPaginationFilterFactory);
    //5.5 does not use order-by as it has reliabel continuation tokens in its next page links - may, however need alteration to use affected-when
    // to maintain consistent behaviour with v4.X systems, but this is probably not actually needed.
    public static final ElementsAPIVersion VERSION_5_5    = new ElementsAPIVersion("5.5", GeneralAPIv4XOr55_URLBuilder.get55Builder(false), allVersionsPaginationFilterFactory);

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
     * This particular Version's number (for less than and greater than comparisons)
     */
    private final float versionNumber;

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
     * the versionNumber will be parsed from the name, if this fails a NumberFormatException exception will be thrown.
     * All params are required and must not be null.
     * @param versionName : the name of the APIVersion (e.g. "4.6". "4.9", etc)
     * @param urlBuilder : a url builder suitable for formulating valid HTTP queries for this version of the API from ElementsFeedQuery objects.
     * @param paginationExtractorFactory : a paginationExtractor factory that will generate paginationExtractor objects suitable for pulling
     *                                   out pagination from this version of the API
     */
    private ElementsAPIVersion(String versionName, ElementsAPIURLBuilder urlBuilder, PaginationExtractingFilterFactory paginationExtractorFactory) throws NumberFormatException{
        this(versionName, Float.parseFloat(versionName), urlBuilder, paginationExtractorFactory);
    }

    /**
     * Private constructor to create a version, and place it in the map of known versions keyed by name (for easy retrieval)
     * All params are required and must not be null.
     * @param versionName : the name of the APIVersion (e.g. "4.6". "4.9", etc)
     * @param versionNumber : the floating point "number" associated with the version (for less than and greater than comparisons..
     * @param urlBuilder : a url builder suitable for formulating valid HTTP queries for this version of the API from ElementsFeedQuery objects.
     * @param paginationExtractorFactory : a paginationExtractor factory that will generate paginationExtractor objects suitable for pulling
     *                                   out pagination from this version of the API
     */
    private ElementsAPIVersion(String versionName, float versionNumber, ElementsAPIURLBuilder urlBuilder, PaginationExtractingFilterFactory paginationExtractorFactory){
        if(StringUtils.isBlank(versionName)) throw new IllegalArgumentException("versionName must not be null or empty");
        if(urlBuilder == null) throw new NullArgumentException("urlBuilder");
        if(paginationExtractorFactory == null) throw new NullArgumentException("paginationExtractor");

        this.versionName = versionName;
        this.versionNumber = versionNumber;
        this.urlBuilder = urlBuilder;
        this.paginationExtractorFactory = paginationExtractorFactory;

        versionMap.put(this.versionName, this);
    }

    @Override
    public String toString(){return getVersionName();}

    public boolean greaterThan(ElementsAPIVersion versionToCompare){
        if(versionToCompare == null) throw new NullArgumentException("versionToCompare");
        return(this.versionNumber > versionToCompare.versionNumber);
    }

    public boolean greaterThanOrEqualTo(ElementsAPIVersion versionToCompare){
        if(versionToCompare == null) throw new NullArgumentException("versionToCompare");
        return(this.versionNumber >= versionToCompare.versionNumber);
    }

    public boolean lessThan(ElementsAPIVersion versionToCompare){
        if(versionToCompare == null) throw new NullArgumentException("versionToCompare");
        return(this.versionNumber < versionToCompare.versionNumber);
    }

    public boolean lessThanOrEqualTo(ElementsAPIVersion versionToCompare){
        if(versionToCompare == null) throw new NullArgumentException("versionToCompare");
        return(this.versionNumber <= versionToCompare.versionNumber);
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
