/*
 * $Id: V2PollFactory.java,v 1.4 2004-09-27 22:39:10 smorabito Exp $
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
import org.lockss.protocol.ProtocolException;
import org.lockss.util.*;
import org.lockss.hasher.HashService;
import org.lockss.daemon.status.*;
import org.lockss.state.*;
import org.mortbay.util.B64Code;
import org.lockss.alert.AlertManager;
import org.lockss.alert.*;

/**
 * <p>Class that creates V2 Poll objects</p>
 * @author David Rosenthal
 * @version 1.0
 */

public class V2PollFactory implements PollFactory {
  static final String PARAM_NAMEPOLL_DEADLINE = Configuration.PREFIX +
      "poll.namepoll.deadline";
  static final String PARAM_CONTENTPOLL_MIN = Configuration.PREFIX +
      "poll.contentpoll.min";
  static final String PARAM_CONTENTPOLL_MAX = Configuration.PREFIX +
      "poll.contentpoll.max";

  static final String PARAM_QUORUM = Configuration.PREFIX + "poll.quorum";
  static final String PARAM_DURATION_MULTIPLIER_MIN = Configuration.PREFIX +
      "poll.duration.multiplier.min";
  static final String PARAM_DURATION_MULTIPLIER_MAX = Configuration.PREFIX +
      "poll.duration.multiplier.max";

  // tk - temporary until real name hash estimates
  static final String PARAM_NAME_HASH_ESTIMATE =
    HashService.PARAM_NAME_HASH_ESTIMATE;
  static final long DEFAULT_NAME_HASH_ESTIMATE =
    HashService.DEFAULT_NAME_HASH_ESTIMATE;

  static long DEFAULT_NAMEPOLL_DEADLINE =  10 * Constants.MINUTE;
  static long DEFAULT_CONTENTPOLL_MIN = 3 * Constants.MINUTE;
  static long DEFAULT_CONTENTPOLL_MAX = 5 * Constants.DAY;
  static final int DEFAULT_QUORUM = 5;
  static final int DEFAULT_DURATION_MULTIPLIER_MIN = 3;
  static final int DEFAULT_DURATION_MULTIPLIER_MAX = 7;

  protected static long m_minContentPollDuration;
  protected static long m_maxContentPollDuration;
  protected static long m_minNamePollDuration;
  protected static long m_maxNamePollDuration;
  protected static long m_nameHashEstimate;
  protected static int m_quorum;
  protected static int m_minDurationMultiplier;
  protected static int m_maxDurationMultiplier;

  protected static Logger theLog = Logger.getLogger("V2PollFactory");
  private static LockssRandom theRandom = new LockssRandom();

  protected V2PollFactory() {
  }

  /**
   * Call a poll.  Only used by the tree walk via the poll manager.
   * For V2 sends the poll request.
   * @param pollspec the <code>PollSpec</code> that defines the subject of
   *                 the <code>Poll</code>.
   * @param pm       the PollManager that called this method
   * @return true if the poll was successfuly called.
   */
  public boolean callPoll(PollSpec pollspec,
			  PollManager pm,
			  IdentityManager im) {
    boolean ret = false;
    try {
      sendV2PollRequest(pollspec, pm, im);
      ret = true;
    } catch (IOException ioe) {
      theLog.debug("Exception sending V2 poll request for " +
		   pollspec + ioe);
    }
    return ret;
  }
 
  /**
   * cause a V2 poll by sending a request packet.
   * @param pollspec the <code>PollSpec</code> used to define the range,
   *                 version, and location of poll
   * @param pm       the PollManager that called this method
   * @throws IOException thrown if <code>LcapMessage</code> construction fails.
   */
  private void sendV2PollRequest(PollSpec pollspec,
				 PollManager pm,
				 IdentityManager im)
    throws IOException {
    int opcode = -1;
    switch (pollspec.getPollType()) {
    case Poll.NAME_POLL:
      opcode = LcapMessage.NAME_POLL_REQ;
      break;
    case Poll.CONTENT_POLL:
      opcode = LcapMessage.CONTENT_POLL_REQ;
      break;
    case Poll.VERIFY_POLL:
      opcode = LcapMessage.VERIFY_POLL_REQ;
      break;
    }
    theLog.debug("sending a request for polltype: "
                 + LcapMessage.POLL_OPCODES[opcode] +
                 " for spec " + pollspec);
    CachedUrlSet cus = pollspec.getCachedUrlSet();
    long duration = calcDuration(opcode, cus, pm);
    if(duration <=0) {
      theLog.debug("not sending request for polltype: "
                   + LcapMessage.POLL_OPCODES[opcode] +
                   " for spec " + pollspec + "not enough hash time.");
      return;
    }
    byte[] challenge = pm.makeVerifier(duration);
    byte[] verifier = pm.makeVerifier(duration);
    LcapMessage msg =
      LcapMessage.makeRequestMsg(pollspec,
                                 null,
                                 challenge,
                                 verifier,
                                 opcode,
                                 duration,
                                 im.getLocalPeerIdentity());
    // before we actually send the message make sure that another poll
    // isn't going to conflict with this and create a split poll
    if(checkForConflicts(cus,pm) == null) {
      theLog.debug2("sending poll request message: " +  msg.toString());
      pm.sendMessage(msg, cus.getArchivalUnit());
    }
    else {
      theLog.debug("not sending request for polltype: "
                   + LcapMessage.POLL_OPCODES[opcode] +
                   " for spec " + pollspec + "- conflicts with existing poll.");
    }
  }
  
  /**
   * createPoll is invoked when an incoming message requires a new
   * Poll to be created.
   * @param pollspec the PollSpec for the poll.
   * @param pm the PollManager that called this method
   * @return a Poll object describing the new poll.
   */
  public BasePoll createPoll(LcapMessage msg,
			     PollSpec pollspec,
			     PollManager pm,
			     IdentityManager im) throws ProtocolException {
    BasePoll ret_poll = null;

    if (pollspec.getPollVersion() != 2) {
      throw new ProtocolException("V2PollFactory: bad version " +
				  pollspec.getPollVersion());
    }
    switch (msg.getOpcode()) {
      case LcapMessage.CONTENT_POLL_REP:
      case LcapMessage.CONTENT_POLL_REQ:
	theLog.debug2("Creating content poll for " + pollspec);
	ret_poll = new V2ContentPoll(msg, pollspec, pm);
	break;
      case LcapMessage.NAME_POLL_REP:
      case LcapMessage.NAME_POLL_REQ:
	theLog.debug2("Creating name poll for " + pollspec);
	ret_poll = new V2NamePoll(msg, pollspec, pm);
	break;
      case LcapMessage.VERIFY_POLL_REP:
      case LcapMessage.VERIFY_POLL_REQ:
      default:
        throw new ProtocolException("Unknown opcode:" + msg.getOpcode());
    }
    return ret_poll;
  }



  /**
   * shouldPollBeCreated is invoked to check for conflicts or other
   * version-specific reasons why the poll should not be created at
   * this time.
   * @param msg the LcapMessage that triggered the new Poll
   * @param pollspec the PollSpec for the poll.
   * @param pm the PollManager that called this method.
   * @return true if it is OK to call the poll
   */
   public boolean shouldPollBeCreated(LcapMessage msg,
					 PollSpec pollspec,
					 PollManager pm,
					 IdentityManager im) {
     if(msg.isVerifyPoll()) {
      return true;
     }
     if (checkForConflicts(pollspec.getCachedUrlSet(), pm) != null) {
       return false;
     }
     if (msg.isVerifyPoll()) {
       // if we didn't call the poll and we don't have the verifier ignore this
       if ((pm.getSecret(msg.getChallenge())== null) &&
	   !im.isLocalIdentity(msg.getOriginatorID())) {
	 String ver = String.valueOf(B64Code.encode(msg.getChallenge()));
	 theLog.debug("ignoring verify request from " + msg.getOriginatorID()
		      + " on unknown verifier " + ver);
	 return false;
       }
     }
     return true;
   }


  /**
   * getPollActivity returns the type of activity defined by ActivityRegulator
   * that describes this poll.
   * @param msg the LcapMessage that triggered the new Poll
   * @param pollspec the PollSpec for the poll.
   * @param pm the PollManager that called this method.
   * @return one of the activity codes defined by ActivityRegulator
   */
   public int getPollActivity(LcapMessage msg,
				 PollSpec pollspec,
				 PollManager pm) {
     int activity;
     if (msg.isContentPoll()) {
       if (pollspec.getCachedUrlSet().getSpec().isSingleNode()) {
	 activity = ActivityRegulator.SINGLE_NODE_CONTENT_POLL;
       } else {
	 activity = ActivityRegulator.STANDARD_CONTENT_POLL;
       }
     } else {
       activity = ActivityRegulator.STANDARD_NAME_POLL;
     }
     return activity;
   }

  /**
   * check for conflicts between the poll defined by the Message and any
   * currently existing poll.
   * @param cus the <code>CachedUrlSet</code> from the url and reg expression
   * @return the CachedUrlSet of the conflicting poll.
   */
  private CachedUrlSet checkForConflicts(CachedUrlSet cus, PollManager pm) {

    // eliminate incoming verify polls - never conflicts
    Iterator iter = pm.getActivePollSpecIterator();
    while(iter.hasNext()) {
      PollSpec ps = (PollSpec)iter.next();
      if (ps.getPollType() != Poll.VERIFY_POLL) {
	CachedUrlSet pcus = ps.getCachedUrlSet();
        int rel_pos = cus.cusCompare(pcus);
        if(rel_pos != CachedUrlSet.SAME_LEVEL_NO_OVERLAP &&
           rel_pos != CachedUrlSet.NO_RELATION) {
	  theLog.debug("New poll on " + cus + " conflicts with " + pcus);
          return pcus;
        }
      }
    }
    return null;
  }

  // Poll time calculation
  public long calcDuration(int opcode, CachedUrlSet cus, PollManager pm) {
    int quorum = m_quorum;
    switch (opcode) {
    case LcapMessage.NAME_POLL_REQ:
    case LcapMessage.NAME_POLL_REP: {
      long minPoll = (m_minNamePollDuration +
		      theRandom.nextLong(m_maxNamePollDuration -
					 m_minNamePollDuration));

      return findSchedulableDuration(m_nameHashEstimate,
				     minPoll, m_maxNamePollDuration,
				     m_nameHashEstimate, pm);
    }
    case LcapMessage.CONTENT_POLL_REQ:
    case LcapMessage.CONTENT_POLL_REP: {
      long hashEst = cus.estimatedHashDuration();
      theLog.debug3("CUS estimated hash duration: " + hashEst);

      hashEst = getAdjustedEstimate(hashEst, pm);
      theLog.debug3("My adjusted hash duration: " + hashEst);

      long totalHash = hashEst * (quorum + 1);
      long minPoll = Math.max(totalHash * m_minDurationMultiplier,
			      m_minContentPollDuration);
      long maxPoll = Math.max(Math.min(totalHash * m_maxDurationMultiplier,
				       m_maxContentPollDuration),
			      m_minContentPollDuration);
      return findSchedulableDuration(totalHash, minPoll, maxPoll, totalHash, pm);
    }
    default:
      return -1;
    }
  }

  // try poll durations between min and max at increments of incr.
  // return duration in which all hashing can be scheduled, or -1 if not.

  // tk - multiplicative padding factor to avoid poll durations that nearly
  // completely fill the schedule.

  private long findSchedulableDuration(long hashTime,
				       long min, long max, long incr,
				       PollManager pm) {
    // loop goes maybe one more because we don't want to stop before
    // reaching max
    for (long dur = min; dur <= (max + incr - 1); dur += incr) {
      if (dur > max) {
	dur = max;
      }
      theLog.debug3("Trying to schedule poll of duration: " + dur);
      if (canPollBeScheduled(dur, hashTime, pm)) {
	theLog.debug2("Poll duration: " +
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
    theLog.info("Can't schedule poll within " +
		StringUtil.timeIntervalToString(max));
    return -1;
  }

  public boolean canPollBeScheduled(long pollTime, long hashTime,
				  PollManager pm) {
    if (hashTime > pollTime) {
      theLog.warning("Total hash time " +
		     StringUtil.timeIntervalToString(hashTime) +
		     " greater than max poll time " +
		     StringUtil.timeIntervalToString(pollTime));
      return false;
    }
    Deadline when = Deadline.in(pollTime);
    return canHashBeScheduledBefore(hashTime, when, pm);
  }

  private boolean canHashBeScheduledBefore(long duration, Deadline when,
					   PollManager pm) {
    return pm.getHashService().canHashBeScheduledBefore(duration, when);
  }

  private long getAdjustedEstimate(long estTime, PollManager pm) {
    long my_estimate = estTime;
    long my_rate;
    long slow_rate = pm.getSlowestHashSpeed();
    try {
      my_rate = pm.getBytesPerMsHashEstimate();
    }
    catch (SystemMetrics.NoHashEstimateAvailableException e) {
      // if can't get my rate, use slowest rate to prevent adjustment
      theLog.warning("No hash estimate available, " +
                     "not adjusting poll for slow machines");
      my_rate = slow_rate;
    }
    theLog.debug3("My hash speed is " + my_rate
                  + ". Slow speed is " + slow_rate);


    if (my_rate > slow_rate) {
      my_estimate = estTime * my_rate / slow_rate;
      theLog.debug3("I've corrected the hash estimate to " + my_estimate);
    }
    return my_estimate;
  }

  /**
   * setConfig updates the poll factory's configuration
   * @param newConfig the new gonfiguration of the daemon
   * @param oldConfig the previous configuration of the daemon
   * @param changedKeys the items that have changed
   */
  public void setConfig(Configuration newConfig,
			Configuration oldConfig,
			Configuration.Differences changedKeys) {
    long aveDuration = newConfig.getTimeInterval(PARAM_NAMEPOLL_DEADLINE,
                                                  DEFAULT_NAMEPOLL_DEADLINE);
    m_minNamePollDuration = aveDuration - aveDuration / 4;
    m_maxNamePollDuration = aveDuration + aveDuration / 4;

    // tk - temporary until real name hash estimates
    m_nameHashEstimate = newConfig.getTimeInterval(PARAM_NAME_HASH_ESTIMATE,
						   DEFAULT_NAME_HASH_ESTIMATE);


    m_minContentPollDuration = newConfig.getTimeInterval(PARAM_CONTENTPOLL_MIN,
        DEFAULT_CONTENTPOLL_MIN);
    m_maxContentPollDuration = newConfig.getTimeInterval(PARAM_CONTENTPOLL_MAX,
        DEFAULT_CONTENTPOLL_MAX);

    m_quorum = newConfig.getIntParam(PARAM_QUORUM, DEFAULT_QUORUM);

    m_minDurationMultiplier =
      newConfig.getIntParam(PARAM_DURATION_MULTIPLIER_MIN,
			    DEFAULT_DURATION_MULTIPLIER_MIN);
    m_maxDurationMultiplier =
      newConfig.getIntParam(PARAM_DURATION_MULTIPLIER_MAX,
			    DEFAULT_DURATION_MULTIPLIER_MAX);
  }

  public long getMaxContentPollDuration() {
    return m_maxContentPollDuration;
  }

  public long getMinContentPollDuration() {
    return m_minContentPollDuration;
  }

  public long getMaxNamePollDuration() {
    return m_maxNamePollDuration;
  }

  public long getMinNamePollDuration() {
    return m_minNamePollDuration;
  }

  protected static int getQuorum() {
    return m_quorum;
  }
}
