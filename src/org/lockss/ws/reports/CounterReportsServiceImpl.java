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
 * The COUNTER Reports web service implementation.
 */
package org.lockss.ws.reports;

import java.io.File;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jws.WebService;
import javax.xml.ws.soap.MTOM;
import org.lockss.app.LockssDaemon;
import org.lockss.exporter.counter.CounterReport;
import org.lockss.exporter.counter.CounterReportsBookReport1;
import org.lockss.exporter.counter.CounterReportsBookReport1L;
import org.lockss.exporter.counter.CounterReportsBookReport2;
import org.lockss.exporter.counter.CounterReportsBookReport2L;
import org.lockss.exporter.counter.CounterReportsJournalReport1;
import org.lockss.exporter.counter.CounterReportsJournalReport1L;
import org.lockss.exporter.counter.CounterReportsJournalReport5;
import org.lockss.exporter.counter.CounterReportsJournalReport5L;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.CounterReportResult;
import org.lockss.ws.entities.CounterReportParams;
import org.lockss.ws.entities.LockssWebServicesFault;

@MTOM
@WebService
public class CounterReportsServiceImpl implements CounterReportsService {
  private static Logger log = Logger.getLogger(CounterReportsServiceImpl.class);

  private static final String DEFAULT_BOOK_REPORT_ID = "1";
  private static final String DEFAULT_JOURNAL_REPORT_ID = "1";
  private static final String REPORT_FORMAT_CSV = "CSV";
  private static final String REPORT_FORMAT_TSV = "TSV";
  private static final String REPORT_ID_BOOK_1 = "1";
  private static final String REPORT_ID_BOOK_1L = "1L";
  private static final String REPORT_ID_BOOK_2 = "2";
  private static final String REPORT_ID_BOOK_2L = "2L";
  private static final String REPORT_ID_JOURNAL_1 = "1";
  private static final String REPORT_ID_JOURNAL_1L = "1L";
  private static final String REPORT_ID_JOURNAL_5 = "5";
  private static final String REPORT_ID_JOURNAL_5L = "5L";
  private static final String REPORT_TYPE_BOOK = "BOOK";
  private static final String REPORT_TYPE_JOURNAL = "JOURNAL";

  /**
   * Provides a COUNTER report.
   * 
   * @param reportParams
   *          A CounterReportParams with the parameters of the requested COUNTER
   *          report.
   * @return a CounterReportResult with the requested COUNTER report.
   * @throws LockssWebServicesFault
   */
  @Override
  public CounterReportResult getCounterReport(CounterReportParams reportParams)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getCounterReport(): ";

    try {
      // Handle a request for the default report.
      if (reportParams == null) {
	reportParams = new CounterReportParams();
      }

      // Handle any incoming specification of the type of title (book or
      // journal) covered by the report to be provided.
      String reportType = reportParams.getType();

      if (!StringUtil.isNullString(reportType)) {
	// Specified.
	reportType = reportType.toUpperCase();

	// Validation.
	if (!reportType.equals(REPORT_TYPE_BOOK)
	    && !reportType.equals(REPORT_TYPE_JOURNAL)) {
	  log.warning("Unknown report type: " + reportType);
	  reportType = REPORT_TYPE_JOURNAL;
	}
      } else {
	// Not specified.
	log.debug2(DEBUG_HEADER + "reportType is not found");
	reportType = REPORT_TYPE_JOURNAL;
      }

      log.debug2(DEBUG_HEADER + "reportType = " + reportType);

      // Handle any incoming specification of the identifier of the report to be
      // provided.
      String reportId = reportParams.getId();

      if (!StringUtil.isNullString(reportId)) {
	// Specified.
	reportId = reportId.toUpperCase();

	// Validation.
	if (reportType.equals(REPORT_TYPE_BOOK)
	    && !reportId.equals(REPORT_ID_BOOK_1)
	    && !reportId.equals(REPORT_ID_BOOK_1L)
	    && !reportId.equals(REPORT_ID_BOOK_2)
	    && !reportId.equals(REPORT_ID_BOOK_2L)) {
	  log.warning("Unknown book report identifier: " + reportId);
	  reportId = DEFAULT_BOOK_REPORT_ID;
	} else if (reportType.equals(REPORT_TYPE_JOURNAL)
	    && !reportId.equals(REPORT_ID_JOURNAL_1)
	    && !reportId.equals(REPORT_ID_JOURNAL_1L)
	    && !reportId.equals(REPORT_ID_JOURNAL_5)
	    && !reportId.equals(REPORT_ID_JOURNAL_5L)) {
	  log.warning("Unknown journal report identifier: " + reportId);
	  reportId = DEFAULT_JOURNAL_REPORT_ID;
	}
      } else {
	// Not specified.
	log.debug2(DEBUG_HEADER + "reportId is not found");
	if (reportType.equals(REPORT_TYPE_BOOK)) {
	  reportId = DEFAULT_BOOK_REPORT_ID;
	} else {
	  reportId = DEFAULT_JOURNAL_REPORT_ID;
	}
      }

      log.debug2(DEBUG_HEADER + "reportId = " + reportId);

      // Handle any incoming specification of the starting month of the period
      // covered by the report to be provided.
      int startMonth = -1;

      if (reportParams.getStartMonth() != null) {
	// Specified.
	startMonth = reportParams.getStartMonth().intValue();
	log.debug2(DEBUG_HEADER + "startMonth = " + startMonth);
      } else {
	// Not specified.
	log.debug2(DEBUG_HEADER + "startMonth is not found");
      }

      // Handle any incoming specification of the starting year of the period
      // covered by the report to be provided.
      int startYear = -1;

      if (reportParams.getStartYear() != null) {
	// Specified.
	startYear = reportParams.getStartYear().intValue();
	log.debug2(DEBUG_HEADER + "startYear = " + startYear);
      } else {
	// Not specified.
	log.debug2(DEBUG_HEADER + "startYear is not found");
      }

      // Handle any incoming specification of the ending month of the period
      // covered by the report to be provided.
      int endMonth = -1;

      if (reportParams.getEndMonth() != null) {
	// Specified.
	endMonth = reportParams.getEndMonth().intValue();
	log.debug2(DEBUG_HEADER + "endMonth = " + endMonth);
      } else {
	// Not specified.
	log.debug2(DEBUG_HEADER + "endMonth is not found");
      }

      // Handle any incoming specification of the ending year of the period
      // covered by the report to be provided.
      int endYear = -1;

      if (reportParams.getEndYear() != null) {
	// Specified.
	endYear = reportParams.getEndYear().intValue();
	log.debug2(DEBUG_HEADER + "endYear = " + endYear);
      } else {
	// Not specified.
	log.debug2(DEBUG_HEADER + "endYear is not found");
      }

      // Complete the period specification, if needed and possible.
      if (startMonth == -1 && endMonth != -1) {
	startMonth = endMonth;
      } else if (endMonth == -1 && startMonth != -1) {
	endMonth = startMonth;
      }

      if (startYear == -1 && endYear != -1) {
	startYear = endYear;
      } else if (endYear == -1 && startYear != -1) {
	endYear = startYear;
      }

      log.debug2(DEBUG_HEADER + "startMonth = " + startMonth);
      log.debug2(DEBUG_HEADER + "startYear = " + startYear);
      log.debug2(DEBUG_HEADER + "endMonth = " + endMonth);
      log.debug2(DEBUG_HEADER + "endYear = " + endYear);

      // Handle any incoming specification of the format of the report to be
      // provided.
      String reportFormat = reportParams.getFormat();

      if (!StringUtil.isNullString(reportFormat)) {
	// Specified.
	reportFormat = reportFormat.toUpperCase();

	// Validation.
	if (!reportFormat.equals(REPORT_FORMAT_CSV)
	    && !reportFormat.equals(REPORT_FORMAT_TSV)) {
	  log.warning("Unknown report format: " + reportFormat);
	  reportFormat = REPORT_FORMAT_CSV;
	}
      } else {
	// Not specified.
	log.debug2(DEBUG_HEADER + "reportFormat is not found");
	reportFormat = REPORT_FORMAT_CSV;
      }

      log.debug2(DEBUG_HEADER + "reportFormat = " + reportFormat);

      LockssDaemon daemon = LockssDaemon.getLockssDaemon();
      CounterReportsManager counterReportsManager =
	  daemon.getCounterReportsManager();
      CounterReport report = null;

      // Check whether the report with the default period needs to be provided.
      if (startMonth == -1 || startYear == -1 || endMonth == -1
	  || endYear == -1) {
	// Yes: Check whether it is a report on book requests.
	if (reportType.equals(REPORT_TYPE_BOOK)) {
	  // Yes: Initialize the appropriate book report.
	  if (reportId.equals(REPORT_ID_BOOK_1)) {
	    report = new CounterReportsBookReport1(daemon);
	  } else if (reportId.equals(REPORT_ID_BOOK_1L)) {
	    report = new CounterReportsBookReport1L(daemon);
	  } else if (reportId.equals(REPORT_ID_BOOK_2)) {
	    report = new CounterReportsBookReport2(daemon);
	  } else {
	    report = new CounterReportsBookReport2L(daemon);
	  }
	} else {
	  // No: Initialize the appropriate journal report.
	  if (reportId.equals(REPORT_ID_JOURNAL_1)) {
	    report = new CounterReportsJournalReport1(daemon);
	  } else if (reportId.equals(REPORT_ID_JOURNAL_1L)) {
	    report = new CounterReportsJournalReport1L(daemon);
	  } else if (reportId.equals(REPORT_ID_JOURNAL_5)) {
	    report = new CounterReportsJournalReport5(daemon);
	  } else {
	    report = new CounterReportsJournalReport5L(daemon);
	  }
	}
      } else {
	// No: Check whether it is a report on book requests.
	if (reportType.equals(REPORT_TYPE_BOOK)) {
	  // Yes: Initialize the appropriate book report.
	  if (reportId.equals(REPORT_ID_BOOK_1)) {
	    report =
		new CounterReportsBookReport1(daemon, startMonth, startYear,
					      endMonth, endYear);
	  } else if (reportId.equals(REPORT_ID_BOOK_1L)) {
	    report =
		new CounterReportsBookReport1L(daemon, startMonth, startYear,
					       endMonth, endYear);
	  } else if (reportId.equals(REPORT_ID_BOOK_2)) {
	    report =
		new CounterReportsBookReport2(daemon, startMonth, startYear,
					      endMonth, endYear);
	  } else {
	    report =
		new CounterReportsBookReport2L(daemon, startMonth, startYear,
					       endMonth, endYear);
	  }
	} else {
	  // No: Initialize the appropriate journal report.
	  if (reportId.equals(REPORT_ID_JOURNAL_1)) {
	    report =
		new CounterReportsJournalReport1(daemon, startMonth, startYear,
						 endMonth, endYear);
	  } else if (reportId.equals(REPORT_ID_JOURNAL_1L)) {
	    report =
		new CounterReportsJournalReport1L(daemon, startMonth,
						  startYear, endMonth, endYear);
	  } else if (reportId.equals(REPORT_ID_JOURNAL_5)) {
	    report =
		new CounterReportsJournalReport5(daemon, startMonth, startYear,
						 endMonth, endYear);
	  } else {
	    report =
		new CounterReportsJournalReport5L(daemon, startMonth,
						  startYear, endMonth, endYear);
	  }
	}
      }

      // Populate the report with the request counts in the appropriate format.
      if (reportFormat.equals(REPORT_FORMAT_CSV)) {
	report.saveCsvReport();
      } else {
	report.saveTsvReport();
      }

      // Get the report file.
      String extension = reportFormat.equals(REPORT_FORMAT_CSV)
	  ? CounterReportsManager.CSV_EXTENSION
	      : CounterReportsManager.TSV_EXTENSION;

      String reportFileName = report.getReportFileName(extension);

      File reportFile =
	  new File(counterReportsManager.getOutputDir(), reportFileName);
      log.debug2(DEBUG_HEADER + "reportFile = " + reportFile.getAbsolutePath());

      // Populate the response.
      CounterReportResult result = new CounterReportResult();
      result.setFileName(reportFileName);
      result.setDataHandler(new DataHandler(new FileDataSource(reportFile)));

      return result;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }
}
