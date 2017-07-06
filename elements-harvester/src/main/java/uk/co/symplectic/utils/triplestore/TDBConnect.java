/*
 * ******************************************************************************
 *   Copyright (c) 2017 Symplectic. All rights reserved.
 *   This Source Code Form is subject to the terms of the Mozilla Public
 *   License, v. 2.0. If a copy of the MPL was not distributed with this
 *   file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * ******************************************************************************
 */

package uk.co.symplectic.utils.triplestore;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;

public class TDBConnect {

    private static HashMap<String, Dataset> dirDatasets = new HashMap<String, Dataset>();

    private final File dbDir;
    private final String modelName;
    private final Model jenaModel;

    private String getModelName() {
        return this.modelName;
    }
    public Model getJenaModel() {
        return this.jenaModel;
    }

    //use default graph name if not specified.
    public TDBConnect(File dbDir){ this(dbDir, "urn:x-arq:DefaultGraph"); }

    public TDBConnect(File dbDir, String modelName) {
        if(dbDir == null) throw new NullArgumentException("dbDir");
        if(StringUtils.trimToNull(modelName) == null) throw new IllegalArgumentException("modelName must not be null or empty");

        this.dbDir = dbDir;
        this.modelName = modelName;

        dbDir.mkdirs();

        if(!dbDir.exists()){
            throw new IllegalArgumentException(MessageFormat.format("Invalid Directory : failed to create directory at {0}", dbDir.getAbsolutePath()));
        }

        this.jenaModel = this.getDataset().getNamedModel(this.getModelName());
    }

    public void close() {
        this.getJenaModel().close();
    }

    private Dataset getDataset() {
        if(!dirDatasets.containsKey(this.dbDir.getAbsolutePath())) {
            dirDatasets.put(this.dbDir.getAbsolutePath(), TDBFactory.createDataset(this.dbDir.getAbsolutePath()));
        }

        return dirDatasets.get(this.dbDir.getAbsolutePath());
    }
}
