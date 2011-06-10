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
import org.jets3t.service.model.S3Object;

/**
 * Multi-threaded service event fired by {@link S3ServiceMulti#deleteObjects(S3Bucket, S3Object[])}.
 * <p>
 * EVENT_IN_PROGRESS events include an array of the {@link S3Object}s that have been deleted
 * since the last progress event was fired. These objects are available via
 * {@link #getDeletedObjects()}.
 * <p>
 * EVENT_CANCELLED events include an array of the {@link S3Object}s that had not been deleted
 * before the operation was cancelled. These objects are available via
 * {@link #getCancelledObjects()}.
 *
 * @author James Murty
 */
public class DeleteObjectsEvent extends ServiceEvent {
    private S3Object[] objects = null;

    private DeleteObjectsEvent(int eventCode, Object uniqueOperationId) {
        super(eventCode, uniqueOperationId);
    }

    public static DeleteObjectsEvent newErrorEvent(Throwable t, Object uniqueOperationId) {
        DeleteObjectsEvent event = new DeleteObjectsEvent(EVENT_ERROR, uniqueOperationId);
        event.setErrorCause(t);
        return event;
    }

    public static DeleteObjectsEvent newStartedEvent(ThreadWatcher threadWatcher, Object uniqueOperationId) {
        DeleteObjectsEvent event = new DeleteObjectsEvent(EVENT_STARTED, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        return event;
    }

    public static DeleteObjectsEvent newInProgressEvent(ThreadWatcher threadWatcher,
        S3Object[] deletedObjects, Object uniqueOperationId)
    {
        DeleteObjectsEvent event = new DeleteObjectsEvent(EVENT_IN_PROGRESS, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        event.setObjects(deletedObjects);
        return event;
    }

    public static DeleteObjectsEvent newCompletedEvent(Object uniqueOperationId) {
        DeleteObjectsEvent event = new DeleteObjectsEvent(EVENT_COMPLETED, uniqueOperationId);
        return event;
    }

    public static DeleteObjectsEvent newCancelledEvent(S3Object[] remainingObjects, Object uniqueOperationId) {
        DeleteObjectsEvent event = new DeleteObjectsEvent(EVENT_CANCELLED, uniqueOperationId);
        event.setObjects(remainingObjects);
        return event;
    }

    public static DeleteObjectsEvent newIgnoredErrorsEvent(ThreadWatcher threadWatcher,
        Throwable[] ignoredErrors, Object uniqueOperationId)
    {
        DeleteObjectsEvent event = new DeleteObjectsEvent(EVENT_IGNORED_ERRORS, uniqueOperationId);
        event.setIgnoredErrors(ignoredErrors);
        return event;
    }


    private void setObjects(S3Object[] objects) {
        this.objects = objects;
    }

    /**
     * @return
     * the S3Objects that have been deleted since the last progress event was fired.
     * @throws IllegalStateException
     * deleted objects are only available from EVENT_IN_PROGRESS events.
     */
    public S3Object[] getDeletedObjects() throws IllegalStateException {
        if (getEventCode() != EVENT_IN_PROGRESS) {
            throw new IllegalStateException("Deleted Objects are only available from EVENT_IN_PROGRESS events");
        }
        return objects;
    }

    /**
     * @return
     * the S3Objects that were not deleted before the operation was cancelled.
     * @throws IllegalStateException
     * cancelled objects are only available from EVENT_CANCELLED events.
     */
    public S3Object[] getCancelledObjects() throws IllegalStateException {
        if (getEventCode() != EVENT_CANCELLED) {
            throw new IllegalStateException("Cancelled Objects are  only available from EVENT_CANCELLED events");
        }
        return objects;
    }

}
