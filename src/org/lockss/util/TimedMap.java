/*
 * $Id: TimedMap.java,v 1.6 2006-12-06 05:19:01 tlipkis Exp $
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
import org.apache.commons.collections.iterators.UnmodifiableIterator;

/**
 * Map in which entries are automatically removed after some interval.
 * Expired entries live until the map is next accessed.  The set views
 * (keySet(), entrySet(), values()) re unmodifiable.  Fetching any set view
 * updates the map, as does obtainsing an iterator over these sets.
 * Because get() can cause this map to be altered, set iterators may throw
 * throw ConcurrentModificationException even if the only operation
 * performed is get().  The map is not synchronised.  equals() and
 * hashCode() ignore the expiration times, considering only the map keys
 * and values.
 * @author Tyrone Nicholas
 * @version 1.0
 */
public abstract class TimedMap implements Map {

  Map entries;
  Map keytimes;

  /** Remove expired entries */
  abstract void removeExpiredEntries();

  public void clear() {
    entries.clear();
    keytimes.clear();
  }

  public boolean containsKey(Object key) {
    removeExpiredEntries();
    return entries.containsKey(key);
  }

  public boolean containsValue(Object value) {
    removeExpiredEntries();
    return entries.containsValue(value);
  }

  public Object get(Object key) {
    removeExpiredEntries();
    return entries.get(key);
  }

  public boolean isEmpty() {
    removeExpiredEntries();
    return entries.isEmpty();
  }

  public int size() {
    removeExpiredEntries();
    return entries.size();
  }

  /** Return true if the argument if a TimeMap of the same type with the
   * same contents, ignoring expiration times */
  public boolean equals(Object o) {
    if (o == null) return false;
    return (getClass() == o.getClass()) &&
      entries.equals(((TimedMap)o).getEntries());
  }

  public int hashCode() {
    return entries.hashCode();
  }

  /** allow other instances to get underlying entries for equals() */
  Map getEntries() {
    return entries;
  }

  private transient NestedSet keySet = null;
  private transient NestedSet entrySet = null;
  private transient NestedCollection values = null;

  public Set keySet() {
    removeExpiredEntries();
    if (keySet == null) {
      keySet = new NestedSet(this.entries.keySet());
    }
    return keySet;
  }

  public Set entrySet()  {
    removeExpiredEntries();
    if (entrySet==null) {
      entrySet = new NestedSet(this.entries.entrySet());
    }
    return entrySet;
  }

  public Collection values() {
    removeExpiredEntries();
    if (values == null) {
      values = new NestedCollection(this.entries.values());
    }
    return values;
  }

  // Set and collection views delegate to the underlying Set or Collection,
  // and are unmodifiable.  Obtaining a view or an iterator on a vire
  // triggers expired entry removal.

  static final String MSG_UNMOD = "Set views of TimedMaps are unmodifiable";

  private class NestedSet extends AbstractSet {
    private Set set;

    NestedSet(Set set) {
      this.set = set;
    }
    public int size() {
      removeExpiredEntries();
      return set.size();
    }
    public Iterator iterator() {
      removeExpiredEntries();
      return UnmodifiableIterator.decorate(set.iterator());
    }
    public boolean isEmpty() {
      removeExpiredEntries();
      return set.isEmpty();
    }
    public boolean equals(Object o) {
      return set.equals(o);
    }
    public int hashCode() {
      return set.hashCode();
    }
    public boolean remove(Object obj) {
      throw new UnsupportedOperationException(MSG_UNMOD);
    }
    public void clear() {
      throw new UnsupportedOperationException(MSG_UNMOD);
    }
    public boolean add(Object o) {
      throw new UnsupportedOperationException(MSG_UNMOD);
    }
  }

  private class NestedCollection extends AbstractCollection {
    private Collection coll;

    NestedCollection(Collection coll) {
      this.coll = coll;
    }
    public int size() {
      removeExpiredEntries();
      return coll.size();
    }
    public Iterator iterator() {
      removeExpiredEntries();
      return UnmodifiableIterator.decorate(coll.iterator());
    }
    public boolean isEmpty() {
      removeExpiredEntries();
      return coll.isEmpty();
    }
    public boolean equals(Object o) {
      return coll.equals(o);
    }
    public int hashCode() {
      return coll.hashCode();
    }
    public boolean remove(Object obj) {
      throw new UnsupportedOperationException(MSG_UNMOD);
    }
    public void clear() {
      throw new UnsupportedOperationException(MSG_UNMOD);
    }
    public boolean add(Object o) {
      throw new UnsupportedOperationException(MSG_UNMOD);
    }
  }
}
