/*
 * $Id: TestCrawlerImpl.java,v 1.27 2004-10-13 23:07:18 clairegriffin Exp $
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

import java.io.*;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.crawler.CrawlerImpl
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public class TestCrawlerImpl extends LockssTestCase {
  static final String PERMISSION_STRING = CrawlerImpl.LOCKSS_PERMISSION_STRING;
//   private PermissionChecker checker;
  private MockArchivalUnit mau = null;
  private List startUrls = null;

//   private Crawler crawler = null;
  private TestableCrawlerImpl crawler = null;

  private CrawlSpec spec = null;

  public static final String EMPTY_PAGE = "";
  public static final String LINKLESS_PAGE = "Nothing here";

  public static final String startUrl = "http://www.example.com/index.html";
  private MockCrawlRule crawlRule;
  private MockAuState aus = new MockAuState();

  private MockContentParser parser = new MockContentParser();

  private static final String PARAM_RETRY_TIMES =
    Configuration.PREFIX + "CrawlerImpl.numCacheRetries";
  private static final int DEFAULT_RETRY_TIMES = 3;

  public static Class testedClasses[] = {
    org.lockss.crawler.CrawlerImpl.class
  };

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated(10);

    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin());

    startUrls = ListUtil.list(startUrl);
    MockCachedUrlSet cus = new MyMockCachedUrlSet(mau, null);
    mau.setAuCachedUrlSet(cus);
    crawlRule = new MockCrawlRule();
    crawlRule.addUrlToCrawl(startUrl);
    spec = new CrawlSpec(startUrls, startUrls, crawlRule, 1);
    getMockLockssDaemon().getAlertManager();
    crawler = new TestableCrawlerImpl(mau, spec, aus);
    ((CrawlerImpl)crawler).lockssCheckers =
      ListUtil.list(new MyMockPermissionChecker(true));

//     crawler = new NewContentCrawler(mau, spec, aus);
//     // store the orignal checker and replace with a mock checker
//     checker = (PermissionChecker)((CrawlerImpl)crawler).lockssCheckers.get(0);
//     ((CrawlerImpl)crawler).lockssCheckers = ListUtil.list(new MyMockPermissionChecker(true));
// //     crawler = new MyCrawler(mau, spec, aus);

    mau.setParser(parser);
    Properties p = new Properties();
    p.setProperty(NewContentCrawler.PARAM_RETRY_PAUSE, "0");
    ConfigurationUtil.setCurrentConfigFromProps(p);
  }

  public void testConstructorNullAu() {
    try {
      new TestableCrawlerImpl(null, spec, aus);
      fail("Trying to create a CrawlerImpl with a null au should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testConstructorNullSpec() {
    try {
      new TestableCrawlerImpl(mau, null, aus);
      fail("Trying to create a CrawlerImpl with a null spec should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testConstructorNullAuState() {
    try {
      new TestableCrawlerImpl(mau, spec, null);
      fail("Trying to create a CrawlerImpl with a null aus should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testIsSupportedUrlProtocol() {
    assertTrue(crawler.isSupportedUrlProtocol("http://www.example.com"));
    assertTrue(crawler.isSupportedUrlProtocol("HTTP://www.example.com"));
    assertFalse(crawler.isSupportedUrlProtocol("ftp://www.example.com"));
    assertFalse(crawler.isSupportedUrlProtocol("gopher://www.example.com"));
    assertFalse(crawler.isSupportedUrlProtocol("https://www.example.com"));
  }

  public void testGetAu() {
    assertSame(mau, crawler.getAu());
  }

  public void testDoCrawlSignalsEndOfCrawl() {
    TestableCrawlerImpl crawler = new TestableCrawlerImpl(mau, spec, aus);
    crawler.doCrawl();
    MockCrawlStatus status = (MockCrawlStatus)crawler.getStatus();
    assertTrue(status.crawlEndSignaled());
  }

  public void testDoCrawlSignalsEndOfCrawlExceptionThrown() {
    TestableCrawlerImpl crawler = new TestableCrawlerImpl(mau, spec, aus);
    crawler.setDoCrawlThrowException(new RuntimeException("Blah"));
    try {
      crawler.doCrawl();
    } catch (RuntimeException e) {
      //don't do anything with it, since we're causing it to be thrown
    }
    MockCrawlStatus status = (MockCrawlStatus)crawler.getStatus();
    assertTrue(status.crawlEndSignaled());
  }

  /**
   * Subclasses rely on setWatchdog(wdog) assigning this value to the instance
   * varable wdog, so we're testing it
   */
  public void testSetWatchDog() {
    LockssWatchdog wdog = new MockLockssWatchdog();
    TestableCrawlerImpl crawler = new TestableCrawlerImpl(mau, spec, aus);
    crawler.setWatchdog(wdog);
    assertSame(wdog, crawler.wdog);
  }


  public void testToString() {
    assertTrue(crawler.toString().startsWith("[CrawlerImpl:"));
  }

  public void testAbortedCrawlDoesntStart() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    parser.addUrlSetToReturn(url1, SetUtil.set(url1, url2, url3));
    mau.addUrl(startUrl);
    mau.addUrl(url1);
    mau.addUrl(url2);
    mau.addUrl(url3);

    crawler.abortCrawl();
    assertFalse(crawler.doCrawl());
    assertEmpty(cus.getCachedUrls());
//     assertFalse(crawler.doCrawl0Called());
  }

//   public void testReturnsTrueWhenCrawlSuccessful() {
//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     String url1="http://www.example.com/blah.html";
//     mau.addUrl(startUrl, false, true);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
//     mau.addUrl(url1, false, true);

//     assertTrue(crawler.doCrawl());
//    }

//   public void testReturnsFalseWhenFailingUnretryableExceptionThrown() {
//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     String url1="http://www.example.com/blah.html";
//     mau.addUrl(startUrl, false, true);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
//     MyMockUnretryableCacheException exception =
//       new MyMockUnretryableCacheException("Test exception");
//     mau.addUrl(url1, exception, DEFAULT_RETRY_TIMES);
//     crawlRule.addUrlToCrawl(url1);

//     assertFalse(crawler.doCrawl());
//   }

//   public void testReturnsFalseWhenIOExceptionThrown() {
//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     String url1="http://www.example.com/blah.html";
//     mau.addUrl(startUrl, false, true);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
//     mau.addUrl(url1, new IOException("Test exception"), DEFAULT_RETRY_TIMES);
//     crawlRule.addUrlToCrawl(url1);

//     assertFalse(crawler.doCrawl());
//   }

//   public void testReturnsTrueWhenNonFailingUnretryableExceptionThrown() {
//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     String url1="http://www.example.com/blah.html";
//     mau.addUrl(startUrl, false, true);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
//     MyMockCacheException exception = new MyMockCacheException("Test exception");
//     mau.addUrl(url1, exception, DEFAULT_RETRY_TIMES);
//     crawlRule.addUrlToCrawl(url1);

//     assertTrue(crawler.doCrawl());
//   }

//   public void testReturnsRetriesWhenRetryableExceptionThrown() {
//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     String url1="http://www.example.com/blah.html";
//     mau.addUrl(startUrl, false, true);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
//     MyMockRetryableCacheException exception =
//       new MyMockRetryableCacheException("Test exception");
//     mau.addUrl(url1, exception, DEFAULT_RETRY_TIMES-1);
//     crawlRule.addUrlToCrawl(url1);

//     crawler.doCrawl();
//     Set expected = SetUtil.set(startUrl, url1);
//     assertEquals(expected, cus.getCachedUrls());
//   }

//   public void testReturnsDoesntRetryWhenUnretryableExceptionThrown() {
//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     String url1="http://www.example.com/blah.html";
//     mau.addUrl(startUrl, false, true);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
//     MyMockUnretryableCacheException exception =
//       new MyMockUnretryableCacheException("Test exception");
//     mau.addUrl(url1, exception, DEFAULT_RETRY_TIMES-1);
//     crawlRule.addUrlToCrawl(url1);

//     crawler.doCrawl();
//     Set expected = SetUtil.set(startUrl);
//     assertEquals(expected, cus.getCachedUrls());
//   }

//   public void testRetryNumSetByParam() {
//     int retryNum = DEFAULT_RETRY_TIMES + 3;
//     assertTrue("Test is worthless unless retryNum is greater than "
// 	       +"DEFAULT_RETRY_TIMES", retryNum > DEFAULT_RETRY_TIMES);
//     Properties p = new Properties();
//     p.setProperty(PARAM_RETRY_TIMES, String.valueOf(retryNum));
//     p.setProperty(NewContentCrawler.PARAM_RETRY_PAUSE, "0");
//     ConfigurationUtil.setCurrentConfigFromProps(p);

//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     String url1="http://www.example.com/blah.html";
//     mau.addUrl(startUrl, false, true);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
//     mau.addUrl(url1,
// 	       new MyMockRetryableCacheException("Test exception"),
// 	       retryNum-1);
//     crawlRule.addUrlToCrawl(url1);

//     crawler.doCrawl();
//     Set expected = SetUtil.set(startUrl, url1);
//     assertEquals(expected, cus.getCachedUrls());
//   }

//   public void testCachesFailedFetches() {
//     int retryNum = 3;
//     Properties p = new Properties();
//     p.setProperty(PARAM_RETRY_TIMES, String.valueOf(retryNum));
//     p.setProperty(NewContentCrawler.PARAM_RETRY_PAUSE, "0");
//     ConfigurationUtil.setCurrentConfigFromProps(p);

//     MyMockCachedUrlSet cus = (MyMockCachedUrlSet)mau.getAuCachedUrlSet();
//     String url1="http://www.example.com/blah.html";
//     String url2="http://www.example.com/blah2.html";
//     String url3="http://www.example.com/blah3.html";
//     mau.addUrl(startUrl, false, true);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
//     mau.addUrl(url2,
// 	       new CacheException.UnretryableException("Test exception"),
// 	       retryNum);
//     parser.addUrlSetToReturn(url2, SetUtil.set(url1));
//     mau.addUrl(url3,
// 	       new CacheException.RetryableException("Test exception"),
// 	       retryNum);
//     parser.addUrlSetToReturn(url3, SetUtil.set(url1));
//     mau.addUrl(url1, new IOException("Test exception"), retryNum);

//     crawlRule.addUrlToCrawl(url1);
//     crawlRule.addUrlToCrawl(url2);
//     crawlRule.addUrlToCrawl(url3);

//     crawler.doCrawl();
//     // IOException should not be retried
//     assertEquals(1, cus.getNumCacheAttempts(url1));
//     // UnretryableException should not be retried
//     assertEquals(1, cus.getNumCacheAttempts(url2));
//     // RetryableException should be retried
//     assertEquals(retryNum, cus.getNumCacheAttempts(url3));
//   }


//   public void testReturnsTrueWhenNonFailingExceptionThrown() {
//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     String url1="http://www.example.com/blah.html";
//     mau.addUrl(startUrl, false, true);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
//     mau.addUrl(url1, new FileNotFoundException("Test exception"), 0);
//     crawlRule.addUrlToCrawl(url1);

//     assertTrue(crawler.doCrawl());
//   }

//   public void testPluginThrowsRuntimeException() {
//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     String url1="http://www.example.com/blah.html";
//     mau.addUrl(startUrl, false, true);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
//     mau.addUrl(url1, new ExpectedRuntimeException("Test exception"), 0);
//     crawlRule.addUrlToCrawl(url1);

//     assertFalse(crawler.doCrawl());
//   }


  public void testGetStatusCrawlNotStarted() {
    Crawler.Status crawlStatus = crawler.getStatus();
    assertEquals(-1, crawlStatus.getStartTime());
    assertEquals(-1, crawlStatus.getEndTime());
    assertEquals(0, crawlStatus.getNumFetched());
    assertEquals(0, crawlStatus.getNumParsed());
  }

  public void testGetStatusCrawlDone() {
    String url1 = "http://www.example.com/link1.html";
    String url2 = "http://www.example.com/link2.html";
    String url3 = "http://www.example.com/link3.html";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
    mau.addUrl(startUrl);
    parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
    mau.addUrl(url1);
    mau.addUrl(url2);
    mau.addUrl(url3);
    crawlRule.addUrlToCrawl(url1);
    crawlRule.addUrlToCrawl(url2);
    crawlRule.addUrlToCrawl(url3);

    long expectedStart = TimeBase.nowMs();
    crawler.doCrawl();
    long expectedEnd = TimeBase.nowMs();
    Crawler.Status crawlStatus = crawler.getStatus();
    assertEquals(expectedStart, crawlStatus.getStartTime());
    assertEquals(expectedEnd, crawlStatus.getEndTime());
//     assertEquals(5, crawlStatus.getNumFetched());
//     assertEquals(4, crawlStatus.getNumParsed());
  }

//   public void testGetStatusIncomplete() {
//     //System.out.println("CrawlStatus is " + crawler.getStatus().getCrawlStatus());
//     assertEquals(Crawler.STATUS_INCOMPLETE,
// 		 crawler.getStatus().getCrawlStatus());
//   }

//   public void testGetStatusSuccessful() {
//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     mau.addUrl(startUrl);
//     crawler.doCrawl();
//     assertEquals(Crawler.STATUS_SUCCESSFUL,
// 		 crawler.getStatus().getCrawlStatus());
//   }

//   public void testGetStatusError() {
//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     String url1="http://www.example.com/blah.html";
//     mau.addUrl(startUrl, false, true);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1));
//     mau.addUrl(url1, new IOException("Test exception"), DEFAULT_RETRY_TIMES);
//     crawlRule.addUrlToCrawl(url1);

//     crawler.doCrawl();
//     assertEquals(Crawler.STATUS_ERROR,
// 		 crawler.getStatus().getCrawlStatus());
//   }

//   public void testCrawlWindow() {
//     String url1 = "http://www.example.com/link1.html";
//     String url2 = "http://www.example.com/link2.html";
//     String url3 = "http://www.example.com/link3.html";

//     CrawlSpec spec = new CrawlSpec(startUrl, crawlRule);
//     spec.setCrawlWindow(new MockCrawlWindowThatCountsDown(3));
//     mau.setCrawlSpec(spec);

//     MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAuCachedUrlSet();
//     mau.addUrl(startUrl);
//     parser.addUrlSetToReturn(startUrl, SetUtil.set(url1, url2, url3));
//     mau.addUrl(url1);
//     mau.addUrl(url2);
//     mau.addUrl(url3);
//     crawlRule.addUrlToCrawl(url1);
//     crawlRule.addUrlToCrawl(url2);
//     crawlRule.addUrlToCrawl(url3);


//     crawler = new TestableCrawlerImpl(mau, spec, aus);
// //     crawler = new NewContentCrawler(mau, spec, new MockAuState());
//     ((CrawlerImpl)crawler).lockssCheckers = ListUtil.list(new MockPermissionChecker(true));

//     mau.setParser(parser);
//     crawler.doCrawl();
//     // only gets 2 urls because start url is fetched twice (manifest & parse)
//     Set expected = SetUtil.set(startUrl, url1);
//     assertEquals(expected, cus.getCachedUrls());
//   }

  private class MockCrawlWindowThatCountsDown implements CrawlWindow {
    int counter;

    public MockCrawlWindowThatCountsDown(int counter) {
      this.counter = counter;
    }

    public int getCurrentCount() {
      return counter;
    }

    public boolean canCrawl() {
      if (counter > 0) {
        counter--;
        return true;
      }
      return false;
    }

    public boolean canCrawl(Date serverDate) {
      return canCrawl();
    }

    public boolean crawlIsPossible() {
      return true;
    }
  }

  private class MyMockPermissionChecker implements PermissionChecker{
    boolean permission = false;

    MyMockPermissionChecker(boolean permission) {
      this.permission = permission;
    }

    /**
     * checkPermission
     *
     * @param reader Reader
     * @return boolean
     */
    public boolean checkPermission(Reader reader) {
      return permission;
    }

    public void setPermission(boolean permission) {
      this.permission = permission;
    }
  }
  private class MyMockCacheException
    extends CacheException {
    public MyMockCacheException(String msg) {
      super(msg);
    }
    public void setFailing() {
      attributeBits.set(CacheException.ATTRIBUTE_FAIL);
    }
  }

  private class MyMockRetryableCacheException
    extends CacheException.RetryableException {
    public MyMockRetryableCacheException(String msg) {
      super(msg);
    }
//     public void setFailing() {
//       attributeBits.set(CacheException.ATTRIBUTE_FAIL);
//     }
  }

  private class MyMockUnretryableCacheException
    extends CacheException.UnretryableException {
    public MyMockUnretryableCacheException(String msg) {
      super(msg);
    }
//     public void setFailing() {
//       attributeBits.set(CacheException.ATTRIBUTE_FAIL);
//     }
  }

  private class MyMockCachedUrlSet extends MockCachedUrlSet {
    public MyMockCachedUrlSet(MockArchivalUnit owner, CachedUrlSetSpec spec) {
      super(owner, spec);
    }
    protected MockUrlCacher makeMockUrlCacher(String url,
					      MockArchivalUnit parent) {
      return new MockUrlCacherThatStepsTimebase(url, parent);
    }

  }

  private class MockUrlCacherThatStepsTimebase extends MockUrlCacher {
    public MockUrlCacherThatStepsTimebase(String url, MockArchivalUnit au) {
      super(url, au);
    }
    public int cache() throws IOException {
      TimeBase.step();
      return super.cache();
    }

    public InputStream getUncachedInputStream() {
      return new StringInputStream("");
    }
  }

  private static class TestableCrawlerImpl extends CrawlerImpl {
    RuntimeException crawlExceptionToThrow = null;
    protected TestableCrawlerImpl(ArchivalUnit au,
				  CrawlSpec spec, AuState aus) {
      super(au, spec, aus);
      crawlStatus = new MockCrawlStatus();
    }

    public int getType() {
      throw new UnsupportedOperationException("not implemented");
    }

    protected boolean doCrawl0() {
      if (crawlExceptionToThrow != null) {
	throw crawlExceptionToThrow;
      }
      return true;
    }

    public void setDoCrawlThrowException(RuntimeException e) {
      crawlExceptionToThrow = e;
    }
  }


  public static void main(String[] argv) {
   String[] testCaseList = {TestCrawlerImpl.class.getName()};
   junit.textui.TestRunner.main(testCaseList);
  }
}
