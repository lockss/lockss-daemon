/*
 * $Id: TestBaseUrlCacher.java,v 1.7.2.1 2003-10-09 23:22:44 eaalto Exp $
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

package org.lockss.plugin.base;

import java.io.*;
import java.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.repository.*;

/**
 * This is the test class for org.lockss.plugin.simulated.GenericFileUrlCacher
 *
 * @author  Emil Aalto
 * @version 0.0
 */
public class TestBaseUrlCacher extends LockssTestCase {
  MyMockBaseUrlCacher cacher;
  MockCachedUrlSet mcus;
  private MockArchivalUnit mau;
  private MockLockssDaemon theDaemon;
  private int pauseBeforeFetchCounter;

  private static final String TEST_URL = "http://www.example.com/testDir/leaf1";

  public void setUp() throws Exception {
    super.setUp();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = new MockLockssDaemon();
    theDaemon.getHashService();

    mau = new MyMockArchivalUnit();
    mau.setCrawlSpec(new CrawlSpec(tempDirPath, null, null, 1));
    MockPlugin plugin = new MyMockPlugin();
    plugin.initPlugin(theDaemon);
    plugin.setDefiningConfigKeys(Collections.EMPTY_LIST);
    mau.setPlugin(plugin);

    theDaemon.getLockssRepository(mau);
    theDaemon.getNodeManager(mau);

    mcus = new MockCachedUrlSet(TEST_URL);
    mcus.setArchivalUnit(mau);
    cacher = new MyMockBaseUrlCacher(mcus, TEST_URL);
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  public void testCache() throws IOException {
    pauseBeforeFetchCounter = 0;

    cacher._input = new StringInputStream("test stream");
    cacher._headers = new Properties();
    cacher.cache();
    // should cache
    assertTrue(cacher.wasStored);
    assertEquals(1, pauseBeforeFetchCounter);
  }

  public void testLastModifiedCache() throws IOException {
    // add the 'cached' version
    Properties cachedProps = new Properties();
    cachedProps.setProperty("date", "12345");
    mcus.addUrl("test stream", TEST_URL, true, true, cachedProps);

    TimeBase.setSimulated(10000);
    cacher._input = new StringInputStream("test stream");
    cacher._headers = new Properties();
    cacher.cache();
    // shouldn't cache
    assertFalse(cacher.wasStored);

    TimeBase.step(5000);
    cacher._input = new StringInputStream("test stream");
    cacher._headers = new Properties();
    cacher.cache();
    // should cache now
    assertTrue(cacher.wasStored);

    TimeBase.setReal();
  }

  public void testForceCache() throws IOException {
    // add the 'cached' version
    Properties cachedProps = new Properties();
    cachedProps.setProperty("date", "12345");
    mcus.addUrl("test stream", TEST_URL, true, true, cachedProps);

    TimeBase.setSimulated(10000);
    cacher._input = new StringInputStream("test stream");
    cacher._headers = cachedProps;
    // should still cache
    cacher.forceCache();
    assertTrue(cacher.wasStored);

    TimeBase.setReal();
  }

  public void testCacheExceptions() throws IOException {
    cacher._input = new StringInputStream("test stream");
    cacher._headers = null;
    try {
      cacher.cache();
      fail("Should have thrown CachingException.");
    } catch (BaseUrlCacher.CachingException ce) { }
    assertFalse(cacher.wasStored);

    // no exceptions from null inputstream
    cacher._input = null;
    cacher._headers = new Properties();
    cacher.cache();
    // should simply skip
    assertFalse(cacher.wasStored);

    cacher._input = new StringInputStream("test stream");
    cacher._headers = new Properties();
    cacher.cache();
    assertTrue(cacher.wasStored);
  }

  public void testFileCache() throws IOException {
    cacher._input = new StringInputStream("test content");
    Properties props = new Properties();
    props.setProperty("test1", "value1");
    cacher._headers = props;
    cacher.cache();

    CachedUrl url = new BaseCachedUrl(mcus, TEST_URL);
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
    String[] testCaseList = { TestBaseUrlCacher.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

  private class MyMockPlugin extends MockPlugin {
    public CachedUrlSet makeCachedUrlSet(ArchivalUnit owner,
                                         CachedUrlSetSpec cuss) {
      return new BaseCachedUrlSet(owner, cuss);
    }

    public CachedUrl makeCachedUrl(CachedUrlSet owner, String url) {
      return ((MockCachedUrlSet)owner).makeCachedUrl(url);
    }

    public UrlCacher makeUrlCacher(CachedUrlSet owner, String url) {
      return new BaseUrlCacher(owner,url);
    }
  }

  private class MyMockBaseUrlCacher extends BaseUrlCacher {
    InputStream _input = null;
    Properties _headers = null;
    boolean wasStored = false;

    public MyMockBaseUrlCacher(CachedUrlSet owner, String url) {
      super(owner, url);
    }

    public InputStream getUncachedInputStream(long lastCached) {
      // simple version which returns null if shouldn't fetch
      if (lastCached < TimeBase.nowMs()) {
        return _input;
      } else {
        return null;
      }
    }

    public Properties getUncachedProperties() {
      return _headers;
    }

    public void storeContent(InputStream input, Properties headers)
        throws IOException {
      super.storeContent(input, headers);
      wasStored = true;
    }
  }

  private class MyMockArchivalUnit extends MockArchivalUnit {
    public void pauseBeforeFetch() {
      pauseBeforeFetchCounter++;
    }
  }
}
