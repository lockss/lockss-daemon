/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.jetty;

import java.io.*;
import java.net.*;
import java.util.*;
import org.mortbay.util.Resource;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestCuUrlResource extends LockssTestCase {
  static Logger log = Logger.getLogger("TestCuUrlResource");
  private MockLockssDaemon theDaemon;
  private UrlManager uMgr;
  private StaticContentPlugin.SAU au;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();

    // make and start a UrlManager to set up the URLStreamHandlerFactory
    uMgr = new UrlManager();
    uMgr.initService(theDaemon);
    theDaemon.setDaemonInited(true);
    uMgr.startService();

    // create an AU with some static content
    StaticContentPlugin spl = new StaticContentPlugin();
    spl.initPlugin(theDaemon);
    au = (StaticContentPlugin.SAU)spl.createAu(null);
    PluginTestUtil.registerArchivalUnit(spl, au);
    fillAu(au);
  }

  public void tearDown() throws Exception {
    uMgr.stopService();
    super.tearDown();
  }

  // constants for static content
  public static String testUrls[] = {
    "http://foo.bar/one",
    "http://foo.bar/two",
    "http://foo.bar/three",
  };

  public static long testContentLengthOffsets[] = {
    0,
    4,
    -3,
  };

  public static String testContents[] = {
    "content 10123456789012345678901234567890123456789",
    "content 201234567890123456789012",
    "content 301234567890123456789012345678901234567890123456789",
  };

  /**
   * Create and store some content in the au
   */
  public static ArchivalUnit fillAu(StaticContentPlugin.SAU au) {
    for (int i = 0; i < testUrls.length; i++) {
      au.storeCachedUrl(testUrls[i], "text/plain", testContents[i]);
      CachedUrl scu = au.makeCachedUrl(testUrls[i]);
      Properties props = scu.getProperties();
      long clen = Long.parseLong(props.getProperty("Content-Length"));
      props.setProperty("Content-Length",
			""+(clen+testContentLengthOffsets[i]));
    }
    return au;
  }

  public void testNull() throws Exception {
    Resource res = new CuUrlResource();
    assertNull(res.addPath(null));
  }

  public void testAll() throws Exception {
    for (int i = 0; i < testUrls.length; i++) {
      testOne(i);
    }
  }

  public void testOne(int n) throws Exception {
    // get a test CU
    CachedUrl cu = au.makeCachedUrl(new String(testUrls[n]));
    assertNotNull(cu);

    // make a CuUrl URL from it
    URL cuurl = CuUrl.fromCu(au, cu);
    assertNotNull(cuurl);

    Resource res = new CuUrlResource().addPath(cuurl.toString());
    assertNotNull(res);
    assertTrue(res.exists());
    assertTrue(CuUrl.isCuUrl(res.getURL()));
    // length() shoudl retuwn actual content length
    assertEquals(testContents[n].length(), res.length());

    CuUrlResource cures = (CuUrlResource)res;
    long off = testContentLengthOffsets[n];
    assertEquals(testContents[n].length() + off,
		 cures.getHeaderContentLength());

    assertInputStreamMatchesString(testContents[n], res.getInputStream());
  }

}
