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

package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * An extension of ElementsStoredResourceObserverAdapter that represents the idea of an observer that monitors the
 * observed store for changes in an "input" resourceType and in response will somehow generate an "output" resource
 * and store that output in a second store (e.g. a translation)
 *
 * Implements observeTypeCleardown on the basis that if the input type is cleared down in its entirety in the observed
 * store, we know we want to cleardown the output type in the output store too.
 *
 * Does not implement observeObjectDeletion as subclasses may be more discriminative than just inputType, so
 * we may need to be more careful in what we delete from the outputStore.
 * Nonetheless provides protected "safelyDeleteItem" methods to ease the task of removing items from the outputStore
 */

public abstract class ElementsStoreOutputItemObserver extends IElementsStoredItemObserver.ElementsStoredResourceObserverAdapter {

    private static final Logger log = LoggerFactory.getLogger(ElementsStoreOutputItemObserver.class);
    private final ElementsItemFileStore outputStore;
    private final StorableResourceType outputType;
    private final boolean tolerateIndividualIoFailures;

    protected ElementsItemFileStore getStore(){return outputStore;}
    protected StorableResourceType getOutputType(){return outputType;}

    @SuppressWarnings("SameParameterValue")
    protected ElementsStoreOutputItemObserver(ElementsItemFileStore outputStore, StorableResourceType inputType, StorableResourceType outputType, boolean tolerateIndividualIoFailures) {
        super(inputType);
        if(outputStore == null) throw new NullArgumentException("outputStore");
        if(outputType == null) throw new NullArgumentException("outputType");
        if(inputType.getKeyItemType() != outputType.getKeyItemType()) throw new IllegalArgumentException("outputType must be compatible with inputType");

        this.outputStore = outputStore;
        this.outputType = outputType;
        this.tolerateIndividualIoFailures = tolerateIndividualIoFailures;
    }

    protected void safelyDeleteItem(ElementsItemId itemId, String failureMessage){safelyDeleteItem(itemId, null, failureMessage);}

    protected void safelyDeleteItem(ElementsItemId itemId, File[] additionalFilesToDelete, String failureMessage){
        try {
            outputStore.deleteItem(itemId, outputType);
            if(additionalFilesToDelete != null){
                for(File file : additionalFilesToDelete){
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        }
        catch (IOException e){
            log.error(MessageFormat.format("{0} : {1}", failureMessage, e.getMessage()));
            if(!tolerateIndividualIoFailures) throw new IllegalStateException(failureMessage, e);
        }
    }

    @Override
    public void observeTypeCleardown(StorableResourceType type, ElementsItemStore source){
        try {
            outputStore.cleardown(outputType, true);
        }
        catch (IOException e){
            String failureMessage = MessageFormat.format("Failed to cleardown {0} output from store", type.toString());
            log.error(failureMessage);
            throw new IllegalStateException(failureMessage, e);
        }
    }
}

