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

import org.jets3t.service.model.S3Bucket;

/**
 * Multi-threaded service event fired by {@link S3ServiceMulti#createBuckets(S3Bucket[])}.
 * <p>
 * EVENT_IN_PROGRESS events include an array of the {@link S3Bucket}s that have been created
 * since the last progress event was fired. These objects are available via
 * {@link #getCreatedBuckets()}.
 * <p>
 * EVENT_CANCELLED events include an array of the {@link S3Bucket}s that had not been created
 * before the operation was cancelled. These objects are available via
 * {@link #getCancelledBuckets()}.
 *
 * @author James Murty
 */
public class CreateBucketsEvent extends ServiceEvent {
    private S3Bucket[] buckets = null;

    private CreateBucketsEvent(int eventCode, Object uniqueOperationId) {
        super(eventCode, uniqueOperationId);
    }


    public static CreateBucketsEvent newErrorEvent(Throwable t, Object uniqueOperationId) {
        CreateBucketsEvent event = new CreateBucketsEvent(EVENT_ERROR, uniqueOperationId);
        event.setErrorCause(t);
        return event;
    }

    public static CreateBucketsEvent newStartedEvent(ThreadWatcher threadWatcher, Object uniqueOperationId) {
        CreateBucketsEvent event = new CreateBucketsEvent(EVENT_STARTED, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        return event;
    }

    public static CreateBucketsEvent newInProgressEvent(ThreadWatcher threadWatcher,
        S3Bucket[] completedBuckets, Object uniqueOperationId)
    {
        CreateBucketsEvent event = new CreateBucketsEvent(EVENT_IN_PROGRESS, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        event.setBuckets(completedBuckets);
        return event;
    }

    public static CreateBucketsEvent newCompletedEvent(Object uniqueOperationId) {
        CreateBucketsEvent event = new CreateBucketsEvent(EVENT_COMPLETED, uniqueOperationId);
        return event;
    }

    public static CreateBucketsEvent newCancelledEvent(S3Bucket[] incompletedBuckets, Object uniqueOperationId) {
        CreateBucketsEvent event = new CreateBucketsEvent(EVENT_CANCELLED, uniqueOperationId);
        event.setBuckets(incompletedBuckets);
        return event;
    }

    public static CreateBucketsEvent newIgnoredErrorsEvent(ThreadWatcher threadWatcher,
        Throwable[] ignoredErrors, Object uniqueOperationId)
    {
        CreateBucketsEvent event = new CreateBucketsEvent(EVENT_IGNORED_ERRORS, uniqueOperationId);
        event.setIgnoredErrors(ignoredErrors);
        return event;
    }


    private void setBuckets(S3Bucket[] buckets) {
        this.buckets = buckets;
    }


    /**
     * @return
     * the S3Buckets that have been created since the last progress event was fired.
     * @throws IllegalStateException
     * created buckets are only available from EVENT_IN_PROGRESS events.
     */
    public S3Bucket[] getCreatedBuckets() throws IllegalStateException {
        if (getEventCode() != EVENT_IN_PROGRESS) {
            throw new IllegalStateException("Created Buckets are only available from EVENT_IN_PROGRESS events");
        }
        return buckets;
    }

    /**
     * @return
     * the S3Buckets that had not been created before the operation was cancelled.
     * @throws IllegalStateException
     * cancelled buckets are only available from EVENT_CANCELLED events.
     */
    public S3Bucket[] getCancelledBuckets() throws IllegalStateException {
        if (getEventCode() != EVENT_CANCELLED) {
            throw new IllegalStateException("Cancelled Buckets are  only available from EVENT_CANCELLED events");
        }
        return buckets;
    }

}
