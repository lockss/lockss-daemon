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

import java.io.IOException;
import java.text.*;
import java.util.*;

import org.apache.commons.collections.ListUtils;
import org.lockss.daemon.*;
import org.lockss.daemon.PluginException.InvalidDefinition;
import org.lockss.plugin.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.test.*;
import org.lockss.util.*;

import com.lyncode.xoai.model.oaipmh.Header;
import com.lyncode.xoai.serviceprovider.ServiceProvider;

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
  protected TypedEntryMap p;
  protected DateFormat df;
  
  public void setUp() throws Exception {
    super.setUp();
    
    TimeZone utc = TimeZone.getTimeZone("UTC");
    df = new SimpleDateFormat(BaseOaiPmhCrawlSeed.DATE_FORMAT);
    df.setTimeZone(utc);

    theDaemon = getMockLockssDaemon();

    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin(theDaemon));
    mau.setAuId("MyMockTestAu");
    permissionUrls = ListUtil.list(permissionUrl);
    mau.setPermissionUrls(permissionUrls);    
    
    p = new TypedEntryMap();
    p.putInt(ConfigParamDescr.YEAR.getKey(), 1988);
    p.putString(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_SET, "");
    p.putString(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_URL_POSTFIX, "face");
    p.putString(ConfigParamDescr.BASE_URL.getKey(), "http://www.example.com/oai/");
    mau.setPropertyMap(p);
    cs = new TestableIdentifierListOaiPmhCrawlSeed(mau);
  }
  
  public void testNullAu() throws PluginException, ConfigurationException {
    try {
      cs = new TestableIdentifierListOaiPmhCrawlSeed(null);
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

  public void testBadProps() 
      throws PluginException, ConfigurationException {
    //No dates
    p.removeMapElement(ConfigParamDescr.YEAR.getKey());
    mau.setPropertyMap(p);
    try {
      cs = new TestableIdentifierListOaiPmhCrawlSeed(mau);
      fail("no dates set should have thrown");
    } catch (InvalidDefinition e) {
      assertMatchesRE(BaseOaiPmhCrawlSeed.KEY_AU_OAI_UNTIL_DATE,
          e.getMessage());
    }
    
    //Missing from date
    p.putString(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_UNTIL_DATE,
        "1988-01-01T00:00:00");
    p.removeMapElement(ConfigParamDescr.YEAR.getKey());
    mau.setPropertyMap(p);
    try {
      cs = new TestableIdentifierListOaiPmhCrawlSeed(mau);
      fail("missing from date should have thrown");
    } catch (InvalidDefinition e) {
      assertMatchesRE(BaseOaiPmhCrawlSeed.KEY_AU_OAI_FROM_DATE,
          e.getMessage());
    }
    
    //Missing until date
    p.putString(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_FROM_DATE,
        "1988-01-01T00:00:00");
    p.removeMapElement(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_UNTIL_DATE);
    mau.setPropertyMap(p);
    try {
      cs = new TestableIdentifierListOaiPmhCrawlSeed(mau);
      fail("missing until date should have thrown");
    } catch (InvalidDefinition e) {
      assertMatchesRE(BaseOaiPmhCrawlSeed.KEY_AU_OAI_FROM_DATE,
          e.getMessage());
    }
    
    //No BaseUrl
    p.putString(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_FROM_DATE,
        "1987-01-01T00:00:00");
    p.putString(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_UNTIL_DATE, 
        "1988-01-01T00:00:00");
    p.removeMapElement(ConfigParamDescr.BASE_URL.getKey());
    mau.setPropertyMap(p);
    try {
      cs = new TestableIdentifierListOaiPmhCrawlSeed(mau);
      fail("base url set should have thrown");
    } catch (InvalidDefinition e) {
      assertMatchesRE(ConfigParamDescr.BASE_URL.getKey(), e.getMessage());
    }
  }
  
  public void testBadOaiPerameters() 
      throws PluginException, ConfigurationException {
    //Invalid date range
    p.putString(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_FROM_DATE, 
        "1988-01-01T00:00:00");
    p.putString(IdentifierListOaiPmhCrawlSeed.KEY_AU_OAI_UNTIL_DATE, 
        "1987-01-01T00:00:00");
    p.removeMapElement(ConfigParamDescr.YEAR.getKey());
    //This should fail, but does not
    cs = new TestableIdentifierListOaiPmhCrawlSeed(mau);
  }
  
  public void testEmptyIdentifiers() 
      throws ParseException, ConfigurationException, PluginException, IOException {
    msp = (MockServiceProvider) cs.getServiceProvider();
    msp.setIdentifiers(buildHeaderIter(ListUtils.EMPTY_LIST));
    assertEquals(ListUtils.EMPTY_LIST, cs.getStartUrls());
  }
  
  public void testIdentifiers() 
      throws ConfigurationException, PluginException, IOException {
    msp = (MockServiceProvider) cs.getServiceProvider();
    msp.setIdentifiers(buildHeaderIter(ListUtil.list("1","2")));
    String prefix = "http://www.example.com/oai/oai/"
        + "request?verb=GetRecord&identifier=";
    String postfix = "&metadataPrefix=oai_dc";
    List<String> expected = ListUtil.list(
        prefix + "1" + postfix,
        prefix + "2" + postfix);
    assertEquals(expected, cs.getStartUrls());
  }
  
  public void testLotsOfIdentifiers()
      throws ConfigurationException, PluginException, IOException {
    msp = (MockServiceProvider) cs.getServiceProvider();
    List<String> idList = new ArrayList(1024);
    List<String> expected= new ArrayList(1024);
    String prefix = "http://www.example.com/oai/oai/"
        + "request?verb=GetRecord&identifier=";
    String postfix = "&metadataPrefix=oai_dc";
    
    for(int i = 1; i <= 1024; i++) {
      idList.add("" + i);
      expected.add(prefix + i + postfix);
    }
    
    msp.setIdentifiers(buildHeaderIter(idList));
    
    assertEquals(expected, cs.getStartUrls());
  }
  
  protected Iterator<Header> buildHeaderIter(Collection<String> ids) {
    Collection<Header> headerList = new ArrayList<Header>();
    Header tmpHeader;
    for(String id: ids) {
      tmpHeader = new Header();
      tmpHeader.withIdentifier(id);
      headerList.add(tmpHeader);
    }
    
    return headerList.iterator();
  }
  
  public class TestableIdentifierListOaiPmhCrawlSeed 
    extends IdentifierListOaiPmhCrawlSeed {

    public TestableIdentifierListOaiPmhCrawlSeed(ArchivalUnit au)
        throws PluginException, ConfigurationException {
      super(au);
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
