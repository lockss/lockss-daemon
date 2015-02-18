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

package org.lockss.plugin.ubiquitypress;

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
import org.lockss.plugin.definable.*;

public class TestUbiquityPressArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String ROOT_URL = "http://www.presentpasts.info/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String journal_id,String year)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(JOURNAL_ID_KEY,journal_id);
    props.setProperty(YEAR_KEY,year);
    if (url!=null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
                      "org.lockss.plugin.ubiquitypress.ClockssUbiquityPressPlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testSiteNormalizer() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit pmAu = makeAu(base, "pp","2003");
    theDaemon.getLockssRepository(pmAu);
    theDaemon.getNodeManager(pmAu);

    assertEquals("http://www.google.com/ui/uo",
                 pmAu.siteNormalizeUrl("http://www.google.com/ui/uo"));
    assertEquals(
        "http://www.presentpasts.info/index.php/pp/article/52/64",
        pmAu.siteNormalizeUrl("http://www.presentpasts.info/article/52/64"));
    assertEquals(
        "http://www.presentpasts.info/index.php/pp/article/view/pp.52/92",
        pmAu.siteNormalizeUrl("http://www.presentpasts.info/article/view/pp.52/92"));
    assertEquals(
        "http://www.presentpasts.info/index.php/pp/help.html",
        pmAu.siteNormalizeUrl("http://www.presentpasts.info/help.html"));
  }
  
  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "pp","2003");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit pmAu = makeAu(base, "pp","2003");
    theDaemon.getLockssRepository(pmAu);
    theDaemon.getNodeManager(pmAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(pmAu,
        new RangeCachedUrlSetSpec(base.toString()));
    // archives page
    shouldCacheTest(ROOT_URL+"/issue/archive.htm", false, pmAu, cus);
    // index page
    shouldCacheTest(ROOT_URL+"index.html", false, pmAu, cus);
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, pmAu, cus);
    // other sites
    shouldCacheTest("http://www.dandelionbooks.net/", false, pmAu, cus);
    shouldCacheTest("http://www.sixgallerypress.com/", false, pmAu, cus);
  }

  private void shouldCacheTest(String url, boolean shouldCache,
                               ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    // 4 digit
    String expected = ROOT_URL+"index.php/pp/gateway/lockss?year=2003";
    DefinableArchivalUnit pmAu = makeAu(url, "pp","2003");
    assertEquals(ListUtil.list(expected), pmAu.getStartUrls());
  }


  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), "pp","2003");
    assertEquals("Ubiquity Press Plugin (CLOCKSS), Base URL http://www.presentpasts.info/, Journal ID pp, Year 2003", au.getName());
     }

  public static void main(String[] argv) {
    String[] testCaseList = {TestUbiquityPressArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
