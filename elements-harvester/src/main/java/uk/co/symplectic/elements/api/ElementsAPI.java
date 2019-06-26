/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.elements.api;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.utils.http.HttpClient;
import uk.co.symplectic.utils.http.ValidatedUrl;
import uk.co.symplectic.utils.xml.StAXUtils;
import uk.co.symplectic.utils.xml.XMLEventProcessor;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Main Elements API Client class.
 * represents an Elements API and exposes the ability to execute queries or fetch resources.
 * Contains a few static classes that represent how queries should be processed.
 */
@SuppressWarnings("WeakerAccess")
public class ElementsAPI {

    /**
     * Small immutable class to represent options relating to how an API query should be processed
     */
    public static class ProcessingOptions{
        private final boolean processAllPages;
        private final int perPage;
        //private final Integer page;

        /**
         * Constructor to specify this set of options
         * @param processAllPages whether queries should make multiple request to the Elements API to retrieve all
         *                        available data relating to the query, or should just return the first page of results.
         * @param perPage how many results should be returned on each page by the API.
         */
        public ProcessingOptions(boolean processAllPages, int perPage) {
            this.processAllPages = processAllPages;
            this.perPage = perPage;
        }

        /**
         * Number of records per page in the feed
         * @return An integer, 0 or below uses the feed default
         */
        public int getPerPage() {
            return perPage;
        }

        /**
         * For a query that goes over multiple pages, should all the pages be processed
         * @return true to process all pages, false to only process the first
         */
        public boolean getProcessAllPages() {
            return processAllPages;
        }
    }

    /**
     * Immutable class to represent the "default" processing options that should be used if none are explicitly provided
     * when a query is executed by the ElementsAPI class.
     *
     * Note that different defaults can exist for different types of query.
     */
    public static class ProcessingDefaults{

        /**
         * The "Default" set of ProcessingDefaults - used by the ElementsAPI class if no defaults are provided on construction.
         * fetch all pages of queries, fetch 25 results per page for "full" detail queries, fetch 100 results per page for
         * "ref" detail queries.
         */
        private static ProcessingDefaults DEFAULTS = new ProcessingDefaults(true, 25, 100);

        private final ProcessingOptions fullDetailOptions;
        private final ProcessingOptions refDetailOptions;

        /**
         * Constructor to set up processing defaults
         * @param processAllPages to indicate whether all the pages be processed
         * @param perPageFull An integer > 0, the amount to fetch per-page for full detail queries
         * @param perPageRef An integer > 0, the amount to fetch per-page for ref detail queries
         */
        public ProcessingDefaults(boolean processAllPages, int perPageFull, int perPageRef) {
            this.fullDetailOptions = new ProcessingOptions(processAllPages, perPageFull);
            this.refDetailOptions = new ProcessingOptions(processAllPages, perPageRef);
        }

        /**
         * Method to get hold of the ProcessingOptions that should be used based on the current query.
         * If there are no "override options the appropriate defaults will be returned.
         * If there are override options, then the override options will be returned.
         * @param query the query being run
         * @param overrideOptions any "override" options that have been supplied
         *                        appropriate defaults will be returned if this is null
         * @return ProcessingOptions : the options actually in use
         */
        ProcessingOptions getProcessingOptions(ElementsFeedQuery query, ProcessingOptions overrideOptions){
            if(overrideOptions != null) return overrideOptions;
            //otherwise
            return query.getFullDetails() ? fullDetailOptions : refDetailOptions;
        }
    }


    //Useful API Namespaces
    /**
      The namespace of the Elements API native XML
     */
    public static final String apiNS = "http://www.symplectic.co.uk/publications/api";

    /**
     The namespace of the atom document that is interspersed with the Elements API xml in API responses.
     */
    public static final String atomNS = "http://www.w3.org/2005/Atom";

    //timing utility functions and fields- very basic - not very realistic results.
    private static long timeSpentInNetwork = 0;
    private static long timeSpentInProcessing = 0;
    private static void resetTimers(){
        timeSpentInNetwork = 0;
        timeSpentInProcessing = 0;
    }

    /**
     * The APIResponseFilter class represents a Filter that will be used to parse the xml documents retrieved from the
     * Elements API when a query is being executed. It is a simple wrapper for an XMLEventProcessor.EventFilter
     * that supports the concept of being valid only for certain APIVersions.
     */
    public static class APIResponseFilter{
        private final XMLEventProcessor.EventFilter filter;
        private final List<ElementsAPIVersion> supportedVersions = new ArrayList<ElementsAPIVersion>();

        public APIResponseFilter(XMLEventProcessor.EventFilter filter,ElementsAPIVersion... supportedVersions){
            if(filter == null) throw new NullArgumentException("filter");
            this.filter = filter;

            for(ElementsAPIVersion version : supportedVersions){
                if(version != null) this.supportedVersions.add(version);
            }
            if(this.supportedVersions.size() == 0) throw new IllegalArgumentException("Must supply at least one supported ElementsAPIVersion");
        }

        boolean supports(ElementsAPIVersion version){
            return supportedVersions.contains(version);
        }

        XMLEventProcessor.EventFilter getEventFilter() { return filter; }
    }

    /**
     * SLF4J Logger
     */
    private static final Logger log = LoggerFactory.getLogger(ElementsAPI.class);

    private final ElementsAPIVersion version;

    private final String url;
    private final String username;
    private final String password;

    //TODO: move to processing defaults?
    private final boolean rewriteMismatchedURLs;
    @SuppressWarnings("FieldCanBeLocal")
    private int maxRetries = 5;
    @SuppressWarnings("FieldCanBeLocal")
    private int retryDelayMillis = 500;

    private final ProcessingDefaults defaults;

    private boolean issuedWarningAboutMismatchedFetchUrls = false;

    /**
     * Chained constructor - to ease creation of a default ElementsAPI
     * @param version the version of the API being contacted, can legitimately be null - version will be extracted
     * @param url the base url of the API being contacted
     */
    public ElementsAPI(ElementsAPIVersion version, String url){
        this(version, url, null, null, false, null);
    }

    /**
     * Main constructor for the ElementsAPI class
     * @param version the version of the API being contacted - can legitimately be null - version will be extracted
     * @param url the base url of the API being contacted
     * @param username user credentials for the API being contacted
     * @param password user credentials for the API being contacted
     * @param rewriteMismatchedURLs whether the
     * @param defaults the ProcessingDefaults to be used
     *                 the "default" ProcessingDefaults of processing all pages at 25 and 100 items per page for full and
     *                 ref detail queries respectively will be used if @defaults is null.
     */
    public ElementsAPI(ElementsAPIVersion version, String url, String username, String password, boolean rewriteMismatchedURLs, ProcessingDefaults defaults) {

        ValidatedUrl validatedUrl = getValidatedUrl(url, new MessageFormat("Provided api base URL was invalid: {0}"));
        this.url = StringUtils.stripEnd(url, "/") + "/";

        if(validatedUrl.isSecure()) {
            if(StringUtils.isBlank(username) || StringUtils.isBlank(password)){
                String errorMsg = "Must supply username and password when connecting to a secure api endpoint";
                log.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
            this.username = username;
            this.password = password;
        }
        else {
            if (StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password)) {
                String warnMsg = MessageFormat.format("Provided API credentials{0} ignored as the API url ({1}) is not secure", (username == null ? "" : "(" + username + ")"), url);
                log.warn(warnMsg);
            }
            this.username = null;
            this.password = null;
        }

        this.rewriteMismatchedURLs = rewriteMismatchedURLs;
        this.defaults = defaults == null ? ProcessingDefaults.DEFAULTS : defaults;

        ElementsAPIVersion extractedVersion = tryToExtractVersion();

        if(extractedVersion == null){
            String errorMessage = "Could not extract a valid API version from the Elements API's \"my-account\" resource on construction";
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        if(version != null && !version.equals(extractedVersion)){
            String errorMessage = MessageFormat.format("provided version ({0}) must match version reported by API ({1})", version, extractedVersion);
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        this.version = extractedVersion;

    }

    public ElementsAPIVersion getVersion(){ return this.version; }

    //Should call at end of construction to ensure client is as set up as can be without the version.
    private ElementsAPIVersion tryToExtractVersion(){
        ElementsAPIVersion.VersionExtractingFilter filter = new ElementsAPIVersion.VersionExtractingFilter();
        List<XMLEventProcessor.EventFilter> filters = new ArrayList<XMLEventProcessor.EventFilter>();
        filters.add(filter);
        executeInternalQuery(getValidatedUrl(this.url + "my-account", new MessageFormat("Constructed my-account URL was invalid: {0}")), filters);
        return filter.getExtractedItem();
    }

    private ValidatedUrl getValidatedUrl(String urlString, MessageFormat failureMessageTemplate){
        return getValidatedUrl(urlString, null, failureMessageTemplate);
    }

    private ValidatedUrl getValidatedUrl(String urlString, String comparisonUrlString, MessageFormat failureMessageTemplate){
        try{
            return new ValidatedUrl(urlString, comparisonUrlString);
        }
        catch(URISyntaxException e){
            String errorMsg = failureMessageTemplate.format(e.getMessage());
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * Method to construct an XMLEventFilter that counts every "atom entry" that passes through it.
     * @return a NEW ItemCountingFilter
     */
    private XMLEventProcessor.ItemCountingFilter getEntryCounter(){
        XMLEventProcessor.EventFilter.DocumentLocation entryLocation = new XMLEventProcessor.EventFilter.DocumentLocation(
            new QName(atomNS, "feed"), new QName(atomNS, "entry")
        );
        return new XMLEventProcessor.ItemCountingFilter(entryLocation);
    }

    /**
     * Method to execute the requested feedQuery and parse the resulting XML responses using the specified filters.
     * The query will be run using the appropriate ProcessingDefaults.
     * @param feedQuery an ElementsFeedQuery to be run against the Elements API
     * @param filters a set of APIResponseFilters that will be used to parse the XML responses from the API.
     */
    public void executeQuery(ElementsFeedQuery feedQuery, APIResponseFilter... filters) {
        executeQuery(feedQuery, null, filters);
    }

    /**
     * Method to execute the requested feedQuery and parse the resulting XML responses using the specified filters.
     * @param feedQuery an ElementsFeedQuery to be run against the Elements API
     * @param overrideOptions the specific options that should be used to run the query.
     *                        the appropriate ProcessingDefaults will be used if @overrideOptions is null.
     * @param filters a set of APIResponseFilters that will be used to parse the XML responses from the API.
     */

    public void executeQuery(ElementsFeedQuery feedQuery, ProcessingOptions overrideOptions, APIResponseFilter... filters) {
        List<XMLEventProcessor.EventFilter> eventFilters = new ArrayList<XMLEventProcessor.EventFilter>();
        for(APIResponseFilter filter : filters){
            if(!filter.supports(version)){
                String message = MessageFormat.format("Filter {0} does not support API ElementsAPIVersion {1}", filter.getClass().getName(), version.getVersionName());
                throw new IllegalStateException(message);
            }
            eventFilters.add(filter.getEventFilter());
        }

        //get and add int the entry counter to work out how many items we have processed
        XMLEventProcessor.ItemCountingFilter itemCounter = getEntryCounter();
        eventFilters.add(itemCounter);

        ProcessingOptions processingOptions = defaults.getProcessingOptions(feedQuery, overrideOptions);
        ElementsFeedQuery.QueryIterator iterator = feedQuery.getQueryIterator(url, version.getUrlBuilder(), processingOptions);
        ElementsFeedPagination pagination = null;
        ValidatedUrl previousQuery = null;
        int queryCounter = 0;
        while(iterator.hasNext(pagination)){
            String previousUrl = previousQuery == null ? null : previousQuery.getUrl();
            ValidatedUrl currentQuery = getValidatedUrl(iterator.next(pagination), previousUrl, new MessageFormat("Invalid API query detected : {0}"));
            if(previousQuery != null) {
                if (currentQuery.isMismatched()) {
                    if (queryCounter == 1) {
                        log.warn(MessageFormat.format("Next URL in a feed \"{0}\" has a different host to the previous URL: {1}", currentQuery.getUrl(), previousQuery.getUrl()));
                        log.warn("There is probably a mismatch between the configured API URL in this program and the API baseURI configured in Elements");
                    }
                    //if we want to rewrite any mismatched urls to use the original base url from our query
                    if(rewriteMismatchedURLs) currentQuery.useRewrittenVersion(true);
                }
                if (currentQuery.getUrl().equals(previousQuery.getUrl())) {
                    throw new IllegalStateException("Error detected in the pagination response from Elements - unable to continue processing. Note that this can often indicate a corrupt or missing Search Index in Elements");
                }
            }
            pagination = executeInternalQuery(currentQuery, eventFilters);

            queryCounter++;
            if (queryCounter % 40 == 0) {
                log.trace(MessageFormat.format("{0} queries processed: network-time: {1}, processing-time: {2}", queryCounter, ElementsAPI.timeSpentInNetwork, ElementsAPI.timeSpentInProcessing));
                ElementsAPI.resetTimers();
            }

            previousQuery = currentQuery;
        }
        log.info(MessageFormat.format("Query completed {0} items processed in total", itemCounter.getItemCount()));
    }


    /**
     * Method to fetch a specific resource from the Elements API and store it in the Output stream provided
     * @param resourceURL the url of the resource to be fetched.
     * @param outputStream the output stream to be populated with the fetched data.
     * @return boolean indicating if the fetch was successful (actually will return true or will error out..)
     */
    //TODO : rationalise with main query call to have common usage of the underlying client with nice retry behaviour etc.
    @SuppressWarnings("SameReturnValue")
    public boolean fetchResource(String resourceURL, OutputStream outputStream) {
        HttpClient.ApiResponse apiResponse = null;
        try {
            ValidatedUrl validatedUrl = new ValidatedUrl(resourceURL, url);
            if(validatedUrl.isMismatched()){
                if(!issuedWarningAboutMismatchedFetchUrls) {
                    log.warn(MessageFormat.format("Requested fetch URL \"{0}\" has a different host to the configured base URL: {1}", resourceURL, url));
                    log.warn("There is probably a mismatch between the configured API URL in this program and the API baseURI configured in Elements");
                    issuedWarningAboutMismatchedFetchUrls = true;
                }
                if(rewriteMismatchedURLs) validatedUrl.useRewrittenVersion(true);
            }
            HttpClient apiClient = new HttpClient(validatedUrl, username, password);
            apiResponse = apiClient.executeGetRequest();
            IOUtils.copy(apiResponse.getResponseStream(), outputStream);
        }
        catch (IOException ignored) { }
        catch (URISyntaxException ignored){ }
        finally {
            if (apiResponse != null) {
                try {
                    apiResponse.dispose();
                }
                catch (IOException ignored) { }
            }
        }
        return true;
    }

    /**
     * Internal helper method to perform the processing of a particular URL as part of executing a query
     * @param url the url to be processed.
     * @param eventFilters the filters to be run against the returned XML.
     * @return an ElementsFeedPagination object representing the position of the current URL in a query of multiple pages.
     * @throws IllegalStateException if errors
     */
    private ElementsFeedPagination executeInternalQuery(ValidatedUrl url, Collection<XMLEventProcessor.EventFilter> eventFilters) throws IllegalStateException {
        int retryCount = 0;
        do {
            HttpClient.ApiResponse apiResponse = null;
            IOException responseDisposeError = null;
            try {
                long startTime = System.currentTimeMillis();
                HttpClient apiClient = new HttpClient(url, username, password);
                apiResponse = apiClient.executeGetRequest();
                long endTime = System.currentTimeMillis();
                timeSpentInNetwork += (endTime - startTime);
                return parseEventResponse(apiResponse.getResponseStream(), eventFilters);
            }
            catch (IOException e) {
                if(e instanceof HttpClient.InvalidResponseException){
                    int statusCode = ((HttpClient.InvalidResponseException) e).getResponseCode();
                    //if forbidden then just jump out here..
                    if(statusCode == HttpStatus.SC_FORBIDDEN || statusCode == HttpStatus.SC_UNAUTHORIZED) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                    log.error(e.getMessage(), e);
                }
                else log.error("IO Error handling API request", e);

                if (++retryCount >= maxRetries) {
                    throw new IllegalStateException("IO Error handling API request", e);
                }
            } catch (XMLStreamException e) {
                log.error("XML Stream Error handling API request", e);
                if (++retryCount >= maxRetries) {
                    throw new IllegalStateException("XML Stream Error handling API request", e);
                }
            } finally {
                if (apiResponse != null) {
                    try {
                        apiResponse.dispose();
                    } catch (IOException e) {
                        responseDisposeError = e;
                    }
                }
            }

            if(responseDisposeError != null){
                throw new IllegalStateException("IOException attempting to dispose apiResponse", responseDisposeError);
            }

            try {
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted whilst retrying query");
            }

        } while (true);
    }

    /**
     * helper method to process the provided API XML response (represented by the inputStream) using the supplied filters.
     * Note, eventFilters does not need to contain a filter to extract pagination info as one is automatically added
     * by this method and the result is returned in the return parameter.
     * @param response the API XML response being processed.
     * @param eventFilters the filters to be run against the XML.
     * @return an ElementsFeedPagination object representing the position of the current URL in a query of multiple pages.
     * @throws XMLStreamException if XML structure is invalid.
     */
    private ElementsFeedPagination parseEventResponse(InputStream response, Collection<XMLEventProcessor.EventFilter> eventFilters) throws XMLStreamException {
        final long startTime = System.currentTimeMillis();
        //set up the xml reader
        XMLInputFactory xmlInputFactory = StAXUtils.getXMLInputFactory();
        XMLEventReader atomReader = xmlInputFactory.createXMLEventReader(response);

        XMLEventProcessor processor = new XMLEventProcessor(eventFilters.toArray(new XMLEventProcessor.EventFilter[eventFilters.size()]));
        ElementsAPIVersion.PaginationExtractingFilter paginationFilter = null;
        if(version != null) {
            paginationFilter = version.getPaginationExtractor();
            processor.addFilter(paginationFilter);
        }
        processor.process(atomReader);
        final long endTime = System.currentTimeMillis();
        timeSpentInProcessing += (endTime - startTime);
        return paginationFilter == null ? null : paginationFilter.getExtractedItem();
    }
}
