/*
 * $Id: LcapIdentity.java,v 1.3 2002-11-20 00:50:48 troberts Exp $
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
package org.lockss.protocol;

import java.net.*;
import java.util.HashMap;
import java.util.Random;
import org.mortbay.util.B64Code;
import org.lockss.util.Logger;
import org.lockss.daemon.*;

/**
 * quick and dirty wrapper class for a network identity.
 * Should this class implement <code>PeerIdentity</code>?
 * @author Claire Griffin
 * @version 1.0
 */
public class LcapIdentity {
  //TODO:
  //1) have hash of identities, so we can ensure uniqueness
  //2) hook up Configuration callback to change local identity

  protected static final int INITIAL_REPUTATION = 500;
  protected static final int REPUTATION_NUMERATOR = 1000;
  protected static final int MAX_REPUTATION_DELTA = 100;
  protected static final int AGREE_DELTA = 100;
  protected static final int DISAGREE_DELTA = -150;
  protected static final int CALL_INTERNAL_DELTA = -100;
  protected static final int SPOOF_DETECTED = -30;
  protected static final int REPLAY_DETECTED = -20;
  protected static final int ATTACK_DETECTED = -500;
  protected static final int VOTE_NOT_VERIFIED = -30;
  protected static final int VOTE_VERIFIED = 40;
  protected static final int VOTE_DISOWNED = -400;
  
  long m_lastActiveTime;
  long m_lastOpTime;
  long m_incrPackets;   // Total packets arrived this interval
  long m_origPackets;   // Unique pkts from this indentity this interval
  long m_forwPackets;   // Unique pkts forwarded by this identity this interval
  long m_duplPackets;   // Duplicate packets originated by this identity
  long m_totalPackets;
  long m_lastTimeZeroed;

  HashMap m_pktsThisInterval = new HashMap();
  HashMap m_pktsLastInterval = new HashMap();

  InetAddress m_address;
  int m_reputation;
  Object m_idKey;
  static HashMap theIdentities = null; // all known identities
  static LcapIdentity theLocalIdentity;
  static Logger theLog=Logger.getLogger("Identity",Logger.LEVEL_DEBUG);
  static Random theRandom = new Random();


  LcapIdentity(Object idKey)  {
    m_idKey = idKey;
    m_reputation = INITIAL_REPUTATION;
    m_lastActiveTime = 0;
    m_lastOpTime = 0;
    m_lastTimeZeroed = 0;
    m_incrPackets = 0;
    m_totalPackets = 0;
    m_origPackets = 0;
    m_forwPackets = 0;
    m_duplPackets = 0;
    if(theIdentities == null) {
      reloadIdentities();
    }
    theIdentities.put(m_idKey, this);
  }

  /**
   * construct a new Identity from an address
   * @param addr the InetAddress
   */
  LcapIdentity(InetAddress addr) {
    m_address = addr;
    m_idKey = makeIdKey(addr);
    m_reputation = INITIAL_REPUTATION;
    m_lastActiveTime = 0;
    m_lastOpTime = 0;
    m_lastTimeZeroed = 0;
    m_incrPackets = 0;
    m_totalPackets = 0;
    m_origPackets = 0;
    m_forwPackets = 0;
    m_duplPackets = 0;
    if(theIdentities == null) {
      reloadIdentities();
    }
    theIdentities.put(m_idKey, this);
  }

  /**
   * construct a new Identity from the information found in
   * a datagram socket
   * @param socket the DatagramSocket
   * @return newly constructed <code>Identity<\code>
   */
  LcapIdentity(DatagramSocket socket) {
    this(socket.getInetAddress());
  }


  /**
   * public constructor for creation of an Identity object
   * from a DatagramSocket
   * @param socket the DatagramSocket
   * @return newly constructed <code>Identity<\code>
   */
  public static LcapIdentity getIdentity(DatagramSocket socket) {
    return getIdentity(socket.getInetAddress());
  }

  /**
   * public constructor for the creation of an Identity object
   * from an address.
   * @param addr the InetAddress
   * @return a newly constructed Identity
   */
  public static LcapIdentity getIdentity(InetAddress addr) {
    LcapIdentity ret;

    if(theIdentities == null)  {
      reloadIdentities();
    }

    if(addr == null)  {
      ret = getLocalIdentity();
    }
    else  {
      ret = findIdentity(makeIdKey(addr));
      if(ret == null)  {
	ret = new LcapIdentity(addr);
      }
    }

    return ret;
  }


  public static LcapIdentity findIdentity(Object idKey)  {
    if(theIdentities == null)  {
      reloadIdentities();
    }

    return (LcapIdentity) theIdentities.get(idKey);
  }

  /**
   * public constructor for the creation of an Identity object that
   * represents the local address
   * @param socket the DatagramSocket used to extract the local info.
   * @return a newly constructed Identity
   */
  public static LcapIdentity getLocalIdentity(DatagramSocket socket) {
    if(theLocalIdentity == null) {
      theLocalIdentity = new LcapIdentity(socket.getLocalAddress());
    }
    return theLocalIdentity;
  }

  /**
   * get the Identity of the local host
   * @return newly constructed <code>Identity<\code>
   */
  public static LcapIdentity getLocalIdentity() {
    if(theLocalIdentity == null)  {
      String identStr = 
	Configuration.getParam(Configuration.PREFIX+"localIPAddress");
      theLocalIdentity = 
      	new LcapIdentity(identStr);
    }
    return theLocalIdentity;
  }

  // accessor methods
  /**
   * return the address of the Identity
   * @return the <code>InetAddress<\code> for this Identity
   */
  public InetAddress getAddress() {
    return m_address;
  }

  /**
   * return the current value of this Identity's reputation
   * @return the int value of reputation
   */
  public int getReputation() {
    return m_reputation;
  }

  /**
   * return true if this Identity is the same as the local host
   * @return boolean true if is the local identity, false otherwise
   */
  public boolean isLocalIdentity() {
    if(theLocalIdentity == null)  {
      getLocalIdentity();
    }
    return isEqual(theLocalIdentity);
  }



  // methods which may need to be overridden
  /**
   * return true if two Identity are found to be the same
   * @param id the Identity to compare with this one
   * @return true if the id keys are the same
   */
  public boolean isEqual(LcapIdentity id) {
    String idKey = (String)id.m_idKey;

    return idKey.equals((String)m_idKey);
  }

  /**
   * return the identity of the Identity
   * @return the String representation of the Identity
   */
  public String toString() {
    return (String)m_idKey;
  }

  /**
   * return the name of the host as a string
   * @return the String representation of the Host
   */
  protected String toHost() {
    return (String)m_idKey;
  }

  //

  /**
   * change the reputation by the amount defined for a agree vote
   */
  public void agreeWithVote() {
    changeReputation(AGREE_DELTA);
  }

  /**
   * change the reputation by the amount defined for a disagree vote
   */
  public void disagreeWithVote() {
    changeReputation(DISAGREE_DELTA);
  }

  /**
   * change the reputation by the amount defined for a internal vote
   */
  public void callInternalPoll() {
    changeReputation(CALL_INTERNAL_DELTA);
  }

  /**
   * change the reputation by the amount defined for a spoofed vote
   */
  public void spoofDetected() {
    changeReputation(SPOOF_DETECTED);
  }

  /**
   * change the reputation by the amount defined for a replayed vote
   */
  public void replayDetected() {
    if (false)	 {
      changeReputation(REPLAY_DETECTED);
    }
  }

  /**
   * change the reputation by the amount defined for an attack attempt
   */
  public void attackDetected() {
    changeReputation(ATTACK_DETECTED);
  }

  /**
   * change the reputation by the amount defined for a unverified vote
   */
  public void voteNotVerify() {
    changeReputation(VOTE_NOT_VERIFIED);
  }

  /**
   * change the reputation by the amount defined for a verify vote
   */
  public void voteVerify() {
    changeReputation(VOTE_VERIFIED);
  }

  /**
   * change the reputation by the amount defined for a disowned vote
   */
  public void voteDisown() {
    changeReputation(VOTE_DISOWNED);
  }

  /**
   * update the active packet counter
   * @param NoOp boolean true if this is a no-op message
   * @param msg the active message
   */
  public void rememberActive(boolean NoOp, LcapMessage msg) {
    m_lastActiveTime = System.currentTimeMillis();
    if (!NoOp) {
      m_lastOpTime = m_lastActiveTime;
    }
    m_incrPackets++;
    m_totalPackets++;
    if (msg.getOriginID() == this) {
      char[] encoded = B64Code.encode(msg.getVerifier());

      String verifier = String.valueOf(encoded);
      Integer count = (Integer) m_pktsThisInterval.get(verifier);
      if (count != null) {
	// We've seen this packet before
	count = new Integer(count.intValue() + 1);
      }
      else {
	count = new Integer(1);
      }
      m_pktsThisInterval.put(verifier, count);
    }
  }

  /**
   * increment the originator packet counter
   * @param msg Message ignored
   */
  public void rememberValidOriginator(LcapMessage msg) {
    m_origPackets++;
  }

  /**
   * increment the forwarded packet counter
   * @param msg Message ignored
   */
  public void rememberValidForward(LcapMessage msg) {
    m_forwPackets++;
  }

  /**
   * increment the duplicate packet counter
   * @param msg Message ignored
   */
  public void rememberDuplicate(LcapMessage msg) {
    m_duplPackets++;
  }

  static void storeIdentities()  {
    // XXX store our identities here
  }
  static void reloadIdentities()  {
    // XXX load our saved Ids here
    theIdentities = new HashMap();
  }

  /**
   * update the reputation value for this Identity
   * @param delta the change in reputation
   */
  void changeReputation(int delta) {
    if (this == theLocalIdentity) {
      theLog.debug(m_idKey + " ignoring reputation delta " + delta);
      return;
    }

    delta = (int) (((float) delta) * theRandom.nextFloat());
    if (delta > 0) {
      if (delta > MAX_REPUTATION_DELTA) {
	delta = MAX_REPUTATION_DELTA;

      }
      if (delta > (REPUTATION_NUMERATOR - m_reputation)) {
	delta = (REPUTATION_NUMERATOR - m_reputation);

      }
    }
    else if (delta < 0) {
      if (delta < (-MAX_REPUTATION_DELTA)) {
	delta = -MAX_REPUTATION_DELTA;
      }
      if ((m_reputation + delta) < 0) {
	delta = -m_reputation;
      }
    }
    if (delta != 0)
      theLog.debug(m_idKey +" change reputation from " + m_reputation +
		   " to " + (m_reputation + delta));
    m_reputation += delta;
  }

  static Object makeIdKey(InetAddress addr)  {
    return addr.getHostAddress();
  }
}
