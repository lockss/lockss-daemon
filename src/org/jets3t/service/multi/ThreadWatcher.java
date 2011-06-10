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
package org.jets3t.service.multi;

import org.jets3t.service.io.BytesProgressWatcher;

/**
 * A thread watcher is associated with a multi-threaded operation and contains information about
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
 * It is possible to cancel some operations. If an operation may be cancelled, this object will
 * include a {@link CancelEventTrigger} (available from {@link #getCancelEventListener()}) which can
 * be used to trigger a cancellation. Whether the operation can be cancelled is indicated by
 * {@link #isCancelTaskSupported()}.
 *
 * @author James Murty
 */
public class ThreadWatcher {
    private long completedThreads = 0;
    private long threadCount = 0;
    private CancelEventTrigger cancelEventListener = null;
    private BytesProgressWatcher[] progressWatchers = null;

    public ThreadWatcher(BytesProgressWatcher[] progressWatchers) {
        this.progressWatchers = progressWatchers;
        this.threadCount = this.progressWatchers.length;
    }

    public ThreadWatcher(long threadCount) {
        this.threadCount = threadCount;
    }

    /**
     * Sets information about the number of threads completed and the total number of threads.
     *
     * @param completedThreads
     * the number of threads that have completed.
     */
    public void updateThreadsCompletedCount(long completedThreads) {
        updateThreadsCompletedCount(completedThreads, null);
    }

    /**
     * Sets information about the number of threads completed and the total number of threads,
     * as well as setting the cancellation listener that will be notified if the event is cancelled.
     *
     * @param completedThreads
     * the number of threads that have completed.
     * @param cancelEventListener
     * the listener to notify of cancellation events.
     */
    public void updateThreadsCompletedCount(long completedThreads,
        CancelEventTrigger cancelEventListener)
    {
        this.completedThreads = completedThreads;
        this.cancelEventListener = cancelEventListener;
    }

    /**
     * @return
     * the number of threads that have completed.
     */
    public long getCompletedThreads() {
        return completedThreads;
    }

    /**
     * @return
     * the total number of threads involved in an operation.
     */
    public long getThreadCount() {
        return threadCount;
    }

    /**
     * If this method returns true, the methods {@link #getBytesTotal()} and {@link #getBytesTransferred()}
     * will contain information about the amount of data being transferred by the watched threads.
     *
     * @return
     * true if this watcher contains information about the bytes transferred by
     * the threads it is watching.
     */
    public boolean isBytesTransferredInfoAvailable() {
        return (progressWatchers != null);
    }

    /**
     * @return
     * the expected total of bytes that will be transferred by the watched threads.
     * @throws IllegalStateException
     * if the bytes transferred information is not available - check this availability
     * with the {@link #isBytesTransferredInfoAvailable()} method.
     */
    public long getBytesTotal() throws IllegalStateException {
        if (!isBytesTransferredInfoAvailable()) {
            throw new IllegalStateException("Bytes Transferred Info is not available in this object");
        }
        return BytesProgressWatcher.sumBytesToTransfer(progressWatchers);
    }

    /**
     * @return
     * the count of bytes that have been transferred by the watched threads.
     * @throws IllegalStateException
     * if the bytes transferred information is not available - check this availability
     * with the {@link #isBytesTransferredInfoAvailable()} method.
     */
    public long getBytesTransferred() {
        if (!isBytesTransferredInfoAvailable()) {
            throw new IllegalStateException("Bytes Transferred Info is not available in this object");
        }
        return BytesProgressWatcher.sumBytesTransferred(progressWatchers);
    }

    /**
     * @return
     * an estimate of the recent rate of bytes/second transfer speed.
     */
    public long getBytesPerSecond() {
        return BytesProgressWatcher.calculateRecentByteRatePerSecond(progressWatchers);
    }

    /**
     * If this method returns true, the method {@link #getTimeRemaining()} will contain
     * an estimate of the completion time for the data transfer.
     *
     * @return
     * true if this watcher contains an estimate of the completion time for the data transfer.
     */
    public boolean isTimeRemainingAvailable() {
        return (progressWatchers != null);
    }

    /**
     * @return
     * an estimate of the how many <b>seconds</b> until the data transfer completes, based on the
     * overall byte rate of the transmission.
     * @throws IllegalStateException
     * if the time remaining estimave is not available - check this availability
     * with the {@link #isTimeRemainingAvailable()} method.
     */
    public long getTimeRemaining() {
        if (!isTimeRemainingAvailable()) {
            throw new IllegalStateException("Time remaining estimate is not available in this object");
        }
        return BytesProgressWatcher.calculateRemainingTime(progressWatchers);
    }

    /**
     * @return
     * true if the operation this object is associated with can be cancelled, and a
     * {@link CancelEventTrigger} is available.
     */
    public boolean isCancelTaskSupported() {
        return cancelEventListener != null;
    }

    /**
     * Convenience method to trigger an event cancellation via {@link CancelEventTrigger#cancelTask}
     * if this thread watcher is associated with an operation that can be cancelled.
     */
    public void cancelTask() {
        if (isCancelTaskSupported()) {
            cancelEventListener.cancelTask(this);
        }
    }

    /**
     * @return
     * the cancel event trigger associated with an operation, if any.
     */
    public CancelEventTrigger getCancelEventListener() {
        return cancelEventListener;
    }

}
