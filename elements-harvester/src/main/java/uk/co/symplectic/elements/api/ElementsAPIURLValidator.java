/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.elements.api;

import org.apache.commons.lang.StringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ElementsAPIURLValidator {
    private static final List<String> allowedInsecureSchemes = Arrays.asList(new String[]{"http"});
    private static final List<String> allowedSecureSchemes = Arrays.asList(new String[]{"https"});

    private String url;
    private boolean isSecure = false;
    private boolean isMismatched = false;

    ElementsAPIURLValidator(String url) throws URISyntaxException {
        this(url, null);
    }

    ElementsAPIURLValidator(String url, String comparisonUrl) throws URISyntaxException {
        this.url = url;
        validate(comparisonUrl);
    }

    private void validate(String comparisonUrl) throws URISyntaxException {

        String comprisonTestHost = null;
        if(comparisonUrl != null) {
            try {
                URL comparisonUrlObj = new URL(comparisonUrl);
                URI comparisonUri = comparisonUrlObj.toURI();
                comprisonTestHost = comparisonUri.getHost();
            } catch (MalformedURLException mue) {
                throw new URISyntaxException(comparisonUrl, "Could not parse provided comparisonUrl");
            }
        }

        try {
            if (url == null) throw new URISyntaxException(url, "URL must not be null");

            URL urlCheck = new URL(url);
            URI uriTest = urlCheck.toURI();

            String scheme = uriTest.getScheme().toLowerCase();

            List<String> validSchemes = new ArrayList<String>();
            validSchemes.addAll(allowedInsecureSchemes);
            validSchemes.addAll(allowedSecureSchemes);

            if(!validSchemes.contains(scheme)) {
                throw new URISyntaxException(url, MessageFormat.format("Invalid Scheme used in {0}, must be one of : {1}", this.getClass().getName(), String.join(", ", validSchemes)));
            }

            if(allowedSecureSchemes.contains(scheme)) { isSecure = true; }
            if(!uriTest.getHost().equals(comprisonTestHost)){ isMismatched = true; }

        } catch (MalformedURLException mue) {
            throw new URISyntaxException(url, mue.getMessage());
        }
    }

    public boolean urlIsSecure() {
        return isSecure;
    }

    public boolean urlIsMismatched() {
        return isMismatched;
    }
}
