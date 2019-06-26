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

package uk.co.symplectic.translate;

import org.w3c.dom.Document;

/**
 * Interface representing the concept of an XML "Document" that may need to be provided as a parameter to an XSL translation.
 * If a class implementing this interface is provided as a parameter to the TranslationService as a parameter (e.g. in the <String, Object> array)
 * Then the TranslationService will (immediately before translation) call into getDocument() to retrieve the actual XML document to pass into the XSLT engine
 * against the parameter name.
 *
 * Exists to minimise memory consumption of enqueued translations by not loading XML documents needed as input params until the translation is about to occur
 */

public interface TranslationDocumentProvider {
    Document getDocument();
}
