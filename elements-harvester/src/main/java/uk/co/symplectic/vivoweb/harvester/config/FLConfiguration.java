/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.config;
import org.apache.commons.lang.text.StrBuilder;
import uk.co.symplectic.utils.configuration.ConfigKey;
import uk.co.symplectic.utils.configuration.ConfigParser;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Configuration class for the Fragment Loader application"
 */
public class FLConfiguration {
    private static class Parser extends ConfigParser {
        //keys
        private ConfigKey ARG_SPARQL_API_ENDPOINT = new ConfigKey("sparqlApiEndpoint");
        private ConfigKey ARG_SPARQL_API_GRAPH_URI = new ConfigKey("sparqlApiGraphUri", "http://vitro.mannlib.cornell.edu/default/vitro-kb-2");
        private ConfigKey ARG_SPARQL_API_USERNAME = new ConfigKey("sparqlApiUsername");
        private ConfigKey ARG_SPARQL_API_PASSWORD = new ConfigKey("sparqlApiPassword");

        private ConfigKey ARG_PROCESS_SUBTRACT_FILES_FIRST = new ConfigKey("processSubtractFilesFirst", "false");

        private ConfigKey ARG_TDB_OUTPUT_DIRECTORY = new ConfigKey("tdbOutput", "../previous-harvest/");

        private ConfigKey ARG_RETRY_DELAY = new ConfigKey("retryDelay", "500");
        private ConfigKey ARG_MAX_RETRIES = new ConfigKey("maxRetries", "5");

        private ConfigKey ARG_API_SOCKET_TIMEOUT = new ConfigKey("apiSocketTimeout", "300000"); //TODO: review this default

        //storage
        private String sparqlApiEndpoint;
        private String sparqlApiGraphUri;
        private String sparqlApiUsername;
        private String sparqlApiPassword;
        private File tdbOutputDir;
        private boolean processSubtractFilesFirst;

        private int retryDelay = -1;
        private int maxRetries = -1;
        private int apiSocketTimeout = -1;

        //Constructor and methods
        Parser(Properties props, List<String> errors) {
            super(props, errors);
        }

        void parse() {
            values.sparqlApiEndpoint = getString(ARG_SPARQL_API_ENDPOINT, false);
            values.sparqlApiGraphUri = getString(ARG_SPARQL_API_GRAPH_URI, false);
            values.sparqlApiUsername = getString(ARG_SPARQL_API_USERNAME, false);
            values.sparqlApiPassword = getString(ARG_SPARQL_API_PASSWORD, false);
            values.tdbOutputDir = getFileDirFromConfig(ARG_TDB_OUTPUT_DIRECTORY);
            values.processSubtractFilesFirst = getBoolean(ARG_PROCESS_SUBTRACT_FILES_FIRST);
            values.retryDelay = getInt(ARG_RETRY_DELAY);
            values.maxRetries = getInt(ARG_MAX_RETRIES);
            values.apiSocketTimeout = getInt(ARG_API_SOCKET_TIMEOUT);
        }
    }

    private static List<String> configErrors = new ArrayList<String>();
    private static Parser values = null;

    public static String getSparqlApiEndpoint() {
        return values.sparqlApiEndpoint;
    }

    public static String getSparqlApiGraphUri() {
        return values.sparqlApiGraphUri;
    }

    public static String getSparqlApiUsername() {
        return values.sparqlApiUsername;
    }

    public static String getSparqlApiPassword() {
        return values.sparqlApiPassword;
    }

    public static File getTdbOutputDir() {
        return values.tdbOutputDir;
    }

    public static boolean getProcessSubtractFilesFirst() {
        return values.processSubtractFilesFirst;
    }

    public static int getApiSocketTimeout() {
        return values.apiSocketTimeout;
    }

    public static int getMaxRetries() {
        return values.maxRetries;
    }

    public static int getRetryDelay() {
        return values.retryDelay;
    }

    //Has the system being successfully configured to move forwards?
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isConfigured() {
        return configErrors.size() == 0;
    }

    public static String getConfiguredValues() {
        return values == null ? null : values.reportConfiguredValues();
    }

    public static String getUsage() {
        StrBuilder builder = new StrBuilder();

        String configuredValueString = getConfiguredValues();
        if (configuredValueString != null) {
            builder.appendln(configuredValueString);
            builder.appendln("");
        }

        if (configErrors.size() != 0) {
            builder.appendln("Errors detected in supplied configuration values: ");
            for (String error : configErrors) {
                builder.append("\t");
                builder.appendln(error);
            }
        }

        if (builder.length() != 0) return builder.toString();
        return "Error generating usage string";
    }

    @SuppressWarnings("RedundantThrows")
    public static void parse(String propertiesFileName) throws IOException, ConfigParser.UsageException {
        try {
            Properties props = ConfigParser.getPropsFromFile(propertiesFileName);
            values = new Parser(props, configErrors);
            values.parse();
            //props.load(new FileInputStream("elementsfetch.properties"));
        } catch (Exception e) {
            configErrors.add(MessageFormat.format("Could not parse properties file: \"{0}\" : {1}", propertiesFileName, e.getMessage()));
        }

        if (!isConfigured()) throw new ConfigParser.UsageException();
    }
}

