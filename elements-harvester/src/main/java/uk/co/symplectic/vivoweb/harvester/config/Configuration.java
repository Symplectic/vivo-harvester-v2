/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.config;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.elements.api.ElementsAPIVersion;
import uk.co.symplectic.utils.configuration.ConfigKey;
import uk.co.symplectic.utils.configuration.ConfigParser;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Configuration {

    private static class Parser extends ConfigParser {
        //keys
        private ConfigKey ARG_RAW_OUTPUT_DIRECTORY = new ConfigKey("rawOutput", "../data/raw-records/");
        private ConfigKey ARG_RDF_OUTPUT_DIRECTORY = new ConfigKey("rdfOutput", "../data/translated-records/");
        private ConfigKey ARG_TDB_OUTPUT_DIRECTORY = new ConfigKey("tdbOutput", "../previous-harvest/");

        private ConfigKey ARG_XSL_TEMPLATE = new ConfigKey("xslTemplate");

        private ConfigKey ARG_ELEMENTS_API_ENDPOINT = new ConfigKey("apiEndpoint");
        private ConfigKey ARG_ELEMENTS_API_VERSION = new ConfigKey("apiVersion");
        private ConfigKey ARG_ELEMENTS_API_USERNAME = new ConfigKey("apiUsername");
        private ConfigKey ARG_ELEMENTS_API_PASSWORD = new ConfigKey("apiPassword");
        private ConfigKey ARG_IGNORE_SSL_ERRORS = new ConfigKey("ignoreSSLErrors", "false"); //TODO: review this default
        private ConfigKey ARG_REWRITE_MISMATCHED_URLS = new ConfigKey("rewriteMismatchedPaginationUrls", "false"); //TODO: review this default

        private ConfigKey ARG_CURRENT_STAFF_ONLY = new ConfigKey("currentStaffOnly", "true"); //TODO: review this default
        private ConfigKey ARG_ACADEMICS_ONLY = new ConfigKey("academicsOnly", "true"); //TODO: review this default
        private ConfigKey ARG_VISIBLE_LINKS_ONLY = new ConfigKey("visibleLinksOnly", "false"); //TODO: review this default

        private ConfigKey ARG_USE_FULL_UTF8 = new ConfigKey("useFullUTF8", "false"); //TODO: review this default

        private ConfigKey ARG_VIVO_IMAGE_DIR = new ConfigKey("vivoImageDir", "/data");
        private ConfigKey ARG_VIVO_BASE_URI = new ConfigKey("vivoBaseURI", "http://vivo.mydomain.edu/individual/");

        private ConfigKey ARG_API_QUERY_CATEGORIES = new ConfigKey("queryObjects"); //TODO : rename this input param?
        private ConfigKey ARG_API_PARAMS_GROUPS = new ConfigKey("paramGroups");
        private ConfigKey ARG_API_EXCLUDE_GROUPS = new ConfigKey("excludeGroups");

        private ConfigKey ARG_API_PARAMS_USER_GROUPS = new ConfigKey("paramUserGroups");
        private ConfigKey ARG_API_EXCLUDE_USER_GROUPS = new ConfigKey("excludeUserGroups");

        private ConfigKey ARG_API_FULL_DETAIL_PER_PAGE = new ConfigKey("fullDetailPerPage", "25");
        private ConfigKey ARG_API_REF_DETAIL_PER_PAGE = new ConfigKey("refDetailPerPage", "100");

        private ConfigKey ARG_API_SOCKET_TIMEOUT = new ConfigKey("apiSocketTimeout", "0"); //TODO: review this default
        private ConfigKey ARG_API_REQUEST_DELAY = new ConfigKey("apiRequestDelay", "-1"); //TODO: review this default

        private ConfigKey ARG_MAX_XSL_THREADS = new ConfigKey("maxXslThreads", "0"); //TODO: review this default
        private ConfigKey ARG_MAX_RESOURCE_THREADS = new ConfigKey("maxResourceThreads", "0"); //TODO: review this default

        private ConfigKey ARG_MAX_FRAGMENT_FILE_SIZE = new ConfigKey("maxFragmentFileSize", "1228800"); //TODO: review this default

        private ConfigKey ARG_ZIP_FILES = new ConfigKey("zipFiles", "false"); //TODO: review this default

        //storage
        private int maxThreadsResource = -1;
        private int maxThreadsXsl = -1;

        private int maxFragmentFileSize = -1;

        private String apiEndpoint;
        private ElementsAPIVersion apiVersion;

        private String apiUsername;
        private String apiPassword;
        private boolean ignoreSSLErrors = false;
        private boolean rewriteMismatchedUrls = false;

        private int apiSoTimeout = -1;
        private int apiRequestDelay = -1;

        private int fullDetailPerPage = -1;
        private int refDetailPerPage = -1;

        private List<ElementsItemId.GroupId> groupsToExclude;
        private List<ElementsItemId.GroupId> groupsToHarvest;

        private List<ElementsItemId.GroupId> groupsOfUsersToHarvest;
        private List<ElementsItemId.GroupId> groupsOfUsersToExclude;

        private List<ElementsObjectCategory> categoriesToHarvest;

        private boolean currentStaffOnly = true;
        private boolean academicsOnly = true;
        private boolean visibleLinksOnly = false;

        private boolean useFullUTF8 = false;

        private String vivoImageDir;
        private String baseURI;
        private String xslTemplate;

        private File rawOutputDir;
        private File rdfOutputDir;
        private File tdbOutputDir;

        private boolean zipFiles = false;

        //Constructor and methods
        Parser(Properties props, List<String> errors){ super(props, errors); }

        private List<ElementsObjectCategory> getCategories(ConfigKey configKey, boolean tolerateNull) {
            String key = configKey.getName();
            List<ElementsObjectCategory> categories = new ArrayList<ElementsObjectCategory>();
            String value = configKey.getValue(props);
            if (!StringUtils.isEmpty(value)) {
                for (String category : value.split("\\s*,\\s*")) {
                    ElementsObjectCategory cat = ElementsObjectCategory.valueOf(category);
                    if (cat == null) {
                        configErrors.add(MessageFormat.format("Invalid value ({0}) provided within argument {1} : {2} (every value must represent a valid Elements Category)", category, key, value));
                    }
                    categories.add(cat);
                }
            } else if(!tolerateNull) {
                configErrors.add(MessageFormat.format("Invalid value provided within argument {0} : {1} (must supply at least one Elements Category)", key, value));
            }
            return categories;
        }

        private ElementsAPIVersion getApiVersion(ConfigKey configKey) {
            String key = configKey.getName();
            String value = configKey.getValue(props);
            try {
                return ElementsAPIVersion.parse(value);
            } catch (IllegalStateException e) {
                configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be a valid elements api version)", key, value));
            }
            return null;
        }

        void parse(){
            values.maxThreadsResource = getInt(ARG_MAX_RESOURCE_THREADS);
            values.maxThreadsXsl = getInt(ARG_MAX_XSL_THREADS);

            values.apiEndpoint = getString(ARG_ELEMENTS_API_ENDPOINT, false);
            values.apiVersion = getApiVersion(ARG_ELEMENTS_API_VERSION);

            values.apiUsername = getString(ARG_ELEMENTS_API_USERNAME, true); //allow null as may be a plain http endpoint
            values.apiPassword = getString(ARG_ELEMENTS_API_PASSWORD, true); //allow null as may be a plain http endpoint

            values.apiSoTimeout = getInt(ARG_API_SOCKET_TIMEOUT);
            values.apiRequestDelay = getInt(ARG_API_REQUEST_DELAY);

            List<ElementsItemId.GroupId> groups = new ArrayList<ElementsItemId.GroupId>();
            //allow null when getting integers as that means include everything
            for (Integer groupId : getIntegers(ARG_API_PARAMS_GROUPS, true)) groups.add(ElementsItemId.createGroupId(groupId));
            values.groupsToHarvest = groups;

            groups = new ArrayList<ElementsItemId.GroupId>();
            //allow null as that means exclude nothing
            for (Integer groupId : getIntegers(ARG_API_EXCLUDE_GROUPS, true)) groups.add(ElementsItemId.createGroupId(groupId));
            values.groupsToExclude = groups;

            groups = new ArrayList<ElementsItemId.GroupId>();
            //allow null as that means exclude nothing
            for (Integer groupId : getIntegers(ARG_API_PARAMS_USER_GROUPS, true)) groups.add(ElementsItemId.createGroupId(groupId));
            values.groupsOfUsersToHarvest = groups;

            groups = new ArrayList<ElementsItemId.GroupId>();
            //allow null as that means exclude nothing
            for (Integer groupId : getIntegers(ARG_API_EXCLUDE_USER_GROUPS, true)) groups.add(ElementsItemId.createGroupId(groupId));
            values.groupsOfUsersToExclude = groups;

            values.categoriesToHarvest = getCategories(ARG_API_QUERY_CATEGORIES, false); //do not allow null for categories to query

            values.fullDetailPerPage = getInt(ARG_API_FULL_DETAIL_PER_PAGE);
            values.refDetailPerPage = getInt(ARG_API_REF_DETAIL_PER_PAGE);

            values.currentStaffOnly = getBoolean(ARG_CURRENT_STAFF_ONLY);
            values.academicsOnly = getBoolean(ARG_ACADEMICS_ONLY);
            values.visibleLinksOnly = getBoolean(ARG_VISIBLE_LINKS_ONLY);

            values.useFullUTF8 = getBoolean(ARG_USE_FULL_UTF8);

            String baseUriString = getString(ARG_VIVO_BASE_URI, false);
            //ensure the uri ends with a / - required by mappings.
            if(!baseUriString.endsWith("/")) baseUriString = baseUriString + "/";
            values.baseURI = baseUriString;
            values.vivoImageDir = getString(ARG_VIVO_IMAGE_DIR, false);
            values.xslTemplate = getString(ARG_XSL_TEMPLATE, false);

            values.rawOutputDir = getFileDirFromConfig(ARG_RAW_OUTPUT_DIRECTORY);
            values.rdfOutputDir = getFileDirFromConfig(ARG_RDF_OUTPUT_DIRECTORY);
            values.tdbOutputDir = getFileDirFromConfig(ARG_TDB_OUTPUT_DIRECTORY);

            values.ignoreSSLErrors = getBoolean(ARG_IGNORE_SSL_ERRORS);
            values.rewriteMismatchedUrls = getBoolean(ARG_REWRITE_MISMATCHED_URLS);
            values.zipFiles = getBoolean(ARG_ZIP_FILES);
            values.maxFragmentFileSize = getInt(ARG_MAX_FRAGMENT_FILE_SIZE);
        }
    }

    private static List<String> configErrors = new ArrayList<String>();
    private static Parser values = null;

    public static Integer getMaxThreadsResource() {
        return values.maxThreadsResource;
    }

    public static Integer getMaxThreadsXsl() {
        return values.maxThreadsXsl;
    }

    public static String getApiEndpoint() {
        return values.apiEndpoint;
    }

    public static ElementsAPIVersion getApiVersion() {
        return values.apiVersion;
    }

    public static String getApiUsername() {
        return values.apiUsername;
    }

    public static String getApiPassword() {
        return values.apiPassword;
    }

    public static int getApiSoTimeout() { return values.apiSoTimeout; }

    public static int getApiRequestDelay() {
        return values.apiRequestDelay;
    }

    public static int getFullDetailPerPage() {
        return values.fullDetailPerPage;
    }

    public static int getRefDetailPerPage() {
        return values.refDetailPerPage;
    }

    public static int getMaxFragmentFileSize() {
        return values.maxFragmentFileSize;
    }

    public static List<ElementsItemId.GroupId> getGroupsToExclude() {
        return values.groupsToExclude;
    }

    public static List<ElementsItemId.GroupId> getGroupsToHarvest() {
        return values.groupsToHarvest;
    }

    public static List<ElementsItemId.GroupId> getGroupsOfUsersToExclude() {
        return values.groupsOfUsersToExclude;
    }

    public static List<ElementsItemId.GroupId> getGroupsOfUsersToHarvest() {
        return values.groupsOfUsersToHarvest;
    }

    public static List<ElementsObjectCategory> getCategoriesToHarvest() {
        return values.categoriesToHarvest;
    }

    public static boolean getCurrentStaffOnly() {
        return values.currentStaffOnly;
    }

    public static boolean getAcademicsOnly() {
        return values.academicsOnly;
    }

    public static boolean getVisibleLinksOnly() {
        return values.visibleLinksOnly;
    }

    public static boolean getUseFullUTF8() {
        return values.useFullUTF8;
    }

    public static String getVivoImageDir() {
        return values.vivoImageDir;
    }

    public static String getBaseURI() { return values.baseURI; }

    public static String getXslTemplate() {
        return values.xslTemplate;
    }

    public static File getRawOutputDir() {
        return values.rawOutputDir;
    }

    public static File getRdfOutputDir() {
        return values.rdfOutputDir;
    }

    public static File getTdbOutputDir() { return values.tdbOutputDir; }

    public static boolean getIgnoreSSLErrors() {
        return values.ignoreSSLErrors;
    }

    public static boolean getRewriteMismatchedUrls() { return values.rewriteMismatchedUrls; }

    public static boolean getZipFiles() {
        return values.zipFiles;
    }

    //Has the system being successfully configured to move forwards?
    public static boolean isConfigured() {
        return configErrors.size() == 0;
    }

    public static String getConfiguredValues(){ return values == null ? null : values.reportConfiguredValues();}

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

    public static void parse(String propertiesFileName) throws IOException, ConfigParser.UsageException {
        InputStream stream = null;
        try {
            try{
                stream = Configuration.class.getClassLoader().getResourceAsStream(propertiesFileName);
                Properties props = new Properties();
                props.load(stream);
                values = new Parser(props, configErrors);
                values.parse();
            }
            finally{
                if(stream != null) stream.close();
            }
        }
        catch(Exception e){
            configErrors.add(MessageFormat.format("Could not load properties file: \"{0}\"", propertiesFileName));
        }

        if (!isConfigured()) throw new ConfigParser.UsageException();
    }
}

