/*
 * $Id: AuConfig.java,v 1.2 2003-07-23 06:40:41 tlipkis Exp $
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

  // Used to insert error messages into the page
  private String errMsg;
  private String statusMsg;

  String action;
  Plugin plug;
  Configuration auConfig;
  Collection defKeys;
  Collection allKeys;
  java.util.List editKeys;

  // don't hold onto objects after request finished
  private void resetLocals() {
    plug = null;
    auConfig = null;
    defKeys = null;
    allKeys = null;
  }

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
      if (action.equals("Add")) {
	displayAddAu();
	break op;
      }
      if (action.equals("EditNew")) {
	displayEditNew();
	break op;
      }
      if (action.equals("Create")) {
	createAu();
	break op;
      }
      // all other actions require AU.  If missing, display summary page
      String auid = req.getParameter("auid");
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
    resetLocals();
  }

  private void displayAuSummary() throws IOException {
    Page page = newPage();
    addErrMsg(page);
    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");
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

  private void addPlugId(Composite comp, Plugin plugin) {
    comp.add(new Input(Input.Hidden, "PluginId", plugin.getPluginId()));
  }

  private void fetchAuConfig(ArchivalUnit au) {
    auConfig = au.getConfiguration();
    log.debug("auConfig: " + auConfig);
    fetchPluginConfig(au.getPlugin());
  }

  private void fetchPluginConfig(Plugin plug) {
    this.plug = plug;
    defKeys = plug.getDefiningConfigKeys();
    allKeys = plug.getAUConfigProperties();
    editKeys = new LinkedList(allKeys);
    editKeys.removeAll(defKeys);
    Collections.sort(editKeys);
  }

  private void addPropRows(Table tbl, Collection keys, Configuration props,
		      boolean editable) {
    for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      tbl.newRow();
      tbl.newCell();
      tbl.add(key);
      tbl.newCell();
      String val = props != null ? props.get(key) : null;
      if (editable) {
	tbl.add(new Input(Input.Text, key, val));
      } else {
	tbl.add(val);
      }
    }
  }

  private Form createAUEditForm(String action, ArchivalUnit au)
      throws IOException {
    boolean isNew = au == null;

    Form frm = new Form(srvURL(myServletDescr(), null));
    frm.method("POST");

    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");
    tbl.newRow();
    tbl.newCell("colspan=2 align=center");
    tbl.add("Volume Definition");
//     tbl.add(isNew ? "Defining Properties" : "Fixed Properties");
    addPropRows(tbl, defKeys, auConfig, isNew);
    tbl.newRow();
    tbl.newRow();
    tbl.newCell("colspan=2 align=center");
    if (editKeys.isEmpty()) {
      if (!isNew) {
	tbl.add("No Editable Properties");
      }
    } else {
      tbl.add("Variable Parameters");
      addPropRows(tbl, editKeys, auConfig, true);

      tbl.newRow();
      tbl.newCell("colspan=2 align=center");
      if (isNew) {
	addPlugId(tbl, plug);
      } else {
        addAuid(tbl, au);
      }
    }
    if (isNew || !editKeys.isEmpty()) {
      tbl.add(new Input(Input.Submit, "action", action));
    }
    frm.add(tbl);
    return frm;
  }

  private void displayEditAu(ArchivalUnit au) throws IOException {
    Page page = newPage();
    fetchAuConfig(au);

    page.add("<br><center>Editing Configuration of ");
    page.add(au.getName());
    page.add("<br>AUID: ");
    page.add(au.getAUId());
    page.add("<br>");
    addErrMsg(page);
    page.add("</center><br>");

    Form frm = createAUEditForm("Update", au);

    page.add(frm);

    page.add(getFooter());
    page.write(resp.getWriter());
  }


  private void displayAddAu() throws IOException {
    Page page = newPage();
    addErrMsg(page);
    SortedMap pMap = new TreeMap();
    for (Iterator iter = pluginMgr.getRegisteredPlugins().iterator();
	 iter.hasNext(); ) {
      Plugin p = (Plugin)iter.next();
      pMap.put(p.getPluginName(), p);
    }
    Form frm = new Form(srvURL(myServletDescr(), null));
    frm.method("POST");
    frm.add("<center>Add New Journal Volume<br>");
    if (!pMap.isEmpty()) {
      frm.add("<br>Choose a plugin:<br>");
      Select sel = new Select("PluginId", false);
	sel.add("-no selection-", true, "");
      for (Iterator iter = pMap.keySet().iterator(); iter.hasNext(); ) {
	String pName = (String)iter.next();
	Plugin p = (Plugin)pMap.get(pName);
	sel.add(pName, false, p.getPluginId());
      }
      frm.add(sel);
      frm.add("<br>or");
    }
    frm.add("<br>Enter the class name of a plugin:<br>");
    Input in = new Input(Input.Text, "PluginClass");
    in.setSize(40);
    frm.add(in);
    frm.add("<br>");
    frm.add(new Input(Input.Hidden, "action", "EditNew"));
    frm.add(new Input(Input.Submit, "button", "Configure Volume"));
    page.add(frm);
    page.add("</center><br>");

    page.add(getFooter());
    page.write(resp.getWriter());
  }

  private void displayEditNew() throws IOException {
    String pid = req.getParameter("PluginId");
    String pKey;
    if (!StringUtil.isNullString(pid)) {
      pKey = pluginMgr.pluginKeyFromId(pid);
    } else {
      pid = req.getParameter("PluginClass");
      pKey = pluginMgr.pluginKeyFromName(pid);
    }
    if (!pluginMgr.ensurePluginLoaded(pKey)) {
      if (StringUtil.isNullString(pKey)) {
	errMsg = "You must specify a plugin.";
      } else {
	errMsg = "Can't find plugin: " + pid;
      }
      displayAddAu();
      return;
    }
    fetchPluginConfig(pluginMgr.getPlugin(pKey));
    
    Page page = newPage();

    page.add("<br><center>Creating New Journal Volume<br>with plugin ");
    page.add(plug.getPluginName());
    page.add("</center>");
    addErrMsg(page);

    Form frm = createAUEditForm("Create", null);

    page.add(frm);

    page.add(getFooter());
    page.write(resp.getWriter());
  }

  private void createAu() throws IOException {
    String pid = req.getParameter("PluginId");
    String pKey = pluginMgr.pluginKeyFromId(pid);
    if (!pluginMgr.ensurePluginLoaded(pKey)) {
      errMsg = "Can't find plugin: " + pid;
      displayAddAu();
      return;
    }
    fetchPluginConfig(pluginMgr.getPlugin(pKey));
    Configuration newConfig = getAuConfigFromForm(true);
    try {
      ArchivalUnit au =
	pluginMgr.createAndSaveAUConfiguration(plug, newConfig);
      statusMsg = "AU created.";
      displayEditAu(au);
    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Error configuring AU", e);
      errMsg = "Error configuring AU:<br>" + e.getMessage();
      displayEditNew();
    } catch (IOException e) {
      log.error("Error saving AU configuration", e);
      errMsg = "Error saving AU configuration:<br>" + e.getMessage();
      displayEditNew();
    }
  }

  private void updateAu(ArchivalUnit au) throws IOException {
    fetchAuConfig(au);
    //    Properties p = new Properties();
    Configuration newConfig = getAuConfigFromForm(false);
    if (isChanged(auConfig, newConfig)) {
      try {
	pluginMgr.setAndSaveAUConfiguration(au, newConfig);
	statusMsg = "AU configuration saved.";
      } catch (ArchivalUnit.ConfigurationException e) {
	log.error("Couldn't reconfigure AU", e);
	errMsg = e.getMessage();
      } catch (IOException e) {
	log.error("Couldn't save AU configuraton", e);
	errMsg = "Error saving AU:<br>" + e.getMessage();
	displayEditNew();
      }
    } else {
      statusMsg = "No changes made.";
    }
    displayEditAu(au);
  }

  private void putFormVal(Properties p, String key) {
    String val = req.getParameter(key);
    // Must treat empty string as unset param.
    if (!StringUtil.isNullString(val)) {
      p.put(key, val);
    }
  }

  boolean isChanged(Configuration oldConfig, Configuration newConfig) {
    Collection dk = oldConfig.differentKeys(newConfig);
    boolean changed = false;
    for (Iterator iter = dk.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      String oldVal = oldConfig.get(key);
      String newVal = newConfig.get(key);
      if (!isEqualFormVal(oldVal, newVal)) {
	changed = true;
	log.debug("Key " + key + " changed from \"" +
		  oldVal + "\" to \"" + newVal + "\"");
      }
    }
    return changed;
  }

  Configuration getAuConfigFromForm(boolean isNew) {
    Properties p = new Properties();
    for (Iterator iter = defKeys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      if (isNew) {
	putFormVal(p, key);
      } else {
	if (auConfig.get(key) != null) {
	  p.put(key, auConfig.get(key));
	}
      }
    }
    for (Iterator iter = editKeys.iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      putFormVal(p, key);
    }
    return ConfigManager.fromProperties(p);
  }

  private boolean isEqualFormVal(String formVal, String oldVal) {
    return (StringUtil.isNullString(formVal))
      ? StringUtil.isNullString(oldVal)
      : formVal.equals(oldVal);
    }

  private void addErrMsg(Composite comp) {
    if (errMsg != null) {
      comp.add("<br><center><font color=red>");
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
