/*
 * $Id: TestCounterReportsRequestAggregator.java,v 1.3 2012-09-28 00:13:23 fergaloy-sf Exp $
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
 * Test class for org.lockss.exporter.counter.CounterReportsRequestAggregator.
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
import org.lockss.exporter.counter.CounterReportsJournal;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.exporter.counter.CounterReportsRequestAggregator;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.TimeBase;

public class TestCounterReportsRequestAggregator extends LockssTestCase {
  // Query to count all the request rows.
  private static final String SQL_QUERY_REQUEST_COUNT = "select count(*) from "
      + SQL_TABLE_REQUESTS;

  // Query to count all the rows of type aggregated totals.
  private static final String SQL_QUERY_TYPE_AGGREGATED_TOTAL_COUNT =
      "select count(*) from " + SQL_TABLE_TYPE_AGGREGATES;

  // Query to count monthly rows of type aggregated totals.
  private static final String SQL_QUERY_TYPE_AGGREGATED_MONTH_COUNT = "select "
      + "count(*) "
      + "from " + SQL_TABLE_TYPE_AGGREGATES
      + " where " + SQL_COLUMN_REQUEST_YEAR + " = ? "
      + "and " + SQL_COLUMN_REQUEST_MONTH + " = ? "
      + "and " + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ?";

  // Query to get aggregated type book request counts for a month.
  private static final String SQL_QUERY_TYPE_AGGREGATED_MONTH_BOOK_SELECT = "select "
      + SQL_COLUMN_FULL_BOOK_REQUESTS + ","
      + SQL_COLUMN_SECTION_BOOK_REQUESTS
      + " from " + SQL_TABLE_TYPE_AGGREGATES
      + " where " + SQL_COLUMN_LOCKSS_ID + " = ? "
      + "and " + SQL_COLUMN_REQUEST_YEAR + " = ? "
      + "and " + SQL_COLUMN_REQUEST_MONTH + " = ? "
      + "and " + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ?";

  // Query to get aggregated type journal request counts for a month.
  private static final String SQL_QUERY_TYPE_AGGREGATED_MONTH_JOURNAL_SELECT = "select "
      + SQL_COLUMN_HTML_JOURNAL_REQUESTS + ","
      + SQL_COLUMN_PDF_JOURNAL_REQUESTS + ","
      + SQL_COLUMN_TOTAL_JOURNAL_REQUESTS
      + " from " + SQL_TABLE_TYPE_AGGREGATES
      + " where " + SQL_COLUMN_LOCKSS_ID + " = ? "
      + "and " + SQL_COLUMN_REQUEST_YEAR + " = ? "
      + "and " + SQL_COLUMN_REQUEST_MONTH + " = ? "
      + "and " + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ?";

  // Query to count all the rows of publication year aggregated totals.
  private static final String SQL_QUERY_PUBYEAR_AGGREGATED_TOTAL_COUNT =
      "select count(*) from " + SQL_TABLE_PUBYEAR_AGGREGATES;

  // Query to get aggregated publication year journal request counts for a
  // month.
  private static final String SQL_QUERY_PUBYEAR_AGGREGATED_MONTH_JOURNAL_SELECT = "select "
      + SQL_COLUMN_PUBLICATION_YEAR + ","
      + SQL_COLUMN_REQUEST_COUNT
      + " from " + SQL_TABLE_PUBYEAR_AGGREGATES
      + " where " + SQL_COLUMN_LOCKSS_ID + " = ? "
      + "and " + SQL_COLUMN_REQUEST_YEAR + " = ? "
      + "and " + SQL_COLUMN_REQUEST_MONTH + " = ? "
      + "and " + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ?";

  private MockLockssDaemon theDaemon;
  private DbManager dbManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath();

    // Set the database log.
    System.setProperty("derby.stream.error.file",
		       new File(tempDirPath, "derby.log").getAbsolutePath());

    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props
	.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(CounterReportsManager.PARAM_COUNTER_ENABLED, "true");
    props.setProperty(CounterReportsManager.PARAM_REPORT_BASEDIR_PATH,
		      tempDirPath);
    props
	.setProperty(CounterReportsRequestAggregator
	             .PARAM_COUNTER_REQUEST_AGGREGATION_TASK_FREQUENCY,
		     "hourly");
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
    runTestEmptyAggregation();
    runTestMonthBookAggregation();
    runTestMonthJournalAggregation();
    runTestNextTime();
  }

  /**
   * Tests the aggregation of an empty system.
   * 
   * @throws Exception
   */
  public void runTestEmptyAggregation() throws Exception {
    CounterReportsRequestAggregator aggregator =
	new CounterReportsRequestAggregator(theDaemon);
    aggregator.getCronTask().execute();

    checkTypeAggregatedRowCount(0);
    checkPublicationYearAggregatedRowCount(0);
  }

  /**
   * Checks the expected count of rows in the aggregation by type table.
   * 
   * @param expected
   *          An int with the expected number of rows in the table.
   * @throws SQLException
   */
  private void checkTypeAggregatedRowCount(int expected) throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_TYPE_AGGREGATED_TOTAL_COUNT;

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
    }

    assertEquals(expected, count);
  }

  /**
   * Checks the expected count of rows in the aggregation by publication year
   * table.
   * 
   * @param expected
   *          An int with the expected number of rows in the table.
   * @throws SQLException
   */
  private void checkPublicationYearAggregatedRowCount(int expected)
      throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_PUBYEAR_AGGREGATED_TOTAL_COUNT;

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
    }

    assertEquals(expected, count);
  }

  /**
   * Tests the monthly aggregation of book requests.
   * 
   * @throws Exception
   */
  public void runTestMonthBookAggregation() throws Exception {
    Map<String, Object> requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsBook.IS_SECTION_KEY, Boolean.TRUE);
    requestData.put(CounterReportsBook.IS_PUBLISHER_INVOLVED_KEY, Boolean.TRUE);

    CounterReportsBook book =
	new CounterReportsBook("Book1", "Publisher1", null, null, null,
			       "123-456789-0123", "9876-5432");
    book.identify();

    Calendar cal = Calendar.getInstance();
    cal.setTime(TimeBase.nowDate());

    int requestMonth = cal.get(Calendar.MONTH) + 1;
    int requestYear = cal.get(Calendar.YEAR);
    Connection conn = null;
    boolean success = false;

    try {
      conn = dbManager.getConnection();
      book.persistRequest(requestData, conn);
      book.persistRequest(requestData, conn);
      book.persistRequest(requestData, conn);
      book.persistRequest(requestData, conn);
      book.persistRequest(requestData, conn);
      book.persistRequest(requestData, conn);

      success = true;
    } finally {
      if (success) {
	try {
	  conn.commit();
	  DbManager.safeCloseConnection(conn);
	} catch (SQLException sqle) {
	  success = false;
	  log.error("Exception caught committing the connection", sqle);
	  DbManager.safeRollbackAndClose(conn);
	}
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkRequestRowCount(6);

    CounterReportsRequestAggregator aggregator =
	new CounterReportsRequestAggregator(theDaemon);
    aggregator.getCronTask().execute();

    checkTypeAggregatedRowCount(5);
    checkTypeAggregatedRowCount(requestYear, requestMonth, Boolean.TRUE, 3);
    checkRequestRowCount(0);
    checkBookMonthlyTypeRequests(book.getLockssId(), requestYear, requestMonth,
				 Boolean.TRUE, 0, 6);
    checkPublicationYearAggregatedRowCount(0);
  }

  /**
   * Checks the expected count of rows in the requests table.
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
    }

    assertEquals(expected, count);
  }

  /**
   * Checks the expected count of monthly rows in the aggregation by type table.
   * 
   * @param requestYear
   *          An int with the year of the month to check.
   * @param requestMonth
   *          An int with the month to check.
   * @param isPublisherInvolved
   *          A Boolean with the indication of whether a publisher is involved
   *          in the requests to check.
   * @param expected
   *          An int with the expected number of monthly rows in the table.
   * @throws SQLException
   */
  private void checkTypeAggregatedRowCount(int requestYear, int requestMonth,
      Boolean isPublisherInvolved, int expected) throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_TYPE_AGGREGATED_MONTH_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = conn.prepareStatement(sql);
      statement.setInt(1, requestYear);
      statement.setInt(2, requestMonth);
      statement.setBoolean(3, isPublisherInvolved);
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	count = resultSet.getInt(1);
      }
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    assertEquals(expected, count);
  }

  /**
   * Checks the expected type counts of monthly requests for a book.
   * 
   * @param lockssId
   *          An long with the LOCKSS identifier of the book.
   * @param requestYear
   *          An int with the year of the month to check.
   * @param requestMonth
   *          An int with the month to check.
   * @param isPublisherInvolved
   *          A Boolean with the indication of whether a publisher is involved
   *          in the requests to check.
   * @param expectedFull
   *          An int with the expected number of full requests.
   * @param expectedSection
   *          An int with the expected number of section requests.
   * @throws SQLException
   */
  private void checkBookMonthlyTypeRequests(long lockssId, int requestYear,
      int requestMonth, Boolean isPublisherInvolved, int expectedFull,
      int expectedSection) throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int fullCount = -1;
    int sectionCount = -1;
    String sql = SQL_QUERY_TYPE_AGGREGATED_MONTH_BOOK_SELECT;

    try {
      conn = dbManager.getConnection();

      statement = conn.prepareStatement(sql);
      statement.setLong(1, lockssId);
      statement.setInt(2, requestYear);
      statement.setInt(3, requestMonth);
      statement.setBoolean(4, isPublisherInvolved);
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	fullCount = resultSet.getInt(SQL_COLUMN_FULL_BOOK_REQUESTS);
	sectionCount = resultSet.getInt(SQL_COLUMN_SECTION_BOOK_REQUESTS);
      }
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    assertEquals(expectedFull, fullCount);
    assertEquals(expectedSection, sectionCount);
  }

  /**
   * Tests the monthly aggregation of journal requests.
   * 
   * @throws Exception
   */
  public void runTestMonthJournalAggregation() throws Exception {
    Map<String, Object> requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsJournal.IS_HTML_KEY, Boolean.FALSE);
    requestData.put(CounterReportsJournal.IS_PDF_KEY, Boolean.TRUE);
    requestData.put(CounterReportsJournal.IS_PUBLISHER_INVOLVED_KEY,
		    Boolean.TRUE);
    requestData.put(CounterReportsJournal.PUBLICATION_YEAR_KEY, "2007");

    CounterReportsJournal journal =
	new CounterReportsJournal("Journal1", "Publisher1", null, null, null,
				  "9876-5432", "1234-5678");
    journal.identify();

    Calendar cal = Calendar.getInstance();
    cal.setTime(TimeBase.nowDate());

    int requestMonth = cal.get(Calendar.MONTH) + 1;
    int requestYear = cal.get(Calendar.YEAR);
    Connection conn = null;
    boolean success = false;

    try {
      conn = dbManager.getConnection();
      journal.persistRequest(requestData, conn);
      journal.persistRequest(requestData, conn);
      journal.persistRequest(requestData, conn);
      journal.persistRequest(requestData, conn);
      journal.persistRequest(requestData, conn);
      journal.persistRequest(requestData, conn);

      success = true;
    } finally {
      if (success) {
	try {
	  conn.commit();
	  DbManager.safeCloseConnection(conn);
	} catch (SQLException sqle) {
	  success = false;
	  log.error("Exception caught committing the connection", sqle);
	  DbManager.safeRollbackAndClose(conn);
	}
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    assertEquals(true, success);
    checkRequestRowCount(6);

    CounterReportsRequestAggregator aggregator =
	new CounterReportsRequestAggregator(theDaemon);
    aggregator.getCronTask().execute();

    checkTypeAggregatedRowCount(6);
    checkTypeAggregatedRowCount(requestYear, requestMonth, Boolean.TRUE, 4);
    checkJournalMonthlyTypeRequests(journal.getLockssId(), requestYear,
				    requestMonth, Boolean.TRUE, 0, 6, 6);
    checkPublicationYearAggregatedRowCount(1);
    checkJournalMonthlyPublicationYearRequests(journal.getLockssId(),
					       requestYear, requestMonth,
					       Boolean.TRUE, "2007", 6);
    checkRequestRowCount(0);
  }

  /**
   * Checks the expected type counts of monthly requests for a journal.
   * 
   * @param lockssId
   *          An long with the LOCKSS identifier of the journal.
   * @param requestYear
   *          An int with the year of the month to check.
   * @param requestMonth
   *          An int with the month to check.
   * @param isPublisherInvolved
   *          A Boolean with the indication of whether a publisher is involved
   *          in the requests to check.
   * @param expectedHtml
   *          An int with the expected number of HTML requests.
   * @param expectedPdf
   *          An int with the expected number of PDF requests.
   * @param expectedTotal
   *          An int with the expected number of total requests.
   * @throws SQLException
   */
  private void checkJournalMonthlyTypeRequests(long lockssId, int requestYear,
      int requestMonth, Boolean isPublisherInvolved, int expectedHtml,
      int expectedPdf, int expectedTotal) throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int htmlCount = -1;
    int pdfCount = -1;
    int totalCount = -1;
    String sql = SQL_QUERY_TYPE_AGGREGATED_MONTH_JOURNAL_SELECT;

    try {
      conn = dbManager.getConnection();

      statement = conn.prepareStatement(sql);
      statement.setLong(1, lockssId);
      statement.setInt(2, requestYear);
      statement.setInt(3, requestMonth);
      statement.setBoolean(4, isPublisherInvolved);
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	htmlCount = resultSet.getInt(SQL_COLUMN_HTML_JOURNAL_REQUESTS);
	pdfCount = resultSet.getInt(SQL_COLUMN_PDF_JOURNAL_REQUESTS);
	totalCount = resultSet.getInt(SQL_COLUMN_TOTAL_JOURNAL_REQUESTS);
      }
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    assertEquals(expectedHtml, htmlCount);
    assertEquals(expectedPdf, pdfCount);
    assertEquals(expectedTotal, totalCount);
  }

  /**
   * Checks the expected publication year count of monthly requests for a
   * journal.
   * 
   * @param lockssId
   *          An long with the LOCKSS identifier of the journal.
   * @param requestYear
   *          An int with the year of the month to check.
   * @param requestMonth
   *          An int with the month to check.
   * @param isPublisherInvolved
   *          A Boolean with the indication of whether a publisher is involved
   *          in the requests to check.
   * @param expectedPublicationYear
   *          A String with the expected publication year.
   * @param expectedCount
   *          An int with the expected count of requests.
   * @throws SQLException
   */
  private void checkJournalMonthlyPublicationYearRequests(long lockssId,
      int requestYear, int requestMonth, Boolean isPublisherInvolved,
      String expectedPublicationYear, int expectedCount) throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String pubYear = null;
    int count = -1;
    String sql = SQL_QUERY_PUBYEAR_AGGREGATED_MONTH_JOURNAL_SELECT;

    try {
      conn = dbManager.getConnection();

      statement = conn.prepareStatement(sql);
      statement.setLong(1, lockssId);
      statement.setInt(2, requestYear);
      statement.setInt(3, requestMonth);
      statement.setBoolean(4, isPublisherInvolved);
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	pubYear = resultSet.getString(SQL_COLUMN_PUBLICATION_YEAR);
	count = resultSet.getInt(SQL_COLUMN_REQUEST_COUNT);
      }
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    assertEquals(expectedPublicationYear, pubYear);
    assertEquals(expectedCount, count);
  }

  /**
   * Tests the next scheduled execution time.
   * 
   * @throws Exception
   */
  public void runTestNextTime() throws Exception {
    CounterReportsRequestAggregator aggregator =
	new CounterReportsRequestAggregator(theDaemon);

    long now = TimeBase.nowMs();
    Calendar cal1 = Calendar.getInstance();
    cal1.setTimeInMillis(now);
    cal1.add(Calendar.HOUR, 1);

    long next = aggregator.getCronTask().nextTime(now);
    Calendar cal2 = Calendar.getInstance();
    cal2.setTimeInMillis(next);

    assertEquals(cal1.get(Calendar.YEAR), cal2.get(Calendar.YEAR));
    assertEquals(cal1.get(Calendar.MONTH), cal2.get(Calendar.MONTH));
    assertEquals(cal1.get(Calendar.DAY_OF_MONTH),
		 cal2.get(Calendar.DAY_OF_MONTH));
    assertEquals(cal1.get(Calendar.HOUR_OF_DAY),
                 cal2.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, cal2.get(Calendar.MINUTE));
    assertEquals(0, cal2.get(Calendar.SECOND));
  }
}
