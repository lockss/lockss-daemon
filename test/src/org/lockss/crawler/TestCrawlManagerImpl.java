/*
 * $Id: TestCrawlManagerImpl.java,v 1.48 2004-09-29 18:58:18 tlipkis Exp $
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
import junit.framework.TestCase;
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
  private TestableCrawlManagerImpl crawlManager = null;
  private MockArchivalUnit mau = null;
  private MockLockssDaemon theDaemon;
  private MockNodeManager nodeManager;
  private MockActivityRegulator activityRegulator;
  private MockCrawler crawler;
  private CachedUrlSet cus;
  private CrawlManager.StatusSource statusSource;
  private MockAuState maus;
  private Plugin plugin;

  public TestCrawlManagerImpl(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    mau.setPlugin(new MockPlugin());
    mau.setCrawlSpec(new CrawlSpec("blah", null));
    plugin = mau.getPlugin();

    crawlManager = new TestableCrawlManagerImpl();
    statusSource = (CrawlManager.StatusSource)crawlManager;
    theDaemon = getMockLockssDaemon();
    nodeManager = new MockNodeManager();
    theDaemon.setNodeManager(nodeManager, mau);

    activityRegulator = new MockActivityRegulator(mau);
    activityRegulator.initService(theDaemon);
    theDaemon.setActivityRegulator(activityRegulator, mau);
    theDaemon.setLockssRepository(new MockLockssRepository(), mau);

    crawlManager.initService(theDaemon);
    crawlManager.startService();

    cus = plugin.makeCachedUrlSet(mau,
				  new SingleNodeCachedUrlSetSpec(GENERIC_URL));
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

  public void testNullAuForIsCrawlingAu() {
    try {
      TestCrawlCB cb = new TestCrawlCB(new SimpleBinarySemaphore());
      crawlManager.startNewContentCrawl(null, cb, "blah", null);
      fail("Didn't throw an IllegalArgumentException on a null AU");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testShouldRecrawlReturnsFalse() {
    //for now shouldRecrawl() should always return false
    assertFalse(crawlManager.shouldRecrawl(mau, null));
  }


  private void waitForCrawlToFinish(SimpleBinarySemaphore sem) {
    if (!sem.take(TIMEOUT_SHOULDNT)) {
      fail(didntMsg("finish", TIMEOUT_SHOULDNT));
    }
  }

//   public void testNewCrawlDeadlineIsMax() {
//     SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

//     crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);

//     waitForCrawlToFinish(sem);
//     assertEquals(Deadline.MAX, crawler.getDeadline());
//   }

  public void testStoppingCrawlAbortsNewContentCrawl() {
    SimpleBinarySemaphore finishedSem = new SimpleBinarySemaphore();
    //start the crawler, but use a crawler that will hang

    TestCrawlCB cb = new TestCrawlCB(finishedSem);
    SimpleBinarySemaphore sem1 = new SimpleBinarySemaphore();
    SimpleBinarySemaphore sem2 = new SimpleBinarySemaphore();
    //gives sem1 when doCrawl is entered, then takes sem2
    MockCrawler crawler = new HangingCrawler(sem1, sem2);
    crawlManager.setTestCrawler(crawler);

    crawlManager.startNewContentCrawl(mau, cb, null, null);
    assertTrue(didntMsg("start", TIMEOUT_SHOULDNT),
	       sem1.take(TIMEOUT_SHOULDNT));
    //we know that doCrawl started

    crawlManager.cancelAuCrawls(mau);

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
    MockCrawler crawler = new HangingCrawler(sem1, sem2);
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


  public void testBasicNewContentCrawl() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

    crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);

    waitForCrawlToFinish(sem);
    assertTrue("doCrawl() not called", crawler.doCrawlCalled());
  }

  public void testNCCrawlFreesActivityLockWhenDone() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();

    crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);

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
      plugin.makeCachedUrlSet(mau, new SingleNodeCachedUrlSetSpec(url1));
    CachedUrlSet cus2 =
      plugin.makeCachedUrlSet(mau, new SingleNodeCachedUrlSetSpec(url2));

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
    MockCrawler crawler = new HangingCrawler(sem1, sem2);
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
      plugin.makeCachedUrlSet(mau, new SingleNodeCachedUrlSetSpec(url1));
    CachedUrlSet cus2 =
      plugin.makeCachedUrlSet(mau, new SingleNodeCachedUrlSetSpec(url2));

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
      plugin.makeCachedUrlSet(mau, new SingleNodeCachedUrlSetSpec(url1));
    CachedUrlSet cus2 =
      plugin.makeCachedUrlSet(mau, new SingleNodeCachedUrlSetSpec(url2));

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

    public void signalCrawlAttemptCompleted(boolean success, Object cookie) {
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

  //StatusSource tests

  public void testGetActiveAusReturnsEmptyCollectionIfNoCrawls() {
    Collection coll = statusSource.getActiveAus();
    assertEquals(0, coll.size());
  }

  public void testGetActiveAusOneNCCrawl() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);

    Collection actual = statusSource.getActiveAus();
    Collection expected = ListUtil.list(mau.getAuId());
    assertSameElements(expected, actual);
    waitForCrawlToFinish(sem);
  }

  public void testGetActiveAusOneRepairCrawl() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    crawlManager.startRepair(mau, ListUtil.list(GENERIC_URL),
			     new TestCrawlCB(sem), null, null);
    Collection actual = statusSource.getActiveAus();
    Collection expected = ListUtil.list(mau.getAuId());
    assertSameElements(expected, actual);
    waitForCrawlToFinish(sem);
  }

  public void testGetActiveAusMulti() {
    SimpleBinarySemaphore sem1 = new SimpleBinarySemaphore();
    SimpleBinarySemaphore sem2 = new SimpleBinarySemaphore();
    crawlManager.startRepair(mau, ListUtil.list(GENERIC_URL),
			     new TestCrawlCB(sem1), null, null);
    MockArchivalUnit mau2 = new MockArchivalUnit();
    mau2.setCrawlSpec(new CrawlSpec("blah", null));

    theDaemon.setNodeManager(nodeManager, mau2);
    MockActivityRegulator activityRegulator2 = new MockActivityRegulator(mau2);
    activityRegulator2.setStartAuActivity(true);
    theDaemon.setActivityRegulator(activityRegulator2, mau2);

    crawlManager.startNewContentCrawl(mau2, new TestCrawlCB(sem2), null, null);

    Collection actual = statusSource.getActiveAus();
    Collection expected = ListUtil.list(mau.getAuId(), mau2.getAuId());
    assertSameElements(expected, actual);
    waitForCrawlToFinish(sem1);
    waitForCrawlToFinish(sem2);
  }

  public void testGetCrawlsNoCrawls() {
    Collection coll = statusSource.getCrawlStatus(mau.getAuId());
    assertEquals(0, coll.size());
  }

  public void testGetCrawlsOneRepairCrawl() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    crawlManager.startRepair(mau, ListUtil.list(GENERIC_URL),
			     new TestCrawlCB(sem), null, null);
    Collection actual = statusSource.getCrawlStatus(mau.getAuId());
    Collection expected = ListUtil.list(crawler.getStatus());

    assertEquals(expected, actual);
    waitForCrawlToFinish(sem);
  }

  public void testGetCrawlsOneNCCrawl() {
    SimpleBinarySemaphore sem = new SimpleBinarySemaphore();
    crawlManager.startNewContentCrawl(mau, new TestCrawlCB(sem), null, null);
    Collection actual = statusSource.getCrawlStatus(mau.getAuId());
    Collection expected = ListUtil.list(crawler.getStatus());
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

    Collection actual = statusSource.getCrawlStatus(mau.getAuId());
    Collection expected = ListUtil.list(crawler.getStatus(),
					crawler2.getStatus());
    assertEquals(expected, actual);
    waitForCrawlToFinish(sem1);
    waitForCrawlToFinish(sem2);
  }

  private static class TestableCrawlManagerImpl extends CrawlManagerImpl {
    private MockCrawler mockCrawler;
    protected Crawler makeNewContentCrawler(ArchivalUnit au, CrawlSpec spec) {
      mockCrawler.setAu(au);
      mockCrawler.setUrls(spec.getStartingUrls());
      mockCrawler.setFollowLinks(true);
      mockCrawler.setType(CrawlerImpl.NEW_CONTENT);
      return mockCrawler;
    }

    protected Crawler makeRepairCrawler(ArchivalUnit au, CrawlSpec spec,
					Collection repairUrls,
					float percentRepairFromCache) {
      mockCrawler.setAu(au);
      mockCrawler.setUrls(repairUrls);
      mockCrawler.setFollowLinks(false);
      mockCrawler.setType(CrawlerImpl.REPAIR);
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
    SimpleBinarySemaphore sem1;
    SimpleBinarySemaphore sem2;
    public HangingCrawler(SimpleBinarySemaphore sem1,
			  SimpleBinarySemaphore sem2) {
      this.sem1 = sem1;
      this.sem2 = sem2;
    }

    public boolean doCrawl() {
      sem1.give();
      sem2.take();
      return true;
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

  public static void main(String[] argv) {
    String[] testCaseList = { TestCrawlManagerImpl.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}
