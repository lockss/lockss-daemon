/*
 * $Id: Deadline.java,v 1.11 2002-11-25 21:31:52 tal Exp $
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
import java.text.*;

/** Deadline represents a time (at which some operation must complete).
 */
public class Deadline implements Comparable {
  /** A long time from now (but not really never). */
  public static final Deadline NEVER = new Deadline(Long.MAX_VALUE);

  protected static Logger log = Logger.getLogger("Deadline");

  private static LockssRandom random = null;
  protected Date expiration;
  protected long duration;		// only for testing
  private List subscribers;		// those who wish to be notified
					// if/when this Deadline's duration
					// changes
  
  /** Create a Deadline that expires at the specified Date, with the
   * specified duration.  Done this way so factory methods don't risk a
   * timer tick between getting the current time, and the constructor
   * computing the duration, which would then be different from what was
   * specified.
   */
  private Deadline(Date at, long duration) {
    expiration = at;
    this.duration = duration;
  }
  
  /** Create a Deadline that expires at the specified Date. */
  private Deadline(Date at) {
    duration = at.getTime() - nowMs();
    expiration = at;
  }
  
  /** Create a Deadline that expires at the specified date. */
  private Deadline(long at) {
    this(new Date(at));
  }
  
  /** Create a Deadline that expires in <code>duration</code> milliseconds. */
  public static Deadline in(long duration) {
    return new Deadline(new Date(nowMs() + duration), duration);
  }

  /** Create a Deadline representing the specified Date.
   * @param at
   */
  public static Deadline at(Date at) {
    return new Deadline(at);
  }
  
  /** Create a Deadline representing the specified date/time.
   * @param at date/time in milliseconds from the epoch.
   */
  public static Deadline at(long at) {
    return new Deadline(at);
  }
  
  /** Create a Deadline representing a random time between
   * <code>earliest</code> (inclusive) and <code>latest</code> (exclusive).
   * The random time is uniformly distributed between the endpoints.
   * @param earliest The earliest possible time
   * @param latest The latest possible time
   */
  public static Deadline atRandomRange(long earliest, long latest) {
    return new Deadline(earliest + getRandom().nextLong(latest - earliest));
  }

  /** Create a Deadline representing a random time between now (inclusive)
   * and <code>before</code> (exclusive).  The random time is uniformly
   * distributed.
   * @param before The time before which the deadline should expire
   */
  public static Deadline atRandomBefore(long before) {
    return atRandomRange(nowMs(), before);
  }

  /** Create a Deadline representing a random time between
   * <code>earliest</code> (inclusive) and <code>latest</code> (exclusive).
   * The random time is uniformly distributed between the endpoints.
   * @param earliest The earliest possible time
   * @param latest The latest possible time
   */
  public static Deadline atRandomRange(Deadline earliest, Deadline latest) {
    return atRandomRange(earliest.getExpirationTime(),
			 latest.getExpirationTime());
  }

  /** Create a Deadline representing a random time between now (inclusive)
   * and <code>before</code> (exclusive).  The random time is uniformly
   * distributed.
   * @param before The time before which the deadline should expire
   */
  public static Deadline atRandomBefore(Deadline before) {
    return atRandomRange(nowMs(), before.getExpirationTime());
  }

  /** Create a Deadline representing a random time between
   * <code>minDuration</code> (inclusive) and <code>maxDuration</code>
   * (exclusive) milliseconds from now.  The random time is uniformly
   * distributed between the endpoints.
   * @param minDuration The minimum duration, in milliseconds.
   * @param maxDuration The maximum duration, in milliseconds.
   */
  public static Deadline inRandomRange(long minDuration, long maxDuration) {
    return atRandomRange(nowMs() + minDuration, nowMs() + maxDuration);
  }

  /** Create a Deadline representing a random time between now (inclusive)
   * and <code>maxDuration</code> (exclusive) milliseconds from now.  The
   * random time is uniformly distributed.
   * @param maxDuration The maximum duration, in milliseconds.
   */
  public static Deadline inRandomBefore(long maxDuration) {
    return inRandomRange(0, maxDuration);
  }

//   /** Return a timer whose duration is a random, normally distrubuted value
//    * whose mean is <code>meanDuration</code> and standard deviation
//    * <code>stddev</code>.  */
//   public Deadline withinOf(double stddev, long meanDuration) {
//     super(meanDuration + (long)(stddev * getRandom().nextGaussian()));
//   }

  /** Return the absolute expiration time, in milliseconds */
  public long getExpirationTime() {
    return expiration.getTime();
  }

  /** Return the expiration time as a Date */
  public Date getExpiration() {
    return expiration;
  }

  /** Return the time remaining until expiration, in milliseconds.
   * This method should not be used to obtain a duration to sleep; use
   * {@link #getSleepTime()} for that.
   */
  public synchronized long getRemainingTime() {
    return (expired() ? 0 : expiration.getTime() - nowMs());
  }

  /** Return the time to sleep, in milliseconds.
   * This method should be used instead of {@link #getRemainingTime()} to
   * determine how long to sleep.  When operating with the real time base
   * it returns the same value as getRemainingTime().  When using a fake
   * time base for debugging, it returns a small number so that any sleeps
   * will complete quickly, as it is impossible to predict when the fake
   * time base will be advanced to the deadline.
  */
  public synchronized long getSleepTime() {
    if (TimeBase.isSimulated()) {
      return expired() ? 0 : 5;
    } else {
      return getRemainingTime();
    }
  }

  /** Return true iff the timer has expired */
  public synchronized boolean expired() {
    return (!now().before(expiration));
  }

  /** Return true iff this deadline expires before <code>other</code>. */
  public boolean before(Deadline other) {
    return expiration.before(other.expiration);
  }

  /** Cause the deadline to expire immediately */
  public synchronized void expire() {
    expiration.setTime(0);
    changed();
  }

  /** Add <code>delta</code> milliseconds to the deadline. */
  public synchronized void later(long delta) {
    expiration.setTime(expiration.getTime() + delta);
    changed();
  }

  /** Subtract <code>delta</code> from the deadline. */
  public synchronized void sooner(long delta) {
    expiration.setTime(expiration.getTime() - delta);
    changed();
  }

  /** Register a callback that will be called if/when the Deadline's
   * duration changes (by a call to expire(), sooner(), etc.)
   */
  public synchronized void registerCallback(Callback callback) {
    if (subscribers == null) {
      subscribers = new LinkedList();
    }
    subscribers.add(callback);
  }

  /** Unregister a change callback */
  public synchronized void unregisterCallback(Callback callback) {
    subscribers.remove(callback);
  }

  /** Call dedlineChanged() method of all sunbscribers   */
  protected synchronized void changed() {
    if (subscribers != null) {
      for (Iterator iter = subscribers.iterator(); iter.hasNext(); ) {
	// tk - run these in a separate thread
	try {
	  Callback cb = (Callback)iter.next();
	  cb.changed(this);
	} catch (Exception e) {
	  log.error("Callback threw", e);
	}
      }
    }
  }

  protected static Date now() {
    return TimeBase.nowDate();
//     return new Date();
  }

  protected static long nowMs() {
    return TimeBase.nowMs();
//     return System.currentTimeMillis();
  }

  private static LockssRandom getRandom() {
    if (random == null) {
      random = new LockssRandom();
    }
    return random;
  }

  // Comparable interface

  public int compareTo(Object o) {
    return expiration.compareTo(((Deadline)o).expiration);
  }

  public boolean equals(Object o) {
    return expiration.equals(((Deadline)o).expiration);
  }

  // tk - should include "+n days" or some such
  private static final DateFormat df1 = new SimpleDateFormat("HH:mm:ss");
  private static final DateFormat df = DateFormat.getTimeInstance();

  public String toString() {
    return "[deadline: in " + duration + ", at " +
      (TimeBase.isSimulated()
       ? ("sim " + expiration.getTime())
       : df1.format(expiration))
      + "]";
  }

  /**
   * The Deadline.Callback interface defines the
   * method that will be called if/when a deadline changes.
   */
  public interface Callback {
    /**
     * Called when the deadline's duration is changed.
     * @param deadline  the Deadline that changed.
     */
    public void changed(Deadline deadline);
  }

  /** Sleep, returning when the deadline is reached, or possibly earlier.
   * In order to guarantee that the deadline has actually been reached, this
   * must be called in a <code>while (!deadline.expired()) { ... }</code>
   * loop.
   * @throws InterruptedException if either the timer duration is changed
   * (<i>eg</i>, by {@link #expire()} or {@link #sooner(long)}) or the
   * thread is otherwise interrupted.
   */
  public void sleep() throws InterruptedException {
    final Thread thread = Thread.currentThread();
    Callback cb = new Callback() {
	public void changed(Deadline deadline) {
	  thread.interrupt();
	}};
    long nap;
    try {
      registerCallback(cb);
      while ((nap = getSleepTime()) > 0) {
	thread.sleep(nap);
      }
    } finally {
      unregisterCallback(cb);
    }
  }

  // for testing
  private long getDuration() {
    return duration;
  }
}
