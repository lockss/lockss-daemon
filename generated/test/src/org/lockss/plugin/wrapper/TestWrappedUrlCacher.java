/*
 * $Id: TestWrappedUrlCacher.java,v 1.1 2003-09-04 23:11:17 tyronen Exp $
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

/**
 * This is the test class for org.lockss.plugin.wrapper.WrappedUrlCacher
 *
 * Based on the code of TestGenericFileUrlCacher
 */
public class TestWrappedUrlCacher extends LockssTestCase {
  private WrappedArchivalUnit wau;
  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = new MockLockssDaemon();
    theDaemon.getHashService();


    MockGenericFileArchivalUnit mgfau = new MockGenericFileArchivalUnit();
    wau = (WrappedArchivalUnit)WrapperState.getWrapper(mgfau);
    mgfau.setCrawlSpec(new CrawlSpec(tempDirPath, null));
    MockPlugin plugin = new MockPlugin();
    WrappedPlugin wplug = (WrappedPlugin)WrapperState.getWrapper(plugin);
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
    MockWrappedUrlCacher cacher = new MockWrappedUrlCacher(
        wau.getAUCachedUrlSet(), "http://www.example.com/testDir/leaf1");
    cacher.setUncachedInputStream(new StringInputStream("test content"));
    Properties props = new Properties();
    props.setProperty("test1", "value1");
    WrappedUrlCacher wcacher = (WrappedUrlCacher)WrapperState.getWrapper(cacher);
    cacher.setUncachedProperties(props);
    wcacher.cache();

    WrappedCachedUrl url = (WrappedCachedUrl)wau.makeCachedUrl(
        wau.getAUCachedUrlSet(),"http://www.example.com/testDir/leaf1");
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
    String[] testCaseList = { TestWrappedUrlCacher.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  private class MockWrappedUrlCacher extends GenericFileUrlCacher {
    private InputStream uncachedIS;
    private Properties uncachedProp;

    public MockWrappedUrlCacher(CachedUrlSet owner, String url) {
      super(owner, url);
    }

    public InputStream getUncachedInputStream(long lastCached) {
      return uncachedIS;
    }

    public Properties getUncachedProperties() {
      return uncachedProp;
    }

    //mock specific acessors
    public void setUncachedInputStream(InputStream is) {
      uncachedIS = is;
    }

    public void setUncachedProperties(Properties prop) {
      uncachedProp = prop;
    }
  }

}
