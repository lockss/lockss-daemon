/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2010 James Murty
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
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Input stream that wraps another stream and deflates (compresses) the underlying stream's
 * data on-the-fly. This class provides only a basic implementation of GZip functionality.
 *
 * @author James Murty
 */
public class GZipDeflatingInputStream extends InputStream implements InputStreamWrapper {
    private int BUFFER_SIZE = 8192;

    Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
    private InputStream inputStream = null;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private byte[] deflatedBytes = new byte[BUFFER_SIZE];
    private CRC32 crc = new CRC32();
    int bufferOffset = 0;
    int bufferEnd = 0;
    boolean EOFInput = false;
    boolean EOFDeflated = false;
    boolean EOFTail = false;

    public GZipDeflatingInputStream(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        crc.reset();

        // Write the GZip Header.
        int GZIP_MAGIC = 0x8b1f;
        writeShort(GZIP_MAGIC); // Magic number
        deflatedBytes[bufferEnd++] = (byte) Deflater.DEFLATED; // Compression method (CM)
        deflatedBytes[bufferEnd++] = 0; // Flags (FLG)
        writeInt(0); // Modification time (MTIME)
        deflatedBytes[bufferEnd++] = 0; // Extra flags (XFL)
        deflatedBytes[bufferEnd++] = 0; // Operating system (OS)
    }

    private void primeDeflateBuffer() throws IOException {
        bufferEnd = 0;
        while (bufferEnd == 0) {
            if (!deflater.needsInput()) {
                // Do pending deflation.
                bufferEnd = deflater.deflate(deflatedBytes);
                bufferOffset = 0;
            } else {
                if (!EOFInput) {
                    // Obtain more data from the input stream.
                    int byteCount = inputStream.read(buffer, 0, buffer.length);
                    if (byteCount > 0) {
                        crc.update(buffer, 0, byteCount);
                        deflater.setInput(buffer, 0, byteCount);
                        bufferEnd = deflater.deflate(deflatedBytes);
                        bufferOffset = 0;
                    } else if (byteCount == 0) {
                        // The underlying input stream must be non-blocking. Do nothing.
                    } else {
                        // EOF of the underlying Input Stream.
                        deflater.finish();
                        EOFInput = true;
                        bufferEnd = 0;
                        bufferOffset = 0;
                    }
                } else {
                    // No more input data available, time to finish up.
                    if (!deflater.finished()) {
                        bufferEnd = deflater.deflate(deflatedBytes);
                        bufferOffset = 0;
                    } else  if (!EOFDeflated) {
                        EOFDeflated = true;
                        bufferOffset = 0;
                        bufferEnd = 0;
                        writeInt((int)crc.getValue());  // CRC-32 of uncompressed data
                        writeInt(deflater.getTotalIn());    // Number of uncompressed bytes
                    } else {
                        EOFTail = true;
                        return;
                    }
                }
            }
        }
    }

    private int getDeflatedBufferAvail() {
        return bufferEnd - bufferOffset;
    }

    public int read() throws IOException {
        if (getDeflatedBufferAvail() == 0) {
            primeDeflateBuffer();
        }
        if (EOFTail) {
            return -1;
        } else {
            return (int) deflatedBytes[bufferOffset++] & 0xFF;
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (getDeflatedBufferAvail() == 0) {
            primeDeflateBuffer();
        }
        if (EOFTail) {
            return -1;
        } else {
            if (len > getDeflatedBufferAvail()) {
                // More data requested than is available in the buffer, return everything we have.
                int bytesAvailable = getDeflatedBufferAvail();
                System.arraycopy(deflatedBytes, bufferOffset, b, off, bytesAvailable);
                bufferOffset = 0;
                bufferEnd = 0;
                return bytesAvailable;
            } else {
                // Return some of the data we have buffered.
                System.arraycopy(deflatedBytes, bufferOffset, b, off, len);
                bufferOffset += len;
                return len;
            }
        }
    }

    public int available() throws IOException {
        if (EOFTail) {
            return -1;
        } else if (getDeflatedBufferAvail() == 0) {
            primeDeflateBuffer();
        }
        return getDeflatedBufferAvail();
    }

    public void close() throws IOException {
        EOFTail = true;
        inputStream.close();
    }

    public InputStream getWrappedInputStream() {
        return inputStream;
    }

    /*
     * Writes integer in Intel byte order.
     */
    private void writeInt(int i) {
        writeShort(i & 0xffff);
        writeShort((i >> 16) & 0xffff);
    }

    /*
     * Writes short integer in Intel byte order.
     */
    private void writeShort(int s) {
            deflatedBytes[bufferEnd++] = (byte) (s & 0xff);
            deflatedBytes[bufferEnd++] = (byte) ((s >> 8) & 0xff);
    }

}
