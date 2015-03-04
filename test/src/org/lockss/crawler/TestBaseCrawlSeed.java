/*
 * $Id$
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

package org.lockss.crawler;

import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.PluginException.InvalidDefinition;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockAuState;
import org.lockss.test.MockCrawlRule;
import org.lockss.test.MockLinkExtractor;
import org.lockss.test.MockLockssDaemon;
import org.lockss.test.MockPlugin;
import org.lockss.util.ListUtil;


public class TestBaseCrawlSeed extends LockssTestCase {
  
  protected MockLockssDaemon theDaemon;
  protected MockArchivalUnit mau = null;
  protected MockAuState aus = new MockAuState();
  protected MockCrawlRule crawlRule = null;
  protected String startUrl = "http://www.example.com/index.html";
  protected String permissionUrl = "http://www.example.com/permission.html";
  protected List<String> startUrls;
  protected List<String> permissionUrls;
  protected MockLinkExtractor extractor = new MockLinkExtractor();
  protected BaseCrawlSeed bcs;
  
  public void setUp() throws Exception {
    super.setUp();

    theDaemon = getMockLockssDaemon();

    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin(theDaemon));
    mau.setAuId("MyMockTestAu");
    startUrls = ListUtil.list(startUrl);
    permissionUrls = ListUtil.list(permissionUrl);
    mau.setStartUrls(startUrls);
    mau.setPermissionUrls(permissionUrls);
    
    bcs = new BaseCrawlSeed(mau);
  
  }
  
  public void testNullAu() {
    try {
      bcs = new BaseCrawlSeed((ArchivalUnit)null);
    } catch(Exception e) {
      fail("Should not throw it is conceivable"
          + " for a crawlseed to not need the au");
    }
  }
  
  public void testBadUrlListThrows() 
      throws ConfigurationException, PluginException {
    mau.setStartUrls(null);
    try {
      bcs.getStartUrls();
      fail("null start url list should have thrown");
    } catch(InvalidDefinition e) {
      assertMatchesRE("non null start url", e.getMessage());
    }
    
    mau.setPermissionUrls(null);
    try {
      bcs.getPermissionUrls();
      fail("null permission url list should have thrown");
    } catch(InvalidDefinition e) {
      assertMatchesRE("non null permission url", e.getMessage());
    }
    
    mau.setStartUrls(ListUtils.EMPTY_LIST);
    try {
      bcs.getStartUrls();
      fail("empty start url list should have thrown");
    } catch(InvalidDefinition e) {
      assertMatchesRE("non null start url", e.getMessage());
    }
    
    mau.setPermissionUrls(ListUtils.EMPTY_LIST);
    try {
      bcs.getPermissionUrls();
      fail("empty permission url list should have thrown");
    } catch(InvalidDefinition e) {
      assertMatchesRE("non null permission url", e.getMessage());
    }
  }
  
  public void testSingleUrl() 
      throws ConfigurationException, PluginException {
    assertEquals(startUrls, bcs.getStartUrls());
    assertEquals(permissionUrls, bcs.getPermissionUrls());
  }
  
  public void testMultipleUrls() 
      throws ConfigurationException, PluginException {
    //Adding to urls to mock AU lists
    startUrls.add("http://www.example2.com/index2.html");
    startUrls.add("http://www.example3.com/index3.html");
    permissionUrls.add("http://www.exampletwo.com/permissiontwo.html");
    permissionUrls.add("http://www.exampleother.com/somethingElse.html");
    
    assertEquals(startUrls, bcs.getStartUrls());
    assertEquals(permissionUrls, bcs.getPermissionUrls());
  }
  
  public void testIsFailOnStartUrl() {
    assertTrue(bcs.isFailOnStartUrlError());
  }
  
}
