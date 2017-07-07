/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.utils.triplestore;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * Class to represent the destination of a Jena Model being "outputted" somewherem, either to t File (FileOutput) or
 * to a TDB triple store (TripleStoreOutput).
 */
public abstract class ModelOutput {

    private static final Logger log = LoggerFactory.getLogger(ModelOutput.class);

    abstract public void output(Model modelToOutput);

    public static class FileOutput extends ModelOutput {
        final private File outputFile;
        //Valid values are : "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE", "N-TRIPLES", "N3", "N3-PP", "N3-PLAIN", "N3-TRIPLES", "N3-TRIPLE", "TURTLE", "Turtle", "TTL"
        String language;
        //private static String default_language = "N3";
        private static String default_language = "N-TRIPLES";

        public FileOutput(File outputFile) {
            this(outputFile, default_language);
        }

        public FileOutput(File outputFile, String language) {
            if (outputFile == null) throw new NullArgumentException("outputFile");
            if (language == null) throw new NullArgumentException("language");
            this.outputFile = outputFile;
            this.language = language;
        }

        private void ensureParentDirIsPresent() {
            File parentFile = outputFile.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
        }

        @Override
        public void output(Model modelToOutput) {
            ensureParentDirIsPresent();
            RDFWriter fasterWriter = modelToOutput.getWriter(language);
            if (language.equals("RDF/XML")) {
                fasterWriter.setProperty("showXmlDeclaration", "true");
                fasterWriter.setProperty("allowBadURIs", "true");
                fasterWriter.setProperty("relativeURIs", "");
            }
            OutputStreamWriter osw = null;
            try {
                try {
                    osw = new OutputStreamWriter(new FileOutputStream(outputFile), (Charset) Charset.availableCharsets().get("UTF-8"));
                    fasterWriter.write(modelToOutput, osw, "");
                    //log.info(MessageFormat.format("{0} data was exported to {1}", language, outputFile.getPath()));
                } finally {
                    if (osw != null) {
                        osw.close();
                    }
                }
            } catch (IOException e) {
                //log.debug(MessageFormat.format("IOException occurred trying to write {0} data to {1}", language, outputFile.getPath()));
                throw new IllegalStateException(e);
            }
        }
    }

    public static class TripleStoreOutput extends ModelOutput {
        private final TDBConnect outputStore;

        public TripleStoreOutput(TDBConnect outputStore) {
            if (outputStore == null) throw new NullArgumentException("outputStore");
            this.outputStore = outputStore;
        }

        @Override
        public void output(Model modelToOutput) {
            this.outputStore.getJenaModel().add(modelToOutput);
            //this.outputStore.sync(); //sync in TDBJenaConnect does nowt..
        }
    }
}