/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */
package uk.co.symplectic.vivoweb.harvester.config;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.elements.api.ElementsAPIVersion;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Configuration {

    public static class UsageException extends Exception {
        public UsageException() {
        }
    }

    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

//    // Maximum of 25 is mandated by 4.6 and newer APIs since we request full detail for objects
//    private static final int OBJECTS_PER_PAGE = 25;
//
//    // Default of 100 for optimal performance
//    private static final int RELATIONSHIPS_PER_PAGE = 100;
//
//    private static final String DEFAULT_IMAGE_DIR = "/Library/Tomcat/webapps/vivo";
//    private static final String DEFAULT_BASE_URI = "http://localhost:8080/vivo/individual/";
//
//    private static final String DEFAULT_RAW_OUTPUT_DIR = "../data/raw-records/";
//    private static final String DEFAULT_RDF_OUTPUT_DIR = "../data/translated-records/";
//    private static final String DEFAULT_TDB_OUTPUT_DIR = "../previous-harvest/";


    enum ConfigKeys{

        ARG_RAW_OUTPUT_DIRECTORY("rawOutput", "../data/raw-records/"),
        ARG_RDF_OUTPUT_DIRECTORY("rdfOutput", "../data/translated-records/"),
        ARG_TDB_OUTPUT_DIRECTORY("tdbOutput", "../previous-harvest/"),

        ARG_XSL_TEMPLATE("xslTemplate"),

        ARG_ELEMENTS_API_ENDPOINT("apiEndpoint"),
        ARG_ELEMENTS_API_VERSION("apiVersion"),
        ARG_ELEMENTS_API_USERNAME("apiUsername"),
        ARG_ELEMENTS_API_PASSWORD("apiPassword"),

        ARG_CURRENT_STAFF_ONLY("currentStaffOnly", "true"), //TODO: review this default
        ARG_VISIBLE_LINKS_ONLY("visibleLinksOnly", "false"), //TODO: review this default

        ARG_USE_FULL_UTF8("useFullUTF8", "false"), //TODO: review this default

        ARG_VIVO_IMAGE_DIR("vivoImageDir", "/Library/Tomcat/webapps/vivo"),
        ARG_VIVO_BASE_URI("vivoBaseURI", "http://localhost:8080/vivo/individual/"),

        ARG_API_QUERY_CATEGORIES("queryObjects"), //TODO : rename this input param?
        ARG_API_PARAMS_GROUPS("paramGroups"),
        ARG_API_EXCLUDE_GROUPS("excludeGroups"),

        ARG_API_OBJECTS_PER_PAGE("objectsPerPage", "25"),
        ARG_API_RELS_PER_PAGE("relationshipsPerPage", "100"),

        ARG_API_SOCKET_TIMEOUT("apiSocketTimeout", "0"), //TODO: review this default
        ARG_API_REQUEST_DELAY("apiRequestDelay", "-1"), //TODO: review this default

        ARG_MAX_XSL_THREADS("maxXslThreads", "0"), //TODO: review this default
        ARG_MAX_RESOURCE_THREADS("maxResourceThreads", "0"), //TODO: review this default

        ARG_IGNORE_SSL_ERRORS("ignoreSSLErrors", "false"), //TODO: review this default
        ARG_ZIP_FILES("zipFiles", "false"); //TODO: review this default

        private String name;
        private String defaultValue;

        String getName(){ return name; }
        String getValue(Properties props){
            String value = StringUtils.trimToNull(props.getProperty(name));
            return value == null ? defaultValue : value;
        }

        ConfigKeys(String name) { this(name, null); }
        ConfigKeys(String name, String defaultValue){
            if(name == null) throw new NullArgumentException("name");
            this.name = name;
            this.defaultValue = defaultValue;
        }
    }

//    private static final String ARG_RAW_OUTPUT_DIRECTORY = "rawOutput";
//    private static final String ARG_RDF_OUTPUT_DIRECTORY = "rdfOutput";
//    private static final String ARG_TDB_OUTPUT_DIRECTORY = "tdbOutput";
//
//    private static final String ARG_XSL_TEMPLATE = "xslTemplate";
//
//    private static final String ARG_ELEMENTS_API_ENDPOINT = "apiEndpoint";
//    private static final String ARG_ELEMENTS_API_VERSION = "apiVersion";
//    private static final String ARG_ELEMENTS_API_USERNAME = "apiUsername";
//    private static final String ARG_ELEMENTS_API_PASSWORD = "apiPassword";
//
//    private static final String ARG_CURRENT_STAFF_ONLY = "currentStaffOnly";
//    private static final String ARG_VISIBLE_LINKS_ONLY = "visibleLinksOnly";
//
//    private static final String ARG_USE_FULL_UTF8 = "useFullUTF8";
//
//    private static final String ARG_VIVO_IMAGE_DIR = "vivoImageDir";
//    private static final String ARG_VIVO_BASE_URI = "vivoBaseURI";
//
//    private static final String ARG_API_QUERY_CATEGORIES = "queryObjects"; //TODO : rename this input param?
//    private static final String ARG_API_PARAMS_GROUPS = "paramGroups";
//    private static final String ARG_API_EXCLUDE_GROUPS = "excludeGroups";
//
//    private static final String ARG_API_OBJECTS_PER_PAGE = "objectsPerPage";
//    private static final String ARG_API_RELS_PER_PAGE = "relationshipsPerPage";
//
//    private static final String ARG_API_SOCKET_TIMEOUT = "apiSocketTimeout";
//    private static final String ARG_API_REQUEST_DELAY = "apiRequestDelay";
//
//    private static final String ARG_MAX_XSL_THREADS = "maxXslThreads";
//    private static final String ARG_MAX_RESOURCE_THREADS = "maxResourceThreads";
//
//    private static final String ARG_IGNORE_SSL_ERRORS = "ignoreSSLErrors";
//    private static final String ARG_ZIP_FILES = "zipFiles";
//

    private static Properties props = new Properties();

    private static class ConfigurationValues {
        private int maxThreadsResource = -1;
        private int maxThreadsXsl = -1;

        private String apiEndpoint;
        private ElementsAPIVersion apiVersion;

        private String apiUsername;
        private String apiPassword;

        private int apiSoTimeout = -1;
        private int apiRequestDelay = -1;

        private int apiObjectsPerPage = -1;
        private int apiRelationshipsPerPage = -1;

        private List<ElementsItemId.GroupId> groupsToExclude;
        private List<ElementsItemId.GroupId> groupsToHarvest;
        private List<ElementsObjectCategory> categoriesToHarvest;

        private boolean currentStaffOnly = true;
        private boolean visibleLinksOnly = false;

        private boolean useFullUTF8 = false;

        private String vivoImageDir;
        private String baseURI;
        private String xslTemplate;

        private File rawOutputDir;
        private File rdfOutputDir;
        private File tdbOutputDir;

        private boolean ignoreSSLErrors = false;

        private boolean zipFiles = false;
    }

    private static ConfigurationValues values = new ConfigurationValues();

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

    public static int getApiObjectsPerPage() {
        return values.apiObjectsPerPage;
    }

    public static int getApiRelationshipsPerPage() {
        return values.apiRelationshipsPerPage;
    }

    public static List<ElementsItemId.GroupId> getGroupsToExclude() {
        return values.groupsToExclude;
    }

    public static List<ElementsItemId.GroupId> getGroupsToHarvest() {
        return values.groupsToHarvest;
    }

    public static List<ElementsObjectCategory> getCategoriesToHarvest() {
        return values.categoriesToHarvest;
    }

    public static boolean getCurrentStaffOnly() {
        return values.currentStaffOnly;
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

    public static String getBaseURI() {
        return values.baseURI;
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

    public static boolean getIgnoreSSLErrors() {
        return values.ignoreSSLErrors;
    }

    public static boolean getZipFiles() {
        return values.zipFiles;
    }

    //Has the system being successfully configured to move forwards?
    public static boolean isConfigured() {
        return configErrors.size() == 0;
    }

    public static String getUsage() {
        StrBuilder builder = new StrBuilder();
        if (configErrors.size() != 0) {
            builder.appendln("Errors detected in supplied configuration values: ");
            for (String error : configErrors) {
                builder.append("\t");
                builder.appendln(error);
            }
        }

        //TODO: create usage string?
//        if (false) {
//            if (configErrors.size() != 0) builder.appendln("");
//            builder.appendln("Usage printed below - if using an XML config file (-X) you must use the long name  (without --) as the XML Element name");
//            builder.append(parser.getUsage());
//        }

        builder.appendln(reportUsage());

        if (builder.length() != 0) return builder.toString();
        return "Error generating usage string";
    }

    private static String reportUsage(){
        StringBuilder configuredValuesBuilder = new StringBuilder("config file args: ");
        int count = 0;
        for(ConfigKeys key : ConfigKeys.values()){
            if(count > 0 ) configuredValuesBuilder.append(", ");
            configuredValuesBuilder.append(MessageFormat.format("{0}:{1}", key.getName(), key.getValue(props)));
            count++;
        }
        return configuredValuesBuilder.toString();
    }

    //list to contain any configuration errors
    private static List<String> configErrors = new ArrayList<String>();

    public static void parse(String appName, String[] args) throws IOException, UsageException {
        //rely on defulat logback initialisation..
        //InitLog.initLogger(args, parser);
        props.load(new FileInputStream("elements.config.properties"));

        values.maxThreadsResource = getInt(ConfigKeys.ARG_MAX_RESOURCE_THREADS);
        values.maxThreadsXsl = getInt(ConfigKeys.ARG_MAX_XSL_THREADS);

        values.apiEndpoint = getString(ConfigKeys.ARG_ELEMENTS_API_ENDPOINT, false);
        values.apiVersion = getApiVersion(ConfigKeys.ARG_ELEMENTS_API_VERSION);

        values.apiUsername = getString(ConfigKeys.ARG_ELEMENTS_API_USERNAME, true); //allow null as may be a plain http endpoint
        values.apiPassword = getString(ConfigKeys.ARG_ELEMENTS_API_PASSWORD, true); //allow null as may be a plain http endpoint

        values.apiSoTimeout = getInt(ConfigKeys.ARG_API_SOCKET_TIMEOUT);
        values.apiRequestDelay = getInt(ConfigKeys.ARG_API_REQUEST_DELAY);

        List<ElementsItemId.GroupId> groupsToHarvest = new ArrayList<ElementsItemId.GroupId>();
        //allow null when getting integers as that means include everything
        for (Integer groupId : getIntegers(ConfigKeys.ARG_API_PARAMS_GROUPS, true)) groupsToHarvest.add(ElementsItemId.createGroupId(groupId));
        values.groupsToHarvest = groupsToHarvest;

        List<ElementsItemId.GroupId> groupsToExclude = new ArrayList<ElementsItemId.GroupId>();
        //allow null as that means exclude nothing
        for (Integer groupId : getIntegers(ConfigKeys.ARG_API_EXCLUDE_GROUPS, true)) groupsToExclude.add(ElementsItemId.createGroupId(groupId));
        values.groupsToExclude = groupsToExclude;

        values.categoriesToHarvest = getCategories(ConfigKeys.ARG_API_QUERY_CATEGORIES, false); //do not allow null for categories to query

        values.apiObjectsPerPage = getInt(ConfigKeys.ARG_API_OBJECTS_PER_PAGE);
        values.apiRelationshipsPerPage = getInt(ConfigKeys.ARG_API_RELS_PER_PAGE);

        values.currentStaffOnly = getBoolean(ConfigKeys.ARG_CURRENT_STAFF_ONLY);
        values.visibleLinksOnly = getBoolean(ConfigKeys.ARG_VISIBLE_LINKS_ONLY);

        values.useFullUTF8 = getBoolean(ConfigKeys.ARG_USE_FULL_UTF8);

        values.baseURI = getString(ConfigKeys.ARG_VIVO_BASE_URI, false);
        values.vivoImageDir = getString(ConfigKeys.ARG_VIVO_IMAGE_DIR, false);
        values.xslTemplate = getString(ConfigKeys.ARG_XSL_TEMPLATE, false);

        values.rawOutputDir = getFileDirFromConfig(ConfigKeys.ARG_RAW_OUTPUT_DIRECTORY);
        values.rdfOutputDir = getFileDirFromConfig(ConfigKeys.ARG_RDF_OUTPUT_DIRECTORY);
        values.tdbOutputDir = getFileDirFromConfig(ConfigKeys.ARG_TDB_OUTPUT_DIRECTORY);

        values.ignoreSSLErrors = getBoolean(ConfigKeys.ARG_IGNORE_SSL_ERRORS);
        values.zipFiles = getBoolean(ConfigKeys.ARG_ZIP_FILES);

        if (!isConfigured()) throw new UsageException();
        log.info("running ElementsFetchAndTranslate");
        log.info(reportUsage());
    }

    //private static List<ElementsObjectCategory> getCategories(String key, boolean tolerateNull) {
    private static List<ElementsObjectCategory> getCategories(ConfigKeys configKey, boolean tolerateNull) {
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

    //private static List<Integer> getIntegers(String key, boolean tolerateNull) {
    private static List<Integer> getIntegers(ConfigKeys configKey, boolean tolerateNull) {
        List<Integer> integers = new ArrayList<Integer>();
        String key = configKey.getName();
        String value = configKey.getValue(props);
        if (!StringUtils.isEmpty(value)) {
            for (String intString : value.split("\\s*,\\s*")) {
                try {
                    int anInt = Integer.parseInt(intString);
                    integers.add(anInt);
                } catch (NumberFormatException e) {
                    configErrors.add(MessageFormat.format("Invalid value ({0}) provided within argument {1} : {2} (every value must represent a valid integer)", intString, key, value));
                }
            }
        }
        else if(!tolerateNull) {
            configErrors.add(MessageFormat.format("Invalid value provided within argument {0} : {1} (must supply at least one integer)", key, value));
        }
        return integers;
    }

    //private static boolean getBoolean(String key, Boolean defValue) {
    private static boolean getBoolean(ConfigKeys configKey) {
        String key = configKey.getName();
        String value = configKey.getValue(props);

        if ("false".equalsIgnoreCase(value)) {
            return false;
        } else if ("true".equalsIgnoreCase(value)) {
            return true;
        } else {
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be \"true\" or \"false\")", key, value));
            return false;
        }
    }


    //private static int getInt(String key, Integer defValue) {
    private static int getInt(ConfigKeys configKey) {
        String key = configKey.getName();
        String value = configKey.getValue(props);
        try {
            if (value != null) {
                return Integer.parseInt(value, 10);

            }
        } catch (NumberFormatException nfe) {
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be an integer)", key, value));
        }
        return -1;
    }

//      private static String getString(String key, boolean tolerateNull) {
//        return getString(key, null, tolerateNull);
//    }

    //private static String getString(String key, String defValue, boolean tolerateNull) {
    private static String getString(ConfigKeys configKey, boolean tolerateNull) {
        String key = configKey.getName();
        String value = configKey.getValue(props);

        if (!StringUtils.isEmpty(value)) return value;
        //otherwise
        if(!tolerateNull)
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be a non empty string)", key, value));
        return null;
    }

//  private static ElementsAPIVersion getApiVersion(String key) throws UsageException {
    private static ElementsAPIVersion getApiVersion(ConfigKeys configKey) {
        String key = configKey.getName();
        String value = configKey.getValue(props);
        try {
            return ElementsAPIVersion.parse(value);
        } catch (IllegalStateException e) {
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be a valid elements api version)", key, value));
        }
        return null;
    }

    //private static File getFileDirFromConfig(String key, String defValue) {
    private static File getFileDirFromConfig(ConfigKeys configKey) {
        return new File(normaliseDirectoryFormat(getString(configKey, false)));
    }

//    private static String getFileDirFromConfig(String key, String defValue) {
//        String fileDir;
//        try {
//            fileDir = getRawFileDirFromConfig(key, defValue);
//        }catch(Exception e){
//            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must point to a parseable RecordHandler config file)", key, argList.get(key)));
//            return null;
//        }
//        if (!StringUtils.isEmpty(fileDir)) {
//            fileDir = normaliseDirectoryFormat(fileDir);
//        } else {
//            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be able to extract a fileDir from the config file)", key, argList.get(key)));
//        }
//        return fileDir;
//    }

    private static String normaliseDirectoryFormat(String directoryName){
        if (directoryName.contains("/")) {
            if (!directoryName.endsWith("/")) {
                return directoryName + "/";
            }
        } else if (directoryName.contains("\\")) {
            if (!directoryName.endsWith("\\")) {
                return directoryName + "\\";
            }
        } else {
            if (!directoryName.endsWith(File.separator)) {
                return directoryName + File.separator;
            }
        }
        return directoryName;
    }

//    private static String getRawFileDirFromConfig(String key, String defValue) throws SAXException, ParserConfigurationException, IOException {
//        String filename = argList.get(key);
//        if (filename != null) {
//            File file = new File(filename);
//            if (file.exists()) {
//                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//                DocumentBuilder db = dbf.newDocumentBuilder();
//
//                Document doc = db.parse(file);
//                if (doc != null) {
//                    doc.getDocumentElement().normalize();
//                    NodeList nodes = doc.getDocumentElement().getChildNodes();
//                    for (int i = 0; i < nodes.getLength(); i++) {
//                        Node node = nodes.item(i);
//                        if (node.hasAttributes()) {
//                            Node nameAttr = node.getAttributes().getNamedItem("name");
//                            if (nameAttr != null && "fileDir".equals(nameAttr.getTextContent())) {
//                                return node.getTextContent();
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return defValue;
//    }
}

