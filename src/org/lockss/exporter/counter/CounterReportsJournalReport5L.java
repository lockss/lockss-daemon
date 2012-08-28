/*
 * $Id: CounterReportsJournalReport5L.java,v 1.1 2012-08-28 17:36:49 fergaloy-sf Exp $
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
 * The COUNTER Journal Report 5L.
 * 
 * @version 1.0
 * 
 */
package org.lockss.exporter.counter;

import static org.lockss.exporter.counter.CounterReportsManager.*;
import org.lockss.app.LockssDaemon;

public class CounterReportsJournalReport5L extends CounterReportsJournalReport5 {
  // Query to get the journals to be included in the report.
  // This the same query used for Journal Report 5, except for the fact that in
  // this one the publisher involvement is not considered as a filtering
  // criteria.
  private static final String SQL_QUERY_REPORT_JOURNALS_5L_SELECT = "select "
      + "distinct a." + SQL_COLUMN_LOCKSS_ID
      + ", t." + SQL_COLUMN_TITLE_NAME
      + ", t." + SQL_COLUMN_PUBLISHER_NAME
      + ", t." + SQL_COLUMN_PLATFORM_NAME
      + ", t." + SQL_COLUMN_DOI
      + ", t." + SQL_COLUMN_PROPRIETARY_ID
      + ", t." + SQL_COLUMN_PRINT_ISSN
      + ", t." + SQL_COLUMN_ONLINE_ISSN
      + " from " + SQL_TABLE_PUBYEAR_AGGREGATES + " a,"
      + SQL_TABLE_TITLES + " t "
      + "where (t." + SQL_COLUMN_TITLE_NAME + " != '" + ALL_JOURNALS_TITLE_NAME
      + "' or t." + SQL_COLUMN_PUBLISHER_NAME + " != '" + ALL_PUBLISHERS_NAME
      + "') "
      + "and t." + SQL_COLUMN_IS_BOOK + " = false "
      + "and ((a." + SQL_COLUMN_REQUEST_MONTH + " >= ? "
      + "and a." + SQL_COLUMN_REQUEST_YEAR + " = ?) "
      + "or a." + SQL_COLUMN_REQUEST_YEAR + " > ?) "
      + "and ((a." + SQL_COLUMN_REQUEST_MONTH + " <= ? "
      + "and a." + SQL_COLUMN_REQUEST_YEAR + " = ?) "
      + "or a." + SQL_COLUMN_REQUEST_YEAR + " < ?) "
      + "and a." + SQL_COLUMN_LOCKSS_ID + " = t." + SQL_COLUMN_LOCKSS_ID
      + " order by t." + SQL_COLUMN_TITLE_NAME + " asc";

  // Query to get the journal request counts to be included in the report.
  // This the same query used for Journal Report 5, except for the fact that in
  // this one the publisher involvement is not considered as a filtering
  // criteria.
  private static final String SQL_QUERY_REPORT_REQUESTS_5L_SELECT = "select "
      + "t." + SQL_COLUMN_TITLE_NAME
      + ", a." + SQL_COLUMN_LOCKSS_ID
      + ", a." + SQL_COLUMN_PUBLICATION_YEAR
      + ", sum(a." + SQL_COLUMN_REQUEST_COUNT + ") as " + SQL_RESULT_YEAR_COUNT
      + " from " + SQL_TABLE_PUBYEAR_AGGREGATES + " a,"
      + SQL_TABLE_TITLES + " t "
      + "where (t." + SQL_COLUMN_TITLE_NAME + " != '" + ALL_JOURNALS_TITLE_NAME
      + "' or t." + SQL_COLUMN_PUBLISHER_NAME + " != '" + ALL_PUBLISHERS_NAME
      + "') "
      + "and t." + SQL_COLUMN_IS_BOOK + " = false "
      + "and ((a." + SQL_COLUMN_REQUEST_MONTH + " >= ? "
      + "and a." + SQL_COLUMN_REQUEST_YEAR + " = ?) "
      + "or a." + SQL_COLUMN_REQUEST_YEAR + " > ?) "
      + "and ((a." + SQL_COLUMN_REQUEST_MONTH + " <= ? "
      + "and a." + SQL_COLUMN_REQUEST_YEAR + " = ?) "
      + "or a." + SQL_COLUMN_REQUEST_YEAR + " < ?) "
      + "and a." + SQL_COLUMN_LOCKSS_ID + " = t." + SQL_COLUMN_LOCKSS_ID
      + " group by t." + SQL_COLUMN_TITLE_NAME
      + ", a." + SQL_COLUMN_LOCKSS_ID
      + ", a." + SQL_COLUMN_PUBLICATION_YEAR
      + " order by t." + SQL_COLUMN_TITLE_NAME + " asc, "
      + "a." + SQL_COLUMN_PUBLICATION_YEAR + " desc";

  /**
   * Constructor for the default report period.
   * 
   * @param daemon
   *          A LockssDaemon with the LOCKSS daemon.
   */
  public CounterReportsJournalReport5L(LockssDaemon daemon) {
    super(daemon);
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
  public CounterReportsJournalReport5L(LockssDaemon daemon, int startMonth,
      int startYear, int endMonth, int endYear) throws IllegalArgumentException {
    super(daemon, startMonth, startYear, endMonth, endYear);
  }

  /**
   * Provides the SQL query used to find all the journals to be included in the
   * report.
   * 
   * @return a String with the SQL query used to find all the journals to be
   *         included in the report.
   */
  protected String getReportJournalsSqlQuery() {
    return SQL_QUERY_REPORT_JOURNALS_5L_SELECT;
  }

  /**
   * Provides the SQL query used to find all the requests to be included in the
   * report.
   * 
   * @return a String with the SQL query used to find all the requests to be
   *         included in the report.
   */
  protected String getReportRequestsSqlQuery() {
    return SQL_QUERY_REPORT_REQUESTS_5L_SELECT;
  }

  /**
   * Provides the name of the report to be used in the report file name.
   * 
   * @return a String with the name of the report to be used in the report file
   *         name.
   */
  protected String getFileReportName() {
    return "COUNTER_Journal_5L";
  }

  /**
   * Provides the header items in the report.
   * 
   * @return a String[] with the report header items.
   */
  @Override
  protected void populateReportHeaderEntries() {
    header.reportName = "Journal Report 5L (R4)";
    header.reportDescription =
	"Number of Successful Full-Text Article Requests by Year-of-Publication (YOP) and Journal";
    header.periodTitle = "Period covered by Report:";
    header.runDateTitle = "Date run:";
  }
}
