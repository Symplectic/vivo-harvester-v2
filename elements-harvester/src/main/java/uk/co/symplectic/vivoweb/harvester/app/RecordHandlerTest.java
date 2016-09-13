package uk.co.symplectic.vivoweb.harvester.app;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
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
import org.vivoweb.harvester.transfer.Transfer;
import org.vivoweb.harvester.util.repo.JenaConnect;
import org.vivoweb.harvester.util.repo.Record;
import org.vivoweb.harvester.util.repo.RecordHandler;
import org.vivoweb.harvester.util.repo.TDBJenaConnect;
import tdb.cmdline.ModTDBDataset;
import uk.co.symplectic.vivoweb.harvester.store.ElementsRecordHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by ajpc2_000 on 20/07/2016.
 */
public class RecordHandlerTest {
    public static void main(String[] args) {

        JenaConnect jc = new TDBJenaConnect("C:\\Users\\ajpc2_000\\Documents\\CambridgeVivo\\TestTDB");
        try {
            logTime("Start Test");
            ElementsRecordHandler rh = new ElementsRecordHandler("C:\\Users\\ajpc2_000\\Documents\\CambridgeVivo\\translated-records");
            //ElementsRecordHandler rh = new ElementsRecordHandler("C:\\Users\\ajpc2_000\\Documents\\CambridgeVivo\\translated-records\\translated-records\\relationship");
            //int processed = loadRdfFromRH(jc,rh,null,null);
            tdbLoaderTest(jc, rh.iterator());

        } catch (IOException e) {
            e.printStackTrace();
        }


/*        try {
            logTime("Start Test");
            ElementsRecordHandler rh = new ElementsRecordHandler("C:\\Users\\ajpc2_000\\Documents\\CambridgeVivo\\translated-records");
            Iterator<Record> iterator = rh.iterator();
            logTime("Iterator built");
            int counter = 0;
            while(iterator.hasNext()){
                Record record = iterator.next();
                //System.out.print(record.getID());
                //System.out.print(" : ");
                //System.out.println(record.getData().length());
                if((counter % 1000) == 0) {
                    System.out.println("Current record : " + record.getID());
                    logTime(counter + " records processed at");
                }
                counter++;

            }
            logTime("Iterator Complete");
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    private static void logTime(String message){
        if(message == null) message = "Time";
        System.out.println(message + " : " + new Date().toString());
    }

    public static int loadRdfFromRH(JenaConnect jc, RecordHandler rh, String namespace, String language) {
        int processCount = 0;

        for(Iterator i$ = rh.iterator(); i$.hasNext(); ++processCount) {
            Record r = (Record)i$.next();
            //log.trace("loading record: " + r.getID());
            //System.out.println(new Date() + " loading record: " + r.getID());
            if(processCount % 1000 == 0) {
                System.out.println(new Date().toString() + " " + processCount + " records processed : current record = " + r.getID());
            }
            if(namespace != null) {
                ;
            }

            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(r.getData().getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            jc.getJenaModel().read(bais, namespace, language);

        }

        return processCount;
    }


    public static void tdbLoaderTest(JenaConnect jc, Iterator<Record> iterator){
        //GraphTDB graph = (GraphTDB) new ModTDBDataset().getDataset().getDefaultModel().getGraph();
        GraphTDB graph = (GraphTDB) jc.getJenaModel().getGraph();

        Destination dest = destinationDefaultGraph(graph.getDataset());
        try {
            dest.start();
            int processCount = 0;
            while(iterator.hasNext()){
                Record aRecord = iterator.next();
                if(processCount % 1000 == 0) {
                    System.out.println(new Date().toString() + " " + processCount + " records processed : current record = " + aRecord.getID());
                }
                ByteArrayInputStream input = new ByteArrayInputStream(aRecord.getData().getBytes("UTF-8"));
                try {
                    RiotReader.parseTriples(input, Lang.RDFXML, (String) null, dest);
                }
                catch(RiotException e) {
                    System.out.println("Item : " + aRecord.getID() + " is corrupt.");
                }
                processCount++;
            }
            dest.finish();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

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
