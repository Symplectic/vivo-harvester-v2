/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api;

import com.sun.xml.xsom.impl.ContentTypeImpl;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.lang.NullArgumentException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Date;


public class ElementsAPIHttpClient {
    final private String username;
    final private String password;
    final private String url;

    private static final int defaultSoTimeout = 5 * 60 * 1000; // 5 minutes, in milliseconds

    public static void setSocketTimeout(int millis) {
        HttpConnectionManager connectionManager = ElementsAPIHttpConnectionManager.getInstance();
        HttpConnectionManagerParams params = connectionManager.getParams();
        params.setSoTimeout(millis);
        connectionManager.setParams(params);
    }

    public ElementsAPIHttpClient(String url) throws URISyntaxException {
        this(url, null, null);
    }

    public ElementsAPIHttpClient(String url, String username, String password) throws URISyntaxException {
        this(url, username, password, defaultSoTimeout);
    }

    public ElementsAPIHttpClient(String url, String username, String password, int socketTimeout) throws URISyntaxException {
        if(url == null) throw new NullArgumentException("url");
        ElementsAPIURLValidator validator = new ElementsAPIURLValidator(url);
        this.url = url;

        //Only store the username and password if the scheme being used is considered "secure" - to avoid accidentally sending credentials in the clear.
        if(validator.urlIsSecure() && username != null) {
            this.username = username;
            this.password = password;
        } else {
            this.username = null;
            this.password = null;
        }

        setSocketTimeout(socketTimeout);
    }

    public ApiResponse executeGetRequest() throws IOException {
        // Prepare the HttpClient
        HttpClient client = new HttpClient(ElementsAPIHttpConnectionManager.getInstance());
        if (username != null) {
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
            client.getState().setCredentials(AuthScope.ANY, credentials);
            HttpClientParams params = new HttpClientParams(client.getParams());
            client.setParams(params);
        }

        // Ensure we do not send request too frequently
        regulateRequestFrequency();

        // Issue get request
        GetMethod getMethod = new GetMethod(url);
        int responseCode = client.executeMethod(getMethod);

        //convert non 200 responses into exceptions.
        HttpException exception;
        if(responseCode != HttpStatus.SC_OK){
            exception = new HttpException(MessageFormat.format("Invalid Http response code received: {0} ({1})", responseCode, HttpStatus.getStatusText(responseCode)));
            exception.setReasonCode(responseCode);
            throw exception;
        }
        return new ApiResponse(responseCode, getMethod);
    }

    /**
     * Execute a get request,
     * @param maxRetries Number of times to retry the request
     * @return InputStream corresponding to the request body
     * @throws IOException Failure reading the request stream
     */
    ApiResponse executeGetRequest(int maxRetries) throws IOException {
        if (maxRetries == 0) {
            return executeGetRequest();
        }

        IOException lastError = null;

        while (maxRetries-- > 0) {
            try {
                return executeGetRequest();
            } catch (IOException io) {
                lastError = io;
            }
        }

        throw lastError;
    }

    public static void setRequestDelay(int millis) {
        intervalInMSecs = millis;
    }

    /**
     * Delay method - ensure that requests are not sent too frequently to the Elements API,
     * by calling this method prior to executing the HttpClient request.
     */
    private static Date lastRequest = null;
    private static int intervalInMSecs = 250;
    private static synchronized void regulateRequestFrequency() {
        try {
            if (lastRequest != null) {
                Date current = new Date();
                if (lastRequest.getTime() + intervalInMSecs > current.getTime()) {
                    Thread.sleep(intervalInMSecs - (current.getTime() - lastRequest.getTime()));
                }
            }
        } catch (InterruptedException ie) {
            // Ignore an interrupt
        } finally {
            lastRequest = new Date();
        }
    }

    /*
    Inner class to represent the response from an API and offer a "dispose" method to close http connections when finished with the stream.
     */
    public static class ApiResponse{
        final private HttpMethodBase method;
        final private int responseCode;
        private boolean disposed = false;

        ApiResponse(int responseCode, HttpMethodBase method){
            if(method == null) throw new NullArgumentException("method");
            this.method = method;
            this.responseCode = responseCode;
        }
        public InputStream getResponseStream() throws IOException{
            if(!disposed) {
                //timing testing hack
                //method.getResponseBodyAsString();
                return new BufferedInputStream(method.getResponseBodyAsStream());
            }
            throw new IOException("APIResponse object already disposed");
        }

        public void dispose() throws IOException{
            if(!disposed) {
                InputStream stream = getResponseStream();
                if (stream != null) stream.close();
                method.releaseConnection();
                disposed = true;
            }
        }

        public int getResponseCode() {
            return responseCode;
        }
    }
}
