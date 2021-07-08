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
import java.net.URLDecoder;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.time.FastDateFormat;
import org.lockss.config.*;
import org.lockss.daemon.status.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.remote.RemoteApi;
import org.lockss.util.*;
import org.mortbay.html.*;
import org.w3c.dom.Document;

/**
 * Display Content Status servlet
 */
public class DisplayContentStatus extends LockssServlet {
  
  private static final Logger log = Logger.getLogger(DisplayContentStatus.class);

  public static final String AU_TO_REMOVE = "removeAu";
  /**
   * Supported output formats
   */
  static final int OUTPUT_HTML = 1;
  static final int OUTPUT_TEXT = 2;
  static final int OUTPUT_XML = 3;
  static final int OUTPUT_CSV = 4;
  private String tableName;
  private String tableKey;
  private String sortKey;
  private String groupKey;
  private String typeKey;
  private String filterKey;
  private String tabKey;
  private String timeKey;
  private StatusService statSvc;
  private int outputFmt;
  private java.util.List rules;
  private BitSet tableOptions;
  private PluginManager pluginMgr;
  private RemoteApi remoteApi;
  private String action;
  private String auName;
  private String deleteMessage;

  protected void resetLocals() {
    super.resetLocals();
    rules = null;
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    statSvc = getLockssDaemon().getStatusService();
    pluginMgr = getLockssDaemon().getPluginManager();
    remoteApi = getLockssDaemon().getRemoteApi();
  }
  static final Set fixedParams =
          SetUtil.set("text", "output", "options", "table", "key", "sort");

  /**
   * Handle a request
   *
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

    action = req.getParameter(ACTION_TAG);
    auName = req.getParameter(AU_TO_REMOVE);
    if ("Delete selected".equals(req.getParameter("submit"))) {
      String[] deleteAUs = req.getParameterValues("deleteAu");
      if (deleteAUs != null) {
        log.error("AUs: " + Arrays.asList(deleteAUs));
        doRemoveAus(Arrays.asList(deleteAUs));
      } else {
        log.error("No AUs selected");
        deleteMessage = "No AUs selected!";
      }
    }

    String publisher = req.getParameter("deletePublisher");
      if (!StringUtil.isNullString(publisher)) {
        TreeMap<String, TreeMap<String, TreeSet<ArchivalUnit>>> auMap = DisplayContentTab.getAusByPublisherName();
          ArrayList<String> auIds = new ArrayList<String>();
          if (auMap.containsKey(publisher)) {
           Iterator it = auMap.entrySet().iterator();
              while (it.hasNext()) {
                  Map.Entry pairs = (Map.Entry)it.next();
                  String publisherString = pairs.getKey().toString();
                  log.error("Publisher: " + publisher);
                  log.error("Publisher string: " + publisherString);
                  if (publisher.equals(publisherString)) {
                    TreeMap<String, TreeSet<ArchivalUnit>> titleMap =
                            (TreeMap<String, TreeSet<ArchivalUnit>>)pairs.getValue();
                    Iterator titleIterator = titleMap.entrySet().iterator();
                      while (titleIterator.hasNext()) {
                          Map.Entry titlePairs = (Map.Entry)titleIterator.next();
                          TreeSet<ArchivalUnit> auSet = (TreeSet<ArchivalUnit>)titlePairs.getValue();
                          for (ArchivalUnit au : auSet) {
                            auIds.add(au.getAuId());
                          }
                      }
                  }
              }
            doRemoveAus(auIds);
          }
      }

    if (action != null && auName != null) {
      String auString = URLDecoder.decode(auName, "UTF-8");
      java.util.List<String> auList = new ArrayList<String>();
      auList.add(auString);
      doRemoveAus(auList);
    }

    if (StringUtil.isNullString(action)) {
      try {
        getMultiPartRequest();
        if (multiReq != null) {
          action = multiReq.getString(ACTION_TAG);
        }
      } catch (FormDataTooLongException e) {
        errMsg = "Uploaded file too large: " + e.getMessage();
        // leave action null, will call displayAuSummary() below
      }
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
            iter.hasNext();) {
      String s = (String) iter.next();
      if ("norows".equalsIgnoreCase(s)) {
        tableOptions.set(StatusTable.OPTION_NO_ROWS);
      }
    }

    tableName = req.getParameter("table");
    tableKey = req.getParameter("key");
    if (StringUtil.isNullString(tableName)) {
      tableName = "AuOverview";
    }
    if (StringUtil.isNullString(tableKey)) {
      tableKey = null;
    }
    sortKey = req.getParameter("sort");
    if (StringUtil.isNullString(sortKey)) {
      sortKey = null;
    }
    groupKey = req.getParameter("group");
    if (StringUtil.isNullString(groupKey)) {
      groupKey = "publisher";
    }
    typeKey = req.getParameter("type");
    filterKey = req.getParameter("filterKey");
    tabKey = req.getParameter("tab");
    timeKey = req.getParameter("timeKey");

    switch (outputFmt) {
      case OUTPUT_HTML:
        doHtmlStatusTable();
        break;
      case OUTPUT_XML:
        try {
          doXmlStatusTable();
        } catch (Exception e) {
          throw new IOException("Error building XML", e);
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
      page.add(ServletUtil.removeElementWithId("dsSelectBox"));
    }

    //       page.add("<center>");
    //       page.add(srvLink(SERVLET_DAEMON_STATUS, ".",
    // 		       concatParams("text=1", req.getQueryString())));
    //       page.add("</center><br><br>");
//    page.add(deleteMessage);
    deleteMessage = null;
    DisplayContentTable content = new DisplayContentTable(page, groupKey, 
            typeKey, filterKey, timeKey);
//    Script selectTab = new Script("$( \"#tabs\" ).tabs(\"select\", 3);");
//    page.addHeader(selectTab);
    return page;
  }

  private void doRemoveAus(java.util.List aus) throws IOException {
    remoteApi.deleteAus(aus);
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
      vPlatform = ", platform="
              + StringUtil.ckvEscape(pVer.displayString());
    } else {
      vPlatform = "";
    }
    Date now = new Date();
    Date startDate = getLockssDaemon().getStartDate();
    wrtr.println("host=" + getLcapIPAddr()
            + ",time=" + now.getTime()
            + ",up=" + TimeBase.msSince(startDate.getTime())
            + ",version=" + BuildInfo.getBuildInfoString()
            + vPlatform);
    doTextStatusTable(wrtr);
  }

  private StatusTable makeTable() throws StatusService.NoSuchTableException {
    StatusTable table = new StatusTable(tableName, tableKey);
    table.setOptions(tableOptions);
    for (Enumeration en = req.getParameterNames(); en.hasMoreElements();) {
      String name = (String) en.nextElement();
      if (!fixedParams.contains(name)) {
        table.setProperty(name, req.getParameter(name));
      }
    }
    statSvc.fillInTable(table);
    return table;
  }

  // build and send an XML DOM Document of the StatusTable
  private void doXmlStatusTable() throws Exception {
    // By default, XmlDomBuilder will produce UTF-8.  Must set content type
    // *before* calling getWriter()
    resp.setContentType("text/xml; charset=UTF-8");
    PrintWriter wrtr = resp.getWriter();

    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

    Document doc = docBuilder.newDocument();
    org.w3c.dom.Element rootElement = doc.createElement("root");
    doc.appendChild(rootElement);

    org.w3c.dom.Element notImplemented = doc.createElement("notImplemented");
    rootElement.appendChild(notImplemented);

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    javax.xml.transform.Transformer transformer = 
            transformerFactory.newTransformer();
    DOMSource domSource = new DOMSource(doc);
    StringWriter stringWriter = new StringWriter();
    StreamResult streamResult = new StreamResult(stringWriter);
    transformer.transform(domSource, streamResult);

    wrtr.append(stringWriter.toString());
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
    page = newTablePage();
    if (tabKey != null) {
      Script tabSelect = new Script("$(document).ready(function () {$(\"#tabs\").tabs({ active: " + tabKey + " })});");
      page.addHeader(tabSelect);
    }
    return page;
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
      for (Iterator iter = summary.iterator(); iter.hasNext();) {
        StatusTable.SummaryInfo sInfo = (StatusTable.SummaryInfo) iter.next();
        wrtr.print(",");
        wrtr.print(sInfo.getTitle());
        wrtr.print("=");
        Object dispVal = getTextDisplayString(sInfo.getValue());
        String valStr = dispVal != null ? dispVal.toString() : "(null)";
        wrtr.print(StringUtil.ckvEscape(valStr));
      }
    }
    wrtr.println();

    // Create (but do not yet refer to) any footnotes that the table wants
    // to be in a specific order
    for (String orderedFoot : statTable.getOrderedFootnotes()) {
      addFootnote(orderedFoot);
    }
    java.util.List rowList = getRowList(statTable);
    if (rowList != null) {
      // output rows
      for (Iterator rowIter = rowList.iterator(); rowIter.hasNext();) {
        Map rowMap = (Map) rowIter.next();
        for (Iterator iter = rowMap.keySet().iterator(); iter.hasNext();) {
          Object o = iter.next();
          if (!(o instanceof String)) {
            // ignore special markers (eg, StatusTable.ROW_SEPARATOR)
            continue;
          }
          String key = (String) o;
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
      for (Iterator colIter = colList.iterator(); colIter.hasNext();) {
        ColumnDescriptor cd = (ColumnDescriptor) colIter.next();
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
          for (Iterator colIter = colList.iterator(); colIter.hasNext();) {
            ColumnDescriptor cd = (ColumnDescriptor) colIter.next();
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
  static final Image DOWNARROW1 = ServletUtil.image("downarrow1blue.gif", 16, 16,
          0, "Primary sort column, descending");
  static final Image DOWNARROW2 = ServletUtil.image("downarrow2blue.gif", 16, 16,
          0, "Secondary sort column, descending");

  /**
   * Create a column heading element:<ul> <li> plain text if not sortable <li>
   * if sortable, link with sortkey set to column name, descending if was
   * previous primary ascending key <li> plus a possible secondary sort key set
   * to previous sort column <li> if is current primary or secondary sort
   * column, display an up or down arrow.</ul>
   *
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
        StatusTable.SortRule rule1 = (StatusTable.SortRule) rules.get(0);
        if (cd.getColumnName().equals(rule1.getColumnName())) {
          // This column is the current primary sort; link to reverse order
          ascending = !rule1.sortAscending();
          // and display a primary arrow
          sortArrow = rule1.sortAscending() ? UPARROW1 : DOWNARROW1;
          if (rules.size() > 1) {
            // keep same secondary sort key if there was one
            StatusTable.SortRule rule2 = (StatusTable.SortRule) rules.get(1);
            ruleParam = ruleParam(cd, ascending) + "," + ruleParam(rule2);
          } else {
            ruleParam = ruleParam(cd, ascending);
          }
        } else {
          if (rules.size() > 1) {
            StatusTable.SortRule rule2 = (StatusTable.SortRule) rules.get(1);
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
    for (Iterator iter = cols.iterator(); iter.hasNext();) {
      String spec = (String) iter.next();
      boolean ascending = spec.charAt(0) == 'A';
      String col = spec.substring(1);
      StatusTable.SortRule defaultRule =
              getDefaultRuleForColumn(statTable, col);
      StatusTable.SortRule rule;
      Comparator comparator = null;
      if (columnDescriptorMap.containsKey(col)) {
        ColumnDescriptor cd = (ColumnDescriptor) columnDescriptorMap.get(col);
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
    for (Iterator iter = defaults.iterator(); iter.hasNext();) {
      StatusTable.SortRule rule = (StatusTable.SortRule) iter.next();
      if (col.equals(rule.getColumnName())) {
        return rule;
      }
    }
    return null;
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
    if (val instanceof java.util.List) {
      StringBuilder sb = new StringBuilder();
      for (Iterator iter = ((java.util.List) val).iterator(); iter.hasNext();) {
        sb.append(StatusTable.getActualValue(iter.next()));
      }
      return sb.toString();
    } else {
      Object dispVal = StatusTable.getActualValue(val);
      return dispVal != null ? dispVal.toString() : "(null)";
    }
  }

  // Handle lists
  private String getDisplayString(Object val, int type) {
    if (val instanceof java.util.List) {
      StringBuilder sb = new StringBuilder();
      for (Iterator iter = ((java.util.List) val).iterator(); iter.hasNext();) {
        sb.append(getDisplayString0(iter.next(), type));
      }
      return sb.toString();
    } else {
      return getDisplayString0(val, type);
    }
  }

  // Process References and other links
  private String getDisplayString0(Object val, int type) {
    if (val instanceof StatusTable.Reference) {
      return getRefString((StatusTable.Reference) val, type);
    } else if (val instanceof StatusTable.SrvLink) {
      // Display as link iff user is allowed access to the target servlet
      StatusTable.SrvLink slink = (StatusTable.SrvLink) val;
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
      for (Iterator iter = refProps.entrySet().iterator(); iter.hasNext();) {
        Map.Entry ent = (Map.Entry) iter.next();
        sb.append("&");
        sb.append(ent.getKey());
        sb.append("=");
        sb.append(urlEncode((String) ent.getValue()));
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
      StatusTable.DisplayedValue aval = (StatusTable.DisplayedValue) val;
      String str = aval.hasDisplayString()
              ? HtmlUtil.htmlEncode(aval.getDisplayString())
              : getDisplayString1(aval.getValue(), type);
      String color = aval.getColor();
      java.util.List<String> footnotes = aval.getFootnotes();
      if (color != null) {
        str = "<font color=" + color + ">" + str + "</font>";
      }
      if (aval.getBold()) {
        str = "<b>" + str + "</b>";
      }
      boolean notFirst = false;
      for (String foot : footnotes) {
	str = str + addFootnote(foot, notFirst);
        notFirst = true;
      }
      String hoverText = aval.getHoverText();
      if (!StringUtil.isNullString(hoverText)) {
        str = "<div title=\"" + hoverText + "\">" + str + "</div>";
      }
      return str;
    } else {
      String str = convertDisplayString(val, type);
      if (type == ColumnDescriptor.TYPE_STRING) {
        str = HtmlUtil.htmlEncode(str);
      }
      return str;
    }
  }
  // Thread-safe formatters.
  // FastDateFormat is thread-safe, NumberFormat & subclasses aren't.
  /**
   * Format to display date/time in tables
   */
  private static final Format tableDf =
          FastDateFormat.getInstance("HH:mm:ss MM/dd/yy");

//   public static final DateFormat tableDf =
//     DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
  static Format getTableDateFormat() {
    return tableDf;
  }
  private static final ThreadLocal<NumberFormat> agmntFmt =
          new ThreadLocal<NumberFormat>() {
            @Override
            protected NumberFormat initialValue() {
              return new DecimalFormat("0.00");
            }
          };

  static NumberFormat getAgreementFormat() {
    return agmntFmt.get();
  }
  private static final ThreadLocal<NumberFormat> floatFmt =
          new ThreadLocal<NumberFormat>() {
            @Override
            protected NumberFormat initialValue() {
              return new DecimalFormat("0.0");
            }
          };

  static NumberFormat getFloatFormat() {
    return floatFmt.get();
  }
  private static final ThreadLocal<NumberFormat> bigIntFmt =
          new ThreadLocal<NumberFormat>() {
            @Override
            protected NumberFormat initialValue() {
              NumberFormat fmt = NumberFormat.getInstance();
//       if (fmt instanceof DecimalFormat) {
//         ((DecimalFormat)fmt).setDecimalSeparatorAlwaysShown(true);
//       }
              return fmt;
            }
          };

  static NumberFormat getBigIntFormat() {
    return bigIntFmt.get();
  }

  /* DecimalFormat automatically applies half-even rounding to
   * values being formatted under Java < 1.6.  This is a workaround. */
  private static String doubleToPercent(double d) {
    int i = (int) (d * 10000);
    double pc = i / 100.0;
    return getAgreementFormat().format(pc);
  }

  // turn a value into a display string
  public static String convertDisplayString(Object val, int type) {
    if (val == null) {
      return "";
    }
    try {
      switch (type) {
        case ColumnDescriptor.TYPE_INT:
          if (val instanceof Number) {
            long lv = ((Number) val).longValue();
            if (lv >= 1000000) {
              return getBigIntFormat().format(lv);
            }
          }
        // fall thru
        case ColumnDescriptor.TYPE_STRING:
        default:
          return val.toString();
        case ColumnDescriptor.TYPE_FLOAT:
          return getFloatFormat().format(((Number) val).doubleValue());
        case ColumnDescriptor.TYPE_PERCENT:
          float fv = ((Number) val).floatValue();
          return Integer.toString(Math.round(fv * 100)) + "%";
        case ColumnDescriptor.TYPE_AGREEMENT:
          float av = ((Number) val).floatValue();
          return doubleToPercent(av) + "%";
        case ColumnDescriptor.TYPE_DATE:
          Date d;
          if (val instanceof Number) {
            d = new Date(((Number) val).longValue());
          } else if (val instanceof Date) {
            d = (Date) val;
          } else if (val instanceof Deadline) {
            d = ((Deadline) val).getExpiration();
          } else {
            return val.toString();
          }
          return dateString(d);
        case ColumnDescriptor.TYPE_IP_ADDRESS:
          return ((IPAddr) val).getHostAddress();
        case ColumnDescriptor.TYPE_TIME_INTERVAL:
          long millis = ((Number) val).longValue();
          return StringUtil.timeIntervalToString(millis);
      }
    } catch (NumberFormatException e) {
      log.warning("Bad number: " + val.toString(), e);
      return val.toString();
    } catch (ClassCastException e) {
      log.warning("Wrong type value: " + val.toString(), e);
      return val.toString();
    } catch (Exception e) {
      log.warning("Error formatting value: " + val.toString(), e);
      return val.toString();
    }
  }

  public static String dateString(Date d) {
    long val = d.getTime();
    if (val == 0 || val == -1) {
      return "never";
    } else {
      return getTableDateFormat().format(d);
    }
  }
  private static BitSet debugOptions = new BitSet();

  static {
    debugOptions.set(StatusTable.OPTION_DEBUG_USER);
  }

  protected boolean isAllTablesTable() {
    return StatusService.ALL_TABLES_TABLE.equals(tableName);
  }

  // make me a link in nav table unless I'm displaying table of all tables
  protected boolean linkMeInNav() {
    return !isAllTablesTable();
  }
}
