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
package org.jets3t.service.impl.rest.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.io.InputStreamWrapper;
import org.jets3t.service.io.ProgressMonitoredInputStream;
import org.jets3t.service.io.RepeatableInputStream;
import org.jets3t.service.utils.ServiceUtils;

/**
 * An HttpClient request entity whose underlying data can be re-read (that is, repeated)
 * if necessary to retry failed transmissions. This class also provides basic byte-rate
 * throttling by throttling the reading of request bodies, the throttling value is set
 * with the JetS3t property <tt>httpclient.read-throttle</tt>. If Logging is enabled
 * for this class the MD5 hash values (Base64 and Hex) are logged after all data has
 * been written to the output stream.
 * <p>
 * This class works by taking advantage of the reset capability of the original
 * data input stream, or by wrapping the input stream in a reset-able class if
 * it is not so capable.
 * <p>
 * When data is repeated, any attached {@link ProgressMonitoredInputStream} is notified
 * that a repeat transmission is occurring.
 *
 * @author James Murty
 */
public class RepeatableRequestEntity implements RequestEntity {
    private static final Log log = LogFactory.getLog(RepeatableRequestEntity.class);

    private String name = null;
    private InputStream is = null;
    private String contentType = null;
    private long contentLength = 0;

    private long bytesWritten = 0;
    private InputStream repeatableInputStream = null;
    private ProgressMonitoredInputStream progressMonitoredIS = null;

    protected static long MAX_BYTES_PER_SECOND = 0;
    private static volatile long bytesWrittenThisSecond = 0;
    private static volatile long currentSecondMonitored = 0;
    private static final Random random = new Random();

    private boolean isLiveMD5HashingEnabled = true;
    private byte[] dataMD5Hash = null;

    /**
     * Creates a repeatable request entity for the input stream provided.
     * <p>
     * If the input stream provided, or any underlying wrapped input streams, supports the
     * {@link InputStream#reset()} method then it will be capable of repeating data
     * transmission. If the input stream provided does not supports this method, it will
     * automatically be wrapped in a {@link RepeatableInputStream} -- in this case, the data
     * read from the wrapped input stream will be buffered up to the limit set by the JetS3t
     * property <tt>uploads.stream-retry-buffer-size</tt> (default: 131072 bytes).
     *
     * <p>
     * This constructor also detects when an underlying {@link ProgressMonitoredInputStream} is
     * present, and will notify this monitor if a repeat occurs.
     * <p>
     * If the JetS3t properties option <code>httpclient.read-throttle</code> is set to a
     * non-zero value, all simultaneous uploads performed by this class will be throttled
     * to the specified speed.
     *
     *
     * @param is
     * the input stream that supplies the data to be made repeatable.
     * @param contentType
     * @param contentLength
     * @param enableLiveMD5Hashing
     * if true, data that passes through the object will be hashed to an MD5 digest
     * and this digest will be available from {@link #getMD5DigestOfData()}. If false,
     * the digest will not be calculated.
     */
    public RepeatableRequestEntity(String name, InputStream is, String contentType,
        long contentLength, Jets3tProperties jets3tProperties, boolean enableLiveMD5Hashing)
    {
        if (is == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        this.is = is;
        this.name = name;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.isLiveMD5HashingEnabled = enableLiveMD5Hashing;

        InputStream inputStream = is;
        while (true) {
            if (inputStream instanceof ProgressMonitoredInputStream) {
                progressMonitoredIS = (ProgressMonitoredInputStream) inputStream;
            }
            if (inputStream.markSupported()) {
                repeatableInputStream = inputStream;
                // Mark the start of this input stream so we can reset it if necessary.
                repeatableInputStream.mark(Integer.MAX_VALUE);
            }

            if (inputStream instanceof InputStreamWrapper) {
                inputStream = ((InputStreamWrapper) inputStream).getWrappedInputStream();
            } else {
                break;
            }
        }

        if (this.repeatableInputStream == null) {
            if (log.isDebugEnabled()) {
                log.debug("Wrapping non-repeatable input stream in a RepeatableInputStream");
            }
            int bufferSize = jets3tProperties.getIntProperty(
                "uploads.stream-retry-buffer-size", 131072);
            this.is = new RepeatableInputStream(is, bufferSize);

            this.repeatableInputStream = this.is;
        }

        MAX_BYTES_PER_SECOND = 1024 * jets3tProperties.getLongProperty("httpclient.read-throttle", 0);
    }


    public long getContentLength() {
      return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * @return
     * always returns true.
     * If the input stream is not actually repeatable, an IOException will be thrown
     * later by the {@link #writeRequest(OutputStream)} method when the repeat is attempted.
     */
    public boolean isRepeatable() {
        return true;
    }

    /**
     * Writes the request to the output stream. If the request is being repeated, the underlying
     * repeatable input stream will be reset with a call to {@link InputStream#reset()}.
     * <p>
     * If a {@link ProgressMonitoredInputStream} is attached, this monitor will be notified that
     * data is being repeated by being reset with
     * {@link ProgressMonitoredInputStream#resetProgressMonitor()}.
     */
    public void writeRequest(OutputStream out) throws IOException {
        if (bytesWritten > 0) {
            // This entity is being repeated.
            repeatableInputStream.reset();
            if (log.isWarnEnabled()) {
                log.warn("Repeating transmission of " + bytesWritten + " bytes");
            }

            // Notify progress monitored input stream that we've gone backwards (if one is attached)
            if (progressMonitoredIS != null) {
                progressMonitoredIS.resetProgressMonitor();
            }

            bytesWritten = 0;
        }

        MessageDigest messageDigest = null;
        if (isLiveMD5HashingEnabled) {
            try {
                messageDigest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                if (log.isWarnEnabled()) {
                    log.warn("Unable to calculate MD5 hash of data sent as algorithm is not available", e);
                }
            }
        }

        byte[] tmp = new byte[16384];
        int count = 0;

        while ((count = this.is.read(tmp)) >= 0) {
            throttle(count);

            bytesWritten += count;

            out.write(tmp, 0, count);

            if (messageDigest != null) {
                messageDigest.update(tmp, 0, count);
            }
        }

        if (messageDigest != null) {
            dataMD5Hash = messageDigest.digest();
            if (log.isDebugEnabled()) {
                log.debug("MD5 digest of data sent for '" + name + "' - B64:"
                + ServiceUtils.toBase64(dataMD5Hash) + " Hex:" + ServiceUtils.toHex(dataMD5Hash));
            }
        }
    }

    /**
     * @return
     * The MD5 digest of the data transmitted by this RequestEntity.
     */
    public byte[] getMD5DigestOfData() {
        if (dataMD5Hash != null) {
            return dataMD5Hash;
        } else {
            return new byte[0];
        }
    }

    /**
     * Throttles the speed at which data is written by this request entity to the
     * maximum rate in KB/s specified by {@link #MAX_BYTES_PER_SECOND}. The method
     * works by repeatedly delaying its completion until writing the requested number
     * of bytes will not exceed the imposed limit for the current second. The delay
     * imposed each time the completion is deferred is a random value between 0-250ms.
     * <p>
     * This method is static and is shared by all instances of this class, so the byte
     * rate limit applies for all currently active RepeatableRequestEntity instances.
     *
     * @param bytesToWrite
     * the count of bytes that will be written once this method returns.
     * @throws IOException
     * an exception is thrown if the sleep delay is interrupted.
     */
    protected static void throttle(int bytesToWrite) throws IOException {
        if (MAX_BYTES_PER_SECOND <= 0) {
            // No throttling is applied.
            return;
        }

        // All calculations are based on the current second time interval.
        long currentSecond = System.currentTimeMillis() / 1000;
        boolean willExceedThrottle;

        // All calculations are synchronized as this method can be called by multiple threads.
        synchronized (random) {
            // Check whether a new second has ticked over.
            boolean isCurrentSecond = currentSecond == currentSecondMonitored;

            // If a new second hasn't ticked over, we must limit the number of extra bytes
            // written this second.
            willExceedThrottle = isCurrentSecond
                && bytesWrittenThisSecond + bytesToWrite > MAX_BYTES_PER_SECOND;

            if (!isCurrentSecond) {
                // We are in a brand new second, it is safe to write some bytes.
                currentSecondMonitored = currentSecond;
                bytesWrittenThisSecond = bytesToWrite;
            }
            if (!willExceedThrottle) {
                // We can write bytes without exceeding the limit.
                bytesWrittenThisSecond += bytesToWrite;
            }
        }

        if (willExceedThrottle) {
            // Sleep for a random interval, then make a recursive call to see if we
            // will be allowed to write bytes then.
            try {
                Thread.sleep(random.nextInt(250));
            } catch (InterruptedException e) {
                throw new IOException("Throttling of transmission was interrupted");
            }
            throttle(bytesToWrite);
        }
    }

}
