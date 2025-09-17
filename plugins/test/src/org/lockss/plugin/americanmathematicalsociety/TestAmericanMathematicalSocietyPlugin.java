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

package org.lockss.plugin.americanmathematicalsociety;

import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.wrapper.WrapperUtil;
import org.lockss.util.PatternFloatMap;
import org.lockss.util.RegexpUtil;

public class TestAmericanMathematicalSocietyPlugin extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  
  public TestAmericanMathematicalSocietyPlugin(String msg) {
    super(msg);
  }
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(theDaemon,
        "org.lockss.plugin.americanmathematicalsociety." +
        "ClockssAmericanMathematicalSocietyPlugin");
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
    props.setProperty(YEAR_KEY, "2004");
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
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ID_KEY, "j_id");
    props.setProperty(YEAR_KEY, "2004");
    
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("American Mathematical Society Journals Plugin (CLOCKSS), " +
        "Base URL http://www.example.com/, " +
        "Journal ID j_id, Year 2004", au.getName());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.americanmathematicalsociety." +
        "ClockssAmericanMathematicalSocietyPlugin",
        plugin.getPluginId());
  }
  
  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.BASE_URL,
        ConfigParamDescr.JOURNAL_ID,
        ConfigParamDescr.YEAR),
        plugin.getLocalAuConfigDescrs());
  }
  
  public void testGetArticleMetadataExtractor() {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ID_KEY, "asdf");
    props.setProperty(YEAR_KEY, "2004");
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
        instanceof org.lockss.plugin.americanmathematicalsociety.
        AmericanMathematicalSocietyArticleIteratorFactory);
  }
  
  // Test the crawl rules for AmericanMathematicalSocietyPlugin
  public void testShouldCacheProperPages() throws Exception {
    String ROOT_URL = "http://www.example.com/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(JOURNAL_ID_KEY, "asdf");
    props.setProperty(YEAR_KEY, "2004");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // Test for pages that should get crawled
    // permission page/start url
    shouldCacheTest(ROOT_URL + "clockssdata/?p=asdf&y=2004", true, au);
    shouldCacheTest(ROOT_URL + "lockssdata/?p=asdf&y=2004", false, au);
    // toc page for an issue
    shouldCacheTest(ROOT_URL + "journals/asdf/2004-82-281/", true, au);
    shouldCacheTest(ROOT_URL + "journals/asdf/2004-82-281", false, au);
    shouldCacheTest(ROOT_URL + "asdf/home-2004.html", true, au);
    // article files
    
    // should not get crawled - wrong journal/year
    shouldCacheTest(ROOT_URL + "clockssdata/?p=ecgd&y=2004", false, au);
    shouldCacheTest(ROOT_URL + "clockssdata/?p=asdf&y=2014", false, au);
    shouldCacheTest(ROOT_URL + "ejournals/html/s-0029-1214947", false, au);  
    shouldCacheTest(ROOT_URL + "journals/asdf/2014-82-281/", false, au);
    shouldCacheTest(ROOT_URL + "ecgd/home-2004.html", false, au);
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    //log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }

  public void testPollandRepair() throws Exception {

    String[] pollAndRepair = {
            ".+[.](bmp|css|dfont|eot|ico|jpe?g|js|otf|png|svg|tif?f|ttc|ttf|woff.?)(\\?.*)?$",
            "^https?://[^/]+/(publications|images)/.*\\.gif$"
    };

    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.org");
    props.setProperty(JOURNAL_ID_KEY, "journal_od");
    props.setProperty(YEAR_KEY, "2003");

    Configuration config = ConfigurationUtil.fromProps(props);
    DefinableArchivalUnit au = (DefinableArchivalUnit)plugin.createAu(config);

    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }

    theDaemon.getLockssRepository(au);

    assertEquals(Arrays.asList(pollAndRepair),
            RegexpUtil.regexpCollection(au.makeRepairFromPeerIfMissingUrlPatterns()));

    List <String> repaireList = ListUtil.list(
            "http://www.ams.org/css/2018_slidenav.css",
            "http://www.ams.org/fontawesome/css/all.css",
            "http://www.ams.org/journals/javascript/bull_nav.js",
            "http://www.ams.org/publications/journals/images/bull-mh-1904.gif",
            "https://ajax.googleapis.com/ajax/libs/jquery/2.1.4/jquery.min.js",
            "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js",
            "https://fonts.gstatic.com/s/opensans/v15/mem5YaGs126MiZpBA-UN7rgOXOhs.ttf",
            "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css",
            "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/fonts/glyphicons-halflings-regular.eot",
            "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/fonts/glyphicons-halflings-regular.svg",
            "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/fonts/glyphicons-halflings-regular.ttf",
            "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/fonts/glyphicons-halflings-regular.woff",
            "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/fonts/glyphicons-halflings-regular.woff2",
            "http://www.ams.org/images/content/footer-line.png",
            "http://www.ams.org/publications/journals/images/bull-mh-2006.gif",
            "http://www.ams.org/publications/journals/images/journal.cover.bull.gif",
            "http://www.ams.org/images/rssBullet.gif"

    );

    Pattern p0 = Pattern.compile(pollAndRepair[0]);
    Pattern p1 = Pattern.compile(pollAndRepair[1]);

    Matcher m0, m1;

    for (String urlString : repaireList) {
      m0 = p0.matcher(urlString);
      m1 = p1.matcher(urlString);

      assertEquals(urlString, true, m0.find() || m1.find());
    }

    // Failed case
    List<String>  wrongStringList = ListUtil.list(
            "http://www.ams.org/fontawesome/css/all.less",
            "http://www.ams.org/journals/bull/2018-55-04/S0273-0979-2018-01639-X/images/img5.gif");

    for (String urlString : wrongStringList) {
      m0 = p0.matcher(urlString);
      m1 = p1.matcher(urlString);

      assertEquals(urlString, false, m0.find() &&  m1.find());
    }


    PatternFloatMap urlPollResults = au.makeUrlPollResultWeightMap();

    assertNotNull(urlPollResults);
  }
}
