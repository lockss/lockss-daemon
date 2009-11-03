/*
 * $Id: EditAccountBase.java,v 1.3.6.2 2009-11-03 23:52:01 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
import javax.servlet.http.HttpSession;

import org.lockss.account.*;
import org.lockss.config.*;
import org.mortbay.html.*;

/** Edit account data, add/remove accts (user admin only)
 */
public abstract class EditAccountBase extends LockssServlet {

  protected static final String ACTION_TAG = "action";
  protected static final String ACTION_CONFIRM = "Confirm";
  protected static final String ACTION_USER_UPDATE = "Update";

  protected static final String KEY_USER = "User";
  protected static final String KEY_OLD_PASSWD = "OldPasswd";
  protected static final String KEY_NEW_PASSWD = "NewPasswd";
  protected static final String KEY_NEW_PASSWD_2 = "NewPasswd2";
  protected static final String KEY_EMAIL = "Email";
  protected static final String KEY_FORM = "Form";
  protected static final String FORM_SUMMARY = "Summary";
  protected static final String FORM_ADD_USER = "AddUser";

  protected static final String SESSION_KEY_USER = "EditUser";

  protected final String FORM_TAMPERED_ERROR = "Bad hacker, no donut!";

  protected ConfigManager configMgr;
  protected AccountManager acctMgr;

  protected String action;			// action request by form

  // don't hold onto objects after request finished
  protected void resetLocals() {
    super.resetLocals();
  }

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    configMgr = getLockssDaemon().getConfigManager();
    acctMgr =  getLockssDaemon().getAccountManager();
  }

  protected void lockssHandleRequest() throws IOException {
    if (!getLockssDaemon().areAusStarted()) {
      displayNotStarted();
      return;
    }
    action = req.getParameter(ACTION_TAG);
    handleAccountRequest();
  }

  abstract protected void handleAccountRequest() throws IOException;

  static class RoleDesc {
    String name;
    String shortDesc;
    String longDesc;

    RoleDesc(String name, String shortDesc, String longDesc) {
      this.name = name;
      this.shortDesc = shortDesc;
      this.longDesc = longDesc;
    }
  }

  RoleDesc[] roleDescs = {
    new RoleDesc(ROLE_USER_ADMIN, "User Admin",
		 "Administer user accounts and control who is allowed access to the admin UI"),
    new RoleDesc(ROLE_CONTENT_ADMIN, "Access Admin",
		 "Control who is allowed access to content"),
    new RoleDesc(ROLE_AU_ADMIN, "Collection Admin",
		 "Select content (AUs) to be collected and preserved"),
    new RoleDesc(ROLE_DEBUG, "Debug",
		 "View debug info"),
  };

  Input addTextInput(Table tbl, String label, String key, boolean isPassword) {
    tbl.newRow();
    tbl.newCell("align=right");
    tbl.add(label);
    Input in = new Input(isPassword ? Input.Password : Input.Text,
			 key);
    setTabOrder(in);
    tbl.add(in);
    return in;
  }

  protected void endPage(Page page) throws IOException {
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

}
