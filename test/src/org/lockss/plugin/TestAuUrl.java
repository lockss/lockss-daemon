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

package org.lockss.plugin;

import java.io.*;
import java.net.*;
import junit.framework.TestCase;
//import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.test.*;


/**
 * Test class for org.lockss.plugin.AuUrl
 */

public class TestAuUrl extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private PluginManager pluginMgr;
  private UrlManager uMgr;

  public void setUp() throws Exception {
    super.setUp();

    // Can't use URLStreamHandlerFactory from two different test classes,
    // because they're loaded using different class loaders.  AuUrl doesn't
    // need its URLStreamHandler, so don't test that here, in favor of
    // TestCuUrl.

//     theDaemon = getMockLockssDaemon();
//     pluginMgr = theDaemon.getPluginManager();
//     uMgr = new UrlManager();
//     uMgr.initService(theDaemon);
//     theDaemon.setDaemonInited(true);
//     uMgr.startService();
  }

  public void tearDown() throws Exception {
//     uMgr.stopService();
//     theDaemon.stopDaemon();
    super.tearDown();
  }

  static final String testConfigString = "a/string with?special&chars";

  // Turned off.  See above
  public void atestAuUrl() throws Exception {
    URL auurl = new URL("lockssau://foo.bar/123/journal-config");
    assertTrue(AuUrl.isAuUrl(auurl));
    assertEquals("lockssau", auurl.getProtocol());
    assertEquals("foo.bar", auurl.getHost());
    assertEquals("/123/journal-config", auurl.getPath());
    // make sure we can still create "normal" URLs
    URL url = new URL("http://example.com/path");
    assertFalse(AuUrl.isAuUrl(url));

    URL au = AuUrl.fromAuId(testConfigString);
    assertEquals(testConfigString, AuUrl.getAuId(au));
  }

  public void testIsAuUrl() {
    assertTrue(AuUrl.isAuUrl("lockssau://foo"));
    assertTrue(AuUrl.isAuUrl("LOCKSSAU://foo"));
    assertTrue(AuUrl.isAuUrl("lockssau:"));
    assertTrue(AuUrl.isAuUrl("LOCKSSAU:"));
    assertFalse( AuUrl.isAuUrl("lockssau"));
    assertFalse( AuUrl.isAuUrl("http://foo"));
  }
}
