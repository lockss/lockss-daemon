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
package org.lockss.exporter;

import static org.lockss.db.SqlConstants.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.daemon.Cron;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.util.FileUtil;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.NumberUtil;
import org.lockss.util.StringUtil;
import org.lockss.util.TimeBase;

/**
 * Periodically exports fetch times of recently added metadata items.
 * 
 * @version 1.0
 */
public class FetchTimeExporter {
  /**
   * Prefix for the export configuration entries.
   */
  public static final String PREFIX = Configuration.PREFIX + "export.";

  /**
   * Frequency of fetch time export operations.
   */
  public static final String PARAM_FETCH_TIME_EXPORT_TASK_FREQUENCY =
      PREFIX + "fetchTimeExportFrequency";

  /**
   * Default value of the frequency of fetch time export operations.
   */
  public static final String DEFAULT_FETCH_TIME_EXPORT_TASK_FREQUENCY =
      "hourly";

  /**
   * Name of this server for the purpose of assigning to it the fetch time
   * export output.
   * <p />
   * Defaults to the networking host name.
   */
  public static final String PARAM_SERVER_NAME =
      PREFIX + "fetchTimeExportServerName";

  /**
   * Name of the directory used to store the fetch time export output files.
   * <p />
   * Defaults to <code>fetchTime</code>.
   */
  public static final String PARAM_FETCH_TIME_EXPORT_OUTPUTDIR = PREFIX
      + "fetchTimeExportDirectoryName";

  /**
   * Default value of the directory used to store the fetch time export output
   * files.
   */
  public static final String DEFAULT_FETCH_TIME_EXPORT_OUTPUTDIR = "fetchTime";

  /**
   * Name of the key used to store in the database the identifier of the last
   * metadata item for which the data has been exported.
   * <p />
   * Defaults to <code>export_fetch_time_md_item_seq</code>.
   */
  public static final String PARAM_FETCH_TIME_EXPORT_LAST_ITEM_LABEL =
      PREFIX + "fetchTimeExportLastMdItemSeqLabel";

  /**
   * Default value of the key used to store in the database the identifier of
   * the last metadata item for which the data has been exported.
   */
  public static final String DEFAULT_FETCH_TIME_EXPORT_LAST_ITEM_LABEL =
      "export_fetch_time_md_item_seq";

  /**
   * The maximum number of metadata items to write to one file.
   * <p />
   * Defaults to <code>100000</code>.
   */
  public static final String PARAM_MAX_NUMBER_OF_EXPORTED_ITEMS_PER_FILE =
      PREFIX + "fetchTimeExportMaxNumberOfExportedItemsPerFile";

  /**
   * Default value of the maximum number of metadata items to write to one file.
   */
  public static final int DEFAULT_MAX_NUMBER_OF_EXPORTED_ITEMS_PER_FILE =
      100000;

  private static final Logger log = Logger.getLogger(FetchTimeExporter.class);

  // Query to get the identifier of the last metadata item for which the data
  // has been exported.
  private static final String GET_LAST_EXPORTED_MD_ITEM_QUERY = "select "
      + LAST_VALUE_COLUMN
      + " from " + LAST_RUN_TABLE
      + " where " + LABEL_COLUMN + " = ?";

  // Query to update the identifier of the last metadata item for which the data
  // has been exported.
  private static final String INSERT_LAST_EXPORTED_MD_ITEM_QUERY = "insert "
      + "into " + LAST_RUN_TABLE
      + " (" + LABEL_COLUMN
      + "," + LAST_VALUE_COLUMN
      + ") values (?,?)";

  // Query to get the data to be exported.
  private static final String GET_EXPORT_FETCH_TIME_QUERY = "select "
      + "pr." + PUBLISHER_NAME_COLUMN
      + ", pl." + PLUGIN_ID_COLUMN
      + ", pl." + IS_BULK_CONTENT_COLUMN
      + ", min1." + NAME_COLUMN + " as PUBLICATION_NAME"
      + ", mit." + TYPE_NAME_COLUMN
      + ", mi2." + MD_ITEM_SEQ_COLUMN
      + ", min2." + NAME_COLUMN + " as ITEM_TITLE"
      + ", mi2." + DATE_COLUMN
      + ", mi2." + FETCH_TIME_COLUMN
      + ", b." + VOLUME_COLUMN
      + ", b." + ISSUE_COLUMN
      + ", b." + START_PAGE_COLUMN
      + ", a." + AU_KEY_COLUMN
      + ", u." + URL_COLUMN
      + ", d." + DOI_COLUMN
      + ", is1." + ISSN_COLUMN + " as " + P_ISSN_TYPE
      + ", is2." + ISSN_COLUMN + " as " + E_ISSN_TYPE
      + ", ib1." + ISBN_COLUMN + " as " + P_ISBN_TYPE
      + ", ib2." + ISBN_COLUMN + " as " + E_ISBN_TYPE
      + ", pv." + PROVIDER_LID_COLUMN
      + ", pv." + PROVIDER_NAME_COLUMN
      + " from " + PUBLISHER_TABLE + " pr"
      + "," + PLUGIN_TABLE + " pl"
      + "," + PUBLICATION_TABLE + " pn"
      + "," + MD_ITEM_NAME_TABLE + " min1"
      + "," + MD_ITEM_TYPE_TABLE + " mit"
      + "," + BIB_ITEM_TABLE + " b"
      + "," + AU_MD_TABLE + " am"
      + "," + AU_TABLE + " a"
      + "," + PROVIDER_TABLE + " pv"
      + "," + MD_ITEM_TABLE + " mi2"
      + " left outer join " + MD_ITEM_NAME_TABLE + " min2"
      + " on mi2." + MD_ITEM_SEQ_COLUMN + " = min2." + MD_ITEM_SEQ_COLUMN
      + " and min2." + NAME_TYPE_COLUMN + " = 'primary'"
      + " left outer join " + DOI_TABLE + " d"
      + " on mi2." + MD_ITEM_SEQ_COLUMN + " = d." + MD_ITEM_SEQ_COLUMN
      + " left outer join " + ISSN_TABLE + " is1"
      + " on mi2." + PARENT_SEQ_COLUMN + " = is1." + MD_ITEM_SEQ_COLUMN
      + " and is1." + ISSN_TYPE_COLUMN + " = '" + P_ISSN_TYPE + "'"
      + " left outer join " + ISSN_TABLE + " is2"
      + " on mi2." + PARENT_SEQ_COLUMN + " = is2." + MD_ITEM_SEQ_COLUMN
      + " and is2." + ISSN_TYPE_COLUMN + " = '" + E_ISSN_TYPE + "'"
      + " left outer join " + ISBN_TABLE + " ib1"
      + " on mi2." + PARENT_SEQ_COLUMN + " = ib1." + MD_ITEM_SEQ_COLUMN
      + " and ib1." + ISBN_TYPE_COLUMN + " = '" + P_ISBN_TYPE + "'"
      + " left outer join " + ISBN_TABLE + " ib2"
      + " on mi2." + PARENT_SEQ_COLUMN + " = ib2." + MD_ITEM_SEQ_COLUMN
      + " and ib2." + ISBN_TYPE_COLUMN + " = '" + E_ISBN_TYPE + "'"
      + " left outer join " + URL_TABLE + " u"
      + " on mi2." + MD_ITEM_SEQ_COLUMN + " = u." + MD_ITEM_SEQ_COLUMN
      + " and u." + FEATURE_COLUMN + " = '"
      + MetadataManager.ACCESS_URL_FEATURE + "'"
      + " where pr." + PUBLISHER_SEQ_COLUMN + " = pn." + PUBLISHER_SEQ_COLUMN
      + " and pn." + MD_ITEM_SEQ_COLUMN + " = min1." + MD_ITEM_SEQ_COLUMN
      + " and min1." + NAME_TYPE_COLUMN + " = 'primary'"
      + " and pn." + MD_ITEM_SEQ_COLUMN + " = mi2." + PARENT_SEQ_COLUMN
      + " and mi2." + MD_ITEM_TYPE_SEQ_COLUMN
      + " = mit." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and mi2." + MD_ITEM_SEQ_COLUMN + " = b." + MD_ITEM_SEQ_COLUMN
      + " and mi2." + AU_MD_SEQ_COLUMN + " = am." + AU_MD_SEQ_COLUMN
      + " and am." + AU_SEQ_COLUMN + " = a." + AU_SEQ_COLUMN
      + " and a." + PLUGIN_SEQ_COLUMN + " = pl." + PLUGIN_SEQ_COLUMN
      + " and am." + PROVIDER_SEQ_COLUMN + " = pv." + PROVIDER_SEQ_COLUMN
      + " and " + "mi2." + MD_ITEM_SEQ_COLUMN + " > ?"
      + " order by mi2." + MD_ITEM_SEQ_COLUMN;

  // Query to update the identifier of the last metadata item for which the data
  // has been exported.
  private static final String UPDATE_LAST_EXPORTED_MD_ITEM_SEQ_QUERY = "update "
      + LAST_RUN_TABLE
      + " set " + LAST_VALUE_COLUMN + " = ?"
      + " where " + LABEL_COLUMN + " = ?";

  // The format of a date as required by the export output file name.
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
      "yyyy-MM-dd-HH-mm-ss");

  private static final String SEPARATOR = "\t";

  private final DbManager dbManager;
  private final FetchTimeExportManager exportManager;

  // The name of the server for the purpose of assigning to it the fetch time
  // export output.
  private String serverName = null;

  // The directory where the fetch time export output files reside.
  private File outputDir = null;

  // The key used to store in the database the identifier of the last metadata
  // item for which the data has been exported.
  private String lastMdItemSeqLabel = null;

  // The maximum number of metadata items to write to one file.
  private int maxNumberOfExportedItemsPerFile =
      DEFAULT_MAX_NUMBER_OF_EXPORTED_ITEMS_PER_FILE;

  // The version of the export file format.
  private int exportVersion = 4;

  /**
   * Constructor.
   * 
   * @param daemon
   *          A LockssDaemon with the application daemon.
   */
  public FetchTimeExporter(LockssDaemon daemon) {
    dbManager = daemon.getDbManager();
    exportManager = daemon.getFetchTimeExportManager();
  }

  /**
   * Provides the Cron task used to schedule the export.
   * 
   * @return a Cron.Task with the task used to schedule the export.
   */
  public Cron.Task getCronTask() {
    return new FetchTimeExporterCronTask();
  }

  /**
   * Performs the periodic task of exporting the fetch time data.
   * 
   * @return <code>true</code> if the task can be considered executed,
   *         <code>false</code> otherwise.
   */
  private boolean export() {
    final String DEBUG_HEADER = "export(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Do nothing more if the configuration failed.
    if (!configure()) {
      return true;
    }

    // Get a connection to the database.
    Connection conn = null;

    try {
      conn = dbManager.getConnection();
    } catch (DbException dbe) {
      log.error("Cannot get a connection to the database", dbe);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return true;
    }

    try {
      // Do nothing more if the required database version has not been
      // completed.
      if (!dbManager.isVersionCompleted(conn, 20)) {
	return true;
      }
    } catch (DbException dbe) {
      log.error("Cannot determine whether a database upgrade is done", dbe);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return true;
    }

    // Determine the report file name.
    String fileName = getReportFileName();
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "fileName = '" + fileName + "'.");

    File exportFile = new File(outputDir, fileName + ".ignore");
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "exportFile = '" + exportFile + "'.");

    // Get the writer for this report.
    PrintWriter writer = null;

    try {
      writer = new PrintWriter(new FileWriter(exportFile));
    } catch (IOException ioe) {
      log.error("Cannot get a PrintWriter for the export output file '"
	  + exportFile + "'", ioe);
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
      return true;
    }

    // An indication of whether any new data has been written out.
    boolean newDataWritten = false;

    try {
      // Get the database version.
      int dbVersion = dbManager.getDatabaseVersion(conn);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "dbVersion = " + dbVersion);

      // Check whether the database version is appropriate.
      if (dbVersion >= 10) {
	// Yes: Perform the export.
	newDataWritten = processExport(conn, exportFile, writer);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "newDataWritten = " + newDataWritten);
      } else {
	log.info("Database version is " + dbVersion
	    + " (< 10). Export skipped.");
      }
    } catch (DbException dbe) {
      log.error("Cannot export fetch times", dbe);
    } finally {
      DbManager.safeRollbackAndClose(conn);
      IOUtil.safeClose(writer);
    }

    // Check whether any new data has been written out.
    if (newDataWritten) {
      // Yes: Rename the output file to mark it as available.
      boolean renamed = exportFile.renameTo(new File(outputDir, fileName));
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "renamed = " + renamed);
    } else {
      // No: Delete the empty file to avoid cluttering.
      boolean deleted = exportFile.delete();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "deleted = " + deleted);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return true;
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

    // Do nothing more if the fetch time export subsystem is disabled.
    if (!exportManager.isReady()) {
      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "Export of fetch times is disabled.");
      return false;
    }

    // Get the name of the server for the purpose of assigning to it the fetch
    // time export output.
    try {
      serverName = config.get(PARAM_SERVER_NAME,
	  InetAddress.getLocalHost().getHostName());
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "serverName = '" + serverName + "'.");
    } catch (UnknownHostException uhe) {
      log.error("Export of fetch times is disabled: No server name.", uhe);
      return false;
    }

    // Get the configured base directory for the export files.
    File exportDir = exportManager.getExportDir();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "exportDir = '"
	+ exportDir.getAbsolutePath() + "'.");

    // Check whether it exists, creating it if necessary.
    if (FileUtil.ensureDirExists(exportDir)) {
      // Specify the configured directory where to put the fetch time export
      // output files.
      outputDir = new File(exportDir,
	  config.get(PARAM_FETCH_TIME_EXPORT_OUTPUTDIR,
	      DEFAULT_FETCH_TIME_EXPORT_OUTPUTDIR));

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "outputDir = '"
	  + outputDir.getAbsolutePath() + "'.");

      // Check whether it exists, creating it if necessary.
      if (!FileUtil.ensureDirExists(outputDir)) {

	log.error("Error creating the fetch time export output directory '"
	    + outputDir.getAbsolutePath() + "'.");
	return false;
      }
    } else {
      log.error("Error creating the export directory '"
	  + exportDir.getAbsolutePath() + "'.");
      return false;
    }

    // Get the label used to key in the database the identifier of the last
    // metadata item for which the fetch time has been exported.
    lastMdItemSeqLabel =
	config.get(PARAM_FETCH_TIME_EXPORT_LAST_ITEM_LABEL,
	    DEFAULT_FETCH_TIME_EXPORT_LAST_ITEM_LABEL);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "lastMdItemSeqLabel = '"
	+ lastMdItemSeqLabel + "'.");

    // Get the maximum number of metadata items to write to one file.
    maxNumberOfExportedItemsPerFile =
	config.getInt(PARAM_MAX_NUMBER_OF_EXPORTED_ITEMS_PER_FILE,
	    DEFAULT_MAX_NUMBER_OF_EXPORTED_ITEMS_PER_FILE);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "maxNumberOfExportedItemsPerFile = "
	  + maxNumberOfExportedItemsPerFile + ".");

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return true;
  }

  /**
   * Provides the name of an export file.
   * 
   * @return a String with the report file name. The format is
   *         'fetch_time-serverName-yyyy-MM-dd-HH-mm-ss.tsv'.
   */
  private String getReportFileName() {
    return String.format("%s-%s-%s.%s", "fetch_time", serverName,
			 dateFormat.format(TimeBase.nowDate()), "tsv");
  }

  /**
   * Exports the fetch time data.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param exportFile
   *          A File where to export the data.
   * @param writer
   *          A PrintWriter used to write the export file.
   * @return <code>true</code> if any new data has been written out,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private boolean processExport(Connection conn, File exportFile,
      PrintWriter writer) throws DbException {
    final String DEBUG_HEADER = "processExport(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // An indication of whether any new data has been written out.
    boolean newDataWritten = false;

    // Get the identifier of the last metadata item for which the fetch time has
    // been exported.
    long lastMdItemSeq = getLastExportedMdItemSeq(conn);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "lastMdItemSeq = " + lastMdItemSeq);

    // Export the data for metadata items newer than the last metadata item for
    // which the fetch time has been exported and get the new value for the
    // identifier of the last metadata item for which the fetch time has been
    // exported.
    long newLastMdItemSeq =
	exportNewData(conn, lastMdItemSeq, exportFile, writer);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "newLastMdItemSeq = " + newLastMdItemSeq);

    // Get the indication of whether any new data has been written out.
    newDataWritten = newLastMdItemSeq > lastMdItemSeq;
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "newDataWritten = " + newDataWritten);

    // Check whether any new data has been written out.
    if (newDataWritten) {
      // Yes: Update in the database the identifier of the last metadata item
      // for which the fetch time has been exported.
      int count = updateLastExportedMdItemSeq(conn, newLastMdItemSeq);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

      DbManager.commitOrRollback(conn, log);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "newDataWritten = " + newDataWritten);
    return newDataWritten;
  }

  /**
   * Provides the identifier of the last metadata item for which the fetch time
   * has been exported.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a long with the metadata item identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private long getLastExportedMdItemSeq(Connection conn) throws DbException {
    final String DEBUG_HEADER = "getLastExportedMdItemSeq(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new DbException("Null connection");
    }

    long lastMdItemSeq = -1;
    String lastMdItemSeqAsString = null;
    PreparedStatement insertStmt = null;
    PreparedStatement selectStmt = null;
    ResultSet resultSet = null;
    String sql = GET_LAST_EXPORTED_MD_ITEM_QUERY;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = " + sql);

    String message = "Cannot get the identifier of the last exported item";

    try {
      // Prepare the statement used to get from the database the identifier of
      // the last metadata item for which the fetch time has been exported.
      selectStmt = dbManager.prepareStatement(conn, sql);
      selectStmt.setString(1, lastMdItemSeqLabel);

      // Try to get the value from the database.
      resultSet = dbManager.executeQuery(selectStmt);

      // Check whether the value was found.
      if (resultSet.next()) {
	// Yes: Convert it from text to a numeric value.
	lastMdItemSeqAsString = resultSet.getString(LAST_VALUE_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "lastMdItemSeqAsString = "
	    + lastMdItemSeqAsString);

	lastMdItemSeq = NumberUtil.parseLong(lastMdItemSeqAsString);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "lastMdItemSeq = " + lastMdItemSeq);
      } else {
	// No: Initialize it in the database because this is the first run.
	sql = INSERT_LAST_EXPORTED_MD_ITEM_QUERY;
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = " + sql);

	message = "Cannot initialize the identifier of the last exported item";

        // Prepare the statement used to initialize the identifier of the last
	// metadata item for which the fetch time has been exported.
	insertStmt = dbManager.prepareStatement(conn, sql);
	insertStmt.setString(1, lastMdItemSeqLabel);
	insertStmt.setString(2, String.valueOf(lastMdItemSeq));

        // Insert the record.
        int count = dbManager.executeUpdate(insertStmt);
        log.debug2(DEBUG_HEADER + "count = " + count);

        DbManager.commitOrRollback(conn, log);
      }
    } catch (NumberFormatException nfe) {
      log.error(message, nfe);
      log.error("SQL = '" + sql + "'.");
      log.error("lastMdItemSeqAsString = '" + lastMdItemSeqAsString + "'.");
    } catch (SQLException sqle) {
      log.error(message, sqle);
      log.error("SQL = '" + sql + "'.");
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      log.error(message, dbe);
      log.error("SQL = '" + sql + "'.");
      throw dbe;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(selectStmt);
      DbManager.safeCloseStatement(insertStmt);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "lastMdItemSeq = " + lastMdItemSeq);
    return lastMdItemSeq;
  }

  /**
   * Exports the data for metadata items newer than the last metadata item for
   * which the fetch time has been exported.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param lastMdItemSeq
   *          A long with the identifier of the last metadata item for which the
   *          fetch time has been exported.
   * @param exportFile
   *          A File where to export the data.
   * @param writer
   *          A PrintWriter used to write the export file.
   * @return a long with the new value for the identifier of the last metadata
   *         item for which the fetch time has been exported.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private long exportNewData(Connection conn, long lastMdItemSeq,
      File exportFile, PrintWriter writer) throws DbException {
    final String DEBUG_HEADER = "exportNewData(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "lastMdItemSeq = " + lastMdItemSeq);
      log.debug2(DEBUG_HEADER + "exportFile = " + exportFile);
    }

    if (conn == null) {
      throw new DbException("Null connection");
    }

    String message = "Cannot get the data to be exported";
    boolean hasError = false;

    String sql = GET_EXPORT_FETCH_TIME_QUERY;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "SQL = '" + sql + "'.");

    PreparedStatement getExportData = null;
    ResultSet results = null;

    try {
      // Prepare the statement used to get the data to be exported.
      getExportData = dbManager.prepareStatement(conn, sql);
      getExportData.setMaxRows(maxNumberOfExportedItemsPerFile);
      getExportData.setLong(1, lastMdItemSeq);

      // Get the data to be exported.
      results = dbManager.executeQuery(getExportData);

      // Loop through all the data to be exported.
      while (results.next()) {
	// Extract the fetch time.
	Long fetchTime = results.getLong(FETCH_TIME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "fetchTime = " + fetchTime);

	// Check whether it has been initialized.
	if (fetchTime >= 0) {
	  // Yes: Extract the other individual pieces of data to be exported.
	  String publisherName = results.getString(PUBLISHER_NAME_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

	  String pluginId = results.getString(PLUGIN_ID_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);

	  boolean isBulkContent = results.getBoolean(IS_BULK_CONTENT_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "isBulkContent = " + isBulkContent);

	  String publicationName = results.getString("PUBLICATION_NAME");
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "publicationName = " + publicationName);

	  String typeName = results.getString(TYPE_NAME_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "typeName = " + typeName);

	  String itemTitle = results.getString("ITEM_TITLE");
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "itemTitle = " + itemTitle);

	  String date = results.getString(DATE_COLUMN);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "date = " + date);

	  String auKey = results.getString(AU_KEY_COLUMN);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

	  String accessUrl = results.getString(URL_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "accessUrl = " + accessUrl);

	  String doi = results.getString(DOI_COLUMN);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "doi = " + doi);

	  String pIssn = results.getString(P_ISSN_TYPE);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pIssn = " + pIssn);

	  String eIssn = results.getString(E_ISSN_TYPE);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "eIssn = " + eIssn);

	  String pIsbn = results.getString(P_ISBN_TYPE);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pIsbn = " + pIsbn);

	  String eIsbn = results.getString(E_ISBN_TYPE);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "eIsbn = " + eIsbn);

	  String volume = results.getString(VOLUME_COLUMN);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "volume = " + volume);

	  String issue = results.getString(ISSUE_COLUMN);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issue = " + issue);

	  String startPage = results.getString(START_PAGE_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "startPage = " + startPage);

	  String providerLid = results.getString(PROVIDER_LID_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "providerLid = " + providerLid);

	  String providerName = results.getString(PROVIDER_NAME_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "providerName = " + providerName);

	  // Create the line to be written to the output file.
	  StringBuilder sb = new StringBuilder();

	  sb.append(exportVersion).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(serverName)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(publisherName))
	  .append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(pluginId)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(auKey)).append(SEPARATOR)
	  .append(isBulkContent).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(publicationName))
	  .append(SEPARATOR)
	  .append(typeName).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(itemTitle)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(date)).append(SEPARATOR)
	  .append(fetchTime).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(accessUrl)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(doi)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(pIssn)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(eIssn)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(pIsbn)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(eIsbn)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(volume)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(issue)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(startPage)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(providerLid)).append(SEPARATOR)
	  .append(StringUtil.blankOutNlsAndTabs(providerName));

	  // Write the line to the export output file.
	  writer.println(sb.toString());

	  // Check whether there were errors writing the line.
	  if (writer.checkError()) {
	    // Yes: Report the error.
	    writer.close();
	    message = "Encountered unrecoverable error writing " +
		"export output file '" + exportFile + "'";
	    log.error(message);
	    hasError = true;
	    break;
	  }

	  writer.flush();

	  // Get the new value of the identifier of the last metadata item for
	  // which the fetch time has been exported.
	  lastMdItemSeq = results.getLong(MD_ITEM_SEQ_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "lastMdItemSeq = " + lastMdItemSeq);
	}
      }
    } catch (SQLException sqle) {
      log.error(message, sqle);
      log.error("SQL = '" + sql + "'.");
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      log.error(message, dbe);
      log.error("SQL = '" + sql + "'.");
      throw dbe;
    } finally {
      DbManager.rollback(conn, log);
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(getExportData);
    }

    if (hasError) {
      throw new DbException(message);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "lastMdItemSeq = " + lastMdItemSeq);
    return lastMdItemSeq;
  }

  /**
   * Updates in the database the value for the identifier of the last metadata
   * item for which the fetch time has been exported.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param lastMdItemSeq
   *          A long with the identifier of the last metadata item for which the
   *          fetch time has been exported.
   * @return an int with the count of updated rows.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private int updateLastExportedMdItemSeq(Connection conn, long lastMdItemSeq)
      throws DbException {
    final String DEBUG_HEADER = "updateLastExportedMdItemSeq(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "lastMdItemSeq = " + lastMdItemSeq);
    }

    String message =
	"Cannot update the last item with exported data identifier";

    String sql = UPDATE_LAST_EXPORTED_MD_ITEM_SEQ_QUERY;
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "SQL = '" + sql + "'.");

    PreparedStatement updateLastId = null;
    int count = -1;

    try {
      // Prepare the statement used to update the identifier of the last
      // metadata item for which the data has been exported.
      updateLastId = dbManager.prepareStatement(conn, sql);
      updateLastId.setString(1, String.valueOf(lastMdItemSeq));
      updateLastId.setString(2, lastMdItemSeqLabel);

      // Update the identifier of the last item with exported data.
      count = dbManager.executeUpdate(updateLastId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(updateLastId);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Provides the instant when this task needs to be executed based on the last
   * execution and the frequency.
   * 
   * @param lastTime
   *          A long with the instant of this task's last execution.
   * @param frequency
   *          A String that represents the frequency.
   * @return a long with the instant when this task needs to be executed.
   */
  private long nextTimeA(long lastTime, String frequency) {
    final String DEBUG_HEADER = "nextTime(): ";

    log.debug2(DEBUG_HEADER + "lastTime = " + lastTime);
    log.debug2(DEBUG_HEADER + "frequency = '" + frequency + "'.");

    if ("hourly".equalsIgnoreCase(frequency)) {
      return Cron.nextHour(lastTime);
    } else if ("daily".equalsIgnoreCase(frequency)) {
      return Cron.nextDay(lastTime);
    } else if ("weekly".equalsIgnoreCase(frequency)) {
      return Cron.nextWeek(lastTime);
    } else {
      return Cron.nextMonth(lastTime);
    }
  }

  /**
   * Implementation of the Cron task interface.
   */
  public class FetchTimeExporterCronTask implements Cron.Task {
    /**
     * Performs the periodic task of exporting the fetch time data.
     * 
     * @return <code>true</code> if the task can be considered executed,
     *         <code>false</code> otherwise.
     */
    @Override
    public boolean execute() {
      return export();
    }

    /**
     * Provides the identifier of the periodic task.
     * 
     * @return a String with the identifier of the periodic task.
     */
    @Override
    public String getId() {
      return "FetchTimeExporter";
    }

    /**
     * Provides the instant when this task needs to be executed based on the
     * last execution.
     * 
     * @param lastTime
     *          A long with the instant of this task's last execution.
     * @return a long with the instant when this task needs to be executed.
     */
    @Override
    public long nextTime(long lastTime) {
      return nextTimeA(lastTime,
	  CurrentConfig.getParam(PARAM_FETCH_TIME_EXPORT_TASK_FREQUENCY,
	      			 DEFAULT_FETCH_TIME_EXPORT_TASK_FREQUENCY));
    }
  }
}
