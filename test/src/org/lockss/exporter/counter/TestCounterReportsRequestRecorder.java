/*
 * $Id: TestCounterReportsRequestRecorder.java,v 1.3 2012-09-28 00:13:23 fergaloy-sf Exp $
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
 * Test class for org.lockss.exporter.counter.CounterReportsRequestRecorder.
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
import org.lockss.config.TdbAu;
import org.lockss.daemon.Cron;
import org.lockss.daemon.TitleConfig;
import org.lockss.db.DbManager;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.Plugin;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockPlugin;
import org.lockss.util.Constants;
import org.lockss.util.TimeBase;

public class TestCounterReportsRequestRecorder extends LockssTestCase {
  // A URL that exists in the metadata table.
  private static final String RECORDABLE_URL =
      "http://example.com/fulltext.url";

  // A URL that does not exist in the metadata table.
  private static final String IGNORABLE_URL = "http://example.com/index.html";

  // Query to create the metadata table.
  private static final String SQL_QUERY_METADATA_CREATE =
      "create table metadata ("
	  + "md_id bigint primary key generated always as identity,"
	  + "access_url varchar(4096) not null)";

  // Query to populate metadata.
  private static final String SQL_QUERY_METADATA_INSERT =
      "insert into metadata (access_url) values (?)";

  // Query to count all the rows of titles.
  private static final String SQL_QUERY_TITLE_COUNT = "select count(*) from "
      + SQL_TABLE_TITLES;

  // Query to count all the rows of requests.
  private static final String SQL_QUERY_REQUEST_COUNT = "select count(*) from "
      + SQL_TABLE_REQUESTS;

  // Query to count the rows of requests for a LOCKSS identifier.
  private static final String SQL_QUERY_REQUEST_BY_ID_COUNT =
      "select count(*) from " + SQL_TABLE_REQUESTS + " where "
	  + SQL_COLUMN_LOCKSS_ID + " = ?";

  // Query to get the properties of a book.
  private static final String SQL_QUERY_BOOK_SELECT = "select "
      + SQL_COLUMN_TITLE_NAME + "," + SQL_COLUMN_PUBLISHER_NAME + ","
      + SQL_COLUMN_PLATFORM_NAME + "," + SQL_COLUMN_DOI + ","
      + SQL_COLUMN_PROPRIETARY_ID + "," + SQL_COLUMN_IS_BOOK + ","
      + SQL_COLUMN_ISBN + "," + SQL_COLUMN_BOOK_ISSN + " from "
      + SQL_TABLE_TITLES + " where " + SQL_COLUMN_LOCKSS_ID + " = ?";

  // Query to get the properties of a journal.
  private static final String SQL_QUERY_JOURNAL_SELECT = "select "
      + SQL_COLUMN_TITLE_NAME + "," + SQL_COLUMN_PUBLISHER_NAME + ","
      + SQL_COLUMN_PLATFORM_NAME + "," + SQL_COLUMN_DOI + ","
      + SQL_COLUMN_PROPRIETARY_ID + "," + SQL_COLUMN_IS_BOOK + ","
      + SQL_COLUMN_PRINT_ISSN + "," + SQL_COLUMN_ONLINE_ISSN + " from "
      + SQL_TABLE_TITLES + " where " + SQL_COLUMN_LOCKSS_ID + " = ?";

  // Query to get the properties of a book request.
  private static final String SQL_QUERY_BOOK_REQUEST_SELECT = "select "
      + SQL_COLUMN_IS_SECTION + "," + SQL_COLUMN_IS_PUBLISHER_INVOLVED + ","
      + SQL_COLUMN_REQUEST_YEAR + "," + SQL_COLUMN_REQUEST_MONTH + ","
      + SQL_COLUMN_REQUEST_DAY + " from " + SQL_TABLE_REQUESTS + " where "
      + SQL_COLUMN_LOCKSS_ID + " = ?";

  // Query to get the properties of a journal request.
  private static final String SQL_QUERY_JOURNAL_REQUEST_SELECT = "select "
      + SQL_COLUMN_PUBLICATION_YEAR + "," + SQL_COLUMN_IS_HTML + ","
      + SQL_COLUMN_IS_PDF + "," + SQL_COLUMN_IS_PUBLISHER_INVOLVED + ","
      + SQL_COLUMN_REQUEST_YEAR + "," + SQL_COLUMN_REQUEST_MONTH + ","
      + SQL_COLUMN_REQUEST_DAY + " from " + SQL_TABLE_REQUESTS + " where "
      + SQL_COLUMN_LOCKSS_ID + " = ?";

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
    props.setProperty(DbManager.PARAM_DATASOURCE_CLASSNAME,
	"org.apache.derby.jdbc.ClientDataSource");
    props.setProperty(CounterReportsManager.PARAM_COUNTER_ENABLED, "true");
    props.setProperty(CounterReportsManager.PARAM_REPORT_BASEDIR_PATH,
	tempDirPath);
    props
	.setProperty(
	    CounterReportsRequestAggregator.PARAM_COUNTER_REQUEST_AGGREGATION_TASK_FREQUENCY,
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

    counterReportsManager = new CounterReportsManager();
    theDaemon.setCounterReportsManager(counterReportsManager);
    counterReportsManager.initService(theDaemon);
    counterReportsManager.startService();

    initializeMetadata();
  }

  private void initializeMetadata() throws SQLException {
    Connection conn = null;
    PreparedStatement statement = null;

    try {
      conn = dbManager.getConnection();
      statement = conn.prepareStatement(SQL_QUERY_METADATA_CREATE);
      statement.executeUpdate();
      DbManager.safeCloseStatement(statement);

      statement = conn.prepareStatement(SQL_QUERY_METADATA_INSERT);
      statement.setString(1, RECORDABLE_URL);
      statement.executeUpdate();
    } finally {
      DbManager.safeCloseStatement(statement);
      conn.commit();
      DbManager.safeCloseConnection(conn);
    }
  }

  /**
   * Runs all the tests.
   * <br />
   * This avoids unnecessary set up and tear down of the database.
   * 
   * @throws Exception
   */
  public void testAll() throws Exception {
    runTestRecordBookRequest();
    runTestJournalRecordRequest();
    runTestRecordMultipleRequests();
  }

  /**
   * Tests the recording of a book request.
   * 
   * @throws Exception
   */
  public void runTestRecordBookRequest() throws Exception {
    CounterReportsBook book =
	new CounterReportsBook("Book1", "Publisher1", "Platform1", null, null,
	    "987-654321-0987", "1234-5678");

    ArchivalUnit bookAu = new MyMockBookArchivalUnit(book, "1234");

    CounterReportsRequestRecorder recorder =
	CounterReportsRequestRecorder.getInstance();
    recorder.recordRequest(IGNORABLE_URL, bookAu,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(2);
    checkRequestRowCount(0);

    recorder.recordRequest(RECORDABLE_URL, bookAu,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(3);
    checkRequestRowCount(1);
  }

  private class MyMockBookArchivalUnit extends MockArchivalUnit {
    private String platform = null;
    private String auId = null;
    private String doi = null;
    private String isbn = null;
    private String issn = null;
    private String publisherName = null;

    public MyMockBookArchivalUnit(CounterReportsBook book, String auId) {
      setName(book.getName());
      this.platform = book.getPublishingPlatform();
      this.auId = auId;
      this.doi = book.getDoi();
      this.isbn = book.getIsbn();
      this.issn = book.getIssn();
      this.publisherName = book.getPublisherName();
    }

    public Plugin getPlugin() {
      return new MyMockPlugin();
    }

    public TitleConfig getTitleConfig() {
      return new MockBookConfig("displayName", "pluginName");
    }

    private class MyMockPlugin extends MockPlugin {
      public String getPublishingPlatform() {
	return platform;
      }
    }

    private class MockBookConfig extends TitleConfig {
      public MockBookConfig(String displayName, String pluginName) {
	super(displayName, pluginName);
      }

      public TdbAu getTdbAu() {
	return new MockTdbAu("displayName", "pluginId");
      }

      private class MockTdbAu extends TdbAu {
	public MockTdbAu(String displayName, String pluginId) {
	  super(displayName, pluginId);
	  setAuId(auId);
	}

	public String getDoi() {
	  return doi;
	}

	public String getIsbn() {
	  return isbn;
	}

	public String getIssn() {
	  return issn;
	}

	public String getPublisherName() {
	  return publisherName;
	}
      }
    }
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
   * Checks the expected properties of a book in the database.
   * 
   * @param book
   *          A CounterReportsBook with the book to be checked.
   * @param requestData
   *          A Map<String, Object> with the data of the request to be checked.
   * @throws SQLException
   */
  private void checkBookRequest(CounterReportsBook book,
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
   * Tests the recording of a journal request.
   * 
   * @throws Exception
   */
  public void runTestJournalRecordRequest() throws Exception {
    CounterReportsJournal journal =
	new CounterReportsJournal("Journal1", "Publisher1", "Platform1", null,
	    null, "1234-5678", "9876-5432");

    ArchivalUnit journalAu =
	new MyMockJournalArchivalUnit(journal, "1234", "1954",
	    Constants.MIME_TYPE_HTML);

    CounterReportsRequestRecorder recorder =
	CounterReportsRequestRecorder.getInstance();
    recorder.recordRequest(IGNORABLE_URL, journalAu,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(3);
    checkRequestRowCount(1);

    recorder.recordRequest(RECORDABLE_URL, journalAu,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(4);
    checkRequestRowCount(2);

    Map<String, Object> requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsJournal.PUBLICATION_YEAR_KEY, "1954");
    requestData.put(CounterReportsJournal.IS_HTML_KEY, Boolean.TRUE);
    requestData.put(CounterReportsJournal.IS_PDF_KEY, Boolean.FALSE);
    requestData.put(CounterReportsJournal.IS_PUBLISHER_INVOLVED_KEY,
	Boolean.FALSE);
  }

  private class MyMockJournalArchivalUnit extends MockArchivalUnit {
    private String journalTitle;
    private String platform = null;
    private String auId = null;
    private String doi = null;
    private String printIssn = null;
    private String onlineIssn = null;
    private String publisherName = null;
    private String year = null;
    private String contentType = null;

    public MyMockJournalArchivalUnit(CounterReportsJournal journal,
	String auId, String year, String contentType) {
      this.journalTitle = journal.getName();
      this.platform = journal.getPublishingPlatform();
      this.auId = auId;
      this.doi = journal.getDoi();
      this.printIssn = journal.getPrintIssn();
      this.onlineIssn = journal.getOnlineIssn();
      this.publisherName = journal.getPublisherName();
      this.year = year;
      this.contentType = contentType;
    }

    public Plugin getPlugin() {
      return new MyMockPlugin();
    }

    public TitleConfig getTitleConfig() {
      return new MockJournalConfig("displayName", "pluginName");
    }

    public CachedUrl makeCachedUrl(String url) {
      return new MyMockCachedUrl(url);
    }

    private class MockJournalConfig extends TitleConfig {
      public MockJournalConfig(String displayName, String pluginName) {
	super(displayName, pluginName);
      }

      public TdbAu getTdbAu() {
	return new MockTdbAu("displayName", "pluginId");
      }

      private class MockTdbAu extends TdbAu {
	public MockTdbAu(String displayName, String pluginId) {
	  super(displayName, pluginId);
	  setAuId(auId);
	}

	public String getEissn() {
	  return onlineIssn;
	}

	public String getIsbn() {
	  return null;
	}

	public String getJournalDoi() {
	  return doi;
	}

	public String getJournalId() {
	  return auId;
	}

	public String getJournalTitle() {
	  return journalTitle;
	}

	public String getPrintIssn() {
	  return printIssn;
	}

	public String getPublisherName() {
	  return publisherName;
	}

	public String getYear() {
	  return year;
	}
      }
    }

    private class MyMockPlugin extends MockPlugin {
      public String getPublishingPlatform() {
	return platform;
      }
    }

    private class MyMockCachedUrl extends MockCachedUrl {
      public MyMockCachedUrl(String url) {
	super(url);
      }

      @Override
      public String getContentType() {
	return contentType;
      }
    }
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
   * Checks the expected properties of a journal in the database.
   * 
   * @param book
   *          A CounterReportsJournal with the journal to be checked.
   * @param requestData
   *          A Map<String, Object> with the data of the request to be checked.
   * @throws SQLException
   */
  private void checkJournalRequest(CounterReportsJournal journal,
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
   * Tests the recording of requests for multiple book.
   * 
   * @throws Exception
   */
  public void runTestRecordMultipleRequests() throws Exception {
    CounterReportsBook book1 =
	new CounterReportsBook("Book1", "Publisher1", "Platform1", null, null,
	    "987-654321-0987", "1234-5678");

    ArchivalUnit bookAu1 = new MyMockBookArchivalUnit(book1, "1234");

    CounterReportsBook book2 =
	new CounterReportsBook("Book2", "Publisher2", "Platform2", null, null,
	    "987-654322-0987", "2234-5678");

    ArchivalUnit bookAu2 = new MyMockBookArchivalUnit(book2, "2234");

    CounterReportsBook book3 =
	new CounterReportsBook("Book3", "Publisher3", "Platform3", null, null,
	    "987-654323-0987", "3234-5678");

    ArchivalUnit bookAu3 = new MyMockBookArchivalUnit(book3, "3234");

    CounterReportsBook book4 =
	new CounterReportsBook("Book4", "Publisher4", "Platform4", null, null,
	    "987-654324-0987", "4234-5678");

    ArchivalUnit bookAu4 = new MyMockBookArchivalUnit(book4, "4234");

    CounterReportsJournal journal1 =
	new CounterReportsJournal("Journal1", "Publisher1", "Platform1", null,
	    null, "1234-5678", "9876-5432");

    ArchivalUnit journalAu1 =
	new MyMockJournalArchivalUnit(journal1, "1234", "1954",
	    Constants.MIME_TYPE_HTML);

    CounterReportsJournal journal2 =
	new CounterReportsJournal("Journal2", "Publisher2", "Platform2", null,
	    null, "2234-5678", "9876-5433");

    ArchivalUnit journalAu2 =
	new MyMockJournalArchivalUnit(journal2, "2234", "1964",
	    Constants.MIME_TYPE_PDF);

    CounterReportsJournal journal3 =
	new CounterReportsJournal("Journal3", "Publisher3", "Platform3", null,
	    null, "3234-5678", "9876-5434");

    ArchivalUnit journalAu3 =
	new MyMockJournalArchivalUnit(journal3, "3234", "1974",
	    Constants.MIME_TYPE_HTML);

    CounterReportsJournal journal4 =
	new CounterReportsJournal("Journal4", "Publisher4", "Platform4", null,
	    null, "4234-5678", "9876-5435");

    ArchivalUnit journalAu4 =
	new MyMockJournalArchivalUnit(journal4, "4234", "1984",
	    Constants.MIME_TYPE_PDF);

    CounterReportsRequestRecorder recorder =
	CounterReportsRequestRecorder.getInstance();

    recorder.recordRequest(IGNORABLE_URL, bookAu1,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(4);
    checkRequestRowCount(2);

    recorder.recordRequest(RECORDABLE_URL, bookAu1,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(4);
    checkRequestRowCount(3);

    Map<String, Object> requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsBook.IS_SECTION_KEY, Boolean.FALSE);
    requestData
	.put(CounterReportsBook.IS_PUBLISHER_INVOLVED_KEY, Boolean.FALSE);

    recorder.recordRequest(IGNORABLE_URL, bookAu2,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(4);
    checkRequestRowCount(3);

    recorder.recordRequest(RECORDABLE_URL, bookAu2,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(5);
    checkRequestRowCount(4);

    requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsBook.IS_SECTION_KEY, Boolean.FALSE);
    requestData.put(CounterReportsBook.IS_PUBLISHER_INVOLVED_KEY, Boolean.TRUE);

    recorder.recordRequest(RECORDABLE_URL, bookAu2,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(5);
    checkRequestRowCount(5);

    recorder.recordRequest(IGNORABLE_URL, bookAu3,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(5);
    checkRequestRowCount(5);

    recorder.recordRequest(RECORDABLE_URL, bookAu3,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(6);
    checkRequestRowCount(6);

    requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsBook.IS_SECTION_KEY, Boolean.FALSE);
    requestData.put(CounterReportsBook.IS_PUBLISHER_INVOLVED_KEY, Boolean.TRUE);

    recorder.recordRequest(RECORDABLE_URL, bookAu3,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(6);
    checkRequestRowCount(7);

    recorder.recordRequest(RECORDABLE_URL, bookAu3,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(6);
    checkRequestRowCount(8);

    recorder.recordRequest(IGNORABLE_URL, journalAu1,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(6);
    checkRequestRowCount(8);

    recorder.recordRequest(RECORDABLE_URL, journalAu1,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(6);
    checkRequestRowCount(9);

    requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsJournal.PUBLICATION_YEAR_KEY, "1954");
    requestData.put(CounterReportsJournal.IS_HTML_KEY, Boolean.TRUE);
    requestData.put(CounterReportsJournal.IS_PDF_KEY, Boolean.FALSE);
    requestData.put(CounterReportsJournal.IS_PUBLISHER_INVOLVED_KEY,
	Boolean.FALSE);

    recorder.recordRequest(RECORDABLE_URL, journalAu1,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(6);
    checkRequestRowCount(10);

    recorder.recordRequest(RECORDABLE_URL, journalAu1,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(6);
    checkRequestRowCount(11);

    recorder.recordRequest(RECORDABLE_URL, journalAu1,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(6);
    checkRequestRowCount(12);

    recorder.recordRequest(IGNORABLE_URL, bookAu4,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(6);
    checkRequestRowCount(12);

    recorder.recordRequest(RECORDABLE_URL, bookAu4,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(7);
    checkRequestRowCount(13);

    requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsBook.IS_SECTION_KEY, Boolean.FALSE);
    requestData
	.put(CounterReportsBook.IS_PUBLISHER_INVOLVED_KEY, Boolean.FALSE);

    recorder.recordRequest(RECORDABLE_URL, bookAu4,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(7);
    checkRequestRowCount(14);

    recorder.recordRequest(RECORDABLE_URL, bookAu4,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(7);
    checkRequestRowCount(15);

    recorder.recordRequest(RECORDABLE_URL, bookAu4,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(7);
    checkRequestRowCount(16);

    recorder.recordRequest(IGNORABLE_URL, journalAu2,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(7);
    checkRequestRowCount(16);

    recorder.recordRequest(RECORDABLE_URL, journalAu2,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(8);
    checkRequestRowCount(17);

    requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsJournal.PUBLICATION_YEAR_KEY, "1964");
    requestData.put(CounterReportsJournal.IS_HTML_KEY, Boolean.FALSE);
    requestData.put(CounterReportsJournal.IS_PDF_KEY, Boolean.TRUE);
    requestData.put(CounterReportsJournal.IS_PUBLISHER_INVOLVED_KEY,
	Boolean.TRUE);

    recorder.recordRequest(RECORDABLE_URL, journalAu2,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(8);
    checkRequestRowCount(18);

    recorder.recordRequest(RECORDABLE_URL, journalAu2,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(8);
    checkRequestRowCount(19);

    recorder.recordRequest(IGNORABLE_URL, journalAu3,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(8);
    checkRequestRowCount(19);

    recorder.recordRequest(RECORDABLE_URL, journalAu3,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(9);
    checkRequestRowCount(20);

    requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsJournal.PUBLICATION_YEAR_KEY, "1974");
    requestData.put(CounterReportsJournal.IS_HTML_KEY, Boolean.TRUE);
    requestData.put(CounterReportsJournal.IS_PDF_KEY, Boolean.FALSE);
    requestData.put(CounterReportsJournal.IS_PUBLISHER_INVOLVED_KEY,
	Boolean.TRUE);

    recorder.recordRequest(RECORDABLE_URL, journalAu3,
	CounterReportsRequestRecorder.PublisherContacted.TRUE, 200);

    checkTitleRowCount(9);
    checkRequestRowCount(21);

    recorder.recordRequest(IGNORABLE_URL, journalAu4,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(9);
    checkRequestRowCount(21);

    recorder.recordRequest(RECORDABLE_URL, journalAu4,
	CounterReportsRequestRecorder.PublisherContacted.FALSE, 200);

    checkTitleRowCount(10);
    checkRequestRowCount(22);

    requestData = new HashMap<String, Object>();
    requestData.put(CounterReportsJournal.PUBLICATION_YEAR_KEY, "1984");
    requestData.put(CounterReportsJournal.IS_HTML_KEY, Boolean.FALSE);
    requestData.put(CounterReportsJournal.IS_PDF_KEY, Boolean.TRUE);
    requestData.put(CounterReportsJournal.IS_PUBLISHER_INVOLVED_KEY,
	Boolean.FALSE);
  }
}
