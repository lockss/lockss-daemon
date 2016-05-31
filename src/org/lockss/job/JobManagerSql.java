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

import static java.sql.Types.*;
import static org.lockss.db.SqlConstants.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.LocalDate;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.plugin.PluginManager;
import org.lockss.util.Logger;

/**
 * The JobManager SQL code executor.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class JobManagerSql {
  private static final Logger log = Logger.getLogger(JobManagerSql.class);

  private static final String INITIAL_JOB_STATUS_MESSAGE = "Waiting for launch";

  // Query to retrieve a job.
  private static final String FIND_JOB_QUERY = "select "
      + JOB_SEQ_COLUMN
      + ", " + JOB_TYPE_SEQ_COLUMN
      + ", " + DESCRIPTION_COLUMN
      + ", " + PLUGIN_ID_COLUMN
      + ", " + AU_KEY_COLUMN
      + ", " + CREATION_TIME_COLUMN
      + ", " + START_TIME_COLUMN
      + ", " + END_TIME_COLUMN
      + ", " + JOB_STATUS_SEQ_COLUMN
      + ", " + STATUS_MESSAGE_COLUMN
      + ", " + PRIORITY_COLUMN
      + " from " + JOB_TABLE
      + " where " + JOB_SEQ_COLUMN + " = ?";

  // Query to retrieve all the jobs of an Archival Unit.
  private static final String FIND_AU_JOB_QUERY = "select "
      + JOB_SEQ_COLUMN
      + ", " + JOB_TYPE_SEQ_COLUMN
      + ", " + DESCRIPTION_COLUMN
      + ", " + PLUGIN_ID_COLUMN
      + ", " + AU_KEY_COLUMN
      + ", " + CREATION_TIME_COLUMN
      + ", " + START_TIME_COLUMN
      + ", " + END_TIME_COLUMN
      + ", " + JOB_STATUS_SEQ_COLUMN
      + ", " + STATUS_MESSAGE_COLUMN
      + ", " + PRIORITY_COLUMN
      + " from " + JOB_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?"
      + " order by " + JOB_SEQ_COLUMN;

  // Query to delete all jobs.
  private static final String DELETE_ALL_JOBS_QUERY = "delete from "
      + JOB_TABLE;

  // Query to delete a job.
  private static final String DELETE_JOB_QUERY = DELETE_ALL_JOBS_QUERY
      + " where " + JOB_SEQ_COLUMN + " = ?";

  // Query to retrieve all the job statues.
  private static final String GET_JOB_STATUSES_QUERY = "select "
      + JOB_STATUS_SEQ_COLUMN
      + "," + STATUS_NAME_COLUMN
      + " from " + JOB_STATUS_TABLE;

  // Query to retrieve all the job types.
  private static final String GET_JOB_TYPES_QUERY = "select "
      + JOB_TYPE_SEQ_COLUMN
      + "," + TYPE_NAME_COLUMN
      + " from " + JOB_TYPE_TABLE;

  // Query to add a job.
  private static final String INSERT_JOB_QUERY = "insert into "
      + JOB_TABLE
      + "(" + JOB_SEQ_COLUMN
      + "," + JOB_TYPE_SEQ_COLUMN
      + "," + DESCRIPTION_COLUMN
      + "," + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + "," + CREATION_TIME_COLUMN
      + "," + START_TIME_COLUMN
      + "," + END_TIME_COLUMN
      + "," + JOB_STATUS_SEQ_COLUMN
      + "," + STATUS_MESSAGE_COLUMN
      + "," + PRIORITY_COLUMN
      + ") values (default,?,?,?,?,?,?,?,?,?,"
      + "(select coalesce(max(" + PRIORITY_COLUMN + "), 0) + 1"
      + " from " + JOB_TABLE
      + " where " + PRIORITY_COLUMN + " >= 0))";

  // Query to add a job using MySQL.
  private static final String INSERT_JOB_MYSQL_QUERY = "insert into "
      + JOB_TABLE
      + "(" + JOB_SEQ_COLUMN
      + "," + JOB_TYPE_SEQ_COLUMN
      + "," + DESCRIPTION_COLUMN
      + "," + PLUGIN_ID_COLUMN
      + "," + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + "," + CREATION_TIME_COLUMN
      + "," + START_TIME_COLUMN
      + "," + END_TIME_COLUMN
      + "," + JOB_STATUS_SEQ_COLUMN
      + "," + STATUS_MESSAGE_COLUMN
      + "," + PRIORITY_COLUMN
      + ") values (default,?,?,?,?,?,?,?,?,?,?,"
      + "(select next_priority from "
      + "(select coalesce(max(" + PRIORITY_COLUMN + "), 0) + 1 as next_priority"
      + " from " + JOB_TABLE
      + " where " + PRIORITY_COLUMN + " >= 0) as temp_job_table))";

  // Query to find a page of Archival Units.
  private static final String FIND_OFFSET_AUS_QUERY = "select "
      + JOB_SEQ_COLUMN
      + ", " + JOB_TYPE_SEQ_COLUMN
      + ", " + PLUGIN_ID_COLUMN
      + ", " + AU_KEY_COLUMN
      + " from " + JOB_TABLE
      + " order by " + PLUGIN_ID_COLUMN + "," + AU_KEY_COLUMN
      + " offset ?";

  // Query to find a page of jobs.
  private static final String FIND_OFFSET_JOBS_QUERY = "select "
      + JOB_SEQ_COLUMN
      + ", " + JOB_TYPE_SEQ_COLUMN
      + ", " + DESCRIPTION_COLUMN
      + ", " + PLUGIN_ID_COLUMN
      + ", " + AU_KEY_COLUMN
      + ", " + CREATION_TIME_COLUMN
      + ", " + START_TIME_COLUMN
      + ", " + END_TIME_COLUMN
      + ", " + JOB_STATUS_SEQ_COLUMN
      + ", " + STATUS_MESSAGE_COLUMN
      + ", " + PRIORITY_COLUMN
      + " from " + JOB_TABLE
      + " order by " + JOB_SEQ_COLUMN
      + " offset ?";

  // Query to delete inactive jobs.
  private static final String DELETE_INACTIVE_JOBS_QUERY = DELETE_ALL_JOBS_QUERY
	+ " where " + JOB_STATUS_SEQ_COLUMN + " != ?"
	+ " and " + JOB_STATUS_SEQ_COLUMN + " != ?";

  // Query to delete an inactive job.
  private static final String DELETE_INACTIVE_JOB_QUERY = DELETE_JOB_QUERY
	+ " and " + JOB_STATUS_SEQ_COLUMN + " != ?"
	+ " and " + JOB_STATUS_SEQ_COLUMN + " != ?";

  // Query to free incomplete jobs so that they can be started again.
  private static final String FREE_INCOMPLETE_JOBS_QUERY = "update " + JOB_TABLE
	+ " set " + START_TIME_COLUMN + " = null"
	+ ", " + END_TIME_COLUMN + " = null"
	+ ", " + JOB_STATUS_SEQ_COLUMN + " = ?"
	+ ", " + STATUS_MESSAGE_COLUMN + " = '" + INITIAL_JOB_STATUS_MESSAGE
	+ "'"
	+ ", " + OWNER_COLUMN + " = null"
	+ " where " + OWNER_COLUMN + " is not null"
	+ " and " + JOB_STATUS_SEQ_COLUMN + " < ?";

  // Query to select the highest priority job.
  private static final String FIND_HIGHEST_PRIORITY_JOBS_QUERY = "select "
      + JOB_SEQ_COLUMN
      + " from " + JOB_TABLE
      + " where " + OWNER_COLUMN + " is null"
      + " order by " + PRIORITY_COLUMN
      + ", " + JOB_SEQ_COLUMN;

  // Query to claim an unclaimed job.
  private static final String CLAIM_UNCLAIMED_JOB_QUERY = "update "
      + JOB_TABLE
      + " set " + OWNER_COLUMN + " = ?"
      + " where " + OWNER_COLUMN + " is null"
      + " and " + JOB_SEQ_COLUMN + " = ?";

  // Query to retrieve a job type.
  private static final String FIND_JOB_TYPE_QUERY = "select "
      + "jt." + TYPE_NAME_COLUMN
      + " from " + JOB_TABLE + " j"
      + ", " + JOB_TYPE_TABLE + " jt"
      + " where j." + JOB_TYPE_SEQ_COLUMN + " = jt." + JOB_TYPE_SEQ_COLUMN
      + " and j." + JOB_SEQ_COLUMN + " = ?";

  // Query to mark a job as running.
  private static final String MARK_JOB_AS_RUNNING_QUERY = "update "
      + JOB_TABLE
      + " set " + START_TIME_COLUMN + " = ?"
      + ", " + JOB_STATUS_SEQ_COLUMN + " = ?"
      + ", " + STATUS_MESSAGE_COLUMN + " = ?"
      + " where " + JOB_SEQ_COLUMN + " = ?";

  // Query to mark a job as finished.
  private static final String MARK_JOB_AS_FINISHED_QUERY = "update "
      + JOB_TABLE
      + " set " + END_TIME_COLUMN + " = ?"
      + ", " + JOB_STATUS_SEQ_COLUMN + " = ?"
      + ", " + STATUS_MESSAGE_COLUMN + " = ?"
      + " where " + JOB_SEQ_COLUMN + " = ?";

  // Query to update the status of a job.
  private static final String UPDATE_JOB_STATUS_QUERY = "update "
      + JOB_TABLE
      + " set " + JOB_STATUS_SEQ_COLUMN + " = ?"
      + ", " + STATUS_MESSAGE_COLUMN + " = ?"
      + " where " + JOB_SEQ_COLUMN + " = ?";

  private final DbManager dbManager;
  private final Map<String, Long> jobStatusSeqByName;
  private final Map<String, Long> jobTypeSeqByName;

  /**
   * Constructor.
   */
  JobManagerSql(DbManager dbManager) throws DbException {
    this.dbManager = dbManager;

    // Get a connection to the database.
    Connection conn = dbManager.getConnection();

    try {
      jobStatusSeqByName = mapJobStatusByName(conn);
      jobTypeSeqByName = mapJobTypeByName(conn);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Populates the map of job status database identifiers by name.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a Map<String, Long> with the map of job status database identifiers
   *         by name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Map<String, Long> mapJobStatusByName(Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "mapJobStatusByName(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
    Map<String, Long> result = new HashMap<String, Long>();

    PreparedStatement stmt =
	dbManager.prepareStatement(conn, GET_JOB_STATUSES_QUERY);
    ResultSet resultSet = null;

    try {
      resultSet = dbManager.executeQuery(stmt);

      // Loop through the job statuses.
      while (resultSet.next()) {
  	Long jobStatusSeq = resultSet.getLong(JOB_STATUS_SEQ_COLUMN);
  	if (log.isDebug3())
  	  log.debug3(DEBUG_HEADER + "jobStatusSeq = " + jobStatusSeq);

  	String statusName = resultSet.getString(STATUS_NAME_COLUMN);
  	if (log.isDebug3())
  	  log.debug3(DEBUG_HEADER + "statusName = " + statusName);

  	result.put(statusName, jobStatusSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot get the job statuses";
      log.error(message, sqle);
      log.error("SQL = '" + GET_JOB_STATUSES_QUERY + "'.");
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot get the job statuses";
      log.error(message, dbe);
      log.error("SQL = '" + GET_JOB_STATUSES_QUERY + "'.");
      throw dbe;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(stmt);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return result;
  }

  /**
   * Populates the map of job type database identifiers by name.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a Map<String, Long> with the map of job type database identifiers
   *         by name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Map<String, Long> mapJobTypeByName(Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "mapJobTypeByName(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");
    Map<String, Long> result = new HashMap<String, Long>();

    PreparedStatement stmt =
	dbManager.prepareStatement(conn, GET_JOB_TYPES_QUERY);
    ResultSet resultSet = null;

    try {
      resultSet = dbManager.executeQuery(stmt);

      // Loop through the job statuses.
      while (resultSet.next()) {
  	Long jobTypeSeq = resultSet.getLong(JOB_TYPE_SEQ_COLUMN);
  	if (log.isDebug3())
  	  log.debug3(DEBUG_HEADER + "jobTypeSeq = " + jobTypeSeq);

  	String typeName = resultSet.getString(TYPE_NAME_COLUMN);
  	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "typeName = " + typeName);

  	result.put(typeName, jobTypeSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot get the job statuses";
      log.error(message, sqle);
      log.error("SQL = '" + GET_JOB_TYPES_QUERY + "'.");
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot get the job statuses";
      log.error(message, dbe);
      log.error("SQL = '" + GET_JOB_TYPES_QUERY + "'.");
      throw dbe;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(stmt);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return result;
  }

  /**
   * Creates a job to extract and store the metadata of an Archival Unit.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a JobAuStatus with the created metadata extraction job properties.
   * @throws IllegalArgumentException
   *           if the Archival Unit does not exist.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  JobAuStatus createMetadataExtractionJob(String auId)
      throws IllegalArgumentException, DbException {
    final String DEBUG_HEADER = "createMetadataExtractionJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    JobAuStatus result = null;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Create the Archival Unit metadata extraction job.
      result = createMetadataExtractionJob(conn, auId);

      DbManager.commitOrRollback(conn, log);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates a job to extract and store the metadata of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a JobAuStatus with the created metadata extraction job properties.
   * @throws IllegalArgumentException
   *           if the Archival Unit does not exist.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private JobAuStatus createMetadataExtractionJob(Connection conn, String auId)
      throws IllegalArgumentException, DbException {
    final String DEBUG_HEADER = "createMetadataExtractionJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    JobAuStatus result = null;

    // Get any existing jobs for the Archival Unit.
    List<JobAuStatus> auJobs = getAuJobs(conn, auId);

    // Loop through all the Archival Unit jobs.
    for (JobAuStatus job : auJobs) {
      Long jobTypeSeq = job.getType();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "jobTypeSeq = " + jobTypeSeq);

      // Check whether it's a job extracting the metadata of the Archival Unit.
      if (jobTypeSeqByName.get(JOB_TYPE_PUT_AU).equals(jobTypeSeq)) {
	// Yes: Get its status.
	Long jobStatusSeq = (long)job.getStatusCode();

	// Check whether it has not been started.
	if (jobStatusSeqByName.get(JOB_STATUS_CREATED).equals(jobStatusSeq)) {
	  // Yes: Do not create a new job: Reuse the existing one.
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Reusing job = " + job);
	  result = job;
	} else {
	  // No: Try to delete it.
	  boolean deletedPutJob =
	      deleteInactiveJob(conn, Long.valueOf(job.getId()));
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "deletedPutJob = " + deletedPutJob);

	  // Check whether it could not be deleted.
	  if (!deletedPutJob) {
	    // Yes: Do not create a new job: Reuse the existing one.
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "Reusing job = " + job);
	    result = job;
	  } else {
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "New job needed");
	  }
	}
      } else if (jobTypeSeqByName.get(JOB_TYPE_DELETE_AU).equals(jobTypeSeq)) {
	// Yes: Try to delete it.
	boolean deletedDeleteJob =
	    deleteInactiveJob(conn, Long.valueOf(job.getId()));
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "deletedDeleteJob = " + deletedDeleteJob);
      }
    }

    // Check whether no job can be reused.
    if (result == null) {
      // Yes: Create the job.
      Long jobSeq = addJob(conn, jobTypeSeqByName.get(JOB_TYPE_PUT_AU),
	  "Metadata Extraction", auId, new Date().getTime(), null, null,
	  jobStatusSeqByName.get(JOB_STATUS_CREATED),
	  INITIAL_JOB_STATUS_MESSAGE);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobSeq = " + jobSeq);

      result = getJob(conn, jobSeq);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the properties of a job.
   * 
   * @param jobSeq
   *          A Long with the job identifier.
   * @return a JobAuStatus with the properties of the job.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  JobAuStatus getJob(Long jobSeq) throws DbException {
    final String DEBUG_HEADER = "getJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    JobAuStatus result = null;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      result = getJob(conn, jobSeq);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the properties of a job.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param jobSeq
   *          A Long with the job identifier.
   * @return a JobAuStatus with the properties of the job.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private JobAuStatus getJob(Connection conn, Long jobSeq) throws DbException {
    final String DEBUG_HEADER = "getJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    JobAuStatus job = null;

    PreparedStatement findJob =
	dbManager.prepareStatement(conn, FIND_JOB_QUERY);

    ResultSet resultSet = null;

    try {
      // Get the job.
      findJob.setLong(1, jobSeq);
      resultSet = dbManager.executeQuery(findJob);

      if (resultSet.next()) {
	job = getJobFromResultSet(resultSet);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Found job " + job);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find AU jobs";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_JOB_QUERY + "'.");
      log.error("jobSeq = " + jobSeq);
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot find AU jobs";
      log.error(message, dbe);
      log.error("SQL = '" + FIND_JOB_QUERY + "'.");
      log.error("jobSeq = " + jobSeq);
      throw dbe;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findJob);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "job = " + job);
    return job;
  }

  /**
   * Populates the data of a job with a result set.
   * 
   * @param resultSet
   *          A ResultSet with the source of the data.
   * @return a JobAuStatus with the resulting job data.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private JobAuStatus getJobFromResultSet(ResultSet resultSet)
      throws SQLException {
    JobAuStatus job = new JobAuStatus();

    job.setId(String.valueOf(resultSet.getLong(JOB_SEQ_COLUMN)));
    job.setType(resultSet.getLong(JOB_TYPE_SEQ_COLUMN));

    String description = resultSet.getString(DESCRIPTION_COLUMN);

    if (!resultSet.wasNull()) {
      job.setDescription(description);
    }

    job.setCreationDate(new LocalDate(resultSet.getLong(CREATION_TIME_COLUMN)));

    Long startTime = resultSet.getLong(START_TIME_COLUMN);

    if (!resultSet.wasNull()) {
      job.setStartDate(new LocalDate(startTime));
    }

    Long endTime = resultSet.getLong(END_TIME_COLUMN);

    if (!resultSet.wasNull()) {
      job.setEndDate(new LocalDate(endTime));
    }

    String pluginId = resultSet.getString(PLUGIN_ID_COLUMN);

    if (pluginId != null && pluginId.trim().length() > 0) {
      String auKey = resultSet.getString(AU_KEY_COLUMN);

      if (auKey != null && auKey.trim().length() > 0) {
	job.setAuId(PluginManager.generateAuId(pluginId, auKey));
      }
    }

    job.setStatusCode((int)resultSet.getLong(JOB_STATUS_SEQ_COLUMN));

    String statusMessage = resultSet.getString(STATUS_MESSAGE_COLUMN);

    if (!resultSet.wasNull()) {
      job.setStatusMessage(statusMessage);
    }

    return job;
  }

  /**
   * Provides the jobs of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a List<JobAuStatus> with the jobs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private List<JobAuStatus> getAuJobs(Connection conn, String auId)
      throws DbException {
    final String DEBUG_HEADER = "getAuJobs(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    List<JobAuStatus> jobs = new ArrayList<JobAuStatus>();

    String pluginId = null;
    String auKey = null;
    PreparedStatement findJobs =
	dbManager.prepareStatement(conn, FIND_AU_JOB_QUERY);

    ResultSet resultSet = null;
    JobAuStatus job;

    try {

      // Get the Archival Unit jobs.
      pluginId = PluginManager.pluginIdFromAuId(auId);
      findJobs.setString(1, pluginId);

      auKey = PluginManager.auKeyFromAuId(auId);
      findJobs.setString(2, auKey);

      resultSet = dbManager.executeQuery(findJobs);

      // Loop through the results.
      while (resultSet.next()) {
	// Get the next job.
	job = getJobFromResultSet(resultSet);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Found job " + job);

	// Add it to the results.
	jobs.add(job);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find AU jobs";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_AU_JOB_QUERY + "'.");
      log.error("auId = " + auId);
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot find AU jobs";
      log.error(message, dbe);
      log.error("SQL = '" + FIND_AU_JOB_QUERY + "'.");
      log.error("auId = " + auId);
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
      throw dbe;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findJobs);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobs = " + jobs);
    return jobs;
  }

  /**
   * Adds a job to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a Long with the database identifier of the created metadata
   *         extraction job.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long addJob(Connection conn, Long jobTypeSeq, String description,
      String auId, long creationTime, Long startTime, Long endTime,
      Long jobStatusSeq, String statusMessage) throws DbException {
    final String DEBUG_HEADER = "addJob(): ";
    if (log.isDebug2()) {
      log.debug(DEBUG_HEADER + "jobTypeSeq = " + jobTypeSeq);
      log.debug(DEBUG_HEADER + "description = " + description);
      log.debug(DEBUG_HEADER + "auId = " + auId);
      log.debug(DEBUG_HEADER + "creationTime = " + creationTime);
      log.debug(DEBUG_HEADER + "startTime = " + startTime);
      log.debug(DEBUG_HEADER + "endTime = " + endTime);
      log.debug(DEBUG_HEADER + "jobStatusSeq = " + jobStatusSeq);
      log.debug(DEBUG_HEADER + "statusMessage = " + statusMessage);
    }

    String pluginId = null;
    String auKey = null;
    ResultSet resultSet = null;
    Long jobSeq = null;
    PreparedStatement createJob = null;

    if (dbManager.isTypeMysql()) {
      	createJob = dbManager.prepareStatement(conn, INSERT_JOB_MYSQL_QUERY,
      	    Statement.RETURN_GENERATED_KEYS);
    } else {
      createJob = dbManager.prepareStatement(conn, INSERT_JOB_QUERY,
	  Statement.RETURN_GENERATED_KEYS);
    }

    try {
      // skip auto-increment key field #0
      createJob.setLong(1, jobTypeSeq);

      if (description != null) {
	createJob.setString(2, description);
      } else {
	createJob.setNull(2, VARCHAR);
      }

      pluginId = PluginManager.pluginIdFromAuId(auId);
      createJob.setString(3, pluginId);

      auKey = PluginManager.auKeyFromAuId(auId);
      createJob.setString(4, auKey);

      createJob.setLong(5, creationTime);

      if (startTime != null) {
	createJob.setLong(6, startTime.longValue());
      } else {
	createJob.setNull(6, BIGINT);
      }

      if (endTime != null) {
	createJob.setLong(7, endTime.longValue());
      } else {
	createJob.setNull(7, BIGINT);
      }

      createJob.setLong(8, jobStatusSeq);

      if (statusMessage != null) {
	createJob.setString(9, statusMessage);
      } else {
	createJob.setNull(9, VARCHAR);
      }

      dbManager.executeUpdate(createJob);
      resultSet = createJob.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create Job table row for auId " + auId);
	return null;
      }

      jobSeq = resultSet.getLong(1);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Added jobSeq = " + jobSeq);
    } catch (SQLException sqle) {
      String message = "Cannot add job";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_JOB_QUERY + "'.");
      log.error("jobTypeSeq = " + jobTypeSeq);
      log.error("description = " + description);
      log.error("auId = " + auId);
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
      log.error("creationTime = " + creationTime);
      log.error("startTime = " + startTime);
      log.error("endTime = " + endTime);
      log.error("jobStatusSeq = " + jobStatusSeq);
      log.error("statusMessage = " + statusMessage);
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot add job";
      log.error(message, dbe);
      log.error("SQL = '" + INSERT_JOB_QUERY + "'.");
      log.error("jobTypeSeq = " + jobTypeSeq);
      log.error("description = " + description);
      log.error("auId = " + auId);
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
      log.error("creationTime = " + creationTime);
      log.error("startTime = " + startTime);
      log.error("endTime = " + endTime);
      log.error("jobStatusSeq = " + jobStatusSeq);
      log.error("statusMessage = " + statusMessage);
      throw dbe;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(createJob);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);
    return jobSeq;
  }

  /**
   * Provides a list of existing Archival Units.
   * 
   * @param page
   *          An Integer with the index of the page to be returned.
   * @param limit
   *          An Integer with the maximum number of Archival Units to be
   *          returned.
   * @return a List<JobAuStatus> with the existing Archival Units.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  List<JobAuStatus> getAus(Integer page, Integer limit) throws DbException {
    final String DEBUG_HEADER = "getAus(): ";
    if (log.isDebug2()) {
      log.debug(DEBUG_HEADER + "page = " + page);
      log.debug(DEBUG_HEADER + "limit = " + limit);
    }

    List<JobAuStatus> result = null;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Get the Archival Units.
      result = getAus(conn, page, limit);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides a list of existing Archival Units.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param page
   *          An Integer with the index of the page to be returned.
   * @param limit
   *          An Integer with the maximum number of Archival Units to be
   *          returned.
   * @return a List<JobAuStatus> with the existing Archival Units.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private List<JobAuStatus> getAus(Connection conn, Integer page, Integer limit)
      throws DbException {
    final String DEBUG_HEADER = "getAus(): ";
    if (log.isDebug2()) {
      log.debug(DEBUG_HEADER + "page = " + page);
      log.debug(DEBUG_HEADER + "limit = " + limit);
    }

    List<JobAuStatus> aus = new ArrayList<JobAuStatus>();
    String sql = FIND_OFFSET_AUS_QUERY;

    if (dbManager.isTypeDerby()) {
      sql = sql + " rows";
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = " + sql);

    int offset = 0;

    if (page != null && page.intValue() > 0
	&& limit != null && limit.intValue() >= 0) {
      offset = (page - 1) * limit;
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "offset = " + offset);

    PreparedStatement findAus = dbManager.prepareStatement(conn, sql);
    ResultSet resultSet = null;
    JobAuStatus au;

    try {
      // Get the Archival Units.
      findAus.setInt(1, offset);

      if (limit != null && limit.intValue() >= 0) {
	findAus.setMaxRows(limit);
      }

      resultSet = dbManager.executeQuery(findAus);

      Long deleteJobTypeSeq = jobTypeSeqByName.get(JOB_TYPE_DELETE_AU);
      Long putJobTypeSeq = jobTypeSeqByName.get(JOB_TYPE_PUT_AU);
      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER + "deleteJobTypeSeq = " + deleteJobTypeSeq);
	log.debug3(DEBUG_HEADER + "putJobTypeSeq = " + putJobTypeSeq);
      }

      String previousAuId = null;
      Long previousJobTypeSeq = null;

      // Loop through the results.
      while (resultSet.next()) {
	// Get the next Archival Unit.
	au = getAuFromResultSet(resultSet);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Found au " + au);

	String thisAuId = au.getAuId();
	Long thisJobTypeSeq = au.getType();

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "previousAuId = " + previousAuId);
	  log.debug3(DEBUG_HEADER + "previousJobTypeSeq = "
	      + previousJobTypeSeq);
	  log.debug3(DEBUG_HEADER + "thisAuId = " + thisAuId);
	  log.debug3(DEBUG_HEADER + "thisJobTypeSeq = " + thisJobTypeSeq);
	}

	// Check whether this is the same Archival Unit as the previous one.
	if (previousAuId != null && previousAuId.equals(thisAuId)) {
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Matched previous");
	  // Yes: Check whether the previous one was a PUT operation.
	  if (putJobTypeSeq == previousJobTypeSeq) {
	    // Yes: Ignore this one.
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Ignore this one");
	    continue;
	    // No: Check whether the previous one was a DELETE operation while
	    // this one is a PUT operation.
	  } else if (deleteJobTypeSeq == previousJobTypeSeq
	      && putJobTypeSeq == thisJobTypeSeq) {
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Delete previous");
	    // Yes: Replace the previous one with this one.
	    aus.remove(aus.size() - 1);
	  }
	}

	// Remember this Archival Unit to compare it to the next one.
	previousAuId = thisAuId;
	previousJobTypeSeq = thisJobTypeSeq;

	// Add it to the results.
	aus.add(au);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find AUs";
      log.error(message, sqle);
      log.error("SQL = '" + sql + "'.");
      log.error("page = " + page);
      log.error("limit = " + limit);
      log.error("offset = " + offset);
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot find AUs";
      log.error(message, dbe);
      log.error("SQL = '" + sql + "'.");
      log.error("page = " + page);
      log.error("limit = " + limit);
      log.error("offset = " + offset);
      throw dbe;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findAus);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "aus = " + aus);
    return aus;
  }

  /**
   * Populates the data of an Archival Unit with a result set.
   * 
   * @param resultSet
   *          A ResultSet with the source of the data.
   * @return a JobAuStatus with the resulting Archival Unit data.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private JobAuStatus getAuFromResultSet(ResultSet resultSet)
      throws SQLException {
    JobAuStatus au = new JobAuStatus();

    au.setId(String.valueOf(resultSet.getLong(JOB_SEQ_COLUMN)));
    au.setType(resultSet.getLong(JOB_TYPE_SEQ_COLUMN));

    String pluginId = resultSet.getString(PLUGIN_ID_COLUMN);

    if (pluginId != null && pluginId.trim().length() > 0) {
      String auKey = resultSet.getString(AU_KEY_COLUMN);

      if (auKey != null && auKey.trim().length() > 0) {
	au.setAuId(PluginManager.generateAuId(pluginId, auKey));
      }
    }

    return au;
  }

  /**
   * Creates a job to remove the metadata of an Archival Unit.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a JobAuStatus with the created metadata removal job properties.
   * @throws IllegalArgumentException
   *           if the Archival Unit does not exist.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  JobAuStatus createMetadataRemovalJob(String auId)
      throws IllegalArgumentException, DbException {
    final String DEBUG_HEADER = "createMetadataRemovalJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    JobAuStatus result = null;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Create the Archival Unit metadata removal job.
      result = createMetadataRemovalJob(conn, auId);

      DbManager.commitOrRollback(conn, log);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates a job to remove the metadata of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a JobAuStatus with the created metadata removal job properties.
   * @throws IllegalArgumentException
   *           if the Archival Unit does not exist.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private JobAuStatus createMetadataRemovalJob(Connection conn, String auId)
      throws IllegalArgumentException, DbException {
    final String DEBUG_HEADER = "createMetadataRemovalJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    JobAuStatus result = null;

    // Get any existing jobs for the Archival Unit.
    List<JobAuStatus> auJobs = getAuJobs(conn, auId);

    // Loop through all the Archival Unit jobs.
    for (JobAuStatus job : auJobs) {
      Long jobTypeSeq = job.getType();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "jobTypeSeq = " + jobTypeSeq);

      // Check whether it's a job deleting the metadata of the Archival Unit.
      if (jobTypeSeqByName.get(JOB_TYPE_DELETE_AU).equals(jobTypeSeq)) {
	// Yes: Get its status.
	Long jobStatusSeq = (long)job.getStatusCode();

	// Check whether it has not been started.
	if (jobStatusSeqByName.get(JOB_STATUS_CREATED).equals(jobStatusSeq)) {
	  // Yes: Do not create a new job: Reuse the existing one.
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Reusing job = " + job);
	  result = job;
	} else {
	  // No: Try to delete it.
	  boolean deletedDeleteJob =
	      deleteInactiveJob(conn, Long.valueOf(job.getId()));
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "deletedDeleteJob = " + deletedDeleteJob);

	  // Check whether it could not be deleted.
	  if (!deletedDeleteJob) {
	    // Yes: Do not create a new job: Reuse the existing one.
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "Reusing job = " + job);
	    result = job;
	  } else {
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "New job needed");
	  }
	}
      } else if (jobTypeSeqByName.get(JOB_TYPE_PUT_AU).equals(jobTypeSeq)) {
	// Yes: Try to delete it.
	boolean deletedPutJob =
	    deleteInactiveJob(conn, Long.valueOf(job.getId()));
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "deletedPutJob = " + deletedPutJob);
      }
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result = " + result);

    // Check whether no job can be reused.
    if (result == null) {
      // Yes: Create the job.
      Long jobSeq = addJob(conn, jobTypeSeqByName.get(JOB_TYPE_DELETE_AU),
	  "Metadata Removal", auId, new Date().getTime(), null, null,
	  jobStatusSeqByName.get(JOB_STATUS_CREATED),
	  INITIAL_JOB_STATUS_MESSAGE);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobSeq = " + jobSeq);

      result = getJob(conn, jobSeq);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides an Archival Unit job.
   * 
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a JobAuStatus with the created metadata removal job properties.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  JobAuStatus getAuJob(String auId) throws DbException {
    final String DEBUG_HEADER = "getAuJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    JobAuStatus result = null;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Get the Archival Unit job.
      result = getAuJob(conn, auId);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides an Archival Unit job.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a JobAuStatus with the created metadata removal job properties.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  JobAuStatus getAuJob(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "getAuJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    JobAuStatus result = null;

    // Get any existing jobs for the Archival Unit.
    List<JobAuStatus> auJobs = getAuJobs(conn, auId);

    // Loop through all the Archival Unit jobs.
    for (JobAuStatus job : auJobs) {
      Long jobTypeSeq = job.getType();
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "jobTypeSeq = " + jobTypeSeq);

      // Check whether it's a job extracting the metadata of the Archival Unit.
      if (jobTypeSeqByName.get(JOB_TYPE_PUT_AU).equals(jobTypeSeq)) {
	// Yes: Return it.
	result = job;
	break;
	// No: Check whether it's a job deleting the metadata of the Archival
	// Unit.
      } else if (jobTypeSeqByName.get(JOB_TYPE_DELETE_AU).equals(jobTypeSeq)) {
	// Yes: Return it if there is no metadata extraction job.
	result = job;
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides a list of existing jobs.
   * 
   * @param page
   *          An Integer with the index of the page to be returned.
   * @param limit
   *          An Integer with the maximum number of jobs to be returned.
   * @return a List<Job> with the existing jobs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  List<JobAuStatus> getJobs(Integer page, Integer limit) throws DbException {
    final String DEBUG_HEADER = "getJobs(): ";
    if (log.isDebug2()) {
      log.debug(DEBUG_HEADER + "page = " + page);
      log.debug(DEBUG_HEADER + "limit = " + limit);
    }

    List<JobAuStatus> result = null;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Get the jobs.
      result = getJobs(conn, page, limit);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides a list of existing jobs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param page
   *          An Integer with the index of the page to be returned.
   * @param limit
   *          An Integer with the maximum number of jobs to be returned.
   * @return a List<Job> with the existing jobs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private List<JobAuStatus> getJobs(Connection conn, Integer page,
      Integer limit) throws DbException {
    final String DEBUG_HEADER = "getJobs(): ";
    if (log.isDebug2()) {
      log.debug(DEBUG_HEADER + "page = " + page);
      log.debug(DEBUG_HEADER + "limit = " + limit);
    }

    List<JobAuStatus> jobs = new ArrayList<JobAuStatus>();
    String sql = FIND_OFFSET_JOBS_QUERY;

    if (dbManager.isTypeDerby()) {
      sql = sql + " rows";
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = " + sql);

    int offset = 0;

    if (page != null && page.intValue() > 0
	&& limit != null && limit.intValue() >= 0) {
      offset = (page - 1) * limit;
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "offset = " + offset);

    PreparedStatement findJobs = dbManager.prepareStatement(conn, sql);
    ResultSet resultSet = null;
    JobAuStatus job;

    try {
      // Get the jobs.
      findJobs.setInt(1, offset);

      if (limit != null && limit.intValue() >= 0) {
	findJobs.setMaxRows(limit);
      }

      resultSet = dbManager.executeQuery(findJobs);

      // Loop through the results.
      while (resultSet.next()) {
	// Get the next job.
	job = getJobFromResultSet(resultSet);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Found job " + job);

	// Add it to the results.
	jobs.add(job);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find jobs";
      log.error(message, sqle);
      log.error("SQL = '" + sql + "'.");
      log.error("page = " + page);
      log.error("limit = " + limit);
      log.error("offset = " + offset);
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot find jobs";
      log.error(message, dbe);
      log.error("SQL = '" + sql + "'.");
      log.error("page = " + page);
      log.error("limit = " + limit);
      log.error("offset = " + offset);
      throw dbe;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findJobs);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobs = " + jobs);
    return jobs;
  }

  /**
   * Deletes from the database all inactive jobs.
   * 
   * @return an int with the count of jobs deleted.
   * @throws Exception
   *           if there are problems deleting the jobs.
   */
  int deleteAllInactiveJobs() throws Exception {
    final String DEBUG_HEADER = "deleteAllInactiveJobs(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

    int deletedCount = -1;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      deletedCount = deleteInactiveJobs(conn);
      DbManager.commitOrRollback(conn, log);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "deletedCount = " + deletedCount);
    return deletedCount;
  }

  /**
   * Deletes from the database a job given the job identifier.
   * 
   * @param jobSeq
   *          A Long with the job database identifier.
   * @return a JobAuStatus with the details of the deleted job.
   * @throws IllegalArgumentException
   *           if the job does not exist.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  JobAuStatus deleteJob(Long jobSeq)
      throws IllegalArgumentException, DbException {
    final String DEBUG_HEADER = "deleteJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    JobAuStatus job = null;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      job = deleteJob(conn, jobSeq);
      DbManager.commitOrRollback(conn, log);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "job = " + job);
    return job;
  }

  /**
   * Deletes from the database a job given the job identifier.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param jobSeq
   *          A Long with the job database identifier.
   * @return a JobAuStatus with the details of the deleted job.
   * @throws IllegalArgumentException
   *           if the job does not exist.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private JobAuStatus deleteJob(Connection conn, Long jobSeq)
      throws IllegalArgumentException, DbException {
    final String DEBUG_HEADER = "deleteJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    String message = "Cannot find job for jobSeq " + jobSeq + "'";
    JobAuStatus job = null;

    // Try to delete it.
    boolean deleted = false;

    try {
      job = getJob(conn, jobSeq);

      if (job == null) {
	log.error(message);
	throw new IllegalArgumentException(message);
      }

      message = "Cannot delete job for jobSeq " + jobSeq + "'";
      deleted = deleteInactiveJob(conn, jobSeq);
    } catch (DbException dbe) {
      log.error(message, dbe);
      throw dbe;
    }
    
    if (deleted) {
      job.setStatusCode(
	  jobStatusSeqByName.get(JOB_STATUS_DELETED).intValue());
      job.setEndDate(new LocalDate(Long.valueOf(new Date().getTime())));
      job.setStatusMessage("Deleted");
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "job = " + job);
    return job;
  }

  /**
   * Deletes all inactive jobs (not running and not terminating).
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return an int with the count of jobs deleted.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int deleteInactiveJobs(Connection conn) throws DbException {
    final String DEBUG_HEADER = "deleteInactiveJobs(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

    int deletedCount = -1;
    String message = "Cannot delete all inactive jobs";
    PreparedStatement deleteJob =
	dbManager.prepareStatement(conn, DELETE_INACTIVE_JOBS_QUERY);

    try {
      deleteJob.setLong(1, jobStatusSeqByName.get(JOB_STATUS_RUNNING));
      deleteJob.setLong(2, jobStatusSeqByName.get(JOB_STATUS_TERMINATING));

      deletedCount = dbManager.executeUpdate(deleteJob);
    } catch (DbException dbe) {
      log.error(message, dbe);
      log.error("SQL = '" + DELETE_INACTIVE_JOBS_QUERY + "'.");
      throw dbe;
    } catch (SQLException sqle) {
      log.error(message, sqle);
      log.error("SQL = '" + DELETE_INACTIVE_JOBS_QUERY + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(deleteJob);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "deletedCount = " + deletedCount);
    return deletedCount;
  }

  /**
   * Deletes an inactive job (not running and not terminating).
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param jobSeq
   *          A Long with the job identifier.
   * @return a boolean with <code>true</code> if the job was deleted,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private boolean deleteInactiveJob(Connection conn, Long jobSeq)
      throws DbException {
    final String DEBUG_HEADER = "deleteInactiveJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    int deletedCount = -1;
    PreparedStatement deleteJob =
	dbManager.prepareStatement(conn, DELETE_INACTIVE_JOB_QUERY);

    try {
      deleteJob.setLong(1, jobSeq);
      deleteJob.setLong(1, jobStatusSeqByName.get(JOB_STATUS_RUNNING));
      deleteJob.setLong(2, jobStatusSeqByName.get(JOB_STATUS_TERMINATING));

      deletedCount = dbManager.executeUpdate(deleteJob);
    } catch (SQLException sqle) {
      String message = "Cannot delete job";
      log.error(message, sqle);
      log.error("jobSeq = " + jobSeq);
      log.error("SQL = '" + DELETE_INACTIVE_JOB_QUERY + "'.");
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot delete job";
      log.error(message, dbe);
      log.error("jobSeq = " + jobSeq);
      log.error("SQL = '" + DELETE_INACTIVE_JOB_QUERY + "'.");
      throw dbe;
    } finally {
      DbManager.safeCloseStatement(deleteJob);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "result = " + (deletedCount > 0));
    return deletedCount > 0;
  }

  /**
   * Provides the properties of a job.
   * 
   * @param jobId
   *          A String with the job database identifier.
   * @return a JobAuStatus with the details of the removed job.
   * @throws IllegalArgumentException
   *           if the job does not exist.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  JobAuStatus getJob(String jobId)
      throws IllegalArgumentException, DbException {
    final String DEBUG_HEADER = "getJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobId = " + jobId);

    JobAuStatus result = null;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      result = getJob(conn, jobId);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the properties of a job.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param jobId
   *          A String with the job database identifier.
   * @return a JobAuStatus with the details of the removed job.
   * @throws IllegalArgumentException
   *           if the job does not exist.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private JobAuStatus getJob(Connection conn, String jobId)
      throws IllegalArgumentException, DbException {
    final String DEBUG_HEADER = "getJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobId = " + jobId);

    String message = "Cannot find job for jobId " + jobId + "'";
    Long jobSeq = null;
    JobAuStatus job = null;

    try {
      jobSeq = Long.valueOf(jobId);
      job = getJob(conn, jobSeq);
    } catch (NumberFormatException nfe) {
      log.error(message, nfe);
      throw new IllegalArgumentException(message + ": Not numeric jobId");
    }

    if (job == null) {
      String message2 = "No job found for jobId " + jobId + "'";
      log.error(message2);
      throw new IllegalArgumentException(message2);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "job = " + job);
    return job;
  }

  /**
   * Frees any claimed jobs that have not been finished.
   * 
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void freeIncompleteJobs() throws DbException {
    final String DEBUG_HEADER = "freeIncompleteJobs(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      freeIncompleteJobs(conn);
      DbManager.commitOrRollback(conn, log);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Frees any claimed jobs that have not been finished.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void freeIncompleteJobs(Connection conn) throws DbException {
    final String DEBUG_HEADER = "freeIncompleteJobs(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

    int updatedCount = -1;

    PreparedStatement freeIncompleteJobs =
	dbManager.prepareStatement(conn, FREE_INCOMPLETE_JOBS_QUERY);

    try {
      freeIncompleteJobs.setLong(1, jobStatusSeqByName.get(JOB_STATUS_CREATED));
      freeIncompleteJobs.setLong(2,
	  jobStatusSeqByName.get(JOB_STATUS_TERMINATED));

      updatedCount = dbManager.executeUpdate(freeIncompleteJobs);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "updatedCount = " + updatedCount);
    } catch (SQLException sqle) {
      String message = "Cannot free incomplete jobs";
      log.error(message, sqle);
      log.error("SQL = '" + FREE_INCOMPLETE_JOBS_QUERY + "'.");
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot free incomplete jobs";
      log.error(message, dbe);
      log.error("SQL = '" + FREE_INCOMPLETE_JOBS_QUERY + "'.");
      throw dbe;
    } finally {
      DbManager.safeCloseStatement(freeIncompleteJobs);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
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
    final String DEBUG_HEADER = "claimNextJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "owner = " + owner);

    Long jobSeq = null;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      jobSeq = claimNextJob(conn, owner);
      DbManager.commitOrRollback(conn, log);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);
    return jobSeq;
  }

  /**
   * Claims the next job for a task.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param owner
   *          A String with the name of the task claiming its next job.
   * @return a Long with the database identifier of the job that has been
   *         claimed.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long claimNextJob(Connection conn, String owner) throws DbException {
    final String DEBUG_HEADER = "claimNextJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "owner = " + owner);

    Long jobSeq = null;
    boolean claimed = false;

    while (!claimed) {
      jobSeq = findHighestPriorityUnclaimedJob(conn);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobSeq = " + jobSeq);

      if (jobSeq == null) {
	break;
      }

      claimed = claimUnclaimedJob(conn, owner, jobSeq);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "claimed = " + claimed);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);
    return jobSeq;
  }

  /**
   * Provides the unclaimed job with the highest priority.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a Long with the database identifier of the unclaimed job with the
   *         highest priority.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findHighestPriorityUnclaimedJob(Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "findHighestPriorityUnclaimedJob(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

    Long jobSeq = null;

    PreparedStatement findHighestPriorityJob =
	dbManager.prepareStatement(conn, FIND_HIGHEST_PRIORITY_JOBS_QUERY);

    ResultSet resultSet = null;

    try {
      // Request the single top job.
      findHighestPriorityJob.setMaxRows(1);

      resultSet = dbManager.executeQuery(findHighestPriorityJob);

      // Get the results.
      if (resultSet.next()) {
	jobSeq = resultSet.getLong(JOB_SEQ_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobSeq = " + jobSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find the highest priority unclaimed job";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_HIGHEST_PRIORITY_JOBS_QUERY + "'.");
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot find the highest priority unclaimed job";
      log.error(message, dbe);
      log.error("SQL = '" + FIND_HIGHEST_PRIORITY_JOBS_QUERY + "'.");
      throw dbe;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findHighestPriorityJob);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);
    return jobSeq;
  }

  /**
   * Claims an unclaimed job for a task.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param owner
   *          A String with the name of the task claiming its next job.
   * @param jobSeq
   *          A Long with the database identifier of the job being claimed.
   * @return <code>true</code> if the job was actually claimed,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  boolean claimUnclaimedJob(Connection conn, String owner, Long jobSeq)
      throws DbException {
    final String DEBUG_HEADER = "claimUnclaimedJob(): ";
    if (log.isDebug2()) {
      log.debug(DEBUG_HEADER + "owner = " + owner);
      log.debug(DEBUG_HEADER + "jobSeq = " + jobSeq);
    }

    int updatedCount = -1;

    PreparedStatement claimUnclaimedJob =
	dbManager.prepareStatement(conn, CLAIM_UNCLAIMED_JOB_QUERY);

    try {
      claimUnclaimedJob.setString(1, owner);
      claimUnclaimedJob.setLong(2, jobSeq);
      updatedCount = dbManager.executeUpdate(claimUnclaimedJob);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "updatedCount = " + updatedCount);
    } catch (SQLException sqle) {
      String message = "Cannot claim unclaimed job";
      log.error(message, sqle);
      log.error("owner = '" + owner + "'.");
      log.error("jobSeq = " + jobSeq);
      log.error("SQL = '" + CLAIM_UNCLAIMED_JOB_QUERY + "'.");
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot claim unclaimed job";
      log.error(message, dbe);
      log.error("owner = '" + owner + "'.");
      log.error("jobSeq = " + jobSeq);
      log.error("SQL = '" + CLAIM_UNCLAIMED_JOB_QUERY + "'.");
      throw dbe;
    } finally {
      DbManager.safeCloseStatement(claimUnclaimedJob);
    }

    if (log.isDebug2())
      log.debug(DEBUG_HEADER + "updatedCount == 1 = " + (updatedCount == 1));
    return updatedCount == 1;
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
    final String DEBUG_HEADER = "getJobType(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    String result = null;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      result = getJobType(conn, jobSeq);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the type of a job.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param jobSeq
   *          A Long with the job identifier.
   * @return a String with the job type.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private String getJobType(Connection conn, Long jobSeq) throws DbException {
    final String DEBUG_HEADER = "getJobType(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    String jobType = null;

    PreparedStatement findJob =
	dbManager.prepareStatement(conn, FIND_JOB_TYPE_QUERY);

    ResultSet resultSet = null;

    try {
      // Get the job type.
      findJob.setLong(1, jobSeq);
      resultSet = dbManager.executeQuery(findJob);

      if (resultSet.next()) {
	jobType = resultSet.getString(TYPE_NAME_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "jobType " + jobType);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find Archival Unit jobs";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_JOB_TYPE_QUERY + "'.");
      log.error("jobSeq = " + jobSeq);
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot find Archival Unit jobs";
      log.error(message, dbe);
      log.error("SQL = '" + FIND_JOB_TYPE_QUERY + "'.");
      log.error("jobSeq = " + jobSeq);
      throw dbe;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findJob);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobType = " + jobType);
    return jobType;
  }

  /**
   * Marks a job as running.
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
  int markJobAsRunning(Connection conn, Long jobSeq, String statusMessage)
      throws DbException {
    final String DEBUG_HEADER = "markJobAsRunning(): ";
    if (log.isDebug2()) {
      log.debug(DEBUG_HEADER + "jobSeq = " + jobSeq);
      log.debug(DEBUG_HEADER + "statusMessage = " + statusMessage);
    }

    int updatedCount = -1;

    PreparedStatement updateJobStatus =
	dbManager.prepareStatement(conn, MARK_JOB_AS_RUNNING_QUERY);

    try {
      updateJobStatus.setLong(1, new Date().getTime());
      updateJobStatus.setLong(2, jobStatusSeqByName.get(JOB_STATUS_RUNNING));
      updateJobStatus.setString(3, statusMessage);
      updateJobStatus.setLong(4, jobSeq);
      updatedCount = dbManager.executeUpdate(updateJobStatus);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "updatedCount = " + updatedCount);
    } catch (SQLException sqle) {
      String message = "Cannot update job status";
      log.error(message, sqle);
      log.error("jobSeq = " + jobSeq);
      log.error("statusMessage = '" + statusMessage + "'.");
      log.error("SQL = '" + MARK_JOB_AS_RUNNING_QUERY + "'.");
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot update job status";
      log.error(message, dbe);
      log.error("jobSeq = " + jobSeq);
      log.error("statusMessage = '" + statusMessage + "'.");
      log.error("SQL = '" + MARK_JOB_AS_RUNNING_QUERY + "'.");
      throw dbe;
    } finally {
      DbManager.safeCloseStatement(updateJobStatus);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "updatedCount = " + updatedCount);
    return updatedCount;
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
    final String DEBUG_HEADER = "markJobAsFinished(): ";
    if (log.isDebug2()) {
      log.debug(DEBUG_HEADER + "jobSeq = " + jobSeq);
      log.debug(DEBUG_HEADER + "statusName = " + statusName);
      log.debug(DEBUG_HEADER + "statusMessage = " + statusMessage);
    }

    int updatedCount = -1;

    PreparedStatement updateJobStatus =
	dbManager.prepareStatement(conn, MARK_JOB_AS_FINISHED_QUERY);

    try {
      updateJobStatus.setLong(1, new Date().getTime());
      updateJobStatus.setLong(2, jobStatusSeqByName.get(statusName));
      updateJobStatus.setString(3, statusMessage);
      updateJobStatus.setLong(4, jobSeq);
      updatedCount = dbManager.executeUpdate(updateJobStatus);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "updatedCount = " + updatedCount);
    } catch (SQLException sqle) {
      String message = "Cannot mark a job as finished";
      log.error(message, sqle);
      log.error("jobSeq = " + jobSeq);
      log.error("statusName = '" + statusName + "'.");
      log.error("statusMessage = '" + statusMessage + "'.");
      log.error("SQL = '" + MARK_JOB_AS_FINISHED_QUERY + "'.");
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot mark a job as finished";
      log.error(message, dbe);
      log.error("jobSeq = " + jobSeq);
      log.error("statusName = '" + statusName + "'.");
      log.error("statusMessage = '" + statusMessage + "'.");
      log.error("SQL = '" + MARK_JOB_AS_FINISHED_QUERY + "'.");
      throw dbe;
    } finally {
      DbManager.safeCloseStatement(updateJobStatus);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "updatedCount = " + updatedCount);
    return updatedCount;
  }

  /**
   * Marks a job as terminated.
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
  int markJobAsTerminated(Connection conn, Long jobSeq, String statusMessage)
      throws DbException {
    return markJobAsFinished(conn, jobSeq, JOB_STATUS_TERMINATED,
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
    return markJobAsFinished(conn, jobSeq, JOB_STATUS_DONE, statusMessage);
  }

  /**
   * Updates the status of job.
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
  int updateJobStatus(Connection conn, Long jobSeq, String statusName,
      String statusMessage) throws DbException {
    final String DEBUG_HEADER = "updateJobStatus(): ";
    if (log.isDebug2()) {
      log.debug(DEBUG_HEADER + "jobSeq = " + jobSeq);
      log.debug(DEBUG_HEADER + "statusName = " + statusName);
      log.debug(DEBUG_HEADER + "statusMessage = " + statusMessage);
    }

    int updatedCount = -1;

    PreparedStatement updateJobStatus =
	dbManager.prepareStatement(conn, UPDATE_JOB_STATUS_QUERY);

    try {
      updateJobStatus.setString(1, statusName);
      updateJobStatus.setString(2, statusMessage);
      updateJobStatus.setLong(3, jobSeq);
      updatedCount = dbManager.executeUpdate(updateJobStatus);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "updatedCount = " + updatedCount);
    } catch (SQLException sqle) {
      String message = "Cannot update job status";
      log.error(message, sqle);
      log.error("jobSeq = " + jobSeq);
      log.error("statusName = '" + statusName + "'.");
      log.error("statusMessage = '" + statusMessage + "'.");
      log.error("SQL = '" + UPDATE_JOB_STATUS_QUERY + "'.");
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot update job status";
      log.error(message, dbe);
      log.error("jobSeq = " + jobSeq);
      log.error("statusName = '" + statusName + "'.");
      log.error("statusMessage = '" + statusMessage + "'.");
      log.error("SQL = '" + UPDATE_JOB_STATUS_QUERY + "'.");
      throw dbe;
    } finally {
      DbManager.safeCloseStatement(updateJobStatus);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "updatedCount = " + updatedCount);
    return updatedCount;
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
    final String DEBUG_HEADER = "getJobAuId(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "jobSeq = " + jobSeq);

    String auId = null;

    PreparedStatement findJob =
	dbManager.prepareStatement(conn, FIND_JOB_QUERY);

    ResultSet resultSet = null;

    try {
      // Get the job.
      findJob.setLong(1, jobSeq);
      resultSet = dbManager.executeQuery(findJob);

      if (resultSet.next()) {
	String auKey = resultSet.getString(AU_KEY_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

	String pluginId = resultSet.getString(PLUGIN_ID_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);

	auId = PluginManager.generateAuId(pluginId, auKey);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find job AuId";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_JOB_QUERY + "'.");
      log.error("jobSeq = " + jobSeq);
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot find job AuId";
      log.error(message, dbe);
      log.error("SQL = '" + FIND_JOB_QUERY + "'.");
      log.error("jobSeq = " + jobSeq);
      throw dbe;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findJob);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);
    return auId;
  }
}
