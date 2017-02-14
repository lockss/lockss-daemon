/*

 Copyright (c) 2014-2017 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.db;

import static org.lockss.db.SqlConstants.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.apache.derby.jdbc.ClientConnectionPoolDataSource;
import org.apache.derby.jdbc.ClientDataSource;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.postgresql.ds.PGPoolingDataSource;
import org.postgresql.ds.PGSimpleDataSource;

/**
 * The generic database manager SQL code executor.
 * 
 * @author Fernando García-Loygorri
 */
public class DbManagerSql {
  private static final Logger log = Logger.getLogger(DbManagerSql.class);

  /**
   * The indicator to be inserted in the database at the end of truncated text
   * values.
   */
  public static final String TRUNCATION_INDICATOR = "\u0019";

  private static final String DATABASE_VERSION_TABLE_SYSTEM = "database";

  // Derby definition of a big integer primary key column with a value
  // automatically generated from a sequence.
  private static final String BIGINT_SERIAL_PK_DERBY =
      "bigint primary key generated always as identity";

  // PostgreSQL definition of a big integer primary key column with a value
  // automatically generated from a sequence.
  private static final String BIGINT_SERIAL_PK_PG = "bigserial primary key";

  // MySQL definition of a big integer primary key column with a value
  // automatically generated from a sequence.
  private static final String BIGINT_SERIAL_PK_MYSQL =
      "bigint auto_increment primary key";

  // Database metadata keys.
  private static final String COLUMN_NAME = "COLUMN_NAME";
  private static final String COLUMN_SIZE = "COLUMN_SIZE";
  private static final String FUNCTION_NAME = "FUNCTION_NAME";
  private static final String TYPE_NAME = "TYPE_NAME";

  // Query to create the table for recording versions.
  protected static final String CREATE_VERSION_TABLE_QUERY = "create table "
      + VERSION_TABLE + " ("
      + SYSTEM_COLUMN + " varchar(" + MAX_SYSTEM_COLUMN + ") not null,"
      + VERSION_COLUMN + " smallint not null"
      + ")";

  // Query to insert the database subsystem version.
  private static final String INSERT_VERSION_QUERY = "insert into "
      + VERSION_TABLE
      + "(" + SYSTEM_COLUMN
      + "," + SUBSYSTEM_COLUMN
      + "," + VERSION_COLUMN
      + ") values (?,?,?)";

  // Query to get the database versions sorted in ascending order.
  private static final String GET_SORTED_DATABASE_VERSIONS_QUERY = "select "
      + VERSION_COLUMN
      + " from " + VERSION_TABLE
      + " where " + SYSTEM_COLUMN + " = '" + DATABASE_VERSION_TABLE_SYSTEM
      + "' and " + SUBSYSTEM_COLUMN + " = ?"
      + " order by " + VERSION_COLUMN + " asc";

  // SQL statement that creates a database in PostgreSQL.
  private static final String CREATE_DATABASE_QUERY_PG =
      "create database \"--databaseName--\" with template template0";

  // SQL statement that finds a database in PostgreSQL.
  private static final String FIND_DATABASE_QUERY_PG = "select datname"
      + " from pg_catalog.pg_database where datname = ?";
  
  // SQL statement that creates a schema in PostgreSQL.
  private static final String CREATE_SCHEMA_QUERY_PG =
      "create schema \"--schemaName--\"";
  
  // SQL statement that finds a schema in PostgreSQL.
  private static final String FIND_SCHEMA_QUERY_PG = "select nspname"
      + " from pg_namespace where nspname = ?";

  // SQL statement that finds a database in MySQL.
  private static final String FIND_DATABASE_QUERY_MYSQL = "select schema_name"
      + " from schemata where schema_name = ?";
  
  // SQL statement that creates a database in MySQL.
  private static final String CREATE_DATABASE_QUERY_MYSQL = "create database "
      + "--databaseName-- character set utf8 collate utf8_general_ci";

  // Query to determine whether a specific database version has been completed.
  private static final String COUNT_DATABASE_VERSION_QUERY = "select "
      + "count(*)  from " + VERSION_TABLE
      + " where " + SYSTEM_COLUMN + " = '" + DATABASE_VERSION_TABLE_SYSTEM
      + "' and " + SUBSYSTEM_COLUMN + " = ?"
      + " and " + VERSION_COLUMN + " = ?";

  // SQL statement that adds the subsystem column to the version table.
  private static final String ADD_SUBSYSTEM_COLUMN = "alter table "
      + VERSION_TABLE
      + " add column " + SUBSYSTEM_COLUMN
      + " varchar(" + MAX_SUBSYSTEM_COLUMN + ") not null "
      + "default 'MetadataDbManager'";

  // SQL statements that create the necessary version 18 indices.
  static final String CREATE_VERSION_INDEX_QUERY = "create unique "
      + "index idx2_" + VERSION_TABLE + " on " + VERSION_TABLE + "("
      + SYSTEM_COLUMN + "," + SUBSYSTEM_COLUMN + "," + VERSION_COLUMN + ")";

  // The database data source.
  protected DataSource dataSource = null;

  // The data source class name.
  protected String dataSourceClassName = null;

  // The data source user.
  protected String dataSourceUser = null;

  // The maximum number of retries to be attempted when encountering transient
  // SQL exceptions.
  protected int maxRetryCount = -1;

  // The interval to wait between consecutive retries when encountering
  // transient SQL exceptions.
  protected long retryDelay = -1;

  // The SQL statement fetch size.
  protected int fetchSize = -1;

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
  protected DbManagerSql(DataSource dataSource, String dataSourceClassName,
      String dataSourceUser, int maxRetryCount, long retryDelay, int fetchSize)
      {
    this.dataSource = dataSource;
    this.dataSourceClassName = dataSourceClassName;
    this.dataSourceUser = dataSourceUser;
    this.maxRetryCount = maxRetryCount;
    this.retryDelay = retryDelay;
    this.fetchSize = fetchSize;
  }

  protected DataSource getDataSource() {
    return dataSource;
  }

  void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  protected String getDataSourceClassName() {
    return dataSourceClassName;
  }

  void setDataSourceClassName(String dataSourceClassName) {
    this.dataSourceClassName = dataSourceClassName;
  }

  protected String getDataSourceUser() {
    return dataSourceUser;
  }

  void setDataSourceUser(String dataSourceUser) {
    this.dataSourceUser = dataSourceUser;
  }

  protected long getRetryDelay() {
    return retryDelay;
  }

  public void setRetryDelay(long retryDelay) {
    this.retryDelay = retryDelay;
  }

  protected int getMaxRetryCount() {
    return maxRetryCount;
  }

  public void setMaxRetryCount(int maxRetryCount) {
    this.maxRetryCount = maxRetryCount;
  }

  protected int getFetchSize() {
    return fetchSize;
  }

  public void setFetchSize(int fetchSize) {
    this.fetchSize = fetchSize;
  }

  /**
   * Closes a result set without throwing exceptions.
   * 
   * @param resultSet
   *          A ResultSet with the database result set to be closed.
   */
  protected static void safeCloseResultSet(ResultSet resultSet) {
    JdbcBridge.safeCloseResultSet(resultSet);
  }

  /**
   * Closes a statement without throwing exceptions.
   * 
   * @param statement
   *          A Statement with the database statement to be closed.
   */
  protected static void safeCloseStatement(Statement statement) {
    JdbcBridge.safeCloseStatement(statement);
  }

  /**
   * Closes a connection without throwing exceptions.
   * 
   * @param conn
   *          A Connection with the database connection to be closed.
   */
  static void safeCloseConnection(Connection conn) {
    JdbcBridge.safeCloseConnection(conn);
  }

  /**
   * Rolls back and closes a connection without throwing exceptions.
   * 
   * @param conn
   *          A Connection with the database connection to be rolled back and
   *          closed.
   */
  public static void safeRollbackAndClose(Connection conn) {
    JdbcBridge.safeRollbackAndClose(conn);
  }

  /**
   * Commits a connection or rolls it back if it's not possible.
   * 
   * @param conn
   *          A connection with the database connection to be committed.
   * @param logger
   *          A Logger used to report errors.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  public static void commitOrRollback(Connection conn, Logger logger)
      throws SQLException {
    JdbcBridge.commitOrRollback(conn, logger);
  }

  /**
   * Rolls back a transaction.
   * 
   * @param conn
   *          A connection with the database connection to be rolled back.
   * @param logger
   *          A Logger used to report errors.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static void rollback(Connection conn, Logger logger) throws SQLException {
    JdbcBridge.rollback(conn, logger);
  }

  /**
   * Provides a version of a text truncated to a maximum length, if necessary,
   * including an indication of the truncation.
   * 
   * @param original
   *          A String with the original text to be truncated, if necessary.
   * @param maxLength
   *          An int with the maximum length of the truncated text to be
   *          provided.
   * @return a String with the original text if it is not longer than the
   *         maximum length allowed or the truncated text including an
   *         indication of the truncation.
   */
  public static String truncateVarchar(String original, int maxLength) {
    if (original.length() <= maxLength) {
      return original;
    }

    return original.substring(0, maxLength - TRUNCATION_INDICATOR.length())
	+ TRUNCATION_INDICATOR;
  }

  /**
   * Provides an indication of whether a text has been truncated.
   * 
   * @param text
   *          A String with the text to be evaluated for truncation.
   * @return <code>true</code> if the text has been truncated,
   *         <code>false</code> otherwise.
   */
  public static boolean isTruncatedVarchar(String text) {
    return text.endsWith(TRUNCATION_INDICATOR);
  }

  /**
   * Provides an indication of whether the Derby database is being used.
   * 
   * @return <code>true</code> if the Derby database is being used,
   *         <code>false</code> otherwise.
   */
  protected boolean isTypeDerby() {
    final String DEBUG_HEADER = "isTypeDerby(): ";

    boolean result = EmbeddedDataSource.class.getCanonicalName()
	.equals(dataSourceClassName)
	|| ClientDataSource.class.getCanonicalName().equals(dataSourceClassName)
	|| EmbeddedConnectionPoolDataSource.class.getCanonicalName()
	.equals(dataSourceClassName)
	|| ClientConnectionPoolDataSource.class.getCanonicalName()
	.equals(dataSourceClassName);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides an indication of whether the PostgreSQL database is being used.
   * 
   * @return <code>true</code> if the PostgreSQL database is being used,
   *         <code>false</code> otherwise.
   */
  protected boolean isTypePostgresql() {
    final String DEBUG_HEADER = "isTypePostgresql(): ";

    boolean result = PGSimpleDataSource.class.getCanonicalName()
	.equals(dataSourceClassName)
	|| PGPoolingDataSource.class.getCanonicalName()
	.equals(dataSourceClassName);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides an indication of whether the MySQL database is being used.
   * 
   * @return <code>true</code> if the MySQL database is being used,
   *         <code>false</code> otherwise.
   */
  protected boolean isTypeMysql() {
    final String DEBUG_HEADER = "isTypeMysql(): ";

    boolean result =
	MysqlDataSource.class.getCanonicalName().equals(dataSourceClassName);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides a database connection using the default datasource, retrying the
   * operation in the default manner in case of transient failures.
   * <p />
   * Autocommit is disabled to allow the client code to manage transactions.
   * 
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred getting the connection.
   */
  public Connection getConnection() throws SQLException {
    return getConnection(dataSource, maxRetryCount, retryDelay, false, true);
  }

  /**
   * Provides a database connection using a passed datasource, retrying the
   * operation in the default manner in case of transient failures.
   * <p />
   * Auto-commit is disabled to allow the client code to manage transactions.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred getting the connection.
   */
  private Connection getConnection(DataSource ds) throws SQLException {
    return getConnection(ds, maxRetryCount, retryDelay, false, true);
  }

  /**
   * Provides a database connection using a passed datasource, retrying the
   * operation in the default manner in case of transient failures.
   * <p />
   * Auto-commit is disabled to allow the client code to manage transactions.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @param logException
   *          A boolean indicating whether any exception received should be
   *          logged.
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred getting the connection.
   */
  Connection getConnection(DataSource ds, boolean logException)
      throws SQLException {
    return getConnection(ds, maxRetryCount, retryDelay, false, logException);
  }

  /**
   * Provides a database connection using a passed datasource, retrying the
   * operation in the default manner in case of transient failures.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @param autoCommit
   *          A boolean indicating the value of the connection auto-commit
   *          property.
   * @param logException
   *          A boolean indicating whether any exception received should be
   *          logged.
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred getting the connection.
   */
  private Connection getConnection(DataSource ds, boolean autoCommit,
      boolean logException) throws SQLException {
    return getConnection(ds, maxRetryCount, retryDelay, autoCommit,
	logException);
  }

  /**
   * Provides a database connection using a passed datasource, retrying the
   * operation with the specified parameters in case of transient failures.
   * <p />
   * Autocommit is disabled to allow the client code to manage transactions.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @param autoCommit
   *          A boolean indicating the value of the connection auto-commit
   *          property.
   * @param logException
   *          A boolean indicating whether any exception received should be
   *          logged.
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  Connection getConnection(DataSource ds, int maxRetryCount, long retryDelay,
      boolean autoCommit, boolean logException) throws SQLException {
    if (ds == null) {
      throw new IllegalArgumentException("Null datasource");
    }

    try {
      return
	  JdbcBridge.getConnection(ds, maxRetryCount, retryDelay, autoCommit);
    } catch (SQLException sqle) {
      // Check whether the client code wants the exception logged.
      if (logException) {
	// Yes: Report the problem.
	log.error("Cannot get a database connection", sqle);
	log.error("maxRetryCount = " + maxRetryCount);
	log.error("retryDelay = " + retryDelay);
	log.error("autoCommit = " + autoCommit);
      }

      throw sqle;
    } catch (RuntimeException re) {
      // Check whether the client code wants the exception logged.
      if (logException) {
	// Yes: Report the problem.
	log.error("Cannot get a database connection", re);
	log.error("maxRetryCount = " + maxRetryCount);
	log.error("retryDelay = " + retryDelay);
	log.error("autoCommit = " + autoCommit);
      }

      throw re;
    }
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
   *           if any problem occurred accessing the database.
   */
  public boolean tableExists(Connection conn, String tableName)
      throws SQLException {
    final String DEBUG_HEADER = "tableExists(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    boolean result = false;
    ResultSet resultSet = null;

    try {
      // Get the database schema table data.
      if (isTypeDerby()) {
	resultSet = JdbcBridge.getStandardTables(conn, null, dataSourceUser,
	    tableName.toUpperCase());
      } else if (isTypePostgresql()) {
	resultSet = JdbcBridge.getStandardTables(conn, null, dataSourceUser,
	    tableName.toLowerCase());
      } else if (isTypeMysql()) {
	resultSet = JdbcBridge.getStandardTables(conn, null, dataSourceUser,
	    tableName);
      }

      // Determine whether the table exists.
      result = resultSet.next();
    } catch (SQLException sqle) {
	log.error("Cannot determine whether table '" + tableName + "' exists",
	    sqle);
	throw sqle;
    } catch (RuntimeException re) {
	log.error("Cannot determine whether table '" + tableName + "' exists",
	    re);
	throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Prepares a statement, retrying the preparation in the default manner in
   * case of transient failures.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @return a PreparedStatement with the prepared statement.
   * @throws SQLException
   *           if any problem occurred preparing the statement.
   */
  protected PreparedStatement prepareStatement(Connection conn, String sql)
      throws SQLException {
    return prepareStatement(conn, sql, maxRetryCount, retryDelay);
  }

  /**
   * Prepares a statement, retrying the preparation with the specified
   * parameters in case of transient failures.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return a PreparedStatement with the prepared statement.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  PreparedStatement prepareStatement(Connection conn, String sql,
      int maxRetryCount, long retryDelay) throws SQLException {
    final String DEBUG_HEADER = "prepareStatement(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "sql = '" + sql + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      // Prepare the statement.
      statement = JdbcBridge.prepareStatement(conn, sql,
	  Statement.NO_GENERATED_KEYS, maxRetryCount, retryDelay, fetchSize);
    } catch (SQLException sqle) {
      log.error("Cannot prepare a statement", sqle);
      log.error("sql = '" + sql + "'");
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot prepare a statement", re);
      log.error("sql = '" + sql + "'");
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw re;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return statement;
  }

  /**
   * Prepares a statement, retrying the preparation in the default manner in
   * case of transient failures and specifying whether to return generated keys.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @param returnGeneratedKeys
   *          An int indicating whether generated keys should be made available
   *          for retrieval.
   * @return a PreparedStatement with the prepared statement.
   * @throws SQLException
   *           if any problem occurred preparing the statement.
   */
  protected PreparedStatement prepareStatement(Connection conn, String sql,
      int returnGeneratedKeys) throws SQLException {
    return prepareStatement(conn, sql, returnGeneratedKeys,
	maxRetryCount, retryDelay);
  }

  /**
   * Prepares a statement, retrying the preparation with the specified
   * parameters in case of transient failures and specifying whether to return
   * generated keys.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @param returnGeneratedKeys
   *          An int indicating whether generated keys should be made available
   *          for retrieval.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return a PreparedStatement with the prepared statement.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  PreparedStatement prepareStatement(Connection conn, String sql,
      int returnGeneratedKeys, int maxRetryCount, long retryDelay)
      throws SQLException {
    final String DEBUG_HEADER = "prepareStatement(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "sql = '" + sql + "'");
      log.debug2(DEBUG_HEADER + "returnGeneratedKeys = " + returnGeneratedKeys);
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      // Prepare the statement.
      statement = JdbcBridge.prepareStatement(conn, sql, returnGeneratedKeys,
	  maxRetryCount, retryDelay, fetchSize);
    } catch (SQLException sqle) {
      log.error("Cannot prepare a statement", sqle);
      log.error("sql = '" + sql + "'");
      log.error("returnGeneratedKeys = " + returnGeneratedKeys);
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot prepare a statement", re);
      log.error("sql = '" + sql + "'");
      log.error("returnGeneratedKeys = " + returnGeneratedKeys);
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw re;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return statement;
  }

  /**
   * Executes a querying prepared statement, retrying the execution in the
   * default manner in case of transient failures.
   * 
   * @param statement
   *          A PreparedStatement with the querying prepared statement to be
   *          executed.
   * @return a ResultSet with the results of the query.
   * @throws SQLException
   *           if any problem occurred executing the query.
   */
  protected ResultSet executeQuery(PreparedStatement statement)
      throws SQLException {
    return executeQuery(statement, maxRetryCount, retryDelay);
  }

  /**
   * Executes a querying prepared statement, retrying the execution with the
   * specified parameters in case of transient failures.
   * 
   * @param statement
   *          A PreparedStatement with the querying prepared statement to be
   *          executed.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return a ResultSet with the results of the query.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  ResultSet executeQuery(PreparedStatement statement, int maxRetryCount,
      long retryDelay) throws SQLException {
    final String DEBUG_HEADER = "executeQuery(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    ResultSet results = null;

    try {
      // Execute the query.
      results = JdbcBridge.executeQuery(statement, maxRetryCount, retryDelay);
    } catch (SQLException sqle) {
      log.error("Cannot execute a query", sqle);
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot execute a query");
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw re;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return results;
  }

  /**
   * Executes an updating prepared statement, retrying the execution in the
   * default manner in case of transient failures.
   * 
   * @param statement
   *          A PreparedStatement with the updating prepared statement to be
   *          executed.
   * @return an int with the number of database rows updated.
   * @throws SQLException
   *           if any problem occurred executing the query.
   */
  protected int executeUpdate(PreparedStatement statement) throws SQLException {
    return executeUpdate(statement, maxRetryCount, retryDelay);
  }

  /**
   * Executes an updating prepared statement, retrying the execution  with the
   * specified parameters in case of transient failures.
   * 
   * @param statement
   *          A PreparedStatement with the updating prepared statement to be
   *          executed.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return an int with the number of database rows updated.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  int executeUpdate(PreparedStatement statement, int maxRetryCount,
      long retryDelay) throws SQLException {
    final String DEBUG_HEADER = "executeUpdate(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    int updatedCount = 0;

    try {
      // Execute the query.
      updatedCount =
	  JdbcBridge.executeUpdate(statement, maxRetryCount, retryDelay);
    } catch (SQLException sqle) {
      log.error("Cannot execute an update", sqle);
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot execute an update", re);
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw re;
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "updatedCount = " + updatedCount);
    return updatedCount;
  }

  /**
   * Provides the highest-numbered database update version from the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return an int with the database update version.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  public int getHighestNumberedDatabaseVersion(Connection conn,
      String subsystem) throws SQLException {
    final String DEBUG_HEADER = "getHighestNumberedDatabaseVersion(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "subsystem = " + subsystem);

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    int version = 0;
    PreparedStatement stmt = null;
    ResultSet resultSet = null;

    try {
      stmt = prepareStatement(conn, GET_SORTED_DATABASE_VERSIONS_QUERY);
      stmt.setString(1, subsystem);
      resultSet = executeQuery(stmt);

      while (resultSet.next()) {
	version = resultSet.getShort(VERSION_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "version = " + version);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the database version", sqle);
      log.error("SQL = '" + GET_SORTED_DATABASE_VERSIONS_QUERY + "'.");
      log.error("subsystem = " + subsystem);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot get the database version", re);
      log.error("SQL = '" + GET_SORTED_DATABASE_VERSIONS_QUERY + "'.");
      log.error("subsystem = " + subsystem);
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(stmt);
    }

    log.debug2(DEBUG_HEADER + "version = " + version);
    return version;
  }

  /**
   * Adds to the database a database subsystem version.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subsystem
   *          A String with database subsystem name.
   * @param version
   *          An int with version to be updated.
   * @return an int with the number of database rows added.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  protected int addDbVersion(Connection conn, String subsystem,
      int version) throws SQLException {
    final String DEBUG_HEADER = "addDbVersion(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subsystem = " + subsystem);
      log.debug2(DEBUG_HEADER + "version = " + version);
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    int addedCount = 0;
    PreparedStatement insertVersion = null;

    try {
      insertVersion = prepareStatement(conn, INSERT_VERSION_QUERY);
      insertVersion.setString(1, DATABASE_VERSION_TABLE_SYSTEM);
      insertVersion.setString(2, subsystem);
      insertVersion.setShort(3, (short)version);

      addedCount = executeUpdate(insertVersion);
    } catch (SQLException sqle) {
      log.error("Cannot add the database version", sqle);
      log.error("SQL = '" + INSERT_VERSION_QUERY + "'.");
      log.error("subsystem = " + subsystem + ".");
      log.error("version = " + version + ".");
      log.error("DATABASE_VERSION_TABLE_SYSTEM = '"
	  + DATABASE_VERSION_TABLE_SYSTEM + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot add the database version", re);
      log.error("SQL = '" + INSERT_VERSION_QUERY + "'.");
      log.error("subsystem = " + subsystem + ".");
      log.error("version = " + version + ".");
      log.error("DATABASE_VERSION_TABLE_SYSTEM = '"
	  + DATABASE_VERSION_TABLE_SYSTEM + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(insertVersion);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "addedCount = " + addedCount);
    return addedCount;
  }

  /**
   * Executes a batch of statements.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param statements
   *          A String[] with the statements to be executed.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  protected void executeBatch(Connection conn, String[] statements)
      throws SQLException {
    final String DEBUG_HEADER = "executeBatch(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "statements = "
	+ StringUtil.toString(statements));

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    try {
      // Execute the batch.
      JdbcBridge.executeBatch(conn, statements);
    } catch (SQLException sqle) {
      log.error("Cannot execute batch", sqle);
      log.error("statements = " + StringUtil.toString(statements));
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot execute batch", re);
      log.error("statements = " + StringUtil.toString(statements));
      throw re;
    }
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
   * @throws SQLException
   *           if any problem occurred creating the table.
   */
  // TODO: If the table exists, verify that it matches the table to be created.
  public boolean createTableIfMissing(Connection conn, String tableName,
      String tableCreateSql) throws SQLException {
    final String DEBUG_HEADER = "createTableIfMissing(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'.");
      log.debug2(DEBUG_HEADER + "tableCreateSql = '" + tableCreateSql + "'.");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Check whether the table needs to be created.
    if (!tableExists(conn, tableName)) {
      // Yes: Create it.
      createTable(conn, tableName, tableCreateSql);
      return true;
    } else {
      // No.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Table '" + tableName
	  + "' exists - Not creating it.");
      return false;
    }
  }

  /**
   * Creates a database table.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param tableName
   *          A String with the name of the table to create, if missing.
   * @param tableCreateSql
   *          A String with the SQL code used to create the table, if missing.
   * @throws SQLException
   *           if any problem occurred creating the table.
   */
  void createTable(Connection conn, String tableName, String tableCreateSql)
      throws SQLException {
    final String DEBUG_HEADER = "createTable(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'.");
      log.debug2(DEBUG_HEADER + "tableCreateSql = '" + tableCreateSql + "'.");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    executeDdlQuery(conn, localizeCreateQuery(tableCreateSql));

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "Table '" + tableName + "' created.");

    logTableSchema(conn, tableName);
  }

  /**
   * Executes a DDL query.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param ddlQuery
   *          A String with the DDL query to be executed.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  protected void executeDdlQuery(Connection conn, String ddlQuery)
      throws SQLException {
    final String DEBUG_HEADER = "executeDdlQuery(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "ddlQuery = '" + ddlQuery + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    try {
      JdbcBridge.executeDdlQuery(conn, ddlQuery, maxRetryCount, retryDelay,
	  fetchSize);
    } catch (SQLException sqle) {
      log.error("Cannot execute a DDL query", sqle);
      log.error("ddlQuery = '" + ddlQuery + "'");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot execute a DDL query", re);
      log.error("ddlQuery = '" + ddlQuery + "'");
      throw re;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides a version of a table creation query localized for the database
   * being used.
   * 
   * @param query
   *          A String with the generic table creation query.
   * 
   * @return a String with the localized table creation query.
   */
  private String localizeCreateQuery(String query) {
    final String DEBUG_HEADER = "localizeCreateQuery(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "query = '" + query + "'");

    String result = query;

    // Check whether the Derby database is being used.
    if (isTypeDerby()) {
      // Yes.
      result = localizeCreateQueryForDerby(query);
      // No: Check whether the PostgreSQL database is being used.
    } else if (isTypePostgresql()) {
      result = localizeCreateQueryForPostgresql(query);
    } else if (isTypeMysql()) {
      result = localizeCreateQueryForMysql(query);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides a version of a table creation query localized for Derby.
   * 
   * @param query
   *          A String with the generic table creation query.
   * 
   * @return a String with the localized table creation query.
   */
  private String localizeCreateQueryForDerby(String query) {
    return StringUtil.replaceString(query, "--BigintSerialPk--",
	BIGINT_SERIAL_PK_DERBY);
  }

  /**
   * Provides a version of a table creation query localized for PostgreSQL.
   * 
   * @param query
   *          A String with the generic table creation query.
   * 
   * @return a String with the localized table creation query.
   */
  private String localizeCreateQueryForPostgresql(String query) {
    return StringUtil.replaceString(query, "--BigintSerialPk--",
	BIGINT_SERIAL_PK_PG);
  }

  /**
   * Provides a version of a table creation query localized for MySQL.
   * 
   * @param query
   *          A String with the generic table creation query.
   * 
   * @return a String with the localized table creation query.
   */
  private String localizeCreateQueryForMysql(String query) {
    return StringUtil.replaceString(query + " ENGINE = INNODB",
	"--BigintSerialPk--", BIGINT_SERIAL_PK_MYSQL);
  }

  /**
   * Writes the named table schema to the log. To be used for debugging purposes
   * only.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param tableName
   *          A String with name of the table to log.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  public void logTableSchema(Connection conn, String tableName)
      throws SQLException {
    final String DEBUG_HEADER = "logTableSchema(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'");

    // Do nothing more if the current log level is not appropriate.
    if (!log.isDebug()) {
      return;
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Report a non-existent table.
    if (!tableExists(conn, tableName)) {
      log.debug("Table '" + tableName + "' does not exist.");
      return;
    }

    String columnName = null;
    String padding = "                               ";
    ResultSet resultSet = null;

    try {
      // Get the table column data.
      if (isTypeDerby()) {
	resultSet = JdbcBridge.getColumns(conn, null, dataSourceUser,
	    tableName.toUpperCase(), null);
      } else if (isTypePostgresql()) {
	resultSet = JdbcBridge.getColumns(conn, null, dataSourceUser,
	    tableName.toLowerCase(), null);
      } else if (isTypeMysql()) {
	resultSet = JdbcBridge.getColumns(conn, null, dataSourceUser,
	    tableName, null);
      }
      
      log.debug("Table Name : " + tableName);
      log.debug("Column" + padding.substring(0, 32 - "Column".length())
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
    } catch (SQLException sqle) {
      log.error("Cannot log table schema", sqle);
      log.error("tableName = '" + tableName + "'");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot log table schema", re);
      log.error("tableName = '" + tableName + "'");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates a PostgreSQL database if it does not exist.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @param databaseName
   *          A String with the name of the database to create, if missing.
   * @return <code>true</code> if the database did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if the database creation process failed.
   */
  boolean createPostgreSqlDbIfMissing(DataSource ds, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "createPostgreSqlDbIfMissing(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    boolean result = false;

    // Connect to the template database. PostgreSQL does not allow a database
    // to be created within a transaction.
    Connection conn = getConnection(ds, true, true);

    // Check whether the database does not exist.
    if (!postgresqlDbExists(conn, databaseName)) {
      // Yes: Create it.
      createPostgresqlDb(conn, databaseName);

      result = true;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Determines whether a named PostgreSQL database exists.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param databaseName
   *          A String with the name of the database.
   * @return <code>true</code> if the database exists, <code>false</code>
   *         otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private boolean postgresqlDbExists(Connection conn, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "postgresqlDbExists(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    boolean result = false;
    PreparedStatement findDb = null;
    ResultSet resultSet = null;

    try {
      findDb = prepareStatement(conn, FIND_DATABASE_QUERY_PG);
      findDb.setString(1, databaseName);

      resultSet = executeQuery(findDb);
      result = resultSet.next();
    } catch (SQLException sqle) {
      log.error("Cannot find database", sqle);
      log.error("databaseName = '" + databaseName + "'.");
      log.error("SQL = '" + FIND_DATABASE_QUERY_PG + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find database", re);
      log.error("databaseName = '" + databaseName + "'.");
      log.error("SQL = '" + FIND_DATABASE_QUERY_PG + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(findDb);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates a PostgreSQL database.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param databaseName
   *          A String with the name of the database to create.
   * @throws SQLException
   *           if the database creation process failed.
   */
  private void createPostgresqlDb(Connection conn, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "createPostgresqlDb(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    String sql = StringUtil.replaceString(CREATE_DATABASE_QUERY_PG,
					  "--databaseName--", databaseName);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = '" + sql + "'");

    executeDdlQuery(conn, sql);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Turns on user authentication and authorization on a Derby database.
   * 
   * @param user
   *          A String with the user name.
   * @return <code>true</code> if the the database needs to be shut down to make
   *         static properties take effect, <code>false</code> otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  boolean setUpDerbyAuthentication(String user) throws SQLException {
    final String DEBUG_HEADER = "setUpDerbyAuthentication(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "user = '" + user + "'");

    // Start with an unmodified database.
    boolean requiresCommit = false;

    Statement statement = null;

    // Get a connection to the database.
    Connection conn = getConnection();

    try {
      // Create a statement for authentication queries.
      statement = JdbcBridge.createStatement(conn);

      // Get the indication of whether the database requires authentication.
      String requiresAuthentication = getDerbyDatabaseProperty(statement,
	  "derby.connection.requireAuthentication", "false");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "requiresAuthentication = '"
	  + requiresAuthentication + "'");

      // Check whether it does not require authentication already.
      if ("false".equals(requiresAuthentication.trim().toLowerCase())) {
	// Yes: Change it to require authentication.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.connection.requireAuthentication', 'true')");

	// Get the indication of whether the database requires authentication.
	requiresAuthentication = getDerbyDatabaseProperty(statement,
	    "derby.connection.requireAuthentication", "false");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "requiresAuthentication = '" + requiresAuthentication + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database authentication provider.
      String authenticationProvider = getDerbyDatabaseProperty(statement,
	  "derby.authentication.provider", "BUILTIN");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "authenticationProvider = '"
	  + authenticationProvider + "'");

      String customAuthenticatorClassName =
	  DerbyUserAuthenticator.class.getCanonicalName();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "customAuthenticatorClassName = '" + customAuthenticatorClassName
	  + "'");

      // Check whether the database does not use the LOCKSS custom Derby
      // authentication provider already.
      if (!customAuthenticatorClassName.equals(authenticationProvider.trim())) {
	// Yes: Change it to use the LOCKSS custom Derby authentication
	// provider.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.authentication.provider', '"
	    + customAuthenticatorClassName + "')");

	// Get the database authentication provider.
	authenticationProvider = getDerbyDatabaseProperty(statement,
	    "derby.authentication.provider", "BUILTIN");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "authenticationProvider = '" + authenticationProvider + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database default connection mode.
      String defaultConnectionMode = getDerbyDatabaseProperty(statement,
	  "derby.database.defaultConnectionMode", "fullAccess");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "defaultConnectionMode = '"
	  + defaultConnectionMode + "'");

      // Check whether the database does not prevent unauthenticated access
      // already.
      if (!"noAccess".equals(defaultConnectionMode.trim())) {
	// Yes: Change it to prevent unauthenticated access.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.defaultConnectionMode', 'noAccess')");

	// Get the database default connection mode.
	defaultConnectionMode = getDerbyDatabaseProperty(statement,
	    "derby.database.defaultConnectionMode", "fullAccess");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "defaultConnectionMode = '" + defaultConnectionMode + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the names of the database full access users.
      String fullAccessUsers = getDerbyDatabaseProperty(statement,
	      "derby.database.fullAccessUsers", "");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullAccessUsers = '"
	  + fullAccessUsers + "'");

      // Check whether the user is not in the list of full access users already.
      if (fullAccessUsers.indexOf(user) < 0) {
	// Yes: Change it to allow the configured user full access.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.fullAccessUsers', '" + user + "')");

	// Get the names of the database full access users.
	fullAccessUsers = getDerbyDatabaseProperty(statement,
	    "derby.database.fullAccessUsers", "");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullAccessUsers = '"
	    + fullAccessUsers + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database read-only access users.
      String readOnlyAccessUsers = getDerbyDatabaseProperty(statement,
	  "derby.database.readOnlyAccessUsers", "");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "readOnlyAccessUsers = '"
	  + readOnlyAccessUsers + "'");

      // Check whether changes to the database properties have been made.
      if (requiresCommit) {
	// Yes: Allow override using system properties.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.propertiesOnly', 'false')");
      }
    } catch (SQLException sqle) {
      // Prevent partial changes to be committed.
      requiresCommit = false;

      log.error("Cannot set database property", sqle);
      log.error("user = '" + user + "'");
      throw sqle;
    } catch (RuntimeException re) {
      // Prevent partial changes to be committed.
      requiresCommit = false;

      log.error("Cannot set database property", re);
      log.error("user = '" + user + "'");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);

      // Check whether we need to commit the changes made to the database.
      if (requiresCommit) {
	JdbcBridge.commitOrRollback(conn, log);
      }

      JdbcBridge.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return requiresCommit;
  }

  /**
   * Provides a Derby database property.
   * 
   * @param statement
   *          A Statement to query the database.
   * @param propertyName
   *          A String with the name of the requested property.
   * @param defaultValue
   *          A String with the default value of the requested property.
   * @return a String with the value of the database property.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private String getDerbyDatabaseProperty(Statement statement,
      String propertyName, String defaultValue) throws SQLException {
    final String DEBUG_HEADER = "getDerbyDatabaseProperty(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "propertyName = '" + propertyName + "'");
      log.debug2(DEBUG_HEADER + "defaultValue = '" + defaultValue + "'");
    }

    if (statement == null) {
      throw new IllegalArgumentException("Null statement.");
    }

    ResultSet rs = null;
    String propertyValue = null;

    try {
      // Get the property.
      rs = statement
	  .executeQuery("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('"
	      + propertyName + "')");
      rs.next();
      propertyValue = rs.getString(1);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "propertyValue = '" + propertyValue + "'");
    } catch (SQLException sqle) {
      log.error("Cannot get database property with name '" + propertyName
	  + "'.", sqle);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot get database property with name '" + propertyName
	  + "'.", re);
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(rs);
    }

    // Return the default, if necessary.
    if (propertyValue == null) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Using defaultValue = '"
	  + defaultValue + "'");
      propertyValue = defaultValue;
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "propertyValue = '" + propertyValue + "'");
    return propertyValue;
  }

  /**
   * Turns off user authentication and authorization on a Derby database.
   * 
   * @return <code>true</code> if the the database needs to be shut down to make
   *         static properties take effect, <code>false</code> otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  boolean removeDerbyAuthentication() throws SQLException {
    final String DEBUG_HEADER = "removeDerbyAuthentication(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Start with an unmodified database.
    boolean requiresCommit = false;

    Statement statement = null;

    // Get a connection to the database.
    Connection conn = getConnection();

    try {
      // Create a statement for authentication queries.
      statement = JdbcBridge.createStatement(conn);

      // Get the indication of whether the database requires authentication.
      String requiresAuthentication = getDerbyDatabaseProperty(statement,
	  "derby.connection.requireAuthentication", "false");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "requiresAuthentication = '"
	  + requiresAuthentication + "'");

      // Check whether it does require authentication.
      if ("true".equals(requiresAuthentication.trim().toLowerCase())) {
	// Yes: Change it to not require authentication.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.connection.requireAuthentication', 'false')");

	// Get the indication of whether the database requires authentication.
	requiresAuthentication = getDerbyDatabaseProperty(statement,
	    "derby.connection.requireAuthentication", "false");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "requiresAuthentication = '" + requiresAuthentication + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database authentication provider.
      String authenticationProvider = getDerbyDatabaseProperty(statement,
	  "derby.authentication.provider", "BUILTIN");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "authenticationProvider = '"
	  + authenticationProvider + "'");

      // Check whether it does not use the built-in Derby authentication
      // provider already.
      if (!"BUILTIN".equals(authenticationProvider.trim().toUpperCase())) {
	// Yes: Change it to use the built-in Derby authentication provider.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.authentication.provider', 'BUILTIN')");

	// Get the database authentication provider.
	authenticationProvider = getDerbyDatabaseProperty(statement,
	    "derby.authentication.provider", "BUILTIN");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "authenticationProvider = '" + authenticationProvider + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database default connection mode.
      String defaultConnectionMode = getDerbyDatabaseProperty(statement,
	  "derby.database.defaultConnectionMode", "fullAccess");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "defaultConnectionMode = '"
	  + defaultConnectionMode + "'");

      // Check whether it does not allow full unauthenticated access already.
      if (!"fullAccess".equals(defaultConnectionMode.trim())) {
	// Yes: Change it to allow full unauthenticated access.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.defaultConnectionMode', 'fullAccess')");

	// Get the default connection mode.
	defaultConnectionMode = getDerbyDatabaseProperty(statement,
	    "derby.database.defaultConnectionMode", "fullAccess");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "defaultConnectionMode = '" + defaultConnectionMode + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Check whether changes to the database properties have been made.
      if (requiresCommit) {
	// Yes: Allow override using system properties.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.propertiesOnly', 'false')");
      }
    } catch (SQLException sqle) {
      // Prevent partial changes to be committed.
      requiresCommit = false;

      log.error("Cannot set database property", sqle);
      throw sqle;
    } catch (RuntimeException re) {
      // Prevent partial changes to be committed.
      requiresCommit = false;

      log.error("Cannot set database property", re);
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);

      // Check whether we need to commit the changes made to the database.
      if (requiresCommit) {
	JdbcBridge.commitOrRollback(conn, log);
      }

      JdbcBridge.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return requiresCommit;
  }

  /**
   * Creates a PostgreSQL database schema if it does not exist.
   * 
   * @param schemaName
   *          A String with the name of the schema to create, if missing.
   * @param ds
   *          A DataSource with the data source to be used to get a connection.
   * @return <code>true</code> if the schema did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if the schema creation process failed.
   */
  boolean createPostgresqlSchemaIfMissing(String schemaName, DataSource ds)
      throws SQLException {
    final String DEBUG_HEADER = "createPostgresqlSchemaIfMissing(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "schemaName = '" + schemaName + "'");

    boolean result = false;

    // Get a database connection.
    Connection conn = getConnection(ds);

    try {
      // Check whether the schema does not exist.
      if (!postgresqlSchemaExists(conn, schemaName)) {
	// Yes: Create it.
	createPostgresqlSchema(conn, schemaName);

	// Finish the transaction.
	JdbcBridge.commitOrRollback(conn, log);
	result = true;
      }
    } finally {
      JdbcBridge.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Determines whether a named PostgreSQL schema exists.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param schemaName
   *          A String with the name of the schema.
   * @return <code>true</code> if the schema exists, <code>false</code>
   *         otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private boolean postgresqlSchemaExists(Connection conn, String schemaName)
      throws SQLException {
    final String DEBUG_HEADER = "postgresqlSchemaExists(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "schemaName = '" + schemaName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    boolean result = false;
    PreparedStatement findSchema = null;
    ResultSet resultSet = null;

    try {
      findSchema = prepareStatement(conn, FIND_SCHEMA_QUERY_PG);
      findSchema.setString(1, schemaName);

      resultSet = executeQuery(findSchema);
      result = resultSet.next();
    } catch (SQLException sqle) {
      log.error("Cannot find schema", sqle);
      log.error("schemaName = '" + schemaName + "'.");
      log.error("SQL = '" + FIND_SCHEMA_QUERY_PG + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find schema", re);
      log.error("schemaName = '" + schemaName + "'.");
      log.error("SQL = '" + FIND_SCHEMA_QUERY_PG + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(findSchema);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates a PostgreSQL schema.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param schemaName
   *          A String with the name of the schema to create.
   * @throws SQLException
   *           if the schema creation process failed.
   */
  private void createPostgresqlSchema(Connection conn, String schemaName)
      throws SQLException {
    final String DEBUG_HEADER = "createPostgresqlSchema(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "schemaName = '" + schemaName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    String sql = StringUtil.replaceString(CREATE_SCHEMA_QUERY_PG,
					  "--schemaName--", schemaName);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = '" + sql + "'");

    executeDdlQuery(conn, sql);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates a MySQl database if it does not exist.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @param databaseName
   *          A String with the name of the database to create, if missing.
   * @return <code>true</code> if the database did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if the database creation process failed.
   */
  boolean createMySqlDbIfMissing(DataSource ds, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "createMySqlDbIfMissing(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    boolean result = false;

    // Connect to the standard connectable MySql database.
    Connection conn = getConnection(ds);

    // Check whether the database does not exist.
    if (!mysqlDbExists(conn, databaseName)) {
      // Yes: Create it.
      createMysqlDb(conn, databaseName);
      result = true;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Determines whether a named MySQL database exists.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param databaseName
   *          A String with the name of the database.
   * @return <code>true</code> if the database exists, <code>false</code>
   *         otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private boolean mysqlDbExists(Connection conn, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "mysqlDbExists(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    boolean result = false;
    PreparedStatement findDb = null;
    ResultSet resultSet = null;

    try {
      findDb = prepareStatement(conn, FIND_DATABASE_QUERY_MYSQL);
      findDb.setString(1, databaseName);

      resultSet = executeQuery(findDb);
      result = resultSet.next();
    } catch (SQLException sqle) {
      log.error("Cannot find database", sqle);
      log.error("databaseName = '" + databaseName + "'.");
      log.error("SQL = '" + FIND_DATABASE_QUERY_MYSQL + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find database", re);
      log.error("databaseName = '" + databaseName + "'.");
      log.error("SQL = '" + FIND_DATABASE_QUERY_MYSQL + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(findDb);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates a MySQL database.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param databaseName
   *          A String with the name of the database to create.
   * @throws SQLException
   *           if the database creation process failed.
   */
  private void createMysqlDb(Connection conn, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "createMysqlDb(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    String sql = StringUtil.replaceString(CREATE_DATABASE_QUERY_MYSQL,
	  "--databaseName--", databaseName);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = '" + sql + "'");

    executeDdlQuery(conn, sql);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the query used to drop a table.
   * 
   * @param tableName
   *          A string with the name of the table to be dropped.
   * @return a String with the query used to drop the table.
   */
  protected static String dropTableQuery(String tableName) {
    return "drop table " + tableName;
  }

  /**
   * Creates the necessary database tables if they do not exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableMap
   *          A Map<String, String> with the creation queries indexed by table
   *          name.
   * @throws SQLException
   *           if any problem occurred creating the tables.
   */
  protected void createTablesIfMissing(Connection conn,
      Map<String, String> tableMap) throws SQLException {
    final String DEBUG_HEADER = "createTablesIfMissing(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "tableMap = " + tableMap);

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Loop through all the table names.
    for (String tableName : tableMap.keySet()) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "tableName = '" + tableName + "'.");

      // Create the table if it does not exist.
      createTableIfMissing(conn, tableName,
	  localizeCreateQuery(tableMap.get(tableName)));
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Done with tableName '" + tableName + "'.");
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes database tables if they exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableMap
   *          A Map<String, String> with the removal queries indexed by table
   *          name.
   * @throws SQLException
   *           if any problem occurred removing the tables.
   */
  protected void removeTablesIfPresent(Connection conn,
      Map<String, String> tableMap) throws SQLException {
    final String DEBUG_HEADER = "removeTablesIfPresent(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "tableMap = " + tableMap);

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

      // Loop through all the table names.
    for (String tableName : tableMap.keySet()) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "tableName = '" + tableName + "'.");

      // Remove the table if it does exist.
      removeTableIfPresent(conn, tableName, tableMap.get(tableName));
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Done with tableName '" + tableName + "'.");
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Deletes a database table if it does exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableName
   *          A String with the name of the table to delete, if present.
   * @param tableDropSql
   *          A String with the SQL code used to drop the table, if present.
   * @return <code>true</code> if the table did exist and it was removed,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if any problem occurred removing the table.
   */
  private boolean removeTableIfPresent(Connection conn, String tableName,
      String tableDropSql) throws SQLException {
    final String DEBUG_HEADER = "removeTableIfPresent(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'.");
      log.debug2(DEBUG_HEADER + "tableDropSql = '" + tableDropSql + "'.");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Check whether the table needs to be removed.
    if (tableExists(conn, tableName)) {
      // Yes: Delete it.
      executeBatch(conn, new String[] { tableDropSql });
      log.debug2(DEBUG_HEADER + "Dropped table '" + tableName + "'.");
      return true;
    } else {
      // No.
      log.debug2(DEBUG_HEADER + "Table '" + tableName
	  + "' does not exist - Not dropping it.");
      return false;
    }
  }

  /**
   * Removes database functions if they exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param functionMap
   *          A Map<String, String> with the removal queries indexed by function
   *          name.
   * @throws SQLException
   *           if any problem occurred removing the functions.
   */
  protected void removeFunctionsIfPresent(Connection conn,
      Map<String, String> functionMap) throws SQLException {
    final String DEBUG_HEADER = "removeFunctionsIfPresent(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "functionMap = " + functionMap);

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Loop through all the function names.
    for (String functionName : functionMap.keySet()) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "functionName = '" + functionName + "'");

      // Remove the function if it does exist.
      removeFunctionIfPresent(conn, functionName,
	  functionMap.get(functionName));
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done with functionName '"
	  + functionName + "'");
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Deletes a database function if it does exist.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param functionName
   *          A String with the name of the function to delete, if present.
   * @param functionDropSql
   *          A String with the SQL code used to drop the function, if present.
   * @return <code>true</code> if the function did exist and it was removed,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if any problem occurred removing the functions.
   */
  private boolean removeFunctionIfPresent(Connection conn, String functionName,
      String functionDropSql) throws SQLException {
    final String DEBUG_HEADER = "removeFunctionIfPresent(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "functionName = '" + functionName + "'.");
      log.debug2(DEBUG_HEADER + "functionDropSql = '" + functionDropSql + "'.");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Check whether the function needs to be removed.
    if (functionExists(conn, functionName)) {
      // Yes: Delete it.
      executeBatch(conn, new String[] { functionDropSql });
      log.debug2(DEBUG_HEADER + "Dropped function '" + functionName + "'.");
      return true;
    } else {
      // No.
      log.debug2(DEBUG_HEADER + "Function '" + functionName
	  + "' does not exist - Not dropping it.");
      return false;
    }
  }

  /**
   * Provides an indication of whether a function exists.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param functionName
   *          A String with name of the function to be checked.
   * @return <code>true</code> if the named function exists, <code>false</code>
   *         otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private boolean functionExists(Connection conn, String functionName)
      throws SQLException {
    final String DEBUG_HEADER = "functionExists(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "functionName = '" + functionName + "'.");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    boolean result = false;
    ResultSet resultSet = null;

    try {
      // Get the database schema function data.
      resultSet = JdbcBridge.getFunctions(conn, null, dataSourceUser, null);

      // Loop through each function found.
      while (resultSet.next()) {
	// Check whether this is the function sought.
	if (functionName.toUpperCase().equals(resultSet
	                                      .getString(FUNCTION_NAME))) {
	  // Found the function: No need to check further.
	  result = true;
	  break;
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot determine whether a function exists", sqle);
      log.error("functionName = '" + functionName + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot determine whether a function exists", re);
      log.error("functionName = '" + functionName + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
    }

    // The function does not exist.
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the query used to drop a function.
   * 
   * @param functionName
   *          A string with the name of the function to be dropped.
   * @return a String with the query used to drop the function.
   */
  protected static String dropFunctionQuery(String functionName) {
    return "drop function " + functionName;
  }

  /**
   * Executes DDL queries.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param queries
   *          A String[] with the database queries.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  protected void executeDdlQueries(Connection conn, String[] queries)
      throws SQLException {
    final String DEBUG_HEADER = "executeDdlQueries(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "queries = " + StringUtil.toString(queries));

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Loop through all the indices.
    for (String query : queries) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Query = '" + query + "'");

      try {
	JdbcBridge.executeDdlQuery(conn, query, maxRetryCount, retryDelay,
	    fetchSize);
      } catch (SQLException sqle) {
	log.error("Cannot execute DDL query '" + query + "'", sqle);
	throw sqle;
      } catch (RuntimeException re) {
	log.error("Cannot execute DDL query '" + query + "'", re);
	throw re;
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the query used to drop a column.
   * 
   * @param tableName
   *          A String with the name of the table to which the column to be
   *          dropped belongs.
   * @param columnName
   *          A String with the name of the column to be dropped.
   * @return a String with the query used to drop the column.
   */
  protected String dropColumnQuery(String tableName, String columnName) {
    if (isTypeMysql()) {
      return "alter table " + tableName + " drop column " + columnName;
    }

    return "alter table " + tableName
	+ " drop column " + columnName + " restrict";
  }

  /**
   * Trims text columns in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param textKind
   *          A String with the kind of column text to be trimmed.
   * @param findTrimmablesSql
   *          A String with the SQL query used to find rows in the database that
   *          contain trimmable text.
   * @param columnName
   *          A String with the name of the column with the text to be trimmed.
   * @param updateTrimmedSql
   *          A String with the SQL query used to update the text in the
   *          database with the trimmed version.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  protected void trimTextColumns(Connection conn, String textKind,
      String findTrimmablesSql, String columnName, String updateTrimmedSql)
      throws SQLException {
    final String DEBUG_HEADER = "trimTextColumns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "textKind = '" + textKind + "'");
      log.debug2(DEBUG_HEADER + "findTrimmablesSql = '" + findTrimmablesSql
	  + "'");
      log.debug2(DEBUG_HEADER + "columnName = '" + columnName + "'");
      log.debug2(DEBUG_HEADER + "updateTrimmedSql = '" + updateTrimmedSql
	  + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Find the instances of trimmable text strings.
    List<String> trimmableTextColumns = findTrimmableTextColumns(conn,
	textKind, findTrimmablesSql, columnName);

    // Loop through the trimmable text strings.
    for (String trimmableText : trimmableTextColumns) {
      // Trim this text string.
      trimTextColumn(conn, trimmableText, textKind, updateTrimmedSql);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Finds trimmable text columns in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param textKind
   *          A String with the kind of column text to be trimmed.
   * @param findTrimmablesSql
   *          A String with the SQL query used to find rows in the database that
   *          contain trimmable text.
   * @param columnName
   *          A String with the name of the column with the text to be trimmed.
   * @return a List<String> with the text strings that are trimmable.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private List<String> findTrimmableTextColumns(Connection conn,
      String textKind, String findTrimmablesSql, String columnName)
      throws SQLException {
    final String DEBUG_HEADER = "findTrimmableTextColumns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "textKind = '" + textKind + "'");
      log.debug2(DEBUG_HEADER + "findTrimmablesSql = '" + findTrimmablesSql
	  + "'");
      log.debug2(DEBUG_HEADER + "columnName = '" + columnName + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    List<String> trimmableStrings = new ArrayList<String>();
    PreparedStatement statement = null;
    ResultSet resultSet = null;

    try {
      // Get the trimmable text strings.
      statement = prepareStatement(conn, findTrimmablesSql);
      resultSet = executeQuery(statement);

      // Loop through all the trimmable text strings.
      while (resultSet.next()) {
	// Get the trimmable column text.
	String text = resultSet.getString(columnName);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "text = '" + text + "'");

	// Check whether it's not empty.
	if (!StringUtil.isNullString(text) && text.trim().length() > 0) {
	  // Yes: Add it to the result.
	  trimmableStrings.add(text);
	} else {
	  // No: Report the problem.
	  log.warning("Ignored empty " + textKind + " '" + text + "'.");
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot get trimmable " + textKind + "s", sqle);
      log.error("SQL = '" + findTrimmablesSql + "'.");
      log.error("columnName = '" + columnName + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot get trimmable " + textKind + "s", re);
      log.error("SQL = '" + findTrimmablesSql + "'.");
      log.error("columnName = '" + columnName + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "trimmableStrings.size() = "
	+ trimmableStrings.size());
    return trimmableStrings;
  }

  /**
   * Trims a text column in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param trimmableText
   *          A String with the text of the column to be trimmed.
   * @param textKind
   *          A String with the kind of column text to be trimmed.
   * @param updateTrimmedSql
   *          A String with the SQL query used to update the text in the
   *          database with the trimmed version.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void trimTextColumn(Connection conn, String trimmableText,
      String textKind, String updateTrimmedSql) throws SQLException {
    final String DEBUG_HEADER = "trimTextColumn(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "trimmableText = '" + trimmableText + "'");
      log.debug2(DEBUG_HEADER + "textKind = '" + textKind + "'");
      log.debug2(DEBUG_HEADER + "updateTrimmedSql = '" + updateTrimmedSql
	  + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    String trimmedText = trimmableText.trim();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "trimmedText = '" + trimmedText + "'");

    // Update the trimmable text with its trimmed value.
    PreparedStatement updateStatement = null;

    try {
      updateStatement = prepareStatement(conn, updateTrimmedSql);
      updateStatement.setString(1, trimmedText);
      updateStatement.setString(2, trimmableText);
      int count = executeUpdate(updateStatement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
      commitOrRollback(conn, log);
    } catch (SQLException sqle) {
      String message = "Cannot update trimmed " + textKind + " text";
      log.warning(message, sqle);
      log.warning("trimmableText = '" + trimmableText + "'.");
      log.warning("trimmedText = '" + trimmedText + "'.");
      log.warning("SQL = '" + updateTrimmedSql + "'.");
      rollback(conn, log);
    } catch (RuntimeException re) {
      String message = "Cannot update trimmed " + textKind + " text";
      log.warning(message, re);
      log.warning("trimmableText = '" + trimmableText + "'.");
      log.warning("trimmedText = '" + trimmedText + "'.");
      log.warning("SQL = '" + updateTrimmedSql + "'.");
      rollback(conn, log);
    } finally {
      JdbcBridge.safeCloseStatement(updateStatement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides all the database updates recorded in the database, sorted in
   * ascending order.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a List<Integer> with the recorded database update versions.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  List<Integer> getSortedDatabaseVersions(Connection conn, String subsystem)
      throws SQLException {
    final String DEBUG_HEADER = "getSortedDatabaseVersions(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "subsystem = " + subsystem);

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    List<Integer> result = new ArrayList<Integer>();
    PreparedStatement stmt = null;
    ResultSet resultSet = null;

    try {
      stmt = prepareStatement(conn, GET_SORTED_DATABASE_VERSIONS_QUERY);
      stmt.setString(1, subsystem);
      resultSet = executeQuery(stmt);

      while (resultSet.next()) {
	int version = resultSet.getShort(VERSION_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "version = " + version);
	result.add(version);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the database version", sqle);
      log.error("SQL = '" + GET_SORTED_DATABASE_VERSIONS_QUERY + "'.");
      log.error("subsystem = " + subsystem);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot get the database version", re);
      log.error("SQL = '" + GET_SORTED_DATABASE_VERSIONS_QUERY + "'.");
      log.error("subsystem = " + subsystem);
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(stmt);
    }

    log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the query used to drop a constraint.
   * 
   * @param tableName
   *          A String with the name of the table to which the constraint to be
   *          dropped belongs.
   * @param constraintName
   *          A String with the name of the constraint to be dropped.
   * @return a String with the query used to drop the constraint.
   */
  protected static String dropConstraintQuery(String tableName,
      String constraintName) {
    return "alter table " + tableName + " drop constraint " + constraintName;
  }

  /**
   * Provides the query used to drop a MySQL foreign key constraint.
   * 
   * @param tableName
   *          A String with the name of the table to which the constraint to be
   *          dropped belongs.
   * @param foreignKeyIndexName
   *          A String with the name used to index the foreign key.
   * @return a String with the query used to drop the constraint.
   */
  protected static String dropMysqlForeignKeyQuery(String tableName,
      String foreignKeyIndexName) {
    return "alter table " + tableName
	+ " drop foreign key " + foreignKeyIndexName;
  }

  /**
   * Provides an indication of whether a given database subsystem upgrade has
   * been completed.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subsystem
   *          A String with database subsystem name.
   * @param version
   *          An int with the version of the database upgrade to check.
   * @return <code>true</code> if the database upgrade has been completed,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  public boolean isVersionCompleted(Connection conn, String subsystem,
      int version) throws SQLException {
    final String DEBUG_HEADER = "isVersionCompleted(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subsystem = " + subsystem);
      log.debug2(DEBUG_HEADER + "version = " + version);
    }

    boolean result = false;

    PreparedStatement stmt = null;
    ResultSet resultSet = null;

    try {
      stmt = prepareStatement(conn, COUNT_DATABASE_VERSION_QUERY);
      stmt.setString(1, subsystem);
      stmt.setInt(2, version);

      resultSet = executeQuery(stmt);
      resultSet.next();
      result = resultSet.getLong(1) > 0;
    } catch (SQLException sqle) {
      log.error("Cannot find a database version", sqle);
      log.error("SQL = '" + COUNT_DATABASE_VERSION_QUERY + "'.");
      log.error("subsystem = " + subsystem + ".");
      log.error("version = " + version);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find a database version", re);
      log.error("SQL = '" + COUNT_DATABASE_VERSION_QUERY + "'.");
      log.error("subsystem = " + subsystem + ".");
      log.error("version = " + version);
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(stmt);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates the database version table.
   *
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  void createVersionTable(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "createVersionTable(): ";

    // Create the table if it does not exist.
    createTableIfMissing(conn, VERSION_TABLE,
	localizeCreateQuery(CREATE_VERSION_TABLE_QUERY));
    if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Created table '" + VERSION_TABLE + "'.");
  }

  /**
   * Adds the subsystem column to the version table.
   *
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  void addVersionSubsystemColumn(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "addVersionSubsystemColumn(): ";

    // Create the table if it does not exist.
    executeDdlQuery(conn, ADD_SUBSYSTEM_COLUMN);
    if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Added column '" + SUBSYSTEM_COLUMN + "'.");

    // Create the necessary index.
    executeDdlQuery(conn, CREATE_VERSION_INDEX_QUERY);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "Added index to table '" + VERSION_TABLE + "'.");
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
   *           if any problem occurred accessing the database.
   */
  boolean columnExists(Connection conn, String tableName, String columnName)
      throws SQLException {
    final String DEBUG_HEADER = "columnExists(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'");
      log.debug2(DEBUG_HEADER + "columnName = '" + columnName + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    boolean result = false;
    ResultSet resultSet = null;

    try {
      // Get the database schema table data.
      if (isTypeDerby()) {
	resultSet = JdbcBridge.getColumns(conn, null, dataSourceUser,
	    tableName.toUpperCase(), columnName.toUpperCase());
      } else if (isTypePostgresql()) {
	resultSet = JdbcBridge.getColumns(conn, null, dataSourceUser,
	    tableName.toLowerCase(), columnName.toLowerCase());
      } else if (isTypeMysql()) {
	resultSet = JdbcBridge.getColumns(conn, null, dataSourceUser,
	    tableName, columnName);
      }

      // Determine whether the table exists.
      result = resultSet.next();
    } catch (SQLException sqle) {
	log.error("Cannot determine whether column '" + columnName
	    + "' in table '" + tableName + "' exists", sqle);
	throw sqle;
    } catch (RuntimeException re) {
	log.error("Cannot determine whether column '" + columnName
	    + "' in table '" + tableName + "' exists", re);
	throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }
}
