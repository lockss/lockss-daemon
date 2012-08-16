/*
 * $Id: CounterReportsRequestAggregator.java,v 1.3 2012-08-16 22:34:52 fergaloy-sf Exp $
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
 * Periodically aggregates request statistics used in COUNTER reports and stores
 * them in the database.
 * 
 * @version 1.0
 */
package org.lockss.exporter.counter;

import static org.lockss.exporter.counter.CounterReportsManager.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.daemon.Cron;
import org.lockss.db.DbManager;
import org.lockss.util.Logger;

public class CounterReportsRequestAggregator {
  // Prefix for the reporting configuration entries.
  public static final String PREFIX = Configuration.PREFIX + "report.";

  /*
   * Frequency of database persistence of COUNTER report request aggregations.
   */
  public static final String PARAM_COUNTER_REQUEST_AGGREGATION_TASK_FREQUENCY =
      PREFIX + "aggregationFrequency";

  public static final String DEFAULT_COUNTER_REQUEST_AGGREGATION_TASK_FREQUENCY =
      "hourly";

  // The maximum number of aggregate months required for COUNTER reports.
  public static final int MAX_NUMBER_OF_AGGREGATE_MONTHS = 24;

  // TODO: DERBYLOCK - This is needed to lock the tables for multiple shorter
  // periods of time instead of a single longer period. Once this problem is
  // solved, the parameter can be increased or the code that depends on it can
  // be refactored out.
  //
  // The maximum number of titles to aggregate in one transaction.
  public static final int MAX_NUMBER_OF_TRANSACTION_AGGREGATED_TITLES = 100;

  private static final Logger log = Logger
      .getLogger("CounterReportsRequestAggregator");

  // Query to insert book type aggregates.
  private static final String SQL_QUERY_BOOK_TYPE_AGGREGATE_INSERT =
      "insert into " + SQL_TABLE_TYPE_AGGREGATES + " (" + SQL_COLUMN_LOCKSS_ID
	  + "," + SQL_COLUMN_IS_PUBLISHER_INVOLVED + ","
	  + SQL_COLUMN_REQUEST_YEAR + "," + SQL_COLUMN_REQUEST_MONTH + ","
	  + SQL_COLUMN_FULL_BOOK_REQUESTS + ","
	  + SQL_COLUMN_SECTION_BOOK_REQUESTS + ") values (?,?,?,?,?,?)";

  // Query to insert journal type aggregates.
  private static final String SQL_QUERY_JOURNAL_TYPE_AGGREGATE_INSERT =
      "insert into " + SQL_TABLE_TYPE_AGGREGATES + " (" + SQL_COLUMN_LOCKSS_ID
	  + "," + SQL_COLUMN_IS_PUBLISHER_INVOLVED + ","
	  + SQL_COLUMN_REQUEST_YEAR + "," + SQL_COLUMN_REQUEST_MONTH + ","
	  + SQL_COLUMN_TOTAL_JOURNAL_REQUESTS + ","
	  + SQL_COLUMN_HTML_JOURNAL_REQUESTS + ","
	  + SQL_COLUMN_PDF_JOURNAL_REQUESTS + ") values (?,?,?,?,?,?,?)";

  // Query to insert title publication year aggregates.
  private static final String SQL_QUERY_TITLE_PUBYEAR_AGGREGATE_INSERT =
      "insert into " + SQL_TABLE_PUBYEAR_AGGREGATES + " ("
	  + SQL_COLUMN_LOCKSS_ID + "," + SQL_COLUMN_IS_PUBLISHER_INVOLVED + ","
	  + SQL_COLUMN_REQUEST_YEAR + "," + SQL_COLUMN_REQUEST_MONTH + ","
	  + SQL_COLUMN_PUBLICATION_YEAR + "," + SQL_COLUMN_REQUEST_COUNT
	  + ") values (?,?,?,?,?,?)";

  // Query to count the full requests for a book instance during a month.
  private static final String SQL_QUERY_MONTH_BOOK_TYPE_REQUEST_COUNT =
      "select count(*) from " + SQL_TABLE_REQUESTS + " where "
	  + SQL_COLUMN_IS_SECTION + " = ? and " + SQL_COLUMN_REQUEST_YEAR
	  + " = ? and " + SQL_COLUMN_REQUEST_MONTH + " = ? and "
	  + SQL_COLUMN_LOCKSS_ID + " = ? and "
	  + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
	  + SQL_COLUMN_IN_AGGREGATION + " = true";

  // Query to count the HTML requests for a journal instance during a month.
  private static final String SQL_QUERY_MONTH_JOURNAL_HTML_REQUEST_COUNT =
      "select count(*) from " + SQL_TABLE_REQUESTS + " where "
	  + SQL_COLUMN_IS_HTML + " = true and " + SQL_COLUMN_REQUEST_YEAR
	  + " = ? and " + SQL_COLUMN_REQUEST_MONTH + " = ? and "
	  + SQL_COLUMN_LOCKSS_ID + " = ? and "
	  + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
	  + SQL_COLUMN_IN_AGGREGATION + " = true";

  // Query to count the PDF requests for a journal instance during a month.
  private static final String SQL_QUERY_MONTH_JOURNAL_PDF_REQUEST_COUNT =
      "select count(*) from " + SQL_TABLE_REQUESTS + " where "
	  + SQL_COLUMN_IS_PDF + " = true and " + SQL_COLUMN_REQUEST_YEAR
	  + " = ? and " + SQL_COLUMN_REQUEST_MONTH + " = ? and "
	  + SQL_COLUMN_LOCKSS_ID + " = ? and "
	  + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
	  + SQL_COLUMN_IN_AGGREGATION + " = true";

  // Query to count the total requests for a title instance during a month.
  private static final String SQL_QUERY_MONTH_TITLE_TOTAL_REQUEST_COUNT =
      "select count(*) from " + SQL_TABLE_REQUESTS + " where "
	  + SQL_COLUMN_REQUEST_YEAR + " = ? and " + SQL_COLUMN_REQUEST_MONTH
	  + " = ? and " + SQL_COLUMN_LOCKSS_ID + " = ? and "
	  + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
	  + SQL_COLUMN_IN_AGGREGATION + " = true";

  // Query to get the identifiers of the books requested during a month.
  private static final String SQL_QUERY_MONTH_TITLE_REQUEST_SELECT =
      "select distinct " + "h." + SQL_COLUMN_LOCKSS_ID + " from "
	  + SQL_TABLE_REQUESTS + " h," + SQL_TABLE_TITLES + " t" + " where "
	  + "t." + SQL_COLUMN_IS_BOOK + " = ? and " + "h."
	  + SQL_COLUMN_REQUEST_YEAR + " = ? and " + "h."
	  + SQL_COLUMN_REQUEST_MONTH + " = ? and " + "h."
	  + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
	  + SQL_COLUMN_IN_AGGREGATION + " = true and " + "h."
	  + SQL_COLUMN_LOCKSS_ID + " = " + "t." + SQL_COLUMN_LOCKSS_ID;

  // Query to get the aggregated type requests for a book during a month.
  private static final String SQL_QUERY_BOOK_TYPE_AGGREGATE_SELECT = "select "
      + SQL_COLUMN_FULL_BOOK_REQUESTS + "," + SQL_COLUMN_SECTION_BOOK_REQUESTS
      + " from " + SQL_TABLE_TYPE_AGGREGATES + " where " + SQL_COLUMN_LOCKSS_ID
      + " = ? and " + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
      + SQL_COLUMN_REQUEST_YEAR + " = ? and " + SQL_COLUMN_REQUEST_MONTH
      + " = ?";

  // Query to get the aggregated type requests for a journal during a month.
  private static final String SQL_QUERY_JOURNAL_TYPE_AGGREGATE_SELECT =
      "select " + SQL_COLUMN_TOTAL_JOURNAL_REQUESTS + ","
	  + SQL_COLUMN_HTML_JOURNAL_REQUESTS + ","
	  + SQL_COLUMN_PDF_JOURNAL_REQUESTS + " from "
	  + SQL_TABLE_TYPE_AGGREGATES + " where " + SQL_COLUMN_LOCKSS_ID
	  + " = ? and " + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
	  + SQL_COLUMN_REQUEST_YEAR + " = ? and " + SQL_COLUMN_REQUEST_MONTH
	  + " = ?";

  // Query to get the publication year requests for a journal instance during a
  // month.
  private static final String SQL_QUERY_MONTH_TITLE_PUBYEAR_REQUEST_SELECT =
      "select " + SQL_COLUMN_PUBLICATION_YEAR + ", count("
	  + SQL_COLUMN_PUBLICATION_YEAR + ") as " + SQL_COLUMN_REQUEST_COUNT
	  + " from " + SQL_TABLE_REQUESTS + " where " + SQL_COLUMN_REQUEST_YEAR
	  + " = ? and " + SQL_COLUMN_REQUEST_MONTH + " = ? and "
	  + SQL_COLUMN_LOCKSS_ID + " = ? and "
	  + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
	  + SQL_COLUMN_IN_AGGREGATION + " = true " + "group by "
	  + SQL_COLUMN_PUBLICATION_YEAR;

  // Query to get the aggregated publication year requests for a journal during
  // a month.
  private static final String SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATE_SELECT =
      "select " + SQL_COLUMN_REQUEST_COUNT + " from "
	  + SQL_TABLE_PUBYEAR_AGGREGATES + " where " + SQL_COLUMN_LOCKSS_ID
	  + " = ? and " + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
	  + SQL_COLUMN_REQUEST_YEAR + " = ? and " + SQL_COLUMN_REQUEST_MONTH
	  + " = ? and " + SQL_COLUMN_PUBLICATION_YEAR + " = ?";

  // Query to get the year/month pairs that have requests to be aggregated.
  private static final String SQL_QUERY_YEAR_MONTH_REQUEST_SELECT =
      "select distinct " + SQL_COLUMN_REQUEST_YEAR + ","
	  + SQL_COLUMN_REQUEST_MONTH + " from " + SQL_TABLE_REQUESTS
	  + " where " + SQL_COLUMN_IN_AGGREGATION + " = true";

  // Query to mark the requests to be aggregated.
  private static final String SQL_QUERY_MARK_REQUESTS_UPDATE = "update "
      + SQL_TABLE_REQUESTS + " set " + SQL_COLUMN_IN_AGGREGATION + " = true";

  // Query to update book type aggregates.
  private static final String SQL_QUERY_BOOK_TYPE_AGGREGATE_UPDATE = "update "
      + SQL_TABLE_TYPE_AGGREGATES + " set " + SQL_COLUMN_FULL_BOOK_REQUESTS
      + " = ?," + SQL_COLUMN_SECTION_BOOK_REQUESTS + " = ? where "
      + SQL_COLUMN_LOCKSS_ID + " = ? and " + SQL_COLUMN_IS_PUBLISHER_INVOLVED
      + " = ? and " + SQL_COLUMN_REQUEST_YEAR + " = ? and "
      + SQL_COLUMN_REQUEST_MONTH + " = ?";

  // Query to update journal type aggregates.
  private static final String SQL_QUERY_JOURNAL_TYPE_AGGREGATE_UPDATE =
      "update " + SQL_TABLE_TYPE_AGGREGATES + " set "
	  + SQL_COLUMN_TOTAL_JOURNAL_REQUESTS + " = ?,"
	  + SQL_COLUMN_HTML_JOURNAL_REQUESTS + " = ?,"
	  + SQL_COLUMN_PDF_JOURNAL_REQUESTS + " = ? where "
	  + SQL_COLUMN_LOCKSS_ID + " = ? and "
	  + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
	  + SQL_COLUMN_REQUEST_YEAR + " = ? and " + SQL_COLUMN_REQUEST_MONTH
	  + " = ?";

  // Query to update title publication year aggregates.
  private static final String SQL_QUERY_TITLE_PUBYEAR_AGGREGATE_UPDATE =
      "update " + SQL_TABLE_PUBYEAR_AGGREGATES + " set "
	  + SQL_COLUMN_REQUEST_COUNT + " = ? where " + SQL_COLUMN_LOCKSS_ID
	  + " = ? and " + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
	  + SQL_COLUMN_REQUEST_YEAR + " = ? and " + SQL_COLUMN_REQUEST_MONTH
	  + " = ? and " + SQL_COLUMN_PUBLICATION_YEAR + " = ?";

  // Query to delete title requests for a month and a given publisher
  // involvement.
  private static final String SQL_QUERY_TITLE_REQUEST_DELETE = "delete from "
      + SQL_TABLE_REQUESTS + " where " + SQL_COLUMN_LOCKSS_ID + " = ? and "
      + SQL_COLUMN_IS_PUBLISHER_INVOLVED + " = ? and "
      + SQL_COLUMN_REQUEST_YEAR + " = ? and " + SQL_COLUMN_REQUEST_MONTH
      + " = ? and " + SQL_COLUMN_IN_AGGREGATION + " = true";

  private LockssDaemon daemon;

  /**
   * Constructor.
   * 
   * @param daemon
   *          A LockssDaemon with the application daemon.
   */
  public CounterReportsRequestAggregator(LockssDaemon daemon) {
    this.daemon = daemon;
  }

  /**
   * Provides the Cron task used to schedule the aggregation.
   * 
   * @return a Cron.Task with the task used to schedule the aggregation.
   */
  public Cron.Task getCronTask() {
    return new CounterReportsRequestAggregatorCronTask();
  }

  /**
   * Performs the periodic task of aggregating COUNTER reports data.
   * 
   * @return <code>true</code> if the task can be considered executed,
   *         <code>false</code> otherwise.
   */
  private boolean aggregate() {
    final String DEBUG_HEADER = "aggregate(): ";

    // Mark the requests to be aggregated.
    try {
      markRequestsToAggregate();
    } catch (SQLException sqle) {
      log.error("Cannot mark the requests to be aggregated", sqle);
      return false;
    }

    // Get the different months with requests to be aggregated.
    Map<Integer, List<Integer>> yearMonthsWithRequests = null;
    try {
      yearMonthsWithRequests = getYearMonthsWithRequests();
    } catch (SQLException sqle) {
      log.error("Cannot mark the requests to be aggregated", sqle);
      return false;
    }

    List<Integer> months = null;

    // Loop through all the years with requests.
    for (int year : yearMonthsWithRequests.keySet()) {
      months = yearMonthsWithRequests.get(year);

      // Loop through all the months in the year with requests.
      for (int month : months) {
	log.debug2("Year = " + year + ", Month = " + month);
	try {
	  // Aggregate the month.
	  persistMonthAggregates(year, month);
	} catch (SQLException sqle) {
	  log.error("Cannot aggregate the current month", sqle);
	  log.error("Year = " + year + ", Month = " + month);
	}
      }
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return true;
  }

  /**
   * Marks the requests to aggregate.
   * 
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void markRequestsToAggregate() throws SQLException {
    final String DEBUG_HEADER = "markRequestsToAggregate: ";
    log.debug2(DEBUG_HEADER + "Starting...");

    DbManager dbManager = daemon.getDbManager();
    Connection conn = null;
    PreparedStatement markRequests = null;
    boolean success = false;
    String sql = SQL_QUERY_MARK_REQUESTS_UPDATE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Prepare the statement used to mark the requests.
      markRequests = conn.prepareStatement(sql);

      // Mark the requests.
      int count = markRequests.executeUpdate();

      success = true;
      log.debug2(DEBUG_HEADER + "count = " + count);
    } finally {
      DbManager.safeCloseStatement(markRequests);
      if (success) {
	DbManager.commitOrRollback(conn, log);
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the year/month pairs for which there are requests that need to be
   * aggregated.
   * 
   * @return a Map<Integer, List<Integer>> with the year/month pairs.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private Map<Integer, List<Integer>> getYearMonthsWithRequests()
      throws SQLException {
    final String DEBUG_HEADER = "getYearMonthsWithRequests(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    DbManager dbManager = daemon.getDbManager();
    Connection conn = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int year;
    int month;
    List<Integer> months;

    Map<Integer, List<Integer>> yearMonths =
	new HashMap<Integer, List<Integer>>();
    String sql = SQL_QUERY_YEAR_MONTH_REQUEST_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Prepare the statement used to get the year/month pairs for which there
      // are requests that need to be aggregated.
      statement = conn.prepareStatement(sql);

      // Get the year/month pairs.
      resultSet = statement.executeQuery();

      while (resultSet.next()) {
	year = resultSet.getInt(SQL_COLUMN_REQUEST_YEAR);
	log.debug2(DEBUG_HEADER + "year = " + year);
	month = resultSet.getInt(SQL_COLUMN_REQUEST_MONTH);
	log.debug2(DEBUG_HEADER + "month = " + month);

	if (yearMonths.containsKey(year)) {
	  months = yearMonths.get(year);
	} else {
	  months = new ArrayList<Integer>();
	}

	log.debug2(DEBUG_HEADER + "months.size() = " + months.size());

	months.add(month);
	yearMonths.put(year, months);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the year/month pairs with requests", sqle);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return yearMonths;
  }

  /**
   * Persists all the aggregates for a month.
   * 
   * @param year
   *          An int with the year of the month to be persisted.
   * @param month
   *          An int with the month to be persisted.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void persistMonthAggregates(int year, int month) throws SQLException {
    // Persist the book request aggregates during the month for requests in
    // which the publisher is involved.
    persistMonthBookAggregates(year, month, true);

    // Persist the book request aggregates during the month for requests in
    // which the publisher is not involved.
    persistMonthBookAggregates(year, month, false);

    // Persist the journal request aggregates during the month for requests in
    // which the publisher is involved.
    persistMonthJournalAggregates(year, month, true);

    // Persist the journal request aggregates during the month for requests in
    // which the publisher is not involved.
    persistMonthJournalAggregates(year, month, false);
  }

  /**
   * Persists all the book aggregates during a month.
   * 
   * @param year
   *          An int with the year of the month to be persisted.
   * @param month
   *          An int with the month to be persisted.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the aggregated requests.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void persistMonthBookAggregates(int year, int month,
      boolean publisherInvolved) throws SQLException {
    final String DEBUG_HEADER = "persistMonthBookAggregates(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    // Get the identifiers of the books with requests to aggregate.
    List<Long> lockssIds =
	getMonthBooksToAggregate(year, month, publisherInvolved);

    DbManager dbManager = daemon.getDbManager();
    Connection conn = null;
    Map<String, Integer> previousAggregate = null;
    int previousFullCount = 0;
    int previousSectionCount = 0;
    Map<String, Integer> aggregate = null;
    int fullCount = 0;
    int sectionCount = 0;
    int allBooksFullCount = 0;
    int allBooksSectionCount = 0;
    int aggregatedBooks = 0;
    boolean success = false;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Loop through all the books requested during this month.
      for (long lockssId : lockssIds) {

	// Aggregate the month book requests.
	aggregate =
	    aggregateMonthBookTypes(year, month, lockssId, publisherInvolved,
				    conn);

	fullCount = aggregate.get(SQL_COLUMN_FULL_BOOK_REQUESTS);
	log.debug2(DEBUG_HEADER + "fullCount = " + fullCount);

	sectionCount = aggregate.get(SQL_COLUMN_SECTION_BOOK_REQUESTS);
	log.debug2(DEBUG_HEADER + "sectionCount = " + sectionCount);

	// Retrieve the previous aggregate of book requests.
	previousAggregate =
	    getExistingAggregateMonthBookTypes(year, month, lockssId,
					       publisherInvolved, conn);

	// Check whether there were previously recorded aggregates.
	if (previousAggregate != null) {
	  // Yes: Get the previously recorded aggregates.
	  previousFullCount =
	      previousAggregate.get(SQL_COLUMN_FULL_BOOK_REQUESTS);
	  log.debug2(DEBUG_HEADER + "previousFullCount = " + previousFullCount);

	  previousSectionCount =
	      previousAggregate.get(SQL_COLUMN_SECTION_BOOK_REQUESTS);
	  log.debug2(DEBUG_HEADER + "previousSectionCount = "
	      + previousSectionCount);

	  // Update the existing row in the database.
	  updateBookTypeAggregate(year, month, lockssId, publisherInvolved,
				  previousFullCount + fullCount,
				  previousSectionCount + sectionCount, conn);
	} else {
	  // No: Insert a new row in the database.
	  insertBookTypeAggregate(year, month, lockssId, publisherInvolved,
				  fullCount, sectionCount, conn);
	}

	// Accumulate the aggregations.
	allBooksFullCount += fullCount;
	allBooksSectionCount += sectionCount;

	// Delete the request rows.
	deleteMonthTitleRequests(year, month, lockssId, publisherInvolved, conn);

	// Check whether the maximum number of aggregated books to be persisted
	// in a transaction has been reached.
	if (++aggregatedBooks >= MAX_NUMBER_OF_TRANSACTION_AGGREGATED_TITLES) {
	  // Yes: Persist the aggregation for all the books accumulated so far.
	  persistMonthAllBookAggregates(year, month, publisherInvolved,
					allBooksFullCount,
					allBooksSectionCount, conn);

	  // Commit the changes so far.
	  DbManager.commitOrRollback(conn, log);

	  // Reset the accumulators.
	  allBooksFullCount = 0;
	  allBooksSectionCount = 0;
	  aggregatedBooks = 0;
	}
      }

      log.debug2(DEBUG_HEADER + "allBooksFullCount = " + allBooksFullCount);
      log.debug2(DEBUG_HEADER + "allBooksSectionCount = " + allBooksSectionCount);

      // Persist the remaining aggregation accumulation for all the books.
      persistMonthAllBookAggregates(year, month, publisherInvolved,
				    allBooksFullCount, allBooksSectionCount,
				    conn);

      success = true;
    } catch (SQLException sqle) {
      log.error("Cannot persist book aggregates", sqle);
      log.error("Year = " + year + ", Month = " + month);
      throw sqle;
    } finally {
      if (success) {
	DbManager.commitOrRollback(conn, log);
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the identifiers of the books with requests to be aggregated during
   * a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @return a List<Long> with the identifiers of the journals with requests to
   *         be aggregated during the month.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private List<Long> getMonthBooksToAggregate(int year, int month,
      boolean publisherInvolved) throws SQLException {
    final String DEBUG_HEADER = "getMonthBooksToAggregate(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    DbManager dbManager = daemon.getDbManager();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Connection conn = null;
    List<Long> lockssIds = new ArrayList<Long>();

    String sql = SQL_QUERY_MONTH_TITLE_REQUEST_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Prepare the statement used to get the books requested during the month.
      statement = conn.prepareStatement(sql);

      int index = 1;

      // Populate the title type indication.
      statement.setBoolean(index++, true);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the books requested during the month.
      resultSet = statement.executeQuery();

      // Save them.
      while (resultSet.next()) {
	lockssIds.add(Long.valueOf(resultSet.getLong(SQL_COLUMN_LOCKSS_ID)));
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the books to aggregates", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return lockssIds;
  }

  /**
   * Aggregates the type of requests for a book during a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param lockssId
   *          A long with the LOCKSS identifier of the book involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @return a Map<String, Integer> with the aggregated request counts.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private Map<String, Integer> aggregateMonthBookTypes(int year, int month,
      long lockssId, boolean publisherInvolved, Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "aggregateMonthBookTypes(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    Map<String, Integer> aggregates = new HashMap<String, Integer>();

    // Aggregate the full book requests.
    aggregates.put(SQL_COLUMN_FULL_BOOK_REQUESTS,
		   aggregateMonthBookType(year, month, lockssId,
					  publisherInvolved, false, conn));

    // Aggregate the book section requests.
    aggregates.put(SQL_COLUMN_SECTION_BOOK_REQUESTS,
		   aggregateMonthBookType(year, month, lockssId,
					  publisherInvolved, true, conn));

    log.debug2(DEBUG_HEADER + "Done.");
    return aggregates;
  }

  /**
   * Aggregates one type of requests for a book during a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param lockssId
   *          A long with the LOCKSS identifier of the book involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param isSection
   *          A boolean with the indication of whether the request was for a
   *          section of the book.
   * @param conn
   *          A Connection representing the database connection.
   * @return an int with the aggregated request count.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private int aggregateMonthBookType(int year, int month, long lockssId,
      boolean publisherInvolved, boolean isSection, Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "aggregateMonthBookType(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;

    String sql = SQL_QUERY_MONTH_BOOK_TYPE_REQUEST_COUNT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the count of requests.
      statement = conn.prepareStatement(sql);

      int index = 1;

      // Populate the request book type.
      statement.setBoolean(index++, isSection);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the LOCKSS identifier.
      statement.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	count = resultSet.getInt(1);
	log.debug2(DEBUG_HEADER + "count = " + count);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the month full book request count", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return count;
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
   * @param lockssId
   *          A long with the LOCKSS identifier of the book involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @return a Map<String, Integer> with the existing aggregated request counts
   *         or <code>null</code> if there are none.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private Map<String, Integer> getExistingAggregateMonthBookTypes(int year,
      int month, long lockssId, boolean publisherInvolved, Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "getExistingAggregateMonthBookTypes(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Map<String, Integer> aggregates = null;

    String sql = SQL_QUERY_BOOK_TYPE_AGGREGATE_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the existing aggregates.
      statement = conn.prepareStatement(sql);

      int index = 1;

      // Populate the LOCKSS identifier.
      statement.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	aggregates = new HashMap<String, Integer>();

	int fullCount = resultSet.getInt(SQL_COLUMN_FULL_BOOK_REQUESTS);
	log.debug2(DEBUG_HEADER + "fullCount = " + fullCount);

	int sectionCount = resultSet.getInt(SQL_COLUMN_SECTION_BOOK_REQUESTS);
	log.debug2(DEBUG_HEADER + "sectionCount = " + sectionCount);

	aggregates.put(SQL_COLUMN_FULL_BOOK_REQUESTS, fullCount);
	aggregates.put(SQL_COLUMN_SECTION_BOOK_REQUESTS, sectionCount);
      }

    } catch (SQLException sqle) {
      log.error("Cannot get the existing month book request aggregates", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
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
   * @param lockssId
   *          A long with the LOCKSS identifier of the book involved.
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
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void updateBookTypeAggregate(int year, int month, long lockssId,
      boolean publisherInvolved, int fullCount, int sectionCount,
      Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateBookTypeAggregate(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement updateAggregate = null;
    String sql = SQL_QUERY_BOOK_TYPE_AGGREGATE_UPDATE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      updateAggregate = conn.prepareStatement(sql);

      int index = 1;

      // Populate the full count.
      updateAggregate.setInt(index++, fullCount);

      // Populate the section count.
      updateAggregate.setInt(index++, sectionCount);

      // Populate the LOCKSS identifier.
      updateAggregate.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      updateAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      updateAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      updateAggregate.setShort(index++, (short) month);

      // Update the record.
      int count = updateAggregate.executeUpdate();
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update the month book request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
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
   * @param lockssId
   *          A long with the LOCKSS identifier of the book involved.
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
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void insertBookTypeAggregate(int year, int month, long lockssId,
      boolean publisherInvolved, int fullCount, int sectionCount,
      Connection conn) throws SQLException {
    final String DEBUG_HEADER = "insertBookTypeAggregate(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement insertAggregate = null;
    String sql = SQL_QUERY_BOOK_TYPE_AGGREGATE_INSERT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      insertAggregate = conn.prepareStatement(sql);

      int index = 1;

      // Populate the LOCKSS identifier.
      insertAggregate.setLong(index++, lockssId);

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
      int count = insertAggregate.executeUpdate();
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot insert the month book request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseStatement(insertAggregate);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Persists the aggregates for all books during a month.
   * 
   * @param year
   *          An int with the year of the month to be persisted.
   * @param month
   *          An int with the month to be persisted.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param fullCount
   *          An int with the count of full requests for all books during the
   *          month.
   * @param sectionCount
   *          An int with the count of section requests for all the books during
   *          the month.
   * @param conn
   *          A Connection representing the database connection.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void persistMonthAllBookAggregates(int year, int month,
      boolean publisherInvolved, int fullCount, int sectionCount,
      Connection conn) throws SQLException {
    final String DEBUG_HEADER = "persistMonthAllBookAggregates(): ";
    log.debug2(DEBUG_HEADER + "fullCount = " + fullCount);
    log.debug2(DEBUG_HEADER + "sectionCount = " + sectionCount);

    CounterReportsManager manager = daemon.getCounterReportsManager();

    // Retrieve the previous aggregates of all book requests.
    Map<String, Integer> previousAggregate =
	getExistingAggregateMonthBookTypes(year, month,
					   manager.getAllBooksLockssId(),
					   publisherInvolved, conn);

    // Check whether there were previously recorded aggregates for all books.
    if (previousAggregate != null) {
      // Yes: Add the previously recorded aggregates for all books.
      int allBooksFullCount =
	  fullCount + previousAggregate.get(SQL_COLUMN_FULL_BOOK_REQUESTS);
      log.debug2(DEBUG_HEADER + "allBooksFullCount = " + allBooksFullCount);

      int allBooksSectionCount =
	  sectionCount
	      + previousAggregate.get(SQL_COLUMN_SECTION_BOOK_REQUESTS);
      log.debug2(DEBUG_HEADER + "allBooksSectionCount = " + allBooksSectionCount);

      // Update the existing row in the database.
      updateBookTypeAggregate(year, month, manager.getAllBooksLockssId(),
			      publisherInvolved, allBooksFullCount,
			      allBooksSectionCount, conn);
    } else {
      // No: Insert a new row in the database.
      insertBookTypeAggregate(year, month, manager.getAllBooksLockssId(),
			      publisherInvolved, fullCount, sectionCount, conn);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Persists all the journal aggregates during a month.
   * 
   * @param year
   *          An int with the year of the month to be persisted.
   * @param month
   *          An int with the month to be persisted.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the aggregated requests.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void persistMonthJournalAggregates(int year, int month,
      boolean publisherInvolved) throws SQLException {
    final String DEBUG_HEADER = "persistMonthJournalAggregates(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    // Get the identifiers of the journals with requests to aggregate.
    List<Long> lockssIds =
	getMonthJournalsToAggregate(year, month, publisherInvolved);

    DbManager dbManager = daemon.getDbManager();
    Connection conn = null;
    Map<String, Integer> previousAggregate = null;
    int previousTotalCount = 0;
    int previousHtmlCount = 0;
    int previousPdfCount = 0;
    Map<String, Integer> typeAggregate = null;
    int totalCount = 0;
    int htmlCount = 0;
    int pdfCount = 0;
    int allJournalsTotalCount = 0;
    int allJournalsHtmlCount = 0;
    int allJournalsPdfCount = 0;
    Map<Integer, Integer> pubYearAggregate = null;
    Integer previousPubYearAggregate = null;
    int aggregatedJournals = 0;
    boolean success = false;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Loop through all the journals requested during this month.
      for (long lockssId : lockssIds) {

	// Aggregate the month journal requests by type.
	typeAggregate =
	    aggregateMonthJournalTypes(year, month, lockssId,
				       publisherInvolved, conn);

	totalCount = typeAggregate.get(SQL_COLUMN_TOTAL_JOURNAL_REQUESTS);
	log.debug2(DEBUG_HEADER + "totalCount = " + totalCount);

	htmlCount = typeAggregate.get(SQL_COLUMN_HTML_JOURNAL_REQUESTS);
	log.debug2(DEBUG_HEADER + "htmlCount = " + htmlCount);

	pdfCount = typeAggregate.get(SQL_COLUMN_PDF_JOURNAL_REQUESTS);
	log.debug2(DEBUG_HEADER + "pdfCount = " + pdfCount);

	// Retrieve the previous aggregate of journal requests.
	previousAggregate =
	    getExistingAggregateMonthJournalTypes(year, month, lockssId,
						  publisherInvolved, conn);

	// Check whether there were previously recorded aggregates.
	if (previousAggregate != null) {
	  // Yes: Get the previously recorded aggregates.
	  previousTotalCount =
	      previousAggregate.get(SQL_COLUMN_TOTAL_JOURNAL_REQUESTS);
	  log.debug2(DEBUG_HEADER + "previousTotalCount = " + previousTotalCount);

	  previousHtmlCount =
	      previousAggregate.get(SQL_COLUMN_HTML_JOURNAL_REQUESTS);
	  log.debug2(DEBUG_HEADER + "previousHtmlCount = " + previousHtmlCount);

	  previousPdfCount =
	      previousAggregate.get(SQL_COLUMN_PDF_JOURNAL_REQUESTS);
	  log.debug2(DEBUG_HEADER + "previousPdfCount = " + previousPdfCount);

	  // Update the existing row in the database.
	  updateJournalTypeAggregate(year, month, lockssId, publisherInvolved,
				     previousTotalCount + totalCount,
				     previousHtmlCount + htmlCount,
				     previousPdfCount + pdfCount, conn);
	} else {
	  // No: Insert a new row in the database.
	  insertJournalTypeAggregate(year, month, lockssId, publisherInvolved,
				     totalCount, htmlCount, pdfCount, conn);
	}

	// Accumulate the aggregations.
	allJournalsTotalCount += totalCount;
	allJournalsHtmlCount += htmlCount;
	allJournalsPdfCount += pdfCount;

	// Get the aggregates by publication year.
	pubYearAggregate =
	    aggregateMonthJournalPubYears(year, month, lockssId,
					  publisherInvolved, conn);

	// Loop through the aggregates by publication year.
	for (int publicationYear : pubYearAggregate.keySet()) {
	  // Retrieve the previous aggregate for the publication year.
	  previousPubYearAggregate =
	      getExistingAggregateMonthJournalPubYear(year, month, lockssId,
						      publisherInvolved,
						      publicationYear, conn);

	  // Check whether there was previously a recorded aggregate for the
	  // publication year.
	  if (previousPubYearAggregate != null) {
	    updateTitlePubYearAggregate(year,
					month,
					lockssId,
					publisherInvolved,
					publicationYear,
					previousPubYearAggregate.intValue()
					    + pubYearAggregate
						.get(publicationYear)
						.intValue(), conn);
	  } else {
	    // No: Insert a new row in the database.
	    insertTitlePubYearAggregate(year, month, lockssId,
					publisherInvolved, publicationYear,
					pubYearAggregate.get(publicationYear),
					conn);
	  }
	}

	// Delete the request rows.
	deleteMonthTitleRequests(year, month, lockssId, publisherInvolved, conn);

	// Check whether the maximum number of aggregated journals to be
	// persisted in a transaction has been reached.
	if (++aggregatedJournals >= MAX_NUMBER_OF_TRANSACTION_AGGREGATED_TITLES) {
	  // Yes: Persist the aggregation accumulation for all the journals.
	  persistMonthAllJournalAggregates(year, month, publisherInvolved,
					   allJournalsTotalCount,
					   allJournalsHtmlCount,
					   allJournalsPdfCount, conn);

	  // Commit the changes so far.
	  DbManager.commitOrRollback(conn, log);

	  allJournalsTotalCount = 0;
	  allJournalsHtmlCount = 0;
	  allJournalsPdfCount = 0;
	  aggregatedJournals = 0;
	}
      }

      log.debug2(DEBUG_HEADER + "allJournalsTotalCount = "
	  + allJournalsTotalCount);
      log.debug2(DEBUG_HEADER + "allJournalsHtmlCount = " + allJournalsHtmlCount);
      log.debug2(DEBUG_HEADER + "allJournalsPdfCount = " + allJournalsPdfCount);

      // Persist the all journals accumulations.
      persistMonthAllJournalAggregates(year, month, publisherInvolved,
				       allJournalsTotalCount,
				       allJournalsHtmlCount,
				       allJournalsPdfCount, conn);

      success = true;
    } catch (SQLException sqle) {
      log.error("Cannot persist journal aggregates", sqle);
      log.error("Year = " + year + ", Month = " + month);
      throw sqle;
    } finally {
      if (success) {
	DbManager.commitOrRollback(conn, log);
	DbManager.safeCloseConnection(conn);
      } else {
	DbManager.safeRollbackAndClose(conn);
      }
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the identifiers of the journals with requests to be aggregated
   * during a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private List<Long> getMonthJournalsToAggregate(int year, int month,
      boolean publisherInvolved) throws SQLException {
    final String DEBUG_HEADER = "getMonthJournalsToAggregate(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    DbManager dbManager = daemon.getDbManager();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Connection conn = null;
    List<Long> lockssIds = new ArrayList<Long>();

    String sql = SQL_QUERY_MONTH_TITLE_REQUEST_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Prepare the statement used to get the journals.
      statement = conn.prepareStatement(sql);

      int index = 1;

      // Populate the title type indication.
      statement.setBoolean(index++, false);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the journals requested during the month.
      resultSet = statement.executeQuery();

      // Save them.
      while (resultSet.next()) {
	lockssIds.add(Long.valueOf(resultSet.getLong(SQL_COLUMN_LOCKSS_ID)));
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the journals to aggregates", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return lockssIds;
  }

  /**
   * Aggregates the type requests for a journal during a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param lockssId
   *          A long with the LOCKSS identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @return a Map<String, Integer> with the aggregated request counts.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private Map<String, Integer> aggregateMonthJournalTypes(int year, int month,
      long lockssId, boolean publisherInvolved, Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "aggregateMonthJournalTypes(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int totalCount = -1;
    int htmlCount = -1;
    int pdfCount = -1;

    Map<String, Integer> aggregates = new HashMap<String, Integer>();
    String sql = SQL_QUERY_MONTH_TITLE_TOTAL_REQUEST_COUNT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to aggregate the total requests.
      statement = conn.prepareStatement(sql);

      int index = 1;

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the LOCKSS identifier.
      statement.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the total request count.
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	totalCount = resultSet.getInt(1);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the month total journal request count", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    sql = SQL_QUERY_MONTH_JOURNAL_HTML_REQUEST_COUNT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to aggregate the HTML requests.
      statement = conn.prepareStatement(sql);

      int index = 1;

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the LOCKSS identifier.
      statement.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the HTML request count.
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	htmlCount = resultSet.getInt(1);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the month HTML journal request count", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    sql = SQL_QUERY_MONTH_JOURNAL_PDF_REQUEST_COUNT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to aggregate the PDF requests.
      statement = conn.prepareStatement(sql);

      int index = 1;

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the LOCKSS identifier.
      statement.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the PDF request count.
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	pdfCount = resultSet.getInt(1);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the month PDF journal request count", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    aggregates.put(SQL_COLUMN_TOTAL_JOURNAL_REQUESTS, totalCount);
    aggregates.put(SQL_COLUMN_HTML_JOURNAL_REQUESTS, htmlCount);
    aggregates.put(SQL_COLUMN_PDF_JOURNAL_REQUESTS, pdfCount);

    log.debug2(DEBUG_HEADER + "Done.");
    return aggregates;
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
   * @param lockssId
   *          A long with the LOCKSS identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @return a Map<String, Integer> with the existing aggregated request counts
   *         or <code>null</code> if there are none.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private Map<String, Integer> getExistingAggregateMonthJournalTypes(int year,
      int month, long lockssId, boolean publisherInvolved, Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "getExistingAggregateMonthJournalTypes(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Map<String, Integer> aggregates = null;

    String sql = SQL_QUERY_JOURNAL_TYPE_AGGREGATE_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the existing aggregations.
      statement = conn.prepareStatement(sql);

      int index = 1;

      // Populate the LOCKSS identifier.
      statement.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Get the existing requests counts.
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	aggregates = new HashMap<String, Integer>();

	int totalCount = resultSet.getInt(SQL_COLUMN_TOTAL_JOURNAL_REQUESTS);
	log.debug2(DEBUG_HEADER + "totalCount = " + totalCount);

	int htmlCount = resultSet.getInt(SQL_COLUMN_HTML_JOURNAL_REQUESTS);
	log.debug2(DEBUG_HEADER + "htmlCount = " + htmlCount);

	int pdfCount = resultSet.getInt(SQL_COLUMN_PDF_JOURNAL_REQUESTS);
	log.debug2(DEBUG_HEADER + "pdfCount = " + pdfCount);

	aggregates.put(SQL_COLUMN_TOTAL_JOURNAL_REQUESTS, totalCount);
	aggregates.put(SQL_COLUMN_HTML_JOURNAL_REQUESTS, htmlCount);
	aggregates.put(SQL_COLUMN_PDF_JOURNAL_REQUESTS, pdfCount);
      }

    } catch (SQLException sqle) {
      log.error("Cannot get the existing month book request aggregates", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
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
   * @param lockssId
   *          A long with the LOCKSS identifier of the journal involved.
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
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void updateJournalTypeAggregate(int year, int month, long lockssId,
      boolean publisherInvolved, int totalCount, int htmlCount, int pdfCount,
      Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateJournalTypeAggregate(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement updateAggregate = null;
    String sql = SQL_QUERY_JOURNAL_TYPE_AGGREGATE_UPDATE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      updateAggregate = conn.prepareStatement(sql);

      int index = 1;

      // Populate the total count.
      updateAggregate.setInt(index++, totalCount);

      // Populate the HTML count.
      updateAggregate.setInt(index++, htmlCount);

      // Populate the PDF count.
      updateAggregate.setInt(index++, pdfCount);

      // Populate the LOCKSS identifier.
      updateAggregate.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      updateAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      updateAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      updateAggregate.setShort(index++, (short) month);

      // Update the record.
      int count = updateAggregate.executeUpdate();
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update the month journal request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
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
   * @param lockssId
   *          A long with the LOCKSS identifier of the journal involved.
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
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void insertJournalTypeAggregate(int year, int month, long lockssId,
      boolean publisherInvolved, int totalCount, int htmlCount, int pdfCount,
      Connection conn) throws SQLException {
    final String DEBUG_HEADER = "insertJournalAggregate(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement insertAggregate = null;
    String sql = SQL_QUERY_JOURNAL_TYPE_AGGREGATE_INSERT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      insertAggregate = conn.prepareStatement(sql);

      int index = 1;

      // Populate the LOCKSS identifier.
      insertAggregate.setLong(index++, lockssId);

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
      int count = insertAggregate.executeUpdate();
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot insert the month journal request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseStatement(insertAggregate);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Aggregates the publication year requests for a journal during a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param lockssId
   *          A long with the LOCKSS identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @return a Map<Integer, Integer> with the aggregated request counts.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private Map<Integer, Integer> aggregateMonthJournalPubYears(int year,
      int month, long lockssId, boolean publisherInvolved, Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "aggregateMonthJournalPubYears(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement statement = null;
    ResultSet resultSet = null;

    Map<Integer, Integer> aggregates = new HashMap<Integer, Integer>();
    String sql = SQL_QUERY_MONTH_TITLE_PUBYEAR_REQUEST_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to aggregate the requests by publication
      // year.
      statement = conn.prepareStatement(sql);

      int index = 1;

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the LOCKSS identifier.
      statement.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the aggregations of requests by publication year.
      resultSet = statement.executeQuery();

      while (resultSet.next()) {
	aggregates.put(resultSet.getInt(SQL_COLUMN_PUBLICATION_YEAR),
		       resultSet.getInt(SQL_COLUMN_REQUEST_COUNT));
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the month publication year journal request count",
		sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return aggregates;
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
   * @param lockssId
   *          A long with the LOCKSS identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param publicationYear
   *          An int with the publication year.
   * @param conn
   *          A Connection representing the database connection.
   * @return an Integer with the existing aggregated request count or
   *         <code>null</code> if there is none.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private Integer getExistingAggregateMonthJournalPubYear(int year, int month,
      long lockssId, boolean publisherInvolved, int publicationYear,
      Connection conn) throws SQLException {
    final String DEBUG_HEADER = "getExistingAggregateMonthJournalPubYear(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Integer count = null;

    String sql = SQL_QUERY_JOURNAL_PUBYEAR_AGGREGATE_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the existing aggregation.
      statement = conn.prepareStatement(sql);

      int index = 1;

      // Populate the LOCKSS identifier.
      statement.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publication year.
      statement.setInt(index++, publicationYear);

      // Get the existing requests count.
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
	count = resultSet.getInt(SQL_COLUMN_REQUEST_COUNT);
	log.debug2(DEBUG_HEADER + "count = " + count);
      }

    } catch (SQLException sqle) {
      log.error("Cannot get the existing month journal publication year request aggregate",
		sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
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
   * @param lockssId
   *          A long with the LOCKSS identifier of the journal involved.
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
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void updateTitlePubYearAggregate(int year, int month, long lockssId,
      boolean publisherInvolved, int publicationYear, int requestCount,
      Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateTitlePubYearAggregate(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement updateAggregate = null;
    String sql = SQL_QUERY_TITLE_PUBYEAR_AGGREGATE_UPDATE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      updateAggregate = conn.prepareStatement(sql);

      int index = 1;

      // Populate the request count.
      updateAggregate.setInt(index++, requestCount);

      // Populate the LOCKSS identifier.
      updateAggregate.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      updateAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      updateAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      updateAggregate.setShort(index++, (short) month);

      // Populate the publication year.
      updateAggregate.setShort(index++, (short) publicationYear);

      // Insert the record.
      int count = updateAggregate.executeUpdate();
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update the month journal request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
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
   * @param lockssId
   *          A long with the LOCKSS identifier of the journal involved.
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
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void insertTitlePubYearAggregate(int year, int month, long lockssId,
      boolean publisherInvolved, int publicationYear, int requestCount,
      Connection conn) throws SQLException {
    final String DEBUG_HEADER = "insertTitlePubYearAggregate(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement insertAggregate = null;
    String sql = SQL_QUERY_TITLE_PUBYEAR_AGGREGATE_INSERT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to persist the record.
      insertAggregate = conn.prepareStatement(sql);

      int index = 1;

      // Populate the LOCKSS identifier.
      insertAggregate.setLong(index++, lockssId);

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
      int count = insertAggregate.executeUpdate();
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot insert the month journal request counts", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("LockssId = " + lockssId);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseStatement(insertAggregate);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Persists the aggregates for all journals during a month.
   * 
   * @param year
   *          An int with the year of the month to be persisted.
   * @param month
   *          An int with the month to be persisted.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param totalCount
   *          An int with the total count of requests for all the journals
   *          during the month.
   * @param htmlCount
   *          An int with the count of HTML requests for all the journals during
   *          the month.
   * @param pdfCount
   *          An int with the count of PDF requests for all the journals during
   *          the month.
   * @param conn
   *          A Connection representing the database connection.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void persistMonthAllJournalAggregates(int year, int month,
      boolean publisherInvolved, int totalCount, int htmlCount, int pdfCount,
      Connection conn) throws SQLException {
    final String DEBUG_HEADER = "persistMonthAllJournalAggregates(): ";
    log.debug2(DEBUG_HEADER + "totalCount = " + totalCount);
    log.debug2(DEBUG_HEADER + "htmlCount = " + htmlCount);
    log.debug2(DEBUG_HEADER + "pdfCount = " + pdfCount);

    CounterReportsManager manager = daemon.getCounterReportsManager();

    // Retrieve the previous aggregates of all journal requests.
    Map<String, Integer> previousAggregate =
	getExistingAggregateMonthJournalTypes(year, month,
					      manager.getAllJournalsLockssId(),
					      publisherInvolved, conn);

    // Check whether there were previously recorded aggregates for all journals.
    if (previousAggregate != null) {
      // Yes: Add the previously recorded aggregates for all journals.
      int allJournalsTotalCount =
	  totalCount + previousAggregate.get(SQL_COLUMN_TOTAL_JOURNAL_REQUESTS);
      log.debug2(DEBUG_HEADER + "allJournalsTotalCount = "
	  + allJournalsTotalCount);

      int allJournalsHtmlCount =
	  htmlCount + previousAggregate.get(SQL_COLUMN_HTML_JOURNAL_REQUESTS);
      log.debug2(DEBUG_HEADER + "allJournalsHtmlCount = " + allJournalsHtmlCount);

      int allJournalsPdfCount =
	  pdfCount + previousAggregate.get(SQL_COLUMN_PDF_JOURNAL_REQUESTS);
      log.debug2(DEBUG_HEADER + "allJournalsPdfCount = " + allJournalsPdfCount);

      // Update the existing row in the database.
      updateJournalTypeAggregate(year, month, manager.getAllJournalsLockssId(),
				 publisherInvolved, allJournalsTotalCount,
				 allJournalsHtmlCount, allJournalsPdfCount,
				 conn);
    } else {
      // No: Insert a new row in the database.
      insertJournalTypeAggregate(year, month, manager.getAllJournalsLockssId(),
				 publisherInvolved, totalCount, htmlCount,
				 pdfCount, conn);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Deletes all the requests recorded during a month.
   * 
   * @param year
   *          An int with the year of the month of the requests to be deleted.
   * @param month
   *          An int with the month of the requests to be deleted.
   * @param lockssId
   *          A long with the LOCKSS identifier of the title involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @throws SQLException
   *           if there are problems accessing the database.
   */
  private void deleteMonthTitleRequests(int year, int month, long lockssId,
      boolean publisherInvolved, Connection conn) throws SQLException {
    final String DEBUG_HEADER = "deleteMonthTitleRequests(): ";
    log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement deleteAggregate = null;
    String sql = SQL_QUERY_TITLE_REQUEST_DELETE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to delete the rows.
      deleteAggregate = conn.prepareStatement(sql);

      int index = 1;

      // Populate the LOCKSS identifier.
      deleteAggregate.setLong(index++, lockssId);

      // Populate the publisher involvement indicator.
      deleteAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      deleteAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      deleteAggregate.setShort(index++, (short) month);

      // Delete the record.
      int count = deleteAggregate.executeUpdate();
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot delete the title month requests", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseStatement(deleteAggregate);
    }
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
  public class CounterReportsRequestAggregatorCronTask implements Cron.Task {
    /**
     * Performs the periodic task of aggregating COUNTER reports data.
     * 
     * @return <code>true</code> if the task can be considered executed,
     *         <code>false</code> otherwise.
     */
    @Override
    public boolean execute() {
      return aggregate();
    }

    /**
     * Provides the identifier of the periodic task.
     * 
     * @return a String with the identifier of the periodic task.
     */
    @Override
    public String getId() {
      return "CounterReportsRequestAggregator";
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
		       CurrentConfig
			   .getParam(PARAM_COUNTER_REQUEST_AGGREGATION_TASK_FREQUENCY,
				     DEFAULT_COUNTER_REQUEST_AGGREGATION_TASK_FREQUENCY));
    }
  }
}