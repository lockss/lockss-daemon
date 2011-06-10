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

import org.jets3t.service.io.BytesProgressWatcher;

/**
 * A thread watcher is associated with a multi-threaded S3 operation and contains information about
 * the progress of the operation.
 * <p>
 * As a minimum, this object will contain a count of the total number of threads involved in the operation
 * (via {@link #getThreadCount}) and the count of threads that have already finished
 * (via {@link #getCompletedThreads}).
 * <p>
 * For operations involving data transfer, such as uploads or downloads, this object may
 * also include a count of the total bytes being transferred (via {@link #getBytesTotal}) and a count
 * of how many bytes have already been transferred (via {@link #getBytesTransferred}). The
 * availability of this information is indicated by the result of {@link #isBytesTransferredInfoAvailable()}.
 * <p>
 * Further data tranfer information may be also available, such as the current transfer rate (via
 * {@link #getBytesPerSecond()}) and an estimate of the time remaining until the transfer is
 * completed (via {@link #getTimeRemaining()}). The availability of this information is indicated
 * by the result of {@link #isTimeRemainingAvailable()}.
 * <p>
 * It is possible to cancel some S3 operations. If an operation may be cancelled, this object will
 * include a {@link CancelEventTrigger} (available from {@link #getCancelEventListener()}) which can
 * be used to trigger a cancellation. Whether the operation can be cancelled is indicated by
 * {@link #isCancelTaskSupported()}.
 *
 * @author James Murty
 *
 * @deprecated 0.8.0 use {@link org.jets3t.service.multi.ThreadWatcher} instead.
 */
@Deprecated
public class ThreadWatcher extends org.jets3t.service.multi.ThreadWatcher {

    protected ThreadWatcher(BytesProgressWatcher[] progressWatchers) {
        super(progressWatchers);
    }

    protected ThreadWatcher(long threadCount) {
        super(threadCount);
    }

    public void updateThreadsCompletedCount(long completedThreads,
        org.jets3t.service.multithread.CancelEventTrigger cancelEventListener) {
        // TODO Auto-generated method stub
        super.updateThreadsCompletedCount(completedThreads, cancelEventListener);
    }

    @Override
    public org.jets3t.service.multithread.CancelEventTrigger getCancelEventListener() {
        // TODO Auto-generated method stub
        return (org.jets3t.service.multithread.CancelEventTrigger) super.getCancelEventListener();
    }

}
