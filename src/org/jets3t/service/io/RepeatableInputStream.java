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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Jets3tProperties;

/**
 * A repeatable input stream wrapper for any input stream. This input stream relies on buffered
 * data to repeat, and can therefore only be repeated when less data has been read than this
 * buffer can hold.
 * <p>
 * <b>Note:</b> Always use a {@link RepeatableFileInputStream} instead of this class if you are
 * sourcing data from a file, as the file-based repeatable input stream can be repeated without
 * any limitations.
 *
 * <p>
 * This class uses properties obtained through {@link Jets3tProperties}. For more information on
 * these properties please refer to
 * <a href="http://www.jets3t.org/toolkit/configuration.html">JetS3t Configuration</a>
 * </p>
 *
 * @author James Murty
 */
public class RepeatableInputStream extends InputStream implements InputStreamWrapper {
    private static final Log log = LogFactory.getLog(RepeatableInputStream.class);

    private InputStream is = null;
    private int bufferSize = 0;
    private int bufferOffset = 0;
    private long bytesReadPastMark = 0;
    private byte[] buffer = null;

    /**
     * Creates a repeatable input stream based on another input stream.
     *
     * @param inputStream
     * an input stream to wrap. The data read from the wrapped input stream is buffered as it is
     * read, up to the buffer limit specified.
     * @param bufferSize
     * the number of bytes buffered by this class.
     */
    public RepeatableInputStream(InputStream inputStream, int bufferSize) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        this.is = inputStream;

        this.bufferSize = bufferSize;
        this.buffer = new byte[this.bufferSize];

        if (log.isDebugEnabled()) {
            log.debug("Underlying input stream will be repeatable up to " + this.buffer.length + " bytes");
        }
    }

    /**
     * Resets the input stream to the beginning by pointing the buffer offset to the beginning of the
     * available data buffer.
     *
     * @throws UnrecoverableIOException
     * when the available buffer size has been exceeded, in which case the input stream data cannot
     * be repeated.
     */
    @Override
    public void reset() throws IOException {
        if (bytesReadPastMark <= bufferSize) {
            if (log.isDebugEnabled()) {
                log.debug("Reset after reading " + bytesReadPastMark + " bytes.");
            }
            bufferOffset = 0;
        } else {
            throw new UnrecoverableIOException(
                "Input stream cannot be reset as " + this.bytesReadPastMark
                + " bytes have been written, exceeding the available buffer size of " + this.bufferSize);
        }
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * This method can only be used while less data has been read from the input
     * stream than fits into the buffer. The readLimit parameter is ignored entirely.
     */
    @Override
    public synchronized void mark(int readlimit) {
        if (log.isDebugEnabled()) {
            log.debug("Input stream marked at " + bytesReadPastMark + " bytes");
        }
        if (bytesReadPastMark <= bufferSize && buffer != null) {
            // Clear buffer of already-read data to make more space.
            // it is safe to cast bytesReadPastMark to an int because it is known to be less than bufferSize, which is an int
            byte[] newBuffer = new byte[this.bufferSize];
            System.arraycopy(buffer, bufferOffset, newBuffer, 0, (int)(bytesReadPastMark - bufferOffset));
            this.buffer = newBuffer;
            this.bytesReadPastMark -= bufferOffset;
            this.bufferOffset = 0;
        } else {
            // If mark is called after the buffer was already exceeded, create a new buffer.
            this.bufferOffset = 0;
            this.bytesReadPastMark = 0;
            this.buffer = new byte[this.bufferSize];
        }
    }

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public int read(byte[] out, int outOffset, int outLength) throws IOException {
        byte[] tmp = new byte[outLength];

        // Check whether we already have buffered data.
        if (bufferOffset < bytesReadPastMark && buffer != null) {
            // Data is being repeated, so read from buffer instead of wrapped input stream.
            int bytesFromBuffer = tmp.length;
            if (bufferOffset + bytesFromBuffer > bytesReadPastMark) {
                bytesFromBuffer = (int) bytesReadPastMark - bufferOffset;
            }

            // Write to output.
            System.arraycopy(buffer, bufferOffset, out, outOffset, bytesFromBuffer);
            bufferOffset += bytesFromBuffer;
            return bytesFromBuffer;
        }

        // Read data from input stream.
        int count = is.read(tmp);

        if (count <= 0) {
            return count;
        }

        // Fill the buffer with data, as long as we won't exceed its capacity.
        if (bytesReadPastMark + count <= bufferSize) {
            System.arraycopy(tmp, 0, buffer, (int) bytesReadPastMark, count);
            bufferOffset += count;
        } else if (buffer != null) {
            // We have exceeded the buffer capacity, after which point it is of no use. Free the memory.
            if (log.isDebugEnabled()) {
                log.debug("Buffer size " + bufferSize + " has been exceeded and the input stream "
                + "will not be repeatable until the next mark. Freeing buffer memory");
            }
            buffer = null;
        }

        // Write to output byte array.
        System.arraycopy(tmp, 0, out, outOffset, count);
        bytesReadPastMark += count;

        return count;
    }

    @Override
    public int read() throws IOException {
        byte[] tmp = new byte[1];
        int count = read(tmp);
        if (count != -1) {
            return tmp[0];
        } else {
            return count;
        }
    }

    public InputStream getWrappedInputStream() {
        return is;
    }

}
