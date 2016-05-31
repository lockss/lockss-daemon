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
package org.lockss.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.lockss.util.Logger;

/**
 * A JDBC context that allows the cancellation of SQL queries.
 */
public class JdbcContext {
  private static Logger log = Logger.getLogger(JdbcContext.class);

  private Connection connection;
  private Statement statement;

  /**
   * Default constructor.
   */
  public JdbcContext() {
  }

  /**
   * Connection constructor.
   * 
   * @param connection
   *          A Connection with the connection to the database
   */
  public JdbcContext(Connection connection) {
    this.connection = connection;
  }

  /**
   * Aborts the current SQL query.
   */
  public void abort() {
    final String DEBUG_HEADER = "abort(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

    boolean isValidStatement = false;

    try {
      // Check whether the statement exists and it's not closed.
      if (statement != null && !statement.isClosed()) {
	// Yes: Tell the database to cancel it.
	isValidStatement = true;
	statement.cancel();
      }
    } catch (SQLException sqle) {
      // The call to cancel the statement will throw a SQLException.
      if (log.isDebug3()) log.debug3(DEBUG_HEADER
	  + "Caught expected statement execution cancellation SQLException: "
	  +  sqle.getMessage());
    } finally {
      // Check whether the statement exists and it's not closed.
      if (isValidStatement) {
	// Yes: Close any result set.
	try {
	  DbManager.safeCloseResultSet(statement.getResultSet());
	} catch (SQLException sqle) {
	  log.error("Cannot close result set", sqle);
	}

	// Close the statement.
	DbManager.safeCloseStatement(statement);
      }

      // Release the connection.
      if (connection != null) {
	try {
	  if (!connection.isClosed()) {
	    DbManager.safeRollbackAndClose(connection);
	  }
	} catch (SQLException sqle) {
	  log.error("Cannot determine whether connection is closed", sqle);
	}
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  public Connection getConnection() {
    return connection;
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public Statement getStatement() {
    return statement;
  }

  public void setStatement(Statement statement) {
    this.statement = statement;
  }
}
