/*
 * $Id: TestCrawlManagerImpl.java,v 1.59 2006-04-11 08:33:33 tlipkis Exp $
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
  protected CrawlManager.StatusSource statusSource;
  protected PluginManager pluginMgr;
  protected MockAuState maus;
  protected Plugin plugin;

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin());
    mau.setCrawlSpec(new SpiderCrawlSpec("blah", new MockCrawlRule()));
    plugin = mau.getPlugin();

    crawlManager = new TestableCrawlManagerImpl();
    statusSource = (CrawlManager.StatusSource)crawlManager;
    theDaemon = getMockLockssDaemon();
    nodeManager = new MockNodeManager();
    theDaemon.setNodeManager(nodeManager, mau);

    pluginMgr = new PluginManager();
    pluginMgr.initService(theDaemon);
    theDaemon.setPluginManager(pluginMgr);

    PluginTestUtil.registerArchivalUnit(plugin, mau);
    activityRegulator = new MyMockActivityRegulator(mau);
    activityRegulator.initService(theDaemon);
    theDaemon.setActivityRegulator(activityRegulator, mau);
    theDaemon.setLockssRepository(new MockLockssRepository(), mau);

    crawlManager.initService(theDaemon);

    cus = mau.makeCachedUrlSet(new SingleNodeCachedUrlSetSpec(GENERIC_URL));
    activityRegulator.setStartCusActivity(cus, true);
    activityRegulator.setStartAuActivity(true);
    crawler = new MockCrawler();
    crawlManager.setTestCrawler(crawler);

    maus = new MockAuState();
    nodeManager.setAuState(maus);
  }

  public void tearDown() throws Exception {
    nodeManager.stopService();
    crawlManager.stopService();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  String didntMsg(String what, long time) {
    return "Crawl didn't " + what + " in " +
      StringUtil.timeIntervalToString(time);
  }

  protected void waitForCrawlToFinish(SimpleBinarySemaphore sem) {
    if (!sem.take(TIMEOUT_SHOULDNT)) {
      fail(didntMsg("finish", TIMEOUT_SHOULDNT));
    }
  }

  public static class TestsWithAutoStart extends TestCrawlManagerImpl {
    public void setUp() throws Exception {
      super.setUp();
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
      crawlManager.setTestCrawler(crawler);

      crawlManager.startRepair(mau, urls, cb, null, null);
      assertTrue(didntMsg("start", TIMEOUT_SHOULDNT),
		 sem1.take(TIMEOUT_SHOULDNT));
      //we know that doCrawl started

      crawlManager.cancelAuCrawls(mau);

      assertTrue(crawler.wasAborted());
    }


    public void testDoesntNCCrawlWhenNotAllowed() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      activityRegulator.setStartAuActivity(false);

      crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);

      waitForCrawlToFinish(sem);
      assertFalse("doCrawl() called", crawler.doCrawlCalled());
    }


    private void setNewContentRateLimit(int events, long interval) {
      ConfigurationUtil.
	setFromArgs(CrawlManagerImpl.PARAM_MAX_NEW_CONTENT_RATE,
		    events+"/"+interval);
    }

    private void setRepairRateLimit(int events, long interval) {
      ConfigurationUtil.
	setFromArgs(CrawlManagerImpl.PARAM_MAX_REPAIR_RATE,
		    events+"/"+interval);
    }

    private void assertDoesCrawlNew() {
      crawler.setDoCrawlCalled(false);
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);
      waitForCrawlToFinish(sem);
      assertTrue("doCrawl() not called at time " + TimeBase.nowMs(),
		 crawler.doCrawlCalled());
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

    public void testNewContentRateLimiter() {
      TimeBase.setSimulated(100);
      setNewContentRateLimit(4, 100);
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
      setNewContentRateLimit(2, 5);
      assertDoesCrawlNew();
      assertDoesCrawlNew();
      assertDoesNotCrawlNew();
      TimeBase.step(5);
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
      setNewContentRateLimit(1, 200);
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

    public void testCompletedCrawlUpdatesLastCrawlTime() {
      long lastCrawlTime = maus.getLastCrawlTime();

      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      TestCrawlCB cb = new TestCrawlCB(sem);
      crawlManager.startNewContentCrawl(mau, cb, null, null);

      waitForCrawlToFinish(sem);
      assertNotEquals(lastCrawlTime, maus.getLastCrawlTime());
    }

    public void testUnsuccessfulCrawlDoesntUpdateLastCrawlTime() {
      long lastCrawlTime = maus.getLastCrawlTime();

      crawler.setCrawlSuccessful(false);

      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      TestCrawlCB cb = new TestCrawlCB(sem);
      crawlManager.startNewContentCrawl(mau, cb, null, null);

      waitForCrawlToFinish(sem);
      assertEquals(lastCrawlTime, maus.getLastCrawlTime());
    }

    public void testCompletedCrawlUpdatesLastCrawlTimeIfFNFExceptionThrown() {
      long lastCrawlTime = maus.getLastCrawlTime();

      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      TestCrawlCB cb = new TestCrawlCB(sem);
      crawlManager.startNewContentCrawl(mau, cb, null, null);

      waitForCrawlToFinish(sem);
      assertNotEquals(lastCrawlTime, maus.getLastCrawlTime());
    }

    //StatusSource tests

    public void testGetCrawlStatusListReturnsEmptyListIfNoCrawls() {
      List list = statusSource.getStatus().getCrawlStatusList();
      assertEquals(0, list.size());
    }


    public void testGetCrawlsOneRepairCrawl() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      crawlManager.startRepair(mau, ListUtil.list(GENERIC_URL),
			       new TestCrawlCB(sem), null, null);
      List actual = statusSource.getStatus().getCrawlStatusList();
      List expected = ListUtil.list(crawler.getStatus());

      assertEquals(expected, actual);
      waitForCrawlToFinish(sem);
    }

    public void testGetCrawlsOneNCCrawl() {
      SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
      crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);
      List actual = statusSource.getStatus().getCrawlStatusList();
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

      List actual = statusSource.getStatus().getCrawlStatusList();
      List expected = ListUtil.list(crawler.getStatus(), crawler2.getStatus());
      assertEquals(expected, actual);
      waitForCrawlToFinish(sem1);
      waitForCrawlToFinish(sem2);
    }

  }

  public static class TestsWithoutAutoStart extends TestCrawlManagerImpl {

    public void testNoQueuePoolSize(int max) {
      Properties p = new Properties();
      p.put(CrawlManagerImpl.PARAM_CRAWLER_QUEUE_ENABLED,
	    "false");
      p.put(CrawlManagerImpl.PARAM_CRAWLER_THREAD_POOL_MAX,
	    Integer.toString(max));
      p.put(CrawlManagerImpl.PARAM_MAX_NEW_CONTENT_RATE, (max*2)+"/1h");
      ConfigurationUtil.setCurrentConfigFromProps(p);
      crawlManager.startService();
      SimpleBinarySemaphore[] startSem = new SimpleBinarySemaphore[max];
      SimpleBinarySemaphore[] endSem = new SimpleBinarySemaphore[max];
      SimpleBinarySemaphore[] finishedSem = new SimpleBinarySemaphore[max];
      TestCrawlCB[] cb = new TestCrawlCB[max];
      for (int ix = 0; ix < max; ix++) {
        finishedSem[ix] = new SimpleBinarySemaphore();
        //start a crawler that hangs until we post its semaphore
        cb[ix] = new TestCrawlCB(finishedSem[ix]);
        startSem[ix] = new SimpleBinarySemaphore();
        endSem[ix] = new SimpleBinarySemaphore();
        //gives sem1 when doCrawl is entered, then takes sem2
        MockCrawler crawler = new HangingCrawler("testNoQueuePoolSize " + ix,
						 startSem[ix], endSem[ix]);
        crawlManager.setTestCrawler(crawler);

        crawlManager.startNewContentCrawl(mau, cb[ix], null, null);
      }
      for (int ix = 0; ix < max; ix++) {
        assertTrue(didntMsg("start("+ix+")", TIMEOUT_SHOULDNT),
		   startSem[ix].take(TIMEOUT_SHOULDNT));
        //we know that doCrawl started
      }
      MockCrawler crawler = new HangingCrawler("testNoQueuePoolSize ix+1",
					       new SimpleBinarySemaphore(),
					       new SimpleBinarySemaphore());
      crawlManager.setTestCrawler(crawler);
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
      int tot = qMax + poolMax;
      Properties p = new Properties();
      p.put(CrawlManagerImpl.PARAM_CRAWLER_QUEUE_ENABLED,
	    "true");
      p.put(CrawlManagerImpl.PARAM_CRAWLER_THREAD_POOL_MAX,
	    Integer.toString(poolMax));
      p.put(CrawlManagerImpl.PARAM_CRAWLER_THREAD_POOL_QUEUE_SIZE,
	    Integer.toString(qMax));
      p.put(CrawlManagerImpl.PARAM_MAX_NEW_CONTENT_RATE, (tot*2)+"/1h");
      ConfigurationUtil.setCurrentConfigFromProps(p);
      crawlManager.startService();
      SimpleBinarySemaphore[] startSem = new SimpleBinarySemaphore[tot];
      SimpleBinarySemaphore[] endSem = new SimpleBinarySemaphore[tot];
      SimpleBinarySemaphore[] finishedSem = new SimpleBinarySemaphore[tot];
      TestCrawlCB[] cb = new TestCrawlCB[tot];
      for (int ix = 0; ix < tot; ix++) {
        finishedSem[ix] = new SimpleBinarySemaphore();
        //start a crawler that hangs until we post its semaphore
        cb[ix] = new TestCrawlCB(finishedSem[ix]);
        startSem[ix] = new SimpleBinarySemaphore();
        endSem[ix] = new SimpleBinarySemaphore();
        //gives sem1 when doCrawl is entered, then takes sem2
        MockCrawler crawler = new HangingCrawler("testQueuedPool " + ix,
						 startSem[ix], endSem[ix]);
        crawlManager.setTestCrawler(crawler);

        crawlManager.startNewContentCrawl(mau, cb[ix], null, null);
      }
      for (int ix = 0; ix < tot; ix++) {
	if (ix < poolMax) {
	  assertTrue(didntMsg("start("+ix+")", TIMEOUT_SHOULDNT),
		     startSem[ix].take(TIMEOUT_SHOULDNT));
	  startSem[ix] = null;
	} else {
	  assertFalse("Wasn't queued "+ix, cb[ix].wasTriggered());
	  assertFalse("Shouldn't have started " + ix, 
		     startSem[ix].take(0));
	}
      }
      MockCrawler crawler = new HangingCrawler("testNoQueuePoolSize ix+1",
					       new SimpleBinarySemaphore(),
					       new SimpleBinarySemaphore());
      crawlManager.setTestCrawler(crawler);
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

  }

  class MyMockActivityRegulator extends MockActivityRegulator {
    public MyMockActivityRegulator(ArchivalUnit au) {
      super(au);
    }
    public void stopService() {
    }
  }

  private class TestCrawlCB implements CrawlManager.Callback {
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

    public void signalCrawlAttemptCompleted(boolean success, Set urlsFetched,
					    Object cookie,
					    Crawler.Status status) {
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
    protected Crawler makeNewContentCrawler(ArchivalUnit au, CrawlSpec spec) {
      mockCrawler.setAu(au);
      SpiderCrawlSpec cs = (SpiderCrawlSpec) spec;
      mockCrawler.setUrls(cs.getStartingUrls());
      mockCrawler.setFollowLinks(true);
      mockCrawler.setType(BaseCrawler.NEW_CONTENT);
      mockCrawler.setIsWholeAU(true);
      return mockCrawler;
    }

    protected Crawler makeRepairCrawler(ArchivalUnit au, CrawlSpec spec,
					Collection repairUrls,
					float percentRepairFromCache) {
      mockCrawler.setAu(au);
      mockCrawler.setUrls(repairUrls);
      mockCrawler.setFollowLinks(false);
      mockCrawler.setType(BaseCrawler.REPAIR);
      mockCrawler.setIsWholeAU(false);
      return mockCrawler;
    }

    private void setTestCrawler(MockCrawler mockCrawler) {
      this.mockCrawler = mockCrawler;
    }
  }

  private static class ThrowingCrawler extends MockCrawler {
    RuntimeException e = null;
    public ThrowingCrawler(RuntimeException e) {
      this.e = e;
    }

    public boolean doCrawl(Deadline deadline) {
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
      sem1.give();
      return sem2.take();
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
      sem.give();
      return true;
    }
  }

  private class ThrowingAU extends NullPlugin.ArchivalUnit {
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
    return variantSuites(TestCrawlManagerImpl.class);
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestCrawlManagerImpl.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}
