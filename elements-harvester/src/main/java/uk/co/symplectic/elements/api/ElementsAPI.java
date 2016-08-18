/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.xml.StAXUtils;
import uk.co.symplectic.xml.XMLEventProcessor;

import javax.xml.stream.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;

/**
 * Main Elements API Client class
 */
public class ElementsAPI {

    public static long timeSpentInNetwork = 0;
    public static long timeSpentInProcessing = 0;
    public static void resetTimers(){
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

    private int maxRetries = 5;
    private int retryDelayMillis = 500;

    public ElementsAPI(ElementsAPIVersion version, String url){
        this(version, url, null, null);
    }

    public ElementsAPI(ElementsAPIVersion version, String url, String username, String password) {
        if(version == null){
            log.error("provided version must not be null on construction");
            throw new NullArgumentException("version");
        }

        this.version = version;

        try{
            ElementsAPIURLValidator validator = new ElementsAPIURLValidator(url);
            this.url = StringUtils.stripEnd(url, "/") + "/";

            if(validator.urlIsSecure()) {
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
        }
        catch(URISyntaxException e){
            String errorMsg = MessageFormat.format("Provided URL was invalid: {0}", e.getMessage());
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    public void executeQuery(ElementsFeedQuery feedQuery, APIResponseFilter... filters) {
        List<XMLEventProcessor.EventFilter> eventFilters = new ArrayList<XMLEventProcessor.EventFilter>();
        for(APIResponseFilter filter : filters){
            if(!filter.supports(version)){
                String message = MessageFormat.format("Filter {0} does not support API ElementsAPIVersion {1}", filter.getClass().getName(), version.getVersionName());
                throw new IllegalStateException(message);
            }
            eventFilters.add(filter.getEventFilter());
        }
//        XMLEventProcessor processor = new XMLEventProcessor(eventFilters.toArray(new XMLEventProcessor.EventFilter[eventFilters.size()]));

        String currentQueryUrl = feedQuery.getUrlString(url, version.getUrlBuilder());
        ElementsFeedPagination pagination = executeInternalQuery(currentQueryUrl, eventFilters);

        //todo: decide if keep low level logging.
        int counter = 1;

        if (pagination != null && feedQuery.getProcessAllPages()) {
            //hack hack
            while (pagination.getNextURL() != null) {
                // Some versions of Elements incorrectly return the pagination information
                // Check that the next URL is valid before continuing
                //TODO: make this test if pagination has been retrieved correctly (e.g. test current against last to exit or something?)
                String nextQueryUrl = pagination.getNextURL();
                try {
                    ElementsAPIURLValidator validator = new ElementsAPIURLValidator(nextQueryUrl, currentQueryUrl);
                    if(validator.urlIsMismatched()){
                        log.warn(MessageFormat.format("Next URL in a feed \"{0}\" has a different host to the previous URL: {1}", nextQueryUrl, currentQueryUrl));
                        log.warn("There is probably a mismatch between the configured API URL in this program and the API baseURI configured in Elements");
                    }
                }
                catch (URISyntaxException e){
                    String errorMsg = MessageFormat.format("Next URL for a feed was invalid: {0}", e.getMessage());
                    log.error(errorMsg);
                    throw new IllegalStateException(errorMsg, e);
                }
                //System.out.println(nextQueryUrl);

                if (nextQueryUrl.equals(currentQueryUrl)) {
                    throw new IllegalStateException("Error in the pagination response from Elements - unable to continue processing");
                }
                currentQueryUrl = nextQueryUrl;
                pagination = executeInternalQuery(currentQueryUrl, eventFilters);

                //todo: decide if keep low level logging.
                counter++;
                if(counter % 40 == 0){
                    log.info(MessageFormat.format("{0} items processed: network-time: {1}, processing-time: {2}", counter*25, ElementsAPI.timeSpentInNetwork, ElementsAPI.timeSpentInProcessing));
                    ElementsAPI.resetTimers();
                }

            }
        }
    }

    //TODO : rationalise with main query call to have common usage of the underlying client with nice retry behaviour etc.
    public boolean fetchResource(String resourceURL, OutputStream outputStream) {
        ElementsAPIHttpClient.ApiResponse apiResponse = null;
        try {
            ElementsAPIHttpClient apiClient = new ElementsAPIHttpClient(resourceURL, username, password);
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

    private ElementsFeedPagination executeInternalQuery(String url, Collection<XMLEventProcessor.EventFilter> eventFilters) throws IllegalStateException {
        int retryCount = 0;
        do {
            ElementsAPIHttpClient.ApiResponse apiResponse = null;
            try {
                long startTime = System.currentTimeMillis();
                ElementsAPIHttpClient apiClient = new ElementsAPIHttpClient(url, username, password);
                apiResponse = apiClient.executeGetRequest();
                long endTime = System.currentTimeMillis();
                timeSpentInNetwork += (endTime - startTime);
                return parseEventResponse(apiResponse.getResponseStream(), eventFilters);
            }
            catch (URISyntaxException e) {
                log.error("Invalid API query detected", e);
                //not going to recover from this so just fail.
                throw new IllegalStateException(e);
            }
            catch (IOException e) {
                if(e instanceof HttpException){
                    int statusCode = ((HttpException) e).getReasonCode();
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
