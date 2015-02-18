/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.georgthiemeverlag;

import java.net.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.wrapper.*;

public class TestGeorgThiemeVerlagPlugin extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private MockLockssDaemon theDaemon;
  
  private DefinablePlugin plugin;
  
  public TestGeorgThiemeVerlagPlugin(String msg) {
    super(msg);
  }
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
                      "org.lockss.plugin.georgthiemeverlag.ClockssGeorgThiemeVerlagPlugin");
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
    props.setProperty(JOURNAL_ID_KEY, "10.1055/s-00000002");
    props.setProperty(VOLUME_NAME_KEY, "2010");
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
    props.setProperty(JOURNAL_ID_KEY, "10.foo/s-00000002");
    props.setProperty(VOLUME_NAME_KEY, "2010");
    
    try {
      DefinableArchivalUnit au = makeAuFromProps(props);
      fail ("Didn't throw InstantiationException when given a bad url");
      assertNull(au);
    } catch (ArchivalUnit.ConfigurationException auie) {
      assertNotNull(auie.getCause());
    }
  }
  
  public void testGetAuConstructsProperAu()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ID_KEY, "10.1055/s-00000002");
    props.setProperty(VOLUME_NAME_KEY, "2010");
    
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("Georg Thieme Verlag Plugin (CLOCKSS), Base URL http://www.example." +
                 "com/, Journal ID 10.1055/s-00000002, Volume 2010", au.getName());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.georgthiemeverlag.ClockssGeorgThiemeVerlagPlugin",
        plugin.getPluginId());
  }
  
  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.BASE_URL,
                               ConfigParamDescr.JOURNAL_ID,
                               ConfigParamDescr.VOLUME_NAME),
                 plugin.getLocalAuConfigDescrs());
  }
  
  public void testGetArticleMetadataExtractor() {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ID_KEY, "10.1055/s-00000002");
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
    assertTrue(""+plugin.getFileMetadataExtractor(MetadataTarget.Any(), "application/pdf", au),
        plugin.getFileMetadataExtractor(MetadataTarget.Any(), "application/pdf", au) instanceof
        FileMetadataExtractor
        );
  }
  
  public void testGetHashFilterFactory() {
    assertNull(plugin.getHashFilterFactory("BogusFilterFactory"));
    assertNotNull(plugin.getHashFilterFactory("application/pdf"));
    assertNotNull(plugin.getHashFilterFactory("text/html"));
    assertTrue(WrapperUtil.unwrap(plugin.getHashFilterFactory("text/html"))
        instanceof org.lockss.plugin.georgthiemeverlag.GeorgThiemeVerlagHtmlFilterFactory);
  }
  
  public void testGetArticleIteratorFactory() {
    assertTrue(WrapperUtil.unwrap(plugin.getArticleIteratorFactory())
        instanceof org.lockss.plugin.georgthiemeverlag.GeorgThiemeVerlagArticleIteratorFactory);
  }
  
  // Test the crawl rules for GeorgThiemeVerlagPlugin
  public void testShouldCacheProperPages() throws Exception {
    String ROOT_URL = "http://www.example.com/";
    URL baseUrl = new URL(ROOT_URL);
    String JOURNAL_ID = "10.1055/s-00000001";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ID_KEY, "10.1055/s-00000001");
    props.setProperty(VOLUME_NAME_KEY, "2013");
    DefinableArchivalUnit au = null;
    try {
      Configuration config = ConfigurationUtil.fromProps(props);
      DefinablePlugin tstDefinablePlugin = new DefinablePlugin();
      tstDefinablePlugin.initPlugin(getMockLockssDaemon(), plugin.getPluginId());
      au = (DefinableArchivalUnit)tstDefinablePlugin.createAu(config);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(au, 
        new RangeCachedUrlSetSpec(baseUrl.toString()));
    // Test for pages that should get crawled
    // permission page/start url
    shouldCacheTest(ROOT_URL + "ejournals/issues/" + JOURNAL_ID + "/2013", true, au);  
    // toc page for an issue
    shouldCacheTest(ROOT_URL + "ejournals/issue/" + "10.1055/s-003-26177", true, au);  
    // article files
    // https://www.thieme-connect.de/ejournals/issues/10.1055/s-00000001/2013
    // https://www.thieme-connect.de/ejournals/issue/10.1055/s-003-26177
    // https://www.thieme-connect.de/ejournals/abstract/10.1055/s-0029-1214947
    // https://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947
    // https://www.thieme-connect.de/ejournals/html/10.1055/s-0029-1214947?issue=10.1055/s-003-25342
    // https://www.thieme-connect.de/ejournals/pdf/10.1055/s-0029-1214947.pdf
    // https://www.thieme-connect.de/ejournals/pdf/10.1055/s-0029-1214947.pdf?issue=10.1055/s-003-25342
    // https://www.thieme-connect.de/ejournals/ris/10.1055/s-0031-1296349/BIB
    shouldCacheTest(ROOT_URL + "ejournals/abstract/10.1055/s-0029-1214947", true, au);
    shouldCacheTest(ROOT_URL + "ejournals/html/10.1055/s-0029-1214947", true, au);
    shouldCacheTest(ROOT_URL +
        "ejournals/html/10.1055/s-0029-1214947?issue=10.1055/s-003-25342", true, au);
    shouldCacheTest(ROOT_URL + "ejournals/pdf/10.1055/s-0029-1214947.pdf", true, au);
    shouldCacheTest(ROOT_URL +
        "ejournals/pdf/10.1055/s-0029-1214947.pdf?issue=10.1055/s-003-25342", true, au);
    shouldCacheTest(ROOT_URL + "ejournals/ris/10.1055/s-0031-1296349/BIB", true, au);
    // css files
    shouldCacheTest(ROOT_URL + "css/img/themes/bg-pageHeader.jpg", true, au);
    shouldCacheTest(ROOT_URL + "css/style.css", true, au);
    // supplement materials
    shouldCacheTest(ROOT_URL +
        "media/ains/20131112/supmat/ains_11_2013_18_sup_10-1055-s-0033-1361983.pdf",
        true, au);
    
    // should not get crawled - missing doi prefix
    shouldCacheTest(ROOT_URL + "ejournals/html/s-0029-1214947", false, au);  
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
}
