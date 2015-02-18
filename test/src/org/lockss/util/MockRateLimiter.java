/*
 * $Id$
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
public class MockRateLimiter extends RateLimiter {

  public List eventList = new ArrayList();
  private String rate;
  private int events;
  private long interval;

  public MockRateLimiter(String rate) {
    super(rate);
  }

  public MockRateLimiter(int events, long interval) {
    super(events, interval);
  }

  /** Return the limit as a rate string n/interval */
  public synchronized String getRate() {
    return rate;
  }

  /** Return the limit on the number of events */
  public int getLimit() {
    return events;
  }

  /** Return the interval over which events are limited */
  public long getInterval() {
    return interval;
  }

  /** Return true if the rate limiter is of specified rate */
  public boolean isRate(String rate) {
    return getRate().equals(rate);
  }

  /** Return true if the rate limiter is of specified rate */
  public boolean isRate(int events, long interval) {
    return this.events == events && this.interval == interval;
  }

  /** Return true iff the rate limiter imposes no limit */
  public boolean isUnlimited() {
    return interval == 0;
  }

  /** Change the rate */
  public void setRate(String newRate) {
    this.rate = newRate;
  }

  /** Change the rate */
  public void setRate(String newRate, String dfault) {
    this.rate = newRate;
  }

  /** Change the rate */
  public synchronized void setRate(int newEvents, long newInterval) {
    this.events = newEvents;
    this.interval = newInterval;
  }

  /** Record an occurrence of the event */
  public synchronized void event() {
    eventList.add("event");
  }

  /** Cancel the occurrence of an event.  This is sometimes necessary if an
   * event is aborted and should be allowed again soon. */
  public synchronized void unevent() {
    eventList.add("unevent");
  }

  /** Return true if an event could occur now without exceeding the limit */
  public synchronized boolean isEventOk() {
    eventList.add("isEventOk");
    return true;
  }

  /** Return the amount of time until the next event is allowed */
  public synchronized long timeUntilEventOk() {
    eventList.add("timeUntilEventOk");
    return 0;
  }

  /** Wait until the next event is allowed */
  public synchronized boolean waitUntilEventOk() throws InterruptedException {
    eventList.add("waitUntilEventOk");
    return true;
  }

  public boolean fifoWaitAndSignalEvent() throws InterruptedException {
    eventList.add("fifoWaitAndSignalEvent");
    return true;
  }

  public String rateString() {
    return rate;
  }

  public String toString() {
    return "[RL: " + getRate() + "]";
  }

}
