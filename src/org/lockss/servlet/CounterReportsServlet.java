/*
 * $Id: CounterReportsServlet.java,v 1.1 2012-10-01 21:09:41 fergaloy-sf Exp $
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
 * A servlet used to serve COUNTER reports.
 */
package org.lockss.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
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
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.MimeUtil;
import org.lockss.util.StreamUtil;
import org.lockss.util.StringUtil;

@SuppressWarnings("serial")
public class CounterReportsServlet extends LockssServlet {
  private static final Logger log = Logger.getLogger(CounterReportsServlet.class);

  private static final String DEFAULT_BOOK_REPORT_ID = "1";
  private static final String DEFAULT_JOURNAL_REPORT_ID = "1";
  private static final String DEFAULT_REPORT_FORMAT = "CSV";
  private static final String DEFAULT_REPORT_TYPE = "JOURNAL";
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
  
  private static final Map<String, Class<?>> BOOK_REPORTS =
      new HashMap<String, Class<?>>() {{
	put(REPORT_ID_BOOK_1, CounterReportsBookReport1.class);
	put(REPORT_ID_BOOK_1L, CounterReportsBookReport1L.class);
	put(REPORT_ID_BOOK_2, CounterReportsBookReport2.class);
	put(REPORT_ID_BOOK_2L, CounterReportsBookReport2L.class);
      }};
      
  private static final Map<String, Class<?>> JOURNAL_REPORTS =
      new HashMap<String, Class<?>>() {{
    	put(REPORT_ID_JOURNAL_1, CounterReportsJournalReport1.class);
    	put(REPORT_ID_JOURNAL_1L, CounterReportsJournalReport1L.class);
    	put(REPORT_ID_JOURNAL_5, CounterReportsJournalReport5.class);
    	put(REPORT_ID_JOURNAL_5L, CounterReportsJournalReport5L.class);
      }};
      
  private static final Map<String, Map<String, Class<?>>> REPORT_TYPES =
      new HashMap<String, Map<String, Class<?>>>() {{
    	put(REPORT_TYPE_BOOK, BOOK_REPORTS);
    	put(REPORT_TYPE_JOURNAL, JOURNAL_REPORTS);
      }};

  private CounterReportsManager counterReportsManager;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    counterReportsManager = getLockssDaemon().getCounterReportsManager();
  }

  /**
   * Processes the user request.
   * 
   * @throws IOException
   */
  public void lockssHandleRequest() throws IOException {
    final String DEBUG_HEADER = "lockssHandleRequest(): ";
    
    // Check whether the COUNTER reports manager is disabled.
    if (!counterReportsManager.isReady()) {
	// Yes: Report the problem.
	throw new RuntimeException("COUNTER reports are not enabled.");
    }
    
    // Handle any incoming specification of the type of title (book or journal)
    // covered by the report to be provided.
    String reportType = req.getParameter("type");

    if (!StringUtil.isNullString(reportType)) {
      // Specified.
      reportType = reportType.toUpperCase();

      // Validation.
      if (!REPORT_TYPES.containsKey(reportType)) {
	String message = "Invalid report type: " + reportType;
	log.warning(message);
	throw new RuntimeException(message);
      }
    } else {
      // Not specified.
      log.debug2(DEBUG_HEADER + "reportType is not found");
      reportType = DEFAULT_REPORT_TYPE;
    }

    log.debug2(DEBUG_HEADER + "reportType = " + reportType);

    // Handle any incoming specification of the identifier of the report to be
    // provided.
    String reportId = req.getParameter("id");
    
    if (!StringUtil.isNullString(reportId)) {
      // Specified.
      reportId = reportId.toUpperCase();

      if (!REPORT_TYPES.get(reportType).containsKey(reportId)) {
	String message = "Invalid " + reportType + " report identifier: "
	    + reportId;
	log.warning(message);
	throw new RuntimeException(message);
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

    // Handle any incoming specification of the period covered by the report to
    // be provided.
    Integer startMonth = convertRequestParameterToInt("startMonth");
    Integer startYear = convertRequestParameterToInt("startYear");
    Integer endMonth = convertRequestParameterToInt("endMonth");
    Integer endYear = convertRequestParameterToInt("endYear");
    
    // Complete the period specification, if needed and possible.
    if (startMonth == null && endMonth != null) {
      startMonth = endMonth;
    } else if (endMonth == null && startMonth != null) {
      endMonth = startMonth;
    }

    if (startYear == null && endYear != null) {
      startYear = endYear;
    } else if (endYear == null && startYear != null) {
      endYear = startYear;
    }
    
    log.debug2(DEBUG_HEADER + "startMonth = " + startMonth);
    log.debug2(DEBUG_HEADER + "startYear = " + startYear);
    log.debug2(DEBUG_HEADER + "endMonth = " + endMonth);
    log.debug2(DEBUG_HEADER + "endYear = " + endYear);

    // Handle any incoming specification of the format of the report to be
    // provided.
    String reportFormat = req.getParameter("format");
    
    if (!StringUtil.isNullString(reportFormat)) {
      // Specified.
      reportFormat = reportFormat.toUpperCase();
      
      // Validation.
      if (!reportFormat.equals(REPORT_FORMAT_CSV)
	  && !reportFormat.equals(REPORT_FORMAT_TSV)) {
	String message = "Invalid report format: " + reportFormat;
	log.warning(message);
	throw new RuntimeException(message);
      }
    } else {
      // Not specified.
      log.debug2(DEBUG_HEADER + "reportFormat is not found");
      reportFormat = DEFAULT_REPORT_FORMAT;
    }

    log.debug2(DEBUG_HEADER + "reportFormat = " + reportFormat);

    LockssDaemon daemon = getLockssDaemon();
    CounterReport report = null;
    
    try {
      // Check whether the report with the default period needs to be provided.
      if (startMonth == null || startYear == null
	  || endMonth == null || endYear == null) {
	// Yes: Initialize the appropriate report.
	report = (CounterReport)(REPORT_TYPES.get(reportType).get(reportId)
	    .getConstructor(LockssDaemon.class).newInstance(daemon));
      } else {
	// No: Initialize the appropriate book report.
	report = (CounterReport)(REPORT_TYPES.get(reportType).get(reportId)
	    .getConstructor(LockssDaemon.class, int.class, int.class, int.class,
	                    int.class).newInstance(daemon, startMonth,
	                                           startYear, endMonth,
	                                           endYear));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Populate the report with the request counts in the appropriate format.
    String contentType = null;
    File reportFile = null;
    try {
      if (reportFormat.equals(REPORT_FORMAT_CSV)) {
	reportFile = report.saveCsvReport();
	contentType = MimeUtil
	    .getMimeTypeFromExtension(CounterReportsManager.CSV_EXTENSION);
      } else {
	reportFile = report.saveTsvReport();
	contentType = MimeUtil
	    .getMimeTypeFromExtension(CounterReportsManager.TSV_EXTENSION);
      }
    } catch (SQLException sqle) {
      throw new RuntimeException(sqle);
    } catch (Exception e) {
      if (e instanceof IOException) {
	throw (IOException) e;
      } else {
	throw new RuntimeException(e);
      }
    }

    log.debug2(DEBUG_HEADER + "reportFile = " + reportFile.getAbsolutePath());

    resp.setContentType(contentType);
    resp.setContentLength((int)reportFile.length());
    resp.setHeader("Content-Disposition",
                   "attachment; filename=\"" + reportFile.getName() + "\"");

    // Stream the report file to the servlet response.
    ServletOutputStream sos = null;
    FileInputStream fis = null;

    try {
      fis = new FileInputStream(reportFile);
      sos = resp.getOutputStream();
      StreamUtil.copy(fis, sos);
    } finally {
      IOUtil.safeClose(fis);
      IOUtil.safeClose(sos);
    }
  }

  /**
   * Converts a request parameter to an integer.
   * @param name A String with the name of the request parameter to convert.
   * @return an int with 
   */
  private Integer convertRequestParameterToInt(String name) {
    final String DEBUG_HEADER = "convertPeriodStringToInt(): ";
    Integer result = null;

    if (!StringUtil.isNullString(req.getParameter(name))) {
      try {
	result = Integer.valueOf(req.getParameter(name));
      } catch (NumberFormatException nfe) {
	String message =
	    "Invalid parameter " + name + ": " + req.getParameter(name) + " - It must be numeric.";
	log.warning(message);
	throw new RuntimeException(message);
      }
      log.debug2(DEBUG_HEADER + name + " = " + result);
    } else {
      // Not specified.
      log.debug2(DEBUG_HEADER + name + " is not found");
    }
    
    return result;
  }
}
