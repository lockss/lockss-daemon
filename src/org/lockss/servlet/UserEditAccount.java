/*
 * $Id: UserEditAccount.java,v 1.1 2009-06-01 07:53:32 tlipkis Exp $
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
import org.lockss.util.*;
import org.mortbay.html.*;

/** Edit account data, add/remove accts (user admin only)
 */
public class UserEditAccount extends EditAccountBase {

  protected void handleAccountRequest() throws IOException {
    if (StringUtil.isNullString(action)) {
      displayUserEdit();
    } else if (action.equals(ACTION_USER_UPDATE)) {
      doUserUpdate();
    } else {
      errMsg = "Unknown action: " + action;
      displayUserEdit();
    }
  }

  private void displayUserEdit() throws IOException {
    String name = req.getUserPrincipal().toString();
    UserAccount acct = acctMgr.getUser(name);
    if (acct == null) {
      displayWarningInLieuOfPage("Error: User " + name + " does not exist");
      return;
    }
    Page page = newPage();
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, "Editing User " + name);
    Form frm = ServletUtil.newForm(srvURL(myServletDescr()));
    Table tbl =
      new Table(0, "align=center cellspacing=4 border=1 cellpadding=2");
    tbl.newRow();
    tbl.newCell();

    tbl.add(buildUserEditTable(acct));

    frm.add(tbl);
    ServletUtil.layoutSubmitButton(this, frm, ACTION_USER_UPDATE);
    page.add(frm);
    endPage(page);
  }

  private Table buildUserEditTable(UserAccount acct) {
    Table tbl = new Table(0, "align=center cellspacing=1 border=1 cellpadding=2");
    addTextInput(tbl, "Old password: ", KEY_OLD_PASSWD, true);
    addTextInput(tbl, "New password: ", KEY_NEW_PASSWD, true);
    addTextInput(tbl, "Confirm password: ", KEY_NEW_PASSWD_2, true);
    tbl.newRow();
    tbl.newCell("style=\"height:1em\"");
    Input eml = addTextInput(tbl, "Email address: ", KEY_EMAIL, false);
    eml.attribute("value", acct.getEmail());
    return tbl;
  }


  protected void doUserUpdate() throws IOException {
    HttpSession session = getSession();

    String name = req.getUserPrincipal().toString();
    UserAccount acct = acctMgr.getUser(name);
    if (acct == null) {
      displayWarningInLieuOfPage("Error: User " + name + " does not exist");
      return;
    }

    String oldPwd = req.getParameter(KEY_OLD_PASSWD);
    String pwd1 = req.getParameter(KEY_NEW_PASSWD);
    String pwd2 = req.getParameter(KEY_NEW_PASSWD_2);
    String email = req.getParameter(KEY_EMAIL);

    if (StringUtil.isNullString(pwd1)
	&& StringUtil.isNullString(pwd2)
	&& StringUtil.isNullString(email)) {
      statusMsg = "No changes";
      displayUserEdit();
      return;
    }
    // validate all input first
    if (!acct.check(oldPwd)) {
      errMsg = "Incorrect password";
      displayUserEdit();
      return;
    }

    if (!StringUtil.isNullString(pwd1) || !StringUtil.isNullString(pwd2)) {
      if (!StringUtil.equalStrings(pwd1, pwd2)) {
	errMsg = "Passwords don't match";
	displayUserEdit();
	return;
      }
    }
    if (!StringUtil.isNullString(email)) {
      try {
	new javax.mail.internet.InternetAddress(email, true);
      } catch (javax.mail.internet.AddressException e) {
	errMsg = "Malformed email address: " + e.getMessage();
	displayUserEdit();
	return;
      }
    }

    // then update
    if (!StringUtil.isNullString(pwd1)) {
      try {
	acct.setPassword(pwd1);
      } catch (UserAccount.IllegalPasswordChange e) {
	errMsg = e.getMessage();
	displayUserEdit();
	return;
      }
    }
    if (!StringUtil.isNullString(email)) {
      acct.setEmail(email);
    }
      try {
	acctMgr.storeUser(acct);
      } catch (AccountManager.NotStoredException e) {
	errMsg = "Error: " + e.getMessage();
	displayUserEdit();
	return;
      }
    statusMsg = "Update successful";
    displayUserEdit();
  }

}
