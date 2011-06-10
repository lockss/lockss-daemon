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
package org.jets3t.service.io;

import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class that tracks the number of bytes transferred from a source, and uses this
 * information to calculate transfer rates and estimate end times. The watcher stores the
 * number of bytes that will be transferred, the number of bytes that have been transferred
 * in the current session and the time this has taken, and the number of bytes and time taken
 * overal (eg for transfers that have been restarted).
 *
 * @author James Murty
 */
public class BytesProgressWatcher {
    /**
     * The number of seconds worth of historical byte transfer information that will be
     * stored and used to calculate the recent transfer rate.
     */
    public static final int SECONDS_OF_HISTORY = 5;

    private boolean isStarted = false;
    private long bytesToTransfer = 0;

    private long startTimeAllTransfersMS = -1;
    private long totalBytesInAllTransfers = 0;

    private long startTimeCurrentTransferMS = -1;
    private long totalBytesInCurrentTransfer = 0;
    private long endTimeCurrentTransferMS = -1;

    private Map<Long, Long> historyOfBytesBySecond = new TreeMap<Long, Long>();
    private long earliestHistorySecond = Long.MAX_VALUE;

    /**
     * Construct a watcher for a transfer that will involve a given number of bytes.
     *
     * @param bytesToTransfer
     * the number of bytes that will be transferred, eg the size of a file being uploaded.
     */
    public BytesProgressWatcher(long bytesToTransfer) {
        this.bytesToTransfer = bytesToTransfer;
    }

    /**
     * @return
     * the count of bytes that will be transferred by the object watched by this class.
     */
    public synchronized long getBytesToTransfer() {
        return bytesToTransfer;
    }

    /**
     * Resets the byte count and timer variables for a watcher. This method is called
     * automatically when a transfer is started (ie the first bytes are registered in
     * the method {@link #updateBytesTransferred(long)}), or
     * when a transfer is restarted (eg due to transmission errors).
     *
     */
    public synchronized void resetWatcher() {
        startTimeCurrentTransferMS = System.currentTimeMillis();
        if (startTimeAllTransfersMS == -1) {
            startTimeAllTransfersMS = startTimeCurrentTransferMS;
        }
        endTimeCurrentTransferMS = -1;
        totalBytesInCurrentTransfer = 0;
        isStarted = true;
    }

    /**
     * Notifies this watcher that bytes have been transferred.
     *
     * @param byteCount
     * the number of bytes that have been transferred.
     */
    public synchronized void updateBytesTransferred(long byteCount) {
        // Start the monitor when we are notified of the first bytes transferred.
        if (!isStarted) {
            resetWatcher();
        }

        // Store the total byte count for the current transfer, and for all transfers.
        totalBytesInCurrentTransfer += byteCount;
        totalBytesInAllTransfers += byteCount;

        // Recognise when all the expected bytes have been transferred and mark the end time.
        if (totalBytesInCurrentTransfer >= bytesToTransfer) {
            endTimeCurrentTransferMS = System.currentTimeMillis();
        }

        // Keep historical records of the byte counts transferred in a given second.
        Long currentSecond = new Long(System.currentTimeMillis() / 1000);
        Long bytesInSecond = historyOfBytesBySecond.get(currentSecond);
        if (bytesInSecond != null) {
            historyOfBytesBySecond.put(currentSecond,
                new Long(byteCount + bytesInSecond.longValue()));
        } else {
            historyOfBytesBySecond.put(currentSecond, new Long(byteCount));
        }

        // Remember the earliest second value for which we have historical info.
        if (currentSecond.longValue() < earliestHistorySecond) {
            earliestHistorySecond = currentSecond.longValue();
        }

        // Remove any history records we are no longer interested in.
        long removeHistoryBeforeSecond = currentSecond.longValue() - SECONDS_OF_HISTORY;
        for (long sec = earliestHistorySecond; sec < removeHistoryBeforeSecond; sec++) {
            historyOfBytesBySecond.remove(new Long(sec));
        }
        earliestHistorySecond = removeHistoryBeforeSecond;
    }

    /**
     * @return
     * the number of bytes that have so far been transferred in the most recent transfer session.
     */
    public synchronized long getBytesTransferred() {
        return totalBytesInCurrentTransfer;
    }

    /**
     * @return
     * the number of bytes that are remaining to be transferred.
     */
    public synchronized long getBytesRemaining() {
        return bytesToTransfer - totalBytesInCurrentTransfer;
    }

    /**
     * @return
     * an estimate of the time (in seconds) it will take for the transfer to completed, based
     * on the number of bytes remaining to transfer and the overall bytes/second rate.
     */
    public synchronized long getRemainingTime() {
        BytesProgressWatcher[] progressWatchers = new BytesProgressWatcher[1];
        progressWatchers[0] = this;

        long bytesRemaining = bytesToTransfer - totalBytesInCurrentTransfer;
        double remainingSecs =
            bytesRemaining / calculateOverallBytesPerSecond(progressWatchers);
        return Math.round(remainingSecs);
    }

    /**
     * @return
     * the byte rate (per second) based on the historical information for the last
     * {@link #SECONDS_OF_HISTORY} seconds before the current time.
     */
    public synchronized double getRecentByteRatePerSecond() {
        if (!isStarted) {
            return 0;
        }

        long currentSecond = System.currentTimeMillis() / 1000;
        long startSecond = 1 + (currentSecond - SECONDS_OF_HISTORY);
        long endSecond = (endTimeCurrentTransferMS != -1
            ? endTimeCurrentTransferMS / 1000
            : currentSecond);

        if (currentSecond - SECONDS_OF_HISTORY > endSecond) {
            // This item finished too long ago, ignore it now.
            historyOfBytesBySecond.clear();
            return 0;
        }

        // Count the number of bytes transferred from SECONDS_OF_HISTORY ago to the second before now.
        long sumOfBytes = 0;
        long numberOfSecondsInHistory = 0;
        for (long sec = startSecond; sec <= endSecond; sec++) {
            numberOfSecondsInHistory++;
            Long bytesInSecond = historyOfBytesBySecond.get(new Long(sec));
            if (bytesInSecond != null) {
                sumOfBytes += bytesInSecond.longValue();
            }
        }
        return (numberOfSecondsInHistory == 0
            ? 0
            : (double)sumOfBytes / numberOfSecondsInHistory);
    }

    ///////////////////////////////////////////////////////////////////////////
    // These methods are only used to make information available to the public
    // static utility methods in this class.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @return
     * the number of milliseconds time elapsed for a transfer. The value returned is the time
     * elapsed so far if the transfer is ongoing, the total time taken for the transfer if it
     * is complete, or 0 if the transfer has not yet started.
     */
    protected synchronized long getElapsedTimeMS() {
        if (!isStarted) {
            return 0;
        }
        if (endTimeCurrentTransferMS != -1) {
            // Transfer is complete, report the time it took.
            return endTimeCurrentTransferMS - startTimeCurrentTransferMS;
        } else {
            return System.currentTimeMillis() - startTimeCurrentTransferMS;
        }
    }

    /**
     * @return
     * the number of bytes that have been transferred over all sessions, including any sessions
     * that have been restarted.
     */
    protected synchronized long getTotalBytesInAllTransfers() {
        return totalBytesInAllTransfers;
    }

    protected synchronized boolean isStarted() {
        return isStarted;
    }

    /**
     * @return
     * the time (in milliseconds) when the first bytes were transferred, regardless of how many
     * times the transfer was reset.
     */
    protected synchronized long getHistoricStartTimeMS() {
        return startTimeAllTransfersMS;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods below this point are public static utility methods that perform
    // calculations over a set of BytesProgressWatcher objects.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @param progressWatchers
     * all the watchers involved in the same byte transfer operation.
     * @return
     * the total number of bytes to transfer.
     */
    public static long sumBytesToTransfer(BytesProgressWatcher[] progressWatchers) {
        long sumOfBytes = 0;
        for (int i = 0; i < progressWatchers.length; i++) {
            sumOfBytes += progressWatchers[i].getBytesToTransfer();
        }
        return sumOfBytes;
    }

    /**
     * @param progressWatchers
     * all the watchers involved in the same byte transfer operation.
     * @return
     * the total number of bytes already transferred.
     */
    public static long sumBytesTransferred(BytesProgressWatcher[] progressWatchers) {
        long sumOfBytes = 0;
        for (int i = 0; i < progressWatchers.length; i++) {
            sumOfBytes += progressWatchers[i].getBytesTransferred();
        }
        return sumOfBytes;
    }

    /**
     * @param progressWatchers
     * all the watchers involved in the same byte transfer operation.
     * @return
     * an estimate of the time (in seconds) it will take for the transfer to completed, based
     * on the number of bytes remaining to transfer and the overall bytes/second rate.
     */
    public static long calculateRemainingTime(BytesProgressWatcher[] progressWatchers) {
        long bytesRemaining =
            sumBytesToTransfer(progressWatchers)
            - sumBytesTransferred(progressWatchers);
        double bytesPerSecond = calculateOverallBytesPerSecond(progressWatchers);

        if (Math.abs(bytesPerSecond) < 0.001d) {
            // No transfer has occurred yet.
            return 0;
        }

        double remainingSecs =
            bytesRemaining / bytesPerSecond;
        return Math.round(remainingSecs);
    }

    /**
     * @param progressWatchers
     * all the watchers involved in the same byte transfer operation.
     * @return
     * the overall rate of bytes/second over all transfers for all watchers.
     */
    public static double calculateOverallBytesPerSecond(BytesProgressWatcher[] progressWatchers) {
        long initialStartTime = Long.MAX_VALUE; // The oldest start time of any monitor.

        long bytesTotal = 0;
        for (int i = 0; i < progressWatchers.length; i++) {
            // Ignore any watchers that have not yet started.
            if (!progressWatchers[i].isStarted()) {
                continue;
            }

            // Add up all the bytes transferred by all started watchers.
            bytesTotal += progressWatchers[i].getTotalBytesInAllTransfers();

            // Find the earliest starting time of any monitor.
            if (progressWatchers[i].getHistoricStartTimeMS() < initialStartTime) {
                initialStartTime = progressWatchers[i].getHistoricStartTimeMS();
            }
        }

        // Determine how much time has elapsed since the earliest watcher start time.
        long elapsedTimeSecs = (System.currentTimeMillis() - initialStartTime) / 1000;

        // Calculate the overall rate of bytes/second over all transfers for all watchers.
        double bytesPerSecondOverall = (elapsedTimeSecs == 0
            ? bytesTotal
            : (double)bytesTotal / elapsedTimeSecs);

        return bytesPerSecondOverall;
    }

    /**
     * @param progressWatchers
     * all the watchers involved in the same byte transfer operation.
     * @return
     * the rate of bytes/second that has been achieved recently (ie within the last
     * {@link #SECONDS_OF_HISTORY} seconds).
     */
    public static long calculateRecentByteRatePerSecond(BytesProgressWatcher[] progressWatchers) {
        double sumOfRates = 0;
        for (int i = 0; i < progressWatchers.length; i++) {
            if (progressWatchers[i].isStarted()) {
                sumOfRates += progressWatchers[i].getRecentByteRatePerSecond();
            }
        }
        return Math.round(sumOfRates);
    }

}
