/*
 * $Id: CounterReportsJournalReport1.java,v 1.1 2012-08-28 17:36:49 fergaloy-sf Exp $
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
 * The COUNTER Journal Report 1.
 * 
 * @version 1.0
 * 
 */
package org.lockss.exporter.counter;

import static org.lockss.exporter.counter.CounterReportsManager.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import org.lockss.app.LockssDaemon;
import org.lockss.db.DbManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class CounterReportsJournalReport1 extends CounterReportsJournalReport {
  private static final Logger log = Logger
      .getLogger("CounterReportsJournalReport1");

  // Query to get the journals to be included in the report.
  //
  // It joins the Titles and Aggregates tables to get the titles that are not
  // books with requests that did not involve the publisher during the period
  // of the report.
  private static final String SQL_QUERY_REPORT_JOURNALS_SELECT = "select "
      + "distinct a." + SQL_COLUMN_LOCKSS_ID
      + ", t." + SQL_COLUMN_TITLE_NAME
      + ", t." + SQL_COLUMN_PUBLISHER_NAME
      + ", t." + SQL_COLUMN_PLATFORM_NAME
      + ", t." + SQL_COLUMN_DOI
      + ", t." + SQL_COLUMN_PROPRIETARY_ID
      + ", t." + SQL_COLUMN_PRINT_ISSN
      + ", t." + SQL_COLUMN_ONLINE_ISSN
      + " from " + SQL_TABLE_TYPE_AGGREGATES + " a,"
      + SQL_TABLE_TITLES + " t "
      + "where "
      + "(t." + SQL_COLUMN_TITLE_NAME + " != '" + ALL_JOURNALS_TITLE_NAME
      + "' or t." + SQL_COLUMN_PUBLISHER_NAME + " != '"
      + ALL_PUBLISHERS_NAME + "') "
      + "and t." + SQL_COLUMN_IS_BOOK + " = false "
      + "and a." + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = false "
      + "and ((a." + SQL_COLUMN_REQUEST_MONTH + " >= ? "
      + "and a." + SQL_COLUMN_REQUEST_YEAR + " = ?) "
      + "or a." + SQL_COLUMN_REQUEST_YEAR + " > ?) "
      + "and ((a." + SQL_COLUMN_REQUEST_MONTH + " <= ? "
      + "and a." + SQL_COLUMN_REQUEST_YEAR + " = ?) "
      + "or a." + SQL_COLUMN_REQUEST_YEAR + " < ?) "
      + "and a." + SQL_COLUMN_LOCKSS_ID + " = t." + SQL_COLUMN_LOCKSS_ID
      + " order by t." + SQL_COLUMN_TITLE_NAME + " asc";

  // Query to get the journal request counts to be included in the report.
  //
  // It uses the same selection criteria and the same sorting as the query
  // above, so as to be able to synchronize the result sets of both queries.
  private static final String SQL_QUERY_REPORT_REQUESTS_SELECT = "select "
      + "t." + SQL_COLUMN_TITLE_NAME
      + ", a." + SQL_COLUMN_LOCKSS_ID
      + ", a." + SQL_COLUMN_REQUEST_YEAR
      + ", a." + SQL_COLUMN_REQUEST_MONTH
      + ", a." + SQL_COLUMN_TOTAL_JOURNAL_REQUESTS
      + ", a." + SQL_COLUMN_HTML_JOURNAL_REQUESTS
      + ", a." + SQL_COLUMN_PDF_JOURNAL_REQUESTS
      + " from " + SQL_TABLE_TYPE_AGGREGATES + " a,"
      + SQL_TABLE_TITLES + " t "
      + "where "
      + "(t." + SQL_COLUMN_TITLE_NAME  + " != '" + ALL_JOURNALS_TITLE_NAME
      + "' or t." + SQL_COLUMN_PUBLISHER_NAME + " != '" + ALL_PUBLISHERS_NAME
      + "') "
      + "and t." + SQL_COLUMN_IS_BOOK + " = false "
      + "and a."  + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = false "
      + "and ((a." + SQL_COLUMN_REQUEST_MONTH + " >= ? "
      + "and a." + SQL_COLUMN_REQUEST_YEAR + " = ?) "
      + "or a." + SQL_COLUMN_REQUEST_YEAR + " > ?) "
      + "and ((a." + SQL_COLUMN_REQUEST_MONTH + " <= ? "
      + "and a." + SQL_COLUMN_REQUEST_YEAR + " = ?) "
      + "or a." + SQL_COLUMN_REQUEST_YEAR + " < ?) "
      + "and a." + SQL_COLUMN_LOCKSS_ID + " = t." + SQL_COLUMN_LOCKSS_ID
      + " order by t." + SQL_COLUMN_TITLE_NAME
      + ", a." + SQL_COLUMN_REQUEST_YEAR
      + ", a." + SQL_COLUMN_REQUEST_MONTH + " asc";

  // The count of months included in the report.
  private int monthCount = 0;

  /**
   * Constructor for the default report period.
   * 
   * @param daemon
   *          A LockssDaemon with the LOCKSS daemon.
   */
  public CounterReportsJournalReport1(LockssDaemon daemon) {
    super(daemon);

    // Count the months included in the report.
    monthCount = getMonthIndex(endMonth, endYear);

    if (monthCount > CounterReportsRequestAggregator.MAX_NUMBER_OF_AGGREGATE_MONTHS) {
      throw new IllegalArgumentException("The report period cannot exceed "
	  + CounterReportsRequestAggregator.MAX_NUMBER_OF_AGGREGATE_MONTHS
	  + " months.");
    }
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
   *          An int with the year of the end of the time period covered by the
   *          report.
   * @param endYear
   *          An int with the year of the end of the time period covered by the
   *          report.
   * @throws IllegalArgumentException
   *           if the period specified is not valid.
   */
  public CounterReportsJournalReport1(LockssDaemon daemon, int startMonth,
      int startYear, int endMonth, int endYear) throws IllegalArgumentException {
    super(daemon, startMonth, startYear, endMonth, endYear);

    // Count the months included in the report.
    monthCount = getMonthIndex(endMonth, endYear);

    if (monthCount > CounterReportsRequestAggregator.MAX_NUMBER_OF_AGGREGATE_MONTHS) {
      throw new IllegalArgumentException("The report period cannot exceed "
	  + CounterReportsRequestAggregator.MAX_NUMBER_OF_AGGREGATE_MONTHS
	  + " months.");
    }
  }

  /**
   * Initializes the data rows to be included in the report with the title data.
   *
   * The data portion of the report contains as many rows as journals have
   * requests during the report period plus one additional row for the
   * aggregation of all  the journals, which is the first row.
   * 
   * This method creates the list of data rows in the same order in which they
   * will be displayed in the report and, for each row, it populates the title
   * data only; the requests counts will be determined and stored later.
   * 
   * @param conn
   *          A Connection with a connection to the database.
   * @return a List<Row> with the initialized rows to be included in the report.
   * @throws SQLException
   */
  protected void initializeReportRows(Connection conn)
      throws SQLException, Exception {
    final String DEBUG_HEADER = "initializeReportRows(): ";
    log.debug2(DEBUG_HEADER + "Starting...");
    long lockssId = 0L;

    // The first row is a placeholder for the totals for all journals.
    CounterReportsJournal journal =
	new CounterReportsJournal(TOTAL_LABEL, null, null, null, null, null,
	                          null);
    List<Row> rows = new ArrayList<Row>();
    rows.add(new Row(lockssId, journal));

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String sql = getReportJournalsSqlQuery();
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Get the journals to be included in the report.
      statement = conn.prepareStatement(sql);

      short index = 1;

      statement.setInt(index++, startMonth);
      statement.setInt(index++, startYear);
      statement.setInt(index++, startYear);
      statement.setInt(index++, endMonth);
      statement.setInt(index++, endYear);
      statement.setInt(index++, endYear);

      resultSet = statement.executeQuery();

      // Loop through all the journals to be included in the report.
      while (resultSet.next()) {
	// Get the LOCKSS identifier for the journal.
	lockssId = resultSet.getLong(SQL_COLUMN_LOCKSS_ID);
	log.debug2(DEBUG_HEADER + "lockssId = " + lockssId + ".");

	// Get the journal properties.
	journal =
	    new CounterReportsJournal(
		resultSet.getString(SQL_COLUMN_TITLE_NAME),
		resultSet.getString(SQL_COLUMN_PUBLISHER_NAME),
		resultSet.getString(SQL_COLUMN_PLATFORM_NAME),
		resultSet.getString(SQL_COLUMN_DOI),
		resultSet.getString(SQL_COLUMN_PROPRIETARY_ID),
		resultSet.getString(SQL_COLUMN_PRINT_ISSN),
		resultSet.getString(SQL_COLUMN_ONLINE_ISSN));
	log.debug2(DEBUG_HEADER + "Journal = [" + journal + "].");

	// Add the row to the results.
	rows.add(new Row(lockssId, journal));
      }
    } catch (SQLException sqle) {
      log.error("Cannot retrieve the journals to be included in a report", sqle);
      log.error("StartMonth = " + startMonth + ", StartYear = " + startYear
	  + ", EndMonth = " + endMonth + ", EndYear = " + endYear);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    // Remember the data rows in the report.
    setRows(rows);
    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the SQL query used to find all the journals to be included in the
   * report.
   * 
   * @return a String with the SQL query used to find all the journals to be
   *         included in the report.
   */
  protected String getReportJournalsSqlQuery() {
    return SQL_QUERY_REPORT_JOURNALS_SELECT;
  }

  /**
   * Adds the request counts to the rows to be included in the report.
   * 
   * This method adds the request counts to the report data rows already
   * populated with the journal data.
   * 
   * The query used returns multiple request count results for each journal,
   * one request count per month in the period.
   * 
   * The request count results are grouped by journal and they are sorted with
   * the journals in the same order as the data rows, so as to be able to
   * iterate synchronously the list of rows and the list of request counts.
   * 
   * Note that if, for any given month in the period, there are no requests,
   * the month requests counts are not returned at all, rather than return zero.
   * 
   * @param conn
   *          A Connection with a connection to the database.
   * @throws SQLException
   * @throws CounterReportsException
   */
  protected void addReportRequestCounts(Connection conn) throws SQLException,
      CounterReportsException {
    final String DEBUG_HEADER = "addReportRequestCounts(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    // Verify that the placeholder row for the totals for all journals exists.
    Iterator<Row> rowIterator = getRows().iterator();
    if (!rowIterator.hasNext()) {
      throw new CounterReportsException(BaseCounterReport
                                        .ERROR_ALL_JOURNALS_MISSING);
    }

    // Get the row for the request totals for all journals.
    Row allJournalsRow = rowIterator.next();

    // Verify that it is the row for the request totals for all journals.
    if (!TOTAL_LABEL.equals(allJournalsRow.getTitle().getName())) {
      throw new CounterReportsException(BaseCounterReport
                                        .ERROR_ALL_JOURNALS_NOT_FIRST);
    }

    // Initialize to zero all its request counts. Only those that are non-zero
    // will be updated later.
    List<ItemCounts> allJournalsRowMonthRequestCounts =
	initializeRowRequestCounts(allJournalsRow);

    // Do nothing more if there are no journals with requests for this report.
    if (!rowIterator.hasNext()) {
      return;
    }

    // Get the keys used to identify the individual request counts for non-total
    // items in the report. Each key corresponds to an individual column in the
    // report.
    String[] itemKeys = getItemColumnKeys();

    // Get the keys used to identify the individual request counts for the total
    // item in the report. Each key corresponds to an individual column in the
    // report.
    String[] totalKeys = getTotalColumnKeys();

    // The current row to be populated with request counts.
    Row currentRow = rowIterator.next();

    // Initialize to zero all its request counts. Only those that are non-zero
    // will be updated later.
    List<ItemCounts> currentRowMonthRequestCounts =
	initializeRowRequestCounts(currentRow);

    int month;
    int year;
    long lockssId;
    ItemCounts counts = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String sql = getReportRequestsSqlQuery();
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Get the request counts for all the rows to be included in the report.
      // Each result will correspond to an item (month) in the report.
      // They will be sorted by journal in exactly the same order as the
      // journals are when using the rowIterator to walk the rows.
      statement = conn.prepareStatement(sql);

      short index = 1;

      statement.setInt(index++, startMonth);
      statement.setInt(index++, startYear);
      statement.setInt(index++, startYear);
      statement.setInt(index++, endMonth);
      statement.setInt(index++, endYear);
      statement.setInt(index++, endYear);

      resultSet = statement.executeQuery();

      // Loop through all the request count items to be included in the report.
      // There will be possibly multiple items for a journal. Once all the
      // items for a journal have been processed, the rowIterator will be
      // advanced.
      while (resultSet.next()) {
	// Get the LOCKSS identifier for this item.
	lockssId = resultSet.getLong(SQL_COLUMN_LOCKSS_ID);
	log.debug2(DEBUG_HEADER + "lockssId = " + lockssId + ".");

	// Check whether this item is for a row different than the current one.
	if (currentRow.getLockssId() != lockssId) {
	  // Yes: This means that all the items for the current row have been
	  // processed. Verify that there are more rows in the report.
	  if (!rowIterator.hasNext()) {
	    throw new CounterReportsException(BaseCounterReport
	                                      .ERROR_UNEXPECTED_IDENTIFIER);
	  }
	  
	  // Make the next row the current one.
	  currentRow = rowIterator.next();

	  // Check whether this row is not in sync with this item.
	  if (currentRow.getLockssId() != lockssId) {
	    throw new CounterReportsException(BaseCounterReport
	                                      .ERROR_WRONG_SORTING);
	  }
	    
	  // Initialize the period request counts of the current row.
	  currentRowMonthRequestCounts =
	      initializeRowRequestCounts(currentRow);
	}

	// Get the month for this item.
	month = resultSet.getShort(SQL_COLUMN_REQUEST_MONTH);
	year = resultSet.getShort(SQL_COLUMN_REQUEST_YEAR);
	log.debug2(DEBUG_HEADER + "Month = " + month + ", Year = " + year);

	// Retrieve and save the request counts for this row during this month.
	counts = new ItemCounts();
	for (int i = 0; i < itemKeys.length; i++) {
	  counts.put(itemKeys[i], resultSet.getInt(itemKeys[i]));
	}
	for (int i = 0; i < totalKeys.length; i++) {
	  counts.put(totalKeys[i], resultSet.getInt(totalKeys[i]));
	}

	// Populate the request counts for the journal during the appropriate
	// month by overwriting the initial zero request counts.
	currentRowMonthRequestCounts.set(getMonthIndex(month, year), counts);

	// Update the request counts for the journal during the report period
	// by accumulating the request counts for this item.
	accumulateRequestCounts(totalKeys, counts,
	    currentRowMonthRequestCounts.get(0));

	// Update the request counts for all the journals during the appropriate
	// month by accumulating the request counts for this item.
	accumulateRequestCounts(itemKeys, counts,
	    allJournalsRowMonthRequestCounts.get(getMonthIndex(month, year)));

	// Update the request counts for all the books during the report period
	// by accumulating the request counts for this item.
	accumulateRequestCounts(totalKeys, counts,
	    allJournalsRowMonthRequestCounts.get(0));
      }
    } catch (SQLException sqle) {
      log.error(
	  "Cannot retrieve the journal requests to be included in a report",
	  sqle);
      log.error("StartMonth = " + startMonth + ", StartYear = " + startYear
	  + ", EndMonth = " + endMonth + ", EndYear = " + endYear);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } catch (CounterReportsException cre) {
      log.error("Error processing journal requests", cre);
      log.error("StartMonth = " + startMonth + ", StartYear = " + startYear
	  + ", EndMonth = " + endMonth + ", EndYear = " + endYear);
      log.error("SQL = '" + sql + "'.");
      throw cre;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the count of items in a row, excluding any total item.
   * 
   * @return an int with the count of items in a row, excluding any total item.
   */
  protected int getNonTotalItemCount() {
    return monthCount;
  }

  /**
   * Provides an indication of whether the report includes a total column.
   * 
   * @return a boolean with the indication of whether the report includes a
   *         total column.
   */
  protected boolean hasTotalColumn() {
    return true;
  }

  /**
   * Provides the SQL query used to find all the requests to be included in the
   * report.
   * 
   * @return a String with the SQL query used to find all the requests to be
   *         included in the report.
   */
  protected String getReportRequestsSqlQuery() {
    return SQL_QUERY_REPORT_REQUESTS_SELECT;
  }

  /**
   * Provides the text line used as the table header.
   * 
   * @param separator
   *          A String with the separator to be used between items in report
   *          lines.
   * @return a String with the text line used as the table header.
   */
  protected String getTableHeaderTextLine(String separator) {
    // Place the title data headers.
    String[] tableHeader =
	new String[] { "Journal", "Publisher", "Platform", "Journal DOI",
	    "Proprietary Identifier", "Print ISSN", "Online ISSN",
	    "Reporting Period Total", "Reporting Period HTML",
	    "Reporting Period PDF" };

    StringBuilder sb =
	new StringBuilder(StringUtil.separatedString(tableHeader, separator));

    // Place the request month headers.
    Calendar calendar = Calendar.getInstance();
    int month = startMonth;
    int year = startYear;

    for (int i = 0; i < monthCount; i++) {
      calendar.set(year, month - 1, 1);

      sb.append(separator).append(monthFormat.format(calendar.getTime()));
      month++;

      if (month > 12) {
	month = 1;
	year++;
      }
    }

    return sb.toString();
  }

  /**
   * Provides the name of the report to be used in the report file name.
   * 
   * @return a String with the name of the report to be used in the report file
   *         name.
   */
  protected String getFileReportName() {
    return "COUNTER_Journal_1";
  }

  /**
   * Provides the header items in the report.
   * 
   * @return a String[] with the report header items.
   */
  @Override
  protected void populateReportHeaderEntries() {
    header.reportName = "Journal Report 1 (R4)";
    header.reportDescription =
	"Number of Successful Full-Text Article Requests by Month and Journal";
    header.periodTitle = "Period covered by Report:";
    header.runDateTitle = "Date run:";
  }

  /**
   * Provides the keys used to populate the total columns.
   * 
   * @return a String[] with the keys used to populate the total columns.
   */
  protected String[] getTotalColumnKeys() {
    return new String[] { SQL_COLUMN_TOTAL_JOURNAL_REQUESTS,
	SQL_COLUMN_HTML_JOURNAL_REQUESTS, SQL_COLUMN_PDF_JOURNAL_REQUESTS };
  }

  /**
   * Provides the keys used to populate the item columns.
   * 
   * @return a String[] with the keys used to populate the item columns.
   */
  protected String[] getItemColumnKeys() {
    return new String[] { SQL_COLUMN_TOTAL_JOURNAL_REQUESTS };
  }
}
