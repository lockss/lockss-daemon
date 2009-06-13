/*
 * $Id: LoginForm.java,v 1.1.2.1 2009-06-13 08:52:42 tlipkis Exp $
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

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.text.*;
import java.security.*;
import org.mortbay.html.*;
import org.mortbay.util.B64Code;
import org.mortbay.http.Authenticator;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.jetty.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

/** Login page form
 */
public class LoginForm extends LockssServlet {
  static Logger log = Logger.getLogger("LoginForm");

  static final String FORM_ACTION = "j_security_check";
  static final String FORM_METHOD = "post";
  static final String KEY_USERNAME = "j_username";
  static final String KEY_PASSWORD = "j_password";

  static final String ACTION_SUBMIT = "Login";

  /** String to display on login page. */
  static final String PARAM_UI_LOGIN_BANNER =
    Configuration.PREFIX + "ui.loginBanner";



  private LockssDaemon daemon;
  private ConfigManager cfgMgr;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    daemon = getLockssDaemon();
    cfgMgr = daemon.getConfigManager();
  }

  public void lockssHandleRequest() throws IOException {
    boolean logout = !StringUtil.isNullString(req.getParameter("logout"));
    if (logout) {
      logout();
    }
    displayForm();
  }

  private void logout() {
    Authenticator auth = getServletManager().getAuthenticator();
    if (auth instanceof LockssFormAuthenticator) {
      ((LockssFormAuthenticator)auth).logout(getSession());
    }
  }

  private void displayForm() throws IOException {
    Page page = newPage();
    String banner = CurrentConfig.getParam(PARAM_UI_LOGIN_BANNER);
    if (banner != null) {
      Composite comp = new Composite();
      comp.add("<center>");
      comp.add(banner);
      comp.add("</center><br>");
      page.add(comp);
    }
    Object msg =
      getSession().getAttribute(LockssFormAuthenticator.__J_LOCKSS_AUTH_ERROR_MSG);
    if (msg != null) {
      errMsg = msg.toString();
      // Display the message only once
      getSession().setAttribute(LockssFormAuthenticator.__J_LOCKSS_AUTH_ERROR_MSG,
				null);

    }
    if (!StringUtil.isNullString(getParameter("error"))) {
      if (StringUtil.isNullString(errMsg)) {
	errMsg = "Invalid username or password";
      }
    }
    if (!StringUtil.isNullString(errMsg)) {
      layoutErrorBlock(page);
    }
    page.add(makeForm());
    page.add("<br>");
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  private Element makeForm() {
    Form frm = new Form(srvURL(myServletDescr()));
    frm.method("POST");
    frm.action(FORM_ACTION);
    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");
    tbl.newRow();
    tbl.newCell("colspan=2 align=center");
    tbl.add("<font size=\"+1\"/>Please login<font size=\"-1\"/>");

    tbl.newRow();
    tbl.newCell("align=right");
    tbl.add("Username: ");
    Input userInput = new Input(Input.Text, KEY_USERNAME);
    setTabOrder(userInput);
    tbl.add(userInput);

    tbl.newRow();
    tbl.newCell("align=right");
    tbl.add("Password: ");
    Input pwdInput = new Input(Input.Password, KEY_PASSWORD);
    setTabOrder(pwdInput);
    tbl.add(pwdInput);

    tbl.newRow();
    tbl.newCell("colspan=2 align=center");
    Input loginButton = new Input(Input.Submit, null, ACTION_SUBMIT);
    setTabOrder(loginButton);
    tbl.add(loginButton);
    frm.add(tbl);
    return frm;
  }

}
