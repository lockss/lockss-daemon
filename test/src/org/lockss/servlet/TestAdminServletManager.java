/*
 * $Id: TestAdminServletManager.java,v 1.4 2009-07-22 06:37:20 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import static org.lockss.servlet.AdminServletManager.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * Test class for org.lockss.servlet.AdminServletManager
 */
public class TestAdminServletManager extends LockssTestCase {

  AdminServletManager mgr;

  public void setUp() throws Exception {
    super.setUp();
    mgr = new AdminServletManager();
  }

  ServletDescr readOnlyDescrs[] = {
    SERVLET_HOME,
    SERVLET_PROXY_INFO,
    SERVLET_DAEMON_STATUS,
    SERVLET_DISPLAY_CONTENT,
    SERVLET_SERVE_CONTENT,
    SERVLET_LIST_OBJECTS,
    SERVLET_HASH_CUS,
    LINK_LOGS,
    SERVLET_THREAD_DUMP,
    LINK_CONTACT,
    LINK_HELP,
    LOGIN_FORM,
    LINK_LOGOUT,
    SERVLET_EDIT_ACCOUNT,
  };

  ServletDescr userAdminDescrs[] = {
    SERVLET_ADMIN_ACCESS_CONTROL,
    LINK_ISOS,
    SERVLET_RAISE_ALERT,
    SERVLET_EDIT_ACCOUNTS,
    SERVLET_EXPERT_CONFIG,
  };

  ServletDescr auAdminDescrs[] = {
    SERVLET_BATCH_AU_CONFIG,
    SERVLET_AU_CONFIG,
    SERVLET_PLUGIN_CONFIG,
    SERVLET_DEBUG_PANEL,
};

  ServletDescr contentAdminDescrs[] = {
    SERVLET_PROXY_ACCESS_CONTROL,
    SERVLET_PROXY_AND_CONTENT,
  };

  ServletDescr debugDescrs[] = {
    SERVLET_HASH_CUS,
    SERVLET_DEBUG_PANEL,
    LINK_LOGS,
    SERVLET_THREAD_DUMP,
    SERVLET_RAISE_ALERT,
  };


  // Ensure only the expected servlets are available to non-admin users
  public void testAdminOnlyDescrs() throws Exception {

    Set userAdminOnly = new HashSet();
    Set auAdminOnly = new HashSet();
    Set contentAdminOnly = new HashSet();
    Set readOnly = new HashSet();
    Set debugOnly = new HashSet();
    for (ServletDescr descr : mgr.getServletDescrs()) {
      if (descr.needsUserAdminRole()) {
	userAdminOnly.add(descr);
      }
      if (descr.needsAuAdminRole()) {
	auAdminOnly.add(descr);
      }
      if (descr.needsContentAdminRole()) {
	contentAdminOnly.add(descr);
      }
      if (descr.needsDebugRole()) {
	debugOnly.add(descr);
      }
      if (!descr.needsUserAdminRole()
	  && !descr.needsAuAdminRole()
	  && !descr.needsContentAdminRole()) {
	readOnly.add(descr);
      }

    }
    assertEquals(SetUtil.fromArray(userAdminDescrs), userAdminOnly);
    assertEquals(SetUtil.fromArray(auAdminDescrs), auAdminOnly);
    assertEquals(SetUtil.fromArray(contentAdminDescrs), contentAdminOnly);
    assertEquals(SetUtil.fromArray(debugDescrs), debugOnly);
    assertEquals(SetUtil.fromArray(readOnlyDescrs), readOnly);
  }
}
