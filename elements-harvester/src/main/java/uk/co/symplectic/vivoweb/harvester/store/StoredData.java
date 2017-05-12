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

import java.io.*;
import java.util.zip.GZIPInputStream;

public interface StoredData {
    String getAddress();
    InputStream getInputStream() throws IOException;
    void delete();


    class InRam implements StoredData {
        private byte[] data;

        public InRam(byte[] data) {
            if (data == null) throw new NullArgumentException("data");
            this.data = data;
        }

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
            if(file.exists()) file.delete();
        }
    }
}
