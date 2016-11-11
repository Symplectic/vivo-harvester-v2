/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.store;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.tdb.nodetable.NodeTupleTable;
import com.hp.hpl.jena.tdb.store.DatasetGraphTDB;
import com.hp.hpl.jena.tdb.store.GraphTDB;
import com.hp.hpl.jena.tdb.store.bulkloader.BulkLoader;
import com.hp.hpl.jena.tdb.store.bulkloader.Destination;
import com.hp.hpl.jena.tdb.store.bulkloader.LoadMonitor;
import com.hp.hpl.jena.tdb.store.bulkloader.LoaderNodeTupleTable;
import org.openjena.riot.Lang;
import org.openjena.riot.RiotException;
import org.openjena.riot.RiotReader;
import org.slf4j.Logger;
import org.vivoweb.harvester.util.repo.JenaConnect;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by ajpc2_000 on 10/11/2016.
 */
public class TDBLoadUtility {

    public static void load(JenaConnect jc, Iterator<StoredData.InFile> iterator){
        //GraphTDB graph = (GraphTDB) new ModTDBDataset().getDataset().getDefaultModel().getGraph();
        GraphTDB graph = (GraphTDB) jc.getJenaModel().getGraph();

        Destination dest = destinationDefaultGraph(graph.getDataset());

        dest.start();
        int processCount = 0;
        while(iterator.hasNext()){
            StoredData.InFile current = iterator.next();
            if(processCount % 1000 == 0) {
                System.out.println(new Date().toString() + " " + processCount + " records processed : current record = " + current.getFile().getAbsolutePath());
            }

            //TODO: update logback to log any messages from here into the log file properly...
            try {
                RiotReader.parseTriples(current.getInputStream(), Lang.RDFXML, (String) null, dest);
            }
            catch(IOException e) {
                //TODO: make this log properly
                System.out.println("Item : " + current.getFile().getAbsolutePath() + " is corrupt.");
                e.printStackTrace();
            }
            catch(RiotException e) {
                //TODO: make this log properly
                System.out.println("Item : " + current.getFile().getAbsolutePath() + " is corrupt.");
                e.printStackTrace();
            }
            processCount++;
        }
        dest.finish();

    }

    private static Destination<Triple> destinationDefaultGraph(DatasetGraphTDB dsg) {
        NodeTupleTable ntt = dsg.getTripleTable().getNodeTupleTable();
        return destination(dsg, ntt);
    }

    private static Destination<Triple> destination(DatasetGraphTDB dsg, NodeTupleTable nodeTupleTable) {
        //LoadMonitor monitor = createLoadMonitor(dsg, "triples", showProgress);
        LoadMonitor monitor = new LoadMonitor(dsg, (Logger)null, "triples", (long) BulkLoader.DataTickPoint, BulkLoader.IndexTickPoint);
        final LoaderNodeTupleTable loaderTriples = new LoaderNodeTupleTable(nodeTupleTable, "triples", monitor);
        Destination sink = new Destination<Triple>() {
            long count = 0L;

            public final void start() {
                loaderTriples.loadStart();
                loaderTriples.loadDataStart();
            }

            public final void send(Triple triple) {
                loaderTriples.load(new Node[]{triple.getSubject(), triple.getPredicate(), triple.getObject()});
                ++this.count;
            }

            public final void flush() {
            }

            public void close() {
            }

            public final void finish() {
                loaderTriples.loadDataFinish();
                loaderTriples.loadIndexStart();
                loaderTriples.loadIndexFinish();
                loaderTriples.loadFinish();
            }
        };
        return sink;
    }

}
