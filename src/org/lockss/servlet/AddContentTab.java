/*
 * $Id DisplayContentStatus.java 2013/7/02 14:52:00 rwincewicz $
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
package org.lockss.servlet;

import org.apache.commons.lang3.time.FastDateFormat;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbUtil;
import org.lockss.daemon.TitleConfig;
import org.lockss.daemon.TitleSet;
import org.lockss.daemon.status.ColumnDescriptor;
import org.lockss.daemon.status.StatusService;
import org.lockss.daemon.status.StatusTable;
import org.lockss.plugin.*;
import org.lockss.remote.RemoteApi;
import org.lockss.state.AuState;
import org.lockss.util.*;
import org.mortbay.html.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/**
 * Display Content Status servlet
 */
public class AddContentTab extends LockssServlet {
    
  private static final Logger log = Logger.getLogger(AddContentTab.class);

    /**
     * Supported output formats
     */
    static final int OUTPUT_HTML = 1;
    private String tableName;
    private String tableKey;
    private String sortKey;
    private String groupKey;
    private String typeKey;
    private String filterKey;
    private String timeKey;
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

    private Page page;

//    private static final String OK_ICON = "images/button_ok.png";
//    private static final String CANCEL_ICON = "images/button_cancel.png";
//    private static final String WARN_ICON = "images/button_warn.png";
//    private static final String CLOCK_ICON = "images/button_clock.png";
//    private static final String QUESTION_ICON = "images/button_question.png";
    private static final String EXTERNAL_LINK_ICON = "images/external_link.png";
    private static final String EXPAND_ICON = "images/extend-right.gif";
//    private static final String COLLAPSE_ICON = "images/expanded.gif";
    private static final String PLUS_ICON = "images/control_add.png";
    private static final String DELETE_ICON = "images/decline.png";
    private static final String CLOSE_ICON = "images/button_cancel_bw.png";

    private ArrayList<String> columnArrayList = new ArrayList<String>() {{
        add("");
        add("");
        add("Year");
        add("Collected");
        add("Last crawl");
        add("Last poll");
        add("");
        add("");
    }};

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
        filterKey = req.getParameter("filter");
        if (StringUtil.isNullString(filterKey)) {
            filterKey = "";
        }
        timeKey = req.getParameter("timeKey");
        if (StringUtil.isNullString(timeKey)) {
            timeKey = "";
        }
        switch (outputFmt) {
            case OUTPUT_HTML:
                doHtmlStatusTable();
                break;
        }
    }

    private void doHtmlStatusTable() throws IOException {
        writePage(doHtmlStatusTable0());
    }

    // Build the table, adding elements to page
    private Page doHtmlStatusTable0() throws IOException {
        page = new Page();
        addJS("js/DisplayContentTab.js");
        HttpSession session = getSession();
        Object actionMessage = session.getAttribute("actionMessage");
        if (actionMessage != null) {
            Block messageDiv = new Block(Block.Div);
            Image closeMessageImage = new Image(CLOSE_ICON);
            closeMessageImage.alt("Close message bar");
            closeMessageImage.attribute("title", "Close message bar");
            closeMessageImage.attribute("class", "messageDivClose");
            closeMessageImage.attribute("onclick", "hideMessages();");
            messageDiv.add(closeMessageImage);
            messageDiv.attribute("class", "messageDiv");
            Block messageSpan = new Block(Block.Span);
            messageSpan.add(actionMessage.toString());
            messageDiv.add(messageSpan);
            session.setAttribute("actionMessage", null);
            page.add(messageDiv);
        }
        Table divTable = createTabDiv(auStart);
        Collection<TitleSet> sets = getTitleSetsByName(auStart, auEnd, pluginMgr);
        for (TitleSet ts : sets) {
            if (!"All active AUs".equals(ts.getName()) && !"All inactive AUs".equals(ts.getName())) {
                String cleanNameString = cleanName(ts.getName());
                Collection<TitleConfig> titles = ts.getTitles();
                TreeMap<String, List<TitleConfig>> titleMap = orderTitleConfigByTitle(titles);
                createTitleRow(divTable, ts.getName(), titleMap.size());
                createAuRow(divTable, titleMap, cleanNameString);
                addClearRow(divTable, cleanNameString, false);
            }
        }
        Form tabForm = new Form();
        tabForm.method("POST");
        tabForm.attribute("id", "submitForm");
        tabForm.add(divTable);
        if (sets.size() > 0) {
            Input formAdd = new Input("submit", "addSubmit");
            formAdd.attribute("value", "Add selected");
            formAdd.attribute("id", "add-submit");
            formAdd.attribute("class", "submit-button");
            formAdd.attribute("onclick", "button='add'");
            tabForm.add(formAdd);
            Input formDelete = new Input("submit", "deleteSubmit");
            formDelete.attribute("value", "Delete selected");
            formDelete.attribute("id", "delete-submit");
            formDelete.attribute("class", "submit-button");
            formDelete.attribute("onclick", "button='delete'");
            tabForm.add(formDelete);
        }
        page.add(tabForm);
        return page;
    }

    public static Collection<TitleSet> getTitleSetsByName(Character startLetter, Character endLetter, PluginManager pluginManager) {
        Collection<TitleSet> titleSets = pluginManager.getTitleSets();
        Collection<TitleSet> filteredTitleSets = new ArrayList<TitleSet>();
        for (TitleSet ts : titleSets) {
//            if (ts.countTitles(TitleSet.SET_ADDABLE) > 0) {
                String titleName = ts.getName().substring(4, ts.getName().length()).toUpperCase();
                if (titleName.charAt(0) >= startLetter && titleName.charAt(0) <= endLetter) {
                    filteredTitleSets.add(ts);
                }
//            }
        }
        return filteredTitleSets;
    }

    public static TreeMap<String, List<TitleConfig>> orderTitleConfigByTitle(Collection<TitleConfig> allTitles) {
        TreeMap<String, List<TitleConfig>> titleMap = new TreeMap<String, List<TitleConfig>>();
        for (TitleConfig tc : allTitles) {
                String titleString = tc.getJournalTitle();
                List<TitleConfig> titleList;
                if (titleMap.containsKey(titleString)) {
                    titleList = titleMap.get(titleString);
                    titleList.add(tc);
                } else {
                    titleList = new ArrayList<TitleConfig>();
                    titleList.add(tc);
                }
                titleMap.put(titleString, titleList);
            }
        return titleMap;
    }

    /**
     * Creates the div for each tab
     *
     * @param letter Start letter of the tab group
     * @return Returns string to add to the page
     */
    private static Table createTabDiv(Character letter) {

        Block tabDiv = new Block(Block.Div, "id='" + letter + "'");
        Table divTable = new Table(0, "class='status-table' cellspacing='0'");
        tabDiv.add(divTable);
        Block tabsDiv = new Block(Block.Div, "id='tabs'");
        tabsDiv.add(tabDiv);
        return divTable;
    }

    /**
     * Builds the title row for each of the publishers or plugins
     *
     * @param divTable The table object to add to
     * @param sortName Name of the publisher or plugin
     */
    private void createTitleRow(Table divTable, String sortName, Integer auCount) {
        String cleanNameString = cleanName(sortName);
        sortName = sortName.replaceAll("All ", "").replaceAll(" AUs", "");
        divTable.newRow();
        Link headingLink = new Link("javascript:showRows('" + cleanNameString
                + "_class', '" + cleanNameString + "_id', '" + cleanNameString
                + "_AUimage', '" + cleanNameString + "')");
        headingLink.attribute("id='" + cleanNameString + "_id'");
        headingLink.attribute("class='pub-title'");
        Image headingLinkImage = new Image(PLUS_ICON);
        headingLinkImage.attribute("id ='" + cleanNameString + "_AUimage'");
        headingLinkImage.attribute("class='publisher-icon'");
        headingLinkImage.attribute("alt", "Expand Title");
        headingLinkImage.attribute("title", "Expand Title");
        headingLink.add(headingLinkImage);
        headingLink.add(HtmlUtil.encode(sortName, HtmlUtil.ENCODE_TEXT) + getTitleCountString(auCount));
        Block boldHeadingLink = new Block(Block.Bold);
        boldHeadingLink.add(headingLink);
        Form removePubForm = new Form();
        Input removePubButton = new Input(Input.Image, "deletePublisher");
        removePubButton.attribute("value", HtmlUtil.encode(sortName, HtmlUtil.ENCODE_TEXT));
        removePubButton.attribute("onclick", "return confirm('Confirm deletion of AUs associated with the publisher " + HtmlUtil.encode(sortName, HtmlUtil.ENCODE_TEXT) + "');");
        Block pubDiv = new Block(Block.Div);
        pubDiv.add(boldHeadingLink);
        Block pubSpan = new Block(Block.Span);
        removePubButton.attribute("class='remove-pub-button'");
        removePubButton.attribute("src", DELETE_ICON);
        removePubButton.attribute("title", "Delete publisher");
        pubDiv.add(pubSpan);
        pubDiv.add(removePubButton);
        removePubForm.add(pubDiv);
        divTable.addHeading(removePubForm, "class='pub-title' colspan='8'");
        addClearRow(divTable, cleanNameString, true);
    }

    private static String getTitleCountString(Integer auCount) {
        StringBuilder titleCountString = new StringBuilder("title");
        if (auCount != 1) {
            titleCountString.append("s");
        }
        return " (" + auCount + " " + titleCountString.toString() + ")";
    }

    private void createAuRow(Table divTable, TreeMap<String, List<TitleConfig>> titleConfigs, String cleanNameString)
            throws UnsupportedEncodingException {
        for (Map.Entry<String, List<TitleConfig>> pairs : titleConfigs.entrySet()) {
            String title = pairs.getKey();
            String cleanTitleString = cleanName(title);
            List<TitleConfig> configList = pairs.getValue();
            divTable.newRow();
            Block columnDiv = divTable.row();
            columnDiv.attribute("id", cleanName(title) + "_column");
            columnDiv.attribute("class", "journal-title " + cleanNameString + "_class hide-row");
            for (String columnArray : columnArrayList) {
                Block columnHeader = new Block(Block.Bold);
                columnHeader.add(columnArray);
                divTable.addCell(columnHeader, "class=\"column-header\"");
            }
            divTable.newRow();
            Block titleDiv = divTable.row();
            titleDiv.attribute("id", cleanName(title) + "_title");
            titleDiv.attribute("class", "journal-title " + cleanNameString + "_class hide-row");
            Table titleTable = new Table();
            titleTable.attribute("class", "title-table");
            Input titleCheckbox = new Input(Input.Checkbox, cleanNameString + "_checkbox");
            titleCheckbox.attribute("onClick", "titleCheckbox(this, \"" + cleanTitleString + "\")");
            titleCheckbox.attribute("class", "chkbox");
            Block titleCheckboxDiv = new Block(Block.Div);
            titleCheckboxDiv.attribute("class", "checkboxDiv");
            titleCheckboxDiv.add(titleCheckbox);
            divTable.addCell(titleCheckboxDiv, "class=\"checkboxDiv\"");
            addTitleRow(titleTable, "Title:", HtmlUtil.encode(title, HtmlUtil.ENCODE_TEXT));
            addTitleRow(titleTable, "Print ISSN:", configList.get(0).getTdbAu().getIssn());
            addTitleRow(titleTable, "E-ISSN:", configList.get(0).getTdbAu().getEissn());
            divTable.addCell(titleTable, "colspan=\"7\"");
            int rowCount = 0;
            for (TitleConfig tc : configList) {
                String rowClass = rowCss(rowCount);
                TdbAu tdbAu = tc.getTdbAu();
                ArchivalUnit au = pluginMgr.getAuFromId(tc.getAuId(pluginMgr));
                String auName = HtmlUtil.encode(tc.getDisplayName(), HtmlUtil.ENCODE_TEXT);
                String cleanedAuName = cleanAuName(auName);
                divTable.newRow();
                Block newRow = divTable.row();
                newRow.attribute("class", cleanNameString + "_class hide-row "
                        + rowClass);
                if (au == null) {
                if (tdbAu != null) {
                    createInactiveRow(divTable, auName, cleanTitleString, tc);
                } else {
                    createEmptyRow(divTable, auName, cleanNameString, rowCount, tc);
                }
                } else {
                    createActiveRow(divTable, auName, cleanNameString, cleanTitleString, au);
                }
                divTable.newRow("class='hide-row " + rowClass + " " + cleanNameString + "-au' id='" + cleanedAuName + "_row'");
                Block detailsDiv = new Block(Block.Div);
                detailsDiv.attribute("id", cleanedAuName + "_cell");
                detailsDiv.attribute("class", rowClass);
                divTable.addCell(detailsDiv, "colspan='8' id='" + cleanedAuName + "'");
            }
            addClearRow(divTable, cleanNameString, true);
        }
    }

    private void createEmptyRow(Table divTable, String auName, String cleanNameString, Integer rowCount, TitleConfig tc) {
        String cleanedAuName = cleanAuName(auName);
        Block auDiv = new Block(Block.Div);
        auDiv.attribute("id", cleanedAuName + "_au_title");
        auDiv.attribute("class", "au-title");
        Link auLink = new Link("DaemonStatus");
        log.debug("TdbAu not found for Au " + tc.getDisplayName());
                    auLink.attribute("onClick", "updateDiv('" + cleanedAuName + "', '"
                            + tc.getAuId(pluginMgr) + "', '" + cleanNameString + "-au', '"
                            + cleanNameString + rowCount + "_image');return false");
                    Image auLinkImage = new Image(EXPAND_ICON);
                    auLinkImage.attribute("id", cleanNameString + rowCount + "_image");
                    auLinkImage.attribute("class", "title-icon");
                    auLinkImage.attribute("alt", "Expand Volume");
                    auLinkImage.attribute("title", "Expand Volume");
                    auLink.add(auLinkImage);
                    auLink.add(auName);
                    auDiv.add(auLink);
                    divTable.addCell(auDiv);
                    divTable.addCell(tc.getDisplayName());
    }

    private void createInactiveRow(Table divTable, String auName, String cleanTitleString, TitleConfig tc) {
        String cleanedAuName = cleanAuName(auName);
        TdbAu tdbAu = tc.getTdbAu();
        Block auDiv = new Block(Block.Div);
        auDiv.attribute("id", cleanedAuName + "_au_title");
        auDiv.attribute("class", "au-title");
        auDiv.add(auName);
        Block checkboxDiv = new Block(Block.Div);
        checkboxDiv.attribute("class", "checkboxDiv");
        Input checkbox = new Input("checkbox", "au");
        checkbox.attribute("value", tc.getAuId(pluginMgr));
        checkbox.attribute("class", "chkbox " + cleanTitleString + "_chkbox");
        checkboxDiv.add(checkbox);
        divTable.addCell(checkboxDiv, "class='checkboxDiv'");
        divTable.addCell(auDiv, "class='au-title-cell'");
        divTable.addCell(tdbAu.getYear());
        divTable.addCell("");
        divTable.addCell("");
        divTable.addCell("");
        divTable.addCell("");
        divTable.addCell("");
    }

    private static void createActiveRow(Table divTable, String auName, String cleanNameString, String cleanTitleString,
                                        ArchivalUnit au) throws UnsupportedEncodingException {
        String cleanedAuName = cleanAuName(auName);
        String encodedHref = URLEncoder.encode(au.getAuId(), "UTF-8");
        TdbAu tdbAu = au.getTdbAu();
        AuState auState = AuUtil.getAuState(au);
        Block auDiv = new Block(Block.Div);
        auDiv.attribute("id", cleanedAuName + "_au_title");
        auDiv.attribute("class", "au-title");
        Link auLink = new Link("DaemonStatus");
        auLink.attribute("onClick", "updateDiv('" + cleanedAuName + "', '"
                + au.getAuId() + "', '" + cleanNameString + "-au', '"
                + cleanNameString + tdbAu.getYear() + "_image');return false");
        Image auLinkImage = new Image(EXPAND_ICON);
        auLinkImage.attribute("id", cleanNameString + tdbAu.getYear() + "_image");
        auLinkImage.attribute("class", "title-icon");
        auLinkImage.attribute("alt", "Expand Volume");
        auLinkImage.attribute("title", "Expand Volume");
        auLink.add(auLinkImage);
        auLink.add(auName);
        auDiv.add(auLink);
        Block checkboxDiv = new Block(Block.Div);
        checkboxDiv.attribute("class", "checkboxDiv");
        Input checkbox = new Input("checkbox", "au");
        checkbox.attribute("value", au.getAuId());
        checkbox.attribute("class", "chkbox " + cleanTitleString + "_chkbox");
        checkboxDiv.add(checkbox);
        divTable.addCell(checkboxDiv, "class='checkboxDiv'");
        divTable.addCell(auDiv, "class='au-title-cell'");
        divTable.addCell(tdbAu.getYear());
        divTable.addCell(DisplayContentTab.checkCollected(au, auState.getLastCrawlTime()));
        divTable.addCell(DisplayContentTab.showTimeElapsed(auState.getLastCrawlTime(), ""));
        divTable.addCell(DisplayContentTab.showTimeElapsed(auState.getLastPollStart(), ""));
        Block serveContentDiv = new Block(Block.Div);
        serveContentDiv.attribute("class", "au-serve-content-div");
        Link serveContentLink = new Link("ServeContent?auid=" + encodedHref);
        serveContentLink.target("_blank");
        Image externalLinkImage = new Image(EXTERNAL_LINK_ICON);
        serveContentLink.add("Serve content<sup>" + externalLinkImage + "</sup>");
        serveContentLink.attribute("class", "au-link");
        serveContentDiv.add("[ " + serveContentLink + " ]");
        divTable.addCell(serveContentDiv, "colspan=\"2\"");
    }

    private static void addClearRow(Table table, String titleString, Boolean hidden) {
        table.newRow();
        StringBuilder classString = new StringBuilder("class=\"clear-row");
        if (hidden) {
            classString.append(" " + titleString + "_class hide-row");
        }
        classString.append(" colspan=\"6\"");
        table.addCell("", classString.toString());
    }

    private static void addTitleRow(Table table, String key, String value) {
        table.newRow();
        table.addCell(key);
        Block titleBold = new Block(Block.Bold);
        titleBold.add(value);
        table.addCell(titleBold);
    }

    /**
     * Sanitises a string so that it can be used as a div id
     *
     * @param name
     * @return Returns sanitised string
     */
    public static String cleanName(String name) {
        return Normalizer.normalize(HtmlUtil.encode(name.replace(" ", "_").replace("&", "").replace("(", "")
                .replace(")", "").replace(",", "").replace("+", "_"), HtmlUtil.ENCODE_TEXT), Normalizer.Form.NFC);
    }

    public static String cleanAuName(String auName) {
        return auName.replaceAll(" ", "_").replaceAll(",", "");
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
