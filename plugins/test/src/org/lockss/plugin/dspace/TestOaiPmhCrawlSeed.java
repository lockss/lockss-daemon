/*

Copyright (c) 2000-2020 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.dspace;

import java.io.IOException;
import java.text.*;
import java.util.*;
import org.apache.commons.collections.ListUtils;
import org.dspace.xoai.model.oaipmh.Header;
import org.dspace.xoai.serviceprovider.ServiceProvider;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException.InvalidDefinition;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestOaiPmhCrawlSeed extends LockssTestCase {
  
  protected MockLockssDaemon theDaemon;
  protected MockArchivalUnit mau = null;
  protected MockAuState aus = new MockAuState();
  protected MockCrawlRule crawlRule = null;
  protected String permissionUrl = "http://www.example.com/permission.html";
  protected List<String> permissionUrls;
  protected MockLinkExtractor extractor = new MockLinkExtractor();
  protected TestableIdentifierListOaiPmhCrawlSeed cs;
  protected MockServiceProvider msp;
  protected Configuration config;
  protected DateFormat df;
  
  public void setUp() throws Exception {
    super.setUp();
    
    TimeZone utc = TimeZoneUtil.getExactTimeZone("UTC");
    df = new SimpleDateFormat(BaseOaiPmhCrawlSeed.DATE_FORMAT);
    df.setTimeZone(utc);

    theDaemon = getMockLockssDaemon();

    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin(theDaemon));
    mau.setAuId("MyMockTestAu");
    permissionUrls = ListUtil.list(permissionUrl);
    mau.setPermissionUrls(permissionUrls);    
    
    config = ConfigManager.newConfiguration();
    config.put(ConfigParamDescr.YEAR.getKey(), "1988");
    config.put(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_SET, "");
    config.put(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_URL_POSTFIX, "oai/request");
    config.put(ConfigParamDescr.BASE_URL.getKey(), "http://www.example.com/");
    mau.setConfiguration(config);
    cs = new TestableIdentifierListOaiPmhCrawlSeed(new MockCrawler().new MockCrawlerFacade(mau));
  }
  
  public void testNullAu() throws PluginException, ConfigurationException {
    try {
      cs = new TestableIdentifierListOaiPmhCrawlSeed(new MockCrawler().new MockCrawlerFacade(null));
      fail("should throw because there is no au");
    } catch(IllegalArgumentException e) {
      assertMatchesRE("Valid ArchivalUnit", e.getMessage());
    }
  }
  
  public void testBadPermissionUrlListThrows() 
      throws ConfigurationException, PluginException, IOException {
    
    mau.setPermissionUrls(null);
    try {
      cs.getPermissionUrls();
      fail("null permission url list should have thrown");
    } catch(InvalidDefinition e) {
      assertMatchesRE("non-null permission URL list", e.getMessage());
    }
    
    mau.setPermissionUrls(ListUtils.EMPTY_LIST);
    try {
      cs.getPermissionUrls();
      fail("empty permission url list should have thrown");
    } catch(InvalidDefinition e) {
      assertMatchesRE("non-null permission URL list", e.getMessage());
    }
  }
  
  public void testPermissionUrl() 
      throws ConfigurationException, PluginException, IOException {
    assertEquals(permissionUrls, cs.getPermissionUrls());
  }
  
  public void testIsFailOnStartUrl() {
    assertFalse(cs.isFailOnStartUrlError());
  }
  
  public void testBadOaiPerameters() 
      throws PluginException, ConfigurationException {
    //Invalid date range
    config.put(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_FROM_DATE, 
        "1988-01-01T00:00:00");
    config.put(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_UNTIL_DATE, 
        "1987-01-01T00:00:00");
    config.remove(ConfigParamDescr.YEAR.getKey());
    //This should fail, but does not
    cs = new TestableIdentifierListOaiPmhCrawlSeed(new MockCrawler().new MockCrawlerFacade(mau));
  }
  
  public void testEmptyIdentifiers() 
      throws ParseException, ConfigurationException, PluginException, IOException {
    msp = (MockServiceProvider) cs.getServiceProvider();
    msp.setIdentifiers(buildHeaderList(ListUtils.EMPTY_LIST));
    assertEquals(ListUtils.EMPTY_LIST, cs.getStartUrls());
  }
  
  public void testIdentifiers() 
      throws ConfigurationException, PluginException, IOException {
    msp = (MockServiceProvider) cs.getServiceProvider();
    msp.setIdentifiers(buildHeaderList(ListUtil.list("oai:1","oai:2")));
    String prefix = "http://www.example.com/oai/"
        + "request?verb=GetRecord&identifier=";
    String postfix = "&metadataPrefix=" + BaseOaiPmhCrawlSeed.DEFAULT_METADATA_PREFIX;
    List<String> expected = ListUtil.list(
        "http://www.example.com/xmlui/handle/" + 1,
        "http://www.example.com/xmlui/handle/" + 2);
    assertEquals(expected, cs.getStartUrls());
  }
  
  public void testLotsOfIdentifiers()
      throws ConfigurationException, PluginException, IOException {
    msp = (MockServiceProvider) cs.getServiceProvider();
    List<String> idList = new ArrayList(1024);
    List<String> expected= new ArrayList(1024);
    String prefix = "http://www.example.com/oai/"
        + "request?verb=GetRecord&identifier=";
    String postfix = "&metadataPrefix=" + BaseOaiPmhCrawlSeed.DEFAULT_METADATA_PREFIX;
    for(int i = 1; i <= 1024; i++) {
      idList.add("oai:" + i);
      expected.add("http://www.example.com/xmlui/handle/" + i);
    }
    
    msp.setIdentifiers(buildHeaderList(idList));
    
    assertEquals(expected, cs.getStartUrls());
  }
  
  protected Collection<Header> buildHeaderList(Collection<String> ids) {
    Collection<Header> headerList = new ArrayList<Header>();
    Header tmpHeader;
    for(String id: ids) {
      tmpHeader = new Header();
      tmpHeader.withIdentifier(id);
      headerList.add(tmpHeader);
    }
    
    return headerList;
  }
  
  public class TestableIdentifierListOaiPmhCrawlSeed 
    extends IdentifierListOaiPmhCrawlSeed {

    public TestableIdentifierListOaiPmhCrawlSeed(CrawlerFacade cf)
        throws PluginException, ConfigurationException {
      super(cf);
    }
    
    @Override
    protected ServiceProvider getServiceProvider() {
      if(sp == null){
        sp = new MockServiceProvider(buildContext(baseUrl + oaiUrlPostfix));
      }
      return sp;
    }
    
  }
  
}
