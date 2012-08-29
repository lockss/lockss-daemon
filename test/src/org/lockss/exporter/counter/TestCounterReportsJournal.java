/*
 * $Id: TestCounterReportsJournal.java,v 1.2 2012-08-29 23:07:12 fergaloy-sf Exp $
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
 * Test class for org.lockss.exporter.counter.CounterReportsJournal.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
package org.lockss.exporter.counter;

import static org.lockss.exporter.counter.CounterReportsManager.*;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.lockss.config.ConfigManager;
import org.lockss.daemon.Cron;
import org.lockss.db.DbManager;
import org.lockss.exporter.counter.CounterReportsJournal;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.TimeBase;

public class TestCounterReportsJournal extends LockssTestCase {
  // Query to count all the rows of titles.
  private static final String SQL_QUERY_TITLE_COUNT = "select count(*) from "
      + SQL_TABLE_TITLES;

  // Query to count all the rows of requests.
  private static final String SQL_QUERY_REQUEST_COUNT = "select count(*) from "
      + SQL_TABLE_REQUESTS;

  // Query to count the rows of requests for a LOCKSS identifier.
  private static final String SQL_QUERY_REQUEST_BY_ID_COUNT = "select "
      + "count(*) "
      + "from " + SQL_TABLE_REQUESTS
      + " where " + SQL_COLUMN_LOCKSS_ID + " = ?";

  // Query to get the properties of a journal.
  private static final String SQL_QUERY_JOURNAL_SELECT = "select "
      + SQL_COLUMN_TITLE_NAME + ","
      + SQL_COLUMN_PUBLISHER_NAME + ","
      + SQL_COLUMN_PLATFORM_NAME + ","
      + SQL_COLUMN_DOI + ","
      + SQL_COLUMN_PROPRIETARY_ID + ","
      + SQL_COLUMN_IS_BOOK + ","
      + SQL_COLUMN_PRINT_ISSN + ","
      + SQL_COLUMN_ONLINE_ISSN
      + " from " + SQL_TABLE_TITLES
      + " where " + SQL_COLUMN_LOCKSS_ID + " = ?";

  // Query to get the properties of a journal request.
  private static final String SQL_QUERY_JOURNAL_REQUEST_SELECT = "select "
      + SQL_COLUMN_PUBLICATION_YEAR + ","
      + SQL_COLUMN_IS_HTML + ","
      + SQL_COLUMN_IS_PDF + ","
      + SQL_COLUMN_IS_PUBLISHER_INVOLVED + ","
      + SQL_COLUMN_REQUEST_YEAR + ","
      + SQL_COLUMN_REQUEST_MONTH + ","
      + SQL_COLUMN_REQUEST_DAY
      + " from " + SQL_TABLE_REQUESTS
      + " where " + SQL_COLUMN_LOCKSS_ID + " = ?";

  private MockLockssDaemon theDaemon;
  private DbManager dbManager;

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

    CounterReportsManager counterReportsManager = new CounterReportsManager();
    theDaemon.setCounterReportsManager(counterReportsManager);
    counterReportsManager.initService(theDaemon);
    counterReportsManager.startService();
  }

  /**
   * Runs all the tests.
   * <br />
   * This avoids unnecessary set up and tear down of the database.
   * 
   * @throws Exception
   */
  public void testAll() throws Exception {
    runTestCreation();
    runTestPersist();
    runTestPersistRequest();
    runTestNoRequestDataPersistRequest();
    runTestPersistMultipleRequests();
    runTestNoTitleFailure();
  }

  /**
   * Tests the successful creation of a journal.
   * 
   * @throws Exception
   */
  public void runTestCreation() throws Exception {
    CounterReportsJournal journal =
	new CounterReportsJournal("Journal1", "Publisher1", null, "02468",
	    null, "1234-5678", "9876-5432");

    assertEquals("Journal1", journal.getName());
    assertEquals("Publisher1", journal.getPublisherName());
    assertEquals("02468", journal.getDoi());
    assertEquals("1234-5678", journal.getPrintIssn());
    assertEquals("9876-5432", journal.getOnlineIssn());
    journal.identify();
    assertEquals(1575314412851003212L, journal.getLockssId());
  }

  /**
   * Tests the successful persistence of a journal.
   * 
   * @throws Exception
   */
  public void runTestPersist() throws Exception {
    checkTitleRowCount(3);

    CounterReportsJournal journal =
	new CounterReportsJournal("Journal1", "Publisher1", null, null, null,
	    "1234-5678", "9876-5432");
    journal.identify();

    checkTitleRowCount(4);
    checkJournal(journal);
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
   * Checks the expected properties of a journal in the database.
   * 
   * @param journal
   *          A CounterReportsJournal with the journal to be checked.
   * @throws SQLException
   */
  private void checkJournal(CounterReportsJournal journal) throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String sql = SQL_QUERY_JOURNAL_SELECT;

    try {
      conn = dbManager.getConnection();

      statement = conn.prepareStatement(sql);
      statement.setLong(1, journal.getLockssId());
      resultSet = statement.executeQuery();

      assertEquals(true, resultSet.next());
      assertEquals(journal.getName(),
	  resultSet.getString(SQL_COLUMN_TITLE_NAME));
      assertEquals(journal.getPublisherName(),
	  resultSet.getString(SQL_COLUMN_PUBLISHER_NAME));
      assertEquals(journal.getPublishingPlatform(),
	  resultSet.getString(SQL_COLUMN_PLATFORM_NAME));
      assertEquals(journal.getDoi(), resultSet.getString(SQL_COLUMN_DOI));
      assertEquals(journal.getProprietaryId(),
	  resultSet.getString(SQL_COLUMN_PROPRIETARY_ID));
      assertEquals(false, resultSet.getBoolean(SQL_COLUMN_IS_BOOK));
      assertEquals(journal.getPrintIssn(),
	  resultSet.getString(SQL_COLUMN_PRINT_ISSN));
      assertEquals(journal.getOnlineIssn(),
	  resultSet.getString(SQL_COLUMN_ONLINE_ISSN));
      assertEquals(false, resultSet.next());
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Tests the successful persistence of a journal request.
   * 
   * @throws Exception
   */
  public void runTestPersistRequest() throws Exception {
    checkTitleRowCount(4);
    checkRequestRowCount(0);

    CounterReportsJournal journal =
	new CounterReportsJournal("Journal1", "Publisher1", null, null, null,
	    "1234-5678", "9876-5432");
    journal.identify();

    Map<String, Object> requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsJournal.PUBLICATION_YEAR_KEY, "1954");
    requestData.put(CounterReportsJournal.IS_HTML_KEY, Boolean.TRUE);
    requestData.put(CounterReportsJournal.IS_PDF_KEY, Boolean.FALSE);
    requestData.put(CounterReportsJournal.IS_PUBLISHER_INVOLVED_KEY,
	Boolean.TRUE);

    Connection conn = null;
    boolean success = false;

    try {
      conn = dbManager.getConnection();
      journal.persistRequest(requestData, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(4);
    checkJournal(journal);
    checkRequestRowCount(1);
    checkRequestRowCountById(journal.getLockssId(), 1);
    checkRequest(journal, requestData);
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
   * Checks the expected count of rows in the request table for a given LOCKSS
   * identifier.
   * 
   * @param expected
   *          An int with the expected number of rows in the table.
   * @throws SQLException
   */
  private void checkRequestRowCountById(long lockssId, int expected)
      throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_REQUEST_BY_ID_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = conn.prepareStatement(sql);
      statement.setLong(1, lockssId);
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
   * Checks the expected properties of a journal in the database.
   * 
   * @param book
   *          A CounterReportsJournal with the journal to be checked.
   * @param requestData
   *          A Map<String, Object> with the data of the request to be checked.
   * @throws SQLException
   */
  private void checkRequest(CounterReportsJournal journal,
      Map<String, Object> requestData) throws SQLException {
    if (requestData == null) {
      requestData = new HashMap<String, Object>();
    }

    Calendar cal = Calendar.getInstance();
    cal.setTime(TimeBase.nowDate());

    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String sql = SQL_QUERY_JOURNAL_REQUEST_SELECT;

    try {
      conn = dbManager.getConnection();

      statement = conn.prepareStatement(sql);
      statement.setLong(1, journal.getLockssId());
      resultSet = statement.executeQuery();

      assertEquals(true, resultSet.next());
      if (requestData.get(CounterReportsJournal.PUBLICATION_YEAR_KEY) != null) {
	assertEquals(
	    ((String) requestData
		.get(CounterReportsJournal.PUBLICATION_YEAR_KEY)),
	    resultSet.getString(SQL_COLUMN_PUBLICATION_YEAR));
      } else {
	assertNull(resultSet.getString(SQL_COLUMN_PUBLICATION_YEAR));
      }

      if (requestData.get(CounterReportsJournal.IS_HTML_KEY) != null) {
	assertEquals(
	    ((Boolean) requestData.get(CounterReportsJournal.IS_HTML_KEY))
		.booleanValue(),
	    resultSet.getBoolean(SQL_COLUMN_IS_HTML));
      } else {
	assertEquals(false, resultSet.getBoolean(SQL_COLUMN_IS_HTML));
      }

      if (requestData.get(CounterReportsJournal.IS_PDF_KEY) != null) {
	assertEquals(
	    ((Boolean) requestData.get(CounterReportsJournal.IS_PDF_KEY))
		.booleanValue(),
	    resultSet.getBoolean(SQL_COLUMN_IS_PDF));
      } else {
	assertEquals(false, resultSet.getBoolean(SQL_COLUMN_IS_PDF));
      }

      if (requestData.get(CounterReportsBook.IS_PUBLISHER_INVOLVED_KEY) != null) {
	assertEquals(
	    ((Boolean) requestData.get(CounterReportsBook.IS_PUBLISHER_INVOLVED_KEY))
		.booleanValue(), resultSet
		.getBoolean(SQL_COLUMN_IS_PUBLISHER_INVOLVED));
      } else {
	assertEquals(false,
	    resultSet.getBoolean(SQL_COLUMN_IS_PUBLISHER_INVOLVED));
      }

      assertEquals(cal.get(Calendar.YEAR),
	  resultSet.getShort(SQL_COLUMN_REQUEST_YEAR));
      assertEquals(cal.get(Calendar.MONTH) + 1,
	  resultSet.getShort(SQL_COLUMN_REQUEST_MONTH));
      assertEquals(cal.get(Calendar.DAY_OF_MONTH),
	  resultSet.getShort(SQL_COLUMN_REQUEST_DAY));
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Tests the successful creation of a journal request with no request data.
   * 
   * @throws Exception
   */
  public void runTestNoRequestDataPersistRequest() throws Exception {
    checkTitleRowCount(4);
    checkRequestRowCount(1);

    CounterReportsJournal journal =
	new CounterReportsJournal("Journal1", "Publisher1", null, null, null,
	    "1234-5678", "9876-5432");
    journal.identify();

    Connection conn = null;
    boolean success = false;

    try {
      conn = dbManager.getConnection();
      journal.persistRequest(null, conn);
      success = true;
    } catch (NullPointerException npe) {
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(4);
    checkJournal(journal);
    checkRequestRowCount(2);
    checkRequestRowCountById(journal.getLockssId(), 2);

    Map<String, Object> requestData = new HashMap<String, Object>();
    success = false;

    try {
      conn = dbManager.getConnection();
      journal.persistRequest(requestData, conn);
      success = true;
    } catch (NullPointerException npe) {
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(4);
    checkRequestRowCount(3);
    checkRequestRowCountById(journal.getLockssId(), 3);
  }

  /**
   * Tests the successful creation of multiple journal requests.
   * 
   * @throws Exception
   */
  public void runTestPersistMultipleRequests() throws Exception {
    checkTitleRowCount(4);
    checkRequestRowCount(3);

    CounterReportsJournal journal1 =
	new CounterReportsJournal("Journal1", "Publisher1", null, null, null,
	    "1234-5678", "9876-5432");
    journal1.identify();

    CounterReportsJournal journal2 =
	new CounterReportsJournal("Journal2", "Publisher2", null, null, null,
	    "9876-5432", "1234-5678");
    journal2.identify();

    Map<String, Object> requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsJournal.PUBLICATION_YEAR_KEY, "1954");
    requestData.put(CounterReportsJournal.IS_HTML_KEY, Boolean.FALSE);
    requestData.put(CounterReportsJournal.IS_PDF_KEY, Boolean.TRUE);
    requestData.put(CounterReportsJournal.IS_PUBLISHER_INVOLVED_KEY,
	Boolean.TRUE);

    Connection conn = null;
    boolean success = false;

    try {
      conn = dbManager.getConnection();
      journal1.persistRequest(requestData, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(5);
    checkJournal(journal1);
    checkRequestRowCount(4);
    checkRequestRowCountById(journal1.getLockssId(), 4);

    try {
      conn = dbManager.getConnection();
      journal2.persistRequest(requestData, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(5);
    checkJournal(journal2);
    checkRequestRowCount(5);
    checkRequestRowCountById(journal2.getLockssId(), 1);
    checkRequest(journal2, requestData);

    try {
      conn = dbManager.getConnection();
      journal1.persistRequest(requestData, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(5);
    checkRequestRowCount(6);
    checkRequestRowCountById(journal1.getLockssId(), 5);

    try {
      conn = dbManager.getConnection();
      journal1.persistRequest(null, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(5);
    checkRequestRowCount(7);
    checkRequestRowCountById(journal1.getLockssId(), 6);

    try {
      conn = dbManager.getConnection();
      journal2.persistRequest(requestData, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(5);
    checkRequestRowCount(8);
    checkRequestRowCountById(journal2.getLockssId(), 2);

    try {
      conn = dbManager.getConnection();
      journal2.persistRequest(null, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(5);
    checkRequestRowCount(9);
    checkRequestRowCountById(journal2.getLockssId(), 3);

    try {
      conn = dbManager.getConnection();
      journal1.persistRequest(requestData, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(5);
    checkRequestRowCount(10);
    checkRequestRowCountById(journal1.getLockssId(), 7);

    try {
      conn = dbManager.getConnection();
      journal1.persistRequest(requestData, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(5);
    checkRequestRowCount(11);
    checkRequestRowCountById(journal1.getLockssId(), 8);

    try {
      conn = dbManager.getConnection();
      journal2.persistRequest(null, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(5);
    checkRequestRowCount(12);
    checkRequestRowCountById(journal2.getLockssId(), 4);

    try {
      conn = dbManager.getConnection();
      journal2.persistRequest(null, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(5);
    checkRequestRowCount(13);
    checkRequestRowCountById(journal2.getLockssId(), 5);

    try {
      conn = dbManager.getConnection();
      journal2.persistRequest(null, conn);
      success = true;
    } finally {
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkTitleRowCount(5);
    checkRequestRowCount(14);
    checkRequestRowCountById(journal2.getLockssId(), 6);
  }

  /**
   * Tests the failure to create a journal with no title.
   * 
   * @throws Exception
   */
  public void runTestNoTitleFailure() throws Exception {
    CounterReportsJournal journal = null;
    try {
      journal =
	  new CounterReportsJournal(null, "Publisher1", null, null, null,
	      "1234-5678", "9876-5432");
      fail("Invalid null journal title");
    } catch (IllegalArgumentException iae) {
    }

    assertNull(journal);

    try {
      journal =
	  new CounterReportsJournal("", "Publisher1", null, null, null,
	      "1234-5678", "9876-5432");
      fail("Invalid empty journal title");
    } catch (IllegalArgumentException iae) {
    }

    assertNull(journal);

    try {
      journal =
	  new CounterReportsJournal(" ", "Publisher1", null, null, null,
	      "1234-5678", "9876-5432");
      fail("Invalid empty journal title");
    } catch (IllegalArgumentException iae) {
    }

    assertNull(journal);
  }
}
