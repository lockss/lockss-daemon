/*
 * $Id: V1PollFactory.java,v 1.13 2004-10-23 01:38:22 clairegriffin Exp $
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
 * <p>Class that creates V1 Poll objects</p>
 * @author David Rosenthal
 * @version 1.0
 */

public class V1PollFactory implements PollFactory {
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

  protected static Logger theLog = Logger.getLogger("V1PollFactory");
  private static LockssRandom theRandom = new LockssRandom();

  protected V1PollFactory() {
  }

  /**
   * Call a poll.  Only used by the tree walk via the poll manager.
   * For V1 sends the poll request.
   * @param poll     the <code>Poll</code> to be called
   * @param pm       the PollManager that called this method
   * @return true if the poll was successfuly called.
   */
  public boolean callPoll(Poll poll,
			  PollManager pm,
			  IdentityManager im) {
    boolean ret = false;
    try {
      sendV1PollRequest(poll, pm, im);
      ret = true;
    } catch (IOException ioe) {
      theLog.warning("Exception sending V1 poll request for " + poll, ioe);
    }
    return ret;
  }

  /**
   * cause a V1 poll by sending a request packet.
   * @param pollspec the <code>PollSpec</code> used to define the range,
   *                 version, and location of poll
   * @param pm       the PollManager that called this method
   * @throws IOException thrown if <code>LcapMessage</code> construction fails.
   */
  private void sendV1PollRequest(Poll poll,
				 PollManager pm,
				 IdentityManager im)
    throws IOException {
    PollSpec pollspec = poll.getPollSpec();
    CachedUrlSet cus = pollspec.getCachedUrlSet();
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
    long duration = poll.getDeadline().getRemainingTime();
    if(duration <=0) {
      theLog.debug("not sending request for polltype: "
                   + LcapMessage.POLL_OPCODES[opcode] +
                   " for spec " + pollspec + "not enough hash time.");
      return;
    }
    byte[] challenge = ((V1Poll)poll).getChallenge();
    byte[] verifier = pm.makeVerifier(duration);
    LcapMessage msg =
      LcapMessage.makeRequestMsg(pollspec,
                                 null,
                                 challenge,
                                 verifier,
                                 opcode,
                                 duration,
                                 im.getLocalPeerIdentity(Poll.V1_POLL));
    // before we actually send the message make sure that another poll
    // isn't going to conflict with this and create a split poll
    if(checkForConflicts(cus, pm, ((BasePoll)poll)) == null) {
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
   * @param orig the PeerIdentity that called the poll
   * @param challenge the poll challenge
   * @param verifier the poll verifier
   * @param duration the duration of the poll
   * @param hashAlg the hash algorithm in use
   * @return a Poll object describing the new poll.
   */
  public BasePoll createPoll(PollSpec pollspec,
			     PollManager pm,
			     IdentityManager im,
			     PeerIdentity orig,
			     byte[] challenge,
			     byte[] verifier,
			     long duration,
			     String hashAlg) throws ProtocolException {
    BasePoll ret_poll = null;

    if (pollspec.getPollVersion() != 1) {
      throw new ProtocolException("V1PollFactory: bad version " +
				  pollspec.getPollVersion());
    }
    if (duration <= 0) {
      throw new ProtocolException("V1Pollfactory: bad duration " + duration);
    }
    switch (pollspec.getPollType()) {
      case Poll.CONTENT_POLL:
	theLog.debug2("Creating content poll for " + pollspec);
	ret_poll = new V1ContentPoll(pollspec, pm, orig,
				     challenge, duration,
				     hashAlg);
	break;
      case Poll.NAME_POLL:
	theLog.debug2("Creating name poll for " + pollspec);
	ret_poll = new V1NamePoll(pollspec, pm, orig,
				     challenge, duration,
				     hashAlg);
	break;
      case Poll.VERIFY_POLL:
	theLog.debug2("Creating verify poll for " + pollspec);
	ret_poll = new V1VerifyPoll(pollspec, pm, orig,
				     challenge, duration,
				     hashAlg, verifier);
	break;
      default:
        throw new ProtocolException("Unknown poll type:" +
				    pollspec.getPollType());
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
   * @param challenge the poll challenge
   * @param orig the PeerIdentity that called the poll
   * @return true if it is OK to call the poll
   */
   public boolean shouldPollBeCreated(PollSpec pollspec,
				      PollManager pm,
				      IdentityManager im,
				      byte[] challenge,
				      PeerIdentity orig) {
     if (pollspec.getPollType() == Poll.VERIFY_POLL) {
       // if we didn't call the poll and we don't have the verifier ignore this
       if ((pm.getSecret(challenge)== null) &&
	   !im.isLocalIdentity(orig)) {
	 String ver = String.valueOf(B64Code.encode(challenge));
	 theLog.debug("ignoring verify request from " + orig
		      + " on unknown verifier " + ver);
	 return false;
       }
       theLog.debug2("OK to call verify poll");
       return true;
     }
     CachedUrlSet cus = checkForConflicts(pollspec.getCachedUrlSet(), pm);
     if (cus != null) {
       theLog.debug("Poll on " + pollspec + " has conflicts with " + cus);
       return false;
     }
     return true;
   }


  /**
   * getPollActivity returns the type of activity defined by ActivityRegulator
   * that describes this poll.
   * @param pollspec the PollSpec for the poll.
   * @param pm the PollManager that called this method.
   * @return one of the activity codes defined by ActivityRegulator
   */
   public int getPollActivity(PollSpec pollspec,
			      PollManager pm) {
     int activity = ActivityRegulator.NO_ACTIVITY;
     int pollType = pollspec.getPollType();
     if (pollType == Poll.CONTENT_POLL) {
       if (pollspec.getCachedUrlSet().getSpec().isSingleNode()) {
	 activity = ActivityRegulator.SINGLE_NODE_CONTENT_POLL;
       } else {
	 activity = ActivityRegulator.STANDARD_CONTENT_POLL;
       }
     } else if (pollType == Poll.NAME_POLL) {
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

    return checkForConflicts(cus, pm, null);
  }

  private CachedUrlSet checkForConflicts(CachedUrlSet cus,
					 PollManager pm,
					 BasePoll poll) {
    if (theLog.isDebug2()) {
      if (poll == null) {
	theLog.debug2("checkForConflicts on " + cus);
      } else {
	theLog.debug2("checkForConflicts on " + cus + " excluding " + poll);
      }
    }
    Iterator iter = pm.getActivePollSpecIterator(cus.getArchivalUnit(), poll);
    while(iter.hasNext()) {
      PollSpec ps = (PollSpec)iter.next();
      if (theLog.isDebug2()) {
	theLog.debug2("compare " + cus + " with " + ps.getCachedUrlSet());
      }
      if (ps.getPollType() != Poll.VERIFY_POLL) {
        CachedUrlSet pcus = ps.getCachedUrlSet();
        int rel_pos = cus.cusCompare(pcus);
        if (rel_pos != CachedUrlSet.SAME_LEVEL_NO_OVERLAP &&
            rel_pos != CachedUrlSet.NO_RELATION) {
          // allow name polls to overlap
          if (ps.getPollType() != Poll.NAME_POLL ||
              rel_pos != CachedUrlSet.SAME_LEVEL_OVERLAP) {
            theLog.debug("New poll on " + cus + " conflicts with " + pcus);
            return pcus;
          }
        }
      }
    }
    theLog.debug2("New poll on " + cus + " no conflicts");
    return null;
  }

  // Poll time calculation
  public long calcDuration(PollSpec pollspec, PollManager pm) {
    CachedUrlSet cus = pollspec.getCachedUrlSet();
    int quorum = m_quorum;
    switch (pollspec.getPollType()) {
    case Poll.NAME_POLL: {
      long minPoll = (m_minNamePollDuration +
		      theRandom.nextLong(m_maxNamePollDuration -
					 m_minNamePollDuration));

      return findSchedulableDuration(m_nameHashEstimate,
				     minPoll, m_maxNamePollDuration,
				     m_nameHashEstimate, pm);
    }
    case Poll.CONTENT_POLL: {
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
    theLog.debug("Try to schedule " + pollTime + " poll " + hashTime + " poll");
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

  boolean canHashBeScheduledBefore(long duration, Deadline when,
					   PollManager pm) {
    boolean ret = pm.getHashService().canHashBeScheduledBefore(duration, when);
    theLog.debug("canHashBeScheduledBefore(" + duration + "," + when + ")" +
		 " returns " + ret);
    return ret;
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

  public long getMaxPollDuration(int pollType) {
    switch (pollType) {
    case Poll.CONTENT_POLL: return m_maxContentPollDuration;
    case Poll.NAME_POLL: return m_maxNamePollDuration;
    default:
      theLog.warning("getMaxPollDuration(" + pollType + ")");
      return 0;
    }
  }

  public long getMinPollDuration(int pollType) {
    switch (pollType) {
    case Poll.CONTENT_POLL: return m_minContentPollDuration;
    case Poll.NAME_POLL: return m_minNamePollDuration;
    default:
      theLog.warning("getMinPollDuration(" + pollType + ")");
      return 0;
    }
  }

  protected static int getQuorum() {
    return m_quorum;
  }
}
