/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.io.*;
import java.net.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * Test class for org.lockss.util.ResourceURLConnection
 */
public class TestResourceURLConnection extends LockssTestCase {
  private MockLockssDaemon daemon;
  private UrlManager uMgr;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();

    // make and start a UrlManager to set up the URLStreamHandlerFactory
    uMgr = new UrlManager();
    uMgr.initService(daemon);
    daemon.setDaemonInited(true);
    uMgr.startService();
  }

  public void tearDown() throws Exception {
    uMgr.stopService();
    super.tearDown();
  }

  static String testPat =
    "Test file for ResourceURLConnection\\. " +
    "Named \\.xml so will be copied into the jar\\.";

  public void testResourceUrl() throws Exception {
    URL url = new URL("resource:org/lockss/util/resource-url-test.xml");
    URLConnection conn = url.openConnection();
    InputStream in = conn.getInputStream();
    String content = StringUtil.fromReader(new InputStreamReader(in));
    assertMatchesRE(testPat, content);
  }

  public void testNotFound() throws Exception {
    URL url = new URL("resource:org/lockss/util/no.such.file.nohow");
    try {
      URLConnection conn = url.openConnection();
      fail("Should have throw FileNotFoundException");
    } catch (FileNotFoundException e) {
    }
  }
}
