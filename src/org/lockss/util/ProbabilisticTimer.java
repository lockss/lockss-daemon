// ========================================================================
// $Id: ProbabilisticTimer.java,v 1.4 2002-09-19 20:45:34 tal Exp $
// ========================================================================

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
import java.text.DateFormat;


/** Probabilistic timers measure a duration with randomized jitter.
 */
public class ProbabilisticTimer extends Deadline {
  private static Random random = null;
  private Thread thread;

  /** Return a timer with the specified duration. */
  public ProbabilisticTimer(long duration) {
    super(duration);
  }

  /** Return a timer whose duration is a random, normally distrubuted value
   * whose mean is <code>meanDuration</code> and standard deviation
   * <code>stddev</code>.
   */
  public ProbabilisticTimer(long meanDuration, double stddev) {
    super(meanDuration + (long)(stddev * getRandom().nextGaussian()));
  }

  private static Random getRandom() {
    if (random == null) {
      random = new Random();
    }
    return random;
  }

  /** Cause the timer to expire immediately, and wake up the thread waiting
      on it, if any. */
  public synchronized void expire() {
    super.expire();
//      expiration.setTime(0);
    if (thread != null)
      thread.interrupt();
  }

  /** Return true iff this timer is shorter than <code>other</code>. */
  public boolean isShorterThan(Deadline other) {
    return before(other);
  }

  /** Add <code>delta</code> to the timer's expiration time. */
  public synchronized void slower(long delta) {
    expiration.setTime(expiration.getTime() + delta);
  }

  /** Subtract <code>delta</code> from the timer's expiration time, and
      wake up the waiting thread, if any, so it can recalculate the time
      to wait. */
  public synchronized void faster(long delta) {
    expiration.setTime(expiration.getTime() - delta);
    if (thread != null)
      thread.interrupt();
  }

  /** Set the thread to be interrupted to the current thread. */
  void setThread() {
    thread = Thread.currentThread();
  }

  /** Set the thread to be interrupted to null. */
  void clearThread() {
    thread = null;
  }

  // This needs fixing.
  private void sleep() {
    long nap = getRemainingTime();
    // XXX make these parameters
    long minimumSleep = 1000;
    long maximumSleep = 60000;
    // make sure we always sleep if there's
    if (nap > 0) {
      if (nap < minimumSleep)
	nap = minimumSleep;
      try {
	thread = Thread.currentThread();
	thread.sleep(nap > maximumSleep ? maximumSleep : nap);
      } catch (InterruptedException e) {
	// XXX check that it's harmless
      }
    }
  }

  /** Sleep until the timer expires. */
  public void sleepUntil() {
    long nap = getRemainingTime();
    while (nap > 0) {
      try {
	thread = Thread.currentThread();
	thread.sleep(nap);
      } catch (InterruptedException e) {
	// Just wake up and see if it's time to expire
      }
      nap = getRemainingTime();
    }
  }

  public String toString() {
    DateFormat df = DateFormat.getTimeInstance();
    return "[Timeout " + duration + " at " + df.format(expiration) + "]";
  }
}
