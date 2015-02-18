/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.lockss.util.Deadline;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * Bridge between the database manager and the JDBC code.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class JdbcBridge {
  private static final Logger log = Logger.getLogger(JdbcBridge.class);

  private static final String[] STANDARD_TABLE_TYPES = new String[] {"TABLE"};

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
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static Connection getConnection(DataSource ds, int maxRetryCount,
      long retryDelay, boolean autoCommit) throws SQLException {
    final String DEBUG_HEADER = "getConnection(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "maxRetryCount = " + maxRetryCount);
      log.debug2(DEBUG_HEADER + "retryDelay = " + retryDelay);
      log.debug2(DEBUG_HEADER + "autoCommit = " + autoCommit);
    }

    if (ds == null) {
      throw new IllegalArgumentException("Null datasource");
    }

    boolean success = false;
    int retryCount = 0;
    Connection conn = null;

    // Keep trying until success.
    while (!success) {
      try {
	// Get the connection.
	if (ds instanceof javax.sql.ConnectionPoolDataSource) {
	  conn = ((javax.sql.ConnectionPoolDataSource) ds)
	      .getPooledConnection().getConnection();
	} else {
	  conn = ds.getConnection();
    	}

	conn.setAutoCommit(autoCommit);
	success = true;
      } catch (SQLTransientException sqlte) {
	// A SQLTransientException is caught: Count the next retry.
	retryCount++;

	// Check whether the next retry would go beyond the specified maximum
	// number of retries.
	if (retryCount > maxRetryCount) {
	  // Yes: Report the failure.
	  log.error("Transient exception caught", sqlte);
	  log.error("Maximum retry count of " + maxRetryCount + " reached.");
	  throw new RuntimeException("Cannot get a database connection", sqlte);
	} else {
	  // No: Wait for the specified amount of time before attempting the
	  // next retry.
	  log.debug(DEBUG_HEADER + "Transient exception caught", sqlte);
	  log.debug(DEBUG_HEADER + "Waiting "
	      	    + StringUtil.timeIntervalToString(retryDelay)
	      	    + " before retry number " + retryCount + "...");

	  try {
	    Deadline.in(retryDelay).sleep();
	  } catch (InterruptedException ie) {
	    // Continue with the next retry.
	  }
	}
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return conn;
  }

  /**
   * Provides a description of the tables available in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param catalog
   *          a catalog name; must match the catalog name as it is stored in the
   *          database; "" retrieves those without a catalog; <code>null</code>
   *          means that the catalog name should not be used to narrow the
   *          search.
   * @param schemaPattern
   *          a schema name pattern; must match the schema name as it is stored
   *          in the database; "" retrieves those without a schema;
   *          <code>null</code> means that the schema name should not be used to
   *          narrow the search.
   * @param tableNamePattern
   *          a table name pattern; must match the table name as it is stored in
   *          the database.
   * @return a ResultSet in which each row is a table description.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static ResultSet getStandardTables(Connection conn, String catalog,
      String schemaPattern, String tableNamePattern) throws SQLException {
    return getTables(conn, catalog, schemaPattern, tableNamePattern,
	STANDARD_TABLE_TYPES);
  }

  /**
   * Provides a description of the tables available in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param catalog
   *          a catalog name; must match the catalog name as it is stored in the
   *          database; "" retrieves those without a catalog; <code>null</code>
   *          means that the catalog name should not be used to narrow the
   *          search.
   * @param schemaPattern
   *          a schema name pattern; must match the schema name as it is stored
   *          in the database; "" retrieves those without a schema;
   *          <code>null</code> means that the schema name should not be used to
   *          narrow the search.
   * @param tableNamePattern
   *          a table name pattern; must match the table name as it is stored in
   *          the database.
   * @param types
   *          a list of table types, which must be from the list of table types
   *          returned from {@link java.sql.DatabaseMetaData#getTableTypes},
   *          to include; <code>null</code> returns all types.
   * @return a ResultSet in which each row is a table description.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static ResultSet getTables(Connection conn, String catalog,
      String schemaPattern, String tableNamePattern, String[] types)
      throws SQLException {
    final String DEBUG_HEADER = "getTables(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "catalog = '" + catalog + "'");
      log.debug2(DEBUG_HEADER + "schemaPattern = '" + schemaPattern + "'");
      log.debug2(DEBUG_HEADER + "tableNamePattern = '" + tableNamePattern
	  + "'");
      log.debug2(DEBUG_HEADER + "types = " + StringUtil.toString(types));
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    return getMetadata(conn).getTables(catalog, schemaPattern,
	tableNamePattern, types);
  }

  /**
   * Provides a description of the system and user functions available in the
   * database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param catalog
   *          a catalog name; must match the catalog name as it is stored in the
   *          database; "" retrieves those without a catalog; <code>null</code>
   *          means that the catalog name should not be used to narrow the
   *          search.
   * @param schemaPattern
   *          a schema name pattern; must match the schema name as it is stored
   *          in the database; "" retrieves those without a schema;
   *          <code>null</code> means that the schema name should not be used to
   *          narrow the search.
   * @param functionNamePattern
   *          a function name pattern; must match the function name as it is
   *          stored in the database
   * @return a ResultSet in which each row is a function description.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static ResultSet getFunctions(Connection conn, String catalog,
      String schemaPattern, String functionNamePattern) throws SQLException {
    final String DEBUG_HEADER = "getFunctions(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "catalog = '" + catalog + "'");
      log.debug2(DEBUG_HEADER + "schemaPattern = '" + schemaPattern + "'");
      log.debug2(DEBUG_HEADER + "functionNamePattern = " + functionNamePattern
	  + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    return getMetadata(conn).getFunctions(catalog, schemaPattern,
	functionNamePattern);
  }

  /**
   * Provides a description of the table columns available in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param catalog
   *          a catalog name; must match the catalog name as it is stored in the
   *          database; "" retrieves those without a catalog; <code>null</code>
   *          means that the catalog name should not be used to narrow the
   *          search.
   * @param schemaPattern
   *          a schema name pattern; must match the schema name as it is stored
   *          in the database; "" retrieves those without a schema;
   *          <code>null</code> means that the schema name should not be used to
   *          narrow the search.
   * @param tableNamePattern
   *          a table name pattern; must match the table name as it is stored in
   *          the database
   * @param columnNamePattern
   *          a column name pattern; must match the column name as it is stored
   *          in the database
   * @return a ResultSet in which each row is a column description.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static ResultSet getColumns(Connection conn, String catalog,
      String schemaPattern, String tableNamePattern, String columnNamePattern)
      throws SQLException {
    final String DEBUG_HEADER = "getColumns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "catalog = '" + catalog + "'");
      log.debug2(DEBUG_HEADER + "schemaPattern = '" + schemaPattern + "'");
      log.debug2(DEBUG_HEADER + "tableNamePattern = '" + tableNamePattern
	  + "'");
      log.debug2(DEBUG_HEADER + "columnNamePattern = '" + columnNamePattern
	  + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    return getMetadata(conn).getColumns(catalog, schemaPattern,
	tableNamePattern, columnNamePattern);
  }

  /**
   * Provides the database metadata.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @return a DatabaseMetaData with the database metadata.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static DatabaseMetaData getMetadata(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "getMetadata(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    DatabaseMetaData metaData = conn.getMetaData();
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return metaData;
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
   * @param fetchSize
   *          An int with the SQL statement fetch size.
   * @return a PreparedStatement with the prepared statement.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static PreparedStatement prepareStatement(Connection conn, String sql,
      int returnGeneratedKeys, int maxRetryCount, long retryDelay,
      int fetchSize) throws SQLException {
    final String DEBUG_HEADER = "prepareStatement(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "sql = '" + sql + "'");
      log.debug2(DEBUG_HEADER + "returnGeneratedKeys = " + returnGeneratedKeys);
      log.debug2(DEBUG_HEADER + "maxRetryCount = " + maxRetryCount);
      log.debug2(DEBUG_HEADER + "retryDelay = " + retryDelay);
      log.debug2(DEBUG_HEADER + "fetchSize = " + fetchSize);
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    boolean success = false;
    int retryCount = 0;
    PreparedStatement statement = null;

    // Keep trying until success.
    while (!success) {
      try {
	// Prepare the statement.
	statement = conn.prepareStatement(sql, returnGeneratedKeys);
	statement.setFetchSize(fetchSize);
	success = true;
      } catch (SQLTransientException sqlte) {
	// A SQLTransientException is caught: Count the next retry.
	retryCount++;

	// Check whether the next retry would go beyond the specified maximum
	// number of retries.
	if (retryCount > maxRetryCount) {
	  // Yes: Report the failure.
	  log.error("Transient exception caught", sqlte);
	  log.error("Maximum retry count of " + maxRetryCount + " reached.");
	  throw new RuntimeException("Cannot prepare a statement", sqlte);
	} else {
	  // No: Wait for the specified amount of time before attempting the
	  // next retry.
	  log.debug(DEBUG_HEADER + "Transient exception caught", sqlte);
	  log.debug(DEBUG_HEADER + "Waiting "
	      	    + StringUtil.timeIntervalToString(retryDelay)
	      	    + " before retry number " + retryCount + "...");

	  try {
	    Deadline.in(retryDelay).sleep();
	  } catch (InterruptedException ie) {
	    // Continue with the next retry.
	  }
	}
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return statement;
  }

  /**
   * Create a statement.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @return a Statement with the created statement.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static Statement createStatement(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "createStatement(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    Statement statement = null;

    try {
      statement = conn.createStatement();
    } catch (SQLException sqle) {
      log.error("Cannot create statement", sqle);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot create statement", re);
      throw re;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return statement;
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
  static ResultSet executeQuery(PreparedStatement statement, int maxRetryCount,
      long retryDelay) throws SQLException {
    final String DEBUG_HEADER = "executeQuery(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "maxRetryCount = " + maxRetryCount);
      log.debug2(DEBUG_HEADER + "retryDelay = " + retryDelay);
    }

    if (statement == null) {
      throw new IllegalArgumentException("Null statement");
    }

    boolean success = false;
    int retryCount = 0;
    ResultSet results = null;

    // Keep trying until success.
    while (!success) {
      try {
	// Execute the query.
	results = statement.executeQuery();
	success = true;
      } catch (SQLTransientException sqlte) {
	// A SQLTransientException is caught: Count the next retry.
	retryCount++;

	// Check whether the next retry would go beyond the specified maximum
	// number of retries.
	if (retryCount > maxRetryCount) {
	  // Yes: Report the failure.
	  log.error("Transient exception caught", sqlte);
	  log.error("Maximum retry count of " + maxRetryCount + " reached.");
	  throw new RuntimeException("Cannot execute a query", sqlte);
	} else {
	  // No: Wait for the specified amount of time before attempting the
	  // next retry.
	  log.debug(DEBUG_HEADER + "Transient exception caught", sqlte);
	  log.debug(DEBUG_HEADER + "Waiting "
		    + StringUtil.timeIntervalToString(retryDelay)
		    + " before retry number " + retryCount + "...");

	  try {
	    Deadline.in(retryDelay).sleep();
	  } catch (InterruptedException ie) {
	    // Continue with the next retry.
	  }
	}
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return results;
  }

  /**
   * Executes an updating prepared statement, retrying the execution  with the
   * specified parameters in case of transient failures. To be used during
   * initialization.
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
  static int executeUpdate(PreparedStatement statement, int maxRetryCount,
      long retryDelay) throws SQLException {
    final String DEBUG_HEADER = "executeUpdate(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "maxRetryCount = " + maxRetryCount);
      log.debug2(DEBUG_HEADER + "retryDelay = " + retryDelay);
    }

    if (statement == null) {
      throw new IllegalArgumentException("Null statement");
    }

    boolean success = false;
    int retryCount = 0;
    int updatedCount = 0;

    // Keep trying until success.
    while (!success) {
      try {
	// Execute the query.
	updatedCount = statement.executeUpdate();
	success = true;
      } catch (SQLTransientException sqlte) {
	// A SQLTransientException is caught: Count the next retry.
	retryCount++;

	// Check whether the next retry would go beyond the specified maximum
	// number of retries.
	if (retryCount > maxRetryCount) {
	  // Yes: Report the failure.
	  log.error("Transient exception caught", sqlte);
	  log.error("Maximum retry count of " + maxRetryCount + " reached.");
	  throw new RuntimeException("Cannot execute a query", sqlte);
	} else {
	  // No: Wait for the specified amount of time before attempting the
	  // next retry.
	  log.debug(DEBUG_HEADER + "Transient exception caught", sqlte);
	  log.debug(DEBUG_HEADER + "Waiting "
		    + StringUtil.timeIntervalToString(retryDelay)
		    + " before retry number " + retryCount + "...");

	  try {
	    Deadline.in(retryDelay).sleep();
	  } catch (InterruptedException ie) {
	    // Continue with the next retry.
	  }
	}
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "updatedCount = " + updatedCount);
    return updatedCount;
  }

  /**
   * Executes a DDL query.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param ddlQuery
   *          A String with the DDL query to be executed.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @param fetchSize
   *          An int with the SQL statement fetch size.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static void executeDdlQuery(Connection conn, String ddlQuery,
      int maxRetryCount, long retryDelay, int fetchSize) throws SQLException {
    final String DEBUG_HEADER = "executeDdlQuery(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "ddlQuery = '" + ddlQuery + "'");
      log.debug2(DEBUG_HEADER + "maxRetryCount = " + maxRetryCount);
      log.debug2(DEBUG_HEADER + "retryDelay = " + retryDelay);
      log.debug2(DEBUG_HEADER + "fetchSize = " + fetchSize);
    }

    if (conn == null) {
      throw new RuntimeException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      statement = prepareStatement(conn, ddlQuery, Statement.NO_GENERATED_KEYS,
	  maxRetryCount, retryDelay, fetchSize);

      int count = executeUpdate(statement, maxRetryCount, retryDelay);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } finally {
      safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Executes a batch of statements.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param statements
   *          A String[] with the statements to be executed.
   * @return an int[] with the number of database rows updated by each
   *         statement.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static int[] executeBatch(Connection conn, String[] statements)
      throws SQLException {
    final String DEBUG_HEADER = "executeBatch(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "statements = "
	+ StringUtil.toString(statements));

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    int[] counts = null;
    Statement batchStatement = null;

    try {
      batchStatement = createStatement(conn);

      // Loop through all the statements.
      for (String statement : statements) {
	// Add each statement to the batch.
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "statement = '" + statement + "'.");
	batchStatement.addBatch(statement);
      }

      // Execute the batch.
      counts = batchStatement.executeBatch();
    } catch (SQLException sqle) {
      log.error("Cannot execute batch", sqle);
      log.error("statements = " + StringUtil.toString(statements));
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot execute batch", re);
      log.error("statements = " + StringUtil.toString(statements));
      throw re;
    } finally {
      safeCloseStatement(batchStatement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "counts = "
	+ StringUtil.toString(counts));
    return counts;
  }

  /**
   * Closes a result set without throwing exceptions.
   * 
   * @param resultSet
   *          A ResultSet with the database result set to be closed.
   */
  static void safeCloseResultSet(ResultSet resultSet) {
    if (resultSet != null) {
      try {
	resultSet.close();
      } catch (SQLException sqle) {
	log.error("Cannot close result set", sqle);
      } catch (RuntimeException re) {
	log.error("Cannot close result set", re);
      }
    }
  }

  /**
   * Closes a statement without throwing exceptions.
   * 
   * @param statement
   *          A Statement with the database statement to be closed.
   */
  static void safeCloseStatement(Statement statement) {
    if (statement != null) {
      try {
	statement.close();
      } catch (SQLException sqle) {
	log.error("Cannot close statement", sqle);
      } catch (RuntimeException re) {
	log.error("Cannot close statement", re);
      }
    }
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
  static void commitOrRollback(Connection conn, Logger logger)
      throws SQLException {
    final String DEBUG_HEADER = "commitOrRollback(): ";

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    try {
      conn.commit();
      if (logger != null && logger.isDebug3())
	logger.debug3(DEBUG_HEADER + "Committed.");
    } catch (SQLException sqle) {
      if (logger != null)
	logger.error("Exception caught committing the connection", sqle);
      safeRollbackAndClose(conn);
      throw sqle;
    } catch (RuntimeException re) {
      if (logger != null)
	logger.error("Exception caught committing the connection", re);
      safeRollbackAndClose(conn);
      throw re;
    }
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
    final String DEBUG_HEADER = "rollback(): ";

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    try {
      conn.rollback();
      if (logger != null && logger.isDebug3())
	logger.debug3(DEBUG_HEADER + "Rolled back.");
    } catch (SQLException sqle) {
      if (logger != null)
	logger.error("Exception caught rolling back the connection", sqle);
      safeCloseConnection(conn);
      throw sqle;
    } catch (RuntimeException re) {
      if (logger != null)
	logger.error("Exception caught rolling back the connection", re);
      safeCloseConnection(conn);
      throw re;
    }
  }

  /**
   * Rolls back and closes a connection without throwing exceptions.
   * 
   * @param conn
   *          A Connection with the database connection to be rolled back and
   *          closed.
   */
  static void safeRollbackAndClose(Connection conn) {
    // Roll back the connection.
    try {
      rollback(conn, log);
    } catch (SQLException sqle) {
      log.error("Cannot roll back the connection", sqle);
    } catch (RuntimeException re) {
      log.error("Cannot roll back the connection", re);
    }
    // Close it.
    safeCloseConnection(conn);
  }

  /**
   * Closes a connection without throwing exceptions.
   * 
   * @param conn
   *          A Connection with the database connection to be closed.
   */
  static void safeCloseConnection(Connection conn) {
    try {
      if ((conn != null) && !conn.isClosed()) {
	conn.close();
      }
    } catch (SQLException sqle) {
      log.error("Cannot close connection", sqle);
    } catch (RuntimeException re) {
      log.error("Cannot close connection", re);
    }
  }
}
