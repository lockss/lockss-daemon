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
 * Variant of TimedMap each entry has its own interval
 * and will expire accordingly.
 * @author Tyrone Nicholas
 * @version 1.0
 */

public class VariableTimedMap extends TimedMap implements Map {

  public VariableTimedMap() {
    entries = new HashMap();
    keytimes = new TreeMap();
  }

 void updateEntries()
  {
    while (!entries.isEmpty())
    {
      Deadline entry = (Deadline)((SortedMap)keytimes).firstKey();
      if (entry.expired())
      {
        Object obj = keytimes.get(entry);
        keytimes.remove(entry);
        entries.remove(obj);
      }
      else
        return;
    }
  }

  boolean areThereExpiredEntries() {
    if (keytimes.isEmpty()) {
      return false;
    }
    Deadline first = (Deadline)((SortedMap)keytimes).firstKey();
    return first.expired();
  }

  public Object remove(Object key) {
    if (containsKey(key)) {
      keytimes.values().remove(key);
    }
    return entries.remove(key);
  }

  public Object put(Object key, Object value, int interval)
  {
    updateEntries();
    Object oldkey = null;
    if (containsKey(key)) {
      keytimes.values().remove(key);
    }
    Deadline deadline = Deadline.in(interval);
    keytimes.put(deadline,key);
    return entries.put(key, value);
  }

  public Object put(Object key, Object value) {
    throw new UnsupportedOperationException("Interval required.");
  }

  public void putAll(Map t) {
    throw new UnsupportedOperationException("putAll not supported.");
  }

}
