/*
 * $Id: UniqueRefLruCache.java,v 1.1 2004-10-18 03:25:35 tlipkis Exp $
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
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.ReferenceMap;

/**
 * An LRU cache that guarantees to hold and return a reference to an object
 * as long as anyone else does.  Intended to cache objects for which
 * multiple instances per key must not exist.  This cache is synchronized.
 */
public class UniqueRefLruCache {
  LRUMap lruMap;
  ReferenceMap refMap;

  // logging variables
  private int cacheHits = 0;
  private int cacheMisses = 0;
  private int refHits = 0;
  private int refMisses = 0;

  /**
   * Standard constructor.  Size must be positive.
   * @param maxSize maximum size for the LRUMap
   */
  public UniqueRefLruCache(int maxSize) {
    if (maxSize<=0) {
      throw new IllegalArgumentException("Negative cache size");
    }
    lruMap = new LRUMap(maxSize);
    refMap = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
  }

  /**
   * Returns the max cache size.
   * @return the max size
   */
  public synchronized int getMaxSize() {
    return lruMap.maxSize();
  }

  /**
   * Sets the NodeState cache size.
   * @param newSize the new size
   */
  public synchronized void setMaxSize(int newSize) {
    if (newSize<=0) {
      throw new IllegalArgumentException("Negative cache size");
    }
    if (lruMap.maxSize() != newSize) {
      LRUMap newMap = new LRUMap(newSize);
      newMap.putAll(lruMap);
      lruMap = newMap;
    }
  }

  /**
   * Get an object from the cache.
   * @param key the key
   * @return the corresponding object, or null if no such object exists in
   * memory.
   */
  public synchronized Object get(Object key) {
    // first check the LRUMap
    Object obj = lruMap.get(key);
    if (obj!=null) {
      cacheHits++;
      return obj;
    } else {
      cacheMisses++;
      // then check the refMap to see if one is still in use
      obj = refMap.get(key);
      if (obj!=null) {
        refHits++;
        // if found, put back in LRUMap
        lruMap.put(key, obj);
        return obj;
      } else {
        refMisses++;
        return null;
      }
    }
  }

  /**
   * Put an object in the cache.
   * @param key the key
   * @param obj the Object
   */
  public synchronized void put(Object key, Object obj) {
    refMap.put(key, obj);
    lruMap.put(key, obj);
  }

  /**
   * Returns a snapshot of the values in the cache
   * @return a Set of Objects
   */
  public synchronized Set snapshot() {
    return new HashSet(lruMap.values());
  }

  /**
   * Clears the cache
   */
  public synchronized void clear() {
    lruMap.clear();
    refMap.clear();
  }

  // logging accessors

  public int getCacheHits() { return cacheHits; }
  public int getCacheMisses() { return cacheMisses; }
  public int getRefHits() { return refHits; }
  public int getRefMisses() { return refMisses; }
}
