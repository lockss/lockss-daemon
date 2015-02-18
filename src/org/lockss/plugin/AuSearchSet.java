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

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.collections.map.LRUMap;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.plugin.PluginManager;

/**
 * The set of AUs that need to be searched find incoming URLs with a given
 * stem, with a cache of the most recently found AUs.  Updates can be
 * performed concurrently with iterations.
 * @ThreadSafe
 */
public class AuSearchSet implements Iterable<ArchivalUnit> {

  private static Logger log = Logger.getLogger("AuSearchSet");

  PluginManager pluginMgr;

  ConcurrentLinkedQueue aus;
  Collection<ArchivalUnit> sorted;
  AuSearchCache cache;
  int size;

  // Cache of recent 404s.  Lazily created as content in most AUs is never
  // accessed.
  volatile LRUMap recent404s;
  int recent404Size = 0;


  AuSearchSet() {
    aus = new ConcurrentLinkedQueue();
  }

  /** Create an AuSearchSet that will get it's configuration from
   * pluginMgr */
  public AuSearchSet(PluginManager pluginMgr) {
    this();
    this.pluginMgr = pluginMgr;
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    set404CacheSize(getConfigured404CacheSize());
  }

  /** Add an au to the set */
  public void addAu(ArchivalUnit au) {
    synchronized (this) {
      aus.add(au);
      sorted = null;
      size++;
      set404CacheSize(getConfigured404CacheSize());
    }
  }

  /** Remove an au from the set */
  public boolean delAu(ArchivalUnit au) {
    synchronized (this) {
      boolean res;
      if (res = aus.remove(au)) {
	sorted = null;
	size--;
      }
      if (cache != null) {
	cache = cache.remove(au);
      }
      return res;
    }
  }

  /** Add an AU to the front of the cache (or move it to the front if
   * already in the cache).  It is assumed that the AU already belongs to
   * the set. */
  public void addToCache(ArchivalUnit au) {
    synchronized (this) {
      if (cache == null) {
	if (size() >= 2) {
	  cache = new AuSearchCache(getConfiguredCacheSize(), au);
	  return;
	}
      } else {
	cache = cache.addToFront(au, getConfiguredCacheSize());
      }
    }
  }

  protected int getConfiguredCacheSize() {
    return pluginMgr.getAuSearchCacheSize(size());
  }

  protected int getConfigured404CacheSize() {
    return pluginMgr.getAuSearch404CacheSize(size());
  }

  /** Return an unordered collection of the AUs in the set.  The collection
   * is concurrently iterable, and unmodifiable. */
  public Collection<ArchivalUnit> getAllAus() {
    return Collections.unmodifiableCollection(aus);
  }

  /** Return the collection of AUs, sorted in title order */
  public Collection<ArchivalUnit> getSortedAus() {
    synchronized (this) {
      if (sorted == null) {
	SortedSet<ArchivalUnit> res = 
	  new TreeSet<ArchivalUnit>(new AuOrderComparator());
	res.addAll(aus);
	sorted = Collections.unmodifiableSortedSet(res);
      }
      return sorted;
    }
  }

  /** Return the number of AUs in the set */
  public int size() {
    return size;
  }

  /** Return true iff there are no AUs in the set */
  public boolean isEmpty() {
    return aus.isEmpty();
  }

  /** Return an iterator that returns all the AUs in the set, starting with
   * the one most recently added to the cache.  If modifications are made
   * while the iterator is active, the addedd or removed elements may or may
   * not be included in the iterator
   */
  public Iterator<ArchivalUnit> iterator() {
    synchronized (this) {
      return new AuSearchIterator();
    }
  }

  public  boolean isRecent404(String url) {
    LRUMap map = recent404s;
    if (map == null) {
      if (log.isDebug2()) log.debug2("isRecent404("+url+"): false (no cache)");
      return false;
    }
    synchronized (map) {
      boolean res = (map.get(url) != null);
      if (log.isDebug2()) log.debug2("isRecent404("+url+"): " + res);
      return res;
    }
  }

  public void addRecent404(String url) {
    if (isEmpty()) {
      return;
    }
    LRUMap map = recent404s;
    if (map == null) {
      if (recent404Size <= 0) {
	return;
      }

      map = new LRUMap(recent404Size);
      recent404s = map;
    }
    synchronized (map) {
      map.put(url, "");
    }
  }

  // Grow the 404 cache if necessary.  Never shrinks the cache; to avoid
  // churn when large batch of AUs is deactivated/reactivated.
  private void set404CacheSize(int newSize) {
    if (newSize < 0) {
      // shouldn't happen, but ignore rather than throw
      log.error("AuSearchSet size < 0");
    }
    LRUMap map = recent404s;
    if (map == null) {
      recent404Size = newSize;
      return;
    }
    synchronized (map) {
      if (newSize == 0) {
	if (log.isDebug2()) log.debug2("Removing 404 cache.");
	recent404s = null;
      } else if (map.maxSize() < newSize) {
	LRUMap newMap = new LRUMap(newSize);
	newMap.putAll(map);
	if (log.isDebug2()) log.debug2("Enlarging 404 cache.");
	recent404s = newMap;
      }
      recent404Size = newSize;
    }
  }

  public void flush404Cache() {
    if (log.isDebug2()) log.debug2("Flushing 404 cache.");
    recent404s = null;
  }

  class AuSearchIterator implements Iterator<ArchivalUnit> {
    AuSearchCache iterCache;
    Iterator<ArchivalUnit> iter;
    ArchivalUnit next;
    boolean inCache;
    
    AuSearchIterator() {
      if (cache == null || cache.isEmpty()) {
	iter = aus.iterator();
	inCache = false;
      } else {
	iterCache = cache;
	inCache = true;
	iter = cache.iterator();
      }
    }

    public boolean hasNext() {
      return findNext() != null;
    }

    public ArchivalUnit next() {
      ArchivalUnit au = findNext();
      if (au == null) {
	throw new NoSuchElementException();
      }
      next = null;
      return au;
    }

    public ArchivalUnit findNext() {
      if (next != null) {
	return next;
      }
      while (true) {
	if (iter.hasNext()) {
	  ArchivalUnit au = iter.next();
	  if (!inCache && iterCache != null && iterCache.contains(au)) {
	    continue;
	  }
	  next = au;
	  return au;
	}
	if (inCache) {
	  iter = aus.iterator();
	  inCache = false;
	  continue;
	}
	next = null;
	return next;
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }


  // synchronized by calls in outer class

  static class AuSearchCache {
    ArchivalUnit[] cachedAus;
    int capacity;

    AuSearchCache(int capacity, ArchivalUnit firstAu) {
      this.capacity = capacity;
      cachedAus = new ArchivalUnit[1];
      cachedAus[0] = firstAu;
    }

    private AuSearchCache(int capacity, ArchivalUnit[] cachedAus) {
      this.capacity = capacity;
      this.cachedAus = cachedAus;
    }

    AuSearchCache addToFront(ArchivalUnit au) {
      return addToFront(au, capacity);
    }

    AuSearchCache addToFront(ArchivalUnit au, int newCapacity) {
      int oldIndex = indexOf(au);
      if (oldIndex == 0) {
	// already at front
	return this;
      }
      int curLen = cachedAus.length;
      int newSize;
      if (oldIndex >= 0) {
	newSize = Math.min(curLen, newCapacity);
      } else {
	newSize = curLen < newCapacity ? curLen + 1 : newCapacity;
      }
      ArchivalUnit[] newArray = new ArchivalUnit[newSize];
      if (oldIndex >= 0) {
	System.arraycopy(cachedAus, 0, newArray, 1, oldIndex);
	int len2 = curLen - oldIndex - 1;
	if (len2 > 0) {
	  System.arraycopy(cachedAus, oldIndex + 1,
			   newArray, oldIndex + 1, len2);
	}
      } else {
	System.arraycopy(cachedAus, 0, newArray, 1,
			 curLen < newCapacity ? curLen : newCapacity - 1);
      }
      newArray[0] = au;
      return new AuSearchCache(newCapacity, newArray);
    }

    AuSearchCache remove(ArchivalUnit au) {
      int oldIndex = indexOf(au);
      if (oldIndex < 0) {
	// not there
	return this;
      }
      int curLen = cachedAus.length;
      ArchivalUnit[] newArray = new ArchivalUnit[curLen - 1];
      if (oldIndex >= 1) {
	System.arraycopy(cachedAus, 0, newArray, 0, oldIndex);
      }
      int len2 = curLen - oldIndex - 1;
	if (len2 > 0) {
	  System.arraycopy(cachedAus, oldIndex + 1,
			   newArray, oldIndex, len2);
	}
      return new AuSearchCache(capacity, newArray);
    }

    boolean isEmpty() {
      return cachedAus == null || cachedAus.length == 0;
    }

    boolean contains(ArchivalUnit au) {
      return indexOf(au) >= 0;
    }

    int indexOf(ArchivalUnit au) {
      if (cachedAus == null) {
	return -1;
      }
      for (int ix = 0; ix < cachedAus.length; ix++) {
	if (au == cachedAus[ix]) {
	  return ix;
	}
	if (cachedAus[ix] == null) {
	  // no more entries
	  return -1;
	}
      }
      return -1;
    }

    Iterator<ArchivalUnit> iterator() {
      if (cachedAus == null) {
	return CollectionUtil.EMPTY_ITERATOR;
      }
      return new AuSearchCacheIterator(cachedAus);
    }

    static class AuSearchCacheIterator implements Iterator<ArchivalUnit> {
      ArchivalUnit[] array;
      int index;
    
      AuSearchCacheIterator(ArchivalUnit[] array) {
	this.array = array;
	index = 0;
      }

      public boolean hasNext() {
        return (index < array.length && array[index] != null);
      }

      public ArchivalUnit next() {
        if (!hasNext()) {
	  throw new NoSuchElementException();
        }
        return array[index++];
      }

      public void remove() {
	throw new UnsupportedOperationException();
      }
    }
  }
}
