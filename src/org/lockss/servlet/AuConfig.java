/*
 * $Id: AuConfig.java,v 1.1 2003-07-21 08:35:46 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import org.mortbay.html.*;
import org.mortbay.tools.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

/** Create and update AU configuration.
 */
public class AuConfig extends LockssServlet {

  static Logger log = Logger.getLogger("AuConfig");

  private ConfigManager configMgr;
  private PluginManager pluginMgr;

  String action;
  Configuration auConfig;
  Plugin plug;
  Collection defKeys;
  Collection allKeys;
  java.util.List editKeys;

  // Used to insert error messages into the page
  private String errMsg;
  private String statusMsg;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    configMgr = getLockssDaemon().getConfigManager();
    pluginMgr = getLockssDaemon().getPluginManager();
  }

  public void lockssHandleRequest() throws IOException {
    action = req.getParameter("action");

    errMsg = null;
    statusMsg = null;

    op: {
      if (StringUtil.isNullString(action)) {
	displayAuSummary();
	break op;
      }
      log.debug("action=" + action);
      if (action.equals("Add")) {
	displayAddAu();
	break op;
      }
      // all other actions require AU.  If missing, display summary page
      String auid = req.getParameter("auid");
      log.debug("auid=" + auid);
      ArchivalUnit au = pluginMgr.getAuFromId(auid);
      if (au == null) {
	errMsg = "Invalid AuId: " + auid;
	displayAuSummary();
	break op;
      }
      if (action.equals("Edit")) {
	displayEditAu(au);
	break op;
      }
      if (action.equals("Update")) {
	updateAu(au);
	break op;
      }
    }
    // don't hold onto objects
    auConfig= null;
    plug= null;
    defKeys= null;
    allKeys= null;
  }

  private void displayAuSummary() throws IOException {
    Page page = newPage();
    addErrMsg(page);
    Table tbl = new Table(0, "ALIGN=CENTER CELLSPACING=4 CELLPADDING=0");
    addAddAuRow(tbl);
    Collection allAUs = pluginMgr.getAllAUs();
    if (!allAUs.isEmpty()) {
      for (Iterator iter = allAUs.iterator(); iter.hasNext(); ) {
	addAuSummaryRow(tbl, (ArchivalUnit)iter.next());
      }
    }
    page.add(tbl);
    page.add(getFooter());
    page.write(resp.getWriter());
  }

  private void addAddAuRow(Table tbl) {
    Form frm = new Form(srvURL(myServletDescr(), null));
    frm.method("POST");
    frm.add(new Input(Input.Submit, "action", "Add"));
    tbl.newRow();
    tbl.newCell("align=right");
    tbl.add(frm);
    tbl.newCell();
    tbl.add("Add new Archival Unit");
  }

  private void addAuSummaryRow(Table tbl, ArchivalUnit au) {
    Form frm = new Form(srvURL(myServletDescr(), null));
    frm.method("POST");
    addAuid(frm, au);
    frm.add(new Input(Input.Submit, "action", "Edit"));
    tbl.newRow();
    tbl.newCell("align=right");
    tbl.add(frm);
    tbl.newCell();
    tbl.add(au.getName());
  }

  private void addAuid(Composite comp, ArchivalUnit au) {
    comp.add(new Input(Input.Hidden, "auid", au.getAUId()));
  }

  private void fetchAuConfig(ArchivalUnit au) {
    auConfig = au.getConfiguration();
    log.debug("auConfig: " + auConfig);
    plug = au.getPlugin();
    defKeys = plug.getDefiningConfigKeys();
    allKeys = plug.getAUConfigProperties();
    editKeys = new LinkedList(allKeys);
    editKeys.removeAll(defKeys);
    Collections.sort(editKeys);
  }

  private void displayEditAu(ArchivalUnit au) throws IOException {
    Page page = newPage();
    fetchAuConfig(au);

    Form frm = new Form(srvURL(myServletDescr(), null));
    frm.method("POST");

    page.add("<br><center>Editing Configuration of ");
    page.add(au.getName());
    page.add("<br>AUID: ");
    page.add(au.getAUId());
    page.add("<br>");
    addErrMsg(page);
    page.add("</center><br>");

    Table tbl = new Table(0, "ALIGN=CENTER CELLSPACING=4 CELLPADDING=0");
    tbl.newRow();
    tbl.newCell("colspan=2 align=center");
    tbl.add("Fixed Properties");
    for (Iterator iter = defKeys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      tbl.newRow();
      tbl.newCell();
      tbl.add(key);
      tbl.newCell();
      tbl.add(auConfig.get(key));
    }
    tbl.newRow();
    tbl.newRow();
    tbl.newCell("colspan=2 align=center");
    if (editKeys.isEmpty()) {
      tbl.add("No Editable Properties");
    } else {
      tbl.add("Editable Properties");
      for (Iterator iter = editKeys.iterator(); iter.hasNext(); ) {
	String key = (String)iter.next();
	tbl.newRow();
	tbl.newCell();
	tbl.add(key);
	tbl.newCell();
	tbl.add(new Input(Input.Text, key, auConfig.get(key)));
      }
	tbl.newRow();
	tbl.newCell("colspan=2 align=center");
	addAuid(tbl, au);
	tbl.add(new Input(Input.Submit, "action", "Update"));
    }
    frm.add(tbl);
    page.add(frm);

    page.add(getFooter());
    page.write(resp.getWriter());
  }


  private void displayAddAu() throws IOException {
    Page page = newPage();
    errMsg = "Not implemented yet.";
    addErrMsg(page);
    page.add(getFooter());
    page.write(resp.getWriter());
  }

  private void updateAu(ArchivalUnit au) throws IOException {
    fetchAuConfig(au);
    Properties p = new Properties();
    boolean changed = false;
    for (Iterator iter = defKeys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      p.put(key, auConfig.get(key));
    }
    for (Iterator iter = editKeys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      String val = req.getParameter(key);
      if (!StringUtil.isNullString(val)) {
	p.put(key, val);
      }
      if (!val.equals(auConfig.get(key))) {
	changed = true;
	log.debug("Key " + key + " changed");
      }
    }
    if (changed) {
      try {
	log.debug("reconfiguring au");
	pluginMgr.setAndSaveAUConfiguration(au, p);
	statusMsg = "AU configuration saved.";
      } catch (ArchivalUnit.ConfigurationException e) {
	log.error("Couldn't reconfigure AU", e);
	log.error("Couldn't reconfigure AU: " + e.getMessage());
	errMsg = e.getMessage();
      }
    } else {
      statusMsg = "No changes made.";
    }
    displayEditAu(au);
  }

  private void addErrMsg(Composite comp) {
    if (errMsg != null) {
      comp.add("<br><center><Font color=red>");
      comp.add(errMsg);
      comp.add("</font></center><br>");
    }
    if (statusMsg != null) {
      comp.add("<br><center>");
      comp.add(statusMsg);
      comp.add("</center><br>");
    }
  }

  // make me a link in nav table if not on initial journal config page
  protected boolean linkMeInNav() {
    return action != null;
  }

}
