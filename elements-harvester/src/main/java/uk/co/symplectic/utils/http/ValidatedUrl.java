/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */
package uk.co.symplectic.utils.http;

import org.apache.commons.lang.NullArgumentException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidatedUrl {
    private static final List<String> allowedInsecureSchemes = Collections.singletonList("http");
    private static final List<String> allowedSecureSchemes = Collections.singletonList("https");

    private final String url;
    private final String rewrittenUrl;
    private final boolean isSecure;
    private final boolean isMismatched;
    private boolean useRewritten = false;

    public ValidatedUrl(String url) throws URISyntaxException {
        this(url, null);
    }

    public ValidatedUrl(String url, String comparisonUrl) throws URISyntaxException {
        if (url == null) throw new NullArgumentException("url");

        URI comparisonUri = null;
        String comprisonTestHost = null;
        if(comparisonUrl != null) {
            try {
                URL comparisonUrlObj = new URL(comparisonUrl);
                comparisonUri = comparisonUrlObj.toURI();
                comprisonTestHost = comparisonUri.getHost();
            } catch (MalformedURLException mue) {
                throw new URISyntaxException(comparisonUrl, "Could not parse provided comparisonUrl");
            }
        }

        try {
            URL urlCheck = new URL(url);
            URI uriTest = urlCheck.toURI();

            String scheme = uriTest.getScheme().toLowerCase();

            List<String> validSchemes = new ArrayList<String>();
            validSchemes.addAll(allowedInsecureSchemes);
            validSchemes.addAll(allowedSecureSchemes);

            if(!validSchemes.contains(scheme)) {
                throw new URISyntaxException(url, MessageFormat.format("Invalid Scheme used in {0}, must be one of : {1}", this.getClass().getName(), String.join(", ", validSchemes)));
            }

            this.isSecure = allowedSecureSchemes.contains(scheme);
            this.isMismatched = comparisonUrl != null && !uriTest.getHost().equals(comprisonTestHost);

            if(isMismatched) {
                this.rewrittenUrl = new URI(uriTest.getScheme(), uriTest.getUserInfo(), comparisonUri.getHost(), uriTest.getPort(),
                        uriTest.getPath(), uriTest.getQuery(), uriTest.getFragment()).toURL().toString();
            }
            else {
                this.rewrittenUrl = null;
            }

            this.url = url;

        } catch (MalformedURLException mue) {
            throw new URISyntaxException(url, mue.getMessage());
        }
    }

    public String getUrl() {
        return isMismatched && useRewritten && rewrittenUrl != null ? rewrittenUrl : url;

    }

    public boolean isSecure() {
        return isSecure;
    }

    public boolean isMismatched() {
        return isMismatched;
    }

    public void useRewrittenVersion(boolean useRewritten) {
        this.useRewritten = useRewritten;
    }
}
