/*
* $Id: PollManager.java,v 1.47 2003-03-15 07:47:13 claire Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import org.mortbay.util.*;
import gnu.regexp.*;
import org.lockss.hasher.HashService;
import org.lockss.repository.LockssRepository;
import org.lockss.daemon.status.*;

public class PollManager  implements LockssManager {
  static final String PARAM_RECENT_EXPIRATION = Configuration.PREFIX +
      "poll.expireRecent";
  static final String PARAM_REPLAY_EXPIRATION = Configuration.PREFIX +
      "poll.expireReplay";
  static final String PARAM_VERIFY_EXPIRATION = Configuration.PREFIX +
      "poll.expireVerifier";

  static final String PARAM_NAMEPOLL_DEADLINE = Configuration.PREFIX +
      "poll.namepoll.deadline";
  static final String PARAM_CONTENTPOLL_MIN = Configuration.PREFIX +
      "poll.contentpoll.min";
  static final String PARAM_CONTENTPOLL_MAX = Configuration.PREFIX +
      "poll.contentpoll.max";

  static final String PARAM_QUORUM = Configuration.PREFIX + "poll.quorum";

  static long DEFAULT_NAMEPOLL_DEADLINE =  10 * Constants.MINUTE;
  static long DEFAULT_CONTENTPOLL_MIN = Constants.HOUR;
  static long DEFAULT_CONTENTPOLL_MAX = 5 * Constants.DAY;
  static final int DEFAULT_QUORUM = 5;

  static final long DEFAULT_RECENT_EXPIRATION = Constants.DAY;
  static final long DEFAULT_REPLAY_EXPIRATION = DEFAULT_RECENT_EXPIRATION/2;
  static final long DEFAULT_VERIFIER_EXPIRATION = Constants.DAY;

  private static PollManager theManager = null;
  private static Logger theLog=Logger.getLogger("PollManager");

  private static Hashtable thePolls = new Hashtable();
  private static HashMap theVerifiers = new HashMap();

  private static LockssRandom theRandom = new LockssRandom();
  private static LcapComm theComm = null;
  private static LockssDaemon theDaemon;

  public PollManager() {
  }


  /**
   * init the plugin manager.
   * @param daemon the LockssDaemon instance
   * @throws LockssDaemonException if we already instantiated this manager
   * @see org.lockss.app.LockssManager#initService(LockssDaemon daemon)
   */
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if(theManager == null) {
      theDaemon = daemon;
      theManager = this;
    }
    else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    theComm = theDaemon.getCommManager();
    theComm.registerMessageHandler(LockssDatagram.PROTOCOL_LCAP,
                                   new CommMessageHandler());
    // register our status
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.registerStatusAccessor(ManagerStatus.TABLE_NAME,
                                      new ManagerStatus());
    statusServ.registerStatusAccessor(PollStatus.TABLE_NAME,
                                      new PollStatus());
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // TODO: checkpoint here.
    // unregister our status
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.unregisterStatusAccessor(ManagerStatus.TABLE_NAME);
    statusServ.unregisterStatusAccessor(PollStatus.TABLE_NAME);
    theManager = null;
  }



  /**
   * make an election by sending a request packet.  This is only
   * called from the tree walk. The poll remains pending in the
   * @param opcode the poll message opcode
   * @param pollspec the PollSpec used to define the range and location of poll
   * @throws IOException thrown if Message construction fails.
   */
  public void requestPoll(int opcode, PollSpec pollspec)
      throws IOException {
    theLog.debug("sending a request for poll of type: " + opcode +
                 " for spec " + pollspec);
    CachedUrlSet cus = pollspec.getCachedUrlSet();
    long duration = calcDuration(opcode, cus);
    byte[] challenge = makeVerifier();
    byte[] verifier = makeVerifier();
    IdentityManager idmgr = theDaemon.getIdentityManager();
    LcapMessage msg =
      LcapMessage.makeRequestMsg(pollspec,
				 null,
				 challenge,
				 verifier,
				 opcode,
				 duration,
				 idmgr.getLocalIdentity());

    theLog.debug2("send poll request: " +  msg.toString());
    sendMessage(msg,cus.getArchivalUnit());
  }


  /**
   * handle an incoming message packet.  This will create a poll if
   * one is not already running. It will then call recieveMessage on
   * the poll.  This was moved from node state which kept track of the polls
   * running in the node.  This will need to be moved or amended to support this.
   * @param msg the message used to generate the poll
   * @throws IOException thrown if the poll was unsucessfully created
   */
  void handleMessage(LcapMessage msg) throws IOException {
    theLog.info("Got a message: " + msg);
    String key = msg.getKey();
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme != null) {
      if(pme.isPollCompleted() || pme.isPollSuspended()) {
        theLog.debug("Message received after poll was closed." + msg);
        return;
      }
    }
    Poll p = findPoll(msg);
    if (p != null) {
      p.receiveMessage(msg);
    }
    else {
      theLog.info("Unable to create poll for Message: " + msg);
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
  synchronized Poll findPoll(LcapMessage msg) throws IOException {
    String key = msg.getKey();
    Poll ret = null;

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
   * make a new poll of the type defined by the incoming message.
   * @param msg <code>Message</code> to use for
   * @return a new Poll object of the required type
   * @throws ProtocolException if message opcode is unknown
   */
  Poll makePoll(LcapMessage msg) throws ProtocolException {
    Poll ret_poll;
    PollSpec spec = new PollSpec(msg);
    CachedUrlSet cus;

    // check for presence of item in the cache
    cus = spec.getCachedUrlSet();
    theLog.debug("making poll from: " + spec);
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

    // create the appropriate poll for the message type
    ret_poll = createPoll(msg, spec);

    if(ret_poll != null) {
      thePolls.put(ret_poll.m_key, new PollManagerEntry(ret_poll));
      ret_poll.startPoll();
      theLog.debug2("Started new poll: " + ret_poll.m_key);
    }

    return ret_poll;
  }


  /**
   * send a message to the multicast address for this archival unit
   * @param msg the LcapMessage to send
   * @param au the ArchivalUnit for this message
   * @throws IOException
   */
  void sendMessage(LcapMessage msg, ArchivalUnit au) throws IOException {
    LockssDatagram ld = new LockssDatagram(LockssDatagram.PROTOCOL_LCAP,
        msg.encodeMsg());
    if(theComm != null) {
      theComm.send(ld, au);
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
    LockssDatagram ld = new LockssDatagram(LockssDatagram.PROTOCOL_LCAP,
        msg.encodeMsg());
    theComm.sendTo(ld, au, id);
  }

  /**
   * check for conflicts between the poll defined by the Message and any
   * currently exsiting poll.
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
      Poll p = entry.poll;

      if(!entry.isPollCompleted() && !p.getMessage().isVerifyPoll()) { // eliminate running verify polls
        CachedUrlSet pcus = p.getPollSpec().getCachedUrlSet();
	ArchivalUnit au = cus.getArchivalUnit();
	LockssRepository repo = theDaemon.getLockssRepository(au);
        int rel_pos = repo.cusCompare(cus, pcus);
        if(rel_pos != LockssRepository.SAME_LEVEL_NO_OVERLAP &&
           rel_pos != LockssRepository.NO_RELATION) {
          return pcus;
        }
      }
    }
    return null;
  }


  /**
   * close the poll from any further voting
   * @param key the poll signature
   */
  void closeThePoll(String key)  {
    long expiration = TimeBase.nowMs() +
                      Configuration.getLongParam(PARAM_RECENT_EXPIRATION,
                      DEFAULT_RECENT_EXPIRATION);
    Deadline d = Deadline.in(expiration);
    TimerQueue.schedule(d, new ExpireRecentCallback(), key);
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    pme.setPollCompleted(d);
    theLog.debug2("completed poll " + key);
  }

  /**
   * suspend a poll while we wait for a repair
   * @param key the identifier key of the poll to suspend
   */
  public void suspendPoll(String key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    pme.setPollSuspended();
    theLog.debug2("suspended poll " + key);
  }


  /**
   * resume a poll that had been suspended for a repair and check the repair
   * @param replayNeeded true we now need to replay the poll results
   * @param key the key of the suspended poll
   */
  public void resumePoll(boolean replayNeeded, Object key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme != null) {
      Poll p = pme.poll;
      long expiration = 0;
      Deadline d;
      if(replayNeeded) {
        theLog.debug2("replaying poll " + (String) key);
        expiration = TimeBase.nowMs() +
                     Configuration.getLongParam(PARAM_REPLAY_EXPIRATION,
                     DEFAULT_REPLAY_EXPIRATION);
        d = Deadline.in(expiration);
        p.getVoteTally().startReplay(d);
      }

      // now we want to make sure we add it to the recent polls.
      expiration += Configuration.getLongParam(PARAM_RECENT_EXPIRATION,
          DEFAULT_RECENT_EXPIRATION);

      d = Deadline.in(expiration);
      TimerQueue.schedule(d, new ExpireRecentCallback(), (String)key);
      pme.setPollCompleted(d);
      theLog.debug2("completed suspended poll " + (String)key);
    }
  }

  void requestVerifyPoll(PollSpec pollspec, long duration, Vote vote)
      throws IOException {

    theLog.debug("Calling a verify poll...");
    IdentityManager idmgr = theDaemon.getIdentityManager();
    CachedUrlSet cus = pollspec.getCachedUrlSet();
    LcapMessage reqmsg = LcapMessage.makeRequestMsg(pollspec,
        null,
        vote.getVerifier(),
        makeVerifier(),
        LcapMessage.VERIFY_POLL_REQ,
        duration,
        idmgr.getLocalIdentity());

    LcapIdentity originator =  idmgr.findIdentity(vote.getIDAddress());
    theLog.debug2("sending our verification request to " + originator.toString());
    sendMessageTo(reqmsg, cus.getArchivalUnit(), originator);

    theLog.debug3("Creating a local poll instance...");
    Poll poll = findPoll(reqmsg);
    poll.m_pollstate = Poll.PS_WAIT_TALLY;
  }

  LockssDaemon getDaemon() {
    return theDaemon;
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
   * @return the array of bytes representing the verifier
   */
  byte[] makeVerifier() {
    byte[] s_bytes = generateRandomBytes();
    byte[] v_bytes = generateVerifier(s_bytes);
    if(v_bytes != null) {
      rememberVerifier(v_bytes, s_bytes);
    }
    return v_bytes;
  }



  /**
   * generate a random array of 20 bytes
   * @return the array of bytes
   */
  byte[] generateRandomBytes() {
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

  protected Poll createPoll(LcapMessage msg, PollSpec pollspec) throws ProtocolException {
    Poll ret_poll = null;

    switch(msg.getOpcode()) {
      case LcapMessage.CONTENT_POLL_REP:
      case LcapMessage.CONTENT_POLL_REQ:
        theLog.debug("Making a content poll on "+ pollspec);
        ret_poll = new ContentPoll(msg, pollspec, this);
        break;
      case LcapMessage.NAME_POLL_REP:
      case LcapMessage.NAME_POLL_REQ:
        theLog.debug("Making a name poll on "+pollspec);
        ret_poll = new NamePoll(msg, pollspec, this);
        break;
      case LcapMessage.VERIFY_POLL_REP:
      case LcapMessage.VERIFY_POLL_REQ:
        theLog.debug("Making a verify poll on "+pollspec);
        ret_poll = new VerifyPoll(msg, pollspec, this);
        break;
      default:
        throw new ProtocolException("Unknown opcode:" + msg.getOpcode());
    }
    addPoll(ret_poll);
    return ret_poll;
  }


  private void rememberVerifier(byte[] verifier,
                                byte[] secret) {
    String ver = String.valueOf(B64Code.encode(verifier));
    String sec = secret == null ? "" : String.valueOf(B64Code.encode(secret));
    long expiration = TimeBase.nowMs() + DEFAULT_VERIFIER_EXPIRATION;
    Deadline d = Deadline.in(expiration);
    TimerQueue.schedule(d, new ExpireVerifierCallback(), ver);
    synchronized (theVerifiers) {
      theVerifiers.put(ver, sec);
    }
  }



  int getQuorum() {
    return Configuration.getIntParam(PARAM_QUORUM, DEFAULT_QUORUM);
  }

  long calcDuration(int opcode, CachedUrlSet cus) {
    long ret = 0;
    int quorum = getQuorum();
    switch (opcode) {
      case LcapMessage.NAME_POLL_REQ:
      case LcapMessage.NAME_POLL_REP:
         ret = Configuration.getLongParam(PARAM_NAMEPOLL_DEADLINE,
            DEFAULT_NAMEPOLL_DEADLINE);
         long earliest = ret - ret/4;
         long latest = ret + ret/4;
         ret = earliest + theRandom.nextLong(latest - earliest);
        theLog.debug2("Name Poll duration: " + ret/1000 + " seconds.");
        break;

      case LcapMessage.CONTENT_POLL_REQ:
      case LcapMessage.CONTENT_POLL_REP:
        long minContent = Configuration.getLongParam(PARAM_CONTENTPOLL_MIN,
            DEFAULT_CONTENTPOLL_MIN);
        long maxContent = Configuration.getLongParam(PARAM_CONTENTPOLL_MAX,
            DEFAULT_CONTENTPOLL_MAX);
        ret = cus.estimatedHashDuration() * 2 * (quorum + 1);
        ret = ret < minContent ? minContent : (ret > maxContent ? maxContent : ret);
        theLog.debug2("Content Poll duration: " + ret/1000 + " seconds.");
        break;

      default:
    }

    return ret;
  }

//-------------- TestPollManager Accessors ----------------------------
/**
 * remove the poll represented by the given key from the poll table and
 * return it.
 * @param key the String representation of the polls key
 * @return Poll the poll if found or null
 */
  Poll removePoll(String key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.remove(key);
    return (pme != null) ? pme.poll : null;
  }

  void addPoll(Poll p) {
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

  static Poll makeTestPoll(LcapMessage msg) throws ProtocolException {
    if(theManager == null) {
      theManager = new PollManager();
    }
    return theManager.makePoll(msg);
  }


// ----------------  Callbacks -----------------------------------
  static class CommMessageHandler implements LcapComm.MessageHandler {

    public void handleMessage(LockssReceivedDatagram rd) {
      theLog.debug3("handling incoming message:" + rd.toString());
      byte[] msgBytes = rd.getData();
      try {
        LcapMessage msg = LcapMessage.decodeToMsg(msgBytes, rd.isMulticast());
        theManager.handleMessage(msg);
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
    static final int ACTIVE = 0;
    static final int WON = 1;
    static final int LOST = 2;
    static final int SUSPENDED = 3;
    static final String[] StatusStrings = { "Active", "Won","Lost", "Suspended"};
    Poll poll;
    PollSpec spec;
    int type;
    Deadline pollDeadline;
    Deadline deadline;
    int status;
    String key;

    PollManagerEntry(Poll p) {
      poll = p;
      spec = p.getPollSpec();
      type = p.getVoteTally().getType();
      key = p.getKey();
      pollDeadline = p.m_deadline;
      deadline = null;
      status = ACTIVE;
    }

    boolean isPollActive() {
      return status == ACTIVE;
    }

    boolean isPollCompleted() {
      return status == WON || status == LOST;
    }

    boolean isPollSuspended() {
      return status == SUSPENDED;
    }

    synchronized void setPollCompleted(Deadline d) {
//      poll = null;
      deadline = d;
      status = poll.getVoteTally().didWinPoll() ? WON : LOST;
    }

    synchronized void setPollSuspended() {
      if(deadline != null) {
        deadline.expire();
        deadline = null;
      }
      status = SUSPENDED;
    }

    String getStatusString() {
      return StatusStrings[status];
    }

    String getTypeString() {
      return Poll.PollName[type];
    }
  }

  static class ManagerStatus implements StatusAccessor {
    static final String TABLE_NAME = "PollManagerTable";

    static final int STRINGTYPE = ColumnDescriptor.TYPE_STRING;
    private static String[] allowedKeys = {
        "Plugin:", "AU:", "URL:", "PollType:", "Status:"};
    private static String[] columnDescriptors = {
        "PluginID", "AuID", "URL", "Range", "PollType", "Status", "Deadline",
        "PollID"};
    private static int[] columnTypes =
        { STRINGTYPE, STRINGTYPE, STRINGTYPE, STRINGTYPE, STRINGTYPE,STRINGTYPE,
    ColumnDescriptor.TYPE_DATE, STRINGTYPE};

    private static String[] preferredOrder =  { "Plugin", "AU", "URL", "Deadline"};
    private static boolean[] ascendPref = { true, true, true, false };

    public List getColumnDescriptors(String key) throws StatusService.NoSuchTableException {
      checkKey(key);
      ArrayList descrsL= new ArrayList(columnDescriptors.length);
      for(int i=0; i< columnDescriptors.length; i++) {
        descrsL.add(new ColumnDescriptor(columnDescriptors[i],
            columnDescriptors[i],columnTypes[i]));
      }
      return descrsL;
    }

    public List getRows(String key) throws StatusService.NoSuchTableException {
      checkKey(key);
      ArrayList rowL = new ArrayList();
      Iterator it = thePolls.values().iterator();
      while(it.hasNext()) {
        PollManagerEntry entry = (PollManagerEntry)it.next();

        if(key == null || matchKey(entry, key)) {
          rowL.add(makeRow(entry));
        }
      }
      return rowL;
    }

    public List getDefaultSortRules(String key)
        throws StatusService.NoSuchTableException {

      checkKey(key);

      ArrayList rulesL = new ArrayList(preferredOrder.length);
      for(int i=0; i< preferredOrder.length; i++) {
        rulesL.add(new StatusTable.SortRule(preferredOrder[i], ascendPref[i]));
      }

      return rulesL;
    }

    public boolean requiresKey() {
      return false;
    }

    public String getTitle(String key) {
      return "Poll Manager Table";
    }

    // utility methods for making a Reference

    public StatusTable.Reference makePluginRef(Object value, String key) {
      return new StatusTable.Reference(value, TABLE_NAME, "Plugin:" + key);
    }

    public StatusTable.Reference makeAURef(Object value, String key) {
      return new StatusTable.Reference(value, TABLE_NAME, "AU:" + key);
    }

    public StatusTable.Reference makeURLRef(Object value, String key) {
      return new StatusTable.Reference(value, TABLE_NAME, "URL:" + key);
    }

    public StatusTable.Reference makePollTypeRef(Object value, String key) {
      return new StatusTable.Reference(value, TABLE_NAME, "PollType:" + key);
    }

    public StatusTable.Reference makeStatusRef(Object value, String key) {
      return new StatusTable.Reference(value, TABLE_NAME, "Status:" + key);
    }

    // key support routines
    private void checkKey(String key) throws StatusService.NoSuchTableException {
      if(key != null && !allowableKey(allowedKeys, key)) {
        throw new StatusService.NoSuchTableException("unknonwn key: " + key);
      }
    }

    private boolean allowableKey(String []keyArray, String key) {
      for(int i=0; i< keyArray.length; i++) {
        if(keyArray[i].startsWith(key))
          return true;
      }
      return false;
    }

    private boolean matchKey(PollManagerEntry entry, String key) {
      boolean isMatch = false;
      PollSpec spec = entry.spec;
      String keyValue = key.substring(key.indexOf(':'));
      if(key.startsWith("Plugin:")) {
        if(spec.getPluginId().equals(keyValue)) {
          isMatch = true;
        }
      }
      else if(key.startsWith("AU:")) {
        if(spec.getAUId().equals(keyValue)) {
          isMatch = true;
        }
      }
      else if(key.startsWith("URL:")) {
        if(spec.getUrl().equals(keyValue)) {
          isMatch = true;
        }
      }
      else if(key.startsWith("PollType:")) {
        if(entry.getTypeString().equals(keyValue)) {
          isMatch = true;
        }
      }
      else if(key.startsWith("Status:")) {
        if(entry.getStatusString().equals(keyValue)) {
          isMatch = true;
        }
      }

      return isMatch;
    }

    private Map makeRow(PollManagerEntry entry) {
      HashMap rowMap = new HashMap();
      PollSpec spec = entry.spec;
      //"PluginID"
      String shortID = spec.getPluginId();
      shortID = shortID.substring(shortID.lastIndexOf('|') + 1);
      rowMap.put("PluginID", shortID);
      //"AuID"
      rowMap.put("AuID", spec.getAUId());
      //"URL"
      rowMap.put("URL", spec.getUrl());
      //"Range"
      rowMap.put("Range", spec.getRangeString());
      //"PollType"
      rowMap.put("PollType",entry.getTypeString());
      //"Status"
      rowMap.put("Status",entry.getStatusString());
      //"Deadline"
      if(entry.pollDeadline != null) {
        rowMap.put("Deadline", new Long(entry.pollDeadline.getExpirationTime()));
      }
      //"PollID"
      if(entry.isPollActive()) {
        rowMap.put("PollID", PollStatus.makePollRef(entry.key,entry.key));
      }
      else {
        rowMap.put("PollID", entry.key);
      }
      return rowMap;
    }
  }

  static class PollStatus implements StatusAccessor {
    static final String TABLE_NAME = "PollTable";

    static final int IPTYPE = ColumnDescriptor.TYPE_IP_ADDRESS;
    static final int INTTYPE = ColumnDescriptor.TYPE_INT;
    static final int STRINGTYPE = ColumnDescriptor.TYPE_STRING;

    private static String[] columnDescriptors = {"Identity", "Reputation", "Agree",
        "Challenge", "Verifier", "Hash" };
    private static int[] columnTypes =
        { IPTYPE, INTTYPE, STRINGTYPE, STRINGTYPE, STRINGTYPE, STRINGTYPE};

    private static String[] preferredOrder =  { "Agree" };


    public List getColumnDescriptors(String key) throws StatusService.NoSuchTableException {
      ArrayList descrsL= new ArrayList(columnDescriptors.length);
      for(int i=0; i< columnDescriptors.length; i++) {
        descrsL.add(new ColumnDescriptor(columnDescriptors[i],
            columnDescriptors[i],columnTypes[i]));
      }
      return descrsL;
    }

    public List getRows(String key) throws StatusService.NoSuchTableException {
      Poll poll = getPoll(key);
      Poll.VoteTally tally = poll.getVoteTally();

      ArrayList l = new ArrayList();
      Iterator it = tally.pollVotes.iterator();
      while(it.hasNext()) {
        Vote vote = (Vote)it.next();
        l.add(makeRow(vote));
      }
      return l;
    }

    public List getDefaultSortRules(String key)
        throws StatusService.NoSuchTableException {

      ArrayList rulesL = new ArrayList(preferredOrder.length);
      for(int i=0; i< preferredOrder.length; i++) {
        rulesL.add(new StatusTable.SortRule(preferredOrder[i], true));
      }

      return rulesL;
    }

    public boolean requiresKey() {
      return true;
    }

    public String getTitle(String key) {
      return "Table for running poll " + key;
    }

    // utility methods for making a Reference

    public static StatusTable.Reference makePollRef(Object value, String key) {
      return new StatusTable.Reference(value, TABLE_NAME, key);
    }

    // key support routines
    private Poll getPoll(String key) throws StatusService.NoSuchTableException {
      Poll poll = null;

      PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
      if(pme != null) {
        poll =  pme.poll;
      }
      if(poll == null) {
        throw new StatusService.NoSuchTableException("unknown poll key: " + key);
      }
      return poll;
    }

    private Map makeRow(Vote vote) {
      HashMap rowMap = new HashMap();

      rowMap.put("Identity", vote.getIDAddress());
      LcapIdentity id = theDaemon.getIdentityManager().findIdentity(vote.getIDAddress());
      rowMap.put("Reputation", String.valueOf(id.getReputation()));
      rowMap.put("Agree", String.valueOf(vote.agree));
      rowMap.put("Challenge", vote.getChallengeString());
      rowMap.put("Verifier",vote.getVerifierString());
      rowMap.put("Hash",vote.getHashString());

      return rowMap;
    }
  }

}
