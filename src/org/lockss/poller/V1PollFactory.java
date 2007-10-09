/*
 * $Id: V1PollFactory.java,v 1.23 2007-10-09 00:49:55 smorabito Exp $
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

import java.io.IOException;
import java.security.*;
import java.util.Iterator;

import org.mortbay.util.B64Code;

import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.daemon.ActivityRegulator;
import org.lockss.hasher.HashService;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.state.NodeManager;
import org.lockss.util.*;

/**
 * <p>Class that creates V1 Poll objects</p>
 * @author David Rosenthal
 * @version 1.0
 */

public class V1PollFactory extends BasePollFactory {
  static final String PREFIX = Configuration.PREFIX + "poll.";

  static final String PARAM_NAMEPOLL_DEADLINE = PREFIX +
    "namepoll.deadline";
  static final String PARAM_CONTENTPOLL_MIN = PREFIX +
    "contentpoll.min";
  static final String PARAM_CONTENTPOLL_MAX = PREFIX +
    "contentpoll.max";

  static final String PARAM_QUORUM = PREFIX + "quorum";
  static final String PARAM_DURATION_MULTIPLIER_MIN = PREFIX +
    "duration.multiplier.min";
  static final String PARAM_DURATION_MULTIPLIER_MAX = PREFIX +
    "duration.multiplier.max";

  static final String PARAM_VERIFY_EXPIRATION = PREFIX + "expireVerifier";

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
  static final long DEFAULT_VERIFY_EXPIRATION = 6 * Constants.HOUR;


  protected long m_minContentPollDuration;
  protected long m_maxContentPollDuration;
  protected long m_minNamePollDuration;
  protected long m_maxNamePollDuration;
  protected long m_nameHashEstimate;
  protected static int m_quorum;
  protected int m_minDurationMultiplier;
  protected int m_maxDurationMultiplier;
  protected long m_verifierExpireTime = DEFAULT_VERIFY_EXPIRATION;


  private static VariableTimedMap theVerifiers = new VariableTimedMap();

  protected static Logger theLog = Logger.getLogger("V1PollFactory");
  private static LockssRandom theRandom = new LockssRandom();

  protected V1PollFactory() {
  }

  /**
   * Call a poll.  Only used by the tree walk via the poll manager.
   * For V1 sends the poll request.
   * @param poll     the <code>Poll</code> to be called
   * @param daemon   the <code>LockssDaemon</code>
   * @return true if the poll was successfuly called.
   */
  public boolean callPoll(Poll poll,
			  LockssDaemon daemon) {
    boolean ret = false;
    try {
      sendPollRequest(poll,
                      daemon.getPollManager(),
                      daemon.getIdentityManager());
      ret = true;
    } catch (IOException ioe) {
      theLog.warning("Exception sending V1 poll request for " + poll, ioe);
    }
    return ret;
  }

  /**
   * cause a V1 poll by sending a request packet.
   * @param poll {@link Poll} for which we are sending a request
   * @param pm the PollManager that called this method
   * @param im {@link IdentityManager}
   * @throws IOException thrown if <code>LcapMessage</code> construction fails.
   */
  private void sendPollRequest(Poll poll,
                               PollManager pm,
                               IdentityManager im)
      throws IOException {
    PollSpec pollspec = poll.getPollSpec();
    CachedUrlSet cus = pollspec.getCachedUrlSet();
    int opcode = -1;
    switch (pollspec.getPollType()) {
    case Poll.V1_NAME_POLL:
      opcode = V1LcapMessage.NAME_POLL_REQ;
      break;
    case Poll.V1_CONTENT_POLL:
      opcode = V1LcapMessage.CONTENT_POLL_REQ;
      break;
    case Poll.V1_VERIFY_POLL:
      opcode = V1LcapMessage.VERIFY_POLL_REQ;
      break;
    }
    theLog.debug("Constructing request for poll: " + pollspec);
    long duration = poll.getDeadline().getRemainingTime();
    if (duration <= 0) {
      theLog.debug("duration < 0 (" + duration +
		   "), not sending request for poll: " + pollspec);
      return;
    }
    byte[] challenge = ((V1Poll)poll).getChallenge();
    byte[] verifier = makeVerifier(duration);
    V1LcapMessage msg =
      V1LcapMessage.makeRequestMsg(pollspec,
				   null,
				   challenge,
				   verifier,
				   opcode,
				   duration,
				   im.getLocalPeerIdentity(Poll.V1_PROTOCOL));
    // before we actually send the message make sure that another poll
    // isn't going to conflict with this and create a split poll
    if(checkForConflicts(cus, pm, ((BasePoll)poll)) == null) {
      theLog.debug2("sending poll request message: " +  msg.toString());
      pm.sendMessage(msg, cus.getArchivalUnit());
    }
    else {
      theLog.debug("not sending request for polltype: "
		   + V1LcapMessage.POLL_OPCODES[opcode] +
		   " for spec " + pollspec + "- conflicts with existing poll.");
    }
  }

  /**
   * createPoll is invoked when an incoming message requires a new
   * Poll to be created.
   * @param pollspec the PollSpec for the poll.
   * @param daemon the LockssDaemon
   * @param orig the PeerIdentity that called the poll
   * @param duration the duration of the poll
   * @param hashAlg the hash algorithm in use
   * @return a Poll object describing the new poll.
   */
  public BasePoll createPoll(PollSpec pollspec,
			     LockssDaemon daemon,
			     PeerIdentity orig,
			     long duration,
			     String hashAlg,
                             LcapMessage msg) throws ProtocolException {
    BasePoll ret_poll = null;
    PollManager pm = daemon.getPollManager();
    IdentityManager im = daemon.getIdentityManager();
    CachedUrlSet cus = pollspec.getCachedUrlSet();
    ArchivalUnit au = cus.getArchivalUnit();
    byte[] challenge;
    byte[] verifier;
    ActivityRegulator.Lock lock = null;

    // check with regulator if not verify poll
    if (pollspec.getPollType() != Poll.V1_VERIFY_POLL) {
      // get expiration time for the lock
      long expiration = 2 * duration;
      if (AuUrl.isAuUrl(cus.getUrl())) {
        lock = daemon.getActivityRegulator(au).
          getAuActivityLock(ActivityRegulator.TOP_LEVEL_POLL, expiration);
        if (lock==null) {
          theLog.debug2("New top-level poll aborted due to activity lock.");
          return null;
        }
      } else {
        int activity = getPollActivity(pollspec, daemon.getPollManager());
        ActivityRegulator ar = daemon.getActivityRegulator(au);
        if (ar == null) {
          theLog.warning("Activity regulator null for au: " + au.toString());
          return null;
        }
        if (theLog.isDebug2()) {
          theLog.debug2("about to get lock for " + cus.toString() + " act " +
                        activity + " for " +
                        StringUtil.timeIntervalToString(expiration));
        }
        lock = ar.getCusActivityLock(cus, activity, expiration);
        if (lock==null) {
          theLog.debug("New poll aborted due to activity lock.");
          return null;
        }
      }
    }

    if (msg == null) {
      challenge = makeVerifier(duration);
      verifier = makeVerifier(duration);
    } else {
      V1LcapMessage m = (V1LcapMessage)msg;
      challenge = m.getChallenge();
      verifier = m.getVerifier();
    }

    // check for conflicts
    if (!shouldPollBeCreated(pollspec, pm, im,
                             challenge, orig)) {
      theLog.debug("Poll request ignored");
      return null;
    }

    if (pollspec.getProtocolVersion() != 1) {
      throw new ProtocolException("V1PollFactory: bad version " +
				  pollspec.getProtocolVersion());
    }
    if (duration <= 0) {
      throw new ProtocolException("V1Pollfactory: bad duration " + duration);
    }
    switch (pollspec.getPollType()) {
    case Poll.V1_CONTENT_POLL:
      theLog.debug2("Creating content poll for " + pollspec);
      ret_poll = new V1ContentPoll(pollspec, pm, orig,
				   challenge, duration,
				   hashAlg);
      break;
    case Poll.V1_NAME_POLL:
      theLog.debug2("Creating name poll for " + pollspec);
      ret_poll = new V1NamePoll(pollspec, pm, orig,
				challenge, duration,
				hashAlg);
      break;
    case Poll.V1_VERIFY_POLL:
      theLog.debug2("Creating verify poll for " + pollspec);
      ret_poll = new V1VerifyPoll(pollspec, pm, orig,
				  challenge, duration,
				  hashAlg, verifier);
      break;
    default:
      throw new ProtocolException("Unknown poll type:" +
				  pollspec.getPollType());
    }


    NodeManager nm = daemon.getNodeManager(cus.getArchivalUnit());
    if (pollspec.getPollType() != Poll.V1_VERIFY_POLL) {
      if (!nm.shouldStartPoll(cus, ret_poll.getVoteTally())) {
          theLog.debug("NodeManager said not to start poll: " + ret_poll);
          // clear the lock
          lock.expire();
          return null;
        }
    }

    if (pollspec.getPollType() != Poll.V1_VERIFY_POLL &&
        !(pollspec.getPollType() == Poll.V1_NAME_POLL &&
            pollspec.getLwrBound() != null)) {
        // set the activity lock in the tally
        ret_poll.getVoteTally().setActivityLock(lock);
        nm.startPoll(cus, ret_poll.getVoteTally(), false);
    }

    ret_poll.startPoll();
    theLog.debug2("Started new poll: " + ret_poll.getKey());

    return ret_poll;
  }

  boolean shouldPollBeCreated(PollSpec pollspec,
                              PollManager pm,
                              IdentityManager im,
                              byte[] challenge,
                              PeerIdentity orig) {
    if (pollspec.getPollType() == Poll.V1_VERIFY_POLL) {
      // if we didn't call the poll and we don't have the verifier ignore this
      if ((getSecret(challenge) == null) &&
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
    if (pollType == Poll.V1_CONTENT_POLL) {
      if (pollspec.getCachedUrlSet().getSpec().isSingleNode()) {
	activity = ActivityRegulator.SINGLE_NODE_CONTENT_POLL;
      } else {
	activity = ActivityRegulator.STANDARD_CONTENT_POLL;
      }
    } else if (pollType == Poll.V1_NAME_POLL) {
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
      if (theLog.isDebug3()) {
	theLog.debug3("compare " + cus + " with " + ps.getCachedUrlSet());
      }
      if (ps.getPollType() != Poll.V1_VERIFY_POLL) {
	CachedUrlSet pcus = ps.getCachedUrlSet();
	int rel_pos = cus.cusCompare(pcus);
	if (rel_pos != CachedUrlSet.SAME_LEVEL_NO_OVERLAP &&
	    rel_pos != CachedUrlSet.NO_RELATION) {
	  if (theLog.isDebug2()) {
	    theLog.debug2("New poll on " + cus + " conflicts with " + pcus);
	  }
	  return pcus;
	  //           // allow name polls to overlap
	  //           if (ps.getPollType() != Poll.NAME_POLL ||
	  //               rel_pos != CachedUrlSet.SAME_LEVEL_OVERLAP) {
	  //	    if (theLog.isDebug2()) {
	  //	      theLog.debug2("New poll on " + cus + " conflicts with " + pcus);
	  //	    }
	  //             return pcus;
	  //           }
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
    case Poll.V1_NAME_POLL: {
      long minPoll = (m_minNamePollDuration +
		      theRandom.nextLong(m_maxNamePollDuration -
					 m_minNamePollDuration));

      return PollUtil.findSchedulableDuration(m_nameHashEstimate,
                                              minPoll, m_maxNamePollDuration,
                                              m_nameHashEstimate, pm);
    }
    case Poll.V1_CONTENT_POLL: {
      long hashEst = cus.estimatedHashDuration();
      theLog.debug3("CUS estimated hash duration: " + hashEst);

      hashEst = PollUtil.getAdjustedEstimate(pollspec, pm);
      theLog.debug3("My adjusted hash duration: " + hashEst);

      long totalHash = hashEst * (quorum + 1);
      long minPoll = Math.max(totalHash * m_minDurationMultiplier,
			      m_minContentPollDuration);
      long maxPoll = Math.max(Math.min(totalHash * m_maxDurationMultiplier,
				       m_maxContentPollDuration),
			      m_minContentPollDuration);
      return PollUtil.findSchedulableDuration(totalHash, minPoll, maxPoll, 
                                              totalHash, pm);
    }
    default:
      return -1;
    }
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
    
    m_minContentPollDuration = newConfig.getTimeInterval(PARAM_CONTENTPOLL_MIN,
                                                         DEFAULT_CONTENTPOLL_MIN);
    m_maxContentPollDuration = newConfig.getTimeInterval(PARAM_CONTENTPOLL_MAX,
                                                         DEFAULT_CONTENTPOLL_MAX);
    
    m_quorum = CurrentConfig.getIntParam(PARAM_QUORUM, DEFAULT_QUORUM);
    
    m_minDurationMultiplier =
      CurrentConfig.getIntParam(PARAM_DURATION_MULTIPLIER_MIN,
                                DEFAULT_DURATION_MULTIPLIER_MIN);
    m_maxDurationMultiplier =
      CurrentConfig.getIntParam(PARAM_DURATION_MULTIPLIER_MAX,
                                DEFAULT_DURATION_MULTIPLIER_MAX);
    m_verifierExpireTime =
      newConfig.getTimeInterval(PARAM_VERIFY_EXPIRATION,
                                DEFAULT_VERIFY_EXPIRATION);
    // tk - temporary until real name hash estimates
    m_nameHashEstimate = newConfig.getTimeInterval(PARAM_NAME_HASH_ESTIMATE,
                                                   DEFAULT_NAME_HASH_ESTIMATE);
  }

  public long getMaxPollDuration(int pollType) {
    switch (pollType) {
    case Poll.V1_CONTENT_POLL: return m_maxContentPollDuration;
    case Poll.V1_NAME_POLL: return m_maxNamePollDuration;
    default:
      theLog.warning("getMaxPollDuration(" + pollType + ")");
      return 0;
    }
  }

  public long getMinPollDuration(int pollType) {
    switch (pollType) {
    case Poll.V1_CONTENT_POLL: return m_minContentPollDuration;
    case Poll.V1_NAME_POLL: return m_minNamePollDuration;
    default:
      theLog.warning("getMinPollDuration(" + pollType + ")");
      return 0;
    }
  }

  public boolean isDuplicateMessage(LcapMessage msg, PollManager pm) {
    byte[] verifier = ((V1LcapMessage)msg).getVerifier();
    String ver = String.valueOf(B64Code.encode(verifier));
    // lock paranoia - access (synchronized) thePolls outside theVerifiers lock

    boolean havePoll = pm.hasPoll(msg.getKey());

    synchronized (theVerifiers) {
      String secret = (String)theVerifiers.get(ver);
      // if we have a secret and we don't have a poll
      // we made the original message but haven't made a poll yet
      if(!StringUtil.isNullString(secret) && !havePoll) {
        return false;
      }
      else if(secret == null) { // we didn't make the verifier-we don't have a secret
        rememberVerifier(verifier, null, msg.getDuration());
      }
      return secret != null;   // we made the verifier-we should have a secret
    }
  }

  private void rememberVerifier(byte[] verifier,
                                byte[] secret,
                                long duration) {
    String ver = String.valueOf(B64Code.encode(verifier));
    String sec = secret == null ? "" : String.valueOf(B64Code.encode(secret));
    Deadline d = Deadline.in(m_verifierExpireTime + duration);
    synchronized (theVerifiers) {
      theVerifiers.put(ver, sec, d);
    }
  }

  /* Non-interface methods */

  /**
   * Called by verify polls to get the array of bytes that represents the
   * secret used to generate the verifier bytes.
   * @param verifier the array of bytes that is a hash of the secret.
   * @return the array of bytes representing the secret or if no matching
   * verifier is found, null.
   */
  byte[] getSecret(byte[] verifier) {
    String ver = String.valueOf(B64Code.encode(verifier));
    String sec;
    synchronized (theVerifiers) {
      sec = (String)theVerifiers.get(ver);
    }
    if (sec != null && sec.length() > 0) {
      return (B64Code.decode(sec.toCharArray()));
    }
    return null;
  }

  /**
   * make a verifier by generating a secret and hashing it. Then store the
   * verifier/secret pair in the verifiers table.
   * @param duration time the item we're verifying is expected to take.
   * @return the array of bytes representing the verifier
   */
  byte[] makeVerifier(long duration) {
    byte[] s_bytes = ByteArray.makeRandomBytes(20);
    byte[] v_bytes = generateVerifier(s_bytes);
    if(v_bytes != null) {
      rememberVerifier(v_bytes, s_bytes, duration);
    }
    return v_bytes;
  }

  /**
   * generate a verifier from a array of bytes representing a secret
   * @param secret the bytes representing a secret to be hashed
   * @return an array of bytes representing a verifier
   */
  public byte[] generateVerifier(byte[] secret) {
    byte[] verifier = null;
    MessageDigest digest = getMessageDigest(null);
    digest.update(secret, 0, secret.length);
    verifier = digest.digest();

    return verifier;
  }

  /**
   * return a MessageDigest needed to hash this message
   * @param msg the LcapMessage which needs to be hashed or null to use
   * the default digest algorithm
   * @return the MessageDigest
   */
  public MessageDigest getMessageDigest(LcapMessage msg) {
    MessageDigest digest = null;
    String algorithm;
    if(msg == null) {
      algorithm = LcapMessage.getDefaultHashAlgorithm();
    }
    else {
      algorithm = msg.getHashAlgorithm();
    }
    try {
      digest = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException ex) {
      theLog.error("Unable to run - no MessageDigest");
    }

    return digest;
  }

  protected static int getQuorum() {
    return m_quorum;
  }
}
