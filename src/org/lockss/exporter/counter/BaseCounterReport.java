/*
 * $Id$
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
 * A generic COUNTER report.
 * 
 * @version 1.0
 * 
 */
package org.lockss.exporter.counter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lockss.app.LockssDaemon;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.TimeBase;

public abstract class BaseCounterReport implements CounterReport {
  private static final Logger log = Logger.getLogger(BaseCounterReport.class);

  // The reports separators.
  protected static final String COMMA = ",";
  protected static final String TAB = "\t";

  protected static final String ERROR_ALL_BOOKS_MISSING =
      "Inconsistent data set - Missing ALL BOOKS data";
      
  protected static final String ERROR_ALL_BOOKS_NOT_FIRST =
      "Inconsistent data set - First result is not ALL BOOKS data";

  protected static final String ERROR_ALL_JOURNALS_MISSING =
      "Inconsistent data set - Missing ALL JOURNALS data";
      
  protected static final String ERROR_ALL_JOURNALS_NOT_FIRST =
      "Inconsistent data set - First result is not ALL JOURNALS data";

  protected static final String ERROR_UNEXPECTED_IDENTIFIER =
      "Inconsistent data set - Unexpected additional identifier";

  protected static final String ERROR_WRONG_SORTING =
      "Inconsistent data set - Wrong identifier sorting";

  // The format of a date as required by the report.
  protected static final SimpleDateFormat dateFormat = new SimpleDateFormat(
      "yyyy-MM-dd");

  // The format of a month as required by the report.
  protected static final SimpleDateFormat monthFormat = new SimpleDateFormat(
      "MMM-yyyy");

  // The month of the beginning of the time period covered by the report.
  protected int startMonth;

  // The year of the beginning of the time period covered by the report.
  protected int startYear;

  // The month of the end of the time period covered by the report.
  protected int endMonth;

  // The year of the end of the time period covered by the report.
  protected int endYear;

  protected final LockssDaemon daemon;

  // The report header.
  protected Header header = new Header();

  // An indication of whether this report is ready to be output.
  protected boolean ready = false;

  // The rows included in the report.
  private List<Row> rows = null;

  /**
   * Constructor for the default report period.
   * 
   * @param daemon
   *          A LockssDaemon with the LOCKSS daemon.
   */
  protected BaseCounterReport(LockssDaemon daemon) {
    this.daemon = daemon;

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(TimeBase.nowDate());

    // The end of the report period.
    calendar.add(Calendar.MONTH, -1);
    endYear = calendar.get(Calendar.YEAR);
    endMonth = calendar.get(Calendar.MONTH) + 1;

    // The beginning of the report period.
    calendar.add(Calendar.MONTH, -23);
    startYear = calendar.get(Calendar.YEAR);
    startMonth = calendar.get(Calendar.MONTH) + 1;
  }

  /**
   * Constructor for a custom report period.
   * 
   * @param daemon
   *          A LockssDaemon with the LOCKSS daemon.
   * @param startMonth
   *          An int with the month of the beginning of the time period covered
   *          by the report.
   * @param startYear
   *          An int with the year of the beginning of the time period covered
   *          by the report.
   * @param endMonth
   *          An int with the month of the end of the time period covered by the
   *          report.
   * @param endYear
   *          An int with the year of the end of the time period covered by the
   *          report.
   * @throws IllegalArgumentException
   *           if the period specified is not valid.
   */
  protected BaseCounterReport(LockssDaemon daemon, int startMonth,
      int startYear, int endMonth, int endYear) throws IllegalArgumentException {

    // Validate the beginning of the report period.
    if (startMonth < 1 || startMonth > 12) {
      throw new IllegalArgumentException(
	  "Start month must be a number between 1 and 12, both inclusive.");
    }

    // Validate the end of the report period.
    if (endMonth < 1 || endMonth > 12) {
      throw new IllegalArgumentException(
	  "End month must be a number between 1 and 12, both inclusive.");
    }

    if (endYear < startYear || (endYear == startYear && endMonth < startMonth)) {
      throw new IllegalArgumentException(
	  "The period end must not be earlier than the period start.");
    }

    this.daemon = daemon;
    this.startMonth = startMonth;
    this.startYear = startYear;
    this.endMonth = endMonth;
    this.endYear = endYear;
  }

  /**
   * Saves the report in a CSV-formatted file.
   * 
   * @return a File with the report output file.
   * @throws SQLException
   * @throws IOException
   * @throws Exception
   */
  public File saveCsvReport() throws SQLException, IOException, Exception {
    return saveReport(COMMA, CounterReportsManager.CSV_EXTENSION);
  }

  /**
   * Saves the report in a TSV-formatted file.
   * 
   * @return a File with the report output file.
   * @throws SQLException
   * @throws IOException
   * @throws Exception
   */
  public File saveTsvReport() throws SQLException, IOException, Exception {
    return saveReport(TAB, CounterReportsManager.TSV_EXTENSION);
  }

  /**
   * Saves the report in a *SV-formatted file.
   * 
   * @param separator
   *          A String with the separator to be used between items in report
   *          lines.
   * @param extension
   *          A String with the file extension to be used.
   * @return a File with the report output file.
   * @throws DbException
   * @throws IOException
   * @throws CounterReportsException
   */
  private File saveReport(String separator, String extension)
      throws DbException, IOException, CounterReportsException {
    // Determine the report file name.
    CounterReportsManager reportManager = daemon.getCounterReportsManager();
    String fileName = getReportFileName(extension);
    File reportFile = new File(reportManager.getOutputDir(), fileName);

    // Get the writer for this report.
    PrintWriter writer = reportManager.getReportOutputWriter(fileName);

    // Check whether one was obtained.
    if (writer != null) {
      // Yes: Get the report data, if not available already.
      if (!ready) {
	try {
	  compileReportData();
	} catch (DbException dbe) {
	  writer.close();

	  // Delete the partially-written file.
	  if (reportManager.deleteReportOutputFile(fileName)) {
	    log.error("Failed to delete invalid " + extension.toUpperCase()
		+ " report file.");
	  }

	  // Notify the caller.
	  throw dbe;
	} catch (CounterReportsException cre) {
	  writer.close();

	  // Delete the partially-written file.
	  if (reportManager.deleteReportOutputFile(fileName)) {
	    log.error("Failed to delete invalid " + extension.toUpperCase()
		+ " report file.");
	  }

	  // Notify the caller.
	  throw cre;
	} catch (Exception e) {
	  writer.close();

	  // Delete the partially-written file.
	  if (reportManager.deleteReportOutputFile(fileName)) {
	    log.error("Failed to delete invalid " + extension.toUpperCase()
		+ " report file.");
	  }

	  // Notify the caller.
	  throw new CounterReportsException(e);
	}
      }

      // Loop through all the lines in the report.
      for (String line : getReportTextLines(separator)) {
	// Write this report line to the file.
	writer.println(line);

	// Check whether there were errors writing the line.
	if (writer.checkError()) {
	  // Yes: Report the error.
	  String message =
	      "Encountered unrecoverable error saving "
		  + extension.toUpperCase() + " report file.";
	  log.error(message);
	  writer.close();

	  // Delete the partially-written file.
	  if (reportManager.deleteReportOutputFile(fileName)) {
	    log.error("Failed to delete invalid " + extension.toUpperCase()
		+ " report file.");
	  }

	  // Notify the caller.
	  throw new IOException(message);
	}
      }

      IOUtil.safeClose(writer);
    }
    
    return reportFile;
  }

  /**
   * Compiles the data needed by the report.
   * 
   * @throws DBException
   * @throws CounterReportsException
   */
  private void compileReportData() throws DbException, CounterReportsException {
    final String DEBUG_HEADER = "compileReportData(): ";

    DbManager dbManager = daemon.getDbManager();
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Initialize the report data rows with the title data.
      initializeReportRows(conn);
      log.debug2(DEBUG_HEADER + "rows.size() = " + rows.size() + ".");

      // Add the request counts to be included in the report.
      addReportRequestCounts(conn);

      ready = true;
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Provides the name of a report file.
   * 
   * @param extension
   *          A String with the file extension to be used.
   * @return a String with the report file name. The format is
   *         'Report_Name-YYYYStart_MMStart-YYYYEnd_MMEnd-YYYYNow-MMNow-DDNow'.
   */
  public String getReportFileName(String extension) {
    return String.format("%s-%4d_%02d-%4d_%02d-%s.%s", getFileReportName(),
			 startYear, startMonth, endYear, endMonth,
			 dateFormat.format(TimeBase.nowDate()), extension);
  }

  /**
   * Provides the text lines that form the report.
   * 
   * @param separator
   *          A String with the separator to be used between items in report
   *          lines.
   * @return a List<String> with the text lines that form the report.
   */
  private List<String> getReportTextLines(String separator) {

    // Get the lines of the report header.
    List<String> reportLines = getReportHeaderTextLines(separator);

    // Add the table header line.
    reportLines.add(getTableHeaderTextLine(separator));

    // Add the table data lines.
    reportLines.addAll(getTableDataTextLines(separator));

    return reportLines;
  }

  /**
   * Initializes the rows to be included in the report with the title data.
   * 
   * @param conn
   *          A Connection with a connection to the database.
   * @throws SQLException
   */
  protected abstract void initializeReportRows(Connection conn)
      throws DbException;

  /**
   * Adds the request counts to the rows to be included in the report.
   * 
   * @param conn
   *          A Connection with a connection to the database.
   * @throws SQLException
   * @throws CounterReportsException
   */
  protected abstract void addReportRequestCounts(Connection conn)
      throws DbException, CounterReportsException;

  /**
   * Provides the name of the report to be used in the report file name.
   * 
   * @return a String with the name of the report to be used in the report file
   *         name.
   */
  protected abstract String getFileReportName();

  /**
   * Provides the index of a month within the months covered by the report.
   * 
   * @param month
   *          An int with month to be indexed.
   * @param year
   *          An int with the year of the month to be indexed.
   * @return an int with the index of the month within the months covered by the
   *         report.
   */
  protected int getMonthIndex(int month, int year) {
    // The first index (0) is reserved for the total of all months.
    return 12 * (year - startYear) + month - startMonth + 1;
  }

  /**
   * Provides the text lines that comprise the report header.
   * 
   * @param separator
   *          A String with the separator to be used between items in report
   *          lines.
   * @return a List<String> with the text lines that comprise the report header.
   */
  protected List<String> getReportHeaderTextLines(String separator) {

    // Besides the table data rows, there are 7 report header lines and 1 table
    // header line.
    List<String> reportLines = new ArrayList<String>(rows.size() + 8);

    // Populate the report header entries.
    populateReportHeaderEntries();
    
    // The report title line.
    reportLines.add(new StringBuilder(header.reportName).append(separator)
	.append(header.reportDescription).toString());

    // The customer line.
    reportLines.add(new StringBuilder(header.customerName).append(separator)
	.append(header.sectionTitle).toString());

    // The institutional identifier line.
    reportLines.add(new StringBuilder(header.institutionalIdentifier)
    	.append(separator).append(header.sectionType).toString());

    // The report period lines.
    reportLines.add(header.periodTitle);
    reportLines.add(new StringBuilder(getDisplayPeriodFirstDate())
	.append(" to ").append(getDisplayPeriodLastDate()).toString());

    // The run date lines.
    reportLines.add(header.runDateTitle);
    reportLines.add(dateFormat.format(TimeBase.nowDate()));

    return reportLines;
  }

  /**
   * Provides the text line used as the table header.
   * 
   * @param separator
   *          A String with the separator to be used between items in report
   *          lines.
   * @return a String with the text line used as the table header.
   */
  protected abstract String getTableHeaderTextLine(String separator);

  /**
   * Provides the text lines that comprise the report data.
   * 
   * @param separator
   *          A String with the separator to be used between items in report
   *          lines.
   * @return a List<String> with the text lines that comprise the report data.
   */
  protected abstract List<String> getTableDataTextLines(String separator);

  /**
   * Provides the count of items in a row, excluding any total item.
   * 
   * @return an int with the count of items in a row, excluding any total item.
   */
  protected abstract int getNonTotalItemCount();

  /**
   * Populates the header items in the report.
   */
  protected abstract void populateReportHeaderEntries();

  /**
   * Converts a NULL String to an empty String.
   * 
   * @param text
   *          A String with the text to be converted, if necessary.
   * @return an empty String if the original text is NULL; otherwise, the
   *         original text.
   */
  protected String blankNull(String text) {
    return text == null ? "" : text;
  }

  /**
   * Provides an indication of whether the report includes a total column.
   * 
   * @return a boolean with the indication of whether the report includes a
   *         total column.
   */
  protected abstract boolean hasTotalColumn();

  /**
   * Provides the keys used to populate the total columns.
   * 
   * @return a String[] with the keys used to populate the total columns.
   */
  protected abstract String[] getTotalColumnKeys();

  /**
   * Provides the keys used to populate the item columns.
   * 
   * @return a String[] with the keys used to populate the item columns.
   */
  protected abstract String[] getItemColumnKeys();

  /**
   * Writes the report to the log. For debugging purposes only.
   * 
   * @throws SQLException
   * @throws Exception
   */
  public void logReport() throws SQLException, Exception {
    // Do nothing more if the current log level is not appropriate.
    if (!log.isDebug()) {
      return;
    }

    // Get the report data, if not available already.
    if (!ready) {
      compileReportData();
    }

    // Loop through all the lines in the report.
    log.debug("REPORT START");
    for (String line : getReportTextLines(COMMA)) {
      // Write this report line to the log.
      log.debug(line);
    }
    log.debug("REPORT END");
  }

  /**
   * Initializes the request counts of a row.
   * 
   * For each row in the report, the requests counts are displayed for a set of
   * items. Each report has its own set of items; for some reports, an item is a
   * month in the report period; for others, it is a publication year.
   * <br />
   * Also, some  reports include as the first item a total of request counts.
   * <br />
   * Each item is represented by an ItemCounts object and it will correspond to
   * at least one column in the report, possibly more. The request count for
   * each column in the report is linked to the appropriate key in the
   * ItemCounts object.
   * <br />
   * The reason why the request counts need to be initialized to zero is that if
   * for any given item there are no requests, the requests counts are not
   * stored in the database at all, rather than being stored as zero.
   * @param row
   *          A Row with the request counts to be initialized.
   * @return List<ItemCounts> with the initialized request counts of the row.
   */
  protected List<ItemCounts> initializeRowRequestCounts(Row row) {

    // The request counts for an item.
    ItemCounts itemCounts = null;

    // Get the request counts for the row.
    List<ItemCounts> requestCounts = row.getRequestCounts();

    // Check whether this report includes total columns.
    if (hasTotalColumn()) {
      // Yes: Get the keys used to populate the total columns.
      String[] totalKeys = getTotalColumnKeys();

      // Initialize this row total counts.
      itemCounts = new ItemCounts();
      for (int j = 0; j < totalKeys.length; j++) {
	itemCounts.put(totalKeys[j], 0);
      }

      // Add them to the request counts for the row.
      requestCounts.add(itemCounts);
    }

    // Get the keys used to populate the non-total item columns.
    String[] itemKeys = getItemColumnKeys();

    // Loop through all the items.
    int nonTotalItemCount = getNonTotalItemCount();
    for (int i = 0; i < nonTotalItemCount; i++) {
      // Initialize this row item counts.
      itemCounts = new ItemCounts();
      for (int j = 0; j < itemKeys.length; j++) {
	itemCounts.put(itemKeys[j], 0);
      }

      // Add them to the request counts for the row.
      requestCounts.add(itemCounts);
    }

    return requestCounts;
  }

  /**
   * Accumulates the different types of request counts for a row item.
   * 
   * @param keys
   *          A String[] with the keys used to identify the different types of
   *          request counts to be accumulated.
   * @param counts
   *          An ItemCounts with the counts to be accumulated.
   * @param accumulator
   *          An ItemCounts where the counts are to be accumulated.
   */
  protected void accumulateRequestCounts(String[] keys,
      ItemCounts counts, ItemCounts accumulator) {
    // Loop through all the keys of the request counts that need to be
    // accumulated.
    for (int i = 0; i < keys.length; i++) {
      // Accumulate the request count for this key.
      accumulator.put(keys[i], accumulator.get(keys[i]) + counts.get(keys[i]));
    }
  }

  /**
   * Provides the first date of the period in a format suitable for display in
   * the report.
   * 
   * @return a String with the first date of the period in a format suitable for
   *         display in the report.
   */
  private String getDisplayPeriodFirstDate() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(startYear, startMonth - 1, 1);

    return dateFormat.format(calendar.getTime());
  }

  /**
   * Provides the last date of the period in a format suitable for display in
   * the report.
   * 
   * @return a String with the last date of the period in a format suitable for
   *         display in the report.
   */
  private String getDisplayPeriodLastDate() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(endYear, endMonth - 1, 1);
    int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    calendar.set(Calendar.DAY_OF_MONTH, maxDay);

    return dateFormat.format(calendar.getTime());
  }

  /**
   * Provides the rows included in the report.
   * 
   * @return a List<Row> with the rows included in the report.
   */
  protected List<Row> getRows() {
    return rows;
  }

  /**
   * Saves the rows included in the report.
   * 
   * @param rows A List<Row> with the rows to be saved.
   */
  protected void setRows(List<Row> rows) {
    this.rows = rows;
  }

  /**
   * Formats an ISBN for display.
   * 
   * @param unformatted
   *          A String with the ISBN without punctuation.
   * @return a String with the formatted ISBN.
   */
  protected String formatIsbn(String unformatted) {
    String formatted = unformatted;
    
    if (unformatted != null && unformatted.length() > 9) {
      formatted =
	  unformatted.substring(0, 3) + "-" + unformatted.substring(3, 9) + "-"
	      + unformatted.substring(9);
    }

    return formatted;
  }

  /**
   * Formats an ISSN for display.
   * 
   * @param unformatted
   *          A String with the ISSN without punctuation.
   * @return a String with the formatted ISSN.
   */
  protected String formatIssn(String unformatted) {
    String formatted = unformatted;
    
    if (unformatted != null && unformatted.length() > 4) {
      formatted = unformatted.substring(0,4) + "-" + unformatted.substring(4);
    }

    return formatted;
  }

  /**
   * Representation of the report header.
   * 
   * A report has a 7-line header. Each line has one or two columns.
   * In the report specification document, the rows are numbered from 1 to 7
   * and the columns are labeled A and B.
   */
  protected class Header {
    // The name of the report (cell A1).
    protected String reportName = "";
    
    // The description of the report (cell B1).
    protected String reportDescription = "";

    // The name of the customer (cell A2).
    protected String customerName = "";

    // The section type title (cell B2).
    protected String sectionTitle = "";

    // The institutional identifier (cell A3).
    protected String institutionalIdentifier = "";

    // The section type (cell B3).
    protected String sectionType = "";

    // The period title identifier (cell A4).
    protected String periodTitle = "";

    // The run date title (cell A6).
    protected String runDateTitle = "";
  }

  /**
   * Representation of a report data row.
   * 
   * A report is considered a list of rows. The list is divided in two parts:
   * <br />
   * A header, comprised of a fixed number of rows, and<br />
   * The request data, comprised of a variable number of rows.
   * <br />
   * This class represents a row in the request data set, not the header.
   * <br />
   * Each row displays the request counts for one title during the period of
   * the report. The first row is special because it displays accumulated totals
   * for all the titles in the report.
   * <br />
   * If a title does not have any requests during the period of the report, it
   * does not appear in the report.
   * <br />
   * Therefore, if there are n titles with requests during the period of the
   * report, there will be n+1 rows in the request data set of the report.
   */
  protected class Row {
    // The identifier of the row title.
    private Long titleId;

    // The title properties.
    private CounterReportsTitle title;

    // The title request counts.
    private List<ItemCounts> requestCounts =
	new ArrayList<ItemCounts>();

    /**
     * Constructor.
     * 
     * @param titleId
     *          A Long with the identifier of the row title.
     * @param title
     *          A CounterReportsTitle with the title properties.
     */
    public Row(Long titleId, CounterReportsTitle title) {
      this.titleId = titleId;
      this.title = title;
    }

    /**
     * Provides the identifier of the row title.
     * 
     * @return a Long with the identifier of the row title.
     */
    public long getTitleId() {
      return titleId;
    }

    /**
     * Provides the title properties.
     * 
     * @return a CounterReportsTitle with the title properties.
     */
    public CounterReportsTitle getTitle() {
      return title;
    }

    /**
     * Provides the request counts associated to a row.
     * 
     * @return a List<ItemCounts> with the request counts for the row title.
     */
    public List<ItemCounts> getRequestCounts() {
      return requestCounts;
    }
  }

  /**
   * Representation of the request counts for an item in a report row.
   * 
   * The request counts for a title are calculated for a set of items, which
   * vary from report to  report.
   * <br />
   * For example, for a report that lists the requests for various months, each
   * month is an item. In this case, the number of items will depend on the
   * period covered by the report.
   * <br />
   * Also, if the report must include totals across the items in the report, one
   * additional item will be included in the report, before all the others.
   * <br />
   * An item may include one request count or several, resulting in one actual
   * column in the report, or several. All the items must have the same number
   * of request counts, except for the item with accumulated totals, which may
   * be different.
   */
  protected class ItemCounts {
    // The backing map. The key identifies the kind of request count, while the
    // value contains the actual request count.
    Map<String, Integer> theCounts;
    
    /**
     * Constructor.
     */
    public ItemCounts() {
      theCounts = new HashMap<String, Integer>();
    }
    
    /**
     * Provides the request count of a given kind.
     * @param key A String identifying the kind of request count to be provided.
     * @return an Integer with the request count.
     */
    public Integer get(String key) {
      return theCounts.get(key);
    }
    
    /**
     * Saves the request count of a given kind.
     * @param key A String identifying the kind of request count to be saved.
     * @param value An Integer with the request count to be saved.
     */
    public void put(String key, Integer value) {
      theCounts.put(key, value);
    }
  }
}
