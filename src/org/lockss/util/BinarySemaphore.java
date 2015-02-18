/*
 * $Id$
 *

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

/** <code>BinarySemaphore</code> allows one thread to wait for another
 * thread to complete some operation, with a required timeout.
 * The semaphore is initially empty;
 * <code>give()</code> fills it, <code>take()</code> waits for it to be full,
 * then empties it and returns.
 */
public class BinarySemaphore {
  private boolean state = false;

  /** Wait until the semaphore is full or the timer expires.
   * If the semaphore is already full, return immediately.
   * Use {@link Deadline#expire()} to make this return early.
   * Always leaves the semaphore empty.
   * @param timer time to wait.  If null, returns immediately.
   * @return true if <code>take()</code> was successful (semaphore was or
   * became full), else false (timer expired).
   * @throws InterruptedException if interrupted while waiting
   */
  public boolean take(Deadline timer) throws InterruptedException {
    if (timer != null) {
      // Don't call any methods on the timer while holding the semaphore's
      // lock, or we create a potential deadlock.

      Deadline.InterruptCallback cb = new Deadline.InterruptCallback();
      try {
	timer.registerCallback(cb);
	while (!timer.expired()) {
	  long sleep = timer.getSleepTime();
	  synchronized (this) {
	    if (!state) {
	      this.wait(sleep);
	    }
	    if (state) {
	      state = false;
	      return true;
	    }
	  }
	}
      } finally {
	cb.disable();
	timer.unregisterCallback(cb);
      }
    }
    synchronized (this) {
      if (state) {
	state = false;
	return true;
      } else {
	return false;
      }
    }
  }

  /** Fill the semaphore.  If another thread is waiting for the
   * semaphore to be full, it will proceed.  If multiple threads are waiting,
   * one of them will proceed.
   */
  synchronized public void give() {
    state = true;
    this.notifyAll();
  }

  public String toString() {
    return "[sem: " + (state ? "full]" : "empty]");
  }
}
