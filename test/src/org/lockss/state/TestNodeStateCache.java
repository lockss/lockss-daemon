/*
 * $Id: TestNodeStateCache.java,v 1.4 2003-04-02 19:32:30 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.state;

import java.io.*;
import java.util.*;
import java.net.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.daemon.*;

/**
 * This is the test class for org.lockss.daemon.LockssRepositoryImpl
 */

public class TestNodeStateCache extends LockssTestCase {
  private MockHistoryRepository repo;
  private String tempDirPath;
  private NodeStateCache cache;

  public void setUp() throws Exception {
    super.setUp();

    cache = new NodeStateCache(10);
  }

  public void testMaxCacheSize() throws Exception {
    assertEquals(10, cache.getCacheSize());

    for (int ii=0; ii<11; ii++) {
      cache.putState("test"+ii, new NodeStateImpl());
    }
    assertEquals(10, cache.lruMap.size());

    cache.setCacheSize(20);
    assertEquals(20, cache.getCacheSize());

    for (int ii=0; ii<21; ii++) {
      cache.putState("test"+ii, new NodeStateImpl());
    }
    assertEquals(20, cache.lruMap.size());

    cache.setCacheSize(10);
    assertEquals(10, cache.getCacheSize());
    assertEquals(10, cache.lruMap.size());
  }

  public void testCaching() throws Exception {
    CachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com");

    NodeState node = cache.getState("http://www.example.com");
    assertEquals(0, cache.getCacheHits());
    assertEquals(1, cache.getCacheMisses());
    assertNull(node);

    node = new NodeStateImpl(mcus, null, new ArrayList(), repo);
    cache.putState("http://www.example.com", node);
    NodeState node2 = cache.getState("http://www.example.com");
    assertSame(node, node2);
    assertEquals(1, cache.getCacheHits());
    assertEquals(1, cache.getCacheMisses());
  }

  public void testWeakReferenceCaching() throws Exception {
    CachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com");

    NodeState node = cache.getState("http://www.example.com");
    assertEquals(1, cache.getCacheMisses());
    assertEquals(1, cache.getRefMisses());
    assertNull(node);

    node = new NodeStateImpl(mcus, null, null, repo);
    cache.putState("http://www.example.com", node);
    node = cache.getState("http://www.example.com");
    assertEquals(1, cache.getCacheHits());

    NodeState node2 = null;
    int loopSize = 1;
    int refHits = 0;
    // create nodes in a loop until fetching the original creates a cache miss
    while (true) {
      loopSize *= 2;
      for (int ii=0; ii<loopSize; ii++) {
        cache.putState("http://www.example.com/test"+ii,
                new NodeStateImpl(mcus, null, null, repo));
      }
      int misses = cache.getCacheMisses();
      refHits = cache.getRefHits();
      node2 = cache.getState("http://www.example.com");
      if (cache.getCacheMisses() == misses+1) {
        break;
      }
    }
    assertSame(node, node2);
    assertEquals(refHits+1, cache.getRefHits());
  }

  public void testRemovingFromLRU() throws Exception {
    CachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com");
    NodeState node = new NodeStateImpl(mcus, null, null, repo);
    cache.putState("http://www.example.com", node);
    node = cache.getState("http://www.example.com");

    for (int ii=0; ii<cache.getCacheSize(); ii++) {
      cache.putState("http://www.example.com/test"+ii,
              new NodeStateImpl(mcus, null, null, repo));
    }

    NodeState node2 = cache.getState("http://www.example.com");
    assertEquals(1, cache.getCacheMisses());
  }

  public void testSnapshot() throws Exception {
    CachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com");
    NodeState node = new NodeStateImpl(mcus, null, null, repo);
    cache.putState("http://www.example.com", node);

    mcus = new MockCachedUrlSet("http://www.example.com/test1");
    node = new NodeStateImpl(mcus, null, null, repo);
    cache.putState("http://www.example.com/test1", node);

    Iterator snapshot = cache.snapshot().iterator();
    assertTrue(snapshot.hasNext());
    snapshot.next();

    mcus = new MockCachedUrlSet("http://www.example.com/test2");
    node = new NodeStateImpl(mcus, null, null, repo);
    cache.putState("http://www.example.com/test2", node);

    assertTrue(snapshot.hasNext());
    snapshot.next();

    assertFalse(snapshot.hasNext());
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestNodeStateCache.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
