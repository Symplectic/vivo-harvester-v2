/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.utils.configuration;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import java.io.File;
import java.text.MessageFormat;
import java.util.*;


//Class to be extended to create a relevant parser for your tool.
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

    public ConfigParser(Properties input, List<String> errorList) {
        if (input == null) throw new NullArgumentException("input");
        if (errorList == null) throw new NullArgumentException("errorList");

        this.props = input;
        this.configErrors = errorList;
    }

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

    protected String getString(ConfigKey configKey, boolean tolerateNull) {
        String key = configKey.getName();
        String value = configKey.getValue(props);

        if (!StringUtils.isEmpty(value)) return value;
        //otherwise
        if (!tolerateNull)
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} : {1} (must be a non empty string)", key, value));
        return null;
    }


    protected File getFileDirFromConfig(ConfigKey configKey) {
        return new File(normaliseDirectoryFormat(getString(configKey, false)));
    }

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


