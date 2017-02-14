/*
 * $Id$
 */

/*

 Copyright (c) 2012-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.lockss.app.LockssDaemon;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * A generic journal COUNTER report.
 * 
 * @version 1.0
 * 
 */
public abstract class CounterReportsJournalReport extends BaseCounterReport {
  private static final Logger log = Logger
      .getLogger("CounterReportsJournalReport");

  protected static final String TOTAL_LABEL = "Total for all journals";

  /**
   * Constructor for the default report period.
   * 
   * @param daemon
   *          A LockssDaemon with the LOCKSS daemon.
   */
  protected CounterReportsJournalReport(LockssDaemon daemon) {
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
  protected CounterReportsJournalReport(LockssDaemon daemon, int startMonth,
      int startYear, int endMonth, int endYear)
      throws IllegalArgumentException {
    super(daemon, startMonth, startYear, endMonth, endYear);
  }

  /**
   * Provides the text lines that comprise the report data.
   * 
   * @param separator
   *          A String with the separator to be used between items in report
   *          lines.
   * @return a List<String> with the text lines that comprise the report data.
   */
  protected List<String> getTableDataTextLines(String separator) {
    final String DEBUG_HEADER = "getTableDataTextLines(): ";
    List<String> reportLines = new ArrayList<String>(getRows().size());
    CounterReportsJournal journal = null;
    Iterator<ItemCounts> countsIterator = null;
    ItemCounts aggregateCounts = null;
    StringBuilder sb = null;

    // Loop through all the journals included in the report.
    for (Row row : getRows()) {
      // Output the journal information, sanitizing NULLs and escaping
      // separators when appropriate.
      sb = new StringBuilder();
      journal = (CounterReportsJournal) row.getTitle();

      if (COMMA.equals(separator)) {
	sb.append(StringUtil.csvEncode(journal.getName()))
	    .append(separator)
	    .append(StringUtil.csvEncode(blankNull(journal.getPublisherName())))
	    .append(separator)
	    .append(
		StringUtil.csvEncode(blankNull(journal.getPublishingPlatform())))
	    .append(separator)
	    .append(StringUtil.csvEncode(blankNull(journal.getDoi())))
	    .append(separator)
	    .append(StringUtil.csvEncode(StringUtil.separatedString(journal.
		getProprietaryIds()))).append(separator)
	    .append(StringUtil.csvEncode(blankNull(journal.getPrintIssn())))
	    .append(separator)
	    .append(StringUtil.csvEncode(blankNull(journal.getOnlineIssn())));
      } else {
	sb.append(journal.getName()).append(separator)
	    .append(blankNull(journal.getPublisherName())).append(separator)
	    .append(blankNull(journal.getPublishingPlatform()))
	    .append(separator).append(blankNull(journal.getDoi()))
	    .append(separator)
	    .append(StringUtil.separatedString(journal.getProprietaryIds()))
	    .append(separator).append(blankNull(journal.getPrintIssn()))
	    .append(separator).append(blankNull(journal.getOnlineIssn()));
      }

      // Output the aggregate request counts during the period.
      countsIterator = row.getRequestCounts().iterator();

      // Check whether the report includes total columns.
      if (hasTotalColumn()) {
	// Yes: Get the request totals.
	aggregateCounts = countsIterator.next();

	// Output the request totals.
	String[] totalKeys = getTotalColumnKeys();
	for (int i = 0; i < totalKeys.length; i++) {
	  sb.append(separator).append(aggregateCounts.get(totalKeys[i]));
	}
      }

      // Loop through each request count in the report.
      String[] columnKeys = getItemColumnKeys();
      while (countsIterator.hasNext()) {
	// Output the request count.
	for (int i = 0; i < columnKeys.length; i++) {
	  sb.append(separator).append(countsIterator.next().get(columnKeys[i]));
	}
      }

      String rowText = sb.toString();
      log.debug2(DEBUG_HEADER + "rowText = '" + rowText + "'.");

      // Add to the result the row for this journal.
      reportLines.add(rowText);
    }

    return reportLines;
  }
}
