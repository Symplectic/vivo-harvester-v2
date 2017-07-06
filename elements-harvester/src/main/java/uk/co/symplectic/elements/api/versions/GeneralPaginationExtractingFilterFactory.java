/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.elements.api.versions;

import uk.co.symplectic.elements.api.ElementsAPI;
import uk.co.symplectic.elements.api.ElementsAPIVersion;
import uk.co.symplectic.elements.api.ElementsAPIVersion.PaginationExtractingFilterFactory;
import uk.co.symplectic.elements.api.ElementsFeedPagination;
import uk.co.symplectic.utils.xml.XMLEventProcessor;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.List;

public class GeneralPaginationExtractingFilterFactory extends PaginationExtractingFilterFactory {

    static class GeneralPaginationExtractingFilter extends ElementsAPIVersion.PaginationExtractingFilter {

        private static final String apiNS = ElementsAPI.apiNS;
        private static final String atomNS = ElementsAPI.atomNS;

        ElementsFeedPagination workspace = null;

        public GeneralPaginationExtractingFilter(){
            super(new XMLEventProcessor.EventFilter.DocumentLocation(new QName(atomNS, "feed"), new QName(apiNS, "pagination")));
        }

        @Override
        protected void initialiseItemExtraction(XMLEventProcessor.WrappedXmlEvent initialEvent) throws XMLStreamException {
            workspace = new ElementsFeedPagination();
        }

        @Override
        protected void processEvent(XMLEventProcessor.WrappedXmlEvent event, List<QName> relativeLocation) {
            if (event.isRelevantForExtraction()) {
                QName name = event.getName();
                if (name.equals(new QName(apiNS, "pagination"))) {
                    if(event.hasAttribute("items-per-page")) {
                        workspace.setItemsPerPage(Integer.parseInt(event.getAttribute("items-per-page")));
                    }
                }
                if (name.equals(new QName(apiNS, "page"))) {
                    if (event.hasAttribute("position") && event.hasAttribute("href")) {
                        String posValue = event.getAttribute("position");
                        String hrefValue = event.getAttribute("href");
                        if ("first".equals(posValue)) workspace.setFirstURL(hrefValue);
                        else if ("previous".equals(posValue)) workspace.setPreviousURL(hrefValue);
                        else if ("next".equals(posValue)){
                            //pre 5.4 hack..
                            if(hrefValue.contains("/relationships/pending/deleted")) {
                                hrefValue = hrefValue.replaceFirst("/relationships/pending/deleted", "/relationships/deleted");
                            }
                            workspace.setNextURL(hrefValue);
                        }
                        else if ("last".equals(posValue)) workspace.setLastURL(hrefValue);
                    }
                }
            }
        }

        @Override
        protected ElementsFeedPagination finaliseItemExtraction(XMLEventProcessor.WrappedXmlEvent finalEvent){
            return workspace;
        }
    }

    @Override
    public ElementsAPIVersion.PaginationExtractingFilter createPaginationExtractor(){
        return new GeneralPaginationExtractingFilter();
    }
}