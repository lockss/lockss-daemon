/*
 * $Id: BasePollFactory.java,v 1.3 2006-11-15 08:24:53 smorabito Exp $
 */

/*

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

package org.lockss.poller;

import java.io.*;
import java.security.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.hasher.HashService;
import org.mortbay.util.B64Code;

/**
 * <p>Class that creates V1 Poll objects</p>
 * @author David Rosenthal
 * @version 1.0
 */

public abstract class BasePollFactory implements PollFactory {

  protected static final Logger log = Logger.getLogger("BasePollFactory");

  protected long getAdjustedEstimate(long estTime, PollManager pm) {
    long my_estimate = estTime;
    long my_rate;
    long slow_rate = pm.getSlowestHashSpeed();
    try {
      my_rate = pm.getBytesPerMsHashEstimate();
    }
    catch (SystemMetrics.NoHashEstimateAvailableException e) {
      // if can't get my rate, use slowest rate to prevent adjustment
      log.warning("No hash estimate available, " +
                     "not adjusting poll for slow machines");
      my_rate = slow_rate;
    }
    log.debug3("My hash speed is " + my_rate
                  + ". Slow speed is " + slow_rate);


    if (my_rate > slow_rate) {
      my_estimate = estTime * my_rate / slow_rate;
      log.debug3("I've corrected the hash estimate to " + my_estimate);
    }
    return my_estimate;
  }

  // try poll durations between min and max at increments of incr.
  // return duration in which all hashing can be scheduled, or -1 if not.

  // tk - multiplicative padding factor to avoid poll durations that nearly
  // completely fill the schedule.

  protected long findSchedulableDuration(long hashTime,
                                         long min, long max, long incr,
                                         PollManager pm) {
    // loop goes maybe one more because we don't want to stop before
    // reaching max
    if (min > max) {
      log.info("Can't schedule a poll with min poll time [" +
               min + "] greater than max poll time [" +
               max + "]");
      return -1;
    }
    for (long dur = min; dur <= (max + incr - 1); dur += incr) {
      if (dur > max) {
        dur = max;
      }
      log.debug3("Trying to schedule poll of duration: " + dur);
      if (canPollBeScheduled(dur, hashTime, pm)) {
        log.debug2("Poll duration: " +
                   StringUtil.timeIntervalToString(dur));
        return dur;
      }
      if (dur >= max) {
        // Complete paranoia here.  This shouldn't be necessary, because if
        // we've reset dur to max, the loop will exit on the next iteration
        // because (max + incr) > (max + incr - 1).  But because we might
        // reduce dur in the loop the possibility of an infinite loop has
        // been raised; this is insurance against that.
        break;
      }
    }
    log.info("Can't schedule poll within " +
             StringUtil.timeIntervalToString(max));
    return -1;
  }

  public boolean canPollBeScheduled(long pollTime, long hashTime,
                                    PollManager pm) {
    log.debug("Try to schedule " + pollTime + " poll " + hashTime + " poll");
    if (hashTime > pollTime) {
      log.warning("Total hash time " +
                  StringUtil.timeIntervalToString(hashTime) +
                  " greater than max poll time " +
                  StringUtil.timeIntervalToString(pollTime));
      return false;
    }
    Deadline when = Deadline.in(pollTime);
    return canHashBeScheduledBefore(hashTime, when, pm);
  }

  boolean canHashBeScheduledBefore(long duration, Deadline when,
                                   PollManager pm) {
    boolean ret = pm.getHashService().canHashBeScheduledBefore(duration, when);
    log.debug("canHashBeScheduledBefore(" + duration + "," + when + ")" +
              " returns " + ret);
    return ret;
  }

}
