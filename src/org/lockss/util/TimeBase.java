/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * TimeBase allows use of a simulated time base for testing.

 * Instead of calling <code>System.currentTimeMillis()</code> or <code>new
 * Date()</code>, other parts of the system should call {@link #nowMs()} or
 * {@link #nowDate()}.  When in real mode (the default), these methods
 * return the same value as the normal methods.  In simulated mode, they
 * return the contents of an internal counter, which can be incremented
 * programmatically.  This allows time-dependent functions to be tested
 * quickly and predictably.
 */
public class TimeBase {
  /** A long time from now. */
  public static final long MAX = Long.MAX_VALUE;

  private static volatile boolean isSimulated = false;
  private static volatile long simulatedTime;

  /** No instances */
  private TimeBase() {
  }

  /** Set TimeBase into real mode.
   */
  public static void setReal() {
    isSimulated = false;
  }

  /** Set TimeBase into simulated mode.
   * @param time  Simulated time to set as current
   */
  public static void setSimulated(long time) {
    isSimulated = true;
    simulatedTime = time;
  }

  /** Set TimeBase into simulated mode.
   * @param time Date/time string to set as current time, in format
   * <code>yyyy/MM/dd HH:mm:ss</code>
   */
  public static void setSimulated(String dateTime) throws ParseException {
    DateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    fmt.setTimeZone(Constants.DEFAULT_TIMEZONE);
    fmt.setLenient(true);
    simulatedTime = fmt.parse(dateTime).getTime();
    isSimulated = true;
  }

  /** Set TimeBase into simulated mode, at time 0 */
  public static void setSimulated() {
    setSimulated(0);
  }

  /** Return true iff simulated time base is in effect */
  public static boolean isSimulated() {
    return isSimulated;
  }

  /** Step simulated time base by n ticks */
  public static void step(long n) {
    if (!isSimulated) {
      throw new IllegalStateException("Can't step TimeBase when in real mode");
    }
    simulatedTime += n;
    // ensure that all timer queue events whose time has come get executed
    // before this returns
    TimerQueue.runAllExpired();
  }

  /** Step simulated time base by 1 tick */
  public static void step() {
    step(1);
  }

  /** Return the current time, in milliseconds.  In real mode, this returns
   * System.currentTimeMillis(); in simulated mode it returns the simulated
   * time.
   */
  public static long nowMs() {
    if (isSimulated) {
      return simulatedTime;
    } else {
      return System.currentTimeMillis();
    }
  }

  /** Return the current time, as a Date.  In real mode, this returns
   * new Date(); in simulated mode it returns the simulated time as a Date.
   */
  public static Date nowDate() {
    if (isSimulated) {
      return new Date(simulatedTime);
    } else {
      return new Date();
    }
  }

  /** Return the number of milliseconds since the argument
   * @param when a time
   */
  public static long msSince(long when) {
    return nowMs() - when;
  }

  /** Return the number of milliseconds until the argument
   * @param when a time
   */
  public static long msUntil(long when) {
    return when - nowMs();
  }

  /** Return a Calenday set to the current real or simulated time */
  public static Calendar nowCalendar() {
    Calendar res = Calendar.getInstance();
    res.setTimeInMillis(nowMs());
    return res;
  }

}
