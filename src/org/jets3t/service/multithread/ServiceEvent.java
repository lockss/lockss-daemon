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

import org.jets3t.service.multi.ThreadWatcher;


/**
 * Base class of all events produced by {@link S3ServiceMulti}.
 * <p>
 * Every event has an event code that indicates the state of a process when the event was
 * generated. The event code will also give a guide as to what information the event will contain.
 * <p>
 * The event codes, and their meanings, are:
 * <ul>
 * <li>EVENT_STARTED: An S3 operation has commenced, but no work has yet been done.</li>
 * <li>EVENT_IN_PROGRESS: An S3 operation is in progress. Progress events are fired at regular time
 *     intervals, and will include information about any work that have been completed
 *     as part of the overall operation.</li>
 * <li>EVENT_COMPLETED: An S3 operation has completed, and all the work has been done.</li>
 * <li>EVENT_CANCELLED: An S3 operation was started but has been cancelled before it could complete.
 *     If an operation is cancelled, this event will be fired instead of the EVENT_COMPLETED.</li>
 * <li>EVENT_ERROR: An S3 operation has failed and an exception has been thrown. The error
 *     will be availble from {@link #getErrorCause()}</li>
 * <li>EVENT_IGNORED_ERRORS: One or more operations have failed but ,because the
 *     "s3service.ignore-exceptions-in-multi" JetS3t property value is set to true,
 *     the overall operation has continued. The errors will be available from
 *     {@link #getIgnoredErrors()}</li>
 * </ul>
 * <p>
 * EVENT_STARTED and EVENT_IN_PROGRESS events may include a {@link ThreadWatcher} object containing
 * detailed information about the progress of an S3 operation, such as how many threads have
 * completed and, of uploads and downloads, how many bytes have been transferred at what speed and
 * how long until the transfer is complete.
 * <p>
 * See the event object specific to the operation you are performing for more details about the
 * information available in service events.
 *
 * @author James Murty
 * @deprecated 0.8.0 use {@link org.jets3t.service.multi.event.ServiceEvent} instead.
 */
@Deprecated
public abstract class ServiceEvent extends org.jets3t.service.multi.event.ServiceEvent {

    protected ServiceEvent(int eventCode, Object uniqueOperationId) {
        super(eventCode, uniqueOperationId);
    }

    @Override
    public org.jets3t.service.multithread.ThreadWatcher getThreadWatcher() throws IllegalStateException {
        return (org.jets3t.service.multithread.ThreadWatcher) super.getThreadWatcher();
    }

}
