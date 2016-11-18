/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.config;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.vivoweb.harvester.util.args.UsageException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.co.symplectic.elements.api.ElementsAPIVersion;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;
import uk.co.symplectic.vivoweb.harvester.model.ElementsObjectCategory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class Configuration {
    private static final String ARG_RAW_OUTPUT_DIRECTORY = "rawOutput";
    private static final String ARG_RDF_OUTPUT_DIRECTORY = "rdfOutput";

    private static final String ARG_XSL_TEMPLATE = "xslTemplate";

    private static final String ARG_ELEMENTS_API_ENDPOINT = "apiEndpoint";
    private static final String ARG_ELEMENTS_API_VERSION = "apiVersion";
    private static final String ARG_ELEMENTS_API_USERNAME = "apiUsername";
    private static final String ARG_ELEMENTS_API_PASSWORD = "apiPassword";

    private static final String ARG_CURRENT_STAFF_ONLY = "currentStaffOnly";
    private static final String ARG_VISIBLE_LINKS_ONLY = "visibleLinksOnly";

    private static final String ARG_USE_FULL_UTF8 = "useFullUTF8";

    private static final String ARG_VIVO_IMAGE_DIR = "vivoImageDir";
    private static final String ARG_VIVO_BASE_URI = "vivoBaseURI";

    private static final String ARG_API_QUERY_CATEGORIES = "queryObjects"; //TODO : rename this input param?
    private static final String ARG_API_PARAMS_GROUPS = "paramGroups";
    private static final String ARG_API_EXCLUDE_GROUPS = "excludeGroups";

    private static final String ARG_API_OBJECTS_PER_PAGE = "objectsPerPage";
    private static final String ARG_API_RELS_PER_PAGE = "relationshipsPerPage";

    private static final String ARG_API_SOCKET_TIMEOUT = "apiSocketTimeout";
    private static final String ARG_API_REQUEST_DELAY = "apiRequestDelay";

    private static final String ARG_MAX_XSL_THREADS = "maxXslThreads";
    private static final String ARG_MAX_RESOURCE_THREADS = "maxResourceThreads";

    private static final String ARG_IGNORE_SSL_ERRORS = "ignoreSSLErrors";
    private static final String ARG_ZIP_FILES = "zipFiles";

    // Maximum of 25 is mandated by 4.6 and newer APIs since we request full detail for objects
    private static final int OBJECTS_PER_PAGE = 25;

    // Default of 100 for optimal performance
    private static final int RELATIONSHIPS_PER_PAGE = 100;

    private static final String DEFAULT_IMAGE_DIR = "/Library/Tomcat/webapps/vivo";
    private static final String DEFAULT_BASE_URI = "http://localhost:8080/vivo/individual/";

    private static final String DEFAULT_RAW_OUTPUT_DIR = "data/raw-records/";
    private static final String DEFAULT_RDF_OUTPUT_DIR = "data/translated-records/";

    private static ArgParser parser = null;
    private static ArgList argList = null;

    private static class ConfigurationValues {
        private int maxThreadsResource = 0;
        private int maxThreadsXsl = 0;

        private String apiEndpoint;
        private ElementsAPIVersion apiVersion;

        private String apiUsername;
        private String apiPassword;

        private int apiSoTimeout = 0;
        private int apiRequestDelay = -1;

        private int apiObjectsPerPage = OBJECTS_PER_PAGE;
        private int apiRelationshipsPerPage = RELATIONSHIPS_PER_PAGE;

        private List<ElementsItemId.GroupId> groupsToExclude;
        private List<ElementsItemId.GroupId> groupsToHarvest;
        private List<ElementsObjectCategory> categoriesToHarvest;

        private boolean currentStaffOnly = true;
        private boolean visibleLinksOnly = false;

        private boolean useFullUTF8 = false;

        private String vivoImageDir = DEFAULT_IMAGE_DIR;
        private String baseURI = DEFAULT_BASE_URI;
        private String xslTemplate;

        private String rawOutputDir = DEFAULT_RAW_OUTPUT_DIR;
        private String rdfOutputDir = DEFAULT_RDF_OUTPUT_DIR;

        private boolean ignoreSSLErrors = false;

        private boolean zipFiles = false;

    }

    ;

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

    public static int getApiSoTimeout() {
        return values.apiSoTimeout;
    }

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

    public static String getRawOutputDir() {
        return values.rawOutputDir;
    }

    public static String getRdfOutputDir() {
        return values.rdfOutputDir;
    }

    public static boolean getIgnoreSSLErrors() {
        return values.ignoreSSLErrors;
    }

    public static boolean getZipFiles() {
        return values.zipFiles;
    }


    //Has the system being successfully configured to move forwards?
    public static boolean isConfigured() {
        return argList == null ? false : configErrors.size() == 0;
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

        if (parser != null) {
            if (configErrors.size() != 0) builder.appendln("");
            builder.appendln("Usage printed below - if using an XML config file (-X) you must use the long name  (without --) as the XML Element name");
            builder.append(parser.getUsage());
        }
        if (builder.length() != 0) return builder.toString();
        return "Error generating usage string";
    }

    //list to contain any configuration errors
    private static List<String> configErrors = new ArrayList<String>();

    public static void parse(String appName, String[] args) throws IOException, UsageException {
        argList = null;
        parser = new ArgParser(appName);
        parser.addArgument(new ArgDef().setShortOption('r').setLongOpt(ARG_RAW_OUTPUT_DIRECTORY).setDescription("Raw RecordHandler config file path").withParameter(true, "SYSTEM PATH"));
        parser.addArgument(new ArgDef().setShortOption('t').setLongOpt(ARG_RDF_OUTPUT_DIRECTORY).setDescription("Translated RecordHandler config file path").withParameter(true, "SYSTEM PATH"));

        parser.addArgument(new ArgDef().setShortOption('e').setLongOpt(ARG_ELEMENTS_API_ENDPOINT).setDescription("Elements API endpoint url").withParameter(true, "URL STRING"));
        parser.addArgument(new ArgDef().setShortOption('v').setLongOpt(ARG_ELEMENTS_API_VERSION).setDescription("Elements API version").withParameter(true, "API VERSION STRING"));
        parser.addArgument(new ArgDef().setShortOption('u').setLongOpt(ARG_ELEMENTS_API_USERNAME).setDescription("Elements API username").withParameter(true, "STRING"));
        parser.addArgument(new ArgDef().setShortOption('p').setLongOpt(ARG_ELEMENTS_API_PASSWORD).setDescription("Elements API password").withParameter(true, "STRING"));

        parser.addArgument(new ArgDef().setShortOption('g').setLongOpt(ARG_API_PARAMS_GROUPS).setDescription("Elements' IDs of Groups to restrict queries to").withParameter(true, "COMMA SEPARATED INTEGERS"));
        parser.addArgument(new ArgDef().setShortOption('g').setLongOpt(ARG_API_EXCLUDE_GROUPS).setDescription("Elements' IDs of Groups to exclude users of").withParameter(true, "COMMA SEPARATED INTEGERS"));
        parser.addArgument(new ArgDef().setShortOption('c').setLongOpt(ARG_API_QUERY_CATEGORIES).setDescription("Elements object categories to include").withParameter(true, "COMMA SEPARATED NAMES"));
        parser.addArgument(new ArgDef().setLongOpt(ARG_CURRENT_STAFF_ONLY).setDescription("Include only current staff?").withParameter(true, "BOOLEAN"));
        parser.addArgument(new ArgDef().setLongOpt(ARG_VISIBLE_LINKS_ONLY).setDescription("Include only visible links?").withParameter(true, "BOOLEAN"));

        parser.addArgument(new ArgDef().setLongOpt(ARG_USE_FULL_UTF8).setDescription("Use full UTF8").withParameter(true, "BOOLEAN"));

        parser.addArgument(new ArgDef().setLongOpt(ARG_VIVO_IMAGE_DIR).setDescription("Vivo image directory (where output image files should be located)").withParameter(true, "SYSTEM PATH"));
        parser.addArgument(new ArgDef().setLongOpt(ARG_VIVO_BASE_URI).setDescription("Vivo Base URI (as used within the targetted Vivo system)").withParameter(true, "URI STRING"));

        parser.addArgument(new ArgDef().setLongOpt(ARG_API_OBJECTS_PER_PAGE).setDescription("Objects Per Page").withParameter(true, "INTEGER"));
        parser.addArgument(new ArgDef().setLongOpt(ARG_API_RELS_PER_PAGE).setDescription("Relationships Per Page").withParameter(true, "INTEGER"));

        parser.addArgument(new ArgDef().setLongOpt(ARG_API_SOCKET_TIMEOUT).setDescription("HTTP Socket Timeout").withParameter(true, "INTEGER"));
        parser.addArgument(new ArgDef().setLongOpt(ARG_API_REQUEST_DELAY).setDescription("API Request Delay").withParameter(true, "INTEGER"));

        parser.addArgument(new ArgDef().setLongOpt(ARG_MAX_XSL_THREADS).setDescription("Maximum number of Threads to use for the XSL Translation").withParameter(true, "INTEGER"));
        parser.addArgument(new ArgDef().setLongOpt(ARG_MAX_RESOURCE_THREADS).setDescription("Maximum number of Threads to use for the Resource (photo) downloads").withParameter(true, "INTEGER"));

        parser.addArgument(new ArgDef().setLongOpt(ARG_IGNORE_SSL_ERRORS).setDescription("Ignore SSL Errors").withParameter(true, "BOOLEAN"));
        parser.addArgument(new ArgDef().setLongOpt(ARG_ZIP_FILES).setDescription("GZip intermediate output ").withParameter(true, "BOOLEAN"));

        parser.addArgument(new ArgDef().setShortOption('z').setLongOpt(ARG_XSL_TEMPLATE).setDescription("XSL Template entry point").withParameter(true, "SYSTEM FILE"));

        InitLog.initLogger(args, parser);
        argList = parser.parse(args);

        if (argList != null) {
            values.maxThreadsResource = getInt(ARG_MAX_RESOURCE_THREADS, 0);
            values.maxThreadsXsl = getInt(ARG_MAX_XSL_THREADS, 0);

            values.apiEndpoint = getString(ARG_ELEMENTS_API_ENDPOINT, false);
            values.apiVersion = getApiVersion(ARG_ELEMENTS_API_VERSION);

            values.apiUsername = getString(ARG_ELEMENTS_API_USERNAME, true); //allow null as may be a plain http endpoint
            values.apiPassword = getString(ARG_ELEMENTS_API_PASSWORD, true); //allow null as may be a plain http endpoint

            values.apiSoTimeout = getInt(ARG_API_SOCKET_TIMEOUT, 0);
            values.apiRequestDelay = getInt(ARG_API_REQUEST_DELAY, -1);


            List<ElementsItemId.GroupId> groupsToHarvest = new ArrayList<ElementsItemId.GroupId>();
            //allow null when getting integers as that means include everything
            for (Integer groupId : getIntegers(ARG_API_PARAMS_GROUPS, true)) groupsToHarvest.add(ElementsItemId.createGroupId(groupId));
            values.groupsToHarvest = groupsToHarvest;

            List<ElementsItemId.GroupId> groupsToExclude = new ArrayList<ElementsItemId.GroupId>();
            //allow null as that means exclude nothing
            for (Integer groupId : getIntegers(ARG_API_EXCLUDE_GROUPS, true)) groupsToExclude.add(ElementsItemId.createGroupId(groupId));
            values.groupsToExclude = groupsToExclude;

            values.categoriesToHarvest = getCategories(ARG_API_QUERY_CATEGORIES, false); //do not allow null for categories to query

            values.apiObjectsPerPage = getInt(ARG_API_OBJECTS_PER_PAGE, OBJECTS_PER_PAGE);
            values.apiRelationshipsPerPage = getInt(ARG_API_RELS_PER_PAGE, RELATIONSHIPS_PER_PAGE);

            values.currentStaffOnly = getBoolean(ARG_CURRENT_STAFF_ONLY, true);
            values.visibleLinksOnly = getBoolean(ARG_VISIBLE_LINKS_ONLY, false);

            values.useFullUTF8 = getBoolean(ARG_USE_FULL_UTF8, false);

            values.baseURI = getString(ARG_VIVO_BASE_URI, DEFAULT_BASE_URI, false);
            values.vivoImageDir = getString(ARG_VIVO_IMAGE_DIR, DEFAULT_IMAGE_DIR, false);
            values.xslTemplate = getString(ARG_XSL_TEMPLATE, false);

            values.rawOutputDir = getFileDirFromConfig(ARG_RAW_OUTPUT_DIRECTORY, DEFAULT_RAW_OUTPUT_DIR);
            values.rdfOutputDir = getFileDirFromConfig(ARG_RDF_OUTPUT_DIRECTORY, DEFAULT_RDF_OUTPUT_DIR);

            values.ignoreSSLErrors = getBoolean(ARG_IGNORE_SSL_ERRORS, false);
            values.zipFiles = getBoolean(ARG_ZIP_FILES, false);
        }
        if (!isConfigured()) throw new UsageException();
    }

    private static List<ElementsObjectCategory> getCategories(String key, boolean tolerateNull) {
        List<ElementsObjectCategory> categories = new ArrayList<ElementsObjectCategory>();
        String catList = argList.get(key);
        if (!StringUtils.isEmpty(catList)) {
            for (String category : catList.split("\\s*,\\s*")) {
                if (category == null) {
                    configErrors.add(MessageFormat.format("Invalid value ({0}) provided within argument {1} : {2} (every value must represent a valid Elements Category)", category, key, argList.get(key)));
                }
                categories.add(ElementsObjectCategory.valueOf(category));
            }
        } else if(!tolerateNull) {
            configErrors.add(MessageFormat.format("Invalid value provided within argument {0} : {1} (must supply at least one Elements Category)", key, argList.get(key)));
        }
        return categories;
    }

    private static List<Integer> getIntegers(String key, boolean tolerateNull) {
        List<Integer> integers = new ArrayList<Integer>();
        String intList = argList.get(key);
        if (!StringUtils.isEmpty(intList)) {
            for (String intString : intList.split("\\s*,\\s*")) {
                try {
                    int anInt = Integer.parseInt(intString);
                    integers.add(anInt);
                } catch (NumberFormatException e) {
                    configErrors.add(MessageFormat.format("Invalid value ({0}) provided within argument {1} : {2} (every value must represent a valid integer)", intString, key, argList.get(key)));
                }
            }
        }
        else if(!tolerateNull) {
            configErrors.add(MessageFormat.format("Invalid value provided within argument {0} : {1} (must supply at least one integer)", key, argList.get(key)));
        }
        return integers;
    }

    private static boolean getBoolean(String key, Boolean defValue) {
        String value = argList.get(key);
        if ("false".equalsIgnoreCase(value)) {
            return false;
        } else if ("true".equalsIgnoreCase(value)) {
            return true;
        } else {
            if (defValue != null) return defValue.booleanValue();
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be \"true\" or \"false\")", key, argList.get(key)));
            return false;
        }
    }

    private static int getInt(String key, Integer defValue) {
        try {
            String value = argList.get(key);
            if (value != null) {
                return Integer.parseInt(value, 10);

            }
        } catch (NumberFormatException nfe) {
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be an integer)", key, argList.get(key)));
        }
        if(defValue != null) return defValue.intValue();
        return -1;
    }

    private static String getString(String key, boolean tolerateNull) {
        return getString(key, null, tolerateNull);
    }

    private static String getString(String key, String defValue, boolean tolerateNull) {
        String str = argList.get(key);
        if (!StringUtils.isEmpty(str)) return str;
        //otherwise
        if (!StringUtils.isEmpty(defValue)) return defValue;
        //otherwise
        if(!tolerateNull)
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be a non empty string)", key, argList.get(key)));
        return null;
    }

    private static ElementsAPIVersion getApiVersion(String key) throws UsageException {
        try {
            return ElementsAPIVersion.parse(argList.get(key));
        } catch (IllegalStateException e) {
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be a valid elements api version)", key, argList.get(key)));
        }
        return null;
    }

    private static String getFileDirFromConfig(String key, String defValue) {
        String fileDir= null;
        try {
            fileDir = getRawFileDirFromConfig(key, defValue);
        }catch(Exception e){
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must point to a parseable RecordHandler config file)", key, argList.get(key)));
            return null;
        }

        if (!StringUtils.isEmpty(fileDir)) {
            if (fileDir.contains("/")) {
                if (!fileDir.endsWith("/")) {
                    fileDir = fileDir + "/";
                }
            } else if (fileDir.contains("\\")) {
                if (!fileDir.endsWith("\\")) {
                    fileDir = fileDir + "\\";
                }
            } else {
                if (!fileDir.endsWith(File.separator)) {
                    fileDir = fileDir + File.separator;
                }
            }
        } else {
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be able to extract a fileDir from the config file)", key, argList.get(key)));
        }
        return fileDir;
    }

    private static String getRawFileDirFromConfig(String key, String defValue) throws SAXException, ParserConfigurationException, IOException {
        String filename = argList.get(key);
        try {
            if (filename != null) {
                File file = new File(filename);
                if (file.exists()) {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    DocumentBuilder db = dbf.newDocumentBuilder();

                    Document doc = db.parse(file);
                    if (doc != null) {
                        doc.getDocumentElement().normalize();
                        NodeList nodes = doc.getDocumentElement().getChildNodes();
                        for (int i = 0; i < nodes.getLength(); i++) {
                            Node node = nodes.item(i);
                            if (node.hasAttributes()) {
                                Node nameAttr = node.getAttributes().getNamedItem("name");
                                if (nameAttr != null && "fileDir".equals(nameAttr.getTextContent())) {
                                    return node.getTextContent();
                                }
                            }
                        }
                    }
                }
            }
        } catch (SAXException e) {
            throw e;
        } catch (ParserConfigurationException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
        return defValue;
    }
}

