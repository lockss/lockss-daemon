/*
 * $Id: PollManager.java,v 1.151 2005-02-02 09:42:26 tlipkis Exp $
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
 * <p>Class that manages the polling process.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class PollManager
  extends BaseLockssDaemonManager implements ConfigurableManager {

  protected static Logger theLog = Logger.getLogger("PollManager");

  static final String PREFIX = Configuration.PREFIX + "poll.";
  static final String PARAM_RECENT_EXPIRATION = PREFIX + "expireRecent";
  static final String PARAM_VERIFY_EXPIRATION = PREFIX + "expireVerifier";

  static final long DEFAULT_RECENT_EXPIRATION = Constants.DAY;
  static final long DEFAULT_VERIFY_EXPIRATION = 6 * Constants.HOUR;

  // Items are moved between thePolls and theRecentPolls, so it's simplest
  // to synchronize all accesses on a single object.  That object is
  // currently thePolls, which is itself synchronized.

  private static Hashtable thePolls = new Hashtable();

  // all accesses must be synchronized on pollMapLock
  private static FixedTimedMap theRecentPolls =
    new FixedTimedMap(DEFAULT_RECENT_EXPIRATION);

  private static Object pollMapLock = thePolls;

  private static VariableTimedMap theVerifiers = new VariableTimedMap();


  private static LockssRandom theRandom = new LockssRandom();

  private static PollManager theManager = null;
  private static LcapDatagramRouter.MessageHandler  m_msgHandler;
  private static IdentityManager theIDManager;
  private static HashService theHashService;
  private static LcapDatagramRouter theRouter = null;
  private AlertManager theAlertManager = null;
  private static SystemMetrics theSystemMetrics = null;

  // our configuration variables
  protected long m_recentPollExpireTime = DEFAULT_RECENT_EXPIRATION;
  protected long m_verifierExpireTime = DEFAULT_VERIFY_EXPIRATION;

  // The PollFactory instances
  PollFactory [] pf = {
    null,
    new V1PollFactory(),
    null, // new V2PollFactory(),
    null, // new V3PollFactory(),
  };

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
    theAlertManager = theDaemon.getAlertManager();

    // register a message handler with the router
    theRouter = theDaemon.getDatagramRouterManager();
    m_msgHandler =  new RouterMessageHandler();
    theRouter.registerMessageHandler(m_msgHandler);

    // get System Metrics
    theSystemMetrics = theDaemon.getSystemMetrics();
    // register our status
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.registerStatusAccessor(PollerStatus.MANAGER_STATUS_TABLE_NAME,
                                      new PollerStatus.ManagerStatus(this));
    statusServ.registerStatusAccessor(PollerStatus.POLL_STATUS_TABLE_NAME,
                                      new PollerStatus.PollStatus(this));
    statusServ.registerObjectReferenceAccessor(PollerStatus.MANAGER_STATUS_TABLE_NAME,
					       ArchivalUnit.class,
					       new PollerStatus.ManagerStatusAuRef(this));
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
    theRecentPolls.clear();
    super.stopService();
  }


  /** Cancel all polls on the specified AU.
   * @param au the AU
   */
  public void cancelAuPolls(ArchivalUnit au) {
    // first collect polls to cancel
    Set toCancel = new HashSet();
    synchronized (pollMapLock) {
      for (Iterator it = thePolls.values().iterator(); it.hasNext(); ) {
	PollManagerEntry pme = (PollManagerEntry) it.next();
	ArchivalUnit pau = pme.poll.m_cus.getArchivalUnit();
	if (pau == au && !pme.isPollCompleted()) {
	  toCancel.add(pme);
	}
      }
    }
    // then actually cancel them while not holding lock
    for (Iterator it = toCancel.iterator(); it.hasNext(); ) {
      PollManagerEntry pme = (PollManagerEntry) it.next();
      ArchivalUnit pau = pme.poll.m_cus.getArchivalUnit();
      theHashService.cancelAuHashes(pau);
      pme.poll.stopPoll();
    }
  }

  /**
   * Call a poll.  Only used by the tree walk.
   * @param pollspec the <code>PollSpec</code> that defines the subject of
   *                 the <code>Poll</code>.
   * @return the poll, if it was successfuly called, else null.
   */
  public Poll callPoll(PollSpec pollspec) {
    BasePoll thePoll = null;
    PollFactory pollFact = getPollFactory(pollspec);
    if (pollFact == null) {
      return null;
    } else {
      long duration = pollFact.calcDuration(pollspec, this);
      if (duration <= 0) {
	theLog.debug("Duration for " + pollspec + " too short " + duration);
	return null;
      }
      byte[] challenge = makeVerifier(duration);
      byte[] verifier = makeVerifier(duration);
      try {
	thePoll = makePoll(pollspec, duration, challenge, verifier,
			   theIDManager.getLocalPeerIdentity(pollspec.getPollVersion()),
			   LcapMessage.getDefaultHashAlgorithm());
	if (thePoll != null) {
	  if (pollFact.callPoll(thePoll, this, theIDManager)) {
	    return thePoll;
	  } else {
	    theLog.debug("pollFact.callPoll() returned false");
	  }
	} else {
	  theLog.debug("makePoll(" + pollspec + ") returned null");
	}
      } catch (ProtocolException ex) {
	theLog.debug("Error in makePoll or callPoll", ex);
      }
    }
    return null;
  }

   /**
   * Is a poll of the given type and spec currently running
   * @param type the type of the poll.
   * @param spec the PollSpec definining the location of the poll.
   * @return true if we have a poll which is running that matches pollspec
   */
  public boolean isPollRunning(int type, PollSpec spec) {
    synchronized (pollMapLock) {
      for (Iterator it = thePolls.values().iterator(); it.hasNext(); ) {
	PollManagerEntry pme = (PollManagerEntry)it.next();
	if(pme.isSamePoll(type,spec)) {
	  return !pme.isPollCompleted();
	}
      }
    }
    return false;
  }

  /** Return the PollManagerEntry for the poll with the specified key. */
  public PollManagerEntry getPollManagerEntry(String key) {
    return (PollManagerEntry)thePolls.get(key);
  }

  /** Find the poll either in current or recent polls */
  PollManagerEntry getCurrentOrRecentPollEntry(String key) {
    synchronized (pollMapLock) {
      PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
      if(pme == null) {
	pme = (PollManagerEntry)theRecentPolls.get(key);
      }
      return pme;
    }
  }

  public ActivityRegulator.Lock acquirePollLock(String key) {
    ActivityRegulator.Lock lock = null;
    PollManagerEntry pme = getCurrentOrRecentPollEntry(key);
    if(pme != null) {
      PollTally tally = pme.poll.getVoteTally();
      if(tally != null) {
        lock = tally.getActivityLock();
        tally.setActivityLock(null);
      }
    }
    return lock;
  }

  /**
   * suspend a poll while we wait for a repair
   * @param key the identifier key of the poll to suspend
   */
  public void suspendPoll(String key) {
    PollManagerEntry pme;
    synchronized (pollMapLock) {
      pme = getCurrentOrRecentPollEntry(key);
      if (pme != null) {
	theRecentPolls.remove(key);
	thePolls.put(key, pme);
	pme.setPollSuspended();
      }
    }
    if (pme == null) {
      theLog.debug2("ignoring suspend request for unknown key " + key);
    } else {
      theLog.debug("suspended poll " + key);
    }
  }


  /**
   * resume a poll that had been suspended for a repair and check the repair
   * @param replayNeeded true we now need to replay the poll results
   * @param key the key of the suspended poll
   */
  public void resumePoll(boolean replayNeeded,
                         Object key,
                         ActivityRegulator.Lock lock) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme == null) {
      theLog.debug2("ignoring resume request for unknown key " + key);
      return;
    }
    theLog.debug("resuming poll " + key);
    PollTally tally = pme.poll.getVoteTally();
    tally.setActivityLock(lock);
    long expiration = 0;
    Deadline d;
    NodeManager nm = theDaemon.getNodeManager(tally.getArchivalUnit());
    nm.startPoll(tally.getCachedUrlSet(), tally, true);
    if (replayNeeded) {
      theLog.debug2("starting replay of poll " + key);
      PollFactory pollFact = getPollFactory(pme.poll.getVersion());
      // should be equivalent to this.  is it?
//       PollFactory pollFact = getPollFactory(pme.spec);
      if (pollFact != null) {
	expiration = pollFact.getMaxPollDuration(Poll.CONTENT_POLL);
      } else {
	expiration = 0; // XXX
      }
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
    if (theLog.isDebug2()) theLog.debug2("Got a message: " + msg);
    if(isDuplicateMessage(msg)) {
      theLog.debug3("Dropping duplicate message:" + msg);
      return;
    }
    String key = msg.getKey();
    PollManagerEntry pme = getCurrentOrRecentPollEntry(key);
    if(pme != null) {
      if(pme.isPollCompleted() || pme.isPollSuspended()) {
        theLog.debug("Message received after poll was closed." + msg);
        return;
      }
    }
    BasePoll p = findPoll(msg);
    if (p != null) {
      p.setMessage(msg);
      p.receiveMessage(msg);
    }
  }

  /**
   * Find the poll defined by the <code>Message</code>.  If the poll
   * does not exist this will create a new poll (iff there are no conflicts)
   * @param msg <code>Message</code>
   * @return <code>Poll</code> which matches the message opcode, or a new
   * poll, or null if the new poll would conflict with a currently running poll.
   * @throws IOException if message opcode is unknown.
   * @see <code>Poll.createPoll</code>
   */
  synchronized BasePoll findPoll(LcapMessage msg) throws IOException {
    String key = msg.getKey();
    BasePoll ret = null;

    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme == null) {
      theLog.debug3("Making new poll: " + key);
      ret = makePoll(msg);
      if (theLog.isDebug3()) {
	if (ret != null) {
	  theLog.debug3("Made new poll: " + key);
	} else {
	  theLog.debug3("Did not make new poll: " + key);
	}
      }
    }
    else {
      theLog.debug3("Returning existing poll: " + key);
      ret = pme.poll;
    }
    return ret;
  }

  /**
   * make a new poll of the type and version defined by the incoming message.
   * @param msg <code>Message</code> to use for
   * @return a new Poll object of the required type, or null if we don't
   * want to run this poll now (<i>ie</i>, due to a conflict with another
   * poll).
   * @throws ProtocolException if message opcode is unknown
   */
  BasePoll makePoll(LcapMessage msg) throws ProtocolException {
    BasePoll ret_poll = null;
    PollSpec spec = new PollSpec(msg);
    long duration = msg.getDuration();
    byte[] challenge = msg.getChallenge();
    byte[] verifier = msg.getVerifier();
    PeerIdentity orig = msg.getOriginatorID();
    String hashAlg = msg.getHashAlgorithm();

    ret_poll = makePoll(spec, duration, challenge, verifier, orig, hashAlg);
    if (ret_poll != null) {
      ret_poll.setMessage(msg);
    }
    return ret_poll;
  }

  BasePoll makePoll(PollSpec spec,
		    long duration,
		    byte[] challenge,
		    byte[] verifier,
		    PeerIdentity orig,
		    String hashAlg) throws ProtocolException {
    BasePoll ret_poll = null;
    CachedUrlSet cus = spec.getCachedUrlSet();
    // check for presence of item in the cache
    if (cus == null) {
      theLog.debug("Ignoring poll request, don't have AU: " + spec.getAuId());
      return null;
    }
    ArchivalUnit au = cus.getArchivalUnit();
    if (!spec.getPluginVersion().equals(au.getPlugin().getVersion())) {
      theLog.debug("Ignoring poll request for " + au.getName() +
		   ", plugin version mismatch; have: " +
		   au.getPlugin().getVersion() +
		   ", need: " + spec.getPluginVersion());
      return null;
    }
    theLog.debug("Making poll from: " + spec);
    ActivityRegulator.Lock lock = null;
    PollFactory pollFact = getPollFactory(spec);
    if (pollFact == null) {
      return null;
    }

    // check for conflicts
    if (!pollFact.shouldPollBeCreated(spec, this, theIDManager,
					     challenge, orig)) {
      theLog.debug("Poll request ignored");
      return null;
    }
    // check with regulator if not verify poll
    else if (spec.getPollType() != Poll.VERIFY_POLL) {
      // get expiration time for the lock
      long expiration = 2 * duration;
      if (AuUrl.isAuUrl(cus.getUrl())) {
        lock = theDaemon.getActivityRegulator(au).
	  getAuActivityLock(ActivityRegulator.TOP_LEVEL_POLL, expiration);
        if (lock==null) {
          theLog.debug2("New top-level poll aborted due to activity lock.");
          return null;
        }
      } else {
        int activity = pollFact.getPollActivity(spec, this);
	ActivityRegulator ar = theDaemon.getActivityRegulator(au);
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

    // create the appropriate poll for the message type
    try {
      ret_poll = pollFact.createPoll(spec, this, theIDManager,
					    orig, challenge, verifier,
					    duration, hashAlg);
    }
    catch (Exception ex) {
      if(ex instanceof ProtocolException) {
        throw (ProtocolException) ex;
      }
      else {
        theLog.error("Failed to create poll:" + ex);
        if (lock!=null) {
          // clear the lock
          lock.expire();
        }
        return null;
      }
    }

    if (ret_poll != null) {
      NodeManager nm = theDaemon.getNodeManager(cus.getArchivalUnit());
      if (spec.getPollType() != Poll.VERIFY_POLL) {
        if (!nm.shouldStartPoll(cus, ret_poll.getVoteTally())) {
	  theLog.debug("NodeManager said not to start poll: "+ret_poll);
          // clear the lock
          lock.expire();
          return null;
        }
      }

      thePolls.put(ret_poll.m_key, new PollManagerEntry(ret_poll));
      if (spec.getPollType() != Poll.VERIFY_POLL &&
	  !(spec.getPollType() == Poll.NAME_POLL &&
	    spec.getLwrBound() != null)) {
        // set the activity lock in the tally
        ret_poll.getVoteTally().setActivityLock(lock);
        nm.startPoll(cus, ret_poll.getVoteTally(), false);
      }

      ret_poll.startPoll();
      theLog.debug2("Started new poll: " + ret_poll.m_key);
      return ret_poll;
    } else {
      theLog.error("Got a null ret_poll from createPoll");
      if (lock!=null) {
        // clear the lock
        lock.expire();
      }
      return null;
    }
  }

  /**
   * close the poll from any further voting
   * @param key the poll signature
   */
  void closeThePoll(String key)  {
    PollManagerEntry pme = (PollManagerEntry)thePolls.remove(key);
    if(pme== null || pme.poll == null) {
      theLog.warning("Attempt to close unknown poll : " + key);
      return;
    }
    // mark the poll completed because if we need to call a repair poll
    // we don't want this one to be in conflict with it.
    PollTally tally = pme.poll.getVoteTally();
    pme.setPollCompleted();
    synchronized (pollMapLock) {
      theRecentPolls.put(key, pme);
    }
    // Don't tell the node manager about verify polls
    // If closing a name poll that started ranged subpolls, don't tell
    // the node manager about it until all ranged subpolls have finished
    if (tally.getType() != Poll.VERIFY_POLL && !pme.poll.isSubpollRunning()) {
      // if closing last name poll, concatenate all the name lists into the
      // first tally and pass that to node manager
      if (tally.getType() == Poll.NAME_POLL && tally instanceof V1PollTally) {
	V1PollTally lastTally = (V1PollTally)tally;
	tally = lastTally.concatenateNameSubPollLists();
      }
      NodeManager nm = theDaemon.getNodeManager(tally.getArchivalUnit());
      theLog.debug("handing poll results to node manager: " + tally);
      nm.updatePollResults(tally.getCachedUrlSet(), tally);
      try {
        theIDManager.storeIdentities();
      } catch (ProtocolException ex) {
        theLog.error("Unable to write Identity DB file.");
      }
      // free the activity lock
      ActivityRegulator.Lock lock = tally.getActivityLock();
      if(lock != null) {
        lock.expire();
      }
    }
  }

  /**
   * getActivePollSpecIterator returns an Iterator over the set of
   * PollSpec instances which currently have active polls on the given au.
   * @return Iterator over set of PollSpec
   */
  protected Iterator getActivePollSpecIterator(ArchivalUnit au,
					       BasePoll dontIncludePoll) {
    Set pollspecs = new HashSet();
    synchronized (pollMapLock) {
      for (Iterator iter = thePolls.values().iterator(); iter.hasNext(); ) {
	PollManagerEntry pme = (PollManagerEntry)iter.next();
	ArchivalUnit pau = pme.poll.m_cus.getArchivalUnit();
	if (pau == au &&
	    pme.poll != dontIncludePoll &&
	    !pme.isPollCompleted()) {
	  pollspecs.add(pme.poll.getPollSpec());
	}
      }
    }
    return (pollspecs.iterator());
  }

  void raiseAlert(Alert alert) {
    theAlertManager.raiseAlert(alert);
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
   * @param id the PeerIdentity of the identity to send to
   * @throws IOException
   */
  void sendMessageTo(LcapMessage msg, ArchivalUnit au, PeerIdentity id)
      throws IOException {
    theRouter.sendTo(msg, au, id);
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

  private boolean isDuplicateMessage(LcapMessage msg) {
    byte[] verifier = msg.getVerifier();
    String ver = String.valueOf(B64Code.encode(verifier));
    // lock paranoia - access (synchronized) thePolls outside theVerifiers lock
    boolean havePoll = thePolls.contains(msg.getKey());

    synchronized (theVerifiers) {
      String secret = (String)theVerifiers.get(ver);
      // if we have a secret and we don't have a poll
      // we made the original message but haven't made a poll yet
      if(!StringUtil.isNullString(secret) && !havePoll) {
        return false;
      }
      else if(secret == null) { // we didn't make the verifier-we don't have a secret
        rememberVerifier(verifier,null, msg.getDuration());
      }
      return secret != null;   // we made the verifier-we should have a secret
    }
  }


  public void setConfig(Configuration newConfig,
			Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      m_recentPollExpireTime =
	newConfig.getTimeInterval(PARAM_RECENT_EXPIRATION,
				  DEFAULT_RECENT_EXPIRATION);
      theRecentPolls.setInterval(m_recentPollExpireTime);
      m_verifierExpireTime =
	newConfig.getTimeInterval(PARAM_VERIFY_EXPIRATION,
				  DEFAULT_VERIFY_EXPIRATION);
    }
    for (int i = 0; i < pf.length; i++) {
      if (pf[i] != null) {
	pf[i].setConfig(newConfig, oldConfig, changedKeys);
      }
    }
  }

  public PollFactory getPollFactory(PollSpec spec) {
    int version = spec.getPollVersion();
    if (version > 0 || version <= pf.length) {
      return pf[version];
    }
    theLog.error("Unknown poll version: " + spec);
    return null;
  }

  public PollFactory getPollFactory(int version) {
    if (version > 0 || version <= pf.length) {
      return pf[version];
    }
    theLog.error("Unknown poll version: " + version, new Throwable());
    return null;
  }


//--------------- PollerStatus Accessors -----------------------------
  Iterator getPolls() {
    Map polls = new HashMap();
    synchronized (pollMapLock) {
      polls.putAll(thePolls);
      polls.putAll(theRecentPolls);
    }
    return polls.values().iterator();
  }

  BasePoll getPoll(String key) {
    PollManagerEntry pme = getCurrentOrRecentPollEntry(key);
    if(pme != null) {
      return pme.poll;
    }
    return null;
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
    PollManagerEntry pme;
    synchronized (pollMapLock) {
      pme = (PollManagerEntry)theRecentPolls.get(key);
    }
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

  long getSlowestHashSpeed() {
    return theSystemMetrics.getSlowestHashSpeed();
  }

  long getBytesPerMsHashEstimate()
      throws SystemMetrics.NoHashEstimateAvailableException {
    return theSystemMetrics.getBytesPerMsHashEstimate();
  }

// ----------------  Callbacks -----------------------------------

  class RouterMessageHandler implements LcapDatagramRouter.MessageHandler {
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
      PollTally tally = poll.getVoteTally();
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
