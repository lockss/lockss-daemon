/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008 James Murty
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

import java.util.List;

import org.jets3t.service.StorageObjectsChunk;
import org.jets3t.service.multi.ThreadedStorageService;
import org.jets3t.service.multi.ThreadWatcher;

/**
 * Multi-threaded service event fired by
 * {@link ThreadedStorageService#listObjects(String, String[], String, long)}.
 * <p>
 * EVENT_IN_PROGRESS events include a List of {@link StorageObjectsChunk} objects
 * that contain information about the objects and common-prefixes for the
 * bucket listing operation. Each chunk object will contain a prefix identifying
 * the prefix value used by the listing operation that produced the chunk.
 * These objects are available via {@link #getChunkList()}.
 *
 * @author James Murty
 */
public class ListObjectsEvent extends ServiceEvent {
    private List<StorageObjectsChunk> chunkList = null;

    private ListObjectsEvent(int eventCode, Object uniqueOperationId) {
        super(eventCode, uniqueOperationId);
    }


    public static ListObjectsEvent newErrorEvent(Throwable t, Object uniqueOperationId) {
        ListObjectsEvent event = new ListObjectsEvent(EVENT_ERROR, uniqueOperationId);
        event.setErrorCause(t);
        return event;
    }

    public static ListObjectsEvent newStartedEvent(ThreadWatcher threadWatcher, Object uniqueOperationId) {
        ListObjectsEvent event = new ListObjectsEvent(EVENT_STARTED, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        return event;
    }

    public static ListObjectsEvent newInProgressEvent(ThreadWatcher threadWatcher,
        List<StorageObjectsChunk> chunkList, Object uniqueOperationId)
    {
        ListObjectsEvent event = new ListObjectsEvent(EVENT_IN_PROGRESS, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        event.setChunkList(chunkList);
        return event;
    }

    public static ListObjectsEvent newCompletedEvent(Object uniqueOperationId) {
        ListObjectsEvent event = new ListObjectsEvent(EVENT_COMPLETED, uniqueOperationId);
        return event;
    }

    public static ListObjectsEvent newCancelledEvent(Object uniqueOperationId) {
        ListObjectsEvent event = new ListObjectsEvent(EVENT_CANCELLED, uniqueOperationId);
        return event;
    }

    public static ListObjectsEvent newIgnoredErrorsEvent(ThreadWatcher threadWatcher,
        Throwable[] ignoredErrors, Object uniqueOperationId)
    {
        ListObjectsEvent event = new ListObjectsEvent(EVENT_IGNORED_ERRORS, uniqueOperationId);
        event.setIgnoredErrors(ignoredErrors);
        return event;
    }


    private void setChunkList(List<StorageObjectsChunk> chunkList) {
        this.chunkList = chunkList;
    }


    /**
     * @return
     * a list of the {@link StorageObjectsChunk}s that have been generated since the
     * last progress event was fired.
     * @throws IllegalStateException
     * listed objects are only available from EVENT_IN_PROGRESS events.
     */
    public List<StorageObjectsChunk> getChunkList() throws IllegalStateException {
        if (getEventCode() != EVENT_IN_PROGRESS) {
            throw new IllegalStateException("Chunk list is only available from EVENT_IN_PROGRESS events");
        }
        return this.chunkList;
    }

}
