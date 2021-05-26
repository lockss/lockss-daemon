/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.servlet;

import java.io.*;
import java.util.*;

import javax.servlet.*;

import org.mortbay.html.*;
import org.w3c.dom.Document;

import org.lockss.config.*;
import org.lockss.daemon.status.*;
import org.lockss.plugin.PluginManager;
import org.lockss.util.*;


/**
 * DaemonStatus servlet
 */
public class DaemonStatus extends LockssServlet {
  
  private static final Logger log = Logger.getLogger(DaemonStatus.class);

  /** Supported output formats */
  static final int OUTPUT_HTML = 1;
  static final int OUTPUT_TEXT = 2;
  static final int OUTPUT_XML = 3;
  static final int OUTPUT_CSV = 4;

  private String tableName;
  private String tableKey;
  private String sortKey;
  private StatusTable statTable;
  private StatusService statSvc;
  private int outputFmt;
  private java.util.List rules;
  private BitSet tableOptions;
  private PluginManager pluginMgr;

  protected void resetLocals() {
    super.resetLocals();
    rules = null;
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    statSvc = getLockssDaemon().getStatusService();
    pluginMgr = getLockssDaemon().getPluginManager();
  }

  static final Set fixedParams =
    SetUtil.set("text", "output", "options", "table", "key", "sort");

  /**
   * Handle a request
   * @throws IOException
   */
  public void lockssHandleRequest() throws IOException {
    if (!StringUtil.isNullString(req.getParameter("isDaemonReady"))) {
      if (pluginMgr.areAusStarted()) {
	resp.setStatus(200);
	PrintWriter wrtr = resp.getWriter();
	resp.setContentType("text/plain");
	wrtr.println("true");
      } else {
	PrintWriter wrtr = resp.getWriter();
	resp.setContentType("text/plain");
	wrtr.println("false");
	resp.sendError(202, "Not ready");
      }
      return;
    }

    outputFmt = OUTPUT_HTML;	// default output is html

    String outputParam = req.getParameter("output");
    if (!StringUtil.isNullString(outputParam)) {
      if ("html".equalsIgnoreCase(outputParam)) {
	outputFmt = OUTPUT_HTML;
      } else if ("xml".equalsIgnoreCase(outputParam)) {
	outputFmt = OUTPUT_XML;
      } else if ("text".equalsIgnoreCase(outputParam)) {
	outputFmt = OUTPUT_TEXT;
      } else if ("csv".equalsIgnoreCase(outputParam)) {
	outputFmt = OUTPUT_CSV;
      } else {
	log.warning("Unknown output format: " + outputParam);
      }
    }
    String optionsParam = req.getParameter("options");

    tableOptions = new BitSet();

    if (isDebugUser()) {
      log.debug2("Debug user.  Setting OPTION_DEBUG_USER");
      tableOptions.set(StatusTable.OPTION_DEBUG_USER);
    }

    for (Iterator iter = StringUtil.breakAt(optionsParam, ',').iterator();
	 iter.hasNext(); ) {
      String s = (String)iter.next();
      if ("norows".equalsIgnoreCase(s)) {
	tableOptions.set(StatusTable.OPTION_NO_ROWS);
      }
    }

    tableName = req.getParameter("table");
    tableKey = req.getParameter("key");
    if (StringUtil.isNullString(tableName)) {
      tableName = statSvc.getDefaultTableName();
    }
    if (StringUtil.isNullString(tableKey)) {
      tableKey = null;
    }
    sortKey = req.getParameter("sort");
    if (StringUtil.isNullString(sortKey)) {
      sortKey = null;
    }

    switch (outputFmt) {
    case OUTPUT_HTML:
      doHtmlStatusTable();
      break;
    case OUTPUT_XML:
      try {
        doXmlStatusTable();
      } catch (XmlDomBuilder.XmlDomException xde) {
        throw new IOException("Error building XML", xde);
      }
      break;
    case OUTPUT_TEXT:
      doTextStatusTable();
      break;
    case OUTPUT_CSV:
      doCsvStatusTable();
      break;
    }
  }

  private Page newTablePage() throws IOException {
    Page page = newPage();
    addJavaScript(page);

    if (!pluginMgr.areAusStarted()) {
      page.add(ServletUtil.notStartedWarning());
    }

    // After resp.getWriter() has been called, throwing an exception will
    // result in a blank page, so don't call it until the end.
    // (HttpResponse.sendError() calls getOutputStream(), and only one of
    // getWriter() or getOutputStream() may be called.)

    // all pages but index get a select box to choose a different table
    if (!isAllTablesTable()) {
      Block centeredBlock = new Block(Block.Center);
      centeredBlock.add(getSelectTableForm());
      page.add(centeredBlock);
      page.add(ServletUtil.removeElementWithId("dsSelectBox"));
    }

    //       page.add("<center>");
    //       page.add(srvLink(SERVLET_DAEMON_STATUS, ".",
    // 		       concatParams("text=1", req.getQueryString())));
    //       page.add("</center><br><br>");

    return page;
  }

  private void doHtmlStatusTable() throws IOException {
    Page page = doHtmlStatusTable0();
    endPage(page);
  }

  private void doTextStatusTable() throws IOException {
    PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");

//     String vPlatform = CurrentConfig.getParam(PARAM_PLATFORM_VERSION);
    String vPlatform;
    PlatformVersion pVer = ConfigManager.getPlatformVersion();
    if (pVer != null) {
      vPlatform = ", platform=" + StringUtil.ckvEscape(pVer.displayString());
    } else {
      vPlatform = "";
    }
    Date now = new Date();
    Date startDate = getLockssDaemon().getStartDate();
    wrtr.println("host=" + getLcapIPAddr() +
		 ",time=" + now.getTime() +
		 ",up=" + TimeBase.msSince(startDate.getTime()) +
		 ",version=" + BuildInfo.getBuildInfoString() +
		 vPlatform);
    doTextStatusTable(wrtr);
  }

  private StatusTable makeTable() throws StatusService.NoSuchTableException {
    StatusTable table = new StatusTable(tableName, tableKey);
    table.setOptions(tableOptions);
    for (Enumeration en = req.getParameterNames(); en.hasMoreElements(); ) {
      String name = (String)en.nextElement();
      if (!fixedParams.contains(name)) {
	table.setProperty(name, req.getParameter(name));
      }
    }
    statSvc.fillInTable(table);
    return table;
  }
  // build and send an XML DOM Document of the StatusTable
  private void doXmlStatusTable()
      throws IOException, XmlDomBuilder.XmlDomException {
    // By default, XmlDomBuilder will produce UTF-8.  Must set content type
    // *before* calling getWriter()
    resp.setContentType("text/xml; charset=UTF-8");
    PrintWriter wrtr = resp.getWriter();
    try {
      StatusTable statTable = makeTable();
      XmlStatusTable xmlTable = new XmlStatusTable(statTable);
      String over = req.getParameter("outputVersion");
      if (over != null) {
	try {
	  int ver = Integer.parseInt(over);
	  xmlTable.setOutputVersion(ver);
	} catch (NumberFormatException e) {
	  log.warning("Illegal outputVersion: " + over + ": " + e.toString());
	}
      }
      Document xmlTableDoc = xmlTable.getTableDocument();
      XmlDomBuilder.serialize(xmlTableDoc, wrtr);
    } catch (Exception e) {
      XmlDomBuilder xmlBuilder =
          new XmlDomBuilder(XmlStatusConstants.NS_PREFIX,
                            XmlStatusConstants.NS_URI,
                            "1.0");
      Document errorDoc = XmlDomBuilder.createDocument();
      org.w3c.dom.Element rootElem = xmlBuilder.createRoot(errorDoc,
          XmlStatusConstants.ERROR);
      if (e instanceof StatusService.NoSuchTableException) {
        XmlDomBuilder.addText(rootElem, "No such table: " + e.toString());
      } else {
        String emsg = e.toString();
        StringBuilder buffer = new StringBuilder("Error getting table: ");
        buffer.append(emsg);
        buffer.append("\n");
        buffer.append(StringUtil.trimStackTrace(emsg,
                                                StringUtil.stackTraceString(e)));
        XmlDomBuilder.addText(rootElem, buffer.toString());
      }
      XmlDomBuilder.serialize(errorDoc, wrtr);
      return;
    }
  }

  private java.util.List getRowList(StatusTable statTable) {
    java.util.List rowList;
    if (sortKey != null) {
      try {
	rules = makeSortRules(statTable, sortKey);
	rowList = statTable.getSortedRows(rules);
      } catch (Exception e) {
	// There are lots of ways a user-specified sort can fail if the
	// table creator isn't careful.  Fall back to default if that
	// happens.
	log.warning("Error sorting table by: " + rules, e);
	// XXX should display some sort of error msg
	rowList = statTable.getSortedRows();
	rules = null;		  // prevent column titles from indicating
				  // the sort order that didn't work
      }
    } else {
      rowList = statTable.getSortedRows();
    }
    return rowList;
  }

  // Build the table, adding elements to page
  private Page doHtmlStatusTable0() throws IOException {
    Page page;
    try {
      statTable = makeTable();
    } catch (StatusService.NoSuchTableException e) {
      page = newTablePage();
      errMsg = "No such table: " + e.getMessage();
      layoutErrorBlock(page);
      return page;
    } catch (Exception e) {
      page = newTablePage();
      errMsg = "Error getting table: " + e.toString();
      layoutErrorBlock(page);
      if (isDebugUser()) {
	page.add("<br><pre>    ");
	page.add(StringUtil.trimStackTrace(e.toString(),
					   StringUtil.stackTraceString(e)));
	page.add("</pre>");
      }
      return page;
    }
    java.util.List colList = statTable.getColumnDescriptors();
    java.util.List rowList = getRowList(statTable);
    String title0 = htmlEncode(statTable.getTitle());
    String titleFoot = htmlEncode(statTable.getTitleFootnote());

    page = newTablePage();
    Table table = null;

    // convert list of ColumnDescriptors to array of ColumnDescriptors
    ColumnDescriptor cds[];
    int cols;
    if (colList != null) {
      cds = (ColumnDescriptor [])colList.toArray(new ColumnDescriptor[0]);
      cols = cds.length;
    } else {
      cds = new ColumnDescriptor[0];
      cols = 1;
    }
    if (true || !rowList.isEmpty()) {
      // if table not empty, output column headings

      // Make the table.  Make a narrow empty column between real columns,
      // for spacing.  Resulting table will have 2*cols-1 columns
      table = new Table(0, "ALIGN=CENTER CELLSPACING=2 CELLPADDING=0");
      String title = title0 + addFootnote(titleFoot);

      table.newRow();
      table.addHeading(title, "ALIGN=CENTER COLSPAN=" + (cols * 2 - 1));
      table.newRow();

      addSummaryInfo(table, statTable, cols);

      if (colList != null) {
	// output column headings
	for (int ix = 0; ix < cols; ix++) {
	  ColumnDescriptor cd = cds[ix];
	  table.newCell("class=\"colhead\" valign=\"bottom\" align=\"" +
			((cols == 1) ? "center" : getColAlignment(cd)) + "\"");
	  table.add(getColumnTitleElement(statTable, cd, rules));
	  if (ix < (cols - 1)) {
	    table.newCell("width=8");
	    table.add("&nbsp;");
	  }
	}
      }
    }
    // Create (but do not yet refer to) any footnotes that the table wants
    // to be in a specific order
    for (String orderedFoot : statTable.getOrderedFootnotes()) {
      addFootnote(orderedFoot);
    }
    if (rowList != null) {
      // output rows
      for (Iterator rowIter = rowList.iterator(); rowIter.hasNext(); ) {
	Map rowMap = (Map)rowIter.next();
	if (rowMap.get(StatusTable.ROW_SEPARATOR) != null) {
	  table.newRow();
	  table.newCell("align=center colspan=" + (cols * 2 - 1));
	  table.add("<hr>");
	}
	table.newRow();
	for (int ix = 0; ix < cols; ix++) {
	  ColumnDescriptor cd = cds[ix];
	  Object val = rowMap.get(cd.getColumnName());

	  table.newCell("valign=\"top\" align=\"" + getColAlignment(cd) + "\"");
	  table.add(getDisplayString(val, cd.getType()));
	  if (ix < (cols - 1)) {
	    table.newCell();	// empty column for spacing
	  }
	}
      }
    }
    if (table != null) {
      Form frm = new Form(srvURL(myServletDescr()));
      // use GET so user can refresh in browser
      frm.method("GET");
      frm.add(table);
      page.add(frm);
      page.add("<br>");
    }
    return page;
  }

  private String htmlEncode(String s) {
    if (s == null) {
      return null;
    }
    return HtmlUtil.htmlEncode(s);
  }

  /** Prepend table name to servlet-specfici part of page title */
  protected String getTitleHeading() {
    if (statTable == null) {
      return super.getTitleHeading();
    } else {
      return statTable.getTitle() + " - " + super.getTitleHeading();
    }
  }

  // Build the table, writing text to wrtr
  private void doTextStatusTable(PrintWriter wrtr) throws IOException {
    StatusTable statTable;
    try {
      statTable = makeTable();
    } catch (StatusService.NoSuchTableException e) {
      wrtr.println("No table: " + e.toString());
      return;
    } catch (Exception e) {
      wrtr.println("Error getting table: " + e.toString());
      return;
    }
    wrtr.println();
    wrtr.print("table=" + StringUtil.ckvEscape(statTable.getTitle()));
    if (tableKey != null) {
      wrtr.print(",key=" + StringUtil.ckvEscape(tableKey));
    }

    java.util.List summary = statTable.getSummaryInfo();
    if (summary != null && !summary.isEmpty()) {
      for (Iterator iter = summary.iterator(); iter.hasNext(); ) {
	StatusTable.SummaryInfo sInfo = (StatusTable.SummaryInfo)iter.next();
	wrtr.print(",");
	wrtr.print(sInfo.getTitle());
	wrtr.print("=");
	Object dispVal = getTextDisplayString(sInfo.getValue());
	String valStr = dispVal != null ? dispVal.toString() : "(null)";
	wrtr.print(StringUtil.ckvEscape(valStr));
      }
    }
    wrtr.println();

    java.util.List rowList = getRowList(statTable);
    if (rowList != null) {
      // output rows
      for (Iterator rowIter = rowList.iterator(); rowIter.hasNext(); ) {
	Map rowMap = (Map)rowIter.next();
	for (Iterator iter = rowMap.keySet().iterator(); iter.hasNext(); ) {
	  Object o = iter.next();
	  if (!(o instanceof String)) {
	    // ignore special markers (eg, StatusTable.ROW_SEPARATOR)
	    continue;
	  }
	  String key = (String)o;
	  Object val = rowMap.get(key);
	  Object dispVal = getTextDisplayString(val);
	  String valStr = dispVal != null ? dispVal.toString() : "(null)";
	  wrtr.print(key + "=" + StringUtil.ckvEscape(valStr));
	  if (iter.hasNext()) {
	    wrtr.print(",");
	  } else {
	    wrtr.println();
	  }
	}
      }
    }
  }

  // Build the table, writing csv to wrtr
  private void doCsvStatusTable() throws IOException {
    PrintWriter wrtr = resp.getWriter();
    resp.setContentType("text/plain");

    StatusTable statTable;
    try {
      statTable = makeTable();
    } catch (StatusService.NoSuchTableException e) {
      wrtr.println("No table: " + e.toString());
      return;
    } catch (Exception e) {
      wrtr.println("Error getting table: " + e.toString());
      return;
    }
    java.util.List<ColumnDescriptor> colList =
      statTable.getColumnDescriptors();
    java.util.List<Map> rowList = getRowList(statTable);
    if (colList != null) {
      for (Iterator colIter = colList.iterator(); colIter.hasNext(); ) {
	ColumnDescriptor cd = (ColumnDescriptor)colIter.next();
	wrtr.print(StringUtil.csvEncode(cd.getTitle()));
	if (colIter.hasNext()) {
	  wrtr.print(",");
	} else {
	  wrtr.println();
	}
      }
      if (rowList != null) {
	// output rows
	for (Map rowMap : rowList) {
	  for (Iterator colIter = colList.iterator(); colIter.hasNext(); ) {
	    ColumnDescriptor cd = (ColumnDescriptor)colIter.next();
	    Object val = rowMap.get(cd.getColumnName());
	    Object dispVal = getTextDisplayString(val);
	    String valStr = dispVal != null ? dispVal.toString() : "(null)";
	    wrtr.print(StringUtil.csvEncode(valStr));
	    if (colIter.hasNext()) {
	      wrtr.print(",");
	    } else {
	      wrtr.println();
	    }
	  }
	}
      }
    } else {
      wrtr.println("(Empty table)");
    }
  }

  static final Image UPARROW1 = ServletUtil.image("uparrow1blue.gif", 16, 16, 0,
				      "Primary sort column, ascending");
  static final Image UPARROW2 = ServletUtil.image("uparrow2blue.gif", 16, 16, 0,
				      "Secondary sort column, ascending");
  static final Image DOWNARROW1 = ServletUtil.image("downarrow1blue.gif", 16, 16, 0,
					"Primary sort column, descending");
  static final Image DOWNARROW2 = ServletUtil.image("downarrow2blue.gif", 16, 16, 0,
					"Secondary sort column, descending");

  /** Create a column heading element:<ul>
   *   <li> plain text if not sortable
   *   <li> if sortable, link with sortkey set to solumn name,
   *        descending if was previous primary ascending key
   *   <li> plus a possible secondary sort key set to previous sort column
   *   <li> if is current primary or secondary sort colume, display an up or
   *        down arrow.</ul>
   * @param statTable
   * @param cd
   * @param rules the SortRules used to sort the currently displayed table
   */
  Composite getColumnTitleElement(StatusTable statTable, ColumnDescriptor cd,
				  java.util.List rules) {
    Composite elem = new Composite();
    Image sortArrow = null;
    boolean ascending = getDefaultSortAscending(cd);
    String colTitle = cd.getTitle();
    if (true && statTable.isResortable() && cd.isSortable()) {
      String ruleParam;
      if (rules != null && !rules.isEmpty()) {
	StatusTable.SortRule rule1 = (StatusTable.SortRule)rules.get(0);
	if (cd.getColumnName().equals(rule1.getColumnName())) {
	  // This column is the current primary sort; link to reverse order
	  ascending = !rule1.sortAscending();
	  // and display a primary arrow
	  sortArrow = rule1.sortAscending() ? UPARROW1 : DOWNARROW1;
	  if (rules.size() > 1) {
	    // keep same secondary sort key if there was one
	    StatusTable.SortRule rule2 = (StatusTable.SortRule)rules.get(1);
	    ruleParam = ruleParam(cd, ascending) + "," + ruleParam(rule2);
	  } else {
	    ruleParam = ruleParam(cd, ascending);
	  }
	} else {
	  if (rules.size() > 1) {
	    StatusTable.SortRule rule2 = (StatusTable.SortRule)rules.get(1);
	    if (cd.getColumnName().equals(rule2.getColumnName())) {
	      // This is the secondary sort column; display secondary arrow
	      sortArrow = rule2.sortAscending() ? UPARROW2 : DOWNARROW2;
	    }
	  }
	  // primary sort is this column, secondary is previous primary
	  ruleParam = ruleParam(cd, ascending) + "," + ruleParam(rule1);
	}
      } else {
	// no previous, sort by column
	ruleParam = ruleParam(cd, ascending);
      }
      Link link = new Link(srvURL(myServletDescr(),
				  modifyParams("sort", ruleParam)),
			   colTitle);
      link.attribute("class", "colhead");
      elem.add(link);
      String foot = cd.getFootnote();
      if (foot != null) {
	elem.add(addFootnote(foot));
      }
      if (sortArrow != null) {
	elem.add(sortArrow);
      }
    } else {
      elem.add(colTitle);
      elem.add(addFootnote(cd.getFootnote()));
    }
    return elem;
  }

  String ruleParam(ColumnDescriptor cd, boolean ascending) {
    return (ascending ? "A" : "D") + cd.getColumnName();
  }

  String ruleParam(StatusTable.SortRule rule) {
    return (rule.sortAscending() ? "A" : "D") + rule.getColumnName();
  }

  java.util.List makeSortRules(StatusTable statTable, String sortKey) {
    Map columnDescriptorMap = statTable.getColumnDescriptorMap();
    java.util.List cols = StringUtil.breakAt(sortKey, ',');
    java.util.List res = new ArrayList();
    for (Iterator iter = cols.iterator(); iter.hasNext(); ) {
      String spec = (String)iter.next();
      boolean ascending = spec.charAt(0) == 'A';
      String col = spec.substring(1);
      StatusTable.SortRule defaultRule =
	getDefaultRuleForColumn(statTable, col);
      StatusTable.SortRule rule;
      Comparator comparator = null;
      if (columnDescriptorMap.containsKey(col)) {
	ColumnDescriptor cd = (ColumnDescriptor)columnDescriptorMap.get(col);
	comparator = cd.getComparator();
      }
      if (defaultRule != null && defaultRule.getComparator() != null) {
	comparator = defaultRule.getComparator();
      }
      if (comparator != null) {
	rule = new StatusTable.SortRule(col, comparator, ascending);
      } else {
	rule = new StatusTable.SortRule(col, ascending);
      }
      res.add(rule);
    }
    log.debug2("rules: " + res);
    return res;
  }

  private StatusTable.SortRule getDefaultRuleForColumn(StatusTable statTable,
						       String col) {
    java.util.List defaults = statTable.getDefaultSortRules();
    for (Iterator iter = defaults.iterator(); iter.hasNext(); ) {
      StatusTable.SortRule rule = (StatusTable.SortRule)iter.next();
      if (col.equals(rule.getColumnName())) {
	return rule;
      }
    }
    return null;
  }


  private void addSummaryInfo(Table table, StatusTable statTable, int cols) {
    java.util.List summary = statTable.getSummaryInfo();
    if (summary != null && !summary.isEmpty()) {
      for (Iterator iter = summary.iterator(); iter.hasNext(); ) {
	StatusTable.SummaryInfo sInfo =
	  (StatusTable.SummaryInfo)iter.next();
	table.newRow();
	StringBuilder sb = null;
	String stitle = sInfo.getTitle();
	if (!StringUtil.isNullString(stitle)) {
	  sb = new StringBuilder();
	  sb.append("<b>");
	  sb.append(stitle);
	  if (sInfo.getHeaderFootnote() != null) {
	    sb.append(addFootnote(sInfo.getHeaderFootnote()));
	  }
	  sb.append("</b>:&nbsp;");
	}
	table.newCell("COLSPAN=" + (cols * 2 - 1));
	// make a 2 cell table for each row, so multiline values will be
	// aligned
 	Table itemtab = new Table(0, "align=left cellspacing=0 cellpadding=0");
	itemtab.newRow();
	if (sb != null) {
	  itemtab.newCell("valign=top");
	  itemtab.add(sb.toString());
	}
	Object sval = sInfo.getValue();
	if (sval != null) {
	  itemtab.newCell();
	  StringBuilder valSb = new StringBuilder();
	  valSb.append(getDisplayString(sval, sInfo.getType()));
	  if (sInfo.getValueFootnote() != null) {
	    valSb.append(addFootnote(sInfo.getValueFootnote()));
	  }
	  itemtab.add(valSb.toString());
	}
	table.add(itemtab);
      }
      table.newRow();
    }
  }

  private String getColAlignment(ColumnDescriptor cd) {
    switch (cd.getType()) {
    case ColumnDescriptor.TYPE_STRING:
    case ColumnDescriptor.TYPE_DATE:
    case ColumnDescriptor.TYPE_IP_ADDRESS:
    case ColumnDescriptor.TYPE_TIME_INTERVAL:
    default:
      return "left";
    case ColumnDescriptor.TYPE_INT:
    case ColumnDescriptor.TYPE_PERCENT:
    case ColumnDescriptor.TYPE_AGREEMENT:
    case ColumnDescriptor.TYPE_FLOAT:	// tk - should align decimal points?
      return "right";
    }
  }

  private boolean getDefaultSortAscending(ColumnDescriptor cd) {
    switch (cd.getType()) {
    case ColumnDescriptor.TYPE_STRING:
    case ColumnDescriptor.TYPE_IP_ADDRESS:
    case ColumnDescriptor.TYPE_TIME_INTERVAL:
    default:
      return true;
    case ColumnDescriptor.TYPE_INT:
    case ColumnDescriptor.TYPE_PERCENT:
    case ColumnDescriptor.TYPE_AGREEMENT:
    case ColumnDescriptor.TYPE_FLOAT:	// tk - should align decimal points?
    case ColumnDescriptor.TYPE_DATE:
      return false;
    }
  }


  // Handle lists
  private String getTextDisplayString(Object val) {
    if (val == StatusTable.NO_VALUE) {
      // Some, but not all text formats avoid calling this with NO_VALUE
      return "";
    }
    Object aval = StatusTable.getActualValue(val);
    if (aval instanceof Collection) {
      StringBuilder sb = new StringBuilder();
      for (Iterator iter = ((Collection)aval).iterator(); iter.hasNext(); ) {
	sb.append(StatusTable.getActualValue(iter.next()));
      }
      return sb.toString();
    } else {
      return aval != null ? aval.toString() : "(null)";
    }
  }


  // Handle lists
  private String getDisplayString(Object val, int type) {
    if (val instanceof Collection) {
      return getCollectionDisplayString((Collection)val,
					StatusTable.DisplayedValue.Layout.None,
					type);
    } else {
      return getDisplayString0(val, type);
    }
  }

  // Handle lists
  private String getCollectionDisplayString(Collection coll,
					    StatusTable.DisplayedValue.Layout layout,
					    int type) {
    StringBuilder sb = new StringBuilder();
    for (Iterator iter = coll.iterator(); iter.hasNext(); ) {
      sb.append(getDisplayString0(iter.next(), type));
      switch (layout) {
      case Column:
	if (iter.hasNext()) {
	  sb.append("<br>");
	}
      default:
      }
    }
    return sb.toString();
  }

  // Process References and other links
  private String getDisplayString0(Object val, int type) {
    if (val instanceof StatusTable.Reference) {
      return getRefString((StatusTable.Reference)val, type);
    } else if (val instanceof StatusTable.SrvLink) {
      // Display as link iff user is allowed access to the target servlet
      StatusTable.SrvLink slink = (StatusTable.SrvLink)val;
      if (isServletRunnable(slink.getServletDescr())) {
	return getSrvLinkString(slink, type);
      } else {
	return getDisplayString1(StatusTable.getActualValue(val), type);
      }
    } else if (val instanceof StatusTable.LinkValue) {
      // A LinkValue type we don't know about.  Just display its embedded
      // value.
      return getDisplayString1(StatusTable.getActualValue(val), type);
    } else {
      return getDisplayString1(val, type);
    }
  }

  // turn References into html links
  private String getRefString(StatusTable.Reference ref, int type) {
    StringBuilder sb = new StringBuilder();
    sb.append("table=");
    sb.append(ref.getTableName());
    String key = ref.getKey();
    if (!StringUtil.isNullString(key)) {
      sb.append("&key=");
      sb.append(urlEncode(key));
    }
    Properties refProps = ref.getProperties();
    if (refProps != null) {
      for (Iterator iter = refProps.entrySet().iterator(); iter.hasNext(); ) {
	Map.Entry ent = (Map.Entry)iter.next();
	sb.append("&");
	sb.append(ent.getKey());
	sb.append("=");
	sb.append(urlEncode((String)ent.getValue()));
      }
    }
    if (ref.getPeerId() != null) {
      return srvAbsLink(ref.getPeerId(),
			myServletDescr(),
			getDisplayString(ref.getValue(), type),
			sb.toString());
    } else {
      return srvLink(myServletDescr(),
		     getDisplayString(ref.getValue(), type),
		     sb.toString());
    }
  }

  // turn UrlLink into html link
  private String getSrvLinkString(StatusTable.SrvLink link, int type) {
    return srvLink(link.getServletDescr(),
		   getDisplayString1(link.getValue(), type),
		   link.getArgs());
  }

  // add display attributes from a DisplayedValue
  private String getDisplayString1(Object val, int type) {
    if (val instanceof StatusTable.DisplayedValue) {
      StatusTable.DisplayedValue dval = (StatusTable.DisplayedValue)val;
      Object innerVal = dval.getValue();
      if (innerVal instanceof Collection) {
	return getCollectionDisplayString((Collection)innerVal,
					  dval.getLayout(),
					  type);
      }
      String str = dval.hasDisplayString()
	? HtmlUtil.htmlEncode(dval.getDisplayString())
	: getDisplayString1(innerVal, type);
      String color = dval.getColor();
      java.util.List<String> footnotes = dval.getFootnotes();
      if (color != null) {
	str = "<font color=" + color + ">" + str + "</font>";
      }
      if (dval.getBold()) {
	str = "<b>" + str + "</b>";
      }
      boolean notFirst = false;
      for (String foot : footnotes) {
	str = str + addFootnote(foot, notFirst);
        notFirst = true;
      }
      String hoverText = dval.getHoverText();
      if (!StringUtil.isNullString(hoverText)) {
        str = "<div title=\"" + hoverText + "\">" + str + "</div>";
      }
      return str;
    } else {
      String str = getDisplayConverter().convertDisplayString(val, type);
      if (type == ColumnDescriptor.TYPE_STRING) {
        str = HtmlUtil.htmlEncode(str);
      }
      return str;
    }
  }

  DisplayConverter dispConverter;

  private DisplayConverter getDisplayConverter() {
    if (dispConverter == null) {
      dispConverter = new DisplayConverter();
    }
    return dispConverter;
  }

  private static BitSet debugOptions = new BitSet();
  static {
    debugOptions.set(StatusTable.OPTION_DEBUG_USER);
  }

  /**
   * Build a form with a select box that fetches a named table
   * @return the Composite object
   */
  private Composite getSelectTableForm() {
    try {
      StatusTable statTable =
        statSvc.getTable(StatusService.ALL_TABLES_TABLE, null,
			 isDebugUser() ? debugOptions : null);
      java.util.List colList = statTable.getColumnDescriptors();
      java.util.List rowList = statTable.getSortedRows();
      ColumnDescriptor cd = (ColumnDescriptor)colList.get(0);
      Select sel = new Select("table", false);
      sel.attribute("onchange", "this.form.submit()");
      boolean foundIt = false;
      for (Iterator rowIter = rowList.iterator(); rowIter.hasNext(); ) {
        Map rowMap = (Map)rowIter.next();
        Object val = rowMap.get(cd.getColumnName());
        String display = StatusTable.getActualValue(val).toString();
        if (val instanceof StatusTable.Reference) {
          StatusTable.Reference ref = (StatusTable.Reference)val;
          String key = ref.getTableName();
          // select the current table
          boolean isThis = (tableKey == null) && tableName.equals(key);
          foundIt = foundIt || isThis;
          sel.add(display, isThis, key);
        } else {
          sel.add(display, false);
        }
      }
      // if not currently displaying a table in the list, select a blank entry
      if (!foundIt) {
        sel.add(" ", true, "");
      }
      Form frm = new Form(srvURL(myServletDescr()));
      // use GET so user can refresh in browser
      frm.method("GET");
      frm.add(sel);
      Input submit = new Input(Input.Submit, "foo", "Go");
      submit.attribute("id", "dsSelectBox");
      frm.add(submit);
      return frm;
    } catch (Exception e) {
      // if this fails for any reason, just don't include this form
      log.warning("Failed to build status table selector", e);
      return new Composite();
    }
  }

  protected boolean isAllTablesTable() {
    return StatusService.ALL_TABLES_TABLE.equals(tableName);
  }

  // make me a link in nav table unless I'm displaying table of all tables
  protected boolean linkMeInNav() {
    return !isAllTablesTable();
  }
}
