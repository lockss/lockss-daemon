/*
 * $Id: TestCounterReportsJournalReport5.java,v 1.8 2013-03-04 19:26:59 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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
 * Test class for org.lockss.exporter.counter.CounterReportsJournalReport5.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
package org.lockss.exporter.counter;

import static org.lockss.db.DbManager.*;
import static org.lockss.metadata.MetadataManager.PRIMARY_NAME_TYPE;
import static org.lockss.plugin.ArticleFiles.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;
import org.lockss.config.ConfigManager;
import org.lockss.daemon.Cron;
import org.lockss.db.DbManager;
import org.lockss.exporter.counter.CounterReportsJournalReport5;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.IOUtil;
import org.lockss.util.TimeBase;

public class TestCounterReportsJournalReport5 extends LockssTestCase {
  private static final String JOURNAL_URL = "http://example.com/journal.url";
  private static final String HTML_URL = "http://example.com/html.url";
  private static final String PDF_URL = "http://example.com/pdf.url";

  // Query to clean up the publication year aggregated request rows.
  private static final String SQL_QUERY_PUBYEAR_REQUEST_DELETE = "delete from "
      + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE;

  // Query to clean up the type aggregated request rows.
  private static final String SQL_QUERY_TYPE_REQUEST_DELETE = "delete from "
      + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE;

  // Query to clean up the test journal rows.
  private static final String SQL_QUERY_JOURNAL_DELETE = "delete from "
      + PUBLICATION_TABLE + " where publication_seq > 2";

  private MockLockssDaemon theDaemon;
  private DbManager dbManager;
  private MetadataManager metadataManager;
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
    props.setProperty(CounterReportsManager.PARAM_COUNTER_ENABLED, "true");
    props.setProperty(CounterReportsManager.PARAM_REPORT_BASEDIR_PATH,
	tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.setDaemonInited(true);

    dbManager = new DbManager();
    theDaemon.setDbManager(dbManager);
    dbManager.initService(theDaemon);
    dbManager.startService();

    metadataManager = new MetadataManager();
    theDaemon.setMetadataManager(metadataManager);
    metadataManager.initService(theDaemon);
    metadataManager.startService();

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

    // Today's date.
    Calendar calendar = new GregorianCalendar();
    calendar.setTime(TimeBase.nowDate());
    int year = calendar.get(Calendar.YEAR);
    int month = calendar.get(Calendar.MONTH) + 1;
    int day = calendar.get(Calendar.DAY_OF_MONTH);

    // Run the tests for days in the next 10 years.
    for (int runYear = 0; runYear < 10; runYear++) {
      runTestEmptyReport(year);

      initializeJournalMetadata(year);

      counterReportsManager.persistRequest(JOURNAL_URL, false);
      counterReportsManager.persistRequest(JOURNAL_URL, true);
      counterReportsManager.persistRequest(HTML_URL, false);
      counterReportsManager.persistRequest(HTML_URL, true);
      counterReportsManager.persistRequest(PDF_URL, false);
      counterReportsManager.persistRequest(PDF_URL, true);

      CounterReportsRequestAggregator aggregator =
	  new CounterReportsRequestAggregator(theDaemon);
      aggregator.getCronTask().execute();

      runTestDefaultPeriodReport(year);
      runTestCustomPeriodReport(year);

      cleanUpAggregatesAndJournals();

      // Point to the next year.
      year++;

      // Handle leap years.
      if (month == 2 && day == 29 && year % 4 != 0) {
	day = 28;
      }

      TimeBase.setSimulated(year + "/" + month + "/" + day + " " + month
	  + ":" + day + ":" + day);
    }
  }

  /**
   * Tests the validation of the constructor parameters.
   * 
   * @throws Exception
   */
  public void runTestValidation() throws Exception {

    try {
      new CounterReportsJournalReport5(theDaemon, 0, 2011, 7, 2012);
      fail("Invalid start month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsJournalReport5(theDaemon, 13, 2011, 7, 2012);
      fail("Invalid start month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsJournalReport5(theDaemon, 1, 2011, 0, 2012);
      fail("Invalid end month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsJournalReport5(theDaemon, 1, 2011, 13, 2012);
      fail("Invalid end month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsJournalReport5(theDaemon, 1, 2012, 12, 2011);
      fail("Invalid report period - End must not precede start");
    } catch (IllegalArgumentException iae) {
    }

    boolean validArgument = false;

    try {
      new CounterReportsJournalReport5(theDaemon, 1, 2012, 1, 2012);
      validArgument = true;
    } catch (IllegalArgumentException iae) {
    }

    assertEquals(true, validArgument);
  }

  /**
   * Tests an empty report.
   * 
   * @param year
   *          An int with the year when the test is run.
   * @throws Exception
   */
  public void runTestEmptyReport(int year) throws Exception {
    CounterReportsJournalReport5 report =
	new CounterReportsJournalReport5(theDaemon);

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

    StringBuilder expected =
	new StringBuilder("\"Total for all journals\",,,,,,,0,");

    int columnCount = 11 + year % 10;

    for (int i = 0; i < columnCount; i++) {
      expected.append("0,");
    }

    expected.append("0,0");

    assertEquals(expected.toString(), line);
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

    expected = new StringBuilder("Total for all journals\t\t\t\t\t\t\t0\t");

    for (int i = 0; i < columnCount; i++) {
      expected.append("0\t");
    }

    expected.append("0\t0");

    assertEquals(expected.toString(), line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());
  }

  /**
   * Creates a journal for which to aggregate requests.
   * 
   * @param year
   *          An int with the year when the test is run.
   * @return a Long with the identifier of the created journal.
   * @throws SQLException
   */
  private Long initializeJournalMetadata(int year) throws SQLException {
    Long publicationSeq = null;
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Add the publisher.
      Long publisherSeq =
	  metadataManager.findOrCreatePublisher(conn, "publisher");

      // Add the publication.
      publicationSeq =
	  metadataManager.findOrCreatePublication(conn, "12345678", "98765432",
						  null, null, publisherSeq,
						  "JOURNAL", null, null);

      // Add the publishing platform.
      Long platformSeq = metadataManager.findOrCreatePlatform(conn, "platform");

      // Add the plugin.
      Long pluginSeq = metadataManager.findOrCreatePlugin(conn, "pluginId",
	  platformSeq);

      // Add the AU.
      Long auSeq = metadataManager.findOrCreateAu(conn, pluginSeq, "auKey");

      // Add the AU metadata.
      Long auMdSeq = metadataManager.addAuMd(conn, auSeq, 1, 0L);

      Long parentSeq =
	  metadataManager.findPublicationMetadataItem(conn, publicationSeq);

      metadataManager.addMdItemDoi(conn, parentSeq, "10.1000/182");

      Long mdItemTypeSeq = metadataManager.findMetadataItemType(conn,
	  MD_ITEM_TYPE_JOURNAL_ARTICLE);

      // The publication date is a couple of years ago.
      String publicationDate = (year-2) + "-01-01";

      Long mdItemSeq = metadataManager.addMdItem(conn, parentSeq, mdItemTypeSeq,
                                            auMdSeq, publicationDate, null);

      metadataManager.addMdItemName(conn, mdItemSeq, "htmlArticle",
				    PRIMARY_NAME_TYPE);

      metadataManager.addMdItemUrl(conn, mdItemSeq, ROLE_FULL_TEXT_HTML,
                                   HTML_URL);

      mdItemSeq = metadataManager.addMdItem(conn, parentSeq, mdItemTypeSeq,
                                            auMdSeq, publicationDate, null);

      metadataManager.addMdItemName(conn, mdItemSeq, "pdfArticle",
				    PRIMARY_NAME_TYPE);

      metadataManager.addMdItemUrl(conn, mdItemSeq, ROLE_FULL_TEXT_PDF,
                                   PDF_URL);
    } finally {
      conn.commit();
      DbManager.safeCloseConnection(conn);
    }
    
    return publicationSeq;
  }

  /**
   * Tests a report for the default period.
   * 
   * @param year
   *          An int with the year when the test is run.
   * @throws Exception
   */
  public void runTestDefaultPeriodReport(int year) throws Exception {
    CounterReportsJournalReport5 report =
	new CounterReportsJournalReport5(theDaemon);

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

    StringBuilder expected =
	new StringBuilder("\"Total for all journals\",,,,,,,0,");

    int columnCount = 11 + year % 10;

    for (int i = 0; i < columnCount; i++) {
      expected.append("0,");
    }

    expected.append("0,0");

    assertEquals(expected.toString(), line);
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

    expected = new StringBuilder("Total for all journals\t\t\t\t\t\t\t0\t");

    for (int i = 0; i < columnCount; i++) {
      expected.append("0\t");
    }

    expected.append("0\t0");

    assertEquals(expected.toString(), line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());
  }

  /**
   * Tests a report for a custom period.
   * 
   * @param year
   *          An int with the year when the test is run.
   * @throws Exception
   */
  public void runTestCustomPeriodReport(int year) throws Exception {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(TimeBase.nowDate());
    int endYear = calendar.get(Calendar.YEAR);
    int endMonth = calendar.get(Calendar.MONTH) + 1;
    calendar.add(Calendar.MONTH, -4);
    int startYear = calendar.get(Calendar.YEAR);
    int startMonth = calendar.get(Calendar.MONTH) + 1;

    CounterReportsJournalReport5 report =
	new CounterReportsJournalReport5(theDaemon, startMonth, startYear,
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

    StringBuilder expected =
	new StringBuilder("\"Total for all journals\",,,,,,,0,0,0,2,");

    int columnCount = 8 + year % 10;

    for (int i = 0; i < columnCount; i++) {
      expected.append("0,");
    }

    expected.append("0,0");

    assertEquals(expected.toString(), line);
    line = reader.readLine();

    expected =
	new StringBuilder("JOURNAL,publisher,platform,10.1000/182,,1234-5678,9876-5432,0,0,0,2,");

    for (int i = 0; i < columnCount; i++) {
      expected.append("0,");
    }

    expected.append("0,0");

    assertEquals(expected.toString(), line);
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

    expected =
	new StringBuilder("Total for all journals\t\t\t\t\t\t\t0\t0\t0\t2\t");

    for (int i = 0; i < columnCount; i++) {
      expected.append("0\t");
    }

    expected.append("0\t0");

    assertEquals(expected.toString(), line);
    line = reader.readLine();

    expected =
	new StringBuilder("JOURNAL\tpublisher\tplatform\t10.1000/182\t\t1234-5678\t9876-5432\t0\t0\t0\t2\t");

    for (int i = 0; i < columnCount; i++) {
      expected.append("0\t");
    }

    expected.append("0\t0");

    assertEquals(expected.toString(), line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());
  }

  /**
   * Deletes the existing journals.
   * 
   * @throws SQLException
   */
  private void cleanUpAggregatesAndJournals() throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;

    try {
      conn = dbManager.getConnection();

      statement =
	  dbManager.prepareStatement(conn, SQL_QUERY_PUBYEAR_REQUEST_DELETE);
      dbManager.executeUpdate(statement);
      DbManager.safeCloseStatement(statement);

      statement =
	  dbManager.prepareStatement(conn, SQL_QUERY_TYPE_REQUEST_DELETE);
      dbManager.executeUpdate(statement);
      DbManager.safeCloseStatement(statement);

      statement = dbManager.prepareStatement(conn, SQL_QUERY_JOURNAL_DELETE);
      dbManager.executeUpdate(statement);

      conn.commit();
    } finally {
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }
  }
}
