/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.app;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.FileAide;
import uk.co.symplectic.utils.triplestore.FileSplitter;
import uk.co.symplectic.utils.http.HttpClient;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


//TODO: move to proper logging framework, and make log retries vs actual errors better and return a failure return code properly.
public class FragmentLoader {

    private static final Logger log = LoggerFactory.getLogger(FragmentLoader.class);

    private static int retryDelayMillis = 500;
    private static int maxRetries = 5;

    public static void main(String[] args) {
        //initLogger();
        if(args.length != 1) throw new IllegalArgumentException("Args must contain 1 parameter, containing the path of the directory to monitor");
        String storeDirPath = args[0];
        File storeDir = new File(storeDirPath);
        if(!storeDir.exists() || !storeDir.isDirectory())
            throw new IllegalArgumentException(MessageFormat.format("Directory \"{0}\" does not exist", storeDirPath));

        //TODO: parametrise these
        //String vivoUrl = "http://silver.symplectic.co.uk/vivoTest";
        String vivoUrl = "http://localhost:8087/vivoTest";
        String graphUri = "http://vitro.mannlib.cornell.edu/default/vitro-kb-2";
        String username = "vivo_root@mydomain.edu";
        String password = "letmein";
        int logEveryN = 100;

        log.info(MessageFormat.format("Monitoring directory {0} for fragments", storeDir.getPath()));
        //FileSplitter splitter = new FileSplitter(storeDir);
        FileSplitter splitter = new FileSplitter.NTriplesSplitter(storeDir);

        try {
            //loop until stopped.
            while (true) {
                //get current file list and then loop to load them all.
                List<File> filesToSort = splitter.getFragmentFilesInOrder();

                if (filesToSort.size() > 0) {
                    log.info(MessageFormat.format("{0} new fragments detected - sending to vivo at :{1}", filesToSort.size(), vivoUrl));
                    try {
                        URI validGraphURI = new URI(graphUri);
                        int count = 0;
                        HttpClient.setSocketTimeout(15 * 60 * 1000); //15 min in milliseconds
                        for (File file : filesToSort) {
                            //if (count > 10) break;
                            //get the content we are sending from the fragment file
                            BufferedReader fileInput = null;
                            String sparqlContent = null;
                            try {
                                fileInput = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                                String line;
                                StringBuilder builder = new StringBuilder();
                                while ((line = fileInput.readLine()) != null) {
                                    builder.append(line).append("\n");
                                }
                                sparqlContent = StringUtils.trimToNull(builder.toString());
                            }
                            finally{
                                if(fileInput != null) fileInput.close();
                            }

                            //work out what type of content this is (add or subtract)
                            boolean shouldDeleteContent;
                            switch(splitter.getFileType(file)){
                                case Additions:
                                    shouldDeleteContent = false;
                                    break;
                                case Subtractions:
                                    shouldDeleteContent = true;
                                    break;
                                default:
                                    throw new IllegalStateException("Invalid File Type detected");
                            }


                            //if there is content to send to vivo then send it
                            if(sparqlContent != null) {
                                SparqlUpdateHttpClient client = new SparqlUpdateHttpClient(vivoUrl + "/api/sparqlUpdate", username, password, validGraphURI);
                                trySendFragment(client, sparqlContent, shouldDeleteContent);
                            }

                            //if we have not errored out delete the file in question.
                            if (!file.delete())
                                throw new IllegalStateException(MessageFormat.format("failed to delete file {0}", file.getName()));

                            count++;

                            System.out.print('.');
                            if(count % logEveryN == 0) {
                                log.info(MessageFormat.format(" {0} fragments processed", count));
                                System.out.println();
                            }
                        }
                    } catch (URISyntaxException e) {
                        throw new IllegalStateException(e);
                        //todo: do something sensible
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                        //todo: do something sensible
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Interrupted whilst sleeping between directory checks");
                }
            }
        }
        catch (IllegalStateException e){
            log.error("Fragment loader exited unexpectedly", e);
        }
    }

    //tryies to send the fragment as required, attempting to repeat if general io type exceptions occur
    private static void trySendFragment(SparqlUpdateHttpClient client, String sparqlContent, boolean shouldDeleteContent) throws IllegalStateException {
        int retryCount = 0;
        do {
            HttpClient.ApiResponse apiResponse = null;
            try {
                apiResponse = client.postSparqlFragment(sparqlContent, shouldDeleteContent);
                if(retryCount != 0) System.out.print(')');
                return;
            }
            catch (IOException e) {
                if(e instanceof HttpClient.InvalidResponseException){
                    int statusCode = ((HttpClient.InvalidResponseException) e).getResponseCode();
                    //if forbidden then just jump out here..
                    if(statusCode == HttpStatus.SC_FORBIDDEN || statusCode == HttpStatus.SC_UNAUTHORIZED) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                }
                //always log a warning..
                log.warn("IO Error handling Sparql API request", e);

                if (++retryCount >= maxRetries) {
                    throw new IllegalStateException("IO Error handling Sparql API request", e);
                }
            } finally {
                if (apiResponse != null) {
                    try {
                        apiResponse.dispose();
                    } catch (IOException e) {
                        throw new IllegalStateException("IOException attempting to dispose apiResponse from Sparql api request", e);
                    }
                }
            }

            try {
                //log some *'s to indicate failures.
                if(retryCount == 0) System.out.print('(');
                System.out.print('*');
                Thread.sleep(retryDelayMillis);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted whilst retrying query");
            }
        } while (true);
    }

    static void initLogger(){
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator jc = new JoranConfigurator();
        jc.setContext(context);

        try {
            InputStream e = FileAide.getFirstFileNameChildInputStream(".", "logback-test.xml");
            if(e == null) {
                e = FileAide.getFirstFileNameChildInputStream(".", "logback.xml");
            }

            if(e == null) {
                e = Thread.currentThread().getContextClassLoader().getResourceAsStream("logback-test.xml");
            }

            if(e == null) {
                e = Thread.currentThread().getContextClassLoader().getResourceAsStream("logback.xml");
            }

            if(e != null) {
                context.reset();
                context.stop();
                jc.doConfigure(e);
                context.start();
            }

        } catch (IOException var7) {
            throw new IllegalArgumentException(var7);
        } catch (JoranException var8) {
            throw new IllegalArgumentException(var8);
        }
    }

    static class SparqlUpdateHttpClient extends HttpClient {

        final private String username;
        final private String password;

        private final URI graphToModify;

        private static final String addTemplate =      "INSERT DATA '{'\nGRAPH <{0}> '{'\n{1}\n'}'\n'}'";
        private static final String subtractTemplate = "DELETE DATA '{'\nGRAPH <{0}> '{'\n{1}\n'}'\n'}'";

        private SparqlUpdateHttpClient(String url, String username, String password, URI graphToModify) throws URISyntaxException{
            super(url, username, password);
            if(graphToModify == null) throw new NullArgumentException("graphToModify");
            this.graphToModify = graphToModify;
            this.username = username;
            this.password = password;
        }

        public ApiResponse postSparqlFragment(String sparqlUpdate, boolean deleteData) throws IOException {
            // Prepare the HttpClient, note creds are passed in the post request for this operation..
            CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(getConnectionManager()).setDefaultRequestConfig(getDefaultRequestConfig()).build();

            // Ensure we do not send request too frequently
            regulateRequestFrequency();

            String template = deleteData ? subtractTemplate : addTemplate;
            String updateValue = MessageFormat.format(template, graphToModify.toString(), sparqlUpdate);

            // Issue post request
            HttpPost post = new HttpPost(getUrl());
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("email",username));
            nameValuePairs.add(new BasicNameValuePair("password",password));
            nameValuePairs.add(new BasicNameValuePair("update", updateValue));
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

            CloseableHttpResponse response = httpclient.execute(post);

            ///convert non 200 responses into exceptions.
            HttpClient.InvalidResponseException exception;
            int responseCode = response.getStatusLine().getStatusCode();
            if(responseCode != HttpStatus.SC_OK){
                String codeDescription = EnglishReasonPhraseCatalog.INSTANCE.getReason(responseCode, null);
                String message = MessageFormat.format("Invalid Http response code received: {0} ({1})", responseCode, codeDescription);
                exception = new HttpClient.InvalidResponseException(message, responseCode);
                throw exception;
            }
            return new ApiResponse(response);
        }
    }
}
