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

import org.jets3t.service.multi.ThreadedStorageService;
import org.jets3t.service.multi.ThreadWatcher;

/**
 * Base class of all events produced by {@link ThreadedStorageService}.
 * <p>
 * Every event has an event code that indicates the state of a process when the event was
 * generated. The event code will also give a guide as to what information the event will contain.
 * <p>
 * The event codes, and their meanings, are:
 * <ul>
 * <li>EVENT_STARTED: An operation has commenced, but no work has yet been done.</li>
 * <li>EVENT_IN_PROGRESS: An operation is in progress. Progress events are fired at regular time
 *     intervals, and will include information about any work that have been completed
 *     as part of the overall operation.</li>
 * <li>EVENT_COMPLETED: An operation has completed, and all the work has been done.</li>
 * <li>EVENT_CANCELLED: An operation was started but has been cancelled before it could complete.
 *     If an operation is cancelled, this event will be fired instead of the EVENT_COMPLETED.</li>
 * <li>EVENT_ERROR: An operation has failed and an exception has been thrown. The error
 *     will be availble from {@link #getErrorCause()}</li>
 * <li>EVENT_IGNORED_ERRORS: One or more operations have failed but ,because the
 *     "threaded-service.ignore-exceptions-in-multi" JetS3t property value is set to true,
 *     the overall operation has continued. The errors will be available from
 *     {@link #getIgnoredErrors()}</li>
 * </ul>
 * <p>
 * EVENT_STARTED and EVENT_IN_PROGRESS events may include a {@link ThreadWatcher} object containing
 * detailed information about the progress of an operation, such as how many threads have
 * completed and, of uploads and downloads, how many bytes have been transferred at what speed and
 * how long until the transfer is complete.
 * <p>
 * See the event object specific to the operation you are performing for more details about the
 * information available in service events.
 *
 * @author James Murty
 */
public abstract class ServiceEvent {
    public static final int EVENT_ERROR = 0;
    public static final int EVENT_STARTED = 1;
    public static final int EVENT_COMPLETED = 2;
    public static final int EVENT_IN_PROGRESS = 3;
    public static final int EVENT_CANCELLED = 4;
    public static final int EVENT_IGNORED_ERRORS = 5;

    private int eventCode = 0;
    private Object uniqueOperationId = null;
    private Throwable t = null;
    private ThreadWatcher threadWatcher = null;
    private Throwable[] ignoredErrors = null;

    protected ServiceEvent(int eventCode, Object uniqueOperationId) {
        this.eventCode = eventCode;
        this.uniqueOperationId = uniqueOperationId;
    }

    protected void setThreadWatcher(ThreadWatcher threadWatcher) {
        this.threadWatcher = threadWatcher;
    }

    protected void setErrorCause(Throwable t) {
        this.t = t;
    }

    protected void setIgnoredErrors(Throwable[] ignoredErrors) {
        this.ignoredErrors = ignoredErrors;
    }

    public Object getUniqueOperationId() {
        return uniqueOperationId;
    }

    /**
     * @return
     * the event code, which will match one of this class's public static EVENT_XXX variables.
     */
    public int getEventCode() {
        return eventCode;
    }

    /**
     * @return
     * the error that caused an operation to fail.
     * @throws IllegalStateException
     * an error cause can only be retrieved from an EVENT_ERROR event.
     */
    public Throwable getErrorCause() throws IllegalStateException {
        if (eventCode != EVENT_ERROR) {
            throw new IllegalStateException("Error Cause is only available from EVENT_ERROR events");
        }
        return t;
    }

    /**
     * @return
     * a list of one or more errors that occurred during an operation, but which were
     * ignored at the time (so the overall operation continued).
     * @throws IllegalStateException
     * ignored errors can only be retrieved from an EVENT_IGNORED_ERRORS event.
     */
    public Throwable[] getIgnoredErrors() throws IllegalStateException {
        if (eventCode != EVENT_IGNORED_ERRORS) {
            throw new IllegalStateException("Ignored errors are only available from EVENT_IGNORED_ERRORS events");
        }
        return ignoredErrors;
    }

    /**
     * @return
     * a thread watcher object containing information about the progress of an operation.
     * @throws IllegalStateException
     * a thread watcher can only be retrieved from an EVENET_STARTED or EVENT_IN_PROGRESS event.
     */
    public ThreadWatcher getThreadWatcher() throws IllegalStateException {
        if (eventCode != EVENT_STARTED && eventCode != EVENT_IN_PROGRESS) {
            throw new IllegalStateException("Thread Watcher is only available from EVENT_STARTED "
                + "or EVENT_IN_PROGRESS events");
        }
        return threadWatcher;
    }

    @Override
    public String toString() {
        String eventText = eventCode == EVENT_ERROR ? "EVENT_ERROR"
            : eventCode == EVENT_STARTED ? "EVENT_STARTED"
                : eventCode == EVENT_COMPLETED ? "EVENT_COMPLETED"
                    : eventCode == EVENT_IN_PROGRESS ? "EVENT_IN_PROGRESS"
                        : eventCode == EVENT_CANCELLED ? "EVENT_CANCELLED"
                            : eventCode == EVENT_IGNORED_ERRORS ? "EVENT_IGNORED_ERRORS"
                                : "Unrecognised event status code: " + eventCode;

        if (eventCode == EVENT_ERROR && getErrorCause() != null) {
            return eventText + " " + getErrorCause();
        } else {
            return eventText;
        }
    }


}
