/*
 * $Id: TestNodeStateMap.java,v 1.3 2003-03-27 00:50:23 aalto Exp $
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

public class TestNodeStateMap extends LockssTestCase {
  private MockHistoryRepository repo;
  private String tempDirPath;
  private NodeStateMap map;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    String s = HistoryRepositoryImpl.PARAM_HISTORY_LOCATION +
        "=" + tempDirPath;
    TestConfiguration.setCurrentConfigFromString(s);

    repo = new MockHistoryRepository();
    map = new NodeStateMap(repo, 10);
  }

  public void testCaching() throws Exception {
    CachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com");
    assertEquals(10, map.getMaximumSize());

    NodeState node = (NodeState)map.get("http://www.example.com");
    assertEquals(0, map.getCacheHits());
    assertEquals(1, map.getCacheMisses());
    assertNull(node);

    node = new NodeStateImpl(mcus, null, new ArrayList(), repo);
    map.put("http://www.example.com", node);
    NodeState node2 = (NodeState)map.get("http://www.example.com");
    assertSame(node, node2);
    assertEquals(1, map.getCacheHits());
    assertEquals(1, map.getCacheMisses());
  }

  public void testWeakReferenceCaching() throws Exception {
    CachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com");

    NodeState node = (NodeState)map.get("http://www.example.com");
    assertEquals(1, map.getCacheMisses());
    assertEquals(1, map.getRefMisses());
    assertNull(node);

    node = new NodeStateImpl(mcus, null, null, repo);
    map.put("http://www.example.com", node);
    node = (NodeState)map.get("http://www.example.com");
    assertEquals(1, map.getCacheHits());

    NodeState node2 = null;
    int loopSize = 1;
    int refHits = 0;
    // create nodes in a loop until fetching the original creates a cache miss
    while (true) {
      loopSize *= 2;
      for (int ii=0; ii<loopSize; ii++) {
        map.put("http://www.example.com/test"+ii,
                new NodeStateImpl(mcus, null, null, repo));
      }
      int misses = map.getCacheMisses();
      refHits = map.getRefHits();
      node2 = (NodeState)map.get("http://www.example.com");
      if (map.getCacheMisses() == misses+1) {
        break;
      }
    }
    assertSame(node, node2);
    assertEquals(refHits+1, map.getRefHits());
  }

  public void testRemovingFromLRU() throws Exception {
    CachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com");
    NodeState node = new NodeStateImpl(mcus, null, null, repo);
    map.put("http://www.example.com", node);
    node = (NodeState)map.get("http://www.example.com");
    assertEquals(0, repo.storedNodes.size());

    for (int ii=0; ii<map.getMaximumSize(); ii++) {
      map.put("http://www.example.com/test"+ii,
              new NodeStateImpl(mcus, null, null, repo));
    }
    assertEquals(1, repo.storedNodes.size());
    NodeState node2 = (NodeState)repo.storedNodes.get(mcus);
    assertSame(node, node2);

    node2 = (NodeState)map.get("http://www.example.com");
    assertEquals(1, map.getCacheMisses());
  }

  public void testCallbackRefresh() throws Exception {
    NodeStateMap.Callback callback = map.theCallback;

    CachedUrlSet mcus = new MockCachedUrlSet("http://www.example.com");
    NodeState node = new NodeStateImpl(mcus, null, null, repo);
    map.put("http://www.example.com", node);

    mcus = new MockCachedUrlSet("http://www.example.com/test");
    NodeState node2 = new NodeStateImpl(mcus, null, null, repo);
    map.put("http://www.example.com/test", node2);

    callback.refreshInLRUMap("http://www.example.com", (NodeStateImpl)node);

    // put in n-1 new items
    for (int ii=0; ii<map.getMaximumSize()-1; ii++) {
      map.put("http://www.example.com/test"+ii,
              new NodeStateImpl(mcus, null, null, repo));
    }
    map.refMap.clear();
    // this should be knocked out
    assertNull(map.get("http://www.example.com/test"));
    // this should not be, since it was refreshed
    assertNotNull(map.get("http://www.example.com"));

  }

  public void testCallbackRemoval() throws Exception {
    NodeStateMap.Callback callback = map.theCallback;
    assertEquals(0, map.refMap.size());

    map.refMap.put("test", "nothing");
    assertEquals(1, map.refMap.size());
    assertEquals("nothing", map.refMap.get("test"));

    callback.removeReference("test");
    assertEquals(0, map.refMap.size());
    assertNull(map.refMap.get("test"));
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestNodeStateMap.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }

}
