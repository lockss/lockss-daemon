/*
 * $Id: RateLimiter.java,v 1.1 2003-01-21 18:34:26 tal Exp $
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
 * RateLimiter is used to limit the rate at which some class of events occur.
 */
public class RateLimiter {
  int limit;				// limit on events / interval
  long interval;
  long time[];				// history of (limit) event times
  int count = 0;

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
  public void event() {
    time[count] = TimeBase.nowMs();
    count = (count + 1) % limit;
  }

  /** Return true if an event could occur now without exceeding the limit */
  public boolean isEventOk() {
    return (TimeBase.nowMs() - time[count]) >= interval;
  }
}
