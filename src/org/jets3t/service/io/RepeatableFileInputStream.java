/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008 James Murty
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jets3t.service.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A repeatable input stream for files. This input stream can be repeated an unlimited number of
 * times, without any limitation on when a repeat can occur.
 *
 * @author James Murty
 */
public class RepeatableFileInputStream extends InputStream implements InputStreamWrapper {
    private static final Log log = LogFactory.getLog(RepeatableFileInputStream.class);

    protected File file = null;
    protected FileInputStream fis = null;
    protected long bytesReadPastMarkPoint = 0;
    protected long markPoint = 0;

    /**
     * Creates a repeatable input stream based on a file.
     *
     * @param file
     * @throws FileNotFoundException
     */
    public RepeatableFileInputStream(File file) throws FileNotFoundException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        this.fis = new FileInputStream(file);
        this.file = file;
    }

    @Override
    public long skip(long toSkip) throws IOException {
        long skipped = this.fis.skip(toSkip);
        bytesReadPastMarkPoint += skipped;
        return skipped;
    }

    /**
     * Resets the input stream to the last mark point, or the beginning of the stream if
     * there is no mark point, by creating a new FileInputStream based on the
     * underlying file.
     *
     * @throws UnrecoverableIOException
     * when the FileInputStream cannot be re-created.
     */
    @Override
    public void reset() throws IOException {
        try {
            this.fis.close();
            this.fis = new FileInputStream(file);

            long skipped = 0;
            long toSkip = markPoint;
            while (toSkip > 0) {
                skipped = skip(toSkip);
                toSkip -= skipped;
            }

            if (log.isDebugEnabled()) {
                log.debug("Reset to mark point " + markPoint + " after returning " + bytesReadPastMarkPoint + " bytes");
            }
            this.bytesReadPastMarkPoint = 0;
        } catch (IOException e) {
            throw new UnrecoverableIOException("Input stream is not repeatable: " + e.getMessage());
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readlimit) {
        this.markPoint += bytesReadPastMarkPoint;
        this.bytesReadPastMarkPoint = 0;
        if (log.isDebugEnabled()) {
            log.debug("Input stream marked at " + this.markPoint + " bytes");
        }
    }

    @Override
    public int available() throws IOException {
        return fis.available();
    }

    @Override
    public void close() throws IOException {
        fis.close();
    }

    @Override
    public int read() throws IOException {
        int byteRead = fis.read();
        if (byteRead != -1) {
            bytesReadPastMarkPoint++;
            return byteRead;
        } else {
            return -1;
        }
    }

    @Override
    public int read(byte[] arg0, int arg1, int arg2) throws IOException {
        int count = fis.read(arg0, arg1, arg2);
        bytesReadPastMarkPoint += count;
        return count;
    }

    public InputStream getWrappedInputStream() {
        return this.fis;
    }

}
