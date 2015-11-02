/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.servlet.ServeContent.PubState;
import org.lockss.servlet.ServeContent.MissingFileAction;

public class TestServeContent extends LockssServletTestCase {

  private static final Logger log = Logger.getLogger(TestServeContent.class);

  private MyServeContent sc;

  protected void setUp() throws Exception {
    super.setUp();
    sc = new MyServeContent();
  }

  public void testGetMissingFileAction() throws Exception {
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.KnownDown));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.RecentlyDown));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.NoContent));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.Unknown));

    sc.setNeverProxy(true);

    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.KnownDown));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.RecentlyDown));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.NoContent));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.Unknown));

    sc.setNeverProxy(false);
    ConfigurationUtil.setFromArgs(ServeContent.PARAM_MISSING_FILE_ACTION,
				  "Redirect");

    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.KnownDown));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.RecentlyDown));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.NoContent));
    assertEquals(MissingFileAction.Redirect,
		 sc.getMissingFileAction(PubState.Unknown));

    sc.setNeverProxy(true);

    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.KnownDown));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.RecentlyDown));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.NoContent));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.Unknown));

    sc.setNeverProxy(false);
    ConfigurationUtil.setFromArgs(ServeContent.PARAM_MISSING_FILE_ACTION,
				  "AlwaysRedirect");

    assertEquals(MissingFileAction.AlwaysRedirect,
		 sc.getMissingFileAction(PubState.KnownDown));
    assertEquals(MissingFileAction.AlwaysRedirect,
		 sc.getMissingFileAction(PubState.RecentlyDown));
    assertEquals(MissingFileAction.AlwaysRedirect,
		 sc.getMissingFileAction(PubState.NoContent));
    assertEquals(MissingFileAction.AlwaysRedirect,
		 sc.getMissingFileAction(PubState.Unknown));

    sc.setNeverProxy(true);

    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.KnownDown));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.RecentlyDown));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.NoContent));
    assertEquals(MissingFileAction.HostAuIndex,
		 sc.getMissingFileAction(PubState.Unknown));

    sc.setNeverProxy(false);
    ConfigurationUtil.setFromArgs(ServeContent.PARAM_MISSING_FILE_ACTION,
				  "Error_404");

    assertEquals(MissingFileAction.Error_404,
		 sc.getMissingFileAction(PubState.KnownDown));
    assertEquals(MissingFileAction.Error_404,
		 sc.getMissingFileAction(PubState.RecentlyDown));
    assertEquals(MissingFileAction.Error_404,
		 sc.getMissingFileAction(PubState.NoContent));
    assertEquals(MissingFileAction.Error_404,
		 sc.getMissingFileAction(PubState.Unknown));

    sc.setNeverProxy(true);

    assertEquals(MissingFileAction.Error_404,
		 sc.getMissingFileAction(PubState.KnownDown));
    assertEquals(MissingFileAction.Error_404,
		 sc.getMissingFileAction(PubState.RecentlyDown));
    assertEquals(MissingFileAction.Error_404,
		 sc.getMissingFileAction(PubState.NoContent));
    assertEquals(MissingFileAction.Error_404,
		 sc.getMissingFileAction(PubState.Unknown));
  }  

  public void testGetRealReferer() {
    assertNull("http://pub.host/path/file.html", sc.getRealReferer(null));

    assertEquals("http://pub.host/path/file.html",
		 sc.getRealReferer("http://localhost:8081/ServeContent?url=" +
				   "http%3A%2F%2Fpub.host%2Fpath%2Ffile.html"));
    assertEquals("http://pub.host/path/file.html?arg1=val1&arg2=a%3Db",
		 sc.getRealReferer("http://localhost:8081/ServeContent?url=" +
				   "http%3A%2F%2Fpub.host%2Fpath%2Ffile.html%3Farg1%3Dval1%26arg2%3Da%253Db"));

    assertEquals("http://pub.host/path/file2.html",
		 sc.getRealReferer("http://localhost:8081/ServeContent/" +
				   "http://pub.host/path/file2.html"));
    assertEquals("http://pub.host/path/file3.html?arg1=val2&arg3=d%3Df",
		 sc.getRealReferer("http://localhost:8081/ServeContent/" +
				   "http://pub.host/path/file3.html?arg1=val2&arg3=d%3Df"));


  }

  class MyServeContent extends ServeContent {
    boolean isNeverProxy = false;
    protected boolean isNeverProxy() {
      return isNeverProxy;
    }
    void setNeverProxy(boolean val) {
      isNeverProxy = val;
    }
    protected ServletDescr myServletDescr() {
      return AdminServletManager.SERVLET_SERVE_CONTENT;
    }
  }

}
