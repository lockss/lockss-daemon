/*
 * $Id: V3PollFactory.java,v 1.6 2006-08-07 18:47:48 tlipkis Exp $
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

  /** The minimum duration multiplier for a V3 poll */
  public static final String PARAM_DURATION_MULTIPLIER_MIN =
    PREFIX + "minMultiplier";
  public static final int DEFAULT_DURATION_MULTIPLIER_MIN = 3;
  /** The maximum duration multiplier for a V3 poll */
  public static final String PARAM_DURATION_MULTIPLIER_MAX =
    PREFIX + "maxMultiplier";
  public static final int DEFAULT_DURATION_MULTIPLIER_MAX = 7;
  /** The minimum duration for a V3 poll */
  public static final String PARAM_POLL_DURATION_MIN =
    PREFIX + "minPollDuration";
  public static long DEFAULT_POLL_DURATION_MIN = 24 * Constants.HOUR;
  /** The maximum duration for a V3 poll */
  public static final String PARAM_POLL_DURATION_MAX = 
    PREFIX + "maxPollDuration";
  public static long DEFAULT_POLL_DURATION_MAX = 5 * Constants.WEEK;

  private int minDurationMultiplier = DEFAULT_DURATION_MULTIPLIER_MIN;
  private int maxDurationMultiplier = DEFAULT_DURATION_MULTIPLIER_MAX;
  private long minPollDuration = DEFAULT_POLL_DURATION_MIN;
  private long maxPollDuration = DEFAULT_POLL_DURATION_MAX;

  public static Logger log = Logger.getLogger("V3PollFactory");


  public boolean callPoll(Poll poll, LockssDaemon daemon) {
    poll.startPoll();
    return true;
  }

  public BasePoll createPoll(PollSpec pollspec, LockssDaemon daemon,
                             PeerIdentity orig, long duration,
                             String hashAlg, LcapMessage msg)
      throws ProtocolException {
    BasePoll retPoll = null;
    IdentityManager idManager = daemon.getIdentityManager();

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
        log.debug("Creating V3Poller to call a new poll...");
        // XXX: Better keys?
        String key =
          String.valueOf(B64Code.encode(ByteArray.makeRandomBytes(20)));
        retPoll = new V3Poller(pollspec, daemon, orig, key, duration, hashAlg);
      } else {
        // Ignore messages from ourself.
        if (orig == idManager.getLocalPeerIdentity(Poll.V3_PROTOCOL)) {
          log.info("Not responding to poll request from myself.");
          return null;
        }

        V3LcapMessage m = (V3LcapMessage)msg;
        PollSpec s = new PollSpec(m);
        // Only participate if we have and have successfully crawled this AU.
        if (AuUtil.getAuState(au).getLastCrawlTime() > 0) { 
          log.debug("Creating V3Voter to participate in poll " + m.getKey());
          retPoll = new V3Voter(s, daemon, m.getOriginatorId(), m.getKey(),
                                m.getEffortProof(), m.getPollerNonce(),
                                m.getDuration(), m.getHashAlgorithm());
          retPoll.startPoll(); // Voters need to be started immediately.
        } else {
          log.debug("Have not completed new content crawl.  Not " +
                    "participating in vote.");
        }
      }
    } catch (V3Serializer.PollSerializerException ex) {
      log.error("Serialization exception creating new V3Poller: ", ex);
    }
    return retPoll;
  }

  // XXX: Should V3 be different than 'standard'?
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

  public long getMaxPollDuration(int pollType) {
    // only one type of V3 poll.
    return maxPollDuration;
  }

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

    long minPoll = Math.max(hashEst * minDurationMultiplier,
                            minPollDuration);
    long maxPoll = Math.max(Math.min(hashEst * maxDurationMultiplier,
                                     maxPollDuration),
                                     minPollDuration);
    return findSchedulableDuration(hashEst, minPoll, maxPoll, hashEst, pm);
  }

  // XXX: It is very unlikely that a V3 poll would cause duplicate messages,
  // but there should still be a way to determine if this ever happens, and
  // log some sort of error.
  public boolean isDuplicateMessage(LcapMessage msg, PollManager pm) {
    return false;
  }
}
