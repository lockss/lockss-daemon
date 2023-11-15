/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
    SERVLET_LIST_OBJECTS,
    SERVLET_LIST_HOLDINGS,   
    SERVLET_THREAD_DUMP,
    LINK_CONTACT,
    LINK_HELP,
    LOGIN_FORM,
    LINK_LOGOUT,
    SERVLET_EDIT_ACCOUNT,
    SERVLET_COUNTER_REPORTS,
    SERVLET_CXF_WEB_SERVICES,
    SERVLET_MD_MONITOR,
    SERVLET_OIOSAML
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
    SERVLET_SUB_MANAGEMENT,
    SERVLET_MD_CONTROL
};

  ServletDescr contentAdminDescrs[] = {
    SERVLET_PROXY_ACCESS_CONTROL,
    SERVLET_PROXY_AND_CONTENT,
  };

  ServletDescr contentAccessDescrs[] = {
    SERVLET_SERVE_CONTENT,
    SERVLET_EXPORT_CONTENT,
    SERVLET_MIGRATE_CONTENT,
    SERVLET_DISPLAY_CONTENT,
    SERVLET_ADD_CONTENT,
    SERVLET_ADD_CONTENT_TAB,
    SERVLET_HASH_CUS,
    SERVLET_TIME_GATE,
    SERVLET_TIME_MAP,
    LINK_EXPORTS,
    LINK_LOGS,
  };

  ServletDescr debugDescrs[] = {
    SERVLET_HASH_CUS,
    SERVLET_DEBUG_PANEL,
    LINK_LOGS,
    SERVLET_THREAD_DUMP,
    SERVLET_RAISE_ALERT,
    SERVLET_MD_MONITOR
  };


  // Ensure only the expected servlets are available to non-admin users
  public void testAdminOnlyDescrs() throws Exception {

    Set<ServletDescr> userAdminOnly = new HashSet<ServletDescr>();
    Set<ServletDescr> auAdminOnly = new HashSet<ServletDescr>();
    Set<ServletDescr> contentAdminOnly = new HashSet<ServletDescr>();
    Set<ServletDescr> contentAccessOnly = new HashSet<ServletDescr>();
    Set<ServletDescr> readOnly = new HashSet<ServletDescr>();
    Set<ServletDescr> debugOnly = new HashSet<ServletDescr>();
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
      if (descr.needsContentAccessRole()) {
	contentAccessOnly.add(descr);
      }
      if (descr.needsDebugRole()) {
	debugOnly.add(descr);
      }
      if (!descr.needsUserAdminRole()
	  && !descr.needsAuAdminRole()
	  && !descr.needsContentAdminRole()
	  && !descr.needsContentAccessRole()) {
	readOnly.add(descr);
      }

    }
    assertEquals(SetUtil.fromArray(userAdminDescrs), userAdminOnly);
    assertEquals(SetUtil.fromArray(auAdminDescrs), auAdminOnly);
    assertEquals(SetUtil.fromArray(contentAdminDescrs), contentAdminOnly);
    assertEquals(SetUtil.fromArray(contentAccessDescrs), contentAccessOnly);
    assertEquals(SetUtil.fromArray(debugDescrs), debugOnly);
    assertEquals(SetUtil.fromArray(readOnlyDescrs), readOnly);
  }
}
