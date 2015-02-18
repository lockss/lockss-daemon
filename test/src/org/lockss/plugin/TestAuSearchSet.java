/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.net.*;
import java.util.*;

import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.plugin.AuSearchSet
 */
public class TestAuSearchSet extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestAuSearchSet");

  MockArchivalUnit[] maus;

  public void setUp() throws Exception {
    super.setUp();
  }

  void setUpAus(int n) {
    maus = new MockArchivalUnit[n];
    for (int ix = 0; ix < n; ix++) {
      MockArchivalUnit mau = new MockArchivalUnit("au" + ix);
      mau.setName("au" + ix + " name");
      maus[ix] = mau;
    }
  }

  void setUpSizeFuncs() {
    ConfigurationUtil.setFromArgs(PluginManager.PARAM_AU_SEARCH_404_CACHE_SIZE,
				  "[1,1],[2,2],[3,3],[4,4],[5,6]",
				  PluginManager.PARAM_AU_SEARCH_CACHE_SIZE,
				  "[1,1],[2,2],[3,3],[4,5]");
  }


  Collection sortedAus(Collection aus) {
    Set<ArchivalUnit> res = new TreeSet<ArchivalUnit>(new AuOrderComparator());
    res.addAll(aus);
    return res;
  }

  void assertEmpty(AuSearchSet ss) {
    assertTrue(ss.isEmpty());
    assertEquals(0, ss.size());
    assertFalse(ss.iterator().hasNext());
    assertEmpty(ListUtil.fromIterator(ss.iterator()));
    assertEmpty(ss.getAllAus());
    assertEmpty(ss.getSortedAus());
    try {
      ss.iterator().next();
      fail("Empty AuSearchSet.iterator().next() should throw");
    } catch (NoSuchElementException e) {
    }
  }

  void assertHasAus(List exp, AuSearchSet ss) {
    assertFalse(ss.isEmpty());
    assertEquals(exp.size(), ss.size());
    assertEquals(exp, ListUtil.fromIterator(ss.iterator()));
    assertSameElements(exp, ss.getAllAus());
    assertIsomorphic(sortedAus(exp), ss.getSortedAus());
  }


  public void testEmpty() {
    AuSearchSet ss = new AuSearchSet();
    assertEmpty(ss);
    ss.delAu(new MockArchivalUnit());
    assertEmpty(ss);
    ss.addToCache(new MockArchivalUnit());
  }

  public void testOne() {
    MockArchivalUnit mau = new MockArchivalUnit();
    AuSearchSet ss = new MyAuSearchSet();
    ss.addAu(mau);
    assertHasAus(ListUtil.list(mau), ss);
    ss.addToCache(mau);
    assertHasAus(ListUtil.list(mau), ss);
    ss.delAu(mau);
    assertEmpty(ss);
  }

  public void testTwo() {
    setUpAus(2);
    MockArchivalUnit mau1 = maus[0];
    MockArchivalUnit mau2 = maus[1];
    MyAuSearchSet ss = new MyAuSearchSet().setCacheSize(1);
    ss.addAu(mau1);
    ss.addAu(mau2);
    assertHasAus(ListUtil.list(mau1, mau2), ss);
    ss.addToCache(mau2);
    assertHasAus(ListUtil.list(mau2, mau1), ss);
    ss.delAu(mau2);
    assertHasAus(ListUtil.list(mau1), ss);
  }

  List byIndices(final int... indices) {
    return new ArrayList() {{
      for (int ix : indices) {
	add(maus[ix]);
      }
    }};
  }

  public void testTen() {
    setUpAus(10);
    MyAuSearchSet ss = new MyAuSearchSet().setCacheSize(3);
    for (MockArchivalUnit mau : maus) {
      ss.addAu(mau);
    }

    assertHasAus(byIndices(0,1,2,3,4,5,6,7,8,9), ss);
    ss.addToCache(maus[6]);
    assertHasAus(byIndices(6,0,1,2,3,4,5,7,8,9), ss);
    ss.addToCache(maus[2]);
    assertHasAus(byIndices(2,6,0,1,3,4,5,7,8,9), ss);
    ss.addToCache(maus[6]);
    assertHasAus(byIndices(6,2,0,1,3,4,5,7,8,9), ss);
    ss.addToCache(maus[5]);
    assertHasAus(byIndices(5,6,2,0,1,3,4,7,8,9), ss);
    ss.addToCache(maus[1]);
    assertHasAus(byIndices(1,5,6,0,2,3,4,7,8,9), ss);

    ss.delAu(maus[3]);
    assertHasAus(byIndices(1,5,6,0,2,4,7,8,9), ss);
    ss.delAu(maus[1]);
    assertHasAus(byIndices(5,6,0,2,4,7,8,9), ss);
    ss.addToCache(maus[9]);
    assertHasAus(byIndices(9,5,6,0,2,4,7,8), ss);
    ss.delAu(maus[5]);
    assertHasAus(byIndices(9,6,0,2,4,7,8), ss);
    ss.addToCache(maus[7]);
    assertHasAus(byIndices(7,9,6,0,2,4,8), ss);
    ss.delAu(maus[6]);
    assertHasAus(byIndices(7,9,0,2,4,8), ss);

    ss.setCacheSize(1);
    assertHasAus(byIndices(7,9,0,2,4,8), ss);
    ss.addToCache(maus[0]);
    assertHasAus(byIndices(0,2,4,7,8,9), ss);
    ss.addToCache(maus[4]);
    assertHasAus(byIndices(4,0,2,7,8,9), ss);
  }

  public void testConcurrent() {
    setUpAus(11);
    MyAuSearchSet ss = new MyAuSearchSet().setCacheSize(3);
    for (MockArchivalUnit mau : maus) {
      if (mau == maus[10]) {
	break;
      }
      ss.addAu(mau);
    }
    assertHasAus(byIndices(0,1,2,3,4,5,6,7,8,9), ss);
    Iterator iter = ss.iterator();
    assertEquals(byIndices(0,1), ListUtil.list(iter.next(), iter.next()));
    ss.addAu(maus[10]);
    assertEquals(byIndices(2,3,4,5,6,7,8,9,10), ListUtil.fromIterator(iter));
    ss.addToCache(maus[6]);
    ss.addToCache(maus[8]);
    ss.addToCache(maus[7]);
    iter = ss.iterator();
    assertEquals(byIndices(7,8), ListUtil.list(iter.next(), iter.next()));
    assertTrue(ss.delAu(maus[9]));
    assertEquals(byIndices(6,0,1,2,3,4,5,10), ListUtil.fromIterator(iter));
    assertHasAus(byIndices(7,8,6,0,1,2,3,4,5,10), ss);

    iter = ss.iterator();
    assertEquals(byIndices(7,8), ListUtil.list(iter.next(), iter.next()));
    assertTrue(ss.delAu(maus[6]));
    assertTrue(ss.delAu(maus[4]));
    assertEquals(byIndices(6,0,1,2,3,5,10), ListUtil.fromIterator(iter));
    assertHasAus(byIndices(7,8,0,1,2,3,5,10), ss);
  }

  public void test404Cache() {
    MockArchivalUnit mau = new MockArchivalUnit("mau1");
    MyAuSearchSet ss = new MyAuSearchSet().set404CacheSize0(0);
    ss.addAu(mau);
    String url1 = "url1";
    String url2 = "url2";
    String url3 = "url3";
    String url4 = "url4";
    String url5 = "url5";
    assertFalse(ss.isRecent404(url1));
    ss.addRecent404(url1);
    assertFalse(ss.isRecent404(url1));

    ss = new MyAuSearchSet().set404CacheSize0(1);
    ss.addAu(mau);
    assertFalse(ss.isRecent404(url1));
    ss.addRecent404(url1);

    assertTrue(ss.isRecent404(url1));
    assertFalse(ss.isRecent404(url2));
    ss.addRecent404(url2);
    assertFalse(ss.isRecent404(url1));
    assertTrue(ss.isRecent404(url2));
    ss.set404CacheSize0(2);
    ss.addAu(new MockArchivalUnit("mau2"));
    assertFalse(ss.isRecent404(url1));
    assertTrue(ss.isRecent404(url2));
    ss.addRecent404(url1);
    assertTrue(ss.isRecent404(url1));
    assertTrue(ss.isRecent404(url2));
    ss.addRecent404(url3);
    assertFalse(ss.isRecent404(url1));
    assertTrue(ss.isRecent404(url3));
    assertTrue(ss.isRecent404(url2));
    ss.addRecent404(url4);
    assertFalse(ss.isRecent404(url1));
    assertFalse(ss.isRecent404(url3));
    assertTrue(ss.isRecent404(url2));
    assertTrue(ss.isRecent404(url4));

    ss.set404CacheSize0(4);
    ss.addAu(new MockArchivalUnit("mau3"));
    assertFalse(ss.isRecent404(url1));
    assertFalse(ss.isRecent404(url3));
    assertTrue(ss.isRecent404(url4));
    assertTrue(ss.isRecent404(url2));

    ss.addRecent404(url1);
    assertTrue(ss.isRecent404(url1));
    assertFalse(ss.isRecent404(url3));
    assertTrue(ss.isRecent404(url4));
    assertTrue(ss.isRecent404(url2));

    ss.addRecent404(url5);
    assertFalse(ss.isRecent404(url3));
    assertTrue(ss.isRecent404(url4));
    assertTrue(ss.isRecent404(url2));
    assertTrue(ss.isRecent404(url5));
    assertTrue(ss.isRecent404(url1));

    ss.addRecent404(url3);
    assertTrue(ss.isRecent404(url1));
    assertTrue(ss.isRecent404(url3));
    assertFalse(ss.isRecent404(url4));
    assertTrue(ss.isRecent404(url2));
    assertTrue(ss.isRecent404(url5));

  }

  class MyAuSearchSet extends AuSearchSet {
    int mockSize = -1;
    int cacheSize404 = -1;
    int cacheSize = -1;

    MyAuSearchSet setSize(int size) {
      this.mockSize = size;
      return this;
    }      

    private MyAuSearchSet setCacheSize(int cacheSize) {
      this.cacheSize = cacheSize;
      return this;
    }      

    private MyAuSearchSet set404CacheSize0(int cacheSize) {
      this.cacheSize404 = cacheSize;
      return this;
    }      

    @Override
    public int size() {
      if (mockSize < 0) {
	return super.size();
      } else {
	return mockSize;
      }
    }

    @Override
    protected int getConfiguredCacheSize() {
      if (cacheSize >= 0) {
	return cacheSize;
      } else {
	String func =
	  CurrentConfig.getParam(PluginManager.PARAM_AU_SEARCH_CACHE_SIZE,
				 PluginManager.DEFAULT_AU_SEARCH_CACHE_SIZE);
	return new IntStepFunction(func).getValue(size());
      }
    }

    @Override
    protected int getConfigured404CacheSize() {
      if (cacheSize404 >= 0) {
	return cacheSize404;
      } else {
	String func =
	  CurrentConfig.getParam(PluginManager.PARAM_AU_SEARCH_404_CACHE_SIZE,
				 PluginManager.DEFAULT_AU_SEARCH_404_CACHE_SIZE);
	return new IntStepFunction(func).getValue(size());
      }
    }

  }


}
