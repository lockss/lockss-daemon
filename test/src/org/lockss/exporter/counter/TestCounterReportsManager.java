/*
 * $Id: TestCounterReportsManager.java,v 1.2 2012-08-29 23:07:12 fergaloy-sf Exp $
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
 * Test class for org.lockss.exporter.counter.CounterReportsManager.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
package org.lockss.exporter.counter;

import static org.lockss.exporter.counter.CounterReportsManager.*;
import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import org.lockss.config.ConfigManager;
import org.lockss.daemon.Cron;
import org.lockss.db.DbManager;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.IOUtil;

public class TestCounterReportsManager extends LockssTestCase {
  // Query to count all the rows of titles.
  private static final String SQL_QUERY_TITLE_COUNT = "select count(*) from "
      + SQL_TABLE_TITLES;

  // Query to count all the rows of requests.
  private static final String SQL_QUERY_REQUEST_COUNT = "select count(*) from "
      + SQL_TABLE_REQUESTS;

  private MockLockssDaemon theDaemon;
  private DbManager dbManager;
  private CounterReportsManager counterReportsManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath();

    // Set the database log.
    System.setProperty("derby.stream.error.file", new File(tempDirPath,
	"derby.log").getAbsolutePath());

    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
	      tempDirPath);
    props.setProperty(CounterReportsManager.PARAM_REPORT_BASEDIR_PATH,
	tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.setDaemonInited(true);

    dbManager = new DbManager();
    theDaemon.setDbManager(dbManager);
    dbManager.initService(theDaemon);
    dbManager.startService();

    Cron cron = new Cron();
    theDaemon.setCron(cron);
    cron.initService(theDaemon);
    cron.startService();
  }

  /**
   * Runs all the tests.
   * <br />
   * This avoids unnecessary set up and tear down of the database.
   * 
   * @throws Exception
   */
  public void testAll() throws Exception {
    startService();
    runTestReady();
    runTestAllBooksAllJournalsTitles();
    runTestOutputDir();
    runTestRequestPersistence();
    runTestWriteDeleteReportFile();
    runTestNotReady();
  }

  /**
   * Creates and starts the CounterReportsManager.
   * 
   * @throws Exception
   */
  private void startService() throws Exception {
    counterReportsManager = new CounterReportsManager();
    theDaemon.setCounterReportsManager(counterReportsManager);
    counterReportsManager.initService(theDaemon);
    counterReportsManager.startService();
  }

  /**
   * Tests a CounterReportsManager that is ready to be used.
   * 
   * @throws Exception
   */
  public void runTestReady() throws Exception {
    assertEquals(true, counterReportsManager.isReady());
  }

  /**
   * Tests the generation of titles used for totals.
   * 
   * @throws Exception
   */
  public void runTestAllBooksAllJournalsTitles() throws Exception {
    assertEquals(4796129050038543734L,
	counterReportsManager.getAllBooksLockssId());
    assertEquals(3570692956966825695L,
	counterReportsManager.getAllJournalsLockssId());
  }

  /**
   * Tests the output directory.
   * 
   * @throws Exception
   */
  public void runTestOutputDir() throws Exception {
    assertEquals(true, counterReportsManager.getOutputDir().exists());
  }

  /**
   * Tests the persistence of a title request.
   * 
   * @throws Exception
   */
  public void runTestRequestPersistence() throws Exception {
    CounterReportsBook book =
	new CounterReportsBook("Book1", "Publisher1", null, "02468", null,
	    "987-654321-0987", "1234-5678");
    book.identify();

    counterReportsManager.persistRequest(book, null);
    checkTitleRowCount(3);
    checkRequestRowCount(1);
  }

  /**
   * Checks the expected count of rows in the title table.
   * 
   * @param expected
   *          An int with the expected number of rows in the table.
   * @throws SQLException
   */
  private void checkTitleRowCount(int expected) throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_TITLE_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = conn.prepareStatement(sql);
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	count = resultSet.getInt(1);
      }
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }

    assertEquals(expected, count);
  }

  /**
   * Checks the expected count of rows in the request table.
   * 
   * @param expected
   *          An int with the expected number of rows in the table.
   * @throws SQLException
   */
  private void checkRequestRowCount(int expected) throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_REQUEST_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = conn.prepareStatement(sql);
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	count = resultSet.getInt(1);
      }
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }

    assertEquals(expected, count);
  }

  /**
   * Tests the writing and removal of a report file.
   * 
   * @throws Exception
   */
  public void runTestWriteDeleteReportFile() throws Exception {
    String fileName = "testFile";
    PrintWriter writer = counterReportsManager.getReportOutputWriter(fileName);
    writer.println("test line");
    assertEquals(false, writer.checkError());
    IOUtil.safeClose(writer);
    assertEquals(true, counterReportsManager.deleteReportOutputFile(fileName));
    assertEquals(false, counterReportsManager.deleteReportOutputFile(fileName));
  }

  /**
   * Tests a CounterReportsManager that is not ready to be used.
   * 
   * @throws Exception
   */
  public void runTestNotReady() throws Exception {
    counterReportsManager.stopService();
    dbManager.stopService();
    startService();
    assertEquals(false, counterReportsManager.isReady());
    assertEquals(false, counterReportsManager.deleteReportOutputFile(""));
    assertNull(counterReportsManager.getReportOutputWriter(""));
  }
}
