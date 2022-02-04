/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

import static org.lockss.laaws.V2AuMover.compileRegexps;
import static org.lockss.laaws.V2AuMover.isMatch;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.laaws.V2AuMover;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.mortbay.html.Block;
import org.mortbay.html.Composite;
import org.mortbay.html.Element;
import org.mortbay.html.Form;
import org.mortbay.html.Input;
import org.mortbay.html.Page;
import org.mortbay.html.Script;
import org.mortbay.html.Select;
import org.mortbay.html.Table;

/**
 */
public class MigrateContent extends LockssServlet {

  static Logger log = Logger.getLogger("MigrateContent");

  static final String PREFIX = Configuration.PREFIX + "v2.migrate.";
  public static final String PARAM_ENABLE_MIGRATION = PREFIX + "enabled";
  public static final boolean DEFAULT_ENABLE_MIGRATION = false;

  public static final String PARAM_HOSTNAME=PREFIX +"hostname";
  static final String DEFAULT_HOSTNAME="localhost";
  public static final String PARAM_AU_SELECT_FILTER=PREFIX +"au_select_filter";
  public static final List<String> DEFAULT_AU_SELECT_FILTER=ListUtil.fromArray(new String[] {".*"});

  // paramdoc only
  static final String KEY_ACTION = "action";
  static final String KEY_MSG = "msg";
  static final String KEY_AUID = "auid";
  static final String HOSTNAME="hostname";
  static final String KEY_USER_NAME="username";
  static final String KEY_PASSWD="password";

  public static final String ACTION_MIGRATE_AU= "Migrate One AU to V2 Repository";
  public static final String ACTION_MIGRATE_ALL= "Migrate All AUs to V2 Repository";

  private static final String HOST_URL_FOOT =
    "The V2 REST Service host name (localhost by default).";
  private static final String USER_NAME_FOOT =
    "The username used to connect to the rest interface of the V2 services.";
  private static final String PASSWD_FOOT =
    "The password used to connect to the rest interface of the V2 services.";

  private PluginManager pluginMgr;
  private V2AuMover auMover;

  String auid;
  String userName;
  String userPass;
  String hostName=DEFAULT_HOSTNAME;
  List<String> auSelectFilter;
  List<Pattern> auSelectPatterns;


  protected void resetLocals() {
    auid = null;
    errMsg = null;
    statusMsg = null;
    userName=null;
    userPass =null;
    super.resetLocals();
  }

  void initParams() {
    Configuration config = ConfigManager.getCurrentConfig();
    hostName=config.get(PARAM_HOSTNAME, DEFAULT_HOSTNAME);
    auSelectFilter=config.getList(PARAM_AU_SELECT_FILTER, DEFAULT_AU_SELECT_FILTER);
    if(auSelectFilter != DEFAULT_AU_SELECT_FILTER) {
      auSelectPatterns = compileRegexps(auSelectFilter);
    }
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginMgr = getLockssDaemon().getPluginManager();
  }

  public void lockssHandleRequest() throws IOException {
    initParams();
    String action = getParameter(KEY_ACTION);

    if (!StringUtil.isNullString(action)) {
      auid = getParameter(KEY_AUID);
      userName=getParameter(KEY_USER_NAME);
      userPass =getParameter(KEY_PASSWD);
      hostName=getParameter(HOSTNAME);
      if(hostName==null) hostName="localhost";
      if (ACTION_MIGRATE_AU.equals(action)) {
        //Todo: This should be eliminated when we are done with testing.
        doMigrateAu();
      } else if (ACTION_MIGRATE_ALL.equals(action)) {
        doMigrateAll();
      }
    }
    displayPage();
  }

  private void doMigrateAll() {
    try {
      auMover = new V2AuMover();
      auMover.moveAllAus(hostName, userName, userPass, auSelectPatterns);
      java.util.List<String> errs = auMover.getErrors();
      if (!errs.isEmpty()) {
        errMsg = StringUtil.separatedString(errs, "\n");
      } else {
        statusMsg = "All AUs have been migrated.";
      }
    } catch (Exception e) {
      log.error("Unexpected Exception enqueuing AUs.", e);
      errMsg = e.getMessage();
    }
  }

  private void doMigrateAu() {
    ArchivalUnit au = getAu();
    if (au == null) return;
    if (pluginMgr.isInternalAu(au)) {
      errMsg = "Can't migrate internal AUs";
      return;
    }
    try {
      auMover = new V2AuMover();
      auMover.moveOneAu(hostName, userName, userPass, au);
      java.util.List<String> errs = auMover.getErrors();
      log.debug("errs: " + errs);
      if (!errs.isEmpty()) {
        errMsg = StringUtil.separatedString(errs, "\n");
      } else {
        statusMsg = au.getName() + " has been migrated.";
      }
    } catch (Exception e) {
      log.error("Unexpected Exception enqueuing AU " + au.getName(), e);
      errMsg = e.getMessage();
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
    addCssLocations(page);
    addReactJSLocations(page);
    addJSXLocation(page, "js/auMigrationStatus.js");
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, "");
    page.add(new Block(Block.Div, "id='AuMigrationStatusApp'"));
    page.add(makeForm());
    page.add("<br>");
    endPage(page);
  }

  protected void addJSXLocation(Page page, String jsxLocation) {
    Script jsxScript = new Script("");
    jsxScript.attribute("src", jsxLocation);
    jsxScript.attribute("type", "text/babel");
    page.add(jsxScript);
  }

  static String CENTERED_CELL = "align=\"center\" colspan=3";

  private Element makeForm() {
    Composite comp = new Composite();
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
    Table tbl = new Table(0, "align=center cellspacing=2 cellpadding=0");
    addAusToTable(tbl,KEY_AUID, auid);
    tbl.newRow();
    tbl.newCell();

    addInputToTable(tbl,
      "V2 Rest Services Hostname" + addFootnote(HOST_URL_FOOT),
      HOSTNAME, hostName, 40);
    addInputToTable(tbl,
      "V2 Rest Services Username:" + addFootnote(USER_NAME_FOOT),
      KEY_USER_NAME, userName, 20);
    addHiddenInputToTable(tbl,
      "V2 Rest Services Password:" + addFootnote(PASSWD_FOOT),
      KEY_PASSWD,"", 20);

    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    Input migrateAu = new Input(Input.Submit, KEY_ACTION, ACTION_MIGRATE_AU);
    tbl.add(migrateAu);
    Input migrateAll = new Input(Input.Submit, KEY_ACTION, ACTION_MIGRATE_ALL);
    tbl.add(migrateAll);

    frm.add(tbl);
    comp.add(frm);
    return comp;
  }


  private void addInputToTable(Table tbl, String label, String key, String init, int size) {
    Input in = new Input(Input.Text, key, init);
    in.setSize(size);
    addElementToTable(tbl, label, in);
  }

  private void addHiddenInputToTable(Table tbl, String label, String key, String init, int size) {
    Input in = new Input(Input.Password, key, init);
    in.setSize(size);
    addElementToTable(tbl, label, in);
  }

  private void addElementToTable(Table tbl, String label, Element elem) {
    tbl.newRow();
    tbl.newCell("align=right");
    tbl.add(label);
    tbl.add(":");
    tbl.newCell();
    tbl.add("&nbsp;");
    tbl.newCell();
    tbl.add(elem);
  }

  private void addAusToTable( Table tbl, String key, String preselId) {
    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    tbl.add("Select AU<br>");
    Select sel = new Select(key, false);
    sel.add("", preselId == null, "");
    for (ArchivalUnit au0 : pluginMgr.getAllAus()) {
      if (pluginMgr.isInternalAu(au0)) {
        continue;
      }
      String id = au0.getAuId();
      if (auSelectPatterns == null || auSelectPatterns.isEmpty() ||
          isMatch(id, auSelectPatterns)) {
        sel.add(encodeAttr(au0.getName()), id.equals(preselId), id);
      }
    }
    tbl.add(sel);
    setTabOrder(sel);
  }

}
