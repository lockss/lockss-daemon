/*
 * $Id: NodeStateMap.java,v 1.4 2003-03-27 00:50:23 aalto Exp $
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

import org.lockss.daemon.Configuration;
import org.apache.commons.collections.LRUMap;
import org.apache.commons.collections.ReferenceMap;

/**
 * Subclass of the LRUMap, which holds {@link NodeState} objects and saves them
 * via the {@link HistoryRepository} upon removal.  Also contains a weak
 * reference map to protect against multiple instances of the same NodeState.
 */
public class NodeStateMap extends LRUMap {
  private HistoryRepository repository;
  ReferenceMap refMap = new ReferenceMap(ReferenceMap.HARD,
                                         ReferenceMap.WEAK);
  Callback theCallback = new Callback();

  private int cacheHits = 0;
  private int cacheMisses = 0;
  private int refHits = 0;
  private int refMisses = 0;

  public NodeStateMap(HistoryRepository repo, int maxSize) {
    super(maxSize);
    repository = repo;
  }

  public void processRemovedLRU(Object key, Object value) {
    super.processRemovedLRU(key, value);
    NodeState node = (NodeState)value;
    repository.storeNodeState(node);
  }

  public Object get(Object key) {
    Object obj = super.get(key);
    if (obj!=null) {
      cacheHits++;
      return obj;
    } else {
      cacheMisses++;
      obj = refMap.get(key);
      if (obj!=null) {
        refHits++;
        super.put(key, obj);
        return obj;
      } else {
        refMisses++;
        return null;
      }
    }
  }

  public Object put(Object key, Object value) {
    if (value instanceof NodeStateImpl) {
      ((NodeStateImpl)value).setMapCallback(theCallback);
      refMap.put(key, value);
      return super.put(key, value);
    } else {
      throw new IllegalArgumentException(
          "Putting a non-NodeStateImpl into NodeStateMap.");
    }
  }

  int getCacheHits() { return cacheHits; }
  int getCacheMisses() { return cacheMisses; }
  int getRefHits() { return refHits; }
  int getRefMisses() { return refMisses; }

  /**
   * An inner class which the NodeStateMap hands out to {@link NodeStateImpl}s
   * stored in it.  This allows them to make calls to the NodeStateMap.
   */
  class Callback {
    /**
     * Removes a weak reference from the reference map.  Called by finalize()
     * of {@link NodeStateImpl}.
     * @param urlKey the reference key url
     */
    synchronized void removeReference(String urlKey) {
      if (refMap!=null) {
        refMap.remove(urlKey);
      }
    }

    /**
     * This simply re-'puts' the object into the LRUMap, refreshing its
     * 'last-used' ranking.
     * @param urlKey the reference key url
     * @param node the {@link NodeStateImpl}
     */
    void refreshInLRUMap(String urlKey, NodeStateImpl node) {
      put(urlKey, node);
    }
  }

}
