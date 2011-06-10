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

import org.jets3t.service.model.MultipartCompleted;
import org.jets3t.service.model.MultipartUpload;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.multi.ThreadWatcher;
import org.jets3t.service.multi.event.ServiceEvent;

/**
 * Multi-threaded service event fired by
 * {@link ThreadedS3Service#multipartCompleteUploads(java.util.List)}.
 * <p>
 * EVENT_IN_PROGRESS events include an array of the {@link S3Object}s that have been created
 * since the last progress event was fired. These objects are available via
 * {@link #getCompletedUploads()}.
 * <p>
 * EVENT_CANCELLED events include an array of the {@link S3Object}s that had not been created
 * before the operation was cancelled. These objects are available via
 * {@link #getCancelledUploads()}.
 *
 * @author James Murty
 */
public class MultipartCompletesEvent extends ServiceEvent {
    private MultipartUpload[] incompleteUploads = null;
    private MultipartCompleted[] completedUploads = null;

    private MultipartCompletesEvent(int eventCode, Object uniqueOperationId) {
        super(eventCode, uniqueOperationId);
    }


    public static MultipartCompletesEvent newErrorEvent(Throwable t, Object uniqueOperationId) {
        MultipartCompletesEvent event = new MultipartCompletesEvent(EVENT_ERROR, uniqueOperationId);
        event.setErrorCause(t);
        return event;
    }

    public static MultipartCompletesEvent newStartedEvent(ThreadWatcher threadWatcher, Object uniqueOperationId) {
        MultipartCompletesEvent event = new MultipartCompletesEvent(EVENT_STARTED, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        return event;
    }

    public static MultipartCompletesEvent newInProgressEvent(ThreadWatcher threadWatcher,
        MultipartCompleted[] completedUploads, Object uniqueOperationId)
    {
        MultipartCompletesEvent event = new MultipartCompletesEvent(EVENT_IN_PROGRESS, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        event.setCompleteUploads(completedUploads);
        return event;
    }

    public static MultipartCompletesEvent newCompletedEvent(Object uniqueOperationId) {
        MultipartCompletesEvent event = new MultipartCompletesEvent(EVENT_COMPLETED, uniqueOperationId);
        return event;
    }

    public static MultipartCompletesEvent newCancelledEvent(MultipartUpload[] incompletedUploads,
        Object uniqueOperationId)
    {
        MultipartCompletesEvent event = new MultipartCompletesEvent(EVENT_CANCELLED, uniqueOperationId);
        event.setIncompleteUploads(incompletedUploads);
        return event;
    }

    public static MultipartCompletesEvent newIgnoredErrorsEvent(ThreadWatcher threadWatcher,
        Throwable[] ignoredErrors, Object uniqueOperationId)
    {
        MultipartCompletesEvent event = new MultipartCompletesEvent(EVENT_IGNORED_ERRORS, uniqueOperationId);
        event.setIgnoredErrors(ignoredErrors);
        return event;
    }


    private void setIncompleteUploads(MultipartUpload[] uploads) {
        this.incompleteUploads = uploads;
    }

    private void setCompleteUploads(MultipartCompleted[] completed) {
        this.completedUploads = completed;
    }

    /**
     * @return
     * the {@link MultipartUpload}s that have been completed since the last progress event was fired.
     * @throws IllegalStateException
     * created objects are only available from EVENT_IN_PROGRESS events.
     */
    public MultipartCompleted[] getCompletedUploads() throws IllegalStateException {
        if (getEventCode() != EVENT_IN_PROGRESS) {
            throw new IllegalStateException("Started Objects are only available from EVENT_IN_PROGRESS events");
        }
        return completedUploads;
    }

    /**
     * @return
     * the {@link MultipartUpload}s that were not completed before the operation was cancelled.
     * @throws IllegalStateException
     * cancelled objects are only available from EVENT_CANCELLED events.
     */
    public MultipartUpload[] getCancelledUploads() throws IllegalStateException {
        if (getEventCode() != EVENT_CANCELLED) {
            throw new IllegalStateException("Cancelled Objects are  only available from EVENT_CANCELLED events");
        }
        return incompleteUploads;
    }

}
