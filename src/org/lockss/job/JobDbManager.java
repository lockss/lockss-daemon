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
import java.sql.SQLException;
import org.lockss.app.ConfigurableManager;
import org.lockss.config.Configuration;
import org.lockss.db.DbManager;
import org.lockss.db.DbException;
import org.lockss.util.Logger;

/**
 * Database manager.
 * 
 * @author Fernando García-Loygorri
 */
public class JobDbManager extends DbManager implements ConfigurableManager {
  protected static final Logger log = Logger.getLogger(JobDbManager.class);

  // Prefix for the database manager configuration entries.
  private static final String PREFIX = Configuration.PREFIX + "jobDbManager.";

  // Prefix for the Derby configuration entries.
  private static final String DERBY_ROOT = PREFIX + "derby";

  /**
   * Derby log append option. Changes require daemon restart.
   */
  public static final String PARAM_DERBY_INFOLOG_APPEND = DERBY_ROOT
      + ".infologAppend";

  /**
   * Derby log query plan option. Changes require daemon restart.
   */
  public static final String PARAM_DERBY_LANGUAGE_LOGQUERYPLAN = DERBY_ROOT
      + ".languageLogqueryplan";

  /**
   * Derby log statement text option. Changes require daemon restart.
   */
  public static final String PARAM_DERBY_LANGUAGE_LOGSTATEMENTTEXT = DERBY_ROOT
      + ".languageLogstatementtext";

  /**
   * Name of the Derby log file path. Changes require daemon restart.
   */
  public static final String PARAM_DERBY_STREAM_ERROR_FILE = DERBY_ROOT
      + ".streamErrorFile";

  /**
   * Name of the Derby log severity level. Changes require daemon restart.
   */
  public static final String PARAM_DERBY_STREAM_ERROR_LOGSEVERITYLEVEL =
      DERBY_ROOT + ".streamErrorLogseveritylevel";

  // Prefix for the datasource configuration entries.
  private static final String DATASOURCE_ROOT = PREFIX + "datasource";

  /**
   * Name of the database datasource class. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_CLASSNAME = DATASOURCE_ROOT
      + ".className";

  /**
   * Name of the database create. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_CREATEDATABASE = DATASOURCE_ROOT
      + ".createDatabase";

  /**
   * Name of the database with the relative path to the DB directory. Changes
   * require daemon restart.
   */
  public static final String PARAM_DATASOURCE_DATABASENAME = DATASOURCE_ROOT
      + ".databaseName";

  /**
   * Port number of the database. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_PORTNUMBER = DATASOURCE_ROOT
      + ".portNumber";

  /**
   * Name of the server. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_SERVERNAME = DATASOURCE_ROOT
      + ".serverName";

  /**
   * Name of the database user. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_USER = DATASOURCE_ROOT + ".user";

  /**
   * Name of the existing database password. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_PASSWORD = DATASOURCE_ROOT
      + ".password";

  /**
   * Set to false to prevent DbManager from running
   */
  public static final String PARAM_DBMANAGER_ENABLED = PREFIX + "enabled";

  /**
   * Maximum number of retries for transient SQL exceptions.
   */
  public static final String PARAM_MAX_RETRY_COUNT = PREFIX + "maxRetryCount";

  /**
   * Delay  between retries for transient SQL exceptions.
   */
  public static final String PARAM_RETRY_DELAY = PREFIX + "retryDelay";

  /**
   * SQL statement fetch size.
   */
  public static final String PARAM_FETCH_SIZE = PREFIX + "fetchSize";

  // The SQL code executor.
  private JobDbManagerSql jobDbManagerSql = new JobDbManagerSql(null,
      DEFAULT_DATASOURCE_CLASSNAME, DEFAULT_DATASOURCE_USER,
      DEFAULT_MAX_RETRY_COUNT, DEFAULT_RETRY_DELAY, DEFAULT_FETCH_SIZE);

  /**
   * Default constructor.
   */
  public JobDbManager() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param skipAsynchronousUpdates A boolean indicating whether to skip the
   * asynchronous updates and just mark them as done.
   */
  public JobDbManager(boolean skipAsynchronousUpdates) {
    super(skipAsynchronousUpdates);
  }

  /**
   * Starts the DbManager service.
   */
  @Override
  public void startService() {
    final String DEBUG_HEADER = "startService(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    setDbManagerSql(jobDbManagerSql);
    super.startService();
  }

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
      // Update the reconfigured parameters.
      maxRetryCount =
	  config.getInt(PARAM_MAX_RETRY_COUNT, DEFAULT_MAX_RETRY_COUNT);
      dbManagerSql.setMaxRetryCount(maxRetryCount);

      retryDelay =
	  config.getTimeInterval(PARAM_RETRY_DELAY, DEFAULT_RETRY_DELAY);
      dbManagerSql.setRetryDelay(retryDelay);

      dbManagerEnabled =
	  config.getBoolean(PARAM_DBMANAGER_ENABLED, DEFAULT_DBMANAGER_ENABLED);

      fetchSize = config.getInt(PARAM_FETCH_SIZE, DEFAULT_FETCH_SIZE);
      dbManagerSql.setFetchSize(fetchSize);
    }

    targetDatabaseVersion = 1;
    asynchronousUpdates = new int[] {};

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the key used by the application to locate this manager.
   * 
   * @return a String with the manager key.
   */
  public static String getManagerKey() {
    return "JobDbManager";
  }

  @Override
  protected String getDataSourceRootName() {
    return DATASOURCE_ROOT;
  }

  @Override
  protected String getDataSourceClassName(Configuration config) {
    return config.get(PARAM_DATASOURCE_CLASSNAME, DEFAULT_DATASOURCE_CLASSNAME);
  }

  @Override
  protected String getDataSourceCreatedDatabase(Configuration config) {
    return config.get(PARAM_DATASOURCE_CREATEDATABASE,
	DEFAULT_DATASOURCE_CREATEDATABASE);
  }

  @Override
  protected String getDataSourcePortNumber(Configuration config) {
    if (isTypePostgresql()) {
      return config.get(PARAM_DATASOURCE_PORTNUMBER,
	  DEFAULT_DATASOURCE_PORTNUMBER_PG);
    } else if (isTypeMysql()) {
      return config.get(PARAM_DATASOURCE_PORTNUMBER,
	  DEFAULT_DATASOURCE_PORTNUMBER_MYSQL);
    }

    return config.get(PARAM_DATASOURCE_PORTNUMBER,
	DEFAULT_DATASOURCE_PORTNUMBER);
  }

  @Override
  protected String getDataSourceServerName(Configuration config) {
    return config.get(PARAM_DATASOURCE_SERVERNAME,
	DEFAULT_DATASOURCE_SERVERNAME);
  }

  @Override
  protected String getDataSourceUser(Configuration config) {
    return config.get(PARAM_DATASOURCE_USER, DEFAULT_DATASOURCE_USER);
  }

  @Override
  protected String getDataSourcePassword(Configuration config) {
    return config.get(PARAM_DATASOURCE_PASSWORD, DEFAULT_DATASOURCE_PASSWORD);
  }

  @Override
  protected String getDataSourceDatabaseName(Configuration config) {
    if (isTypeDerby()) {
      return config.get(PARAM_DATASOURCE_DATABASENAME,
	"db/" + this.getClass().getSimpleName());
    }

    return config.get(PARAM_DATASOURCE_DATABASENAME,
	this.getClass().getSimpleName());
}

  @Override
  protected String getDerbyInfoLogAppend(Configuration config) {
    return config.get(PARAM_DERBY_INFOLOG_APPEND, DEFAULT_DERBY_INFOLOG_APPEND);
  }

  @Override
  protected String getDerbyLanguageLogQueryPlan(Configuration config) {
    return config.get(PARAM_DERBY_LANGUAGE_LOGQUERYPLAN,
	DEFAULT_DERBY_LANGUAGE_LOGQUERYPLAN);
  }

  @Override
  protected String getDerbyLanguageLogStatementText(Configuration config) {
    return config.get(PARAM_DERBY_LANGUAGE_LOGSTATEMENTTEXT,
	DEFAULT_DERBY_LANGUAGE_LOGSTATEMENTTEXT);
  }

  @Override
  protected String getDerbyStreamErrorFile(Configuration config) {
    return config.get(PARAM_DERBY_STREAM_ERROR_FILE,
	DEFAULT_DERBY_STREAM_ERROR_FILE);
  }

  @Override
  protected String getDerbyStreamErrorLogSeverityLevel(Configuration config) {
    return config.get(PARAM_DERBY_STREAM_ERROR_LOGSEVERITYLEVEL,
	DEFAULT_DERBY_STREAM_ERROR_LOGSEVERITYLEVEL);
  }

  /**
   * Provides the SQL code executor.
   * 
   * @return a DbManagerSql with the SQL code executor.
   * @throws DbException
   *           if this object is not ready.
   */
  JobDbManagerSql getJobDbManagerSql() throws DbException {
    if (!ready) {
      throw new DbException("JobDbManager has not been initialized.");
    }

    return getJobDbManagerSqlBeforeReady();
  }

  /**
   * Provides the SQL code executor. To be used during initialization.
   * 
   * @return a DbManagerSql with the SQL code executor.
   */
  JobDbManagerSql getJobDbManagerSqlBeforeReady() {
    return jobDbManagerSql;
  }

  /**
   * Sets the version of the database that is the upgrade target of this daemon.
   * 
   * @param version
   *          An int with the target version of the database.
   */
  void setTargetDatabaseVersion(int version) {
    targetDatabaseVersion = version;
  }

  /**
   * Sets up the database for a given version.
   * 
   * @param finalVersion
   *          An int with the version of the database to be set up.
   * @return <code>true</code> if the database was successfully set up,
   *         <code>false</code> otherwise.
   */
  boolean setUpDatabase(int finalVersion) {
    final String DEBUG_HEADER = "setUpDatabase(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "finalVersion = " + finalVersion);

    // Do nothing to set up a non-existent database.
    if (finalVersion < 1) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "success = true");
      return true;
    }

    boolean success = false;
    Connection conn = null;

    try {
      // Set up the database infrastructure.
      setUpInfrastructure();

      // Get a connection to the database.
      conn = dbManagerSql.getConnection();

      // Set up the database to version 1.
      jobDbManagerSql.setUpDatabaseVersion1(conn);

      // Update the database to the final version.
      int lastRecordedVersion = updateDatabase(conn, 1, finalVersion);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "lastRecordedVersion = "
	  + lastRecordedVersion);

      // Commit this partial update.
      JobDbManagerSql.commitOrRollback(conn, log);

      success = true;
    } catch (SQLException sqle) {
      log.error(sqle.getMessage() + " - DbManager not ready", sqle);
    } catch (DbException dbe) {
      log.error(dbe.getMessage() + " - DbManager not ready", dbe);
    } catch (RuntimeException re) {
      log.error(re.getMessage() + " - DbManager not ready", re);
    } finally {
      JobDbManagerSql.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "success = " + success);
    return success;
  }

  @Override
  protected void updateDatabaseToVersion(Connection conn, int databaseVersion)
      throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseToVersion(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseVersion = " + databaseVersion);

    // Perform the appropriate update for this version.
    if (databaseVersion == 1) {
      jobDbManagerSql.setUpDatabaseVersion1(conn);
    } else {
      throw new RuntimeException("Non-existent method to update the database "
	  + "to version " + databaseVersion + ".");
    }
  }
}
