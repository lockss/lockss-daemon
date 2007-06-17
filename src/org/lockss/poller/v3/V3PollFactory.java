/*
 * $Id: V3PollFactory.java,v 1.13.2.1 2007-06-17 05:54:11 smorabito Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import org.mortbay.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.config.Configuration.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.StringUtil;

public class V3PollFactory extends BasePollFactory {

  private static final String PREFIX = Configuration.PREFIX + "poll.v3.";

  /** The duration multiplier for the minimum length of a V3 poll.  */
  public static final String PARAM_DURATION_MULTIPLIER_MIN =
    PREFIX + "minMultiplier";
  public static final int DEFAULT_DURATION_MULTIPLIER_MIN = 10;
  /** The duration multiplier for the maximum length of a V3 poll */
  public static final String PARAM_DURATION_MULTIPLIER_MAX =
    PREFIX + "maxMultiplier";
  public static final int DEFAULT_DURATION_MULTIPLIER_MAX = 20;
  /** The minimum duration for a V3 poll.  The minimum duration is calculated
   * from the hash duration and maximum number of participants, and this
   * parameter is no longer used.
   * 
   *  @deprecated */
  public static final String PARAM_POLL_DURATION_MIN =
    PREFIX + "minPollDuration";
  public static long DEFAULT_POLL_DURATION_MIN = 24 * Constants.HOUR;
  /** The maximum duration for a V3 poll */
  public static final String PARAM_POLL_DURATION_MAX = 
    PREFIX + "maxPollDuration";
  public static long DEFAULT_POLL_DURATION_MAX = 8 * Constants.WEEK;
  
  /** If set to 'false', do not start V3 Voters when vote requests are
   * received.  This parameter is used by V3PollFactory and PollManager.
   */
  public static final String PARAM_ENABLE_V3_VOTER =
    PREFIX + "enableV3Voter";
  public static final boolean DEFAULT_ENABLE_V3_VOTER = true;

  /** If set to 'false', do not start V3 Polls.  This parameter is used
   * by NodeManagerImpl and PollManager.
   */
  public static final String PARAM_ENABLE_V3_POLLER =
    PREFIX + "enableV3Poller";
  public static final boolean DEFAULT_ENABLE_V3_POLLER = true;

  private int minDurationMultiplier = DEFAULT_DURATION_MULTIPLIER_MIN;
  private int maxDurationMultiplier = DEFAULT_DURATION_MULTIPLIER_MAX;
  private long minPollDuration = DEFAULT_POLL_DURATION_MIN;
  private long maxPollDuration = DEFAULT_POLL_DURATION_MAX;

  public static Logger log = Logger.getLogger("V3PollFactory");


  public boolean callPoll(Poll poll, LockssDaemon daemon) {
    poll.startPoll();
    return true;
  }

  /**
   * Create a V3 Poller or V3 Voter, as appropriate.
   */
  public BasePoll createPoll(PollSpec pollspec, LockssDaemon daemon,
                             PeerIdentity orig, long duration,
                             String hashAlg, LcapMessage msg)
      throws ProtocolException {
    BasePoll retPoll = null;

    CachedUrlSet cus = pollspec.getCachedUrlSet();
    // check for presence of item in the cache
    if (cus == null) {
      log.debug("Ignoring poll request, don't have AU: " + pollspec.getAuId());
      return null;
    }
    ArchivalUnit au = cus.getArchivalUnit();
    if (!pollspec.getPluginVersion().equals(au.getPlugin().getVersion())) {
      log.debug("Ignoring poll request for " + au.getName() +
                   ", plugin version mismatch; have: " +
                   au.getPlugin().getVersion() +
                   ", need: " + pollspec.getPluginVersion());
      return null;
    }
    log.debug("Making poll from: " + pollspec);
    if (pollspec.getProtocolVersion() != Poll.V3_PROTOCOL) {
      throw new ProtocolException("bad version " +
                                  pollspec.getProtocolVersion());
    }
    if (duration <= 0) {
      throw new ProtocolException("bad duration " + duration);
    }
    if (pollspec.getPollType() != Poll.V3_POLL) {
      throw new ProtocolException("Unexpected poll type:" +
                                  pollspec.getPollType());
    }
    
    try {
      if (msg == null) {
        // If there's no message, we're making a poller
        retPoll = makeV3Poller(daemon, pollspec, orig, duration, hashAlg);
      } else {
        // If there's a message, we're making a voter
        retPoll = makeV3Voter(msg, daemon, orig, au);
      }
    } catch (V3Serializer.PollSerializerException ex) {
      log.error("Serialization exception creating new V3Poller: ", ex);
      return null;
    }
    return retPoll;
  }
  

  /**
   * Construct a new V3 Poller to call a poll.
   * 
   * @param daemon The LOCKSS daemon.
   * @param pollspec  The Poll Spec fotr this poll.
   * @param orig  The caller of the poll.
   * @param duration  The duration of the poll.
   * @param hashAlg  The Hash Algorithm used to call the poll.
   * @return A V3 Poller.
   * @throws V3Serializer.PollSerializerException
   */
  private V3Poller makeV3Poller(LockssDaemon daemon, PollSpec pollspec,
                                PeerIdentity orig, long duration,
                                String hashAlg)
      throws V3Serializer.PollSerializerException {
    // Check to see if we're already running too many polls.
    int maxPolls =
      CurrentConfig.getIntParam(V3Poller.PARAM_MAX_SIMULTANEOUS_V3_POLLERS,
                                V3Poller.DEFAULT_MAX_SIMULTANEOUS_V3_POLLERS);
    int activePolls = daemon.getPollManager().getActiveV3Pollers().size();
    if (activePolls >= maxPolls) {
      log.info("Not starting new V3 Poll on AU " + pollspec.getAuId()
               + ".  Maximum number of active pollers is " + maxPolls 
               + "; " + activePolls + " are already running.");
      return null;
    }
    log.debug("Creating V3Poller to call a new poll...");
    String key =
      String.valueOf(B64Code.encode(ByteArray.makeRandomBytes(20)));
    return new V3Poller(pollspec, daemon, orig, key, duration, hashAlg);
  }

  /**
   * Construct a new V3 Voter to participate in a poll.
   * 
   * @param msg  The Poll message that invited this peer.
   * @param daemon  The LOCKSS Daemon
   * @param orig  The caller of the poll
   * @param au  The ArchivalUnit on which the poll is being run.
   * @return  An active V3 Voter.
   * @throws V3Serializer.PollSerializerException
   */
  private V3Voter makeV3Voter(LcapMessage msg, LockssDaemon daemon,
                              PeerIdentity orig, ArchivalUnit au)
      throws V3Serializer.PollSerializerException {
    V3Voter voter = null;
    // Ignore messages from ourself.
    if (orig == daemon.getIdentityManager().getLocalPeerIdentity(Poll.V3_PROTOCOL)) {
      log.info("Not responding to poll request from myself.");
      return null;
    }
    V3LcapMessage m = (V3LcapMessage)msg;
//    // Ignore messages not coming from our group
//    String ourGroup = ConfigManager.getPlatformGroup();
//    if (m.getGroup() == null ||
//        !m.getGroup().equals(ourGroup)) {
//      log.debug("Ignoring message from peer " + m.getOriginatorId()
//                + " in group " + m.getGroup() + " due to group mismatch (" 
//                + ourGroup  + ")");
//      return null;
//    }
    
    // Check to see if we're running too many polls already.
    int maxVoters =
      CurrentConfig.getIntParam(V3Voter.PARAM_MAX_SIMULTANEOUS_V3_VOTERS,
                                V3Voter.DEFAULT_MAX_SIMULTANEOUS_V3_VOTERS);
    int activeVoters = daemon.getPollManager().getActiveV3Voters().size();

    if (activeVoters >= maxVoters) {
      log.info("Not starting new V3 Voter for poll on AU " 
               + au.getAuId() + ".  Maximum number of active voters is " 
               + maxVoters + "; " + activeVoters + " are already running.");
      return null;
    }

    // Only participate if we have and have successfully crawled this AU,
    // and if 'enableV3Voter' is set.
    boolean enableV3Voter =
      CurrentConfig.getBooleanParam(PARAM_ENABLE_V3_VOTER,
                                    DEFAULT_ENABLE_V3_VOTER);
    if (enableV3Voter) { 
      if (AuUtil.getAuState(au).getLastCrawlTime() > 0 ||
          AuUtil.isPubDown(au)) { 
        log.debug("Creating V3Voter to participate in poll " + m.getKey());
        voter = new V3Voter(daemon, m);
        voter.startPoll(); // Voters need to be started immediately.
        if (((V3Voter)voter).isPollCompleted()) {
          return null;
        }
      } else {
        log.debug("Have not completed new content crawl, and publisher " +
                  "is not down, so not participating in poll " + m.getKey());
      }
    } else {
      log.debug("V3 Voter not enabled, so not participating in poll " +
                m.getKey());
    } 
    
    return voter;
  }

  // Not used.
  public int getPollActivity(PollSpec pollspec, PollManager pm) {
    return ActivityRegulator.STANDARD_CONTENT_POLL;
  }

  public void setConfig(Configuration newConfig, Configuration oldConfig,
                        Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      minDurationMultiplier =
        newConfig.getInt(PARAM_DURATION_MULTIPLIER_MIN,
                         DEFAULT_DURATION_MULTIPLIER_MIN);
      maxDurationMultiplier =
        newConfig.getInt(PARAM_DURATION_MULTIPLIER_MAX,
                         DEFAULT_DURATION_MULTIPLIER_MAX);
      minPollDuration =
        newConfig.getTimeInterval(PARAM_POLL_DURATION_MIN,
                                  DEFAULT_POLL_DURATION_MIN);
      maxPollDuration =
        newConfig.getTimeInterval(PARAM_POLL_DURATION_MAX,
                                  DEFAULT_POLL_DURATION_MAX);
    }
  }

  // Not used.
  public long getMaxPollDuration(int pollType) {
    // only one type of V3 poll.
    return maxPollDuration;
  }

  // Not used.
  public long getMinPollDuration(int pollType) {
    // only one type of V3 poll.
    return minPollDuration;
  }

  public long calcDuration(PollSpec ps, PollManager pm) {
    CachedUrlSet cus = ps.getCachedUrlSet();

    long hashEst = cus.estimatedHashDuration();
    log.debug3("CUS estimated hash duration: " + hashEst);

    hashEst = getAdjustedEstimate(hashEst, pm);
    log.debug3("My adjusted hash duration: " + hashEst);

    // In order to calculate the min and max duration, we'll need to find
    // the maximum possible number of participants (the size of the invitation
    // list) from V3Poller, as well as the V3 vote timer padding.
    int maxPollParticipants =
      CurrentConfig.getIntParam(V3Poller.PARAM_MAX_POLL_SIZE,
                                V3Poller.DEFAULT_MAX_POLL_SIZE);
    
    long voteDurationPadding =
      CurrentConfig.getLongParam(V3Poller.PARAM_V3_EXTRA_POLL_TIME,
                                 V3Poller.DEFAULT_V3_EXTRA_POLL_TIME);

    long voteDurationMultiplier =
      CurrentConfig.getLongParam(V3Poller.PARAM_VOTE_DURATION_MULTIPLIER,
                                 V3Poller.DEFAULT_VOTE_DURATION_MULTIPLIER);
      
    long minHashTime = hashEst * (maxPollParticipants + 1);
    
    // Worst case minimum time to complete the poll.
    // - Must hash n versions for each participant, plus myself.
    // - Must account for Vote Deadline Padding and Vote Deadline Multiplier.
    // - Can apply an additional multiplier for very slow machines, or for 
    //   testing with run_multiple_daemons, etc.
    // - Can't be shorter than the vote deadline duration.

    long voteDuration = (hashEst * voteDurationMultiplier) + 
                        voteDurationPadding;
    
    long minPoll = Math.max(minDurationMultiplier * minHashTime +
			    voteDurationPadding,
			    voteDuration);

    // Maximum amount of time we want to allow the poll to run.
    long maxPoll = Math.min(minPoll * maxDurationMultiplier,
			    maxPollDuration);
    
    return findSchedulableDuration(minHashTime, minPoll, maxPoll, 
                                   minHashTime, pm);
  }

  public boolean isDuplicateMessage(LcapMessage msg, PollManager pm) {
    return false;
  }
}
