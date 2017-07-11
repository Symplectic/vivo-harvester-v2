/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.utils.configuration;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import java.io.File;
import java.text.MessageFormat;
import java.util.*;

/**
Class representing the idea of a a utility for parsing config out of .properties files based on configured ConfigKeys.
 provides a set of utility functions designed to extract typed data based on ConfigKeys.
This abstract class should be extended to create a relevant parser for your tool.
 */
public abstract class ConfigParser {

    public static class UsageException extends Exception {
        public UsageException() {
            this(null);
        }

        public UsageException(Throwable innerException) {
            super(innerException);
        }
    }

    protected Properties props;
    protected List<String> configErrors;

    /**
     * Constructor for a parser that accepts the properties file being processed and an errorList
     * where any issues encountered by the parsing utility functions will be listed.
     * @param input
     * @param errorList
     */
    public ConfigParser(Properties input, List<String> errorList) {
        if (input == null) throw new NullArgumentException("input");
        if (errorList == null) throw new NullArgumentException("errorList");

        this.props = input;
        this.configErrors = errorList;
    }

    /**
     * returns a report about all the configured values being used for all the ConfigKeys that exist in this app.
     * @return
     */
    public String reportConfiguredValues(){
        if(props == null) return null;
        StringBuilder configuredValuesBuilder = new StringBuilder("config file args: ");
        int count = 0;
        for(ConfigKey key : ConfigKey.keys){
            if(count > 0 ) configuredValuesBuilder.append(", ");
            configuredValuesBuilder.append(MessageFormat.format("{0}:{1}", key.getName(), key.getValue(props)));
            count++;
        }
        return configuredValuesBuilder.toString();
    }

    //parsing utility functions

    /**
     * method to extract a set of Strings from the named configKey (delimited by ",")
     * @param configKey
     * @param tolerateNull whether parsing no value is acceptable.
     * @return
     */
    protected List<String> getStrings(ConfigKey configKey, boolean tolerateNull) {
        String key = configKey.getName();
        List<String> detectedValues = new ArrayList<String>();
        String value = configKey.getValue(props);
        if (!StringUtils.isEmpty(value)) {
            for (String splitValue : value.split("\\s*,\\s*")) {
                String trimmedSplitValue = StringUtils.trimToNull(splitValue);
                if (trimmedSplitValue != null)
                    detectedValues.add(trimmedSplitValue);
            }
        }

        if (!tolerateNull && detectedValues.size() == 0) {
            configErrors.add(MessageFormat.format("Invalid value provided within argument {0} : {1} (must supply at least one string value)", key, value));
        }
        return detectedValues;
    }

    /**
     * method to extract a set of Integers from the named configKey (delimited by ",")
     * @param configKey
     * @param tolerateNull whether parsing no value is acceptable
     * @return
     */
    protected List<Integer> getIntegers(ConfigKey configKey, boolean tolerateNull) {
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
        } else if (!tolerateNull) {
            configErrors.add(MessageFormat.format("Invalid value provided within argument {0} : {1} (must supply at least one integer)", key, value));
        }
        return integers;
    }

    /**
     * method to extract a boolean from the named configKey.
     * retrieving null is not acceptable and will reult in an error being logged in the list.
     * @param configKey
     * @return
     */
    protected boolean getBoolean(ConfigKey configKey) {
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

    /**
     * method to extract an integer from the named configKey.
     * retrieving null is not acceptable and will reult in an error being logged in the list.
     * @param configKey
     * @return
     */
    protected int getInt(ConfigKey configKey) {
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

    /**
     * method to extract a string from the named configKey.
     * @param configKey
     * @param tolerateNull whether parsing no value is acceptable
     * @return
     */
    protected String getString(ConfigKey configKey, boolean tolerateNull) {
        String key = configKey.getName();
        String value = configKey.getValue(props);

        if (!StringUtils.isEmpty(value)) return value;
        //otherwise
        if (!tolerateNull)
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be a non empty string)", key, value));
        return null;
    }


    /**
     * method to extract a File object representing a directory from the named configKey.
     * @param configKey
     * @return
     */
    protected File getFileDirFromConfig(ConfigKey configKey) {
        return new File(normaliseDirectoryFormat(getString(configKey, false)));
    }

    /**
     * helper method to try and ensure that directory paths are correctly formatted.
     * @param directoryName
     * @return
     */
    protected String normaliseDirectoryFormat(String directoryName) {
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
}


