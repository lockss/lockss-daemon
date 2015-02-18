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

package org.lockss.plugin.clockss.onixbooks;

import java.net.URL;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.*;

//
// This plugin test framework is set up to run the same tests in two variants - CLOCKSS and GLN
// without having to actually duplicate any of the written tests
//
public class TestOnixBooksSourceArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String ROOT_URL = "http://www.sourcecontent.com/"; //this is not a real url
  
  static Logger log = Logger.getLogger("TestOnixBooksSourceArchivalUnit");
  
  static final String PLUGIN_ID = "org.lockss.plugin.clockss.onixbooks.ClockssOnix3BooksSourcePlugin";
  static final String PluginName = "ONIX 3 Books Source Plugin (CLOCKSS)";
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private DefinableArchivalUnit makeAu(URL url, String year)
      throws Exception {

    Properties props = new Properties();
    props.setProperty(YEAR_KEY, year);
    if (url != null) {
      props.setProperty(BASE_URL_KEY, url.toString());
    }
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
        PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(config);
    return au;
  }

  public void testConstructNullUrl() throws Exception {
    try {
      makeAu(null, "2012");
      fail("Should have thrown ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  
  //
  // Test the crawl rules
  //
  public void testShouldCacheProperPages() throws Exception {
    URL base = new URL(ROOT_URL);
    ArchivalUnit sourceAU = makeAu(base, "2012");
    theDaemon.getLockssRepository(sourceAU);
    theDaemon.getNodeManager(sourceAU);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(sourceAU,
        new RangeCachedUrlSetSpec(base.toString()));

    // Test for pages that should get crawled

    //with a subdirectory under the year
    shouldCacheTest(ROOT_URL+"2012/Berghahn_Press/", true, sourceAU, cus);    
    shouldCacheTest(ROOT_URL+"2012/Berghahn_Press/blah.xml", true, sourceAU, cus);    
    shouldCacheTest(ROOT_URL+"2012/Berghahn_Press/blah.pdf", true, sourceAU, cus);    
    shouldCacheTest(ROOT_URL+"2012/Berghahn_Press/blah.epub", true, sourceAU, cus);    
    shouldCacheTest(ROOT_URL+"2012/Berghahn_Press/blah.jpg", true, sourceAU, cus);
    //without a subdirectory under the year
    shouldCacheTest(ROOT_URL+"2012/", true, sourceAU, cus);    
    shouldCacheTest(ROOT_URL+"2012/blah.xml", true, sourceAU, cus);    
    shouldCacheTest(ROOT_URL+"2012/blah.pdf", true, sourceAU, cus);    
    shouldCacheTest(ROOT_URL+"2012/blah.epub", true, sourceAU, cus);    
    shouldCacheTest(ROOT_URL+"2012/blah.jpg", true, sourceAU, cus);
    
    // Now a couple that shouldn't get crawled
    // md5sum file
    shouldCacheTest(ROOT_URL+"2012/xxxx/blah.pdf.md5sum", true, sourceAU, cus);
    //wrong year
    shouldCacheTest(ROOT_URL+"2013/xxxx/blah.pdf", false, sourceAU, cus);
    // too deep
    shouldCacheTest(ROOT_URL+"2012/Berghahn_Press/randomdirectory/blah.jpg", true, sourceAU, cus);
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  
  public void testgetName() throws Exception {
    DefinableArchivalUnit au =
      makeAu(new URL("http://www.source.com/"), "2011");
    assertEquals(PluginName + ", Base URL http://www.source.com/, Year 2011", au.getName());
    DefinableArchivalUnit au1 =
      makeAu(new URL("http://www.source.com/"), "2009");
    assertEquals(PluginName + ", Base URL http://www.source.com/, Year 2009", au1.getName());
  }

}

