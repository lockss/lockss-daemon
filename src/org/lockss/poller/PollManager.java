/*
* $Id: PollManager.java,v 1.22 2003-01-31 09:47:19 claire Exp $
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


import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.protocol.ProtocolException;
import org.lockss.util.*;
import org.mortbay.util.*;
import gnu.regexp.*;
/*
 TODO: use correct algorithm for determining deadlines for polls
 TODO: move checkForConflict into the appropriate poll class
 TODO: replace checkForConflict url position with call to NodeManager
 */
public class PollManager {
  static final String PARAM_RECENT_EXPIRATION = Configuration.PREFIX +
      "poll.expireRecent";
  static final String PARAM_REPLAY_EXPIRATION = Configuration.PREFIX +
      "poll.expireReplay";
  static final String PARAM_VERIFY_EXPIRATION = Configuration.PREFIX +
      "poll.expireVerifier";


  static final long DEFAULT_RECENT_EXPIRATION = 24 * 60 * 60 * 1000; // 1 day
  static final long DEFAULT_REPLAY_EXPIRATION = DEFAULT_RECENT_EXPIRATION/2;
  static final long DEFAULT_VERIFIER_EXPIRATION = 24 * 60 * 60 * 1000;

  private static PollManager theManager = null;
  private static Logger theLog=Logger.getLogger("PollManager");

  private static HashMap thePolls = new HashMap();
  private static HashMap theVerifiers = new HashMap();

  private static Random theRandom = new Random();
  private static LcapComm theComm = null;
  private static IdentityManager theIdMgr = IdentityManager.getIdentityManager();

  private PollManager() {
  }

  public static PollManager getPollManager() {
    if(theManager == null) {
      theManager = new PollManager();

      try {
        theComm = LcapComm.getComm();
        theComm.registerMessageHandler(LockssDatagram.PROTOCOL_LCAP,
                                       new CommMessageHandler());
      }
      catch(Exception ex) {
        theLog.warning("Unitialized Comm!");
      }
    }
    return theManager;
  }


  /**
   * make an election by sending a request packet.  This is only
   * called from the tree walk. The poll remains pending in the
   * @param url the String representing the url for this poll
   * @param regexp the String representing the regexp for this poll
   * @param opcode the poll message opcode
   * @param duration the time this poll has to run
   * @throws IOException thrown if Message construction fails.
   */
  public void requestPoll(String url,
                              String regexp,
                              int opcode,
                              long duration)
      throws IOException {
    long voteRange = duration/4;
    Deadline deadline = Deadline.inRandomRange(duration - voteRange,
        duration + voteRange);
    long timeUntilCount = deadline.getRemainingTime();
    byte[] challenge = makeVerifier();
    byte[] verifier = makeVerifier();
    LcapMessage msg = LcapMessage.makeRequestMsg(url,regexp,null,
        challenge,verifier,opcode,timeUntilCount,
        theIdMgr.getLocalIdentity());

    theLog.debug("send: " +  msg.toString());
    sendMessage(msg, PluginManager.findArchivalUnit(url));
  }


  /**
   * return the default MessageDigest hasher
   * @return MessageDigest the hasher
   */
  public MessageDigest getHasher() {
    return getHasher(null);
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
        theLog.info("Message received after poll was closed." + msg.toString());
        return;
      }
    }
    Poll p = findPoll(msg);
    if (p != null) {
      p.receiveMessage(msg);
      // NodeManager.updatePollState(???);
    }
    else {
      theLog.info("Unable to create poll for Message: " + msg.toString());
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
      theLog.info("Making new poll: "+ key);
      ret = makePoll(msg);
      theLog.info("Done making new poll: "+ key);
    }
    else {
      ret = pme.poll;
    }
    return ret;
  }

  /**
   * make a new poll of the type defined by the incoming message.
   * @param msg <code>Message</code> to use for
   * @return a new Poll object of the required type
   * @throws ProtocolException if message opcode is unknown or if new poll would
   * conflict with currently running poll.
   */
  Poll makePoll(LcapMessage msg) throws ProtocolException {
    Poll ret_poll;
    ArchivalUnit au;
    CachedUrlSet cus;

    // check for presence of item in the cache
    try {
      au = PluginManager.findArchivalUnit(msg.getTargetUrl());
      cus = au.makeCachedUrlSet(msg.getTargetUrl(), msg.getRegExp());
    }
    catch (Exception ex) {
      theLog.debug(msg.getTargetUrl() + " not in this cache.");
      return null;
    }

    // check for conflicts
    CachedUrlSet conflict = checkForConflicts(msg);
    if(conflict != null) {
      String err = msg.toString() + " conflicts with " + conflict.toString() +
                   " in makeElection()";
      theLog.debug(err);
      return null;
    }

    // create the appropriate message type
    switch(msg.getOpcode()) {
      case LcapMessage.CONTENT_POLL_REP:
      case LcapMessage.CONTENT_POLL_REQ:
        theLog.debug("Making a content poll on "+cus);
        ret_poll = new ContentPoll(msg, cus, this);
        break;
      case LcapMessage.NAME_POLL_REP:
      case LcapMessage.NAME_POLL_REQ:
        theLog.debug("Making a name poll on "+cus);
        ret_poll = new NamePoll(msg, cus, this);
        break;
      case LcapMessage.VERIFY_POLL_REP:
      case LcapMessage.VERIFY_POLL_REQ:
        theLog.debug("Making a verify poll on "+cus);
        ret_poll = new VerifyPoll(msg, cus, this);
        break;
      default:
        throw new ProtocolException("Unknown opcode:" + msg.getOpcode());
    }

    if(ret_poll != null) {
      thePolls.put(ret_poll.m_key, new PollManagerEntry(ret_poll));
      ret_poll.startPoll();
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
    else {
      theLog.warning("Uninitialized comm.");
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
    if(theComm != null) {
      theComm.sendTo(ld, au, id);
    }
    else {
      theLog.warning("Uninitialized comm.");
    }
  }

  /**
   * check for conflicts between the poll defined by the Message and any
   * currently exsiting poll.
   * @param msg the <code>Message</code> to check
   * @return the CachedUrlSet of the conflicting poll.
   */
  CachedUrlSet checkForConflicts(LcapMessage msg) {
    String url = msg.getTargetUrl();
    String regexp = msg.getRegExp();
    int    opcode = msg.getOpcode();
    boolean isRegExp = regexp != null;

    // verify polls are never conflicts
    if(msg.isVerifyPoll()) {
      return null;
    }

    Iterator iter = thePolls.values().iterator();
    while(iter.hasNext()) {
      Poll p = ((PollManagerEntry)iter.next()).poll;

      LcapMessage p_msg = p.getMessage();
      String  p_url = p_msg.getTargetUrl();
      String  p_regexp = p_msg.getRegExp();
      int     p_opcode = p_msg.getOpcode();
      boolean p_isRegExp = p_regexp != null;

      // eliminate verify polls
      if(p_msg.isVerifyPoll()) {
        continue;
      }
      // int position = NodeManager.getRelativePosition(cus1, cus2);
      //XXX this should be handled by something else
      boolean alongside = p_url.equals(url);
      boolean below = (p_url.startsWith(url) &&
                       url.endsWith(File.separator));
      boolean above = (url.startsWith(p_url) &&
                       p_url.endsWith(File.separator));

      // check for content poll conflicts
      if(msg.isContentPoll()) {

        if(alongside) { // only verify polls are allowed
          theLog.debug("conflict new content or name poll alongside content poll");
          return p.getCachedUrlSet();
        }

        if(above || below) { // verify and regexp polls are allowed
          if(p_msg.isContentPoll()) {
            if(p_isRegExp) {
              continue;
            }
          }
          theLog.debug("conflict new name poll above or below content poll");
          return p.getCachedUrlSet();
        }
      }

      // check for name poll conflicts
      if(msg.isNamePoll()) {
        if(alongside) { // only verify polls are allowed
          theLog.debug("conflict new content or name poll alongside name poll");
          return p.getCachedUrlSet();
        }
        if(above) { // only reg exp polls are allowed
          if(p_msg.isContentPoll()) {
            if(p_isRegExp) {
              continue;
            }
          }
          theLog.debug("conflict new name poll above name poll");
          return p.getCachedUrlSet();
        }

        if(below) { // always a conflict
          theLog.debug("conflict new poll below name poll");
          return p.getCachedUrlSet();
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
    synchronized(thePolls) {
      PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
      if(pme != null) {
        pme.setPollCompleted(d);
      }
    }
  }

  /**
   * suspend a poll while we wait for a repair
   * @param p the poll to suspend
   */
  void suspendPoll(Poll p) {
    synchronized(thePolls) {
      PollManagerEntry pme = (PollManagerEntry)thePolls.get(p.m_key);
      if(pme != null) {
        if(pme.deadline != null) {
          pme.deadline.expire();
        }
      }
      else {
        pme = new PollManagerEntry(p);
        thePolls.put(p.m_key, pme);
      }
      pme.setPollSuspended();
    }

  }


  /**
   * resume a poll that had been suspended for a repair and check the repair
   * @param key the key of the suspended poll
   */
  void resumePoll(Object key) {
    synchronized(thePolls) {
      PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
      if(pme != null) {
        Poll p = pme.poll;
        long expiration = TimeBase.nowMs() +
                          Configuration.getLongParam(PARAM_REPLAY_EXPIRATION,
                          DEFAULT_REPLAY_EXPIRATION);
        Deadline d = Deadline.in(expiration);
        p.getVoteTally().startReplay(d);

        // now we want to make sure we add it to the recent polls.
        expiration += Configuration.getLongParam(PARAM_RECENT_EXPIRATION,
            DEFAULT_RECENT_EXPIRATION);

        d = Deadline.in(expiration);
        TimerQueue.schedule(d, new ExpireRecentCallback(), (String)key);
        pme.setPollCompleted(d);
      }
    }
  }

  void requestVerifyPoll(String url, String regexp, long duration, Vote vote)
      throws IOException {

    theLog.debug("Calling a verify poll...");

    LcapMessage reqmsg = LcapMessage.makeRequestMsg(url,
        regexp,
        null,
        vote.getVerifier(),
        makeVerifier(),
        LcapMessage.VERIFY_POLL_REQ,
        duration,
        theIdMgr.getLocalIdentity());

    LcapIdentity originator = vote.getIdentity();
    theLog.debug("sending our verification request to " + originator.toString());
    sendMessageTo(reqmsg, PluginManager.findArchivalUnit(url), originator);

    theLog.debug("Creating a local poll instance...");
    Poll poll = findPoll(reqmsg);
    poll.m_pollstate = Poll.PS_WAIT_TALLY;
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


//-------------- TestPollManager Accessors ----------------------------
  /**
   * remove the poll represented by the given key from the poll table and
   * return it.
   * @param key the String representation of the polls key
   * @return Poll the poll if found or null
   */
  synchronized Poll removePoll(String key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.remove(key);
    return (pme != null) ? pme.poll : null;
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


// ----------------  Callbacks -----------------------------------
  static class CommMessageHandler implements LcapComm.MessageHandler {

    public void handleMessage(LockssReceivedDatagram rd) {
      theLog.debug("handling incoming message:" + rd.toString());
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
    static final int COMPLETED = 1;
    static final int SUSPENDED = 2;

    Poll poll;
    Deadline deadline;
    int status;

    PollManagerEntry(Poll p) {
      poll = p;
      deadline = null;
      status = ACTIVE;
    }

    boolean isPollActive() {
      return status == ACTIVE;
    }

    boolean isPollCompleted() {
      return status == COMPLETED;
    }

    boolean isPollSuspended() {
      return status == SUSPENDED;
    }


    void setPollCompleted(Deadline d) {
      poll = null;
      deadline = d;
      status = COMPLETED;
    }

    void setPollSuspended() {
      if(status == SUSPENDED) {
        if(deadline != null) {
          deadline.expire();
          deadline = null;
        }
      }
       status = SUSPENDED;
    }

  }

}