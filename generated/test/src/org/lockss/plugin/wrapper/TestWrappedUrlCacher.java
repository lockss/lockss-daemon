/*
 * $Id: TestWrappedUrlCacher.java,v 1.2 2004-01-27 00:41:49 tyronen Exp $
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

package org.lockss.plugin.wrapper;

import java.io.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.plugin.*;
import junit.framework.*;

/**
 * This is the test class for org.lockss.plugin.wrapper.WrappedUrlCacher
 *
 * Based on the code of TestBaseUrlCacher
 */
public class TestWrappedUrlCacher extends LockssTestCase {
  private WrappedArchivalUnit wau;
  private WrappedPlugin wplug;
  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = new MockLockssDaemon();
    theDaemon.getHashService();


    MockArchivalUnit mgfau = new MockArchivalUnit();
    wau = (WrappedArchivalUnit)WrapperState.getWrapper(mgfau);
    mgfau.setCrawlSpec(new CrawlSpec(tempDirPath, null));
    MockPlugin plugin = new MockPlugin();
    wplug = (WrappedPlugin)WrapperState.getWrapper(plugin);
    wplug.initPlugin(theDaemon);
    plugin.setDefiningConfigKeys(Collections.EMPTY_LIST);
    mgfau.setPlugin(wplug);

    theDaemon.getLockssRepository(wau);
    theDaemon.getNodeManager(wau);
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testCache() throws IOException {
    final String URL = "http://www.example.com/testDir/leaf1";
    WrappedCachedUrlSet wset = (WrappedCachedUrlSet)wau.getAuCachedUrlSet();
    MockCachedUrlSet mset = (MockCachedUrlSet)wset.getOriginal();
    Properties props = new Properties();
    props.setProperty("test1", "value1");
    mset.addUrl(URL,false,true,props);
    WrappedUrlCacher wcacher = (WrappedUrlCacher)wplug.makeUrlCacher(wset,URL);
    int count = mset.getNumCacheAttempts(URL);
    wcacher.cache();
    assertEquals(mset.getNumCacheAttempts(URL),count+1);
  }

}
