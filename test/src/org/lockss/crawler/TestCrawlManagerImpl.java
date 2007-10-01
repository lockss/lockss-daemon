/*
 * $Id: TestCrawlManagerImpl.java,v 1.71 2007-10-01 08:22:21 tlipkis Exp $
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
import junit.framework.Test;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;

/**
 * Test class for CrawlManagerImpl.
 */
public class TestCrawlManagerImpl extends LockssTestCase {
  public static final String GENERIC_URL = "http://www.example.com/index.html";
  protected TestableCrawlManagerImpl crawlManager = null;
  protected MockArchivalUnit mau = null;
  protected MockLockssDaemon theDaemon;
  protected MockNodeManager nodeManager;
  protected MockActivityRegulator activityRegulator;
  protected MockCrawler crawler;
  protected CachedUrlSet cus;
  protected CrawlSpec crawlSpec;
  protected CrawlManager.StatusSource statusSource;
  protected MyPluginManager pluginMgr;
  protected MockAuState maus;
  protected Plugin plugin;
  protected Properties cprops = new Properties();
  protected List semsToGive;

  public void setUp() throws Exception {
    super.setUp();
    semsToGive = new ArrayList();
    // some tests start the service, but most don't want the crawl starter
    // to run.
    cprops.put(CrawlManagerImpl.PARAM_START_CRAWLS_INTERVAL, "0");

    plugin = new MockPlugin(getMockLockssDaemon());
    crawlSpec = new SpiderCrawlSpec("blah", new MockCrawlRule());

    crawlManager = new TestableCrawlManagerImpl();
    statusSource = (CrawlManager.StatusSource)crawlManager;
    theDaemon = getMockLockssDaemon();
    nodeManager = new MockNodeManager();

    pluginMgr = new MyPluginManager();
    pluginMgr.initService(theDaemon);
    theDaemon.setPluginManager(pluginMgr);

    crawlManager.initService(theDaemon);

  }

  void setUpMockAu() {
    mau = new MockArchivalUnit();
    mau.setPlugin(plugin);
    mau.setCrawlSpec(crawlSpec);
    theDaemon.setNodeManager(nodeManager, mau);

    PluginTestUtil.registerArchivalUnit(plugin, mau);
    activityRegulator = new MyMockActivityRegulator(mau);
    activityRegulator.initService(theDaemon);
    theDaemon.setActivityRegulator(activityRegulator, mau);
    theDaemon.setLockssRepository(new MockLockssRepository(), mau);


    cus = mau.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(GENERIC_URL));
    activityRegulator.setStartCusActivity(cus, true);
    activityRegulator.setStartAuActivity(true);
    crawler = new MockCrawler();
    crawlManager.setTestCrawler(crawler);

    maus = new MockAuState();
    nodeManager.setAuState(maus);
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    for (Iterator iter = semsToGive.iterator(); iter.hasNext(); ) {
      SimpleBinarySemaphore sem = (SimpleBinarySemaphore)iter.next();
      sem.give();
    }
    nodeManager.stopService();
    crawlManager.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  MockArchivalUnit newMockArchivalUnit(String auid) {
    MockArchivalUnit mau = new MockArchivalUnit(plugin, auid);
    MockNodeManager nodeMgr = new MockNodeManager();
    theDaemon.setNodeManager(nodeMgr, mau);
    return mau;
  }

  SimpleBinarySemaphore semToGive(SimpleBinarySemaphore sem) {
    semsToGive.add(sem);
    return sem;
  }

  String didntMsg(String what, long time) {
    return "Crawl didn't " + what + " in " +
      StringUtil.timeIntervalToString(time);
  }

  protected void waitForCrawlToFinish(SimpleBinarySemaphore sem) {
    if (!sem.take(TIMEOUT_SHOULDNT)) {
      DebugUtils.getInstance().threadDump(true);
      TimerUtil.guaranteedSleep(1000);
      fail(didntMsg("finish", TIMEOUT_SHOULDNT));
    }
  }

  public static class TestsWithAutoStart extends TestCrawlManagerImpl {
    public void setUp() throws Exception {
      super.setUp();
      setUpMockAu();
      theDaemon.setAusStarted(true);
      cprops.put(CrawlManagerImpl.PARAM_USE_ODC, "false");
      ConfigurationUtil.addFromProps(cprops);
      crawlManager.startService();
    }

    public void testNullAuForIsCrawlingAu() {
      try {
	TestCrawlCB cb = new TestCrawlCB(new SimpleBinarySemaphore());
	crawlManager.startNewContentCrawl(null, cb, "blah", null);
	fail("Didn't throw an IllegalArgumentException on a null AU");
      } catch (IllegalArgumentException iae) {
      }
    }

    //   public void testNewCrawlDeadlineIsMax() {
    //     SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

    //     crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);

    //     waitForCrawlToFinish(sem);
    //     assertEquals(Deadline.MAX, crawler.getDeadline());
    //   }

    public void testStoppingCrawlAbortsNewContentCrawl() throws Exception {
      SimpleBinarySemaphore finishedSem = new SimpleBinarySemaphore();
      //start the crawler, but use a crawler that will hang

      TestCrawlCB cb = new TestCrawlCB(finishedSem);
      SimpleBinarySemaphore sem1 = new SimpleBinarySemaphore();
      SimpleBinarySemaphore sem2 = new SimpleBinarySemaphore();
      //gives sem1 when doCrawl is entered, then takes sem2
      MockCrawler crawler =
	new HangingCrawler("testStoppingCrawlAbortsNewContentCrawl",
			   sem1, sem2);
      semToGive(sem2);
      crawlManager.setTestCrawler(crawler);

      assertFalse(sem1.take(0));
      crawlManager.startNewContentCrawl(mau, cb, null, null);
      assertTrue(didntMsg("start", TIMEOUT_SHOULDNT),
		 sem1.take(TIMEOUT_SHOULDNT));
      //we know that doCrawl started

      // stopping AU should run AuEventHandler, which should cancel crawl
      pluginMgr.stopAu(mau);

      assertTrue(crawler.wasAborted());
    }

    public void testStoppingCrawlDoesntAbortCompletedCrawl() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

      crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);

      waitForCrawlToFinish(sem);
      assertTrue("doCrawl() not called", crawler.doCrawlCalled());

      crawlManager.cancelAuCrawls(mau);

      assertFalse(crawler.wasAborted());
    }

    public void testStoppingCrawlAbortsRepairCrawl() {
      String url1 = "http://www.example.com/index1.html";
      String url2 = "http://www.example.com/index2.html";
      List urls = ListUtil.list(url1, url2);

      SimpleBinarySemaphore finishedSem = new SimpleBinarySemaphore();
      //start the crawler, but use a crawler that will hang

      TestCrawlCB cb = new TestCrawlCB(finishedSem);
      SimpleBinarySemaphore sem1 = new SimpleBinarySemaphore();
      SimpleBinarySemaphore sem2 = new SimpleBinarySemaphore();
      //gives sem1 when doCrawl is entered, then takes sem2
      MockCrawler crawler =
	new HangingCrawler("testStoppingCrawlAbortsRepairCrawl",
			   sem1, sem2);
      semToGive(sem2);
      crawlManager.setTestCrawler(crawler);

      crawlManager.startRepair(mau, urls, cb, null, null);
      assertTrue(didntMsg("start", TIMEOUT_SHOULDNT),
		 sem1.take(TIMEOUT_SHOULDNT));
      //we know that doCrawl started

      crawlManager.cancelAuCrawls(mau);

      assertTrue(crawler.wasAborted());
    }


    private void setNewContentRateLimit(String rate, String startRate) {
      cprops.put(CrawlManagerImpl.PARAM_MAX_NEW_CONTENT_RATE, rate);
      cprops.put(CrawlManagerImpl.PARAM_NEW_CONTENT_START_RATE, startRate);
      ConfigurationUtil.addFromProps(cprops);
    }

    private void setRepairRateLimit(int events, long interval) {
      cprops.put(CrawlManagerImpl.PARAM_MAX_REPAIR_RATE, events+"/"+interval);
      ConfigurationUtil.addFromProps(cprops);
    }

    private void assertDoesCrawlNew(MockCrawler crawler) {
      crawler.setDoCrawlCalled(false);
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);
      waitForCrawlToFinish(sem);
      assertTrue("doCrawl() not called at time " + TimeBase.nowMs(),
		 crawler.doCrawlCalled());
    }

    private void assertDoesCrawlNew() {
      assertDoesCrawlNew(crawler);
    }

    private void assertDoesNotCrawlNew() {
      crawler.setDoCrawlCalled(false);
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);
      waitForCrawlToFinish(sem);
      assertFalse("doCrawl() called at time " + TimeBase.nowMs(),
		  crawler.doCrawlCalled());
    }

    private void assertDoesCrawlRepair() {
      crawler.setDoCrawlCalled(false);
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      crawlManager.startRepair(mau, ListUtil.list(GENERIC_URL),
			       new TestCrawlCB(sem), null, null);
      waitForCrawlToFinish(sem);
      assertTrue("doCrawl() not called at time " + TimeBase.nowMs(),
		 crawler.doCrawlCalled());
    }

    private void assertDoesNotCrawlRepair() {
      crawler.setDoCrawlCalled(false);
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      crawlManager.startRepair(mau, ListUtil.list(GENERIC_URL),
			       new TestCrawlCB(sem), null, null);
      waitForCrawlToFinish(sem);
      assertFalse("doCrawl() called at time " + TimeBase.nowMs(),
		  crawler.doCrawlCalled());
    }

    public void testDoesNCCrawl() {
      assertDoesCrawlNew();
    }

    public void testDoesntNCCrawlWhenCantGetLock() {
      activityRegulator.setStartAuActivity(false);
      assertDoesNotCrawlNew();
    }

    public void testDoesntNCCrawlWhenOutsideWindow() {
      crawlSpec.setCrawlWindow(new ClosedCrawlWindow());
      assertDoesNotCrawlNew();
    }

    public void testNewContentRateLimiter() {
      TimeBase.setSimulated(100);
      setNewContentRateLimit("4/100", "unlimited");
      activityRegulator.setStartAuActivity(true);
      for (int ix = 1; ix <= 4; ix++) {
	assertDoesCrawlNew();
	TimeBase.step(10);
      }
      assertDoesNotCrawlNew();
      TimeBase.step(10);
      assertDoesNotCrawlNew();
      TimeBase.step(50);
      assertDoesCrawlNew();
      TimeBase.step(500);
      // ensure RateLimiter changes when config does
      setNewContentRateLimit("2/5", "unlimited");
      assertDoesCrawlNew();
      assertDoesCrawlNew();
      assertDoesNotCrawlNew();
      TimeBase.step(5);
      assertDoesCrawlNew();
      assertDoesCrawlNew();
    }

    public void testNewContentRateLimiterWindowClose() {
      TimeBase.setSimulated(100);
      CrawlSpec cspec2 = new SpiderCrawlSpec("two", new MockCrawlRule());
      mau.setCrawlSpec(cspec2);
      setNewContentRateLimit("1/10", "unlimited");
      assertDoesCrawlNew();
      assertDoesNotCrawlNew();
      TimeBase.step(10);
      assertDoesCrawlNew();
      assertDoesNotCrawlNew();
      TimeBase.step(10);

      MockCrawler c2 = new CloseWindowCrawler();
      crawlManager.setTestCrawler(mau, c2);
      assertDoesCrawlNew(c2);
      cspec2.setCrawlWindow(null);
      crawlManager.setTestCrawler(mau, null);
      assertDoesCrawlNew();
    }

    public void testRepairRateLimiter() {
      TimeBase.setSimulated(100);
      setRepairRateLimit(6, 200);
      activityRegulator.setStartAuActivity(true);
      for (int ix = 1; ix <= 6; ix++) {
	assertDoesCrawlRepair();
	TimeBase.step(10);
      }
      assertDoesNotCrawlRepair();
      TimeBase.step(10);
      assertDoesNotCrawlRepair();
      TimeBase.step(130);
      assertDoesCrawlRepair();
      TimeBase.step(500);
      // ensure RateLimiter changes when config does
      setRepairRateLimit(2, 5);
      assertDoesCrawlRepair();
      assertDoesCrawlRepair();
      assertDoesNotCrawlRepair();
      TimeBase.step(5);
      assertDoesCrawlRepair();
    }

    public void testRateLimitedNewContentCrawlDoesntGrabLocks() {
      TimeBase.setSimulated(100);
      setNewContentRateLimit("1/200", "unlimited");
      assertDoesCrawlNew();
      activityRegulator.resetLastActivityLock();
      assertDoesNotCrawlNew();
      assertEquals(null, activityRegulator.getLastActivityLock());
    }

    public void testRateLimitedRepairCrawlDoesntGrabLocks() {
      TimeBase.setSimulated(100);
      setRepairRateLimit(1, 200);
      assertDoesCrawlRepair();
      activityRegulator.resetLastActivityLock();
      assertDoesNotCrawlRepair();
      assertEquals(null, activityRegulator.getLastActivityLock());
    }

    public void testBasicNewContentCrawl() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

      crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);

      waitForCrawlToFinish(sem);
      assertTrue("doCrawl() not called", crawler.doCrawlCalled());
    }

    public void testNCCrawlFreesActivityLockWhenDone() {
      activityRegulator.resetLastActivityLock();
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

      crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);
      assertNotNull(activityRegulator.getLastActivityLock());

      waitForCrawlToFinish(sem);
      activityRegulator.assertNewContentCrawlFinished();
    }

    public void testNCCrawlFreesActivityLockWhenError() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      crawler = new ThrowingCrawler(new RuntimeException("Blah"));
      crawlManager.setTestCrawler(crawler);

      crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);

      waitForCrawlToFinish(sem);
      activityRegulator.assertNewContentCrawlFinished();
    }

    //If the AU throws, the crawler should trap it and call the callbacks
    public void testCallbacksCalledWhenPassedThrowingAU() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      TestCrawlCB cb = new TestCrawlCB(sem);
      ThrowingAU au = new ThrowingAU();
      theDaemon.setActivityRegulator(activityRegulator, au);
      crawlManager.startNewContentCrawl(au, cb, null, null);
      assertTrue(cb.wasTriggered());
    }

    public void testRepairCrawlFreesActivityLockWhenDone() {
      String url1 = "http://www.example.com/index1.html";
      String url2 = "http://www.example.com/index2.html";
      List urls = ListUtil.list(url1, url2);

      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      TestCrawlCB cb = new TestCrawlCB(sem);
      CachedUrlSet cus1 =
	mau.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(url1));
      CachedUrlSet cus2 =
	mau.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(url2));

      activityRegulator.setStartCusActivity(cus1, true);
      activityRegulator.setStartCusActivity(cus2, true);

      crawlManager.startRepair(mau, urls, cb, null, null);

      waitForCrawlToFinish(sem);
      activityRegulator.assertRepairCrawlFinished(cus1);
      activityRegulator.assertRepairCrawlFinished(cus2);
    }


    public void testNewContentCallbackTriggered() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

      TestCrawlCB cb = new TestCrawlCB(sem);
      crawlManager.startNewContentCrawl(mau, cb, null, null);

      waitForCrawlToFinish(sem);
      assertTrue("Callback wasn't triggered", cb.wasTriggered());
    }


    public void testNewContentCrawlCallbackReturnsCookie() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

      String cookie = "cookie string";

      TestCrawlCB cb = new TestCrawlCB(sem);
      crawlManager.startNewContentCrawl(mau, cb, cookie, null);

      waitForCrawlToFinish(sem);
      assertEquals(cookie, (String)cb.getCookie());
    }

    public void testNewContentCrawlCallbackReturnsNullCookie() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      String cookie = null;

      TestCrawlCB cb = new TestCrawlCB(sem);
      crawlManager.startNewContentCrawl(mau, cb, cookie, null);

      waitForCrawlToFinish(sem);
      assertEquals(cookie, (String)cb.getCookie());
    }

    public void testKicksOffNewThread() {
      SimpleBinarySemaphore finishedSem = new SimpleBinarySemaphore();
      //start the crawler, but use a crawler that will hang
      //if we aren't running it in another thread we'll hang too

      TestCrawlCB cb = new TestCrawlCB(finishedSem);
      SimpleBinarySemaphore sem1 = new SimpleBinarySemaphore();
      SimpleBinarySemaphore sem2 = new SimpleBinarySemaphore();
      //gives sem1 when doCrawl is entered, then takes sem2
      MockCrawler crawler = new HangingCrawler("testKicksOffNewThread",
					       sem1, sem2);
      semToGive(sem2);
      crawlManager.setTestCrawler(crawler);

      crawlManager.startNewContentCrawl(mau, cb, null, null);
      assertTrue(didntMsg("start", TIMEOUT_SHOULDNT),
		 sem1.take(TIMEOUT_SHOULDNT));
      //we know that doCrawl started

      //if the callback was triggered, the crawl completed
      assertFalse("Callback was triggered", cb.wasTriggered());
      sem2.give();
      waitForCrawlToFinish(finishedSem);
    }

    public void testScheduleRepairNullAu() {
      try{
	crawlManager.startRepair(null, ListUtil.list("http://www.example.com"),
				 new TestCrawlCB(), "blah", null);
	fail("Didn't throw IllegalArgumentException on null AU");
      } catch (IllegalArgumentException iae) {
      }
    }

    public void testScheduleRepairNullUrls() {
      try{
	crawlManager.startRepair(mau, (Collection)null,
				 new TestCrawlCB(), "blah", null);
	fail("Didn't throw IllegalArgumentException on null URL list");
      } catch (IllegalArgumentException iae) {
      }
    }

    public void testNoRepairIfNotAllowed() {
      String url1 = "http://www.example.com/index1.html";
      String url2 = "http://www.example.com/index2.html";
      Set urls = SetUtil.set(url1, url2);

      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      TestCrawlCB cb = new TestCrawlCB(sem);
      CachedUrlSet cus1 =
	mau.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(url1));
      CachedUrlSet cus2 =
	mau.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(url2));

      activityRegulator.setStartCusActivity(cus1, true);
      activityRegulator.setStartCusActivity(cus2, false);

      crawlManager.startRepair(mau, urls, cb, null, null);

      waitForCrawlToFinish(sem);
      assertTrue("doCrawl() not called", crawler.doCrawlCalled());
      assertIsomorphic(SetUtil.set(url1), crawler.getStartUrls());
    }

    public void testBasicRepairCrawl() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      TestCrawlCB cb = new TestCrawlCB(sem);

      crawlManager.startRepair(mau, ListUtil.list(GENERIC_URL), cb, null, null);

      waitForCrawlToFinish(sem);
      assertTrue("doCrawl() not called", crawler.doCrawlCalled());
    }

    public void testRepairCallbackTriggered() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      TestCrawlCB cb = new TestCrawlCB(sem);

      crawlManager.startRepair(mau, ListUtil.list(GENERIC_URL), cb, null, null);

      waitForCrawlToFinish(sem);
      assertTrue("Callback wasn't triggered", cb.wasTriggered());
    }

    public void testRepairCrawlUnsuccessfulIfCantGetAllLocks() {
      String url1 = "http://www.example.com/index1.html";
      String url2 = "http://www.example.com/index2.html";
      List urls = ListUtil.list(url1, url2);

      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      TestCrawlCB cb = new TestCrawlCB(sem);
      CachedUrlSet cus1 =
	mau.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(url1));
      CachedUrlSet cus2 =
	mau.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(url2));

      activityRegulator.setStartCusActivity(cus1, true);
      activityRegulator.setStartCusActivity(cus2, false);

      crawlManager.startRepair(mau, urls, cb, null, null);

      waitForCrawlToFinish(sem);
      assertTrue("Callback wasn't triggered", cb.wasTriggered());
      assertFalse("Crawl was successful even though we couldn't lock everything",
		  cb.wasSuccessful());
    }

    public void testRepairCallbackGetsCookie() {
      String cookie = "test cookie str";
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      TestCrawlCB cb = new TestCrawlCB(sem);

      crawlManager.startRepair(mau, ListUtil.list(GENERIC_URL), cb, cookie, null);

      waitForCrawlToFinish(sem);
      assertEquals(cookie, cb.getCookie());
    }

    //StatusSource tests

    public void testGetCrawlStatusListReturnsEmptyListIfNoCrawls() {
      List list = statusSource.getStatus().getCrawlerStatusList();
      assertEquals(0, list.size());
    }


    public void testGetCrawlsOneRepairCrawl() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      crawlManager.startRepair(mau, ListUtil.list(GENERIC_URL),
			       new TestCrawlCB(sem), null, null);
      List actual = statusSource.getStatus().getCrawlerStatusList();
      List expected = ListUtil.list(crawler.getStatus());

      assertEquals(expected, actual);
      waitForCrawlToFinish(sem);
    }

    public void testGetCrawlsOneNCCrawl() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);
      List actual = statusSource.getStatus().getCrawlerStatusList();
      List expected = ListUtil.list(crawler.getStatus());
      assertEquals(expected, actual);
      waitForCrawlToFinish(sem);
    }

    public void testGetCrawlsMulti() {
      SimpleBinarySemaphore sem1 = new SimpleBinarySemaphore();
      SimpleBinarySemaphore sem2 = new SimpleBinarySemaphore();
      crawlManager.startRepair(mau, ListUtil.list(GENERIC_URL),
			       new TestCrawlCB(sem1), null, null);

      MockCrawler crawler2 = new MockCrawler();
      crawlManager.setTestCrawler(crawler2);
      crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem2), null, null);

      List actual = statusSource.getStatus().getCrawlerStatusList();
      List expected = ListUtil.list(crawler.getStatus(), crawler2.getStatus());
      assertEquals(expected, actual);
      waitForCrawlToFinish(sem1);
      waitForCrawlToFinish(sem2);
    }

  }

  public static class TestsWithoutAutoStart extends TestCrawlManagerImpl {

    public void setUp() throws Exception {
      super.setUp();
    }

    public void testNoQueuePoolSize(int max) {
      setUpMockAu();
      cprops.put(CrawlManagerImpl.PARAM_CRAWLER_QUEUE_ENABLED, "false");
      cprops.put(CrawlManagerImpl.PARAM_USE_ODC, "false");
      cprops.put(CrawlManagerImpl.PARAM_CRAWLER_THREAD_POOL_MAX,
		 Integer.toString(max));
      cprops.put(CrawlManagerImpl.PARAM_MAX_NEW_CONTENT_RATE, (max*2)+"/1h");
      cprops.put(CrawlManagerImpl.PARAM_NEW_CONTENT_START_RATE, "unlimited");
      ConfigurationUtil.addFromProps(cprops);
      crawlManager.startService();
      SimpleBinarySemaphore[] startSem = new SimpleBinarySemaphore[max];
      SimpleBinarySemaphore[] endSem = new SimpleBinarySemaphore[max];
      SimpleBinarySemaphore[] finishedSem = new SimpleBinarySemaphore[max];
      HangingCrawler[] crawler = new HangingCrawler[max];
      TestCrawlCB[] cb = new TestCrawlCB[max];
      for (int ix = 0; ix < max; ix++) {
        finishedSem[ix] = new SimpleBinarySemaphore();
        //start a crawler that hangs until we post its semaphore
        cb[ix] = new TestCrawlCB(finishedSem[ix]);
        startSem[ix] = new SimpleBinarySemaphore();
        endSem[ix] = new SimpleBinarySemaphore();
        //gives sem1 when doCrawl is entered, then takes sem2
	crawler[ix] = new HangingCrawler("testNoQueuePoolSize " + ix,
						 startSem[ix], endSem[ix]);
	MockArchivalUnit au = newMockArchivalUnit("mau" + ix);
	au.setCrawlSpec(crawlSpec);
	PluginTestUtil.registerArchivalUnit(plugin, au);
	setupAuToCrawl(au, crawler[ix]);
	semToGive(endSem[ix]);

	crawlManager.startNewContentCrawl(au, cb[ix], null, null);
      }
      for (int ix = 0; ix < max; ix++) {
        assertTrue(didntMsg("start("+ix+")", TIMEOUT_SHOULDNT),
		   startSem[ix].take(TIMEOUT_SHOULDNT));
        //we know that doCrawl started
      }
      MockCrawler crawlerN =
	new HangingCrawler("testNoQueuePoolSize ix+1",
			   new SimpleBinarySemaphore(),
			   semToGive(new SimpleBinarySemaphore()));
      crawlManager.setTestCrawler(crawlerN);
      TestCrawlCB onecb = new TestCrawlCB();
      crawlManager.startNewContentCrawl(mau, onecb, null, null);
      assertTrue("Callback for non schedulable crawl wasn't triggered",
		 onecb.wasTriggered());
      assertFalse("Non schedulable crawl succeeded",
		 onecb.wasSuccessful());

      for (int ix = 0; ix < max; ix++) {
        //if the callback was triggered, the crawl completed
	assertFalse("Callback was triggered", cb[ix].wasTriggered());
        endSem[ix].give();
        waitForCrawlToFinish(finishedSem[ix]);
      }
    }

    public void testNoQueueBoundedPool() {
      testNoQueuePoolSize(5);
    }

    public void testQueuedPool(int qMax, int poolMax) {
      setUpMockAu();
      int tot = qMax + poolMax;
      cprops.put(CrawlManagerImpl.PARAM_CRAWLER_QUEUE_ENABLED, "true");
      cprops.put(CrawlManagerImpl.PARAM_USE_ODC, "false");
      cprops.put(CrawlManagerImpl.PARAM_CRAWLER_THREAD_POOL_MAX,
		 Integer.toString(poolMax));
      cprops.put(CrawlManagerImpl.PARAM_CRAWLER_THREAD_POOL_QUEUE_SIZE,
		 Integer.toString(qMax));
      cprops.put(CrawlManagerImpl.PARAM_MAX_NEW_CONTENT_RATE, (tot*2)+"/1h");
      int startInterval = 10;
      cprops.put(CrawlManagerImpl.PARAM_NEW_CONTENT_START_RATE,
		 "1/" + startInterval);
      ConfigurationUtil.addFromProps(cprops);
      crawlManager.startService();
      HangingCrawler[] crawler = new HangingCrawler[tot];
      SimpleBinarySemaphore[] startSem = new SimpleBinarySemaphore[tot];
      SimpleBinarySemaphore[] endSem = new SimpleBinarySemaphore[tot];
      SimpleBinarySemaphore[] finishedSem = new SimpleBinarySemaphore[tot];
      TestCrawlCB[] cb = new TestCrawlCB[tot];
      long startTime[] = new long[tot];

      // queue enough crawlers to fill the queue and pool
      for (int ix = 0; ix < tot; ix++) {
	finishedSem[ix] = new SimpleBinarySemaphore();
	//start a crawler that hangs until we post its semaphore
	cb[ix] = new TestCrawlCB(finishedSem[ix]);
	startSem[ix] = new SimpleBinarySemaphore();
	endSem[ix] = new SimpleBinarySemaphore();
	//gives sem1 when doCrawl is entered, then takes sem2
	crawler[ix] = new HangingCrawler("testQueuedPool " + ix,
					 startSem[ix], endSem[ix]);
	MockArchivalUnit au = newMockArchivalUnit("mau" + ix);
	au.setCrawlSpec(crawlSpec);
	PluginTestUtil.registerArchivalUnit(plugin, au);
	setupAuToCrawl(au, crawler[ix]);
	semToGive(endSem[ix]);

	// queue the crawl directly
	crawlManager.startNewContentCrawl(au, cb[ix], null, null);
      }
      // wait for the first poolMax crawlers to start.  Keep track of their
      // start times
      for (int ix = 0; ix < tot; ix++) {
	if (ix < poolMax) {
	  assertTrue(didntMsg("start("+ix+")", TIMEOUT_SHOULDNT),
		     startSem[ix].take(TIMEOUT_SHOULDNT));
	  startSem[ix] = null;

	  startTime[ix] = crawler[ix].getStartTime();
	} else {
	  assertFalse("Wasn't queued "+ix, cb[ix].wasTriggered());
	  assertFalse("Shouldn't have started " + ix, 
		     startSem[ix].take(0));
	}
      }
      // now check that no two start times are closer together than allowed
      // by the start rate limiter.  We don't know what order the crawl
      // threads ran in, so must sort the times first.  Thefirst qMax will
      // be zero because only poolMax actually got started.
      Arrays.sort(startTime);
      long lastTime = 0;
      for (int ix = qMax; ix < tot; ix++) {
	long thisTime =  startTime[ix];
	assertTrue(  "Crawler " + ix + " started early in " +
		     (thisTime - lastTime),
		     thisTime >= lastTime + startInterval);
	lastTime = thisTime;
      }
      MockCrawler failcrawler =
	new HangingCrawler("testNoQueuePoolSize ix+1",
			   new SimpleBinarySemaphore(),
			   semToGive(new SimpleBinarySemaphore()));
      crawlManager.setTestCrawler(failcrawler);
      TestCrawlCB onecb = new TestCrawlCB();
      crawlManager.startNewContentCrawl(mau, onecb, null, null);
      assertTrue("Callback for non schedulable crawl wasn't triggered",
		 onecb.wasTriggered());
      assertFalse("Non schedulable crawl succeeded",
		 onecb.wasSuccessful());

      for (int ix = poolMax; ix < tot; ix++) {
	int poke = randomActive(startSem, endSem);
	assertFalse("Shouldnt have finished "+poke, cb[poke].wasTriggered());
	endSem[poke].give();
	waitForCrawlToFinish(finishedSem[poke]);
	endSem[poke] = null;
	assertTrue(didntMsg("start("+ix+")", TIMEOUT_SHOULDNT),
		   startSem[ix].take(TIMEOUT_SHOULDNT));
	startSem[ix] = null;
      }
    }

    public void testCrawlStarter(int qMax, int poolMax) {
      assertEmpty(pluginMgr.getAllAus());
      int tot = qMax + poolMax;
      Properties p = new Properties();
      p.put(CrawlManagerImpl.PARAM_CRAWLER_QUEUE_ENABLED,
	    "true");
      p.put(CrawlManagerImpl.PARAM_USE_ODC, "false");
      p.put(CrawlManagerImpl.PARAM_CRAWLER_THREAD_POOL_MAX,
	    Integer.toString(poolMax));
      p.put(CrawlManagerImpl.PARAM_CRAWLER_THREAD_POOL_QUEUE_SIZE,
	    Integer.toString(qMax));
      p.put(CrawlManagerImpl.PARAM_MAX_NEW_CONTENT_RATE, (tot*2)+"/1h");
      p.put(CrawlManagerImpl.PARAM_START_CRAWLS_INITIAL_DELAY, "100");
      p.put(CrawlManagerImpl.PARAM_START_CRAWLS_INTERVAL, "1s");
      theDaemon.setAusStarted(true);
      ConfigurationUtil.addFromProps(p);
      crawlManager.ausStartedSem = new SimpleBinarySemaphore();
      crawlManager.startService();
      HangingCrawler[] crawler = new HangingCrawler[tot];
      SimpleBinarySemaphore endSem = new SimpleBinarySemaphore();
      semToGive(endSem);
      crawlManager.recordExecute(true);
      assertEmpty(pluginMgr.getAllAus());
      // Create AUs and build the pieces necessary for the crawl starter to
      // get the list of AUs from the PluginManager and a NodeManager and
      // Activityregulator for each.  The crawl starter should then shortly
      // start poolMax of them.  We only check that it called
      // startNewContentCrawl() on each.
      for (int ix = 0; ix < tot-1; ix++) {
	crawler[ix] = new HangingCrawler("testQueuedPool " + ix,
					 null, endSem);
	MockArchivalUnit au = newMockArchivalUnit("mau" + ix);
	au.setCrawlSpec(crawlSpec);
	setupAuToCrawl(au, crawler[ix]);
	PluginTestUtil.registerArchivalUnit(plugin, au);
      }
      // Last one is a RegistryArchivalUnit crawl
      RegistryArchivalUnit rau = makeRegistryAu();
      crawler[tot-1] = new HangingCrawler("testQueuedPool(reg au) " + (tot-1),
					 null, endSem);
      setupAuToCrawl(rau, crawler[tot-1]);
      pluginMgr.addRegistryAu(rau);
      
      // now let the crawl starter proceed
      crawlManager.ausStartedSem.give();
      // Ensure they all got queued
      List exe = Collections.EMPTY_LIST;
      Interrupter intr = interruptMeIn(TIMEOUT_SHOULDNT, true);
      while (true) {
	exe = crawlManager.getExecuted();
	if (exe.size() == tot) {
	  break;
	}
	crawlManager.waitExecuted();
	assertFalse("Only " + exe.size() + " of " + tot +
		    " expected crawls were started by the crawl starter",
		    intr.did());
      }
      intr.cancel();
      assertEquals(SetUtil.fromArray(crawler),
		   SetUtil.theSet(crawlersOf(exe)));
      // The registry au crawl should always be first
      assertEquals(crawler[tot-1], crawlersOf(exe).get(0));
    }

    void setupAuToCrawl(ArchivalUnit au, MockCrawler crawler) {
      MockActivityRegulator act = new MockActivityRegulator(au);
      act.setStartAuActivity(true);
      theDaemon.setActivityRegulator(act, au);
      crawlManager.setTestCrawler(au, crawler);
    }

    List crawlersOf(List runners) {
      List res = new ArrayList();
      for (Iterator iter = runners.iterator(); iter.hasNext(); ) {
	CrawlManagerImpl.CrawlRunner runner =
	  (CrawlManagerImpl.CrawlRunner)iter.next();
	res.add(runner.getCrawler());
      }
      return res;
    }

    static LockssRandom random = new LockssRandom();

    int randomActive(SimpleBinarySemaphore[] startSem,
		     SimpleBinarySemaphore[] endSem) {
      while (true) {
	int x = random.nextInt(startSem.length);
	if (startSem[x] == null && endSem[x] != null) return x;
      }
    }

    public void testQueuedPool() {
      testQueuedPool(10, 3);
    }

    public void testCrawlStarter() {
      testCrawlStarter(10, 3);
    }


    // ODC tests

    MockArchivalUnit[] makeMockAus(int n) {
      MockArchivalUnit[] res = new MockArchivalUnit[n];
      for (int ix = 0; ix < n; ix++) {
	res[ix] = newMockArchivalUnit("mau" + ix);
      }
      return res;
    }

    CrawlReq[] makeReqs(int n) {
      CrawlReq[] res = new CrawlReq[n];
      for (int ix = 0; ix < n; ix++) {
	res[ix] = new CrawlReq(newMockArchivalUnit("mau" + ix));
      }
      return res;
    }

    void setReq(CrawlReq req,
		int pri, int crawlResult,
		long crawlAttempt, long crawlFinish) {
      setReq(req, pri, crawlResult, crawlAttempt, crawlFinish, null);
    }

    void setReq(CrawlReq req,
		int pri, int crawlResult,
		long crawlAttempt, long crawlFinish,
		String limiterKey) {
      req.priority = pri;
      setAu((MockArchivalUnit)req.au,
	    crawlResult, crawlAttempt, crawlFinish, limiterKey);
    }

    void setAu(MockArchivalUnit mau,
	       int crawlResult, long crawlAttempt, long crawlFinish) {
       setAu(mau, crawlResult, crawlAttempt, crawlFinish, null);
    }

    void setAu(MockArchivalUnit mau,
	       int crawlResult, long crawlAttempt, long crawlFinish,
	       String limiterKey) {
      MockAuState aus = (MockAuState)AuUtil.getAuState(mau);
      aus.setLastCrawlTime(crawlFinish);
      aus.setLastCrawlAttempt(crawlAttempt);
      aus.setLastCrawlResult(crawlResult, "foo");
      mau.setFetchRateLimiterKey(limiterKey);
    }	   

    CrawlManagerImpl.CrawlPriorityComparator cmprtr() {
      return new CrawlManagerImpl.CrawlPriorityComparator();
    }

    void assertCompareLess(CrawlReq r1, CrawlReq r2) {
      assertTrue(cmprtr().compare(r1, r2) < 0);
    }

    public void testCrawlPriorityComparator() {
      CrawlReq[] reqs = makeReqs(7);
      setReq(reqs[0], 1, Crawler.STATUS_WINDOW_CLOSED, 9999, 9999);
      setReq(reqs[1], 1, 0, 5000, 5000);
      setReq(reqs[2], 0, Crawler.STATUS_WINDOW_CLOSED, -1, 2000);
      setReq(reqs[3], 0, Crawler.STATUS_WINDOW_CLOSED, 1000, 1000);
      setReq(reqs[4], 0, 0, -1, 1000);
      setReq(reqs[5], 0, 0, 123, -1);
      setReq(reqs[6], 0, 0, 123, 456);
      for (int ix = 0; ix <= 5; ix++) {
	assertCompareLess(reqs[ix], reqs[ix+1]);
      }
      List lst = ListUtil.fromArray(reqs);
      for (int ix = 0; ix <= 5; ix++) {
	TreeSet sorted = new TreeSet(cmprtr());
	sorted.addAll(CollectionUtil.randomPermutation(lst)); 
	assertIsomorphic(reqs, sorted);
      }
    }

    void registerAus(MockArchivalUnit[] aus) {
      List lst = ListUtil.fromArray(aus);
      List<MockArchivalUnit> rand = CollectionUtil.randomPermutation(lst);
      for (MockArchivalUnit mau : rand) {
	PluginTestUtil.registerArchivalUnit(plugin, mau);
      }
    }    

    public void testOdcQueue() throws Exception {
      Properties p = new Properties();
      p.put(CrawlManagerImpl.PARAM_START_CRAWLS_INTERVAL, "-1");
      p.put(CrawlManagerImpl.PARAM_SHARED_QUEUE_MAX, "4");
      p.put(CrawlManagerImpl.PARAM_UNSHARED_QUEUE_MAX, "3");
      p.put(CrawlManagerImpl.PARAM_CRAWLER_THREAD_POOL_MAX, "3"); 
      p.put(CrawlManagerImpl.PARAM_FAVOR_SHARED_RATE_THREADS, "2"); 
      theDaemon.setAusStarted(true);
      ConfigurationUtil.addFromProps(p);
      crawlManager.startService();

      MockArchivalUnit[] aus = makeMockAus(15);
      registerAus(aus);
      MockArchivalUnit auPri = newMockArchivalUnit("auPri");
      setAu(auPri, 0, 9999, 9999);

      setAu(aus[0], Crawler.STATUS_WINDOW_CLOSED, -1, 2000);
      setAu(aus[1], Crawler.STATUS_WINDOW_CLOSED, 1000, 1000);
      setAu(aus[2], 0, -1, 1000);
      setAu(aus[3], 0, 123, -1);
      setAu(aus[4], 0, 123, 456);

      setAu(aus[5], Crawler.STATUS_WINDOW_CLOSED, -1, 2001, "foo");
      setAu(aus[6], Crawler.STATUS_WINDOW_CLOSED, 1001, 1001, "foo");
      setAu(aus[7], 0, -1, 1001, "foo");
      setAu(aus[8], 0, 124, -1, "foo");
      setAu(aus[9], 0, 124, 457, "foo");

      setAu(aus[10], Crawler.STATUS_WINDOW_CLOSED, -1, 2002, "bar");
      setAu(aus[11], Crawler.STATUS_WINDOW_CLOSED, 1002, 1002, "bar");
      setAu(aus[12], 0, -1, 1002, "bar");
      setAu(aus[13], 0, 125, -1, "bar");
      setAu(aus[14], 0, 125, 458, "bar");

      assertEquals(0, crawlManager.rebuildCount);
      assertEquals(aus[5], crawlManager.nextReq().au);
      assertEquals(1, crawlManager.rebuildCount);
      crawlManager.addToRunningRateKeys(aus[5]);
      assertEquals(aus[10], crawlManager.nextReq().au);
      crawlManager.addToRunningRateKeys(aus[10]);
      assertEquals(aus[0], crawlManager.nextReq().au);
      assertEquals(aus[1], crawlManager.nextReq().au);
      crawlManager.delFromRunningRateKeys(aus[10]);
      assertEquals(aus[11], crawlManager.nextReq().au);
      crawlManager.addToRunningRateKeys(aus[11]);
      aus[5].setShouldCrawlForNewContent(false);
      aus[10].setShouldCrawlForNewContent(false);
      aus[0].setShouldCrawlForNewContent(false);
      aus[1].setShouldCrawlForNewContent(false);
      aus[11].setShouldCrawlForNewContent(false);
      assertEquals(aus[2], crawlManager.nextReq().au);
      crawlManager.delFromRunningRateKeys(aus[5]);
      assertEquals(aus[6], crawlManager.nextReq().au);
      crawlManager.addToRunningRateKeys(aus[6]);
      aus[2].setShouldCrawlForNewContent(false);
      aus[6].setShouldCrawlForNewContent(false);
      assertEquals(1, crawlManager.rebuildCount);
      assertEquals(aus[3], crawlManager.nextReq().au);
      assertEquals(2, crawlManager.rebuildCount);
      assertEquals(aus[4], crawlManager.nextReq().au);
      aus[3].setShouldCrawlForNewContent(false);
      aus[4].setShouldCrawlForNewContent(false);
      assertEquals(null, crawlManager.nextReq());
      crawlManager.delFromRunningRateKeys(aus[11]);
      assertEquals(aus[12], crawlManager.nextReq().au);
      crawlManager.addToRunningRateKeys(aus[12]);
      assertEquals(null, crawlManager.nextReq());
      crawlManager.delFromRunningRateKeys(aus[12]);
      crawlManager.delFromRunningRateKeys(aus[6]);
      PluginTestUtil.registerArchivalUnit(plugin, auPri);
      assertEquals(aus[7], crawlManager.nextReq().au);
      crawlManager.addToRunningRateKeys(aus[7]);
      aus[12].setShouldCrawlForNewContent(false);
      aus[7].setShouldCrawlForNewContent(false);
      auPri.setShouldCrawlForNewContent(false);
      crawlManager.startNewContentCrawl(auPri, null, null, null);
      assertEquals(auPri, crawlManager.nextReq().au);
      crawlManager.addToRunningRateKeys(auPri);
      auPri.setShouldCrawlForNewContent(false);
      assertEquals(aus[13], crawlManager.nextReq().au);
      crawlManager.addToRunningRateKeys(aus[13]);
      assertEquals(null, crawlManager.nextReq());
    }

    public void testOdcCrawlStarter() {
      int num = 15;
      int tot = num+1;
      int rauix = tot-1;
      int nthreads = 3;
      Properties p = new Properties();
      p.put(CrawlManagerImpl.PARAM_USE_ODC, "true");
      p.put(CrawlManagerImpl.PARAM_CRAWLER_THREAD_POOL_MAX, ""+nthreads);
      p.put(CrawlManagerImpl.PARAM_MAX_NEW_CONTENT_RATE, "1/1w");
      p.put(CrawlManagerImpl.PARAM_NEW_CONTENT_START_RATE, (tot*2)+"/1h");
      p.put(CrawlManagerImpl.PARAM_START_CRAWLS_INITIAL_DELAY, "10");
      p.put(CrawlManagerImpl.PARAM_START_CRAWLS_INTERVAL, "10");

      p.put(CrawlManagerImpl.PARAM_SHARED_QUEUE_MAX, "10");
      p.put(CrawlManagerImpl.PARAM_UNSHARED_QUEUE_MAX, "10");
      p.put(CrawlManagerImpl.PARAM_FAVOR_SHARED_RATE_THREADS,
	    ""+(nthreads-1)); 
      theDaemon.setAusStarted(true);
      ConfigurationUtil.addFromProps(p);
      crawlManager.ausStartedSem = new SimpleBinarySemaphore();
      crawlManager.startService();

      MockArchivalUnit[] aus = makeMockAus(num);
      RegistryArchivalUnit rau = makeRegistryAu();

      setAu(aus[0], Crawler.STATUS_WINDOW_CLOSED, -1, 2000);
      setAu(aus[1], Crawler.STATUS_WINDOW_CLOSED, 1000, 1000);
      setAu(aus[2], 0, -1, -2);
      setAu(aus[3], 0, 123, -1);
      setAu(aus[4], 0, 123, 456);

      setAu(aus[5], Crawler.STATUS_WINDOW_CLOSED, -1, 2001, "foo");
      setAu(aus[6], Crawler.STATUS_WINDOW_CLOSED, 1001, 1001, "foo");
      setAu(aus[7], 0, -1, 1001, "foo");
      setAu(aus[8], 0, 124, -1, "foo");
      setAu(aus[9], 0, 124, 457, "foo");

      setAu(aus[10], Crawler.STATUS_WINDOW_CLOSED, -1, 2002, "bar");
      setAu(aus[11], Crawler.STATUS_WINDOW_CLOSED, 1002, 1002, "bar");
      setAu(aus[12], 0, -1, 1002, "bar");
      setAu(aus[13], 0, 125, -1, "bar");
      setAu(aus[14], 0, 125, 458, "bar");

      assertEmpty(pluginMgr.getAllAus());
      registerAus(aus);
      pluginMgr.addRegistryAu(rau);

      HangingCrawler[] crawler = new HangingCrawler[tot];
      TestCrawlCB[] cb = new TestCrawlCB[tot];
      SimpleBinarySemaphore[] startSem = new SimpleBinarySemaphore[tot];
      SimpleBinarySemaphore[] endSem = new SimpleBinarySemaphore[tot];
      SimpleBinarySemaphore[] finishedSem = new SimpleBinarySemaphore[tot];
      long startTime[] = new long[tot];
      crawlManager.recordExecute(true);

      // Create AUs and build the pieces necessary for the crawl starter to
      // get the list of AUs from the PluginManager and a NodeManager and
      // Activityregulator for each.  The crawl starter should then shortly
      // start poolMax of them.  We only check that it called
      // startNewContentCrawl() on each.
      for (int ix = 0; ix < tot; ix++) {
        startSem[ix] = new SimpleBinarySemaphore();
        endSem[ix] = new SimpleBinarySemaphore();
        finishedSem[ix] = new SimpleBinarySemaphore();
	if (ix == rauix) {
	  // Last one is a RegistryArchivalUnit crawl
	  crawler[ix] = new HangingCrawler("testOdcCrawlStarter(reg au) " + ix,
					   startSem[ix], endSem[ix]);
	  setupAuToCrawl(rau, crawler[ix]);
	} else {
	  crawler[ix] = new HangingCrawler("testOdcCrawlStarter " + ix,
					   startSem[ix], endSem[ix]);
	  aus[ix].setCrawlSpec(crawlSpec);
	  setupAuToCrawl(aus[ix], crawler[ix]);
	}
	semToGive(endSem[ix]);
      }
      
      // now let the crawl starter proceed
      crawlManager.ausStartedSem.give();
      // Ensure they all got queued
      List exe = Collections.EMPTY_LIST;
      Interrupter intr = interruptMeIn(TIMEOUT_SHOULDNT * 2, true);

      int pokeOrder[] =
	{ -1, -1, -1, 0, 10, 1, 5, 6, 7, 8, 9, 3,  4, 11, 12, 13, 14};
      int expStartOrder[] =
	{  5, 10,  0, 1, 11, 2, 6, 7, 8, 9, 3, 4, -1, 12, 13, 14, -1};

      // wait for first nthreads to start
      // check correct three (unordered)
      // repeat until done
      //    let one finish
      //    correct next one starts

      List expExec = new ArrayList();
      List expRun = new ArrayList();
      List expEnd = new ArrayList();

      for (int ix = 0; ix < tot; ix++) {
	int poke = pokeOrder[ix];
	if (poke >= 0) {
	  log.info("poke(" + poke + ")");
	  endSem[poke].give();
	}
	int wait = expStartOrder[ix];
	if (wait >= 0) {
	  assertTrue(didntMsg("start("+ix+") = " + wait, TIMEOUT_SHOULDNT),
		     startSem[wait].take(TIMEOUT_SHOULDNT));
	  expExec.add(crawler[wait]);
	}
	if (ix >= nthreads - 1) {
	  while (exe.size() < expExec.size()) {
	    crawlManager.waitExecuted();
	    assertFalse("Expected " + expExec +
			", but timed out and was " + exe,
			intr.did());
	    exe = crawlManager.getExecuted();
	  }
	  assertEquals(expExec, crawlersOf(exe));
	}
      }
    }
  }

  RegistryPlugin regplugin;

  RegistryArchivalUnit makeRegistryAu() {
    if (regplugin == null) {
      regplugin = new RegistryPlugin();
      regplugin.initPlugin(theDaemon);
    }
    RegistryArchivalUnit res = new MyRegistryArchivalUnit(regplugin);
    theDaemon.setNodeManager(new MockNodeManager(), res);
    return res;
  }

  class MyRegistryArchivalUnit extends RegistryArchivalUnit {
    MyRegistryArchivalUnit(RegistryPlugin plugin) {
      super(plugin);
      try {
	setConfiguration(ConfigurationUtil.fromArgs(ConfigParamDescr.BASE_URL.getKey(), "http://foo.bar/"));
      } catch (ArchivalUnit.ConfigurationException e) {
	throw new RuntimeException("setConfiguration()", e);
      }
    }
  }

  class MyPluginManager extends PluginManager {
    private List registryAus;
    public Collection getAllRegistryAus() {
      if (registryAus == null) {
	return super.getAllRegistryAus();
      } else {
	return registryAus;
      }
    }
    public void addRegistryAu(ArchivalUnit au) {
      if (registryAus == null) {
	registryAus = new ArrayList();
      }
      registryAus.add(au);
    }
  }

  class MyMockActivityRegulator extends MockActivityRegulator {
    public MyMockActivityRegulator(ArchivalUnit au) {
      super(au);
    }
    public void stopService() {
    }
  }

  private static class TestCrawlCB implements CrawlManager.Callback {
    SimpleBinarySemaphore sem;
    boolean called = false;
    Object cookie;
    boolean success;

    public TestCrawlCB() {
      this(null);
    }

    public TestCrawlCB(SimpleBinarySemaphore sem) {
      this.sem = sem;
    }

    public void signalCrawlAttemptCompleted(boolean success,
					    Object cookie,
					    CrawlerStatus status) {
      this.success = success;
      called = true;
      this.cookie = cookie;
      if (sem != null) {
	sem.give();
      }
    }

    public boolean wasSuccessful() {
      return success;
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

  private static class TestableCrawlManagerImpl extends CrawlManagerImpl {
    private MockCrawler mockCrawler;
    private HashMap auCrawlers = new HashMap();
    private boolean recordExecute = false;
    private List executed = new ArrayList();
    private SimpleBinarySemaphore executedSem = new SimpleBinarySemaphore();
    private SimpleBinarySemaphore ausStartedSem;

    protected Crawler makeNewContentCrawler(ArchivalUnit au, CrawlSpec spec) {
      MockCrawler crawler = getCrawler(au);
      crawler.setAu(au);
      SpiderCrawlSpec cs = (SpiderCrawlSpec) spec;
      crawler.setUrls(cs.getStartingUrls());
      crawler.setFollowLinks(true);
      crawler.setType(BaseCrawler.NEW_CONTENT);
      crawler.setIsWholeAU(true);
      return crawler;
    }

    protected Crawler makeRepairCrawler(ArchivalUnit au, CrawlSpec spec,
					Collection repairUrls,
					float percentRepairFromCache) {
      MockCrawler crawler = getCrawler(au);
      crawler.setAu(au);
      crawler.setUrls(repairUrls);
      crawler.setFollowLinks(false);
      crawler.setType(BaseCrawler.REPAIR);
      crawler.setIsWholeAU(false);
      return crawler;
    }

    protected void execute(Runnable run) throws InterruptedException {
      if (recordExecute) {
	executed.add(run);
	executedSem.give();
      }
      super.execute(run);
    }    

    void waitUntilAusStarted() throws InterruptedException {
      if (ausStartedSem != null) {
	ausStartedSem.take();
      }
      else {
	super.waitUntilAusStarted();
      }
    }

    void addToRunningRateKeys(ArchivalUnit au) {
      if (au.getFetchRateLimiterKey() != null) {
	runningRateKeys.add(au.getFetchRateLimiterKey());
      }
      highPriorityCrawlRequests.remove(au);
    }
    void delFromRunningRateKeys(ArchivalUnit au) {
      runningRateKeys.remove(au.getFetchRateLimiterKey());
    }

    int rebuildCount = 0;
    void rebuildCrawlQueue() {
      rebuildCount++;
      super.rebuildCrawlQueue();
    }

    public void recordExecute(boolean val) {
      recordExecute = val;
    }

    public void waitExecuted() {
      executedSem.take();
    }

    public List getExecuted() {
      return executed;
    }

    private void setTestCrawler(MockCrawler crawler) {
      this.mockCrawler = crawler;
    }

    private MockCrawler getCrawler(ArchivalUnit au) {
      MockCrawler crawler = (MockCrawler)auCrawlers.get(au);
      if (crawler != null) {
	return crawler;
      }
      return mockCrawler;
    }

    private void setTestCrawler(ArchivalUnit au, MockCrawler crawler) {
      auCrawlers.put(au, crawler);
    }

    protected void instrumentBeforeStartRateLimiterEvent(Crawler crawler) {
      if (crawler instanceof MockCrawler) {
	((MockCrawler)crawler).setStartTime(TimeBase.nowMs());
      }
    }

  }

  private static class ThrowingCrawler extends MockCrawler {
    RuntimeException e = null;
    public ThrowingCrawler(RuntimeException e) {
      this.e = e;
    }

    public boolean doCrawl() {
      if (false) return super.doCrawl();
      throw e;
    }
  }

  private static class HangingCrawler extends MockCrawler {
    String name;
    SimpleBinarySemaphore sem1;
    SimpleBinarySemaphore sem2;

    public HangingCrawler(String name,
			  SimpleBinarySemaphore sem1,
			  SimpleBinarySemaphore sem2) {
      this.name = name;
      this.sem1 = sem1;
      this.sem2 = sem2;
    }

    public boolean doCrawl() {
      if (false) return super.doCrawl();
      if (sem1 != null) sem1.give();
      if (sem2 != null) sem2.take();
      return false;
    }

    public String toString() {
      return "HangingCrawler " + name;
    }
  }

  private static class SignalDoCrawlCrawler extends MockCrawler {
    SimpleBinarySemaphore sem;
    public SignalDoCrawlCrawler(SimpleBinarySemaphore sem) {
      this.sem = sem;
    }

    public boolean doCrawl() {
      if (false) return super.doCrawl();
      sem.give();
      return true;
    }
  }

  private static class CloseWindowCrawler extends MockCrawler {
    public CloseWindowCrawler() {
    }

    public boolean doCrawl() {
      super.doCrawl();
      CrawlSpec spec = getAu().getCrawlSpec();
      spec.setCrawlWindow(new ClosedCrawlWindow());
      return false;
    }
  }

  private static class ClosedCrawlWindow implements CrawlWindow {
    public boolean canCrawl() {
      return false;
    }
    public boolean canCrawl(Date x) {
      return false;
    }
  }

  private static class ThrowingAU extends NullPlugin.ArchivalUnit {
    public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec spec) {
      throw new ExpectedRuntimeException("I throw");
    }

    public CachedUrl makeCachedUrl(CachedUrlSet owner, String url) {
      throw new ExpectedRuntimeException("I throw");
    }

    public UrlCacher makeUrlCacher(CachedUrlSet owner, String url) {
      throw new ExpectedRuntimeException("I throw");
    }

    public CachedUrlSet getAuCachedUrlSet() {
      throw new ExpectedRuntimeException("I throw");
    }

    public CrawlSpec getCrawlSpec() {
      throw new ExpectedRuntimeException("I throw");
    }

    public boolean shouldBeCached(String url) {
      throw new ExpectedRuntimeException("I throw");
    }

    public Collection getUrlStems() {
      throw new ExpectedRuntimeException("I throw");
    }

    public Plugin getPlugin() {
      throw new ExpectedRuntimeException("I throw");
    }
  }

  public static Test suite() {
    return variantSuites(new Class[] {TestsWithAutoStart.class,
				      TestsWithoutAutoStart.class});
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestCrawlManagerImpl.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}
