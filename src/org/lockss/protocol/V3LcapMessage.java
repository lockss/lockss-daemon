/*
 * $Id: V3LcapMessage.java,v 1.1.2.6 2004-11-22 22:27:20 dshr Exp $
 */

/*
  Copyright (c) 2004 Board of Trustees of Leland Stanford Jr. University,
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

import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.StringTokenizer;
import org.lockss.util.*;
import java.io.*;
import org.mortbay.util.B64Code;
import org.lockss.config.*;
import java.security.*;
import org.lockss.app.LockssDaemon;
import org.lockss.poller.*;
import java.util.*;
import org.lockss.effort.*;

/**
 * <p>Description: a V3 version of LcapMessage.</p>
 * @author David Rosenthal
 * @version 1.0
 */

public class V3LcapMessage implements LcapMessage, Serializable {
  public static final int MSG_POLL = 8;
  public static final int MSG_POLL_ACK = 9;
  public static final int MSG_POLL_PROOF = 10;
  public static final int MSG_VOTE = 11;
  public static final int MSG_REPAIR_REQ = 12;
  public static final int MSG_REPAIR_REP = 13;
  public static final int MSG_EVALUATION_RECEIPT = 14;
  public static final String[] POLL_MESSAGES = {
    "0", "1", "2", "3", "4", "5", "6", "7",
    "Poll", "PollAck", "PollProof", "Vote", "RepairReq",
    "RepairRep", "EvaluationReceipt",
  };

  // XXX update this
  /*
    byte
    0-3       signature
    4         multicast
    5         hopcount
    6-7       property length
    8-27      SHA-1 hash of encoded properties
    28-End    encoded properties
  */
  private static Logger log = Logger.getLogger("V3LcapMessage");
  /* items which are not in the property list */
  int m_pollVersion; // poll version number
  boolean m_multicast; // multicast flag - modifiable
  byte m_hopCount; // current hop count - modifiable
  byte[] m_pktHash; // hash of remaining packet
  int m_length; // length of remaining packet

  /* items which are in the property list */
  PeerIdentity m_originatorID; // the peer identity of the originator
  protected String m_hashAlgorithm; // the algorithm used to hash
  byte m_ttl; // The original time-to-live
  long m_startTime; // the original start time
  long m_stopTime; // the original stop time
  int m_opcode; // the kind of packet
  protected String m_pluginVersion; // the plugin version
  protected String m_archivalID; // the archival unit
  protected String m_targetUrl; // the target URL
  protected String m_lwrBound; // the boundary for the url range (opt)
  protected String m_uprBound; // the boundary for the url range (opt)
  protected byte[] m_challenge; // the challenge bytes
  protected byte[] m_verifier; // th verifier bytes
  protected byte[] m_hashed; // the hash of content
  protected String m_lwrRem; // the remaining entries lwr bound (opt)
  protected String m_uprRem; // the remaining entries upr bound (opt)
  protected ArrayList m_entries; // the name poll entry list (opt)
  protected int m_maxSize;

  private EncodedProperty m_props;
  private static byte[] signature = {
    'l', 'p', 'm'};
  private static byte[] pollVersionByte = { '1', '2' };
  private String m_key = null;

  protected V3LcapMessage() throws IOException {
    super();
    m_props = new EncodedProperty();
    m_pollVersion = 3;
    m_maxSize = 0; // XXX

  }

  protected V3LcapMessage(byte[] encodedBytes)
    throws IOException {
    m_props = new EncodedProperty();

    try {
      decodeMsg(encodedBytes);
    }
    catch (IOException ex) {
      log.error("Unreadable Packet", ex);
      throw new ProtocolException("Unable to decode pkt.");
    }
  }

  protected V3LcapMessage(PollSpec ps,
			  ArrayList entries,
			  byte ttl,
			  byte[] challenge,
			  byte[] verifier,
			  byte[] hashedData,
			  int opcode,
			  String hashAlgorithm) throws IOException {
    this();
    // assign the data
    m_targetUrl = ps.getUrl();
    m_uprBound = ps.getUprBound();
    m_lwrBound = ps.getLwrBound();
    m_archivalID = ps.getAuId();
    m_ttl = ttl;
    m_challenge = challenge;
    m_verifier = verifier;
    m_hashed = hashedData;
    m_opcode = opcode;
    m_entries = entries;
    m_hashAlgorithm = hashAlgorithm;
    m_pollVersion = ps.getPollVersion();
    m_pluginVersion = ps.getPluginVersion();
    // null the remaining undefined data
    m_startTime = 0;
    m_stopTime = 0;
    m_multicast = false;
    m_originatorID = null;
    m_hopCount = 0;
  }

  protected V3LcapMessage(V3LcapMessage trigger,
			  PeerIdentity localID,
			  byte[] verifier,
			  byte[] hashedContent,
			  ArrayList entries,
			  int opcode) throws IOException {

    this();
    // copy the essential information from the trigger packet
    m_hopCount = trigger.getHopCount();
    m_ttl = trigger.getTimeToLive();
    m_challenge = trigger.getChallenge();
    m_targetUrl = trigger.getTargetUrl();
    m_uprBound = trigger.getUprBound();
    m_lwrBound = trigger.getLwrBound();
    m_archivalID = trigger.getArchivalId();
    m_entries = entries;
    m_hashAlgorithm = trigger.getHashAlgorithm();
    m_originatorID = localID;
    m_verifier = verifier;
    m_hashed = hashedContent;
    m_opcode = opcode;

    m_startTime = 0;
    m_stopTime = 0;
    m_multicast = false;
    m_pollVersion = trigger.getPollVersion();
    m_pluginVersion = trigger.getPluginVersion();
  }

  /**
   * make a message to request a poll using a pollspec
   * @param props Properties containing the parameters
   * @param challenge the challange bytes
   * @param opcode the kind of poll being requested
   * @param timeRemaining the time remaining for this poll
   * @param localID the identity of the requestor
   * @return message the new V3LcapMessage
   * @throws IOException if unable to create message
   */
  static public LcapMessage makeRequestMsg(PollSpec pollSpec,
					   Properties props,
					   byte[] challenge,
					   int opcode,
                                           long timeRemaining,
                                           PeerIdentity localID,
					   String hashAlgorithm
                                           ) throws IOException {

    V3LcapMessage msg = new V3LcapMessage(pollSpec,
					  null,
					  (byte)0,
					  challenge,
					  null, new byte[0],
					  opcode, hashAlgorithm);
    if (msg != null) {
      msg.m_startTime = TimeBase.nowMs();
      msg.m_stopTime = msg.m_startTime + timeRemaining;
      msg.m_originatorID = localID;
    }
    if (false) {
      // XXX
      msg.storeProps();
    }
    return msg;

  }

  /**
   * static method to make a reply message
   * @param trigger the message which trggered the reply
   * @param hashedContent the hashed content bytes
   * @param verifier the veerifier bytes
   * @param entries the entries which were used to calculate the hash
   * @param opcode an opcode for this message
   * @param timeRemaining the time remaining on the poll
   * @param localID the identity of the requestor
   * @return a new Message object
   * @throws IOException if message construction failed
   */
  static public V3LcapMessage makeReplyMsg(V3LcapMessage trigger,
					   byte[] hashedContent,
					   byte[] verifier,
					   ArrayList entries,
					   int opcode,
					   long timeRemaining,
					   PeerIdentity localID) throws
					     IOException {
    if (hashedContent == null) {
      log.error("Making a reply message with null hashed content");
    }
    V3LcapMessage msg = new V3LcapMessage(trigger, localID, verifier,
					  hashedContent, entries, opcode);
    if (msg != null) {
      msg.m_startTime = TimeBase.nowMs();
      msg.m_stopTime = msg.m_startTime + timeRemaining;
    }
    msg.storeProps();
    return msg;
  }

  /**
   * a static method to create a new message from a byte array
   * as may be found in a message
   * @param data the data from the message packet
   * @param mcast true if packet was from a multicast socket
   * @return a new Message object
   * @throws IOException
   */
  static public V3LcapMessage decodeToMsg(byte[] data,
					  boolean mcast) throws IOException {
    V3LcapMessage msg = new V3LcapMessage(data);
    return msg;
  }

  protected byte[] wrapPacket() {
    return new byte[0];
  }

  /**
   * get a property that was decoded and stored for this packet
   * @param key - the property name under which the it is stored
   * @return a string representation of the property
   */
  public String getPacketProperty(String key) {
    String ret = "";
    // XXX
    return ret;
  }

  /**
   * set or add a new property into the packet.
   * @param key the key under which to store the property
   * @param value the value to store
   */
  public void setMsgProperty(String key, String value) {
    String ret = "";
    // XXX
    return;
  }

  /**
   * decode the raw packet data into a property table
   * @param encodedBytes the array of encoded bytes
   * @throws IOException
   */
  public void decodeMsg(byte[] encodedBytes) throws IOException {
    // XXX
  }

  /**
   * encode the message from a props table into a stream of bytes
   * @return the encoded message as bytes
   * @throws IOException if the packet can not be encoded
   */
  public byte[] encodeMsg() throws IOException {
    byte[] ret = new byte[0];
    // XXX
    return ret;
  }

  /**
   * store the local variables in the property table
   * @throws IOException if the packet can not be stored
   */
  public void storeProps() throws IOException {
    // XXX
  }


  public long getDuration() {
    long now = TimeBase.nowMs();
    long ret = m_stopTime - now;
    if (ret < 0)
      ret = 0;
    return ret;
  }

  public long getElapsed() {
    long now = TimeBase.nowMs();
    long ret = now - m_startTime;
    if (now > m_stopTime)
      ret = m_stopTime - m_startTime;
    return ret;
  }

  public boolean isReply() {
    boolean ret = false;
    // XXX
    return ret;
  }

  public boolean isNamePoll() {
    return false;
  }

  public boolean isContentPoll() {
    return true;
  }

  public boolean isVerifyPoll() {
    return false;
  }

  public boolean isNoOp() {
    //  XXX not needed
    return false;
  }

  /* methods to support data access */
  public long getStartTime() {
    return m_startTime;
  }

  public long getStopTime() {
    return m_stopTime;
  }

  public byte getTimeToLive() {
    return m_ttl;
  }

  public PeerIdentity getOriginatorID() {
    return m_originatorID;
  }

  public int getOpcode() {
    return m_opcode;
  }

  public String getOpcodeString() {
    return POLL_MESSAGES[m_opcode];
  }

  public String getArchivalId() {
    return m_archivalID;
  }

  public String getPluginVersion() {
    return m_pluginVersion;
  }

  public boolean getMulticast() {
    return false;
  }

  public void setMulticast(boolean multicast) {
    // No action intended
  }

  public int getPollVersion() {
    return 3;
  }

  public void setPollVersion(int vers) {
    // No action intended
  }

  public boolean supportedPollVersion(int vers) {
    return (vers == 3);
  }

  public ArrayList getEntries() {
    return null;
  }

  public String getLwrRemain() {
    return m_lwrRem;
  }

  public String getUprRemain() {
    return m_uprRem;
  }

  public String getLwrBound() {
    return m_lwrBound;
  }

  public String getUprBound() {
    return m_uprBound;
  }

  public byte getHopCount() {
    return 0;
  }

  public void setHopCount(int hopCount) {
    // No action intended
  }

  public byte[] getChallenge() {
    return m_challenge;
  }

  public byte[] getVerifier() {
    byte[] ret = new byte[0];
    // XXX
    return ret;
  }

  public byte[] getHashed() {
    byte[] ret = new byte[0];
    // XXX
    return ret;
  }

  public String getTargetUrl() {
    return m_targetUrl;
  }

  public String getHashAlgorithm() {
    return m_hashAlgorithm;
  }

  public String getKey() {
    if (m_key == null) {
      switch (m_pollVersion) {
      case 1:
	m_key = V1Poll.challengeToKey(m_challenge);
	break;
      case 3:
	m_key = V3Poll.challengeToKey(m_challenge);
	break;
      }
    }
    return m_key;
  }

  public String entriesToString(int maxBufSize) {
    return "";
  }

  public ArrayList stringToEntries(String estr) {
    ArrayList ret = new ArrayList(0);
    // XXX
    return ret;
  }


    
  public EffortService.Proof getEffortProof() {
    EffortService.Proof ret = null; // XXX
    return ret;
  }

  public Deadline getDeadline() {
    Deadline ret = Deadline.at(m_stopTime);
    // XXX
    return ret;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[V3LcapMessage: from ");
    sb.append(m_originatorID);
    sb.append(", ");
    sb.append(m_targetUrl);
    sb.append(" ");
    sb.append(m_lwrBound);
    sb.append("-");
    sb.append(m_uprBound);
    sb.append(" ");
    sb.append(POLL_MESSAGES[m_opcode]);
    if (m_challenge != null) {
      sb.append(" C:");
      sb.append(String.valueOf(B64Code.encode(m_challenge)));
    }
    if (m_verifier != null) {
      sb.append(" V:");
      sb.append(String.valueOf(B64Code.encode(m_verifier)));
    }
    if (m_hashed != null) { //can be null for a request message
      sb.append(" H:");
      sb.append(String.valueOf(B64Code.encode(m_hashed)));
    }
    sb.append("]");
    return sb.toString();
  }

}
