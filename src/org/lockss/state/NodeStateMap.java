/*
 * $Id: NodeStateMap.java,v 1.2 2003-03-20 00:01:35 aalto Exp $
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
 * Subclass of of the LRUMap.
 */
public class NodeStateMap extends LRUMap {
  /**
   * This parameter indicates the size of the LRUMap used by the node manager.
   */
  public static final String PARAM_NODESTATE_MAP_SIZE =
      Configuration.PREFIX + "nodestate.map.size";
  static final int DEFAULT_MAP_SIZE = 100;

  private HistoryRepository repository;
  private ReferenceMap refMap = new ReferenceMap(ReferenceMap.HARD,
                                                 ReferenceMap.WEAK);
  private int cacheHits = 0;
  private int cacheMisses = 0;
  private int refHits = 0;
  private int refMisses = 0;

  public NodeStateMap(HistoryRepository repo) {
    super();
    int mapSize = Configuration.getIntParam(PARAM_NODESTATE_MAP_SIZE,
                                            DEFAULT_MAP_SIZE);
    setMaximumSize(mapSize);
    repository = repo;
  }

  public void processRemovedLRU(Object key, Object value) {
//XXX bug?  Could change via reference, then not be saved
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
    refMap.put(key, value);
    return super.put(key, value);
  }

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

  int getCacheHits() { return cacheHits; }
  int getCacheMisses() { return cacheMisses; }
  int getRefHits() { return refHits; }
  int getRefMisses() { return refMisses; }

}
