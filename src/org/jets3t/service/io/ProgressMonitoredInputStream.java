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

/**
 * Input stream wrapper that tracks the number of bytes that have been read through the stream.
 * When data is read through this stream the count of bytes is increased and the associated
 * {@link BytesProgressWatcher} object is notified of the count of bytes read.
 *
 * @author James Murty
 */
public class ProgressMonitoredInputStream extends InputStream implements InputStreamWrapper {
    private InputStream inputStream = null;
    private BytesProgressWatcher progressWatcher = null;

    /**
     * Construts the input stream around an underlying stream and sends notification messages
     * to a progress watcher when bytes are read from the stream.
     *
     * @param inputStream
     *        the input stream to wrap, whose byte transfer count will be monitored.
     * @param progressWatcher
     *        a watcher object that stores information about the bytes read from a stream, and
     *        allows calculations to be perfomed using this information.
     */
    public ProgressMonitoredInputStream(InputStream inputStream, BytesProgressWatcher progressWatcher) {
        if (inputStream == null) {
            throw new IllegalArgumentException(
                "ProgressMonitoredInputStream cannot run with a null InputStream");
        }
        this.inputStream = inputStream;
        this.progressWatcher = progressWatcher;
    }

    /**
     * Checks how many bytes have been transferred since the last notification, and sends a notification
     * message if this number exceeds the minimum bytes transferred value.
     *
     * @param bytesTransmitted
     */
    public void sendNotificationUpdate(long bytesTransmitted) {
        progressWatcher.updateBytesTransferred(bytesTransmitted);
    }

    public void resetProgressMonitor() {
        progressWatcher.resetWatcher();
    }

    public int read() throws IOException {
        int read = inputStream.read();
        if (read != -1) {
            sendNotificationUpdate(1);
        }
        return read;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int read = inputStream.read(b, off, len);
        if (read != -1) {
            sendNotificationUpdate(read);
        }
        return read;
    }

    public int read(byte[] b) throws IOException {
        int read = inputStream.read(b);
        if (read != -1) {
            sendNotificationUpdate(read);
        }
        return read;
    }

    public int available() throws IOException {
        return inputStream.available();
    }

    public void close() throws IOException {
        inputStream.close();
    }

    public InputStream getWrappedInputStream() {
        return inputStream;
    }

}
