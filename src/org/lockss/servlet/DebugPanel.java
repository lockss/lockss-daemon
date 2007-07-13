/*
 * $Id: DebugPanel.java,v 1.10.8.1 2007-07-13 21:15:40 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.state.*;
import org.lockss.config.*;
import org.lockss.remote.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

/** UI to invoke various daemon actions
 */
public class DebugPanel extends LockssServlet {
  static final String KEY_ACTION = "action";
  static final String KEY_MSG = "msg";
  static final String KEY_NAME_SEL = "name_sel";
  static final String KEY_NAME_TYPE = "name_type";
  static final String KEY_AUID = "auid";
  static final String KEY_TEXT = "text";

  static final String ACTION_MAIL_BACKUP = "Mail Backup File";
  static final String ACTION_THROW_IOEXCEPTION = "Throw IOException";
  static final String ACTION_START_V3_POLL = "Start V3 Poll";
  static final String ACTION_FORCE_START_V3_POLL = "Force V3 Poll";

  static final String COL2 = "colspan=2";
  static final String COL2CENTER = COL2 + " align=center";

  static Logger log = Logger.getLogger("DebugPanel");

  private LockssDaemon daemon;
  private PluginManager pluginMgr;
  private PollManager pollManager;
  private RemoteApi rmtApi;

  String auid;
  String name;
  String text;
  boolean showResult;
  boolean showForce;
  protected void resetLocals() {
    resetVars();
    super.resetLocals();
  }

  void resetVars() {
    auid = null;
    errMsg = null;
    statusMsg = null;
    showForce = false;
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    daemon = getLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    pollManager = daemon.getPollManager();
    rmtApi = daemon.getRemoteApi();
  }

  public void lockssHandleRequest() throws IOException {
    resetVars();
    String action = getParameter(KEY_ACTION);

    if (!StringUtil.isNullString(action)) {
      auid = getParameter(KEY_AUID);
    }
    if (ACTION_MAIL_BACKUP.equals(action)) {
      doMailBackup();
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
    displayPage();
  }

  private void doMailBackup() {
    try {
      rmtApi.sendMailBackup();
    } catch (Exception e) {
      errMsg = "Error: " + e.getMessage();
    }
  }

  private void doThrow() throws IOException {
    String msg = getParameter(KEY_MSG);
    throw new IOException(msg != null ? msg : "Test message");
  }

  private void doV3Poll() {
    ArchivalUnit au = getAu();
    if (au == null) return;
    try {
      CachedUrlSet cus = au.getAuCachedUrlSet();
      NodeManager nodeMgr = daemon.getNodeManager(au);
      PollSpec spec = new PollSpec(cus, Poll.V3_POLL);
      // Don't call a poll on this if we're already running a V3 poll on it.
      if (pollManager.isV3PollerRunning(spec)) {
	errMsg = "Poll already running.  Click again to force new poll.";
	showForce = true;
	return;
      }
      // Don't poll if never crawled & not down
      if (nodeMgr.getAuState().getLastCrawlTime() < 0 &&
	  !AuUtil.isPubDown(au)) {
	errMsg = "Not crawled yet.  Click again to force new poll.";
	showForce = true;
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
    PollSpec spec = new PollSpec(au.getAuCachedUrlSet(), Poll.V3_POLL);
    log.debug("Calling a V3 Content Poll on " + au.getName());
    if (pollManager.callPoll(spec) == null) {
      errMsg = "Failed to call poll on " + au.getName() + ", see log.";
    } else {
      statusMsg = "Started V3 poll for " + au.getName();
    }
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
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  private Element makeForm() {
    Composite comp = new Composite();
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");


    Input backup = new Input(Input.Submit, KEY_ACTION, ACTION_MAIL_BACKUP);
    setTabOrder(backup);
    frm.add("<br><center>"+backup+"</center>");
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
			     ( showForce
			       ? ACTION_FORCE_START_V3_POLL
			       : ACTION_START_V3_POLL));
    frm.add("<br><center>" + v3Poll + "</center>");
    comp.add(frm);
    return comp;
  }

}
