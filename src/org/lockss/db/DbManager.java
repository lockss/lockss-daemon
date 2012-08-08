/*
 * $Id: DbManager.java,v 1.4 2012-08-08 07:12:07 tlipkis Exp $
 */

/*

 Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Database manager.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
package org.lockss.db;

import java.io.File;
import java.net.InetAddress;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.derby.drda.NetworkServerControl;
import org.apache.derby.jdbc.ClientConnectionPoolDataSource;
import org.apache.derby.jdbc.ClientDataSource;
import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.util.FileUtil;
import org.lockss.util.Logger;

public class DbManager extends BaseLockssDaemonManager {

  private static final Logger log = Logger.getLogger("DbManager");

  // Metadata keys.
  public static final String COLUMN_NAME = "COLUMN_NAME";
  public static final String COLUMN_SIZE = "COLUMN_SIZE";
  public static final String TABLE_NAME = "TABLE_NAME";
  public static final String TYPE_NAME = "TYPE_NAME";

  // Prefix for the database manager configuration entries.
  private static final String PREFIX = Configuration.PREFIX + "dbManager";

  // Prefix for the datasource configuration entries.
  private static final String DATASOURCE_ROOT = PREFIX + ".datasource";

  /**
   * Name of the database datasource class. Defaults to
   * 'org.apache.derby.jdbc.ClientDataSource'. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_CLASSNAME = DATASOURCE_ROOT
      + ".className";
  public static final String DEFAULT_DATASOURCE_CLASSNAME =
      "org.apache.derby.jdbc.EmbeddedDataSource";

  /**
   * Name of the database create. Defaults to 'create'. Changes require daemon
   * restart.
   */
  public static final String PARAM_DATASOURCE_CREATEDATABASE = DATASOURCE_ROOT
      + ".createDatabase";
  public static final String DEFAULT_DATASOURCE_CREATEDATABASE = "create";

  /**
   * Name of the database. Defaults to 'db/DbManager'. Changes require daemon
   * restart.
   */
  public static final String PARAM_DATASOURCE_DATABASENAME = DATASOURCE_ROOT
      + ".databaseName";
  public static final String DEFAULT_DATASOURCE_DATABASENAME = "db/DbManager";

  /**
   * Port number of the database. Defaults to 1527. Changes require daemon
   * restart.
   */
  public static final String PARAM_DATASOURCE_PORTNUMBER = DATASOURCE_ROOT
      + ".portNumber";
  public static final String DEFAULT_DATASOURCE_PORTNUMBER = "1527";

  /**
   * Name of the server. Defaults to 'localhost'. Changes require daemon
   * restart.
   */
  public static final String PARAM_DATASOURCE_SERVERNAME = DATASOURCE_ROOT
      + ".serverName";
  public static final String DEFAULT_DATASOURCE_SERVERNAME = "localhost";

  /**
   * Name of the database user. Defaults to 'LOCKSS'. Changes require daemon
   * restart.
   */
  public static final String PARAM_DATASOURCE_USER = DATASOURCE_ROOT + ".user";
  public static final String DEFAULT_DATASOURCE_USER = "LOCKSS";

  // The database data source.
  private DataSource dataSource = null;

  // The data source user.
  private String datasourceUser = null;

  // An indication of whether this object is ready to be used.
  private boolean ready = false;

  /**
   * Starts the DbManager service.
   */
  @Override
  public void startService() {
    final String DEBUG_HEADER = "startService(): ";
    log.debug(DEBUG_HEADER + "dataSource != null = " + (dataSource != null));

    // Do nothing more if it is already initialized.
    ready = ready && dataSource != null;
    if (ready) {
      return;
    }

    // Get the datasource configuration.
    Configuration dataSourceConfig = getDataSourceConfig();

    // Create the datasource.
    try {
      dataSource = createDataSource(dataSourceConfig);
    } catch (Exception e) {
      log.error("Cannot create the datasource - DbManager not ready", e);
      return;
    }

    // Check whether the datasource properties have been successfully
    // initialized.
    if (initializeDataSourceProperties(dataSourceConfig)) {
      // Yes: Check whether the Derby NetworkServerControl for client
      // connections needs to be started.
      if (dataSource instanceof ClientDataSource) {
	// Yes: Start it.
	try {
	  ready = startNetworkServerControl();
	  log.debug(DEBUG_HEADER + "DbManager is ready.");
	} catch (Exception e) {
	  log.error(
	      "Cannot enable remote access to Derby database - DbManager not ready",
	      e);
	  dataSource = null;
	}
      } else {
	ready = true;
	log.debug(DEBUG_HEADER + "DbManager is ready.");
      }
    } else {
      dataSource = null;
      log.error("Could not initialize the datasource - DbManager not ready.");
    }
  }

  /**
   * Provides an indication of whether this object is ready to be used.
   * 
   * @return <code>true</code> if this object is ready to be used,
   *         <code>false</code> otherwise.
   */
  public boolean isReady() {
    return ready;
  }

  /**
   * Provides a database connection using the datasource.
   * <p />
   * Autocommit is disabled to allow the client code to manage transactions.
   * 
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if this object is not ready or an error occurred.
   */
  public Connection getConnection() throws SQLException {
    if (!ready) {
      throw new SQLException("DbManager has not been initialized.");
    }

    Connection conn = null;

    if (dataSource instanceof javax.sql.ConnectionPoolDataSource) {
    log.debug("Pooled");
      conn =
	  ((javax.sql.ConnectionPoolDataSource) dataSource)
	      .getPooledConnection().getConnection();
    } else {
      log.debug("Not pooled");
      conn = dataSource.getConnection();
    }

    conn.setAutoCommit(false);

    return conn;
  }

  /**
   * Creates a database table if it does not exist.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param tableName
   *          A String with the name of the table to create, if missing.
   * @param tableCreateSql
   *          A String with the SQL code used to create the table, if missing.
   * @return <code>true</code> if the table did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws BatchUpdateException
   *           , SQLException
   */
  // TODO: If the table exists, verify that it matches the table to be created.
  public boolean createTableIfMissing(Connection conn, String tableName,
      String tableCreateSql) throws BatchUpdateException, SQLException {
    final String DEBUG_HEADER = "createTableIfMissing(): ";

    if (!ready) {
      throw new SQLException("DbManager has not been initialized.");
    }

    if (conn == null) {
      throw new NullPointerException("Null connection.");
    }

    // Check whether the table needs to be created.
    if (!tableExists(conn, tableName)) {
      // Yes: Create it.
      log.debug(DEBUG_HEADER + "Creating table '" + tableName + "'...");
      log.debug(DEBUG_HEADER + "tableCreateSql = '" + tableCreateSql + "'.");

      executeBatch(conn, new String[] { tableCreateSql });
      return true;
    } else {
      // No.
      log.debug(DEBUG_HEADER + "Table '" + tableName
	  + "' exists - Not creating it.");
      return false;
    }
  }

  /**
   * Executes a batch of statements.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param stmts
   *          A String[] with the statements to be executed.
   * @throws BatchUpdateException
   *           if a batch update exception occurred.
   * @throws SQLException
   *           if this object is not ready or any other SQL exception occurred.
   */
  public void executeBatch(Connection conn, String[] stmts)
      throws BatchUpdateException, SQLException {
    if (!ready) {
      throw new SQLException("DbManager has not been initialized.");
    }

    if (conn == null) {
      throw new NullPointerException("Null connection.");
    }

    Statement statement = null;

    try {
      statement = conn.createStatement();
      for (String stmt : stmts) {
	statement.addBatch(stmt);
      }
      statement.executeBatch();
    } finally {
      safeCloseStatement(statement);
    }
  }

  /**
   * Writes the named table schema to the log. For debugging purposes only.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param tableName
   *          A String with name of the table to log.
   * @throws SQLException
   *           if this object is not ready or any problem occurred.
   */
  public void logTableSchema(Connection conn, String tableName)
      throws SQLException {
    if (!ready) {
      throw new SQLException("DbManager has not been initialized.");
    }

    if (conn == null) {
      throw new NullPointerException("Null connection.");
    }

    if (!tableExists(conn, tableName)) {
      log.debug("Table '" + tableName + "' does not exist.");
      return;
    }

    // Do nothing more if the current log level is not appropriate.
    if (!log.isDebug()) {
      return;
    }

    String columnName = null;
    String padding = "                               ";
    ResultSet resultSet = null;

    try {
      // Get the table column data.
      resultSet =
	  conn.getMetaData().getColumns(null, datasourceUser,
	      tableName.toUpperCase(), null);

      log.debug("Table Name : " + tableName);
      log.debug("Field" + padding.substring(0, 32 - "Field".length())
	  + "\tsize\tDataType");

      // Loop through each column.
      while (resultSet.next()) {
	// Output the column data.
	StringBuilder sb = new StringBuilder();
	columnName = resultSet.getString(COLUMN_NAME);
	sb.append(columnName);
	sb.append(padding.substring(0, 32 - columnName.length()));
	sb.append("\t");
	sb.append(resultSet.getString(COLUMN_SIZE));
	sb.append(" \t");
	sb.append(resultSet.getString(TYPE_NAME));
	log.debug(sb.toString());
      }
    } finally {
      safeCloseResultSet(resultSet);
    }
  }

  /**
   * Closes a result set without throwing exceptions.
   * 
   * @param resultSet
   *          A ResultSet with the database result set to be closed.
   */
  public void safeCloseResultSet(ResultSet resultSet) {
    if (resultSet != null) {
      try {
	resultSet.close();
      } catch (SQLException sqle) {
	log.error("Cannot close result set", sqle);
      }
    }
  }

  /**
   * Closes a statement without throwing exceptions.
   * 
   * @param statement
   *          A Statement with the database statement to be closed.
   */
  public void safeCloseStatement(Statement statement) {
    if (statement != null) {
      try {
	statement.close();
      } catch (SQLException sqle) {
	log.error("Cannot close statement", sqle);
      }
    }
  }

  /**
   * Closes a connection without throwing exceptions.
   * 
   * @param conn
   *          A Connection with the database connection to be closed.
   */
  public void safeCloseConnection(Connection conn) {
    try {
      if ((conn != null) && !conn.isClosed()) {
        conn.close();
      }
    } catch (SQLException sqle) {
      log.error("Cannot close connection", sqle);
    }
  }

  /**
   * Rolls back and closes a connection without throwing exceptions.
   * 
   * @param conn
   *          A Connection with the database connection to be rolled back and
   *          closed.
   */
  public void safeRollbackAndClose(Connection conn) {
    // Roll back the connection.
    try {
      if ((conn != null) && !conn.isClosed()) {
        conn.rollback();
      }
    } catch (SQLException sqle) {
      log.error("Cannot roll back the connection", sqle);
    }
    // Close it.
    safeCloseConnection(conn);
  }

  /**
   * Provides an indication of whether a table exists.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableName
   *          A String with name of the table to be checked.
   * @return <code>true</code> if the named table exists, <code>false</code>
   *         otherwise.
   * @throws SQLException
   *           if this object is not ready or any problem occurred.
   */
  public boolean tableExists(Connection conn, String tableName)
      throws SQLException {
    if (!ready) {
      throw new SQLException("DbManager has not been initialized.");
    }

    if (conn == null) {
      throw new NullPointerException("Null connection.");
    }

    ResultSet resultSet = null;

    try {
      // Get the database schema table data.
      resultSet =
	  conn.getMetaData().getTables(null, datasourceUser, null, null);

      // Loop through each table.
      while (resultSet.next()) {
	if (tableName.toUpperCase().equals(resultSet.getString(TABLE_NAME))) {
	  // Found the table: No need to check further.
	  return true;
	}
      }
    } finally {
      safeCloseResultSet(resultSet);
    }

    // The table does not exist.
    return false;
  }

  /**
   * Stops the DbManager service.
   */
  @Override
  public void stopService() {
    ready = false;
    dataSource = null;
  }

  /**
   * Provides the datasource configuration.
   * 
   * @return a Configuration with the datasource configuration parameters.
   */
  private Configuration getDataSourceConfig() {
    final String DEBUG_HEADER = "getDataSourceConfig(): ";

    // Get the current configuration.
    ConfigManager cfgMgr = ConfigManager.getConfigManager();
    Configuration currentConfig = cfgMgr.getCurrentConfig();

    // Create the datasource configuration.
    Configuration dataSourceConfig = ConfigManager.newConfiguration();

    // Populate it from the current configuration datasource tree.
    dataSourceConfig.copyFrom(currentConfig.getConfigTree(DATASOURCE_ROOT));

    // Save the default class name, if not configured.
    dataSourceConfig.put("className", currentConfig.get(
	PARAM_DATASOURCE_CLASSNAME, DEFAULT_DATASOURCE_CLASSNAME));

    // Save the creation directive, if not configured.
    dataSourceConfig.put("createDatabase", currentConfig.get(
	PARAM_DATASOURCE_CREATEDATABASE, DEFAULT_DATASOURCE_CREATEDATABASE));

    // Check whether the configured datasource database name does not exist.
    if (dataSourceConfig.get("databaseName") == null) {
      // Yes: Get the data source root directory.
      File datasourceDir =
	cfgMgr.findConfiguredDataDir(PARAM_DATASOURCE_DATABASENAME,
				     DEFAULT_DATASOURCE_DATABASENAME, false);
      // Save the database name.
      dataSourceConfig.put("databaseName", FileUtil
	  .getCanonicalOrAbsolutePath(datasourceDir));
      log.debug(DEBUG_HEADER + "datasourceDatabaseName = '"
	  + dataSourceConfig.get("databaseName") + "'.");
    }

    // Save the port number, if not configured.
    dataSourceConfig.put("portNumber", currentConfig.get(
	PARAM_DATASOURCE_PORTNUMBER, DEFAULT_DATASOURCE_PORTNUMBER));

    // Save the server name, if not configured.
    dataSourceConfig.put("serverName", currentConfig.get(
	PARAM_DATASOURCE_SERVERNAME, DEFAULT_DATASOURCE_SERVERNAME));

    // Save the user name, if not configured.
    dataSourceConfig.put("user",
	currentConfig.get(PARAM_DATASOURCE_USER, DEFAULT_DATASOURCE_USER));
    datasourceUser = dataSourceConfig.get("user");

    return dataSourceConfig;
  }

  /**
   * Creates a datasource using the specified configuration.
   * 
   * @param dataSourceConfig
   *          A Configuration with the configuration parameters to be used.
   * @return <code>true</code> if created, <code>false</code> otherwise.
   */
  private DataSource createDataSource(Configuration dataSourceConfig)
      throws Exception {
    // Get the datasource class name.
    String dataSourceClassName = dataSourceConfig.get("className");
    Class<?> dataSourceClass;

    // Locate the datasource class.
    try {
      dataSourceClass = Class.forName(dataSourceClassName);
    } catch (Throwable t) {
      throw new Exception("Cannot locate datasource class '"
	  + dataSourceClassName + "'", t);
    }

    // Create the datasource.
    try {
      return ((DataSource) dataSourceClass.newInstance());
    } catch (ClassCastException cce) {
      throw new Exception("Class '" + dataSourceClassName
	  + "' is not a DataSource.", cce);
    } catch (Throwable t) {
      throw new Exception("Cannot create instance of datasource class '"
	  + dataSourceClassName + "'", t);
    }
  }

  /**
   * Initializes the properties of the datasource using the specified
   * configuration.
   * 
   * @param dataSourceConfig
   *          A Configuration with the configuration parameters to be used.
   * @return <code>true</code> if successfully initialized, <code>false</code>
   *         otherwise.
   */
  private boolean initializeDataSourceProperties(Configuration dataSourceConfig) {
    final String DEBUG_HEADER = "initializeDataSourceProperties(): ";

    String dataSourceClassName = dataSourceConfig.get("className");
    log.debug(DEBUG_HEADER + "dataSourceClassName = '" + dataSourceClassName
	+ "'.");
    boolean errors = false;
    String value = null;

    // Loop through all the configured datasource properties.
    for (String key : dataSourceConfig.keySet()) {
      log.debug(DEBUG_HEADER + "key = '" + key + "'.");

      // Skip over the class name, as it is not really part of the datasource
      // definition.
      if (!"className".equals(key)) {
	// Get the property value.
	value = dataSourceConfig.get(key);
	log.debug(DEBUG_HEADER + "value = '" + value + "'.");

	// Set the property value in the datasource.
	try {
	  BeanUtils.setProperty(dataSource, key, value);
	} catch (Throwable t) {
	  errors = true;
	  log.error("Cannot set value '" + value + "' for property '" + key
	      + "' for instance of datasource class '" + dataSourceClassName
	      + "' - Instance of datasource class not initialized", t);
	}
      }
    }

    return !errors;
  }

  /**
   * Starts the Derby NetworkServerControl and waits for it to be ready.
   * 
   * @return <code>true</code> if the Derby NetworkServerControl is started and
   *         ready, <code>false</code> otherwise.
   * @throws Exception
   *           if the network server control could not be started.
   */
  private boolean startNetworkServerControl() throws Exception {
    final String DEBUG_HEADER = "startNetworkServerControl(): ";

    ClientDataSource cds = (ClientDataSource) dataSource;
    String serverName = cds.getServerName();
    log.debug(DEBUG_HEADER + "serverName = '" + serverName + "'.");
    int serverPort = cds.getPortNumber();
    log.debug(DEBUG_HEADER + "serverPort = " + serverPort + ".");

    // Start the network server control.
    InetAddress inetAddr = InetAddress.getByName(serverName);
    NetworkServerControl networkServerControl =
	new NetworkServerControl(inetAddr, serverPort);
    networkServerControl.start(null);

    // Wait for the network server control to be ready.
    for (int i = 0; i < 40; i++) { // At most 20 seconds.
      try {
	networkServerControl.ping();
	log.debug(DEBUG_HEADER + "Remote access to Derby database enabled");
	return true;
      } catch (Exception e) {
	// Control is not ready: wait and try again.
	try {
	  Thread.sleep(500); // About 1/2 second.
	} catch (InterruptedException ie) {
	  break;
	}
      }
    }

    log.error("Cannot enable remote access to Derby database");
    return false;
  }
}
