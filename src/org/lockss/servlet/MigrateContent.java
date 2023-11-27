/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.laaws.V2AuMover;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.laaws.*;
import org.lockss.laaws.MigrationManager.OpType;
import org.mortbay.html.Block;
import org.mortbay.html.Composite;
import org.mortbay.html.Element;
import org.mortbay.html.Form;
import org.mortbay.html.Input;
import org.mortbay.html.Page;
import org.mortbay.html.Script;
import org.mortbay.html.Select;
import org.mortbay.html.Table;
import org.mortbay.html.StyleLink;
/**
 */
public class MigrateContent extends LockssServlet {

  static Logger log = Logger.getLogger("MigrateContent");

  static final String PREFIX = Configuration.PREFIX + "v2.migrate.";
  public static final String PARAM_ENABLE_MIGRATION = PREFIX + "enabled";
  public static final boolean DEFAULT_ENABLE_MIGRATION = true;

  public static final String PARAM_HOSTNAME=PREFIX +"hostname";
  static final String DEFAULT_HOSTNAME="localhost";
  public static final String PARAM_AU_SELECT_FILTER=PREFIX +"au_select_filter";
  public static final List<String> DEFAULT_AU_SELECT_FILTER =
    Collections.emptyList();

  public static final String PARAM_DEFAULT_OPTYPE = PREFIX + "defaultOpType";
  static final OpType DEFAULT_DEFAULT_OPTYPE = OpType.CopyOnly;

  /**
   * If true, the verify step will perform a byte-by-byte comparison
   * between V1 and V2 content
   */
  public static final String PARAM_DEFAULT_COMPARE = PREFIX + "defaultCompare";
  public static final boolean DEFAULT_DEFAULT_COMPARE = false;

  static final String KEY_STATUS = "status";
  static final String KEY_ACTION = "action";
  static final String KEY_OUTPUT = "output";
  static final String KEY_INDEX = "index";
  static final String KEY_SIZE = "size";
  static final String KEY_MSG = "msg";
  static final String KEY_AUID = "auid";
  static final String KEY_HOSTNAME = "hostname";
  static final String KEY_PLUGINID = "pluginid";
  static final String KEY_USER_NAME="username";
  static final String KEY_PASSWD="password";
  static final String KEY_OP_TYPE = "op_type";
  static final String KEY_COMPARE_CONTENT = "compare_content";


  public static final String ACTION_START= "Start";
  public static final String ACTION_ABORT= "Abort";

  private static String ALL_PLUGINS_ID = "_allplugs_";

  private static final String HOST_URL_FOOT =
    "The V2 REST Service host name (localhost by default).";
  private static final String USER_NAME_FOOT =
    "The username used to connect to the rest interface of the V2 services.";
  private static final String PASSWD_FOOT =
    "The password used to connect to the rest interface of the V2 services.";

  private PluginManager pluginMgr;
  private MigrationManager migrationMgr;
  private V2AuMover auMover;

  String auid;
  String pluginId;
  String userName;
  String userPass;
  String hostName=DEFAULT_HOSTNAME;
  OpType defaultOpType = DEFAULT_DEFAULT_OPTYPE;
  boolean defaultCompare = DEFAULT_DEFAULT_COMPARE;
  OpType opType;
  boolean isCompareContent;
  List<String> auSelectFilter;
  List<Pattern> auSelectPatterns;


  protected void resetLocals() {
    auid = null;
    errMsg = null;
    statusMsg = null;
    userName = null;
    userPass = null;
    opType = null;
    super.resetLocals();
  }

  void initParams() {
    Configuration config = ConfigManager.getCurrentConfig();
    hostName = config.get(PARAM_HOSTNAME, DEFAULT_HOSTNAME);
    auSelectFilter =
      config.getList(PARAM_AU_SELECT_FILTER, DEFAULT_AU_SELECT_FILTER);
    if(auSelectFilter != DEFAULT_AU_SELECT_FILTER) {
      auSelectPatterns = compileRegexps(auSelectFilter);
    }
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    pluginMgr = getLockssDaemon().getPluginManager();
    migrationMgr = getLockssDaemon().getMigrationManager();
  }

  public void lockssHandleRequest() throws IOException {
    initParams();

    // Is this a status request?
    String output = getParameter(KEY_OUTPUT);
    String status = getParameter(KEY_STATUS);
    if (!StringUtil.isNullString(status)) {
      switch (status) {
      case "status":
        sendCurrentStatus(output);
        break;
      case "finished":
        sendFinishedChunk(output,
                          getParameter(KEY_INDEX), getParameter(KEY_SIZE));
        break;
      }
      return;
    }

    Configuration config = ConfigManager.getCurrentConfig();
    defaultOpType = config.getEnum(OpType.class,
                                   PARAM_DEFAULT_OPTYPE,
                                   DEFAULT_DEFAULT_OPTYPE);
    // default, if no value from form
    isCompareContent = config.getBoolean(PARAM_DEFAULT_COMPARE,
                                         DEFAULT_DEFAULT_COMPARE);

    String action = getParameter(KEY_ACTION);
    if (!StringUtil.isNullString(action)) {
      userName=getParameter(KEY_USER_NAME);
      userPass =getParameter(KEY_PASSWD);
      hostName=getParameter(KEY_HOSTNAME);
      if(hostName==null) hostName="localhost";
      isCompareContent = getParameter(KEY_COMPARE_CONTENT) != null;

      auid = getParameter(KEY_AUID);
      pluginId = getParameter(KEY_PLUGINID);

      String opTypeStr = getParameter(KEY_OP_TYPE);
      if (!StringUtil.isNullString(opTypeStr)) {
        try {
          opType = OpType.valueOf(opTypeStr);
        } catch (IllegalArgumentException e) {
          errMsg = "Unknown op type: " + opTypeStr;
        }
      }

      if (ACTION_START.equals(action)) {
        if (!StringUtil.isNullString(auid)) {
          doMigrateAu();
        } else if (!StringUtil.isNullString(pluginId)) {
          doMigratePluginAus();
        } else {
          errMsg = "Please select a plugin";
        }
      } else if (ACTION_ABORT.equals(action)) {
        doAbort();
      }
    }
    displayPage();
  }

  private void sendCurrentStatus(String format) throws IOException {
    // TODO: support format other than json?
    Map statMap = getCurrentStatus();
    switch (format) {
    case "json":
    default:
      Gson gson = new GsonBuilder().create();
      gson.toJson(statMap);
      resp.setStatus(200);
      PrintWriter wrtr = resp.getWriter();
      resp.setContentType("application/json");
      String json = gson.toJson(statMap);
      log.debug3("json: " + json);
      wrtr.println(json);
    }
  }
    
  private void sendFinishedChunk(String format, String indexStr, String sizeStr)
      throws IOException {
    // TODO: support format other than json?
    try {
	int index = Integer.parseInt(indexStr);
	int size = Integer.parseInt(sizeStr);
        Map statMap = migrationMgr.getFinishedPage(index, size);
        switch (format) {
        case "json":
        default:
          Gson gson = new GsonBuilder().create();
          resp.setStatus(200);
          PrintWriter wrtr = resp.getWriter();
          resp.setContentType("application/json");
          String json = gson.toJson(statMap);
          log.debug3("json: " + json);
          wrtr.println(json);
        }
    } catch (NumberFormatException e) {
      String msg =
        "Index (" + indexStr + ") or size (" + sizeStr + ") not an int";
      log.error(msg);
      Gson gson = new GsonBuilder().create();
      Map errMap = new HashMap();
      errMap.put("error", msg);
      resp.setStatus(400);
      PrintWriter wrtr = resp.getWriter();
      resp.setContentType("application/json");
      String json = gson.toJson(errMap);
      wrtr.println(json);
    }
  }
    
  Map getCurrentStatus() {
    return migrationMgr.getStatus();
  }

  V2AuMover.Args getCommonFormArgs() {
    return new V2AuMover.Args()
      .setHost(hostName)
      .setUname(userName)
      .setUpass(userPass)
      .setCompareContent(isCompareContent)
      .setOpType(opType);
  }

  private void startRunner(V2AuMover.Args args) {
    try {
      migrationMgr.startRunner(args);
    } catch (Exception e) {
      log.error("Couldn't start migration", e);
      errMsg = "Can't start migration: " + e.getMessage();
    }
  }

  private void doMigrateAll() {
    V2AuMover.Args args = getCommonFormArgs()
      .setSelPatterns(auSelectPatterns);
    startRunner(args);
  }

  private void doMigratePluginAus() {
    V2AuMover.Args args = getCommonFormArgs();
    if (ALL_PLUGINS_ID.equals(pluginId)) {
      args.setPlugins(null);
    } else {
      Plugin plug = pluginMgr.getPluginFromId(pluginId);
      if (plug == null) {
        errMsg = "No plugin with ID: " + pluginId;
        return;
      }
      args.setPlugins(Collections.singletonList(plug));
    }
    startRunner(args);
  }

  private void doMigrateAu() {
    ArchivalUnit au = getAu();
    if (au == null) {
      if (errMsg == null) {
        errMsg = "No AU selected";
      }
      return;
    }
    if (pluginMgr.isInternalAu(au)) {
      errMsg = "Can't migrate internal AUs";
      return;
    }
    V2AuMover.Args args = getCommonFormArgs()
      .setAu(au);
    startRunner(args);
  }

  private void doAbort() {
    try {
      migrationMgr.abortCopy();
      statusMsg = "Abort requested";
    } catch (Exception e) {
      log.error("Couldn't abort", e);
      errMsg = "Couldn't abort: " + e.getMessage();
    }
  }

  ArchivalUnit getAu() {
    if (StringUtil.isNullString(auid)) {
      errMsg = "No AU selected";
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
    page.add(new StyleLink("/css/migrate.css"));
    addReactJSLocations(page);
    addJSXLocation(page, "js/auMigrationStatus.js");
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, "");
    page.add(makeForm());
//     page.add("<br>");
    page.add(new Block(Block.Div, "id='AuMigrationStatusApp'"));
    endPage(page);
  }

  static String CENTERED_CELL = "align=\"center\" colspan=3";

  private void addSelToTable(Table tbl) {
    if (auSelectFilter.isEmpty()) {
      addPluginSelToTable(tbl, KEY_PLUGINID, pluginId);
    } else {
      addAuSelToTable(tbl, KEY_AUID, auid);
    }
  }


  private Element makeForm() {
    Composite comp = new Composite();
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
    Table tbl = new Table(0, "align=center cellspacing=2 cellpadding=0");
    addSelToTable(tbl);
    tbl.newRow();
    tbl.newCell();

    addInputToTable(tbl,
      "V2 Rest Services Hostname" + addFootnote(HOST_URL_FOOT),
      KEY_HOSTNAME, hostName, 40);
    addInputToTable(tbl,
      "V2 Rest Services Username" + addFootnote(USER_NAME_FOOT),
      KEY_USER_NAME, userName, 20);
    addHiddenInputToTable(tbl,
      "V2 Rest Services Password" + addFootnote(PASSWD_FOOT),
      KEY_PASSWD,"", 20);

    OpType selOpType = opType != null ? opType : defaultOpType;

    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    tbl.add(opRadioBtn(OpType.CopyOnly, selOpType));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(opRadioBtn(OpType.CopyAndVerify, selOpType));
    tbl.add("&nbsp;&nbsp;");
    tbl.add(opRadioBtn(OpType.VerifyOnly, selOpType));

    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    tbl.add(checkBox("Full content compare", "true", KEY_COMPARE_CONTENT,
                     isCompareContent));


    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    Input start = new Input(Input.Submit, KEY_ACTION, ACTION_START);
    Input abort = new Input(Input.Submit, KEY_ACTION, ACTION_ABORT);
    tbl.add(start);
    tbl.add(abort);

    frm.add(tbl);
    comp.add(frm);
    return comp;
  }

  Element opRadioBtn(OpType op, OpType selected) {
    return radioButton(op.toString(), op.name(), KEY_OP_TYPE,
                       selected == op);
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

  private void addAuSelToTable(Table tbl, String key, String preselId) {
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

  private void addPluginSelToTable(Table tbl, String key, String preselId) {
    tbl.newRow();
    tbl.newCell(CENTERED_CELL);
    tbl.add("Select Plugin<br>");
    final Select sel = new Select(key, false);
    sel.add("", preselId == null, "");
    // Build plugin -> #AUs map
    Map<Plugin,Integer> plugs = pluginMgr.getRegisteredPlugins().stream()
      // Filter out registry AUs
      .filter(plug -> !(pluginMgr.isInternalPlugin(plug)))
      .collect(Collectors.toMap(plug -> plug,
                                plug -> plug.getAllAus().size()));
    // Sum total AUs
    int totalAus = plugs.entrySet().stream()
      .map(Map.Entry::getValue)
      .reduce(0, Integer::sum);;
    // Add "All plugins" menu item
    sel.add(String.format("%s (%d)", "All plugins", totalAus),
            ALL_PLUGINS_ID.equals(preselId), ALL_PLUGINS_ID);
    // Add menu item for each plugin
    plugs.entrySet().stream()
      .filter(ent -> ent.getValue() > 0)
      .sorted((ent1, ent2) -> ent1.getKey().getPluginName().compareToIgnoreCase(ent2.getKey().getPluginName()))
      .forEach(ent -> sel.add(String.format("%s (%d)",
                                            encodeAttr(ent.getKey().getPluginName()),
                                          ent.getValue()),
                              ent.getKey().getPluginId().equals(preselId),
                              ent.getKey().getPluginId()));
    tbl.add(sel);
    setTabOrder(sel);
  }

}
