/*
 * $Id DisplayContentStatus.java 2013/7/02 14:52:00 rwincewicz $
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
package org.lockss.servlet;

import org.apache.commons.lang.time.FastDateFormat;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbUtil;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.status.ColumnDescriptor;
import org.lockss.daemon.status.StatusService;
import org.lockss.daemon.status.StatusTable;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.remote.RemoteApi;
import org.lockss.state.AuState;
import org.lockss.util.*;
import org.mortbay.html.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/**
 * Display Content Status servlet
 */
public class DisplayContentTab extends LockssServlet {
    protected static Logger log = Logger.getLogger("DisplayContentTab");

    /**
     * Supported output formats
     */
    static final int OUTPUT_HTML = 1;
    private String tableName;
    private String tableKey;
    private String sortKey;
    private String groupKey;
    private String typeKey;
    private StatusService statSvc;
    private int outputFmt;
    private List rules;
    private BitSet tableOptions;
    private PluginManager pluginMgr;
    private RemoteApi remoteApi;
    private String action;
    private Character auStart;
    private Character auEnd;
    private String type;

    private ArrayList<String> columnArrayList = new ArrayList<String>() {{
        add("Title");
        add("Year");
        add("ISSN-L");
        add("Collected");
        add("Last crawl");
        add("Last poll");
    }};

    private Page page;

    private static final String OK_ICON = "images/button_ok.png";
    private static final String CANCEL_ICON = "images/button_cancel.png";
    private static final String WARN_ICON = "images/button_warn.png";
    private static final String CLOCK_ICON = "images/button_clock.png";
    private static final String QUESTION_ICON = "images/button_question.png";
    private static final String EXTERNAL_LINK_ICON = "images/external_link.png";

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
     * @throws java.io.IOException
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

        outputFmt = OUTPUT_HTML;    // default output is html

        String outputParam = req.getParameter("output");
        if (!StringUtil.isNullString(outputParam)) {
            if ("html".equalsIgnoreCase(outputParam)) {
                outputFmt = OUTPUT_HTML;
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

        for (String s : StringUtil.breakAt(optionsParam, ',')) {
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
        type = req.getParameter("type");
        if (StringUtil.isNullString(type)) {
            type = "";
        }
        typeKey = req.getParameter("type");
        String auStartString = req.getParameter("start");
        if (auStartString == null) {
            auStart = 'a';
        } else {
            auStart = auStartString.charAt(0);
        }
        String auEndString = req.getParameter("end");
        if (auEndString == null) {
            auEnd = 'a';
        } else {
            auEnd = auEndString.charAt(0);
        }
        switch (outputFmt) {
            case OUTPUT_HTML:
                doHtmlStatusTable();
                break;
        }
    }

    private TreeMap<String, TreeSet<ArchivalUnit>> getAusByPublisherName(Character start, Character end) {
        TreeMap<String, TreeSet<ArchivalUnit>> aus = new TreeMap<String, TreeSet<ArchivalUnit>>();
        TreeMap<String, TreeSet<ArchivalUnit>> allAus =
                DisplayContentTable.orderAusByPublisher(filterAus(TdbUtil.getConfiguredAus()));
        for (int i = start; i < end; i++) {
            for (Map.Entry<String, TreeSet<ArchivalUnit>> stringTreeSetEntry : allAus.entrySet()) {
                Map.Entry<String, TreeSet<ArchivalUnit>> pairs = (Map.Entry) stringTreeSetEntry;
                String publisherName = pairs.getKey().toLowerCase();
                String startLetter = Character.toString((char) i);
                if (publisherName.startsWith(startLetter.toLowerCase())) {
                    aus.put(pairs.getKey(), pairs.getValue());
                }
            }
        }
        return aus;
    }

    private TreeMap<String, TreeSet<ArchivalUnit>> getAusByPluginName(Character start, Character end) {
        TreeMap<String, TreeSet<ArchivalUnit>> aus = new TreeMap<String, TreeSet<ArchivalUnit>>();
        TreeMap<Plugin, TreeSet<ArchivalUnit>> allAus =
                DisplayContentTable.orderAusByPlugin(filterAus(TdbUtil.getConfiguredAus()));
        for (int i = start; i < end; i++) {
        for (Map.Entry<Plugin, TreeSet<ArchivalUnit>> stringTreeSetEntry : allAus.entrySet()) {
            Map.Entry<Plugin, TreeSet<ArchivalUnit>> pairs = (Map.Entry) stringTreeSetEntry;
            String pluginName = pairs.getKey().getPluginName().toLowerCase();
            String startLetter = Character.toString((char) i);
            if (pluginName.startsWith(startLetter.toLowerCase())) {
                aus.put(pairs.getKey().getPluginName(), pairs.getValue());
            }
        }
        }
        return aus;
    }

    private Collection<ArchivalUnit> filterAus(Collection<ArchivalUnit> aus) {
        Collection<ArchivalUnit> filteredAus;
        if ("books".equals(type)) {
            filteredAus = TdbUtil.filterAusByType(aus, TdbUtil.ContentType.BOOKS);
        } else if ("journals".equals(type)) {
            filteredAus = TdbUtil.filterAusByType(aus, TdbUtil.ContentType.JOURNALS);
        } else {
            filteredAus = TdbUtil.filterAusByType(aus, TdbUtil.ContentType.ALL);
        }
        return filteredAus;
    }

    private void doHtmlStatusTable() throws IOException {
        writePage(doHtmlStatusTable0());
    }

    // Build the table, adding elements to page
    private Page doHtmlStatusTable0() throws IOException {
        page = new Page();
        Table divTable = createTabDiv(auStart);
        TreeMap<String, TreeSet<ArchivalUnit>> aus;
        if ("plugin".equals(groupKey)) {
            aus = getAusByPluginName(auStart, auEnd);
        } else {
            aus = getAusByPublisherName(auStart, auEnd);
        }
        for (Map.Entry<String, TreeSet<ArchivalUnit>> entry : aus.entrySet()) {
            createTabContent(divTable, entry.getKey(), entry.getValue());
        }
        addJS("js/jquery-2.0.3.min.js");
        addJS("js/jquery-ui-1.10.3.min.js");
        addJS("js/auDetails.js");
        page.add(divTable);
        return page;
    }

    /**
     * Creates the div for each tab
     *
     * @param letter Start letter of the tab group
     * @return Returns string to add to the page
     */
    private Table createTabDiv(Character letter) {

        Block tabDiv = new Block("div", "id=\"" + letter + "\"");
        Table divTable = new Table(0, "class=\"status-table\" cellspacing=\"0\"");
        divTable.newRow();
        divTable.addCell("");
        log.error("Columns: " + columnArrayList.toString());
        for (String columnArray : columnArrayList) {
            Block columnHeader = new Block(Block.Bold);
            columnHeader.add(columnArray);
            divTable.addCell(columnHeader, "class=\"column-header\"");
        }
        tabDiv.add(divTable);
        Block tabsDiv = new Block(Block.Div, "id=\"tabs\"");
        tabsDiv.add(tabDiv);
//        page.add(tabsDiv);
        return divTable;
    }

    /**
     * Creates the relevant content for each of the tabs
     *
     * @param divTable The table object to add to
     * @param sortName Name of the publisher or plugin
     * @param auSet    TreeSet of archival units
     * @throws java.io.UnsupportedEncodingException
     *
     */
    private static void createTabContent(Table divTable, String sortName,
                                         TreeSet<ArchivalUnit> auSet) throws UnsupportedEncodingException {
        String cleanNameString = cleanName(sortName);
        createTitleRow(divTable, sortName);
        if (auSet != null) {
            int rowCount = 0;
            for (ArchivalUnit au : auSet) {
                createAuRow(divTable, au, cleanNameString, rowCount);
                rowCount++;
            }
        }
    }

    /**
     * Builds the title row for each of the publishers or plugins
     *
     * @param divTable The table object to add to
     * @param sortName Name of the publisher or plugin
     */
    private static void createTitleRow(Table divTable, String sortName) {
        String cleanNameString = cleanName(sortName);
        divTable.newRow();
        Link headingLink = new Link("javascript:showRows('" + cleanNameString
                + "_class', '" + cleanNameString + "_id', '" + cleanNameString
                + "_AUimage')");
        headingLink.attribute("id=\"" + cleanNameString + "_id\"");
        Image headingLinkImage = new Image("images/expand.png");
        headingLinkImage.attribute("id =\"" + cleanNameString + "_AUimage\"");
        headingLinkImage.attribute("class=\"title-icon\"");
        headingLinkImage.attribute("alt", "Expand AU");
        headingLinkImage.attribute("title", "Expand AU");
        headingLink.add(headingLinkImage);
        headingLink.add(sortName);
        Block boldHeadingLink = new Block(Block.Bold);
        boldHeadingLink.add(headingLink);
        divTable.addHeading(boldHeadingLink, "class=\"pub-title\"");
    }

    /**
     * Creates table rows for each AU
     *
     * @param divTable        The table object to add to
     * @param au              Archival unit object for this row
     * @param cleanNameString Sanitised name used to create classes and divs
     * @param rowCount        Row number, used to allocate CSS class
     * @throws UnsupportedEncodingException
     */
    private static void createAuRow(Table divTable, ArchivalUnit au,
                                    String cleanNameString, int rowCount)
            throws UnsupportedEncodingException {
        String rowClass = rowCss(rowCount);
        TdbAu tdbAu = TdbUtil.getTdbAu(au);
        AuState auState = AuUtil.getAuState(au);
        String auName = au.getName();
        String cleanedAuName = cleanAuName(auName);
        String encodedHref = URLEncoder.encode(au.getAuId(), "UTF-8");
        divTable.newRow();
        Block newRow = divTable.row();
        newRow.attribute("class", cleanNameString + "_class hide-row "
                + rowClass);
        Block auDiv = new Block(Block.Div);
        auDiv.attribute("id", cleanedAuName + "_title");
        auDiv.attribute("class", "au-title");
        Link auLink = new Link("DaemonStatus");
        if (tdbAu != null) {
            auLink.attribute("onClick", "updateDiv('" + cleanedAuName + "', '"
                    + au.getAuId() + "', '"
                    + cleanNameString + tdbAu.getYear() + "_image');return false");
            Image auLinkImage = new Image("images/expand.png");
            auLinkImage.attribute("id", cleanNameString + tdbAu.getYear() + "_image");
            auLinkImage.attribute("class", "title-icon");
            auLinkImage.attribute("alt", "Expand Volume");
            auLinkImage.attribute("title", "Expand Volume");
            auLink.add(auLinkImage);
            auLink.add(auName);
            auDiv.add(auLink);
            divTable.addCell(auDiv);
            divTable.addCell(tdbAu.getJournalTitle());
            divTable.addCell(tdbAu.getYear());
            divTable.addCell(tdbAu.getIssn());
            divTable.addCell(checkCollected(au));
            divTable.addCell(dateString(new Date(auState.getLastCrawlTime())));
            divTable.addCell(dateString(new Date(auState.getLastPollStart())));
        } else {
            log.debug("TdbAu not found for Au " + au.getName());
            auLink.attribute("onClick", "updateDiv('" + cleanedAuName + "', '"
                    + au.getAuId() + "', '"
                    + cleanNameString + rowCount + "_image');return false");
            Image auLinkImage = new Image("images/expand.png");
            auLinkImage.attribute("id", cleanNameString + rowCount + "_image");
            auLinkImage.attribute("class", "title-icon");
            auLinkImage.attribute("alt", "Expand Volume");
            auLinkImage.attribute("title", "Expand Volume");
            auLink.add(auLinkImage);
            auLink.add(auName);
            auDiv.add(auLink);
            divTable.addCell(auDiv);
            divTable.addCell(au.getName());
            divTable.addCell("unknown");
            divTable.addCell("unknown");
            divTable.addCell(checkCollected(au));
        }
        Block serveContentDiv = new Block(Block.Div);
        serveContentDiv.attribute("class", "au-serve-content-div");
        Link serveContentLink = new Link("ServeContent?auid=" + encodedHref);
        serveContentLink.target("_blank");
        Image externalLinkImage = new Image(EXTERNAL_LINK_ICON);
        serveContentLink.add("Serve content<sup>" + externalLinkImage + "</sup>");
        serveContentLink.attribute("class", "au-link");
        serveContentDiv.add("[ " + serveContentLink + " ]");
        divTable.addCell(serveContentDiv);
        divTable.newRow("class='hide-row " + rowClass + "' id='" + cleanedAuName + "_row'");
        Block detailsDiv = new Block(Block.Div);
        detailsDiv.attribute("id", cleanedAuName + "_cell");
        detailsDiv.attribute("class", rowClass);
        divTable.addCell(detailsDiv, "colspan='8' id='" + cleanedAuName + "'");
    }

    /**
     * Checks if the AU is collected and returns the relevant image
     *
     * @param au Archival unit
     * @return URL of relevant image
     */
    private static Image checkCollected(ArchivalUnit au) {
        Image collectedImage;
        AuState state = AuUtil.getAuState(au);
        int crawlResult = state.getLastCrawlResult();
        if (crawlResult == Crawler.STATUS_SUCCESSFUL) {
            collectedImage = new Image(OK_ICON);
            collectedImage.attribute("title", "AU has been collected");
            collectedImage.attribute("alt", "AU has been collected");
        } else if (crawlResult == -1 || crawlResult == Crawler.STATUS_QUEUED) {
            collectedImage = new Image(WARN_ICON);
            collectedImage.attribute("title", "AU has not been collected yet");
            collectedImage.attribute("alt", "AU has not been collected yet");
        } else if (crawlResult == Crawler.STATUS_ACTIVE) {
            collectedImage = new Image(CLOCK_ICON);
            collectedImage.attribute("title", "AU is currently being collected");
            collectedImage.attribute("alt", "AU is currently being collected");
        } else if (crawlResult == Crawler.STATUS_ERROR || crawlResult == Crawler.STATUS_FETCH_ERROR ||
                crawlResult == Crawler.STATUS_RUNNING_AT_CRASH) {
            collectedImage = new Image(CANCEL_ICON);
            collectedImage.attribute("title", "An error occurred when collecting AU");
            collectedImage.attribute("alt", "An error occurred when collecting AU");
        } else if (crawlResult == Crawler.STATUS_NO_PUB_PERMISSION) {
            collectedImage = new Image(CANCEL_ICON);
            collectedImage.attribute("title", "Permission error");
            collectedImage.attribute("alt", "Permission error");
        } else {
            collectedImage = new Image(QUESTION_ICON);
            collectedImage.attribute("title", "Unknown - " + crawlResult);
            collectedImage.attribute("alt", "Unknown - " + crawlResult);
        }
        return collectedImage;
    }

    /**
     * Sanitises a string so that it can be used as a div id
     *
     * @param name
     * @return Returns sanitised string
     */
    public static String cleanName(String name) {
        return name.replace(" ", "_").replace("&", "").replace("(", "")
                .replace(")", "");
    }

    public static String cleanAuName(String auName) {
        return auName.replaceAll(" ", "_");
    }

    /**
     * Provides a CSS class based on the row number
     *
     * @param rowCount Row number
     * @return Returns a CSS class for that row
     */
    public static String rowCss(Integer rowCount) {
        return (rowCount % 2 == 0) ? "even-row" : "odd-row";
    }

    // Handle lists
    private String getDisplayString(Object val, int type) {
        if (val instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Object obj : ((List) val)) {
                sb.append(getDisplayString0(obj, type));
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
            for (Map.Entry<Object, Object> objectObjectEntry : refProps.entrySet()) {
                Map.Entry ent = (Map.Entry) objectObjectEntry;
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
            String footnote = aval.getFootnote();
            if (color != null) {
                str = "<font color=" + color + ">" + str + "</font>";
            }
            if (aval.getBold()) {
                str = "<b>" + str + "</b>";
            }
            if (footnote != null) {
                str = str + addFootnote(footnote);
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
                    //       if (fmt instanceof DecimalFormat) {
//         ((DecimalFormat)fmt).setDecimalSeparatorAlwaysShown(true);
//       }
                    return NumberFormat.getInstance();
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

    /**
     * Adds javascript to the page based on the URL provided
     *
     * @param jsLocation URL of javascript file
     */
    private void addJS(String jsLocation) {
        Script ajaxScript = new Script("");
        ajaxScript.attribute("src", jsLocation);
        ajaxScript.attribute("type", "text/javascript");
        page.addHeader(ajaxScript);
    }
}
