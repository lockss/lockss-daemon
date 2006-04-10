/*
 * $Id: TestBlackbirdArchivalUnit.java,v 1.4 2006-04-10 23:12:28 troberts Exp $
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

package org.lockss.plugin.blackbird;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.state.AuState;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.plugin.definable.*;

public class TestBlackbirdArchivalUnit extends LockssPluginTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();

  static final String ROOT_URL = "http://www.blackbird.vcu.edu/";

  private MockLockssDaemon theDaemon;
  
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  private DefinableArchivalUnit makeAu(URL url, int volume)
      throws Exception {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(volume));
    if (url!=null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(theDaemon,"org.lockss.plugin.blackbird.BlackbirdPlugin");
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, 1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }
/*
  public void testConstructNegativeVolume() throws Exception {
    URL url = new URL(ROOT_URL);
    try {
      makeAu(url, -1);
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }
*/

  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    int volume = 2;
    ArchivalUnit bbAu = makeAu(base, volume);
    theDaemon.getLockssRepository(bbAu);
    theDaemon.getNodeManager(bbAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(bbAu,
        new RangeCachedUrlSetSpec(base.toString()));

    String baseUrl = ROOT_URL + "v2n1/";

    // root pages
    assertShouldCache(baseUrl+"index.htm", bbAu, cus);
    assertShouldCache(ROOT_URL+"v2n2/index.htm", bbAu, cus);

    // volume pages
    assertShouldCache(baseUrl+"poetry.htm", bbAu, cus);
    assertShouldCache(baseUrl+"gallery.htm", bbAu, cus);

    // info pages
    assertShouldCache(baseUrl+"new.htm", bbAu, cus);
    assertShouldCache(baseUrl+"editorial_policy.htm", bbAu, cus);
    assertShouldCache(ROOT_URL+"v2n2/acknowledgements.htm", bbAu, cus);

    // article html
    assertShouldCache(baseUrl+"gallery/burnside_c/index.htm", bbAu, cus);
    assertShouldCache(baseUrl+"gallery/burnside_c/retrospective.htm", 
                    bbAu, cus);
    assertShouldCache(baseUrl+"nonfiction/dillard_r/going.htm", bbAu, cus);
    assertShouldCache(baseUrl+"poetry/black_s/index.htm", bbAu, cus);

    // images
    assertShouldCache(baseUrl+"images/spacer.gif", bbAu, cus);
    assertShouldCache(baseUrl+"images/audio.gif", bbAu, cus);
    assertShouldCache(baseUrl+"gallery/burnside_c/burnside6_125.jpg", 
                    bbAu, cus);

    // stylesheet
    assertShouldCache(baseUrl+"blkbird.css", bbAu, cus);
    assertShouldCache(baseUrl+"poetry/blkbird.css", bbAu, cus);


    // ram files
    assertShouldCache(baseUrl+"gallery/burnside_c/interview_part1.ram",
                    bbAu, cus);
    assertShouldCache(baseUrl+"gallery/burnside_c/interview_part1.rm", 
                      bbAu, cus);
    assertShouldCache(baseUrl+"gallery/burnside_c_091603/lucy1.ram", bbAu, cus);

    assertShouldCache(ROOT_URL+"lockss_media/v2n2/gallery/burnside_c/interview_part1.rm",
		    bbAu, cus);

    assertShouldNotCache(ROOT_URL+"v1n2/gallery/burnside_c/interview_part1.ram",
		    bbAu, cus);

    // current issue
    assertShouldNotCache(ROOT_URL, bbAu, cus);
    assertShouldNotCache(ROOT_URL+"index.htm", bbAu, cus);

    // index links

    // archived root page
    assertShouldNotCache(ROOT_URL+"v1n1/index.htm", bbAu, cus);

    // archived volume page
    assertShouldNotCache(ROOT_URL+"v1n2/gallery/example.htm", bbAu, cus);

    // LOCKSS
    assertShouldNotCache("http://lockss.stanford.edu", bbAu, cus);

    // other site
    assertShouldNotCache("http://www.real.com/", bbAu, cus);
  }

  public void testStartUrlConstruction() throws Exception {
    URL url = new URL(ROOT_URL);

    String expectedStr = ROOT_URL+"lockss/lockss-volume2.htm";
    DefinableArchivalUnit bbAu = makeAu(url, 2);
    assertEquals(expectedStr,
                 bbAu.getProperties().getString(ArchivalUnit.AU_START_URL, null));
  }

  public void testGetUrlStems() throws Exception {
    String stem1 = "http://www.blackbird.vcu.edu";
    DefinableArchivalUnit bbAu1 = makeAu(new URL(stem1 + "/"), 2);
    assertEquals(ListUtil.list(stem1), bbAu1.getUrlStems());
    String stem2 = "http://www.blackbird.vcu.edu:8080";
    DefinableArchivalUnit bbAu2 = makeAu(new URL(stem2 + "/"), 2);
    assertEquals(ListUtil.list(stem2), bbAu2.getUrlStems());
  }

  public void testShouldDoNewContentCrawlTooEarly() throws Exception {
    ArchivalUnit bbAu = makeAu(new URL(ROOT_URL), 2);
    AuState aus = new MockAuState(null, TimeBase.nowMs(), -1, -1, null);
    assertFalse(bbAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlForZero() throws Exception {
    ArchivalUnit bbAu = makeAu(new URL(ROOT_URL), 2);
    AuState aus = new MockAuState(null, 0, -1, -1, null);
    assertTrue(bbAu.shouldCrawlForNewContent(aus));
  }

  public void testShouldDoNewContentCrawlEachMonth() throws Exception {
    ArchivalUnit bbAu = makeAu(new URL(ROOT_URL), 2);
    AuState aus = new MockAuState(null, 4 * Constants.WEEK, -1, -1, null);
    assertTrue(bbAu.shouldCrawlForNewContent(aus));
  }

  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), 2);
    assertEquals("www.blackbird.vcu.edu, vol. 2", au.getName());
    DefinableArchivalUnit au1 = makeAu(new URL("http://www.bmj.com/"), 3);
    assertEquals("www.bmj.com, vol. 3", au1.getName());
  }

  public void testGetFilterRules() throws Exception {
    DefinableArchivalUnit au = makeAu(new URL(ROOT_URL), 2);
    assertNull(au.getFilterRule(null));
    assertNull(au.getFilterRule("jpg"));
    assertNull(au.getFilterRule("text/html"));
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestBlackbirdArchivalUnit.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
