/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.royalsocietyofchemistry;

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

public class TestRSC2014Plugin extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String RESOLVER_URL_KEY = "resolver_url";
  static final String GRAPHICS_URL_KEY = "graphics_url";
  static final String BASE_URL2_KEY = ConfigParamDescr.BASE_URL2.getKey();
  static final String JOURNAL_CODE_KEY = "journal_code";
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  
  static final String GRAPHICS_URL = "http://sod-a.img-cdn.com/";
  // from au_url_poll_result_weight in plugins/src/org/lockss/plugin/royalsocietyofchemistry/RSC2014Plugin.xml
  // <string>"%spubs-core/", graphics_url</string>
  // Note diff: the funky escaped chars, there is probably a call to do the conversion
  // if it changes in the plugin, you will likely need to change the test, so verify
  static final String REPAIR_FROM_PEER_REGEXP = "http\\:\\/\\/sod\\-a\\.img\\-cdn\\.com\\/" + "pubs-core/";
  
  
  private DefinablePlugin plugin;
  
  public TestRSC2014Plugin(String msg) {
    super(msg);
  }
  
  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.royalsocietyofchemistry.ClockssRSC2014Plugin");
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
    props.setProperty(BASE_URL_KEY, "http://pubs.example.com/");
    props.setProperty(RESOLVER_URL_KEY, "http://xlink.example.com/");
    props.setProperty(GRAPHICS_URL_KEY, GRAPHICS_URL);
    props.setProperty(BASE_URL2_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_CODE_KEY, "an");
    props.setProperty(VOLUME_NAME_KEY, "123");
    props.setProperty(YEAR_KEY, "2013");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
      au = null;
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
    props.setProperty(RESOLVER_URL_KEY, "blah3");
    props.setProperty(GRAPHICS_URL_KEY, "blah4");
    props.setProperty(BASE_URL2_KEY, "blah2");
    props.setProperty(JOURNAL_CODE_KEY, "an");
    props.setProperty(VOLUME_NAME_KEY, "9");
    props.setProperty(YEAR_KEY, "2001");
    
    try {
      DefinableArchivalUnit au = makeAuFromProps(props);
      fail ("Didn't throw InstantiationException when given a bad url");
    } catch (ArchivalUnit.ConfigurationException auie) {
      assertNotNull(auie.getCause());
    }
  }
  
  public void testGetAuConstructsProperAu()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://pubs.example.com/");
    props.setProperty(RESOLVER_URL_KEY, "http://xlink.example.com/");
    props.setProperty(GRAPHICS_URL_KEY, GRAPHICS_URL);
    props.setProperty(BASE_URL2_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_CODE_KEY, "an");
    props.setProperty(VOLUME_NAME_KEY, "123");
    props.setProperty(YEAR_KEY, "2013");
    
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("Royal Society of Chemistry 2014 Plugin (CLOCKSS), " +
        "Base URL http://pubs.example.com/, " +
        "Base URL2 http://www.example.com/, " +
        "Resolver URL http://xlink.example.com/, " +
        "Journal Code an, Volume 123, Year 2013", au.getName());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.royalsocietyofchemistry." +
        "ClockssRSC2014Plugin",
        plugin.getPluginId());
  }
  
  public void testGetArticleMetadataExtractor() {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://pubs.example.com/");
    props.setProperty(RESOLVER_URL_KEY, "http://xlink.example.com/");
    props.setProperty(GRAPHICS_URL_KEY, GRAPHICS_URL);
    props.setProperty(BASE_URL2_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_CODE_KEY, "an");
    props.setProperty(VOLUME_NAME_KEY, "123");
    props.setProperty(YEAR_KEY, "2013");
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
    assertNotNull(plugin.getHashFilterFactory("application/pdf"));
    assertNotNull(plugin.getHashFilterFactory("text/html"));
  }
  public void testGetArticleIteratorFactory() {
    assertTrue(WrapperUtil.unwrap(plugin.getArticleIteratorFactory())
        instanceof org.lockss.plugin.royalsocietyofchemistry.RSC2014ArticleIteratorFactory);
  }
  
  
  public void testPollSpecial() throws Exception {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://pubs.example.com/");
    props.setProperty(RESOLVER_URL_KEY, "http://xlink.example.com/");
    props.setProperty(GRAPHICS_URL_KEY, GRAPHICS_URL);
    props.setProperty(BASE_URL2_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_CODE_KEY, "an");
    props.setProperty(VOLUME_NAME_KEY, "123");
    props.setProperty(YEAR_KEY, "2013");
    
    DefinableArchivalUnit au = makeAuFromProps(props);
    
    // if it changes in the plugin, you will likely need to change the test, so verify
    assertEquals(ListUtil.list(
        REPAIR_FROM_PEER_REGEXP),
        RegexpUtil.regexpCollection(au.makeRepairFromPeerIfMissingUrlPatterns()));
    
    // make sure that's the regexp that will match to the expected url string
    // this also tests the regexp (which is the same) for the weighted poll map
    
    List <String> repairList = ListUtil.list(
        GRAPHICS_URL + "pubs-core/2017.0.57/content/NewImages/CCBY-NC.png",
        GRAPHICS_URL + "pubs-core/2016/js/9.3.2/lib/functional.min.js",
        GRAPHICS_URL + "pubs-core/2017.0.57/content/stylesheets/mobileStyle.css");
    Pattern p = Pattern.compile(REPAIR_FROM_PEER_REGEXP);
    for (String urlString : repairList) {
      Matcher m = p.matcher(urlString);
      assertEquals(urlString, true, m.find());
    }
    //and this one should fail - it needs to be weighted correctly and repaired from publisher if possible
    String notString = "http://pubs.example.com/img/close-icon.png";
    Matcher m = p.matcher(notString);
    assertEquals(false, m.find());
    
    PatternFloatMap urlPollResults = au.makeUrlPollResultWeightMap();
    assertNotNull(urlPollResults);
    for (String urlString : repairList) {
      assertEquals(0.0, urlPollResults.getMatch(urlString, (float) 1), .0001);
    }
    assertEquals(1.0, urlPollResults.getMatch(notString, (float) 1), .0001);
  }
  
}
