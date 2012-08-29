/*
 * $Id: TestCounterReportsBook.java,v 1.2 2012-08-29 23:07:12 fergaloy-sf Exp $
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
 * Test class for org.lockss.exporter.counter.CounterReportsBook.
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
import org.lockss.exporter.counter.CounterReportsBook;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.TimeBase;

public class TestCounterReportsBook extends LockssTestCase {
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

  // Query to get the properties of a book.
  private static final String SQL_QUERY_BOOK_SELECT = "select "
      + SQL_COLUMN_TITLE_NAME + ","
      + SQL_COLUMN_PUBLISHER_NAME + ","
      + SQL_COLUMN_PLATFORM_NAME + ","
      + SQL_COLUMN_DOI + ","
      + SQL_COLUMN_PROPRIETARY_ID + ","
      + SQL_COLUMN_IS_BOOK + ","
      + SQL_COLUMN_ISBN + ","
      + SQL_COLUMN_BOOK_ISSN
      + " from " + SQL_TABLE_TITLES
      + " where " + SQL_COLUMN_LOCKSS_ID + " = ?";

  // Query to get the properties of a book request.
  private static final String SQL_QUERY_BOOK_REQUEST_SELECT = "select "
      + SQL_COLUMN_IS_SECTION + ","
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
   * Tests the successful creation of a book.
   * 
   * @throws Exception
   */
  public void runTestCreation() throws Exception {
    CounterReportsBook book =
	new CounterReportsBook("Book1", "Publisher1", null, "02468", null,
	    "987-654321-0987", "1234-5678");

    assertEquals("Book1", book.getName());
    assertEquals("Publisher1", book.getPublisherName());
    assertEquals("02468", book.getDoi());
    assertEquals("987-654321-0987", book.getIsbn());
    assertEquals("1234-5678", book.getIssn());
    book.identify();
    assertEquals(-2709344707106576683L, book.getLockssId());
  }

  /**
   * Tests the successful persistence of a book.
   * 
   * @throws Exception
   */
  public void runTestPersist() throws Exception {
    checkTitleRowCount(3);

    CounterReportsBook book =
	new CounterReportsBook("Book1", "Publisher1", null, null, null,
	    "987-654321-0987", "1234-5678");
    book.identify();

    checkTitleRowCount(4);
    checkBook(book);
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
   * Checks the expected properties of a book in the database.
   * 
   * @param book
   *          A CounterReportsBook with the book to be checked.
   * @throws SQLException
   */
  private void checkBook(CounterReportsBook book) throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String sql = SQL_QUERY_BOOK_SELECT;

    try {
      conn = dbManager.getConnection();

      statement = conn.prepareStatement(sql);
      statement.setLong(1, book.getLockssId());
      resultSet = statement.executeQuery();

      assertEquals(true, resultSet.next());
      assertEquals(book.getName(), resultSet.getString(SQL_COLUMN_TITLE_NAME));
      assertEquals(book.getPublisherName(),
	  resultSet.getString(SQL_COLUMN_PUBLISHER_NAME));
      assertEquals(book.getPublishingPlatform(),
	  resultSet.getString(SQL_COLUMN_PLATFORM_NAME));
      assertEquals(book.getDoi(), resultSet.getString(SQL_COLUMN_DOI));
      assertEquals(book.getProprietaryId(),
	  resultSet.getString(SQL_COLUMN_PROPRIETARY_ID));
      assertEquals(true, resultSet.getBoolean(SQL_COLUMN_IS_BOOK));
      assertEquals(book.getIsbn(), resultSet.getString(SQL_COLUMN_ISBN));
      assertEquals(book.getIssn(), resultSet.getString(SQL_COLUMN_BOOK_ISSN));
      assertEquals(false, resultSet.next());
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Tests the successful persistence of a book request.
   * 
   * @throws Exception
   */
  public void runTestPersistRequest() throws Exception {
    checkTitleRowCount(4);
    checkRequestRowCount(0);

    CounterReportsBook book =
	new CounterReportsBook("Book1", "Publisher1", null, null, null,
	    "987-654321-0987", "1234-5678");
    book.identify();

    Map<String, Object> requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsBook.IS_SECTION_KEY, Boolean.TRUE);
    requestData.put(CounterReportsBook.IS_PUBLISHER_INVOLVED_KEY, Boolean.TRUE);

    Connection conn = null;
    boolean success = false;

    try {
      conn = dbManager.getConnection();
      book.persistRequest(requestData, conn);
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
    checkBook(book);
    checkRequestRowCount(1);
    checkRequestRowCountById(book.getLockssId(), 1);
    checkRequest(book, requestData);
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
   * Checks the expected properties of a book in the database.
   * 
   * @param book
   *          A CounterReportsBook with the book to be checked.
   * @param requestData
   *          A Map<String, Object> with the data of the request to be checked.
   * @throws SQLException
   */
  private void checkRequest(CounterReportsBook book,
      Map<String, Object> requestData) throws SQLException {
    if (requestData == null) {
      requestData = new HashMap<String, Object>();
    }

    Calendar cal = Calendar.getInstance();
    cal.setTime(TimeBase.nowDate());

    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String sql = SQL_QUERY_BOOK_REQUEST_SELECT;

    try {
      conn = dbManager.getConnection();

      statement = conn.prepareStatement(sql);
      statement.setLong(1, book.getLockssId());
      resultSet = statement.executeQuery();

      assertEquals(true, resultSet.next());

      if (requestData.get(CounterReportsBook.IS_SECTION_KEY) != null) {
	assertEquals(
	    ((Boolean) requestData.get(CounterReportsBook.IS_SECTION_KEY))
		.booleanValue(),
	    resultSet.getBoolean(SQL_COLUMN_IS_SECTION));
      } else {
	assertEquals(false, resultSet.getBoolean(SQL_COLUMN_IS_SECTION));
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
   * Tests the successful creation of a book request with no request data.
   * 
   * @throws Exception
   */
  public void runTestNoRequestDataPersistRequest() throws Exception {
    checkTitleRowCount(4);
    checkRequestRowCount(1);

    CounterReportsBook book =
	new CounterReportsBook("Book1", "Publisher1", null, null, null,
	    "987-654321-0987", "1234-5678");
    book.identify();

    Connection conn = null;
    boolean success = false;

    try {
      conn = dbManager.getConnection();
      book.persistRequest(null, conn);
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
    checkBook(book);
    checkRequestRowCount(2);
    checkRequestRowCountById(book.getLockssId(), 2);

    Map<String, Object> requestData = new HashMap<String, Object>();
    success = false;

    try {
      conn = dbManager.getConnection();
      book.persistRequest(requestData, conn);
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
    checkRequestRowCountById(book.getLockssId(), 3);
  }

  /**
   * Tests the successful creation of multiple book requests.
   * 
   * @throws Exception
   */
  public void runTestPersistMultipleRequests() throws Exception {
    checkTitleRowCount(4);
    checkRequestRowCount(3);

    CounterReportsBook book1 =
	new CounterReportsBook("Book1", "Publisher1", null, null, null,
	    "987-654321-0987", "1234-5678");
    book1.identify();

    CounterReportsBook book2 =
	new CounterReportsBook("Book2", "Publisher2", null, null, null,
	    "123-456789-0123", "9876-5432");
    book2.identify();

    Map<String, Object> requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsBook.IS_SECTION_KEY, Boolean.TRUE);
    requestData.put(CounterReportsBook.IS_PUBLISHER_INVOLVED_KEY, Boolean.TRUE);

    Connection conn = null;
    boolean success = false;

    try {
      conn = dbManager.getConnection();
      book1.persistRequest(requestData, conn);
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
    checkBook(book1);
    checkRequestRowCount(4);
    checkRequestRowCountById(book1.getLockssId(), 4);
    checkRequest(book1, requestData);

    try {
      conn = dbManager.getConnection();
      book2.persistRequest(requestData, conn);
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
    checkBook(book2);
    checkRequestRowCount(5);
    checkRequestRowCountById(book2.getLockssId(), 1);
    checkRequest(book2, requestData);

    try {
      conn = dbManager.getConnection();
      book1.persistRequest(requestData, conn);
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
    checkRequestRowCountById(book1.getLockssId(), 5);

    try {
      conn = dbManager.getConnection();
      book1.persistRequest(null, conn);
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
    checkRequestRowCountById(book1.getLockssId(), 6);

    try {
      conn = dbManager.getConnection();
      book2.persistRequest(requestData, conn);
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
    checkRequestRowCountById(book2.getLockssId(), 2);

    try {
      conn = dbManager.getConnection();
      book2.persistRequest(null, conn);
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
    checkRequestRowCountById(book2.getLockssId(), 3);

    try {
      conn = dbManager.getConnection();
      book1.persistRequest(requestData, conn);
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
    checkRequestRowCountById(book1.getLockssId(), 7);

    try {
      conn = dbManager.getConnection();
      book1.persistRequest(requestData, conn);
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
    checkRequestRowCountById(book1.getLockssId(), 8);

    try {
      conn = dbManager.getConnection();
      book2.persistRequest(null, conn);
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
    checkRequestRowCountById(book2.getLockssId(), 4);

    try {
      conn = dbManager.getConnection();
      book2.persistRequest(null, conn);
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
    checkRequestRowCountById(book2.getLockssId(), 5);

    try {
      conn = dbManager.getConnection();
      book2.persistRequest(null, conn);
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
    checkRequestRowCountById(book2.getLockssId(), 6);
  }

  /**
   * Tests the failure to create a book with no title.
   * 
   * @throws Exception
   */
  public void runTestNoTitleFailure() throws Exception {
    CounterReportsBook book = null;

    try {
      book =
	  new CounterReportsBook(null, "Publisher1", null, null, null,
	      "987-654321-0987", "1234-5678");
      fail("Invalid null book title");
    } catch (IllegalArgumentException iae) {
    }

    assertNull(book);

    try {
      book =
	  new CounterReportsBook("", "Publisher1", null, null, null,
	      "987-654321-0987", "1234-5678");
      fail("Invalid empty book title");
    } catch (IllegalArgumentException iae) {
    }

    assertNull(book);

    try {
      book =
	  new CounterReportsBook(" ", "Publisher1", null, null, null,
	      "987-654321-0987", "1234-5678");
      fail("Invalid empty book title");
    } catch (IllegalArgumentException iae) {
    }

    assertNull(book);
  }
}
