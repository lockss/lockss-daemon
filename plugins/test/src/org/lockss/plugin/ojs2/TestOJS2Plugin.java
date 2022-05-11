/*
 * $Id: $
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

package org.lockss.plugin.ojs2;

import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.util.PatternFloatMap;
import org.lockss.util.RegexpUtil;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.wrapper.WrapperUtil;

public class TestOJS2Plugin extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  
  // from au_url_poll_result_weight in plugins/src/org/lockss/plugin/ojs2/OJS2Plugin.xml
  // Note diff: java regex & vs. xml &amp;
  // if it changes in the plugin, you will likely need to change the test, so verify
  static final String  OJS2_REPAIR_FROM_PEER_REGEXP1 = 
	      "/(libs?|site|images|js|public|ads)/.+[.](css|eot|gif|png|jpe?g|js|svg|ttf|woff)([?]((itok|v)=)?[^&]+)?$";
  static final String  OJS2_REPAIR_FROM_PEER_REGEXP2 = 
	      "/page/css([?]name=.*)?$";
  
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  
  public TestOJS2Plugin(String msg) {
    super(msg);
  }
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(theDaemon,
        "org.lockss.plugin.ojs2.ClockssOJS2Plugin");
  }
  
  public void testGetAuNullConfig()
      throws ArchivalUnit.ConfigurationException {
    try {
      plugin.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  public void testCreateAu() {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ID_KEY, "j_id");
    props.setProperty(YEAR_KEY, "2014");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    au.getName();
  }
  
  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit)plugin.configureAu(config, null);
  }
  
  public void testGetAuHandlesBadUrl()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "blah");
    props.setProperty(JOURNAL_ID_KEY, "jams");
    props.setProperty(YEAR_KEY, "2001");
    
    try {
      makeAuFromProps(props);
      fail ("Didn't throw InstantiationException when given a bad url");
    } catch (ArchivalUnit.ConfigurationException auie) {
      assertNotNull(auie.getCause());
    }
  }
  
  public void testGetAuConstructsProperAu()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/ojs2/");
    props.setProperty(JOURNAL_ID_KEY, "j_id");
    props.setProperty(YEAR_KEY, "2014");
    
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("Open Journal Systems Plugin (OJS 2.x for CLOCKSS), " +
        "Base URL http://www.example.com/ojs2/, " +
        "Journal ID j_id, Year 2014", au.getName());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.ojs2." +
        "ClockssOJS2Plugin",
        plugin.getPluginId());
  }
  
  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.YEAR,
        ConfigParamDescr.JOURNAL_ID,
        ConfigParamDescr.BASE_URL),
        plugin.getLocalAuConfigDescrs());
  }
  
  public void testGetArticleMetadataExtractor() {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ID_KEY, "asdf");
    props.setProperty(YEAR_KEY, "2014");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    assertTrue(""+plugin.getArticleMetadataExtractor(MetadataTarget.Any(), au),
        plugin.getArticleMetadataExtractor(null, au) instanceof ArticleMetadataExtractor);
    assertTrue(""+plugin.getFileMetadataExtractor(MetadataTarget.Any(), "text/html", au),
        plugin.getFileMetadataExtractor(MetadataTarget.Any(), "text/html", au) instanceof
        FileMetadataExtractor
        );
  }
  
  public void testGetHashFilterFactory() {
    assertNull(plugin.getHashFilterFactory("BogusFilterFactory"));
    assertNull(plugin.getHashFilterFactory("application/pdf"));
    assertNotNull(plugin.getHashFilterFactory("text/html"));
  }
  public void testGetArticleIteratorFactory() {
    assertTrue(WrapperUtil.unwrap(plugin.getArticleIteratorFactory())
        instanceof org.lockss.plugin.ojs2.
        OJS2ArticleIteratorFactory);
  }
  
  // Test the crawl rules for OJS2Plugin
  public void testShouldCacheProperPages() throws Exception {
    String ROOT_URL = "http://www.example.com/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(JOURNAL_ID_KEY, "j_id");
    props.setProperty(YEAR_KEY, "2014");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // Test for pages that should get crawled
    // permission page/start url
    shouldCacheTest(ROOT_URL + "j_id/gateway/lockss?year=2014", true, au);
    shouldCacheTest(ROOT_URL + "j_id/gateway/clockss?year=2014", false, au);
    // toc page for an issue
    shouldCacheTest(ROOT_URL + "index.php/j_id/issue/view/123", true, au);
    shouldCacheTest(ROOT_URL + "index.php/issue/view/123", true, au);
    shouldCacheTest(ROOT_URL + "j_id/issue/view/123", true, au);
    // article files
    shouldCacheTest(ROOT_URL + "index.php/j_id/article/view/123", true, au);
    shouldCacheTest(ROOT_URL + "index.php/j_id/article/view/123/456", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/123", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/123/456", true, au);
    shouldCacheTest(ROOT_URL + "index.php/j_id/article/download/123/456", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/download/123/456", true, au);
    shouldCacheTest(ROOT_URL + "index.php/article/download/123/456", true, au);
    shouldCacheTest(ROOT_URL + "index.php/j_id/article/download/123", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/123/456", true, au);
    
    // should not get crawled - wrong journal/year
    shouldCacheTest(ROOT_URL + "j_id/gateway/lockss?year=2004", false, au);
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
    
    shouldCacheTest(ROOT_URL + "index.php/j_id/article/viewFile/123/456/%20http://foo.edu", false, au);
    
    shouldCacheTest(ROOT_URL + "j_id/article/view/23854/Background_files/filelist.xml", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/23854/Background_files/Background_files/filelist.xml", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/23854/Background_files/Background_files/Background_files/filelist.xml", false, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/23854/Background_files/Background_files/Background_files/Background_files/filelist.xml", false, au);
    
    shouldCacheTest(ROOT_URL + "j_id/article/download/23854/3563_files/3563_files", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/download/23854/3563_files/3563_files/3563_files", false, au);
    shouldCacheTest(ROOT_URL + "j_id/article/download/23854/3563_files/3563_files/3563_files/3563_files", false, au);
    shouldCacheTest(ROOT_URL + "j_id/article/download/23854/3563_files/3563_files/3563_files/3563_files/3563_files/foo", false, au);
    
    shouldCacheTest(ROOT_URL + "j_id/article/download/23854/3563_files/3563_files/1.gif", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/download/23854/3563_files/3563_files/3563_files/1.gif", false, au);
    
    shouldCacheTest(ROOT_URL + "plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=" +
        ROOT_URL + "index.php/j_id/article/view/123/456", true, au);
    shouldCacheTest(ROOT_URL + "plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=" +
        ROOT_URL + "j_id/article/view/123/456", true, au);
    shouldCacheTest(ROOT_URL + "plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=" +
        URLEncoder.encode(ROOT_URL + "index.php/j_id/article/view/123/456", "UTF-8"), true, au);
    shouldCacheTest(ROOT_URL + "plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=" +
        URLEncoder.encode(ROOT_URL + "j_id/article/view/123/456", "UTF-8"), true, au);
    shouldCacheTest(ROOT_URL + "plugins/generic/pdfJsViewer/pdf.js/web/locale/locale.properties", true, au);
    shouldCacheTest(ROOT_URL + "plugins/generic/pdfJsViewer/pdf.js/build/pdf.js", true, au);
    shouldCacheTest(ROOT_URL + "plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=" +
        URLEncoder.encode(URLEncoder.encode(ROOT_URL + "index.php/j_id/article/view/123/456", "UTF-8"), "UTF-8"), false, au);
    shouldCacheTest(ROOT_URL + "plugins/generic/pdfJsViewer/pdf.js/web/viewer.html?file=" +
        URLEncoder.encode(URLEncoder.encode(ROOT_URL + "j_id/article/view/123/456", "UTF-8"), "UTF-8"), false, au);
    
    shouldCacheTest(ROOT_URL + "modules/user/user.css?nzdhiu", true, au);
    shouldCacheTest(ROOT_URL + "modules/user/user.css?nzdhiu&id=1", false, au);
    shouldCacheTest(ROOT_URL + "sites/all/modules/contrib/views/css/views.css?nzdhiu", true, au);
    shouldCacheTest(ROOT_URL + "misc/jquery.js?v=1.4.4", true, au);
    shouldCacheTest(ROOT_URL + "misc/jquery.js?v=1.4.4&id=1", false, au);
    shouldCacheTest(ROOT_URL + "sites/files/styles/journals/cover%20%282%29_0.png?itok=qGTU4GfX", true, au);
    shouldCacheTest(ROOT_URL + "sites/files/styles/journals/cover%20%282%29_0.png?itok=qGTU4GfX&v=1.1", false, au);
    shouldCacheTest(ROOT_URL + "sites/themes/js/j_id.js?nzdhiu", true, au);
    shouldCacheTest(ROOT_URL + "sites/themes/js/j_id.js?nzdhiu&v=1.2", false, au);
    
    shouldCacheTest(ROOT_URL + "j_id/rt/printerFriendly/123/456", true, au);
    shouldCacheTest(ROOT_URL + "j_id/rt/findingReferences/123/456", false, au);
  }
  
  // Same tests with path on base_url
  public void testShouldCacheProperPagesOJS2() throws Exception {
    String ROOT_URL = "http://www.example.com/ojs2/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(JOURNAL_ID_KEY, "j_id");
    props.setProperty(YEAR_KEY, "2016");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // Test for pages that should get crawled
    // permission page/start url
    shouldCacheTest(ROOT_URL + "j_id/gateway/lockss?year=2016", true, au);
    shouldCacheTest(ROOT_URL + "j_id/gateway/clockss?year=2016", false, au);
    // toc page for an issue
    shouldCacheTest(ROOT_URL + "index.php/j_id/issue/view/123", true, au);
    shouldCacheTest(ROOT_URL + "index.php/issue/view/123", true, au);
    shouldCacheTest(ROOT_URL + "j_id/issue/view/123", true, au);
    // article files
    shouldCacheTest(ROOT_URL + "index.php/j_id/article/view/123", true, au);
    shouldCacheTest(ROOT_URL + "index.php/j_id/article/view/123/456", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/123", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/123/456", true, au);
    shouldCacheTest(ROOT_URL + "index.php/j_id/article/download/123/456", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/download/123/456", true, au);
    shouldCacheTest(ROOT_URL + "index.php/article/download/123/456", true, au);
    shouldCacheTest(ROOT_URL + "index.php/j_id/article/download/123", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/123/456", true, au);
    
    // should not get crawled - wrong journal/year
    shouldCacheTest(ROOT_URL + "j_id/gateway/lockss?year=2004", false, au);
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
    
    shouldCacheTest(ROOT_URL + "index.php/j_id/article/viewFile/123/456/%20http://foo.edu", false, au);
    
    shouldCacheTest(ROOT_URL + "j_id/article/view/23854/Background_files/filelist.xml", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/23854/Background_files/Background_files/filelist.xml", true, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/23854/Background_files/Background_files/Background_files/filelist.xml", false, au);
    shouldCacheTest(ROOT_URL + "j_id/article/view/23854/Background_files/Background_files/Background_files/Background_files/filelist.xml", false, au);
    
    shouldCacheTest(ROOT_URL + "modules/user/user.css?nzdhiu", true, au);
    shouldCacheTest(ROOT_URL + "modules/user/user.css?nzdhiu&id=1", false, au);
    shouldCacheTest(ROOT_URL + "sites/all/modules/contrib/views/css/views.css?nzdhiu", true, au);
    shouldCacheTest(ROOT_URL + "misc/jquery.js?v=1.4.4", true, au);
    shouldCacheTest(ROOT_URL + "misc/jquery.js?v=1.4.4&id=1", false, au);
    shouldCacheTest(ROOT_URL + "sites/files/styles/journals/cover%20%282%29_0.png?itok=qGTU4GfX", true, au);
    shouldCacheTest(ROOT_URL + "sites/files/styles/journals/cover%20%282%29_0.png?itok=qGTU4GfX&v=1.1", false, au);
    shouldCacheTest(ROOT_URL + "sites/themes/js/j_id.js?nzdhiu", true, au);
    shouldCacheTest(ROOT_URL + "sites/themes/js/j_id.js?nzdhiu&v=1.2", false, au);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testPollSpecial() throws Exception {
    String ROOT_URL = "http://www.example.com/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(JOURNAL_ID_KEY, "asdf");
    props.setProperty(YEAR_KEY, "2014");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // if it changes in the plugin, you will likely need to change the test, so verify
    assertEquals(ListUtil.list(
        OJS2_REPAIR_FROM_PEER_REGEXP1,OJS2_REPAIR_FROM_PEER_REGEXP2),
        RegexpUtil.regexpCollection(au.makeRepairFromPeerIfMissingUrlPatterns()));
    
    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    // Add to pattern these urls? Has not been seen as problem, yet
    //  "http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML",
    //  "https://d1bxh8uas1mnw7.cloudfront.net/assets/embed.js",
    //  "https://ajax.googleapis.com/ajax/libs/jquery/1.4.4/jquery.min.js",
    //  ROOT_URL + "global/styles/common.css",
    //  ROOT_URL + "styles/common.css",
    
    List <String> repairList = ListUtil.list(
        ROOT_URL + "public/journals/1/cover_issue_50_en_US.jpg",
        ROOT_URL + "global/images/button_facebook.png",
        ROOT_URL + "global/images/icon_mendeley_16x16b.gif",
        ROOT_URL + "global/js/opentip-jquery-excanvas.js",
        ROOT_URL + "lib/pkp/js/functions/jqueryValidatorI18n.js",
        ROOT_URL + "lib/pkp/styles/lib/jqueryUi/images/ui-bg_glass_95_fef1ec_1x400.png",
        ROOT_URL + "templates/images/progbg.gif",
        ROOT_URL + "submission/public/journals/2/cover_issue_3_en_US.jpg",
        ROOT_URL + "public/site/crosscheck_it_trans1.gif");
    Pattern p1 = Pattern.compile(OJS2_REPAIR_FROM_PEER_REGEXP1);
    Pattern p2 = Pattern.compile(OJS2_REPAIR_FROM_PEER_REGEXP2);
     for (String urlString : repairList) {
         Matcher m1 = p1.matcher(urlString);
         Matcher m2 = p1.matcher(urlString);
       assertEquals(urlString, true, (m1.find() || m1.find()));
     }
     //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
     String notString = ROOT_URL + "index.php/aa/about/editorialPolicies";
     Matcher m1 = p1.matcher(notString);
     assertEquals(false, m1.find());
     
    PatternFloatMap urlPollResults = au.makeUrlPollResultWeightMap();
    assertNotNull(urlPollResults);
    for (String urlString : repairList) {
      assertEquals(0.0, urlPollResults.getMatch(urlString, (float) 1), .0001);
    }
    assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
  }
  
}
