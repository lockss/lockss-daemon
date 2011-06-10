/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2011 James Murty
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
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A repeatable input stream for files. This input stream can be repeated an unlimited number of
 * times, without any limitation on when a repeat can occur.
 *
 * @author James Murty
 */
public class SegmentedRepeatableFileInputStream extends RepeatableFileInputStream {
    private static final Log log = LogFactory.getLog(SegmentedRepeatableFileInputStream.class);

    protected long offset = 0;
    protected long segmentLength = 0;

    /**
     * Creates a repeatable input stream based on a file.
     *
     * @param file
     * @throws IOException
     */
    public SegmentedRepeatableFileInputStream(File file, long offset, long segmentLength)
        throws IOException
    {
        super(file);
        this.offset = offset;
        this.segmentLength = segmentLength;

        if (segmentLength < 1) {
            throw new IllegalArgumentException(
                "Segment length " + segmentLength + " must be greater than 0");
        }
        // Sanity check segment bounds against underlying file
        if (file.length() < this.offset + this.segmentLength) {
            throw new IllegalArgumentException(
                "Offset " + offset + " plus segment length " + segmentLength
                + "exceed length of file " + file);
        }

        // Skip forward to requested offset in file input stream.
        skipToOffset();
    }

    private void skipToOffset() throws IOException {
        long skipped = 0;
        long toSkip = offset;
        while (toSkip > 0) {
            skipped = skip(toSkip);
            toSkip -= skipped;
        }

        // Mark the offset location so we will return here on reset
        super.mark(0);

        if (log.isDebugEnabled()) {
            log.debug("Skipped to segment offset " + offset);
        }
    }

    @Override
    public int available() throws IOException {
        // Nobody will ever need an input stream longer that 2^31 - 1 bytes... D'oh!
        long reallyAvailable = this.segmentLength -
            (bytesReadPastMarkPoint + getRelativeMarkPoint());
        if (reallyAvailable > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) reallyAvailable;
    }

    @Override
    public int read() throws IOException {
        // Ensure we don't read beyond the segment length
        if (bytesReadPastMarkPoint + getRelativeMarkPoint() >= segmentLength) {
            return -1;
        } else {
            return super.read();
        }
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        bytesReadPastMarkPoint += off;
        // Ensure we don't read beyond the segment length
        if (bytesReadPastMarkPoint + getRelativeMarkPoint() >= segmentLength) {
            return -1;
        }
        if (bytesReadPastMarkPoint + getRelativeMarkPoint() + len > segmentLength) {
            len = (int) (segmentLength - (bytesReadPastMarkPoint + getRelativeMarkPoint() + off));
        }
        return super.read(bytes, off, len);
    }

    private long getRelativeMarkPoint() {
        return markPoint - this.offset;
    }

}
