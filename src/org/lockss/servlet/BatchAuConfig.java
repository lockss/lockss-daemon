/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.db.DbException;
import org.lockss.plugin.PluginManager;
import org.lockss.remote.RemoteApi;
import org.lockss.remote.RemoteApi.BatchAuStatus;
import org.lockss.servlet.ServletUtil.LinkWithExplanation;
import org.lockss.subscription.SubscriptionManager;
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
  private RemoteApi remoteApi;
  private SubscriptionManager subManager;

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
    remoteApi = getLockssDaemon().getRemoteApi();
    subManager = getLockssDaemon().getSubscriptionManager();
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
    else if (action.equals(ACTION_ADD_AUS))
      doAddAus(RemoteApi.BATCH_ADD_ADD);
    else if (action.equals(ACTION_REACT_AUS))
      doAddAus(RemoteApi.BATCH_ADD_REACTIVATE);
    else if (action.equals(ACTION_RESTORE_AUS))
      doAddAus(RemoteApi.BATCH_ADD_RESTORE);
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
    int numActive = remoteApi.getAllAus().size();
    boolean someInactive = remoteApi.countInactiveAus() > 0;
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

    // Check whether to show the subscriptions links.
    if (subManager.isReady()) {
      // Yes: Add titles to subscription management.
      list.add(getMenuDescriptor(AdminServletManager.SERVLET_SUB_MANAGEMENT,
	  			 SubscriptionManagement.SHOW_ADD_PAGE_LINK_TEXT,
	  			 ACTION
	  			 + SubscriptionManagement.SHOW_ADD_PAGE_ACTION,
	  			 SubscriptionManagement
	  			 .SHOW_ADD_PAGE_HELP_TEXT));

      // Only show the update link if there are subscriptions already.
      try {
	if (subManager.hasSubscriptionRanges()) {
	  // Add titles to subscription management.
	  list.add(getMenuDescriptor(AdminServletManager.SERVLET_SUB_MANAGEMENT,
	      			     SubscriptionManagement
	      			     .SHOW_UPDATE_PAGE_LINK_TEXT,
	      			     ACTION + SubscriptionManagement
	      			     .SHOW_UPDATE_PAGE_ACTION,
	      			     SubscriptionManagement
	      			     .SHOW_UPDATE_PAGE_HELP_TEXT));
	}
      } catch (DbException dbe) {
	log.error("Error counting subscribedPublications", dbe);
      }

      // Add titles to subscription management.
      list.add(getMenuDescriptor(AdminServletManager.SERVLET_SUB_MANAGEMENT,
	  			 SubscriptionManagement
	  			 .AUTO_ADD_SUBSCRIPTIONS_LINK_TEXT,
	  			 ACTION + SubscriptionManagement
	  			 .AUTO_ADD_SUBSCRIPTIONS_ACTION,
	  			 SubscriptionManagement
	  			 .AUTO_ADD_SUBSCRIPTIONS_HELP_TEXT));
    }

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
      Composite chooseSets = ServletUtil.makeChooseSets(this, remoteApi,
							titleSets.iterator(),
							verb,
							KEY_TITLE_SET,
							doGray,
							isAnySelectable,
							"Select AUs",
							ACTION_SELECT_AUS,
							buttonNumber, 10);
      submitButtonNumber = buttonNumber.intValue();

      if (isAnySelectable.booleanValue()) {
	// Display set chooser
	ServletUtil.layoutExplanationBlock(page,
					   "Select one or more collections of AUs to " + verb.word + ", then click \"Select AUs\".");
	ServletUtil.layoutChooseSets(srvURL(myServletDescr()), page,
				     chooseSets, ACTION_TAG, KEY_VERB, verb);
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
    BatchAuStatus bas = verb.findAusInSetsForVerb(remoteApi, sets);

    // Do chooseSets() if none
    if (bas.getStatusList().isEmpty()) {
      errMsg = "The selected sets contain no AUs.";
      chooseSets(verb);
      return;
    }

    // Do dontChooseAus() if none OK
    if (!bas.hasOk()) {
      dontChooseAus(bas);
      return;
    }

    // Continue in next method
    chooseAus(bas, verb);
  }

  private void chooseAus(BatchAuStatus bas, Verb verb)
      throws IOException {
    // Set up
    HttpSession session = getSession();
    session.setAttribute(SESSION_KEY_BACKUP_INFO, bas.getBackupInfo());
    Map auConfs = new HashMap();
    session.setAttribute(SESSION_KEY_AUID_MAP, auConfs);
    List repos = remoteApi.getRepositoryList();
    boolean repoFlg = verb.isAdd && repos.size() > 1;
    String buttonText = verb.cap + " Selected AUs";

    // Begin page
    Page page = newPage();
    addJavaScript(page);
    layoutErrorBlock(page);

    // Explanation block
    String expl;
    if (repoFlg) {
      String s = "Select the disk(s) on which you want to place AUs, then select the AUs you wish to "
        + verb.word + " (or use \"Select All\"). Then click \""
        + buttonText + "\".";
      Composite c = new Font(1, true); // how to avoid HTML here?
      c.add(s);
      expl = c.toString();
    }
    else {
      expl = "Select the AUs you wish to " + verb.word
        + ". Then click \"" + buttonText + "\".";
    }
    ServletUtil.layoutExplanationBlock(page, expl);

    // Start form
    Form frm = ServletUtil.newForm(srvURL(myServletDescr()));
    frm.add(new Input(Input.Hidden, ACTION_TAG));
    frm.add(new Input(Input.Hidden, KEY_VERB, verb.valStr));

    if (verb.isAdd) {
      // display df for Add even if only one repo
      Map<String,PlatformUtil.DF> repoMap = remoteApi.getRepositoryMap();
      frm.add(ServletUtil.makeRepoTable(this, remoteApi,
					repoMap, KEY_DEFAULT_REPO));
      if (repoFlg) {
	session.setAttribute(SESSION_KEY_REPO_MAP, repoMap);
      }
    }

    MutableInt buttonNumber = new MutableInt(submitButtonNumber);
    frm.add(ServletUtil.makeChooseAus(this, bas.getStatusList().iterator(),
        verb, repos, auConfs, KEY_AUID, KEY_REPO, FOOT_REPO_CHOICE,
        buttonText, buttonNumber, bas.hasAtLeast(LONG_TABLE_AT_LEAST)));
    submitButtonNumber = buttonNumber.intValue();

    if (bas.hasNotOk()) {
      frm.add(ServletUtil.makeNonOperableAuTable(
          "These AUs cannot be " + verb.past, bas.getStatusList().iterator()));
    }
    page.add(frm);
    endPage(page);
  }

  private void dontChooseAus(BatchAuStatus bas) throws IOException {
    Page page = newPage();
    addJavaScript(page);
    layoutErrorBlock(page);
    page.add(ServletUtil.makeNonOperableAuTable(
        "No AUs in set can be " + verb.past, bas.getStatusList().iterator()));
    endPage(page);
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

    // Get the backup information stored in the session.
    RemoteApi.BackupInfo bi =
      (RemoteApi.BackupInfo)session.getAttribute(SESSION_KEY_BACKUP_INFO);

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

    // Perform the operation.
    BatchAuStatus bas = remoteApi.batchAddAus(addOp, auids, repoMap, defRepoId,
	auConfs, auRepoIds, bi);

    displayBatchAuStatus(bas);
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

    BatchAuStatus bas;
    if (isDeactivate) {
      bas = remoteApi.deactivateAus(auids);
    } else {
      bas = remoteApi.deleteAus(auids);
    }
    displayBatchAuStatus(bas);
  }

  /** Display the Backup page */
  private void displayBackup() throws IOException {
    Page page = newPage();
    addJavaScript(page);
    layoutErrorBlock(page);
    MutableInt buttonNumber = new MutableInt(submitButtonNumber);
    ServletUtil.layoutBackup(this, page, remoteApi,
			     ACTION_TAG, KEY_VERB, VERB_BACKUP,
			     buttonNumber, ACTION_DO_BACKUP);
    submitButtonNumber = buttonNumber.intValue();
    endPage(page);
  }

  /** Serve the contents of the local AU config file, as
   * application/binary */
  private void doBackup() throws IOException {
    boolean forceCreate = "true".equalsIgnoreCase(req.getParameter("create"));
    try {
      InputStream in = remoteApi.getAuConfigBackupFileOrStream(getMachineName(),
							       forceCreate);
      try {
	resp.setContentType("application/binary");
	resp.setHeader("Content-disposition",
		       "attachment; filename=\""
		       + remoteApi.getAuConfigBackupFileName(getMachineName())
		       + "\"");
	OutputStream out = resp.getOutputStream();
	StreamUtil.copy(in, out);
	out.close();
      } finally {
	IOUtil.safeClose(in);
      }
    } catch (FileNotFoundException e) {
      errMsg = "No AUs have been configured - nothing to backup";
      displayMenu();
    } catch (IOException e) {
      log.warning("doBackup()", e);
      throw e;
    }
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
      try {
	BatchAuStatus bas = remoteApi.processSavedConfig(ins);
	if (bas.getStatusList().isEmpty()) {
	  errMsg = "Backup file is empty";
	  displayRestore();
	  return;
	}
	chooseAus(bas, VERB_RESTORE);
      } catch (RemoteApi.InvalidAuConfigBackupFile e) {
	errMsg = "Couldn't restore configuration: " + e.getMessage();
	displayRestore();
      }
    }
  }

  private void displayBatchAuStatus(BatchAuStatus status)
      throws IOException {
    List statusList = status.getStatusList();
    int okCnt = status.getOkCnt();
    int errCnt = statusList.size() - okCnt;

    Page page = newPage();
    layoutErrorBlock(page);
    StringBuilder sb = new StringBuilder();
    sb.append(okCnt);
    sb.append(okCnt == 1 ? " AU " : " AUs ");
    sb.append(verb.past);
    if (errCnt != 0) {
      sb.append(" ");
      sb.append(errCnt);
      sb.append(" skipped");
    }
    ServletUtil.layoutExplanationBlock(page, sb.toString());
    ServletUtil.layoutAuStatus(this, page, statusList);
    endPage(page);
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

    RemoteApi.BatchAuStatus findAusInSetForVerb(RemoteApi remoteApi,
						 TitleSet ts) {
      return findAusInSetsForVerb(remoteApi, ListUtil.list(ts));
    }
    RemoteApi.BatchAuStatus findAusInSetsForVerb(RemoteApi remoteApi,
						 Collection sets) {
      switch (val) {
      case VV_ADD:
	return remoteApi.findAusInSetsToAdd(sets);
      case VV_DEL:
      case VV_DEACT:
	return remoteApi.findAusInSetsToDelete(sets);
      case VV_REACT:
	return remoteApi.findAusInSetsToActivate(sets);
      }
      return null;
    }

    int countAusInSetForVerb(RemoteApi remoteApi, TitleSet ts) {
      switch (val) {
      case VV_ADD:
	return ts.countTitles(TitleSet.SET_ADDABLE);
      case VV_DEL:
      case VV_DEACT:
	return ts.countTitles(TitleSet.SET_DELABLE);
      case VV_REACT:
	return ts.countTitles(TitleSet.SET_REACTABLE);
      }
      return 0;
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
