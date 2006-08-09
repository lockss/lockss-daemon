/*
 * $Id: TestOaiCrawler.java,v 1.13 2006-08-09 02:03:07 tlipkis Exp $
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
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.*;

import org.lockss.daemon.*;
import org.lockss.oai.OaiHandler;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.state.AuState;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestOaiCrawler extends LockssTestCase {

  private MockArchivalUnit mau = null;
//private MockCachedUrlSet mcus = null;
  private CrawlSpec spec = null;
  private MockAuState aus = new MockAuState();
  private MockCrawlRule crawlRule = null;
  private String handlerUrl = "http://www.example.com/handler.html";
  private String permissionUrl = "http://www.example.com/permission.html";
  private List permissionList = ListUtil.list(permissionUrl);
  private BaseCrawler crawler = null;

  SimpleDateFormat iso8601DateFormatter = new SimpleDateFormat ("yyyy-MM-dd");

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated(10);

    getMockLockssDaemon().getAlertManager();

    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin(getMockLockssDaemon()));
    mau.setAuId("MyMockTestAu");
//  mcus = (MockCachedUrlSet)mau.getAuCachedUrlSet();

    crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(handlerUrl);
    crawlRule.addUrlToCrawl(permissionUrl);
    mau.addUrl(permissionUrl);

    spec = new OaiCrawlSpec(handlerUrl, crawlRule, permissionList, false);

    mau.setCrawlSpec(spec);

    crawler = new OaiCrawler(mau, spec, aus);
    ((BaseCrawler)crawler).daemonPermissionCheckers =
      ListUtil.list(new MyMockPermissionChecker(1));
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
    assertEquals("OAI", crawler.getTypeString());
  }

  public void testIsWholeAU() {
    assertTrue(crawler.isWholeAU());
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

  /**
   * Test a base case for a first crawl on an AU, ie we have no content
   *
   */
  public void testSimpleCrawl() {
    //set the urls to be crawl
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1="http://www.example.com/blah.html";
    mau.addUrl(url1, false, true);
    crawlRule.addUrlToCrawl(url1);

    MockOaiHandler oaiHandler = new MockOaiHandler();
    oaiHandler.setUpdatedUrls(SetUtil.set(url1));

    //set the crawler
    MyOaiCrawler crawler = new MyOaiCrawler(mau, spec, aus);
    crawler.setOaiHandler(oaiHandler);
    crawler.daemonPermissionCheckers =
      ListUtil.list(new MyMockPermissionChecker(1));

    //do the crawl
    assertTrue(crawler.doCrawl());
    //verify the crawl result
    Set expected = SetUtil.set(url1, permissionUrl);
    assertEquals(expected, cus.getCachedUrls());
    assertEquals(SetUtil.set(), cus.getForceCachedUrls());
  }

  /**
   * We have content, verify that we don't force recrawl it
   *
   */
  public void testSimpleCrawlHasContent() {
    //set the urls to be crawl
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1 = "http://www.example.com/blah.html";
    mau.addUrl(url1, true, true); //exists, and should be cached
    crawlRule.addUrlToCrawl(url1);

    MockOaiHandler oaiHandler = new MockOaiHandler();
    oaiHandler.setUpdatedUrls(SetUtil.set(url1));

    //set the crawler
    MyOaiCrawler crawler = new MyOaiCrawler(mau, spec, aus);
    crawler.setOaiHandler(oaiHandler);
    crawler.daemonPermissionCheckers =
      ListUtil.list(new MyMockPermissionChecker(1));

    //do the crawl
    assertTrue(crawler.doCrawl());
    //verify the crawl result
    Set expected = SetUtil.set(url1, permissionUrl);
    assertEquals(expected, cus.getCachedUrls());
    assertEquals(SetUtil.set(), cus.getForceCachedUrls());
  }

  /**
   * OaiHandler throws exceptions, verify that this causes the crawl to fail
   *
   */
  public void testExceptionsInOaiHandler() {
    //set the urls to be crawl
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    String url1 = "http://www.example.com/blah.html";
    mau.addUrl(url1, true, true); //exists, and should be cached
    crawlRule.addUrlToCrawl(url1);

    MockOaiHandler oaiHandler = 
      new MyMockOaiHandler(new ExpectedRuntimeException());
//    oaiHandler.setUpdatedUrls(SetUtil.set(url1));

    //set the crawler
    MyOaiCrawler crawler = new MyOaiCrawler(mau, spec, aus);
    crawler.setOaiHandler(oaiHandler);
//    crawler.daemonPermissionCheckers =
//      ListUtil.list(new MyMockPermissionChecker(1));

    //do the crawl
    assertFalse(crawler.doCrawl());
    //verify the crawl result
//    Set expected = SetUtil.set(url1, permissionUrl);
//    assertEquals(expected, cus.getCachedUrls());
//    assertEquals(SetUtil.set(), cus.getForceCachedUrls());
  }

  

  /**
   * Testing the simple accessor methods
   *
   */
  public void testAccessors() {
    assertEquals("OAI", crawler.getTypeString());
    assertEquals(Crawler.OAI, crawler.getType());
    assertTrue(crawler.isWholeAU());
  }


  private class MyMockPermissionChecker implements PermissionChecker {
    int numPermissionGranted=0;

    MyMockPermissionChecker(int numPermissionGranted) {
      this.numPermissionGranted = numPermissionGranted;
    }

    public void setNumPermissionGranted(int num){
      numPermissionGranted = num;
    }

    /**
     * @param reader Reader
     * @return true numPermissionGranted times, then false
     */
    public boolean checkPermission(Crawler.PermissionHelper pHelper,
				   Reader reader, String permissionUrl) {
      if (numPermissionGranted-- > 0) {
        return true;
      } else {
        return false;
      }

    }
  }

  private class MyOaiCrawler extends OaiCrawler {
    OaiHandler oaiHandler;

    public MyOaiCrawler(ArchivalUnit au, CrawlSpec crawlSpec, AuState aus){
      super(au, crawlSpec, aus);
    }

    protected OaiHandler getOaiHandler() {
      return this.oaiHandler;
    }

    public void setOaiHandler(OaiHandler oaiHandler) {
      this.oaiHandler = oaiHandler;
    }

    /** suppress these actions */
    protected void doCrawlEndActions() {
    }
  }

  private class MyMockOaiHandler extends MockOaiHandler {
    RuntimeException ex;
    public MyMockOaiHandler(RuntimeException ex) {
      this.ex = ex;
    }
    
    public void processResponse(int maxRetries) {
      throw ex;
    }
  }
  
  public static void main(String[] argv) {
    String[] testCaseList = {TestOaiCrawler.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }

}

