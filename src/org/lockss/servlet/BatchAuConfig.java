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

import java.io.*;
import java.util.*;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.HttpSession;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.lang3.mutable.*;
import org.lockss.config.*;
import org.lockss.daemon.TitleSet;
import org.lockss.plugin.PluginManager;
import org.lockss.servlet.ServletUtil.LinkWithExplanation;
import org.lockss.util.*;
import org.mortbay.html.*;

/** Create and update AU configuration.
 */
@SuppressWarnings("serial")
public class BatchAuConfig extends LockssServlet {

  private static final int LONG_TABLE_AT_LEAST = 10;

  /** Controls the appearance (in select lists) of TitleSets that contain
   * no actionable AUs.  If included, they are greyed. <ul><li><b>All</b>
   * &mdash; they are always included,<li><b>Add</b> &mdash; they are
   * included only in the Add AUs selection, omitted in
   * others,<li><b>None</b> &mdash; they are not included.</ul> */
  static final String PARAM_GREY_TITLESET_ACTION =
    Configuration.PREFIX + "batchAuconfig.greyNonActionableTitleSets";
  static final String DEFAULT_GREY_TITLESET_ACTION = "Add";

  static final String FOOT_REPO_CHOICE =
    "If only one choice is shown for an AU, the contents of that AU " +
    "already exist in the selected disk.";

  private static final Logger log = Logger.getLogger(BatchAuConfig.class);

  static final int VV_ADD = 1;
  static final int VV_DEL = 2;
  static final int VV_DEACT = 3;
  static final int VV_REACT = 4;
  static final int VV_RESTORE = 5;
  static final int VV_BACKUP = 6;

  static final Verb VERB_ADD = new Verb(VV_ADD, "add", "added", true);
  static final Verb VERB_DEL = new Verb(VV_DEL, "remove", "removed", false);
  static final Verb VERB_DEACT =
    new Verb(VV_DEACT, "deactivate", "deactivated", false);
  static final Verb VERB_REACT =
    new Verb(VV_REACT, "reactivate", "reactivated", true);
  static final Verb VERB_RESTORE =
    new Verb(VV_RESTORE, "restore", "restored", true);
  static final Verb VERB_BACKUP =
    new Verb(VV_BACKUP, "backup", "backup", true);
  static final Verb[] verbs = {VERB_ADD, VERB_DEL,
			       VERB_DEACT, VERB_REACT,
			       VERB_RESTORE, VERB_BACKUP};

  static final String ACTION_SELECT_SETS_TO_ADD = "AddAus";
  static final String ACTION_SELECT_SETS_TO_DEL = "RemoveAus";
  static final String ACTION_SELECT_SETS_TO_DEACT = "DeactivateAus";
  static final String ACTION_SELECT_SETS_TO_REACT = "ReactivateAus";
  static final String ACTION_BACKUP = "SelectBackup";
  static final String ACTION_RESTORE = "Restore";
  static final String ACTION_SELECT_RESTORE_TITLES = "SelectRestoreTitles";
  static final String ACTION_SELECT_AUS = "SelectSetAus";
  static final String ACTION_ADD_AUS = "DoAddAus";
  static final String ACTION_REMOVE_AUS = "DoRemoveAus";
  static final String ACTION_DEACT_AUS = "DoDeactivateAus";
  static final String ACTION_REACT_AUS = "DoReactivateAus";
  static final String ACTION_RESTORE_AUS = "DoAddAus";
  static final String ACTION_DO_BACKUP = "Backup";

  static final String KEY_VERB = "Verb";
  static final String KEY_DEFAULT_REPO = "DefaultRepository";
  static final String KEY_REPO = "Repository";
  static final String KEY_TITLE_SET = "TitleSetId";
  static final String KEY_AUID = "auid";
  static final String KEY_REPOS = "repos";

  static final String SESSION_KEY_REPO_MAP = "RepoMap";
  static final String SESSION_KEY_AUID_MAP = "AuidMap";
  static final String SESSION_KEY_BACKUP_INFO = "BackupInfo";

  private PluginManager pluginMgr;

  String action;			// action request by form
  Verb verb;

  // don't hold onto objects after request finished
  protected void resetLocals() {
    verb = null;
    super.resetLocals();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginMgr = getLockssDaemon().getPluginManager();
  }

  protected void lockssHandleRequest() throws IOException {
    errMsg = null;
    statusMsg = null;

    // If the AUs are not started, don't display any AU summary or
    // any form inputs.
    if (!pluginMgr.areAusStarted()) {
      displayNotStarted();
      return;
    }
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
    String s = getParameter(KEY_VERB);
    if (!StringUtil.isNullString(s)) {
      try {
	verb = findVerb(s);
      } catch (IllegalVerb e) {
	errMsg = "Illegal Verb: " + s;
	action = null;
      }
    }

    if (StringUtil.isNullString(action)) displayMenu();
    else if (action.equals(ACTION_SELECT_SETS_TO_ADD)) chooseSets(VERB_ADD);
    else if (action.equals(ACTION_SELECT_SETS_TO_DEL)) chooseSets(VERB_DEL);
    else if (action.equals(ACTION_SELECT_SETS_TO_DEACT)) chooseSets(VERB_DEACT);
    else if (action.equals(ACTION_SELECT_SETS_TO_REACT)) chooseSets(VERB_REACT);
    else if (action.equals(ACTION_BACKUP)) displayBackup();
    else if (action.equals(ACTION_RESTORE)) displayRestore();
    else if (action.equals(ACTION_SELECT_AUS)) chooseAus();
    else if (action.equals(ACTION_SELECT_RESTORE_TITLES)) selectRestoreTitles();
    else if (action.equals(ACTION_DO_BACKUP)) doBackup();
    else if (action.equals(ACTION_REMOVE_AUS)) doRemoveAus(false);
    else if (action.equals(ACTION_DEACT_AUS)) doRemoveAus(true);
    else {
      errMsg = "Unknown action: " + action;
      log.warning(errMsg);
      displayMenu();
    }
  }

  private Iterator<LinkWithExplanation> getMenuDescriptors() {
    String ACTION = ACTION_TAG + "=";
    int numActive = 0;
    boolean someInactive = false;
    ServletDescr myDescr = myServletDescr();
    ArrayList<LinkWithExplanation> list =
	new ArrayList<LinkWithExplanation>(10); // at most 10 entries

    // Add and remove
    list.add(getMenuDescriptor(myDescr,
                               "Add AUs",
                               ACTION + ACTION_SELECT_SETS_TO_ADD,
                               "Add one or more groups of archival units"));
    if (numActive > 0 || isDebugUser() ) {
      list.add(getMenuDescriptor(myDescr,
                                 "Remove AUs",
                                 ACTION + ACTION_SELECT_SETS_TO_DEL,
                                 "Remove selected archival units",
                                 numActive > 0));
    }

    if (isDebugUser() ) {
      // Deactivate and reactivate
      list.add(getMenuDescriptor(myDescr,
                                 "Deactivate AUs",
                                 ACTION + ACTION_SELECT_SETS_TO_DEACT,
                                 "Deactivate selected archival units",
                                 numActive > 0));
      list.add(getMenuDescriptor(myDescr,
                                 "Reactivate AUs",
                                 ACTION + ACTION_SELECT_SETS_TO_REACT,
                                 "Reactivate selected archival units",
                                 someInactive));
    }

    // Backup and restore
    list.add(getMenuDescriptor(myDescr,
                               "Backup",
                               ACTION + ACTION_BACKUP,
                               "Backup cache config to a file on your workstation",
                               numActive > 0 || someInactive));
    list.add(getMenuDescriptor(myDescr,
                               "Restore",
                               ACTION + ACTION_RESTORE,
                               "Restore cache config from a file on your workstation"));

    // Manual edit
    list.add(getMenuDescriptor(AdminServletManager.SERVLET_AU_CONFIG,
                               "Manual Add/Edit",
                               null,
                               "Add, Edit or Delete an individual AU"));

    return list.iterator();
  }

  /** Display top level batch config choices */
  private void displayMenu() throws IOException {
    Page page = newPage();
    layoutErrorBlock(page);
    ServletUtil.layoutMenu(page, getMenuDescriptors());
    endPage(page);
  }

  private void chooseSets(Verb verb) throws IOException {
    this.verb = verb;

    // Begin page
    Page page = newPage();
    addJavaScript(page);
    layoutErrorBlock(page);

    // Prepare sets
    Collection titleSets = pluginMgr.getTitleSets();
    if (titleSets.isEmpty()) {
      ServletUtil.layoutExplanationBlock(page, "No titlesets are defined.");
    } else {
      String grayAction = CurrentConfig.getParam(PARAM_GREY_TITLESET_ACTION,
						 DEFAULT_GREY_TITLESET_ACTION);
      boolean doGray = "All".equalsIgnoreCase(grayAction) ||
	(verb == VERB_ADD && "Add".equalsIgnoreCase(grayAction));
      MutableBoolean isAnySelectable = new MutableBoolean(false);
      MutableInt buttonNumber = new MutableInt(submitButtonNumber);
      submitButtonNumber = buttonNumber.intValue();

      if (isAnySelectable.booleanValue()) {
	// Display set chooser
	ServletUtil.layoutExplanationBlock(page,
					   "Select one or more collections of AUs to " + verb.word + ", then click \"Select AUs\".");
      } else {
	// Set chooser not needed
	String msg = "All AUs in all predefined collections of AUs " +
	  "already exist on this LOCKSS box.";
	ServletUtil.layoutExplanationBlock(page, msg);
      }
    }
    endPage(page);
  }

  private void chooseAus() throws IOException {
    // Gather title sets
    String[] setNames = req.getParameterValues(KEY_TITLE_SET);

    // Do chooseSets() if none
    if (setNames == null || setNames.length == 0) {
      errMsg = "You must select at least one title set.";
      chooseSets(verb);
      return;
    }

    // Find AUs in sets
    Collection sets = findTitleSetsFromNames(setNames);

      errMsg = "The selected sets contain no AUs.";
      chooseSets(verb);
      return;
  }

  Collection findTitleSetsFromNames(String[] names) {
    Map map = pluginMgr.getTitleSetMap();
    Set res = new HashSet(names.length);
    for (int ix = 0; ix < names.length; ix++) {
      TitleSet ts = (TitleSet)map.get(names[ix]);
      if (ts != null) {
	res.add(ts);
      }
    }
    return res;
  }

  private void doAddAus(int addOp) throws IOException {
    if (!hasSession()) {
      errMsg = "Please enable cookies";
      displayMenu();
      return;
    }

    // Get the HTTP session.
    HttpSession session = getSession();

    // Get the repositories stored in the session.
    LinkedMap repoMap = (LinkedMap)session.getAttribute(SESSION_KEY_REPO_MAP);

    // Get the archival unit identifiers specified in the form.
    String[] auids = req.getParameterValues(KEY_AUID);

    // Check whether no archival unit identifiers were specified in the form.
    if (auids == null || auids.length == 0) {
      // Yes: Report the problem.
      errMsg = "No AUs were selected";
      displayMenu();
      return;
    }

    // Get the default repository index from the form.
    String defRepoId = getParameter(KEY_DEFAULT_REPO);

    // Get the archival unit configurations stored in the session.
    Map<String, Configuration> auConfs =
	(Map<String, Configuration>)session.getAttribute(SESSION_KEY_AUID_MAP);

    // The archival unit repository indices.
    Map<String, String> auRepoIds = new HashMap<String, String>();

    // Loop through all the archival unit identifiers.
    for (int ix = 0; ix < auids.length; ix++) {
      // Populate the repository for the archival unit in the map.
      String auid = auids[ix];
      String repoId = getParameter(KEY_REPO + "_" + auid);
      auRepoIds.put(auid, repoId);
    }
  }

  private void doRemoveAus(boolean isDeactivate) throws IOException {
    if (!hasSession()) {
      errMsg = "Please enable cookies";
      displayMenu();
      return;
    }

    String[] auidArr = req.getParameterValues(KEY_AUID);
    if (auidArr == null || auidArr.length == 0) {
      errMsg = "No AUs were selected";
      displayMenu();
      return;
    }
    List auids = ListUtil.fromArray(auidArr);
  }

  /** Display the Backup page */
  private void displayBackup() throws IOException {
    Page page = newPage();
    addJavaScript(page);
    layoutErrorBlock(page);
    MutableInt buttonNumber = new MutableInt(submitButtonNumber);
    submitButtonNumber = buttonNumber.intValue();
    endPage(page);
  }

  /** Serve the contents of the local AU config file, as
   * application/binary */
  private void doBackup() throws IOException {
    boolean forceCreate = "true".equalsIgnoreCase(req.getParameter("create"));
      errMsg = "No AUs have been configured - nothing to backup";
      displayMenu();
  }

  /** Display the Restore page */
  private void displayRestore() throws IOException {
    Page page = newPage();
    addJavaScript(page);
    layoutErrorBlock(page);
    MutableInt buttonNumber = new MutableInt(submitButtonNumber);
    ServletUtil.layoutRestore(this, page, ACTION_TAG, KEY_VERB,
        VERB_RESTORE, "AuConfigBackupContents", buttonNumber,
        ACTION_SELECT_RESTORE_TITLES);
    submitButtonNumber = buttonNumber.intValue();
    endPage(page);
  }

  private void selectRestoreTitles() throws IOException {
    InputStream ins = multiReq.getInputStream("AuConfigBackupContents");
    if (ins == null) {
      errMsg = "No backup file uploaded";
      displayRestore();
    } else {
	  errMsg = "Backup file is empty";
	  displayRestore();
	  return;
    }
  }

  /** Common and page adds Back link, footer */
  protected void endPage(Page page) throws IOException {
    if (action != null) {
      ServletUtil.layoutBackLink(page,
          srvLink(myServletDescr(), "Back to Journal Configuration"));
    }
    super.endPage(page);
  }

  // make me a link in nav table if not on initial journal config page
  protected boolean linkMeInNav() {
    return action != null;
  }

  // Verb support
  Verb findVerb(String valStr) throws IllegalVerb {
    try {
      int val = Integer.parseInt(valStr);
      return verbs[val-1];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalVerb();
    } catch (NumberFormatException e) {
      throw new IllegalVerb();
    }
  }

  static class Verb {
    int val;
    String valStr;
    String word;
    String cap;
    String past;
    boolean isAdd;
    Verb(int val, String word, String past, boolean isAdd) {
      this.val = val;
      this.valStr = Integer.toString(val);
      this.word = word;
      this.cap = word.substring(0,1).toUpperCase() + word.substring(1);
      this.past = past;
      this.isAdd = isAdd;
    }

    String action() {
      switch (val) {
      case VV_ADD:
	return ACTION_ADD_AUS;
      case VV_DEL:
	return ACTION_REMOVE_AUS;
      case VV_DEACT:
	return ACTION_DEACT_AUS;
      case VV_REACT:
	return ACTION_REACT_AUS;
      case VV_RESTORE:
	return ACTION_RESTORE_AUS;
      case VV_BACKUP:
	return ACTION_DO_BACKUP;
      }
      log.error("Unknown action " + val, new Throwable());
      return "UnknownAction";
    }

    boolean isTsAppropriateFor(TitleSet ts) {
      switch (val) {
      case VV_ADD:
	return ts.isSetActionable(TitleSet.SET_ADDABLE);
      case VV_DEL:
      case VV_DEACT:
	return ts.isSetActionable(TitleSet.SET_DELABLE);
      case VV_REACT:
	return ts.isSetActionable(TitleSet.SET_REACTABLE);
      }
      return false;
    }
  }

  static class IllegalVerb extends Exception {
  }
}
