/*
* $Id: Poll.java,v 1.1 2002-10-18 18:05:24 claire Exp $
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
import org.lockss.daemon.*;
import org.lockss.hasher.*;
import org.lockss.protocol.*;
import org.lockss.plugin.*;
import java.util.*;
import org.lockss.util.*;
import java.security.MessageDigest;
import java.io.*;
import java.net.InetAddress;

/**
 * <p>Base class for all poll objects.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class Poll implements Runnable {
  static final String HASH_ALGORITHM = "SHA-1";

  static HashMap thePolls = new HashMap();
  static Logger log=Logger.getLogger("Poll",Logger.LEVEL_DEBUG);

  Message m_msg;          // The message which triggered the poll
  int m_quorum;           // The caller's quorum value
  int m_quorumWt;
  int m_agree;            // The # of votes we've heard that agree with us
  int m_agreeWt;
  int m_disagree;         // The # of votes we've heard that disagree with us
  int m_disagreeWt;

  ArchivalUnit m_arcUnit; // the url as an archival unit
  String m_url;           // the url for this poll
  String m_regExp;        // the regular expression for the poll

  byte[] m_challenge;     // The caller's challenge string
  byte[] m_secret;        // Our secret - where does this come from???
  byte[] m_verifier;      // Our verifier string - hash of secret
  byte[] m_hash;          // Our hash of challenge, verifier and content(S)

  Deadline m_voteTime;    // when to vote
  ProbabilisticTimer m_deadline;    // when election is over

  Identity m_caller;      // who called the poll
  boolean m_voted;        // true if we've voted
  int m_replyOpcode = -1; // opcode used to reply to poll
  int m_hashTime;         // an estimate of the time it will take to hash
/*
  Hashtable checkers = null; // the polls(vote checks) currently active
*/
  int m_counting;          // the number of polls currently active
  long m_createTime;       // poll creation time
  boolean m_voteChecked;   // have we voted on this specific message????
  Thread m_thread;         // the thread for this poll
  String m_key;            // the string we use to store this poll

  Poll(){}

  /**
   * create a new poll for a new message
   *
   * @param msg the <code>Message</code> which contains the information for this
   *            poll.
   *
   */
  Poll(Message msg) {
	m_msg = msg;
	// clear history
	m_agree = 0;
	m_agreeWt = 0;
	m_disagree = 0;
	m_disagreeWt = 0;
	m_quorum = 0;
	m_quorumWt = 0;

	m_createTime = System.currentTimeMillis();

	// now copy the msg elements we need
	m_url = msg.getTargetUrl();
	m_regExp = msg.getRegExp();

	// XXX - why doesn't the plugin require the regexp
	m_arcUnit = Plugin.findArchivalUnit(m_url);

	m_deadline = new ProbabilisticTimer(msg.getDuration());
	m_challenge = msg.getChallenge();

	// XXX - we need to create a verifier
	// m_verifier = Plugin.makeVerifier(m_urlset);
	m_verifier = makeVerifier(this);
	m_caller = msg.getOriginID();
	m_voteChecked = false;
	// XXX - what should this really be
	m_voted = false;

	m_key = new String(m_url + " " + m_regExp	+ m_msg.getOpcode());
	if(thePolls.get(m_url+ m_regExp) == null) {
	  // we actually need to do something
	  thePolls.put(m_key, this);
	}

  }

  public static Poll makePoll(Message msg) throws IOException {
	Poll ret_poll;

	ArchivalUnit arcunit = checkForConflicts(msg);
	if(arcunit != null) {
	  throw new Message.ProtocolException(msg +
		  " conflicts with " + arcunit +
	" in makeElection()");
	}
	switch(msg.getOpcode()) {
	  case Message.CONTENT_POLL_REP:
	  case Message.CONTENT_POLL_REQ:
		ret_poll = new ContentPoll(msg);
		break;
	  case Message.NAME_POLL_REP:
	  case Message.NAME_POLL_REQ:
		ret_poll = new NamePoll(msg);
		break;
	  case Message.VERIFY_POLL_REP:
	  case Message.VERIFY_POLL_REQ:
		ret_poll = new VerifyPoll(msg);
		break;
	  default:
		throw new Message.ProtocolException("Unknown opcode:" +
		msg.getOpcode());
	}
	return ret_poll;

  }

  public static Poll findPoll(Message msg) throws IOException {
	String key = msg.getTargetUrl() + " " + msg.getRegExp() +
			  msg.getOpcode();
	Poll ret = (Poll)thePolls.get(key);
	if(ret == null) {
	  ret = makePoll(msg);
	}
	return ret;
  }

  public Message getMessage() {
	return m_msg;
  }

  public ArchivalUnit getArchivalUnit() {
	return m_arcUnit;
  }


  public boolean acceptMessage(Message msg) {
	return false;
  }

  /**
   * overridden in subclasses
   */
  public void run() {
  }

  protected void abort()  {
	log.info(m_key + " aborted");
	thePolls.remove(m_key);
	m_voteTime.expire();
	m_deadline.expire();
  }

  // pre-vote actions
  static ArchivalUnit checkForConflicts(Message msg) {
	ArchivalUnit  url_set = null;

	String url = msg.getTargetUrl();
	String regexp = msg.getRegExp();
	int    opcode = msg.getOpcode();
	boolean isRegExp = regexp != null;

	// verify polls are never conflicts
	if((opcode == Message.VERIFY_POLL_REP)||
	   (opcode == Message.VERIFY_POLL_REQ)) {
	  return null;

	}
	Iterator iter = thePolls.values().iterator();
	while(iter.hasNext()) {
	  Poll p = (Poll)iter.next();

	  Message p_msg = p.getMessage();
	  String  p_url = p_msg.getTargetUrl();
	  String  p_regexp = p_msg.getRegExp();
	  int     p_opcode = p_msg.getOpcode();
	  boolean p_isRegExp = p_regexp != null;

	  //XXX this should be handled by something else
	  boolean alongside = p_url.equals(url);
	  boolean below = (p_url.startsWith(url) &&
					   url.endsWith(File.separator));
	  boolean above = (url.startsWith(p_url) &&
					   p_url.endsWith(File.separator));

	  // check for content poll conflicts
	  if((opcode == Message.CONTENT_POLL_REP) ||
		 (opcode == Message.CONTENT_POLL_REQ)) {

		if(alongside) { // only verify polls are allowed
		  if((p_opcode == Message.VERIFY_POLL_REP)||
			 (p_opcode == Message.VERIFY_POLL_REQ)) {
			continue;
		  }
		  return p.getArchivalUnit();
		}

		if(above || below) { // verify and regexp polls are allowed
		  if((p_opcode == Message.VERIFY_POLL_REP)||
			 (p_opcode == Message.VERIFY_POLL_REQ)) {
			continue;
		  }
		  if((p_opcode == Message.CONTENT_POLL_REP) ||
			 (p_opcode == Message.CONTENT_POLL_REQ)) {
			if(p_isRegExp) {
			  continue;
			}
		  }
		  return p.getArchivalUnit();
		}
	  }

	  // check for name poll conflicts
	  if((opcode == Message.NAME_POLL_REP) ||
		 (opcode == Message.NAME_POLL_REQ)) {
		if(alongside) { // only verify polls are allowed
		  if((p_opcode == Message.VERIFY_POLL_REP)||
			 (p_opcode == Message.VERIFY_POLL_REQ)) {
			continue;
		  }
		  else {
			return p.getArchivalUnit();
		  }
		}
		if(above) { // only reg exp polls are allowed
		  if((p_opcode == Message.CONTENT_POLL_REP) ||
			 (p_opcode == Message.CONTENT_POLL_REQ)) {
			if(p_isRegExp) {
			  continue;
			}
		  }
		  return p.getArchivalUnit();
		}

		if(below) { // always a conflict
		  return p.getArchivalUnit();
		}
	  }
	}

	return url_set;
  }

  // overridden in the specific polls
  boolean scheduleHash() {
	return false;
  }

  /**
   * choose a vote time which in the range of now and the end of the
   * election.  This is called after we've performed our hash so we
   * don't have to worry about time to complete the hash.
   */
  void chooseVoteTime() {
	// XXX - this needs to be replaced with something better
	long v_mean = m_msg.getDuration()/2;
	long v_stddev = m_msg.getDuration()/5;

	m_voteTime = new ProbabilisticTimer(v_mean,v_stddev);

  }

  void handleAgreeVote(Message msg) {
	int weight = msg.getOriginID().getReputation();
	synchronized (this) {
	  m_agree++;
	  m_agreeWt += weight;
	}

	log.info("I agree with " + msg.toString() + " rep " + weight);
	//XXX - what is a reasonable delay here?
	ProbabilisticTimer wait = new ProbabilisticTimer(1000 * 60);
	wait.sleepUntil();
	rememberVote(msg, true);
	if (!msg.isLocal()) {
	  try {
		VerifyPoll.randomRequestVerify(msg,50);
	  }
	  catch (IOException ex) {
		log.debug("attempt to verify random failed.");
	  }
	}
	m_thread.interrupt();

  }

  void handleDisagreeVote(Message msg) {
	int weight = msg.getOriginID().getReputation();
	boolean local = msg.isLocal();

	synchronized (this) {
	  m_disagree++;
	  m_disagreeWt += weight;
	}
	if (local) {
	  log.error("I disagree with myself about" + msg.toString()
				+ " rep " + weight);
	} else {
	  log.info("I disagree with" + msg.toString() + " rep " + weight);
	}
	//XXX - what is a reasonable delay here?
	ProbabilisticTimer wait = new ProbabilisticTimer(1000 * 60);
	wait.sleepUntil();
	rememberVote(msg, false);
	if (!local) {
	  try {
		VerifyPoll.randomRequestVerify(msg, 50);
	  }
	  catch (IOException ex) {
		log.debug("attempt to verify random failed.");
	  }
	}

	m_thread.interrupt();

  }

  void tally() {
	log.info(m_key + " tally: " + m_agree + "/" + m_agreeWt + " for " +
			 m_disagree + "/" + m_disagreeWt + " against");
	abort();

  }

  // this should be overridden in the specific poll type
  void prepareElection() throws IOException {
	throw new Message.ProtocolException("generic prepareElection");
  }

  // this should be overridden in the specific poll type
  void checkVote() {
  }

  void vote() throws IOException {
	Message msg;
	Identity local_id = Identity.getLocalIdentity();
	if(m_msg == null) {
	  throw new Message.ProtocolException("no trigger!!!");

	}
	long remainingTime = m_deadline.getRemainingTime();
	msg = Message.makeReplyMsg(m_msg, m_hash, m_verifier, m_replyOpcode,
							   remainingTime, local_id);
	log.info("vote:" + msg.toString());
	sendTo(msg,local_id.getAddress(),local_id.getPort());
  }

  private static Random random = new Random();

  static byte[] makeVerifier(Poll p) {
	byte[] v_bytes = null;
	byte[] v_secret = new byte[20];
	// generate random bytes
	random.nextBytes(v_secret);
	// hash them

	MessageDigest hasher;
	try {
	  hasher = MessageDigest.getInstance(HASH_ALGORITHM);
	} catch (java.security.NoSuchAlgorithmException e) {
	  return null;
	}
	hasher.update(v_secret, 0, v_secret.length);
	v_bytes = hasher.digest();

	if(p != null) {
	  p.m_secret = v_secret;
	}

	return v_bytes;
  }

  static void sendTo(Message msg, InetAddress addr, int port)  {
	// XXX implement this
  }

  void closeThePoll(ArchivalUnit arcunit, byte[] challenge)  {
	// XXX implement this
  }

  void rememberVote(Message msg, boolean vote) {
	// XXX implement this

  }

  byte[] findSecret(Message msg) {
	byte[] ret = new byte[0];
	// xxx implement this

	return ret;
  }

  class HashCallback implements HashService.Callback {
	/**
	 * Called to indicate that hashing the content or names of a
	 * <code>CachedUrlSet</code> object has succeeded, if <code>e</code>
	 * is null,  or has failed otherwise.
	 * @param urlset  the <code>CachedUrlSet</code> being hashed.
	 * @param cookie  used to disambiguate callbacks.
	 * @param success true iff the hash completed successfully.
	 * @param hasher  the <code>MessageDigest</code> object that
	 *                contains the hash.
	 * @param e       the exception that caused the hash to fail.
	 */
	public void hashingFinished(CachedUrlSet urlset,
								Object cookie,
								MessageDigest hasher,
                                Exception e) {
	  boolean hash_completed = e == null ? true : false;

	  if(hash_completed)  {
		m_hash = hasher.digest();
		chooseVoteTime();
		checkVote();

	  }

	}

  }

}