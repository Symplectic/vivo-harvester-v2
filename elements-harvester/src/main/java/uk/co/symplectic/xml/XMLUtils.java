/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */
package uk.co.symplectic.xml;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

public final class XMLUtils {

    private static QName idAttributeName = new QName("id");
    private static QName categoryAttributeName = new QName("category");
    private static QName usernameAtributeName = new QName("username");

    private XMLUtils() {}

    public static String getAttributeValueOrNull(StartElement startElement, QName name){
        Attribute attr = startElement.getAttributeByName(name);
        return attr == null ? null : attr.getValue();
    }

    public static String getId(StartElement startElement) {
        return getAttributeValueOrNull(startElement, idAttributeName);
    }
}
