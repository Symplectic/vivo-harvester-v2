/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.config;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.elements.api.ElementsAPIVersion;
import uk.co.symplectic.utils.ImageUtils;
import uk.co.symplectic.utils.configuration.ConfigKey;
import uk.co.symplectic.utils.configuration.ConfigParser;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;

/**
 * Class representing the configuration of the ElementsFetchAndTranslate process.
 */
public class Configuration {

    /**
     * Custom parser extending ConfigParser that knows about the ConfigKeys that can exist in the .properties file
     * exposes a parse method that parses the Properties object passed in the constructor appropriately and populates
     * fields within the Parser object
     */
    private static class Parser extends ConfigParser {
        //keys
        private ConfigKey ARG_RAW_OUTPUT_DIRECTORY = new ConfigKey("rawOutput", "data/raw-records/");
        private ConfigKey ARG_RDF_OUTPUT_DIRECTORY = new ConfigKey("rdfOutput", "data/translated-records/");
        private ConfigKey ARG_TDB_OUTPUT_DIRECTORY = new ConfigKey("tdbOutput", "data/tdb-output/");
        private ConfigKey ARG_OTHER_OUTPUT_DIRECTORY = new ConfigKey("otherOutput", "data/other-data/");

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
        private ConfigKey ARG_REPULL_RELS_TO_CORRECT_VISIBILITY = new ConfigKey("repullRelsToCorrectVis", "true"); //TODO: review this default
        private ConfigKey ARG_RELATIONSHIP_TYPES_TO_REPROCESS = new ConfigKey("relTypesToReprocess", "activity-user-association, user-teaching-association"); //TODO: review this default

        private ConfigKey ARG_USE_FULL_UTF8 = new ConfigKey("useFullUTF8", "false"); //TODO: review this default

        private ConfigKey ARG_ELEMENTS_IMAGE_TYPE = new ConfigKey("elementsImageType", "profile");
        private ConfigKey ARG_VIVO_IMAGE_DIR = new ConfigKey("vivoImageDir", "data/harvestedImages/");
        private ConfigKey ARG_VIVO_IMAGE_BASE_PATH = new ConfigKey("vivoImageBasePath", "/harvestedImages/");

        private ConfigKey ARG_QUERY_CATEGORIES = new ConfigKey("queryObjects"); //TODO : rename this input param?
        private ConfigKey ARG_PARAMS_GROUPS = new ConfigKey("paramGroups");
        private ConfigKey ARG_EXCLUDE_GROUPS = new ConfigKey("excludeGroups");
        private ConfigKey ARG_INCLUDE_CHILD_GROUPS = new ConfigKey("includeChildGroupsOf");
        private ConfigKey ARG_EXCLUDE_CHILD_GROUPS = new ConfigKey("excludeChildGroupsOf");

        private ConfigKey ARG_PARAMS_USER_GROUPS = new ConfigKey("paramUserGroups");
        private ConfigKey ARG_EXCLUDE_USER_GROUPS = new ConfigKey("excludeUserGroups");

        private ConfigKey ARG_ELLIGIBILITY_TYPE = new ConfigKey("elligibilityFilterType");
        private ConfigKey ARG_ELLIGIBILITY_NAME = new ConfigKey("elligibilityFilterName");
        private ConfigKey ARG_ELLIGIBILITY_INLCUDE_VALUE = new ConfigKey("elligibilityFilterInclusionValue");
        private ConfigKey ARG_ELLIGIBILITY_EXLCUDE_VALUE = new ConfigKey("elligibilityFilterExclusionValue");

        private ConfigKey ARG_API_FULL_DETAIL_PER_PAGE = new ConfigKey("fullDetailPerPage", "25");
        private ConfigKey ARG_API_REF_DETAIL_PER_PAGE = new ConfigKey("refDetailPerPage", "100");

        private ConfigKey ARG_API_SOCKET_TIMEOUT = new ConfigKey("apiSocketTimeout", "0"); //TODO: review this default
        private ConfigKey ARG_API_REQUEST_DELAY = new ConfigKey("apiRequestDelay", "-1"); //TODO: review this default

        private ConfigKey ARG_MAX_XSL_THREADS = new ConfigKey("maxXslThreads", "0"); //TODO: review this default
        private ConfigKey ARG_MAX_RESOURCE_THREADS = new ConfigKey("maxResourceThreads", "0"); //TODO: review this default

        private ConfigKey ARG_MAX_FRAGMENT_FILE_SIZE = new ConfigKey("maxFragmentFileSize", "1228800"); //TODO: review this default

        private ConfigKey ARG_ZIP_FILES = new ConfigKey("zipFiles", "false"); //TODO: review this default

        //isntance fields for storage of values parsed from the Properties
        private int maxThreadsResource = -1;
        private int maxThreadsXsl = -1;

        private int maxFragmentFileSize = -1;

        private String apiEndpoint;
        private ElementsAPIVersion apiVersion;

        private String apiUsername;
        private String apiPassword;
        private boolean ignoreSSLErrors = false;
        private boolean rewriteMismatchedUrls = false;
        private ImageUtils.PhotoType imageType = null;

        private int apiSoTimeout = -1;
        private int apiRequestDelay = -1;

        private int fullDetailPerPage = -1;
        private int refDetailPerPage = -1;

        private List<ElementsItemId.GroupId> groupsToExclude;
        private List<ElementsItemId.GroupId> groupsToHarvest;
        private List<ElementsItemId.GroupId> groupsToIncludeChildrenOf;
        private List<ElementsItemId.GroupId> groupsToExcludeChildrenOf;

        private List<ElementsItemId.GroupId> groupsOfUsersToHarvest;
        private List<ElementsItemId.GroupId> groupsOfUsersToExclude;

        private List<ElementsObjectCategory> categoriesToHarvest;

        private boolean visibleLinksOnly = false;
        private boolean repullRelsToCorrectVisibility = true;
        Set<String> relTypesToReprocess;

        private boolean useFullUTF8 = false;

        private String vivoImageBasePath;
        private String vivoImageDir;
        private String xslTemplate;

        private File rawOutputDir;
        private File rdfOutputDir;
        private File tdbOutputDir;
        private File otherOutputDir;

        private boolean zipFiles = false;

        private EligibilityFilter eligibilityFilter = null;

        Map<String, String> xslParameters = null;

        //Constructor and methods
        Parser(Properties props, List<String> errors){ super(props, errors); }

        /**
         * Custom parsing utility function to extract a set of ElementsObjectCategory objects from a ConfigKey
         * ("," delimited set of string values e.g. publications, activities, etc.
         * @param configKey
         * @param tolerateNull
         * @return
         */
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

        /**
         * Custom parsing utility function to extract an ElementsAPIVersion object from the named configKey
         * @param configKey
         * @return
         */
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

        /**
         * Custom parsing utility function to extract an ImageUtils.PhotoType object from the named configKey
         * @param configKey
         * @return
         */
        private ImageUtils.PhotoType getImageType(ConfigKey configKey) {
            String key = configKey.getName();
            String value = configKey.getValue(props);

            ImageUtils.PhotoType imageType = null;
            String testValue = StringUtils.trimToNull(value);
            if(testValue != null) {
                testValue = testValue.toLowerCase();

                for (ImageUtils.PhotoType type : ImageUtils.PhotoType.values()) {
                    if (type.name().toLowerCase().equals(testValue)) {
                        imageType = type;
                        break;
                    }
                }
            }

            if(imageType == null){
                configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be a valid elements photo type : \"original\", \"photo\", \"thummbnail\" or \"none\")", key, value));
            }

            return imageType;
        }

        /**
         * Custom parsing utility function to extract an EligibilityFilter object from a specific hard coded set of
         * configKeys
         * @return
         */
        private EligibilityFilter getEligibilityScheme() {

            String schemeType = getString(ARG_ELLIGIBILITY_TYPE, true);
            String schemeName = getString(ARG_ELLIGIBILITY_NAME, true);
            String includeValue = getString(ARG_ELLIGIBILITY_INLCUDE_VALUE, true);
            String excludeValue = getString(ARG_ELLIGIBILITY_EXLCUDE_VALUE, true);

            boolean academicsOnly = getBoolean(ARG_ACADEMICS_ONLY);
            boolean currentStaffOnly = getBoolean(ARG_CURRENT_STAFF_ONLY);

            if(schemeType == null) {
                return new EligibilityFilter(academicsOnly, currentStaffOnly);
            }
            else{
                try {
                    String lowerCaseSchemeType = schemeType.toLowerCase();
                    if (lowerCaseSchemeType.equals("label-scheme"))
                        return new EligibilityFilter.LabelSchemeFilter(schemeName, includeValue, excludeValue, academicsOnly, currentStaffOnly);
                    if (lowerCaseSchemeType.equals("generic-field"))
                        return new EligibilityFilter.GenericFieldFilter(schemeName, includeValue, excludeValue, academicsOnly, currentStaffOnly);

                    configErrors.add(MessageFormat.format("\"{0}\" is not a valid elligibility type, valid values are label-scheme and generic-field", schemeType));
                }
                catch(Exception e){
                    configErrors.add(MessageFormat.format("Error instantiating Elligibility filter : {0}", e.getMessage()));
                }
            }
            return null;
        }

        /**
         * Custom parsing utility function to extract any property values with a specific naming convention
         * (starting with "xsl-param-") as parameters to be passed to the XSLT translation layer.
         * configKeys
         * @return
         */
        private Map<String, String> getXslParameters(){
            String xslPrefix = "xsl-param-";
            Map<String, String> returnMap = new HashMap<String, String>();
            for(String name : props.stringPropertyNames()){
                if(name.startsWith(xslPrefix)){
                    String key = name.replaceFirst(xslPrefix, "");
                    returnMap.put(key, props.getProperty(name).trim());
                }
            }
            return returnMap;
        }

        /**
         * main parse function that uses the parsing utility functions to extract data from the Properties object passed
         * in the constructor and populate the appropraite instance fields.
         * Note the parsing utility functions will populate the "error-list" if there are problems here.
         */
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
            for (Integer groupId : getIntegers(ARG_PARAMS_GROUPS, true)) groups.add(ElementsItemId.createGroupId(groupId));
            values.groupsToHarvest = groups;

            groups = new ArrayList<ElementsItemId.GroupId>();
            //allow null as that means exclude nothing
            for (Integer groupId : getIntegers(ARG_EXCLUDE_GROUPS, true)) groups.add(ElementsItemId.createGroupId(groupId));
            values.groupsToExclude = groups;

            groups = new ArrayList<ElementsItemId.GroupId>();
            //allow null when getting integers as that means include everything
            for (Integer groupId : getIntegers(ARG_INCLUDE_CHILD_GROUPS, true)) groups.add(ElementsItemId.createGroupId(groupId));
            values.groupsToIncludeChildrenOf = groups;

            groups = new ArrayList<ElementsItemId.GroupId>();
            //allow null when getting integers as that means include everything
            for (Integer groupId : getIntegers(ARG_EXCLUDE_CHILD_GROUPS, true)) groups.add(ElementsItemId.createGroupId(groupId));
            values.groupsToExcludeChildrenOf = groups;

            groups = new ArrayList<ElementsItemId.GroupId>();
            //allow null as that means exclude nothing
            for (Integer groupId : getIntegers(ARG_PARAMS_USER_GROUPS, true)) groups.add(ElementsItemId.createGroupId(groupId));
            values.groupsOfUsersToHarvest = groups;

            groups = new ArrayList<ElementsItemId.GroupId>();
            //allow null as that means exclude nothing
            for (Integer groupId : getIntegers(ARG_EXCLUDE_USER_GROUPS, true)) groups.add(ElementsItemId.createGroupId(groupId));
            values.groupsOfUsersToExclude = groups;

            values.categoriesToHarvest = getCategories(ARG_QUERY_CATEGORIES, false); //do not allow null for categories to query

            values.fullDetailPerPage = getInt(ARG_API_FULL_DETAIL_PER_PAGE);
            values.refDetailPerPage = getInt(ARG_API_REF_DETAIL_PER_PAGE);

            values.visibleLinksOnly = getBoolean(ARG_VISIBLE_LINKS_ONLY);
            values.repullRelsToCorrectVisibility = getBoolean(ARG_REPULL_RELS_TO_CORRECT_VISIBILITY);
            values.relTypesToReprocess = Collections.unmodifiableSet(new HashSet<String>(getStrings(ARG_RELATIONSHIP_TYPES_TO_REPROCESS, true)));

            values.useFullUTF8 = getBoolean(ARG_USE_FULL_UTF8);

            values.vivoImageDir = getString(ARG_VIVO_IMAGE_DIR, false);
            values.vivoImageBasePath = getString(ARG_VIVO_IMAGE_BASE_PATH, false);
            values.imageType = getImageType(ARG_ELEMENTS_IMAGE_TYPE);
            values.xslTemplate = getString(ARG_XSL_TEMPLATE, false);

            values.rawOutputDir = getFileDirFromConfig(ARG_RAW_OUTPUT_DIRECTORY);
            values.rdfOutputDir = getFileDirFromConfig(ARG_RDF_OUTPUT_DIRECTORY);
            values.tdbOutputDir = getFileDirFromConfig(ARG_TDB_OUTPUT_DIRECTORY);
            values.otherOutputDir = getFileDirFromConfig(ARG_OTHER_OUTPUT_DIRECTORY);

            values.ignoreSSLErrors = getBoolean(ARG_IGNORE_SSL_ERRORS);
            values.rewriteMismatchedUrls = getBoolean(ARG_REWRITE_MISMATCHED_URLS);
            values.zipFiles = getBoolean(ARG_ZIP_FILES);
            values.maxFragmentFileSize = getInt(ARG_MAX_FRAGMENT_FILE_SIZE);

            values.eligibilityFilter = getEligibilityScheme();

            values.xslParameters = getXslParameters();

        }
    }

    /**
     * Any errors encounted during parsing of the properties file.
     */
    private static List<String> configErrors = new ArrayList<String>();

    /**
     * The parser that was used to parse the config file.
     * null until the Configuration.parse static function is invoked.
     */
    private static Parser values = null;

    //getter methods to expose the parsed values from within the parser.
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

    public static List<ElementsItemId.GroupId> getGroupsToIncludeChildrenOf() {
        return values.groupsToIncludeChildrenOf;
    }

    public static List<ElementsItemId.GroupId> getGroupsToExcludeChildrenOf() {
        return values.groupsToExcludeChildrenOf;
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

    public static EligibilityFilter getElligibilityFilter(){ return values.eligibilityFilter; }

    public static Map<String, String> getXslParameters(){ return values.xslParameters; }

    public static boolean getVisibleLinksOnly() {
        return values.visibleLinksOnly;
    }

    public static boolean getShouldRepullRelsToCorrectVisibility() {
        return values.repullRelsToCorrectVisibility;
    }

    public static Set<String> getRelTypesToReprocess() {
        return values.relTypesToReprocess;
    }

    public static boolean getUseFullUTF8() {
        return values.useFullUTF8;
    }

    public static ImageUtils.PhotoType getImageType() {
        return values.imageType;
    }

    public static String getVivoImageDir() {
        return values.vivoImageDir;
    }

    public static String getVivoImageBasePath() {
        return values.vivoImageBasePath;
    }

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

    public static File getOtherOutputDir() { return values.otherOutputDir; }

    public static boolean getIgnoreSSLErrors() {
        return values.ignoreSSLErrors;
    }

    public static boolean getRewriteMismatchedUrls() { return values.rewriteMismatchedUrls; }

    public static boolean getZipFiles() {
        return values.zipFiles;
    }

    /**
     *Has the system being successfully configured?
     */
    public static boolean isConfigured() {
        return configErrors.size() == 0;
    }

    /**
     * Get a string report of the values that are in use.
     */
    public static String getConfiguredValues(){ return values == null ? null : values.reportConfiguredValues();}

    /**
     * Get a "usage" report for printing to the command line.
     * @return
     */
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

    /**
     * main methods called to initialise the Configuration class, passing in the filename of the .properties file to parse.
     * @param propertiesFileName
     * @throws IOException
     * @throws ConfigParser.UsageException
     */
    public static void parse(String propertiesFileName) throws IOException, ConfigParser.UsageException {
        InputStream stream = null;
        try {
            Properties props = ConfigParser.getPropsFromFile(propertiesFileName);
            values = new Parser(props, configErrors);
            values.parse();
        }
        catch(Exception e){
            configErrors.add(MessageFormat.format("Could not load properties file: \"{0}\"", propertiesFileName));
        }

        if (!isConfigured()) throw new ConfigParser.UsageException();
    }
}


