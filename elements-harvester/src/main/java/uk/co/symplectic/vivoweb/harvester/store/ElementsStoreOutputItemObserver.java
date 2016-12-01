/*
 * ******************************************************************************
 *  * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *  *****************************************************************************
 */

package uk.co.symplectic.vivoweb.harvester.store;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.symplectic.translate.TemplatesHolder;
import uk.co.symplectic.translate.TranslationService;
import uk.co.symplectic.vivoweb.harvester.config.Configuration;
import uk.co.symplectic.vivoweb.harvester.model.ElementsItemId;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;

/**
 * Created by ajpc2_000 on 07/09/2016.
 */
public abstract class ElementsStoreOutputItemObserver extends IElementsStoredItemObserver.ElementsStoredResourceObserverAdapter {

    private static final Logger log = LoggerFactory.getLogger(ElementsStoreOutputItemObserver.class);
    private final ElementsItemFileStore store;
    private final StorableResourceType outputType;
    private final boolean tolerateIndividualIoFailures;

    protected ElementsItemFileStore getStore(){return store;}
    protected StorableResourceType getOutputType(){return outputType;}

    protected ElementsStoreOutputItemObserver(ElementsItemFileStore fileStore, StorableResourceType inputType, StorableResourceType outputType, boolean tolerateIndividualIoFailures) {
        super(inputType);
        if(fileStore == null) throw new NullArgumentException("fileStore");
        if(outputType == null) throw new NullArgumentException("outputType");
        if(inputType.getKeyItemType() != outputType.getKeyItemType()) throw new IllegalArgumentException("outputType must be compatible with inputType");

        this.store = fileStore;
        this.outputType = outputType;
        this.tolerateIndividualIoFailures = tolerateIndividualIoFailures;
    }

    protected void safelyDeleteItem(ElementsItemId itemId, String failureMessage){
        try {
            store.deleteItem(itemId, outputType);
        }
        catch (IOException e){
            log.error(MessageFormat.format("{0} : {1}", failureMessage, e.getMessage()));
            if(!tolerateIndividualIoFailures) throw new IllegalStateException(failureMessage, e);
        }
    }

    @Override
    public void observeTypeCleardown(StorableResourceType type, ElementsItemStore source){
        try {
            store.cleardown(outputType, true);
        }
        catch (IOException e){
            String failureMessage = MessageFormat.format("Failed to cleardown {0} output from store", type.toString());
            log.error(failureMessage);
            throw new IllegalStateException(failureMessage, e);
        }
    }
}

