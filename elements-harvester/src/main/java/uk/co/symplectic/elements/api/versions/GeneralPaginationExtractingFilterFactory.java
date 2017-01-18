/**
 * Created by ajpc2_000 on 29/07/2016.
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

public class GeneralPaginationExtractingFilterFactory extends PaginationExtractingFilterFactory {

    static class GeneralPaginationExtractingFilter extends ElementsAPIVersion.PaginationExtractingFilter {

        private static final String apiNS = ElementsAPI.apiNS;
        private static final String atomNS = ElementsAPI.atomNS;

        ElementsFeedPagination workspace = null;

        public GeneralPaginationExtractingFilter(){
            super(new XMLEventProcessor.EventFilter.DocumentLocation(new QName(atomNS, "feed"), new QName(apiNS, "pagination")));
        }

        @Override
        protected void initialiseItemExtraction(StartElement initialElement, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
            workspace = new ElementsFeedPagination();
        }

        @Override
        protected void processEvent(XMLEvent event, XMLEventProcessor.ReaderProxy readerProxy) {
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                if (startElement.getName().equals(new QName(apiNS, "pagination"))) {
                    Attribute ippAtt = startElement.getAttributeByName(new QName("items-per-page"));
                    if (ippAtt != null) workspace.setItemsPerPage(Integer.parseInt(ippAtt.getValue()));
                }
                if (startElement.getName().equals(new QName(apiNS, "page"))) {
                    Attribute posAtt = startElement.getAttributeByName(new QName("position"));
                    Attribute hrefAtt = startElement.getAttributeByName(new QName("href"));
                    if (posAtt != null && hrefAtt != null) {
                        String posValue = posAtt.getValue();
                        String hrefValue = hrefAtt.getValue();
                        if ("first".equals(posValue)) workspace.setFirstURL(hrefValue);
                        else if ("previous".equals(posValue)) workspace.setPreviousURL(hrefValue);
                        else if ("next".equals(posValue)) workspace.setNextURL(hrefValue);
                        else if ("last".equals(posValue)) workspace.setLastURL(hrefValue);
                    }
                }
            }
        }

        @Override
        protected ElementsFeedPagination finaliseItemExtraction(EndElement finalElement, XMLEventProcessor.ReaderProxy proxy){
            return workspace;
        }
    }

    @Override
    public ElementsAPIVersion.PaginationExtractingFilter createPaginationExtractor(){
        return new GeneralPaginationExtractingFilter();
    }
}