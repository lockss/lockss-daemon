/*
 * $Id: TestCrawlManagerImpl.java,v 1.1 2003-02-05 22:40:42 troberts Exp $
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
import java.util.*;
import java.net.*;
import junit.framework.TestCase;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 */
public class TestCrawlManagerImpl extends LockssTestCase {
  public static final String startUrl = "http://www.example.com/index.html";
  private CrawlManager crawlManager = null;
  private MockArchivalUnit mau = null;
  private List urlList = null;
  public static final String EMPTY_PAGE = "";
  public static final String LINKLESS_PAGE = "Nothing here";

  public TestCrawlManagerImpl(String msg) {
    super(msg);
  }

  public void setUp() {
    mau = new MockArchivalUnit();
    
    urlList = ListUtil.list(startUrl);
    MockCachedUrlSet cus = new MockCachedUrlSet(mau, null);
    mau.setAUCachedUrlSet(cus);

    crawlManager = new CrawlManagerImpl();
    mau.setNewContentCrawlUrls(ListUtil.list(startUrl));
  }

  public void testNullAUForCanTreeWalkStart() {
    try {
      crawlManager.canTreeWalkStart(null, 
				    new TestCrawlCB(Deadline.NEVER), "blah");
      fail("Didn't throw an IllegalArgumentException on a null AU");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testNullCallbackForCanTreeWalkStart() {
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);
    crawlManager.canTreeWalkStart(mau, null, "blah");
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
    
    Deadline deadline = Deadline.in(1000 * 10);


    assertTrue(!crawlManager.canTreeWalkStart(mau, new TestCrawlCB(deadline), 
					      null));

    while (!deadline.expired()) {
      try{
 	deadline.sleep();
      } catch (InterruptedException ie) {
      }
    }
    
    Set expected = SetUtil.set(startUrl, url1, url2, url3);
    assertEquals(expected, cus.getCachedUrls());
  }


  public void testTriggersNewContentCallback() {
    Deadline deadline = Deadline.in(1000 * 10);
    
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    TestCrawlCB cb = new TestCrawlCB(deadline);
    crawlManager.canTreeWalkStart(mau, cb, null);

    while (!deadline.expired()) {
      try{
	deadline.sleep();
      } catch (InterruptedException ie) {
      }
    }
    assertTrue("Callback wasn't triggered", cb.wasTriggered());
  }

  public void testNewContentCrawlCallbackReturnsCookie() {
    Deadline deadline = Deadline.in(1000 * 10);
    String cookie = "cookie string";

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    TestCrawlCB cb = new TestCrawlCB(deadline);
    crawlManager.canTreeWalkStart(mau, cb, cookie);

    while (!deadline.expired()) {
      try{
	deadline.sleep();
      } catch (InterruptedException ie) {
      }
    }
    assertEquals(cookie, (String)cb.getCookie());
  }

  public void testNewContentCrawlCallbackReturnsNullCookie() {
    Deadline deadline = Deadline.in(1000 * 10);
    String cookie = null;

    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    TestCrawlCB cb = new TestCrawlCB(deadline);
    crawlManager.canTreeWalkStart(mau, cb, cookie);

    while (!deadline.expired()) {
      try{
	deadline.sleep();
      } catch (InterruptedException ie) {
      }
    }
    assertEquals(cookie, (String)cb.getCookie());
  }

  public void testKicksOffNewThread() {
    BinarySemaphore sem = new BinarySemaphore();
    
    TestCrawlCB cb = new TestCrawlCB(Deadline.NEVER);
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();

    cus.addUrl(LINKLESS_PAGE, startUrl);

    WaitOnSemaphoreCallback pauseCB = new WaitOnSemaphoreCallback(sem,
								  1000 * 10);
    mau.setPauseCallback(pauseCB);

    crawlManager.canTreeWalkStart(mau, cb, null);

    //if the callback was triggered, the crawl completed
    assertTrue("Callback was triggered", !cb.wasTriggered());
  }

  public void testScheduleRepairNullAU() throws MalformedURLException {
    try{
      crawlManager.scheduleRepair(null, new URL("http://www.example.com"),
				  new TestCrawlCB(Deadline.NEVER), "blah");
      fail("Didn't throw IllegalArgumentException on null AU");
    } catch (IllegalArgumentException iae) {
    }
  }

  public void testScheduleRepairNullUrl() {
    try{
      crawlManager.scheduleRepair(mau, null,
				  new TestCrawlCB(Deadline.NEVER), "blah");
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
    
    Deadline deadline = Deadline.in(1000 * 10);

    crawlManager.scheduleRepair(mau, new URL(startUrl), 
				new TestCrawlCB(deadline), null);

    while (!deadline.expired()) {
      try{
 	deadline.sleep();
      } catch (InterruptedException ie) {
      }
    }
    Set expected = SetUtil.set(startUrl);
    assertEquals(expected, cus.getCachedUrls());
  }

  public void testTriggersRepairCallback() throws MalformedURLException {
    Deadline deadline = Deadline.in(1000 * 10);
    
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    TestCrawlCB cb = new TestCrawlCB(deadline);

    crawlManager.scheduleRepair(mau, new URL(startUrl), cb, null);

    while (!deadline.expired()) {
      try{
	deadline.sleep();
      } catch (InterruptedException ie) {
      }
    }
    assertTrue("Callback wasn't triggered", cb.wasTriggered());
  }

  public void testRepairCallbackGetsCookie() throws MalformedURLException {
    Deadline deadline = Deadline.in(1000 * 10);
    String cookie = "test cookie str";
    MockCachedUrlSet cus = (MockCachedUrlSet)mau.getAUCachedUrlSet();
    cus.addUrl(LINKLESS_PAGE, startUrl);

    TestCrawlCB cb = new TestCrawlCB(deadline);

    crawlManager.scheduleRepair(mau, new URL(startUrl), cb, cookie);

    while (!deadline.expired()) {
      try{
	deadline.sleep();
      } catch (InterruptedException ie) {
      }
    }
    
    assertEquals(cookie, cb.getCookie());

  }

  

  private class TestCrawlCB implements CrawlCallback {
    Deadline timer;
    boolean called = false;
    Object cookie;

    public TestCrawlCB(Deadline timer) {
      this.timer = timer;
    }

    public void signalCrawlAttemptCompleted(boolean success, 
					    Object cookie) {
      called = true;
      this.cookie = cookie;
      timer.expire();
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
    BinarySemaphore sem;
    int maxWaitTime = 0;

    public WaitOnSemaphoreCallback(BinarySemaphore sem, int maxWaitTime) {
      this.sem = sem;
      this.maxWaitTime = maxWaitTime;
    }
   
    public void callback() {
      try {
	sem.take(Deadline.in(maxWaitTime));
      } catch (InterruptedException ie) {
      }
    }
  }
}
