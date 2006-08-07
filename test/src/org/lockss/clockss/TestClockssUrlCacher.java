/*
 * $Id: TestClockssUrlCacher.java,v 1.1 2006-08-07 07:39:08 tlipkis Exp $
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
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * test class for org.lockss.clockss.ClockssUrlCacher
 */
public class TestClockssUrlCacher extends LockssTestCase {
  protected static Logger logger = Logger.getLogger("TestClockssUrlCacher");

  static final String URL = "http://example.com/foo.html";
  static final String INST_ADDR = "44.55.77.88";
  static final String CLOCKSS_ADDR = "3.2.1.1";

  static final String EVENT_SETINST = "setLocalAddress: " + INST_ADDR;
  static final String EVENT_SETCLOCKSS = "setLocalAddress: " + CLOCKSS_ADDR;
  static final String EVENT_INPUTSTREAM = "getUncachedInputStream";
  static final String EVENT_PROPS = "getUncachedProperties";
  static final String EVENT_CACHE = "cache";

  MockPlugin mp;
  MockArchivalUnit mau;
  MyUrlCacher muc;
  ClockssUrlCacher cuc;
  AuState aus;

  public void setUp() throws Exception {
    super.setUp();
    MockLockssDaemon daemon = getMockLockssDaemon();
    setUpDiskPaths();
    mp = new MockPlugin();
    mp.initPlugin(daemon);
    mau = new MockArchivalUnit(mp);
    muc = new MyUrlCacher(URL, mau);
    cuc = new ClockssUrlCacher(muc);
    daemon.getNodeManager(mau).startService();
    aus = AuUtil.getAuState(mau);
    Properties p = new Properties();
    p.put(ClockssParams.PARAM_INSTITUTION_SUBSCRIPTION_ADDR, INST_ADDR);
    p.put(ClockssParams.PARAM_CLOCKSS_SUBSCRIPTION_ADDR, CLOCKSS_ADDR);
    ConfigurationUtil.addFromProps(p);
    daemon.getClockssParams().startService();
  }

//   public void testSubUnknownYes(List expectedEvents, List results, F fn)
//       throws Exception {
//     assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
// 		 aus.getClockssSubscriptionStatus());
//     muc.setResults(exp);
//     assertSame(exp, fn.f(cuc));
//     assertEquals(expectedEvents, muc.events);
//     assertEquals(AuState.CLOCKSS_SUB_YES,
// 		 aus.getClockssSubscriptionStatus());
//   }

  public void testSubUnknownInst1() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    InputStream exp = new StringInputStream("");
    muc.setResults(ListUtil.list(exp));
    assertSame(exp, cuc.getUncachedInputStream());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM), muc.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownClockss1() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    InputStream exp = new StringInputStream("");
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 exp));
    assertSame(exp, cuc.getUncachedInputStream());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM,
			       EVENT_SETCLOCKSS, EVENT_INPUTSTREAM),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownNeither1() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.getUncachedInputStream();
      fail("getUncachedInputStream() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM,
			       EVENT_SETCLOCKSS, EVENT_INPUTSTREAM),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesInst1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    InputStream exp = new StringInputStream("");
    muc.setResults(ListUtil.list(exp));
    assertSame(exp, cuc.getUncachedInputStream());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM), muc.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesClockss1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    InputStream exp = new StringInputStream("");
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 exp));
    assertSame(exp, cuc.getUncachedInputStream());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM,
			       EVENT_SETCLOCKSS, EVENT_INPUTSTREAM),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesNeither1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.getUncachedInputStream();
      fail("getUncachedInputStream() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_INPUTSTREAM,
			       EVENT_SETCLOCKSS, EVENT_INPUTSTREAM),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoClockss1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    InputStream exp = new StringInputStream("");
    muc.setResults(ListUtil.list(exp));
    assertSame(exp, cuc.getUncachedInputStream());
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_INPUTSTREAM),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoNeither1() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.getUncachedInputStream();
      fail("getUncachedInputStream() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_INPUTSTREAM),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownInst2() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    CIProperties exp = new CIProperties();
    muc.setResults(ListUtil.list(exp));
    assertSame(exp, cuc.getUncachedProperties());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_PROPS), muc.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownClockss2() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    CIProperties exp = new CIProperties();
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 exp));
    assertSame(exp, cuc.getUncachedProperties());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_PROPS,
			       EVENT_SETCLOCKSS, EVENT_PROPS),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownNeither2() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.getUncachedProperties();
      fail("getUncachedProperties() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_PROPS,
			       EVENT_SETCLOCKSS, EVENT_PROPS),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesInst2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    CIProperties exp = new CIProperties();
    muc.setResults(ListUtil.list(exp));
    assertSame(exp, cuc.getUncachedProperties());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_PROPS), muc.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesClockss2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    CIProperties exp = new CIProperties();
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 exp));
    assertSame(exp, cuc.getUncachedProperties());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_PROPS,
			       EVENT_SETCLOCKSS, EVENT_PROPS),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesNeither2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.getUncachedProperties();
      fail("getUncachedProperties() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_PROPS,
			       EVENT_SETCLOCKSS, EVENT_PROPS),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoClockss2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    CIProperties exp = new CIProperties();
    muc.setResults(ListUtil.list(exp));
    assertSame(exp, cuc.getUncachedProperties());
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_PROPS),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoNeither2() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.getUncachedProperties();
      fail("getUncachedProperties() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_PROPS),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownInst3() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muc.setResults(ListUtil.list(new Integer(47)));
    assertEquals(47, cuc.cache());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE), muc.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownClockss3() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new Integer(47)));
    assertEquals(47, cuc.cache());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE,
			       EVENT_SETCLOCKSS, EVENT_CACHE),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubUnknownNeither3() throws Exception {
    assertEquals(AuState.CLOCKSS_SUB_UNKNOWN,
		 aus.getClockssSubscriptionStatus());
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.cache();
      fail("cache()() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE,
			       EVENT_SETCLOCKSS, EVENT_CACHE),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesInst3() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    muc.setResults(ListUtil.list(new Integer(47)));
    assertEquals(47, cuc.cache());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE), muc.events);
    assertEquals(AuState.CLOCKSS_SUB_YES,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesClockss3() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new Integer(47)));
    assertEquals(47, cuc.cache());
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE,
			       EVENT_SETCLOCKSS, EVENT_CACHE),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubYesNeither3() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_YES);
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.cache();
      fail("cache()() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETINST, EVENT_CACHE,
			       EVENT_SETCLOCKSS, EVENT_CACHE),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoClockss3() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    muc.setResults(ListUtil.list(new Integer(47)));
    assertEquals(47, cuc.cache());
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_CACHE),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_NO,
		 aus.getClockssSubscriptionStatus());
  }

  public void testSubNoNeither3() throws Exception {
    aus.setClockssSubscriptionStatus(AuState.CLOCKSS_SUB_NO);
    muc.setResults(ListUtil.list(new CacheException.PermissionException(),
				 new CacheException.PermissionException()));
    try {
      cuc.cache();
      fail("cache()() didn't throw on inaccessible");
    } catch (CacheException.PermissionException e) {
    }
    assertEquals(ListUtil.list(EVENT_SETCLOCKSS, EVENT_CACHE),
		 muc.events);
    assertEquals(AuState.CLOCKSS_SUB_INACCESSIBLE,
		 aus.getClockssSubscriptionStatus());
  }


  static class MyUrlCacher extends MockUrlCacher {
    List events = new ArrayList();
    List results;

    public MyUrlCacher(String url, MockArchivalUnit au) {
      super(url, au);
    }

    void setResults(List res) {
      results = res;
    }

    public int cache() throws IOException {
      events.add(EVENT_CACHE);
      Object res = results.remove(0);
      if (res instanceof IOException) {
	throw (IOException)res;
      }
      return ((Integer)res).intValue();
    }

    public InputStream getUncachedInputStream() throws IOException {
      events.add(EVENT_INPUTSTREAM);
      Object res = results.remove(0);
      if (res instanceof IOException) {
	throw (IOException)res;
      }
      return (InputStream)res;
    }

    public CIProperties getUncachedProperties() throws IOException {
      events.add(EVENT_PROPS);
      Object res = results.remove(0);
      if (res instanceof IOException) {
	throw (IOException)res;
      }
      return (CIProperties)res;
    }

    public void reset() {
    }

    public void setLocalAddress(IPAddr addr) {
      events.add("setLocalAddress: " + addr.getHostAddress());
    }
  }
}
