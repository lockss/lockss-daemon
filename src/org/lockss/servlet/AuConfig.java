/*
 * $Id: AuConfig.java,v 1.15 2004-01-12 06:22:22 tlipkis Exp $
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
import org.lockss.remote.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

/** Create and update AU configuration.
 */
public class AuConfig extends LockssServlet {

  static final String PARAM_INCLUDE_PLUGIN_IN_TITLE_SELECT =
    Configuration.PREFIX + "auconfig.includePluginInTitleSelect";
  static final boolean DEFAULT_INCLUDE_PLUGIN_IN_TITLE_SELECT = false;

  static Logger log = Logger.getLogger("AuConfig");

  // Name given to form element whose value is the action that should be
  // performed when the form is submitted.  (Not always the submit button.)
  static final String ACTION_TAG = "lockssAction";
  // prefix added to config prop keys when used in form, to avoid
  // accidental collisions
  static final String FORM_PREFIX = "lfp.";

  private ConfigManager configMgr;
  private RemoteApi remoteApi;

  // Used to insert messages into the page
  private String errMsg;
  private String statusMsg;

  String action;			// action request by form
  PluginProxy plugin;			// current plugin
  Configuration auConfig;		// current config from AU
  Configuration formConfig;		// config read from form
  TitleConfig titleConfig;		// config specified by title DB
  java.util.List auConfigParams;
  Collection defKeys;			// plugin's definitional keys
  java.util.List editKeys;		// non-definitional keys

  // number submit buttons sequentially so unit tests can find them
  private int submitButtonNumber = 0;

  // don't hold onto objects after request finished
  private void resetLocals() {
    plugin = null;
    auConfig = null;
    formConfig = null;
    titleConfig = null;
    auConfigParams = null;
    defKeys = null;
    editKeys = null;
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    configMgr = getLockssDaemon().getConfigManager();
    remoteApi = getLockssDaemon().getRemoteApi();
  }

  protected void lockssHandleRequest() throws IOException {
    action = req.getParameter(ACTION_TAG);

    errMsg = null;
    statusMsg = null;
    formConfig = null;
    titleConfig = null;
    submitButtonNumber = 0;

    String auid = req.getParameter("auid");

    if (StringUtil.isNullString(action)) displayAuSummary();
    else if (action.equals("Add")) displayAddAu();
    else if (action.equals("EditNew")) displayEditNew();
    else if (action.equals("Create")) createAu();
    else if (action.equals("Reactivate") || action.equals("DoReactivate")) {
      AuProxy au = getAuProxy(auid);
      if (au == null) {
	au = getInactiveAuProxy(auid);
      }
      if (au == null) {
	displayAuSummary();
      } else if (action.equals("Reactivate")) displayReactivateAu(au);
      else if (action.equals("DoReactivate")) doReactivateAu(au);
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
      else if (action.equals("Delete")) confirmDeleteAu(au);
      else if (action.equals("Confirm Delete")) doDeleteAu(au);
      else {
	errMsg = "Unknown action: " + action;
	displayAuSummary();
      }
    }
    resetLocals();
  }

  /** Display "Add Archival Unit" button and list of configured AUs with Edit
   * buttons */
  private void displayAuSummary() throws IOException {
    Page page = newPage();
    addJavaScript(page);
    page.add(getErrBlock());
    page.add(getExplanationBlock("Add a new Archival Unit, or edit an existing one."));
    Form frm = new Form(srvURL(myServletDescr(), null));
    frm.method("POST");
    // make form findable by unit tests
    frm.attribute("id", "AuSummaryForm");
    frm.add("<input type=hidden name=\"" + ACTION_TAG + "\">");
    addAuId(frm, null);
    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");
    // make table findable by unit tests
    tbl.attribute("id", "AuSummaryTable");
    addAddAuRow(tbl);
    Collection allAUs = remoteApi.getAllAus();
    if (!allAUs.isEmpty()) {
      for (Iterator iter = allAUs.iterator(); iter.hasNext(); ) {
	addAuSummaryRow(tbl, (AuProxy)iter.next());
      }
    }
    Collection inactiveAUs = remoteApi.getInactiveAus();
    if (!inactiveAUs.isEmpty()) {
      for (Iterator iter = inactiveAUs.iterator(); iter.hasNext(); ) {
	addAuSummaryRow(tbl, (AuProxy)iter.next());
      }
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

    page.add(getErrBlock());
    page.add(getExplanationBlock("Editing configuration of: " +
				 encodedAuName(au)));

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

    page.add(getErrBlock());
    page.add(getExplanationBlock("Restoring configuration of: " +
				 encodedAuName(au)));

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
    fetchAuConfig(au);
    if (plugin == null) {
      errMsg = "Unknown plugin: " + au.getPluginId() +
	"<br>Cannot reactivate: " + encodeText(au.getName());
      displayAuSummary();
      return;
    }
    page.add(getErrBlock());
    page.add(getExplanationBlock("Reactivating: " + encodedAuName(au)));

    java.util.List actions =
      ListUtil.list(new Input(Input.Hidden, ACTION_TAG, "DoReactivate"),
		    new Input(Input.Submit, "button", "Reactivate"));
    Form frm = createAuEditForm(actions, au, true);
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
// 	String pclass = req.getParameter("PluginClass");
// 	if (!StringUtil.isNullString(pclass)) {
// 	  pid = remoteApi.pluginIdFromName(pclass);
// 	}
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
    page.add(getErrBlock());
    page.add(getExplanationBlock((StringUtil.isNullString(title)
				  ? "Creating new Archival Unit"
				  : ("Creating new Archival Unit of " +
				     encodeText(title))) +
				 " with plugin: " +
				 encodeText(plugin.getPluginName()) +
				 "<br>Edit the parameters then click Create"));

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
    page.add(getErrBlock());
    String addExp = "Adding new Archival Unit" +
      "<br>First, select a title or a publisher plugin." +
      addFootnote("Configuring an AU requires choosing a publisher-dependent"
		  + " plugin, then filling in a set of parameters.  "
		  + "Predefined titles implicitly specify a plugin and "
		  + "some of the parameter values.");
    page.add(getExplanationBlock(addExp));

    Form frm = new Form(srvURL(myServletDescr(), null));
    frm.method("POST");
//     frm.add("<center>");
    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");

    ArrayList titles = new ArrayList(remoteApi.findAllTitles());
    Collections.sort(titles);
    if (!titles.isEmpty()) {
      boolean includePluginInTitleSelect =
	configMgr.getBooleanParam(PARAM_INCLUDE_PLUGIN_IN_TITLE_SELECT,
				  DEFAULT_INCLUDE_PLUGIN_IN_TITLE_SELECT);
      tbl.newRow();
      tbl.newCell("colspan=3 align=center");
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
      tbl.add(sel);
      addOr(tbl);
    }

    Map pMap = getPluginNameMap();
    if (!pMap.isEmpty()) {
      tbl.newRow();
      tbl.newCell("colspan=3 align=center");
      tbl.add("Choose a publisher plugin:<br>");
      Select sel = new Select("PluginId", false);
      sel.attribute("id", "plugin_sel");
      sel.attribute("onchange",
		    "cascadeSelectEnable(this,'plugin_input')");
      sel.add("-no selection-", true, "");
      for (Iterator iter = pMap.keySet().iterator(); iter.hasNext(); ) {
	String pName = (String)iter.next();
	PluginProxy p = (PluginProxy)pMap.get(pName);
	sel.add(encodeText(pName), false, p.getPluginId());
      }
      tbl.add(sel);
      addOr(tbl);
    }
    tbl.newRow();
    tbl.newCell("colspan=3 align=center");
    tbl.add("Enter the class name of a plugin:<br>");
    Input in = new Input(Input.Text, "PluginClass");
    in.setSize(40);
    in.attribute("id", "plugin_input");
    tbl.add(in);
    tbl.newRow();
    tbl.newCell("colspan=3 align=center");
    tbl.add("<br>");
    tbl.add("Then click to edit parameter values");
    tbl.add("<br>");
    tbl.add(new Input(Input.Hidden, ACTION_TAG, "EditNew"));
    tbl.add(new Input(Input.Submit, "button", "Continue"));
    frm.add(tbl);
    page.add(frm);
    page.add("</center><br>");

    endPage(page);
  }

  SortedMap getPluginNameMap() {
    SortedMap pMap = new TreeMap();
    for (Iterator iter = remoteApi.getRegisteredPlugins().iterator();
	 iter.hasNext(); ) {
      PluginProxy plugin = (PluginProxy)iter.next();
      String name = plugin.getPluginName();
      if (name != null) {
	pMap.put(name, plugin);
      }
    }
    return pMap;
  }

  void addOr(Table tbl) {
    tbl.newRow();
    tbl.newCell("align=right");
    tbl.add("<hr align=right width=100>");
    tbl.newCell("align=center");
    tbl.add("or");
    tbl.newCell("align=left");
    tbl.add("<hr align=left width=100>");
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

    Form frm = new Form(srvURL(myServletDescr(), null));
    frm.method("POST");

    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");
    tbl.newRow();
    tbl.newCell("colspan=2 align=center");
    tbl.add("Archival Unit Definition");
//     tbl.add(isNew ? "Defining Properties" : "Fixed Properties");

//     addPropRows(tbl, getDefKeys(), initVals,
// 		(isNew ? getDefKeys() : null));
    addPropRows(tbl, getDefKeys(), initVals,
 		(isNew
 		 ? (org.apache.commons.collections.CollectionUtils.
 		    subtract(getDefKeys(), noEditKeys))
 		 : null));

    tbl.newRow();
    tbl.newRow();
    tbl.newCell("colspan=2 align=center");
    Collection eKeys = getEditKeys();
    if (eKeys.isEmpty()) {
      if (!isNew && editable) {
// 	tbl.add("No Editable Properties");
      }
    } else {
      tbl.add("Other Parameters");
      addPropRows(tbl, eKeys, initVals, editable ? eKeys : null);

      tbl.newRow();
      tbl.newCell("colspan=2 align=center");
    }
    if (isNew) {
      addPlugId(tbl, plugin);
    } else {
      addAuId(tbl, au);
    }
    for (Iterator iter = actions.iterator(); iter.hasNext(); ) {
      Object act = iter.next();
      if (act instanceof String) {
	tbl.add(new Input(Input.Submit, ACTION_TAG, (String)act));
	if (iter.hasNext()) {
	  tbl.add("&nbsp;");
	}
      } else {
	tbl.add(act);
      }
    }
    frm.add(tbl);
    return frm;
  }

  /** Add config props rows to edit AU table.
   * @param tbl the table
   * @param keys the kwys to include (subset of those from getAuConfigParams())
   * @param initVals initial values of fields, or null
   * @param editableKeys the keys whose values may be edited
   */
  private void addPropRows(Table tbl, Collection keys, Configuration initVals,
		      Collection editableKeys) {
    for (Iterator iter = getAuConfigParams().iterator(); iter.hasNext(); ) {
      ConfigParamDescr descr = descrFromObj(iter.next());
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
    createAuFromPlugin("Created");
  }

  /** Process the DoReactivate button */
  private void doReactivateAu(AuProxy aup) throws IOException {
    plugin = aup.getPlugin();
    if (plugin == null) {
      errMsg = "Can't find plugin: " + aup.getPluginId();
      displayAddAu();
      return;
    }
    if (aup.isActiveAu()) {
      updateAu(aup, "Reactivated");
    } else {
      createAuFromPlugin("reactivated");
    }
  }

  private void createAuFromPlugin(String msg) throws IOException {
    formConfig = getAuConfigFromForm(true);
    try {
      AuProxy au =
	remoteApi.createAndSaveAuConfiguration(plugin, formConfig);
      statusMsg = msg + " Archival Unit:<br>" + encodeText(au.getName());
      displayEditAu(au);
    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Error configuring AU", e);
      errMsg = "Error configuring AU:<br>" + encodeText(e.getMessage());
      displayEditNew();
    } catch (IOException e) {
      log.error("Error saving AU configuration", e);
      errMsg = "Error saving AU configuration:<br>" +
	encodeText(e.getMessage());
      displayEditNew();
    }
  }

  /** Process the Update button */
  private void updateAu(AuProxy au, String msg) throws IOException {
    fetchAuConfig(au);
    Configuration formConfig = getAuConfigFromForm(false);
    if (isChanged(auConfig, formConfig) ||
	isChanged(remoteApi.getStoredAuConfiguration(au), formConfig)) {
      try {
	remoteApi.setAndSaveAuConfiguration(au, formConfig);
	statusMsg = msg + " Archival Unit:<br>" + encodeText(au.getName());
      } catch (ArchivalUnit.ConfigurationException e) {
	log.error("Couldn't reconfigure AU", e);
	errMsg = encodeText(e.getMessage());
      } catch (IOException e) {
	log.error("Couldn't save AU configuraton", e);
	errMsg = "Error saving AU:<br>" + encodeText(e.getMessage());
	displayEditAu(au);
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

    page.add(getErrBlock());
    page.add(getExplanationBlock("Are you sure you want to delete" +
				 addFootnote(deleteFoot) + ": " +
				 encodedAuName(au)));

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
      statusMsg = "Deleted Archival Unit:<br>" + encodeText(name);
    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Can't happen", e);
      errMsg = encodeText(e.getMessage());
    } catch (IOException e) {
      log.error("Couldn't save AU configuraton", e);
      errMsg = "Error deleting AU:<br>" + encodeText(e.getMessage());
    }
    displayAuSummary();
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

    page.add(getErrBlock());
    page.add(getExplanationBlock("Are you sure you want to deactivate" +
				 addFootnote(deactivateFoot) + ": " +
				 encodedAuName(au)));

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
    } catch (ArchivalUnit.ConfigurationException e) {
      log.error("Can't happen", e);
      errMsg = encodeText(e.getMessage());
    } catch (IOException e) {
      log.error("Couldn't save AU configuraton", e);
      errMsg = "Error deactivating AU:<br>" + encodeText(e.getMessage());
    }
    displayAuSummary();
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
    return ConfigManager.fromProperties(p);
  }

  /** True iff both values are null (or null strings) or equal strings */
  private boolean isEqualFormVal(String formVal, String oldVal) {
    return (StringUtil.isNullString(formVal))
      ? StringUtil.isNullString(oldVal)
      : formVal.equals(oldVal);
    }

  /** Create message and error message block */
  private Composite getErrBlock() {
    Composite comp = new Composite();
    if (errMsg != null) {
      comp.add("<center><font color=red size=+1>");
      comp.add(errMsg);
      comp.add("</font></center><br>");
    }
    if (statusMsg != null) {
      comp.add("<center><font size=+1>");
      comp.add(statusMsg);
      comp.add("</font></center><br>");
    }
    return comp;
  }

  /** Return a button that invokes the javascript submit routine with the
   * specified action */
  protected Element submitButton(String label, String action) {
    return submitButton(label, action, null, null);
  }


  /** Return a button that invokes the javascript submit routine with the
   * specified action, first storing the value in the specified form
   * prop. */
  protected Element submitButton(String label, String action,
				 String prop, String value) {
    Tag btn = new Tag("input");
    btn.attribute("type", "button");
    btn.attribute("value", label);
    btn.attribute("id", "lsb." + (++submitButtonNumber));
    StringBuffer sb = new StringBuffer(40);
    sb.append("lockssButton(this, '");
    sb.append(action);
    sb.append("'");
    if (prop != null && value != null) {
      sb.append(", '");
      sb.append(prop);
      sb.append("', '");
      sb.append(value);
      sb.append("'");
    }
    sb.append(")");
    btn.attribute("onClick", sb.toString());
    return btn;
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
    page.add(getFooter());
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
    auConfigParams = new ArrayList(plugin.getAuConfigProperties());
    // let the plugin specify the order
    // Collections.sort(auConfigParams);
    defKeys = plugin.getDefiningConfigKeys();
    editKeys = new ArrayList();
    for (Iterator iter = auConfigParams.iterator(); iter.hasNext(); ) {
      ConfigParamDescr descr = descrFromObj(iter.next());
      if (!defKeys.contains(descr.getKey())) {
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

  ConfigParamDescr descrFromObj(Object obj) {
    if (obj instanceof ConfigParamDescr) {
      return (ConfigParamDescr)obj;
    }
    return new ConfigParamDescr(obj.toString());
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
