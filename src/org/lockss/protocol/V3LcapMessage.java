/*
 * $Id: V3LcapMessage.java,v 1.1.2.4 2004-10-29 03:38:19 dshr Exp $
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

public class V3LcapMessage extends LcapMessage implements Serializable {
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

  protected V3LcapMessage() throws IOException {
    super();
  }

  protected V3LcapMessage(byte[] encodedBytes)
    throws IOException {
    super(encodedBytes);
  }

  protected V3LcapMessage(PollSpec ps,
                        ArrayList entries,
                        byte ttl,
                        byte[] challenge,
                        byte[] verifier,
                        byte[] hashedData,
                        int opcode) throws IOException {
    super(ps, entries, ttl, challenge, verifier, hashedData, opcode);
  }

  protected V3LcapMessage(V3LcapMessage trigger,
                        PeerIdentity localID,
                        byte[] verifier,
                        byte[] hashedContent,
                        ArrayList entries,
                        int opcode) throws IOException {

    super(trigger, localID, verifier, hashedContent, entries, opcode);
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
                                           PeerIdentity localID
                                           ) throws IOException {

    V3LcapMessage msg = new V3LcapMessage(pollSpec,
					  null,
					  (byte)0,
					  challenge,
					  null, new byte[0], opcode);
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
  static public LcapMessage decodeToMsg(byte[] data,
                                        boolean mcast) throws IOException {
    V3LcapMessage msg = new V3LcapMessage(data);
    return msg;
  }

    protected byte[] wrapPacket() {
	return new byte[0];
    }

  public String getOpcodeString() {
    return POLL_MESSAGES[m_opcode];
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
