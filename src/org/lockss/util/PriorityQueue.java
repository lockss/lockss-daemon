/*
 * $Id: PriorityQueue.java,v 1.7 2002-11-20 19:42:42 tal Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
 * A thread-safe priority queue
 * Elements must implement the Comparable interface (or be accepted
 * by the specified Comparator).
 * Elements are sorted in order upon insertion.  If any element changes
 * in a way that might affect its sort order, {@link #sort()} must be
 * called to resort the queue.
 */
public class PriorityQueue implements Queue {
  private Vector queue = new Vector();
  Comparator comparator = null;

  /** 
   * Create a new priority queue. 
   */
  public PriorityQueue() {
  }

  /** 
   * Create a new priority queue with the specified Comparator.
   */
  public PriorityQueue(Comparator c) {
    comparator = c;
  }

  private int compare(Object o1, Object o2) {
    return (comparator == null
            ? ((Comparable)o1).compareTo(o2)
            : comparator.compare(o1, o2));
  }

  /**
   * Insert an object in the queue. */
  public synchronized Object put(Object obj) {
    if (obj == null) {
      throw new NullPointerException("Attempt to put null element on Queue");
    }
    int size = queue.size();
    int ix = 0;
    for (; ix < size; ix++) {
      if (compare(obj, queue.elementAt(ix)) < 0) {
        queue.insertElementAt(obj, ix);
        break;
      }
    }
    if (ix >= size) {
      queue.addElement(obj);
    }
    // supposedly allows the highest priority thread to run first
    notifyAll();
    return obj;
  }

  /** 
   * Remove from the beginning of the queue. Does not return until
   * an object appears in the queue and becomes available to this thread.
   * @return the item formerly at the head of the queue, or null if a
   * timeout occurred before an item was available.
   * @throws InterruptedException if interrupted while waiting
   */
  public synchronized Object get(Deadline timer) throws InterruptedException {
    Object head = peekWait(timer);
    if (head != null) {
      queue.removeElementAt(0);
    }
    return head;
  }

  /** 
   * Wait until the queue is non-empty, then return the element at the
   * head of the queue, leaving it on the queue.
   * @return the item at the head of the queue, or null if a
   * timeout occurred before an item was available.
   * @throws InterruptedException if interrupted while waiting
   */
  public synchronized Object peekWait(Deadline timer)
      throws InterruptedException {
    final Thread thread = Thread.currentThread();
    Deadline.Callback cb = new Deadline.Callback() {
	public void changed(Deadline deadline) {
	  thread.interrupt();
	}};
    while (queue.isEmpty() && !timer.expired()) {
      try {
	timer.registerCallback(cb);
	this.wait(timer.getRemainingTime());
      } finally {
	timer.unregisterCallback(cb);
      }
    }
    if (!queue.isEmpty()) {
      // remove from beginning
      Object obj = queue.firstElement();
      return obj;
    } else {
      return null;
    }
  }

  /** 
   * Return first element on queue, without removing it.
   * @return The element at the head of the queue, or null if queue is empty
   */
  public synchronized Object peek() {
    return (queue.isEmpty() ? null : queue.firstElement());
  }

  /** 
   * Remove the specified element from the queue.  If the element appears
   * in the queue more than once, the behavior is undefined.
   * @return true iff the element was present in the queue
   */
  public boolean remove(Object obj) {
    return queue.remove(obj);
  }

  /** 
   * Return true iff the queue is empty
   */
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  /** 
   * Resort the elements on the queue.  Must be called if any element
   * changes in a way that might affect its sort order
   */
  public synchronized void sort() {
    Collections.sort(queue, comparator);
  }
}
