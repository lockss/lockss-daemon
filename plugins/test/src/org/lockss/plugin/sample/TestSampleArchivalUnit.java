/*
 * $Id: TestSampleArchivalUnit.java,v 1.3 2007-01-14 08:13:17 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.sample;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.repository.LockssRepositoryImpl;

public class TestSampleArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String ROOT_URL = "http://www.example.com/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();

  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, 1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testConstructNegativeVolume() throws Exception {
    URL url = new URL(ROOT_URL);
    try {
      makeAu(url, -1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testMakeName() throws Exception {
    // make name should return <base>, vol. <vol>
    SampleArchivalUnit au = makeAu(new URL(ROOT_URL), 108);
    assertEquals("www.example.com, vol. 108", au.getName());

    SampleArchivalUnit au1 =
        makeAu(new URL("http://www.example2.com/"), 109);
    assertEquals("www.example2.com, vol. 109", au1.getName());
  }

  public void testMakeStartUrl() throws Exception {
    URL url = new URL(ROOT_URL);

    String expectedStr = ROOT_URL+ "lockss-volume108.html";
    SampleArchivalUnit sAu = makeAu(url, 108);
    assertEquals(expectedStr, sAu.makeStartUrl());
  }


  public void testMakeRules() throws Exception {
    URL base = new URL(ROOT_URL);
    int volume = 108;
    ArchivalUnit sAu = makeAu(base, volume);
    theDaemon.getLockssRepository(sAu);
    theDaemon.getNodeManager(sAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(sAu,
        new RangeCachedUrlSetSpec(base.toString()));

    String baseUrl = ROOT_URL;

    // *** we should cache the following ***

    // root page
    shouldCacheTest(baseUrl+"lockss-volume108.html", true, sAu, cus);

    // issue index page
    shouldCacheTest(baseUrl+"108/issue1/index.html", true, sAu, cus);

    // issue article html
    shouldCacheTest(baseUrl+"108/issue3/ah0103000001.html", true, sAu, cus);

    // images gif or jpg images
    shouldCacheTest(baseUrl+"images/vol108_iss1.gif", true, sAu, cus);
    shouldCacheTest(baseUrl+"108/images/hunt_fig1b.jpg", true, sAu, cus);

    // *** should not cache these ***

    // archived root page
    shouldCacheTest(baseUrl+"lockss-volume107.html", false, sAu, cus);

    // a different volume page
    shouldCacheTest(baseUrl+"109/", false, sAu, cus);

    // html destinations outside of volume
    shouldCacheTest(ROOT_URL+"outside.html", false, sAu, cus);

    // a different site.
    shouldCacheTest("http://lockss.stanford.edu", false, sAu, cus);

  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://www.example.org/";
    SampleArchivalUnit sAu1 = makeAu(new URL(stem1), 108);
    assertEquals(ListUtil.list(stem1), sAu1.getUrlStems());
  }

  /**
   * Utility function used to determine if a url matched the requested cache
   * @param url the url to check
   * @param shouldCache boolean used to indicate if we expect this url to be cached
   * @param au the ArchivalUnit used to construct a new UrlCacher
   * @param cus the CachedUrlSet for this ArchivalUnit.
   */
  private void shouldCacheTest(String url, boolean shouldCache,
                              ArchivalUnit au, CachedUrlSet cus) {
   UrlCacher uc = au.makeUrlCacher(url);
   assertTrue(uc.shouldBeCached()==shouldCache);
 }


 /**
   * Utiltiy method used to contstruct a new SampleArchivalUnit from
   * a base url and voluem
   * @param url the url to use as the base url
   * @param volume the volume id to use
   * @return a new SampleArchivalUnit
   * @throws ArchivalUnit.ConfigurationException
   */
  private SampleArchivalUnit makeAu(URL url, int volume)
      throws ArchivalUnit.ConfigurationException {
    Properties props = new Properties();
    props.setProperty(SamplePlugin.AUPARAM_VOL, Integer.toString(volume));
    if (url!=null) {
      props.setProperty(SamplePlugin.AUPARAM_BASE_URL, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    SamplePlugin sp = new SamplePlugin();
    sp.initPlugin(theDaemon);
    SampleArchivalUnit au = (SampleArchivalUnit)sp.createAu(config);

    return au;
  }


  public static void main(String[] argv) {
    String[] testCaseList = {TestSampleArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
