/*
 * $Id: PollUtil.java,v 1.1.2.1 2007-10-17 22:28:38 smorabito Exp $
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

  /**
   * @deprecated Only used by V1.  Remove when removing V1.
   */
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

  /**
   * Find a schedulable duration between the min and max time.
   * 
   * @param estDuration  The estimated hash duration.
   * @param min  The minimum duration to try.
   * @param max  The maximum duration to try.
   * @param pm The PollManager
   * @return -1 if no time could be found, otherwise the schedulable duration
   *         that was found.
   */
  public static long findSchedulableDuration(long estDuration,
                                             long min, long max,
                                             PollManager pm) {
    // Can't go on.
    if (min > max) {
      log.info("Can't schedule a poll with min poll time [" +
               min + "] greater than max poll time [" +
               max + "]");
      return -1;
    }

    // Determine the increment to use while looping.  We would like to
    // make it through the loop at least six times.
    int incr = (int)((max - min) / 6);
    
    for (long dur = min; dur <= max; dur += incr) {
      if (dur > max) {
        dur = max;
      }
      if (log.isDebug2()) {
        log.debug2("Asking whether hash of "
                   + StringUtil.timeIntervalToString(estDuration) 
                   + " can be scheduled within duration "
                   + StringUtil.timeIntervalToString(dur));
      }
      if (PollUtil.canPollBeScheduled(dur, estDuration, pm)) {
        if (log.isDebug2()) {
          log.debug2("Yes, found schedulable duration for hash: " +
                     StringUtil.timeIntervalToString(dur));
        }
        return dur;
      }
    }
    log.info("Found no time for hash within " +
             StringUtil.timeIntervalToString(max));
    return -1;
  }
  
  public static boolean canPollBeScheduled(long scheduleWindow,
                                           long hashTime,
                                           PollManager pm) {
    log.debug2("Inquiring whether it is possible to schedule a " +
              scheduleWindow + "ms poll within a window of " + hashTime + "ms");
    // Should really never happen.
    if (hashTime > scheduleWindow) {
      log.critical("Inconceivable!  Hash time [" +
                   StringUtil.timeIntervalToString(hashTime) +
                   "] is greater than schedule window of [" +
                   StringUtil.timeIntervalToString(scheduleWindow) + "]!");
      return false;
    }
    Deadline when = Deadline.in(scheduleWindow);
    return pm.getHashService().canHashBeScheduledBefore(hashTime, when);
  }
  
  public static long calcDuration(PollSpec ps, PollManager pm) {
    long hashEst = ps.getCachedUrlSet().estimatedHashDuration();
    long estVoteDuration = estimateVoteDuration(hashEst);
    long estTallyDuration = estimateTallyDuration(hashEst);
    long minPollDuration = 
      Math.max(estVoteDuration + estTallyDuration,
               CurrentConfig.getLongParam(V3Poller.PARAM_MIN_POLL_DURATION,
                                          V3Poller.DEFAULT_MIN_POLL_DURATION));
    long maxPollDuration =
      CurrentConfig.getLongParam(V3Poller.PARAM_MAX_POLL_DURATION,
                                 V3Poller.DEFAULT_MAX_POLL_DURATION);

    if (log.isDebug2()) {
      log.debug2("[calcDuration] Hash Estimate: " +
                 StringUtil.timeIntervalToString(hashEst));
      log.debug2("[calcDuration] Minimum Poll Duration: " +
                 StringUtil.timeIntervalToString(minPollDuration));
      log.debug2("[calcDuration] Maximum Poll Duration: " +
                 StringUtil.timeIntervalToString(maxPollDuration));
    }
    
    long scheduleTime = PollUtil.findSchedulableDuration(hashEst,
                                                         minPollDuration,
                                                         maxPollDuration, pm);
    if (log.isDebug2()) {
      log.debug("[calcDuration] findSchedulableDuration returns "
                + StringUtil.timeIntervalToString(scheduleTime));
    }

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
