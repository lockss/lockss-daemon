/*
 * $Id: AuConfig.java,v 1.46.6.1 2006-02-15 04:08:06 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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
import org.mortbay.servlet.MultiPartRequest;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.remote.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

/** Create and update AU configuration.
 */
public class AuConfig extends LockssServlet {

  static final String PARAM_INCLUDE_PLUGIN_IN_TITLE_SELECT =
    Configuration.PREFIX + "auconfig.includePluginInTitleSelect";
  static final boolean DEFAULT_INCLUDE_PLUGIN_IN_TITLE_SELECT = false;

  static final String PARAM_ALLOW_EDIT_DEFAULT_ONLY_PARAMS =
    Configuration.PREFIX + "auconfig.allowEditDefaultOnlyParams";
  static final boolean DEFAULT_ALLOW_EDIT_DEFAULT_ONLY_PARAMS = false;

  static final String FOOT_REPOSITORY =
    "Local disk on which AU will be stored";

  static Logger log = Logger.getLogger("AuConfig");

  static final String REPO_TAG = "lockssRepository";

  // prefix added to config prop keys when used in form, to avoid
  // accidental collisions
  static final String FORM_PREFIX = "lfp.";

  private PluginManager pluginMgr;
  private ConfigManager configMgr;
  private RemoteApi remoteApi;

  String action;			// action request by form
  PluginProxy plugin;			// current plugin
  Configuration auConfig;		// current config from AU
  Configuration formConfig;		// config read from form
  TitleConfig titleConfig;		// config specified by title DB
  java.util.List auConfigParams;
  Collection defKeys;			// plugin's definitional keys
  java.util.List editKeys;		// non-definitional keys

  // don't hold onto objects after request finished
  protected void resetLocals() {
    plugin = null;
    auConfig = null;
    formConfig = null;
    titleConfig = null;
    auConfigParams = null;
    defKeys = null;
    editKeys = null;
    super.resetLocals();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    configMgr = getLockssDaemon().getConfigManager();
    pluginMgr = getLockssDaemon().getPluginManager();
    remoteApi = getLockssDaemon().getRemoteApi();
  }

  protected void lockssHandleRequest() throws IOException {
    errMsg = null;
    statusMsg = null;
    formConfig = null;
    titleConfig = null;

    action = req.getParameter(ACTION_TAG);
    if (StringUtil.isNullString(action)) {
      try {
	getMultiPartRequest();
	if (multiReq != null) {
	  action = multiReq.getString(ACTION_TAG);
	  log.debug(ACTION_TAG + " = " + action);
	}
      } catch (FormDataTooLongException e) {
	errMsg = "Uploaded file too large: " + e.getMessage();
	// leave action null, will call displayAuSummary() below
      }
    }

    String auid = req.getParameter("auid");

    if (StringUtil.isNullString(action)) displayAuSummary();
    else if (action.equals("Add")) displayAddAu();
    else if (action.equals("EditNew")) displayEditNew();
    else if (action.equals("Create")) createAu();
    else if (action.equals("Reactivate") || action.equals("DoReactivate") ||
	     action.equals("Delete") || action.equals("Confirm Delete")) {
      AuProxy au = getAuProxy(auid);
      if (au == null) {
	au = getInactiveAuProxy(auid);
      }
      if (au == null) {
	if (auid != null) {
	  errMsg = "Invalid AuId: " + auid;
	}
	displayAuSummary();
      } else if (action.equals("Reactivate")) displayReactivateAu(au);
      else if (action.equals("DoReactivate")) doReactivateAu(au);
      else if (action.equals("Delete")) confirmDeleteAu(au);
      else if (action.equals("Confirm Delete")) doDeleteAu(au);
    } else {
      // all other actions require AU.  If missing, display summary page
      AuProxy au = getAuProxy(auid);
      if (au == null) {
	errMsg = "Invalid AuId: " + auid;
	displayAuSummary();
      } else if (action.equals("Edit")) displayEditAu(au);
      else if (action.equals("Restore")) displayRestoreAu(au);
      else if (action.equals("DoRestore")) updateAu(au, "Restored");
      else if (action.equals("Update")) updateAu(au, "Updated");
      else if (action.equals("Deactivate")) confirmDeactivateAu(au);
      else if (action.equals("Confirm Deactivate")) doDeactivateAu(au);
      else {
	errMsg = "Unknown action: " + action;
	displayAuSummary();
      }
    }
  }

  /** Display "Add Archival Unit" button and list of configured AUs with Edit
   * buttons */
  private void displayAuSummary() throws IOException {
    // If the AUs are not started, don't display any AU summary or
    // any form inputs.
    if (!pluginMgr.areAusStarted()) {
      displayNotStarted();
      return;
    }

    Page page = newPage();
    Collection allAUs = remoteApi.getAllAus();
    addJavaScript(page);
    layoutErrorBlock(page);
    layoutExplanationBlock(page, "Add a new Archival Unit" +
	(allAUs.isEmpty() ? "." : ", or edit an existing one."));

    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
    // make form findable by unit tests
    frm.attribute("id", "AuSummaryForm");
    frm.add("<input type=hidden name=\"" + ACTION_TAG + "\">");
    addAuId(frm, null);
    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");
    // make table findable by unit tests
    tbl.attribute("id", "AuSummaryTable");
    addAddAuRow(tbl);
    for (Iterator iter = allAUs.iterator(); iter.hasNext(); ) {
      addAuSummaryRow(tbl, (AuProxy)iter.next());
    }
    Collection inactiveAUs = remoteApi.getInactiveAus();
    for (Iterator iter = inactiveAUs.iterator(); iter.hasNext(); ) {
      addAuSummaryRow(tbl, (AuProxy)iter.next());
    }
    frm.add(tbl);
    page.add(frm);
    endPage(page);
  }

  /** Add the "Add" row to the table */
  private void addAddAuRow(Table tbl) {
    tbl.newRow();
    tbl.newCell("align=right valign=center");
    tbl.add(submitButton("Add", "Add"));
    tbl.newCell("valign=center");
    tbl.add("Add new Archival Unit");
  }

  /** Add an Edit row to the table */
  private void addAuSummaryRow(Table tbl, AuProxy au) {
    Configuration cfg = remoteApi.getStoredAuConfiguration(au);
    boolean isGrey = true;
    String act;
    if (cfg.isEmpty()) {
      act = "Restore";
    } else if (cfg.getBoolean(PluginManager.AU_PARAM_DISABLED, false)) {
      act = "Reactivate";
    } else {
      act = "Edit";
      isGrey = false;
    }
    tbl.newRow();
    tbl.newCell("align=right valign=center");
    tbl.add(submitButton(act, act, "auid", au.getAuId()));
    tbl.newCell("valign=center");
    tbl.add(greyText(encodedAuName(au), isGrey));
  }

  /** Display form to edit existing AU */
  private void displayEditAu(AuProxy au) throws IOException {
    Page page = newPage();
    fetchAuConfig(au);

    layoutErrorBlock(page);
    layoutExplanationBlock(page,
        "Editing configuration of: " + encodedAuName(au));

    java.util.List actions = ListUtil.list("Deactivate", "Delete");
    if (!getEditKeys().isEmpty()) {
      actions.add(0, "Update");
    }
    Form frm = createAuEditForm(actions, au, true);
    page.add(frm);
    endPage(page);
  }

  /** Display form to restore unconfigured AU */
  private void displayRestoreAu(AuProxy au) throws IOException {
    Page page = newPage();
    fetchAuConfig(au);

    layoutErrorBlock(page);
    layoutExplanationBlock(page,
        "Restoring configuration of: " + encodedAuName(au));

    java.util.List actions =
      ListUtil.list(new Input(Input.Hidden, ACTION_TAG, "DoRestore"),
		    new Input(Input.Submit, "button", "Restore"));
    Form frm = createAuEditForm(actions, au, true);
    page.add(frm);
    endPage(page);
  }

  /** Display form to reactivate deactivated AU */
  private void displayReactivateAu(AuProxy au) throws IOException {
    Page page = newPage();
    addJavaScript(page);
    fetchAuConfig(au);
    if (plugin == null) {
      errMsg = "Unknown plugin: " + au.getPluginId() +
	"<br>Cannot reactivate: " + encodeText(au.getName());
      displayAuSummary();
      return;
    }
    layoutErrorBlock(page);
    layoutExplanationBlock(page,
        "Reactivating: " + encodedAuName(au));

    java.util.List actions =
      ListUtil.list(submitButton("Reactivate", "DoReactivate"),
		    submitButton("Delete", "Delete"));
    Form frm = createAuEditForm(actions, au, true);
    frm.add("<input type=hidden name=\"" + ACTION_TAG + "\">");
    page.add(frm);
    endPage(page);
  }

  // tk - temporary - should handle more than one plugin for title
  PluginProxy getTitlePlugin(String title) {
    Collection c = remoteApi.getTitlePlugins(title);
    if (c == null || c.isEmpty()) {
      return null;
    }
    return (PluginProxy)c.iterator().next();
  }

  /** Display form to add a new AU */
  private void displayEditNew() throws IOException {
    String title = req.getParameter("Title");
    if (!StringUtil.isNullString(title)) {
      // tk - need to deal with > 1 plugin for title
      plugin = getTitlePlugin(title);
      if (plugin == null) {
	errMsg = "Unknown title: " + encodeText(title);
	displayAddAu();
	return;
      }
      titleConfig = plugin.getTitleConfig(title);
      if (formConfig == null) {
	if (titleConfig != null) {
	  formConfig = titleConfig.getConfig();
	}
      }
    } else {
      String pid = req.getParameter("PluginId");
      if (StringUtil.isNullString(pid)) {
	pid = req.getParameter("PluginClass");
//	String pclass = req.getParameter("PluginClass");
//	if (!StringUtil.isNullString(pclass)) {
//	  pid = remoteApi.pluginIdFromName(pclass);
//	}
      }
      plugin = getPluginProxy(pid);
      if (plugin == null) {
	if (StringUtil.isNullString(pid)) {
	  errMsg = "Please choose a title or a plugin.";
	} else {
	  errMsg = "Can't find plugin: " + pid;
	}
	displayAddAu();
	return;
      }
    }
    Page page = newPage();
    layoutErrorBlock(page);

    StringBuffer exp = new StringBuffer();
    exp.append("Creating new Archival Unit");
    if (!StringUtil.isNullString(title)) {
      exp.append(" of ");
      exp.append(encodeText(title));
    }
    exp.append(" with plugin: ");
    exp.append(encodeText(plugin.getPluginName()));
    exp.append("<br>");
    exp.append(getEditKeys().isEmpty() ? "Confirm" : "Edit");
    exp.append(" the parameters, ");
    if (remoteApi.getRepositoryList().size() > 1) {
      exp.append(" choose a repository, ");
    }
    exp.append("then click Create");
    layoutExplanationBlock(page, exp.toString());

    Form frm = createAuEditForm(ListUtil.list("Create"), null, true);
    // Ensure still have title info if come back here on error
    frm.add(new Input(Input.Hidden, "Title", title));
    page.add(frm);
    endPage(page);
  }

  /** Display form to select from list of title, or list of plugins, or
   * enter a plugin name */
  private void displayAddAu() throws IOException {
    Page page = newPage();
    addJavaScript(page);
    layoutErrorBlock(page);
    String addExp = "Adding new Archival Unit" +
      "<br>First, select a title or a publisher plugin." +
      addFootnote("Configuring an AU requires choosing a publisher-dependent"
		  + " plugin, then filling in a set of parameters.  "
		  + "Predefined titles implicitly specify a plugin and "
		  + "some of the parameter values.");
    layoutExplanationBlock(page, addExp);

    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
//     frm.add("<center>");
    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");

    java.util.List titles = remoteApi.findAllTitles();
    if (!titles.isEmpty()) {
      boolean includePluginInTitleSelect =
	ConfigManager.getBooleanParam(PARAM_INCLUDE_PLUGIN_IN_TITLE_SELECT,
				      DEFAULT_INCLUDE_PLUGIN_IN_TITLE_SELECT);
      tbl.newRow();
      tbl.newCell("align=center");
      tbl.add("Choose a title:<br>");
      Select sel = new Select("Title", false);
      sel.attribute("onchange",
		    "cascadeSelectEnable(this,'plugin_sel')");
      sel.add("-no selection-", true, "");
      for (Iterator iter = titles.iterator(); iter.hasNext(); ) {
	String title = (String)iter.next();
	String selText = encodeText(title);
	String dispText = selText;
	if (includePluginInTitleSelect) {
	  PluginProxy titlePlugin = getTitlePlugin(title);
	  if (titlePlugin != null) {
	    String plugName = titlePlugin.getPluginName();
	    dispText = selText + " (" + plugName + ")";
	  }
	}
	// always include select value, even if same as display text, so
	// javascript can find it.  (IE doesn't copy .text to .value)
	sel.add(dispText, false, selText);
      }
      setTabOrder(sel);
      tbl.add(sel);
      addOr(tbl);
    }

    Map pMap = getPluginNameMap();
    if (!pMap.isEmpty()) {
      tbl.newRow();
      tbl.newCell("align=center");
      tbl.add("Choose a publisher plugin:<br>");
      Select sel = new Select("PluginId", false);
      sel.attribute("id", "plugin_sel");
      sel.attribute("onchange",
		    "cascadeSelectEnable(this,'plugin_input')");
      sel.add("-no selection-", true, "");
      for (Iterator iter = pMap.entrySet().iterator(); iter.hasNext(); ) {
	Map.Entry entry = (Map.Entry)iter.next();
	String pName = (String)entry.getKey();
	PluginProxy p = (PluginProxy)entry.getValue();
	sel.add(encodeText(pName), false, p.getPluginId());
      }
      setTabOrder(sel);
      tbl.add(sel);
      addOr(tbl);
    }
    tbl.newRow();
    tbl.newCell("align=center");
    tbl.add("Enter the class name of a plugin:<br>");
    Input in = new Input(Input.Text, "PluginClass");
    in.setSize(40);
    in.attribute("id", "plugin_input");
    setTabOrder(in);
    tbl.add(in);
    tbl.newRow();
    tbl.newCell("align=center");
    tbl.add("<br>");
    tbl.add("Then click to edit parameter values");
    tbl.add("<br>");
    tbl.add(new Input(Input.Hidden, ACTION_TAG, "EditNew"));
    tbl.add(setTabOrder(new Input(Input.Submit, "button", "Continue")));
    frm.add(tbl);
    page.add(frm);
    page.add("</center><br>");

    endPage(page);
  }

  SortedMap getPluginNameMap() {
    SortedMap pMap = new TreeMap();
    for (Iterator iter = remoteApi.getRegisteredPlugins().iterator();
	 iter.hasNext(); ) {
      PluginProxy pp = (PluginProxy)iter.next();
      String name = pp.getPluginName();
      if (name != null) {
	pMap.put(name, pp);
      }
    }
    return pMap;
  }

  void addOr(Table tbl) {
    addOr(tbl, 1);
  }


  void addOr(Table tbl, int cols) {
    Table orTbl =
      new Table(0, "align=center cellspacing=0 cellpadding=0 width=\"100%\"");
    orTbl.newRow();
    orTbl.newCell("align=right");
    orTbl.add("<hr align=right width=100>");
    orTbl.newCell("align=center");
    orTbl.add("&nbsp;or&nbsp;");
    orTbl.newCell("align=left");
    orTbl.add("<hr align=left width=100>");

    tbl.newRow();
    if (cols != 1) {
      tbl.newCell("colspan=" + cols);
    } else {
      tbl.newCell("width=\"100%\"");
    }
    tbl.add(orTbl);
  }

  /** Create a form to edit a (possibly not-yet-existing) AU.
   * @param actions list of action buttons for bottom of form.  If an
   * element is a String, a button will be created for it.
   * @param au the AU
   * @param editable true if the form should be editable.  (Not all the
   * fields will be editable in any case).
   */
  private Form createAuEditForm(java.util.List actions, AuProxy au,
				boolean editable)
      throws IOException {
    boolean isNew = au == null;

    Configuration initVals;
    Collection noEditKeys = Collections.EMPTY_SET;
    if (titleConfig != null) {
      initVals = titleConfig.getConfig();
      noEditKeys = titleConfig.getUnEditableKeys();
    } else if (formConfig != null) {
      initVals = formConfig;
    } else if (auConfig != null) {
      initVals = auConfig;
    } else {
      initVals = ConfigManager.EMPTY_CONFIGURATION;
    }

    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");

    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");
    tbl.newRow();
    tbl.newCell("colspan=2 align=center");
    tbl.add("Archival Unit Definition");
//     tbl.add(isNew ? "Defining Properties" : "Fixed Properties");

//     addPropRows(tbl, getDefKeys(), initVals,
//		(isNew ? getDefKeys() : null));
    addPropRows(tbl, getDefKeys(), initVals,
		(isNew
		 ? (org.apache.commons.collections.CollectionUtils.
		    subtract(getDefKeys(), noEditKeys))
		 : null));

    tbl.newRow();
    Collection eKeys = getEditKeys();
    if (eKeys.isEmpty()) {
      if (!isNew && editable) {
//	tbl.newRow();
//	tbl.newCell("colspan=2 align=center");
//	tbl.add("No Editable Properties");
      }
    } else {
      tbl.newRow();
      tbl.newCell("colspan=2 align=center");
      tbl.add("Other Parameters");
      addPropRows(tbl, eKeys, initVals, editable ? eKeys : null);
    }
    frm.add(tbl);
    if (isNew) {
      addRepoChoice(frm);
    }

    if (isNew) {
      addPlugId(frm, plugin);
    } else {
      addAuId(frm, au);
    }
    Table btnsTbl = new Table(0, "align=center cellspacing=4 cellpadding=0");
    btnsTbl.newRow();
    btnsTbl.newCell("align=center");
    for (Iterator iter = actions.iterator(); iter.hasNext(); ) {
      Object act = iter.next();
      if (act instanceof String) {
	btnsTbl.add(setTabOrder(new Input(Input.Submit, ACTION_TAG,
					  (String)act)));
      } else {
	if (act instanceof Element) {
	  // this will include hidden inputs in tab order, but that seems
	  // to be harmless.
	  Element ele = (Element)act;
	  setTabOrder(ele);
	}
	btnsTbl.add(act);
      }
      if (iter.hasNext()) {
	btnsTbl.add("&nbsp;");
      }
    }
    frm.add(btnsTbl);
    return frm;
  }

  void addRepoChoice(Composite comp) {
    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");
    java.util.List repos = remoteApi.getRepositoryList();
    if (repos.size() > 1) {
      tbl.newRow();
      tbl.newCell("colspan=4 align=center");
      tbl.add("Select Repository");
      tbl.add(addFootnote(FOOT_REPOSITORY));
      tbl.newRow();
      tbl.addHeading("Repository");
      tbl.addHeading("Size");
      tbl.addHeading("Free");
      tbl.addHeading("%Full");
      boolean first = true;
      for (Iterator iter = repos.iterator(); iter.hasNext(); ) {
	String repo = (String)iter.next();
	PlatformInfo.DF df = remoteApi.getRepositoryDF(repo);
	tbl.newRow();
	tbl.newCell("align=left");
	tbl.add(radioButton(repo, REPO_TAG, first));
	if (df != null) {
	  tbl.newCell("align=right");
	  tbl.add("&nbsp;");
	  tbl.add(StringUtil.sizeKBToString(df.getSize()));
	  tbl.newCell("align=right");
	  tbl.add("&nbsp;");
	  tbl.add(StringUtil.sizeKBToString(df.getAvail()));
	  tbl.newCell("align=right");
	  tbl.add("&nbsp;");
	  tbl.add(df.getPercentString());
	}
	first = false;
      }
      comp.add(tbl);
    }
  }

  /** Add config props rows to edit AU table.
   * @param tbl the table
   * @param keys the kwys to include (subset of those from getAuConfigParams())
   * @param initVals initial values of fields, or null
   * @param editableKeys the keys whose values may be edited
   */
  private void addPropRows(Table tbl,
                           Collection keys,
                           Configuration initVals,
                           Collection editableKeys) {
    for (Iterator iter = getAuConfigParams().iterator(); iter.hasNext(); ) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      if (!keys.contains(descr.getKey())) {
	continue;
      }
      String key = descr.getKey();
      tbl.newRow();
      tbl.newCell();
      tbl.add(encodeText(descr.getDisplayName()));
      tbl.add(addFootnote(encodeText(descr.getDescription())));
      tbl.add(": ");
      tbl.newCell();
      String val = initVals != null ? initVals.get(key) : null;
      if (editableKeys != null && editableKeys.contains(key)) {
	Input in = new Input(Input.Text, formKeyFromKey(descr.getKey()),
			     encodeAttr(val));
	if (descr.getSize() != 0) {
	  in.setSize(descr.getSize());
	}
	setTabOrder(in);
	tbl.add(in);
      } else {
	tbl.add(encodeText(val));
	tbl.add(new Input(Input.Hidden, formKeyFromKey(descr.getKey()),
			  encodeAttr(val)));
      }
    }
  }

  /** Process the Create button */
  private void createAu() throws IOException {
    String pid = req.getParameter("PluginId");
    plugin = getPluginProxy(pid);
    if (plugin == null) {
      errMsg = "Can't find plugin: " + pid;
      displayAddAu();
      return;
    }
    String repo = req.getParameter(REPO_TAG);
    if (!StringUtil.isNullString(repo)) {
      java.util.List repos = remoteApi.getRepositoryList();
      if (!repos.contains(repo)) {
	errMsg = "Nonexistent repository: " + repo;
	displayAddAu();
	return;
      }
    }
    createAuFromPlugin("Created", true);
  }

  /** Process the DoReactivate button */
  private void doReactivateAu(AuProxy aup) throws IOException {
    plugin = aup.getPlugin();
    if (plugin == null) {
      errMsg = "Can't find plugin: " + aup.getPluginId();
      displayAddAu();
      return;
    }
    fetchAuConfig(aup);
    if (aup.isActiveAu()) {
      updateAu(aup, "Reactivated");
    } else {
      createAuFromPlugin("Reactivated", false);
    }
  }

  private void createAuFromPlugin(String msg, boolean isNew)
      throws IOException {
    formConfig = getAuConfigFromForm(isNew);
    if (isNew) {
      String repo = req.getParameter(REPO_TAG);
      if (!StringUtil.isNullString(repo)) {
	if (!remoteApi.getRepositoryList().contains(repo)) {
	  errMsg = "Nonexistent repository: " + repo;
	  displayEditNew();
	  return;
	}
	formConfig.put(PluginManager.AU_PARAM_REPOSITORY, repo);
      }
    }
    try {
      AuProxy au =
	remoteApi.createAndSaveAuConfiguration(plugin, formConfig);
      statusMsg = msg + " Archival Unit:<br>" + encodeText(au.getName());
      displayAuSummary();
      return;
    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Error configuring AU", e);
      errMsg = "Error configuring AU:<br>" + encodeText(e.getMessage());
    } catch (IOException e) {
      log.error("Error saving AU configuration", e);
      errMsg = "Error saving AU configuration:<br>" +
	encodeText(e.getMessage());
    }
    displayEditNew();
  }

  /** Process the Update button */
  private void updateAu(AuProxy au, String msg) throws IOException {
    fetchAuConfig(au);
    Configuration formAuConfig = getAuConfigFromForm(false);
    // compare new config against current only, not stored config.  AU
    // config params set in global props file (for forcing crawl, etc.)
    // cause latter to see changes even when we don't need to update.
    boolean checkStored = false;
    if (isChanged(auConfig, formAuConfig) ||
	(checkStored &&
	 isChanged(remoteApi.getStoredAuConfiguration(au), formAuConfig))) {
      try {
	remoteApi.setAndSaveAuConfiguration(au, formAuConfig);
	statusMsg = msg + " Archival Unit:<br>" + encodeText(au.getName());
	displayAuSummary();
	return;
      } catch (ArchivalUnit.ConfigurationException e) {
	log.error("Couldn't reconfigure AU", e);
	errMsg = encodeText(e.getMessage());
      } catch (IOException e) {
	log.error("Couldn't save AU configuraton", e);
	errMsg = "Error saving AU:<br>" + encodeText(e.getMessage());
      }
    } else {
      statusMsg = "No changes made.";
    }
    displayEditAu(au);
  }

  /** Display the Confirm Delete  page */
  private void confirmDeleteAu(AuProxy au) throws IOException {
    String deleteFoot =
      (remoteApi.isRemoveStoppedAus() ? null :
       "Delete does not take effect until the next daemon restart.");

    Page page = newPage();
    fetchAuConfig(au);

    layoutErrorBlock(page);
    layoutExplanationBlock(page, "Are you sure you want to delete" +
	addFootnote(deleteFoot) + ": " + encodedAuName(au));

    Form frm = createAuEditForm(ListUtil.list("Confirm Delete"),
				au, false);
    page.add(frm);
    endPage(page);
  }

  /** Process the Confirm Delete button */
  private void doDeleteAu(AuProxy au) throws IOException {
    String name = au.getName();
    try {
      remoteApi.deleteAu(au);
      statusMsg = "Deleted " + (au.isActiveAu() ? "" : "Inactive ") +
	"Archival Unit:<br>" + encodeText(name);
      displayAuSummary();
      return;
    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Can't happen", e);
      errMsg = encodeText(e.getMessage());
    } catch (IOException e) {
      log.error("Couldn't save AU configuraton", e);
      errMsg = "Error deleting AU:<br>" + encodeText(e.getMessage());
    }
    confirmDeleteAu(au);
  }

  /** Display the Confirm Deactivate  page */
  private void confirmDeactivateAu(AuProxy au) throws IOException {
    String deactivateFoot =
      (remoteApi.isRemoveStoppedAus()
       ? ("A deactivated Archival Unit's contents" +
	  " will remain in the cache and it can be reactivated at any time.")
       : ("Deactivate will not take effect until the next daemon restart.  " +
	  "At that point the Archival Unit will be inactive, but its contents"+
	  " will remain in the cache and it can be reactivated at any time."));

    Page page = newPage();
    fetchAuConfig(au);

    layoutErrorBlock(page);
    layoutExplanationBlock(page, "Are you sure you want to deactivate" +
        addFootnote(deactivateFoot) + ": " + encodedAuName(au));

    Form frm = createAuEditForm(ListUtil.list("Confirm Deactivate"),
				au, false);
    page.add(frm);
    endPage(page);
  }

  /** Process the Confirm Deactivate button */
  private void doDeactivateAu(AuProxy au) throws IOException {
    String name = au.getName();
    try {
      remoteApi.deactivateAu(au);
      statusMsg = "Deactivated Archival Unit:<br>" + encodeText(name);
      displayAuSummary();
      return;
    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Can't happen", e);
      errMsg = encodeText(e.getMessage());
    } catch (IOException e) {
      log.error("Couldn't save AU configuraton", e);
      errMsg = "Error deactivating AU:<br>" + encodeText(e.getMessage());
    }
    confirmDeactivateAu(au);
  }

  /** Put a value from the config form into the properties, iff it is set
   * in the form */
  private void putFormVal(Properties p, String key) {
    String val = req.getParameter(formKeyFromKey(key));
    // Must treat empty string as unset param.
    if (!StringUtil.isNullString(val)) {
      p.put(key, val);
    }
  }

  /** Return true iff newConfig is different from oldConfig */
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

  /** Return the AU Configuration from the values in the form, except for
   * definitional props that already had values in the existing auConfig,
   * which are copied from the existing config.
   */
  Configuration getAuConfigFromForm(boolean isNew) {
    Properties p = new Properties();
    for (Iterator iter = getDefKeys().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      if (isNew) {
	putFormVal(p, key);
      } else {
	if (auConfig.get(key) != null) {
	  p.put(key, auConfig.get(key));
	}
      }
    }
    for (Iterator iter = getEditKeys().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      putFormVal(p, key);
    }
    Configuration res = ConfigManager.fromPropertiesUnsealed(p);
    if (!isNew) {
      res.copyConfigTreeFrom(auConfig, PluginManager.AU_PARAM_RESERVED);
    }
    return res;
  }

  /** True iff both values are null (or null strings) or equal strings */
  private boolean isEqualFormVal(String formVal, String oldVal) {
    return (StringUtil.isNullString(formVal))
      ? StringUtil.isNullString(oldVal)
      : formVal.equals(oldVal);
    }

  /** Add auid to form in a hidden field */
  private void addAuId(Composite comp, AuProxy au) {
    comp.add(new Input(Input.Hidden, "auid",
		       au != null ? au.getAuId() : ""));
  }

  /** Add plugin id to form in a hidden field */
  private void addPlugId(Composite comp, PluginProxy plugin) {
    comp.add(new Input(Input.Hidden, "PluginId", plugin.getPluginId()));
  }

  /** Common and page adds Back link, footer */
  protected void endPage(Page page) throws IOException {
    if (action != null) {
      page.add("<center>");
      page.add(srvLink(myServletDescr(), "Back to Journal Configuration"));
      page.add("</center>");
    }
    layoutFooter(page);
    page.write(resp.getWriter());
  }

  /** Return AU name, encoded for html text */
  String encodedAuName(AuProxy au) {
    return encodeText(au.getName());
  }

  // make me a link in nav table if not on initial journal config page
  protected boolean linkMeInNav() {
    return action != null;
  }

  private void fetchAuConfig(AuProxy au) {
    auConfig = au.getConfiguration();
    plugin = au.getPlugin();
  }

  void prepareConfigParams() {
    auConfigParams = new ArrayList(plugin.getAuConfigDescrs());
    // let the plugin specify the order
    // Collections.sort(auConfigParams);
    defKeys = new ArrayList();
    editKeys = new ArrayList();
    boolean allowEditDefaultOnly =
      ConfigManager.getBooleanParam(PARAM_ALLOW_EDIT_DEFAULT_ONLY_PARAMS,
				    DEFAULT_ALLOW_EDIT_DEFAULT_ONLY_PARAMS);
    for (Iterator iter = auConfigParams.iterator(); iter.hasNext(); ) {
      ConfigParamDescr descr = (ConfigParamDescr)iter.next();
      if (descr.isDefinitional()) {
	defKeys.add(descr.getKey());
      } else if (allowEditDefaultOnly || !descr.isDefaultOnly()) {
	editKeys.add(descr.getKey());
      }
    }
  }

  Collection getDefKeys() {
    if (defKeys == null) {
      prepareConfigParams();
    }
    return defKeys;
  }

  java.util.List getEditKeys() {
    if (editKeys == null) {
      prepareConfigParams();
    }
    return editKeys;
  }

  java.util.List getAuConfigParams() {
    if (auConfigParams == null) {
      prepareConfigParams();
    }
    return auConfigParams;
  }

  private String formKeyFromKey(String key) {
    return FORM_PREFIX + key;
  }

  private String keyFromFormKey(String formKey) {
    return formKey.substring(FORM_PREFIX.length());
  }

  private AuProxy getAuProxy(String auid) {
    try {
      return remoteApi.findAuProxy(auid);
    } catch (Exception e) {
      return null;
    }
  }

  private InactiveAuProxy getInactiveAuProxy(String auid) {
    try {
      return remoteApi.findInactiveAuProxy(auid);
    } catch (Exception e) {
      return null;
    }
  }

  private PluginProxy getPluginProxy(String pid) {
    try {
      return remoteApi.findPluginProxy(pid);
    } catch (Exception e) {
      return null;
    }
  }
}
