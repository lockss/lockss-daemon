/*
 * $Id: CounterReportsBookReport2.java,v 1.1 2012-08-28 17:36:49 fergaloy-sf Exp $
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
 * The COUNTER Book Report 2.
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

public class CounterReportsBookReport2 extends CounterReportsBookReport {
  private static final Logger log = Logger
      .getLogger("CounterReportsBookReport1");

  // Query to get the books to be included in the report.
  private static final String SQL_QUERY_REPORT_BOOKS_SELECT = "select "
      + "distinct a." + SQL_COLUMN_LOCKSS_ID
      + ", t." + SQL_COLUMN_TITLE_NAME
      + ", t." + SQL_COLUMN_PUBLISHER_NAME
      + ", t." + SQL_COLUMN_PLATFORM_NAME
      + ", t." + SQL_COLUMN_DOI
      + ", t." + SQL_COLUMN_PROPRIETARY_ID
      + ", t." + SQL_COLUMN_ISBN
      + ", t." + SQL_COLUMN_BOOK_ISSN
      + " from " + SQL_TABLE_TYPE_AGGREGATES + " a,"
      + SQL_TABLE_TITLES + " t "
      + "where (t." + SQL_COLUMN_TITLE_NAME + " != '" + ALL_BOOKS_TITLE_NAME
      + "' or t." + SQL_COLUMN_PUBLISHER_NAME + " != '" + ALL_PUBLISHERS_NAME
      + "') "
      + "and t." + SQL_COLUMN_IS_BOOK + " = true "
      + "and a." + SQL_COLUMN_SECTION_BOOK_REQUESTS + " > 0 "
      + "and a." + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = false "
      + "and ((a." + SQL_COLUMN_REQUEST_MONTH + " >= ? "
      + "and a." + SQL_COLUMN_REQUEST_YEAR + " = ?) "
      + "or a." + SQL_COLUMN_REQUEST_YEAR + " > ?) "
      + "and ((a." + SQL_COLUMN_REQUEST_MONTH + " <= ? "
      + "and a." + SQL_COLUMN_REQUEST_YEAR + " = ?) "
      + "or a." + SQL_COLUMN_REQUEST_YEAR + " < ?) "
      + "and a." + SQL_COLUMN_LOCKSS_ID + " = t." + SQL_COLUMN_LOCKSS_ID
      + " order by t." + SQL_COLUMN_TITLE_NAME + " asc";

  // Query to get the book request counts to be included in the report.
  private static final String SQL_QUERY_REPORT_REQUESTS_SELECT = "select "
      + "t." + SQL_COLUMN_TITLE_NAME
      + ", a." + SQL_COLUMN_LOCKSS_ID
      + ", a." + SQL_COLUMN_REQUEST_YEAR
      + ", a." + SQL_COLUMN_REQUEST_MONTH
      + ", a." + SQL_COLUMN_SECTION_BOOK_REQUESTS
      + " from " + SQL_TABLE_TYPE_AGGREGATES + " a,"
      + SQL_TABLE_TITLES + " t "
      + "where (t." + SQL_COLUMN_TITLE_NAME + " != '" + ALL_BOOKS_TITLE_NAME
      + "' or t." + SQL_COLUMN_PUBLISHER_NAME + " != '" + ALL_PUBLISHERS_NAME
      + "') "
      + "and t." + SQL_COLUMN_IS_BOOK + " = true "
      + "and a." + SQL_COLUMN_SECTION_BOOK_REQUESTS + " > 0 "
      + "and a." + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = false "
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
  protected int monthCount = 0;

  /**
   * Constructor for the default report period.
   * 
   * @param daemon
   *          A LockssDaemon with the LOCKSS daemon.
   */
  public CounterReportsBookReport2(LockssDaemon daemon) {
    super(daemon);

    // Count the months included in the report.
    monthCount = getMonthIndex(endMonth, endYear);
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
  public CounterReportsBookReport2(LockssDaemon daemon, int startMonth,
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
   * Initializes the rows to be included in the report with the title data.
   * 
   * @param conn
   *          A Connection with a connection to the database.
   * @return a List<Row> with the initialized rows to be included in the report.
   * @throws SQLException
   */
  protected void initializeReportRows(Connection conn) throws SQLException, Exception {
    final String DEBUG_HEADER = "getReportRows(): ";
    log.debug2(DEBUG_HEADER + "Starting...");
    long lockssId = 0L;

    // The first row is a placeholder for the totals for all books.
    CounterReportsBook book =
	new CounterReportsBook(TOTAL_LABEL, null, null, null, null, null, null);
    List<Row> rows = new ArrayList<Row>();
    rows.add(new Row(lockssId, book));

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String sql = getReportBooksSqlQuery();
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Get the books to be included in the report.
      statement = conn.prepareStatement(sql);

      short index = 1;

      statement.setInt(index++, startMonth);
      statement.setInt(index++, startYear);
      statement.setInt(index++, startYear);
      statement.setInt(index++, endMonth);
      statement.setInt(index++, endYear);
      statement.setInt(index++, endYear);

      resultSet = statement.executeQuery();

      // Loop through all the books to be included in the report.
      while (resultSet.next()) {
	// Get the LOCKSS identifier for the book.
	lockssId = resultSet.getLong(SQL_COLUMN_LOCKSS_ID);
	log.debug2(DEBUG_HEADER + "lockssId = " + lockssId + ".");

	// Get the book properties.
	book =
	    new CounterReportsBook(resultSet.getString(SQL_COLUMN_TITLE_NAME),
		resultSet.getString(SQL_COLUMN_PUBLISHER_NAME),
		resultSet.getString(SQL_COLUMN_PLATFORM_NAME),
		resultSet.getString(SQL_COLUMN_DOI),
		resultSet.getString(SQL_COLUMN_PROPRIETARY_ID),
		resultSet.getString(SQL_COLUMN_ISBN),
		resultSet.getString(SQL_COLUMN_BOOK_ISSN));
	log.debug2(DEBUG_HEADER + "Book = [" + book + "].");

	// Add the row to the results.
	rows.add(new Row(lockssId, book));
      }
    } catch (SQLException sqle) {
      log.error("Cannot retrieve the books to be included in a report", sqle);
      log.error("StartMonth = " + startMonth + ", StartYear = " + startYear
	  + ", EndMonth = " + endMonth + ", EndYear = " + endYear);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    setRows(rows);
    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the SQL query used to find all the books to be included in the
   * report.
   * 
   * @return a String with the SQL query used to find all the books to be
   *         included in the report.
   */
  protected String getReportBooksSqlQuery() {
    return SQL_QUERY_REPORT_BOOKS_SELECT;
  }

  /**
   * Adds the request counts to the rows to be included in the report.
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

    // Verify that the placeholder row for the totals for all books exists.
    Iterator<Row> rowIterator = getRows().iterator();
    if (!rowIterator.hasNext()) {
      throw new CounterReportsException(BaseCounterReport
                                        .ERROR_ALL_BOOKS_MISSING);
    }

    // Get the row for the request totals for all books.
    Row allBooksRow = rowIterator.next();

    // Verify that it is the row for the request totals for all books.
    if (!TOTAL_LABEL.equals(allBooksRow.getTitle().getName())) {
      throw new CounterReportsException(BaseCounterReport
                                        .ERROR_ALL_BOOKS_NOT_FIRST);
    }

    // Initialize its request counts.
    List<ItemCounts> allBooksRowMonthRequestCounts =
	initializeRowRequestCounts(allBooksRow);

    // Do nothing more if there are no requests for this report.
    if (!rowIterator.hasNext()) {
      return;
    }

    String[] itemKeys = getItemColumnKeys();
    String[] totalKeys = getTotalColumnKeys();

    // The current row to be populated with request counts.
    Row currentRow = rowIterator.next();

    // Initialize its request counts.
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
      statement = conn.prepareStatement(sql);

      short index = 1;

      statement.setInt(index++, startMonth);
      statement.setInt(index++, startYear);
      statement.setInt(index++, startYear);
      statement.setInt(index++, endMonth);
      statement.setInt(index++, endYear);
      statement.setInt(index++, endYear);

      resultSet = statement.executeQuery();

      // Loop through all the request counts to be included in the report.
      while (resultSet.next()) {
	// Get the LOCKSS identifier for this set of request counts.
	lockssId = resultSet.getLong(SQL_COLUMN_LOCKSS_ID);
	log.debug2(DEBUG_HEADER + "lockssId = " + lockssId + ".");

	// Check whether this set of request counts is for a row different than
	// the current one.
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
	  currentRowMonthRequestCounts = initializeRowRequestCounts(currentRow);
	}

	// Get the month for this set of request counts.
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

	// Populate the request counts for the book during the appropriate
	// month.
	currentRowMonthRequestCounts.set(getMonthIndex(month, year), counts);

	// Update the request counts for the book during the report period.
	accumulateRequestCounts(totalKeys, counts,
	    currentRowMonthRequestCounts.get(0));

	// Update the request counts for all the books during the appropriate
	// month.
	accumulateRequestCounts(itemKeys, counts,
	    allBooksRowMonthRequestCounts.get(getMonthIndex(month, year)));

	// Update the request counts for all the books during the report period.
	accumulateRequestCounts(totalKeys, counts,
	    allBooksRowMonthRequestCounts.get(0));
      }
    } catch (SQLException sqle) {
      log.error("Cannot retrieve the book requests to be included in a report",
	  sqle);
      log.error("StartMonth = " + startMonth + ", StartYear = " + startYear
	  + ", EndMonth = " + endMonth + ", EndYear = " + endYear);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } catch (CounterReportsException cre) {
      log.error("Error processing book requests", cre);
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
	new String[] { "Book", "Publisher", "Platform", "Book DOI",
	    "Proprietary Identifier", "ISBN", "ISSN", "Reporting Period Total" };

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
    return "COUNTER_Book_2";
  }

  /**
   * Provides the header items in the report.
   * 
   * @return a String[] with the report header items.
   */
  @Override
  protected void populateReportHeaderEntries() {
    header.reportName = "Book Report 2 (R4)";
    header.reportDescription =
	"Number of Successful Section Requests by Month and Title";
    header.sectionTitle = "Section Type:";
    header.periodTitle = "Period covered by Report:";
    header.runDateTitle = "Date run:";
  }

  /**
   * Provides the keys used to populate the total columns.
   * 
   * @return a String[] with the keys used to populate the total columns.
   */
  protected String[] getTotalColumnKeys() {
    return new String[] { SQL_COLUMN_SECTION_BOOK_REQUESTS };
  }

  /**
   * Provides the keys used to populate the item columns.
   * 
   * @return a String[] with the keys used to populate the item columns.
   */
  protected String[] getItemColumnKeys() {
    return new String[] { SQL_COLUMN_SECTION_BOOK_REQUESTS };
  }
}
