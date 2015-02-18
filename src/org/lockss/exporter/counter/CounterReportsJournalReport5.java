/*
 * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
import static org.lockss.exporter.counter.CounterReportsManager.*;
import static org.lockss.metadata.MetadataManager.*;

import java.sql.*;
import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.lockss.app.LockssDaemon;
import org.lockss.db.*;
import org.lockss.util.*;

/**
 * The COUNTER Journal Report 5.
 */
public class CounterReportsJournalReport5 extends CounterReportsJournalReport {
  
  private static final Logger log = Logger.getLogger(CounterReportsJournalReport5.class);

  // Query to get the journals to be included in the report.
  private static final String SQL_QUERY_REPORT_JOURNALS_SELECT = "select "
      + "distinct a." + PUBLICATION_SEQ_COLUMN
      + ", n." + NAME_COLUMN
      + ", pi." + PROPRIETARY_ID_COLUMN
      + ", pu." + PUBLISHER_NAME_COLUMN
      + ", pla." + PLATFORM_NAME_COLUMN
      + ", d." + DOI_COLUMN
      + ", i1." + ISSN_COLUMN + " as " + P_ISSN_TYPE
      + ", i2." + ISSN_COLUMN + " as " + E_ISSN_TYPE
      + " from " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE + " a"
      + "," + PUBLICATION_TABLE + " p"
      + "," + PUBLISHER_TABLE + " pu"
      + "," + MD_ITEM_TABLE + " m2"
      + "," + AU_MD_TABLE + " am"
      + "," + AU_TABLE + " au"
      + "," + PLUGIN_TABLE + " pl"
      + "," + PLATFORM_TABLE + " pla"
      + "," + MD_ITEM_TABLE + " m1"
      + " left outer join " + ISSN_TABLE + " i1"
      + " on m1." + MD_ITEM_SEQ_COLUMN + " = i1." + MD_ITEM_SEQ_COLUMN
      + " and i1." + ISSN_TYPE_COLUMN + " = '" + P_ISSN_TYPE + "'"
      + " left outer join " + ISSN_TABLE + " i2"
      + " on m1." + MD_ITEM_SEQ_COLUMN + " = i2." + MD_ITEM_SEQ_COLUMN
      + " and i2." + ISSN_TYPE_COLUMN + " = '" + E_ISSN_TYPE + "'"
      + " left outer join " + DOI_TABLE + " d"
      + " on m1." + MD_ITEM_SEQ_COLUMN + " = d." + MD_ITEM_SEQ_COLUMN
      + " left outer join " + MD_ITEM_NAME_TABLE + " n"
      + " on m1." + MD_ITEM_SEQ_COLUMN + " = n." + MD_ITEM_SEQ_COLUMN
      + " left outer join " + PROPRIETARY_ID_TABLE + " pi"
      + " on m1." + MD_ITEM_SEQ_COLUMN + " = pi." + MD_ITEM_SEQ_COLUMN
      + " where"
      + " a." + IS_PUBLISHER_INVOLVED_COLUMN + " = false"
      + " and ((a." + REQUEST_MONTH_COLUMN + " >= ?"
      + " and a." + REQUEST_YEAR_COLUMN + " = ?)"
      + " or a." + REQUEST_YEAR_COLUMN + " > ?)"
      + " and ((a." + REQUEST_MONTH_COLUMN + " <= ?"
      + " and a." + REQUEST_YEAR_COLUMN + " = ?)"
      + " or a." + REQUEST_YEAR_COLUMN + " < ?)"
      + " and a." + PUBLICATION_SEQ_COLUMN + " = p." + PUBLICATION_SEQ_COLUMN
      + " and p." + PUBLISHER_SEQ_COLUMN + " = pu." + PUBLISHER_SEQ_COLUMN
      + " and pu." + PUBLISHER_NAME_COLUMN + " != '" + ALL_PUBLISHERS_NAME + "'"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m1." + MD_ITEM_SEQ_COLUMN
      + " and n." + NAME_COLUMN + " != '" + ALL_JOURNALS_NAME + "'"
      + " and n." + NAME_TYPE_COLUMN + " = 'primary'"
      + " and m1." + MD_ITEM_SEQ_COLUMN + " = m2." + PARENT_SEQ_COLUMN
      + " and m2." + AU_MD_SEQ_COLUMN + " = am." + AU_MD_SEQ_COLUMN
      + " and am." + AU_SEQ_COLUMN + " = au." + AU_SEQ_COLUMN
      + " and au." + PLUGIN_SEQ_COLUMN + " = pl." + PLUGIN_SEQ_COLUMN
      + " and pl." + PLATFORM_SEQ_COLUMN + " = pla." + PLATFORM_SEQ_COLUMN
      + " order by n." + NAME_COLUMN + " asc"
      + ", a." + PUBLICATION_SEQ_COLUMN + " asc"
      + ", pi." + PROPRIETARY_ID_COLUMN + " asc";

  // Query to get the journal request counts to be included in the report.
  private static final String SQL_QUERY_REPORT_REQUESTS_SELECT = "select "
      + "a." + PUBLICATION_SEQ_COLUMN
      + ", n." + NAME_COLUMN
      + ", a." + PUBLICATION_YEAR_COLUMN
      + ", sum(a." + REQUESTS_COLUMN + ") as " + REQUESTS_COLUMN
      + " from " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE + " a"
      + "," + PUBLICATION_TABLE + " p"
      + "," + PUBLISHER_TABLE + " pu"
      + "," + MD_ITEM_TABLE + " m1"
      + " left outer join " + MD_ITEM_NAME_TABLE + " n"
      + " on m1." + MD_ITEM_SEQ_COLUMN + " = n." + MD_ITEM_SEQ_COLUMN
      + " where"
      + " a." + IS_PUBLISHER_INVOLVED_COLUMN + " = false"
      + " and ((a." + REQUEST_MONTH_COLUMN + " >= ?"
      + " and a." + REQUEST_YEAR_COLUMN + " = ?)"
      + " or a." + REQUEST_YEAR_COLUMN + " > ?)"
      + " and ((a." + REQUEST_MONTH_COLUMN + " <= ?"
      + " and a." + REQUEST_YEAR_COLUMN + " = ?)"
      + " or a." + REQUEST_YEAR_COLUMN + " < ?)"
      + " and a." + PUBLICATION_SEQ_COLUMN + " = p." + PUBLICATION_SEQ_COLUMN
      + " and p." + PUBLISHER_SEQ_COLUMN + " = pu." + PUBLISHER_SEQ_COLUMN
      + " and pu." + PUBLISHER_NAME_COLUMN + " != '" + ALL_PUBLISHERS_NAME + "'"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m1." + MD_ITEM_SEQ_COLUMN
      + " and n." + NAME_COLUMN + " != '" + ALL_JOURNALS_NAME + "'"
      + " and n." + NAME_TYPE_COLUMN + " = 'primary'"
      + " group by n." + NAME_COLUMN
      + ", a." + PUBLICATION_SEQ_COLUMN
      + ", a." + PUBLICATION_YEAR_COLUMN
      + " order by n." + NAME_COLUMN + " asc"
      + ", a." + PUBLICATION_SEQ_COLUMN + " asc"
      + ", a." + PUBLICATION_YEAR_COLUMN + " desc";

  // The count of months included in the report.
  private int monthCount = 0;

  // The current year.
  private int currentYear = 0;

  // The publication year groups to be included in the report.
  private int[] pubYears = null;

  /**
   * Constructor for the default report period.
   * 
   * @param daemon
   *          A LockssDaemon with the LOCKSS daemon.
   */
  public CounterReportsJournalReport5(LockssDaemon daemon) {
    super(daemon);

    // Count the months included in the report.
    monthCount = getMonthIndex(endMonth, endYear);

    if (monthCount > CounterReportsRequestAggregator
	.MAX_NUMBER_OF_AGGREGATE_MONTHS) {
      throw new IllegalArgumentException("The report period cannot exceed "
	  + CounterReportsRequestAggregator.MAX_NUMBER_OF_AGGREGATE_MONTHS
	  + " months.");
    }

    populateCurrentYear();
    populatePublicationYears();
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
  public CounterReportsJournalReport5(LockssDaemon daemon, int startMonth,
      int startYear, int endMonth, int endYear)
	  throws IllegalArgumentException {
    super(daemon, startMonth, startYear, endMonth, endYear);

    // Count the months included in the report.
    monthCount = getMonthIndex(endMonth, endYear);

    if (monthCount > CounterReportsRequestAggregator
	.MAX_NUMBER_OF_AGGREGATE_MONTHS) {
      throw new IllegalArgumentException("The report period cannot exceed "
	  + CounterReportsRequestAggregator.MAX_NUMBER_OF_AGGREGATE_MONTHS
	  + " months.");
    }

    populateCurrentYear();
    populatePublicationYears();
  }

  /**
   * Populates the current calendar year.
   */
  private void populateCurrentYear() {
    final String DEBUG_HEADER = "populateCurrentYear(): ";
    // Get the current year.
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(TimeBase.nowDate());
    int year = calendar.get(Calendar.YEAR);
    currentYear = year;
    log.debug2(DEBUG_HEADER + "currentYear = " + currentYear + ".");
  }

  /**
   * Populates the report publication year groupings.
   */
  private void populatePublicationYears() {
    final String DEBUG_HEADER = "populatePublicationYears(): ";
    int year = currentYear;

    // The publication year groups.
    List<Integer> yops = new ArrayList<Integer>();

    // The first group is for the articles in press (YOP in the future).
    yops.add(new Integer(0));

    // Loop through all the years after the first one in this decade.
    while (year % 10 != 0) {
      yops.add(new Integer(year--));
    }

    // Loop through the first year in this decade and all the years in the
    // previous decade.
    for (int i = 0; i < 11; i++) {
      yops.add(new Integer(year--));
    }

    // The group for the prior years.
    yops.add(new Integer(0));

    // The group for the unknown publication years.
    yops.add(new Integer(0));

    pubYears = ArrayUtils.toPrimitive(yops.toArray(new Integer[yops.size()]));
    
    if (log.isDebug2()) {
      for (int pubYear : pubYears) {
	log.debug2(DEBUG_HEADER + "pubYear = " + pubYear + ".");
      }
    }
  }

  /**
   * Initializes the rows to be included in the report with the title data.
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
   * @throws DbException
   */
  protected void initializeReportRows(Connection conn) throws DbException {
    final String DEBUG_HEADER = "initializeReportRows(): ";
    log.debug2(DEBUG_HEADER + "Starting...");
    Long titleId = 0L;
    String proprietaryId = null;
    Collection<String> proprietaryIds = null;

    // The first row is a placeholder for the totals for all journals.
    CounterReportsJournal journal =
	new CounterReportsJournal(TOTAL_LABEL, null, null, null, null, null,
	                          null);
    List<Row> rows = new ArrayList<Row>();
    rows.add(new Row(titleId, journal));

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String sql = getReportJournalsSqlQuery();
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Get the journals to be included in the report.
      statement = daemon.getDbManager().prepareStatement(conn, sql);

      short index = 1;

      statement.setInt(index++, startMonth);
      statement.setInt(index++, startYear);
      statement.setInt(index++, startYear);
      statement.setInt(index++, endMonth);
      statement.setInt(index++, endYear);
      statement.setInt(index++, endYear);

      resultSet = daemon.getDbManager().executeQuery(statement);

      // Loop through all the journals to be included in the report.
      while (resultSet.next()) {
	// Check whether this journal is the same as the previous one.
	if (resultSet.getLong(PUBLICATION_SEQ_COLUMN) == titleId) {
	  // Yes: This means that the publication has multiple values for some
	  // attributes. Get the proprietary identifier.
	  proprietaryId = resultSet.getString(PROPRIETARY_ID_COLUMN);
	  log.debug3(DEBUG_HEADER + "proprietaryId = '" + proprietaryId + "'.");

	  // Add it to the list of proprietary identifiers, if it exists.
	  if (!StringUtil.isNullString(proprietaryId)) {
	    proprietaryIds.add(proprietaryId);
	    log.debug3(DEBUG_HEADER + "Added proprietaryId = '" + proprietaryId
		+ "'.");
	  }

	  continue;
	}

	// Get the identifier for the journal.
	titleId = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	log.debug2(DEBUG_HEADER + "titleId = " + titleId + ".");

	// Initialize the collection of proprietary identifiers.
	proprietaryIds = new LinkedHashSet<String>();

	// Get the proprietary identifier.
	proprietaryId = resultSet.getString(PROPRIETARY_ID_COLUMN);
	log.debug3(DEBUG_HEADER + "proprietaryId = " + proprietaryId + ".");

	// Add it to the list of proprietary identifiers, if it exists.
	if (!StringUtil.isNullString(proprietaryId)) {
	  proprietaryIds.add(proprietaryId);
	  log.debug3(DEBUG_HEADER + "Added proprietaryId = '" + proprietaryId
	      + "'.");
	}

	// Get the journal properties.
	journal =
	    new CounterReportsJournal(resultSet.getString(NAME_COLUMN),
	                              resultSet
	                              	.getString(PUBLISHER_NAME_COLUMN),
	                              resultSet.getString(PLATFORM_NAME_COLUMN),
	                              resultSet.getString(DOI_COLUMN),
	                              proprietaryIds,
	                              formatIssn(resultSet
	                                         .getString(P_ISSN_TYPE)),
	                              formatIssn(resultSet
	                                         .getString(E_ISSN_TYPE)));
	log.debug2(DEBUG_HEADER + "Journal = [" + journal + "].");

	// Add the row to the results.
	rows.add(new Row(titleId, journal));
      }
    } catch (SQLException sqle) {
      log.error("Cannot retrieve the journals to be included in a report",
                sqle);
      log.error("StartMonth = " + startMonth + ", StartYear = " + startYear
	  + ", EndMonth = " + endMonth + ", EndYear = " + endYear);
      log.error("SQL = '" + sql + "'.");
      throw new DbException(
	  "Cannot retrieve the journals to be included in a report", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    // Remember the data rows in the report.
    log.debug2(DEBUG_HEADER + "rows.size() = " + rows.size() + ".");
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
   * @param conn
   *          A Connection with a connection to the database.
   * @throws DbException
   * @throws CounterReportsException
   */
  protected void addReportRequestCounts(Connection conn) throws DbException,
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

    // Initialize its request counts.
    List<ItemCounts> allJournalsRowYopRequestCounts =
	initializeRowRequestCounts(allJournalsRow);

    // Do nothing more if there are no requests for this report.
    if (!rowIterator.hasNext()) {
      return;
    }

    String[] itemKeys = getItemColumnKeys();
    String[] totalKeys = getTotalColumnKeys();

    // The current row to be populated with request counts.
    Row currentRow = rowIterator.next();

    // Initialize its request counts.
    List<ItemCounts> currentRowYopRequestCounts =
	initializeRowRequestCounts(currentRow);

    int year;
    Integer yearIndex;
    Long titleId;
    ItemCounts counts = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    CounterReportsException exception = null;
    String sql = getReportRequestsSqlQuery();
    log.debug2(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      // Get the request counts for all the rows to be included in the report.
      statement = daemon.getDbManager().prepareStatement(conn, sql);

      short index = 1;

      statement.setInt(index++, startMonth);
      statement.setInt(index++, startYear);
      statement.setInt(index++, startYear);
      statement.setInt(index++, endMonth);
      statement.setInt(index++, endYear);
      statement.setInt(index++, endYear);

      resultSet = daemon.getDbManager().executeQuery(statement);

      // Loop through all the request counts to be included in the report.
      while (resultSet.next()) {
	// Get the identifier for this set of request counts.
	titleId = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	log.debug2(DEBUG_HEADER + "titleId = " + titleId + ".");

	// Check whether this item is for a row different than the current one.
	if (currentRow.getTitleId() != titleId) {
	  // Yes: This means that all the items for the current row have been
	  // processed. Verify that there are more rows in the report.
	  if (!rowIterator.hasNext()) {
	    exception = new CounterReportsException(BaseCounterReport
		.ERROR_UNEXPECTED_IDENTIFIER);
	    break;
	  }

	  // Make the next row the current one.
	  currentRow = rowIterator.next();

	  // Check whether this row is not in sync with this item.
	  if (currentRow.getTitleId() != titleId) {
	    exception = new CounterReportsException(BaseCounterReport
		.ERROR_WRONG_SORTING);
	    break;
	  }
	    
	  // Initialize the period request counts of the current row.
	  currentRowYopRequestCounts = initializeRowRequestCounts(currentRow);
	}

	// Retrieve and save the publication year request counts for this row.
	counts = new ItemCounts();

	for (int i = 0; i < itemKeys.length; i++) {
	  counts.put(itemKeys[i], resultSet.getInt(itemKeys[i]));
	  log.debug2(DEBUG_HEADER + "i = " + i + ", itemKeys[i] = "
	      + itemKeys[i] + ", resultSet.getInt(itemKeys[i]) = "
	      + resultSet.getInt(itemKeys[i]));
	}
	for (int i = 0; i < totalKeys.length; i++) {
	  counts.put(totalKeys[i], resultSet.getInt(totalKeys[i]));
	  log.debug2(DEBUG_HEADER + "i = " + i + ", totalKeys[i] = "
	      + totalKeys[i] + ", resultSet.getInt(totalKeys[i]) = "
	      + resultSet.getInt(totalKeys[i]));
	}

	// Get the publication year for this set of request counts.
	year = resultSet.getShort(PUBLICATION_YEAR_COLUMN);
	log.debug2(DEBUG_HEADER + "year = " + year);

	// Get the index of the publication year within the publication years
	// covered by the report.
	yearIndex = getPubYearIndex(year);
	log.debug2(DEBUG_HEADER + "yearIndex = " + yearIndex);

	// Populate the request counts for the appropriate journal publication
	// year.
	currentRowYopRequestCounts.set(yearIndex, counts);

	// Update the request counts for all the journals publication year.
	accumulateRequestCounts(itemKeys, counts,
	    allJournalsRowYopRequestCounts.get(yearIndex));
      }

      if (exception != null) {
	throw exception;
      }
    } catch (SQLException sqle) {
      log.error(
	  "Cannot retrieve the journal requests to be included in a report",
	  sqle);
      log.error("StartMonth = " + startMonth + ", StartYear = " + startYear
	  + ", EndMonth = " + endMonth + ", EndYear = " + endYear);
      log.error("SQL = '" + sql + "'.");
      throw new DbException(
	  "Cannot retrieve the journal requests to be included in a report",
	  sqle);
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
    return pubYears.length;
  }

  /**
   * Provides an indication of whether the report includes a total column.
   * 
   * @return a boolean with the indication of whether the report includes a
   *         total column.
   */
  protected boolean hasTotalColumn() {
    return false;
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
   * Provides the index of a publication year within the publication years
   * covered by the report.
   * 
   * @param year
   *          An int with the year being indexed.
   * @return an int with the index of the publication year within the
   *         publication years covered by the report.
   */
  private int getPubYearIndex(Integer yop) {
    final String DEBUG_HEADER = "getPubYearIndex(): ";
    log.debug2(DEBUG_HEADER + "yop = " + yop);

    // The last index is for unknown publication years.
    if (yop == null) {
      log.debug2(DEBUG_HEADER + "Unknown YOP.");
      return pubYears.length - 1;
    }

    // The last index is for unknown publication years.
    int year = yop.intValue();
    if (yop == 0) {
      log.debug2(DEBUG_HEADER + "Unknown YOP.");
      return pubYears.length - 1;
    }

    // The first index (0) is for future years.
    if (year > currentYear) {
      log.debug2(DEBUG_HEADER + "Future YOP.");
      return 0;
    }

    // The next to last index is for years prior to the previous decade.
    if (year < pubYears[pubYears.length - 3]) {
      log.debug2(DEBUG_HEADER + "Previous to the previous decade.");
      return pubYears.length - 2;
    }

    return currentYear - year + 1;
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
	    "Articles in Press" };

    StringBuilder sb =
	new StringBuilder(StringUtil.separatedString(tableHeader, separator));

    // Place the publication year headers.
    for (int i = 1; i < pubYears.length - 2; i++) {
      sb.append(separator).append("YOP ").append(pubYears[i]);
    }

    sb.append(separator).append("YOP Pre-")
	.append(pubYears[pubYears.length - 3]);
    sb.append(separator).append("YOP unknown");

    return sb.toString();
  }

  /**
   * Provides the name of the report to be used in the report file name.
   * 
   * @return a String with the name of the report to be used in the report file
   *         name.
   */
  protected String getFileReportName() {
    return "COUNTER_Journal_5";
  }

  /**
   * Provides the header items in the report.
   */
  @Override
  protected void populateReportHeaderEntries() {
    header.reportName = "Journal Report 5 (R4)";
    header.reportDescription =
	"Number of Successful Full-Text Article Requests by Year-of-Publication (YOP) and Journal";
    header.periodTitle = "Period covered by Report:";
    header.runDateTitle = "Date run:";
  }

  /**
   * Provides the keys used to populate the total columns.
   * 
   * @return a String[] with the keys used to populate the total columns.
   */
  protected String[] getTotalColumnKeys() {
    return new String[] {};
  }

  /**
   * Provides the keys used to populate the item columns.
   * 
   * @return a String[] with the keys used to populate the item columns.
   */
  protected String[] getItemColumnKeys() {
    return new String[] { REQUESTS_COLUMN };
  }
}
