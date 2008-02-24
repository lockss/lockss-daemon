/*
 * $Id: PollUtil.java,v 1.4.2.1 2008-02-24 02:31:42 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.scheduler.*;
import org.lockss.util.*;
import static org.lockss.util.StringUtil.timeIntervalToString;

public class PollUtil {

  public static Logger log = Logger.getLogger("PollUtil");

  public static String makeShortPollKey(String pollKey) {
    if (pollKey == null || pollKey.length() <= 10) {
      return pollKey;
    }
    return pollKey.substring(0, 10);
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
  public static long findV3SchedulableDuration(long hashEst,
					       long tgtVoteDuration,
					       long tgtTallyDuration,
					       PollManager pm) {
    long tgtDuration = tgtVoteDuration + tgtTallyDuration;
    long minPollDuration = Math.max(tgtDuration, getV3MinPollDuration());
    long maxPollDuration = getV3MaxPollDuration();

    // fraction of entire duration that is vote duration
    double voteFract =
      (double)tgtVoteDuration / (double)(tgtVoteDuration + tgtTallyDuration);

    double mult =
      CurrentConfig.getDoubleParam(V3Poller.PARAM_POLL_EXTEND_MULTIPLIER,
				   V3Poller.DEFAULT_POLL_EXTEND_MULTIPLIER);

    // amount to increment total duration each time through loop
    long durationIncr = (long)(tgtVoteDuration * mult);
    
    long duration = minPollDuration;
    while (duration < maxPollDuration) {
      long voteDuration = (long)(duration * voteFract);
      long now = TimeBase.nowMs();
      TimeInterval intrvl =
	new TimeInterval(now + voteDuration, now + duration);
      if (canV3PollBeScheduled(intrvl, hashEst, pm)) {
        if (log.isDebug2()) {
          log.debug2("Found schedulable duration for hash:" +
                     timeIntervalToString(hashEst) + " in " +
		     toString(intrvl));
	}
	return duration;
      } else {
	if (log.isDebug2()) {
          log.debug2("No room in schedule for hash: " +
                     timeIntervalToString(hashEst) + " in " +
		     toString(intrvl));
	}
      }
      duration += durationIncr;
    }
    log.info("Found no time for " +
	     timeIntervalToString(duration) +
	     " poll within " +
	     timeIntervalToString(maxPollDuration));
    return -1;
  }
  
  static String toString(TimeInterval intrvl) {
    return "[" + timeIntervalToString(intrvl.getBeginTime()) +
      ", " + timeIntervalToString(intrvl.getEndTime()) + "]";
  }

  public static boolean canV3PollBeScheduled(TimeInterval scheduleWindow,
                                           long hashTime,
                                           PollManager pm) {
    // Should really never happen.
    long dur = scheduleWindow.getTotalTime();
    if (hashTime > dur) {
      log.error("Inconceivable!  Hash time (" +
		timeIntervalToString(hashTime) +
		") greater than schedule window (" +
		timeIntervalToString(dur) + ")");
      return false;
    }
    StepTask task =
      new StepTask(scheduleWindow, hashTime, null, null) {
	public int step(int n) {
	  // this will never be executed
	  return n;
	}
      };
    return pm.getDaemon().getSchedService().isTaskSchedulable(task);
  }
  
  static long getV3MinPollDuration() {
    return
      CurrentConfig.getTimeIntervalParam(V3Poller.PARAM_MIN_POLL_DURATION,
					 V3Poller.DEFAULT_MIN_POLL_DURATION);
  }

  static long getV3MaxPollDuration() {
    return
      CurrentConfig.getTimeIntervalParam(V3Poller.PARAM_MAX_POLL_DURATION,
					 V3Poller.DEFAULT_MAX_POLL_DURATION);
  }

  static int getVoteDurationMultiplier() {
    return
      CurrentConfig.getIntParam(V3Poller.PARAM_VOTE_DURATION_MULTIPLIER,
				V3Poller.DEFAULT_VOTE_DURATION_MULTIPLIER);
  }

  static long getVoteDurationPadding() {
    return
      CurrentConfig.getTimeIntervalParam(V3Poller.PARAM_VOTE_DURATION_PADDING,
					 V3Poller.DEFAULT_VOTE_DURATION_PADDING);
  }

  static int getTallyDurationMultiplier() {
    return
      CurrentConfig.getIntParam(V3Poller.PARAM_TALLY_DURATION_MULTIPLIER,
				V3Poller.DEFAULT_TALLY_DURATION_MULTIPLIER);
  }

  static long getTallyDurationPadding() {
    return
      CurrentConfig.getTimeIntervalParam(V3Poller.PARAM_TALLY_DURATION_PADDING,
					 V3Poller.DEFAULT_TALLY_DURATION_PADDING);
  }

  static long getReceiptPadding() {
    return CurrentConfig.getLongParam(V3Poller.PARAM_RECEIPT_PADDING,
				      V3Poller.DEFAULT_RECEIPT_PADDING);
  }

  public static long calcV3Duration(PollSpec ps, PollManager pm) {
    return calcV3Duration(ps.getCachedUrlSet().estimatedHashDuration(), pm);
  }

  public static long calcV3Duration(long hashEst, PollManager pm) {
    long tgtVoteDuration = v3TargetVoteDuration(hashEst);
    long tgtTallyDuration = v3TargetTallyDuration(hashEst);

    if (log.isDebug2()) {
      log.debug2("[calcDuration] Hash estimate: " +
                 timeIntervalToString(hashEst));
      log.debug2("[calcDuration] Target vote duration: " +
                 timeIntervalToString(tgtVoteDuration));
      log.debug2("[calcDuration] Target tally duration: " +
                 timeIntervalToString(tgtTallyDuration));
    }
    
    long scheduleTime = findV3SchedulableDuration(hashEst,
						  tgtVoteDuration,
						  tgtTallyDuration,
						  pm);
    if (log.isDebug2()) {
      log.debug("[calcDuration] findV3SchedulableDuration returns "
                + timeIntervalToString(scheduleTime));
    }
    if (scheduleTime < 0) {
      return scheduleTime;
    }
    return scheduleTime + getReceiptPadding();
  }
  
  public static long v3TargetVoteDuration(long hashEst) {
    long estVoteDuration  = hashEst * getVoteDurationMultiplier() +
                            getVoteDurationPadding();
    log.debug2("[estimatedVoteDuration] Estimated Vote Duration: " + 
              timeIntervalToString(estVoteDuration));
    return estVoteDuration;
  }
  
  public static long v3TargetTallyDuration(long hashEst) {
    long estTallyDuration = hashEst * getTallyDurationMultiplier() + 
                            getTallyDurationPadding();
    log.debug2("[estimatedTallyDuration] Estimated Tally Duration: " +
              timeIntervalToString(estTallyDuration));
    return estTallyDuration;
  }

  /** This is bassackwards and needs refactoring.  The methods above
   * compute the total poll duration by summing all the phases, but do not
   * record those boundaries because they're called by the poll factory,
   * before the poll exists.  This method takes the total duration and
   * solved for the vote deadline and tally deadline based on the
   * parameters that went into the original claculation */
  public static TimeInterval calcV3TallyWindow(long hashEst,
					       long totalDuration) {
    long sum = totalDuration - getReceiptPadding();
    double ratio =
      (double)(hashEst * getVoteDurationMultiplier()
	       + getVoteDurationPadding())
      / (double)(hashEst * getTallyDurationMultiplier()
		 + getTallyDurationPadding());
    long voteDuration = (long)((ratio * sum) / (ratio + 1));
    long tallyDuration = sum - voteDuration;
    long now = TimeBase.nowMs();
    TimeInterval res = new TimeInterval(now + voteDuration,
					now + voteDuration + tallyDuration);
    log.debug2("[calcV3TallyWindow] " + timeIntervalToString(hashEst) +
	       " hash in " + timeIntervalToString(totalDuration) +
	       ", ratio = " + ratio + ", tally window: " + toString(res));
    return res;
  }

  // V1 only

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
                   + timeIntervalToString(estDuration) 
                   + " can be scheduled within duration "
                   + timeIntervalToString(dur));
      }
      if (canPollBeScheduled(dur, estDuration, pm)) {
        if (log.isDebug2()) {
          log.debug2("Yes, found schedulable duration for hash: " +
                     timeIntervalToString(dur));
        }
        return dur;
      }
    }
    log.info("Found no time for hash within " +
             timeIntervalToString(max));
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
                   timeIntervalToString(hashTime) +
                   "] is greater than schedule window of [" +
                   timeIntervalToString(scheduleWindow) + "]!");
      return false;
    }
    Deadline when = Deadline.in(scheduleWindow);
    return pm.getHashService().canHashBeScheduledBefore(hashTime, when);
  }

}
