/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import javax.servlet.*;
import org.apache.commons.lang3.mutable.MutableInt;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
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
    " is required to collect and preserve content. " +
    "Manual configuration of a plugin should be used with care, " +
    "and should not be necessary for normal usage of a LOCKSS box. " +
    "It will only work with sites of the same type " +
    "for which the plugin was written.";

  private static final Logger log = Logger.getLogger(AuConfig.class);

  static final String REPO_TAG = "lockssRepository";

  // prefix added to config prop keys when used in form, to avoid
  // accidental collisions
  static final String FORM_PREFIX = "lfp.";

  private static final String ACTION_ADD = "Add";
  private static final String ACTION_ADD_BY_AUID = "AddByAuid";
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

  String action;			// action request by form
  Configuration auConfig;		// current config from AU
  Configuration formConfig;		// config read from form
  TitleConfig titleConfig;		// config specified by title DB
  java.util.List auConfigParams;
  Collection defKeys;			// plugin's definitional keys
  java.util.List editKeys;		// non-definitional keys

  // don't hold onto objects after request finished
  protected void resetLocals() {
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
  }

  protected void lockssHandleRequest() throws IOException {
    errMsg = null;
    statusMsg = null;
    formConfig = null;
    titleConfig = null;

    action = getParameter(ACTION_TAG);
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

    String auid = getParameter("auid");

    if (StringUtil.isNullString(action)) displayAuSummary();
    else if (action.equals(ACTION_ADD)) displayAddAu();
    else if (action.equals(ACTION_ADD_BY_AUID)) doAddByAuid();
    else if (action.equals("EditNew")) displayEditNew();
    else if (action.equals(ACTION_CREATE)) createAu();
    else if (  action.equals(ACTION_REACTIVATE)
             || action.equals(ACTION_DO_REACTIVATE)
             || action.equals(ACTION_DELETE)
             || action.equals(ACTION_CONFIRM_DELETE)) {
	if (auid != null) {
	  errMsg = "Invalid AuId: " + auid;
	}
	displayAuSummary();
    } else {
	errMsg = "Invalid AuId: " + auid;
	displayAuSummary();
    }
  }

  /** Display "Add Archival Unit" button and list of configured AUs with Edit
   * buttons */
  private void displayAuSummary() throws IOException {
    // If the AUs are not started, don't display any AU summary or any form
    // inputs.
    if (!pluginMgr.areAusStarted()) {
      displayNotStarted();
      return;
    }

    Page page = newPage();
    addJavaScript(page);
    layoutErrorBlock(page);

    MutableInt buttonNumber = new MutableInt(submitButtonNumber);
    submitButtonNumber = buttonNumber.intValue();

    endPage(page);
  }

  /** Display form to add a new AU */
  private void displayEditNew() throws IOException {
    String title = getParameter("Title");
    if (!StringUtil.isNullString(title)) {
	errMsg = "Unknown title: " + encodeText(title);
	displayAddAu();
	return;
    } else {
      String pid = getParameter("PluginId");
      if (StringUtil.isNullString(pid)) {
	pid = getParameter("PluginClass");
//	String pclass = getParameter("PluginClass");
//	if (!StringUtil.isNullString(pclass)) {
//	  pid = remoteApi.pluginIdFromName(pclass);
//	}
      }
	if (StringUtil.isNullString(pid)) {
	  errMsg = "Please choose a title or a plugin.";
	} else {
	  errMsg = "Can't find plugin: " + pid;
	}
	displayAddAu();
	return;
    }
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
    frm.attribute("id", "AddAuForm");
    frm.method("POST");
//     frm.add("<center>");
    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");


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

  void addRepoChoice(Composite comp) {
  }

  /** Process the Create button */
  private void createAu() throws IOException {
    String pid = getParameter("PluginId");
      errMsg = "Can't find plugin: " + pid;
      displayAddAu();
      return;
  }

  private void createAuFromPlugin(String msg, boolean isNew)
      throws IOException {
    formConfig = getAuConfigFromForm(isNew);
    if (isNew) {
      String repo = getParameter(REPO_TAG);
      if (StringUtil.isNullString(repo)) {
	formConfig.put(PluginManager.AU_PARAM_REPOSITORY, repo);
      } else {
	  errMsg = "Nonexistent repository: " + repo;
	  displayAddAu();
	  return;
      }
    }
      displayAuSummary();
      return;
  }

  private void doAddByAuid() throws IOException {
    displayAddResult();
  }

  public static final String AU_PARAM_PREFIX = "auparam_";

  private Configuration getAdditionalAuConfig() {
    Configuration config = ConfigManager.newConfiguration();
    for (Enumeration en = req.getParameterNames(); en.hasMoreElements(); ) {
      String name = (String)en.nextElement();
      if (name.startsWith(AU_PARAM_PREFIX)) {
	String param = name.substring(AU_PARAM_PREFIX.length());
	config.put(param, req.getParameter(name));
      }
    }
    return config;
  }

  /** Display result of addByAuid */
  private void displayAddResult() throws IOException {
    Page page = newPage();
    addJavaScript(page);
    layoutErrorBlock(page);
    endPage(page);
  }

  /** Put a value from the config form into the properties, iff it is set
   * in the form */
  private void putFormVal(Properties p, String key) {
    String val = getParameter(formKeyFromKey(key));
    // Must treat empty string as unset param.
    if (!StringUtil.isNullString(val)) {
      p.put(key, val);
    }
  }

  /** Return true iff newConfig is different from oldConfig */
  boolean isChanged(Configuration oldConfig, Configuration newConfig) {
    Collection<String> dk = oldConfig.differentKeys(newConfig);
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

  /** Common and page adds Back link, footer */
  protected void endPage(Page page) throws IOException {
    if (action != null) {
      page.add("<center>");
      page.add(srvLink(myServletDescr(), "Back to Journal Configuration"));
      page.add("</center>");
    }
    super.endPage(page);
  }

  // make me a link in nav table if not on initial journal config page
  protected boolean linkMeInNav() {
    return action != null;
  }

  void prepareConfigParams() {
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
      } else if (allowEditDefaultOnly || !descr.isDefaultOnly()
		 && !descr.isDerived()) {
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
}
