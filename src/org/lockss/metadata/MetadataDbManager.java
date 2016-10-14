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
package org.lockss.metadata;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
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
public class MetadataDbManager extends DbManager
  implements ConfigurableManager {
  protected static final Logger log = Logger.getLogger(MetadataDbManager.class);

  // Prefix for the database manager configuration entries.
  private static final String PREFIX =
      Configuration.PREFIX + "metadataDbManager.";

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
  private MetadataDbManagerSql mdDbManagerSql = new MetadataDbManagerSql(null,
      DEFAULT_DATASOURCE_CLASSNAME, DEFAULT_DATASOURCE_USER,
      DEFAULT_MAX_RETRY_COUNT, DEFAULT_RETRY_DELAY, DEFAULT_FETCH_SIZE);

  /**
   * Default constructor.
   */
  public MetadataDbManager() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param skipAsynchronousUpdates A boolean indicating whether to skip the
   * asynchronous updates and just mark them as done.
   */
  public MetadataDbManager(boolean skipAsynchronousUpdates) {
    super(skipAsynchronousUpdates);
  }

  /**
   * Starts the DbManager service.
   */
  @Override
  public void startService() {
    final String DEBUG_HEADER = "startService(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    setDbManagerSql(mdDbManagerSql);
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

    targetDatabaseVersion = 27;
    asynchronousUpdates = new int[] {10, 15, 17, 20, 22};

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the key used by the application to locate this manager.
   * 
   * @return a String with the manager key.
   */
  public static String getManagerKey() {
    return "MetadataDbManager";
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
   * Adds a platform to the database during initialization.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param platformName
   *          A String with the platform name.
   * @return a Long with the identifier of the platform just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long addPlatform(Connection conn, String platformName)
      throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    try {
      return mdDbManagerSql.addPlatform(conn, platformName);
    } catch (SQLException sqle) {
      throw new DbException("Cannot add platform '" + platformName + "'", sqle);
    } catch (RuntimeException re) {
      throw new DbException("Cannot add platform '" + platformName + "'", re);
    }
  }

  /**
   * Provides the URLs of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return a Map<String, String> with the URL/feature pairs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Map<String, String> getMdItemUrls(Connection conn, Long mdItemSeq)
      throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    try {
      return mdDbManagerSql.getMdItemUrls(conn, mdItemSeq);
    } catch (SQLException sqle) {
      throw new DbException("Cannot get URLs for mdItemSeq = " + mdItemSeq,
	  sqle);
    } catch (RuntimeException re) {
      throw new DbException("Cannot get URLs for mdItemSeq = " + mdItemSeq, re);
    }
  }

  /**
   * Updates the active flag of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param isActive
   *          A boolean with the indication of whether the ArchivalUnit is
   *          active.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void updateAuActiveFlag(Connection conn, String auId, boolean isActive)
      throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    try {
      mdDbManagerSql.updateAuActiveFlag(conn, auId, isActive);
    } catch (SQLException sqle) {
      throw new DbException("Cannot update active flag of AU = " + auId + " to "
	  + isActive, sqle);
    } catch (RuntimeException re) {
      throw new DbException("Cannot update active flag of AU = " + auId + " to "
	  + isActive, re);
    }
  }

  /**
   * Provides the SQL code executor.
   * 
   * @return a DbManagerSql with the SQL code executor.
   * @throws DbException
   *           if this object is not ready.
   */
  MetadataDbManagerSql getMetadataDbManagerSql() throws DbException {
    if (!ready) {
      throw new DbException("MetadataDbManager has not been initialized.");
    }

    return getMetadataDbManagerSqlBeforeReady();
  }

  /**
   * Provides the SQL code executor. To be used during initialization.
   * 
   * @return a DbManagerSql with the SQL code executor.
   */
  MetadataDbManagerSql getMetadataDbManagerSqlBeforeReady() {
    return mdDbManagerSql;
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
      mdDbManagerSql.setUpDatabaseVersion1(conn);

      // Update the database to the final version.
      int lastRecordedVersion = updateDatabase(conn, 1, finalVersion);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "lastRecordedVersion = "
	  + lastRecordedVersion);

      // Commit this partial update.
      MetadataDbManagerSql.commitOrRollback(conn, log);

      success = true;
    } catch (SQLException sqle) {
      log.error(sqle.getMessage() + " - DbManager not ready", sqle);
    } catch (DbException dbe) {
      log.error(dbe.getMessage() + " - DbManager not ready", dbe);
    } catch (RuntimeException re) {
      log.error(re.getMessage() + " - DbManager not ready", re);
    } finally {
      MetadataDbManagerSql.safeRollbackAndClose(conn);
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
      mdDbManagerSql.setUpDatabaseVersion1(conn);
    } else if (databaseVersion == 2) {
      mdDbManagerSql.updateDatabaseFrom1To2(conn);
    } else if (databaseVersion == 3) {
      mdDbManagerSql.updateDatabaseFrom2To3(conn);
    } else if (databaseVersion == 4) {
      mdDbManagerSql.updateDatabaseFrom3To4(conn);
    } else if (databaseVersion == 5) {
      mdDbManagerSql.updateDatabaseFrom4To5(conn);
    } else if (databaseVersion == 6) {
      mdDbManagerSql.updateDatabaseFrom5To6(conn);
    } else if (databaseVersion == 7) {
      mdDbManagerSql.updateDatabaseFrom6To7(conn);
    } else if (databaseVersion == 8) {
      mdDbManagerSql.updateDatabaseFrom7To8(conn);
    } else if (databaseVersion == 9) {
      mdDbManagerSql.updateDatabaseFrom8To9(conn);
    } else if (databaseVersion == 10) {
      if (!skipAsynchronousUpdates) {
	mdDbManagerSql.updateDatabaseFrom9To10(conn);
      }
    } else if (databaseVersion == 11) {
      mdDbManagerSql.updateDatabaseFrom10To11(conn);
    } else if (databaseVersion == 12) {
      mdDbManagerSql.updateDatabaseFrom11To12(conn);
    } else if (databaseVersion == 13) {
      mdDbManagerSql.updateDatabaseFrom12To13(conn);
    } else if (databaseVersion == 14) {
      mdDbManagerSql.updateDatabaseFrom13To14(conn);
    } else if (databaseVersion == 15) {
      if (!skipAsynchronousUpdates) {
	mdDbManagerSql.updateDatabaseFrom14To15(conn);
      }
    } else if (databaseVersion == 16) {
      mdDbManagerSql.updateDatabaseFrom15To16(conn);
    } else if (databaseVersion == 17) {
      if (!skipAsynchronousUpdates) {
	mdDbManagerSql.updateDatabaseFrom16To17(conn);
      }
    } else if (databaseVersion == 18) {
      mdDbManagerSql.updateDatabaseFrom17To18(conn);
    } else if (databaseVersion == 19) {
      mdDbManagerSql.updateDatabaseFrom18To19(conn);
    } else if (databaseVersion == 20) {
      if (!skipAsynchronousUpdates) {
	mdDbManagerSql.updateDatabaseFrom19To20(conn);
      }
    } else if (databaseVersion == 21) {
      mdDbManagerSql.updateDatabaseFrom20To21(conn);
    } else if (databaseVersion == 22) {
      if (!skipAsynchronousUpdates) {
	mdDbManagerSql.updateDatabaseFrom21To22(conn);
      }
    } else if (databaseVersion == 23) {
      mdDbManagerSql.updateDatabaseFrom22To23(conn);
    } else if (databaseVersion == 24) {
      mdDbManagerSql.updateDatabaseFrom23To24(conn);
    } else if (databaseVersion == 25) {
      mdDbManagerSql.updateDatabaseFrom24To25(conn);
    } else if (databaseVersion == 26) {
      mdDbManagerSql.updateDatabaseFrom25To26(conn);
    } else if (databaseVersion == 27) {
      mdDbManagerSql.updateDatabaseFrom26To27(conn);
    } else {
      throw new RuntimeException("Non-existent method to update the database "
	  + "to version " + databaseVersion + ".");
    }
  }

  /**
   * Provides the identifier of a provider if existing or after creating it
   * otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param providerLid
   *          A String with the provider LOCKSS identifier.
   * @param providerName
   *          A String with the provider name.
   * @return a Long with the identifier of the provider.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreateProvider(Connection conn, String providerLid,
      String providerName) throws DbException {
    final String DEBUG_HEADER = "findOrCreateProvider(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "providerLid = '" + providerLid + "'");
      log.debug2(DEBUG_HEADER + "providerName = '" + providerName + "'");
    }

    Long providerSeq = null;

    try {
      providerSeq =
	  mdDbManagerSql.findOrCreateProvider(conn, providerLid, providerName);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "providerSeq = " + providerSeq);
    } catch (SQLException sqle) {
      String message = "Cannot find or create provider";
      log.error(message);
      log.error("providerLid = '" + providerLid + "'");
      log.error("providerName = '" + providerName + "'");
      throw new DbException(message, sqle);
    } catch (RuntimeException re) {
      String message = "Cannot find or create provider";
      log.error(message);
      log.error("providerLid = '" + providerLid + "'");
      log.error("providerName = '" + providerName + "'");
      throw new DbException(message, re);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    return providerSeq;
  }

  /**
   * Provides the identifier of a provider.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param providerLid
   *          A String with the provider LOCKSS identifier.
   * @param providerName
   *          A String with the provider name.
   * @return a Long with the identifier of the provider.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findProvider(Connection conn, String providerLid,
      String providerName) throws DbException {
    final String DEBUG_HEADER = "findProvider(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "providerName = '" + providerName + "'");
      log.debug2(DEBUG_HEADER + "providerLid = '" + providerLid + "'");
    }

    Long providerSeq = null;

    try {
      providerSeq =
	  mdDbManagerSql.findProvider(conn, providerLid, providerName);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "providerSeq = " + providerSeq);
    } catch (SQLException sqle) {
      String message = "Cannot find provider";
      log.error(message);
      log.error("providerLid = '" + providerLid + "'");
      log.error("providerName = '" + providerName + "'");
      throw new DbException(message, sqle);
    } catch (RuntimeException re) {
      String message = "Cannot find provider";
      log.error(message);
      log.error("providerLid = '" + providerLid + "'");
      log.error("providerName = '" + providerName + "'");
      throw new DbException(message, re);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    return providerSeq;
  }

  /**
   * Adds to the database a metadata item proprietary identifier.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param proprietaryId
   *          A String with the proprietary identifier of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void addMdItemProprietaryId(Connection conn, Long mdItemSeq,
      String proprietaryId) throws DbException {
    final String DEBUG_HEADER = "addMdItemProprietaryId(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "proprietaryId = " + proprietaryId);
    }

    try {
      mdDbManagerSql.addMdItemProprietaryId(conn, mdItemSeq, proprietaryId);
    } catch (SQLException sqle) {
      String message = "Cannot add proprietary identifier";
      log.error(message);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("proprietaryId = " + proprietaryId);
      throw new DbException(message, sqle);
    } catch (RuntimeException re) {
      String message = "Cannot add proprietary identifier";
      log.error(message);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("proprietaryId = " + proprietaryId);
      throw new DbException(message, re);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the identifier of a publisher if existing or after creating it
   * otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherName
   *          A String with the publisher name.
   * @return a Long with the identifier of the publisher.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findOrCreatePublisher(Connection conn, String publisherName)
      throws DbException {
    final String DEBUG_HEADER = "findOrCreatePublisher(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherName = '" + publisherName + "'");

    Long publisherSeq = null;

    try {
      publisherSeq = mdDbManagerSql.findOrCreatePublisher(conn, publisherName);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    } catch (SQLException sqle) {
      String message = "Cannot find or create publisher";
      log.error(message);
      log.error("publisherName = '" + publisherName + "'");
      throw new DbException(message, sqle);
    } catch (RuntimeException re) {
      String message = "Cannot find or create publisher";
      log.error(message);
      log.error("publisherName = '" + publisherName + "'");
      throw new DbException(message, re);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    return publisherSeq;
  }

  /**
   * Provides the identifier of a publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherName
   *          A String with the publisher name.
   * @return a Long with the identifier of the publisher.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long findPublisher(Connection conn, String publisherName)
      throws DbException {
    final String DEBUG_HEADER = "findPublisher(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherName = '" + publisherName + "'");

    Long publisherSeq = null;

    try {
      publisherSeq = mdDbManagerSql.findPublisher(conn, publisherName);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    } catch (SQLException sqle) {
      String message = "Cannot find publisher";
      log.error(message);
      log.error("publisherName = '" + publisherName + "'");
      throw new DbException(message, sqle);
    } catch (RuntimeException re) {
      String message = "Cannot find publisher";
      log.error(message);
      log.error("publisherName = '" + publisherName + "'");
      throw new DbException(message, re);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    return publisherSeq;
  }

  /**
   * Adds a publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherName
   *          A String with the publisher name.
   * @return a Long with the identifier of the publisher just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public Long addPublisher(Connection conn, String publisherName)
      throws DbException {
    final String DEBUG_HEADER = "addPublisher(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherName = '" + publisherName + "'");

    Long publisherSeq = null;

    try {
      publisherSeq = mdDbManagerSql.addPublisher(conn, publisherName);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    } catch (SQLException sqle) {
      String message = "Cannot add publisher";
      log.error(message);
      log.error("publisherName = '" + publisherName + "'");
      throw new DbException(message, sqle);
    } catch (RuntimeException re) {
      String message = "Cannot add publisher";
      log.error(message);
      log.error("publisherName = '" + publisherName + "'");
      throw new DbException(message, re);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    return publisherSeq;
  }
}
