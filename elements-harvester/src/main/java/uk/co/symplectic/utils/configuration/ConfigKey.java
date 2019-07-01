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

import java.text.MessageFormat;
import java.util.*;

/**
 * Class representing a "key" within a Properties object that has a name and optionally a default value.
 */
public class ConfigKey{

    static class InvalidKeyConfiguration extends RuntimeException{
        InvalidKeyConfiguration(String duplicateValue) {
            super(MessageFormat.format("\"{0}\" is used by multiple ConfigKeys as a name or alias", duplicateValue));
        }
    }


    static Set<ConfigKey> keys = new HashSet<ConfigKey>();
    private static Set<String> keyNames = new HashSet<String>();

    private static synchronized void addKey(ConfigKey configKey) {
        keys.add(configKey);
        //check we are not adding a duplicate key name..
        if(!keyNames.add(configKey.name)){
            throw new InvalidKeyConfiguration(configKey.name);
        }
    }

    // non static section
    private String name;
    //private String lastReadFrom;
    //private boolean defaulted = false;

    //user set to ensure unique aliases within here, but try to preserve order too.
    private Set<String> aliases = new LinkedHashSet<String>();
    private String defaultValue;

    public String getName(){return name;}


    //fluent setter to provide for easy alias configuration
    public ConfigKey withAlias(String ... aliases) {
        for(String alias : aliases) {
            String potentialAlias = StringUtils.trimToNull(alias);
            if(potentialAlias != null) this.aliases.add(potentialAlias);
            if(!keyNames.add(potentialAlias)){
                throw new InvalidKeyConfiguration(potentialAlias);
            }
        }
        return this;
    }

    public ConfigKey(String name) { this(name, null); }
    public ConfigKey(String name, String defaultValue){
        if(name == null) throw new NullArgumentException("name");
        this.name = name;
        this.defaultValue = defaultValue;
        addKey(this);
    }

    /**
     * The getValue method extracts the value associated with this ConfigKey from the passed in Properties object
     * it will return the default (if this ConfigKey has one) if the Properties file has no value for this ConfigKey.
     * @param props the Properties object to extract this Key's value from
     * @return the key's value
     */
    public ConfigValue getValue(Properties props) {
        String aliasReadFrom = null;
        String readValue = getRawValueFromProps(name, props);
        //if we don't yet have a value - look at any aliases..
        if (readValue == null) {
            for (String alias : aliases) {
                //Note alias will not be null (by construction guards)
                readValue = getRawValueFromProps(alias, props);
                // if we got something log the source and break out of this loop.
                if (readValue != null) {
                    aliasReadFrom = alias;
                    break;
                }
            }
        }

        //if we still don't have a value apply the default if one is set.
        if(readValue == null && defaultValue != null){
            readValue = defaultValue;
            aliasReadFrom = "default-value";
        }

        //whether readValue is still null or not we return the value..
        return new ConfigValue(getDescriptor(aliasReadFrom), readValue);
    }

    private String getRawValueFromProps(String key, Properties props){
        return  props == null ? null : StringUtils.trimToNull(props.getProperty(key));
    }

    private String getDescriptor(String readFrom){
        String suffix = readFrom == null ? "" : MessageFormat.format("({0})", readFrom);
        return MessageFormat.format("{0}{1}", name, suffix);
    }
}
