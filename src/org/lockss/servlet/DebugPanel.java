/*
 * $Id: DebugPanel.java,v 1.27 2012-07-13 17:33:59 barry409 Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import java.security.*;
import org.mortbay.html.*;
import org.mortbay.util.B64Code;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.mail.*;
import org.lockss.poller.*;
import org.lockss.crawler.*;
import org.lockss.state.*;
import org.lockss.config.*;
import org.lockss.remote.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.account.*;
import org.lockss.daemon.status.*;

/** UI to invoke various daemon actions
 */
public class DebugPanel extends LockssServlet {

  public static final String PREFIX = Configuration.PREFIX + "debugPanel.";

  /**
   * Priority for crawls started from the debug panel
   */
  public static final String PARAM_CRAWL_PRIORITY = 
    PREFIX + "crawlPriority";
  private static final int DEFAULT_CRAWL_PRIORITY = 10;

  static final String KEY_ACTION = "action";
  static final String KEY_MSG = "msg";
  static final String KEY_NAME_SEL = "name_sel";
  static final String KEY_NAME_TYPE = "name_type";
  static final String KEY_AUID = "auid";
  static final String KEY_TEXT = "text";
  static final String KEY_URL = "url";

  static final String ACTION_MAIL_BACKUP = "Mail Backup File";
  static final String ACTION_THROW_IOEXCEPTION = "Throw IOException";
  static final String ACTION_FIND_URL = "Find Preserved URL";

  static final String ACTION_REINDEX_METADATA = "Reindex Metadata";
  static final String ACTION_FORCE_REINDEX_METADATA = "Force Reindex Metadata";
  static final String ACTION_START_V3_POLL = "Start V3 Poll";
  static final String ACTION_FORCE_START_V3_POLL = "Force V3 Poll";
  static final String ACTION_START_CRAWL = "Start Crawl";
  static final String ACTION_FORCE_START_CRAWL = "Force Start Crawl";
  static final String ACTION_CRAWL_PLUGINS = "Crawl Plugins";
  static final String ACTION_RELOAD_CONFIG = "Reload Config";

  static final String COL2 = "colspan=2";
  static final String COL2CENTER = COL2 + " align=center";

  static Logger log = Logger.getLogger("DebugPanel");

  private LockssDaemon daemon;
  private PluginManager pluginMgr;
  private PollManager pollManager;
  private CrawlManager crawlMgr;
  private ConfigManager cfgMgr;
  private MetadataManager metadataMgr;
  private RemoteApi rmtApi;

  String auid;
  String name;
  String text;
  boolean showResult;
  boolean showForcePoll;
  boolean showForceCrawl;
  boolean showForceReindexMetadata;
  protected void resetLocals() {
    resetVars();
    super.resetLocals();
  }

  void resetVars() {
    auid = null;
    errMsg = null;
    statusMsg = null;
    showForcePoll = false;
    showForceCrawl = false;
    showForceReindexMetadata = false;
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    daemon = getLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    pollManager = daemon.getPollManager();
    crawlMgr = daemon.getCrawlManager();
    cfgMgr = daemon.getConfigManager();
    rmtApi = daemon.getRemoteApi();
    try {
      metadataMgr = daemon.getMetadataManager();
    } catch (IllegalArgumentException ex) {}
  }

  public void lockssHandleRequest() throws IOException {
    resetVars();
    boolean showForm = true;
    String action = getParameter(KEY_ACTION);

    if (!StringUtil.isNullString(action)) {
      auid = getParameter(KEY_AUID);
      UserAccount acct = getUserAccount();
      if (acct != null) {
	acct.auditableEvent("used debug panel action: " + action +
			    " AU ID: " + auid);
      }
    }
    if (ACTION_MAIL_BACKUP.equals(action)) {
      doMailBackup();
    }
    if (ACTION_RELOAD_CONFIG.equals(action)) {
      doReloadConfig();
    }
    if (ACTION_THROW_IOEXCEPTION.equals(action)) {
      doThrow();
    }
    if (ACTION_START_V3_POLL.equals(action)) {
      doV3Poll();
    }
    if (ACTION_FORCE_START_V3_POLL.equals(action)) {
      forceV3Poll();
    }
    if (ACTION_START_CRAWL.equals(action)) {
      doCrawl();
    }
    if (ACTION_FORCE_START_CRAWL.equals(action)) {
      forceCrawl();
    }
    if (ACTION_CRAWL_PLUGINS.equals(action)) {
      crawlPluginRegistries();
    }
    if (ACTION_FIND_URL.equals(action)) {
      showForm = doFindUrl();
    }
    if (ACTION_REINDEX_METADATA.equals(action)) {
      doReindexMetadata();
    }
    if (ACTION_FORCE_REINDEX_METADATA.equals(action)) {
      forceReindexMetadata();
    }
    if (showForm) {
      displayPage();
    }
  }

  private void doMailBackup() {
    try {
      rmtApi.sendMailBackup();
    } catch (Exception e) {
      errMsg = "Error: " + e.getMessage();
    }
  }

  private void doReloadConfig() {
    cfgMgr.requestReload();
  }

  private void doThrow() throws IOException {
    String msg = getParameter(KEY_MSG);
    throw new IOException(msg != null ? msg : "Test message");
  }

  private void doReindexMetadata() {
    ArchivalUnit au = getAu();
    if (au == null) return;
    try {
      startReindexingMetadata(au, false);
    } catch (RuntimeException e) {
      log.error("Can't reindex metadata", e);
      errMsg = "Error: " + e.toString();
    }
  }

  private void forceReindexMetadata() {
    ArchivalUnit au = getAu();
    if (au == null) return;
    try {
      startReindexingMetadata(au, true);
    } catch (RuntimeException e) {
      log.error("Can't reindex metadata", e);
      errMsg = "Error: " + e.toString();
    }
  }

  private void doCrawl() {
    ArchivalUnit au = getAu();
    if (au == null) return;
    try {
      if (startCrawl(au, false)) {
      } else {
 	errMsg = "Not eligible for crawl.  Click again to override rate limiter.";
 	showForceCrawl = true;
 	return;
      }
    } catch (Exception e) {
      log.error("Can't start crawl", e);
      errMsg = "Error: " + e.toString();
    }
  }

  private void forceCrawl() {
    ArchivalUnit au = getAu();
    if (au == null) return;
    try {
      if (!startCrawl(au, true)) {
 	errMsg = "Sorry, crawl still won't start, see log.";
      }
    } catch (RuntimeException e) {
      log.error("Can't start crawl", e);
      errMsg = "Error: " + e.toString();
    }
  }

  private void crawlPluginRegistries() {
    StringBuilder sb = new StringBuilder();
    for (ArchivalUnit au : pluginMgr.getAllRegistryAus()) {
      try {
	sb.append(au.getName());
	sb.append(": ");
	if (startCrawl(au, true)) {
	  sb.append("Queued.");
	} else {
	  sb.append("Failed, see log.");
	}
      } catch (RuntimeException e) {
	log.error("Can't start crawl", e);
	sb.append("Error: ");
	sb.append(e.toString());
      }
      sb.append("\n");
    }
    statusMsg = sb.toString();
  }

  private boolean startCrawl(ArchivalUnit au, boolean force) {
    CrawlManagerImpl cmi = (CrawlManagerImpl)crawlMgr;
    if (force) {
      RateLimiter limit = cmi.getNewContentRateLimiter(au);
      if (!limit.isEventOk()) {
	limit.unevent();
      }
    }
    if (cmi.isEligibleForNewContentCrawl(au)) {
      Configuration config = cfgMgr.getCurrentConfig();
      int pri = config.getInt(PARAM_CRAWL_PRIORITY, DEFAULT_CRAWL_PRIORITY);
      crawlMgr.startNewContentCrawl(au, pri, null, null, null);
      statusMsg = "Crawl requested for " + au.getName();
      return true;
    } else {
      return false;
    }
  }
  
  private boolean startReindexingMetadata(ArchivalUnit au, boolean force) {
    if (metadataMgr == null) {
      errMsg = "Metadata processing is not enabled.";
      return false;
    }

    if (!force) {
      if (!AuUtil.hasCrawled(au)) {
        errMsg = "Au has never crawled. Click again to reindex metadata";
        showForceReindexMetadata = true;
        return false;
      }
      
      AuState auState = AuUtil.getAuState(au);
      switch (auState.getSubstanceState()) {
      case No:
        errMsg = "Au has no substance. Click again to reindex metadata";
        showForceReindexMetadata = true;
        return false;
      case Unknown:
        errMsg = "Unknown substance for Au. Click again to reindex metadata.";
        showForceReindexMetadata = true;
        return false;
      case Yes:
	// fall through
      }
    }
    
    if (metadataMgr.addAuToReindex(au)) {
      statusMsg = "Reindexing metadata for " + au.getName();
      return true;
    }
    if (force) {
      errMsg = "Still annot reindex metadata for " + au.getName();
    } else {
      errMsg = "Cannot reindex metadata for " + au.getName();
    }
    return false;
  }


  private void doV3Poll() {
    ArchivalUnit au = getAu();
    if (au == null) return;
    try {
      NodeManager nodeMgr = daemon.getNodeManager(au);
      // Don't call a poll on this if we're already running a V3 poll on it.
      try {
	pollManager.checkEligibleForPoll(au);
      } catch (PollManager.NotEligibleException e) {
	errMsg = "Ineligible: " + e.getMessage() +
	  "<br>Click again to force new poll.";
	showForcePoll = true;
	return;
      }
      // Don't poll if never crawled & not down
      if (!AuUtil.hasCrawled(au) && !AuUtil.isPubDown(au)) {
	errMsg = "Not crawled yet.  Click again to force new poll.";
	showForcePoll = true;
	return;
      }
      callV3ContentPoll(au);
    } catch (Exception e) {
      log.error("Can't start poll", e);
      errMsg = "Error: " + e.toString();
    }
  }

  private void forceV3Poll() {
    ArchivalUnit au = getAu();
    if (au == null) return;
    try {
      callV3ContentPoll(au);
    } catch (Exception e) {
      log.error("Can't start poll", e);
      errMsg = "Error: " + e.toString();
    }
  }

  private void callV3ContentPoll(ArchivalUnit au) {
    log.debug("Enqueuing a V3 Content Poll on " + au.getName());
    PollSpec spec = new PollSpec(au.getAuCachedUrlSet(), Poll.V3_POLL);
    try {
      pollManager.enqueueHighPriorityPoll(au, spec);
      statusMsg = "Enqueued V3 poll for " + au.getName();
    } catch (PollManager.NotEligibleException e) {
      errMsg = "Failed to enqueue poll on "
	+ au.getName() + ": " + e.getMessage();
    }
  }

  private boolean doFindUrl() throws IOException {
    
    String url = getParameter(KEY_URL);

    String redir =
      srvURL(AdminServletManager.SERVLET_DAEMON_STATUS,
	     PropUtil.fromArgs("table",
			       ArchivalUnitStatus.AUS_WITH_URL_TABLE_NAME,
			       "key", url));

    resp.setContentLength(0);
//     resp.sendRedirect(resp.encodeRedirectURL(redir));
    resp.sendRedirect(redir);
    return false;
  }

  ArchivalUnit getAu() {
    if (StringUtil.isNullString(auid)) {
      errMsg = "Select an AU";
      return null;
    }
    ArchivalUnit au = pluginMgr.getAuFromId(auid);
    if (au == null) {
      errMsg = "No such AU.  Select an AU";
      return null;
    }
    return au;
  }

  private void displayPage() throws IOException {
    Page page = newPage();
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, "Debug Actions");
    page.add(makeForm());
    page.add("<br>");
    endPage(page);
  }

  private Element makeForm() {
    Composite comp = new Composite();
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");


    frm.add("<br><center>");
    Input reload = new Input(Input.Submit, KEY_ACTION, ACTION_RELOAD_CONFIG);
    setTabOrder(reload);
    frm.add(reload);
    frm.add(" ");
    Input backup = new Input(Input.Submit, KEY_ACTION, ACTION_MAIL_BACKUP);
    setTabOrder(backup);
    frm.add(backup);
    frm.add(" ");
    Input crawlplug = new Input(Input.Submit, KEY_ACTION, ACTION_CRAWL_PLUGINS);
    setTabOrder(crawlplug);
    frm.add(crawlplug);
    frm.add("</center>");
    ServletDescr d1 = AdminServletManager.SERVLET_HASH_CUS;
    if (isServletRunnable(d1)) {
      frm.add("<br><center>"+srvLink(d1, d1.heading)+"</center>");
    }
    Input findUrl = new Input(Input.Submit, KEY_ACTION, ACTION_FIND_URL);
    Input findUrlText = new Input(Input.Text, KEY_URL);
    findUrlText.setSize(50);
    setTabOrder(findUrl);
    setTabOrder(findUrlText);
    frm.add("<br><center>"+findUrl+" " + findUrlText + "</center>");

    Input thrw = new Input(Input.Submit, KEY_ACTION, ACTION_THROW_IOEXCEPTION);
    Input thmsg = new Input(Input.Text, KEY_MSG);
    setTabOrder(thrw);
    setTabOrder(thmsg);
    frm.add("<br><center>"+thrw+" " + thmsg + "</center>");

    frm.add("<br><center>AU Actions: select AU</center>");
    Composite ausel = ServletUtil.layoutSelectAu(this, KEY_AUID, auid);
    frm.add("<br><center>"+ausel+"</center>");
    setTabOrder(ausel);

    Input v3Poll = new Input(Input.Submit, KEY_ACTION,
			     ( showForcePoll
			       ? ACTION_FORCE_START_V3_POLL
			       : ACTION_START_V3_POLL));
    Input crawl = new Input(Input.Submit, KEY_ACTION,
			    ( showForceCrawl
			      ? ACTION_FORCE_START_CRAWL
			      : ACTION_START_CRAWL));

    frm.add("<br><center>");
    frm.add(v3Poll);
    frm.add(" ");
    frm.add(crawl);
    if (metadataMgr != null) {
      Input reindex = new Input(Input.Submit, KEY_ACTION,
                                ( showForceReindexMetadata
                                  ? ACTION_FORCE_REINDEX_METADATA
                                  : ACTION_REINDEX_METADATA));
      frm.add(" ");
      frm.add(reindex);
    }
    frm.add("</center>");
    comp.add(frm);
    return comp;
  }

}
