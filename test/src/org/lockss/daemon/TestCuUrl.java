/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.plugin.*;
import org.lockss.config.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.repository.LockssRepositoryImpl;

/**
 * Test class for org.lockss.daemon.CuUrl
 */

public class TestCuUrl extends LockssTestCase {
  static Logger log = Logger.getLogger("TestCuUrl");
  private MockLockssDaemon theDaemon;
  private UrlManager uMgr;
  private StaticContentPlugin.SAU au;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
		      tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();

    // make and init a real Pluginmgr
    theDaemon.getPluginManager();

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

    theDaemon.getLockssRepository(au);
  }

  public void tearDown() throws Exception {
    uMgr.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  // constants for static content
  public static String testUrls[] = {
    "http://foo.bar/one",
    "http://foo.bar/two",
  };

  public static String testTypes[] = {
    "text/plain",
    "text/html",
  };

  public static String testContents[] = {
    "this is one text\n",
    "<html><h3>this is two html</h3></html>",
  };

  /**
   * Create and store some content in the au
   * @param au the static AU
   * @return an ArchivalUnit
   */
  public static ArchivalUnit fillAu(StaticContentPlugin.SAU au) {
    for (int i = 0; i < testUrls.length; i++) {
      au.storeCachedUrl(testUrls[i], testTypes[i], testContents[i]);
    }
    return au;
  }

  public void testCuUrl() throws Exception {
    CachedUrlSet cus = au.getAuCachedUrlSet();
    log.debug("cus: " + cus);
    // non-existent url should return null CU
    assertNull(au.makeCachedUrl("foobarnotthere"));

    tryUrl(au, cus, 0);
    tryUrl(au, cus, 1);
  }

  private void tryUrl(ArchivalUnit au, CachedUrlSet cus, int n)
      throws Exception {

    // get a test CU
    CachedUrl cu = au.makeCachedUrl(new String(testUrls[n]));
    assertNotNull(cu);

    // make a CuUrl URL from it
    URL cuurl = CuUrl.fromCu(au, cu);
    log.debug("cuurl: " + cuurl);
    assertNotNull(cuurl);

    // try opening it and fetching its properties and content
    URLConnection uconn = cuurl.openConnection();
    log.debug("uconn: " + uconn);
    assertNotNull(uconn);

    Object uobj = cuurl.getContent();
    log.debug("uobj: " + uobj);
    assertNotNull(uobj);

    assertEquals(testTypes[n], uconn.getContentType());
    assertEquals(testContents[n].length(), uconn.getContentLength());
    InputStream in = uconn.getInputStream();
    Reader r = new InputStreamReader(in);
    assertEquals(testContents[n], StringUtil.fromReader(r));

    Map hdrFields = uconn.getHeaderFields();
    assertEquals(MapUtil.map("x-lockss-content-type",
			     ListUtil.list(testTypes[n]),
			     "content-length",
			     ListUtil.list(""+testContents[n].length())),
		 hdrFields);
    try {
      hdrFields.put("foo", "bar");
      fail("getHeaderFields() is required to return an unmodifiable map");
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testIsCuUrl() {
    assertTrue(CuUrl.isCuUrl("locksscu://foo"));
    assertTrue(CuUrl.isCuUrl("LOCKSSCU://foo"));
    assertTrue(CuUrl.isCuUrl("locksscu:"));
    assertTrue(CuUrl.isCuUrl("LOCKSSCU:"));
    assertFalse( CuUrl.isCuUrl("locksscu"));
    assertFalse( CuUrl.isCuUrl("http://foo"));
  }
}
