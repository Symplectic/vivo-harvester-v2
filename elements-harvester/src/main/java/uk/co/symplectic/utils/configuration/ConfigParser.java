/*
 * ******************************************************************************
 *   Copyright (c) 2019 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */

package uk.co.symplectic.utils.configuration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;

/**
Class representing the idea of a a utility for parsing config out of .properties files based on configured ConfigKeys.
 provides a set of utility functions designed to extract typed data based on ConfigKeys.
This abstract class should be extended to create a relevant parser for your tool.
 */
@SuppressWarnings("SameParameterValue")
public abstract class ConfigParser {

    final private static Logger log = LoggerFactory.getLogger(ConfigParser.class);

    @SuppressWarnings("WeakerAccess")
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
     * @param input the "Properties" object to be parsed into Config data
     * @param errorList a List<string> that will be used by the parser to log any errors that occur whilst parsing
     */
    protected ConfigParser(Properties input, List<String> errorList) {
        if (input == null) throw new NullArgumentException("input");
        if (errorList == null) throw new NullArgumentException("errorList");

        this.props = input;
        this.configErrors = errorList;
    }

    /**
     * returns a report about all the configured values being used for all the ConfigKeys that exist in this app.
     */
    public String reportConfiguredValues(){
        if(props == null) return null;
        StringBuilder configuredValuesBuilder = new StringBuilder("config file args: ");
        int count = 0;
        for(ConfigKey key : ConfigKey.keys){
            if(count > 0 ) configuredValuesBuilder.append(", ");
            configuredValuesBuilder.append(key.getValue(props).toShortString());
            count++;
        }
        return configuredValuesBuilder.toString();
    }

    //parsing utility functions

    /**
     * method to extract a set of Strings from the named configKey (delimited by ",")
     * @param configKey The Key to be parsed.
     * @param tolerateNull whether parsing no value is acceptable.
     * @return The parsed List of String values List<String>.
     */
    protected List<String> getStrings(ConfigKey configKey, boolean tolerateNull) {
        List<String> detectedValues = new ArrayList<String>();
        ConfigValue confValue = configKey.getValue(props);
        String value = confValue.getReadValue();
        if (!StringUtils.isEmpty(value)) {
            for (String splitValue : value.split("\\s*,\\s*")) {
                String trimmedSplitValue = StringUtils.trimToNull(splitValue);
                if (trimmedSplitValue != null)
                    detectedValues.add(trimmedSplitValue);
            }
        }

        if (!tolerateNull && detectedValues.size() == 0) {
            configErrors.add(MessageFormat.format("Invalid value provided within argument {0} (must supply at least one string value)", confValue));
        }
        return detectedValues;
    }

    /**
     * method to extract a set of Strings from the named configKey (delimited by ",") where the key is formatted as
     * a line in a CSV document would be - to allow possibility of complex characters and reuse of , in values.
     * @param configKey The Key to be parsed.
     * @param tolerateNull whether parsing no value is acceptable.
     * @return The parsed List of String values as a List<String>.
     */
    protected List<String> getStringsFromCSVFragment(ConfigKey configKey, boolean tolerateNull) {

        List<String> detectedValues = new ArrayList<String>();
        ConfigValue confValue = configKey.getValue(props);
        String value = confValue.getReadValue();
        if (!StringUtils.isEmpty(value)) {
            try {
                CSVParser parser = CSVParser.parse(value, CSVFormat.RFC4180.withIgnoreSurroundingSpaces(true));
                //noinspection LoopStatementThatDoesntLoop
                for(CSVRecord record : parser){
                    for(String parsedValue : record){
                        detectedValues.add(parsedValue);
                    }
                    //Only parse one record as we are dealing with a single line from the properties file..
                    break;
                }
            }
            catch(IOException e){
                configErrors.add(MessageFormat.format("Invalid value provided for argument {0} (could not be parsed as a CSV fragment)", confValue));
                return detectedValues;
            }
        }

        if (!tolerateNull && detectedValues.size() == 0) {
            configErrors.add(MessageFormat.format("Invalid value provided within argument {0} (must supply at least one string value)", confValue));
        }
        return detectedValues;
    }

    /**
     * method to extract a set of Integers from the named configKey (delimited by ",")
     * @param configKey The Key to be parsed.
     * @param tolerateNull whether parsing no value is acceptable.
     * @return The parsed List of Integer values List<Integer>.
     */
    protected List<Integer> getIntegers(ConfigKey configKey, boolean tolerateNull) {
        List<Integer> integers = new ArrayList<Integer>();
        ConfigValue confValue = configKey.getValue(props);
        String value = confValue.getReadValue();
        if (!StringUtils.isEmpty(value)) {
            for (String intString : value.split("\\s*,\\s*")) {
                try {
                    int anInt = Integer.parseInt(intString);
                    integers.add(anInt);
                } catch (NumberFormatException e) {
                    configErrors.add(MessageFormat.format("Invalid value ({0}) provided within argument {1} (every value must represent a valid integer)", intString, confValue));
                }
            }
        } else if (!tolerateNull) {
            configErrors.add(MessageFormat.format("Invalid value provided within argument {0} (must supply at least one integer)", confValue));
        }
        return integers;
    }

    /**
     * method to extract a boolean from the named configKey.
     * retrieving null is not acceptable and will result in an error being logged in the list.
     * @param configKey The Key to be parsed as a boolean
     * @return the parsed boolean.
     */
    protected boolean getBoolean(ConfigKey configKey) {
        ConfigValue confValue = configKey.getValue(props);
        String value = confValue.getReadValue();

        if ("false".equalsIgnoreCase(value)) {
            return false;
        } else if ("true".equalsIgnoreCase(value)) {
            return true;
        } else {
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} (must be \"true\" or \"false\")", confValue));
            return false;
        }
    }

    /**
     * method to extract an integer from the named configKey.
     * retrieving null is acceptable and will result in -1.
     * @param configKey The Key to be parsed as an int.
     * @return the parsed int.
     */
    protected int getInt(ConfigKey configKey) {
        ConfigValue confValue = configKey.getValue(props);
        String value = confValue.getReadValue();
        try {
            if (value != null) {
                return Integer.parseInt(value, 10);

            }
        } catch (NumberFormatException nfe) {
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} (must be an integer)", confValue));
        }
        return -1;
    }

    /**
     * method to extract a double precision floating point number from the named configKey.
     * retrieving null is not acceptable and will result in an error being logged in the list.
     * @param configKey The Key to be parsed as a double.
     * @return the parsed double.
     */
    @SuppressWarnings("unused")
    protected double getDouble(ConfigKey configKey){
        return getDouble(configKey, null, null);
    }

    /**
     * method to extract a double precision floating point number from the named configKey.
     * value must be between the provided parameter values -inclusive- (if they are not null)
     * retrieving null is not acceptable and will result in an error being logged in the list.
     * @param configKey The Key to be parsed as a double.
     * @param minValue minimum acceptable value (can be null for no limit).
     * @param maxValue maximum acceptable value (can be null for no limit).
     * @return the parsed double.
     */
    protected double getDouble(ConfigKey configKey, Double minValue, Double maxValue) {
        ConfigValue confValue = configKey.getValue(props);
        String value = confValue.getReadValue();
        try {
            double aDouble = Double.parseDouble(value);
            if(minValue != null && aDouble < minValue){
                configErrors.add(MessageFormat.format("Invalid value provided for argument {0} (must be larger than {1})", confValue, minValue));
                return 0;
            }
            if(maxValue != null && aDouble > maxValue){
                configErrors.add(MessageFormat.format("Invalid value provided for argument {0} (must be less than {1})", confValue, maxValue));
                return 0;
            }
            return aDouble;

        } catch (NumberFormatException nfe) {
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} (must be a floating point value)", confValue));
        }
        catch (NullPointerException npe) {
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} (must supply a floating point value)", confValue));
        }
        return 0;
    }

    /**
     * method to extract a string from the named configKey.
     * @param configKey The Key to be parsed as a string
     * @param tolerateNull whether parsing no value is acceptable.
     * @return The parsed string.
     */
    protected String getString(ConfigKey configKey, boolean tolerateNull) {
        ConfigValue confValue = configKey.getValue(props);
        String value = confValue.getReadValue();

        if (!StringUtils.isEmpty(value)) return value;
        //otherwise
        if (!tolerateNull)
            configErrors.add(MessageFormat.format("Invalid value provided for argument {0} (must be a non empty string)", confValue));
        return null;
    }


    /**
     * method to extract a File object representing a directory from the named configKey.
     * @param configKey The Key to be parsed as file/dir name and turned into a "File" object.
     * @return The parsed File object.
     */
    protected File getFileDirFromConfig(ConfigKey configKey) {
        return new File(normaliseDirectoryFormat(getString(configKey, false)));
    }

    /**
     * helper method to try and ensure that directory paths are correctly formatted.
     * @param directoryName string that should represent a "path" will be cleansed of trailing \ or / characters
     * @return the cleansed string.
     */
    @SuppressWarnings("WeakerAccess")
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


    /**
     * Method to return a "Properties" object from a named file.
     * Will look for a corresponding "developer-xxxxx" file to override the main config and load that if one is present
     * Otherwise looks for the named file in the classpath and loads it into a Properties object
     * @param propertiesFileName the name of the file to look for.
     * @return The loaded "Properties" object.
     * @throws IOException if errors occur loading the file
     */
    public static Properties getPropsFromFile(String propertiesFileName) throws IOException {
        InputStream stream = null;
        try {
            stream = ConfigParser.class.getClassLoader().getResourceAsStream("developer-" + propertiesFileName);
            if(stream != null){
                log.warn(MessageFormat.format("Using developer config file : {0}", "developer-" + propertiesFileName));
            }
            else {
                stream = ConfigParser.class.getClassLoader().getResourceAsStream(propertiesFileName);
            }
            Properties props = new Properties();
            props.load(stream);
            return props;
        } finally {
            if (stream != null) stream.close();
        }
    }
}


