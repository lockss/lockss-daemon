/*
 * $Id: RaiseAlert.java,v 1.13 2006-04-05 22:55:25 tlipkis Exp $
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

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import java.security.*;
import org.mortbay.html.*;
import org.mortbay.util.B64Code;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.alert.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

/** Raise an alert on demand.  For testing alerts
 */
public class RaiseAlert extends LockssServlet {
  static final String KEY_ACTION = "action";
  static final String KEY_NAME_SEL = "name_sel";
  static final String KEY_NAME_TYPE = "name_type";
  static final String KEY_AUID = "auid";
  static final String KEY_TEXT = "text";

  static final String ACTION_RAISE = "Raise";

  static final String COL2 = "colspan=2";
  static final String COL2CENTER = COL2 + " align=center";

  static Logger log = Logger.getLogger("RaiseAlert");

  private LockssDaemon daemon;
  private PluginManager pluginMgr;
  private AlertManager alertMgr;

  String auid;
  String name;
  String text;
  ArchivalUnit au;
  boolean showResult;
  protected void resetLocals() {
    resetVars();
    super.resetLocals();
  }

  void resetVars() {
    auid = null;
    errMsg = null;
    statusMsg = null;
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    daemon = getLockssDaemon();
    alertMgr = daemon.getAlertManager();
    pluginMgr = daemon.getPluginManager();
  }

  public void lockssHandleRequest() throws IOException {
    resetVars();
    String action = getParameter(KEY_ACTION);

    if (ACTION_RAISE.equals(action)) {
      doRaise();
    }
    displayPage();
  }

  private boolean doRaise() {
    auid = getParameter(KEY_AUID);
    au = pluginMgr.getAuFromId(auid);
    name = getParameter(KEY_NAME_SEL);
    if (StringUtil.isNullString(name)) {
      name = getParameter(KEY_NAME_TYPE);
    }
    text = getParameter(KEY_TEXT);

    Alert alert;
    Alert proto = findProtoAlert(name);
    if (proto == null) {
      proto = new Alert(name);
    }
    if (au != null && (proto == null ||
		       proto.getBool(Alert.ATTR_IS_CONTENT))) {
      alert = Alert.auAlert(proto, au);
    } else {
      alert = Alert.cacheAlert(proto);
    }
    alert.setAttribute(Alert.ATTR_TEXT, text);
    alertMgr.raiseAlert(alert);
    return true;
  }

  private void displayPage() throws IOException {
    Page page = newPage();
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, "Raise an Alert");
    page.add(makeForm());
    page.add("<br>");
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  void addResultRow(Table tbl, String head, Object value) {
    tbl.newRow();
    tbl.newCell();
    tbl.add(head);
    tbl.add(":");
    tbl.newCell();
    tbl.add(value.toString());
  }

  private Element makeForm() {
    Composite comp = new Composite();
    Block centeredBlock = new Block(Block.Center);

    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");

    Table autbl = new Table(0, "cellpadding=0");
    autbl.newRow();
    autbl.addHeading("Select AU");
    Select sel = new Select(KEY_AUID, false);
    sel.add("", auid == null, "");
    for (Iterator iter = pluginMgr.getAllAus().iterator(); iter.hasNext(); ) {
      ArchivalUnit au0 = (ArchivalUnit)iter.next();
      String id = au0.getAuId();
      sel.add(au0.getName(), id.equals(auid), id);
    }
    autbl.newRow(); autbl.newCell();
    setTabOrder(sel);
    autbl.add(sel);

    Table alrtbl = new Table(0, "cellpadding=0");
    alrtbl.newRow();
    alrtbl.addHeading("Select Alert Name");
    Select sel2 = new Select(KEY_NAME_SEL, false);
    sel2.add("", auid == null, "");
    for (int ix = 0; ix < protoAlerts.length; ix++) {
      Alert proto = protoAlerts[ix];
      String aname = proto.getName();
      sel2.add(aname, aname.equals(name), aname);
    }
    alrtbl.newRow(); alrtbl.newCell();
    setTabOrder(sel2);
    alrtbl.add(sel2);

    Table tbl = new Table(0, "cellpadding=0");
    tbl.newRow();
    tbl.newCell(COL2CENTER);
    tbl.add(autbl);
    tbl.newRow();
    tbl.newCell(COL2CENTER);
    tbl.add(alrtbl);
    tbl.newRow();
    tbl.newCell();
    tbl.add("&nbsp;");

    addInputRow(tbl, "Name", KEY_NAME_TYPE, 50, name);
    addInputRow(tbl, "Text", KEY_TEXT, 50, text);

    centeredBlock.add(tbl);
    frm.add(centeredBlock);
    Input submit = new Input(Input.Submit, KEY_ACTION, ACTION_RAISE);
    setTabOrder(submit);
    frm.add("<br><center>"+submit+"</center>");
    comp.add(frm);
    return comp;
  }

  void addInputRow(Table tbl, String label, String key,
		   int size, String initVal) {
    tbl.newRow();
//     tbl.newCell();
    tbl.addHeading(label + ":", "align=right");
    tbl.newCell();
    Input in = new Input(Input.Text, key, initVal);
    in.setSize(size);
    setTabOrder(in);
    tbl.add(in);
  }

  Alert findProtoAlert(String name) {
    if (name == null) return null;
    for (int ix = 0; ix < protoAlerts.length; ix++) {
      Alert proto = protoAlerts[ix];
      if (name.equals(proto.getName())) {
	return proto;
      }
    }
    return null;
  }

  Alert[] protoAlerts = {
    Alert.PERMISSION_PAGE_FETCH_ERROR,
    Alert.NO_CRAWL_PERMISSION,
    Alert.NEW_CONTENT,
    Alert.NO_NEW_CONTENT,
    Alert.VOLUME_CLOSED,
    Alert.PUBLISHER_UNREACHABLE,
    Alert.PUBLISHER_CONTENT_CHANGED,
    Alert.DAMAGE_DETECTED,
    Alert.PERSISTENT_DAMAGE,
    Alert.REPAIR_COMPLETE,
    Alert.PERSISTENT_NO_QUORUM,
    Alert.INCONCLUSIVE_POLL,
    Alert.CACHE_DOWN,
    Alert.CACHE_UP,
    Alert.DISK_SPACE_LOW,
    Alert.DISK_SPACE_FULL,
    Alert.INTERNAL_ERROR,
  };
}
