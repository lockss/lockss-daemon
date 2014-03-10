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

import org.apache.commons.collections.map.LinkedMap;
import org.lockss.config.*;
import org.lockss.daemon.status.StatusService;
import org.lockss.daemon.status.StatusTable;
import org.lockss.plugin.*;
import org.lockss.remote.RemoteApi;
import org.lockss.util.*;
import org.mortbay.html.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;

/**
 * Display Content Status servlet
 */
public class AddContent extends LockssServlet {
    protected static Logger log = Logger.getLogger("AddContent");

    private static final int LETTERS_IN_ALPHABET = 26;
    private static final int DEFAULT_NUMBER_IN_GROUP = 2;
    private static final String LOADING_SPINNER = "images/ajax-loader.gif";

    static final String SESSION_KEY_REPO_MAP = "RepoMap";
    static final String SESSION_KEY_BACKUP_INFO = "BackupInfo";
    static final String KEY_DEFAULT_REPO = "DefaultRepository";
    static final String KEY_REPO = "Repository";

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
    private String tabKey;
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
    private String actionMessage;

    private Page page;

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
        session = getSession();
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
        tabKey = req.getParameter("tab");
        switch (outputFmt) {
            case OUTPUT_HTML:
                doHtmlStatusTable();
                break;
        }
        actionMessage = null;
        if ("Delete selected".equals(req.getParameter("deleteSubmit"))) {
            String[] deleteAUs = req.getParameterValues("au");
            if (deleteAUs != null) {
                List<String> aus = Arrays.asList(deleteAUs);
                log.error("AUs: " + aus);
                doRemoveAus(aus);
                actionMessage = createActionMessage(aus, false);
            } else {
                log.error("No AUs selected");
                actionMessage = "No AUs selected!";
            }
        }
        if ("Add selected".equals(req.getParameter("addSubmit"))) {
            String[] addAUs = req.getParameterValues("au");
            if (addAUs != null) {
                List<String> aus = Arrays.asList(addAUs);
                log.error("AUs: " + aus);
                doAddAus(RemoteApi.BATCH_ADD_ADD, aus);
                actionMessage = createActionMessage(aus, true);
            } else {
                log.error("No AUs selected");
                actionMessage = "No AUs selected!";
            }
        }
        if (actionMessage != null) {
            session.setAttribute("actionMessage", actionMessage);
        }

        String publisher = req.getParameter("deletePublisher");
        if (!StringUtil.isNullString(publisher)) {
            deletePublisher(publisher);
        }
    }

    private String createActionMessage(List<String> aus, boolean added) {
        StringBuilder sb = new StringBuilder("AUs ");
        for (String auid : aus) {
            TdbAu tdbAu = TdbUtil.getTdbAu(auid);
            String title = tdbAu.getJournalTitle();
            if (title != null) {
                sb.append(title);
            }
            String volume = tdbAu.getVolume();
            if (volume != null) {
                sb.append(", " + volume);
            }
            String year = tdbAu.getYear();
            if (year != null) {
                sb.append(" (" + year + ")");
            }
            sb.append("; ");
        }
        if (added) {
            sb.append(" were added");
        } else {
            sb.append(" were removed");
        }
        return sb.toString();
    }

    private void deletePublisher(String publisher) throws IOException{
            TreeMap<String, TreeMap<String, TreeSet<ArchivalUnit>>> auMap = DisplayContentTab.getAusByPublisherName();
            ArrayList<String> auIds = new ArrayList<String>();
            if (auMap.containsKey(publisher)) {
                for (Map.Entry<String, TreeMap<String, TreeSet<ArchivalUnit>>> entry : auMap.entrySet()) {
                    String publisherString = entry.getKey();
                    log.error("Publisher: " + publisher);
                    log.error("Publisher string: " + publisherString);
                    if (publisher.equals(publisherString)) {
                        TreeMap<String, TreeSet<ArchivalUnit>> titleMap = entry.getValue();
                        for (Map.Entry<String, TreeSet<ArchivalUnit>> stringTreeSetEntry : titleMap.entrySet()) {
                            TreeSet<ArchivalUnit> auSet = stringTreeSetEntry.getValue();
                            for (ArchivalUnit au : auSet) {
                                auIds.add(au.getAuId());
                            }
                        }
                    }
                }
                doRemoveAus(auIds);
                session.setAttribute("actionMessage", "All AUs associated with publisher " + publisher + " were deleted");
            } else {
                log.error("Could not find publisher");
            }
    }

    private void doRemoveAus(List<String> aus) throws IOException {
        remoteApi.deleteAus(aus);
    }

    private void doAddAus(int addOp, List<String> auids) throws IOException {
        HttpSession session = getSession();
        if (!hasSession()) {
            errMsg = "Please enable cookies";
//            displayMenu();
            log.error("No session!");
            return;
        }
        RemoteApi.BackupInfo bi =
                (RemoteApi.BackupInfo)session.getAttribute(SESSION_KEY_BACKUP_INFO);
        LinkedMap repoMap = (LinkedMap)session.getAttribute(SESSION_KEY_REPO_MAP);
        String defaultRepo = null;
        String defRepoId = getParameter(KEY_DEFAULT_REPO);
        if (StringUtil.isNullString(defRepoId)) {
            defaultRepo =
                    remoteApi.findLeastFullRepository(remoteApi.getRepositoryMap());
        } else if (repoMap != null) {
            try {
                int n = Integer.parseInt(defRepoId);
                defaultRepo = (String)repoMap.get(n - 1);
            } catch (NumberFormatException e) {
                log.warning("Illegal default repoId: " + defRepoId, e);
            } catch (IndexOutOfBoundsException e) {
                log.warning("Illegal default repoId: " + defRepoId, e);
            }
        }
        if (auids == null || auids.size() == 0) {
            errMsg = "No AUs were selected";
//            displayMenu();
            log.error("No AUs listed!");
            return;
        }
        Configuration createConfig = ConfigManager.newConfiguration();
        for (String auid : auids) {
            TdbAu tdbAu = TdbUtil.getTdbAu(auid);
            Properties props = new Properties();
            Map<String, String> attrs = tdbAu.getAttrs();
            for (Map.Entry<String, String> attrPairs : attrs.entrySet()) {
                props.setProperty(attrPairs.getKey(), attrPairs.getValue());
            }
            Map<String, String> params = tdbAu.getParams();
            for (Map.Entry<String, String> paramPairs : params.entrySet()) {
                props.setProperty(paramPairs.getKey(), paramPairs.getValue());
            }
            Map<String, String> properties = tdbAu.getProperties();
            for (Map.Entry<String, String> propertyPairs : properties.entrySet()) {
                props.setProperty(propertyPairs.getKey(), propertyPairs.getValue());
            }
            String volume = tdbAu.getVolume();
            if (volume != null) {
                props.setProperty("volume", volume);
            }
            String year = tdbAu.getYear();
            if (year != null) {
                props.setProperty("year", year);
            }
            Configuration tcConfig = ConfigManager.fromPropertiesUnsealed(props);

            tcConfig.remove(PluginManager.AU_PARAM_REPOSITORY);
            String repoId = getParameter(KEY_REPO + "_" + auid);
            if (!StringUtil.isNullString(repoId) && repoMap != null) {
                try {
                    int repoIx = Integer.parseInt(repoId);
                    if (!StringUtil.isNullString(repoId)) {
                        tcConfig.put(PluginManager.AU_PARAM_REPOSITORY,
                                (String)repoMap.get(repoIx - 1));
                    }
                } catch (NumberFormatException e) {
                    log.warning("Illegal repoId: " + repoId, e);
                } catch (IndexOutOfBoundsException e) {
                    log.warning("Illegal repoId: " + repoId, e);
                }
            }
            if (defaultRepo != null &&
                    !tcConfig.containsKey(PluginManager.AU_PARAM_REPOSITORY)) {
                tcConfig.put(PluginManager.AU_PARAM_REPOSITORY, defaultRepo);
            }
            String prefix = PluginManager.auConfigPrefix(auid);
            createConfig.addAsSubTree(tcConfig, prefix);
        }
        if (log.isDebug2()) log.debug2("createConfig: " + createConfig);

        RemoteApi.BatchAuStatus bas =
                remoteApi.batchAddAus(addOp, createConfig, bi);
        log.debug("Batch add status: " + bas.toString());
    }

    private void doHtmlStatusTable() throws IOException {
        doHtmlStatusTable0();
        endPage(page);
    }

    // Build the table, adding elements to page
    private void doHtmlStatusTable0() throws IOException {
        newTablePage();
        if (tabKey != null) {
            Script tabSelect = new Script("$(document).ready(function () {$(\"#tabs\").tabs({ active: " + tabKey + " })});");
            page.addHeader(tabSelect);
        }
    }

    private void newTablePage() throws IOException {
        page = newPage();
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
        addCss();
        createTabsDiv();
        addJQueryJS();
    }

    protected boolean isAllTablesTable() {
        return StatusService.ALL_TABLES_TABLE.equals(tableName);
    }

    private void addCss() {
        StyleLink jqueryLink = new StyleLink("http://code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css");
        page.addHeader(jqueryLink);
        StyleLink jqueryTooltipLink = new StyleLink("/css/jquery.ui.tooltip.css");
        page.addHeader(jqueryTooltipLink);
        StyleLink cssLink = new StyleLink("/css/lockss.css");
        page.addHeader(cssLink);
    }

    /**
     * Adds JQuery Javascript to the header of the page object
     */
    private void addJQueryJS() {
        addJS("http://code.jquery.com/jquery-1.9.1.js");
        addJS("http://code.jquery.com/ui/1.10.3/jquery-ui.js");
        addJS("js/auDetails.js");
    }

    /**
     * Adds the div required by jQuery tabs
     */
    private void createTabsDiv() {
        page.add(createTabList());
    }

    /**
     * Populates the treemap with start and end letters based on how many letters
     * should be present in each group
     */
    private TreeMap<Character, Character> populateLetterList() {
        TreeMap<Character, Character> startLetterList =
                new TreeMap<Character, Character>();
        int numberOfTabs = LETTERS_IN_ALPHABET / DEFAULT_NUMBER_IN_GROUP;

        if (LETTERS_IN_ALPHABET % DEFAULT_NUMBER_IN_GROUP != 0) {
            numberOfTabs++;
        }
        for (int i = 0; i < numberOfTabs; i++) {
            Character startLetter = (char) ((i * DEFAULT_NUMBER_IN_GROUP) + 65);
            Character endLetter = (char) (startLetter + DEFAULT_NUMBER_IN_GROUP - 1);
            if ((int) endLetter > (25 + 65)) {
                endLetter = (char) (25 + 65);
            }
            startLetterList.put(startLetter, endLetter);
        }
        return startLetterList;
    }

    /**
     * Creates the spans required for jQuery tabs to build the desired tabs
     */
    private Block createTabList() {
        Block tabsDiv = new Block(Block.Div, "id='tabs'");
        org.mortbay.html.List tabList =
                new org.mortbay.html.List(org.mortbay.html.List.Unordered);
        tabsDiv.add(tabList);
        Integer tabCount = 1;
        for (Map.Entry<Character, Character> letterPairs : populateLetterList().entrySet()) {
            Character startLetter = letterPairs.getKey();
            Character endLetter = letterPairs.getValue();
            StringBuilder tabLetter = new StringBuilder(startLetter.toString());
            if (startLetter != endLetter) {
                tabLetter.append(" - " + endLetter.toString());
            }
            Link tabLink = new Link("AddContentTab?start=" + startLetter + "&amp;end=" + endLetter + "&amp;type=" +
                    type + "&amp;filter=" + filterKey + "&amp;timeKey=" + timeKey, tabLetter.toString());
            Composite tabListItem = tabList.newItem();
            tabListItem.add(tabLink);
            Block loadingDiv = new Block(Block.Div, "id='ui-tabs-" + tabCount++ + "'");
            Image loadingImage = new Image(LOADING_SPINNER);
            loadingImage.alt("Loading...");
            loadingDiv.add(loadingImage);
            loadingDiv.add(" Loading...");
            tabsDiv.add(loadingDiv);
        }
        return tabsDiv;
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
