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

package uk.co.symplectic.utils.xml;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

/**
 * Class to simplify use of StAX Factories in the application by wrapping them as thread safe singletons
 */

public final class StAXUtils {
    private static XMLEventFactory xmlEventFactory = null;
    private static XMLInputFactory xmlInputFactory = null;
    private static XMLOutputFactory xmlOutputFactory = null;


    public static XMLEventFactory getXMLEventFactory() {
        if (xmlEventFactory == null) {
            synchronized (StAXUtils.class) {
                if (xmlEventFactory == null) {
                    xmlEventFactory = XMLEventFactory.newFactory();
                }
            }
        }
        return xmlEventFactory;
    }

    public static XMLInputFactory getXMLInputFactory() {
        if (xmlInputFactory == null) {
            synchronized (StAXUtils.class) {
                if (xmlInputFactory == null) {
                    xmlInputFactory = XMLInputFactory.newFactory();
                }
            }
        }
        return xmlInputFactory;
    }

    public static XMLOutputFactory getXMLOutputFactory() {
        if (xmlOutputFactory == null) {
            synchronized (StAXUtils.class) {
                if (xmlOutputFactory == null) {
                    xmlOutputFactory = XMLOutputFactory.newFactory();
                    xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
                }
            }
        }
        return xmlOutputFactory;
    }
}
