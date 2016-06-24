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

package org.lockss.plugin.peerj;

import java.net.URL;
import java.util.List;
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
import org.lockss.test.MockLockssUrlConnection;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.PatternFloatMap;
import org.lockss.util.RegexpUtil;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.HttpResultMap;
import org.lockss.util.urlconn.CacheException.RetrySameUrlException;

/* 
 * Tests archival unit for PeerJ Archival site
 */
public class TestPeerJ2016ArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "https://peerj.com/";
  
  // from au_url_poll_result_weight in plugins/src/org/lockss/plugin/peerj/PeerJ2016Plugin.xml
  // if it changes in the plugin, you will likely need to change the test, so verify
  static final String  PEERJ_REPAIR_FROM_PEER_REGEXP = 
      "/((css|js)/[^.]+[.](css|js)|images/.+[.](jpg|gif|png))$";

  private static final Logger log = Logger.getLogger(TestPeerJ2016ArchivalUnit.class);

  static final String PLUGIN_ID = "org.lockss.plugin.peerj.ClockssPeerJ2016Plugin";
  static final String PluginName = "PeerJ Plugin (CLOCKSS)";
  
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

  private DefinableArchivalUnit makeAu(URL url, String volume, String jid)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(VOL_KEY, volume);
    props.setProperty(JID_KEY, jid);
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);

    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)plugin.createAu(config);
    return au;
  }
  
  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "2014", "cs");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  public void testHandlesHttpResultCodes() throws Exception {
    URL base = new URL("http://www.example.com/");
    DefinableArchivalUnit au = makeAu(base, "2014", "cs");
    MockLockssUrlConnection conn = new MockLockssUrlConnection();
    String starturl =
        "http://www.example.com/archives/?year=2014&journal=cs";
    
    conn.setURL("http://www.example.com/articles/cs-18.pdf");
    CacheException exc =
        ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
            403, "foo");
    assertClass(CacheException.PermissionException.class, exc);
    
    conn.setURL(starturl);
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
        403, "foo");
    assertClass(CacheException.PermissionException.class, exc);
    
    conn.setURL("https://dfzljdn9uc3pi.cloudfront.net/2014/228/1/fig-1-2x.jpg");
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
        403, "foo");
    assertClass(CacheException.NoRetryDeadLinkException.class, exc);
    
    conn.setURL(starturl);
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
        429, "foo");
    assertClass(PeerJHttpResponseHandler.RetryableNetworkException_2_10M.class, exc);
    
    conn.setURL("http://uuu17/");
    exc =
        ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
            500, "foo");
    assertClass(CacheException.RetryDeadLinkException.class, exc);
    
    conn.setURL(starturl);
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
        500, "foo");
    assertClass(RetrySameUrlException.class, exc);
    
    
  }
  
  // Test the crawl rules for PeerJ Archives (main) site; 
  // hardwire the journal = peerj
  // the volume = 2014
  public void testShouldCacheProperPages()
      throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit PJAu = makeAu(base, "2014", "peerj");
    theDaemon.getLockssRepository(PJAu);
    theDaemon.getNodeManager(PJAu);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(PJAu,
        new RangeCachedUrlSetSpec(base.toString()));
    // permission page
    shouldCacheTest("https://peerj.com/lockss.txt", true, PJAu, cus);
    //took this out of crawl rules because led to 403 on some button.png images...and seem unnecessary
    //shouldCacheTest("https://s3.amazonaws.com/static.peerj.com/images/lPJAunch/MontereyJellyPeerJ.jpg", true, PJAu, cus);
    shouldCacheTest("https://d2pdyyx74uypu5.cloudfront.net/images/apple-touch-icon.png", true, PJAu, cus);
    shouldCacheTest("https://peerj.com/assets/icomoon/fonts/icomoon.eot?-90lpjv", true, PJAu, cus);
    shouldCacheTest("https://peerj.com/assets/font-awesome-3.2.1/font/fontawesome-webfont.ttf?v=3.2.1", true, PJAu, cus);
    shouldCacheTest("https://dfzljdn9uc3pi.cloudfront.net/2014/250/1/fig-1-2x.jpg", true, PJAu, cus);
    shouldCacheTest("https://peerj.com/archives/?year=2014&journal=peerj", true, PJAu, cus);
    shouldCacheTest("https://peerj.com/articles/index.html?month=2014-01&journal=peerj", true, PJAu, cus);
    shouldCacheTest("https://peerj.com/articles/250", true, PJAu, cus);
        shouldCacheTest("https://peerj.com/articles/250.pdf", true, PJAu, cus);
    shouldCacheTest("https://peerj.com/articles/250.bib", true, PJAu, cus);
    shouldCacheTest("https://peerj.com/articles/250.ris", true, PJAu, cus);
    shouldCacheTest("https://peerj.com/articles/250.xml", true, PJAu, cus);
    // DO NOT CACHE
    shouldCacheTest("https://peerj.com/articles/250/reviews/", false, PJAu, cus);
    shouldCacheTest("https://peerj.com/articles/250.html", false, PJAu, cus);
    shouldCacheTest("https://peerj.com/articles/250.amp", false, PJAu, cus);
    shouldCacheTest("https://peerj.com/articles/250.citeproc", false, PJAu, cus);
    shouldCacheTest("https://peerj.com/articles/250.json", false, PJAu, cus);
    shouldCacheTest("https://peerj.com/articles/250.rdf", false, PJAu, cus);
    shouldCacheTest("https://peerj.com/articles/250.unixref", false, PJAu, cus);
    // images figures and tables can live here
    // https://dfzljdn9uc3pi.cloudfront.net/2013/55/1/fig-1-2x.jpg
    // https://dfzljdn9uc3pi.cloudfront.net/2013/55/1/fig-1-full.png
    // https://d3amtssd1tejdt.cloudfront.net/2013/21/1/figure1.png
    // https://d2pdyyx74uypu5.cloudfront.net/2013/22/2/figure2.jpg
    shouldCacheTest("https://dfzljdn9uc3pi.cloudfront.net"
    		    + "/2013/55/1/fig-1-2x.jpg", true, PJAu, cus);   
    shouldCacheTest("https://dfzljdn9uc3pi.cloudfront.net"
                    + "/2013/55/1/fig-1-full.png", true, PJAu, cus);   
    shouldCacheTest("https://d3amtssd1tejdt.cloudfront.net"
                    + "/2013/21/1/figure1.png", true, PJAu, cus);
    shouldCacheTest("https://d2pdyyx74uypu5.cloudfront.net"
                   + "/2013/22/2/figure2.jpg", true, PJAu, cus);
    shouldCacheTest("https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js",
          true, PJAu, cus);
    
    // this is under cloudfront, but not directly under journal...we don't want it.
    shouldCacheTest("https://d2pdyyx74uypu5.cloudfront.net/pressReleases/2014/PressReleasePeerJ_Stowell.pdf",false, PJAu, cus);

    // missing something or wrong journal
    shouldCacheTest("http://peerj/?year=2012&journal=peerj", false, PJAu, cus);  
    shouldCacheTest("http://peerj/archives/?year=2012", false, PJAu, cus);  
    shouldCacheTest("http://peerj/?year=2012&journal=cs", false, PJAu, cus);  
    shouldCacheTest("http://peerj/?year=2012&journal=cs", false, PJAu, cus);  
    // LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, PJAu, cus);
    
    //change the AU to be the computer science one and check a few more
    ArchivalUnit CSAu = makeAu(base, "2016", "cs");
    theDaemon.getLockssRepository(CSAu);
    theDaemon.getNodeManager(CSAu);
    BaseCachedUrlSet cscus = new BaseCachedUrlSet(CSAu,
        new RangeCachedUrlSetSpec(base.toString()));
    shouldCacheTest("https://peerj.com/lockss.txt", true, CSAu, cscus);  
    shouldCacheTest("https://peerj.com/assets/font-awesome-3.2.1/font/fontawesome-webfont.ttf?v=3.2.1", true, CSAu, cscus);
    shouldCacheTest("https://peerj.com/archives/?year=2016&journal=cs", true, CSAu, cscus);
    shouldCacheTest("https://peerj.com/articles/index.html?month=2016-01&journal=cs", true, CSAu, cscus);
    shouldCacheTest("https://peerj.com/articles/cs-250", true, CSAu, cscus);
        shouldCacheTest("https://peerj.com/articles/cs-250.pdf", true, CSAu, cscus);
    shouldCacheTest("https://peerj.com/articles/cs-250.bib", true, CSAu, cscus);
    shouldCacheTest("https://peerj.com/articles/cs-250.ris", true, CSAu, cscus);
    shouldCacheTest("https://peerj.com/articles/cs-250.xml", true, CSAu, cscus);
    // DO NOT CACHE
    shouldCacheTest("https://peerj.com/articles/cs-250/reviews/", false, CSAu, cscus);
    shouldCacheTest("https://peerj.com/articles/cs-250.html", false, CSAu, cscus);
    shouldCacheTest("https://peerj.com/articles/cs-250.amp", false, CSAu, cscus);
    shouldCacheTest("https://peerj.com/articles/cs-250.citeproc", false, CSAu, cscus);
    shouldCacheTest("https://peerj.com/articles/cs-250.json", false, CSAu, cscus);
    shouldCacheTest("https://peerj.com/articles/cs-250.rdf", false, CSAu, cscus);
    shouldCacheTest("https://peerj.com/articles/cs-250.unixref", false, CSAu, cscus);
    //it must be the correct journal
    shouldCacheTest("https://peerj.com/articles/peerj-250.pdf", false, CSAu, cscus);
    //alas no way to guard against this one...if the journal cross links to peerj
    shouldCacheTest("https://peerj.com/articles/250.pdf", true, CSAu, cscus);    
    
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    //log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testPollSpecial() throws Exception {
    String ROOT_URL = "http://www.example.com/";
    URL base = new URL(ROOT_URL);
    ArchivalUnit PJAu = makeAu(base, "2014", "peerj");
    theDaemon.getLockssRepository(PJAu);
    
    // if it changes in the plugin, you will likely need to change the test, so verify
    assertEquals(ListUtil.list(
        PEERJ_REPAIR_FROM_PEER_REGEXP),
        RegexpUtil.regexpCollection(PJAu.makeRepairFromPeerIfMissingUrlPatterns()));
    
    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    List <String> repairList = ListUtil.list(
        ROOT_URL + "css/ffc05d8-5868266.css",
        ROOT_URL + "js/2fdf27f-295bb34.js");
     Pattern p = Pattern.compile(PEERJ_REPAIR_FROM_PEER_REGEXP);
     for (String urlString : repairList) {
       Matcher m = p.matcher(urlString);
       assertEquals(urlString, true, m.find());
     }
     //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
     String notString = ROOT_URL + "assets/icomoon/fonts/icomoon.woff?-90lpjv";
     Matcher m = p.matcher(notString);
     assertEquals(false, m.find());
     
    PatternFloatMap urlPollResults = PJAu.makeUrlPollResultWeightMap();
    assertNotNull(urlPollResults);
    for (String urlString : repairList) {
      assertEquals(0.0, urlPollResults.getMatch(urlString, (float) 1), .0001);
    }
    assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
  }
  
}

