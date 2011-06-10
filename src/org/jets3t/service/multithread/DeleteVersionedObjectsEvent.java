/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2010 James Murty
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

import org.jets3t.service.model.S3Version;

/**
 * Multi-threaded service event fired by
 * {@link S3ServiceMulti#deleteVersionsOfObject(String[], String, String)} or
 * {@link S3ServiceMulti#deleteVersionsOfObjectWithMFA(String[], String, String, String, String)}.
 * <p>
 * EVENT_IN_PROGRESS events include an array of the {@link S3Version}s that have been deleted
 * since the last progress event was fired. These versions are available via
 * {@link #getDeletedVersions()}.
 * <p>
 * EVENT_CANCELLED events include an array of the {@link S3Version}s that had not been deleted
 * before the operation was cancelled. These versions are available via
 * {@link #getCancelledVersions()}.
 *
 * @author James Murty
 */
public class DeleteVersionedObjectsEvent extends ServiceEvent {
    private S3Version[] versions = null;

    private DeleteVersionedObjectsEvent(int eventCode, Object uniqueOperationId) {
        super(eventCode, uniqueOperationId);
    }

    public static DeleteVersionedObjectsEvent newErrorEvent(Throwable t, Object uniqueOperationId) {
        DeleteVersionedObjectsEvent event = new DeleteVersionedObjectsEvent(EVENT_ERROR, uniqueOperationId);
        event.setErrorCause(t);
        return event;
    }

    public static DeleteVersionedObjectsEvent newStartedEvent(ThreadWatcher threadWatcher, Object uniqueOperationId) {
        DeleteVersionedObjectsEvent event = new DeleteVersionedObjectsEvent(EVENT_STARTED, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        return event;
    }

    public static DeleteVersionedObjectsEvent newInProgressEvent(ThreadWatcher threadWatcher,
        S3Version[] deletedVersions, Object uniqueOperationId)
    {
        DeleteVersionedObjectsEvent event = new DeleteVersionedObjectsEvent(EVENT_IN_PROGRESS, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        event.setObjects(deletedVersions);
        return event;
    }

    public static DeleteVersionedObjectsEvent newCompletedEvent(Object uniqueOperationId) {
        DeleteVersionedObjectsEvent event = new DeleteVersionedObjectsEvent(EVENT_COMPLETED, uniqueOperationId);
        return event;
    }

    public static DeleteVersionedObjectsEvent newCancelledEvent(S3Version[] remainingVersions, Object uniqueOperationId) {
        DeleteVersionedObjectsEvent event = new DeleteVersionedObjectsEvent(EVENT_CANCELLED, uniqueOperationId);
        event.setObjects(remainingVersions);
        return event;
    }

    public static DeleteVersionedObjectsEvent newIgnoredErrorsEvent(ThreadWatcher threadWatcher,
        Throwable[] ignoredErrors, Object uniqueOperationId)
    {
        DeleteVersionedObjectsEvent event = new DeleteVersionedObjectsEvent(EVENT_IGNORED_ERRORS, uniqueOperationId);
        event.setIgnoredErrors(ignoredErrors);
        return event;
    }


    private void setObjects(S3Version[] versions) {
        this.versions = versions;
    }

    /**
     * @return
     * the S3Versions that have been deleted since the last progress event was fired.
     * @throws IllegalStateException
     * deleted versions are only available from EVENT_IN_PROGRESS events.
     */
    public S3Version[] getDeletedVersions() throws IllegalStateException {
        if (getEventCode() != EVENT_IN_PROGRESS) {
            throw new IllegalStateException("Deleted versions are only available from EVENT_IN_PROGRESS events");
        }
        return versions;
    }

    /**
     * @return
     * the S3Versions that were not deleted before the operation was cancelled.
     * @throws IllegalStateException
     * cancelled objects are only available from EVENT_CANCELLED events.
     */
    public S3Version[] getCancelledVersions() throws IllegalStateException {
        if (getEventCode() != EVENT_CANCELLED) {
            throw new IllegalStateException("Cancelled versions are  only available from EVENT_CANCELLED events");
        }
        return versions;
    }

}
