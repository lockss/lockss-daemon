/*
 * $Id$
 */

/*

 Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Cron;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.util.FileUtil;
import org.lockss.util.Logger;
import org.lockss.util.TimeBase;

/**
 * Service used to manage COUNTER reports.
 * 
 * @version 1.0
 */
public class CounterReportsManager extends BaseLockssDaemonManager {
  // Prefix for the reporting configuration entries.
  public static final String PREFIX = Configuration.PREFIX + "report.";

  /**
   * Indication of whether the COUNTER reports subsystem should be enabled.
   * <p />
   * Defaults to false. If the COUNTER reports subsystem is not enabled, no
   * statistics are collected or aggregated. Changes require daemon restart.
   */
  public static final String PARAM_COUNTER_ENABLED = PREFIX + "counterEnabled";

  /**
   * Default value of COUNTER reports subsystem operation configuration
   * parameter.
   * <p />
   * <code>false</code> to disable, <code>true</code> to enable.
   */
  public static final boolean DEFAULT_COUNTER_ENABLED = false;

  /**
   * Base directory for reporting.
   * <p />
   * Defaults to <code><i>daemon_tmpdir</i>/report</code>. Changes require
   * daemon restart.
   */
  public static final String PARAM_REPORT_BASEDIR_PATH = PREFIX
      + "baseDirectoryPath";

  public static final String DEFAULT_REPORT_BASEDIR = "report";

  public static final String DEFAULT_REPORT_BASEDIR_PATH = "<tmpdir>/"
      + DEFAULT_REPORT_BASEDIR;

  /**
   * Name of the directory used to store the report output files.
   * <p />
   * Defaults to <code>output</code>. Changes require daemon restart.
   */
  public static final String PARAM_REPORT_OUTPUTDIR = PREFIX
      + "reportOutputDirectoryName";

  public static final String DEFAULT_REPORT_OUTPUTDIR = "output";

  // Aggregations names.
  public static final String ALL_BOOKS_NAME = "COUNTER REPORTS ALL BOOKS";
  public static final String ALL_JOURNALS_NAME = "COUNTER REPORTS ALL JOURNALS";
  public static final String ALL_PUBLISHERS_NAME =
      "COUNTER REPORTS ALL PUBLISHERS";

  // The reports extensions.
  public static final String CSV_EXTENSION = "csv";
  public static final String TSV_EXTENSION = "txt";

  private static final Logger log =
      Logger.getLogger(CounterReportsManager.class);

  // Query to insert URL requests used for COUNTER reports.
  private static final String SQL_QUERY_URL_REQUEST_INSERT = "insert into "
      + COUNTER_REQUEST_TABLE
      + " (" + URL_COLUMN
      + "," + IS_PUBLISHER_INVOLVED_COLUMN
      + "," + REQUEST_YEAR_COLUMN
      + "," + REQUEST_MONTH_COLUMN
      + "," + REQUEST_DAY_COLUMN
      + ") values (?,?,?,?,?)";

  // Query to get the aggregated type requests for a book during a month.
  private static final String SQL_QUERY_BOOK_TYPE_AGGREGATE_SELECT = "select "
      + FULL_REQUESTS_COLUMN + ","
      + SECTION_REQUESTS_COLUMN
      + " from " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ? "
      + "and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ? "
      + "and " + REQUEST_YEAR_COLUMN + " = ? "
      + "and " + REQUEST_MONTH_COLUMN + " = ?";

  // Query to update book type aggregates.
  private static final String SQL_QUERY_BOOK_TYPE_AGGREGATE_UPDATE = "update "
      + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
      + " set " + FULL_REQUESTS_COLUMN + " = ?,"
      + SECTION_REQUESTS_COLUMN + " = ? "
      + "where " + PUBLICATION_SEQ_COLUMN + " = ? "
      + "and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ? "
      + "and " + REQUEST_YEAR_COLUMN + " = ? "
      + "and " + REQUEST_MONTH_COLUMN + " = ?";

  // Query to insert book type aggregates.
  private static final String SQL_QUERY_BOOK_TYPE_AGGREGATE_INSERT = "insert "
      + "into " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + ","
      + IS_PUBLISHER_INVOLVED_COLUMN + ","
      + REQUEST_YEAR_COLUMN + ","
      + REQUEST_MONTH_COLUMN + ","
      + FULL_REQUESTS_COLUMN + ","
      + SECTION_REQUESTS_COLUMN + ") values (?,?,?,?,?,?)";

  // Query to get the aggregated type requests for a journal during a month.
  private static final String SQL_QUERY_JOURNAL_TYPE_AGGREGATE_SELECT =
      "select "
      + TOTAL_REQUESTS_COLUMN + ","
      + HTML_REQUESTS_COLUMN + ","
      + PDF_REQUESTS_COLUMN
      + " from " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ? "
      + "and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ? "
      + "and " + REQUEST_YEAR_COLUMN + " = ? "
      + "and " + REQUEST_MONTH_COLUMN + " = ?";

  // Query to update journal type aggregates.
  private static final String SQL_QUERY_JOURNAL_TYPE_AGGREGATE_UPDATE =
      "update "
      + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
      + " set " + TOTAL_REQUESTS_COLUMN + " = ?,"
      + HTML_REQUESTS_COLUMN + " = ?,"
      + PDF_REQUESTS_COLUMN + " = ? "
      + "where " + PUBLICATION_SEQ_COLUMN + " = ? "
      + "and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ? "
      + "and " + REQUEST_YEAR_COLUMN + " = ? "
      + "and " + REQUEST_MONTH_COLUMN + " = ?";

  // Query to insert journal type aggregates.
  private static final String SQL_QUERY_JOURNAL_TYPE_AGGREGATE_INSERT =
      "insert into " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + ","
      + IS_PUBLISHER_INVOLVED_COLUMN + ","
      + REQUEST_YEAR_COLUMN + ","
      + REQUEST_MONTH_COLUMN + ","
      + TOTAL_REQUESTS_COLUMN + ","
      + HTML_REQUESTS_COLUMN + ","
      + PDF_REQUESTS_COLUMN + ") values (?,?,?,?,?,?,?)";

  // Query to get the aggregated publication year requests for a journal during
  // a month.
  private static final String SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATE_SELECT =
      "select "
      + REQUESTS_COLUMN
      + " from " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ? "
      + "and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ? "
      + "and " + REQUEST_YEAR_COLUMN + " = ? "
      + "and " + REQUEST_MONTH_COLUMN + " = ? "
      + "and " + PUBLICATION_YEAR_COLUMN + " = ?";

  // Query to update journal publication year aggregates.
  private static final String SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATE_UPDATE =
      "update "
      + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
      + " set " + REQUESTS_COLUMN + " = ? "
      + "where " + PUBLICATION_SEQ_COLUMN + " = ? "
      + "and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ? "
      + "and " + REQUEST_YEAR_COLUMN + " = ? "
      + "and " + REQUEST_MONTH_COLUMN + " = ? "
      + "and " + PUBLICATION_YEAR_COLUMN + " = ?";

  // Query to insert journal publication year aggregates.
  private static final String SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATE_INSERT =
      "insert into " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + ","
      + IS_PUBLISHER_INVOLVED_COLUMN + ","
      + REQUEST_YEAR_COLUMN + ","
      + REQUEST_MONTH_COLUMN + ","
      + PUBLICATION_YEAR_COLUMN + ","
      + REQUESTS_COLUMN + ") values (?,?,?,?,?,?)";

  // Query to get all the aggregated type requests for a book.
  private static final String SQL_QUERY_BOOK_TYPE_AGGREGATES_SELECT = "select "
      + IS_PUBLISHER_INVOLVED_COLUMN
      + "," + REQUEST_YEAR_COLUMN
      + "," + REQUEST_MONTH_COLUMN
      + "," + FULL_REQUESTS_COLUMN
      + "," + SECTION_REQUESTS_COLUMN
      + " from " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ?";

  // Query to delete the aggregated type requests for a book.
  private static final String SQL_QUERY_BOOK_TYPE_AGGREGATES_DELETE = "delete"
      + " from " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ?";

  // Query to get all the aggregated type requests for a journal.
  private static final String SQL_QUERY_JOURNAL_TYPE_AGGREGATES_SELECT =
      "select "
      + IS_PUBLISHER_INVOLVED_COLUMN
      + "," + REQUEST_YEAR_COLUMN
      + "," + REQUEST_MONTH_COLUMN
      + "," + TOTAL_REQUESTS_COLUMN
      + "," + HTML_REQUESTS_COLUMN
      + "," + PDF_REQUESTS_COLUMN
      + " from " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ?";

  // Query to delete the aggregated type requests for a journal.
  private static final String SQL_QUERY_JOURNAL_TYPE_AGGREGATES_DELETE =
      "delete"
      + " from " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ?";

  // Query to get all the aggregated publication year requests for a journal.
  private static final String SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATES_SELECT =
      "select "
      + IS_PUBLISHER_INVOLVED_COLUMN
      + "," + REQUEST_YEAR_COLUMN
      + "," + REQUEST_MONTH_COLUMN
      + "," + PUBLICATION_YEAR_COLUMN
      + "," + REQUESTS_COLUMN
      + " from " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ?";

  // Query to delete the aggregated type requests for a journal.
  private static final String SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATES_DELETE =
      "delete"
      + " from " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ?";

  // An indication of whether this object is ready to be used.
  private boolean ready = false;

  // The directory where the report output files reside.
  private File outputDir = null;

  // The base directory for the reporting files.
  private File reportDir = null;

  // The identifier of the dummy publication used for the aggregation of all
  // books requests.
  private Long allBooksPublicationSeq = null;

  // The identifier of the dummy publication used for the aggregation of all
  // journals requests.
  private Long allJournalsPublicationSeq = null;

  /** The database manager */
  private DbManager dbManager = null;

  /** The metadata manager */
  private MetadataManager metadataManager = null;

  /**
   * Starts the CounterReportsManager service.
   */
  @Override
  public void startService() {
    final String DEBUG_HEADER = "startService(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    // Do nothing more if the configuration failed.
    if (!configure()) {
      return;
    }

    dbManager = getDaemon().getDbManager();
    Connection conn;

    // Get a connection to the database.
    try {
      conn = dbManager.getConnection();
    } catch (DbException ex) {
      log.error("Cannot connect to database", ex);
      return;
    }

    metadataManager = getDaemon().getMetadataManager();
    String errorMessage = null;
    boolean success = false;

    try {
      errorMessage = "Cannot get the identifier of the publisher used for the "
	  + "aggregation of all title requests";

      Long publisherSeq =
	  metadataManager.findOrCreatePublisher(conn, ALL_PUBLISHERS_NAME);
      log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);

      if (publisherSeq == null) {
	log.error(errorMessage);
	return;
      }

      errorMessage = "Cannot get the identifier of the publication used for "
	  + "the aggregation of all books requests";

      // Get the identifier of the dummy publication used for the aggregation of
      // all book requests.
      allBooksPublicationSeq = metadataManager
	  .findOrCreateBook(conn, publisherSeq, null,  
	      "CRBPISBN", "CRBEISBN", ALL_BOOKS_NAME, null);
      log.debug2(DEBUG_HEADER + "allBooksPublicationSeq = "
	  + allBooksPublicationSeq);

      if (allBooksPublicationSeq == null) {
	log.error(errorMessage);
	return;
      }

      errorMessage = "Cannot get the identifier of the publication used for "
	  + "the aggregation of all journal requests";

      // Get the identifier of the dummy publication used for the aggregation of
      // all journal requests.
      allJournalsPublicationSeq = metadataManager
	  .findOrCreateJournal(conn, publisherSeq, 
	                       "CRJPISSN", "CRJEISSN", ALL_JOURNALS_NAME, null);
      log.debug2(DEBUG_HEADER + "allJournalsPublicationSeq = "
	  + allJournalsPublicationSeq);

      if (allJournalsPublicationSeq == null) {
	log.error(errorMessage);
	return;
      }

      success = true;
    } catch (DbException sqle) {
      log.error(errorMessage, sqle);
    } finally {
      if (success) {
	try {
	  conn.commit();
	  DbManager.safeCloseConnection(conn);
	} catch (SQLException sqle) {
	  log.error("Exception caught committing the connection", sqle);
	  DbManager.safeRollbackAndClose(conn);
	  success = false;
	}
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    if (!success) {
      return;
    }

    // Schedule the recurring task of aggregating requests data.
    Cron cron = (Cron) LockssDaemon.getManager(LockssDaemon.CRON);
    cron.addTask(new CounterReportsRequestAggregator(getDaemon())
    	.getCronTask());
    log.debug2(DEBUG_HEADER
	+ "CounterReportsRequestAggregator task added to cron.");

    ready = true;
  }

  /**
   * Handles configuration parameters.
   * 
   * @return <code>true</code> if the configuration is enabled and successful,
   *         <code>false</code> otherwise.
   */
  private boolean configure() {
    final String DEBUG_HEADER = "configure(): ";
    // Get the current configuration.
    Configuration config = ConfigManager.getCurrentConfig();

    // Check whether COUNTER reports should be disabled.
    if (!config.getBoolean(PARAM_COUNTER_ENABLED, DEFAULT_COUNTER_ENABLED)) {
      // Yes: Do nothing more.
      log.debug2(DEBUG_HEADER + "COUNTER reports are disabled.");
      return false;
    }

    // Specify the configured base directory for the reporting files.
    reportDir = new File(config.get(PARAM_REPORT_BASEDIR_PATH,
				    getDefaultTempRootDirectory()));
    log.debug2(DEBUG_HEADER + "reportDir = '" + reportDir.getAbsolutePath()
	+ "'.");

    // Check whether it exists, creating it if necessary.
    if (FileUtil.ensureDirExists(reportDir)) {
      // Specify the configured directory where to put the report output files.
      outputDir =
	  new File(reportDir, config.get(PARAM_REPORT_OUTPUTDIR,
					 DEFAULT_REPORT_OUTPUTDIR));

      log.debug2(DEBUG_HEADER + "outputDir = '" + outputDir.getAbsolutePath()
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

    log.debug2(DEBUG_HEADER + "Done.");
    return true;
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
   * Provides the identifier of the dummy publication used for the aggregation
   * of all books requests.
   * 
   * @return a Long with the identifier of the dummy publication used for the
   *         aggregation of all books requests.
   */
  public Long getAllBooksPublicationSeq() {
    return allBooksPublicationSeq;
  }

  /**
   * Provides the identifier of the dummy publication used for the aggregation
   * of all journals requests.
   * 
   * @return a Long with the identifier of the dummy publication used for the
   *         aggregation of all journals requests.
   */
  public Long getAllJournalsPublicationSeq() {
    return allJournalsPublicationSeq;
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
	log.debug2(DEBUG_HEADER + "Collision with file '" + fileName + "'.");
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
   * @param url
   *          A String with the requested URL.
   * @param isPublisherInvolved
   *          A boolean indicating the involvement of the publisher.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  public void persistRequest(String url, boolean isPublisherInvolved)
      throws DbException {
    final String DEBUG_HEADER = "persistRequest(): ";

    // Do nothing more if the service is not ready to be used.
    if (!ready) {
      return;
    }

    Connection conn = null;
    boolean success = false;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Get the date of the request.
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(TimeBase.nowDate());

      int requestYear = calendar.get(Calendar.YEAR);
      log.debug2(DEBUG_HEADER + "requestYear = " + requestYear);
      int requestMonth = (calendar.get(Calendar.MONTH) + 1);
      log.debug2(DEBUG_HEADER + "requestMonth = " + requestMonth);
      int requestDay = calendar.get(Calendar.DAY_OF_MONTH);
      log.debug2(DEBUG_HEADER + "requestDay = " + requestDay);

      String sql = SQL_QUERY_URL_REQUEST_INSERT;
      log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");
      PreparedStatement insertRequest = null;

      try {
        // Prepare the statement used to persist the request.
        insertRequest = dbManager.prepareStatement(conn, sql);

        short index = 1;

        // Populate the URL.
        insertRequest.setString(index++, url);

        // Populate the indication of whether this record corresponds to the
        // serving of the request by the publisher.
        insertRequest.setBoolean(index++, isPublisherInvolved);

        // Populate the year of the request.
        insertRequest.setShort(index++, (short) requestYear);

        // Populate the month of the request.
        insertRequest.setShort(index++, (short) requestMonth);

        // Populate the day of the request.
        insertRequest.setShort(index++, (short) requestDay);

        // Insert the record.
        int count = dbManager.executeUpdate(insertRequest);
        log.debug2(DEBUG_HEADER + "count = " + count);
      } catch (SQLException sqle) {
        log.error("Cannot persist URL request", sqle);
        log.error("URL = '" + url + "'.");
        log.error("SQL = '" + sql + "'.");
        throw new DbException("Cannot persist URL request", sqle);
      } finally {
        DbManager.safeCloseStatement(insertRequest);
      }

      success = true;
    } finally {
      if (success) {
	DbManager.commitOrRollback(conn, log);
	log.debug2(DEBUG_HEADER + "Successful commit.");
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }
  }

  /**
   * Provides the existing type aggregates of requests for a book during a
   * month.
   * 
   * @param year
   *          An int with the year of the month for which the aggregates are to
   *          be provided.
   * @param month
   *          An int with the month for which the aggregates are to be provided.
   * @param publicationSeq
   *          A Long with the identifier of the book involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @return a Map<String, Integer> with the existing aggregated request counts
   *         or <code>null</code> if there are none.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  Map<String, Integer> getExistingAggregateMonthBookTypes(int year,
      int month, Long publicationSeq, boolean publisherInvolved,
      Connection conn) throws DbException {
    final String DEBUG_HEADER = "getExistingAggregateMonthBookTypes(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Map<String, Integer> aggregates = null;

    String sql = SQL_QUERY_BOOK_TYPE_AGGREGATE_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the existing aggregates.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the book identifier.
      statement.setLong(index++, publicationSeq);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	aggregates = new HashMap<String, Integer>();

	int fullCount = resultSet.getInt(FULL_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "fullCount = " + fullCount);

	int sectionCount = resultSet.getInt(SECTION_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "sectionCount = " + sectionCount);

	aggregates.put(FULL_REQUESTS_COLUMN, fullCount);
	aggregates.put(SECTION_REQUESTS_COLUMN, sectionCount);
      }

    } catch (SQLException sqle) {
      log.error("Cannot get the existing month book request aggregates", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException(
	  "Cannot get the existing month book request aggregates", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return aggregates;
  }

  /**
   * Updates in the database a row with the aggregates for a book during a
   * month.
   * 
   * @param year
   *          An int with the year corresponding to the month.
   * @param month
   *          An int with the month.
   * @param publicationSeq
   *          A long with the identifier of the book involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param fullCount
   *          An int with the count of full requests for the book during the
   *          month.
   * @param sectionCount
   *          An int with the count of section requests for the book during the
   *          month.
   * @param conn
   *          A Connection representing the database connection.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  void updateBookTypeAggregate(int year, int month, Long publicationSeq,
      boolean publisherInvolved, int fullCount, int sectionCount,
      Connection conn) throws DbException {
    final String DEBUG_HEADER = "updateBookTypeAggregate(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);
    log.debug2(DEBUG_HEADER + "fullCount = " + fullCount);
    log.debug2(DEBUG_HEADER + "sectionCount = " + sectionCount);

    PreparedStatement updateAggregate = null;
    String sql = SQL_QUERY_BOOK_TYPE_AGGREGATE_UPDATE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      updateAggregate = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the full count.
      updateAggregate.setInt(index++, fullCount);

      // Populate the section count.
      updateAggregate.setInt(index++, sectionCount);

      // Populate the book identifier.
      updateAggregate.setLong(index++, publicationSeq);

      // Populate the publisher involvement indicator.
      updateAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      updateAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      updateAggregate.setShort(index++, (short) month);

      // Update the record.
      int count = dbManager.executeUpdate(updateAggregate);
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update the month book request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("fullCount = " + fullCount);
      log.error("sectionCount = " + sectionCount);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot update the month book request counts",
	  sqle);
    } finally {
      DbManager.safeCloseStatement(updateAggregate);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Inserts in the database a row with the aggregates for a book during a
   * month.
   * 
   * @param year
   *          An int with the year corresponding to the month.
   * @param month
   *          An int with the month.
   * @param publicationSeq
   *          A Long with the identifier of the book involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param fullCount
   *          An int with the count of full requests for the book during the
   *          month.
   * @param sectionCount
   *          An int with the count of section requests for the book during the
   *          month.
   * @param conn
   *          A Connection representing the database connection.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  void insertBookTypeAggregate(int year, int month, Long publicationSeq,
      boolean publisherInvolved, int fullCount, int sectionCount,
      Connection conn) throws DbException {
    final String DEBUG_HEADER = "insertBookTypeAggregate(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);
    log.debug2(DEBUG_HEADER + "fullCount = " + fullCount);
    log.debug2(DEBUG_HEADER + "sectionCount = " + sectionCount);

    PreparedStatement insertAggregate = null;
    String sql = SQL_QUERY_BOOK_TYPE_AGGREGATE_INSERT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      insertAggregate = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the book identifier.
      insertAggregate.setLong(index++, publicationSeq);

      // Populate the publisher involvement indicator.
      insertAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      insertAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      insertAggregate.setShort(index++, (short) month);

      // Populate the full requests.
      insertAggregate.setInt(index++, fullCount);

      // Populate the section requests.
      insertAggregate.setInt(index++, sectionCount);

      // Insert the record.
      int count = dbManager.executeUpdate(insertAggregate);
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot insert the month book request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("fullCount = " + fullCount);
      log.error("sectionCount = " + sectionCount);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot insert the month book request counts",
	  sqle);
    } finally {
      DbManager.safeCloseStatement(insertAggregate);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the existing type aggregates of requests for a journal during a
   * month.
   * 
   * @param year
   *          An int with the year of the month for which the aggregates are to
   *          be provided.
   * @param month
   *          An int with the month for which the aggregates are to be provided.
   * @param publicationSeq
   *          A Long with the identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @return a Map<String, Integer> with the existing aggregated request counts
   *         or <code>null</code> if there are none.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  Map<String, Integer> getExistingAggregateMonthJournalTypes(int year,
      int month, Long publicationSeq, boolean publisherInvolved,
      Connection conn) throws DbException {
    final String DEBUG_HEADER = "getExistingAggregateMonthJournalTypes(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Map<String, Integer> aggregates = null;

    String sql = SQL_QUERY_JOURNAL_TYPE_AGGREGATE_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the existing aggregations.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the journal identifier.
      statement.setLong(index++, publicationSeq);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Get the existing requests counts.
      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	aggregates = new HashMap<String, Integer>();

	int totalCount = resultSet.getInt(TOTAL_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "totalCount = " + totalCount);

	int htmlCount = resultSet.getInt(HTML_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "htmlCount = " + htmlCount);

	int pdfCount = resultSet.getInt(PDF_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "pdfCount = " + pdfCount);

	aggregates.put(TOTAL_REQUESTS_COLUMN, totalCount);
	aggregates.put(HTML_REQUESTS_COLUMN, htmlCount);
	aggregates.put(PDF_REQUESTS_COLUMN, pdfCount);
      }

    } catch (SQLException sqle) {
      log.error("Cannot get the existing month journal request aggregates",
	  sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException(
	  "Cannot get the existing month journal request aggregates", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return aggregates;
  }

  /**
   * Updates in the database a row with the aggregates for a journal during a
   * month.
   * 
   * @param year
   *          An int with the year corresponding to the month.
   * @param month
   *          An int with the month.
   * @param publicationSeq
   *          A Long with the identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param totalCount
   *          An int with the total count of requests for the journal during the
   *          month.
   * @param htmlCount
   *          An int with the count of HTML requests for the journal during the
   *          month.
   * @param pdfCount
   *          An int with the count of PDF requests for the journal during the
   *          month.
   * @param conn
   *          A Connection representing the database connection.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  void updateJournalTypeAggregate(int year, int month,
      Long publicationSeq, boolean publisherInvolved, int totalCount,
      int htmlCount, int pdfCount, Connection conn) throws DbException {
    final String DEBUG_HEADER = "updateJournalTypeAggregate(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);
    log.debug2(DEBUG_HEADER + "totalCount = " + totalCount);
    log.debug2(DEBUG_HEADER + "htmlCount = " + htmlCount);
    log.debug2(DEBUG_HEADER + "pdfCount = " + pdfCount);

    PreparedStatement updateAggregate = null;
    String sql = SQL_QUERY_JOURNAL_TYPE_AGGREGATE_UPDATE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      updateAggregate = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the total count.
      updateAggregate.setInt(index++, totalCount);

      // Populate the HTML count.
      updateAggregate.setInt(index++, htmlCount);

      // Populate the PDF count.
      updateAggregate.setInt(index++, pdfCount);

      // Populate the journal identifier.
      updateAggregate.setLong(index++, publicationSeq);

      // Populate the publisher involvement indicator.
      updateAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      updateAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      updateAggregate.setShort(index++, (short) month);

      // Update the record.
      int count = dbManager.executeUpdate(updateAggregate);
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update the month journal request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("totalCount = " + totalCount);
      log.error("htmlCount = " + htmlCount);
      log.error("pdfCount = " + pdfCount);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot update the month journal request counts",
	  sqle);
    } finally {
      DbManager.safeCloseStatement(updateAggregate);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Inserts in the database a row with the type aggregates for a journal during
   * a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param publicationSeq
   *          A Long with the identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param totalCount
   *          An int with the total count of requests for the journal during the
   *          month.
   * @param htmlCount
   *          An int with the count of HTML requests for the journal during the
   *          month.
   * @param pdfCount
   *          An int with the count of PDF requests for the journal during the
   *          month.
   * @param conn
   *          A Connection representing the database connection.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  void insertJournalTypeAggregate(int year, int month,
      Long publicationSeq, boolean publisherInvolved, int totalCount,
      int htmlCount, int pdfCount, Connection conn) throws DbException {
    final String DEBUG_HEADER = "insertJournalTypeAggregate(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);
    log.debug2(DEBUG_HEADER + "totalCount = " + totalCount);
    log.debug2(DEBUG_HEADER + "htmlCount = " + htmlCount);
    log.debug2(DEBUG_HEADER + "pdfCount = " + pdfCount);

    PreparedStatement insertAggregate = null;
    String sql = SQL_QUERY_JOURNAL_TYPE_AGGREGATE_INSERT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      insertAggregate = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the identifier.
      insertAggregate.setLong(index++, publicationSeq);

      // Populate the publisher involvement indicator.
      insertAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      insertAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      insertAggregate.setShort(index++, (short) month);

      // Populate the total requests.
      insertAggregate.setInt(index++, totalCount);

      // Populate the HTML requests.
      insertAggregate.setInt(index++, htmlCount);

      // Populate the PDF requests.
      insertAggregate.setInt(index++, pdfCount);

      // Insert the record.
      int count = dbManager.executeUpdate(insertAggregate);
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot insert the month journal request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("totalCount = " + totalCount);
      log.error("htmlCount = " + htmlCount);
      log.error("pdfCount = " + pdfCount);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot insert the month journal request counts",
	  sqle);
    } finally {
      DbManager.safeCloseStatement(insertAggregate);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the existing publication year aggregate of requests for a journal
   * during a month.
   * 
   * @param year
   *          An int with the year of the month for which the aggregates are to
   *          be provided.
   * @param month
   *          An int with the month for which the aggregates are to be provided.
   * @param publicationSeq
   *          A Long with the identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param publicationYear
   *          An int with the publication year.
   * @param conn
   *          A Connection representing the database connection.
   * @return an Integer with the existing aggregated request count or
   *         <code>null</code> if there is none.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  Integer getExistingAggregateMonthJournalPubYear(int year, int month,
      Long publicationSeq, boolean publisherInvolved, int publicationYear,
      Connection conn) throws DbException {
    final String DEBUG_HEADER = "getExistingAggregateMonthJournalPubYear(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);
    log.debug2(DEBUG_HEADER + "publicationYear = " + publicationYear);

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Integer count = null;

    String sql = SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATE_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the existing aggregation.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the journal identifier.
      statement.setLong(index++, publicationSeq);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publication year.
      statement.setInt(index++, publicationYear);

      // Get the existing requests count.
      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	count = resultSet.getInt(REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "count = " + count);
      }

    } catch (SQLException sqle) {
      log.error("Cannot get the existing month journal publication year "
	  + "request aggregate", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("publicationYear = " + publicationYear);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot get the existing month journal publication "
	  + "year request aggregate", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return count;
  }

  /**
   * Updates in the database the row with the publication year aggregate for a
   * journal during a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param publicationSeq
   *          A Long with the identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param publicationYear
   *          An int with the publication year.
   * @param requestCount
   *          An int with the count of requests for the publication year for the
   *          journal during the month.
   * @param conn
   *          A Connection representing the database connection.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  void updateTitlePubYearAggregate(int year, int month,
      Long publicationSeq, boolean publisherInvolved, int publicationYear,
      int requestCount, Connection conn) throws DbException {
    final String DEBUG_HEADER = "updateTitlePubYearAggregate(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);
    log.debug2(DEBUG_HEADER + "publicationYear = " + publicationYear);
    log.debug2(DEBUG_HEADER + "requestCount = " + requestCount);

    PreparedStatement updateAggregate = null;
    String sql = SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATE_UPDATE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      updateAggregate = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the request count.
      updateAggregate.setInt(index++, requestCount);

      // Populate the journal identifier.
      updateAggregate.setLong(index++, publicationSeq);

      // Populate the publisher involvement indicator.
      updateAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      updateAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      updateAggregate.setShort(index++, (short) month);

      // Populate the publication year.
      updateAggregate.setShort(index++, (short) publicationYear);

      // Insert the record.
      int count = dbManager.executeUpdate(updateAggregate);
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update the month journal request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("publicationYear = " + publicationYear);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot update the month journal request counts",
	  sqle);
    } finally {
      DbManager.safeCloseStatement(updateAggregate);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Inserts in the database a row with the publication year aggregate for a
   * journal during a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param publicationSeq
   *          A Long with the identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param publicationYear
   *          An int with the publication year.
   * @param requestCount
   *          An int with the count of requests for the publication year for the
   *          journal during the month.
   * @param conn
   *          A Connection representing the database connection.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  void insertTitlePubYearAggregate(int year, int month,
      Long publicationSeq, boolean publisherInvolved, int publicationYear,
      int requestCount, Connection conn) throws DbException {
    final String DEBUG_HEADER = "insertTitlePubYearAggregate(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);
    log.debug2(DEBUG_HEADER + "publicationYear = " + publicationYear);
    log.debug2(DEBUG_HEADER + "requestCount = " + requestCount);

    PreparedStatement insertAggregate = null;
    String sql = SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATE_INSERT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      insertAggregate = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the journal identifier.
      insertAggregate.setLong(index++, publicationSeq);

      // Populate the publisher involvement indicator.
      insertAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      insertAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      insertAggregate.setShort(index++, (short) month);

      // Populate the publication year.
      insertAggregate.setShort(index++, (short) publicationYear);

      // Populate the request count.
      insertAggregate.setInt(index++, requestCount);

      // Insert the record.
      int count = dbManager.executeUpdate(insertAggregate);
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot insert the month journal request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("publicationYear = " + publicationYear);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot insert the month journal request counts",
	  sqle);
    } finally {
      DbManager.safeCloseStatement(insertAggregate);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Merges the book type aggregates of a publication into another publication.
   * 
   * @param conn
   *          A Connection representing the database connection.
   * @param sourcePublicationSeq
   *          A Long with the identifier of the source book involved.
   * @param targetPublicationSeq
   *          A Long with the identifier of the target book involved.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  public void mergeBookTypeAggregates(Connection conn,
      Long sourcePublicationSeq, Long targetPublicationSeq)
	  throws DbException {
    final String DEBUG_HEADER = "mergeBookTypeAggregates(): ";

    // Do nothing more if the service is not ready to be used.
    if (!ready) {
      return;
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    boolean publisherInvolved;
    int year;
    int month;
    int fullCount;
    int sectionCount;
    Map<String, Integer> previousAggregate = null;
    int previousFullCount = 0;
    int previousSectionCount = 0;

    String sql = SQL_QUERY_BOOK_TYPE_AGGREGATES_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the existing aggregates.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the identifier of the source book.
      statement.setLong(index++, sourcePublicationSeq);

      resultSet = dbManager.executeQuery(statement);

      // Loop through the existing aggregates.
      while (resultSet.next()) {
	publisherInvolved = resultSet.getBoolean(IS_PUBLISHER_INVOLVED_COLUMN);
	log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

	year = resultSet.getInt(REQUEST_YEAR_COLUMN);
	log.debug2(DEBUG_HEADER + "year = " + year);

	month = resultSet.getInt(REQUEST_MONTH_COLUMN);
	log.debug2(DEBUG_HEADER + "month = " + month);

	fullCount = resultSet.getInt(FULL_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "fullCount = " + fullCount);

	sectionCount = resultSet.getInt(SECTION_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "sectionCount = " + sectionCount);

	// Retrieve the previous aggregate of requests for the target book.
	previousAggregate = getExistingAggregateMonthBookTypes(year,
	    month, targetPublicationSeq, publisherInvolved, conn);

	// Check whether there were previously recorded aggregates.
	if (previousAggregate != null) {
	  // Yes: Get the previously recorded aggregates.
	  previousFullCount = previousAggregate.get(FULL_REQUESTS_COLUMN);
	  log.debug2(DEBUG_HEADER + "previousFullCount = " + previousFullCount);

	  previousSectionCount = previousAggregate.get(SECTION_REQUESTS_COLUMN);
	  log.debug2(DEBUG_HEADER + "previousSectionCount = "
	      + previousSectionCount);

	  // Update the existing row in the database.
	  updateBookTypeAggregate(year, month, targetPublicationSeq,
	                          publisherInvolved,
				  previousFullCount + fullCount,
				  previousSectionCount + sectionCount, conn);
	} else {
	  // No: Insert a new row in the database.
	  insertBookTypeAggregate(year, month, targetPublicationSeq,
	                          publisherInvolved, fullCount, sectionCount,
	                          conn);
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot merge book type aggregates", sqle);
      log.error("sourcePublicationSeq = " + sourcePublicationSeq);
      log.error("targetPublicationSeq = " + targetPublicationSeq);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot merge book type aggregates", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Deletes all the type aggregates of a book.
   * 
   * @param conn
   *          A Connection representing the database connection.
   * @param publicationSeq
   *          A Long with the identifier of the book involved.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  public void deleteBookTypeAggregates(Connection conn, Long publicationSeq)
      throws DbException {
    final String DEBUG_HEADER = "deleteBookTypeAggregates(): ";
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    // Do nothing more if the service is not ready to be used.
    if (!ready) {
      return;
    }

    PreparedStatement deleteAggregate = null;
    String sql = SQL_QUERY_BOOK_TYPE_AGGREGATES_DELETE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to delete the rows.
      deleteAggregate = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the book identifier.
      deleteAggregate.setLong(index++, publicationSeq);

      // Delete the records.
      int count = dbManager.executeUpdate(deleteAggregate);
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot delete the book type aggregates", sqle);
      log.error("publicationSeq = " + publicationSeq);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot delete the book type aggregates", sqle);
    } finally {
      DbManager.safeCloseStatement(deleteAggregate);
    }
  }

  /**
   * Merges the journal type aggregates of a publication into another
   * publication.
   * 
   * @param conn
   *          A Connection representing the database connection.
   * @param sourcePublicationSeq
   *          A Long with the identifier of the source journal involved.
   * @param targetPublicationSeq
   *          A Long with the identifier of the target journal involved.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  public void mergeJournalTypeAggregates(Connection conn,
      Long sourcePublicationSeq, Long targetPublicationSeq)
	  throws DbException {
    final String DEBUG_HEADER = "mergeJournalTypeAggregates(): ";

    // Do nothing more if the service is not ready to be used.
    if (!ready) {
      return;
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    boolean publisherInvolved;
    int year;
    int month;
    int totalCount;
    int htmlCount;
    int pdfCount;
    Map<String, Integer> previousAggregate = null;
    int previousTotalCount = 0;
    int previousHtmlCount = 0;
    int previousPdfCount = 0;

    String sql = SQL_QUERY_JOURNAL_TYPE_AGGREGATES_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the existing aggregates.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the identifier of the source journal.
      statement.setLong(index++, sourcePublicationSeq);

      resultSet = dbManager.executeQuery(statement);

      // Loop through the existing aggregates.
      while (resultSet.next()) {
	publisherInvolved = resultSet.getBoolean(IS_PUBLISHER_INVOLVED_COLUMN);
	log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

	year = resultSet.getInt(REQUEST_YEAR_COLUMN);
	log.debug2(DEBUG_HEADER + "year = " + year);

	month = resultSet.getInt(REQUEST_MONTH_COLUMN);
	log.debug2(DEBUG_HEADER + "month = " + month);

	totalCount = resultSet.getInt(TOTAL_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "totalCount = " + totalCount);

	htmlCount = resultSet.getInt(HTML_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "htmlCount = " + htmlCount);

	pdfCount = resultSet.getInt(PDF_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "pdfCount = " + pdfCount);

	// Retrieve the previous aggregate of requests for the target journal.
	previousAggregate = getExistingAggregateMonthJournalTypes(year,
	    month, targetPublicationSeq, publisherInvolved, conn);

	// Check whether there were previously recorded aggregates.
	if (previousAggregate != null) {
	  // Yes: Get the previously recorded aggregates.
	  previousTotalCount =
	      previousAggregate.get(TOTAL_REQUESTS_COLUMN);
	  log.debug2(DEBUG_HEADER + "previousTotalCount = "
	      + previousTotalCount);

	  previousHtmlCount =
	      previousAggregate.get(HTML_REQUESTS_COLUMN);
	  log.debug2(DEBUG_HEADER + "previousHtmlCount = " + previousHtmlCount);

	  previousPdfCount =
	      previousAggregate.get(PDF_REQUESTS_COLUMN);
	  log.debug2(DEBUG_HEADER + "previousPdfCount = " + previousPdfCount);

	  // Update the existing row in the database.
	  updateJournalTypeAggregate(year, month, targetPublicationSeq,
	                             publisherInvolved,
				     previousTotalCount + totalCount,
				     previousHtmlCount + htmlCount,
				     previousPdfCount + pdfCount, conn);
	} else {
	  // No: Insert a new row in the database.
	  insertJournalTypeAggregate(year, month, targetPublicationSeq,
				     publisherInvolved, totalCount, htmlCount,
				     pdfCount, conn);
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot merge journal type aggregates", sqle);
      log.error("sourcePublicationSeq = " + sourcePublicationSeq);
      log.error("targetPublicationSeq = " + targetPublicationSeq);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot merge journal type aggregates", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Deletes all the type aggregates of a journal.
   * 
   * @param conn
   *          A Connection representing the database connection.
   * @param publicationSeq
   *          A Long with the identifier of the journal involved.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  public void deleteJournalTypeAggregates(Connection conn, Long publicationSeq)
      throws DbException {
    final String DEBUG_HEADER = "deleteJournalTypeAggregates(): ";
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    // Do nothing more if the service is not ready to be used.
    if (!ready) {
      return;
    }

    PreparedStatement deleteAggregate = null;
    String sql = SQL_QUERY_JOURNAL_TYPE_AGGREGATES_DELETE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to delete the rows.
      deleteAggregate = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the journal identifier.
      deleteAggregate.setLong(index++, publicationSeq);

      // Delete the records.
      int count = dbManager.executeUpdate(deleteAggregate);
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot delete the journal type aggregates", sqle);
      log.error("publicationSeq = " + publicationSeq);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot delete the journal type aggregates", sqle);
    } finally {
      DbManager.safeCloseStatement(deleteAggregate);
    }
  }

  /**
   * Merges the journal publication year aggregates of a publication into
   * another publication.
   * 
   * @param conn
   *          A Connection representing the database connection.
   * @param sourcePublicationSeq
   *          A Long with the identifier of the source journal involved.
   * @param targetPublicationSeq
   *          A Long with the identifier of the target journal involved.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  public void mergeJournalPubYearAggregates(Connection conn,
      Long sourcePublicationSeq, Long targetPublicationSeq)
	  throws DbException {
    final String DEBUG_HEADER = "mergeJournalPubYearAggregates(): ";

    // Do nothing more if the service is not ready to be used.
    if (!ready) {
      return;
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    boolean publisherInvolved;
    int year;
    int month;
    int publicationYear;
    int requests;
    Integer previousRequests;

    String sql = SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATES_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the existing aggregates.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the identifier of the source journal.
      statement.setLong(index++, sourcePublicationSeq);

      resultSet = dbManager.executeQuery(statement);

      // Loop through the existing aggregates.
      while (resultSet.next()) {
	publisherInvolved = resultSet.getBoolean(IS_PUBLISHER_INVOLVED_COLUMN);
	log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

	year = resultSet.getInt(REQUEST_YEAR_COLUMN);
	log.debug2(DEBUG_HEADER + "year = " + year);

	month = resultSet.getInt(REQUEST_MONTH_COLUMN);
	log.debug2(DEBUG_HEADER + "month = " + month);

	publicationYear = resultSet.getInt(PUBLICATION_YEAR_COLUMN);
	log.debug2(DEBUG_HEADER + "publicationYear = " + publicationYear);

	requests = resultSet.getInt(REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "requests = " + requests);

	// Retrieve the previous aggregate of requests for the target journal.
	previousRequests =
	    getExistingAggregateMonthJournalPubYear(year, month,
		targetPublicationSeq, publisherInvolved, publicationYear, conn);

	// Check whether there were previously recorded aggregates.
	if (previousRequests != null && previousRequests > 0) {
	  // Yes: Update the existing row in the database.
	  updateTitlePubYearAggregate(year, month, targetPublicationSeq,
	      publisherInvolved, publicationYear,
	      previousRequests.intValue() + requests, conn);
	} else {
	  // No: Insert a new row in the database.
	  insertTitlePubYearAggregate(year, month, targetPublicationSeq,
	      publisherInvolved, publicationYear, requests, conn);
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot merge journal publication year aggregates", sqle);
      log.error("sourcePublicationSeq = " + sourcePublicationSeq);
      log.error("targetPublicationSeq = " + targetPublicationSeq);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot merge journal publication year aggregates",
	  sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Deletes all the publication year aggregates of a journal.
   * 
   * @param conn
   *          A Connection representing the database connection.
   * @param publicationSeq
   *          A Long with the identifier of the journal involved.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  public void deleteJournalPubYearAggregates(Connection conn,
      Long publicationSeq) throws DbException {
    final String DEBUG_HEADER = "deleteJournalPubYearAggregates(): ";
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    // Do nothing more if the service is not ready to be used.
    if (!ready) {
      return;
    }

    PreparedStatement deleteAggregate = null;
    String sql = SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATES_DELETE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to delete the rows.
      deleteAggregate = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the journal identifier.
      deleteAggregate.setLong(index++, publicationSeq);

      // Delete the records.
      int count = dbManager.executeUpdate(deleteAggregate);
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot delete the journal publication year aggregates", sqle);
      log.error("publicationSeq = " + publicationSeq);
      log.error("SQL = '" + sql + "'.");
      throw new DbException(
	  "Cannot delete the journal publication year aggregates", sqle);
    } finally {
      DbManager.safeCloseStatement(deleteAggregate);
    }
  }
}
