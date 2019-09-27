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

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Interface to represent access to some raw data in a store.
 * Provides:
 *     Access to the underlying data as an InputStream
 *     Ability to delete the underlying data (wherever it is stored)
 *     an "Address" string to represent where the data is stored
 */
@SuppressWarnings("unused")
public interface StoredData {
    String getAddress();
    InputStream getInputStream() throws IOException;
    void delete();

    /**
     * Class to implement StoredData interface for some data in main memory
     */
    class InRam implements StoredData {
        private byte[] data;

        public InRam(byte[] data) {
            if (data == null) throw new NullArgumentException("data");
            this.data = data;
        }

        @SuppressWarnings("unused")
        public byte[] getBytes() {
            return data;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        @Override
        public String getAddress() {
            return "in ram";
        }

        @Override
        public void delete(){
            data = null;
        }
    }

    /**
     * Class to implement StoredData interface for some data held in a file
     * where the file may be gzipped or may not.
     */
    @SuppressWarnings("WeakerAccess")
    class InFile implements StoredData {
        private final File file;
        private final boolean isZipped;

        public InFile(File file, boolean isZipped) {
            if (file == null) throw new NullArgumentException("file");
            this.file = file;
            this.isZipped = isZipped;
        }

        public boolean isZipped() {
            return isZipped;
        }

        public File getFile() {
            return file;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream stream = new BufferedInputStream(new FileInputStream(getFile()));
            if (isZipped()) stream = new GZIPInputStream(stream);
            return stream;
        }

        @Override
        public String getAddress() {
            return getFile().getAbsolutePath();
        }

        public void delete(){
            if(file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }
}
