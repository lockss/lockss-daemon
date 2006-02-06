/*
 * $Id: BatchAuConfig.java,v 1.24 2006-02-06 23:54:40 thib_gc Exp $
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

import java.io.*;
import java.util.*;
import java.util.List;

import javax.servlet.*;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.LinkedMap;
import org.lockss.config.*;
import org.lockss.daemon.TitleSet;
import org.lockss.plugin.PluginManager;
import org.lockss.remote.RemoteApi;
import org.lockss.remote.RemoteApi.BatchAuStatus;
import org.lockss.servlet.ServletUtil.LinkWithExplanation;
import org.lockss.util.*;
import org.mortbay.html.*;

/** Create and update AU configuration.
 */
public class BatchAuConfig extends LockssServlet {

  private static final int LONG_TABLE_AT_LEAST = 10;

  /** Controls the appearance (in select lists) of TitleSets that contain
   * no actionable AUs.  If included, they are greyed. <ul><li><b>All</b>
   * &mdash; they are always included,<li><b>Add</b> &mdash; they are
   * included only in the Add Titles selection, omitted in
   * others,<li><b>None</b> &mdash; they are not included.</ul> */
  static final String PARAM_GREY_TITLESET_ACTION =
    Configuration.PREFIX + "batchAuconfig.greyNonActionableTitleSets";
  static final String DEFAULT_GREY_TITLESET_ACTION = "Add";

  static final String FOOT_REPO_CHOICE =
    "If only one choice is shown for an AU, the contents of that AU " +
    "already exist in the selected disk.";

  static Logger log = Logger.getLogger("BatchAuConfig");

  static final int VV_ADD = 1;
  static final int VV_DEL = 2;
  static final int VV_DEACT = 3;
  static final int VV_REACT = 4;
  static final int VV_RESTORE = 5;

  static final Verb VERB_ADD = new Verb(VV_ADD, "add", "added", true);
  static final Verb VERB_DEL = new Verb(VV_DEL, "remove", "removed", false);
  static final Verb VERB_DEACT =
    new Verb(VV_DEACT, "deactivate", "deactivated", false);
  static final Verb VERB_REACT =
    new Verb(VV_REACT, "reactivate", "reactivated", true);
  static final Verb VERB_RESTORE =
    new Verb(VV_RESTORE, "restore", "restored", true);
  static final Verb[] verbs = {VERB_ADD, VERB_DEL,
			       VERB_DEACT, VERB_REACT,
			       VERB_RESTORE};

  static final String ACTION_SELECT_SETS_TO_ADD = "AddTitles";
  static final String ACTION_SELECT_SETS_TO_DEL = "RemoveTitles";
  static final String ACTION_SELECT_SETS_TO_DEACT = "DeactivateTitles";
  static final String ACTION_SELECT_SETS_TO_REACT = "ReactivateTitles";
  static final String ACTION_BACKUP = "Backup";
  static final String ACTION_RESTORE = "Restore";
  static final String ACTION_SELECT_RESTORE_TITLES = "SelectRestoreTitles";
  static final String ACTION_SELECT_AUS = "SelectSetTitles";
  static final String ACTION_ADD_AUS = "DoAddAus";
  static final String ACTION_REMOVE_AUS = "DoRemoveAus";
  static final String ACTION_DEACT_AUS = "DoDeactivateAus";
  static final String ACTION_REACT_AUS = "DoReactivateAus";
  static final String ACTION_RESTORE_AUS = "DoAddAus";

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
  private ConfigManager configMgr;
  private RemoteApi remoteApi;

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
    configMgr = getLockssDaemon().getConfigManager();
    remoteApi = getLockssDaemon().getRemoteApi();
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
	getMultiPartRequest(100000);
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
    else if (action.equals(ACTION_BACKUP)) doSaveAll();
    else if (action.equals(ACTION_RESTORE)) displayRestore();
    else if (action.equals(ACTION_SELECT_AUS)) chooseAus();
    else if (action.equals(ACTION_SELECT_RESTORE_TITLES)) selectRestoreTitles();
    else if (action.equals(ACTION_ADD_AUS)) doAddAus(false);
    else if (action.equals(ACTION_REACT_AUS)) doAddAus(true);
    else if (action.equals(ACTION_RESTORE_AUS)) doAddAus(false);
    else if (action.equals(ACTION_REMOVE_AUS)) doRemoveAus(false);
    else if (action.equals(ACTION_DEACT_AUS)) doRemoveAus(true);
    else {
      errMsg = "Unknown action: " + action;
      displayMenu();
    }
  }

  private Iterator getMenuDescriptors() {
    String ACTION = ACTION_TAG + "=";
    int numActive = remoteApi.getAllAus().size();
    int numInactive = remoteApi.getInactiveAus().size();
    ServletDescr myDescr = myServletDescr();
    ArrayList list = new ArrayList(7); // at most 7 entries

    // Add and remove
    list.add(getMenuDescriptor(myDescr,
                               "Add Titles",
                               ACTION + ACTION_SELECT_SETS_TO_ADD,
                               "Add one or more groups of titles"));
    if (numActive > 0 || isDebugUser() ) {
      list.add(getMenuDescriptor(myDescr,
                                 "Remove Titles",
                                 ACTION + ACTION_SELECT_SETS_TO_DEL,
                                 "Remove selected titles",
                                 numActive > 0));
    }

    if (isDebugUser() ) {
      // Deactivate and reactivate
      list.add(getMenuDescriptor(myDescr,
                                 "Deactivate Titles",
                                 ACTION + ACTION_SELECT_SETS_TO_DEACT,
                                 "Deactivate selected titles",
                                 numActive > 0));
      list.add(getMenuDescriptor(myDescr,
                                 "Reactivate Titles",
                                 ACTION + ACTION_SELECT_SETS_TO_REACT,
                                 "Reactivate selected titles",
                                 numInactive > 0));
    }

    // Backup and restore
    list.add(getMenuDescriptor(myDescr,
                               "Backup",
                               ACTION + ACTION_BACKUP,
                               "Backup cache config to a file on your workstation",
                               numActive > 0 || numInactive > 0));
    list.add(getMenuDescriptor(myDescr,
                               "Restore",
                               ACTION + ACTION_RESTORE,
                               "Restore cache config from a file on your workstation"));

    // Manual edit
    list.add(getMenuDescriptor(SERVLET_AU_CONFIG,
                               "Manual Add/Edit",
                               null,
                               "Add, Edit or Delete an individual AU"));

    return list.iterator();
  }

  /**
   * <p>Makes a new link with explanation.</p>
   * <p>The resulting link is always enabled.</p>
   * @param descr    The link's servlet descriptor.
   * @param linkText The text appearing in the link.
   * @param action   The action associated with the servlet descriptor
   *                 (can be null).
   * @param expl     The explanation associated with the link.
   * @return A {@link LinkWithExplanation} corresponding to the servlet
   *         descriptor (optionally with an action), showing the given
   *         text and explanation.
   */
  private LinkWithExplanation getMenuDescriptor(ServletDescr descr,
                                                String linkText,
                                                String action,
                                                String expl) {
    return getMenuDescriptor(descr, linkText, action, expl, true);
  }

  /**
   * <p>Makes a new link with explanation.</p>
   * @param descr      The link's servlet descriptor.
   * @param linkText   The text appearing in the link.
   * @param linkAction The action associated with the servlet descriptor
   *                   (can be null).
   * @param linkExpl   The explanation associated with the link.
   * @param enabled    Whether or not the link is actually enabled.
   * @return A {@link LinkWithExplanation} corresponding to the servlet
   *         descriptor (optionally with an action), showing the given
   *         text and explanation; the link is enabled or disabled
   *         according to the parameter.
   */
  private LinkWithExplanation getMenuDescriptor(ServletDescr descr,
                                                String linkText,
                                                String linkAction,
                                                String linkExpl,
                                                boolean enabled) {
    return new LinkWithExplanation(
      enabled ? srvLink(descr, linkText, linkAction) : ServletUtil.gray(linkText),
      linkExpl);
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
    String grayAction = ConfigManager.getParam(PARAM_GREY_TITLESET_ACTION,
                                               DEFAULT_GREY_TITLESET_ACTION);
    boolean doGray = "All".equalsIgnoreCase(grayAction) ||
      (verb == VERB_ADD && "Add".equalsIgnoreCase(grayAction));
    MutableBoolean isAnySelectable = new MutableBoolean(false);
    MutableInteger buttonNumber = new MutableInteger(submitButtonNumber);
    Composite chooseSets = ServletUtil.makeChooseSets(this, remoteApi,
        pluginMgr.getTitleSets().iterator(), verb, KEY_TITLE_SET,
        doGray, isAnySelectable, "Select Titles", ACTION_SELECT_AUS,
        buttonNumber, 10);
    submitButtonNumber = buttonNumber.intValue();

    if (isAnySelectable.booleanValue()) {
      // Display set chooser
      ServletUtil.layoutExplanationBlock(page,
          "Select one or more collections of titles to " + verb.word + ", then click \"Select Titles\".");
      ServletUtil.layoutChooseSets(srvURL(myServletDescr()), page,
          chooseSets, ACTION_TAG, KEY_VERB, verb);
    }
    else {
      // Set chooser not needed
      ServletUtil.layoutExplanationBlock(page,
          "All titles in all predefined collections of titles already exist on this cache.");
    }

    // End page
    if (action != null) {
      ServletUtil.layoutBackLink(this, page, "Journal Configuration");
    }
    layoutFooter(page);
    page.write(resp.getWriter());
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
    HttpSession session = req.getSession(true);
    setSessionTimeout(session);
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
      String s = "There are multiple disks on this cache. First, select the disk on which you want to place most AUs, then select the AUs you wish to "
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

    // Only if repoFlg
    if (repoFlg) {
      OrderedMap repoChoices = new LinkedMap();
      for (Iterator iter = repos.iterator(); iter.hasNext(); ) {
        String repo = (String)iter.next();
        PlatformInfo.DF df = remoteApi.getRepositoryDF(repo);
        repoChoices.put(repo, df);
      }
      frm.add(ServletUtil.makeRepoTable(
          this, repoChoices.entrySet().iterator(), KEY_DEFAULT_REPO));
      session.setAttribute(SESSION_KEY_REPO_MAP, repoChoices);
    }

    MutableInteger buttonNumber = new MutableInteger(submitButtonNumber);
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

  private void doAddAus(boolean isReactivate) throws IOException {
    // Check cookies
    HttpSession session = req.getSession(false);
    if (session == null) {
      errMsg = "Please enable cookies";
      displayMenu();
      return;
    }

    RemoteApi.BackupInfo bi =
      (RemoteApi.BackupInfo)session.getAttribute(SESSION_KEY_BACKUP_INFO);
    LinkedMap repoMap = (LinkedMap)session.getAttribute(SESSION_KEY_REPO_MAP);
    String[] auids = req.getParameterValues(KEY_AUID);
    String defaultRepo = null;
    String defRepoId = getParameter(KEY_DEFAULT_REPO);
    if (!StringUtil.isNullString(defRepoId)) {
      try {
	int n = Integer.parseInt(defRepoId);
	defaultRepo = (String)repoMap.get(n - 1);
      } catch (NumberFormatException e) {
	log.warning("Illegal default repoId: " + defRepoId, e);
      } catch (IndexOutOfBoundsException e) {
	log.warning("Illegal default repoId: " + defRepoId, e);
      }
    }
    if (auids == null || auids.length == 0) {
      errMsg = "No AUs were selected";
      displayMenu();
      return;
    }
    Configuration createConfig = ConfigManager.newConfiguration();
    Map auConfs = (Map)session.getAttribute(SESSION_KEY_AUID_MAP);
    for (int ix = 0; ix < auids.length; ix++) {
      String auid = auids[ix];
      Configuration tcConfig = (Configuration)auConfs.get(auid);
      tcConfig.remove(PluginManager.AU_PARAM_REPOSITORY);
      String repoId = getParameter(KEY_REPO + "_" + auid);
      if (!StringUtil.isNullString(repoId)) {
	try {
	  int repoIx = Integer.parseInt(repoId);
	  if (!StringUtil.isNullString(repoId)) {
	    tcConfig.put(PluginManager.AU_PARAM_REPOSITORY,
			 (String)repoMap.get(repoIx - 1));
	  }
	} catch (NumberFormatException e) {
	  log.warning("Illegal repoId: " + repoId, e);
	} catch (IndexOutOfBoundsException e) {
	  log.warning("Illegal repoId: " + repoId, e);
	}
      }
      if (defaultRepo != null &&
	  !tcConfig.containsKey(PluginManager.AU_PARAM_REPOSITORY)) {
	tcConfig.put(PluginManager.AU_PARAM_REPOSITORY, defaultRepo);
      }
      String prefix = PluginManager.PARAM_AU_TREE + "." +
	PluginManager.configKeyFromAuId(auid);
      createConfig.addAsSubTree(tcConfig, prefix);
    }
    if (log.isDebug2()) log.debug2("createConfig: " + createConfig);

    BatchAuStatus bas =
      remoteApi.batchAddAus(isReactivate, createConfig, bi);
    displayBatchAuStatus(bas);
  }

  private void doRemoveAus(boolean isDeactivate) throws IOException {
    // Check cookies
    HttpSession session = req.getSession(false);
    if (session == null) {
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

  /** Serve the contents of the local AU config file, as
   * application/binary */
  private void doSaveAll0() throws IOException {
    try {
      InputStream is = remoteApi.getAuConfigBackupStream(getMachineName());
      Reader rdr = new InputStreamReader(is, Constants.DEFAULT_ENCODING);
      PrintWriter wrtr = resp.getWriter();
      resp.setContentType("application/binary");
      StreamUtil.copy(rdr, wrtr);
      rdr.close();
    } catch (FileNotFoundException e) {
      errMsg = "No AUs have been configured - nothing to backup";
      displayMenu();
    } catch (IOException e) {
      log.warning("doSaveAll()", e);
      throw e;
    }
  }

  /** Serve the contents of the local AU config file, as
   * application/binary */
  private void doSaveAll() throws IOException {
    try {
      InputStream in = remoteApi.getAuConfigBackupStream(getMachineName());
      try {
	resp.setContentType("application/binary");
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
      log.warning("doSaveAll()", e);
      throw e;
    }
  }

  /** Display the Restore page */
  private void displayRestore() throws IOException {
    Page page = newPage();
    addJavaScript(page);
    layoutErrorBlock(page);
    MutableInteger buttonNumber = new MutableInteger(submitButtonNumber);
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
    ServletUtil.layoutExplanationBlock(page, okCnt + " AUs " + verb.past +
        ", " + errCnt + " skipped");
    ServletUtil.layoutAuStatus(page, statusList.iterator());
    endPage(page);
  }

  /** Common and page adds Back link, footer */
  protected void endPage(Page page) throws IOException {
    if (action != null) {
      ServletUtil.layoutBackLink(this, page, "Journal Configuration");
    }
    layoutFooter(page);
    page.write(resp.getWriter());
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
