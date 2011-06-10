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
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Output stream that wraps another stream and inflates (de-compresses) the underlying stream's
 * data on-the-fly. This class provides only a basic implementation of GZip functionality.
 *
 * @author James Murty
 */
public class GZipInflatingOutputStream extends OutputStream implements OutputStreamWrapper {
    /*
     * GZIP header magic number.
     */
    public final static int GZIP_MAGIC = 0x8b1f;

    /*
     * File header flags.
     */
    private final static int FTEXT    = 1;    // Extra text
    private final static int FHCRC    = 2;    // Header CRC
    private final static int FEXTRA    = 4;    // Extra field
    private final static int FNAME    = 8;    // File name
    private final static int FCOMMENT    = 16;    // File comment

    private int BUFFER_SIZE = 8192;

    private Inflater inflater = new Inflater(true);
    private CRC32 crc = new CRC32();
    private OutputStream outputStream = null;
    private byte[] inflatedBytes = new byte[BUFFER_SIZE];

    // Variables used to parse Header.
    private boolean finishedHeader = false;
    private byte[] headerBytes = new byte[156];
    private int expectedHeaderLength = 0;
    private int headerLength = 0;
    private int headerOffset = 0;
    private int headerFlag = 0;

    // Variables used to parse tail.
    private byte[] trailerBytes = new byte[8]; // Can we assume trailer is only ever 8 bytes...?
    private int trailerOffset = 0;


    public GZipInflatingOutputStream(OutputStream outputStream) throws IOException {
        this.outputStream = outputStream;
        crc.reset();
        expectedHeaderLength = 4;
    }

    private void inflateData() throws IOException {
        try {
            int bytesWritten = -1;
            while (bytesWritten != 0) {
                bytesWritten = inflater.inflate(inflatedBytes, 0, inflatedBytes.length);
                outputStream.write(inflatedBytes, 0, bytesWritten);
                crc.update(inflatedBytes, 0, bytesWritten);
            }
        } catch (IOException e) {
            outputStream.close();
            throw e;
        } catch (DataFormatException e) {
            outputStream.close();
            throw new IOException("Invalid GZip data output stream: " + e);
        }
    }

    private void parseHeader(byte[] b, int off, int len) throws IOException {
        // Store all potential header bytes in a buffer.
        for (int i = 0; i < len && headerLength < headerBytes.length; i++) {
            headerBytes[headerLength++] = b[off + i];
        }

        // Once we have the first 4 bytes, check for validity and flag values.
        if (headerOffset == 0 && headerLength >= 4) {
            // Check header magic
            int GZIP_MAGIC = 0x8b1f;
            if (bytesToShort(headerBytes, 0) != GZIP_MAGIC) {
                outputStream.close();
                throw new IOException("Not in GZIP format");
            }
            // Check compression method
            if ((headerBytes[2] & 0xFF) != Deflater.DEFLATED) {
                outputStream.close();
                throw new IOException("Unexpected compression method");
            }

            // Read header content flags
            headerFlag = (headerBytes[3] & 0xFF);

            // Skip MTIME, XFL, and OS fields
            expectedHeaderLength += 6;
            headerOffset = 10;

            if ((headerFlag & FEXTRA) == FEXTRA)
                expectedHeaderLength += 2;

            if ((headerFlag & FNAME) == FNAME)
                expectedHeaderLength++;

            if ((headerFlag & FCOMMENT) == FCOMMENT) {
                expectedHeaderLength++;
            }

            if ((headerFlag & FHCRC) == FHCRC) {
                expectedHeaderLength += 2;
            }
        }

        // Once we have enough bytes to start checking flag values, do so.
        while (headerOffset != expectedHeaderLength && headerLength >= expectedHeaderLength) {
            // Skip optional extra field
            if ((headerFlag & FEXTRA) == FEXTRA) {
                // Skip past this variable-length field.
                int fieldLength = bytesToShort(headerBytes, headerOffset);
                expectedHeaderLength += fieldLength;
                headerOffset += 2 + fieldLength;
            } else
            // Skip optional file name
            if ((headerFlag & FNAME) == FNAME) {
                char fnameChar = (char) headerBytes[headerOffset++];
                if (fnameChar != 0) {
                    // More filename characters to come.
                    expectedHeaderLength++;
                }
            } else
            // Skip optional file comment
            if ((headerFlag & FCOMMENT) == FCOMMENT) {
                // Consume bytes until we get to the end of the comments.
                while ((headerBytes[headerOffset] & 0xFF) != 0) {
                    headerOffset++;
                    expectedHeaderLength++;
                }
            } else
            // Check optional header CRC
            if ((headerFlag & FHCRC) == FHCRC) {
                crc.update(headerBytes, 0, headerOffset);
                int v = (int)crc.getValue() & 0xffff;
                if (bytesToShort(headerBytes, headerOffset) != v) {
                    outputStream.close();
                    throw new IOException("Corrupt GZIP header");
                }
                crc.reset();
                headerOffset++;
                expectedHeaderLength++;
            }
        }

        if (headerOffset == expectedHeaderLength && headerLength >= expectedHeaderLength) {
            // Finished parsing the header.

            finishedHeader = true;
            // Pass any remaining bytes in the header buffer to the inflater.
            if (headerLength > headerOffset) {
                inflater.setInput(headerBytes, headerOffset, headerLength - headerOffset);
            }
            // Pass all other remaining bytes to the inflater.
            if (len > headerLength) {
                inflater.setInput(b, headerOffset, len - headerOffset);
            }
        }
    }

    public void write(int value) throws IOException {
        byte[] single = new byte[] { (byte) (value & 0xFF) };
        write(single, 0, 1);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (len <= 0) {
            return;
        }

        if (!finishedHeader) {
            // Keep parsing header data until the header is finished.
            parseHeader(b, off, len);
        } else {
            inflater.setInput(b, off, len);
        }

        if (!inflater.finished()) {
            inflateData();

            if (inflater.finished() && inflater.getRemaining() > 0) {
                // Copy bytes left-over from inflater into trailer buffer.
                int trailerOffsetInInput = len - inflater.getRemaining();
                while (trailerOffset < inflater.getRemaining()) {
                    trailerBytes[trailerOffset++] = b[trailerOffsetInInput++];
                }
            }
        } else {
            // Inflator has all the input bytes it needs, anything left over is for the tailer fields.
            if (trailerOffset + len > trailerBytes.length) {
                outputStream.close();
                throw new IOException("Corrupt GZIP trailer, too many trailer bytes (only 8 expected)");
            }
            System.arraycopy(b, off, trailerBytes, trailerOffset, len);
            trailerOffset += len;
        }
    }

    public void flush() throws IOException {
        outputStream.flush();
    }

    public void close() throws IOException {
        outputStream.close();

        if (inflater != null) {
            // Check that the data stream has been correctly unzipped.

            if (trailerOffset < trailerBytes.length) {
                throw new IOException("Corrupt GZIP trailer, trailer is incomplete. Expected 8 bytes, only have " + trailerOffset);
            }
            // Check CRC from trail
            long trailerCrc = bytesToInt(trailerBytes, 0);
            if (trailerCrc != crc.getValue()) {
                throw new IOException("Corrupt GZIP trailer, CRC values mismatch");
            }
            long trailerByteCount = bytesToInt(trailerBytes, 4);
            if (trailerByteCount != inflater.getTotalOut()) {
                throw new IOException("Corrupt GZIP trailer, actual size of inflated data mismatch");
            }
            inflater.end();
            inflater = null;
        }
    }

    private int bytesToShort(byte[] b, int offset) {
        int low = (b[offset] & 0xFF);
            int high = (b[offset + 1] & 0xFF);
           return (high << 8) | low;
    }

    private long bytesToInt(byte[] b, int offset) {
            int low = bytesToShort(b, offset);
            int high = bytesToShort(b, offset + 2);
            return ((long)high << 16) | low;
    }

    public OutputStream getWrappedOutputStream() {
        return this.outputStream;
    }

}
