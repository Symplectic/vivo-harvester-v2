/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.vivoweb.harvester.store;

import uk.co.symplectic.vivoweb.harvester.config.Configuration;

public class ElementsStoreFactory {
    private static ElementsObjectFileStore objectStore = null;
    private static ElementsRdfStore rdfStore = null;

    public static ElementsObjectFileStore getObjectStore() {
        if (objectStore != null) {
            return objectStore;
        } else {
            synchronized (ElementsStoreFactory.class) {
                return objectStore != null ? objectStore : (objectStore = new ElementsObjectFileStore.ElementsRawDataStore(Configuration.getRawOutputDir()));
            }
        }
    }

    public static ElementsRdfStore getRdfStore() {
        if (rdfStore != null) {
            return rdfStore;
        } else {
            synchronized (ElementsStoreFactory.class) {
                return rdfStore != null ? rdfStore : (rdfStore = new ElementsRdfStore(Configuration.getRdfOutputDir()));
            }
        }
    }
}
