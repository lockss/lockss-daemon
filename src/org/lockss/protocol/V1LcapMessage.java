/*
 * $Id$
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

package org.lockss.protocol;

import java.io.*;
import java.util.*;

import org.mortbay.util.B64Code;

import org.lockss.config.CurrentConfig;
import org.lockss.poller.*;
import org.lockss.util.*;

/**
 * <p>Encapsulation of a V1 protocol message which has been received or
 * will be sent over the wire.</p>
 *
 * @author Claire Griffin
 * @version 1.0
 */
public class V1LcapMessage extends LcapMessage {
  // Opcode enum
  public static final int NAME_POLL_REQ = 0;
  public static final int NAME_POLL_REP = 1;
  public static final int CONTENT_POLL_REQ = 2;
  public static final int CONTENT_POLL_REP = 3;
  public static final int VERIFY_POLL_REQ = 4;
  public static final int VERIFY_POLL_REP = 5;
  public static final int NO_OP = 6;

  public static final int MAX_HOP_COUNT_LIMIT = 16;
  public static final int DEFAULT_MAX_PKT_SIZE = 1422;
  public static final String[] POLL_OPCODES =  {"NameReq", "NameRep",
						"ContentReq", "ContentRep",
						"VerifyReq", "VerifyRep",
						"NoOp" };


  // V1 specific properties.
  protected int m_maxSize;
  protected boolean m_multicast;
  protected byte m_hopCount;
  protected String m_lwrBound; // the boundary for the url range (opt)
  protected String m_uprBound; // the boundary for the url range (opt)
  protected String m_lwrRem; // the remaining entries lwr bound (opt)
  protected String m_uprRem; // the remaining entries upr bound (opt)
  protected ArrayList m_entries; // the name poll entry list (opt)

  protected byte[] m_challenge; // the challenge bytes
  protected byte[] m_verifier; // the verifier bytes
  protected byte[] m_hashed; // the hash of content

  /*
    byte
    0-3       signature
    4         multicast
    5         hopcount
    6-7       property length
    8-27      SHA-1 hash of encoded properties
    28-End    encoded properties
  */

  private static Logger log = Logger.getLogger("V1LcapMessage");

  protected V1LcapMessage() throws IOException {
    m_props = new EncodedProperty();
    m_pollProtocol = Poll.V1_PROTOCOL;
    m_maxSize = CurrentConfig.getIntParam(PARAM_MAX_PKT_SIZE,
                                          DEFAULT_MAX_PKT_SIZE);
  }

  protected V1LcapMessage(byte[] encodedBytes) throws IOException {
    m_props = new EncodedProperty();
    try {
      decodeMsg(encodedBytes);
    } catch (IOException ex) {
      log.error("Unreadable Packet", ex);
      throw new ProtocolException("Unable to decode pkt.");
    }
  }

  protected V1LcapMessage(PollSpec ps, ArrayList entries,
			  byte[] challenge, byte[] verifier,
			  byte[] hashedData, int opcode)
      throws IOException {
    this();
    // assign the data
    m_targetUrl = ps.getUrl();
    m_uprBound = ps.getUprBound();
    m_lwrBound = ps.getLwrBound();
    m_archivalID = ps.getAuId();
    m_challenge = challenge;
    m_verifier = verifier;
    m_hashed = hashedData;
    m_opcode = opcode;
    m_entries = entries;
    m_hashAlgorithm = LcapMessage.getDefaultHashAlgorithm();
    m_pollProtocol = ps.getProtocolVersion();
    m_pluginVersion = ps.getPluginVersion();
    // null the remaining undefined data
    m_startTime = 0;
    m_stopTime = 0;
    m_multicast = false;
    m_originatorID = null;
    m_hopCount = 0;
  }

  protected V1LcapMessage(V1LcapMessage trigger, PeerIdentity localID,
			  byte[] verifier, byte[] hashedContent,
			  ArrayList entries, int opcode)
      throws IOException {
    this();
    // copy the essential information from the trigger packet
    m_hopCount = trigger.getHopCount();
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
    m_pollProtocol = trigger.getProtocolVersion();
    m_pluginVersion = trigger.getPluginVersion();
  }

  public void decodeMsg(byte[] encodedBytes) throws IOException {
    this.decodeMsg(new ByteArrayInputStream(encodedBytes));
  }

  /**
   * decode the raw packet data into a property table
   *
   * @param is the InputStrem from which the encoded message data will be read
   * @throws IOException
   */
  public void decodeMsg(InputStream is) throws IOException {
    long duration;
    long elapsed;
    // the mutable stuff
    DataInputStream dis = new DataInputStream(is);
    // read in the three header bytes
    for (int i = 0; i < signature.length; i++) {
      if (signature[i] != dis.readByte()) {
	throw new ProtocolException("Invalid Signature");
      }
    }
    // read in the poll version byte and decode
    m_pollProtocol = -1;
    byte ver = dis.readByte();
    for (int i = 0; i < protocolByte.length; i++) {
      if (protocolByte[i] == ver)
	m_pollProtocol = i + 1;
    }
    if (m_pollProtocol <= 0) {
      throw new ProtocolException("Unsupported inbound poll version: " + ver);
    }
    m_multicast = dis.readBoolean();
    m_hopCount = dis.readByte();
    if (!hopCountInRange(m_hopCount)) {
      throw new ProtocolException("Hop count out of range.");
    }
    int prop_len = dis.readShort();
    byte[] hash_bytes = new byte[SHA_LENGTH];
    byte[] prop_bytes = new byte[prop_len];
    dis.read(hash_bytes);
    dis.read(prop_bytes);
    if (!verifyHash(hash_bytes, prop_bytes)) {
      throw new ProtocolException("Hash verification failed.");
    }
    // decode the properties
    m_props.decode(prop_bytes);
    // the immutable stuff
    String addr_str = m_props.getProperty("origIP");
    m_originatorID = m_idManager.stringToPeerIdentity(addr_str);
    m_hashAlgorithm = m_props.getProperty("hashAlgorithm");
    duration = m_props.getInt("duration", 0) * 1000;
    elapsed = m_props.getInt("elapsed", 0) * 1000;
    m_opcode = m_props.getInt("opcode", -1);
    m_archivalID = m_props.getProperty("au", "UNKNOWN");
    m_targetUrl = m_props.getProperty("url");
    m_lwrBound = m_props.getProperty("lwrBnd");
    m_uprBound = m_props.getProperty("uprBnd");
    m_challenge = m_props.getByteArray("challenge", ByteArray.EMPTY_BYTE_ARRAY);
    m_verifier = m_props.getByteArray("verifier", ByteArray.EMPTY_BYTE_ARRAY);
    m_hashed = m_props.getByteArray("hashed", ByteArray.EMPTY_BYTE_ARRAY);
    m_lwrRem = m_props.getProperty("lwrRem");
    m_uprRem = m_props.getProperty("uprRem");
    m_entries = stringToEntries(m_props.getProperty("entries"));
    m_pluginVersion = m_props.getProperty("plugVer");
    // calculate start and stop times
    long now = TimeBase.nowMs();
    m_startTime = now - elapsed;
    m_stopTime = now + duration;
  }

  /**
   * encode the message from a props table into a stream of bytes
   *
   * @return the encoded message as bytes
   * @throws IOException if the packet can not be encoded
   */
  public byte[] encodeMsg() throws IOException {
    if (!isPollVersionSupported(m_pollProtocol))
      throw new ProtocolException("Unsupported outbound poll version: "
				  + m_pollProtocol);
    byte[] prop_bytes = m_props.encode();
    byte[] hash_bytes = computeHash(prop_bytes);
    // build out the remaining packet
    int enc_len = prop_bytes.length + hash_bytes.length + 8; // msg header is 8 bytes
    ByteArrayOutputStream baos = new ByteArrayOutputStream(enc_len);
    DataOutputStream dos = new DataOutputStream(baos);
    dos.write(signature);
    dos.write(protocolByte[m_pollProtocol - 1]);
    dos.writeBoolean(m_multicast);
    dos.writeByte(m_hopCount);
    dos.writeShort(prop_bytes.length);
    dos.write(hash_bytes);
    dos.write(prop_bytes);
    return baos.toByteArray();
  }

  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(encodeMsg());
  }

  /**
   * store the local variables in the property table
   *
   * @throws IOException if the packet can not be stored
   */
  public void storeProps() throws IOException {
    // make sure the props table is up to date
    try {
      // use port 0 here - it will be ignored in the string
      m_props.put("origIP", m_originatorID.getIdString());
    } catch (NullPointerException npe) {
      throw new ProtocolException("encode - null origin host address.");
    }
    if (m_opcode == NO_OP) {
      m_props.putInt("opcode", m_opcode);
      m_props.putByteArray("verifier", m_verifier);
      return;
    }
    m_props.setProperty("hashAlgorithm", getHashAlgorithm());
    // TTL is not used by any V1 or V3 functionality, but this prop
    // should continue to exist for backward compatibility with
    // pre-1.8 daemons.
    m_props.putInt("ttl", 0);
    m_props.putInt("duration", (int) (getDuration() / 1000));
    m_props.putInt("elapsed", (int) (getElapsed() / 1000));
    m_props.putInt("opcode", m_opcode);
    m_props.setProperty("url", m_targetUrl);
    if (m_lwrBound != null) {
      m_props.setProperty("lwrBnd", m_lwrBound);
    }
    if (m_uprBound != null) {
      m_props.setProperty("uprBnd", m_uprBound);
    }
    if (m_archivalID == null) {
      m_archivalID = "UNKNOWN";
    }
    m_props.setProperty("au", m_archivalID);
    m_props.putByteArray("challenge", m_challenge);
    m_props.putByteArray("verifier", m_verifier);
    if (m_hashed != null) {
      m_props.putByteArray("hashed", m_hashed);
    } else if (m_opcode % 2 == 1) { // if we're a reply we'd better have a hash
      throw new ProtocolException("encode - missing hash in reply packet.");
    }
    byte[] cur_bytes = m_props.encode();
    long diff_pktsize = 0;
    do {
      m_maxSize -= diff_pktsize;
      int remaining_bytes = m_maxSize - cur_bytes.length - 28;
      if (m_entries != null) {
	m_props.setProperty("entries", entriesToString(remaining_bytes));
      }
      if (m_lwrRem != null) {
	m_props.setProperty("lwrRem", m_lwrRem);
      }
      if (m_uprRem != null) {
	m_props.setProperty("uprRem", m_uprRem);
      }
      if (m_pluginVersion != null) {
	m_props.setProperty("plugVer", m_pluginVersion);
      }
      long pktsize = new LockssDatagram(LockssDatagram.PROTOCOL_LCAP,
					encodeMsg()).getPacketSize();
      diff_pktsize = pktsize - LockssDatagram.MAX_SIZE;
    } while (diff_pktsize > 0);
  }

  public String getKey() {
    if (m_key == null) {
      m_key = V1Poll.challengeToKey(m_challenge);
    }
    return m_key;
  }

  private boolean hopCountInRange(int hopCount) {
    if (hopCount < 0 || hopCount > MAX_HOP_COUNT_LIMIT)
      return false;
    return true;
  }

  public boolean isReply() {
    return ((m_opcode != NO_OP) && (m_opcode % 2 == 1)) ? true : false;
  }

  public boolean isNamePoll() {
    return m_opcode == NAME_POLL_REQ || m_opcode == NAME_POLL_REP;
  }

  public boolean isContentPoll() {
    return m_opcode == CONTENT_POLL_REQ || m_opcode == CONTENT_POLL_REP;
  }

  public boolean isVerifyPoll() {
    return m_opcode == VERIFY_POLL_REQ || m_opcode == VERIFY_POLL_REP;
  }

  public boolean isNoOp() {
    return m_opcode == NO_OP;
  }

  public String getOpcodeString() {
    return POLL_OPCODES[m_opcode];
  }

  public byte[] getChallenge() {
    return m_challenge;
  }

  public void setChallenge(byte[] b) {
    m_challenge = b;
  }

  public byte[] getVerifier() {
    return m_verifier;
  }

  public void setVerifier(byte[] b) {
    m_verifier = b;
  }

  public byte[] getHashed() {
    return m_hashed;
  }

  public void setHashed(byte[] b) {
    m_hashed = b;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[V1LcapMessage: from ");
    sb.append(m_originatorID);
    sb.append(", ");
    if (isNoOp()) {
      sb.append(POLL_OPCODES[m_opcode]);
    } else {
      sb.append(m_targetUrl);
      sb.append(" ");
      sb.append(m_lwrBound);
      sb.append("-");
      sb.append(m_uprBound);
      sb.append(" ");
      sb.append(POLL_OPCODES[m_opcode]);
      sb.append(" C:");
      sb.append(String.valueOf(B64Code.encode(m_challenge)));
      sb.append(" V:");
      sb.append(String.valueOf(B64Code.encode(m_verifier)));
      if (m_hashed != null) { //can be null for a request message
	sb.append(" H:");
	sb.append(String.valueOf(B64Code.encode(m_hashed)));
      }
      if (m_pollProtocol > 1)
	sb.append(" ver " + m_pollProtocol);
    }
    sb.append("]");
    return sb.toString();
  }

  public String entriesToString(int maxBufSize) {
    StringBuffer buf = new StringBuffer(maxBufSize);

    m_lwrRem = null;
    m_uprRem = null;
    log.debug3("Entries To String max buffer size: " + maxBufSize);
    int entryCount;
    for (entryCount = 0; entryCount < m_entries.size(); entryCount++) {
      // if the length of this entry < max buffer
      byte[] cur_bytes = m_props.encodeString(buf.toString());
      PollTally.NameListEntry entry =
	(PollTally.NameListEntry) m_entries.get(entryCount);
      byte[] entry_bytes = m_props.encodeString(entry.name);
      if (cur_bytes.length + entry_bytes.length < maxBufSize) {
	buf.append(entry.name);
	if (entry.hasContent) {
	  buf.append("\r");
	} else {
	  buf.append("\n");
	}
      } else {
	// we need to set RERemaining and break
	m_lwrRem = entry.name;
	m_uprRem = m_uprBound;
	break;
      }
    }
    log.debug3("Outgoing entries string: " + buf.toString() + " l_rem: "
	       + m_lwrRem + " u_rem: " + m_uprRem);
    log.debug3("Entry count: " + entryCount);
    return buf.toString();
  }

  public ArrayList stringToEntries(String estr) {
    if (estr == null || estr.length() <= 0) {
      return null;
    }
    StringTokenizer tokenizer = new StringTokenizer(estr, "\n\r", true);
    ArrayList entries = new ArrayList();
    while (tokenizer.hasMoreTokens()) {
      String name = tokenizer.nextToken();
      String mark = tokenizer.nextToken();
      boolean hasContent = mark.equals("\r") ? true : false;
      entries.add(new PollTally.NameListEntry(hasContent, name));
    }
    log.debug3("Incoming entries string: " + estr + " l_rem: " + m_lwrRem
	       + " u_rem: " + m_uprRem);
    log.debug3("Entry count: " + entries.size());
    return entries;
  }

  public ArrayList getEntries() {
    return m_entries;
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

  public boolean getMulticast() {
    return m_multicast;
  }

  public void setMulticast(boolean multicast) {
    m_multicast = multicast;
  }

  public void setHopCount(int hopCount) {
    if (hopCount < 0) {
      hopCount = 0;
    } else if (hopCount > MAX_HOP_COUNT_LIMIT) {
      hopCount = MAX_HOP_COUNT_LIMIT;
    }
    m_hopCount = (byte) hopCount;
  }

  public byte getHopCount() {
    return m_hopCount;
  }

  //
  // Factory Methods
  //
  static public V1LcapMessage makeNoOpMsg(PeerIdentity originator,
					  byte[] verifier) throws IOException {
    V1LcapMessage msg = new V1LcapMessage();
    if (msg != null) {
      msg.m_originatorID = originator;
      msg.m_opcode = NO_OP;
      msg.m_hopCount = 0;
      msg.m_verifier = verifier;
      msg.m_pollProtocol = Poll.V1_PROTOCOL;
    }
    msg.storeProps();
    return msg;
  }

  /**
   * make a message to request a poll using a pollspec
   *
   * @param pollspec the pollspec specifying the url and bounds of interest
   * @param entries the array of entries found in the name poll
   * @param challenge the challange bytes
   * @param verifier the verifier bytes
   * @param opcode the kind of poll being requested
   * @param timeRemaining  the time remaining for this poll
   * @param localID the identity of the requestor
   * @return message the new V1LcapMessage
   * @throws IOException if unable to create message
   */
  static public V1LcapMessage makeRequestMsg(PollSpec pollspec,
					     ArrayList entries,
					     byte[] challenge, byte[] verifier,
					     int opcode, long timeRemaining,
					     PeerIdentity localID)
      throws IOException {
    V1LcapMessage msg = new V1LcapMessage(pollspec, entries,
					  challenge, verifier, null, opcode);
    if (msg != null) {
      msg.m_startTime = TimeBase.nowMs();
      msg.m_stopTime = msg.m_startTime + timeRemaining;
      msg.m_originatorID = localID;
    }
    msg.storeProps();
    return msg;
  }

  /**
   * static method to make a reply message
   *
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
  static public V1LcapMessage makeReplyMsg(LcapMessage trigger,
					   byte[] hashedContent,
					   byte[] verifier, ArrayList entries,
					   int opcode, long timeRemaining,
					   PeerIdentity localID)
      throws IOException {
    if (hashedContent == null) {
      log.error("Making a reply message with null hashed content");
    }
    V1LcapMessage msg = new V1LcapMessage((V1LcapMessage) trigger, localID,
					  verifier, hashedContent, entries, opcode);
    if (msg != null) {
      msg.m_startTime = TimeBase.nowMs();
      msg.m_stopTime = msg.m_startTime + timeRemaining;
    }
    msg.storeProps();
    return msg;
  }

  /**
   * a static method to create a new message from a Datagram Packet
   *
   * @param data the data from the message packet
   * @param mcast true if packet was from a multicast socket
   * @return a new Message object
   * @throws IOException
   */
  static public V1LcapMessage decodeToMsg(byte[] data, boolean mcast)
      throws IOException {
    V1LcapMessage msg = new V1LcapMessage(data);
    if (msg != null) {
      msg.m_multicast = mcast;
    }
    return msg;
  }
}
