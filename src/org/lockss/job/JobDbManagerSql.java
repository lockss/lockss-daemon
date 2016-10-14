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

import static org.lockss.job.SqlConstants.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.lockss.db.DbManagerSql;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * The MeatadataDbManager SQL code executor.
 * 
 * @author Fernando García-Loygorri
 */
public class JobDbManagerSql extends DbManagerSql {
  private static final Logger log = Logger.getLogger(JobDbManagerSql.class);

  // Query to create the table for job types.
  private static final String CREATE_JOB_TYPE_TABLE_QUERY = "create table "
      + JOB_TYPE_TABLE + " ("
      + JOB_TYPE_SEQ_COLUMN + " --BigintSerialPk--,"
      + TYPE_NAME_COLUMN + " varchar(" + MAX_TYPE_NAME_COLUMN + ") not null"
      + ")";

  // Query to create the table for job statuses.
  private static final String CREATE_JOB_STATUS_TABLE_QUERY = "create table "
      + JOB_STATUS_TABLE + " ("
      + JOB_STATUS_SEQ_COLUMN + " --BigintSerialPk--,"
      + STATUS_NAME_COLUMN + " varchar(" + MAX_STATUS_NAME_COLUMN + ") not null"
      + ")";

  // Query to create the table for Archival Unit jobs.
  private static final String CREATE_JOB_TABLE_QUERY = "create table "
      + JOB_TABLE + " ("
      + JOB_SEQ_COLUMN + " --BigintSerialPk--,"
      + JOB_TYPE_SEQ_COLUMN + " bigint not null references "
      + JOB_TYPE_TABLE + " (" + JOB_TYPE_SEQ_COLUMN + ") on delete cascade,"
      + DESCRIPTION_COLUMN + " varchar(" + MAX_DESCRIPTION_COLUMN + "),"
      + PLUGIN_ID_COLUMN + " varchar(" + MAX_PLUGIN_ID_COLUMN + ") not null,"
      + AU_KEY_COLUMN + " varchar(" + MAX_AU_KEY_COLUMN + ") not null,"
      + CREATION_TIME_COLUMN + " bigint not null,"
      + START_TIME_COLUMN + " bigint,"
      + END_TIME_COLUMN + " bigint,"
      + JOB_STATUS_SEQ_COLUMN + " bigint not null references "
      + JOB_STATUS_TABLE + " (" + JOB_STATUS_SEQ_COLUMN + ") on delete cascade,"
      + STATUS_MESSAGE_COLUMN + " varchar(" + MAX_STATUS_MESSAGE_COLUMN + "),"
      + PRIORITY_COLUMN + " bigint not null,"
      + OWNER_COLUMN + " varchar(" + MAX_OWNER_COLUMN + ")"
      + ")";

  // The SQL code used to create the necessary version 1 database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_1_TABLE_CREATE_QUERIES =
    new LinkedHashMap<String, String>() {{
      put(JOB_TYPE_TABLE, CREATE_JOB_TYPE_TABLE_QUERY);
      put(JOB_STATUS_TABLE, CREATE_JOB_STATUS_TABLE_QUERY);
      put(JOB_TABLE, CREATE_JOB_TABLE_QUERY);
    }};

  // SQL statements that create the necessary version 1 indices.
  private static final String[] VERSION_1_INDEX_CREATE_QUERIES = new String[] {
    "create unique index idx1_" + JOB_TABLE + " on " + JOB_TABLE
    + "(" + PRIORITY_COLUMN + ")",
    "create index idx2_" + JOB_TABLE + " on " + JOB_TABLE
    + "(" + PLUGIN_ID_COLUMN + "," + AU_KEY_COLUMN + ")",
    "create index idx3_" + JOB_TABLE + " on " + JOB_TABLE
    + "(" + OWNER_COLUMN + ")",
    "create index idx4_" + JOB_TABLE + " on " + JOB_TABLE
    + "(" + JOB_TYPE_SEQ_COLUMN + ")"
  };

  // SQL statements that create the necessary version 1 indices for MySQL.
  private static final String[] VERSION_1_INDEX_CREATE_MYSQL_QUERIES =
      new String[] {
    "create unique index idx1_" + JOB_TABLE + " on " + JOB_TABLE
    + "(" + PRIORITY_COLUMN + ")",
    "create index idx2_" + JOB_TABLE + " on " + JOB_TABLE
    // TODO: Fix when MySQL is fixed.
    + "(" + PLUGIN_ID_COLUMN + "(255)," + AU_KEY_COLUMN + "(255))",
    "create index idx3_" + JOB_TABLE + " on " + JOB_TABLE
    + "(" + OWNER_COLUMN + ")",
    "create index idx4_" + JOB_TABLE + " on " + JOB_TABLE
    + "(" + JOB_TYPE_SEQ_COLUMN + ")"
  };

  // Query to insert a type of job.
  private static final String INSERT_JOB_TYPE_QUERY = "insert into "
      + JOB_TYPE_TABLE
      + "(" + JOB_TYPE_SEQ_COLUMN
      + "," + TYPE_NAME_COLUMN
      + ") values (default,?)";

  // Query to insert a job status.
  private static final String INSERT_JOB_STATUS_QUERY = "insert into "
      + JOB_STATUS_TABLE
      + "(" + JOB_STATUS_SEQ_COLUMN
      + "," + STATUS_NAME_COLUMN
      + ") values (default,?)";

  /**
   * Constructor.
   * 
   * @param dataSource
   *          A DataSource with the datasource that provides the connection.
   * @param dataSourceClassName
   *          A String with the data source class name.
   * @param dataSourceUser
   *          A String with the data source user name.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @param fetchSize
   *          An int with the SQL statement fetch size.
   */
  JobDbManagerSql(DataSource dataSource, String dataSourceClassName,
      String dataSourceUser, int maxRetryCount, long retryDelay, int fetchSize)
      {
    super(dataSource, dataSourceClassName, dataSourceUser, maxRetryCount,
	retryDelay, fetchSize);
  }

  /**
   * Sets up the database to version 1.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred setting up the database.
   */
  void setUpDatabaseVersion1(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "setUpDatabaseVersion1(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Create the necessary tables if they do not exist.
    createTablesIfMissing(conn, VERSION_1_TABLE_CREATE_QUERIES);

    // Initialize necessary data in new tables.
    addJobType(conn, JOB_TYPE_PUT_AU);
    addJobType(conn, JOB_TYPE_DELETE_AU);

    addJobStatus(conn, JOB_STATUS_CREATED);
    addJobStatus(conn, JOB_STATUS_RUNNING);
    addJobStatus(conn, JOB_STATUS_TERMINATING);
    addJobStatus(conn, JOB_STATUS_TERMINATED);
    addJobStatus(conn, JOB_STATUS_DONE);
    addJobStatus(conn, JOB_STATUS_DELETED);

    // Create the necessary indices.
    if (isTypeMysql()) {
      executeDdlQueries(conn, VERSION_1_INDEX_CREATE_MYSQL_QUERIES);
    } else {
      executeDdlQueries(conn, VERSION_1_INDEX_CREATE_QUERIES);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds a job type to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param typeName
   *          A String with the name of the type to be added.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void addJobType(Connection conn, String typeName)
      throws SQLException {
    final String DEBUG_HEADER = "addJobType(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "typeName = '" + typeName + "'.");

    // Ignore an empty job type.
    if (StringUtil.isNullString(typeName)) {
      return;
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement insertJobType = null;

    try {
      insertJobType = prepareStatement(conn, INSERT_JOB_TYPE_QUERY);
      insertJobType.setString(1, typeName);

      int count = executeUpdate(insertJobType);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot add a job type", sqle);
      log.error("typeName = '" + typeName + "'.");
      log.error("SQL = '" + INSERT_JOB_TYPE_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot add a job type", re);
      log.error("typeName = '" + typeName + "'.");
      log.error("SQL = '" + INSERT_JOB_TYPE_QUERY + "'.");
      throw re;
    } finally {
      safeCloseStatement(insertJobType);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds a job status to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param statusName
   *          A String with the name of the status to be added.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void addJobStatus(Connection conn, String statusName)
      throws SQLException {
    final String DEBUG_HEADER = "addJobStatus(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "statusName = '" + statusName + "'.");

    // Ignore an empty job status.
    if (StringUtil.isNullString(statusName)) {
      return;
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement insertJobStatus = null;

    try {
      insertJobStatus = prepareStatement(conn, INSERT_JOB_STATUS_QUERY);
      insertJobStatus.setString(1, statusName);

      int count = executeUpdate(insertJobStatus);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot add a job status", sqle);
      log.error("statusName = '" + statusName + "'.");
      log.error("SQL = '" + INSERT_JOB_STATUS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot add a job status", re);
      log.error("statusName = '" + statusName + "'.");
      log.error("SQL = '" + INSERT_JOB_STATUS_QUERY + "'.");
      throw re;
    } finally {
      safeCloseStatement(insertJobStatus);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }
}
