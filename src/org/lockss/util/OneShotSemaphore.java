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

/** <code>OneShotSemaphore</code> allows one or more threads to wait for an
 * event, with a timeout.
 * The semaphore is initially empty;
 * <code>fill()</code> fills it, <code>waitFull()</code> waits for it to be
 * full, then returns.  Once full, it remains full. */
public class OneShotSemaphore {
  private boolean state = false;

  /** Wait until the semaphore is full or the timer expires.
   * If the semaphore is already full, return immediately.
   * Use {@link Deadline#expire()} to make this return early.
   * Does not change the state of the semaphore.
   * @param timer time to wait.  If null, returns immediately.
   * @return true if <code>take()</code> was successful (semaphore was or
   * became full), else false (timer expired).
   * @throws InterruptedException if interrupted while waiting
   */
  synchronized public boolean waitFull(Deadline timer)
      throws InterruptedException {
    if (timer != null) {
      Deadline.InterruptCallback cb = new Deadline.InterruptCallback();
      try {
	timer.registerCallback(cb);
	while (!state && !timer.expired()) {
	  this.wait(timer.getSleepTime());
	}
      } finally {
	cb.disable();
	timer.unregisterCallback(cb);
      }
    }
    return state;
  }

  /** Fill the semaphore.  All threads waiting on this semaphore will
   * proceed.  */
  synchronized public void fill() {
    state = true;
    this.notifyAll();
  }

  /** Return true iff the sempahore is full */
  public boolean isFull() {
    return state;
  }

  public String toString() {
    return "[1shot: " + (state ? "full]" : "empty]");
  }
}
