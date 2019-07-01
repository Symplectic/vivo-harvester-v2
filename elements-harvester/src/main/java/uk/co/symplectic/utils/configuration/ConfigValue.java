/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 *   Version :  ${git.branch}:${git.commit.id}
 * ******************************************************************************
 */

package uk.co.symplectic.utils.configuration;

import org.apache.commons.lang.NullArgumentException;

import java.text.MessageFormat;

/**
 * Class representing the raw string "value" read (or defaulted) from a Properties object, along with info about the
 * source where the value was read from..
 */
public class ConfigValue {
    private final String value;
    private final String sourceDescriptor;

    ConfigValue(String sourceDescriptor, String value){
        if (sourceDescriptor == null) throw new NullArgumentException("sourceDescriptor");
        this.value = value;
        this.sourceDescriptor = sourceDescriptor;
    }

    public String getReadValue(){ return value; }
    @SuppressWarnings("unused")
    public String getSourceDescriptor(){return sourceDescriptor; }


    @Override
    public String toString(){
        return MessageFormat.format("{0} : \"{1}\"", sourceDescriptor, value);
    }

    public String toShortString(){
        return MessageFormat.format("{0}:{1}", sourceDescriptor, value);
    }
}
