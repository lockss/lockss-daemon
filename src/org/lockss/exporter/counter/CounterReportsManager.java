/*
 * $Id: CounterReportsManager.java,v 1.9 2013-03-04 19:26:08 fergaloy-sf Exp $
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
 * Service used to manage COUNTER reports.
 * 
 * @version 1.0
 */
package org.lockss.exporter.counter;

import static org.lockss.db.DbManager.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import org.lockss.app.BaseLockssDaemonManager;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Cron;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.util.FileUtil;
import org.lockss.util.Logger;
import org.lockss.util.TimeBase;

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

    try {
      conn = dbManager.getConnection();
    } catch (SQLException ex) {
      log.error("Cannot connect to database", ex);
      return;
    }

    metadataManager = getDaemon().getMetadataManager();
    String errorMessage = null;
    boolean success = false;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

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
	  .findOrCreatePublication(conn, null, null, "CRBPISBN", "CRBEISBN",
	                           publisherSeq, ALL_BOOKS_NAME, null, null);
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
	  .findOrCreatePublication(conn, "CRJPISSN", "CRJEISSN", null,null,
	                           publisherSeq, ALL_JOURNALS_NAME, null, null);
      log.debug2(DEBUG_HEADER + "allJournalsPublicationSeq = "
	  + allJournalsPublicationSeq);

      if (allJournalsPublicationSeq == null) {
	log.error(errorMessage);
	return;
      }

      success = true;
    } catch (SQLException sqle) {
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
    reportDir =
	new File(config.get(PARAM_REPORT_BASEDIR_PATH,
			    getDefaultTempDbRootDirectory()));
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

    log.debug2(DEBUG_HEADER + "defaultTempDbRootDir = '" + defaultTempDbRootDir
	+ "'.");
    return defaultTempDbRootDir;
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
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  public void persistRequest(String url, boolean isPublisherInvolved)
      throws SQLException {
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
        throw sqle;
      } finally {
        DbManager.safeCloseStatement(insertRequest);
      }

      success = true;
    } finally {
      if (success) {
	conn.commit();
	log.debug2(DEBUG_HEADER + "Successful commit.");
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }
  }
}
