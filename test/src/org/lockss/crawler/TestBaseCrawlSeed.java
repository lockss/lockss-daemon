package org.lockss.crawler;

import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.PluginException.InvalidDefinition;
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
      bcs = new BaseCrawlSeed(null);
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
      assertMatchesRE("non-null start URL list", e.getMessage());
    }
    
    mau.setPermissionUrls(null);
    try {
      bcs.getPermissionUrls();
      fail("null permission url list should have thrown");
    } catch(InvalidDefinition e) {
      assertMatchesRE("non-null permission URL list", e.getMessage());
    }
    
    mau.setStartUrls(ListUtils.EMPTY_LIST);
    try {
      bcs.getStartUrls();
      fail("empty start url list should have thrown");
    } catch(InvalidDefinition e) {
      assertMatchesRE("non-null start URL list", e.getMessage());
    }
    
    mau.setPermissionUrls(ListUtils.EMPTY_LIST);
    try {
      bcs.getPermissionUrls();
      fail("empty permission url list should have thrown");
    } catch(InvalidDefinition e) {
      assertMatchesRE("non-null permission URL list", e.getMessage());
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
