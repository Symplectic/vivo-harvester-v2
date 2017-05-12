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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ConfigKey{

    static Set<ConfigKey> keys = new HashSet<ConfigKey>();

    private static synchronized void addKey(ConfigKey key) {
        keys.add(key);
    }

    // non static section
    private String name;
    private String defaultValue;

    public String getName(){ return name; }

    public ConfigKey(String name) { this(name, null); }
    public ConfigKey(String name, String defaultValue){
        if(name == null) throw new NullArgumentException("name");
        this.name = name;
        this.defaultValue = defaultValue;
        addKey(this);
    }

    public String getValue(Properties props){
        String value = props == null ? null : StringUtils.trimToNull(props.getProperty(name));
        return value == null ? defaultValue : value;
    }
}
