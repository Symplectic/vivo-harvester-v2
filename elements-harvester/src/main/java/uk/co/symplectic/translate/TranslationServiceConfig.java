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
package uk.co.symplectic.translate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class exposing the main global configuration options of the TranslationService
 * Exposed via getConfig() method on a TranslationService object
 * Users should access via the TranslationService() object
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public class TranslationServiceConfig {
    private boolean ignoreFileNotFound = false;
    private Map<String, Object> xslParameters = new HashMap<String, Object>();
    private boolean tolerateIndividualIOErrors = false;
    private boolean tolerateIndividualTransformErrors = true;
    private boolean useFullUTF8 = true;

    public boolean getIgnoreFileNotFound() {
        return ignoreFileNotFound;
    }

    public void setIgnoreFileNotFound(boolean ignoreFlag) {
        this.ignoreFileNotFound = ignoreFlag;
    }

    Map<String, Object> getXslParameters(){ return Collections.unmodifiableMap(xslParameters); }

    public void addXslParameter(String key, String value){ xslParameters.put(key, value); }

    public boolean getTolerateIndividualIOErrors() {
        return tolerateIndividualIOErrors;
    }

    public void setTolerateIndividualIOErrors(boolean tolerateIndividualIOErrors) {
        this.tolerateIndividualIOErrors = tolerateIndividualIOErrors;
    }

    public boolean getTolerateIndividualTransformErrors() {
        return tolerateIndividualTransformErrors;
    }

    public void setTolerateIndividualTransformErrors(boolean tolerateIndividualTransformErrors) {
        this.tolerateIndividualTransformErrors = tolerateIndividualTransformErrors;
    }

    public boolean getUseFullUTF8() {
        return useFullUTF8;
    }

    public void setUseFullUTF8(boolean useFullUTF8) {
        this.useFullUTF8 = useFullUTF8;
    }

}
