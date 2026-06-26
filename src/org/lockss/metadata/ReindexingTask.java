/*

 Copyright (c) 2013-2016 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.metadata;

import java.io.*;
import java.lang.management.*;
import java.sql.*;
import java.util.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.TdbAu;
import org.lockss.daemon.*;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.extractor.*;
import org.lockss.extractor.ArticleMetadataExtractor.Emitter;
import org.lockss.extractor.MetadataException.ValidationException;
import org.lockss.metadata.ArticleMetadataBuffer.ArticleMetadataInfo;
import org.lockss.metadata.MetadataManager.ReindexingStatus;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * A reindexing task that extracts metadata from all the articles in the
 * specified AU and records it in the metadata database.
 * <p>
 * The task is <b>not</b> scheduled through the {@link
 * org.lockss.scheduler.TaskRunner}; it is run directly on a dedicated thread by
 * {@link MetadataManager#runReindexingTask}, which simply calls {@link
 * #reindex()}. The lifecycle has three phases:
 * <ol>
 * <li>{@link #setUp} - build the article iterator and metadata buffer.</li>
 * <li>{@link #extract} - extract metadata from each article into the buffer
 *     ("Indexing" phase).</li>
 * <li>{@link #updateDb} - write the accumulated metadata to the database
 *     ("Updating" phase).</li>
 * </ol>
 * <p>
 * The task has a single, explicit terminal state, exposed via {@link
 * #getReindexingStatus}:
 * <ul>
 * <li>{@link ReindexingStatus#Success} - metadata extracted and committed.</li>
 * <li>{@link ReindexingStatus#Failed} - an error prevented completion; the AU
 *     is re-queued at a low priority so it is not retried until the underlying
 *     problem is fixed.</li>
 * <li>{@link ReindexingStatus#Rescheduled} - a transient condition prevented
 *     completion; the AU is re-queued normally to be retried later.</li>
 * <li>{@link ReindexingStatus#Cancelled} - the task was aborted by request
 *     (abort priority, disable indexing, AU deletion, or shutdown). Nothing is
 *     committed and the pending queue is left untouched, so the caller that
 *     requested cancellation retains full control over the AU's fate.</li>
 * </ul>
 * <p>
 * Cancellation is authoritative: {@link #cancelRequested} is the master switch
 * checked by every commit / reschedule / fail-to-pending decision, and by
 * {@link AuMetadataRecorder} as it writes, so a cancellation that arrives even
 * in the middle of the database write aborts the write without committing.
 */
public class ReindexingTask {
  private static Logger log = Logger.getLogger(ReindexingTask.class);

  // The archival unit for this task.
  private final ArchivalUnit au;

  // The article metadata extractor for this task.
  private final ArticleMetadataExtractor ae;

  // The article iterator for this task.
  private Iterator<ArticleFiles> articleIterator = null;

  // The hashes of the text strings of the log messages already emitted for this
  // task's AU. Used to prevent duplicate messages from being logged.
  private final HashSet<Integer> auLogTable = new HashSet<Integer>();

  // An indication of whether the AU being indexed is new to the index.
  private volatile boolean isNewAu = true;

  // An indication of whether the AU being indexed needs a full reindex
  private volatile boolean needFullReindex = false;

  // The time from which to extract new metadata.
  private long lastExtractTime = 0;

  // The status of the task.
  private volatile ReindexingStatus status = ReindexingStatus.Running;

  // True once cancellation has been requested. This is the authoritative switch:
  // when set, no metadata is committed, the AU is not re-queued, and the task
  // ends in the Cancelled state. Checked by the worker, by the database commit
  // linearization point, and by AuMetadataRecorder while it writes.
  private volatile boolean cancelRequested = false;

  // True once the metadata has been committed to the database. Guarded by this.
  // Once committed, a late cancellation is a no-op (the data is already
  // persisted) and the task is reported as Success.
  private boolean committed = false;

  // The exception, if any, that caused the task to fail.
  private volatile Exception exception = null;

  // The number of articles indexed by this task.
  private volatile long indexedArticleCount = 0;

  // The number of articles that got errors
  private volatile long errorArticleCount = 0;

  // The number of articles updated by this task.
  private volatile long updatedArticleCount = 0;

  // ThreadMXBean times.
  private volatile long startCpuTime = 0;
  private volatile long startUserTime = 0;
  private volatile long startClockTime = 0;

  private volatile long startUpdateClockTime = 0;

  private volatile long endCpuTime = 0;
  private volatile long endUserTime = 0;
  private volatile long endClockTime = 0;

  // Archival unit properties.
  private final String auName;
  private final String auId;
  private final boolean auNoSubstance;

  private ArticleMetadataBuffer articleMetadataInfoBuffer = null;

  // The database manager.
  private final DbManager dbManager;

  // The metadata manager.
  private final MetadataManager mdManager;

  // The metadata manager SQL executor.
  private final MetadataManagerSql mdManagerSql;

  private final Emitter emitter;
  private int extractedCount = 0;

  private LockssWatchdog watchDog;

  private static ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();

  static {
    log.debug3("current thread CPU time supported? "
        + tmxb.isCurrentThreadCpuTimeSupported());

    if (tmxb.isCurrentThreadCpuTimeSupported()) {
      tmxb.setThreadCpuTimeEnabled(true);
    }
  }

  /**
   * Thrown internally to unwind out of the metadata write when the task has
   * been cancelled, so that the database transaction is rolled back rather than
   * committed. It is unchecked so {@link AuMetadataRecorder} can throw it from
   * deep within the record loop without changing method signatures.
   */
  public static class CancelException extends RuntimeException {
    CancelException() {
      super("Reindexing task cancelled");
    }
  }

  /**
   * Constructor.
   *
   * @param theAu
   *          An ArchivalUnit with the AU for the task.
   * @param theAe
   *          An ArticleMetadataExtractor with the article metadata extractor to
   *          be used.
   */
  public ReindexingTask(ArchivalUnit theAu, ArticleMetadataExtractor theAe) {
    this.au = theAu;
    this.ae = theAe;
    this.auName = au.getName();
    this.auId = au.getAuId();
    this.auNoSubstance = AuUtil.getAuState(au).hasNoSubstance();
    dbManager = LockssDaemon.getLockssDaemon().getDbManager();
    mdManager = LockssDaemon.getLockssDaemon().getMetadataManager();
    mdManagerSql = mdManager.getMetadataManagerSql();

    // The accumulator of article metadata.
    emitter = new ReindexingEmitter();
  }

  public void setWDog(LockssWatchdog watchDog) {
    this.watchDog = watchDog;
  }

  void pokeWDog() {
    watchDog.pokeWDog();
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Lifecycle
  // ///////////////////////////////////////////////////////////////////////////

  /**
   * Runs the complete reindexing lifecycle on the calling thread. Both {@link
   * #updateDb} and {@link #cleanup} are invoked exactly once, on every path
   * (normal completion, setup/extraction/commit error, cancellation, or an
   * unexpected throwable). Running updateDb() unconditionally is what guarantees
   * that even an unchecked throwable from {@link #setUp} or {@link #extract}
   * (e.g. the article iterator throwing) still records the "Updating" timestamp
   * and de-prioritizes the failed AU, rather than leaving it looking like it is
   * still indexing and immediately respawning it. The watchdog must have been
   * set with {@link #setWDog} before calling.
   */
  void reindex() {
    try {
      try {
        setUp();

        // Only extract if setUp succeeded and we have not been cancelled.
        if (status == ReindexingStatus.Running && !cancelRequested) {
          extract();
        }
      } catch (Throwable t) {
        // An unchecked throwable escaped setUp/extract (e.g. building or
        // advancing the article iterator). Record it as a failure and fall
        // through to updateDb()/cleanup() below.
        log.error("Unexpected error indexing AU '" + auName + "'", t);
        setStatusIfRunning(ReindexingStatus.Failed,
            (t instanceof Exception) ? (Exception) t : new RuntimeException(t));
      }

      // Always run the finish phase so the terminal status, timings, and
      // pending-queue bookkeeping are consistent for every path.
      updateDb();
    } catch (Throwable t) {
      // updateDb() handles its own errors; this is a last-resort backstop so a
      // stuck task can never prevent cleanup from running.
      log.error("Unexpected error finishing reindexing task for AU '" + auName
          + "'", t);
    } finally {
      cleanup();
    }
  }

  /**
   * Sets up the article iterator and metadata buffer ("Indexing" phase begins).
   */
  private void setUp() {
    final String DEBUG_HEADER = "setUp(): ";

    // Record the start times.
    startCpuTime = threadCpuTime();
    startUserTime = threadUserTime();
    startClockTime = TimeBase.nowMs();

    log.info("Starting reindexing task for AU '" + auName + "': isNewAu = "
        + isNewAu + ", needFullReindex = " + needFullReindex + "...");

    // Notify the start here, before any early return, so it is always paired
    // with the notifyFinishReindexingAu that cleanup() unconditionally fires.
    mdManager.notifyStartReindexingAu(au);

    // Don't build the (potentially expensive) iterator if we are already
    // cancelled; updateDb()/cleanup() will report Cancelled.
    if (cancelRequested) {
      return;
    }

    MetadataTarget target = MetadataTarget.OpenURL();

    // Only allow incremental extraction if the AU is not new and not doing a
    // full reindex.
    if (!isNewAu && !needFullReindex) {
      if (log.isDebug2())
        log.debug2(DEBUG_HEADER + "lastExtractTime = " + lastExtractTime);
      // Indicate that only new metadata after the last extraction is to be
      // included.
      target.setIncludeFilesChangedAfter(lastExtractTime);
    }

    // The article iterator won't be null because only AUs with article
    // iterators are queued for processing.
    articleIterator = au.getArticleIterator(target);

    if (log.isDebug3()) {
      long articleIteratorInitTime = TimeBase.nowMs() - startClockTime;
      log.debug3(DEBUG_HEADER + "Reindexing task for AU '" + auName
          + "': has articles? " + articleIterator.hasNext()
          + ", initializing iterator took " + articleIteratorInitTime + "ms");
    }

    try {
      articleMetadataInfoBuffer =
          new ArticleMetadataBuffer(new File(PlatformUtil.getSystemTempDir()));
    } catch (IOException ioe) {
      log.error("Failed to set up pending AU '" + auName
          + "' for re-indexing", ioe);
      setStatusIfRunning(ReindexingStatus.Rescheduled, ioe);
    }
  }

  /**
   * Extracts the metadata from all the AU's articles into the buffer.
   */
  private void extract() {
    final String DEBUG_HEADER = "extract(): ";

    while (articleIterator.hasNext()) {
      // Stop promptly if the task is no longer Running, i.e. another thread has
      // cancelled (status -> Cancelled) or rescheduled (status -> Rescheduled)
      // it. Keying off status rather than just cancelRequested ensures a
      // reschedule aborts the extraction immediately instead of letting it run
      // to completion only to be discarded.
      if (status != ReindexingStatus.Running) {
        log.debug2(DEBUG_HEADER + "Stopping extraction of AU '" + auName
            + "': status = " + status);
        return;
      }

      ArticleFiles af = articleIterator.next();
      try {
        ae.extract(MetadataTarget.OpenURL(), af, emitter);
      } catch (org.lockss.repository.LockssRepository.RepositoryStateException ex) {
        log.error("Error extracting metadata for full text URL, continuing: "
            + af.getFullTextUrl(), ex);
        errorArticleCount++;
      } catch (IOException ex) {
        log.error("Failed to index metadata for full text URL: "
            + af.getFullTextUrl(), ex);
        indexedArticleCount = 0;
        setStatusIfRunning(ReindexingStatus.Rescheduled, ex);
        return;
      } catch (PluginException ex) {
        log.error("Failed to index metadata for full text URL: "
            + af.getFullTextUrl(), ex);
        indexedArticleCount = 0;
        setStatusIfRunning(ReindexingStatus.Failed, ex);
        return;
      } catch (RuntimeException ex) {
        log.error("Caught unexpected RuntimeException for full text URL: "
            + af.getFullTextUrl(), ex);
        indexedArticleCount = 0;
        setStatusIfRunning(ReindexingStatus.Failed, ex);
        return;
      } catch (Throwable ex) {
        log.error("Caught unexpected Throwable for full text URL: "
            + af.getFullTextUrl(), ex);
        setStatusIfRunning(ReindexingStatus.Failed,
            (ex instanceof Exception) ? (Exception) ex : new RuntimeException(ex));
        return;
      }

      pokeWDog();
    }
  }

  /**
   * Writes the accumulated metadata to the database ("Updating" phase), or, for
   * an unsuccessful or cancelled task, performs the appropriate pending-queue
   * bookkeeping. Never commits if cancellation has been requested.
   */
  private void updateDb() {
    // "Updating" phase begins. Recorded on every path so the UI shows a sane
    // index/update split even for unsuccessful tasks.
    startUpdateClockTime = TimeBase.nowMs();

    if (cancelRequested) {
      // Cancelled during setUp/extract: do not commit and do not touch the
      // pending queue; the canceller owns the AU's fate.
      log.debug2("Reindexing task for AU '" + auName
          + "' was cancelled; not committing.");
      return;
    }

    // Extraction completed normally: attempt to commit the metadata.
    if (status == ReindexingStatus.Running) {
      commitMetadata();
    }

    // commitMetadata (or extraction) may have ended in a non-committed,
    // non-cancelled terminal state; update the pending queue accordingly.
    if (status == ReindexingStatus.Failed
        || status == ReindexingStatus.Rescheduled) {
      updatePendingQueueForUnsuccessful();
    }
  }

  /**
   * Commits the accumulated metadata to the database. On success sets the status
   * to {@link ReindexingStatus#Success}; on error sets it to {@link
   * ReindexingStatus#Failed} or {@link ReindexingStatus#Rescheduled} (unless
   * cancellation intervened). If cancellation is observed at the commit
   * linearization point, the transaction is rolled back and the status is left
   * as {@link ReindexingStatus#Cancelled}.
   */
  private void commitMetadata() {
    final String DEBUG_HEADER = "commitMetadata(): ";
    Connection conn = null;
    long removedArticleCount = 0L;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();
      dbManager.lockMetadataWrite(conn);

      if (log.isDebug3())
        log.debug3(DEBUG_HEADER + "needFullReindex = " + needFullReindex);

      // For a full reindex, remove the old Archival Unit metadata before adding
      // the new metadata.
      if (needFullReindex) {
        removedArticleCount = mdManagerSql.removeAuMetadataItems(conn, auId);
        log.info("Reindexing task for AU '" + auName + "' removed "
            + removedArticleCount + " database items.");
      }

      Iterator<ArticleMetadataInfo> mditr =
          articleMetadataInfoBuffer.iterator();

      if (needFullReindex && !mditr.hasNext()) {
        log.warning("Non-incremental reindexing task for AU '" + auName
            + "' failed to extract any items.");
      }

      // Write the AU metadata (or just record the extraction). AuMetadataRecorder
      // checks isCancelled() as it writes and throws CancelException to abort.
      if (mditr.hasNext()) {
        new AuMetadataRecorder(this, mdManager, au).recordMetadata(conn, mditr);
        pokeWDog();
      } else {
        new AuMetadataRecorder(this, mdManager, au)
            .recordMetadataExtraction(conn);
      }

      // Remove the AU just re-indexed from the list of AUs pending to be
      // re-indexed.
      mdManagerSql.removeFromPendingAus(conn, auId);
      mdManager.updatePendingAusCount(conn);

      // Commit linearization point: either cancellation has been requested (so
      // we roll back and remain Cancelled) or we commit and become Success.
      // Holding the lock across the commit makes a concurrent cancel() either
      // win (it set cancelRequested before we got here) or become a no-op (it
      // sees committed == true).
      synchronized (this) {
        if (cancelRequested) {
          throw new CancelException();
        }
        DbManager.commitOrRollback(conn, log);
        committed = true;
        status = ReindexingStatus.Success;
      }

      // Update the successful re-indexing count.
      mdManager.addToSuccessfulReindexingTasks(this);

      // Update the total article count.
      mdManager.addToMetadataArticleCount(updatedArticleCount
          - removedArticleCount);

      if (needFullReindex) {
        log.info("Reindexing task for AU '" + auName + "' added "
            + updatedArticleCount + " database articles.");
      } else {
        log.info("Reindexing task for AU '" + auName + "' updated "
            + updatedArticleCount + " database articles.");
      }
    } catch (CancelException ce) {
      // Cancelled in the middle of the metadata write: nothing is committed and
      // the pending queue is left untouched. status is already Cancelled.
      log.info("Reindexing task for AU '" + auName
          + "' cancelled during metadata update; rolled back.");
    } catch (MetadataException me) {
      log.warning("Error updating metadata at FINISH for " + status
          + " -- NOT rescheduling", me);
      log.warning("ArticleMetadataInfo = " + me.getArticleMetadataInfo());
      setStatusIfRunning(ReindexingStatus.Failed, me);
    } catch (DbException dbe) {
      log.warning("Error updating metadata at FINISH for " + status
          + " -- rescheduling", dbe);
      setStatusIfRunning(ReindexingStatus.Rescheduled, dbe);
    } catch (RuntimeException re) {
      log.warning("Error updating metadata at FINISH for " + status
          + " -- NOT rescheduling", re);
      setStatusIfRunning(ReindexingStatus.Failed, re);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Updates the pending-AUs queue for a task that did not complete and was not
   * cancelled: failed tasks are re-queued at a low priority (so they are not
   * retried until the underlying problem is fixed); rescheduled tasks are
   * re-queued normally to be retried later.
   */
  private void updatePendingQueueForUnsuccessful() {
    final String DEBUG_HEADER = "updatePendingQueueForUnsuccessful(): ";
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Reindexing task for AU '"
        + auName + "' was unsuccessful: status = " + status);

    mdManager.addToFailedReindexingTasks(this);

    Connection conn = null;
    try {
      conn = dbManager.getConnection();

      mdManagerSql.removeFromPendingAus(conn, auId);
      mdManager.updatePendingAusCount(conn);

      if (status == ReindexingStatus.Failed) {
        if (log.isDebug3()) log.debug3(DEBUG_HEADER
            + "Marking as failed the reindexing task for AU '" + auName + "'");

        // Add the failed AU to the pending list with the right priority to
        // avoid processing it again before the underlying problem is fixed.
        mdManagerSql.addFailedIndexingAuToPendingAus(conn, auId);
      } else if (status == ReindexingStatus.Rescheduled) {
        if (log.isDebug3()) log.debug3(DEBUG_HEADER
            + "Rescheduling the reindexing task AU '" + auName + "'");

        // Add the re-schedulable AU to the end of the pending list.
        mdManager.addToPendingAusIfNotThere(conn,
            Collections.singleton(au), needFullReindex);
      }

      // Commit the pending-queue changes, unless cancellation has intervened
      // since updateDb() decided this task was unsuccessful. A cancel must leave
      // the pending queue untouched (the canceller owns the AU's fate), so we
      // roll back rather than re-queue. This mirrors the commit linearization
      // in commitMetadata and acquires only this task's monitor, never
      // activeReindexingTasks, so the lock order is preserved.
      synchronized (this) {
        if (cancelRequested) {
          log.debug2("Reindexing task for AU '" + auName
              + "' cancelled before re-queue committed; pending queue left"
              + " untouched.");
          return;
        }
        // Complete the database transaction.
        DbManager.commitOrRollback(conn, log);
      }
    } catch (DbException dbe) {
      log.warning("Error updating pending queue at FINISH for AU '" + auName
          + "', status = " + status, dbe);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Finalizes the task: records timings, releases resources, removes the task
   * from the active set, notifies listeners of the terminal status, and starts
   * the next pending task. Runs exactly once per task, on every path.
   */
  private void cleanup() {
    articleIterator = null;
    endClockTime = TimeBase.nowMs();
    endCpuTime = threadCpuTime();
    endUserTime = threadUserTime();

    // Display timings.
    long elapsedCpuTime = endCpuTime - startCpuTime;
    long elapsedUserTime = endUserTime - startUserTime;
    long elapsedClockTime = endClockTime - startClockTime;

    log.info("Finished reindexing task for AU '" + auName + "': status = "
        + status + ", CPU time: " + elapsedCpuTime / 1.0e9 + " ("
        + endCpuTime / 1.0e9 + "), User time: " + elapsedUserTime / 1.0e9
        + " (" + endUserTime / 1.0e9 + "), Clock time: "
        + elapsedClockTime / 1.0e3 + " (" + endClockTime / 1.0e3 + ")");

    // Release collected metadata info once finished. Null-safe: the buffer may
    // never have been created (e.g. setUp failed or the task was cancelled
    // before setUp built it).
    if (articleMetadataInfoBuffer != null) {
      articleMetadataInfoBuffer.close();
      articleMetadataInfoBuffer = null;
    }

    synchronized (mdManager.activeReindexingTasks) {
      // Only remove our own mapping. If the slot was freed early (e.g. by
      // disableAuIndexing) and a successor task for the same AU was started,
      // remove(auId) would evict that live successor and leave it untracked;
      // remove(auId, this) removes the entry only if it still maps to us.
      mdManager.activeReindexingTasks.remove(auId, this);
      mdManager.notifyFinishReindexingAu(au, status);

      Connection conn = null;
      try {
        // Get a connection to the database.
        conn = dbManager.getConnection();

        // Schedule another task if available.
        mdManager.startReindexing(conn);

        // Complete the database transaction.
        DbManager.commitOrRollback(conn, log);
      } catch (DbException dbe) {
        log.error("Cannot restart indexing", dbe);
      } finally {
        DbManager.safeRollbackAndClose(conn);
      }
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // State control
  // ///////////////////////////////////////////////////////////////////////////

  /**
   * Requests cancellation of this task. Unless the metadata has already been
   * committed, the task ends in the {@link ReindexingStatus#Cancelled} state,
   * does not commit any metadata, and leaves the pending queue untouched. May be
   * called from any thread at any point in the task's life.
   */
  public void cancel() {
    if (log.isDebug2()) {
      log.debug2("Task cancel requested: " + auName, new Throwable());
    } else {
      log.debug("Task cancel requested: " + auName);
    }

    synchronized (this) {
      cancelRequested = true;
      // If the metadata has already been committed, the work is durably done;
      // leave the (Success) status alone. Otherwise this is an abort.
      if (!committed) {
        status = ReindexingStatus.Cancelled;
      }
    }
  }

  /**
   * Requests that this task be cancelled and the AU re-queued to be reindexed
   * later. Has no effect if the task has already been cancelled or committed.
   */
  void reschedule() {
    if (log.isDebug2()) {
      log.debug2("Task reschedule requested: " + auName, new Throwable());
    } else {
      log.debug("Task reschedule requested: " + auName);
    }

    synchronized (this) {
      if (committed || cancelRequested) {
        return;
      }
      if (status == ReindexingStatus.Running) {
        status = ReindexingStatus.Rescheduled;
      }
    }
  }

  /**
   * Provides an indication of whether cancellation of this task has been
   * requested. Consulted by {@link AuMetadataRecorder} so a long database write
   * can be abandoned promptly without committing.
   *
   * @return <code>true</code> if cancellation has been requested.
   */
  public boolean isCancelled() {
    return cancelRequested;
  }

  /**
   * Transitions to the given terminal status only if the task is still running
   * and has not been cancelled. Cancellation always wins, so this never
   * overwrites a Cancelled status with Failed/Rescheduled (which would, e.g.,
   * wrongly re-queue a cancelled or already-deleted AU).
   *
   * @param newStatus
   *          the terminal status to set.
   * @param ex
   *          the causing exception, or <code>null</code>.
   */
  private synchronized void setStatusIfRunning(ReindexingStatus newStatus,
      Exception ex) {
    if (status == ReindexingStatus.Running && !cancelRequested) {
      status = newStatus;
      if (ex != null) {
        exception = ex;
      }
    }
  }

  private static long threadCpuTime() {
    return tmxb.isCurrentThreadCpuTimeSupported()
        ? tmxb.getCurrentThreadCpuTime() : 0;
  }

  private static long threadUserTime() {
    return tmxb.isCurrentThreadCpuTimeSupported()
        ? tmxb.getCurrentThreadUserTime() : 0;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Accessors
  // ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the task AU.
   *
   * @return an ArchivalUnit with the AU of this task.
   */
  ArchivalUnit getAu() {
    return au;
  }

  /**
   * Returns the name of the task AU.
   *
   * @return a String with the name of the task AU.
   */
  String getAuName() {
    return auName;
  }

  /**
   * Returns the auid of the task AU.
   *
   * @return a String with the auid of the task AU.
   */
  String getAuId() {
    return auId;
  }

  /**
   * Returns the substance state of the task AU.
   *
   * @return <code>true</code> if AU has no substance, <code>false</code>
   *         otherwise.
   */
  boolean hasNoAuSubstance() {
    return auNoSubstance;
  }

  /**
   * Returns an indication of whether the AU has not yet been indexed.
   *
   * @return <code>true</code> if the AU has not yet been indexed,
   * <code>false</code> otherwise.
   */
  boolean isNewAu() {
    return isNewAu;
  }

  /**
   * Returns an indication of whether the AU needs a full reindex.
   *
   * @return <code>true</code> if the AU needs a full reindex,
   * <code>false</code> otherwise.
   */
  boolean needsFullReindex() {
    return needFullReindex;
  }

  /**
   * Provides the start time for indexing.
   *
   * @return a long with the start time in miliseconds since epoch (0 if not
   *         started).
   */
  long getStartTime() {
    return startClockTime;
  }

  /**
   * Provides the update start time.
   *
   * @return a long with the update start time in miliseconds since epoch (0 if
   *         not started).
   */
  long getStartUpdateTime() {
    return startUpdateClockTime;
  }

  /**
   * Provides the end time for indexing.
   *
   * @return a long with the end time in miliseconds since epoch (0 if not
   *         finished).
   */
  long getEndTime() {
    return endClockTime;
  }

  /**
   * Returns the reindexing status of this task.
   *
   * @return a ReindexingStatus with the reindexing status.
   */
  ReindexingStatus getReindexingStatus() {
    return status;
  }

  /**
   * Returns the exception, if any, that caused this task to fail.
   *
   * @return an Exception, or <code>null</code> if the task did not fail.
   */
  public Exception getException() {
    return exception;
  }

  /**
   * Returns the number of articles extracted by this task.
   *
   * @return a long with the number of articles extracted by this task.
   */
  long getIndexedArticleCount() {
    return indexedArticleCount;
  }

  /**
   * Returns the number of articles that weren't extracted due to an error
   *
   * @return a long with the number of errors
   */
  long getErrorArticleCount() {
    return errorArticleCount;
  }

  /**
   * Returns the number of articles updated by this task.
   *
   * @return a long with the number of articles updated by this task.
   */
  long getUpdatedArticleCount() {
    return updatedArticleCount;
  }

  /**
   * Increments by one the number of articles updated by this task.
   */
  void incrementUpdatedArticleCount() {
    this.updatedArticleCount++;
  }

  /**
   * Issues a warning for this re-indexing task.
   *
   * @param s
   *          A String with the warning message.
   */
  void taskWarning(String s) {
    int hashcode = s.hashCode();
    if (auLogTable.add(hashcode)) {
        log.warning(s);
    }
  }

  /**
   * Accumulator of article metadata.
   */
  private class ReindexingEmitter implements Emitter {
    private final Logger log = Logger.getLogger(ReindexingEmitter.class);

    @Override
    public void emitMetadata(ArticleFiles af, ArticleMetadata md) {
      final String DEBUG_HEADER = "emitMetadata(): ";

      if (log.isDebug3()) {
        log.debug3(DEBUG_HEADER+"\n"+md.ppString(2));
      }

      Map<String, String> roles = new HashMap<String, String>();

      for (String key : af.getRoleMap().keySet()) {
        String value = af.getRoleAsString(key);
        if (log.isDebug3()) {
          log.debug3(DEBUG_HEADER + "af.getRoleMap().key = " + key
              + ", af.getRoleUrl(key) = " + value);
        }
        roles.put(key, value);
      }

      if (log.isDebug3()) {
        log.debug3(DEBUG_HEADER + "field access url: "
            + md.get(MetadataField.FIELD_ACCESS_URL));
      }

      if (md.get(MetadataField.FIELD_ACCESS_URL) == null) {
        // temporary -- use full text url if not set
        // (should be set by metadata extractor)
        md.put(MetadataField.FIELD_ACCESS_URL, af.getFullTextUrl());
      }

      md.putRaw(MetadataField.FIELD_FEATURED_URL_MAP.getKey(), roles);

      // Get the earliest fetch time of the metadata items URLs.
      long fetchTime = AuUtil.getAuUrlsEarliestFetchTime(au, roles.values());
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fetchTime = " + fetchTime);

      md.put(MetadataField.FIELD_FETCH_TIME, String.valueOf(fetchTime));

      try {
        validateDataAgainstTdb(new ArticleMetadataInfo(md), au);
        articleMetadataInfoBuffer.add(md);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }

      extractedCount++;
      indexedArticleCount++;
    }

    /**
     * Validate data against TDB information.
     *
     * @param mdinfo
     *          the ArticleMetadataInfo
     * @param au
     *          An ArchivalUnit with the archival unit.
     * @throws ValidationException
     *           if field is invalid
     */
    private void validateDataAgainstTdb(ArticleMetadataInfo mdinfo,
        ArchivalUnit au) {
      HashSet<String> isbns = new HashSet<String>();
      if (mdinfo.isbn != null) {
        isbns.add(mdinfo.isbn);
      }

      if (mdinfo.eisbn != null) {
        isbns.add(mdinfo.eisbn);
      }

      HashSet<String> issns = new HashSet<String>();
      if (mdinfo.issn != null) {
        issns.add(mdinfo.issn);
      }

      if (mdinfo.eissn != null) {
        issns.add(mdinfo.eissn);
      }

      TdbAu tdbau = au.getTdbAu();
      boolean isTitleInTdb = !au.isBulkContent();
      String tdbauName = (tdbau == null) ? null : tdbau.getName();
      String tdbauStartYear = (tdbau == null) ? auName : tdbau.getStartYear();
      String tdbauYear = (tdbau == null) ? null : tdbau.getYear();
      String tdbauIsbn = null;
      String tdbauIssn = null;
      String tdbauEissn = null;
      String tdbauJournalTitle = null;

      // Check whether the TDB has title information.
      if (isTitleInTdb && (tdbau != null)) {
        // Yes: Get the title information from the TDB.
        tdbauIsbn = tdbau.getIsbn();
        tdbauIssn = tdbau.getPrintIssn();
        tdbauEissn = tdbau.getEissn();
        tdbauJournalTitle = tdbau.getPublicationTitle();
      }

      if (tdbau != null) {
        // Validate journal title against the TDB journal title.
        if (tdbauJournalTitle != null) {
          if (!tdbauJournalTitle.equals(mdinfo.publicationTitle)) {
            if (mdinfo.publicationTitle == null) {
              taskWarning("tdb title  is " + tdbauJournalTitle + " for "
                  + tdbauName + " -- metadata title is missing");
            } else {
              taskWarning("tdb title " + tdbauJournalTitle + " for "
                  + tdbauName + " -- does not match metadata journal title "
                  + mdinfo.publicationTitle);
            }
          }
        }

        // Validate ISBN against the TDB ISBN.
        if (tdbauIsbn != null) {
          if (!tdbauIsbn.equals(mdinfo.isbn)) {
            isbns.add(tdbauIsbn);
            if (mdinfo.isbn == null) {
              taskWarning("using tdb isbn " + tdbauIsbn + " for " + tdbauName
                  + " -- metadata isbn missing");
            } else {
              taskWarning("also using tdb isbn " + tdbauIsbn + " for "
                  + tdbauName + " -- different than metadata isbn: "
                  + mdinfo.isbn);
            }
          } else if (mdinfo.isbn != null) {
            taskWarning("tdb isbn missing for " + tdbauName + " -- should be: "
                + mdinfo.isbn);
          }
        } else if (mdinfo.isbn != null) {
          if (isTitleInTdb) {
            taskWarning("tdb isbn missing for " + tdbauName + " -- should be: "
                + mdinfo.isbn);
          }
        }

        // validate ISSN against the TDB ISSN.
        if (tdbauIssn != null) {
          if (tdbauIssn.equals(mdinfo.eissn) && (mdinfo.issn == null)) {
            taskWarning("tdb print issn " + tdbauIssn + " for " + tdbauName
                + " -- reported by metadata as eissn");
          } else if (!tdbauIssn.equals(mdinfo.issn)) {
            // add both ISSNs so it can be found either way
            issns.add(tdbauIssn);
            if (mdinfo.issn == null) {
              taskWarning("using tdb print issn " + tdbauIssn + " for "
                  + tdbauName + " -- metadata print issn is missing");
            } else {
              taskWarning("also using tdb print issn " + tdbauIssn + " for "
                  + tdbauName + " -- different than metadata print issn: "
                  + mdinfo.issn);
            }
          }
        } else if (mdinfo.issn != null) {
          if (mdinfo.issn.equals(tdbauEissn)) {
            taskWarning("tdb eissn " + tdbauEissn + " for " + tdbauName
                + " -- reported by metadata as print issn");
          } else if (isTitleInTdb) {
            taskWarning("tdb issn missing for " + tdbauName + " -- should be: "
                + mdinfo.issn);
          }
        }

        // Validate EISSN against the TDB EISSN.
        if (tdbauEissn != null) {
          if (tdbauEissn.equals(mdinfo.issn) && (mdinfo.eissn == null)) {
            taskWarning("tdb eissn " + tdbauEissn + " for " + tdbauName
                + " -- reported by metadata as print issn");
          } else if (!tdbauEissn.equals(mdinfo.eissn)) {
            // Add both ISSNs so that they can be found either way.
            issns.add(tdbauEissn);
            if (mdinfo.eissn == null) {
              taskWarning("using tdb eissn " + tdbauEissn + " for " + tdbauName
                  + " -- metadata eissn is missing");
            } else {
              taskWarning("also using tdb eissn " + tdbauEissn + " for "
                  + tdbauName + " -- different than metadata eissn: "
                  + mdinfo.eissn);
            }
          }
        } else if (mdinfo.eissn != null) {
          if (mdinfo.eissn.equals(tdbauIssn)) {
            taskWarning("tdb print issn " + tdbauIssn + " for " + tdbauName
                + " -- reported by metadata as print eissn");
          } else if (isTitleInTdb) {
            taskWarning("tdb eissn missing for " + tdbauName
                + " -- should be: " + mdinfo.eissn);
          }
        }

        // Validate publication date against the TDB year.
        String pubYear = mdinfo.pubYear;
        if (pubYear != null) {
          if (!tdbau.includesYear(mdinfo.pubYear)) {
            if (tdbauYear != null) {
              taskWarning("tdb year " + tdbauYear + " for " + tdbauName
                  + " -- does not match metadata year " + pubYear);
            } else {
              taskWarning("tdb year missing for " + tdbauName
                  + " -- should include year " + pubYear);
            }
          }
        } else {
          pubYear = tdbauStartYear;
          if (mdinfo.pubYear != null) {
            taskWarning("using tdb start year " + mdinfo.pubYear + " for "
                + tdbauName + " -- metadata year is missing");
          }
        }
      }
    }
  }

  /**
   * Sets the indication of whether the AU being indexed is new to the index.
   *
   * @param isNew
   *          A boolean with the indication of whether the AU being indexed is
   *          new to the index.
   */
  public void setNewAu(boolean isNew) {
    isNewAu = isNew;
  }

  /**
   * Sets the full re-indexing state of this task.
   *
   * @param enable
   *          A boolean with the full re-indexing state of this task.
   */
  public void setFullReindex(boolean enable) {
    needFullReindex = enable;
  }

  /**
   * Sets the last extraction time for the AU of this task.
   *
   * @param time
   *          A boolean with the full re-indexing state of this task.
   */
  public void setLastExtractTime(long time) {
    lastExtractTime = time;
  }
}
