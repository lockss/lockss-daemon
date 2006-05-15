/*
 * $Id: RateLimiter.java,v 1.9 2006-05-15 00:12:49 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.config.Configuration;

/**
 * RateLimiter is used to limit the rate at which some class of events
 * occur.  Individual operations on this class are synchronized, so safe to
 * use from multiple threads.  However, in order to ensure the rate isn't
 * exceeded, multithreaded use requires the pair of calls {@link
 * #isEventOk()} and {@link #event()}, or the pair  {@link
 * #waitUntilEventOk()} and {@link #event()} to be synchronized as a unit:<pre>
    synchronized (rateLimiter) {
      rateLimiter.isEventOk();
      rateLimiter.event();
    }</pre>
 */
public class RateLimiter {
  static Logger log = Logger.getLogger("RateLimiter");

  /** An instance of a RateLimiter that allows events at an unlimited
   * rate. */
  public static RateLimiter UNLIMITED = new RateLimiter.Unlimited();

  private int limit;			// limit on events / interval
  private long interval;
  private long time[];			// history of (limit) event times
  private int count = 0;

  /** Create a RateLimiter according to the specified configuration parameters.
   * @param config the Configuration object
   * @param currentLimiter optional existing RateLimiter, return if it
   * matches the current config values
   * @param maxEventsParam name of the parameter specifying the maximum
   * number of events per interval
   * @param intervalParam name of the parameter specifying the length of
   * the interval
   * @param maxEvantDefault default maximum number of events per interval,
   * if config param has no value
   * @param intervalDefault default interval, if config param has no value
   * @return a new RateLimiter, or the exiting one if it's the same
   */
  public static RateLimiter
    getConfiguredRateLimiter(Configuration config, RateLimiter currentLimiter,
			     String maxEventsParam, int maxEvantDefault,
			     String intervalParam, long intervalDefault) {
    int pkts = config.getInt(maxEventsParam, maxEvantDefault);
    long interval = config.getTimeInterval(intervalParam, intervalDefault);
    if (currentLimiter == null || currentLimiter.getInterval() != interval ||
	currentLimiter.getLimit() != pkts) {
      return new RateLimiter(pkts, interval);
    } else {
      return currentLimiter;
    }
  }

  /** Create a RateLimiter according to the specified configuration
   * parameter, whose value should be a string:
   * <i>events</i>/<i>time-interval</i>.
   * @param config the Configuration object
   * @param currentLimiter optional existing RateLimiter, return if it
   * matches the current config value
   * @param param name of the rate string config parameter
   * @param dfault default rate string
   * @return a new RateLimiter, or the exiting one if it's the same
   * @throws RuntimeException iff the default string is unparseable and the
   * parameter value is either empty or unparseable.
   */
  public static RateLimiter
    getConfiguredRateLimiter(Configuration config, RateLimiter currentLimiter,
			     String param, String dfault) {
    String rate = config.get(param, dfault);
    if ("unlimited".equalsIgnoreCase(rate)) {
      return UNLIMITED;
    }
    Ept ept;
    try {
      ept = new Ept(rate);
    } catch (RuntimeException e) {
      log.warning("Configured rate (" + param + "=" + rate +
		  ") illegal, using default (" + dfault + ")");
      ept = new Ept(dfault);
    }
    if (currentLimiter == null ||
	currentLimiter.getInterval() != ept.interval ||
	currentLimiter.getLimit() != ept.events) {
      return new RateLimiter(ept.events, ept.interval);
    } else {
      return currentLimiter;
    }
  }

  // helper to parse rate string  <events> / <time-interval>
  private static class Ept {
    private int events;
    private long interval;
    Ept(String rate) {
      List pair = StringUtil.breakAt(rate, '/', 3, false, true);
      if (pair.size() != 2) {
	throw new IllegalArgumentException("Rate not n/interval: " + rate);
      }
      events = Integer.parseInt((String)pair.get(0));
      interval = StringUtil.parseTimeInterval((String)pair.get(1));
    }
  }

  /** Create a RateLimiter that limits events to <code>limit</code> per
   * <code>interval</code> milliseconds.
   * @param limit max number of events per interval
   * @param interval length of interval in milliseconds
   */
  public RateLimiter(int limit, long interval) {
    if (limit < 1) {
      throw new IllegalArgumentException("limit: " + limit);
    }
    if (interval < 1) {
      throw new IllegalArgumentException("interval: " + interval);
    }
    this.limit = limit;
    this.interval = interval;
    time = new long[limit];
    Arrays.fill(time, 0);
  }

  /** Return the limit on the number of events */
  public int getLimit() {
    return limit;
  }

  /** Return the interval over which events are limited */
  public long getInterval() {
    return interval;
  }

  /** Record an occurrence of the event */
  public synchronized void event() {
    time[count] = TimeBase.nowMs();
    count = (count + 1) % limit;
  }

  /** Return true if an event could occur now without exceeding the limit */
  public synchronized boolean isEventOk() {
    return time[count] == 0 || TimeBase.msSince(time[count]) >= interval;
  }

  /** Return the amount of time until the next event is allowed */
  public synchronized long timeUntilEventOk() {
    long res = TimeBase.msUntil(time[count] + interval);
    return (res > 0) ? res : 0;
  }

  /** Wait until the next event is allowed */
  public synchronized boolean waitUntilEventOk() throws InterruptedException {
    long time = timeUntilEventOk();
    if (time <= 0) {
      return true;
    }
    Deadline.in(time).sleep();
    return true;
  }

  /** A RateLimiter that imposes no limit */
  static class Unlimited extends RateLimiter {
    public Unlimited() {
      super(1, 1);
    }
    public void event() {
    }
    public boolean isEventOk() {
      return true;
    }
    public long timeUntilEventOk() {
      return 0;
    }
    public int getLimit() {
      return 0;
    }

    public long getInterval() {
      return 0;
    }
  }

}
