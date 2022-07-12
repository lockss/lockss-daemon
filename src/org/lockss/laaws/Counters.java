/*

Copyright (c) 2021-2022 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.laaws;
import java.util.*;
import org.apache.commons.collections4.*;
import org.apache.commons.collections4.multimap.*;
import org.lockss.util.*;
import org.lockss.laaws.V2AuMover.Phase;

/** Collection of named counters.  If has a parent Counters, they are
 * incremented also.  Thread safe. */
public class Counters {
  private static final Logger log = Logger.getLogger(V2AuMover.class);

  /** Counter types */
  public enum CounterType {
    URLS_MOVED,
    URLS_SKIPPED,
    ARTIFACTS_MOVED,
    ARTIFACTS_SKIPPED,
    BYTES_MOVED,
    CONTENT_BYTES_MOVED,
    URLS_VERIFIED,
    ARTIFACTS_VERIFIED,
    BYTES_VERIFIED,
    CONTENT_BYTES_VERIFIED,
    COPY_TIME,
    VERIFY_TIME,
    INDEX_TIME,
    STATE_TIME
  }

  Map<CounterType,Counter> counters = new HashMap<>();
  MultiValuedMap<CounterType,DigestCachedUrl> inProgressDcuMap =
    new ArrayListValuedHashMap<>();
  private List<String> errors = new ArrayList<>();
  private Counters parent;


  public Counters() {
    for (CounterType type : CounterType.values()) {
      counters.put(type, new Counter());
    }
  }

  public Counters setParent(Counters parent) {
    this.parent = parent;
    return this;
  }

  public synchronized Counter get(CounterType type) {
    return counters.get(type);
  }

  public long getVal(CounterType type) {
    return counters.get(type).getVal() + inProgressBytes(type);
  }

  public synchronized long inProgressBytes(CounterType type) {
    long sum = 0;
    for (DigestCachedUrl dcu : inProgressDcuMap.get(type)) {
      switch (type) {
      case BYTES_MOVED:
        sum += dcu.getTotalBytesRead();
        break;
      case CONTENT_BYTES_MOVED:
        sum += dcu.getContentBytesRead();
        break;
      }
    }
    return sum;
  }

  public boolean isNonZero(CounterType type) {
    return counters.get(type).getVal() > 0;
  }

  public void incr(CounterType type) {
    counters.get(type).incr();
    if (parent != null) {
      parent.incr(type);
    }
  }

  public void add(CounterType type, long val) {
    counters.get(type).add(val);
    if (parent != null) {
      parent.add(type, val);
    }
  }

  public void addError(String msg) {
    synchronized (errors) {
      errors.add(msg);
    }
    if (parent != null) {
      parent.addError(msg);
    }
  }

  public List<String> getErrors() {
    synchronized (errors) {
      return new ArrayList<>(errors);
    }
  }

  public synchronized void add(Counters ctrs) {
    for (CounterType type : CounterType.values()) {
      get(type).add(ctrs.get(type));
    }
    synchronized (errors) {
      errors.addAll(ctrs.errors);
    }
  }

  public synchronized void addInProgressDcu(CounterType type,
                                            DigestCachedUrl dcu) {
    inProgressDcuMap.put(type, dcu);
    if (parent != null) {
      parent.addInProgressDcu(type, dcu);
    }
  }

  public synchronized void removeInProgressDcu(CounterType type,
                                               DigestCachedUrl dcu) {
    inProgressDcuMap.removeMapping(type, dcu);
    if (parent != null) {
      parent.removeInProgressDcu(type, dcu);
    }
  }

  public synchronized void clearInProgressDcus(CounterType type) {
    inProgressDcuMap.remove(type);
  }

  /** Individual counter */
  static class Counter {
    private long val = 0;

    public Counter() {
    }

    public synchronized void incr() {
      val++;
    }

    public synchronized void add(long incr) {
      val += incr;
    }

    public synchronized void add(Counter ctr) {
      val += ctr.getVal();
    }

    public synchronized void setVal(long val) {
      this.val = val;
    }

    public synchronized long getVal() {
      return val;
    }
  }
}

