/*

 Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.job;

import static org.lockss.db.SqlConstants.*;
import java.sql.Connection;
import org.lockss.app.LockssDaemon;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.scheduler.StepTask;
import org.lockss.util.Logger;

/**
 * Processor for a job.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class JobTask implements Runnable {
  private static final Logger log = Logger.getLogger(JobTask.class);
  private static final long sleepMs = 60000;

  private String baseTaskName;
  private String taskName;
  private Long jobSeq = null;
  private DbManager dbManager;
  private MetadataManager mdManager;
  private JobManager jobManager;
  private boolean isJobFinished = false;
  private StepTask stepTask = null;

  /**
   * Constructor.
   * 
   * @param jobManagerSql
   *          A JobManagerSql with the Job SQL code executor.
   */
  public JobTask(DbManager dbManager, MetadataManager mdManager,
      JobManager jobManager) {
    this.dbManager = dbManager;
    this.mdManager = mdManager;
    this.jobManager = jobManager;
  }

  /**
   * Main task process.
   */
  public void run() {
    final String DEBUG_HEADER = "run(): ";
    LockssDaemon daemon = LockssDaemon.getLockssDaemon();

    // Wait until the Archival Units have been started.
    if (!daemon.areAusStarted()) {
      log.debug(DEBUG_HEADER + "Waiting for Archival Units to start");

      while (!daemon.areAusStarted()) {
	try {
	  daemon.waitUntilAusStarted();
	} catch (InterruptedException ex) {
	}
      }
    }

    baseTaskName = Thread.currentThread().getName();
    taskName = baseTaskName;
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "Invoked task '" + baseTaskName + "'");

    // Infinite loop.
    while (jobSeq == null) {
      try {
	// Claim the next job.
	jobSeq = jobManager.claimNextJob(taskName);

	// Check whether a job was actually claimed.
	if (jobSeq != null) {
	  // Yes: Process it.
	  taskName = baseTaskName + " - jobSeq=" + jobSeq;
	  processJob(jobSeq);
	} else {
	  // No: Wait before the next attempt.
	  sleep(DEBUG_HEADER);
	}
      } catch (Exception e) {
	log.error("Exception caught claiming or processing job: ", e);
      } finally {
	jobSeq = null;
	stepTask = null;
	taskName = baseTaskName;
	isJobFinished = false;
      }
    }
  }

  /**
   * Processes a job.
   * 
   * @param jobSeq
   *          A Long with the database identifier of the job to be processed.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void processJob(Long jobSeq) throws DbException {
    final String DEBUG_HEADER = "processJob() - " + taskName + ": ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    // Get the type of job.
    String jobType = jobManager.getJobType(jobSeq);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobType = " + jobType);

    // Check whether it's a metadata extraction job.
    if (JOB_TYPE_PUT_AU.equals(jobType)) {
      // Yes: Extract the metadata.
      processPutAuJob(jobSeq);
      // No: Check whether it's a metadata removal job.
    } else if (JOB_TYPE_DELETE_AU.equals(jobType)) {
      // Yes: Remove the metadata.
      processDeleteAuJob(jobSeq);
    } else {
      // No: Handle a job of an unknown type.
      Connection conn = null;

      try {
        // Get a connection to the database.
        conn = dbManager.getConnection();

        jobManager.markJobAsDone(conn, jobSeq, "Unknown job type");
        DbManager.commitOrRollback(conn, log);
        log.error("Ignored job " + jobSeq + " of unknown type = " + jobType);
      } finally {
        DbManager.safeRollbackAndClose(conn);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Processes a job that extracts and stores the metadata of an Archival Unit.
   * 
   * @param jobSeq
   *          A Long with the database identifier of the job to be processed.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void processPutAuJob(Long jobSeq) throws DbException {
    final String DEBUG_HEADER = "processPutAuJob() - " + taskName + ": ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      processPutAuJob(conn, jobSeq);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Processes a job that extracts and stores the metadata of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param jobSeq
   *          A Long with the database identifier of the job to be processed.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void processPutAuJob(Connection conn, Long jobSeq)
      throws DbException {
    final String DEBUG_HEADER = "processPutAuJob() - " + taskName + ": ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    String auId = jobManager.getJobAuId(conn, jobSeq);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

    // Extract the metadata.
    stepTask = mdManager.onDemandStartReindexing(auId);

    // Wait until the process is done.
    while (!isJobFinished) {
      sleep(DEBUG_HEADER);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Processes a job that deletes the metadata of an Archival Unit.
   * 
   * @param jobSeq
   *          A Long with the database identifier of the job to be processed.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void processDeleteAuJob(Long jobSeq) throws DbException {
    final String DEBUG_HEADER = "processDeleteAuJob() - " + taskName + ": ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      processDeleteAuJob(conn, jobSeq);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Processes a job that deletes the metadata of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param jobSeq
   *          A Long with the database identifier of the job to be processed.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void processDeleteAuJob(Connection conn, Long jobSeq)
      throws DbException {
    final String DEBUG_HEADER = "processDeleteAuJob() - " + taskName + ": ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    String auId = jobManager.getJobAuId(conn, jobSeq);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

    // Delete the metadata.
    stepTask = mdManager.startMetadataRemoval(auId);

    // Wait until the process is done.
    while (!isJobFinished) {
      sleep(DEBUG_HEADER);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Terminates this task.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void terminateTask() throws DbException {
    final String DEBUG_HEADER = "terminateTask() - " + taskName + ": ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

    if (getJobId() != null) {
      Connection conn = null;

      try {
	// Get a connection to the database.
	conn = dbManager.getConnection();

	jobManager.markJobAsFinished(conn, jobSeq, JOB_STATUS_TERMINATED,
	    "Terminated by request");
	DbManager.commitOrRollback(conn, log);
      } finally {
	DbManager.safeRollbackAndClose(conn);
      }
    } else {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Nothing to do: jobSeq is null.");
    }

    if (stepTask != null) {
      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER
	    + "stepTask.isFinished() = " + stepTask.isFinished());
	log.debug3(DEBUG_HEADER + "Cancelling task " + stepTask);
      }

      stepTask.cancel();
    } else {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Nothing to do: stepTask is null.");
      notifyJobFinish();
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the job identifier of this task.
   * 
   * @return a Long with the job identifier of this task.
   */
  public Long getJobId() {
    return jobSeq;
  }

  /**
   * Provides the underlying step task of this task.
   * 
   * @return a StepTask with the underlying step task of this task.
   */
  public StepTask getStepTask() {
    return stepTask;
  }

  /**
   * Marks the job of this task as finished.
   */
  void notifyJobFinish() {
    isJobFinished = true;
  }

  /**
   * Waits for the standard polling amount of time to pass.
   * 
   * @param id
   *          A String with the name of the method requesting the wait.
   */
  private void sleep(String id) {
    if (log.isDebug3())
      log.debug3(id + "Going to sleep task '" + taskName + "'");

    try {
      Thread.sleep(sleepMs);
    } catch (InterruptedException ie) {}

    if (log.isDebug3())
      log.debug3(id + "Back from sleep task '" + taskName + "'");
  }

  @Override
  public String toString() {
    return "[JobTask taskname=" + taskName + ", jobSeq=" + jobSeq + "]";
  }
}
