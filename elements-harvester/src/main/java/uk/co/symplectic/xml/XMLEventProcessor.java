/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.xml;

import org.apache.commons.lang.NullArgumentException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.*;

/**
 * Created by ajpc2_000 on 27/07/2016.
 * Processor class for dealing with XMLEventStreams.
 * Filters registered either on construction or using the addFilter method are invoked if the document location specified in the filter is encountered.
 * Filters then receive all events from the stream until that location scope is exited.
 **/
public class XMLEventProcessor {

    /*
    Storage for filters that will be invoked by this processor when parsing an XML stream.
    The Hash map linking a given Filter document location (expressed as a QName list) to the set of filters that should be activated if that location is found in the document.
    This makes it much easier to retrieve the relevant filters when needed.
     */
    private HashMap<List<QName>, List<EventFilter>> filters = new HashMap<List<QName>, List<EventFilter>>();

    /*
    Constructor allowing the easy addition of filters to run on this processor
     */
    public XMLEventProcessor(EventFilter... filters) {
        for (EventFilter filter : filters) {
            addFilter(filter);
        }
    }

    /*
    Method to add a filter to this processor
     */
    public void addFilter(EventFilter filter) {
        if (filter != null) {
            List<QName> key = filter.getDocumentLocation();
            if (this.filters.get(key) == null) this.filters.put(key, new ArrayList<EventFilter>());
            this.filters.get(key).add(filter);
        }
    }

    /*
    Helper method used by "process" - Tests that the incoming reader is in a sensible place to start processing and errors out if not.
     */
    private void CheckInitialState(ReaderProxy proxy) {
        XMLEvent initialEvent = proxy.peek();
        if (!initialEvent.isStartDocument() && !initialEvent.isStartElement())
            throw new IllegalStateException("Must enter process with the XMLEventReader currently on a StartElements or StartDocument event");
    }

    /*
    Main processing method - does the work of advancing the stream, tracking scope,
    invoking filters when relevant and handing out events to any filters in scope
    Note : ONLY this method must ever advance the stream - which is why it only ever hands out events and proxies when invoking external events
     */
    public void process(XMLEventReader reader) {
        if (reader != null) {
            try {
                //Proxied access to the underlying stream,
                //Allows us to grant access to the next-event to filters, etc without allowing them to advance the stream.
                ReaderProxy proxy = new ReaderProxy(reader);

                //Test that incoming reader is in a sensible state to start processing
                CheckInitialState(proxy);

                //initialise scope tracking stack
                Stack<ProcessScope> stack = new Stack<ProcessScope>();

                //MAIN LOOP this is the only thing that should ever advance the reader stream.
                while (reader.hasNext()) {
                    XMLEvent event = reader.nextEvent();

                    //if we have encountered a StartElement we have a new scope
                    if (event.isStartElement()) {
                        StartElement eventAsStartElement = event.asStartElement();
                        //get the new scope's name
                        QName eventQName = eventAsStartElement.getName();

                        //work out the new scope's document location based on the existing scope stack and the name of the element we just encountered
                        List<QName> currentDocumentLocation = new ArrayList<QName>();
                        for (ProcessScope scope : stack) {
                            currentDocumentLocation.add(scope.getName());
                        }
                        currentDocumentLocation.add(eventQName);

                        //retrieve any filters associated with the new scope's document location  and create a ProcessScope object to track the new scope.
                        ProcessScope newScope = new ProcessScope(eventQName, filters.get(currentDocumentLocation));

                        //Inform all the filters activated in our new scope that we are about to start sending them events
                        for (EventFilter filter : newScope.getFiltersInScope()) {
                            filter.itemStart(eventAsStartElement, proxy);
                        }

                        //put the ProcessScope object representing our new scope into the tracking stack
                        stack.push(newScope);
                    }

                    //for all events work through all the filters in all the currently active scopes and pass them the details of the current event
                    //Note: this will include the "StartElement" from a scope that has only just been activated and the EndElements from a scope that is about to close
                    for (ProcessScope scope : stack) {
                        for (EventFilter filter : scope.getFiltersInScope()) {
                            filter.processEvent(event, proxy);
                        }
                    }

                    //if we have encountered an EndElement then we are exiting a scope.
                    if (event.isEndElement()) {
                        EndElement eventAsEndElement = event.asEndElement();
                        //get the name of the scope that is closing.
                        QName eventQName = eventAsEndElement.getName();

                        //Get the name of the scope that we thing we are currently in from our scope tracking stack.
                        ProcessScope currentScope = stack.empty() ? null : stack.peek();

                        //Test that the scope we are exiting in the stream is what we were expecting from our tracking.
                        //Error out if there is a discrepancy
                        if (currentScope == null) throw new XMLStreamException("Invalid XML structure detected");
                        QName expectedName = currentScope.getName();
                        if (!eventQName.equals(expectedName))
                            throw new XMLStreamException("Invalid XML structure detected");

                        //Inform all the filters in the scope we are about to exit that we are about to stop sending them events.
                        for (EventFilter filter : currentScope.getFiltersInScope()) {
                            filter.itemEnd(eventAsEndElement, proxy);
                        }

                        //remove the current scope from the tracking stack.
                        stack.pop();
                    }
                }
            } catch (XMLStreamException e) {
                //TODO: add sensible exception handling here.
            }
        }
    }

    /*
        Immutable Helper class to track XML document scopes as we process the file.
        Each represents the Scope associated with processing an Element of name "name"
        And any filters that were newly activated at this scope
     */
    private class ProcessScope {
        private final QName name;
        private final List<EventFilter> filtersInScope = new ArrayList<EventFilter>();

        ProcessScope(QName name, List<EventFilter> filters) {
            this.name = name;
            if (filters != null) {
                for (EventFilter filter : filters) {
                    if (filter != null) this.filtersInScope.add(filter);
                }
            }
        }

        QName getName() { return name; }
        List<EventFilter> getFiltersInScope() { return filtersInScope; }
    }

    /*
    Helper class to wrap an XMLEventReader to only provide access to the underlying peek method
    Exists to allow access to the stream to be exposed to invoked filters without granting access to methods that advance the stream.
     */
    public class ReaderProxy {
        private final XMLEventReader reader;

        ReaderProxy(XMLEventReader reader) { this.reader = reader; }

        public XMLEvent peek() {
            try {
                return this.reader.peek();
            } catch (XMLStreamException e) {
                //deliberately don't worry in here - just return null, let the wrapping processor complain when it actually processes the event.
                return null;
            }
        }
    }

    /*
    For every location in the document being processed by an XmlEventProcessor that matches the location defined by "names".
    The processor ensures that itemStart is called on the filter, which then receives every event that is passed in from the system
    until the filter goes out of scope at which point the processor invokes itemEnd
     */
    public abstract static class EventFilter {

        public static class DocumentLocation{
            private final List<QName> location = new ArrayList<QName>();

            public DocumentLocation(QName...documentLocation){
                for (QName name : documentLocation) {
                    if(name == null) throw new IllegalArgumentException("Must not supply null as one of the QNames in a Document Location");
                    this.location.add(name);
                }
            }
        }

        //The document location this filter is active at (expressed as a QName array)
        final DocumentLocation documentLocation;

        //TODO: move these to an API utility class?
        //Useful API Namespaces -
        protected static final String apiNS = "http://www.symplectic.co.uk/publications/api";
        protected static final String atomNS = "http://www.w3.org/2005/Atom";

        //Constructor allowing easy specification of the document location this filter is active at.
        protected EventFilter(DocumentLocation location) {
            if(location == null) throw new NullArgumentException("location");
            this.documentLocation = location;
        }

        //Get the document Location where this filter is active (as a QName array)
        List<QName> getDocumentLocation() {
            return documentLocation.location;
        }

        //default methods to do "nothing" on item start and end only override if you need to do something
        protected void itemStart(StartElement initialElement, ReaderProxy readerProxy) throws XMLStreamException { }
        protected void itemEnd(EndElement finalElement, ReaderProxy readerProxy) throws XMLStreamException { }

        //abstract stub for process event - have to do something to create a concrete filter.
        protected abstract void processEvent(XMLEvent event, ReaderProxy readerProxy) throws XMLStreamException;
    }


    public abstract static class ItemExtractingFilter<T> extends EventFilter{
        final private int maximumAmountExpected;
        private boolean extractionAttempted = false;
        private boolean itemReady = false;
        private T previouslyExtractedItem = null;
        private T extractedItem = null;
        private int countOfExtractedItems = 0;

        protected ItemExtractingFilter(DocumentLocation location) {
            this(location, 0);
        }

        public T getExtractedItem(){
            if(extractionAttempted && !itemReady) throw new IllegalAccessError("Must not call getExtractedItem when item is in middle of being processed");
            return extractedItem;
        }

        protected ItemExtractingFilter(DocumentLocation location, int maximumAmountExpected) {
            super(location);
            this.maximumAmountExpected = maximumAmountExpected;
        }

        @Override
        final protected void itemStart(StartElement initialElement, ReaderProxy readerProxy) throws XMLStreamException {
            if(maximumAmountExpected > 0 && countOfExtractedItems >= maximumAmountExpected) throw new IllegalStateException("More items detected in XML than expected");
            itemReady = false;
            extractionAttempted = true;
            initialiseItemExtraction(initialElement, readerProxy);
        }

        protected abstract void initialiseItemExtraction(StartElement initialElement, ReaderProxy readerProxy) throws XMLStreamException;

        @Override
        final protected void itemEnd(EndElement finalElement, ReaderProxy readerProxy) throws XMLStreamException {
            previouslyExtractedItem = extractedItem;
            extractedItem = finaliseItemExtraction(finalElement, readerProxy);
            if(previouslyExtractedItem == extractedItem) throw new IllegalStateException("Must create a new object for each extracted item when implementing an extractor");
            countOfExtractedItems++;
            itemReady = true;
        }

        abstract protected T finaliseItemExtraction(EndElement finalElement, ReaderProxy readerProxy);
    }

    public abstract static class EventFilterWrapper<T extends EventFilter> extends EventFilter {
        final protected T innerFilter;

        public EventFilterWrapper(T innerFilter){
            //slightly dodgy will fail with a null reference if innerFilter is null - can't easily avoid though
            super(innerFilter.documentLocation);
            this.innerFilter = innerFilter;
        }

        @Override
        final protected void itemStart(StartElement initialElement, ReaderProxy readerProxy) throws XMLStreamException {
            preInnerItemStart(initialElement, readerProxy);
            innerFilter.itemStart(initialElement, readerProxy);
            postInnerItemStart(initialElement, readerProxy);
        }
        protected void preInnerItemStart(StartElement initialElement, ReaderProxy readerProxy) throws XMLStreamException {}
        protected void postInnerItemStart(StartElement initialElement, ReaderProxy readerProxy) throws XMLStreamException {}


        @Override
        final protected void itemEnd(EndElement finalElement, ReaderProxy readerProxy) throws XMLStreamException {
            preInnerItemEnd(finalElement, readerProxy);
            innerFilter.itemEnd(finalElement, readerProxy);
            postInnerItemEnd(finalElement, readerProxy);
        }
        protected void preInnerItemEnd(EndElement finalElement, ReaderProxy readerProxy) throws XMLStreamException {}
        protected void postInnerItemEnd(EndElement finalElement, ReaderProxy readerProxy) throws XMLStreamException {}

        @Override
        final protected void processEvent(XMLEvent event, ReaderProxy readerProxy) throws XMLStreamException {
            preInnerProcessEvent(event, readerProxy);
            innerFilter.processEvent(event, readerProxy);
            postInnerProcessEvent(event, readerProxy);
        }
        protected void preInnerProcessEvent(XMLEvent finalElement, ReaderProxy readerProxy) throws XMLStreamException {}
        protected void postInnerProcessEvent(XMLEvent finalElement, ReaderProxy readerProxy) throws XMLStreamException {}
    }

//    HACK hack hack
//    public static class DummyFilter extends EventFilter{
//        int test = 0;
//
//        public DummyFilter() {
//            super(new DocumentLocation(new QName(atomNS, "feed"), new QName(apiNS, "pagination")));
//        }
//
//        @Override
//        protected void processEvent(XMLEvent event, XMLEventProcessor.ReaderProxy readerProxy) throws XMLStreamException {
//            test++;
//        }
//    }

}
