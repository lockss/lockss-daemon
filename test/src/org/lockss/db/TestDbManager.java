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
import java.sql.DriverManager;
import org.lockss.db.DbException;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;

/**
 * Test class for org.lockss.db.DbManager.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
public class TestDbManager extends LockssTestCase {
  private static String TABLE_CREATE_SQL =
      "create table testtable (id bigint NOT NULL, name varchar(512))";

  private MockLockssDaemon theDaemon;
  private String tempDirPath;
  private DbManager dbManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    // Get the temporary directory used during the test.
    tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.setDaemonInited(true);
  }

  /**
   * Tests table creation with the minimal configuration.
   * 
   * @throws Exception
   */
  public void testCreateTable1() throws Exception {
    createTable();
  }

  /**
   * Tests the safe roll back.
   * 
   * @throws Exception
   */
  public void testRollback() throws Exception {
    dbManager = getTestDbManager(tempDirPath);
    assertTrue(dbManager.isReady());
    DbManagerSql dbManagerSql = dbManager.getDbManagerSql();

    Connection conn = dbManager.getConnection();
    assertNotNull(conn);
    assertFalse(dbManagerSql.tableExists(conn, "testtable"));
    assertTrue(dbManagerSql
	.createTableIfMissing(conn, "testtable", TABLE_CREATE_SQL));
    assertTrue(dbManagerSql.tableExists(conn, "testtable"));

    DbManager.safeRollbackAndClose(conn);
    conn = dbManager.getConnection();
    assertNotNull(conn);
    assertFalse(dbManagerSql.tableExists(conn, "testtable"));
  }

  /**
   * Tests the commit or rollback method.
   * 
   * @throws Exception
   */
  public void testCommitOrRollback() throws Exception {
    dbManager = getTestDbManager(tempDirPath);
    assertTrue(dbManager.isReady());

    Connection conn = dbManager.getConnection();
    Logger logger = Logger.getLogger("testCommitOrRollback");
    DbManager.commitOrRollback(conn, logger);
    DbManager.safeCloseConnection(conn);

    conn = null;
    try {
      DbManager.commitOrRollback(conn, logger);
      fail("commitOrRollback() should throw");
    } catch (DbException dbe) {
    }
  }

  @Override
  public void tearDown() throws Exception {
    dbManager.waitForThreadsToFinish(500);
    dbManager.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Creates a table and verifies that it exists.
   * 
   * @throws Exception
   */
  protected void createTable() throws Exception {
    dbManager = getTestDbManager(tempDirPath);
    assertTrue(dbManager.isReady());
    DbManagerSql dbManagerSql = dbManager.getDbManagerSql();

    Connection conn = dbManager.getConnection();
    assertNotNull(conn);
    assertFalse(dbManagerSql.tableExists(conn, "testtable"));
    assertTrue(dbManagerSql
	.createTableIfMissing(conn, "testtable", TABLE_CREATE_SQL));
    assertTrue(dbManagerSql.tableExists(conn, "testtable"));
    dbManagerSql.logTableSchema(conn, "testtable");
    assertFalse(dbManagerSql
	.createTableIfMissing(conn, "testtable", TABLE_CREATE_SQL));
  }

  /**
   * Tests text truncation.
   * 
   * @throws Exception
   */
  public void testTruncation() throws Exception {
    dbManager = getTestDbManager(tempDirPath);

    String original = "Total characters = 21";

    String truncated = DbManagerSql.truncateVarchar(original, 30);
    assertTrue(original.equals(truncated));
    assertFalse(DbManagerSql.isTruncatedVarchar(truncated));

    truncated = DbManagerSql.truncateVarchar(original, original.length());
    assertTrue(original.equals(truncated));
    assertFalse(DbManagerSql.isTruncatedVarchar(truncated));

    truncated = DbManagerSql.truncateVarchar(original, original.length() - 3);
    assertFalse(original.equals(truncated));
    assertTrue(DbManagerSql.isTruncatedVarchar(truncated));
    assertTrue(truncated.length() == original.length() - 3);
  }

  /**
   * Tests authentication with the default data source.
   * 
   * @throws Exception
   */
  public void testAuthenticationDefault() throws Exception {
    dbManager = getTestDbManager(tempDirPath);

    String dbUrlRoot = "jdbc:derby://localhost:1527/" + tempDirPath
	+ "/db/DbManager";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      DriverManager.getConnection(dbUrlRoot);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }

    String dbUrl = dbUrlRoot + ";user=LOCKSS";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      DriverManager.getConnection(dbUrl);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }

    dbUrl = dbUrlRoot + ";user=LOCKSS;password=somePassword";

    try {
      Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
      DriverManager.getConnection(dbUrl);
      fail("getConnection() should throw");
    } catch (Exception e) {
    }
  }
}
