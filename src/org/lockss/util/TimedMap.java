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

package org.lockss.util;

import java.util.*;

/**
 * <p>Title: </p>
 * <p>Description: Variant of maps where each entry has its own interval
 * and will expire accordingly.  </p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

abstract class TimedMap implements Map {

  Map entries;
  Map keytimes;
  Date lastRemoved;

  abstract void updateEntries();

  abstract boolean areThereExpiredEntries();

  public void clear() {
    entries.clear();
    keytimes.clear();
  }

  public boolean containsKey(Object key) {
    updateEntries();
    return entries.containsKey(key);
  }

  public boolean containsValue(Object value) {
    updateEntries();
    return entries.containsValue(value);
  }

  public Object get(Object key) {
    updateEntries();
    return entries.get(key);
  }

  public int hashCode() {
    return 2 * entries.hashCode() + 3 * keytimes.hashCode();
  }

  public boolean isEmpty() {
    updateEntries();
    return entries.isEmpty();
  }

  public int size() {
    updateEntries();
    return entries.size();
  }

  private class ExpiringIterator implements Iterator {
    private Iterator inner_iterator;
    ExpiringIterator(Iterator inner) {
      this.inner_iterator = inner;
    }
    void checkExpired() {
      if (TimedMap.this.areThereExpiredEntries()) {
       throw new TimedIteratorExpiredException();
     }
    }
    public boolean hasNext() {
      checkExpired();
      return inner_iterator.hasNext();
    }
    public Object next() {
      checkExpired();
      return inner_iterator.next();
   }
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported");
    }
  }

  private abstract class ExpiringSet extends AbstractSet {
    public int size() {
      return TimedMap.this.size();
    }
    public void clear() {
      TimedMap.this.clear();
    }
    public boolean isEmpty() {
      return TimedMap.this.isEmpty();
    }
    public boolean remove(Object obj) {
      boolean retval = contains(obj);
      TimedMap.this.remove(obj);
      return retval;
    }
    public boolean add(Object o) {
      throw new java.lang.UnsupportedOperationException(
      "Method add() not implemented.");
    }
    public boolean addAll(Collection c) {
      throw new java.lang.UnsupportedOperationException(
      "Method addAll() not implemented.");
    }
    public boolean equals(Object o) {
      throw new java.lang.UnsupportedOperationException(
      "Method equals() not implemented.");
    }
  }

  private transient Collection values = null;
  private transient ExpiringSet keySet = null;
  private transient ExpiringSet entrySet = null;

  public Set keySet() {
    if (keySet == null) {
      keySet = new ExpiringSet() {
        public boolean contains(Object obj) {
          return TimedMap.this.containsKey(obj);
        }
        public Iterator iterator() {
          return new ExpiringIterator(TimedMap.
          this.entries.keySet().iterator());
        }
      };
    }
    return keySet;
  }

  public Set entrySet()  {
    if (entrySet==null) {
      entrySet = new ExpiringSet() {
        public boolean contains(Object obj) {
          return TimedMap.this.entries.entrySet().contains(obj);
        }

        public Iterator iterator() {
          return new ExpiringIterator(TimedMap.
          this.entries.entrySet().iterator());
        }
      };
    }
    return entrySet;
  }

  public Collection values() {
    if (values == null) {
      values = new AbstractCollection() {
        public Iterator iterator() {
          return new ExpiringIterator(TimedMap.
          this.entries.values().iterator());
        }

        public int size() {
          return TimedMap.this.size();
        }
      };
    }
    return values;
  }

  public boolean equals(Object obj)
  {
    throw new UnsupportedOperationException("Equals operation not yet " +
                                            "implemented for this class.");
  }

}