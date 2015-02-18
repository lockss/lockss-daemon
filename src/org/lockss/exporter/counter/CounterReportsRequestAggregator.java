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
import static org.lockss.plugin.ArticleFiles.*;
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
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.util.Logger;

/**
 * Periodically aggregates request statistics used in COUNTER reports and stores
 * them in the database.
 * 
 * @version 1.0
 */
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
      .getLogger(CounterReportsRequestAggregator.class);

  // Query to count the full requests for a book instance during a month.
  private static final String SQL_QUERY_MONTH_BOOK_FULL_REQUEST_COUNT =
      "select count(*) "
      + "from " + COUNTER_REQUEST_TABLE + " r"
      + "," + URL_TABLE + " u"
      + "," + MD_ITEM_TABLE + " m"
      + "," + MD_ITEM_TYPE_TABLE + " t"
      + "," + PUBLICATION_TABLE + " p"
      + " where " + "r." + URL_COLUMN + " = " + "u." + URL_COLUMN
      + " and " + "(u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_HTML + "'"
      + " or " + "u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_PDF + "')"
      + " and " + "u." + MD_ITEM_SEQ_COLUMN + " = " + "m." + MD_ITEM_SEQ_COLUMN
      + " and " + "m." + MD_ITEM_TYPE_SEQ_COLUMN + " = "
      + "t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and " + "t." + TYPE_NAME_COLUMN + " = '" + MD_ITEM_TYPE_BOOK + "'"
      + " and " + "m." + PARENT_SEQ_COLUMN + " = " + "p." + MD_ITEM_SEQ_COLUMN
      + " and " + "p." + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and " + "r." + REQUEST_YEAR_COLUMN + " = ?"
      + " and " + "r." + REQUEST_MONTH_COLUMN + " = ?"
      + " and " + "r." + IS_PUBLISHER_INVOLVED_COLUMN + " = ?"
      + " and " + "r." + IN_AGGREGATION_COLUMN + " = true";

  // Query to count the section requests for a book instance during a month.
  private static final String SQL_QUERY_MONTH_BOOK_SECTION_REQUEST_COUNT =
      "select count(*) "
      + "from " + COUNTER_REQUEST_TABLE + " r"
      + "," + URL_TABLE + " u"
      + "," + MD_ITEM_TABLE + " m"
      + "," + MD_ITEM_TYPE_TABLE + " t"
      + "," + PUBLICATION_TABLE + " p"
      + " where " + "r." + URL_COLUMN + " = " + "u." + URL_COLUMN
      + " and " + "(u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_HTML + "'"
      + " or " + "u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_PDF + "')"
      + " and " + "u." + MD_ITEM_SEQ_COLUMN + " = " + "m." + MD_ITEM_SEQ_COLUMN
      + " and " + "m." + MD_ITEM_TYPE_SEQ_COLUMN + " = "
      + "t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and " + "t." + TYPE_NAME_COLUMN + " = '" + MD_ITEM_TYPE_BOOK_CHAPTER
      + "'"
      + " and " + "m." + PARENT_SEQ_COLUMN + " = " + "p." + MD_ITEM_SEQ_COLUMN
      + " and " + "p." + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and " + "r." + REQUEST_YEAR_COLUMN + " = ?"
      + " and " + "r." + REQUEST_MONTH_COLUMN + " = ?"
      + " and " + "r." + IS_PUBLISHER_INVOLVED_COLUMN + " = ?"
      + " and " + "r." + IN_AGGREGATION_COLUMN + " = true";

  // Query to count the HTML requests for a journal instance during a month.
  private static final String SQL_QUERY_MONTH_JOURNAL_HTML_REQUEST_COUNT =
      "select count(*) "
      + "from " + COUNTER_REQUEST_TABLE + " r"
      + "," + URL_TABLE + " u"
      + "," + MD_ITEM_TABLE + " m"
      + "," + MD_ITEM_TYPE_TABLE + " t"
      + "," + PUBLICATION_TABLE + " p"
      + " where " + "r." + URL_COLUMN + " = " + "u." + URL_COLUMN
      + " and " + "u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_HTML + "'"
      + " and " + "u." + MD_ITEM_SEQ_COLUMN + " = " + "m." + MD_ITEM_SEQ_COLUMN
      + " and " + "m." + MD_ITEM_TYPE_SEQ_COLUMN + " = "
      + "t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and " + "t." + TYPE_NAME_COLUMN + " = '"
      + MD_ITEM_TYPE_JOURNAL_ARTICLE + "'"
      + " and " + "m." + PARENT_SEQ_COLUMN + " = " + "p." + MD_ITEM_SEQ_COLUMN
      + " and " + "p." + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and " + "r." + REQUEST_YEAR_COLUMN + " = ?"
      + " and " + "r." + REQUEST_MONTH_COLUMN + " = ?"
      + " and " + "r." + IS_PUBLISHER_INVOLVED_COLUMN + " = ?"
      + " and " + "r." + IN_AGGREGATION_COLUMN + " = true";

  // Query to count the PDF requests for a journal instance during a month.
  private static final String SQL_QUERY_MONTH_JOURNAL_PDF_REQUEST_COUNT =
      "select count(*) "
      + "from " + COUNTER_REQUEST_TABLE + " r"
      + "," + URL_TABLE + " u"
      + "," + MD_ITEM_TABLE + " m"
      + "," + MD_ITEM_TYPE_TABLE + " t"
      + "," + PUBLICATION_TABLE + " p"
      + " where " + "r." + URL_COLUMN + " = " + "u." + URL_COLUMN
      + " and " + "u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_PDF + "'"
      + " and " + "u." + MD_ITEM_SEQ_COLUMN + " = " + "m." + MD_ITEM_SEQ_COLUMN
      + " and " + "m." + MD_ITEM_TYPE_SEQ_COLUMN + " = "
      + "t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and " + "t." + TYPE_NAME_COLUMN + " = '"
      + MD_ITEM_TYPE_JOURNAL_ARTICLE + "'"
      + " and " + "m." + PARENT_SEQ_COLUMN + " = " + "p." + MD_ITEM_SEQ_COLUMN
      + " and " + "p." + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and " + "r." + REQUEST_YEAR_COLUMN + " = ?"
      + " and " + "r." + REQUEST_MONTH_COLUMN + " = ?"
      + " and " + "r." + IS_PUBLISHER_INVOLVED_COLUMN + " = ?"
      + " and " + "r." + IN_AGGREGATION_COLUMN + " = true";

  // Query to count the total requests for a journal during a month.
  private static final String SQL_QUERY_MONTH_JOURNAL_TOTAL_REQUEST_COUNT =
      "select count(*) "
      + "from " + COUNTER_REQUEST_TABLE + " r"
      + "," + URL_TABLE + " u"
      + "," + MD_ITEM_TABLE + " m"
      + "," + MD_ITEM_TYPE_TABLE + " t"
      + "," + PUBLICATION_TABLE + " p"
      + " where " + "r." + URL_COLUMN + " = " + "u." + URL_COLUMN
      + " and " + "(u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_HTML + "'"
      + " or " + "u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_PDF + "')"
      + " and " + "u." + MD_ITEM_SEQ_COLUMN + " = " + "m." + MD_ITEM_SEQ_COLUMN
      + " and " + "m." + MD_ITEM_TYPE_SEQ_COLUMN + " = "
      + "t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and " + "t." + TYPE_NAME_COLUMN + " = '"
      + MD_ITEM_TYPE_JOURNAL_ARTICLE + "'"
      + " and " + "m." + PARENT_SEQ_COLUMN + " = " + "p." + MD_ITEM_SEQ_COLUMN
      + " and " + "p." + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and " + "r." + REQUEST_YEAR_COLUMN + " = ?"
      + " and " + "r." + REQUEST_MONTH_COLUMN + " = ?"
      + " and " + "r." + IS_PUBLISHER_INVOLVED_COLUMN + " = ?"
      + " and " + "r." + IN_AGGREGATION_COLUMN + " = true";

  // Query to get the identifiers of the books requested during a month.
  private static final String SQL_QUERY_MONTH_BOOK_REQUEST_SELECT = "select "
      + "distinct " + "p." + PUBLICATION_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE + " p"
      + "," + MD_ITEM_TABLE + " m"
      + "," + URL_TABLE + " u"
      + "," + COUNTER_REQUEST_TABLE + " r"
      + "," + MD_ITEM_TYPE_TABLE + " t"
      + " where " + "p." + MD_ITEM_SEQ_COLUMN + " = " + "m." + PARENT_SEQ_COLUMN
      + " and " + "m." + MD_ITEM_SEQ_COLUMN + " = " + "u." + MD_ITEM_SEQ_COLUMN
      + " and " + "(u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_HTML + "'"
      + " or " + "u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_PDF + "')"
      + " and " + "u." + URL_COLUMN + " = " + "r." + URL_COLUMN
      + " and " + "m." + MD_ITEM_TYPE_SEQ_COLUMN + " = "
      + "t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and " + "(t." + TYPE_NAME_COLUMN + " = '" + MD_ITEM_TYPE_BOOK + "' or"
      + " t." + TYPE_NAME_COLUMN + " = '" + MD_ITEM_TYPE_BOOK_CHAPTER + "')"
      + " and " + "r." + REQUEST_YEAR_COLUMN + " = ?"
      + " and " + "r." + REQUEST_MONTH_COLUMN + " = ?"
      + " and " + "r." + IS_PUBLISHER_INVOLVED_COLUMN + " = ?"
      + " and " + "r." + IN_AGGREGATION_COLUMN + " = true";

  // Query to get the identifiers of the journals requested during a month.
  private static final String SQL_QUERY_MONTH_JOURNAL_REQUEST_SELECT = "select "
      + "distinct " + "p." + PUBLICATION_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE + " p"
      + "," + MD_ITEM_TABLE + " m"
      + "," + URL_TABLE + " u"
      + "," + COUNTER_REQUEST_TABLE + " r"
      + "," + MD_ITEM_TYPE_TABLE + " t"
      + " where " + "p." + MD_ITEM_SEQ_COLUMN + " = " + "m." + PARENT_SEQ_COLUMN
      + " and " + "m." + MD_ITEM_SEQ_COLUMN + " = " + "u." + MD_ITEM_SEQ_COLUMN
      + " and " + "(u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_HTML + "'"
      + " or " + "u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_PDF + "')"
      + " and " + "u." + URL_COLUMN + " = " + "r." + URL_COLUMN
      + " and " + "m." + MD_ITEM_TYPE_SEQ_COLUMN + " = "
      + "t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and " + "t." + TYPE_NAME_COLUMN + " = '"
      + MD_ITEM_TYPE_JOURNAL_ARTICLE + "'"
      + " and " + "r." + REQUEST_YEAR_COLUMN + " = ?"
      + " and " + "r." + REQUEST_MONTH_COLUMN + " = ?"
      + " and " + "r." + IS_PUBLISHER_INVOLVED_COLUMN + " = ?"
      + " and " + "r." + IN_AGGREGATION_COLUMN + " = true";

  // Query to get the publication year requests for a journal instance during a
  // month.
  private static final String SQL_QUERY_MONTH_JOURNAL_PUBYEAR_REQUEST_SELECT =
      "select "
      + "m." + DATE_COLUMN
      + ", count" + "(m." + DATE_COLUMN + ") as "
      + REQUESTS_COLUMN
      + " from " + COUNTER_REQUEST_TABLE + " r"
      + "," + URL_TABLE + " u"
      + "," + MD_ITEM_TABLE + " m"
      + "," + MD_ITEM_TYPE_TABLE + " t"
      + "," + PUBLICATION_TABLE + " p"
      + " where " + "r." + URL_COLUMN + " = " + "u." + URL_COLUMN
      + " and " + "(u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_HTML + "'"
      + " or " + "u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_PDF + "')"
      + " and " + "u." + MD_ITEM_SEQ_COLUMN + " = " + "m." + MD_ITEM_SEQ_COLUMN
      + " and " + "m." + MD_ITEM_TYPE_SEQ_COLUMN + " = "
      + "t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and " + "t." + TYPE_NAME_COLUMN + " = '"
      + MD_ITEM_TYPE_JOURNAL_ARTICLE + "'"
      + " and " + "m." + PARENT_SEQ_COLUMN + " = " + "p." + MD_ITEM_SEQ_COLUMN
      + " and " + "p." + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and " + "r." + REQUEST_YEAR_COLUMN + " = ?"
      + " and " + "r." + REQUEST_MONTH_COLUMN + " = ?"
      + " and " + "r." + IS_PUBLISHER_INVOLVED_COLUMN + " = ?"
      + " and " + "r." + IN_AGGREGATION_COLUMN + " = true"
      + " group by " + "m." + DATE_COLUMN;

  // Query to get the year/month pairs that have requests to be aggregated.
  private static final String SQL_QUERY_YEAR_MONTH_REQUEST_SELECT = "select "
      + "distinct " + REQUEST_YEAR_COLUMN + ","
      + REQUEST_MONTH_COLUMN
      + " from " + COUNTER_REQUEST_TABLE
      + " where " + IN_AGGREGATION_COLUMN + " = true";

  // Query to mark the requests to be aggregated.
  private static final String SQL_QUERY_MARK_REQUESTS_UPDATE = "update "
      + COUNTER_REQUEST_TABLE
      + " set " + IN_AGGREGATION_COLUMN + " = true";

  // Query to delete book requests for a month and a given publisher
  // involvement.
  private static final String SQL_QUERY_BOOK_REQUEST_DELETE = "delete from "
      + COUNTER_REQUEST_TABLE
      + " where " + URL_COLUMN + " in (select u."
      + URL_COLUMN
      + " from " + URL_TABLE + " u"
      + "," + MD_ITEM_TABLE + " m"
      + "," + MD_ITEM_TYPE_TABLE + " t"
      + "," + PUBLICATION_TABLE + " p"
      + " where " + "(u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_HTML + "'"
      + " or " + "u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_PDF + "')"
      + " and u." + MD_ITEM_SEQ_COLUMN + " = m." + MD_ITEM_SEQ_COLUMN
      + " and " + "m." + MD_ITEM_TYPE_SEQ_COLUMN + " = "
      + "t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and " + "(t." + TYPE_NAME_COLUMN + " = '" + MD_ITEM_TYPE_BOOK + "' or"
      + " t." + TYPE_NAME_COLUMN + " = '" + MD_ITEM_TYPE_BOOK_CHAPTER + "')"
      + " and " + "m." + PARENT_SEQ_COLUMN + " = " + "p." + MD_ITEM_SEQ_COLUMN
      + " and " + "p." + PUBLICATION_SEQ_COLUMN + " = ?)"
      + " and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ? "
      + " and " + REQUEST_YEAR_COLUMN + " = ? "
      + " and " + REQUEST_MONTH_COLUMN + " = ? "
      + " and " + IN_AGGREGATION_COLUMN + " = true";

  // Query to delete journal requests for a month and a given publisher
  // involvement.
  private static final String SQL_QUERY_JOURNAL_REQUEST_DELETE = "delete from "
      + COUNTER_REQUEST_TABLE
      + " where " + URL_COLUMN + " in (select u."
      + URL_COLUMN
      + " from " + URL_TABLE + " u"
      + "," + MD_ITEM_TABLE + " m"
      + "," + MD_ITEM_TYPE_TABLE + " t"
      + "," + PUBLICATION_TABLE + " p"
      + " where " + "(u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_HTML + "'"
      + " or " + "u." + FEATURE_COLUMN + " = '" + ROLE_FULL_TEXT_PDF + "')"
      + " and u." + MD_ITEM_SEQ_COLUMN + " = m." + MD_ITEM_SEQ_COLUMN
      + " and " + "m." + MD_ITEM_TYPE_SEQ_COLUMN + " = "
      + "t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and " + "t." + TYPE_NAME_COLUMN + " = '"
      + MD_ITEM_TYPE_JOURNAL_ARTICLE + "'"
      + " and " + "m." + PARENT_SEQ_COLUMN + " = " + "p." + MD_ITEM_SEQ_COLUMN
      + " and " + "p." + PUBLICATION_SEQ_COLUMN + " = ?)"
      + " and " + IS_PUBLISHER_INVOLVED_COLUMN + " = ? "
      + " and " + REQUEST_YEAR_COLUMN + " = ? "
      + " and " + REQUEST_MONTH_COLUMN + " = ? "
      + " and " + IN_AGGREGATION_COLUMN + " = true";

  private final LockssDaemon daemon;
  private final DbManager dbManager;
  private final CounterReportsManager crManager;

  /**
   * Constructor.
   * 
   * @param daemon
   *          A LockssDaemon with the application daemon.
   */
  public CounterReportsRequestAggregator(LockssDaemon daemon) {
    this.daemon = daemon;
    dbManager = daemon.getDbManager();
    crManager = daemon.getCounterReportsManager();
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
    log.debug2(DEBUG_HEADER + "Starting...");

    // Mark the requests to be aggregated.
    try {
      markRequestsToAggregate();
    } catch (DbException dbe) {
      log.error("Cannot mark the requests to be aggregated", dbe);
      return false;
    }

    // Get the different months with requests to be aggregated.
    Map<Integer, List<Integer>> yearMonthsWithRequests = null;
    try {
      yearMonthsWithRequests = getYearMonthsWithRequests();
    } catch (DbException dbe) {
      log.error("Cannot get the months with requests to be aggregated", dbe);
      return false;
    }

    List<Integer> months = null;

    // Loop through all the years with requests.
    for (int year : yearMonthsWithRequests.keySet()) {
      months = yearMonthsWithRequests.get(year);

      // Loop through all the months in the year with requests.
      for (int month : months) {
	log.debug2(DEBUG_HEADER + "Year = " + year + ", Month = " + month);

	try {
	  // Aggregate the month.
	  persistMonthAggregates(year, month);
	} catch (DbException dbe) {
	  log.error("Cannot aggregate the current month", dbe);
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
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private void markRequestsToAggregate() throws DbException {
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
      markRequests = dbManager.prepareStatement(conn, sql);

      // Mark the requests.
      int count = dbManager.executeUpdate(markRequests);
      log.debug2(DEBUG_HEADER + "count = " + count);

      success = true;
    } catch (DbException sqle) {
      log.error("Cannot mark requests to aggregate", sqle);
      log.error("SQL = '" + sql + "'.");
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
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private Map<Integer, List<Integer>> getYearMonthsWithRequests()
      throws DbException {
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
      statement = dbManager.prepareStatement(conn, sql);

      // Get the year/month pairs.
      resultSet = dbManager.executeQuery(statement);

      while (resultSet.next()) {
	year = resultSet.getInt(REQUEST_YEAR_COLUMN);
	log.debug2(DEBUG_HEADER + "year = " + year);
	month = resultSet.getInt(REQUEST_MONTH_COLUMN);
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
      throw new DbException("Cannot get the year/month pairs with requests",
	  sqle);
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
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private void persistMonthAggregates(int year, int month) throws DbException {
    final String DEBUG_HEADER = "persistMonthAggregates(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);

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
    log.debug2(DEBUG_HEADER + "Done.");
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
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private void persistMonthBookAggregates(int year, int month,
      boolean publisherInvolved) throws DbException {
    final String DEBUG_HEADER = "persistMonthBookAggregates(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    // Get the identifiers of the books with requests to aggregate.
    List<Long> publicationSeqs =
	getMonthBooksToAggregate(year, month, publisherInvolved);

    if (publicationSeqs.size() == 0) {
      log.debug2(DEBUG_HEADER + "No books with requests to aggregate.");
      return;
    }

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
      for (Long publicationSeq : publicationSeqs) {
	log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

	// Aggregate the month book requests.
	aggregate =
	    aggregateMonthBookTypes(year, month, publicationSeq,
	                            publisherInvolved, conn);

	fullCount = aggregate.get(FULL_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "fullCount = " + fullCount);

	sectionCount = aggregate.get(SECTION_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "sectionCount = " + sectionCount);

	// Retrieve the previous aggregate of book requests.
	previousAggregate = crManager.getExistingAggregateMonthBookTypes(year,
	    month, publicationSeq, publisherInvolved, conn);

	// Check whether there were previously recorded aggregates.
	if (previousAggregate != null) {
	  // Yes: Get the previously recorded aggregates.
	  previousFullCount =
	      previousAggregate.get(FULL_REQUESTS_COLUMN);
	  log.debug2(DEBUG_HEADER + "previousFullCount = " + previousFullCount);

	  previousSectionCount =
	      previousAggregate.get(SECTION_REQUESTS_COLUMN);
	  log.debug2(DEBUG_HEADER + "previousSectionCount = "
	      + previousSectionCount);

	  // Update the existing row in the database.
	  crManager.updateBookTypeAggregate(year, month, publicationSeq,
	      publisherInvolved, previousFullCount + fullCount,
	      previousSectionCount + sectionCount, conn);
	} else {
	  // No: Insert a new row in the database.
	  crManager.insertBookTypeAggregate(year, month, publicationSeq,
	      publisherInvolved, fullCount, sectionCount, conn);
	}

	// Accumulate the aggregations.
	allBooksFullCount += fullCount;
	log.debug2(DEBUG_HEADER + "allBooksFullCount = " + allBooksFullCount);
	allBooksSectionCount += sectionCount;
	log.debug2(DEBUG_HEADER + "allBooksSectionCount = "
	    + allBooksSectionCount);

	// Delete the request rows.
	deleteMonthBookRequests(year, month, publicationSeq, publisherInvolved,
	                        conn);

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

      // Persist the remaining aggregation accumulation for all the books.
      persistMonthAllBookAggregates(year, month, publisherInvolved,
				    allBooksFullCount, allBooksSectionCount,
				    conn);

      success = true;
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
   * @return a List<Long> with the identifiers of the books with requests to
   *         be aggregated during the month.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private List<Long> getMonthBooksToAggregate(int year, int month,
      boolean publisherInvolved) throws DbException {
    final String DEBUG_HEADER = "getMonthBooksToAggregate(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    DbManager dbManager = daemon.getDbManager();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Connection conn = null;
    List<Long> publicationSeqs = new ArrayList<Long>();
    Long publicationSeq = null;

    String sql = SQL_QUERY_MONTH_BOOK_REQUEST_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Prepare the statement used to get the books requested during the month.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the books requested during the month.
      resultSet = dbManager.executeQuery(statement);

      // Save them.
      while (resultSet.next()) {
	publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
	publicationSeqs.add(publicationSeq);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the books to aggregate", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot get the books to aggregate", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return publicationSeqs;
  }

  /**
   * Aggregates the type of requests for a book during a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param publicationSeq
   *          A Long with the identifier of the book involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @return a Map<String, Integer> with the aggregated request counts.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private Map<String, Integer> aggregateMonthBookTypes(int year, int month,
      Long publicationSeq, boolean publisherInvolved, Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "aggregateMonthBookTypes(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    Map<String, Integer> aggregates = new HashMap<String, Integer>();

    // Aggregate the full book requests.
    aggregates.put(FULL_REQUESTS_COLUMN,
		   aggregateMonthBookFull(year, month, publicationSeq,
		                          publisherInvolved, conn));

    // Aggregate the book section requests.
    aggregates.put(SECTION_REQUESTS_COLUMN,
		   aggregateMonthBookSection(year, month, publicationSeq,
		                             publisherInvolved, conn));

    log.debug2(DEBUG_HEADER + "Done.");
    return aggregates;
  }

  /**
   * Aggregates full requests for a book during a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param publicationSeq
   *          A Long with the identifier of the book involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @return an int with the aggregated request count.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private int aggregateMonthBookFull(int year, int month, Long publicationSeq,
      boolean publisherInvolved, Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "aggregateMonthBookFull(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;

    String sql = SQL_QUERY_MONTH_BOOK_FULL_REQUEST_COUNT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the count of requests.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the book identifier.
      statement.setLong(index++, publicationSeq);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	count = resultSet.getInt(1);
	log.debug2(DEBUG_HEADER + "count = " + count);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the month full book request count", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot get the month full book request count",
	  sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return count;
  }

  /**
   * Aggregates section requests for a book during a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param publicationSeq
   *          A Long with the identifier of the book involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @return an int with the aggregated request count.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private int aggregateMonthBookSection(int year, int month,
      Long publicationSeq, boolean publisherInvolved, Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "aggregateMonthBookSection(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int count = -1;

    String sql = SQL_QUERY_MONTH_BOOK_SECTION_REQUEST_COUNT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to get the count of requests.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the book identifier.
      statement.setLong(index++, publicationSeq);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	count = resultSet.getInt(1);
	log.debug2(DEBUG_HEADER + "count = " + count);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the month section book request count", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot get the month section book request count",
	  sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return count;
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
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private void persistMonthAllBookAggregates(int year, int month,
      boolean publisherInvolved, int fullCount, int sectionCount,
      Connection conn) throws DbException {
    final String DEBUG_HEADER = "persistMonthAllBookAggregates(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);
    log.debug2(DEBUG_HEADER + "fullCount = " + fullCount);
    log.debug2(DEBUG_HEADER + "sectionCount = " + sectionCount);

    CounterReportsManager manager = daemon.getCounterReportsManager();
    Long allBooksPublicationSeq = manager.getAllBooksPublicationSeq();
    log.debug2(DEBUG_HEADER + "allBooksPublicationSeq = "
	+ allBooksPublicationSeq);

    // Retrieve the previous aggregates of all book requests.
    Map<String, Integer> previousAggregate = crManager
	.getExistingAggregateMonthBookTypes(year, month,
	    allBooksPublicationSeq, publisherInvolved, conn);

    // Check whether there were previously recorded aggregates for all books.
    if (previousAggregate != null) {
      // Yes: Add the previously recorded aggregates for all books.
      int allBooksFullCount = fullCount
	  + previousAggregate.get(FULL_REQUESTS_COLUMN);
      log.debug2(DEBUG_HEADER + "allBooksFullCount = " + allBooksFullCount);

      int allBooksSectionCount = sectionCount
	  + previousAggregate.get(SECTION_REQUESTS_COLUMN);
      log.debug2(DEBUG_HEADER + "allBooksSectionCount = "
	  + allBooksSectionCount);

      // Update the existing row in the database.
      crManager.updateBookTypeAggregate(year, month, allBooksPublicationSeq,
	  publisherInvolved, allBooksFullCount, allBooksSectionCount, conn);
    } else {
      // No: Insert a new row in the database.
      crManager.insertBookTypeAggregate(year, month, allBooksPublicationSeq,
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
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private void persistMonthJournalAggregates(int year, int month,
      boolean publisherInvolved) throws DbException {
    final String DEBUG_HEADER = "persistMonthJournalAggregates(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    // Get the identifiers of the journals with requests to aggregate.
    List<Long> publicationSeqs =
	getMonthJournalsToAggregate(year, month, publisherInvolved);

    if (publicationSeqs.size() == 0) {
      log.debug2(DEBUG_HEADER + "No journals with requests to aggregate.");
      return;
    }

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
      for (Long publicationSeq : publicationSeqs) {

	// Aggregate the month journal requests by type.
	typeAggregate = aggregateMonthJournalTypes(year, month, publicationSeq,
	                                           publisherInvolved, conn);

	totalCount = typeAggregate.get(TOTAL_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "totalCount = " + totalCount);

	htmlCount = typeAggregate.get(HTML_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "htmlCount = " + htmlCount);

	pdfCount = typeAggregate.get(PDF_REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "pdfCount = " + pdfCount);

	// Retrieve the previous aggregate of journal requests.
	previousAggregate = crManager.getExistingAggregateMonthJournalTypes(
	    year, month, publicationSeq, publisherInvolved, conn);

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
	  crManager.updateJournalTypeAggregate(year, month, publicationSeq,
	      publisherInvolved, previousTotalCount + totalCount,
	      previousHtmlCount + htmlCount, previousPdfCount + pdfCount, conn);
	} else {
	  // No: Insert a new row in the database.
	  crManager.insertJournalTypeAggregate(year, month, publicationSeq,
	      publisherInvolved, totalCount, htmlCount, pdfCount, conn);
	}

	// Accumulate the aggregations.
	allJournalsTotalCount += totalCount;
	log.debug2(DEBUG_HEADER + "allJournalsTotalCount = "
	    + allJournalsTotalCount);
	allJournalsHtmlCount += htmlCount;
	log.debug2(DEBUG_HEADER + "allJournalsHtmlCount = "
	    + allJournalsHtmlCount);
	allJournalsPdfCount += pdfCount;
	log.debug2(DEBUG_HEADER + "allJournalsPdfCount = "
	    + allJournalsPdfCount);

	// Get the aggregates by publication year.
	pubYearAggregate =
	    aggregateMonthJournalPubYears(year, month, publicationSeq,
					  publisherInvolved, conn);

	// Loop through the aggregates by publication year.
	for (int publicationYear : pubYearAggregate.keySet()) {
	  log.debug2(DEBUG_HEADER + "publicationYear = " + publicationYear);

	  // Retrieve the previous aggregate for the publication year.
	  previousPubYearAggregate = crManager
	      .getExistingAggregateMonthJournalPubYear(year, month,
		  publicationSeq, publisherInvolved, publicationYear, conn);

	  // Check whether there was previously a recorded aggregate for the
	  // publication year.
	  if (previousPubYearAggregate != null) {
	    crManager.updateTitlePubYearAggregate(
		year,
		month,
		publicationSeq,
		publisherInvolved,
		publicationYear,
		previousPubYearAggregate.intValue()
		    + pubYearAggregate.get(publicationYear).intValue(), conn);
	  } else {
	    // No: Insert a new row in the database.
	    crManager.insertTitlePubYearAggregate(year, month, publicationSeq,
		publisherInvolved, publicationYear,
		pubYearAggregate.get(publicationYear), conn);
	  }
	}

	// Delete the request rows.
	deleteMonthJournalRequests(year, month, publicationSeq,
				   publisherInvolved, conn);

	// Check whether the maximum number of aggregated journals to be
	// persisted in a transaction has been reached.
	if (++aggregatedJournals >=
	    MAX_NUMBER_OF_TRANSACTION_AGGREGATED_TITLES) {
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
      log.debug2(DEBUG_HEADER + "allJournalsHtmlCount = "
	  + allJournalsHtmlCount);
      log.debug2(DEBUG_HEADER + "allJournalsPdfCount = " + allJournalsPdfCount);

      // Persist the all journals accumulations.
      persistMonthAllJournalAggregates(year, month, publisherInvolved,
				       allJournalsTotalCount,
				       allJournalsHtmlCount,
				       allJournalsPdfCount, conn);

      success = true;
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
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private List<Long> getMonthJournalsToAggregate(int year, int month,
      boolean publisherInvolved) throws DbException {
    final String DEBUG_HEADER = "getMonthJournalsToAggregate(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    DbManager dbManager = daemon.getDbManager();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Connection conn = null;
    List<Long> publicationSeqs = new ArrayList<Long>();
    Long publicationSeq = null;

    String sql = SQL_QUERY_MONTH_JOURNAL_REQUEST_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Prepare the statement used to get the journals.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the journals requested during the month.
      resultSet = dbManager.executeQuery(statement);

      // Save them.
      while (resultSet.next()) {
	publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
	publicationSeqs.add(publicationSeq);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the journals to aggregate", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot get the journals to aggregate", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return publicationSeqs;
  }

  /**
   * Aggregates the type requests for a journal during a month.
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
   * @param conn
   *          A Connection representing the database connection.
   * @return a Map<String, Integer> with the aggregated request counts.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private Map<String, Integer> aggregateMonthJournalTypes(int year, int month,
      Long publicationSeq, boolean publisherInvolved, Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "aggregateMonthJournalTypes(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    int totalCount = -1;
    int htmlCount = -1;
    int pdfCount = -1;

    Map<String, Integer> aggregates = new HashMap<String, Integer>();
    String sql = SQL_QUERY_MONTH_JOURNAL_TOTAL_REQUEST_COUNT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to aggregate the total requests.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the journal identifier.
      statement.setLong(index++, publicationSeq);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the total request count.
      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	totalCount = resultSet.getInt(1);
	log.debug2(DEBUG_HEADER + "totalCount = " + totalCount);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the month total journal request count", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot get the month total journal request count",
	  sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    sql = SQL_QUERY_MONTH_JOURNAL_HTML_REQUEST_COUNT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to aggregate the HTML requests.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the journal identifier.
      statement.setLong(index++, publicationSeq);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the HTML request count.
      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	htmlCount = resultSet.getInt(1);
	log.debug2(DEBUG_HEADER + "htmlCount = " + htmlCount);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the month HTML journal request count", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot get the month HTML journal request count",
	  sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    sql = SQL_QUERY_MONTH_JOURNAL_PDF_REQUEST_COUNT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to aggregate the PDF requests.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the journal identifier.
      statement.setLong(index++, publicationSeq);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the PDF request count.
      resultSet = dbManager.executeQuery(statement);

      if (resultSet.next()) {
	pdfCount = resultSet.getInt(1);
	log.debug2(DEBUG_HEADER + "pdfCount = " + pdfCount);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the month PDF journal request count", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot get the month PDF journal request count",
	  sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    aggregates.put(TOTAL_REQUESTS_COLUMN, totalCount);
    aggregates.put(HTML_REQUESTS_COLUMN, htmlCount);
    aggregates.put(PDF_REQUESTS_COLUMN, pdfCount);

    log.debug2(DEBUG_HEADER + "Done.");
    return aggregates;
  }

  /**
   * Aggregates the publication year requests for a journal during a month.
   * 
   * @param year
   *          An int with the year of the month to be aggregated.
   * @param month
   *          An int with the month to be aggregated.
   * @param publicationSeq
   *          A long with the identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @return a Map<Integer, Integer> with the aggregated request counts.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private Map<Integer, Integer> aggregateMonthJournalPubYears(int year,
      int month, Long publicationSeq, boolean publisherInvolved,
      Connection conn) throws DbException {
    final String DEBUG_HEADER = "aggregateMonthJournalPubYears(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String date = null;
    int publicationYear;
    int requests;

    Map<Integer, Integer> aggregates = new HashMap<Integer, Integer>();
    String sql = SQL_QUERY_MONTH_JOURNAL_PUBYEAR_REQUEST_SELECT;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to aggregate the requests by publication
      // year.
      statement = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the journal identifier.
      statement.setLong(index++, publicationSeq);

      // Populate the year of the request.
      statement.setInt(index++, year);

      // Populate the month of the request.
      statement.setInt(index++, month);

      // Populate the publisher involvement indicator.
      statement.setBoolean(index++, publisherInvolved);

      // Get the aggregations of requests by publication year.
      resultSet = dbManager.executeQuery(statement);

      while (resultSet.next()) {
	date = resultSet.getString(DATE_COLUMN);
	log.debug2(DEBUG_HEADER + "date = '" + date + "'.");
	publicationYear = Integer.parseInt(date.substring(0,4));
	log.debug2(DEBUG_HEADER + "publicationYear = " + publicationYear);
	requests = resultSet.getInt(REQUESTS_COLUMN);
	log.debug2(DEBUG_HEADER + "requests = " + requests);
	aggregates.put(publicationYear, requests);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the month publication year journal request count",
		sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException(
	  "Cannot get the month publication year journal request count", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    log.debug2(DEBUG_HEADER + "Done.");
    return aggregates;
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
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private void persistMonthAllJournalAggregates(int year, int month,
      boolean publisherInvolved, int totalCount, int htmlCount, int pdfCount,
      Connection conn) throws DbException {
    final String DEBUG_HEADER = "persistMonthAllJournalAggregates(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);
    log.debug2(DEBUG_HEADER + "totalCount = " + totalCount);
    log.debug2(DEBUG_HEADER + "htmlCount = " + htmlCount);
    log.debug2(DEBUG_HEADER + "pdfCount = " + pdfCount);

    CounterReportsManager manager = daemon.getCounterReportsManager();
    Long allJournalsPublicationSeq = manager.getAllJournalsPublicationSeq();
    log.debug2(DEBUG_HEADER + "allJournalsPublicationSeq = "
	+ allJournalsPublicationSeq);

    // Retrieve the previous aggregates of all journal requests.
    Map<String, Integer> previousAggregate = crManager
	.getExistingAggregateMonthJournalTypes(year, month,
	    allJournalsPublicationSeq, publisherInvolved, conn);

    // Check whether there were previously recorded aggregates for all journals.
    if (previousAggregate != null) {
      // Yes: Add the previously recorded aggregates for all journals.
      int allJournalsTotalCount =
	  totalCount + previousAggregate.get(TOTAL_REQUESTS_COLUMN);
      log.debug2(DEBUG_HEADER + "allJournalsTotalCount = "
	  + allJournalsTotalCount);

      int allJournalsHtmlCount =
	  htmlCount + previousAggregate.get(HTML_REQUESTS_COLUMN);
      log.debug2(DEBUG_HEADER + "allJournalsHtmlCount = "
	  + allJournalsHtmlCount);

      int allJournalsPdfCount =
	  pdfCount + previousAggregate.get(PDF_REQUESTS_COLUMN);
      log.debug2(DEBUG_HEADER + "allJournalsPdfCount = " + allJournalsPdfCount);

      // Update the existing row in the database.
      crManager.updateJournalTypeAggregate(year, month,
	  allJournalsPublicationSeq, publisherInvolved, allJournalsTotalCount,
	  allJournalsHtmlCount, allJournalsPdfCount, conn);
    } else {
      // No: Insert a new row in the database.
      crManager.insertJournalTypeAggregate(year, month,
	  allJournalsPublicationSeq, publisherInvolved, totalCount, htmlCount,
	  pdfCount, conn);
    }

    log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Deletes all the requests recorded during a month for a given book.
   * 
   * @param year
   *          An int with the year of the month of the requests to be deleted.
   * @param month
   *          An int with the month of the requests to be deleted.
   * @param publicationSeq
   *          A Long with the identifier of the book involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private void deleteMonthBookRequests(int year, int month, Long publicationSeq,
      boolean publisherInvolved, Connection conn) throws DbException {
    final String DEBUG_HEADER = "deleteMonthBookRequests(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    PreparedStatement deleteAggregate = null;
    String sql = SQL_QUERY_BOOK_REQUEST_DELETE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to delete the rows.
      deleteAggregate = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the book identifier.
      deleteAggregate.setLong(index++, publicationSeq);

      // Populate the publisher involvement indicator.
      deleteAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      deleteAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      deleteAggregate.setShort(index++, (short) month);

      // Delete the record.
      int count = dbManager.executeUpdate(deleteAggregate);
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot delete the book month requests", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot delete the book month requests", sqle);
    } finally {
      DbManager.safeCloseStatement(deleteAggregate);
    }
  }

  /**
   * Deletes all the requests recorded during a month for a given journal.
   * 
   * @param year
   *          An int with the year of the month of the requests to be deleted.
   * @param month
   *          An int with the month of the requests to be deleted.
   * @param publicationSeq
   *          A Long with the identifier of the journal involved.
   * @param publisherInvolved
   *          A boolean with the indication of the publisher involvement in
   *          serving the request.
   * @param conn
   *          A Connection representing the database connection.
   * @throws DbException
   *           if there are problems accessing the database.
   */
  private void deleteMonthJournalRequests(int year, int month,
      Long publicationSeq, boolean publisherInvolved, Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "deleteMonthJournalRequests(): ";
    log.debug2(DEBUG_HEADER + "year = " + year);
    log.debug2(DEBUG_HEADER + "month = " + month);
    log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    log.debug2(DEBUG_HEADER + "publisherInvolved = " + publisherInvolved);

    PreparedStatement deleteAggregate = null;
    String sql = SQL_QUERY_JOURNAL_REQUEST_DELETE;
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Prepare the statement used to delete the rows.
      deleteAggregate = dbManager.prepareStatement(conn, sql);

      int index = 1;

      // Populate the journal identifier.
      deleteAggregate.setLong(index++, publicationSeq);

      // Populate the publisher involvement indicator.
      deleteAggregate.setBoolean(index++, publisherInvolved);

      // Populate the year of the request.
      deleteAggregate.setShort(index++, (short) year);

      // Populate the month of the request.
      deleteAggregate.setShort(index++, (short) month);

      // Delete the record.
      int count = dbManager.executeUpdate(deleteAggregate);
      log.debug2(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot delete the journal month requests", sqle);
      log.error("Year = " + year + ", Month = " + month);
      log.error("publicationSeq = " + publicationSeq);
      log.error("publisherInvolved = " + publisherInvolved);
      log.error("SQL = '" + sql + "'.");
      throw new DbException("Cannot delete the journal month requests", sqle);
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
      return
	  nextTimeA(lastTime,
	            CurrentConfig
	            .getParam(PARAM_COUNTER_REQUEST_AGGREGATION_TASK_FREQUENCY,
	                      DEFAULT_COUNTER_REQUEST_AGGREGATION_TASK_FREQUENCY));
    }
  }
}