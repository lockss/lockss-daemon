/*
 * $Id: PollUtil.java,v 1.1 2007-10-09 00:49:55 smorabito Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.config.CurrentConfig;
import org.lockss.daemon.*;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.poller.v3.V3Poller;
import org.lockss.util.*;

public class PollUtil {

  public static Logger log = Logger.getLogger("PollUtil");

  public static long getAdjustedEstimate(PollSpec ps, PollManager pm) {
    CachedUrlSet cus = ps.getCachedUrlSet();
    long hashEst = cus.estimatedHashDuration();
    long my_estimate = hashEst;
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
      my_estimate = hashEst * my_rate / slow_rate;
      log.debug3("I've corrected the hash estimate to " + my_estimate);
    }
    return my_estimate;
  }

  public static long findSchedulableDuration(long hashTime,
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
      if (PollUtil.canPollBeScheduled(dur, hashTime, pm)) {
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

  public static boolean canPollBeScheduled(long pollTime, long hashTime,
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

  public static boolean canHashBeScheduledBefore(long duration, Deadline when,
                                   PollManager pm) {
    boolean ret = pm.getHashService().canHashBeScheduledBefore(duration, when);
    log.debug("canHashBeScheduledBefore(" + duration + "," + when + ")" +
              " returns " + ret);
    return ret;
  }
  
  public static long calcDuration(PollSpec ps, PollManager pm) {
    long hashEst = ps.getCachedUrlSet().estimatedHashDuration();
    long estVoteDuration = estimateVoteDuration(hashEst);
    long estTallyDuration = estimateTallyDuration(hashEst); 
    long maxPollDuration =
      CurrentConfig.getLongParam(V3Poller.PARAM_MAX_POLL_DURATION,
                                 V3Poller.DEFAULT_MAX_POLL_DURATION);
    
    long minPoll = estVoteDuration + estTallyDuration;
    long maxPoll = Math.min(minPoll, maxPollDuration);

    log.debug("[calcDuration] Hash Estimate: " +
              StringUtil.timeIntervalToString(hashEst));
    log.debug("[calcDuration] Minimum Poll Duration: " +
              StringUtil.timeIntervalToString(minPoll));
    log.debug("[calcDuration] Maximum Poll Duration: " +
              StringUtil.timeIntervalToString(maxPoll));
    
    long scheduleTime = PollUtil.findSchedulableDuration(hashEst, minPoll,
                                                         maxPoll, hashEst, pm);
    log.debug("[calcDuration] findSchedulableDuration returns "
              + StringUtil.timeIntervalToString(scheduleTime));

    return scheduleTime;
  }
  
  public static long estimateVoteDuration(long hashEst) {
    // This multiplier is applied to this box's adjusted hash estimate
    // when calculating the length of time to allow participants to vote.
    // It should probably be fairly small, somewhere around 3 or 4.
    long voteDurationMultiplier =
      CurrentConfig.getLongParam(V3Poller.PARAM_VOTE_DURATION_MULTIPLIER,
                                 V3Poller.DEFAULT_VOTE_DURATION_MULTIPLIER);

    // A small amount of padding to add to the end of the calculated vote
    // duration, to come up with the total vote duration.
    long voteDurationPadding =
      CurrentConfig.getLongParam(V3Poller.PARAM_VOTE_DURATION_PADDING,
                                 V3Poller.DEFAULT_VOTE_DURATION_PADDING);

    long estVoteDuration  = hashEst * voteDurationMultiplier +
                            voteDurationPadding;
    log.debug("[estimatedVoteDuration] Estimated Vote Duration: " + 
              StringUtil.timeIntervalToString(estVoteDuration));
    return estVoteDuration;
  }
  
  public static long estimateTallyDuration(long hashEst) {
    // This multiplier is applied to this box's adjusted hash estimate
    // when calculating the length of time it will likely take to tally
    // the results from all the peers who have voted.
    int tallyDurationMultiplier =
      CurrentConfig.getIntParam(V3Poller.PARAM_TALLY_DURATION_MULTIPLIER,
                                V3Poller.DEFAULT_TALLY_DURATION_MULTIPLIER);
    long tallyDurationPadding =
      CurrentConfig.getLongParam(V3Poller.PARAM_TALLY_DURATION_PADDING,
                                 V3Poller.DEFAULT_TALLY_DURATION_PADDING);
    
    long estTallyDuration = hashEst * tallyDurationMultiplier + 
                            tallyDurationPadding;
    log.debug("[estimatedTallyDuration] Estimated Tally Duration: " +
              StringUtil.timeIntervalToString(estTallyDuration));
    return estTallyDuration;
  }
}
