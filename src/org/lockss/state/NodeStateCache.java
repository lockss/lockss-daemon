/*
 * $Id: NodeStateCache.java,v 1.9 2004-04-01 02:44:32 eaalto Exp $
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

package org.lockss.state;

import java.util.*;
import org.apache.commons.collections.LRUMap;
import org.apache.commons.collections.ReferenceMap;

/**
 * Wrapper for an LRUMap, which holds {@link NodeState} objects and saves them
 * via the {@link HistoryRepository} upon removal.  Also contains a weak
 * reference map to protect against multiple instances of the same NodeState.
 */
public class NodeStateCache {
  LRUMap lruMap;
  ReferenceMap refMap = new ReferenceMap(ReferenceMap.HARD,
                                         ReferenceMap.WEAK);

  // logging variables
  private int cacheHits = 0;
  private int cacheMisses = 0;
  private int refHits = 0;
  private int refMisses = 0;

  /**
   * Standard constructor.  Size must be positive.
   * @param maxSize maximum size for the LRUMap
   */
  public NodeStateCache(int maxSize) {
    if (maxSize<=0) {
      throw new IllegalArgumentException("Cache size must be greater than zero");
    }
    lruMap = new LRUMap(maxSize);
  }

  /**
   * Returns the NodeState cache size.
   * @return the size
   */
  public int getCacheSize() {
    return lruMap.getMaximumSize();
  }

  /**
   * Sets the NodeState cache size.
   * @param newSize the new size
   */
  public void setCacheSize(int newSize) {
    if (newSize<=0) {
      throw new IllegalArgumentException("Cache size must be greater than zero");
    }
    lruMap.setMaximumSize(newSize);
  }

  /**
   * Gets a {@link NodeState} from the cache.
   * @param urlKey the url key
   * @return the {@link NodeState}
   */
  public synchronized NodeState getState(String urlKey) {
    // first check the LRUMap
    NodeState node = (NodeState)lruMap.get(urlKey);
    if (node!=null) {
      cacheHits++;
      return node;
    } else {
      cacheMisses++;
      // then check the refMap to see if one is still in use
      node = (NodeState)refMap.get(urlKey);
      if (node!=null) {
        refHits++;
        // if found, put back in LRUMap
        lruMap.put(urlKey, node);
        return node;
      } else {
        refMisses++;
        return null;
      }
    }
  }

  /**
   * Puts a NodeState in the cache.
   * @param urlKey the url
   * @param node the {@link NodeState}
   */
  public synchronized void putState(String urlKey, NodeState node) {
    refMap.put(urlKey, node);
    lruMap.put(urlKey, node);
  }

  /**
   * Returns a snapshot set of the cache's entries.
   * @return an {@link Set} of {@link NodeState}s
   */
  public synchronized Set snapshot() {
    return new HashSet(lruMap.values());
  }

  /**
   * Clears both the LRUMap and the reference map.
   */
  public void clear() {
    lruMap.clear();
    refMap.clear();
  }

  // logging accessors

  int getCacheHits() { return cacheHits; }
  int getCacheMisses() { return cacheMisses; }
  int getRefHits() { return refHits; }
  int getRefMisses() { return refMisses; }

}
