/*
 * $Id: PollManager.java,v 1.139 2004-09-27 22:39:10 smorabito Exp $
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

  static final String PARAM_RECENT_EXPIRATION = Configuration.PREFIX +
      "poll.expireRecent";
  static final String PARAM_VERIFY_EXPIRATION = Configuration.PREFIX +
      "poll.expireVerifier";

  static final long DEFAULT_RECENT_EXPIRATION = Constants.DAY;
  static final long DEFAULT_VERIFY_EXPIRATION = 6 * Constants.HOUR;

  private static PollManager theManager = null;
  protected static Logger theLog = Logger.getLogger("PollManager");
  private static LcapDatagramRouter.MessageHandler  m_msgHandler;
  private static Hashtable thePolls = new Hashtable();
  private static VariableTimedMap theVerifiers = new VariableTimedMap();
  private static IdentityManager theIDManager;
  private static HashService theHashService;
  private static LockssRandom theRandom = new LockssRandom();
  private static LcapDatagramRouter theRouter = null;
  private AlertManager theAlertManager = null;
  private static SystemMetrics theSystemMetrics = null;

  // our configuration variables
  protected static long m_recentPollExpireTime;
  protected static long m_verifierExpireTime;

  // The PollFactory instances
  PollFactory [] pf = {
    null,
    new V1PollFactory(),
    new V2PollFactory(),
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
    PollerStatus pStatus = new PollerStatus(this);
    statusServ.registerStatusAccessor(PollerStatus.MANAGER_STATUS_TABLE_NAME,
                                      new PollerStatus.ManagerStatus());
    statusServ.registerStatusAccessor(PollerStatus.POLL_STATUS_TABLE_NAME,
                                      new PollerStatus.PollStatus());
    statusServ.registerObjectReferenceAccessor(PollerStatus.MANAGER_STATUS_TABLE_NAME,
					       ArchivalUnit.class,
					       new PollerStatus.ManagerStatusAuRef());
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


  /** Cancel all polls on the specified AU.
   * @param au the AU
   */
  public void cancelAuPolls(ArchivalUnit au) {
    Iterator it = thePolls.values().iterator();
    while (it.hasNext()) {
      PollManagerEntry pme = (PollManagerEntry) it.next();
      ArchivalUnit pau = pme.poll.m_cus.getArchivalUnit();
      if (pau == au) {
        if (!pme.isPollCompleted()) {
          theHashService.cancelAuHashes(pau);
          pme.poll.stopPoll();
        }
      }
    }
  }

  /**
   * Call a poll.  Only used by the tree walk.
   * @param polltype one of <code>Poll.{NAME,CONTENT,VERIFY}_POLL</code>
   * @param pollspec the <code>PollSpec</code> that defines the subject of
   *                 the <code>Poll</code>.
   * @return true if the poll was successfuly called.
   */
  public boolean callPoll(PollSpec pollspec) {
    boolean ret = false;
    int version = pollspec.getPollVersion();
    if (version <= 0 || version >= pf.length) {
      theLog.debug("Bad poll version " + version + " for " + pollspec);
    } else {
      ret = pf[version].callPoll(pollspec, this, theIDManager);
    }
    return ret;
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

  /** Return the PollManagerEntry for the poll with the specified key. */
  public PollManagerEntry getPollManagerEntry(String key) {
    return (PollManagerEntry)thePolls.get(key);
  }

  public ActivityRegulator.Lock acquirePollLock(Object key) {
    ActivityRegulator.Lock lock = null;
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
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
      int version = pme.poll.getVersion();
      if (version > 0 && version < pf.length) {
	expiration = pf[version].getMaxContentPollDuration();
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
    int pollVersion = spec.getPollVersion();
    if (pollVersion <= 0 || pollVersion >= pf.length) {
      theLog.debug("Bad poll version " + pollVersion + " for poll on " + spec);
      return null;
    }

    // check for conflicts
    if (!pf[pollVersion].shouldPollBeCreated(msg, spec, this, theIDManager)) {
      theLog.debug("Poll request ignored");
      return null;
    }
    // check with regulator if not verify poll
    else if (spec.getPollType() != Poll.VERIFY_POLL) {
      // get expiration time for the lock
      long expiration = 2 * msg.getDuration();
      if (AuUrl.isAuUrl(cus.getUrl())) {
        lock = theDaemon.getActivityRegulator(au).
	  getAuActivityLock(ActivityRegulator.TOP_LEVEL_POLL, expiration);
        if (lock==null) {
          theLog.debug2("New top-level poll aborted due to activity lock.");
          return null;
        }
      } else {
        int activity = pf[pollVersion].getPollActivity(msg, spec, this);
        lock = theDaemon.getActivityRegulator(au).
	  getCusActivityLock(cus, activity, expiration);
        if (lock==null) {
          theLog.debug2("New poll aborted due to activity lock.");
          return null;
        }
      }
    }

    // create the appropriate poll for the message type
    try {
      ret_poll = pf[pollVersion].createPoll(msg, spec, this, theIDManager);
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
      if (!msg.isVerifyPoll()) {
        if (!nm.shouldStartPoll(cus, ret_poll.getVoteTally())) {
	  theLog.debug("NodeManager said not to start poll: "+ret_poll);
          // clear the lock
          lock.expire();
          return null;
        }
      }

      thePolls.put(ret_poll.m_key, new PollManagerEntry(ret_poll));
      if (!msg.isVerifyPoll()) {
        nm.startPoll(cus, ret_poll.getVoteTally(), false);
        // set the activity lock in the tally
        ret_poll.getVoteTally().setActivityLock(lock);
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
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme== null || pme.poll == null) {
      theLog.warning("Attempt to close unknown poll : " + key);
      return;
    }
    if(pme.isPollCompleted()) {
      theLog.warning("Attempt to close- previously closed poll: " + key);
      return;
    }
    // mark the poll completed because if we need to call a repair poll
    // we don't want this one to be in conflict with it.
    PollTally tally = pme.poll.getVoteTally();
    pme.setPollCompleted();
    if (tally.getType() != Poll.VERIFY_POLL) {
      NodeManager nm = theDaemon.getNodeManager(tally.getArchivalUnit());
      theLog.debug("sending completed poll results " + tally);
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
   * PollSpec instances which currently have active polls.
   * @return Iterator over set of PollSpec
   */
  protected Iterator getActivePollSpecIterator() {
    Set pollspecs = new HashSet();
    Iterator iter = thePolls.values().iterator();
    while (iter.hasNext()) {
      PollManagerEntry pme = (PollManagerEntry)iter.next();
      if (!pme.isPollCompleted()) {
	pollspecs.add(pme.poll.getPollSpec());
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


  public void setConfig(Configuration newConfig,
			Configuration oldConfig,
			Configuration.Differences changedKeys) {
    m_recentPollExpireTime = newConfig.getTimeInterval(PARAM_RECENT_EXPIRATION,
        DEFAULT_RECENT_EXPIRATION);
    m_verifierExpireTime = newConfig.getTimeInterval(PARAM_VERIFY_EXPIRATION,
                                        DEFAULT_VERIFY_EXPIRATION);
    for (int i = 0; i < pf.length; i++) {
      if (pf[i] != null) {
	pf[i].setConfig(newConfig, oldConfig, changedKeys);
      }
    }
  }

  public PollFactory getPollFactory(int version) {
    PollFactory ret = null;
    if (version > 0 || version <= pf.length) {
      ret = pf[version];
    }
    return ret;
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
