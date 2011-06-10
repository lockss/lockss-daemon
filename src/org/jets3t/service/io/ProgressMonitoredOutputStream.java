/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2007 James Murty
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

/**
 * Output stream wrapper that tracks the number of bytes that have been written through the stream.
 * When data is written through this stream the count of bytes is increased, and at a set minimum
 * interval (eg after at least 1024 bytes) a {@link BytesProgressWatcher} implementation
 * is notified of the count of bytes read since the last notification.
 *
 * @author James Murty
 */
public class ProgressMonitoredOutputStream extends OutputStream implements OutputStreamWrapper {
    private OutputStream outputStream = null;
    private BytesProgressWatcher progressWatcher = null;

    /**
     * Construts the input stream around an underlying stream and sends notification messages
     * to a progress watcher when bytes are read from the stream.
     *
     * @param outputStream
     *        the output stream to wrap, whose byte transfer count will be monitored.
     * @param progressWatcher
     *        a watcher object that stores information about the bytes read from a stream, and
     *        allows calculations to be perfomed using this information.
     */
    public ProgressMonitoredOutputStream(OutputStream outputStream, BytesProgressWatcher progressWatcher) {
        if (outputStream == null) {
            throw new IllegalArgumentException(
                "ProgressMonitoredOutputStream cannot run with a null OutputStream");
        }
        this.outputStream = outputStream;
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

    public void write(int b) throws IOException {
        outputStream.write(b);
        sendNotificationUpdate(1);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
        sendNotificationUpdate(len - off);
    }

    public void write(byte[] b) throws IOException {
        outputStream.write(b.length);
        sendNotificationUpdate(b.length);
    }

    public void close() throws IOException {
        outputStream.close();
    }

    public OutputStream getWrappedOutputStream() {
        return outputStream;
    }

}
