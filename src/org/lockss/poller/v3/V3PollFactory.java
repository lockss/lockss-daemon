/*
 * $Id: V3PollFactory.java,v 1.22 2008-01-30 00:50:15 tlipkis Exp $
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

import java.io.IOException;
import java.util.*;

import org.mortbay.util.*;

import org.apache.commons.collections.CollectionUtils;
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

  public static Logger log = Logger.getLogger("V3PollFactory");

  public boolean callPoll(Poll poll, LockssDaemon daemon) {
    poll.startPoll();
    return true;
  }

  private void sendNak(LockssDaemon daemon, PollNak nak,
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
    BasePoll retPoll = null;

    CachedUrlSet cus = pollspec.getCachedUrlSet();
    // check for presence of item in the cache
    if (cus == null) {
      log.debug("Ignoring poll request, don't have AU: " + pollspec.getAuId());
      sendNak(daemon, PollNak.NAK_NO_AU,
	      pollspec.getAuId(), (V3LcapMessage)msg);
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
        retPoll = makeV3Voter(daemon, msg, pollspec, orig);
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

    if (AuUtil.getAuState(au).getLastCrawlTime() <= 0 &&
	!AuUtil.isPubDown(au)) { 
      log.debug("AU not crawled & pub not down, not voting: " +
		pollspec.getAuId());
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
      sendNak(daemon, PollNak.NAK_NOT_CRAWLED, pollspec.getAuId(), m);
      return null;
    }

    log.debug("Creating V3Voter to participate in poll " + m.getKey());
    voter = new V3Voter(daemon, m);
//        voter.startPoll(); // Voters need to be started immediately.
    
    // Update the status of the peer that called this poll.
    PeerIdentityStatus status = idMgr.getPeerIdentityStatus(orig);
    if (status != null) {
      status.calledPoll();
    }
    return voter;
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
    return PollUtil.calcDuration(ps, pm);
  }

  public boolean isDuplicateMessage(LcapMessage msg, PollManager pm) {
    return false;
  }
}
