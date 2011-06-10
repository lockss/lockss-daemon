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
 * {@link ThreadedStorageService#getObjectsHeads(String, String[])}.
 * <p>
 * EVENT_IN_PROGRESS events include an array of the {@link StorageObject}s that have been retrieved
 * since the last progress event was fired. These objects are available via
 * {@link #getCompletedObjects()}.
 * <p>
 * EVENT_CANCELLED events include an array of the {@link StorageObject}s that had not been retrieved
 * before the operation was cancelled. These objects are available via
 * {@link #getCancelledObjects()}.
 *
 * @author James Murty
 */
public class GetObjectHeadsEvent extends ServiceEvent {
    private StorageObject[] objects = null;

    private GetObjectHeadsEvent(int eventCode, Object uniqueOperationId) {
        super(eventCode, uniqueOperationId);
    }


    public static GetObjectHeadsEvent newErrorEvent(Throwable t, Object uniqueOperationId) {
        GetObjectHeadsEvent event = new GetObjectHeadsEvent(EVENT_ERROR, uniqueOperationId);
        event.setErrorCause(t);
        return event;
    }

    public static GetObjectHeadsEvent newStartedEvent(ThreadWatcher threadWatcher, Object uniqueOperationId) {
        GetObjectHeadsEvent event = new GetObjectHeadsEvent(EVENT_STARTED, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        return event;
    }

    public static GetObjectHeadsEvent newInProgressEvent(ThreadWatcher threadWatcher,
        StorageObject[] completedObjects, Object uniqueOperationId)
    {
        GetObjectHeadsEvent event = new GetObjectHeadsEvent(EVENT_IN_PROGRESS, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        event.setObjects(completedObjects);
        return event;
    }

    public static GetObjectHeadsEvent newCompletedEvent(Object uniqueOperationId) {
        GetObjectHeadsEvent event = new GetObjectHeadsEvent(EVENT_COMPLETED, uniqueOperationId);
        return event;
    }

    public static GetObjectHeadsEvent newCancelledEvent(StorageObject[] incompletedObjects, Object uniqueOperationId) {
        GetObjectHeadsEvent event = new GetObjectHeadsEvent(EVENT_CANCELLED, uniqueOperationId);
        event.setObjects(incompletedObjects);
        return event;
    }

    public static GetObjectHeadsEvent newIgnoredErrorsEvent(ThreadWatcher threadWatcher,
        Throwable[] ignoredErrors, Object uniqueOperationId)
    {
        GetObjectHeadsEvent event = new GetObjectHeadsEvent(EVENT_IGNORED_ERRORS, uniqueOperationId);
        event.setIgnoredErrors(ignoredErrors);
        return event;
    }


    private void setObjects(StorageObject[] objects) {
        this.objects = objects;
    }

    /**
     * @return
     * the {@link StorageObject}s that have been retrieved since the last progress event was fired.
     * @throws IllegalStateException
     * completed objects are only available from EVENT_IN_PROGRESS events.
     */
    public StorageObject[] getCompletedObjects() throws IllegalStateException {
        if (getEventCode() != EVENT_IN_PROGRESS) {
            throw new IllegalStateException("Completed Objects are only available from EVENT_IN_PROGRESS events");
        }
        return objects;
    }

    /**
     * @return
     * the {@link StorageObject}s that were not retrieved before the operation was cancelled.
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
