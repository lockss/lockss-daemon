/*
* $Id: PollManager.java,v 1.111 2003-07-24 20:41:18 clairegriffin Exp $
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
import java.net.*;
import java.security.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.protocol.ProtocolException;
import org.lockss.util.*;
import org.mortbay.util.B64Code;
import gnu.regexp.*;
import org.lockss.hasher.HashService;
import org.lockss.repository.LockssRepository;
import org.lockss.daemon.status.*;
import org.lockss.state.*;

public class PollManager  extends BaseLockssManager {
  static final String PARAM_RECENT_EXPIRATION = Configuration.PREFIX +
      "poll.expireRecent";
  static final String PARAM_VERIFY_EXPIRATION = Configuration.PREFIX +
      "poll.expireVerifier";

  static final String PARAM_NAMEPOLL_DEADLINE = Configuration.PREFIX +
      "poll.namepoll.deadline";
  static final String PARAM_CONTENTPOLL_MIN = Configuration.PREFIX +
      "poll.contentpoll.min";
  static final String PARAM_CONTENTPOLL_MAX = Configuration.PREFIX +
      "poll.contentpoll.max";

  static final String PARAM_QUORUM = Configuration.PREFIX + "poll.quorum";
  static final String PARAM_DURATION_MULTIPLIER= Configuration.PREFIX +
      "poll.duration.multiplier";

  static long DEFAULT_NAMEPOLL_DEADLINE =  10 * Constants.MINUTE;
  static long DEFAULT_CONTENTPOLL_MIN = 3 * Constants.MINUTE;
  static long DEFAULT_CONTENTPOLL_MAX = 5 * Constants.DAY;
  static final int DEFAULT_QUORUM = 5;

  static final long DEFAULT_RECENT_EXPIRATION = Constants.DAY;
  static final long DEFAULT_VERIFY_EXPIRATION = Constants.DAY;

  static final int DEFAULT_DURATION_MULTIPLIER = 4;

  private static PollManager theManager = null;
  private static Logger theLog=Logger.getLogger("PollManager");
  private static LcapRouter.MessageHandler  m_msgHandler;
  private static Hashtable thePolls = new Hashtable();
  private static HashMap theVerifiers = new HashMap();
  private static IdentityManager theIDManager;
  private static HashService theHashService;
  private static LockssRandom theRandom = new LockssRandom();
  private static LcapRouter theRouter = null;
  private static SystemMetrics theSystemMetrics = null;

  // our configuration variables
  protected static long m_minContentPollDuration;
  protected static long m_maxContentPollDuration;
  protected static long m_minNamePollDuration;
  protected static long m_maxNamePollDuration;
  protected static long m_recentPollExpireTime;
  protected static long m_verifierExpireTime;
  protected static int m_quorum;
  protected static int m_durationMultiplier;

  public PollManager() {
  }

  /**
   * start the poll manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    // the services we use on an ongoing basis
    theIDManager = theDaemon.getIdentityManager();
    theHashService = theDaemon.getHashService();

    // register a message handler with the router
    theRouter = theDaemon.getRouterManager();
    m_msgHandler =  new RouterMessageHandler();
    theRouter.registerMessageHandler(m_msgHandler);

    // get System Metrics
    theSystemMetrics = theDaemon.getSystemMetrics();
    // register our status
    StatusService statusServ = theDaemon.getStatusService();
    PollerStatus pStatus = new PollerStatus(this);
    statusServ.registerStatusAccessor(PollerStatus.MANAGER_STATUS_TABLE_NAME,
                                      new PollerStatus.ManagerStatus());
    statusServ.registerStatusAccessor(PollerStatus.POLL_STATUS_TABLE_NAME,
                                      new PollerStatus.PollStatus());
    statusServ.registerObjectReferenceAccessor(PollerStatus.MANAGER_STATUS_TABLE_NAME,
					       ArchivalUnit.class,
					       new PollerStatus.ManagerStatusAURef());
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // unregister our status
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.unregisterStatusAccessor(PollerStatus.MANAGER_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(PollerStatus.POLL_STATUS_TABLE_NAME);
    statusServ.unregisterObjectReferenceAccessor(
        PollerStatus.MANAGER_STATUS_TABLE_NAME, ArchivalUnit.class);

    // unregister our router
    theRouter.unregisterMessageHandler(m_msgHandler);

    // null anything which might cause problems
    theIDManager = null;
    theHashService = null;
    theSystemMetrics = null;
    thePolls.clear();
    super.stopService();
  }


  /**
   * make an election by sending a request packet.  This is only
   * called from the tree walk. The poll remains pending in the
   * @param opcode the poll message opcode
   * @param pollspec the PollSpec used to define the range and location of poll
   * @throws IOException thrown if Message construction fails.
   */
  public void sendPollRequest(int opcode, PollSpec pollspec)
      throws IOException {
    theLog.debug("sending a request for polltype: "
                 + LcapMessage.POLL_OPCODES[opcode] +
                 " for spec " + pollspec);
    CachedUrlSet cus = pollspec.getCachedUrlSet();
    long duration = pollspec.calcDuration(opcode, cus, this);
    if(duration <=0) {
      theLog.debug("not sending request for polltype: "
                   + LcapMessage.POLL_OPCODES[opcode] +
                   " for spec " + pollspec + "not enough hash time.");
      return;
    }
    byte[] challenge = makeVerifier(duration);
    byte[] verifier = makeVerifier(duration);
    LcapMessage msg =
      LcapMessage.makeRequestMsg(pollspec,
				 null,
				 challenge,
				 verifier,
				 opcode,
				 duration,
				 theIDManager.getLocalIdentity());

    // before we actually send the message make sure that another poll
    // isn't going to conflict with this and create a split poll
    if(checkForConflicts(msg,cus) == null) {
      theLog.debug2("sending poll request message: " +  msg.toString());
      sendMessage(msg, cus.getArchivalUnit());
    }
    else {
      theLog.debug("not sending request for polltype: "
                   + LcapMessage.POLL_OPCODES[opcode] +
                   " for spec " + pollspec + "- conflicts with existing poll.");
    }
  }


  /**
   * Is a poll of the given type and spec currently running
   * @param type the type of the poll.
   * @param spec the PollSpec definining the location of the poll.
   * @return true if we have a poll which is running that matches pollspec
   */
  public boolean isPollRunning(int type, PollSpec spec) {
    Iterator it = thePolls.values().iterator();
    while(it.hasNext()) {
      PollManagerEntry pme = (PollManagerEntry)it.next();
      if(pme.isSamePoll(type,spec)) {
        return !pme.isPollCompleted();
      }
    }
    return false;
  }

  /**
   * suspend a poll while we wait for a repair
   * @param key the identifier key of the poll to suspend
   */
  public void suspendPoll(String key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme == null) {
      theLog.debug2("ignoring suspend request for unknown key " + key);
      return;
    }
    pme.setPollSuspended();
    theLog.debug("suspended poll " + key);
  }


  /**
   * resume a poll that had been suspended for a repair and check the repair
   * @param replayNeeded true we now need to replay the poll results
   * @param key the key of the suspended poll
   */
  public void resumePoll(boolean replayNeeded, Object key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme == null) {
      theLog.debug2("ignoring resume request for unknown key " + key);
      return;
    }
    theLog.debug("resuming poll " + key);
    PollTally tally = pme.poll.getVoteTally();
    long expiration = 0;
    Deadline d;
    NodeManager nm = theDaemon.getNodeManager(tally.getArchivalUnit());
    nm.startPoll(tally.getCachedUrlSet(), tally, true);
    if (replayNeeded) {
      theLog.debug2("starting replay of poll " + key);
      expiration = m_maxContentPollDuration;
      d = Deadline.in(expiration);
      tally.startReplay(d);
    }
    else {
      pme.poll.stopPoll();
    }
    theLog.debug3("completed resume poll " + (String) key);
  }


  /**
   * handle an incoming message packet.  This will create a poll if
   * one is not already running. It will then call recieveMessage on
   * the poll.  This was moved from node state which kept track of the polls
   * running in the node.  This will need to be moved or amended to support this.
   * @param msg the message used to generate the poll
   * @throws IOException thrown if the poll was unsuccessfully created
   */
  void handleIncomingMessage(LcapMessage msg) throws IOException {
    theLog.info("Got a message: " + msg);
    if(isDuplicateMessage(msg)) {
      theLog.debug3("Dropping duplicate message:" + msg);
      return;
    }
    String key = msg.getKey();
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme != null) {
      if(pme.isPollCompleted() || pme.isPollSuspended()) {
        theLog.debug("Message received after poll was closed." + msg);
        return;
      }
    }
    BasePoll p = findPoll(msg);
    if (p != null) {
      p.receiveMessage(msg);
    }
  }

  /**
   * Find the poll defined by the <code>Message</code>.  If the poll
   * does not exist this will create a new poll
   * @param msg <code>Message</code>
   * @return <code>Poll</code> which matches the message opcode.
   * @throws IOException if message opcode is unknown or if new poll would
   * conflict with currently running poll.
   * @see <code>Poll.createPoll</code>
   */
  synchronized BasePoll findPoll(LcapMessage msg) throws IOException {
    String key = msg.getKey();
    BasePoll ret = null;

    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme == null) {
      theLog.debug3("Making new poll: " + key);
      ret = makePoll(msg);
      theLog.debug3("Done making new poll: "+ key);
    }
    else {
      theLog.debug3("Returning existing poll:" + key);
      ret = pme.poll;
    }
    return ret;
  }


  /**
   * make a new poll of the type and version defined by the incoming message.
   * @param msg <code>Message</code> to use for
   * @return a new Poll object of the required type
   * @throws ProtocolException if message opcode is unknown
   */
  BasePoll makePoll(LcapMessage msg) throws ProtocolException {
    BasePoll ret_poll;
    PollSpec spec = new PollSpec(msg);
    CachedUrlSet cus = spec.getCachedUrlSet();
    theLog.debug("making poll from: " + spec);

    // check for presence of item in the cache
    if(cus == null) {
      theLog.debug(spec.getUrl()+ " not in this cache, ignoring poll request.");
      return null;
    }

    // check for conflicts
    CachedUrlSet conflict = checkForConflicts(msg, cus);
    if(conflict != null) {
      String err = "New poll " + cus + " conflicts with " + conflict +
          ", ignoring poll request.";
      theLog.debug(err);
      return null;
    }
    if (msg.isVerifyPoll()) {
      // if we didn't call the poll and we don't have the verifier ignore this
      if((getSecret(msg.getChallenge())== null) &&
        !theIDManager.isLocalIdentity(msg.getOriginAddr())) {
       String ver = String.valueOf(B64Code.encode(msg.getChallenge()));
       theLog.debug("ignoring verify request from " + msg.getOriginAddr()
        + " on unknown verifier " + ver);
       return null;
     }
    }
    // check with regulator if not verify poll
    else {
      // get expiration time for the lock
      long expiration = 2 * msg.getDuration();
      if (AuUrl.isAuUrl(cus.getUrl())) {
        ActivityRegulator.Lock lock = theDaemon.getActivityRegulator(
            cus.getArchivalUnit()).startAuActivity(
            ActivityRegulator.TOP_LEVEL_POLL, expiration);
        if (lock==null) {
          theLog.debug2("New top-level poll aborted due to activity lock.");
          return null;
        }
      }
      else {
        int activity;
        if (cus.getSpec().isSingleNode()) {
          activity = (msg.isContentPoll() ?
                          ActivityRegulator.SINGLE_NODE_CONTENT_POLL :
                          ActivityRegulator.STANDARD_NAME_POLL);
        } else {
          activity = (msg.isContentPoll() ?
                          ActivityRegulator.STANDARD_CONTENT_POLL :
                          ActivityRegulator.STANDARD_NAME_POLL);
        }
        ActivityRegulator.Lock lock = theDaemon.getActivityRegulator(
            cus.getArchivalUnit()).startCusActivity(activity, cus, expiration);
        if (lock==null) {
          theLog.debug2("New poll aborted due to activity lock.");
          return null;
        }
      }
    }

    // create the appropriate poll for the message type
    ret_poll = createPoll(msg, spec);

    if (ret_poll != null) {
      NodeManager nm = theDaemon.getNodeManager(cus.getArchivalUnit());
      if (!msg.isVerifyPoll()) {
        if (!nm.shouldStartPoll(cus, ret_poll.getVoteTally())) {
	  theLog.debug("NodeManager said not to start poll: "+ret_poll);
          freePollLock(cus, msg.isContentPoll());
          return null;
        }
      }

      thePolls.put(ret_poll.m_key, new PollManagerEntry(ret_poll));
      if (!msg.isVerifyPoll()) {
        nm.startPoll(cus, ret_poll.getVoteTally(), false);
      }
      ret_poll.startPoll();
      theLog.debug2("Started new poll: " + ret_poll.m_key);
      return ret_poll;
    } else {
      theLog.error("Got a null ret_poll from createPoll");
      freePollLock(cus, msg.isContentPoll());
      return null;
    }
  }

  /**
   * Frees the {@link ActivityRegulator} lock from this poll.
   * @param cus the CachedUrlSet being polled
   * @param isContentPoll true iff a content poll
   */
  private void freePollLock(CachedUrlSet cus, boolean isContentPoll) {
    if (AuUrl.isAuUrl(cus.getUrl())) {
      theDaemon.getActivityRegulator(cus.getArchivalUnit()).auActivityFinished(
          ActivityRegulator.TOP_LEVEL_POLL);
    } else {
      int activity;
      if (cus.getSpec().isSingleNode()) {
        activity = (isContentPoll ?
                    ActivityRegulator.SINGLE_NODE_CONTENT_POLL :
                    ActivityRegulator.STANDARD_NAME_POLL);
      } else {
        activity = (isContentPoll ?
                    ActivityRegulator.STANDARD_CONTENT_POLL :
                    ActivityRegulator.STANDARD_NAME_POLL);
      }
      theDaemon.getActivityRegulator(cus.getArchivalUnit()).cusActivityFinished(activity, cus);
    }
  }

  /**
   * close the poll from any further voting
   * @param key the poll signature
   */
  void closeThePoll(String key)  {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme== null || pme.poll == null) {
      theLog.warning("Attempt to close unknown poll : " + key);
      return;
    }
    // mark the poll completed because if we need to call a repair poll
    // we don't want this one to be in conflict with it.
    PollTally tally = pme.poll.getVoteTally();
    pme.setPollCompleted();
    if(tally.getType() != Poll.VERIFY_POLL) {
      NodeManager nm = theDaemon.getNodeManager(tally.getArchivalUnit());
      theLog.debug("sending completed poll results " + tally);
      nm.updatePollResults(tally.getCachedUrlSet(), tally);
      try {
        theIDManager.storeIdentities();
      }
      catch (ProtocolException ex) {
        theLog.error("Unable to write Identity DB file.");
      }
      // free the activity regulator
      freePollLock(tally.getCachedUrlSet(),
                   tally.getType()==Poll.CONTENT_POLL);
    }
  }

  /**
   * create a poll of the type indicated by the LcapMessage opcode.
   * @param msg the message which triggered this poll
   * @param pollspec the PollSpec which describes the poll
   * @return a newly created Poll object
   * @throws ProtocolException if the opcode in the message is of an unknown
   * type.
   */
  protected BasePoll createPoll(LcapMessage msg, PollSpec pollspec)
      throws ProtocolException {
    BasePoll ret_poll = null;

    switch(msg.getOpcode()) {
      case LcapMessage.CONTENT_POLL_REP:
      case LcapMessage.CONTENT_POLL_REQ:
        theLog.debug3("Making a content poll on "+ pollspec);
	switch (pollspec.getVersion()) {
	case 1:
	  ret_poll = new V1ContentPoll(msg, pollspec, this);
	  break;
	case 2:
	  ret_poll = new V2ContentPoll(msg, pollspec, this);
	  break;
	default:
	  throw new ProtocolException("Unsupported content poll version: " +
				      pollspec.getVersion());
	}
        break;
      case LcapMessage.NAME_POLL_REP:
      case LcapMessage.NAME_POLL_REQ:
        theLog.debug3("Making a name poll on "+pollspec);
	switch (pollspec.getVersion()) {
	case 1:
	  ret_poll = new V1NamePoll(msg, pollspec, this);
	  break;
	case 2:
	  ret_poll = new V2NamePoll(msg, pollspec, this);
	  break;
	default:
	  throw new ProtocolException("Unsupported name poll version: " +
				      pollspec.getVersion());
	}
        break;
      case LcapMessage.VERIFY_POLL_REP:
      case LcapMessage.VERIFY_POLL_REQ:
        theLog.debug3("Making a verify poll on "+pollspec);
	switch (pollspec.getVersion()) {
	case 1:
	  ret_poll = new V1VerifyPoll(msg, pollspec, this);
	  break;
	default:
	  throw new ProtocolException("Unsupported verify poll version: " +
				      pollspec.getVersion());
	}
        break;
      default:
        throw new ProtocolException("Unknown opcode:" + msg.getOpcode());
    }
    return ret_poll;
  }

  /**
   * check for conflicts between the poll defined by the Message and any
   * currently existing poll.
   * @param msg the <code>Message</code> to check
   * @param cus the <code>CachedUrlSet</code> from the url and reg expression
   * @return the CachedUrlSet of the conflicting poll.
   */
  CachedUrlSet checkForConflicts(LcapMessage msg, CachedUrlSet cus) {

    // eliminate incoming verify polls - never conflicts
    if(msg.isVerifyPoll()) {
      return null;
    }

    Iterator iter = thePolls.values().iterator();
    while(iter.hasNext()) {
      PollManagerEntry entry = (PollManagerEntry)iter.next();
      BasePoll p = entry.poll;

      // eliminate completed polls or verify polls
      if(!entry.isPollCompleted() && !p.getMessage().isVerifyPoll()) {
        CachedUrlSet pcus = p.getPollSpec().getCachedUrlSet();
        ArchivalUnit au = cus.getArchivalUnit();
        LockssRepository repo = theDaemon.getLockssRepository(au);
        int rel_pos = repo.cusCompare(cus, pcus);
        if(rel_pos != LockssRepository.SAME_LEVEL_NO_OVERLAP &&
           rel_pos != LockssRepository.NO_RELATION) {
          theLog.debug2("Conflict between new poll '"+cus+"' and running poll '"+
                        pcus+"'");
          return pcus;
        }
      }
    }
    return null;
  }

  /**
   * send a message to the multicast address for this archival unit
   * @param msg the LcapMessage to send
   * @param au the ArchivalUnit for this message
   * @throws IOException
   */
  void sendMessage(LcapMessage msg, ArchivalUnit au) throws IOException {
    if(theRouter != null) {
      theRouter.send(msg, au);
    }
  }

  /**
   * send a message to the unicast address given by an identity
   * @param msg the LcapMessage to send
   * @param au the ArchivalUnit for this message
   * @param id the LcapIdentity of the identity to send to
   * @throws IOException
   */
  void sendMessageTo(LcapMessage msg, ArchivalUnit au, LcapIdentity id)
      throws IOException {
    theRouter.sendTo(msg, au, id);
  }



  /**
   * request a verify poll by creating a new LcapMessage and sending it.
   * @param pollspec the PollSpec describing the request poll
   * @param duration the duration of the poll
   * @param vote to be verifed
   * @throws IOException if messge creation fails or the message cannot be sent.
   */
  void requestVerifyPoll(PollSpec pollspec, long duration, Vote vote)
      throws IOException {

    theLog.debug("Calling a verify poll...");
    CachedUrlSet cus = pollspec.getCachedUrlSet();
    LcapMessage reqmsg = LcapMessage.makeRequestMsg(pollspec,
        null,
        vote.getVerifier(),         // the challenge  becomes the verifier
        makeVerifier(duration),     // we get a new verifier for this poll
        LcapMessage.VERIFY_POLL_REQ,
        duration,
        theIDManager.getLocalIdentity());

    LcapIdentity originator =  theIDManager.findIdentity(vote.getIDAddress());
    theLog.debug2("sending our verification request to " + originator.toString());
    sendMessageTo(reqmsg, cus.getArchivalUnit(), originator);
    // since we won't be getting this message make sure we create our own poll
    BasePoll poll = makePoll(reqmsg);
  }


  /**
   * Called by verify polls to get the array of bytes that represents the
   * secret used to generate the verifier bytes.
   * @param verifier the array of bytes that is a hash of the secret.
   * @return the array of bytes representing the secret or if no matching
   * verifier is found, null.
   */
  byte[] getSecret(byte[] verifier) {
    String ver = String.valueOf(B64Code.encode(verifier));
    String sec = (String)theVerifiers.get(ver);
    if (sec != null && sec.length() > 0) {
      return (B64Code.decode(sec.toCharArray()));
    }
    return null;
  }



  /**
   * return a MessageDigest hasher for needed to hash this message
   * @param msg the LcapMessage which needs to be hashed or null to used
   * the default hasher
   * @return MessageDigest the hasher
   */
  MessageDigest getHasher(LcapMessage msg) {
    MessageDigest hasher = null;
    String algorithm;
    if(msg == null) {
      algorithm = LcapMessage.getDefaultHashAlgorithm();
    }
    else {
      algorithm = msg.getHashAlgorithm();
    }
    try {
      hasher = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException ex) {
      theLog.error("Unable to run - no hasher");
    }

    return hasher;
  }


  /**
   * make a verifier by generating a secret and hashing it. Then store the
   * verifier/secret pair in the verifiers table.
   * @param duration time the item we're verifying is expected to take.
   * @return the array of bytes representing the verifier
   */
  byte[] makeVerifier(long duration) {
    byte[] s_bytes = generateRandomBytes();
    byte[] v_bytes = generateVerifier(s_bytes);
    if(v_bytes != null) {
      rememberVerifier(v_bytes, s_bytes, duration);
    }
    return v_bytes;
  }



  /**
   * generate a random array of 20 bytes
   * @return the array of bytes
   */
  public byte[] generateRandomBytes() {
    byte[] secret = new byte[20];
    theRandom.nextBytes(secret);
    return secret;
  }


  /**
   * generate a verifier from a array of bytes representing a secret
   * @param secret the bytes representing a secret to be hashed
   * @return an array of bytes representing a verifier
   */
  byte[] generateVerifier(byte[] secret) {
    byte[] verifier = null;
    MessageDigest hasher = getHasher(null);
    hasher.update(secret, 0, secret.length);
    verifier = hasher.digest();

    return verifier;
  }

  IdentityManager getIdentityManager() {
    return theIDManager;
  }

  HashService getHashService() {
    return theHashService;
  }

  int getQuorum() {
    return m_quorum;
  }

  private void rememberVerifier(byte[] verifier,
                                byte[] secret,
                                long duration) {
    String ver = String.valueOf(B64Code.encode(verifier));
    String sec = secret == null ? "" : String.valueOf(B64Code.encode(secret));
    Deadline d = Deadline.in(m_verifierExpireTime + duration);
    TimerQueue.schedule(d, new ExpireVerifierCallback(), ver);
    synchronized (theVerifiers) {
      theVerifiers.put(ver, sec);
    }
  }

  private boolean isDuplicateMessage(LcapMessage msg) {
    byte[] verifier = msg.getVerifier();
    String ver = String.valueOf(B64Code.encode(verifier));
    synchronized (theVerifiers) {
      String secret = (String)theVerifiers.get(ver);
      // if we have a secret and we don't have a poll
      // we made the original message but haven't made a poll yet
      if(!StringUtil.isNullString(secret) &&
         !thePolls.contains(msg.getKey())) {
        return false;
      }
      else if(secret == null) { // we didn't make the verifier-we don't have a secret
        rememberVerifier(verifier,null, msg.getDuration());
      }
      return secret != null;   // we made the verifier-we should have a secret
    }
  }


  protected void setConfig(Configuration newConfig,
                           Configuration oldConfig,
                           Set changedKeys) {
    long aveDuration = newConfig.getTimeInterval(PARAM_NAMEPOLL_DEADLINE,
                                                  DEFAULT_NAMEPOLL_DEADLINE);
    m_minNamePollDuration = aveDuration - aveDuration / 4;
    m_maxNamePollDuration = aveDuration + aveDuration / 4;

    m_minContentPollDuration = newConfig.getTimeInterval(PARAM_CONTENTPOLL_MIN,
        DEFAULT_CONTENTPOLL_MIN);
    m_maxContentPollDuration = newConfig.getTimeInterval(PARAM_CONTENTPOLL_MAX,
        DEFAULT_CONTENTPOLL_MAX);

    m_quorum = newConfig.getIntParam(PARAM_QUORUM, DEFAULT_QUORUM);

    m_durationMultiplier = newConfig.getIntParam(PARAM_DURATION_MULTIPLIER,
                                                 DEFAULT_DURATION_MULTIPLIER);
    m_recentPollExpireTime = newConfig.getTimeInterval(PARAM_RECENT_EXPIRATION,
        DEFAULT_RECENT_EXPIRATION);
    m_verifierExpireTime = newConfig.getTimeInterval(PARAM_VERIFY_EXPIRATION,
                                        DEFAULT_VERIFY_EXPIRATION);
  }

//--------------- PollerStatus Accessors -----------------------------
  Iterator getPolls() {
    Map polls = Collections.unmodifiableMap(thePolls);
    return polls.values().iterator();
  }

  BasePoll getPoll(String key) {
    BasePoll poll = null;

    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme != null) {
      poll =  pme.poll;
    }
    return poll;
  }

//-------------- TestPollManager Accessors ----------------------------
/**
 * remove the poll represented by the given key from the poll table and
 * return it.
 * @param key the String representation of the polls key
 * @return Poll the poll if found or null
 */
  BasePoll removePoll(String key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.remove(key);
    return (pme != null) ? pme.poll : null;
  }

  void addPoll(BasePoll p) {
    thePolls.put(p.m_key, new PollManagerEntry(p));
  }

  boolean isPollActive(String key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    return (pme != null) ? pme.isPollActive() : false;
  }

  boolean isPollClosed(String key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    return (pme != null) ? pme.isPollCompleted() : false;
  }

  boolean isPollSuspended(String key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    return (pme != null) ? pme.isPollSuspended() : false;
  }

  static BasePoll makeTestPoll(LcapMessage msg) throws ProtocolException {
    if(theManager == null) {
      theManager = new PollManager();
    }
    return theManager.makePoll(msg);
  }

  static long getSlowestHashSpeed() {
    return theSystemMetrics.getSlowestHashSpeed();
  }

  static long getBytesPerMsHashEstimate()
  throws SystemMetrics.NoHashEstimateAvailableException {
    return theSystemMetrics.getBytesPerMsHashEstimate();
  }

  static long getAvailableHashTimeBefore(Deadline when) {
    return theHashService.getAvailableHashTimeBefore(when);
  }

// ----------------  Callbacks -----------------------------------

  class RouterMessageHandler implements LcapRouter.MessageHandler {
    public void handleMessage(LcapMessage msg) {
      theLog.debug3("received from router message:" + msg.toString());
      try {
	handleIncomingMessage(msg);
      }
      catch (IOException ex) {
	theLog.error("handle incoming message failed.");
      }
    }
  }

  static class ExpireRecentCallback implements TimerQueue.Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      synchronized(thePolls) {
        PollManagerEntry pme = (PollManagerEntry)thePolls.get(cookie);
        if(pme != null  && pme.isPollSuspended()) {
          return;
        }
        thePolls.remove(cookie);
      }
    }
  }

  static class ExpireVerifierCallback implements TimerQueue.Callback {
    /**
     * Called when the timer expires.
     * @param cookie  data supplied by caller to schedule()
     */
    public void timerExpired(Object cookie) {
      synchronized(theVerifiers) {
        theVerifiers.remove(cookie);
      }
    }
  }


  /**
   * <p>PollManagerEntry: </p>
   * <p>Description: Class to represent the data store in the polls table.
   * @version 1.0
   */

  static class PollManagerEntry {
    BasePoll poll;
    PollSpec spec;
    int type;
    Deadline pollDeadline;
    Deadline deadline;
    String key;

    PollManagerEntry(BasePoll p) {
      poll = p;
      spec = p.getPollSpec();
      type = p.getVoteTally().getType();
      key = p.getKey();
      pollDeadline = p.getDeadline();
      deadline = null;
    }

    boolean isPollActive() {
      return poll.getVoteTally().stateIsActive();
    }

    boolean isPollCompleted() {
      return poll.getVoteTally().stateIsFinished();
    }

    boolean isPollSuspended() {
      return poll.getVoteTally().stateIsSuspended();
    }

    synchronized void setPollCompleted() {
      Deadline d = Deadline.in(m_recentPollExpireTime);
      TimerQueue.schedule(d, new ExpireRecentCallback(), (String) key);
      PollTally tally = poll.getVoteTally();
      int version = tally.getPollSpec().getVersion();
      tally.tallyVotes();
    }

    synchronized void setPollSuspended() {
      poll.getVoteTally().setStateSuspended();
      if(deadline != null) {
        deadline.expire();
        deadline = null;
      }
    }

    String getStatusString() {
      if (isPollCompleted()) {
        return poll.getVoteTally().getStatusString();
      }
      else if(isPollActive()) {
        return "Active";
      }
      else if(isPollSuspended()) {
        return "Repairing";
      }
      return "Unknown";
    }

    String getTypeString() {
      return Poll.PollName[type];
    }

    String getShortKey() {
      return(key.substring(0,10));
    }

    boolean isSamePoll(int type, PollSpec spec) {
      if(this.type == type) {
        return this.spec.getCachedUrlSet().equals(spec.getCachedUrlSet());
      }
      return false;
    }
  }
}
