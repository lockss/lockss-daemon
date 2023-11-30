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
