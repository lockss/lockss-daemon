/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.americanmathematicalsociety;

import java.net.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.wrapper.WrapperUtil;

public class TestAmericanMathematicalSocietyBooksPlugin extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String COLLECTION_ID_KEY = "collection_id";
  static final String YEAR_KEY = "year_string";
  
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  
  public TestAmericanMathematicalSocietyBooksPlugin(String msg) {
    super(msg);
  }
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(theDaemon,
        "org.lockss.plugin.americanmathematicalsociety." +
        "ClockssAmericanMathematicalSocietyBooksPlugin");
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
    props.setProperty(COLLECTION_ID_KEY, "c_id");
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
    props.setProperty(COLLECTION_ID_KEY, "jams");
    props.setProperty(YEAR_KEY, "2000-2009");
    
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
    props.setProperty(COLLECTION_ID_KEY, "c_id");
    props.setProperty(YEAR_KEY, "2004");
    
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("American Mathematical Society Books Plugin (CLOCKSS), " +
        "Base URL http://www.example.com/, " +
        "Collection ID c_id, YearStr 2004", au.getName());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.americanmathematicalsociety." +
        "ClockssAmericanMathematicalSocietyBooksPlugin",
        plugin.getPluginId());
  }
  
  public void testGetArticleMetadataExtractor() {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(COLLECTION_ID_KEY, "asdf");
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
    assertNull(plugin.getHashFilterFactory("text/html"));
  }
  public void testGetArticleIteratorFactory() {
    assertTrue(WrapperUtil.unwrap(plugin.getArticleIteratorFactory())
        instanceof org.lockss.plugin.americanmathematicalsociety.
        AmericanMathematicalSocietyBooksArticleIteratorFactory);
  }
  
  // Test the crawl rules for AmericanMathematicalSocietyPlugin
  public void testShouldCacheProperPages() throws Exception {
    String ROOT_URL = "https://www.example.com/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(COLLECTION_ID_KEY, "asdf");
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
    shouldCacheTest(ROOT_URL + "clockssdata?p=asdf", true, au);
    shouldCacheTest(ROOT_URL + "lockssdata?p=asdf", false, au);
    shouldCacheTest(ROOT_URL + "books/asdf/year/2004", true, au);
    shouldCacheTest(ROOT_URL + "books/asdf/year/2000-2009", false, au);
    
    // toc page for a book http://www.ams.org/books/conm/630
    shouldCacheTest(ROOT_URL + "books/asdf/200/", true, au);
    shouldCacheTest(ROOT_URL + "books/asdf/200", true, au);
    shouldCacheTest(ROOT_URL + "books/asdf/9", true, au);
    shouldCacheTest(ROOT_URL + "books/asdf/010.1", true, au);
    shouldCacheTest(ROOT_URL + "books/asdf/abc", false, au);
    
    shouldCacheTest(ROOT_URL + "books/asdf/200/asdf200.pdf", true, au);
    shouldCacheTest(ROOT_URL + "books/asdf/010.1/asdf010.1.pdf", true, au);

    // chapter files
    shouldCacheTest(ROOT_URL + "books/asdf/200/123456", true, au);
    shouldCacheTest(ROOT_URL + "books/asdf/200/asdf2000000.pdf", true, au);
    shouldCacheTest(ROOT_URL + "books/asdf/200/123456/asdf200-123456.pdf", true, au);
    shouldCacheTest(ROOT_URL + "books/asdf/010.1/4567", true, au);
    shouldCacheTest(ROOT_URL + "books/asdf/010.1/4567/asdf010.1-123456.pdf", true, au);
    
    // should not get crawled - wrong journal
    shouldCacheTest(ROOT_URL + "clockssdata/?p=ecgd", false, au);
    // should not get crawled - LOCKSS
    shouldCacheTest("https://lockss.stanford.edu", false, au);
  }
  
  public void testShouldCacheProperPagesYears() throws Exception {
    String ROOT_URL = "https://www.example.com/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(COLLECTION_ID_KEY, "xyz");
    props.setProperty(YEAR_KEY, "2000-2009");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    
    // Test for pages that should get crawled
    // permission page/start url
    shouldCacheTest(ROOT_URL + "clockssdata?p=xyz", true, au);
    shouldCacheTest(ROOT_URL + "lockssdata?p=xyz", false, au);
    shouldCacheTest(ROOT_URL + "books/xyz/year/2000-2009", true, au);
    shouldCacheTest(ROOT_URL + "books/xyz/year/2004", false, au);
    
  }
  

  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    //log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
}
