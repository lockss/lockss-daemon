/*
 * $Id: CounterReportsManager.java,v 1.2 2012-08-16 22:26:17 fergaloy-sf Exp $
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
 * Service used to manage COUNTER reports.
 * 
 * @version 1.0
 */
package org.lockss.exporter.counter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Cron;
import org.lockss.db.DbManager;
import org.lockss.util.FileUtil;
import org.lockss.util.Logger;

public class CounterReportsManager extends BaseLockssDaemonManager {
  // Prefix for the reporting configuration entries.
  public static final String PREFIX = Configuration.PREFIX + "report.";

  /*
   * Base directory for reporting.<p /> Defaults to
   * <code><i>daemon_tmpdir</i>/report</code>. Changes require daemon restart.
   */
  public static final String PARAM_REPORT_BASEDIR_PATH = PREFIX
      + "baseDirectoryPath";

  public static final String DEFAULT_REPORT_BASEDIR = "report";

  public static final String DEFAULT_REPORT_BASEDIR_PATH = "<tmpdir>/"
      + DEFAULT_REPORT_BASEDIR;

  /*
   * Name of the directory used to store the report output files. <p /> Defaults
   * to <code>output</code>. Changes require daemon restart.
   */
  public static final String PARAM_REPORT_OUTPUTDIR = PREFIX
      + "reportOutputDirectoryName";

  public static final String DEFAULT_REPORT_OUTPUTDIR = "output";

  // Aggregations names.
  public static final String ALL_BOOKS_TITLE_NAME = "ALL BOOKS";
  public static final String ALL_JOURNALS_TITLE_NAME = "ALL JOURNALS";
  public static final String ALL_PUBLISHERS_NAME = "ALL PUBLISHERS";

  // Database columns.
  public static final String SQL_COLUMN_LOCKSS_ID = "lockss_id";
  public static final String SQL_COLUMN_TITLE_NAME = "title_name";
  public static final String SQL_COLUMN_PUBLISHER_NAME = "publisher_name";
  public static final String SQL_COLUMN_PLATFORM_NAME = "platform_name";
  public static final String SQL_COLUMN_DOI = "doi";
  public static final String SQL_COLUMN_PROPRIETARY_ID = "proprietary_id";
  public static final String SQL_COLUMN_PRINT_ISSN = "print_issn";
  public static final String SQL_COLUMN_ONLINE_ISSN = "online_issn";
  public static final String SQL_COLUMN_ISBN = "isbn";
  public static final String SQL_COLUMN_BOOK_ISSN = "book_issn";
  public static final String SQL_COLUMN_PUBLICATION_YEAR = "publication_year";
  public static final String SQL_COLUMN_IS_BOOK = "is_book";
  public static final String SQL_COLUMN_IS_SECTION = "is_section";
  public static final String SQL_COLUMN_IS_HTML = "is_html";
  public static final String SQL_COLUMN_IS_PDF = "is_pdf";
  public static final String SQL_COLUMN_IS_PUBLISHER_INVOLVED =
      "is_publisher_involved";
  public static final String SQL_COLUMN_REQUEST_DAY = "request_day";
  public static final String SQL_COLUMN_REQUEST_MONTH = "request_month";
  public static final String SQL_COLUMN_REQUEST_YEAR = "request_year";
  public static final String SQL_COLUMN_IN_AGGREGATION = "in_aggregation";
  public static final String SQL_COLUMN_TOTAL_JOURNAL_REQUESTS =
      "total_journal_requests";
  public static final String SQL_COLUMN_HTML_JOURNAL_REQUESTS =
      "html_journal_requests";
  public static final String SQL_COLUMN_PDF_JOURNAL_REQUESTS =
      "pdf_journal_requests";
  public static final String SQL_COLUMN_FULL_BOOK_REQUESTS =
      "full_book_requests";
  public static final String SQL_COLUMN_SECTION_BOOK_REQUESTS =
      "section_book_requests";
  public static final String SQL_COLUMN_REQUEST_COUNT = "request_count";

  public static final String SQL_RESULT_YEAR_COUNT = "year_count";

  // Database tables.
  public static final String SQL_TABLE_REQUESTS = "counter_requests";
  public static final String SQL_TABLE_PUBYEAR_AGGREGATES =
      "counter_pubyear_aggregates";
  public static final String SQL_TABLE_TITLES = "counter_titles";
  public static final String SQL_TABLE_TYPE_AGGREGATES =
      "counter_type_aggregates";

  private static final Logger log = Logger.getLogger("CounterReportsManager");

  // Query to create the table for recording title data used for COUNTER
  // reports.
  private static final String SQL_QUERY_TITLES_TABLE_CREATE = "create table "
      + SQL_TABLE_TITLES + " (" + SQL_COLUMN_LOCKSS_ID
      + " bigint NOT NULL PRIMARY KEY," + SQL_COLUMN_TITLE_NAME
      + " varchar(512) NOT NULL," + SQL_COLUMN_PUBLISHER_NAME
      + " varchar(512)," + SQL_COLUMN_PLATFORM_NAME + " varchar(512),"
      + SQL_COLUMN_DOI + " varchar(256)," + SQL_COLUMN_PROPRIETARY_ID
      + " varchar(256)," + SQL_COLUMN_IS_BOOK + " boolean NOT NULL,"
      + SQL_COLUMN_PRINT_ISSN + " varchar(9)," + SQL_COLUMN_ONLINE_ISSN
      + " varchar(9)," + SQL_COLUMN_ISBN + " varchar(15),"
      + SQL_COLUMN_BOOK_ISSN + " varchar(9))";

  // Query to create the table for recording requests used for COUNTER reports.
  private static final String SQL_QUERY_REQUESTS_TABLE_CREATE = "create table "
      + SQL_TABLE_REQUESTS + " (" + SQL_COLUMN_LOCKSS_ID
      + " bigint NOT NULL CONSTRAINT FK_LOCKSS_ID_REQUESTS REFERENCES "
      + SQL_TABLE_TITLES + "," + SQL_COLUMN_PUBLICATION_YEAR + " smallint,"
      + SQL_COLUMN_IS_SECTION + " boolean," + SQL_COLUMN_IS_HTML + " boolean,"
      + SQL_COLUMN_IS_PDF + " boolean," + SQL_COLUMN_IS_PUBLISHER_INVOLVED
      + " boolean NOT NULL," + SQL_COLUMN_REQUEST_YEAR + " smallint NOT NULL,"
      + SQL_COLUMN_REQUEST_MONTH + " smallint NOT NULL,"
      + SQL_COLUMN_REQUEST_DAY + " smallint NOT NULL,"
      + SQL_COLUMN_IN_AGGREGATION + " boolean)";

  // Query to create the table for recording type aggregates (PDF vs. HTML, Full
  // vs. Section, etc.) used for COUNTER reports.
  private static final String SQL_QUERY_TYPE_AGGREGATES_TABLE_CREATE =
      "create table "
	  + SQL_TABLE_TYPE_AGGREGATES
	  + " ("
	  + SQL_COLUMN_LOCKSS_ID
	  + " bigint NOT NULL CONSTRAINT FK_LOCKSS_ID_TYPE_AGGREGATES REFERENCES "
	  + SQL_TABLE_TITLES + "," + SQL_COLUMN_IS_PUBLISHER_INVOLVED
	  + " boolean NOT NULL," + SQL_COLUMN_REQUEST_YEAR
	  + " smallint NOT NULL," + SQL_COLUMN_REQUEST_MONTH
	  + " smallint NOT NULL," + SQL_COLUMN_TOTAL_JOURNAL_REQUESTS
	  + " integer," + SQL_COLUMN_HTML_JOURNAL_REQUESTS + " integer,"
	  + SQL_COLUMN_PDF_JOURNAL_REQUESTS + " integer,"
	  + SQL_COLUMN_FULL_BOOK_REQUESTS + " integer,"
	  + SQL_COLUMN_SECTION_BOOK_REQUESTS + " integer)";

  // Query to create the table for recording publication year aggregates used
  // for COUNTER reports.
  private static final String SQL_QUERY_PUBYEAR_AGGREGATES_TABLE_CREATE =
      "create table "
	  + SQL_TABLE_PUBYEAR_AGGREGATES
	  + " ("
	  + SQL_COLUMN_LOCKSS_ID
	  + " bigint NOT NULL CONSTRAINT FK_LOCKSS_ID_PUBYEAR_AGGREGATES REFERENCES "
	  + SQL_TABLE_TITLES + "," + SQL_COLUMN_IS_PUBLISHER_INVOLVED
	  + " boolean NOT NULL," + SQL_COLUMN_REQUEST_YEAR
	  + " smallint NOT NULL," + SQL_COLUMN_REQUEST_MONTH
	  + " smallint NOT NULL," + SQL_COLUMN_PUBLICATION_YEAR
	  + " smallint NOT NULL," + SQL_COLUMN_REQUEST_COUNT
	  + " integer NOT NULL)";

  // The SQL code used to create the database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> SQL_TABLE_CREATE_QUERIES =
      new LinkedHashMap<String, String>() {
	{
	  put(SQL_TABLE_TITLES, SQL_QUERY_TITLES_TABLE_CREATE);
	  put(SQL_TABLE_REQUESTS, SQL_QUERY_REQUESTS_TABLE_CREATE);
	  put(SQL_TABLE_PUBYEAR_AGGREGATES,
	      SQL_QUERY_PUBYEAR_AGGREGATES_TABLE_CREATE);
	  put(SQL_TABLE_TYPE_AGGREGATES, SQL_QUERY_TYPE_AGGREGATES_TABLE_CREATE);
	}
      };

  // An indication of whether this object is ready to be used.
  private boolean ready = false;

  // The directory where the report output files reside.
  private File outputDir = null;

  // The base directory for the reporting files.
  private File reportDir = null;

  // The LOCKSS identifier used for the aggregation of all books requests.
  private long allBooksLockssId = 0L;

  // The LOCKSS identifier used for the aggregation of all journals requests.
  private long allJournalsLockssId = 0L;

  /**
   * Starts the CounterReportsManager service.
   */
  @Override
  public void startService() {
    final String DEBUG_HEADER = "startService(): ";
    log.debug(DEBUG_HEADER + "Starting...");

    // Do nothing more if the configuration failed.
    if (!configure()) {
      return;
    }

    try {
      // Create the necessary tables if they do not exist.
      createTablesIfMissing();
    } catch (Exception e) {
      log.error("Cannot set up database tables", e);
      return;
    }

    CounterReportsBook allBooksTitle = null;
    CounterReportsJournal allJournalsTitle = null;

    try {
      // Dummy book used for the aggregation of all books.
      allBooksTitle =
	  new CounterReportsBook(ALL_BOOKS_TITLE_NAME, ALL_PUBLISHERS_NAME,
				 null, null, null, null, null);

      // Identify it and persist it.
      allBooksTitle.identify();

      // Set up the LOCKSS identifier of the aggregation of all books.
      allBooksLockssId = allBooksTitle.getLockssId();
      log.debug(DEBUG_HEADER + "allBooksLockssId = " + allBooksLockssId);
    } catch (Exception e) {
      log.error("Cannot set up the LOCKSS identifier for the book aggregation",
		e);
      return;
    }

    try {
      // Dummy journal used for the aggregation of all journals.
      allJournalsTitle =
	  new CounterReportsJournal(ALL_JOURNALS_TITLE_NAME,
				    ALL_PUBLISHERS_NAME, null, null, null,
				    null, null);

      // Identify it and persist it.
      allJournalsTitle.identify();

      // Set up the LOCKSS identifier of the aggregation of all journals.
      allJournalsLockssId = allJournalsTitle.getLockssId();
      log.debug(DEBUG_HEADER + "allJournalsLockssId = " + allJournalsLockssId);
    } catch (Exception e) {
      log.error("Cannot set up the LOCKSS identifier for the journal aggregation",
		e);
      return;
    }

    // Schedule the recurring task of aggregating requests data.
    Cron cron = (Cron) LockssDaemon.getManager(LockssDaemon.CRON);
    cron.addTask(new CounterReportsRequestAggregator(getDaemon()).getCronTask());
    log.debug(DEBUG_HEADER
	+ "CounterReportsRequestAggregator task added to cron.");

    ready = true;
  }

  /**
   * Handles configuration parameters.
   * 
   * @return <code>true</code> if the configuration is successful,
   *         <code>false</code> otherwise.
   */
  private boolean configure() {
    final String DEBUG_HEADER = "configure(): ";
    // Get the current configuration.
    Configuration config = ConfigManager.getCurrentConfig();

    // Specify the configured base directory for the reporting files.
    reportDir =
	new File(config.get(PARAM_REPORT_BASEDIR_PATH,
			    getDefaultTempDbRootDirectory()));
    log.debug(DEBUG_HEADER + "reportDir = '" + reportDir.getAbsolutePath()
	+ "'.");

    // Check whether it exists, creating it if necessary.
    if (FileUtil.ensureDirExists(reportDir)) {
      // Specify the configured directory where to put the report output files.
      outputDir =
	  new File(reportDir, config.get(PARAM_REPORT_OUTPUTDIR,
					 DEFAULT_REPORT_OUTPUTDIR));

      log.debug(DEBUG_HEADER + "outputDir = '" + outputDir.getAbsolutePath()
	  + "'.");

      // Check whether it exists, creating it if necessary.
      if (!FileUtil.ensureDirExists(outputDir)) {

	log.error("Error creating the report directory '"
	    + outputDir.getAbsolutePath() + "'.");
	return false;
      }
    } else {
      log.error("Error creating the report directory '"
	  + reportDir.getAbsolutePath() + "'.");
      return false;
    }

    log.debug(DEBUG_HEADER + "Done.");
    return true;
  }

  /**
   * Provides the default temporary root directory used to create the report
   * files.
   * 
   * @return a String with the root directory
   */
  private String getDefaultTempDbRootDirectory() {
    final String DEBUG_HEADER = "getDefaultTempDbRootDirectory(): ";
    String defaultTempDbRootDir = null;
    Configuration config = ConfigManager.getCurrentConfig();

    @SuppressWarnings("unchecked")
    List<String> dSpaceList =
	config.getList(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST);

    if (dSpaceList != null && !dSpaceList.isEmpty()) {
      defaultTempDbRootDir = dSpaceList.get(0);
    } else {
      defaultTempDbRootDir = config.get(ConfigManager.PARAM_TMPDIR);
    }

    log.debug(DEBUG_HEADER + "defaultTempDbRootDir = '" + defaultTempDbRootDir
	+ "'.");
    return defaultTempDbRootDir;
  }

  /**
   * Creates all the necessary database tables if they do not exist.
   * 
   * @throws BatchUpdateException
   * @throws SQLException
   */
  private void createTablesIfMissing() throws BatchUpdateException,
      SQLException {
    final String DEBUG_HEADER = "createTablesIfMissing(): ";
    DbManager dbManager = getDaemon().getDbManager();
    Connection conn = null;
    boolean success = false;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Loop through all the table names.
      for (String tableName : SQL_TABLE_CREATE_QUERIES.keySet()) {
	log.debug(DEBUG_HEADER + "Checking table = " + tableName);

	// Create the table if it does not exist.
	dbManager.createTableIfMissing(conn, tableName,
				       SQL_TABLE_CREATE_QUERIES.get(tableName));
      }

      success = true;
    } finally {
      if (success) {
	try {
	  conn.commit();
	  DbManager.safeCloseConnection(conn);
	} catch (SQLException sqle) {
	  log.error("Exception caught committing the connection", sqle);
	  DbManager.safeRollbackAndClose(conn);
	  ready = false;
	  throw sqle;
	}
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }
  }

  /**
   * Deletes a report output file.
   * 
   * @param fileName
   *          A String with the name of the report output file.
   * @return <code>true</code> if the file was deleted, <code>false</code>
   *         otherwise.
   */
  public boolean deleteReportOutputFile(String fileName) {
    // Do nothing more if the service is not ready to be used.
    if (!ready) {
      return false;
    }

    return new File(outputDir, fileName).delete();
  }

  /**
   * Provides the LOCKSS identifier used for the aggregation of all books
   * requests.
   * 
   * @return a long with the LOCKSS identifier used for the aggregation of all
   *         books requests.
   */
  public long getAllBooksLockssId() {
    return allBooksLockssId;
  }

  /**
   * Provides the LOCKSS identifier used for the aggregation of all journals
   * requests.
   * 
   * @return a long with the LOCKSS identifier used for the aggregation of all
   *         journals requests.
   */
  public long getAllJournalsLockssId() {
    return allJournalsLockssId;
  }

  /**
   * Provides the directory where the report output files reside.
   * 
   * @return a File with the directory where the report output files reside.
   */
  public File getOutputDir() {
    return outputDir;
  }

  /**
   * Provides the writer to the report output file, creating the file if
   * necessary.
   * 
   * @param fileName
   *          A String with the name of the report output file.
   * @return a PrintWriter for the report output file.
   * @throws IOException
   */
  public PrintWriter getReportOutputWriter(String fileName) throws IOException {
    final String DEBUG_HEADER = "getReportOutputWriter(): ";

    // Do nothing more if the service is not ready to be used.
    if (!ready) {
      return null;
    }

    // Make sure that only a single thread writes to the same file.
    synchronized (this) {
      File file = new File(outputDir, fileName);

      if (file.exists()) {
	log.debug(DEBUG_HEADER + "Collision with file '" + fileName + "'.");
	return null;
      }

      // Create a new writer for the report output file.
      return new PrintWriter(new FileWriter(file));
    }
  }

  /**
   * Provides an indication of whether this object is ready to be used.
   * 
   * @return <code>true</code> if this object is ready to be used,
   *         <code>false</code> otherwise.
   */
  public boolean isReady() {
    return ready;
  }

  /**
   * Persists the data involved in a request.
   * 
   * @param title
   *          A CounterReportsTitle representing the title involved in the
   *          request.
   * @param requestData
   *          A Map<String, Object> with properties of the request to be
   *          persisted.
   * @throws SQLException
   *           if there are problems accessing the database.
   * @throws Exception
   *           if there are problems computing the LOCKSS identifier.
   */
  public void persistRequest(CounterReportsTitle title,
      Map<String, Object> requestData) throws SQLException, Exception {
    final String DEBUG_HEADER = "persistRequest(): ";

    // Do nothing more if the service is not ready to be used.
    if (!ready) {
      return;
    }

    DbManager dbManager = getDaemon().getDbManager();
    Connection conn = null;
    boolean success = false;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Persist this title and the request.
      title.persistRequest(requestData, conn);

      success = true;
    } finally {
      if (success) {
	conn.commit();
	log.debug(DEBUG_HEADER + "Successful commit.");
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }
  }
}
