/*
 * $Id: TestOaiCrawler.java,v 1.3 2004-12-18 01:44:56 dcfok Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import java.io.*;

import java.text.SimpleDateFormat;
import org.lockss.oai.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.state.*;
import org.lockss.util.urlconn.*;
import ORG.oclc.oai.harvester2.verb.ListRecords;

public class TestOaiCrawler extends LockssTestCase {

     private MockArchivalUnit mau = null;
//   private MockCachedUrlSet mcus = null;
     private CrawlSpec spec = null;
     private MockAuState aus = new MockAuState();
     private MockCrawlRule crawlRule = null;
     private String handlerUrl = "http://www.example.com/handler.html";
     private String permissionUrl = "http://www.example.com/permission.html";
     private List permissionList = ListUtil.list(permissionUrl);  
     private CrawlerImpl crawler = null;

     SimpleDateFormat iso8601DateFormatter = new SimpleDateFormat ("yyyy-MM-dd");
  
  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated(10);

    getMockLockssDaemon().getAlertManager();

    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin());
    mau.setAuId("MyMockTestAu");
//     mcus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(handlerUrl);
    crawlRule.addUrlToCrawl(permissionUrl);
    mau.addUrl(permissionUrl);
    spec = new OaiCrawlSpec(handlerUrl, crawlRule, permissionList, false);
    crawler = new OaiCrawler(mau, spec, aus);
    ((CrawlerImpl)crawler).lockssCheckers = ListUtil.list(new MyMockPermissionChecker(1));

  }

  public void testMocThrowsForNullAu() {
    try {
      crawler = new OaiCrawler(null, spec, new MockAuState());
      fail("Constructing an OaiCrawler with a null ArchivalUnit"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMocThrowsForNullCrawlSpec() {
    try {
      crawler = new OaiCrawler(mau, null, new MockAuState());
      fail("Constructing an OaiCrawler with a null CrawlSpec"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testMocThrowsForNullAuState() {
    try {
      crawler = new OaiCrawler(mau, spec, null);
      fail("Constructing an OaiCrawler with a null AuState"
	   +" should throw an IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testReturnsProperType() {
    assertEquals(Crawler.OAI, crawler.getType());
  }

  public void testGetFromTime() {
    MockAuState aus = new MockAuState(mau);
    Calendar cal = new GregorianCalendar(1981, 5, 14);
    Date lastCrawlDate = cal.getTime();
    long lastCrawlTime = lastCrawlDate.getTime();
    aus.setLastCrawlTime(lastCrawlTime);

    //System.out.println("last Crawl time = " + lastCrawlTime); 
    String expected = iso8601DateFormatter.format(lastCrawlDate);
    
    spec = new OaiCrawlSpec(handlerUrl, crawlRule);
    crawler = new OaiCrawler(mau, spec, aus);
    assertEquals(expected, ((OaiCrawler)crawler).getFromTime());
  }

  public void testGetUntilTime() {
    //this test might fail if it is run at around 11:59 pm of a day.
    Date currentDate = new Date();
    String expected = iso8601DateFormatter.format(currentDate);
    //System.out.println("current Data = " + currentDate);
    assertEquals(expected, ((OaiCrawler)crawler).getUntilTime());
  }

  //XXX test need to be implement
  // 1. need to add test for different kind of exception throw in parsering 
  //    the XML from OAI repository
  // 2. test the followLink ability of the crawler, not just the boolean
  // 3. test oai crawl with different permission scenarios
  //
  // Tests below are subject to change after re-implement the Oai Crawler

  public void testShouldFollowLinkReturnTrue(){ 
    // this need to made into a real functional test
    spec = new OaiCrawlSpec(handlerUrl, crawlRule, permissionList, true);
    crawler = new OaiCrawler(mau, spec, aus);
    assertTrue(((OaiCrawler)crawler).shouldFollowLink());
  }

  public void testShouldFollowLinkReturnFalse(){
    // this need to made into a real functional test
    spec = new OaiCrawlSpec(handlerUrl, crawlRule, permissionList, false);
    crawler = new OaiCrawler(mau, spec, aus);
    assertFalse(((OaiCrawler)crawler).shouldFollowLink());
  }

  public void testSimpleCrawlReturnTrue(){
    //set the urls to be crawl
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(url1, false, true);
    crawlRule.addUrlToCrawl(url1);

    //set the crawler
    crawler = new MyOaiCrawler(mau, spec, aus);
    ((MyOaiCrawler)crawler).setUrlsToFollow(SetUtil.set(url1));
    ((MyOaiCrawler)crawler).lockssCheckers = ListUtil.list(new MyMockPermissionChecker(1));

    //do the crawl
    assertTrue(crawler.doCrawl());
    //verify the crawl result
    Set expected = SetUtil.set(url1, permissionUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  private class MyMockPermissionChecker implements PermissionChecker{
    int numPermissionGranted=0;

    MyMockPermissionChecker(int numPermissionGranted) {
      this.numPermissionGranted = numPermissionGranted;
    }

    public void setNumPermissionGranted(int num){
      numPermissionGranted = num;
    }

   /**
     * checkPermission
     *
     * @param reader Reader
     * @return boolean
     */
    public boolean checkPermission(Reader reader) {
        if (numPermissionGranted-- > 0) {
          return true;
        } else {
          return false;
        }

    }
  }

  private class MyOaiCrawler extends OaiCrawler{

    Set updatedUrls;

    public MyOaiCrawler(ArchivalUnit au, CrawlSpec crawlSpec, AuState aus){
      super(au, crawlSpec, aus);
    }
    
    protected Set getUrlsToFollow(){
      return updatedUrls;
    }

    protected void setUrlsToFollow(Set urls){
      updatedUrls = urls;
    }

    /** suppress these actions */
    protected void doCrawlEndActions() {
    }
  }//end of MyOaiCrawler

  public static void main(String[] argv) {
    String[] testCaseList = {TestOaiCrawler.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }

}

