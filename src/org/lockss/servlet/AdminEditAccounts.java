/*
 * $Id: AdminEditAccounts.java,v 1.1 2009-06-01 07:53:32 tlipkis Exp $
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

// XXX prevent browser from filling in new password field(s).

public class AdminEditAccounts extends EditAccountBase {

  protected static final String ACTION_ADMIN_ADD = "Add user";
  protected static final String ACTION_ADMIN_UPDATE = "Update user";
  protected static final String ACTION_ADMIN_DELETE = "Delete user";
  protected static final String ACTION_ADMIN_CONFIRM_DELETE = "Confirm delete";

  protected void handleAccountRequest() throws IOException {
    if (!doesUserHaveRole(ROLE_USER_ADMIN)) {
      return;
    }
    if (StringUtil.isNullString(action)) {
      String user = req.getParameter(KEY_USER);
      if (StringUtil.isNullString(user)) {
	displayAdminSummary();
      } else {
	displayEditUser(user);
      }
    } else if (action.equals(ACTION_ADMIN_ADD)) {
      String form = req.getParameter(KEY_FORM);
      if (FORM_SUMMARY.equals(form)) {
	displayAddUser(req.getParameter(KEY_USER));
      } else if (FORM_ADD_USER.equals(form)) {
	doAdminUpdate();
      } else {
	displayAdminSummary();
      }
    } else if (action.equals(ACTION_ADMIN_UPDATE)) {
      doAdminUpdate();
    } else if (action.equals(ACTION_ADMIN_DELETE)) {
      displayEditUser(req.getParameter(KEY_USER));
    } else if (action.equals(ACTION_ADMIN_CONFIRM_DELETE)) {
      doAdminDelete();
    } else {
      errMsg = "Unknown action: " + action;
      displayAdminSummary();
    }
  }

  protected void doAdminDelete() throws IOException {
    HttpSession session = getSession();
    String name = req.getParameter(KEY_USER);
    if (action == null || name == null
	|| !name.equals(session.getAttribute(SESSION_KEY_USER))) {
      errMsg = FORM_TAMPERED_ERROR;
      displayAdminSummary();
      return;
    }
    UserAccount acct = acctMgr.getUser(name);
    if (acct == null) {
      errMsg = "User " + name + " disappeared abruptly!";
      displayAdminSummary();
      return;
    }
    if (acctMgr.deleteUser(acct)) {
      statusMsg = name + " deleted";
      displayAdminSummary();
      return;
    } else {
      errMsg = "Delete failed!";
      displayAdminSummary();
      return;
    }
  }

  protected void doAdminUpdate() throws IOException {
    HttpSession session = getSession();

    String name = req.getParameter(KEY_USER);
    if (action == null || name == null
	|| !name.equals(session.getAttribute(SESSION_KEY_USER))) {
      errMsg = FORM_TAMPERED_ERROR;
      displayAdminSummary();
      return;
    }
    String pwd1 = req.getParameter(KEY_NEW_PASSWD);
    String pwd2 = req.getParameter(KEY_NEW_PASSWD_2);
    String email = req.getParameter(KEY_EMAIL);

    String roles = getRolesFromForm();

    UserAccount acct;
    if (action.equals(ACTION_ADMIN_ADD)) {
      if (acctMgr.hasUser(name)) {
	errMsg = "Error: " + name + " already exists";
	displayAdminSummary();
	return;
      }
      acct = acctMgr.createUser(name);
    } else if (action.equals(ACTION_ADMIN_UPDATE)) {
      acct = acctMgr.getUser(name);
      if (acct == null) {
	errMsg = "User " + name + " disappeared abruptly!";
	displayAdminSummary();
	return;
      }
    } else {
      errMsg = FORM_TAMPERED_ERROR;
      displayAdminSummary();
      return;
    }

    if (!StringUtil.equalStrings(pwd1, pwd2)) {
      errMsg = "Error: passwords don't match";
      displayEditAccount(acct);
      return;
    }

    if (!StringUtil.isNullString(pwd1)) {
      try {
	acct.setPassword(pwd1, true);
      } catch (UserAccount.IllegalPasswordChange e) {
	errMsg = e.getMessage();
	displayEditAccount(acct);
	return;
      }
    }
    acct.setRoles(roles);
    if (!StringUtil.isNullString(email)) {
      acct.setEmail(email);
    }
    if (action.equals(ACTION_ADMIN_ADD)) {
      try {
	acctMgr.addUser(acct);
      } catch (AccountManager.UserExistsException e) {
	errMsg = "Error: " + e.getMessage();
	displayAdminSummary();
	return;
      } catch (AccountManager.NotAddedException e) {
	errMsg = "Error: " + e.getMessage();
	displayEditAccount(acct);
	return;
      } catch (AccountManager.NotStoredException e) {
	errMsg = "Error: " + e.getMessage();
	displayEditAccount(acct);
	return;
      }
    } else if (action.equals(ACTION_ADMIN_UPDATE)) {
      try {
	acctMgr.storeUser(acct);
      } catch (AccountManager.NotStoredException e) {
	errMsg = "Update failed: " + e.getMessage();
	displayAdminSummary();
	return;
      }
    }
    statusMsg = "Update successful";
    displayAdminSummary();
  }

  void addRole(UserAccount acct, Table tbl, String role) {
    tbl.newCell("align=center");
    tbl.add(acct.isUserInRole(role) ? "Yes" : "No");
  }

  void addHeading(Table tbl, String head) {
    tbl.newCell("class=\"colhead\" valign=\"bottom\" align=\"center\"");
    tbl.add(head);
  }

  Comparator USER_COMPARATOR = new UserComparator();

  static class UserComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      UserAccount a1 = (UserAccount)o1;
      UserAccount a2 = (UserAccount)o2;
      if (a1.isStaticUser() && !a2.isStaticUser()) {
	return 1;
      }
      if (!a1.isStaticUser() && a2.isStaticUser()) {
	return -1;
      }
      return a1.getName().compareTo(a2.getName());
    }
  }

  Table buildAdminUserTable() {
    if (!doesUserHaveRole(ROLE_USER_ADMIN)) {
      throw new RuntimeException("Shouldn't happen");
    }

    Table tbl = new Table(0, "align=center cellspacing=4 cellpadding=0");
    addHeading(tbl, "User");
    for (RoleDesc rd : roleDescs) {
      String role = rd.name;
      addHeading(tbl, rd.shortDesc + addFootnote(rd.longDesc));
    }
    addHeading(tbl, "Email address");
    List<UserAccount> users = new ArrayList(acctMgr.getAccounts());
    Collections.sort(users, USER_COMPARATOR);
    for (UserAccount acct : users) {
      tbl.newRow();
      tbl.newCell();
      Object label;
      String name = acct.getName();
      tbl.add(linkIfEditable(acct, encodeText(name)));
      for (RoleDesc rd : roleDescs) {
	String role = rd.name;
	addRole(acct, tbl, rd.name);
      }

      tbl.newCell();
      tbl.add(acct.getEmail());
      if (!acct.isEnabled()) {
	tbl.newCell();
	tbl.add(linkIfEditable(acct, "Disabled"));
      }
    }
    return tbl;

  }

  String linkIfEditable(UserAccount acct, String label) {
    String name = acct.getName();
    if (acct.isEditable()) {
      Properties p = PropUtil.fromArgs(KEY_USER, name);
      return srvLink(myServletDescr(), label, concatParams(p));
    } else {
      return label;
    }
  }

  /** Display list of existing users and attributes and "Add User"
   * button */
  private void displayAdminSummary() throws IOException {

    Page page = newPage();
    layoutErrorBlock(page);

    Form frm = ServletUtil.newForm(srvURL(myServletDescr()));
    Table tbl = new Table(0, "align=center cellspacing=4 border=1 cellpadding=2");
    tbl.newRow();
    tbl.newCell("align=center");
    tbl.add("Click user name to edit");

    Table userTbl = buildAdminUserTable();
    tbl.newRow();
    tbl.newCell();
    tbl.add(userTbl);

    tbl.newRow();
    tbl.newCell("align=center");
    tbl.add("or Add a user");
    tbl.newRow();
    tbl.newCell("align=center");
    Input in = new Input(Input.Text, KEY_USER);
    in.setSize(20);
    setTabOrder(in);
    tbl.add("Username");
    tbl.add(in);
    tbl.add(new Input(Input.Hidden, KEY_FORM, FORM_SUMMARY));
    Input btn = new Input(Input.Submit, ACTION_TAG, ACTION_ADMIN_ADD);
    tbl.add(btn);

    frm.add(tbl);
    page.add(frm);

    endPage(page);
  }

  private void displayAddUser(String name) throws IOException {
    if (StringUtil.isNullString(name)) {
      errMsg = "Error: You must specify a user name to add";
      displayAdminSummary();
      return;
    }
    if (acctMgr.hasUser(name)) {
      errMsg = "Error: " + name + " already exists";
      displayAdminSummary();
      return;
    }
    // Create an initialized user of the appropriate type to supply
    // defaults for the form.  This UserAccount isn't otherwise used.
    UserAccount acct = acctMgr.createUser(name);
    displayEditAccount(acct);
  }

  private void displayEditUser(String name) throws IOException {
    if (StringUtil.isNullString(name)) {
      displayAdminSummary();
    }
    UserAccount acct = acctMgr.getUser(name);
    if (acct == null) {
      errMsg = "No such user: " + name;
      displayAdminSummary();
      return;
    }
    displayEditAccount(acct);
  }

  static String ROLE_PREFIX = "Role_";

  void addEditRole(Table tbl, UserAccount acct, RoleDesc rd) {
    String role = rd.name;
    tbl.newRow();
    tbl.newCell();
    Input cb = new Input(Input.Checkbox, ROLE_PREFIX + role, "true");
    if (acct.isUserInRole(role)) {
      cb.check();
    }      
    tbl.add(cb);
    tbl.add(rd.longDesc);
  }

  String getRolesFromForm() {
    List lst = new ArrayList();
    for (RoleDesc rd : roleDescs) {
      String role = rd.name;
      if (!StringUtil.isNullString(req.getParameter(ROLE_PREFIX + role))) {
	lst.add(role);
      }
    }
    return StringUtil.separatedString(lst, ",");
  }

  private Table buildEditRoleTable(UserAccount acct) {
    Table tbl = new Table(0, "align=center cellspacing=1 border=1 cellpadding=2");
    tbl.newRow();
    tbl.newCell();
    tbl.add("Permissions");
    for (RoleDesc rd : roleDescs) {
      addEditRole(tbl, acct, rd);
    }
    return tbl;
  }

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

  private Table buildEditAttrsTable(UserAccount acct) {
    Table tbl = new Table(0, "align=center cellspacing=1 border=1 cellpadding=2");
    addTextInput(tbl, "New password: ", KEY_NEW_PASSWD, true);
    addTextInput(tbl, "Confirm password: ", KEY_NEW_PASSWD_2, true);
    Input eml = addTextInput(tbl, "Email address: ", KEY_EMAIL, false);
    eml.attribute("value", acct.getEmail());
    return tbl;
  }


  private Table buildUserEditTable(UserAccount acct) {
    Table tbl = new Table(0, "align=center cellspacing=1 border=1 cellpadding=2");
    addTextInput(tbl, "Old password: ", KEY_OLD_PASSWD, true);
    addTextInput(tbl, "New password: ", KEY_NEW_PASSWD, true);
    addTextInput(tbl, "Confirm password: ", KEY_NEW_PASSWD_2, true);
    addTextInput(tbl, "Email address: ", KEY_EMAIL, false);
    return tbl;
  }


  private void displayUserEdit() throws IOException {
    if (false) {
      errMsg = "Error: ";
      displayAdminSummary();
      return;
    }

    String name = req.getUserPrincipal().toString();
    UserAccount acct = acctMgr.getUser(name);
    if (acct == null) {
      errMsg = "Error: User " + name + " does not exist";
      displayAdminSummary();
      return;
    }
    Page page = newPage();
    layoutErrorBlock(page);
    Form frm = ServletUtil.newForm(srvURL(myServletDescr()));
    Table tbl =
      new Table(0, "align=center cellspacing=4 border=1 cellpadding=2");
    tbl.newRow();
    tbl.newCell();

    tbl.add(buildUserEditTable(acct));

    frm.add(tbl);
    page.add(frm);
    endPage(page);
  }

  private void displayEditAccount(UserAccount acct)
      throws IOException {
    List actions;
    StringBuilder sb = new StringBuilder();
    if (acctMgr.hasUser(acct.getName())) {
      if (ACTION_ADMIN_DELETE.equals(action)) {
	sb.append("Confirm delete user: ");
	actions = ListUtil.list(ACTION_ADMIN_UPDATE,
				ACTION_ADMIN_CONFIRM_DELETE);
      } else {
	sb.append("Edit user: ");
	actions = ListUtil.list(ACTION_ADMIN_UPDATE, ACTION_ADMIN_DELETE);
      }
    } else {
      sb.append("Add user: ");
      actions = ListUtil.list(ACTION_ADMIN_ADD);
    }
    sb.append(" ");
    sb.append(acct.getName());

    String disMsg = acct.getDisabledMessage();
    if (disMsg != null) {
      sb.append("<br>");
      sb.append(disMsg);
    }
    HttpSession session = getSession();
    session.setAttribute(SESSION_KEY_USER, acct.getName());

    Page page = newPage();
    layoutErrorBlock(page);
    ServletUtil.layoutExplanationBlock(page, sb.toString());
    Form frm = ServletUtil.newForm(srvURL(myServletDescr()));
    Table tbl = new Table(0, "align=center cellspacing=4 border=1 cellpadding=2");
    tbl.newRow();
    tbl.newCell();

    tbl.add(buildEditAttrsTable(acct));
    tbl.add(buildEditRoleTable(acct));
    tbl.add(new Input(Input.Hidden, KEY_FORM, FORM_ADD_USER));
    tbl.add(new Input(Input.Hidden, KEY_USER, acct.getName()));
    frm.add(tbl);

    ServletUtil.layoutAuPropsButtons(this,
                                     frm,
                                     actions.iterator(),
                                     ACTION_TAG);

//     ServletUtil.layoutSubmitButton(this, frm, action);
    page.add(frm);
    endPage(page);
  }

  protected void endPage(Page page) throws IOException {
    layoutFooter(page);
    ServletUtil.writePage(resp, page);
  }

  // make me a link in nav table if not on initial journal config page
  protected boolean linkMeInNav() {
    return action != null
      || !StringUtil.isNullString(req.getParameter(KEY_USER));
  }

}
