/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.app;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.utils.ExecutorServiceUtils;
import uk.co.symplectic.utils.LoggingUtils;
import uk.co.symplectic.utils.configuration.ConfigParser;
import uk.co.symplectic.utils.triplestore.FileSplitter;
import uk.co.symplectic.utils.http.HttpClient;
import uk.co.symplectic.vivoweb.harvester.config.FLConfiguration;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


//TODO: move to proper logging framework, and make log retries vs actual errors better.
public class FragmentLoader {

    private static final Logger log = LoggerFactory.getLogger(FragmentLoader.class);

    private static int retryDelayMillis = 500;
    private static int maxRetries = 5;

    private static class ShutdownHook extends Thread {
        @Override
        public void run() {
            log.info("FragmentLoader: End");
        }
    }

    public static void main(String[] args) {
        //initLogger();
        //if(args.length != 1) throw new IllegalArgumentException("Args must contain 1 parameter, containing the path of the directory to monitor");
        Throwable caught = null;
        //the amount of data that was loaded during the current period of activity
        int count = 0;
        //is the loader currently active or not..
        boolean isIdle = true;

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        try {
            LoggingUtils.initialise("fl_logback.xml");
            log.info(MessageFormat.format("running {0}", "FragmentLoader"));
            FLConfiguration.parse("fragmentloader.properties");
            log.info(FLConfiguration.getConfiguredValues());
            //String storeDirPath = args[0];
            //File storeDir = new File(storeDirPath);

            File interimTdbDirectory = FLConfiguration.getTdbOutputDir();
            File storeDir = new File(interimTdbDirectory, "fragments");

            String vivoUrl = FLConfiguration.getSparqlApiEndpoint();
            String graphUri = FLConfiguration.getSparqlApiGraphUri();
            String username = FLConfiguration.getSparqlApiUsername();
            String password = FLConfiguration.getSparqlApiPassword();
            boolean processSubtractFilesFirst = FLConfiguration.getProcessSubtractFilesFirst();

            int logEveryN = 100;


            log.info(MessageFormat.format("Monitoring directory {0} for fragments", storeDir.getPath()));
            //FileSplitter splitter = new FileSplitter(storeDir);
            FileSplitter splitter = new FileSplitter.NTriplesSplitter(storeDir);

            //loop until stopped.
            while (true) {
                if (storeDir.exists()) {
                    if (!storeDir.isDirectory()) {
                        throw new IllegalStateException(MessageFormat.format("Directory \"{0}\" is not a directory", storeDir.getAbsolutePath()));
                    }

                    //get current file list and then loop to load them all.
                    List<File> filesToSort = splitter.getFragmentFilesInOrder(processSubtractFilesFirst);

                    if (filesToSort.size() > 0) {
                        if(isIdle) log.info("New work detected - waking up");
                        isIdle = false;

                        log.info(MessageFormat.format("{0} new fragments detected - sending to vivo at :{1}", filesToSort.size(), vivoUrl));
                        try {
                            URI validGraphURI = new URI(graphUri);
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
                                } finally {
                                    if (fileInput != null) fileInput.close();
                                }

                                //work out what type of content this is (add or subtract)
                                boolean shouldDeleteContent;
                                switch (splitter.getFileType(file)) {
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
                                if (sparqlContent != null) {
                                    SparqlUpdateHttpClient client = new SparqlUpdateHttpClient(vivoUrl + "/api/sparqlUpdate", username, password, validGraphURI);
                                    trySendFragment(client, sparqlContent, shouldDeleteContent);
                                }

                                //if we have not errored out delete the file in question.
                                if (!file.delete())
                                    throw new IllegalStateException(MessageFormat.format("failed to delete file {0}", file.getName()));

                                count++;

                                System.out.print('.');
                                if (count % logEveryN == 0) {
                                    log.info(MessageFormat.format(" {0} fragments processed", count));
                                    System.out.println();
                                }
                            }
                        } catch (URISyntaxException e) {
                            throw new IllegalStateException("Either the Sparql API Endpoint or Graph URI is invalid", e);
                            //todo: do something sensible
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                            //todo: do something sensible
                        }
                    }
                    else {
                        if(!isIdle) {
                            log.info(MessageFormat.format("Completed all currently available work: {0} fragments processed in total", count));
                            count = 0;
                            log.info("Going to sleep");
                        }
                        isIdle = true;
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            throw new IllegalStateException("Interrupted whilst sleeping between directory checks", e);
                        }
                    }
                }
            }
        } catch (ConfigParser.UsageException e) {
            caught = e;
            if (!FLConfiguration.isConfigured()) {
                System.out.println(FLConfiguration.getUsage());
            } else {
                System.err.println("Caught UsageException initialising FragmentLoader");
                e.printStackTrace(System.err);
            }
        } catch(LoggingUtils.LoggingInitialisationException e) {
            caught = e;
            System.out.println("Logging Error detected");
            caught.printStackTrace(System.out);
        } catch (Exception e) {
            log.error("Unhandled Exception occurred during processing - terminating application", e);
            caught = e;
        } finally {
            if(!isIdle) {
                log.info(MessageFormat.format("Exiting whilst still active : {0} fragments processed in total", count));
            }
//            if (caught == null || !(caught instanceof LoggingUtils.LoggingInitialisationException)) {
//                log.debug("FragmentLoader: End");
//            }
            if (caught != null) {
                System.exit(1);
            }

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

    private static class SparqlUpdateHttpClient{

        final private HttpClient innerClient;
        //note the username and password are sent in the form data to upload not as HTTP basic auth so do at this layer.
        final private String username;
        final private String password;

        private final URI graphToModify;

        private static final String addTemplate =      "INSERT DATA '{'\nGRAPH <{0}> '{'\n{1}\n'}'\n'}'";
        private static final String subtractTemplate = "DELETE DATA '{'\nGRAPH <{0}> '{'\n{1}\n'}'\n'}'";

        private SparqlUpdateHttpClient(String url, String username, String password, URI graphToModify) throws URISyntaxException{
            if(graphToModify == null) throw new NullArgumentException("graphToModify");
            this.graphToModify = graphToModify;
            this.username = username;
            this.password = password;
            //set up the innerclient (username and password not passed on as are sent in the form data not in the request).
            innerClient = new HttpClient(url);
        }

        HttpClient.ApiResponse postSparqlFragment(String sparqlUpdate, boolean deleteData) throws IOException {

            String template = deleteData ? subtractTemplate : addTemplate;
            String updateValue = MessageFormat.format(template, graphToModify.toString(), sparqlUpdate);

            // prepare post request
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
            nameValuePairs.add(new BasicNameValuePair("email",username));
            nameValuePairs.add(new BasicNameValuePair("password",password));
            nameValuePairs.add(new BasicNameValuePair("update", updateValue));

            return innerClient.executePost(nameValuePairs);
        }
    }
}
