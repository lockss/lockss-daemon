// ========================================================================
// $Id: ProbabilisticTimer.java,v 1.1 2002-08-31 06:39:29 tal Exp $
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


/** Probabilistic timers
 */
public class ProbabilisticTimer {
  static Random random = null;
  Date expiration;
  Thread thread;

  public ProbabilisticTimer(long delay) {
    this(delay, 0.0);
  }

  public ProbabilisticTimer(long delay, double range) {
    if (random == null) {
      initialize();
    }
    Date now = new Date();
    expiration = new Date(now.getTime() +
			  delay +
			  (long) (range * random.nextGaussian()));
  }

  private static void initialize() {
    random = new Random();
  }

  public synchronized void expire() {
    expiration.setTime(0);
    if (thread != null)
      thread.interrupt();
  }

  public synchronized boolean expired() {
    Date now = new Date();
    return (now.after(expiration));
  }

  public synchronized void slower(long delta) {
    expiration.setTime(expiration.getTime() + delta);
  }

  public synchronized void faster(long delta) {
    expiration.setTime(expiration.getTime() - delta);
    if (thread != null)
      thread.interrupt();
  }

  public synchronized long duration() {
    Date now = new Date();
    return (expired() ? 0 : expiration.getTime() - now.getTime());
  }

  public boolean shorterThan(ProbabilisticTimer other) {
    return (this.duration() < other.duration());
  }

  private void snooze() {
    long nap = duration();
    // XXX make these parameters
    long minimumSleep = 1000;
    long maximumSleep = 60000;
    // make sure we always sleep if there's
    if (nap > 0) {
      if (nap < minimumSleep)
	nap = minimumSleep;
      try {
	Thread.sleep(nap > maximumSleep ? maximumSleep : nap);
      } catch (InterruptedException e) {
	// XXX check that it's harmless
      }
    }
  }

  public void snoozeUntil() {
    long nap = duration();
    while (nap > 0) {
      try {
	Thread.sleep(nap);
      } catch (InterruptedException e) {
	// Just wake up and see if it's time to expire
      }
      nap = duration();
    }
  }

  public String toString() {
    DateFormat df = DateFormat.getTimeInstance();
    return "[Timeout at " + df.format(expiration) + "]";
  }
}
