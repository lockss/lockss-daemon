/*
 * $Id: Deadline.java,v 1.22 2003-05-08 22:13:57 tal Exp $
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
  /** A long time from now. */
  public static final Deadline MAX =
    new Deadline(new ConstantDate(TimeBase.MAX));

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
   * @param at the Date
   * @param duration the duration
   * @param checkReasonable if true, log a warning if the Deadline is
   * either in the past or unreasonably far in the future.
   */
  private Deadline(Date at, long duration, boolean checkReasonable) {
    expiration = at;
    this.duration = duration;
    if (checkReasonable) {
      checkReasonable();
    }
  }

  /** Create a Deadline that expires at the specified Date, with the
   * specified duration.  Done this way so factory methods don't risk a
   * timer tick between getting the current time, and the constructor
   * computing the duration, which would then be different from what was
   * specified.
   * @param at the Date
   * @param duration the duration
   */
  private Deadline(Date at, long duration) {
    this(at, duration, true);
  }

  /** Create a Deadline that expires at the specified Date.
   * @param at the Date
   */
  private Deadline(Date at, boolean checkReasonable) {
    this(at, at.getTime() - nowMs(), checkReasonable);
  }

  /** Create a Deadline that expires at the specified Date.
   * @param at the Date
   */
  private Deadline(Date at) {
    this(at, false);
  }

  /** Create a Deadline that expires at the specified date.
   * @param at the time in ms
   */
  private Deadline(long at, boolean checkReasonable) {
    this(new Date(at), checkReasonable);
  }

  /** Create a Deadline that expires at the specified date.
   * @param at the time in ms
   */
  private Deadline(long at) {
    this(at, true);
  }

  /** Create a Deadline that expires in <code>duration</code> milliseconds.
   * @param duration in ms
   * @return the Deadline
   */
  public static Deadline in(long duration) {
    return new Deadline(new Date(nowMs() + duration), duration);
  }

  /** Create a Deadline representing the specified Date.
   * @param at the Date
   * @return the Deadline
   */
  public static Deadline at(Date at) {
    return new Deadline(at);
  }

  /** Create a Deadline representing the specified date/time.
   * @param at date/time in milliseconds from the epoch.
   * @return the Deadline
   */
  public static Deadline at(long at) {
    return new Deadline(at);
  }

  /** Create a Deadline representing the specified date/time.  This is
   * similar to {@link #at(long)} but suppresses the sanity check.  It is
   * intended to be used when loading or restoring a saved deadline.
   * @param at date/time in milliseconds from the epoch.
   * @return the Deadline
   */
  public static Deadline restoreDeadlineAt(long at) {
    return new Deadline(at, false);
  }

  /** Create a Deadline representing a random time between
   * <code>earliest</code> (inclusive) and <code>latest</code> (exclusive).
   * The random time is uniformly distributed between the endpoints.
   * @param earliest The earliest possible time
   * @param latest The latest possible time
   * @return the Deadline
   */
  public static Deadline atRandomRange(long earliest, long latest) {
    return new Deadline(earliest + getRandom().nextLong(latest - earliest));
  }

  /** Create a Deadline representing a random time between now (inclusive)
   * and <code>before</code> (exclusive).  The random time is uniformly
   * distributed.
   * @param before The time before which the deadline should expire
   * @return the Deadline
   */
  public static Deadline atRandomBefore(long before) {
    return atRandomRange(nowMs(), before);
  }

  /** Create a Deadline representing a random time between
   * <code>earliest</code> (inclusive) and <code>latest</code> (exclusive).
   * The random time is uniformly distributed between the endpoints.
   * @param earliest The earliest possible time
   * @param latest The latest possible time
   * @return the Deadline
   */
  public static Deadline atRandomRange(Deadline earliest, Deadline latest) {
    return atRandomRange(earliest.getExpirationTime(),
			 latest.getExpirationTime());
  }

  /** Create a Deadline representing a random time between now (inclusive)
   * and <code>before</code> (exclusive).  The random time is uniformly
   * distributed.
   * @param before The time before which the deadline should expire
   * @return the Deadline
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
   * @return the Deadline
   */
  public static Deadline inRandomRange(long minDuration, long maxDuration) {
    return atRandomRange(nowMs() + minDuration, nowMs() + maxDuration);
  }

  /** Create a Deadline representing a random time between now (inclusive)
   * and <code>maxDuration</code> (exclusive) milliseconds from now.  The
   * random time is uniformly distributed.
   * @param maxDuration The maximum duration, in milliseconds.
   * @return the Deadline
   */
  public static Deadline inRandomBefore(long maxDuration) {
    return inRandomRange(0, maxDuration);
  }

  /** Create a Deadline representing a random time deviating from the
   * meanDuration by at most delta.  The random time is uniformly distributed.
   * @param meanDuration The mean duration, in milliseconds.
   * @param delta the max deviation
   * @return the Deadline
   */
  public static Deadline inRandomDeviation(long meanDuration, long delta) {
    return inRandomRange(meanDuration - delta, meanDuration + delta);
  }

//   /** Return a timer whose duration is a random, normally distrubuted value
//    * whose mean is <code>meanDuration</code> and standard deviation
//    * <code>stddev</code>.  */
//   public Deadline withinOf(double stddev, long meanDuration) {
//     super(meanDuration + (long)(stddev * getRandom().nextGaussian()));
//   }

  private void checkReasonable() {
    if (TimeBase.isSimulated()) {
      // don't complain during testing
      return;
    }
    if (duration < 0 ||
	(duration > (4 * Constants.WEEK) &&
	 getExpirationTime() != TimeBase.MAX)) {
      log.warning("Unreasonable deadline: " + expiration,
		  new Throwable());
    }
  }

  /**
   * Return the absolute expiration time, in milliseconds
   * @return the expriation time
   */
  public synchronized long getExpirationTime() {
    return expiration.getTime();
  }

  /**
   * Return the expiration time as a Date
   * @return the Date
   */
  public Date getExpiration() {
    return expiration;
  }

  /** Return the time remaining until expiration, in milliseconds.
   * This method should not be used to obtain a duration to sleep; use
   * {@link #getSleepTime()} for that.
   * @return remaining time
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
   * @return sleep time
   */
  public synchronized long getSleepTime() {
    if (TimeBase.isSimulated()) {
      return expired() ? 0 : 5;
    } else {
      return getRemainingTime();
    }
  }

  /**
   * Return true iff the timer has expired
   * @return true if expired
   */
  public synchronized boolean expired() {
    return (!now().before(expiration));
  }

  /**
   * Return true iff this deadline expires before <code>other</code>.
   * @param other the other Deadline
   * @return true if expires earlier
   */
  public synchronized boolean before(Deadline other) {
    return expiration.before(other.expiration);
  }

  /** Cause the deadline to expire immediately */
  public synchronized void expire() {
    expiration.setTime(0);
    changed();
  }

  /**
   * Change expiration time to time n.
   * @param millis new expire time
   */
  public synchronized void expireAt(long millis) {
    expiration.setTime(millis);
    duration = millis - nowMs();
    changed();
  }

  /**
   * Change expiration time to n milliseconds from now.
   * @param millis new expire interval
   */
  public synchronized void expireIn(long millis) {
    expiration.setTime(nowMs() + millis);
    duration = millis;
    changed();
  }

  /**
   * Add <code>delta</code> milliseconds to the deadline.
   * @param delta new ms to add
   */
  public synchronized void later(long delta) {
    expireAt(expiration.getTime() + delta);
  }

  /**
   * Subtract <code>delta</code> from the deadline.
   * @param delta new ms to remove
   */
  public synchronized void sooner(long delta) {
    expireAt(expiration.getTime() - delta);
  }

  /** Register a callback that will be called if/when the Deadline's
   * duration changes (by a call to expire(), sooner(), etc.)
   * @param callback the Callback
   */
  public synchronized void registerCallback(Callback callback) {
    if (subscribers == null) {
      subscribers = new LinkedList();
    }
    subscribers.add(callback);
  }

  /**
   * Unregister a change callback
   * @param callback the Callback
   */
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
    if (expiration.getTime() == TimeBase.MAX) {
      return "[deadline: never]";
    }
    boolean isSim = TimeBase.isSimulated();
    StringBuffer sb = new StringBuffer();
    sb.append("[deadline: dur ");
    sb.append(isSim ? Long.toString(duration)
	      : StringUtil.timeIntervalToString(duration));
    sb.append(", at ");
    if (isSim) {
      sb.append("sim ");
      sb.append(expiration.getTime());
    } else {
      sb.append(df1.format(expiration));
    }
    sb.append("]");
    return sb.toString();
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
    if (expired()) {
      return;
    }
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
