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

package org.lockss.plugin.ingenta;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.DefinableArchivalUnit;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.PatternFloatMap;
import org.lockss.util.RegexpUtil;

/* 
 * Tests archival unit for PeerJ Archival site
 */
public class TestIngentaJournalsArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String BASE_API_KEY = "api_url";
  static final String BASE_GRAPH_KEY = "graphics_url";
  static final String PUB_KEY = "publisher_id";
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  
  static final String DEFPUB = "foo";
  static final String DEFBASE = "http://www.ingentaconnect.com/";
  static final String DEFAPI = "http://api.ingentaconnect.com/"; 
  static final String DEFGRAPH = "http://graphics.ingentaconnect.com/"; 

  private static final Logger log = Logger.getLogger(TestIngentaJournalsArchivalUnit.class);

  static final String PLUGIN_ID = "org.lockss.plugin.ingenta.ClockssIngentaJournalPlugin";
  static final String PluginName = "Ingenta Journal Plugin (CLOCKSS)";
  
  private DefinablePlugin plugin;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /*
   * hardcode the three urls and the publisher id
   */
  private DefinableArchivalUnit makeAu(String volume, String jid, String issn)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, volume);
    props.setProperty(ISSN_KEY, issn);
    props.setProperty(JID_KEY, jid);
    props.setProperty(PUB_KEY, DEFPUB);
    props.setProperty(BASE_URL_KEY, DEFBASE);
    props.setProperty(BASE_API_KEY, DEFAPI);
    props.setProperty(BASE_GRAPH_KEY, DEFGRAPH);

    Configuration config = ConfigurationUtil.fromProps(props);

    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)plugin.createAu(config);
    return au;
  }
  
  
  // Test the crawl rules 
  // This is a placeholder as I add an archival unit junit
  public void testShouldCacheProperPages()
      throws Exception {
    ArchivalUnit FooAu = makeAu("23", "myjid", "1111-1111");
    theDaemon.getLockssRepository(FooAu);
    theDaemon.getNodeManager(FooAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(FooAu,
        new RangeCachedUrlSetSpec(DEFBASE));

    // permission page or start_url
    shouldCacheTest("http://docserver.ingentaconnect.com/lockss.txt", true, FooAu, cus);
    shouldCacheTest("http://graphics.ingentaconnect.com/lockss.txt", true, FooAu, cus);
    shouldCacheTest("http://www.ingentaconnect.com/content/1111-1111?format=clockss&volume=23", true, FooAu, cus);
    shouldCacheTest("http://www.ingentaconnect.com/content/foo/myjid/2011/00000023/00000001/art00002", true, FooAu, cus);
    shouldCacheTest("http://www.ingentaconnect.com/content/foo/myjid/2011/00000023/00000001/art00002?format=ris", true, FooAu, cus);
    shouldCacheTest("http://www.ingentaconnect.com/Less/ingenta.css?release=5.1.4", true, FooAu, cus);

    // DO NOT CACHE
    
    //ALLOWABLE
    //permitted_hosts_pattern: .*[.]cloudfront[.]net|cdn[.]mathjax[.]org
    shouldCacheTest("https://dfzljdn9uc3pi.cloudfront.net"
    		    + "/2013/55/1/fig-1-2x.jpg", true, FooAu, cus);   
    shouldCacheTest("https://cdn.mathjax.org/blahblahblahjs",
          true, FooAu, cus);
    
  }
  
  public void testPollSpecial() throws Exception {
    ArchivalUnit FooAu = makeAu("23", "myjid", "1111-1111");
    theDaemon.getLockssRepository(FooAu);
    theDaemon.getNodeManager(FooAu);


    String REPAIR_REGEXP="(/references|[?]format=bib|/images/[^.]+[.](gif|ico|png)|[.](css|js|eot|svg|ttf|woff.?)([?](v|release)=[^&]*)?)$";
    assertEquals(ListUtil.list(
        REPAIR_REGEXP),
        RegexpUtil.regexpCollection(FooAu.makeRepairFromPeerIfMissingUrlPatterns()));
    
    // make sure that's the regexp that will match to the expected url string
    //you could make a list of these and loop over them
     String urlString = "http://www.ingentaconnect.com/Less/ingenta.css?release=5.1.4";
     Pattern p = Pattern.compile(REPAIR_REGEXP);
     Matcher m = p.matcher(urlString);
     assertEquals(true, m.find());
     

    PatternFloatMap urlPollResults = FooAu.makeUrlPollResultWeightMap();
    assertNotNull(urlPollResults);
    assertEquals(0.0,
        urlPollResults.getMatch("http://www.ingentaconnect.com/css/connect/print.css?release=R5_1_4", (float) 1),
        .0001);
    assertEquals(1.0, 
        urlPollResults.getMatch("http://www.ingentaconnect.com/content/random", (float) 1), .0001);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    //log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
}

