/*

Copyright (c) 2013-2016 Board of Trustees of Leland Stanford Jr. University,
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
    LINK_CONTACT,
    LINK_HELP,
    SERVLET_CXF_WEB_SERVICES,
    SERVLET_OIOSAML
  };

  ServletDescr userAdminDescrs[] = {
    LINK_ISOS,
  };

  ServletDescr auAdminDescrs[] = {
};

  ServletDescr contentAdminDescrs[] = {
  };

  ServletDescr contentAccessDescrs[] = {
    LINK_EXPORTS,
    LINK_LOGS,
  };

  ServletDescr debugDescrs[] = {
    LINK_LOGS,
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
