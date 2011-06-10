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
package org.jets3t.service.multi.s3;

import org.jets3t.service.model.MultipartUpload;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.multi.ThreadWatcher;
import org.jets3t.service.multi.event.ServiceEvent;

/**
 * Multi-threaded service event fired by
 * {@link ThreadedS3Service#multipartStartUploads(String, java.util.List)}.
 * <p>
 * EVENT_IN_PROGRESS events include an array of the {@link S3Object}s that have been created
 * since the last progress event was fired. These objects are available via
 * {@link #getStartedUploads()}.
 * <p>
 * EVENT_CANCELLED events include an array of the {@link S3Object}s that had not been created
 * before the operation was cancelled. These objects are available via
 * {@link #getCancelledObjects()}.
 *
 * @author James Murty
 */
public class MultipartStartsEvent extends ServiceEvent {
    private MultipartUpload[] completedMultipartUploads = null;
    private StorageObject[] incompletedObjects = null;

    private MultipartStartsEvent(int eventCode, Object uniqueOperationId) {
        super(eventCode, uniqueOperationId);
    }


    public static MultipartStartsEvent newErrorEvent(Throwable t, Object uniqueOperationId) {
        MultipartStartsEvent event = new MultipartStartsEvent(EVENT_ERROR, uniqueOperationId);
        event.setErrorCause(t);
        return event;
    }

    public static MultipartStartsEvent newStartedEvent(ThreadWatcher threadWatcher, Object uniqueOperationId) {
        MultipartStartsEvent event = new MultipartStartsEvent(EVENT_STARTED, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        return event;
    }

    public static MultipartStartsEvent newInProgressEvent(ThreadWatcher threadWatcher,
        MultipartUpload[] completedMultipartUploads, Object uniqueOperationId)
    {
        MultipartStartsEvent event = new MultipartStartsEvent(EVENT_IN_PROGRESS, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        event.setCompletedUploads(completedMultipartUploads);
        return event;
    }

    public static MultipartStartsEvent newCompletedEvent(Object uniqueOperationId) {
        MultipartStartsEvent event = new MultipartStartsEvent(EVENT_COMPLETED, uniqueOperationId);
        return event;
    }

    public static MultipartStartsEvent newCancelledEvent(StorageObject[] incompletedObjects, Object uniqueOperationId) {
        MultipartStartsEvent event = new MultipartStartsEvent(EVENT_CANCELLED, uniqueOperationId);
        event.setIncompletedObjects(incompletedObjects);
        return event;
    }

    public static MultipartStartsEvent newIgnoredErrorsEvent(ThreadWatcher threadWatcher,
        Throwable[] ignoredErrors, Object uniqueOperationId)
    {
        MultipartStartsEvent event = new MultipartStartsEvent(EVENT_IGNORED_ERRORS, uniqueOperationId);
        event.setIgnoredErrors(ignoredErrors);
        return event;
    }


    private void setIncompletedObjects(StorageObject[] objects) {
        this.incompletedObjects = objects;
    }

    private void setCompletedUploads(MultipartUpload[] uploads) {
        this.completedMultipartUploads = uploads;
    }

    /**
     * @return
     * the {@link MultipartUpload}s that have been started since the last progress event was fired.
     * @throws IllegalStateException
     * created objects are only available from EVENT_IN_PROGRESS events.
     */
    public MultipartUpload[] getStartedUploads() throws IllegalStateException {
        if (getEventCode() != EVENT_IN_PROGRESS) {
            throw new IllegalStateException("Started Objects are only available from EVENT_IN_PROGRESS events");
        }
        return completedMultipartUploads;
    }

    /**
     * @return
     * the {@link StorageObject}s that were not created before the operation was cancelled.
     * @throws IllegalStateException
     * cancelled objects are only available from EVENT_CANCELLED events.
     */
    public StorageObject[] getCancelledObjects() throws IllegalStateException {
        if (getEventCode() != EVENT_CANCELLED) {
            throw new IllegalStateException("Cancelled Objects are  only available from EVENT_CANCELLED events");
        }
        return incompletedObjects;
    }

}
