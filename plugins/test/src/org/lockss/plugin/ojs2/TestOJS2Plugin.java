/*
 * $Id: $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.test.*;
import org.lockss.util.ListUtil;
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
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ID_KEY, "j_id");
    props.setProperty(YEAR_KEY, "2014");
    
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("Open Journal Systems Plugin (OJS 2.x for CLOCKSS), " +
        "Base URL http://www.example.com/, " +
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
    
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
}
