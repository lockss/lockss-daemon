/*
 * $Id$
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

package org.lockss.util;

import java.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;

/**
 * This is the test class for org.lockss.daemon.UniqueRefLruCache
 */
public class TestUniqueRefLruCache extends LockssTestCase {
  private UniqueRefLruCache cache;

  public void setUp() throws Exception {
    super.setUp();
    cache = new UniqueRefLruCache(10);
  }

  public void testMaxSize() throws Exception {
    assertEquals(10, cache.getMaxSize());

    for (int ii=0; ii<11; ii++) {
      cache.put("test"+ii, new Object());
    }
    assertEquals(10, cache.lruMap.size());

    cache.setMaxSize(20);
    assertEquals(20, cache.getMaxSize());

    for (int ii=0; ii<21; ii++) {
      cache.put("test"+ii, new Object());
    }
    assertEquals(20, cache.lruMap.size());

    cache.setMaxSize(10);
    assertEquals(10, cache.getMaxSize());
    assertEquals(10, cache.lruMap.size());

    try {
      cache.setMaxSize(0);
      fail("Should have thrown IllegalArgumentException.");
    } catch (IllegalArgumentException iae) { }

    try {
      cache = new UniqueRefLruCache(0);
      fail("Should have thrown IllegalArgumentException.");
    } catch (IllegalArgumentException iae) { }
  }

  public void testCaching() throws Exception {
    Object obj = cache.get("foo");
    assertEquals(0, cache.getCacheHits());
    assertEquals(1, cache.getCacheMisses());
    assertNull(obj);

    obj = new Object();
    cache.put("foo", obj);
    Object obj2 = cache.get("foo");
    assertSame(obj, obj2);
    assertEquals(1, cache.getCacheHits());
    assertEquals(1, cache.getCacheMisses());
  }

  public void testWeakReferenceCaching() throws Exception {
    Object obj = cache.get("bar");
    assertEquals(1, cache.getCacheMisses());
    assertEquals(1, cache.getRefMisses());
    assertNull(obj);

    obj = new Object();
    cache.put("bar", obj);
    obj = cache.get("bar");
    assertEquals(1, cache.getCacheHits());

    Object obj2 = null;
    int loopSize = 1;
    int refHits = 0;
    // create objs in a loop until fetching the original creates a cache miss
    while (true) {
      loopSize *= 2;
      for (int ii=0; ii<loopSize; ii++) {
        cache.put("key_" + ii, new Object());
      }
      int misses = cache.getCacheMisses();
      refHits = cache.getRefHits();
      obj2 = cache.get("bar");
      if (cache.getCacheMisses() == misses+1) {
        break;
      }
    }
    assertSame(obj, obj2);
    assertEquals(refHits+1, cache.getRefHits());
  }

  public void testPutIfNew() throws Exception {
    Object o1 = cache.get("foo");
    assertEquals(0, cache.getCacheHits());
    assertEquals(1, cache.getCacheMisses());
    assertNull(o1);

    Object o2 = new Object();
    Object o3 = cache.putIfNew("foo", o2);
    assertSame(o2, o3);
    assertSame(o2, cache.get("foo"));

    Object o4 = new Object();
    Object o5 = cache.putIfNew("foo", o4);
    assertSame(o2, o5);
    assertNotSame(o4, o5);
    assertSame(o2, cache.get("foo"));
  }

  public void testRemovingFromLRU() throws Exception {
    Object obj = new Object();
    cache.put("baz", obj);
    obj = cache.get("baz");

    for (int ii=0; ii<cache.getMaxSize(); ii++) {
      cache.put("baz/test"+ii, new Object());
    }

    Object obj2 = cache.get("baz");
    assertEquals(1, cache.getCacheMisses());
  }

  public void testSnapshot() throws Exception {
    Object obj = new Object();
    cache.put("frob", obj);

    obj = new Object();
    cache.put("frob/test1", obj);

    Iterator snapshot = cache.snapshot().iterator();
    assertTrue(snapshot.hasNext());
    snapshot.next();

    obj = new Object();
    cache.put("frob/test2", obj);

    assertTrue(snapshot.hasNext());
    snapshot.next();

    assertFalse(snapshot.hasNext());
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestUniqueRefLruCache.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
