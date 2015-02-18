/*
 * $Id$
 */

/*

 Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.clockss;

import java.io.*;
import java.util.*;

import org.lockss.plugin.*;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.state.*;
import org.lockss.crawler.*;
import org.lockss.test.*;
import org.lockss.test.MockCrawler.MockCrawlerFacade;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * test class for org.lockss.clockss.ClockssUrlCacher
 */
public class TestClockssUrlFetcher extends LockssTestCase {
  protected static Logger logger = Logger.getLogger("TestClockssUrlCacher");

  static final String URL = "http://example.com/foo.html";
  static final String INST_ADDR = "44.55.77.88";
  static final String CLOCKSS_ADDR = "3.2.1.1";

  static final String EVENT_SETINST = "setLocalAddress: " + INST_ADDR;
  static final String EVENT_SETCLOCKSS = "setLocalAddress: " + CLOCKSS_ADDR;
  static final String EVENT_INPUTSTREAM = "getUncachedInputStream";
  static final String EVENT_CACHE = "cache";
  static final String EVENT_RESET = "reset";

  static final InputStream RES_INPUT_STREAM = new StringInputStream("foo");

  MockPlugin mp;
  MockArchivalUnit mau;
  MyUrlFetcher muf;
  ClockssUrlFetcher cuc;
  AuState aus;
  InputStream expin, expin2;
  MockCrawlerFacade mcf;

  public void setUp() throws Exception {
    super.setUp();
    expin = new StringInputStream("");
    expin2 = new StringInputStream("a");

    MockLockssDaemon daemon = getMockLockssDaemon();
    daemon.setClockss(true);
    mp = new MockPlugin();
    mp.initPlugin(daemon);
    mau = new MockArchivalUnit(mp);
    // accessing the AuState requires NodeManager, HistoryRepository
    MockHistoryRepository histRepo = new MockHistoryRepository();
    histRepo.storeAuState(new AuState(mau, histRepo));
    daemon.setHistoryRepository(histRepo, mau);
    MockNodeManager nodeMgr = new MockNodeManager();
    daemon.setNodeManager(nodeMgr, mau);
    aus = AuUtil.getAuState(mau);
    Properties p = new Properties();
    p.put(ClockssParams.PARAM_INSTITUTION_SUBSCRIPTION_ADDR, INST_ADDR);
    p.put(ClockssParams.PARAM_CLOCKSS_SUBSCRIPTION_ADDR, CLOCKSS_ADDR);
    p.put(ClockssParams.PARAM_ENABLE_CLOCKSS_SUBSCRIPTION_DETECTION, "true");
    ConfigurationUtil.addFromProps(p);
    daemon.getClockssParams().startService();
    mcf = new MockCrawler().new MockCrawlerFacade();
    mcf.setAu(mau);
    muf = new MyUrlFetcher(mcf, URL);
    cuc = new ClockssUrlFetcher(muf);
  }

  List append(List l, Object o) {
    l.add(o);
    return l;
  }

  List append(List l, Object o1, Object o2) {
    l.add(o1);
    l.add(o2);
    return l;
  }

  List append(List l, Object o1, Object o2, Object o3) {
    l.add(o1);
    l.add(o2);
    l.add(o3);
    return l;
  }

  public void testDetectDisabled() throws Exception {
    ConfigurationUtil.addFromArgs(ClockssParams.PARAM_ENABLE_CLOCKSS_SUBSCRIPTION_DETECTION, "false");
    muf = new MyUrlFetcher(mcf, URL);
    cuc = new ClockssUrlFetcher(muf);

    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muf.setResults(ListUtil.list(expin, expin2));
    assertSame(expin, cuc.getUncachedInputStream());
    List events = ListUtil.list(EVENT_SETCLOCKSS, EVENT_INPUTSTREAM);
    assertEquals(events, muf.events);
    assertEquals(AuState.CLOCKSS_SUB_NOT_MAINTAINED,
		 aus.getClockssSubscriptionStatus());

  }

  // ensure uc.setLocalAddress() works
  public void testDetectDisabledSetLocal() throws Exception {
    Configuration config = ConfigManager.newConfiguration();
    config.copyFrom(ConfigManager.getCurrentConfig());
    config.remove(ClockssParams.PARAM_CLOCKSS_SUBSCRIPTION_ADDR);
    config.put(ClockssParams.PARAM_ENABLE_CLOCKSS_SUBSCRIPTION_DETECTION, "false");
    ConfigurationUtil.installConfig(config);

    muf = new MyUrlFetcher(mcf, URL);
    cuc = new ClockssUrlFetcher(muf);

    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muf.setResults(ListUtil.list(expin, expin2));
    cuc.setLocalAddress(IPAddr.getByName("4.5.8.3"));
    assertSame(expin, cuc.getUncachedInputStream());
    assertEquals(ListUtil.list("setLocalAddress: " + "4.5.8.3",
			       EVENT_INPUTSTREAM),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_NOT_MAINTAINED,
		 aus.getClockssSubscriptionStatus());

  }

  public void testSubUnknownInst1() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muf.setResults(ListUtil.list(expin, expin2));
    assertSame(expin, cuc.getUncachedInputStream());
    List events = ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM);
    assertEquals(events, muf.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
    // calling getUncachedInputStream() now shouldn't do another probe
    assertSame(expin2, cuc.getUncachedInputStream());
    assertEquals(append(events, EVENT_INPUTSTREAM), muf.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
    cuc.reset();
    // now it should
    assertSame(RES_INPUT_STREAM, cuc.getUncachedInputStream());
    assertEquals(append(events, EVENT_RESET, EVENT_SETINST, EVENT_INPUTSTREAM),
		 muf.events);
  }

  public void testSubUnknownClockss1() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 expin, expin2));
    assertSame(expin, cuc.getUncachedInputStream());
    List events = ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM,
				EVENT_SETCLOCKSS, EVENT_RESET,
				EVENT_INPUTSTREAM);
    assertEquals(events, muf.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
    // calling getUncachedInputStream() now shouldn't do another probe
    assertSame(expin2, cuc.getUncachedInputStream());
    assertEquals(append(events, EVENT_INPUTSTREAM), muf.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
    cuc.reset();
    // now it should
    assertSame(RES_INPUT_STREAM, cuc.getUncachedInputStream());
    assertEquals(append(events, EVENT_RESET, EVENT_SETCLOCKSS,
			EVENT_INPUTSTREAM),
		 muf.events);
  }

  public void testSubUnknownNeither1() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.getUncachedInputStream();
      fail("getUncachedInputStream() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM,
			       EVENT_SETCLOCKSS, EVENT_RESET,
			       EVENT_INPUTSTREAM),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownFail1() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muf.setResults(ListUtil.list(new CacheException.NoRetryDeadLinkException()));
    try {
      cuc.getUncachedInputStream();
      fail("getUncachedInputStream() didn't throw on fetch error");
    } catch (CacheException.NoRetryDeadLinkException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM),
		 muf.events);
    // Should still be unknown
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesInst1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    InputStream exp = new StringInputStream("");
    muf.setResults(ListUtil.list(exp));
    assertSame(exp, cuc.getUncachedInputStream());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM), muf.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesClockss1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    InputStream exp = new StringInputStream("");
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 exp));
    assertSame(exp, cuc.getUncachedInputStream());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM,
			       EVENT_SETCLOCKSS, EVENT_RESET,
			       EVENT_INPUTSTREAM),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesNeither1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.getUncachedInputStream();
      fail("getUncachedInputStream() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM,
			       EVENT_SETCLOCKSS, EVENT_RESET,
			       EVENT_INPUTSTREAM),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesFail1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    muf.setResults(ListUtil.list(new CacheException.NoRetryDeadLinkException()));
    try {
      cuc.getUncachedInputStream();
      fail("getUncachedInputStream() didn't throw on fetch error");
    } catch (CacheException.NoRetryDeadLinkException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM),
		 muf.events);
    // Should still be Yes
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoClockss1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    InputStream exp = new StringInputStream("");
    muf.setResults(ListUtil.list(exp));
    assertSame(exp, cuc.getUncachedInputStream());
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_INPUTSTREAM),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoNeither1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.getUncachedInputStream();
      fail("getUncachedInputStream() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_INPUTSTREAM),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoFail1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    muf.setResults(ListUtil.list(new CacheException.NoRetryDeadLinkException()));
    try {
      cuc.getUncachedInputStream();
      fail("getUncachedInputStream() didn't throw on fetch error");
    } catch (CacheException.NoRetryDeadLinkException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_INPUTSTREAM),
		 muf.events);
    // Should still be No
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubInaccessibleInst1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_INACCESSIBLE);
    InputStream exp = new StringInputStream("");
    muf.setResults(ListUtil.list(exp));
    assertSame(exp, cuc.getUncachedInputStream());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM), muf.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubInaccessibleClockss1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_INACCESSIBLE);
    InputStream exp = new StringInputStream("");
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 exp));
    assertSame(exp, cuc.getUncachedInputStream());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM,
			       EVENT_SETCLOCKSS, EVENT_RESET,
			       EVENT_INPUTSTREAM),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubInaccessibleNeither1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_INACCESSIBLE);
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.getUncachedInputStream();
      fail("getUncachedInputStream() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM,
			       EVENT_SETCLOCKSS, EVENT_RESET,
			       EVENT_INPUTSTREAM),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubInaccessibleFail1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_INACCESSIBLE);
    muf.setResults(ListUtil.list(new CacheException.NoRetryDeadLinkException()));
    try {
      cuc.getUncachedInputStream();
      fail("getUncachedInputStream() didn't throw on fetch error");
    } catch (CacheException.NoRetryDeadLinkException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM),
		 muf.events);
    // Should still be Inaccessible
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownInst2() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muf.setResults(ListUtil.list(FetchResult.FETCHED));
    assertEquals(FetchResult.FETCHED, cuc.fetch());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE), muf.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownClockss2() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 FetchResult.FETCHED));
    assertEquals(FetchResult.FETCHED, cuc.fetch());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE,
			       EVENT_SETCLOCKSS, EVENT_RESET, EVENT_CACHE),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownNeither2() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.fetch();
      fail("cache()() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE,
			       EVENT_SETCLOCKSS, EVENT_RESET, EVENT_CACHE),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownFail2() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muf.setResults(ListUtil.list(new CacheException.NoRetryDeadLinkException()));
    try {
      cuc.fetch();
      fail("cache()() didn't throw on fetch error");
    } catch (CacheException.NoRetryDeadLinkException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE),
		 muf.events);
    // Should still be Unknown
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesInst2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    muf.setResults(ListUtil.list(FetchResult.FETCHED));
    assertEquals(FetchResult.FETCHED, cuc.fetch());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE), muf.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesClockss2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 FetchResult.FETCHED));
    assertEquals(FetchResult.FETCHED, cuc.fetch());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE,
			       EVENT_SETCLOCKSS, EVENT_RESET, EVENT_CACHE),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesNeither2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.fetch();
      fail("cache()() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE,
			       EVENT_SETCLOCKSS, EVENT_RESET, EVENT_CACHE),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesFail2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    muf.setResults(ListUtil.list(new CacheException.NoRetryDeadLinkException()));
    try {
      cuc.fetch();
      fail("cache()() didn't throw on fetch error");
    } catch (CacheException.NoRetryDeadLinkException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE),
		 muf.events);
    // Should still be Yes
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoClockss2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    muf.setResults(ListUtil.list(FetchResult.FETCHED));
    assertEquals(FetchResult.FETCHED, cuc.fetch());
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_CACHE),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoNeither2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.fetch();
      fail("cache()() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_CACHE),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoFail2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    muf.setResults(ListUtil.list(new CacheException.NoRetryDeadLinkException()));
    try {
      cuc.fetch();
      fail("cache()() didn't throw on fetch error");
    } catch (CacheException.NoRetryDeadLinkException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_CACHE),
		 muf.events);
    // Should still be No
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubInaccessibleInst2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_INACCESSIBLE);
    InputStream exp = new StringInputStream("");
    muf.setResults(ListUtil.list(FetchResult.FETCHED));
    assertEquals(FetchResult.FETCHED, cuc.fetch());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE), muf.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubInaccessibleClockss2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_INACCESSIBLE);
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 FetchResult.FETCHED));
    assertEquals(FetchResult.FETCHED, cuc.fetch());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE,
			       EVENT_SETCLOCKSS, EVENT_RESET, EVENT_CACHE),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubInaccessibleNeither2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_INACCESSIBLE);
    muf.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.fetch();
      fail("cache()() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE,
			       EVENT_SETCLOCKSS, EVENT_RESET,
			       EVENT_CACHE),
		 muf.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubInaccessibleFail2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_INACCESSIBLE);
    muf.setResults(ListUtil.list(new CacheException.NoRetryDeadLinkException()));
    try {
      cuc.fetch();
      fail("cache()() didn't throw on fetch error");
    } catch (CacheException.NoRetryDeadLinkException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE),
		 muf.events);
    // Should still be Inaccessible
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }


  static class MyUrlFetcher extends MockUrlFetcher {
    List events = new ArrayList();
    List results;

    public MyUrlFetcher(MockCrawlerFacade mcf, String url) {
      super(mcf, url);
    }

    void setResults(List res) {
      results = res;
    }

    public FetchResult fetch() throws CacheException {
      events.add(EVENT_CACHE);
      if (results.isEmpty()) {
        return FetchResult.FETCHED;
      }
      Object res = results.remove(0);
      if (res instanceof CacheException) {
        throw (CacheException)res;
      }
      return (FetchResult) res;
    }

    public InputStream getUncachedInputStream() throws IOException {
      events.add(EVENT_INPUTSTREAM);
      if (results.isEmpty()) {
	return RES_INPUT_STREAM;
      }
      Object res = results.remove(0);
      if (res instanceof IOException) {
	throw (IOException)res;
      }
      return (InputStream)res;
    }

    public void reset() {
      events.add(EVENT_RESET);
    }

    public void setLocalAddress(IPAddr addr) {
      events.add("setLocalAddress: " +
		 (addr == null ? "(null)" : addr.getHostAddress()));
    }
  }
}
