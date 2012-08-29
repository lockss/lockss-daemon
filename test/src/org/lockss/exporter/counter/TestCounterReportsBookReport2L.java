/*
 * $Id: TestCounterReportsBookReport2L.java,v 1.2 2012-08-29 23:07:12 fergaloy-sf Exp $
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
 * Test class for org.lockss.exporter.counter.CounterReportsBookReport2L.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
package org.lockss.exporter.counter;

import static org.lockss.exporter.counter.CounterReportsManager.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.lockss.config.ConfigManager;
import org.lockss.daemon.Cron;
import org.lockss.db.DbManager;
import org.lockss.exporter.counter.CounterReportsBookReport2L;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.IOUtil;
import org.lockss.util.TimeBase;

public class TestCounterReportsBookReport2L extends LockssTestCase {
  // Query to add a type aggregation.
  private static final String SQL_QUERY_TYPE_AGGREGATION_INSERT = "insert into "
      + SQL_TABLE_TYPE_AGGREGATES
      + " (" + SQL_COLUMN_LOCKSS_ID
      + "," + SQL_COLUMN_IS_PUBLISHER_INVOLVED
      + "," + SQL_COLUMN_REQUEST_YEAR
      + "," + SQL_COLUMN_REQUEST_MONTH
      + "," + SQL_COLUMN_FULL_BOOK_REQUESTS
      + "," + SQL_COLUMN_SECTION_BOOK_REQUESTS
      + ") values (?,?,?,?,?,?)";

  // Query to delete a type aggregation.
  private static final String SQL_QUERY_TYPE_AGGREGATION_DELETE =
      "delete from " + SQL_TABLE_TYPE_AGGREGATES;

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

    counterReportsManager = new CounterReportsManager();
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
    runTestValidation();
    runTestEmptyReport();
    runTestDefaultPeriodReport();
    runTestCustomPeriodReport();
  }

  /**
   * Tests the validation of the constructor parameters.
   * 
   * @throws Exception
   */
  public void runTestValidation() throws Exception {

    try {
      new CounterReportsBookReport2L(theDaemon, 0, 2011, 7, 2012);
      fail("Invalid start month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsBookReport2L(theDaemon, 13, 2011, 7, 2012);
      fail("Invalid start month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsBookReport2L(theDaemon, 1, 2011, 0, 2012);
      fail("Invalid end month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsBookReport2L(theDaemon, 1, 2011, 13, 2012);
      fail("Invalid end month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsBookReport2L(theDaemon, 1, 2012, 12, 2011);
      fail("Invalid report period - End must not precede start");
    } catch (IllegalArgumentException iae) {
    }

    boolean validArgument = false;

    try {
      new CounterReportsBookReport2L(theDaemon, 1, 2012, 1, 2012);
      validArgument = true;
    } catch (IllegalArgumentException iae) {
    }

    assertEquals(true, validArgument);
  }

  /**
   * Tests an empty report.
   * 
   * @throws Exception
   */
  public void runTestEmptyReport() throws Exception {
    CounterReportsBookReport2L report =
	new CounterReportsBookReport2L(theDaemon);

    report.logReport();
    report.saveCsvReport();
    File reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("csv"));
    assertEquals(true, reportFile.exists());

    report.populateReportHeaderEntries();

    BufferedReader reader = new BufferedReader(new FileReader(reportFile));
    String line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals(
	"\"Total for all books\",,,,,,,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());

    report.saveTsvReport();
    reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("txt"));
    assertEquals(true, reportFile.exists());

    reader = new BufferedReader(new FileReader(reportFile));
    line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals(
	"Total for all books\t\t\t\t\t\t\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());
  }

  /**
   * Tests a report for the default period.
   * 
   * @throws Exception
   */
  public void runTestDefaultPeriodReport() throws Exception {

    CounterReportsBook book =
	new CounterReportsBook("Book1", "Publisher1", null, null, null,
	    "987-654321-0987", "1234-5678");
    book.identify();

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(TimeBase.nowDate());
    calendar.add(Calendar.MONTH, -15);

    Map<String, Object> requestData = new HashMap<String, Object>();
    requestData.put(SQL_COLUMN_IS_PUBLISHER_INVOLVED, Boolean.FALSE);
    requestData.put(SQL_COLUMN_REQUEST_YEAR,
	Short.valueOf((short) calendar.get(Calendar.YEAR)));
    requestData.put(SQL_COLUMN_REQUEST_MONTH,
	Short.valueOf((short) calendar.get(Calendar.MONTH)));
    requestData.put(SQL_COLUMN_FULL_BOOK_REQUESTS, Integer.valueOf(10));
    requestData.put(SQL_COLUMN_SECTION_BOOK_REQUESTS, Integer.valueOf(6));

    persistTypeAggregation(book, requestData);

    CounterReportsBookReport2L report =
	new CounterReportsBookReport2L(theDaemon);

    report.logReport();
    report.saveCsvReport();
    File reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("csv"));
    assertEquals(true, reportFile.exists());

    report.populateReportHeaderEntries();

    BufferedReader reader = new BufferedReader(new FileReader(reportFile));
    String line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals(
	"\"Total for all books\",,,,,,,6,0,0,0,0,0,0,0,0,6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
	line);
    line = reader.readLine();
    assertEquals(
	"Book1,Publisher1,,,,987-654321-0987,1234-5678,6,0,0,0,0,0,0,0,0,6,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());

    report.saveTsvReport();
    reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("txt"));
    assertEquals(true, reportFile.exists());

    reader = new BufferedReader(new FileReader(reportFile));
    line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals(
	"Total for all books\t\t\t\t\t\t\t6\t0\t0\t0\t0\t0\t0\t0\t0\t6\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0",
	line);
    line = reader.readLine();
    assertEquals(
	"Book1\tPublisher1\t\t\t\t987-654321-0987\t1234-5678\t6\t0\t0\t0\t0\t0\t0\t0\t0\t6\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());
  }

  /**
   * Persists a type aggregation.
   * 
   * @param book
   *          A CounterReportsBook with the book data for which the request is
   *          persisted.
   * @param requestData
   *          A Map<String, Object> with properties of the request to be
   *          persisted.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  public void persistTypeAggregation(CounterReportsBook book,
      Map<String, Object> requestData) throws SQLException {
    Connection conn = null;
    boolean success = false;

    PreparedStatement deleteAggregation = null;
    PreparedStatement insertAggregation = null;

    try {
      conn = dbManager.getConnection();

      String sql = SQL_QUERY_TYPE_AGGREGATION_DELETE;
      deleteAggregation = conn.prepareStatement(sql);
      deleteAggregation.executeUpdate();

      sql = SQL_QUERY_TYPE_AGGREGATION_INSERT;
      insertAggregation = conn.prepareStatement(sql);

      short index = 1;
      insertAggregation.setLong(index++, book.getLockssId());
      insertAggregation.setBoolean(index++,
	  (Boolean) requestData.get(SQL_COLUMN_IS_PUBLISHER_INVOLVED));
      insertAggregation.setShort(index++,
	  (Short) requestData.get(SQL_COLUMN_REQUEST_YEAR));
      insertAggregation.setShort(index++,
	  (Short) requestData.get(SQL_COLUMN_REQUEST_MONTH));
      insertAggregation.setInt(index++,
	  (Integer) requestData.get(SQL_COLUMN_FULL_BOOK_REQUESTS));
      insertAggregation.setInt(index++,
	  (Integer) requestData.get(SQL_COLUMN_SECTION_BOOK_REQUESTS));

      insertAggregation.executeUpdate();
      DbManager.safeCloseStatement(insertAggregation);
      insertAggregation = conn.prepareStatement(sql);

      index = 1;
      insertAggregation.setLong(index++,
	  counterReportsManager.getAllBooksLockssId());
      insertAggregation.setBoolean(index++,
	  (Boolean) requestData.get(SQL_COLUMN_IS_PUBLISHER_INVOLVED));
      insertAggregation.setShort(index++,
	  (Short) requestData.get(SQL_COLUMN_REQUEST_YEAR));
      insertAggregation.setShort(index++,
	  (Short) requestData.get(SQL_COLUMN_REQUEST_MONTH));
      insertAggregation.setInt(index++,
	  (Integer) requestData.get(SQL_COLUMN_FULL_BOOK_REQUESTS));
      insertAggregation.setInt(index++,
	  (Integer) requestData.get(SQL_COLUMN_SECTION_BOOK_REQUESTS));

      insertAggregation.executeUpdate();
      success = true;
    } finally {
      DbManager.safeCloseStatement(insertAggregation);
      if (success) {
	conn.commit();
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }
  }

  /**
   * Tests a report for a custom period.
   * 
   * @throws Exception
   */
  public void runTestCustomPeriodReport() throws Exception {

    CounterReportsBook book =
	new CounterReportsBook("Book1", "Publisher1", null, null, null,
	    "987-654321-0987", "1234-5678");
    book.identify();

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(TimeBase.nowDate());
    int endYear = calendar.get(Calendar.YEAR);
    int endMonth = calendar.get(Calendar.MONTH);
    calendar.add(Calendar.MONTH, -4);
    int startYear = calendar.get(Calendar.YEAR);
    int startMonth = calendar.get(Calendar.MONTH);

    calendar.add(Calendar.MONTH, 2);

    Map<String, Object> requestData = new HashMap<String, Object>();
    requestData.put(SQL_COLUMN_IS_PUBLISHER_INVOLVED, Boolean.TRUE);
    requestData.put(SQL_COLUMN_REQUEST_YEAR,
	Short.valueOf((short) calendar.get(Calendar.YEAR)));
    requestData.put(SQL_COLUMN_REQUEST_MONTH,
	Short.valueOf((short) calendar.get(Calendar.MONTH)));
    requestData.put(SQL_COLUMN_FULL_BOOK_REQUESTS, Integer.valueOf(10));
    requestData.put(SQL_COLUMN_SECTION_BOOK_REQUESTS, Integer.valueOf(6));

    persistTypeAggregation(book, requestData);

    CounterReportsBookReport2L report =
	new CounterReportsBookReport2L(theDaemon, startMonth, startYear,
	    endMonth, endYear);

    report.logReport();
    report.saveCsvReport();
    File reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("csv"));
    assertEquals(true, reportFile.exists());

    report.populateReportHeaderEntries();

    BufferedReader reader = new BufferedReader(new FileReader(reportFile));
    String line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals("\"Total for all books\",,,,,,,6,0,0,6,0,0", line);
    line = reader.readLine();
    assertEquals("Book1,Publisher1,,,,987-654321-0987,1234-5678,6,0,0,6,0,0",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());

    report.saveTsvReport();
    reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("txt"));
    assertEquals(true, reportFile.exists());

    reader = new BufferedReader(new FileReader(reportFile));
    line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals("Total for all books\t\t\t\t\t\t\t6\t0\t0\t6\t0\t0", line);
    line = reader.readLine();
    assertEquals(
	"Book1\tPublisher1\t\t\t\t987-654321-0987\t1234-5678\t6\t0\t0\t6\t0\t0",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());
  }
}
