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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.ConfigurableManager;
import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.DeleteMetadataTask;
import org.lockss.metadata.MetadataManager;
import org.lockss.metadata.MetadataManager.ReindexingStatus;
import org.lockss.metadata.ReindexingTask;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.scheduler.StepTask;
import org.lockss.util.Logger;

/**
 * This class implements a job manager that is responsible for background tasks.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class JobManager extends BaseLockssDaemonManager implements
    ConfigurableManager {
  private static final Logger log = Logger.getLogger(JobManager.class);

  /**
   * Prefix for configuration properties.
   */
  public static final String PREFIX = Configuration.PREFIX + "jobManager.";

  /**
   * Set to true to allow JobManager to run.
   */
  public static final String PARAM_JOBMANAGER_ENABLED = PREFIX + "enabled";
  public static final boolean DEFAULT_JOBMANAGER_ENABLED = false;

  /**
   * The number of job processing tasks.
   */
  public static final String PARAM_TASK_LIST_SIZE = PREFIX + "taskListSize";

  /** 
   * The default number of job processing tasks.
   */
  public static final int DEFAULT_TASK_LIST_SIZE = 1;

  /**
   * The sleep delay when no jobs are ready.
   */
  public static final String PARAM_SLEEP_DELAY_SECONDS =
      PREFIX + "sleepDelaySeconds";

  /** 
   * The default sleep delay when no jobs are ready.
   */
  public static final long DEFAULT_SLEEP_DELAY_SECONDS = 60;

  // An indication of whether this object has been enabled.
  private boolean jobManagerEnabled = DEFAULT_JOBMANAGER_ENABLED;

  // The task list size.
  private int taskCount = DEFAULT_TASK_LIST_SIZE;

  // The sleep delay when no jobs are ready.
  private long sleepDelaySeconds = DEFAULT_SLEEP_DELAY_SECONDS;

  // The database manager.
  private DbManager dbManager = null;

  // The metadata manager.
  private MetadataManager mdManager = null;

  // The SQL code executor.
  private JobManagerSql jobManagerSql;

  private ExecutorService taskExecutor = null;
  private List<JobTask> tasks = null;

  /**
   * Handler of configuration changes.
   * 
   * @param config
   *          A Configuration with the new configuration.
   * @param prevConfig
   *          A Configuration with the previous configuration.
   * @param changedKeys
   *          A Configuration.Differences with the keys of the configuration
   *          elements that have changed.
   */
  @Override
  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    final String DEBUG_HEADER = "setConfig(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (changedKeys.contains(PREFIX)) {
      jobManagerEnabled = config.getBoolean(PARAM_JOBMANAGER_ENABLED,
	  DEFAULT_JOBMANAGER_ENABLED);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "jobManagerEnabled = " + jobManagerEnabled);

      taskCount = Math.max(0, config.getInt(PARAM_TASK_LIST_SIZE,
	  DEFAULT_TASK_LIST_SIZE));
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "taskCount = " + taskCount);

      sleepDelaySeconds = Math.max(0, config.getLong(PARAM_SLEEP_DELAY_SECONDS,
	  DEFAULT_SLEEP_DELAY_SECONDS));
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "sleepDelaySeconds = " + sleepDelaySeconds);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Starts the JobManager service.
   */
  @Override
  public void startService() {
    final String DEBUG_HEADER = "startService(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting JobManager");

    // Do nothing if not enabled
    if (!jobManagerEnabled) {
      log.info("JobManager not enabled.");
      return;
    }

    dbManager = getDaemon().getDbManager();
    mdManager = getDaemon().getMetadataManager();

    try {
      jobManagerSql = new JobManagerSql(dbManager);
    } catch (DbException dbe) {
      log.error("Cannot obtain JobManagerSql", dbe);
      return;
    }

    try {
      jobManagerSql.freeIncompleteJobs();
    } catch (DbException dbe) {
      log.error("Cannot free incomplete jobs", dbe);
      return;
    }

    // Create and get running the processing tasks.
    taskExecutor = Executors.newFixedThreadPool(taskCount);
    tasks = new ArrayList<JobTask>(taskCount);

    for (int i = 0; i < taskCount; i++) {
      JobTask task = new JobTask(dbManager, mdManager, this);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "task = " + task);

      taskExecutor.submit(task);
      tasks.add(task);
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "tasks.size() = " + tasks.size());

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "JobManager service successfully started");
  }

  /**
   * Schedules the extraction and storage of all of the metadata for an Archival
   * Unit given its identifier.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a JobAuStatus with the details of the scheduled job.
   * @throws IllegalArgumentException
   *           if the Archival Unit does not exist.
   * @throws Exception
   *           if there are problems scheduling the job.
   */
  public JobAuStatus scheduleMetadataExtraction(String auId)
      throws IllegalArgumentException, Exception {
    final String DEBUG_HEADER = "scheduleMetadataExtraction(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    JobAuStatus job = null;
    String auName = getAuName(auId);

    String message =
	"Cannot schedule metadata extraction for auId = '" + auId + "'";

    try {
      job = jobManagerSql.createMetadataExtractionJob(auId);
      job.setAuName(auName);
    } catch (IllegalArgumentException iae) {
      log.error(message, iae);
      throw iae;
    } catch (Exception e) {
      log.error(message, e);
      throw e;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "job = " + job);
    return job;
  }

  /**
   * Provides a list of existing Archival Units.
   * 
   * @param page
   *          An Integer with the index of the page to be returned.
   * @param limit
   *          An Integer with the maximum number of Archival Units to be
   *          returned.
   * @return a List<Au> with the existing Archival Units.
   * @throws Exception
   *           if there are problems getting the Archival Units.
   */
  public List<JobAuStatus> getAus(Integer page, Integer limit)
      throws Exception {
    final String DEBUG_HEADER = "getAus(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "page = " + page);
      log.debug2(DEBUG_HEADER + "limit = " + limit);
    }

    List<JobAuStatus> aus = new ArrayList<JobAuStatus>();
    List<JobAuStatus> dbAus = null;

    try {
      dbAus = jobManagerSql.getAus(page, limit);
    } catch (Exception e) {
      String message =
	  "Cannot get Archival Units for page = " + page + ", limit = " + limit;
      log.error(message, e);
      throw new Exception(message, e);
    }

    for (JobAuStatus dbAu : dbAus) {
      dbAu.setAuName(getAuName(dbAu.getAuId()));
      aus.add(dbAu);
    }

    return aus;
  }

  /**
   * Schedules the removal of the metadata stored for an Archival Unit given its
   * identifier.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a JobAuStatus with the details of the scheduled job.
   * @throws IllegalArgumentException
   *           if the Archival Unit does not exist.
   * @throws Exception
   *           if there are problems scheduling the job.
   */
  public JobAuStatus scheduleMetadataRemoval(String auId)
      throws IllegalArgumentException, Exception {
    final String DEBUG_HEADER = "scheduleMetadataRemoval(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    JobAuStatus job = null;
    String auName = getAuName(auId);

    String message =
	"Cannot schedule metadata removal for auId = '" + auId + "'";

    try {
      job = jobManagerSql.createMetadataRemovalJob(auId);
      job.setAuName(auName);
    } catch (IllegalArgumentException iae) {
      log.error(message, iae);
      throw iae;
    } catch (Exception e) {
      log.error(message, e);
      throw e;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "job = " + job);
    return job;
  }

  /**
   * Provides the job for an Archival Unit given the AU identifier.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a JobAuStatus with the details of the Archival Unit job.
   * @throws IllegalArgumentException
   *           if the Archival Unit does not exist.
   * @throws Exception
   *           if there are problems retrieving the job.
   */
  public JobAuStatus getAuJob(String auId)
      throws IllegalArgumentException, Exception {
    final String DEBUG_HEADER = "getAuJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    JobAuStatus job = null;
    String auName = getAuName(auId);
    String message = "Cannot get job for auId = '" + auId + "'";

    try {
      job = jobManagerSql.getAuJob(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "job = " + job);

      if (job != null) {
	job.setAuName(auName);
      }
    } catch (IllegalArgumentException iae) {
      log.error(message, iae);
      throw iae;
    } catch (Exception e) {
      log.error(message, e);
      throw new Exception(message, e);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "job = " + job);
    return job;
  }

  /**
   * Provides a list of existing jobs.
   * 
   * @param page
   *          An Integer with the index of the page to be returned.
   * @param limit
   *          An Integer with the maximum number of jobs to be returned.
   * @return a List<Job> with the existing jobs.
   * @throws Exception
   *           if there are problems getting the jobs.
   */
  public List<JobAuStatus> getJobs(Integer page, Integer limit)
      throws Exception {
    final String DEBUG_HEADER = "getJobs(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "page = " + page);
      log.debug2(DEBUG_HEADER + "limit = " + limit);
    }

    List<JobAuStatus> jobs = new ArrayList<JobAuStatus>();
    List<JobAuStatus> dbJobs = null;

    try {
      dbJobs = jobManagerSql.getJobs(page, limit);
    } catch (Exception e) {
      String message =
	  "Cannot get jobs for page = " + page + ", limit = " + limit;
      log.error(message, e);
      throw new Exception(message, e);
    }

    for (JobAuStatus dbJob : dbJobs) {
      dbJob.setAuName(getAuName(dbJob.getAuId()));
      jobs.add(dbJob);
    }

    return jobs;
  }

  /**
   * Deletes the job for an AU given the Archival Unit identifier if it's queued
   * and it stops any processing and deletes it if it's active.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a JobAuStatus with the details of the scheduled job removal job.
   * @throws IllegalArgumentException
   *           if the Archival Unit does not exist.
   * @throws Exception
   *           if there are problems removing the job.
   */
  public JobAuStatus removeAuJob(String auId)
      throws IllegalArgumentException, Exception {
    final String DEBUG_HEADER = "removeAuJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    JobAuStatus job = null;
    String message = "Cannot remove AU job for auId = " + auId;

    try {
      JobAuStatus auJob = getAuJob(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auJob = " + auJob);

      if (auJob != null) {
	job = removeJob(auJob.getId());
      }
    } catch (IllegalArgumentException iae) {
      log.error(message, iae);
      throw iae;
    } catch (Exception e) {
      log.error(message, e);
      throw new Exception(message, e);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "job = " + job);
    return job;
  }

  /**
   * Removes all jobs.
   * 
   * @return an int with the count of jobs removed.
   * @throws Exception
   *           if there are problems removing the jobs.
   */
  public int removeAllJobs() throws Exception {
    final String DEBUG_HEADER = "removeAllJobs(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

    for (JobTask jobTask : tasks) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobTask = " + jobTask);
      jobTask.terminateTask();
    }

    return jobManagerSql.deleteAllInactiveJobs();
  }

  /**
   * Deletes a job given the job identifier if it's queued and it stops any
   * processing and deletes it if it's active.
   * 
   * @param jobId
   *          A String with the job database identifier.
   * @return a JobAuStatus with the details of the deleted job.
   * @throws IllegalArgumentException
   *           if the job does not exist.
   * @throws Exception
   *           if there are problems removing the job.
   */
  public JobAuStatus removeJob(String jobId)
      throws IllegalArgumentException, Exception {
    final String DEBUG_HEADER = "removeJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobId = " + jobId);

    String message = "Cannot remove job for jobId = " + jobId;
    JobAuStatus job = null;

    try {
      job = jobManagerSql.getJob(jobId);
      Long jobSeq = Long.valueOf(job.getId());
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobSeq = " + jobSeq);

      job = removeJob(jobSeq);
    } catch (IllegalArgumentException iae) {
      log.error(message, iae);
      throw iae;
    } catch (Exception e) {
      log.error(message, e);
      throw new Exception(message, e);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "job = " + job);
    return job;
  }

  /**
   * Deletes a job given the job identifier if it's queued and it stops any
   * processing and deletes it if it's active.
   * 
   * @param jobSeq
   *          A Long with the job database identifier.
   * @return a JobAuStatus with the details of the deleted job.
   * @throws IllegalArgumentException
   *           if the job does not exist.
   * @throws Exception
   *           if there are problems removing the job.
   */
  private JobAuStatus removeJob(Long jobSeq)
      throws IllegalArgumentException, Exception {
    final String DEBUG_HEADER = "removeJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    String message = "Cannot remove job for jobSeq = " + jobSeq;
    JobAuStatus jobAuStatus = null;

    try {
      for (JobTask jobTask : tasks) {
        if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobTask = " + jobTask);

        if (jobSeq == jobTask.getJobId()) {
          jobTask.terminateTask();
          break;
        }
      }

      jobAuStatus = jobManagerSql.deleteJob(jobSeq);
    } catch (IllegalArgumentException iae) {
      log.error(message, iae);
      throw iae;
    } catch (Exception e) {
      log.error(message, e);
      throw new Exception(message, e);
    }

    jobAuStatus.setAuName(getAuName(jobAuStatus.getAuId()));

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "jobAuStatus = " + jobAuStatus);
    return jobAuStatus;
  }

  /**
   * Provides the status of a job given the job identifier.
   * 
   * @param jobId
   *          A String with the job database identifier.
   * @return a JobAuStatus with the status of the job.
   * @throws IllegalArgumentException
   *           if the job does not exist.
   * @throws Exception
   *           if there are problems getting the status of the job.
   */
  public JobAuStatus getJobStatus(String jobId)
      throws IllegalArgumentException, Exception {
    final String DEBUG_HEADER = "getJobStatus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobId = " + jobId);

    String message = "Cannot get status job for jobId = " + jobId;
    JobAuStatus status = null;

    try {
      status = jobManagerSql.getJob(jobId);
    } catch (IllegalArgumentException iae) {
      log.error(message, iae);
      throw iae;
    } catch (Exception e) {
      log.error(message, e);
      throw new Exception(message, e);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "status = " + status);
    return status;
  }

  /**
   * Handles a metadata extraction job starting event.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit whose metadata
   *          is being extracted.
   */
  public void handlePutAuJobStartEvent(String auId) {
    final String DEBUG_HEADER = "handlePutAuJobStartEvent(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "auId = " + auId);

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      Long jobSeq = Long.valueOf(jobManagerSql.getAuJob(conn, auId).getId());
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobSeq = " + jobSeq);

      jobManagerSql.markJobAsRunning(conn, jobSeq, "Extracting metadata");
      DbManager.commitOrRollback(conn, log);
    } catch (Exception e) {
      String message = "Error handling start of metadata extraction";
      log.error(message, e);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Handles a metadata extraction job finishing event.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit whose metadata
   *          is being extracted.
   * @param status
   *          A ReindexingStatus with the status of the event.
   * @param exception
   *          An Exception with the exception (if any) thrown by the job and to
   *          be recorded as part of the status of the job.
   */
  public void handlePutAuJobFinishEvent(String auId, ReindexingStatus status,
      Exception exception) {
    final String DEBUG_HEADER = "handlePutAuJobFinishEvent(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "auId = " + auId);
      log.debug2(DEBUG_HEADER + "status = " + status);
      log.debug2(DEBUG_HEADER + "exception = " + exception);
    }

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      JobAuStatus jobAuStatus = jobManagerSql.getAuJob(conn, auId);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "jobAuStatus = " + jobAuStatus);

      if (jobAuStatus != null) {
	Long jobSeq = Long.valueOf(jobAuStatus.getId());
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobSeq = " + jobSeq);

	int markedJobs = -1;

	if (status == ReindexingStatus.Success) {
	  markedJobs = jobManagerSql.markJobAsDone(conn, jobSeq, "Success");
	} else {
	  markedJobs = jobManagerSql.markJobAsDone(conn, jobSeq,
	      "Failure: " + exception);
	}

	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "markedJobs = " + markedJobs);

	DbManager.commitOrRollback(conn, log);
      }

      // Loop through all the tasks.
      for (JobTask jobTask : tasks) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobTask = " + jobTask);

	// Get its step task.
	StepTask stepTask = jobTask.getStepTask();
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "stepTask = " + stepTask);

	// Check whether this is the task linked to the finishing event.  
	if (stepTask != null
	    && auId.equals(((ReindexingTask)stepTask).getAuId())) {
	  // Yes: Mark this task as finished.
	  jobTask.notifyJobFinish();
	  break;
	}
      }
    } catch (Exception e) {
      String message = "Error handling finish of metadata extraction";
      log.error(message, e);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Handles a metadata removal job starting event.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit whose metadata
   *          is being removed.
   */
  public void handleDeleteAuJobStartEvent(String auId) {
    final String DEBUG_HEADER = "handleDeleteAuJobStartEvent(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      Long jobSeq = Long.valueOf(jobManagerSql.getAuJob(conn, auId).getId());
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobSeq = " + jobSeq);

      jobManagerSql.markJobAsRunning(conn, jobSeq, "Deleting metadata");
      DbManager.commitOrRollback(conn, log);
    } catch (Exception e) {
      String message = "Error handling start of metadata removal";
      log.error(message, e);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Handles a metadata removal job finishing event.
   * 
   * @param auId
   *          A String with the identifier of the Archival Unit whose metadata
   *          is being removed.
   */
  public void handleDeleteAuJobFinishEvent(String auId, ReindexingStatus status,
      Exception exception) {
    final String DEBUG_HEADER = "handleDeleteAuJobFinishEvent(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "auId = " + auId);
      log.debug2(DEBUG_HEADER + "status = " + status);
      log.debug2(DEBUG_HEADER + "exception = " + exception);
    }

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      JobAuStatus jobAuStatus = jobManagerSql.getAuJob(conn, auId);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "jobAuStatus = " + jobAuStatus);

      if (jobAuStatus != null) {
	Long jobSeq = Long.valueOf(jobAuStatus.getId());
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobSeq = " + jobSeq);

	int markedJobs = -1;

	if (status == ReindexingStatus.Success) {
	  markedJobs = jobManagerSql.markJobAsDone(conn, jobSeq, "Success");
	} else {
	  markedJobs = jobManagerSql.markJobAsDone(conn, jobSeq,
	      "Failure: " + exception);
	}

	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "markedJobs = " + markedJobs);

	DbManager.commitOrRollback(conn, log);
      }

      for (JobTask jobTask : tasks) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobTask = " + jobTask);

	if (auId.equals(
	    ((DeleteMetadataTask)(jobTask.getStepTask())).getAuId())) {
	  jobTask.notifyJobFinish();
	  break;
	}
      }
    } catch (Exception e) {
      String message = "Error handling finish of metadata removal";
      log.error(message, e);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the identifier of the Archival Unit of a job.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param jobSeq
   *          A Long with the job identifier.
   * @return a String with the identifier of the Archival Unit.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  String getJobAuId(Connection conn, Long jobSeq) throws DbException {
    return jobManagerSql.getJobAuId(conn, jobSeq);
  }

  /**
   * Claims the next job for a task.
   * 
   * @param owner
   *          A String with the name of the task claiming its next job.
   * @return a Long with the database identifier of the job that has been
   *         claimed.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long claimNextJob(String owner) throws DbException {
    return jobManagerSql.claimNextJob(owner);
  }

  /**
   * Provides the type of a job.
   * 
   * @param jobSeq
   *          A Long with the job identifier.
   * @return a String with the job type.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  String getJobType(Long jobSeq) throws DbException {
    return jobManagerSql.getJobType(jobSeq);
  }

  /**
   * Marks a job as finished.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param jobSeq
   *          A Long with the database identifier of the job being claimed.
   * @param statusName
   *          A String with the name of the job status.
   * @param statusMessage
   *          A String with the status message.
   * @return an int with the count of jobs updated.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int markJobAsFinished(Connection conn, Long jobSeq, String statusName,
      String statusMessage) throws DbException {
    return jobManagerSql.markJobAsFinished(conn, jobSeq, statusName,
	statusMessage);
  }

  /**
   * Marks a job as done.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param jobSeq
   *          A Long with the database identifier of the job being claimed.
   * @param statusMessage
   *          A String with the status message.
   * @return an int with the count of jobs updated.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int markJobAsDone(Connection conn, Long jobSeq, String statusMessage)
      throws DbException {
    return jobManagerSql.markJobAsDone(conn, jobSeq, statusMessage);
  }

  /**
   * Provides the name of an Archival Unit.
   * 
   * @param auId
   *          a String with the Archival Unit identifier.
   * @return a String with the Archival Unit name.
   * @throws IllegalArgumentException
   *           if the Archival Unit does not exist.
   * @throws Exception
   *           if there are problems getting the Archival Unit name.
   */
  private String getAuName(String auId)
      throws IllegalArgumentException, Exception {
    final String DEBUG_HEADER = "getAuName(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    String message = "Cannot find Archival Unit for auId '" + auId + "'";

    try {
      // Get the Archival Unit.
      ArchivalUnit au =
	  LockssDaemon.getLockssDaemon().getMetadataManager().getAu(auId);
      if (log.isDebug3()) log.debug3("au = " + au);

      // Check whether it does exist.
      if (au != null) {
	// Yes: Get its name.
	String auName = au.getName();
	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auName = " + auName);
	return auName;
      }
    } catch (IllegalArgumentException iae) {
      log.error(message, iae);
      throw iae;
    } catch (Exception e) {
      log.error(message, e);
      throw e;
    }

    // It does not exist: Report the problem.
    log.error(message);
    throw new IllegalArgumentException(message);
  }

  /**
   * Provides the sleep delay in seconds when no jobs are ready.
   * 
   * @return a long with the sleep delay in seconds when no jobs are ready.
   */
  long getSleepDelaySeconds() {
    return sleepDelaySeconds;
  }
}
