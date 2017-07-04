/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
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
 * Main Elements API Client class
 */
public class ElementsAPI {

    public static class ProcessingOptions{
        private final boolean processAllPages;
        private final int perPage;
        //private final Integer page;

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

    public static class ProcessingDefaults{

        public static ProcessingDefaults DEFAULTS = new ProcessingDefaults(true, 25, 100);

        private final ProcessingOptions fullDetailOptions;
        private final ProcessingOptions refDetailOptions;

        /**
         * @param processAllPages to indicate whether all the pages be processed
         * @param perPageFull An integer > 0, the amount to fetch per-page for full detail queries
         * @param perPageRef An integer > 0, the amount to fetch per-page for ref detail queries
         */
        public ProcessingDefaults(boolean processAllPages, int perPageFull, int perPageRef) {
            this.fullDetailOptions = new ProcessingOptions(processAllPages, perPageFull);
            this.refDetailOptions = new ProcessingOptions(processAllPages, perPageRef);
        }

        public ProcessingOptions getProcessingOptions(ElementsFeedQuery query, ProcessingOptions overrideOptions){
            if(overrideOptions != null) return overrideOptions;
            //otherwise
            return query.getFullDetails() ? fullDetailOptions : refDetailOptions;
        }
    }



    //Useful API Namespaces -
    public static final String apiNS = "http://www.symplectic.co.uk/publications/api";
    public static final String atomNS = "http://www.w3.org/2005/Atom";

    private static long timeSpentInNetwork = 0;
    private static long timeSpentInProcessing = 0;
    private static void resetTimers(){
        timeSpentInNetwork = 0;
        timeSpentInProcessing = 0;
    }

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

        public boolean supports(ElementsAPIVersion version){
            return supportedVersions.contains(version);
        }

        public XMLEventProcessor.EventFilter getEventFilter() { return filter; }
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
    private int maxRetries = 5;
    private int retryDelayMillis = 500;

    private final ProcessingDefaults defaults;

    private boolean issuedWarningAboutMismatchedFetchUrls = false;

    public ElementsAPI(ElementsAPIVersion version, String url){
        this(version, url, null, null, false, null);
    }

    public ElementsAPI(ElementsAPIVersion version, String url, String username, String password, boolean rewriteMismatchedURLs, ProcessingDefaults defaults) {
        if(version == null){
            log.error("provided version must not be null on construction");
            throw new NullArgumentException("version");
        }
        this.version = version;

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

    private XMLEventProcessor.ItemCountingFilter getEntryCounter(){
        XMLEventProcessor.EventFilter.DocumentLocation entryLocation = new XMLEventProcessor.EventFilter.DocumentLocation(
            new QName(atomNS, "feed"), new QName(atomNS, "entry")
        );
        return new XMLEventProcessor.ItemCountingFilter(entryLocation);
    }

    public void executeQuery(ElementsFeedQuery feedQuery, APIResponseFilter... filters) {
        executeQuery(feedQuery, null, filters);
    }

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

            //todo: make traced logs end up in a sensible place.
            queryCounter++;
            if (queryCounter % 40 == 0) {
                log.trace(MessageFormat.format("{0} queries processed: network-time: {1}, processing-time: {2}", queryCounter, ElementsAPI.timeSpentInNetwork, ElementsAPI.timeSpentInProcessing));
                ElementsAPI.resetTimers();
            }

            previousQuery = currentQuery;
        }
        log.trace(MessageFormat.format("Query completed {0} items processed in total", itemCounter.getItemCount()));
    }

    //TODO : rationalise with main query call to have common usage of the underlying client with nice retry behaviour etc.
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
        catch (IOException e) { }
        catch (URISyntaxException e2){ }
        finally {
            if (apiResponse != null) {
                try {
                    apiResponse.dispose();
                } catch (IOException e) {

                }
            }
        }
        return true;
    }

    private ElementsFeedPagination executeInternalQuery(ValidatedUrl url, Collection<XMLEventProcessor.EventFilter> eventFilters) throws IllegalStateException {
        int retryCount = 0;
        do {
            HttpClient.ApiResponse apiResponse = null;
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
                        throw new IllegalStateException("IOException attempting to dispose apiResponse", e);
                    }
                }
            }

            try {
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted whilst retrying query");
            }

        } while (true);
    }

    private ElementsFeedPagination parseEventResponse(InputStream response, Collection<XMLEventProcessor.EventFilter> eventFilters) throws XMLStreamException {
        final long startTime = System.currentTimeMillis();
        //set up the xml reader
        XMLInputFactory xmlInputFactory = StAXUtils.getXMLInputFactory();
        XMLEventReader atomReader = xmlInputFactory.createXMLEventReader(response);

        XMLEventProcessor processor = new XMLEventProcessor(eventFilters.toArray(new XMLEventProcessor.EventFilter[eventFilters.size()]));
        ElementsAPIVersion.PaginationExtractingFilter paginationFilter = version.getPaginationExtractor();
        processor.addFilter(paginationFilter);
        processor.process(atomReader);

        final long endTime = System.currentTimeMillis();
        timeSpentInProcessing += (endTime - startTime);
        return paginationFilter.getExtractedItem();
    }
}
