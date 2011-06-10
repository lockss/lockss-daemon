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
package org.jets3t.service.multi.event;

import org.jets3t.service.model.StorageObject;
import org.jets3t.service.multi.ThreadedStorageService;
import org.jets3t.service.multi.ThreadWatcher;

/**
 * Multi-threaded service event fired by
 * {@link ThreadedStorageService#downloadObjects(String, org.jets3t.service.multi.DownloadPackage[])}.
 * <p>
 * EVENT_IN_PROGRESS events include an array of the {@link StorageObject}s that have finished downloading
 * since the last progress event was fired. These objects are available via
 * {@link #getDownloadedObjects()}.
 * <p>
 * EVENT_CANCELLED events include an array of the {@link StorageObject}s that were not downloaded
 * before the operation was cancelled. These objects are available via
 * {@link #getCancelledObjects()}.
 *
 * @author James Murty
 */
public class DownloadObjectsEvent extends ServiceEvent {
    private StorageObject[] objects = null;

    private DownloadObjectsEvent(int eventCode, Object uniqueOperationId) {
        super(eventCode, uniqueOperationId);
    }


    public static DownloadObjectsEvent newErrorEvent(Throwable t, Object uniqueOperationId) {
        DownloadObjectsEvent event = new DownloadObjectsEvent(EVENT_ERROR, uniqueOperationId);
        event.setErrorCause(t);
        return event;
    }

    public static DownloadObjectsEvent newStartedEvent(ThreadWatcher threadWatcher, Object uniqueOperationId) {
        DownloadObjectsEvent event = new DownloadObjectsEvent(EVENT_STARTED, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        return event;
    }

    public static DownloadObjectsEvent newInProgressEvent(ThreadWatcher threadWatcher,
        StorageObject[] completedObjects, Object uniqueOperationId)
    {
        DownloadObjectsEvent event = new DownloadObjectsEvent(EVENT_IN_PROGRESS, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        event.setObjects(completedObjects);
        return event;
    }

    public static DownloadObjectsEvent newCompletedEvent(Object uniqueOperationId) {
        DownloadObjectsEvent event = new DownloadObjectsEvent(EVENT_COMPLETED, uniqueOperationId);
        return event;
    }

    public static DownloadObjectsEvent newCancelledEvent(StorageObject[] incompletedObjects, Object uniqueOperationId) {
        DownloadObjectsEvent event = new DownloadObjectsEvent(EVENT_CANCELLED, uniqueOperationId);
        event.setObjects(incompletedObjects);
        return event;
    }

    public static DownloadObjectsEvent newIgnoredErrorsEvent(ThreadWatcher threadWatcher,
        Throwable[] ignoredErrors, Object uniqueOperationId)
    {
        DownloadObjectsEvent event = new DownloadObjectsEvent(EVENT_IGNORED_ERRORS, uniqueOperationId);
        event.setIgnoredErrors(ignoredErrors);
        return event;
    }


    private void setObjects(StorageObject[] objects) {
        this.objects = objects;
    }

    /**
     * @return
     * the {@link StorageObject}s that have finished downloading since the last progress event was fired.
     * @throws IllegalStateException
     * downloaded objects are only available from EVENT_IN_PROGRESS events.
     */
    public StorageObject[] getDownloadedObjects() throws IllegalStateException {
        if (getEventCode() != EVENT_IN_PROGRESS) {
            throw new IllegalStateException("Downloaded Objects are only available from EVENT_IN_PROGRESS events");
        }
        return objects;
    }

    /**
     * @return
     * the {@link StorageObject}s that were not downloaded before the operation was cancelled.
     * @throws IllegalStateException
     * cancelled objects are only available from EVENT_CANCELLED events.
     */
    public StorageObject[] getCancelledObjects() throws IllegalStateException {
        if (getEventCode() != EVENT_CANCELLED) {
            throw new IllegalStateException("Cancelled Objects are  only available from EVENT_CANCELLED events");
        }
        return objects;
    }

}
