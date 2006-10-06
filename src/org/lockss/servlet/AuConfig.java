/*
 * $Id: AuConfig.java,v 1.59 2006-10-06 20:11:28 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.util.*;
import java.util.List;

import javax.servlet.*;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.remote.*;
import org.lockss.util.*;
import org.mortbay.html.*;

/** Create and update AU configuration.
 */
public class AuConfig extends LockssServlet {

  private static final String ACTION_CREATE = "Create";
  static final String PARAM_INCLUDE_PLUGIN_IN_TITLE_SELECT =
    Configuration.PREFIX + "auconfig.includePluginInTitleSelect";
  static final boolean DEFAULT_INCLUDE_PLUGIN_IN_TITLE_SELECT = false;

  static final String PARAM_ALLOW_EDIT_DEFAULT_ONLY_PARAMS =
    Configuration.PREFIX + "auconfig.allowEditDefaultOnlyParams";
  static final boolean DEFAULT_ALLOW_EDIT_DEFAULT_ONLY_PARAMS = false;

  static final String FOOT_REPOSITORY =
    "Local disk on which AU will be stored";

  static final String FOOT_CHOOSEPLUGWARN =
    "A LOCKSS " + new Link("http://www.lockss.org/lockss/Plugins", "plugin") +
    "is required to collect and preserve content. " +
    "Manual configuration of a plugin should be used with care. " +
    "It will only work with sites of the same type " +
    "for which the plugin was written.";

  static Logger log = Logger.getLogger("AuConfig");

  static final String REPO_TAG = "lockssRepository";

  // prefix added to config prop keys when used in form, to avoid
  // accidental collisions
  static final String FORM_PREFIX = "lfp.";

  private static final String ACTION_ADD = "Add";
  private static final String ACTION_RESTORE = "Restore";
  private static final String ACTION_DO_RESTORE = "DoRestore";
  private static final String ACTION_REACTIVATE = "Reactivate";
  private static final String ACTION_DO_REACTIVATE = "DoReactivate";
  private static final String ACTION_EDIT = "Edit";
  private static final String ACTION_DELETE = "Delete";
  private static final String ACTION_DEACTIVATE = "Deactivate";
  private static final String ACTION_CONFIRM_DELETE = "Confirm Delete";

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
    else if (action.equals(ACTION_ADD)) displayAddAu();
    else if (action.equals("EditNew")) displayEditNew();
    else if (action.equals(ACTION_CREATE)) createAu();
    else if (  action.equals(ACTION_REACTIVATE)
             || action.equals(ACTION_DO_REACTIVATE)
             || action.equals(ACTION_DELETE)
             || action.equals(ACTION_CONFIRM_DELETE)) {
      AuProxy au = getAuProxy(auid);
      if (au == null) {
	au = getInactiveAuProxy(auid);
      }
      if (au == null) {
	if (auid != null) {
	  errMsg = "Invalid AuId: " + auid;
	}
	displayAuSummary();
      } else if (action.equals(ACTION_REACTIVATE)) displayReactivateAu(au);
      else if (action.equals(ACTION_DO_REACTIVATE)) doReactivateAu(au);
      else if (action.equals(ACTION_DELETE)) confirmDeleteAu(au);
      else if (action.equals(ACTION_CONFIRM_DELETE)) doDeleteAu(au);
    } else {
      // all other actions require AU.  If missing, display summary page
      AuProxy au = getAuProxy(auid);
      if (au == null) {
	errMsg = "Invalid AuId: " + auid;
	displayAuSummary();
      } else if (action.equals(ACTION_EDIT)) displayEditAu(au);
      else if (action.equals(ACTION_RESTORE)) displayRestoreAu(au);
      else if (action.equals(ACTION_DO_RESTORE)) doRestoreAu(au);
      else if (action.equals("Update")) updateAu(au, "Updated");
      else if (action.equals(ACTION_DEACTIVATE)) confirmDeactivateAu(au);
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
    // If the AUs are not started, don't display any AU summary or any form inputs.
    if (!pluginMgr.areAusStarted()) {
      displayNotStarted();
      return;
    }

    // Local variables
    Collection allAus = remoteApi.getAllAus();
    Collection inactiveAus = remoteApi.getInactiveAus();

    Page page = newPage();
    addJavaScript(page);
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page,
          "Add a new Archival Unit"
        + (allAus.isEmpty() ? "." : ", or edit an existing one."));

    MutableInteger buttonNumber = new MutableInteger(submitButtonNumber);
    ServletUtil.layoutAuSummary(this,
                                buttonNumber,
                                remoteApi,
                                page,
                                srvURL(myServletDescr()),
                                "AuSummaryForm",
                                "AuSummaryTable",
                                ACTION_TAG,
                                allAus.iterator(),
                                inactiveAus.iterator(),
                                "auid",
                                ACTION_ADD,
                                ACTION_RESTORE,
                                ACTION_REACTIVATE,
                                ACTION_EDIT);
    submitButtonNumber = buttonNumber.intValue();

    endPage(page);
  }

  /** Display form to edit existing AU */
  private void displayEditAu(AuProxy au) throws IOException {
    Page page = newPage();
    fetchAuConfig(au);

    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page,
        "Editing configuration of: " + encodedAuName(au));

    List actions = ListUtil.list(ACTION_DEACTIVATE, ACTION_DELETE);
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
    ServletUtil.layoutExplanationBlock(page,
        "Restoring configuration of: " + encodedAuName(au));

    List actions = ListUtil.list(
        new Input(Input.Hidden, ACTION_TAG, ACTION_DO_RESTORE),
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
    ServletUtil.layoutExplanationBlock(page,
        "Reactivating: " + encodedAuName(au));

    MutableInteger buttonNumber = new MutableInteger(submitButtonNumber);
    List actions = ListUtil.list(
        ServletUtil.submitButton(this, buttonNumber, "Reactivate", ACTION_DO_REACTIVATE),
        ServletUtil.submitButton(this, buttonNumber, "Delete", ACTION_DELETE));
    submitButtonNumber = buttonNumber.intValue();

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
    ServletUtil.layoutExplanationBlock(page, exp.toString());

    Form frm = createAuEditForm(ListUtil.list(ACTION_CREATE), null, true);
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
    ServletUtil.layoutExplanationBlock(page, addExp);

    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
//     frm.add("<center>");
    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");

    java.util.List titles = remoteApi.findAllTitles();
    if (!titles.isEmpty()) {
      boolean includePluginInTitleSelect =
	CurrentConfig.getBooleanParam(PARAM_INCLUDE_PLUGIN_IN_TITLE_SELECT,
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
	PluginProxy titlePlugin = getTitlePlugin(title);
	if (titlePlugin != null) {
	  TitleConfig tc = titlePlugin.getTitleConfig(title);
	  if (tc != null && AuUtil.isPubDown(tc)) {
	    continue;
	  }
	}
	String selText = encodeText(title);
	String dispText = selText;
	if (includePluginInTitleSelect) {
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

      String choosePub = "Choose a publisher plugin:" +
	addFootnote(FOOT_CHOOSEPLUGWARN) + "<br>";
      tbl.add(choosePub);
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
  private Form createAuEditForm(List actions,
                                AuProxy au,
				boolean editable)
      throws IOException {

    boolean isNew = au == null;
    Collection noEditKeys = Collections.EMPTY_SET;
    Configuration initVals;

    if (titleConfig != null) {
      initVals = titleConfig.getConfig();
      noEditKeys = titleConfig.getUnEditableKeys();
    }
    else if (formConfig != null) {
      initVals = formConfig;
    }
    else if (auConfig != null) {
      initVals = auConfig;
    }
    else {
      initVals = ConfigManager.EMPTY_CONFIGURATION;
    }

    Form frm = ServletUtil.newForm(srvURL(myServletDescr()));

    ServletUtil.layoutAuPropsTable(this,
                                   frm,
                                   getAuConfigParams(),
                                   getDefKeys(),
                                   initVals,
                                   noEditKeys,
                                   isNew,
                                   getEditKeys(),
                                   editable);

    if (isNew) {
      addRepoChoice(frm);
      addPlugId(frm, plugin);
    } else {
      addAuId(frm, au);
    }

    ServletUtil.layoutAuPropsButtons(this,
                                     frm,
                                     actions.iterator(),
                                     ACTION_TAG);

    return frm;
  }

  void addRepoChoice(Composite comp) {
    ServletUtil.layoutRepoChoice(this,
                                 comp,
                                 remoteApi,
                                 FOOT_REPOSITORY,
                                 REPO_TAG);
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

  private void doRestoreAu(AuProxy au) throws IOException {
    updateAu0(au, "Restored", true);
  }

  private void updateAu(AuProxy au, String msg)
      throws IOException {
    updateAu0(au, msg, false);
  }

  /** Process the Update button */
  private void updateAu0(AuProxy au, String msg, boolean forceUpdate)
      throws IOException {
    fetchAuConfig(au);
    Configuration formAuConfig = getAuConfigFromForm(false);
    // AU config params set in global props file (for forcing crawl, etc.)
    // cause latter to see changes even when we don't need to update.
    // compare new config against current only, not stored config.  AU
    boolean checkStored = false;
    if (forceUpdate || isChanged(auConfig, formAuConfig) ||
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
    ServletUtil.layoutExplanationBlock(page, "Are you sure you want to delete" +
	addFootnote(deleteFoot) + ": " + encodedAuName(au));

    Form frm = createAuEditForm(ListUtil.list(ACTION_CONFIRM_DELETE),
				au,
				false);
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
    ServletUtil.layoutExplanationBlock(page, "Are you sure you want to deactivate" +
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
    ServletUtil.layoutAuId(comp, au, "auid");
  }

  /** Add plugin id to form in a hidden field */
  private void addPlugId(Composite comp, PluginProxy plugin) {
    ServletUtil.layoutPluginId(comp, plugin, "PluginId");
  }

  /** Common and page adds Back link, footer */
  protected void endPage(Page page) throws IOException {
    if (action != null) {
      page.add("<center>");
      page.add(srvLink(myServletDescr(), "Back to Journal Configuration"));
      page.add("</center>");
    }
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
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
      CurrentConfig.getBooleanParam(PARAM_ALLOW_EDIT_DEFAULT_ONLY_PARAMS,
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
