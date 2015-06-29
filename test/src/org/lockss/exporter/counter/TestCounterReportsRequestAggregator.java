/*
 * $Id$
 */

/*

 Copyright (c) 2013-2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.exporter.counter;

import static org.lockss.db.SqlConstants.*;
import static org.lockss.plugin.ArticleFiles.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import org.lockss.daemon.Cron;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.exporter.counter.CounterReportsRequestAggregator;
import org.lockss.metadata.MetadataManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.TimeBase;

/**
 * Test class for org.lockss.exporter.counter.CounterReportsRequestAggregator.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
public class TestCounterReportsRequestAggregator extends LockssTestCase {
  private static final String FULL_URL = "http://example.com/full.url";
  private static final String SECTION_URL = "http://example.com/section.url";
  private static final String HTML_URL = "http://example.com/html.url";
  private static final String PDF_URL = "http://example.com/pdf.url";

  // Query to count all the request rows.
  private static final String SQL_QUERY_REQUEST_COUNT = "select count(*) from "
      + COUNTER_REQUEST_TABLE;

  // Query to count all the rows of book type aggregated totals.
  private static final String SQL_QUERY_BOOK_TYPE_AGGREGATED_TOTAL_COUNT =
      "select count(*) from " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE;

  // Query to count all the rows of journal type aggregated totals.
  private static final String SQL_QUERY_JOURNAL_TYPE_AGGREGATED_TOTAL_COUNT =
      "select count(*) from " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE;

  // Query to count monthly rows of book type aggregated totals.
  private static final String SQL_QUERY_BOOK_TYPE_AGGREGATED_MONTH_COUNT =
      "select count(*) "
      + "from " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
      + " where " + REQUEST_YEAR_COLUMN + " = ? "
      + "and " + REQUEST_MONTH_COLUMN + " = ? "
      + "and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ?";

  // Query to count monthly rows of journal type aggregated totals.
  private static final String SQL_QUERY_JOURNAL_TYPE_AGGREGATED_MONTH_COUNT =
      "select count(*) "
      + "from " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
      + " where " + REQUEST_YEAR_COLUMN + " = ? "
      + "and " + REQUEST_MONTH_COLUMN + " = ? "
      + "and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ?";

  // Query to get aggregated type book request counts for a month.
  private static final String SQL_QUERY_TYPE_AGGREGATED_MONTH_BOOK_SELECT =
      "select "
      + FULL_REQUESTS_COLUMN + ","
      + SECTION_REQUESTS_COLUMN
      + " from " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ? "
      + "and " + REQUEST_YEAR_COLUMN + " = ? "
      + "and " + REQUEST_MONTH_COLUMN + " = ? "
      + "and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ?";

  // Query to get aggregated type journal request counts for a month.
  private static final String SQL_QUERY_TYPE_AGGREGATED_MONTH_JOURNAL_SELECT =
      "select "
      + HTML_REQUESTS_COLUMN + ","
      + PDF_REQUESTS_COLUMN + ","
      + TOTAL_REQUESTS_COLUMN
      + " from " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ? "
      + "and " + REQUEST_YEAR_COLUMN + " = ? "
      + "and " + REQUEST_MONTH_COLUMN + " = ? "
      + "and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ?";

  // Query to count all the rows of publication year aggregated totals.
  private static final String SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATED_TOTAL_COUNT =
      "select count(*) from " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE;

  // Query to get aggregated publication year journal request counts for a
  // month.
  private static final String SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATED_MONTH_SELECT =
      "select "
      + PUBLICATION_YEAR_COLUMN + ","
      + REQUESTS_COLUMN
      + " from " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ? "
      + "and " + REQUEST_YEAR_COLUMN + " = ? "
      + "and " + REQUEST_MONTH_COLUMN + " = ? "
      + "and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ?";

  private MockLockssDaemon theDaemon;
  private DbManager dbManager;
  private MetadataManager metadataManager;
  private CounterReportsManager counterReportsManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    ConfigurationUtil.addFromArgs(CounterReportsManager.PARAM_COUNTER_ENABLED,
	"true");
    ConfigurationUtil.addFromArgs(CounterReportsManager
	.PARAM_REPORT_BASEDIR_PATH, tempDirPath);
    ConfigurationUtil.addFromArgs(CounterReportsRequestAggregator
        .PARAM_COUNTER_REQUEST_AGGREGATION_TASK_FREQUENCY, "hourly");

    theDaemon = getMockLockssDaemon();
    theDaemon.setDaemonInited(true);

    dbManager = getTestDbManager(tempDirPath);

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
    runTestEmptyAggregations();
    runTestMonthBookAggregation();
    runTestMonthJournalAggregation();
    runTestNextTime();
  }

  /**
   * Tests the aggregation of an empty system.
   * 
   * @throws Exception
   */
  public void runTestEmptyAggregations() throws Exception {
    CounterReportsRequestAggregator aggregator =
	new CounterReportsRequestAggregator(theDaemon);
    aggregator.getCronTask().execute();

    checkBookTypeAggregatedRowCount(0);
    checkJournalTypeAggregatedRowCount(0);
    checkJournalPublicationYearAggregatedRowCount(0);
  }

  /**
   * Checks the expected count of rows in the book aggregation by type table.
   * 
   * @param expected
   *          An int with the expected number of rows in the table.
   * @throws SQLException, DbException
   */
  private void checkBookTypeAggregatedRowCount(int expected)
      throws SQLException, DbException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_BOOK_TYPE_AGGREGATED_TOTAL_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = dbManager.prepareStatement(conn, sql);
      resultSet = dbManager.executeQuery(statement);

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
   * Checks the expected count of rows in the journal aggregation by type table.
   * 
   * @param expected
   *          An int with the expected number of rows in the table.
   * @throws SQLException, DbException
   */
  private void checkJournalTypeAggregatedRowCount(int expected)
      throws SQLException, DbException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_JOURNAL_TYPE_AGGREGATED_TOTAL_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = dbManager.prepareStatement(conn, sql);
      resultSet = dbManager.executeQuery(statement);

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
   * @throws SQLException, DbException
   */
  private void checkJournalPublicationYearAggregatedRowCount(int expected)
      throws SQLException, DbException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATED_TOTAL_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = dbManager.prepareStatement(conn, sql);
      resultSet = dbManager.executeQuery(statement);

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
    Long fullPublicationSeq = initializeFullBookMetadata();
    Long sectionPublicationSeq = initializeSectionBookMetadata();

    counterReportsManager.persistRequest(FULL_URL, false);
    counterReportsManager.persistRequest(FULL_URL, true);
    counterReportsManager.persistRequest(SECTION_URL, false);
    counterReportsManager.persistRequest(SECTION_URL, true);

    checkRequestRowCount(4);

    CounterReportsRequestAggregator aggregator =
	new CounterReportsRequestAggregator(theDaemon);
    aggregator.getCronTask().execute();

    checkBookTypeAggregatedRowCount(6);

    Calendar cal = Calendar.getInstance();
    cal.setTime(TimeBase.nowDate());

    int requestMonth = cal.get(Calendar.MONTH) + 1;
    int requestYear = cal.get(Calendar.YEAR);

    checkBookTypeAggregatedRowCount(requestYear, requestMonth, Boolean.TRUE, 3);
    checkBookTypeAggregatedRowCount(requestYear, requestMonth, Boolean.FALSE,
                                    3);
    checkBookMonthlyTypeRequests(fullPublicationSeq, requestYear, requestMonth,
                                 Boolean.TRUE, 1, 0);
    checkBookMonthlyTypeRequests(sectionPublicationSeq, requestYear,
                                 requestMonth, Boolean.TRUE, 0, 1);
    checkBookMonthlyTypeRequests(fullPublicationSeq, requestYear, requestMonth,
                                 Boolean.FALSE, 1, 0);
    checkBookMonthlyTypeRequests(sectionPublicationSeq, requestYear,
                                 requestMonth, Boolean.FALSE, 0, 1);

    checkRequestRowCount(0);
  }

  /**
   * Creates a full book for which to aggregate requests.
   * 
   * @return a Long with the identifier of the created book.
   * @throws DbException
   */
  private Long initializeFullBookMetadata() throws DbException {
    Long publicationSeq = null;
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Add the publisher.
      Long publisherSeq = dbManager.findOrCreatePublisher(conn, "publisher");

      // Add the publication.
      publicationSeq =
	  metadataManager.findOrCreateBook(conn, publisherSeq, null,
	      "FULLPISBN", "FULLEISBN", "Full Name", null);

      // Add the publishing platform.
      Long platformSeq = metadataManager.findOrCreatePlatform(conn,
	  "fullPlatform");

      // Add the plugin.
      Long pluginSeq = metadataManager.findOrCreatePlugin(conn, "fullPluginId",
	  platformSeq, false);

      // Add the AU.
      Long auSeq = metadataManager.findOrCreateAu(conn, pluginSeq, "fullAuKey");

      // Add the provider.
      Long providerSeq = dbManager.findOrCreateProvider(conn, "fullProviderId",
	  "fullProviderName");

      // Add the AU metadata.
      Long auMdSeq =
	  metadataManager.addAuMd(conn, auSeq, 1, 0L, 123L, providerSeq);

      Long parentSeq =
	  metadataManager.findPublicationMetadataItem(conn, publicationSeq);

      Long mdItemTypeSeq =
	  metadataManager.findMetadataItemType(conn, MD_ITEM_TYPE_BOOK);

      Long mdItemSeq = metadataManager.addMdItem(conn, parentSeq, mdItemTypeSeq,
	  					 auMdSeq, "2010-01-01", null,
	  					 1234L);

      metadataManager.addMdItemName(conn, mdItemSeq, "Full Name",
	  			    PRIMARY_NAME_TYPE);

      metadataManager.addMdItemUrl(conn, mdItemSeq, ROLE_FULL_TEXT_HTML,
                                   FULL_URL);
    } finally {
      DbManager.commitOrRollback(conn, log);
      DbManager.safeCloseConnection(conn);
    }
    
    return publicationSeq;
  }

  /**
   * Creates a book section for which to aggregate requests.
   * 
   * @return a Long with the identifier of the created book.
   * @throws DbException
   */
  private Long initializeSectionBookMetadata() throws DbException {
    Long publicationSeq = null;
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Add the publisher.
      Long publisherSeq = dbManager.findOrCreatePublisher(conn, "publisher");

      // Add the publication.
      publicationSeq =
	  metadataManager.findOrCreateBook(conn, publisherSeq, null,
	      "SECTIONPISBN", "SECTIONEISBN", "Section Name", null);

      // Add the publishing platform.
      Long platformSeq = metadataManager.findOrCreatePlatform(conn,
	  "secPlatform");

      // Add the plugin.
      Long pluginSeq = metadataManager.findOrCreatePlugin(conn, "secPluginId",
	  platformSeq, false);

      // Add the AU.
      Long auSeq = metadataManager.findOrCreateAu(conn, pluginSeq, "secAuKey");

      // Add the provider.
      Long providerSeq = dbManager.findOrCreateProvider(conn, "secProviderId",
	  "secProviderName");

      // Add the AU metadata.
      Long auMdSeq =
	  metadataManager.addAuMd(conn, auSeq, 1, 0L, 123L, providerSeq);

      Long parentSeq =
	  metadataManager.findPublicationMetadataItem(conn, publicationSeq);

      Long mdItemTypeSeq =
	  metadataManager.findMetadataItemType(conn, MD_ITEM_TYPE_BOOK_CHAPTER);

      Long mdItemSeq = metadataManager.addMdItem(conn, parentSeq, mdItemTypeSeq,
	  					 auMdSeq, "2010-01-01", null,
	  					 1234L);

      metadataManager.addMdItemName(conn, mdItemSeq, "Chapter Name",
	  			    PRIMARY_NAME_TYPE);

      metadataManager.addMdItemUrl(conn, mdItemSeq, ROLE_FULL_TEXT_PDF,
                                   SECTION_URL);
    } finally {
      DbManager.commitOrRollback(conn, log);
      DbManager.safeCloseConnection(conn);
    }
    
    return publicationSeq;
  }

  /**
   * Checks the expected count of rows in the requests table.
   * 
   * @param expected
   *          An int with the expected number of rows in the table.
   * @throws SQLException, DbException
   */
  private void checkRequestRowCount(int expected)
      throws SQLException, DbException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_REQUEST_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = dbManager.prepareStatement(conn, sql);
      resultSet = dbManager.executeQuery(statement);

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
   * Checks the expected count of monthly rows in the book aggregation by type
   * table.
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
   * @throws SQLException, DbException
   */
  private void checkBookTypeAggregatedRowCount(int requestYear,
      int requestMonth, Boolean isPublisherInvolved, int expected)
	  throws SQLException, DbException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_BOOK_TYPE_AGGREGATED_MONTH_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = dbManager.prepareStatement(conn, sql);
      statement.setInt(1, requestYear);
      statement.setInt(2, requestMonth);
      statement.setBoolean(3, isPublisherInvolved);
      resultSet = dbManager.executeQuery(statement);

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
   * @param apublicationSeq
   *          A Long with the identifier of the book.
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
   * @throws SQLException, DbException
   */
  private void checkBookMonthlyTypeRequests(Long publicationSeq,
      int requestYear, int requestMonth, Boolean isPublisherInvolved,
      int expectedFull, int expectedSection) throws SQLException, DbException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int fullCount = -1;
    int sectionCount = -1;
    String sql = SQL_QUERY_TYPE_AGGREGATED_MONTH_BOOK_SELECT;

    try {
      conn = dbManager.getConnection();

      statement = dbManager.prepareStatement(conn, sql);
      statement.setLong(1, publicationSeq);
      statement.setInt(2, requestYear);
      statement.setInt(3, requestMonth);
      statement.setBoolean(4, isPublisherInvolved);
      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	fullCount = resultSet.getInt(FULL_REQUESTS_COLUMN);
	sectionCount = resultSet.getInt(SECTION_REQUESTS_COLUMN);
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
    Long publicationSeq = initializeJournalMetadata();

    counterReportsManager.persistRequest(HTML_URL, false);
    counterReportsManager.persistRequest(HTML_URL, true);
    counterReportsManager.persistRequest(PDF_URL, false);
    counterReportsManager.persistRequest(PDF_URL, true);

    checkRequestRowCount(4);

    CounterReportsRequestAggregator aggregator =
	new CounterReportsRequestAggregator(theDaemon);
    aggregator.getCronTask().execute();

    checkJournalTypeAggregatedRowCount(4);

    Calendar cal = Calendar.getInstance();
    cal.setTime(TimeBase.nowDate());

    int requestMonth = cal.get(Calendar.MONTH) + 1;
    int requestYear = cal.get(Calendar.YEAR);

    checkJournalTypeAggregatedRowCount(requestYear, requestMonth, Boolean.TRUE,
                                       2);
    checkJournalTypeAggregatedRowCount(requestYear, requestMonth, Boolean.FALSE,
                                       2);
    checkJournalMonthlyTypeRequests(publicationSeq, requestYear, requestMonth,
                                    Boolean.TRUE, 1, 1, 2);
    checkJournalMonthlyTypeRequests(publicationSeq, requestYear, requestMonth,
                                    Boolean.FALSE, 1, 1, 2);
    checkJournalPublicationYearAggregatedRowCount(2);
    checkJournalMonthlyPublicationYearRequests(publicationSeq, requestYear,
                                               requestMonth, Boolean.TRUE,
                                               "2009", 2);
    checkJournalMonthlyPublicationYearRequests(publicationSeq, requestYear,
                                               requestMonth, Boolean.FALSE,
                                               "2009", 2);
    checkRequestRowCount(0);
  }

  /**
   * Creates a journal for which to aggregate requests.
   * 
   * @return a Long with the identifier of the created journal.
   * @throws DbException
   */
  private Long initializeJournalMetadata() throws DbException {
    Long publicationSeq = null;
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Add the publisher.
      Long publisherSeq = dbManager.findOrCreatePublisher(conn, "publisher");

      // Add the publication.
      publicationSeq =
	  metadataManager.findOrCreateJournal(conn, publisherSeq, 
	                                      "PISSN", "EISSN", null, null);

      // Add the publishing platform.
      Long platformSeq = metadataManager.findOrCreatePlatform(conn, "platform");

      // Add the plugin.
      Long pluginSeq = metadataManager.findOrCreatePlugin(conn, "pluginId",
	  platformSeq, false);

      // Add the AU.
      Long auSeq = metadataManager.findOrCreateAu(conn, pluginSeq, "auKey");

      // Add the provider.
      Long providerSeq = dbManager.findOrCreateProvider(conn, "providerId",
	  "providerName");

      // Add the AU metadata.
      Long auMdSeq =
	  metadataManager.addAuMd(conn, auSeq, 1, 0L, 123L, providerSeq);

      Long parentSeq =
	  metadataManager.findPublicationMetadataItem(conn, publicationSeq);

      Long mdItemTypeSeq = metadataManager
	  .findMetadataItemType(conn, MD_ITEM_TYPE_JOURNAL_ARTICLE);

      Long mdItemSeq = metadataManager.addMdItem(conn, parentSeq, mdItemTypeSeq,
	  					 auMdSeq, "2009-01-01", null,
	  					 1234L);

      metadataManager.addMdItemName(conn, mdItemSeq, "html", PRIMARY_NAME_TYPE);

      metadataManager.addMdItemUrl(conn, mdItemSeq, ROLE_FULL_TEXT_HTML,
                                   HTML_URL);

      mdItemSeq = metadataManager.addMdItem(conn, parentSeq, mdItemTypeSeq,
                                            auMdSeq, "2009-01-01", null, 1234L);

      metadataManager.addMdItemName(conn, mdItemSeq, "pdf", PRIMARY_NAME_TYPE);

      metadataManager.addMdItemUrl(conn, mdItemSeq, ROLE_FULL_TEXT_PDF,
                                   PDF_URL);
    } finally {
      DbManager.commitOrRollback(conn, log);
      DbManager.safeCloseConnection(conn);
    }
    
    return publicationSeq;
  }

  /**
   * Checks the expected count of monthly rows in the journal aggregation by
   * type table.
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
   * @throws SQLException, DbException
   */
  private void checkJournalTypeAggregatedRowCount(int requestYear,
      int requestMonth, Boolean isPublisherInvolved, int expected)
	  throws SQLException, DbException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;
    String sql = SQL_QUERY_JOURNAL_TYPE_AGGREGATED_MONTH_COUNT;

    try {
      conn = dbManager.getConnection();

      statement = dbManager.prepareStatement(conn, sql);
      statement.setInt(1, requestYear);
      statement.setInt(2, requestMonth);
      statement.setBoolean(3, isPublisherInvolved);
      resultSet = dbManager.executeQuery(statement);

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
   * Checks the expected type counts of monthly requests for journals.
   * 
   * @param publicationSeq
   *          An Long with the identifier of the journal.
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
   * @throws SQLException, DbException
   */
  private void checkJournalMonthlyTypeRequests(Long publicationSeq,
      int requestYear, int requestMonth, Boolean isPublisherInvolved,
      int expectedHtml, int expectedPdf, int expectedTotal)
	  throws SQLException, DbException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int htmlCount = -1;
    int pdfCount = -1;
    int totalCount = -1;
    String sql = SQL_QUERY_TYPE_AGGREGATED_MONTH_JOURNAL_SELECT;

    try {
      conn = dbManager.getConnection();

      statement = dbManager.prepareStatement(conn, sql);
      statement.setLong(1, publicationSeq);
      statement.setInt(2, requestYear);
      statement.setInt(3, requestMonth);
      statement.setBoolean(4, isPublisherInvolved);
      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	htmlCount = resultSet.getInt(HTML_REQUESTS_COLUMN);
	pdfCount = resultSet.getInt(PDF_REQUESTS_COLUMN);
	totalCount = resultSet.getInt(TOTAL_REQUESTS_COLUMN);
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
   * @param publicationSeq
   *          An Long with the identifier of the journal.
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
   * @throws SQLException, DbException
   */
  private void checkJournalMonthlyPublicationYearRequests(Long publicationSeq,
      int requestYear, int requestMonth, Boolean isPublisherInvolved,
      String expectedPublicationYear, int expectedCount)
	  throws SQLException, DbException {
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String pubYear = null;
    int count = -1;
    String sql = SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATED_MONTH_SELECT;

    try {
      conn = dbManager.getConnection();

      statement = dbManager.prepareStatement(conn, sql);
      statement.setLong(1, publicationSeq);
      statement.setInt(2, requestYear);
      statement.setInt(3, requestMonth);
      statement.setBoolean(4, isPublisherInvolved);
      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	pubYear = resultSet.getString(PUBLICATION_YEAR_COLUMN);
	count = resultSet.getInt(REQUESTS_COLUMN);
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
