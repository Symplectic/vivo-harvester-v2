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
import uk.co.symplectic.elements.api.ElementsAPIVersion;
import uk.co.symplectic.utils.ImageUtils;
import uk.co.symplectic.utils.configuration.ConfigKey;
import uk.co.symplectic.utils.configuration.ConfigParser;
import uk.co.symplectic.utils.configuration.ConfigValue;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;
import uk.co.symplectic.vivoweb.harvester.utils.GroupMatcher;

import java.io.File;
import java.io.IOException;
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

        private ConfigKey ARG_PUBLIC_STAFF_ONLY = new ConfigKey("publicStaffOnly", "true"); //TODO: review this default
        private ConfigKey ARG_CURRENT_STAFF_ONLY = new ConfigKey("currentStaffOnly", "true"); //TODO: review this default
        private ConfigKey ARG_ACADEMICS_ONLY = new ConfigKey("academicsOnly", "true"); //TODO: review this default

        private ConfigKey ARG_VISIBLE_LINKS_ONLY = new ConfigKey("visibleLinksOnly", "false"); //TODO: review this default
        private ConfigKey ARG_REPULL_RELS_TO_CORRECT_VISIBILITY = new ConfigKey("repullRelsToCorrectVis", "true"); //TODO: review this default
        private ConfigKey ARG_RELATIONSHIP_TYPES_TO_REPROCESS = new ConfigKey("relTypesToReprocess", "activity-user-association, user-teaching-association, publication-user-authorship"); //TODO: review this default

        private ConfigKey ARG_USE_FULL_UTF8 = new ConfigKey("useFullUTF8", "false"); //TODO: review this default

        private ConfigKey ARG_ELEMENTS_IMAGE_TYPE = new ConfigKey("elementsImageType", "profile");
        private ConfigKey ARG_VIVO_IMAGE_DIR = new ConfigKey("vivoImageDir", "data/harvestedImages/");
        private ConfigKey ARG_VIVO_IMAGE_BASE_PATH = new ConfigKey("vivoImageBasePath", "/harvestedImages/");

        private ConfigKey ARG_QUERY_CATEGORIES = new ConfigKey("queryObjects");

        private ConfigKey ARG_INCLUDE_EMPTY_GROUPS = new ConfigKey("includeEmptyGroups", "true");

        private ConfigKey ARG_PARAMS_GROUPS = new ConfigKey("paramGroups").withAlias("includeGroups");
        private ConfigKey ARG_PARAMS_GROUP_REGEXES = new ConfigKey("paramGroupRegexes").withAlias("paramGroupNameRegexes", "includeGroupRegexes", "includeGroupNameRegexes");
        private ConfigKey ARG_PARAMS_GROUP_DESC_REGEXES = new ConfigKey("paramGroupDescriptionRegexes").withAlias("includeGroupDescriptionRegexes");

        private ConfigKey ARG_INCLUDE_CHILD_GROUPS = new ConfigKey("includeChildGroupsOf");
        private ConfigKey ARG_INCLUDE_CHILD_GROUPS_REGEXES = new ConfigKey("includeChildGroupRegexes").withAlias("includeChildGroupNameRegexes");
        private ConfigKey ARG_INCLUDE_CHILD_GROUPS_DESC_REGEXES = new ConfigKey("includeChildGroupDescriptionRegexes");


        private ConfigKey ARG_EXCLUDE_GROUPS = new ConfigKey("excludeGroups");
        private ConfigKey ARG_EXCLUDE_GROUP_REGEXES = new ConfigKey("excludeGroupRegexes").withAlias("excludeGroupNameRegexes");
        private ConfigKey ARG_EXCLUDE_GROUP_DESC_REGEXES = new ConfigKey("excludeGroupDescriptionRegexes");

        private ConfigKey ARG_EXCLUDE_CHILD_GROUPS = new ConfigKey("excludeChildGroupsOf");
        private ConfigKey ARG_EXCLUDE_CHILD_GROUPS_REGEXES = new ConfigKey("excludeChildGroupRegexes").withAlias("excludeChildGroupNameRegexes");
        private ConfigKey ARG_EXCLUDE_CHILD_GROUPS_DESC_REGEXES = new ConfigKey("excludeChildGroupDescriptionRegexes");

        private ConfigKey ARG_EXCISE_GROUPS = new ConfigKey("exciseGroups");
        private ConfigKey ARG_EXCISE_GROUP_REGEXES = new ConfigKey("exciseGroupRegexes").withAlias("exciseGroupNameRegexes");
        private ConfigKey ARG_EXCISE_GROUP_DESC_REGEXES = new ConfigKey("exciseGroupDescriptionRegexes");

        //alias used because config key was miss-spelt at one point...
        private ConfigKey ARG_EXCISE_CHILD_GROUPS = new ConfigKey("exciseChildGroupsOf").withAlias("exiseChildGroupsOf");
        private ConfigKey ARG_EXCISE_CHILD_GROUPS_REGEXES = new ConfigKey("exciseChildGroupRegexes").withAlias("exciseChildGroupNameRegexes");
        private ConfigKey ARG_EXCISE_CHILD_GROUPS_DESC_REGEXES = new ConfigKey("exciseChildGroupDescriptionRegexes");

        private ConfigKey ARG_PARAMS_USER_GROUPS = new ConfigKey("paramUserGroups").withAlias("includeUserGroups");
        private ConfigKey ARG_PARAMS_USER_GROUP_REGEXES = new ConfigKey("paramUserGroupRegexes").withAlias("paramUserGroupNameRegexes", "includeUserGroupRegexes", "includeUserGroupNameRegexes");
        private ConfigKey ARG_PARAMS_USER_GROUP_DESC_REGEXES = new ConfigKey("paramUserGroupDescriptionRegexes").withAlias("includeUserGroupDescriptionRegexes");

        private ConfigKey ARG_EXCLUDE_USER_GROUPS = new ConfigKey("excludeUserGroups");
        private ConfigKey ARG_EXCLUDE_USER_GROUP_REGEXES = new ConfigKey("excludeUserGroupRegexes").withAlias("excludeUserGroupNameRegexes");
        private ConfigKey ARG_EXCLUDE_USER_GROUP_DESC_REGEXES = new ConfigKey("excludeUserGroupDescriptionRegexes");

        //aliases used because config keys were miss-spelt at one point...
        private ConfigKey ARG_ELIGIBILITY_TYPE = new ConfigKey("eligibilityFilterType").withAlias("elligibilityFilterType");
        private ConfigKey ARG_ELIGIBILITY_NAME = new ConfigKey("eligibilityFilterName").withAlias("elligibilityFilterName");
        private ConfigKey ARG_ELIGIBILITY_INCLUDE_VALUE = new ConfigKey("eligibilityFilterInclusionValue").withAlias("elligibilityFilterInclusionValue");
        private ConfigKey ARG_ELIGIBILITY_EXCLUDE_VALUE = new ConfigKey("eligibilityFilterExclusionValue").withAlias("elligibilityFilterExclusionValue");

        private ConfigKey ARG_API_FULL_DETAIL_PER_PAGE = new ConfigKey("fullDetailPerPage", "25");
        private ConfigKey ARG_API_REF_DETAIL_PER_PAGE = new ConfigKey("refDetailPerPage", "100");

        private ConfigKey ARG_API_SOCKET_TIMEOUT = new ConfigKey("apiSocketTimeout", "0"); //TODO: review this default
        private ConfigKey ARG_API_REQUEST_DELAY = new ConfigKey("apiRequestDelay", "-1"); //TODO: review this default

        private ConfigKey ARG_MAX_XSL_THREADS = new ConfigKey("maxXslThreads", "0"); //TODO: review this default
        private ConfigKey ARG_MAX_RESOURCE_THREADS = new ConfigKey("maxResourceThreads", "0"); //TODO: review this default

        private ConfigKey ARG_MAX_FRAGMENT_FILE_SIZE = new ConfigKey("maxFragmentFileSize", "1228800"); //TODO: review this default

        private ConfigKey ARG_ZIP_FILES = new ConfigKey("zipFiles", "false"); //TODO: review this default

        private ConfigKey ARG_CHANGE_PROTECTION_ENABLED = new ConfigKey("changeProtectionEnabled", "true"); //TODO: review this default
        private ConfigKey ARG_ALLOWED_USER_CHANGE_FRACTION = new ConfigKey("allowedUserChangeFraction", "0.2"); //TODO: review this default
        private ConfigKey ARG_ALLOWED_NON_USER_CHANGE_FRACTION= new ConfigKey("allowedNonUserChangeFraction", "0.3"); //TODO: review this default

        //instance fields for storage of values parsed from the Properties
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

        boolean includeEmptyGroups = true;

        private GroupMatcher groupsToHarvestMatcher = null;
        private GroupMatcher groupsToIncludeChildrenOfMatcher = null;
        private GroupMatcher groupsToExcludeMatcher = null;
        private GroupMatcher groupsToExcludeChildrenOfMatcher = null;
        private GroupMatcher groupsToExciseMatcher = null;
        private GroupMatcher groupsToExciseChildrenOfMatcher = null;

        private GroupMatcher groupsOfUsersToHarvestMatcher = null;
        private GroupMatcher groupsOfUsersToExcludeMatcher = null;

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

        private boolean changeProtectionEnabled = true;
        private double allowedUserChangeFraction;
        private double allowedNonUserChangeFraction;

        private EligibilityFilter eligibilityFilter = null;

        Map<String, String> xslParameters = null;

        //Constructor and methods
        Parser(Properties props, List<String> errors){ super(props, errors); }

        /**
         * Custom parsing utility function to extract a set of ElementsObjectCategory objects from a ConfigKey
         * ("," delimited set of string values e.g. publications, activities, etc.
         * @param configKey The Key to be parsed.
         * @param tolerateNull whether parsing no value is acceptable.
         * @return whether parsing no value is acceptable.
         */
        @SuppressWarnings("SameParameterValue")
        private List<ElementsObjectCategory> getCategories(ConfigKey configKey, boolean tolerateNull) {
            ConfigValue confValue = configKey.getValue(props);
            String value = confValue.getReadValue();
            List<ElementsObjectCategory> categories = new ArrayList<ElementsObjectCategory>();
            if (!StringUtils.isEmpty(value)) {
                for (String categoryString : value.split("\\s*,\\s*")) {
                    try {
                        ElementsObjectCategory cat = ElementsObjectCategory.valueOf(categoryString);
                        categories.add(cat);
                    }
                    catch(IndexOutOfBoundsException e){
                        configErrors.add(MessageFormat.format("Invalid value ({0}) provided within argument {1} (every value must represent a valid Elements Category)", categoryString, confValue));
                    }
                }
            } else if(!tolerateNull) {
                configErrors.add(MessageFormat.format("Invalid value provided within argument {0} (must supply at least one Elements Category)", confValue));
            }
            return categories;
        }

        /**
         * Custom parsing utility function to extract an ElementsAPIVersion object from the named configKey
         * @param configKey The Key to be parsed.
         * @return the parsed ElementsAPIVersion value (can legitimately be null)
         */
        private ElementsAPIVersion getApiVersion(ConfigKey configKey) {
            ConfigValue confValue = configKey.getValue(props);
            String value = confValue.getReadValue();
            try {
                if(value != null) {
                    return ElementsAPIVersion.parse(value);
                }
            } catch (IllegalStateException e) {
                configErrors.add(MessageFormat.format("Invalid value provided for argument {0} (must be a valid elements api version)", confValue));
            }
            return null;
        }

        /**
         * Custom parsing utility function to extract an ImageUtils.PhotoType object from the named configKey
         * @param configKey The Key to be parsed.
         * @return the parsed PhotoType enum value. (cannot be null)
         */
        private ImageUtils.PhotoType getImageType(ConfigKey configKey) {
            ConfigValue confValue = configKey.getValue(props);
            String value = confValue.getReadValue();

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
                configErrors.add(MessageFormat.format("Invalid value provided for argument {0} (must be a valid elements photo type : \"original\", \"photo\", \"thumbnail\" or \"none\")", confValue));
            }

            return imageType;
        }


        private GroupMatcher getGroupMatcher(ConfigKey groupIdsKey, ConfigKey nameRegexesKey, ConfigKey descRegexesKey) {
            List<ElementsItemId.GroupId> groupIds = new ArrayList<ElementsItemId.GroupId>();
            //allow null as that means exclude nothing
            if(groupIdsKey != null) {
                for (Integer groupId : getIntegers(groupIdsKey, true))
                    groupIds.add(ElementsItemId.createGroupId(groupId));
            }

            Set<String> groupNameRegexes = null;
            if(nameRegexesKey != null) {
                //allow null input as that means exclude nothing
                groupNameRegexes = new HashSet<String>(getStringsFromCSVFragment(nameRegexesKey, true));
            }

            Set<String> groupDescriptionRegexes = null;
            if(descRegexesKey != null) {
                //allow null input as that means exclude nothing
                groupDescriptionRegexes = new HashSet<String>(getStringsFromCSVFragment(descRegexesKey, true));
            }

            //parsing of sets above will have created appropriate errors if necessary...
            return new GroupMatcher(groupIds, groupNameRegexes, groupDescriptionRegexes);
        }

        /**
         * Custom parsing utility function to extract an EligibilityFilter object from a specific hard coded set of keys
         * (ARG_ELIGIBILITY_TYPE, ARG_ELIGIBILITY_NAME, ARG_ELIGIBILITY_INCLUDE_VALUE, ARG_ELIGIBILITY_EXCLUDE_VALUE)
         * @return the parsed EligibilityFilter object constructed from the keys (can legitimately be null)
         */
        private EligibilityFilter getEligibilityScheme() {

            String schemeType = getString(ARG_ELIGIBILITY_TYPE, true);
            String schemeName = getString(ARG_ELIGIBILITY_NAME, true);
            String includeValue = getString(ARG_ELIGIBILITY_INCLUDE_VALUE, true);
            String excludeValue = getString(ARG_ELIGIBILITY_EXCLUDE_VALUE, true);

            boolean publicStaffOnly = getBoolean(ARG_PUBLIC_STAFF_ONLY);
            boolean academicsOnly = getBoolean(ARG_ACADEMICS_ONLY);
            boolean currentStaffOnly = getBoolean(ARG_CURRENT_STAFF_ONLY);


            if(schemeType == null) {
                return new EligibilityFilter(academicsOnly, currentStaffOnly, publicStaffOnly);
            }
            else{
                try {
                    String lowerCaseSchemeType = schemeType.toLowerCase();
                    if (lowerCaseSchemeType.equals("label-scheme"))
                        return new EligibilityFilter.LabelSchemeFilter(schemeName, includeValue, excludeValue, academicsOnly, currentStaffOnly, publicStaffOnly);
                    if (lowerCaseSchemeType.equals("generic-field"))
                        return new EligibilityFilter.GenericFieldFilter(schemeName, includeValue, excludeValue, academicsOnly, currentStaffOnly, publicStaffOnly);

                    configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : \"{1}\", valid values are label-scheme and generic-field", ARG_ELIGIBILITY_TYPE.getName(), schemeType));
                }
                catch(Exception e){
                    configErrors.add(MessageFormat.format("Error instantiating Eligibility filter : {0}", e.getMessage()));
                }
            }
            return null;
        }

        /**
         * Custom parsing utility function to extract any property values with a specific naming convention
         * (starting with "xsl-param-") as parameters to be passed to the XSLT translation layer.
         * configKeys
         * @return a Map<String, String> of xsl parameter name-value pairs.
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
         * in the constructor and populate the appropriate instance fields.
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

            values.groupsToHarvestMatcher = getGroupMatcher(ARG_PARAMS_GROUPS, ARG_PARAMS_GROUP_REGEXES, ARG_PARAMS_GROUP_DESC_REGEXES);
            values.groupsToIncludeChildrenOfMatcher = getGroupMatcher(ARG_INCLUDE_CHILD_GROUPS, ARG_INCLUDE_CHILD_GROUPS_REGEXES, ARG_INCLUDE_CHILD_GROUPS_DESC_REGEXES);

            values.groupsToExcludeMatcher = getGroupMatcher(ARG_EXCLUDE_GROUPS, ARG_EXCLUDE_GROUP_REGEXES, ARG_EXCLUDE_GROUP_DESC_REGEXES);
            values.groupsToExcludeChildrenOfMatcher = getGroupMatcher(ARG_EXCLUDE_CHILD_GROUPS, ARG_EXCLUDE_CHILD_GROUPS_REGEXES, ARG_EXCLUDE_CHILD_GROUPS_DESC_REGEXES);

            values.groupsToExciseMatcher = getGroupMatcher(ARG_EXCISE_GROUPS, ARG_EXCISE_GROUP_REGEXES, ARG_EXCISE_GROUP_DESC_REGEXES);
            values.groupsToExciseChildrenOfMatcher = getGroupMatcher(ARG_EXCISE_CHILD_GROUPS, ARG_EXCISE_CHILD_GROUPS_REGEXES, ARG_EXCISE_CHILD_GROUPS_DESC_REGEXES);

            values.groupsOfUsersToHarvestMatcher = getGroupMatcher(ARG_PARAMS_USER_GROUPS, ARG_PARAMS_USER_GROUP_REGEXES, ARG_PARAMS_USER_GROUP_DESC_REGEXES);
            values.groupsOfUsersToExcludeMatcher = getGroupMatcher(ARG_EXCLUDE_USER_GROUPS, ARG_EXCLUDE_USER_GROUP_REGEXES, ARG_EXCLUDE_USER_GROUP_DESC_REGEXES);

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

            values.changeProtectionEnabled = getBoolean(ARG_CHANGE_PROTECTION_ENABLED);
            values.allowedUserChangeFraction = getDouble(ARG_ALLOWED_USER_CHANGE_FRACTION, (double) 0 ,(double) 1);
            values.allowedNonUserChangeFraction = getDouble(ARG_ALLOWED_NON_USER_CHANGE_FRACTION, (double) 0 ,(double) 1);

            values.includeEmptyGroups = getBoolean(ARG_INCLUDE_EMPTY_GROUPS);
        }
    }

    /**
     * Any errors encountered during parsing of the properties file.
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

    public static boolean getIncludeEmptyGroups() {
        return values.includeEmptyGroups;
    }

    public static GroupMatcher getGroupsToHarvestMatcher() {
        return values.groupsToHarvestMatcher;
    }

    public static GroupMatcher getGroupsToIncludeChildrenOfMatcher() {
        return values.groupsToIncludeChildrenOfMatcher;
    }

    public static GroupMatcher getGroupsToExcludeMatcher() {
        return values.groupsToExcludeMatcher;
    }

    public static GroupMatcher getGroupsToExcludeChildrenOfMatcher() {
        return values.groupsToExcludeChildrenOfMatcher;
    }

    public static GroupMatcher getGroupsToExciseMatcher() {
        return values.groupsToExciseMatcher;
    }

    public static GroupMatcher getGroupsToExciseChildrenOfMatcher() {
        return values.groupsToExciseChildrenOfMatcher;
    }

    public static GroupMatcher getGroupsOfUsersToHarvestMatcher() {
        return values.groupsOfUsersToHarvestMatcher;
    }

    public static GroupMatcher getGroupsOfUsersToExcludeMatcher() {
        return values.groupsOfUsersToExcludeMatcher;
    }

    public static List<ElementsObjectCategory> getCategoriesToHarvest() {
        return values.categoriesToHarvest;
    }

    public static EligibilityFilter getEligibilityFilter(){ return values.eligibilityFilter; }

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

    public static boolean getChangeProtectionEnabled() {
        return values.changeProtectionEnabled;
    }

    public static double getAllowedUserChangeFraction() { return values.allowedUserChangeFraction; }

    public static double getAllowedNonUserChangeFraction() {
        return values.allowedNonUserChangeFraction;
    }

    /**
     *Has the system being successfully configured?
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isConfigured() {
        return configErrors.size() == 0;
    }

    /**
     * Get a string report of the values that are in use.
     */
    public static String getConfiguredValues(){ return values == null ? null : values.reportConfiguredValues();}

    /**
     * Get a "usage" report for printing to the command line.
     * @return a String representing how the application is configured, and any config parsing errors.
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
     * @param propertiesFileName the name of the properties file from which config should be parsed
     * @throws IOException if cannot access the requested properties file
     * @throws ConfigParser.UsageException if file cannot be parsed into config without errors
     */
    @SuppressWarnings("RedundantThrows")
    public static void parse(String propertiesFileName) throws IOException, ConfigParser.UsageException {
        try {
            Properties props = ConfigParser.getPropsFromFile(propertiesFileName);
            values = new Parser(props, configErrors);
            values.parse();
        }
        catch(Exception e){
            configErrors.add(MessageFormat.format("Could not parse properties file: \"{0}\" : {1}", propertiesFileName, e.getMessage()));
        }
        if (!isConfigured()) throw new ConfigParser.UsageException();
    }
}


