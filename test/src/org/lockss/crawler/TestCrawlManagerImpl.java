/*
 * $Id: TestCrawlManagerImpl.java,v 1.17 2003-04-18 22:31:02 troberts Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.*;
import junit.framework.TestCase;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.state.*;
import org.lockss.plugin.*;

/**
 */
public class TestCrawlManagerImpl extends LockssTestCase {
  static final long TEN_SECONDS = 10 * Constants.SECOND;
  public static final String startUrl = "http://www.example.com/index.html";
  private CrawlManagerImpl crawlManager = null;
  private MockArchivalUnit mau = null;
  private List urlList = null;
  public static final String EMPTY_PAGE = "";
  public static final String LINKLESS_PAGE = "Nothing here";

  private MockLockssDaemon theDaemon;
  private MockNodeManager nodeManager;

  public TestCrawlManagerImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();

    urlList = ListUtil.list(startUrl);
    MockCachedUrlSet cus = new MockCachedUrlSet(mau, null);
    mau.setAUCachedUrlSet(cus);

    crawlManager = new CrawlManagerImpl();
    mau.setNewContentCrawlUrls(ListUtil.list(startUrl));

    theDaemon = new MockLockssDaemon();
    theDaemon.setNodeManagerService(new MockNodeManagerService());
    nodeManager = (MockNodeManager)theDaemon.getNodeManager(mau);

    crawlManager.initService(theDaemon);
    crawlManager.startService();
  }

  public void tearDown() throws Exception {
    nodeManager.stopService();
    crawlManager.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  public void testNullAUForIsCrawlingAU() {
    try {
      crawlManager.isCrawlingAU(null,
				new TestCrawlCB(new SimpleBinarySemaphore()),
				"blah");
      fail("Didn't throw an IllegalArgumentException on a null AU");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testDoesNotStartCrawlIfShouldNot() {
    mau.setShouldCrawlForNewContent(false);
    assertFalse(crawlManager.isCrawlingAU(mau, null, null));
  }

  public void testNullCallbackForIsCrawlingAU() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);
    crawlManager.isCrawlingAU(mau, null, "blah");
  }

  public void testDoesNewContentCrawl() {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href="+url2+">link2</a>"+
      "<a href="+url3+">link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);

    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

    assertTrue(crawlManager.isCrawlingAU(mau, new TestCrawlCB(sem), null));

    assertTrue("Crawl didn't finish in 10 seconds", sem.take(TEN_SECONDS));

    Set expected = SetUtil.set(startUrl, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }


  public void testTriggersNewContentCallback() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    TestCrawlCB cb = new TestCrawlCB(sem);
    crawlManager.isCrawlingAU(mau, cb, null);

    assertTrue("Crawl didn't finish in 10 seconds", sem.take(TEN_SECONDS));
    assertTrue("Callback wasn't triggered", cb.wasTriggered());
  }


  public void testNewContentCrawlCallbackReturnsCookie() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

    String cookie = "cookie string";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    TestCrawlCB cb = new TestCrawlCB(sem);
    crawlManager.isCrawlingAU(mau, cb, cookie);

    assertTrue("Crawl didn't finish in 10 seconds", sem.take(TEN_SECONDS));
    assertEquals(cookie, (String)cb.getCookie());
  }

  public void testNewContentCrawlCallbackReturnsNullCookie() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    String cookie = null;

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    TestCrawlCB cb = new TestCrawlCB(sem);
    crawlManager.isCrawlingAU(mau, cb, cookie);

    assertTrue("Crawl didn't finish in 10 seconds", sem.take(TEN_SECONDS));
    assertEquals(cookie, (String)cb.getCookie());
  }

  public void testKicksOffNewThread() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    TestCrawlCB cb = new TestCrawlCB();
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();

    cus.addUrl(LINKLESS_PAGE, startUrl);

    WaitOnSemaphoreCallback pauseCB = new WaitOnSemaphoreCallback(sem);

    mau.setPauseCallback(pauseCB);

    crawlManager.isCrawlingAU(mau, cb, null);

    //if the callback was triggered, the crawl completed
    assertFalse("Callback was triggered", cb.wasTriggered());
  }

  public void testScheduleRepairNullAU() throws MalformedURLException {
    try{
      crawlManager.scheduleRepair(null, new URL("http://www.example.com"),
				  new TestCrawlCB(), "blah");
      fail("Didn't throw IllegalArgumentException on null AU");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testScheduleRepairNullUrl() {
    try{
      crawlManager.scheduleRepair(mau, null,
				  new TestCrawlCB(), "blah");
      fail("Didn't throw IllegalArgumentException on null URL");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testBasicRepairCrawl() throws MalformedURLException {
    String url1= "http://www.example.com/link1.html";
    String url2= "http://www.example.com/link2.html";
    String url3= "http://www.example.com/link3.html";

    String source =
      "<html><head><title>Test</title></head><body>"+
      "<a href="+url1+">link1</a>"+
      "Filler, with <b>bold</b> tags and<i>others</i>"+
      "<a href="+url2+">link2</a>"+
      "<a href="+url3+">link3</a>";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(source, startUrl);
    cus.addUrl(LINKLESS_PAGE, url1);
    cus.addUrl(LINKLESS_PAGE, url2);
    cus.addUrl(LINKLESS_PAGE, url3);

    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

    crawlManager.scheduleRepair(mau, new URL(startUrl),
				new TestCrawlCB(sem), null);

    assertTrue("Crawl didn't finish in 10 seconds", sem.take(TEN_SECONDS));
    assertEquals(SetUtil.set(startUrl), cus.getCachedUrls());
  }

  public void testTriggersRepairCallback() throws MalformedURLException {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    TestCrawlCB cb = new TestCrawlCB(sem);

    crawlManager.scheduleRepair(mau, new URL(startUrl), cb, null);

    assertTrue("Crawl didn't finish in 10 seconds", sem.take(TEN_SECONDS));
    assertTrue("Callback wasn't triggered", cb.wasTriggered());
  }

  public void testRepairCallbackGetsCookie() throws MalformedURLException {
    String cookie = "test cookie str";
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    TestCrawlCB cb = new TestCrawlCB(sem);

    crawlManager.scheduleRepair(mau, new URL(startUrl), cb, cookie);

    assertTrue("Crawl didn't finish in 10 seconds", sem.take(TEN_SECONDS));
    assertEquals(cookie, cb.getCookie());
  }

  public void testCompletedCrawlUpdatesLastCrawlTime() {
    MockAuState maus = new MockAuState();
    long lastCrawlTime = maus.getLastCrawlTime();
    nodeManager.setAuState(maus);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    TestCrawlCB cb = new TestCrawlCB(sem);
    crawlManager.isCrawlingAU(mau, cb, null);

    assertTrue("Crawl didn't finish in 10 seconds", sem.take(TEN_SECONDS));
    assertNotEquals(lastCrawlTime, maus.getLastCrawlTime());
  }

  public void testCompletedCrawlDoesntUpdateLastCrawlTimeIfExceptionThrown() {
    MockAuState maus = new MockAuState();
    long lastCrawlTime = maus.getLastCrawlTime();
    nodeManager.setAuState(maus);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(startUrl, new IOException("Test exception"));

    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    TestCrawlCB cb = new TestCrawlCB(sem);
    crawlManager.isCrawlingAU(mau, cb, null);

    assertTrue("Crawl didn't finish in 10 seconds", sem.take(TEN_SECONDS));
    assertEquals(lastCrawlTime, maus.getLastCrawlTime());
  }

  public void testCompletedCrawlUpdatesLastCrawlTimeIfFNFExceptionThrown() {
    MockAuState maus = new MockAuState();
    long lastCrawlTime = maus.getLastCrawlTime();
    nodeManager.setAuState(maus);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(startUrl, new FileNotFoundException("Test exception"));

    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    TestCrawlCB cb = new TestCrawlCB(sem);
    crawlManager.isCrawlingAU(mau, cb, null);

    assertTrue("Crawl didn't finish in 10 seconds", sem.take(TEN_SECONDS));
    assertNotEquals(lastCrawlTime, maus.getLastCrawlTime());
  }


  public void testCompletedCrawlNoted() {
    MockAuState maus = new MockAuState();
    long lastCrawlTime = maus.getLastCrawlTime();
    nodeManager.setAuState(maus);

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    TestCrawlCB cb = new TestCrawlCB(sem);
    crawlManager.isCrawlingAU(mau, cb, null);

    assertTrue("Crawl didn't finish in 10 seconds", sem.take(TEN_SECONDS));
    mau.setShouldCrawlForNewContent(false);

    //crawl has finished and we shouldn't crawl for new content
    assertFalse(crawlManager.isCrawlingAU(mau, null, null));
  }


  public void testDontScheduleTwoCrawlsOnAU() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

    TestCrawlCB cb = new TestCrawlCB(sem);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();

    cus.addUrl(LINKLESS_PAGE, startUrl);

    WaitOnSemaphoreCallback pauseCB = new WaitOnSemaphoreCallback(sem);
    mau.setPauseCallback(pauseCB);

    assertTrue(crawlManager.isCrawlingAU(mau, null, null));
    assertTrue(crawlManager.isCrawlingAU(mau, cb, null));

    //if the callback was triggered, the second crawl completed
    //XXX meant to check that a second crawl isn't schedule, bogus
    assertFalse("Callback was triggered", cb.wasTriggered());
  }

  private class TestCrawlCB implements CrawlManager.Callback {
    SimpleBinarySemaphore sem;
    boolean called = false;
    Object cookie;

    public TestCrawlCB() {
      this(null);
    }

    public TestCrawlCB(SimpleBinarySemaphore sem) {
      this.sem = sem;
    }

    public void signalCrawlAttemptCompleted(boolean success,
					    Object cookie) {
      called = true;
      this.cookie = cookie;
      if (sem != null) {
	sem.give();
      }
    }

    public void signalCrawlSuspended(Object cookie) {
      this.cookie = cookie;
    }

    public Object getCookie() {
      return cookie;
    }

    public boolean wasTriggered() {
      return called;
    }
  }

  /**
   * Callback to wait on a semaphore before returning.
   */
  private class WaitOnSemaphoreCallback implements MockObjectCallback {
    SimpleBinarySemaphore sem;

    public WaitOnSemaphoreCallback(SimpleBinarySemaphore sem) {
      this.sem = sem;
    }

    public void callback() {
      sem.take();
    }
  }
}
