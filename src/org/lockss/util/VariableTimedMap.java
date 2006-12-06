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

/**
 * Variant of TimedMap where each entry has its own interval
 * and will expire accordingly.
 * @author Tyrone Nicholas
 * @version 1.0
 */

public class VariableTimedMap extends TimedMap implements Map {
  SortedMap deadlineKeys;

  public VariableTimedMap() {
    entries = new HashMap();
    keytimes = new HashMap();
    deadlineKeys = new TreeMap();
  }

  void removeExpiredEntries() {
    while (!keytimes.isEmpty()) {
      Deadline dead = (Deadline)deadlineKeys.firstKey();
      if (dead.expired()) {
	List keys = (List)deadlineKeys.get(dead);
	for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
	  Object key = iter.next();
	  entries.remove(key);
	  keytimes.remove(key);
	}
        deadlineKeys.remove(dead);
      } else {
        return;
      }
    }
  }

  public void clear() {
    super.clear();
    deadlineKeys.clear();
  }

  public Object remove(Object key) {
    if (containsKey(key)) {
      removeKeyFromDeadlineKeys(key);
      keytimes.remove(key);
    }
    return entries.remove(key);
  }

  /** Add an antry that will expire in interval milliseconds. */
  public Object put(Object key, Object value, long interval) {
    return put(key, value, Deadline.in(interval));
  }

  /** Add an antry that will expire at the deadline. */
  public Object put(Object key, Object value, Deadline deadline) {
    removeExpiredEntries();
    if (containsKey(key)) {
      removeKeyFromDeadlineKeys(key);
    }
    keytimes.put(key, deadline);
    List keys = (List)deadlineKeys.get(deadline);
    if (keys == null) {
      keys = new ArrayList(4);
      deadlineKeys.put(deadline, keys);
    }
    keys.add(key);
    return entries.put(key, value);
  }

  private void removeKeyFromDeadlineKeys(Object key) {
    Deadline oldDead = (Deadline)keytimes.get(key);
    if (oldDead != null) {
      List keys = (List)deadlineKeys.get(oldDead);
      if (keys != null) {
	keys.remove(key);
      }
    }
  }

  public boolean equals(Object o) {
    return (o instanceof VariableTimedMap) &&
      entries.equals(((VariableTimedMap)o).getEntries());
  }

  public int hashCode() {
    return entries.hashCode();
  }

  public Object put(Object key, Object value) {
    throw new UnsupportedOperationException("Interval required.");
  }

  public void putAll(Map t) {
    throw new UnsupportedOperationException("putAll not supported.");
  }

  public void putAll(Map t, long interval) {
    putAll(t, Deadline.in(interval));
  }

  public void putAll(Map t, Deadline deadline) {
    removeExpiredEntries();
    Iterator i = t.entrySet().iterator();
    while (i.hasNext()) {
      Entry e = (Entry) i.next();
      put(e.getKey(), e.getValue(), deadline);
    }
  }
}
