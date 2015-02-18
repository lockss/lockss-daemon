/*
 * $Id$
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
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
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
import org.mortbay.html.Form;
import org.mortbay.html.Page;
import org.mortbay.html.Select;
import org.mortbay.html.Table;

@SuppressWarnings("serial")
public class CounterReportsServlet extends LockssServlet {
  private static final Logger log = Logger
      .getLogger(CounterReportsServlet.class);

  private static final String DEFAULT_BOOK_REPORT_ID = "1";
  private static final String DEFAULT_JOURNAL_REPORT_ID = "1";
  private static final String DEFAULT_REPORT_FORMAT = "CSV";
  private static final String DEFAULT_REPORT_TYPE = "JOURNAL";
  private static final String REPORT_FORMAT_CSV = "CSV";
  private static final String REPORT_FORMAT_CSV_NAME = "COMMA-SEPARATED";
  private static final String REPORT_FORMAT_TSV = "TSV";
  private static final String REPORT_FORMAT_TSV_NAME = "TAB-SEPARATED";
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

  private static final SortedMap<String, Class<?>> BOOK_REPORTS =
      new TreeMap<String, Class<?>>() {
	{
	  put(REPORT_ID_BOOK_1, CounterReportsBookReport1.class);
	  put(REPORT_ID_BOOK_1L, CounterReportsBookReport1L.class);
	  put(REPORT_ID_BOOK_2, CounterReportsBookReport2.class);
	  put(REPORT_ID_BOOK_2L, CounterReportsBookReport2L.class);
	}
      };

  private static final SortedMap<String, Class<?>> JOURNAL_REPORTS =
      new TreeMap<String, Class<?>>() {
	{
	  put(REPORT_ID_JOURNAL_1, CounterReportsJournalReport1.class);
	  put(REPORT_ID_JOURNAL_1L, CounterReportsJournalReport1L.class);
	  put(REPORT_ID_JOURNAL_5, CounterReportsJournalReport5.class);
	  put(REPORT_ID_JOURNAL_5L, CounterReportsJournalReport5L.class);
	}
      };

  private static final SortedMap<String, String> REPORT_FORMATS =
      new TreeMap<String, String>() {
	{
	  put(REPORT_FORMAT_CSV, REPORT_FORMAT_CSV_NAME);
	  put(REPORT_FORMAT_TSV, REPORT_FORMAT_TSV_NAME);
	}
      };

  private static final SortedMap<String, Map<String, Class<?>>> REPORT_TYPES =
      new TreeMap<String, Map<String, Class<?>>>() {
	{
	  put(REPORT_TYPE_BOOK, BOOK_REPORTS);
	  put(REPORT_TYPE_JOURNAL, JOURNAL_REPORTS);
	}
      };

  private static final String[] MONTHS = { "JANUARY", "FEBRUARY", "MARCH",
      "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMEBER", "OCTOBER",
      "NOVEMBER", "DECEMBER" };

  private static final String EXPLANATION =
      "Configure the COUNTER Report to be retrieved.";

  private static final String FORM_ACTION = "Get Report";

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
      errMsg = "COUNTER reports are not enabled.";
      Page page = newPage();
      addJavaScript(page);
      ServletUtil.layoutExplanationBlock(page, EXPLANATION);
      layoutErrorBlock(page);
      endPage(page);
      return;
    }

    // The form needs to be shown when no parameters have been passed.
    boolean showForm = !req.getParameterNames().hasMoreElements();
    log.debug2(DEBUG_HEADER + "showForm = " + showForm);

    // Check whether the form needs to be shown.
    if (showForm) {
      // Yes: Show the form.
      displayForm();
    } else {
      // No: Stream the report back.
      try {
	processRequest();
      } catch (Exception e) {
	// Report the problem.
	errMsg = e.getMessage();
	displayForm();
      }
    }
  }

  /**
   * Displays the form used to specify the parameters of the COUNTER report to
   * be generated.
   * 
   * @throws IOException
   */
  private void displayForm() throws IOException {
    final String DEBUG_HEADER = "displayForm(): ";
    Page page = newPage();
    addJavaScript(page);
    ServletUtil.layoutExplanationBlock(page, EXPLANATION);

    // Sanitize the error message by removing the name of the Java exception, if
    // necessary.
    if (!StringUtil.isNullString(errMsg)) {
      String endCutText = "Exception: ";
      log.debug2(DEBUG_HEADER + "errMsg = '" + errMsg + "'.");
      int location = errMsg.indexOf(endCutText);
      if (location != -1) {
	errMsg = errMsg.substring(location + endCutText.length());
	log.debug2(DEBUG_HEADER + "errMsg = '" + errMsg + "'.");
      }
    }

    layoutErrorBlock(page);

    // Start form
    Form form = ServletUtil.newForm(srvURL(myServletDescr()));
    Table table = new Table(0, "align=\"center\" cellpadding=\"5\"");

    // The report type row.
    table.newRow();
    table.newCell("align=\"right\"");
    table.add("Report Type: ");

    Select selType = new Select("type", false);

    String reportType = getReportType();
    if (!REPORT_TYPES.containsKey(reportType)) {
      reportType = DEFAULT_REPORT_TYPE;
    }

    for (String type : REPORT_TYPES.keySet()) {
      selType.add(type, type.equals(reportType), type);
    }

    selType.attribute("onchange", "toggleElements('bookId', 'journalId')");
    setTabOrder(selType);

    // The report identifier row.
    table.newCell("align=\"left\"");
    table.add(selType);

    table.newRow();
    table.newCell("align=\"right\"");
    table.add("Report Id: ");
    table.newCell("align=\"left\"");

    addReportId(table, "bookId", BOOK_REPORTS.keySet(),
		REPORT_TYPE_BOOK.equals(reportType));

    addReportId(table, "journalId", JOURNAL_REPORTS.keySet(),
		REPORT_TYPE_JOURNAL.equals(reportType));

    // The report period rows.
    String[] years = populateYears();
    addPeriodMonthYear(table, "Start Month: ", "startMonth", years,
		       "Start Year: ", "startYear");

    addPeriodMonthYear(table, "End Month: ", "endMonth", years, "End Year: ",
		       "endYear");

    // The report format row.
    table.newRow();
    table.newCell("align=\"right\"");
    table.add("Report Format: ");

    Select selFormat = new Select("format", false);

    for (String format : REPORT_FORMATS.keySet()) {
      selFormat.add(REPORT_FORMATS.get(format),
		    format.equals(getReportFormat()), format);
    }

    setTabOrder(selFormat);

    table.newCell("align=\"left\"");
    table.add(selFormat);
    form.add(table);

    // The submit button.
    ServletUtil.layoutSubmitButton(this, form, ACTION_TAG, FORM_ACTION,
				   FORM_ACTION);
    page.add(form);
    endPage(page);
  }

  /**
   * Displays the selector of report identifiers.
   * 
   * @param table
   *          A Table where to add the created selector.
   * @param idNameId
   *          A String with name and identifier attributes to be used for the
   *          selector.
   * @param ids
   *          A Set<String> with the identifiers to be included in the selector.
   * @param show
   *          A boolean indicating whether the selector needs to be displayed
   *          initially.
   */
  private void addReportId(Table table, String idNameId, Set<String> ids,
      boolean show) {
    Select selBookId = new Select(idNameId, false);

    for (String id : ids) {
      selBookId.add(id, id.equals(getReportId()), id);
    }

    selBookId.attribute("id", idNameId);
    if (!show) {
      selBookId.attribute("style", "display: none");
    }

    setTabOrder(selBookId);
    table.add(selBookId);
  }

  /**
   * Provides the years to be included in the report period selectors.
   * 
   * @return a String[] with the years to be included in the report period
   *         selectors.
   */
  private String[] populateYears() {
    Calendar cal = new GregorianCalendar();

    // Always end the list with next year.
    cal.add(Calendar.YEAR, 1);
    int lastYear = cal.get(Calendar.YEAR);

    Collection<String> yearList = new ArrayList<String>();

    for (int year = 2010; year <= lastYear; year++) {
      yearList.add(String.valueOf(year));
    }

    return (String[]) yearList.toArray(new String[0]);
  }

  /**
   * Displays the month and year selectors of one edge of the report period.
   * 
   * @param table
   *          A Table where to add the created selectors.
   * @param monthLabel
   *          A String with display label of the month selector.
   * @param monthName
   *          A String with name attribute to be used for the month selector.
   * @param years
   *          A String[] with the years to be included in the year selector.
   * @param yearLabel
   *          A String with display label of the year selector.
   * @param yearName
   *          A String with name attribute to be used for the year selector.
   */
  private void addPeriodMonthYear(Table table, String monthLabel,
      String monthName, String[] years, String yearLabel, String yearName) {
    table.newRow();

    // The month selector.
    table.newCell("align=\"right\"");
    table.add(monthLabel);

    Select selStartMonth = new Select(monthName, false);
    String value = null;

    for (int i = 0; i < MONTHS.length; i++) {
      value = String.valueOf(i + 1);
      selStartMonth.add(MONTHS[i], value.equals(req.getParameter(monthName)),
			value);
    }

    setTabOrder(selStartMonth);
    table.add(selStartMonth);

    // The year selector.
    table.newCell("align=\"left\"");
    table.add(yearLabel);

    Select selStartYear = new Select(yearName, false);

    for (int i = 0; i < years.length; i++) {
      selStartYear.add(years[i], years[i].equals(req.getParameter(yearName)),
		       years[i]);
    }

    setTabOrder(selStartYear);
    table.add(selStartYear);
  }

  /**
   * Validates the request and it streams the requested COUNTER report back to
   * the browser.
   * 
   * @throws IOException
   */
  private void processRequest() throws IOException {
    final String DEBUG_HEADER = "processRequest(): ";

    // Handle any incoming specification of the type of title (book or journal)
    // covered by the report to be provided.
    String reportType = getReportType();

    // Validate the report type.
    if (!REPORT_TYPES.containsKey(reportType)) {
      String message = "Invalid report type: " + reportType;
      log.warning(message);
      throw new RuntimeException(message);
    }

    // Handle any incoming specification of the identifier of the report to be
    // provided.
    String reportId = getReportId();

    // Validate the report identifier.
    if (!REPORT_TYPES.get(reportType).containsKey(reportId)) {
      String message =
	  "Invalid " + reportType + " report identifier: " + reportId;
      log.warning(message);
      throw new RuntimeException(message);
    }

    log.debug2(DEBUG_HEADER + "reportId = " + reportId);

    // Handle any incoming specification of the period covered by the report to
    // be provided.
    Integer startMonth = convertRequestParameterToInteger("startMonth");
    Integer startYear = convertRequestParameterToInteger("startYear");
    Integer endMonth = convertRequestParameterToInteger("endMonth");
    Integer endYear = convertRequestParameterToInteger("endYear");

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
    String reportFormat = getReportFormat();

    // Validate the report format.
    if (!REPORT_FORMATS.containsKey(reportFormat)) {
      String message = "Invalid report format: " + reportFormat;
      log.warning(message);
      throw new RuntimeException(message);
    }

    log.debug2(DEBUG_HEADER + "reportFormat = " + reportFormat);

    LockssDaemon daemon = getLockssDaemon();
    CounterReport report = null;

    try {
      // Check whether the report with the default period needs to be provided.
      if (startMonth == null || startYear == null || endMonth == null
	  || endYear == null) {
	// Yes: Initialize the appropriate report.
	report =
	    (CounterReport) (REPORT_TYPES.get(reportType).get(reportId)
		.getConstructor(LockssDaemon.class).newInstance(daemon));
      } else {
	// No: Initialize the appropriate book report.
	report =
	    (CounterReport) (REPORT_TYPES
		.get(reportType)
		.get(reportId)
		.getConstructor(LockssDaemon.class, int.class, int.class,
				int.class, int.class).newInstance(daemon,
								  startMonth,
								  startYear,
								  endMonth,
								  endYear));
      }
    } catch (InvocationTargetException ite) {
      throw new RuntimeException(ite.getCause());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Populate the report with the request counts in the appropriate format.
    String contentType = null;
    File reportFile = null;
    try {
      if (reportFormat.equals(REPORT_FORMAT_CSV)) {
	reportFile = report.saveCsvReport();
	contentType =
	    MimeUtil
		.getMimeTypeFromExtension(CounterReportsManager.CSV_EXTENSION);
      } else {
	reportFile = report.saveTsvReport();
	contentType =
	    MimeUtil
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
    resp.setContentLength((int) reportFile.length());
    resp.setHeader("Content-Disposition", "attachment; filename=\""
	+ reportFile.getName() + "\"");

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
   * Provides the submitted report type, if any, or the default, if no report
   * type is specified.
   * 
   * @return a String with the report type.
   */
  private String getReportType() {
    final String DEBUG_HEADER = "getReportType(): ";
    // Handle any incoming specification of the type of title (book or journal)
    // covered by the report to be provided.
    String reportType = req.getParameter("type");

    if (!StringUtil.isNullString(reportType)) {
      // Specified.
      reportType = reportType.toUpperCase();
    } else {
      // Not specified.
      log.debug2(DEBUG_HEADER + "reportType is not found");
      reportType = DEFAULT_REPORT_TYPE;
    }

    log.debug2(DEBUG_HEADER + "reportType = " + reportType);
    return reportType;
  }

  /**
   * Provides the submitted report identifier, if any, or the default, if no
   * report identifier is specified.
   * 
   * @return a String with the report type.
   */
  private String getReportId() {
    final String DEBUG_HEADER = "getReportId(): ";
    // Handle any incoming specification of the identifier of the report to be
    // provided.
    String reportId = req.getParameter("id");

    if (!StringUtil.isNullString(reportId)) {
      // Specified.
      reportId = reportId.toUpperCase();
    } else {
      // Not specified generically. Maybe it has been specified for the
      // selected type.
      String reportType = getReportType();
      if (reportType.equals(REPORT_TYPE_BOOK)
	  && !StringUtil.isNullString(req.getParameter("bookId"))) {
	reportId = req.getParameter("bookId").toUpperCase();
      } else if (reportType.equals(REPORT_TYPE_JOURNAL)
	  && !StringUtil.isNullString(req.getParameter("journalId"))) {
	reportId = req.getParameter("journalId").toUpperCase();
      }

      // Use the default if it has not been specified.
      if (StringUtil.isNullString(reportId)) {
	log.debug2(DEBUG_HEADER + "reportId is not found");
	if (reportType.equals(REPORT_TYPE_BOOK)) {
	  reportId = DEFAULT_BOOK_REPORT_ID;
	} else {
	  reportId = DEFAULT_JOURNAL_REPORT_ID;
	}
      }
    }

    log.debug2(DEBUG_HEADER + "reportId = " + reportId);
    return reportId;
  }

  /**
   * Converts a request parameter to an integer.
   * 
   * @param name
   *          A String with the name of the request parameter to convert.
   * @return an Integer with the converted parameter.
   */
  private Integer convertRequestParameterToInteger(String name) {
    final String DEBUG_HEADER = "convertPeriodStringToInteger(): ";
    Integer result = null;

    if (!StringUtil.isNullString(req.getParameter(name))) {
      try {
	result = Integer.valueOf(req.getParameter(name));
	log.debug2(DEBUG_HEADER + name + " = " + result);
      } catch (NumberFormatException nfe) {
	String message =
	    "Invalid parameter " + name + ": " + req.getParameter(name)
		+ " - It must be numeric.";
	log.warning(message);
	throw new RuntimeException(message);
      }
    } else {
      // Not specified.
      log.debug2(DEBUG_HEADER + name + " is not found");
    }

    return result;
  }

  /**
   * Provides the submitted report format, if any, or the default, if no report
   * format is specified.
   * 
   * @return a String with the report type.
   */
  private String getReportFormat() {
    final String DEBUG_HEADER = "getReportFormat(): ";
    // Handle any incoming specification of the format of the report to be
    // provided.
    String reportFormat = req.getParameter("format");

    if (!StringUtil.isNullString(reportFormat)) {
      // Specified.
      reportFormat = reportFormat.toUpperCase();
    } else {
      // Not specified.
      log.debug2(DEBUG_HEADER + "reportFormat is not found");
      reportFormat = DEFAULT_REPORT_FORMAT;
    }

    log.debug2(DEBUG_HEADER + "reportFormat = " + reportFormat);
    return reportFormat;
  }
}
