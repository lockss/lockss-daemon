/*
 * $Id: TestGenericFileUrlCacher.java,v 1.16 2003-05-03 00:45:51 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.StreamUtil;
import org.lockss.repository.TestLockssRepositoryServiceImpl;
import org.lockss.plugin.base.*;

/**
 * This is the test class for org.lockss.plugin.simulated.GenericFileUrlCacher
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class TestGenericFileUrlCacher extends LockssTestCase {
  private MockGenericFileArchivalUnit mgfau;
  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    mgfau = new MockGenericFileArchivalUnit();
    mgfau.setCrawlSpec(new CrawlSpec(tempDirPath, null));
    TestLockssRepositoryServiceImpl.configCacheLocation(tempDirPath);

    theDaemon = new MockLockssDaemon();
    theDaemon.getHashService();
    theDaemon.getLockssRepositoryService().startService();
    theDaemon.setNodeManagerService(new MockNodeManagerService());

    MockPlugin plugin = new MockPlugin();
    plugin.setDefiningConfigKeys(Collections.EMPTY_LIST);
    mgfau.setPlugin(plugin);

    theDaemon.getLockssRepository(mgfau);
    theDaemon.getNodeManager(mgfau);
  }

  public void tearDown() throws Exception {
    theDaemon.getLockssRepositoryService().stopService();
    super.tearDown();
  }

  public void testCache() throws IOException {
    MockGenericFileUrlCacher cacher = new MockGenericFileUrlCacher(
        mgfau.getAUCachedUrlSet(), "http://www.example.com/testDir/leaf1");
    cacher.setUncachedInputStream(new StringInputStream("test content"));
    Properties props = new Properties();
    props.setProperty("test1", "value1");
    cacher.setUncachedProperties(props);
    cacher.cache();

    CachedUrl url = mgfau.cachedUrlFactory(mgfau.getAUCachedUrlSet(),
        "http://www.example.com/testDir/leaf1");
    InputStream is = url.openForReading();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(12);
    StreamUtil.copy(is, baos);
    is.close();
    assertTrue(baos.toString().equals("test content"));
    baos.close();

    props = url.getProperties();
    assertTrue(props.getProperty("test1").equals("value1"));
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestGenericFileUrlCacher.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
