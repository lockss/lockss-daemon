/*
 * $Id: AbstractQueue.java,v 1.3 2006-06-01 23:57:09 tlipkis Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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
 * Abstract base class for queue implementations that keep their queue in
 * order of removal
 */
public abstract class AbstractQueue implements Queue {
  protected Vector queue = new Vector();

  /**
   * Create a new queue.
   */
  public AbstractQueue() {
  }

  /**
   * Insert an object in the queue, in the correct relative position. */
  public abstract Object put(Object obj);

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
    Deadline.InterruptCallback cb = new Deadline.InterruptCallback();
    try {
      timer.registerCallback(cb);
      while (queue.isEmpty() && !timer.expired()) {
	this.wait(timer.getSleepTime());
      }
    } finally {
      cb.disable();
      timer.unregisterCallback(cb);
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
  public synchronized boolean remove(Object obj) {
    return queue.remove(obj);
  }

  /**
   * Return the number of elements in the queue
   */
  public int size() {
    return queue.size();
  }

  /**
   * Return true iff the queue is empty
   */
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  /**
   * Return a snapshot of the queue contents
   */
  public synchronized List copyAsList() {
    return new ArrayList(queue);
  }

}
