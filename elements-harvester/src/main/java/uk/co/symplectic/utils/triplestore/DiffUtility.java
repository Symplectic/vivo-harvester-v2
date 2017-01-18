/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.utils.triplestore;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.vivoweb.harvester.util.repo.JenaConnect;


public class DiffUtility {
        static public void diff(JenaConnect input1, JenaConnect input2, ModelOutput... outputs) {
        if(outputs.length == 0) throw new IllegalArgumentException("outputs must not be empty when performing a triple store diff");
        Model diffModel = ModelFactory.createDefaultModel();
        Model minuendModel = input1.getJenaModel();
        Model subtrahendModel = input2.getJenaModel();
        diffModel = minuendModel.difference(subtrahendModel);
        for(ModelOutput anOutput : outputs){ anOutput.output(diffModel); }
    }
}
