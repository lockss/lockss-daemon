/*
 * $Id: V3PollFactory.java,v 1.28 2008-11-08 08:15:46 tlipkis Exp $
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

package org.lockss.poller.v3;

import java.io.IOException;
import java.util.*;

import org.mortbay.util.*;

import org.apache.commons.collections.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.config.Configuration.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.protocol.V3LcapMessage.PollNak;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.StringUtil;

public class V3PollFactory extends BasePollFactory {
  public static Logger log = Logger.getLogger("V3PollFactory");

  private static final String PREFIX = Configuration.PREFIX + "poll.v3.";
  
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

  /** Curve expressing probability of accepting invitation based on number
   * of safe replicas known.  E.g., <code>[10,100],[10,10] sets probability
   * to 100% if 10 of fewer repairers, else 10%.  If not set, number of
   * repairers is not considered.
   * @see org.lockss.util.CompoundLinearSlope */
  public static final String PARAM_ACCEPT_PROBABILITY_SAFETY_CURVE =
    PREFIX + "acceptProbabilitySafetyCurve";
  public static final String DEFAULT_ACCEPT_PROBABILITY_SAFETY_CURVE = null;

  /** When counting willing repairers to determine probability of accepting
   * poll, only count those from whom we have heard this recently */
  public static final String PARAM_WILLING_REPAIRER_LIVENESS =
    PREFIX + "willingRepairerLiveness";
  public static final long DEFAULT_WILLING_REPAIRER_LIVENESS =
    2 * Constants.WEEK;

  /** Percentage of time we will participate in a poll called by a peer
   * whom we believe is already a willing repairer.  Applied after
   * acceptProbabilitySafetyCurve. */
  public static final String PARAM_ACCEPT_REPAIRERS_POLL_PERCENT =
    PREFIX + "acceptRepairersPollPercent";
  public static final float DEFAULT_ACCEPT_REPAIRERS_POLL_PERCENT = 0.9f;
    

  private PollManager pollMgr;
  protected IdentityManager idMgr;

  public V3PollFactory(PollManager pollMgr) {
    this.pollMgr = pollMgr;
  }

  public boolean callPoll(Poll poll, LockssDaemon daemon) {
    poll.startPoll();
    return true;
  }

  protected void sendNak(LockssDaemon daemon, PollNak nak,
			 String auid, V3LcapMessage msg) {
    IdentityManager idMgr = daemon.getIdentityManager();

    V3LcapMessage response =
      new V3LcapMessage(auid, msg.getKey(),
			msg.getPluginVersion(), null, null,
			V3LcapMessage.MSG_POLL_ACK,
			TimeBase.nowMs() + msg.getDuration(),
			idMgr.getLocalPeerIdentity(Poll.V3_PROTOCOL),
			null, daemon);
      
    response.setNak(nak);

    try {
      daemon.getPollManager().sendMessageTo(response, msg.getOriginatorId());
    } catch (IOException ex) {
      log.error("IOException trying to send POLL_ACK message: " + ex);
    }
  }

  /**
   * Create a V3 Poller or V3 Voter, as appropriate.
   */
  public BasePoll createPoll(PollSpec pollspec, LockssDaemon daemon,
                             PeerIdentity orig, long duration,
                             String hashAlg, LcapMessage msg)
      throws ProtocolException {
    if (idMgr == null) {
      idMgr = daemon.getIdentityManager();
    }

    BasePoll retPoll = null;

    CachedUrlSet cus = pollspec.getCachedUrlSet();
    // check for presence of item in the cache
    if (cus == null) {
      log.debug("Ignoring poll request, don't have AU: " + pollspec.getAuId());
      
      PollNak reason =
	daemon.areAusStarted() ? PollNak.NAK_NO_AU : PollNak.NAK_NOT_READY;
      sendNak(daemon, reason, pollspec.getAuId(), (V3LcapMessage)msg);
      return null;
    }
    ArchivalUnit au = cus.getArchivalUnit();
    if (!pollspec.getPluginVersion().equals(au.getPlugin().getVersion())) {
      log.debug("Ignoring poll request for " + au.getName() +
                   ", plugin version mismatch; have: " +
                   au.getPlugin().getVersion() +
                   ", need: " + pollspec.getPluginVersion());
      sendNak(daemon, PollNak.NAK_PLUGIN_VERSION_MISMATCH,
	      pollspec.getAuId(), (V3LcapMessage)msg);
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
	if (msg.getOpcode() != V3LcapMessage.MSG_POLL) {
	  log.warning("Received msg for nonexistent poll: " + msg);
	  return null;
	}
	// Remove any record that this peer doesn't have the AU
	deleteFromNoAuPeers(au, orig);
        retPoll = makeV3Voter(daemon, msg, pollspec, orig);
      }
    } catch (V3Serializer.PollSerializerException ex) {
      log.error("Serialization exception creating new V3Poller: ", ex);
      return null;
    }
    return retPoll;
  }
  
  void deleteFromNoAuPeers(ArchivalUnit au, PeerIdentity peer) {
    DatedPeerIdSet noAuSet = pollMgr.getNoAuPeerSet(au);
    log.debug2("Deleting from NoAuPeer: " + au);
    synchronized (noAuSet) {
      try {
	noAuSet.remove(peer);
      } catch (IOException e) {
	log.error("Failed to remove peer from AU set", e);
      }
    }	
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
    log.debug("Creating V3Poller to call a new poll...");
    String key =
      String.valueOf(B64Code.encode(ByteArray.makeRandomBytes(20)));
    return new V3Poller(pollspec, daemon, orig, key, duration, hashAlg);
  }

  /**
   * Construct a new V3 Voter to participate in a poll.
   * 
   * @param daemon  The LOCKSS Daemon
   * @param msg  The Poll message that invited this peer.
   * @param orig  The caller of the poll
   * @param pollspec  The Poll Spec fotr this poll.
   * @return  An active V3 Voter or null if decide not to participate.
   * @throws V3Serializer.PollSerializerException
   */
  private V3Voter makeV3Voter(LockssDaemon daemon, LcapMessage msg,
			      PollSpec pollspec, PeerIdentity orig)
      throws V3Serializer.PollSerializerException {
    IdentityManager idMgr = daemon.getIdentityManager();
    V3LcapMessage m = (V3LcapMessage)msg;

    // Ignore messages from ourself.
    if (orig == idMgr.getLocalPeerIdentity(Poll.V3_PROTOCOL)) {
      log.warning("Got request from myself, ignoring.");
      return null;
    }

    if (!CurrentConfig.getBooleanParam(PARAM_ENABLE_V3_VOTER,
				       DEFAULT_ENABLE_V3_VOTER)) { 
      log.debug("V3 Voter not enabled, so not participating in poll " +
                m.getKey());
      sendNak(daemon, PollNak.NAK_DISABLED, pollspec.getAuId(), m);
      return null;
    }

    // check polling group
    List ourGroups = ConfigManager.getPlatformGroupList();
    if (m.getGroupList() == null ||
        !CollectionUtils.containsAny(ourGroups, m.getGroupList())) {
      sendNak(daemon, PollNak.NAK_GROUP_MISMATCH, pollspec.getAuId(), m);
      return null;
    }
    // check that we have AU
    CachedUrlSet cus = pollspec.getCachedUrlSet();
    if (cus == null) {
      log.debug("Ignoring poll request from " + orig + ", don't have AU: " +
		pollspec.getAuId());
      sendNak(daemon, PollNak.NAK_NO_AU,
	      pollspec.getAuId(), m);
      return null;
    }
    ArchivalUnit au = cus.getArchivalUnit();

    // Never vote if not crawled, even if pub down
    if (AuUtil.getAuState(au).getLastCrawlTime() <= 0) { 
      log.debug("AU not crawled, not voting: " + pollspec.getAuId());
      sendNak(daemon, PollNak.NAK_NOT_CRAWLED, pollspec.getAuId(), m);
      return null;
    }

    // and that plugin version agrees
    if (!pollspec.getPluginVersion().equals(au.getPlugin().getVersion())) {
      log.debug("Ignoring poll request for " + au.getName() +
                   ", plugin version mismatch; have: " +
                   au.getPlugin().getVersion() +
                   ", need: " + pollspec.getPluginVersion());
      sendNak(daemon, PollNak.NAK_PLUGIN_VERSION_MISMATCH,
	      pollspec.getAuId(), m);
      return null;
    }
    V3Voter voter = null;
    // Check to see if we're running too many polls already.

    int maxVoters =
      CurrentConfig.getIntParam(V3Voter.PARAM_MAX_SIMULTANEOUS_V3_VOTERS,
                                V3Voter.DEFAULT_MAX_SIMULTANEOUS_V3_VOTERS);
    int activeVoters = daemon.getPollManager().getActiveV3Voters().size();

    if (activeVoters >= maxVoters) {
      log.info("Not starting new V3 Voter for poll on AU " 
               + au.getAuId() + ".  Maximum number of active voters is " 
               + maxVoters + "; " + activeVoters + " are already running.");
      sendNak(daemon, PollNak.NAK_TOO_MANY_VOTERS, pollspec.getAuId(), m);
      return null;
    }

    PeerIdentityStatus status = idMgr.getPeerIdentityStatus(orig);

    // Make a probabilistic choice based on the number of willing repairers
    // we have for this AU
    if (!ProbabilisticChoice.choose(acceptProb(orig, au))) {
      log.info("Not participating in poll; AU is safe: " + au.getName());
      sendNak(daemon, PollNak.NAK_HAVE_SUFFICIENT_REPAIRERS,
	      pollspec.getAuId(),
	      m);
      return null;
    }

    log.debug("Creating V3Voter to participate in poll " + m.getKey());
    voter = new V3Voter(daemon, m);
//        voter.startPoll(); // Voters need to be started immediately.
    
    // Update the status of the peer that called this poll.
    if (status != null) {
      status.calledPoll();
    }
    return voter;
  }

  /**
   * Compute the probability that we should participate in caller's poll on
   * the AU, based on the need we perceive for it to have more willing
   * repairers.
   *  
   * @param caller the caller of the poll
   * @param au
   * @return A double between 0.0 and 1.0 representing the probability
   *      that we should participate in caller's poll on this AU
   */
  double acceptProb(PeerIdentity caller, ArchivalUnit au) {
    double repairThreshold = pollMgr.getMinPercentForRepair();
    CompoundLinearSlope acceptProbabilityCurve =
      pollMgr.getAcceptProbabilitySafetyCurve();
    if (acceptProbabilityCurve == null) {
      return 1.0;
    }
    int willing = countWillingRepairers(au);
    double prob = acceptProbabilityCurve.getY(willing);
    log.debug2("Accept probability " + prob + "ybased on " + willing +
	       " willing repairers");

    // further reduce probability by acceptRepairersPollPercent if the
    // poll's caller is already a willing repairer
    if (idMgr.getHighestPercentAgreementHint(caller, au) >= repairThreshold) {
      double callerProb = pollMgr.getAcceptRepairersPollPercent();
      if (callerProb != 1.0) {
	prob *= callerProb;
	log.debug2("Reducing probability to " + prob +
		   " because caller is a willing repairer");
      }
    }
    return prob;
  }

  int countWillingRepairers(ArchivalUnit au) {
    return PollUtil.countWillingRepairers(au, pollMgr, idMgr);
  }

  // Not used.
  public int getPollActivity(PollSpec pollspec, PollManager pm) {
    return ActivityRegulator.STANDARD_CONTENT_POLL;
  }

  public void setConfig(Configuration newConfig, Configuration oldConfig,
                        Differences changedKeys) {
  }

  /** Not used.  Only implemented because our interface demands it. */
  public long getMaxPollDuration(int pollType) {
    return 0;
  }

  public long calcDuration(PollSpec ps, PollManager pm) {
    return PollUtil.calcV3Duration(ps, pm);
  }

  public boolean isDuplicateMessage(LcapMessage msg, PollManager pm) {
    return false;
  }
}
