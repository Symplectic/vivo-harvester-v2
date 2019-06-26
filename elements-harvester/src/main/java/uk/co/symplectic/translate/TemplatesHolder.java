/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */
package uk.co.symplectic.translate;

import javax.xml.transform.Templates;
import java.io.File;

/**
 * Class to retain a thread local copy (i.e. one per thread) of the compiled "XSL templates" that represent the crosswalks.
 * Operates effectively as a thread local singleton, ensuring we only compile the XSL once per thread.
 */

public class TemplatesHolder {
    private String xslFilename;
    private ThreadLocal<Templates> myTemplates = new ThreadLocal<Templates>();

    public TemplatesHolder(String xslFilename) {
        this.xslFilename = xslFilename;
    }

    public Templates getTemplates() {
        if (myTemplates.get() == null) {
            File xslFile = new File(xslFilename);
            if (xslFile.exists()) {
                Templates template = TranslationService.compileSource(xslFile);
                myTemplates.set(template);
            } else {
                throw new IllegalStateException("XSL Translation file not found: " + xslFilename);
            }
        }
        return myTemplates.get();
    }
}
