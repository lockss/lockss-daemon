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
package org.jets3t.service.multithread;

/**
 * Simple implementation of {@link S3ServiceEventListener} to listen for events produced by
 * {@link S3ServiceMulti}.
 * <p>
 * By default this adaptor does nothing but store the first Error event it comes across, if any,
 * and make it available through {@link #getErrorThrown}.
 * </p>
 * <p>
 * The behaviour of this class can be specialised by over-riding the appropriate
 * <tt>s3ServiceEventPerformed</tt> methods, though always be sure to call the <b>super</b>
 * version of these methods if you are relying on the default error-trapping functions of this
 * class.
 * </p>
 *
 * @author James Murty
 */
public class S3ServiceEventAdaptor implements S3ServiceEventListener {

    private Throwable t[] = new Throwable[1];

    public void s3ServiceEventPerformed(ListObjectsEvent event) {
        storeThrowable(event);
    }

    public void s3ServiceEventPerformed(CreateObjectsEvent event) {
        storeThrowable(event);
    }

    public void s3ServiceEventPerformed(CopyObjectsEvent event) {
        storeThrowable(event);
    }

    public void s3ServiceEventPerformed(CreateBucketsEvent event) {
        storeThrowable(event);
    }

    public void s3ServiceEventPerformed(DeleteObjectsEvent event) {
        storeThrowable(event);
    }

    public void s3ServiceEventPerformed(DeleteVersionedObjectsEvent event) {
        storeThrowable(event);
    }

    public void s3ServiceEventPerformed(GetObjectsEvent event) {
        storeThrowable(event);
    }

    public void s3ServiceEventPerformed(GetObjectHeadsEvent event) {
        storeThrowable(event);
    }

    public void s3ServiceEventPerformed(LookupACLEvent event) {
        storeThrowable(event);
    }

    public void s3ServiceEventPerformed(UpdateACLEvent event) {
        storeThrowable(event);
    }

    public void s3ServiceEventPerformed(DownloadObjectsEvent event) {
        storeThrowable(event);
    }

    protected void storeThrowable(ServiceEvent event) {
        if (t[0] == null && event.getEventCode() == ServiceEvent.EVENT_ERROR) {
            t[0] = event.getErrorCause();
        }
    }

    /**
     * @return
     * true if an event has resulted in an exception.
     */
    public boolean wasErrorThrown() {
        return t[0] != null;
    }

    /**
     * @return
     * the first error thrown by an event, or null if no error has been thrown.
     */
    public Throwable getErrorThrown() {
        return t[0];
    }

}
