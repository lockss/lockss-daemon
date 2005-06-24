/*
 * $Id: V3LcapMessage.java,v 1.6 2005-06-24 08:09:30 smorabito Exp $
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

import java.security.MessageDigest;
import java.io.*;
import java.security.*;
import java.util.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.poller.*;

import org.mortbay.util.B64Code;

/**
 * Class that encapsulates a V3 LCAP message that has been received
 * or will be sent over the wire.
 *
 * V3 LCAP Messages are not carried over UDP, so their encoded forms
 * are not required to fit in one UDP packet.  They do not have Lower
 * and Upper bounds or remainders like V1 LCAP Messages.
 */
public class V3LcapMessage extends LcapMessage {
  public static final int MSG_POLL = 10;
  public static final int MSG_POLL_ACK = 11;
  public static final int MSG_POLL_PROOF = 12;
  public static final int MSG_NOMINATE = 13;
  public static final int MSG_VOTE_REQ = 14;
  public static final int MSG_VOTE = 15;
  public static final int MSG_REPAIR_REQ = 16;
  public static final int MSG_REPAIR_REP = 17;
  public static final int MSG_EVALUATION_RECEIPT = 18;
  public static final int MSG_NO_OP = 19;

  // XXX: There should be a more general way to do this.
  public static final int POLL_MESSAGES_BASE = 10;
  public static final String[] POLL_MESSAGES = {
    "Poll", "PollAck", "PollProof", "Nominate", "Vote Request",
    "Vote", "RepairReq", "RepairRep", "EvaluationReceipt", "NoOp"
  };

  public static final byte[] pollVersionByte = {'1'};

  private static Logger log = Logger.getLogger("V3LcapMessage");

  // V3 Specific properties.
  private byte[] m_challenge;
  private byte[] m_pollProof;
  
  private List m_voteBlocks; // List<V3VoteBlock> of vote blocks.
  private List m_nominees;   // List of outer circle nominees.

  /*
    byte
    0-3        signature
    4-5        property length
    6-25       SHA-1 hash of encoded properties
    26-End     encoded properties
  */

  /**
   * Construct a new V3LcapMessage.
   */
  public V3LcapMessage() {
    m_props = new EncodedProperty();
    m_voteBlocks = new ArrayList();
    m_pollVersion = Configuration.getIntParam(PollSpec.PARAM_USE_V3_POLL_VERSION,
					      PollSpec.DEFAULT_USE_V3_POLL_VERSION);
  }

  public V3LcapMessage(int opcode, PeerIdentity origin, String url, long start,
		       long stop, byte[] challenge) {
    this();
    m_opcode = opcode;
    m_originatorID = origin;
    m_targetUrl = url;
    m_startTime = start;
    m_stopTime = stop;
    m_challenge = challenge;
  }

  /**
   * Construct a V3LcapMessage from an encoded array of bytes.
   */
  public V3LcapMessage(byte[] encodedBytes) throws IOException {
    this();
    try {
      decodeMsg(encodedBytes);
    } catch (IOException ex) {
      log.error("Unreadable Packet", ex);
      throw new ProtocolException("Unable to decode pkt.");
    }
  }

  /**
   * Construct a V3LcapMessage from an encoded InputStream.
   */
  public V3LcapMessage(InputStream inputStream) throws IOException {
    this();
    try {
      decodeMsg(inputStream);
    } catch (IOException ex) {
      log.error("Unreadable Packet", ex);
      throw new ProtocolException("Unable to decode pkt.");
    }
  }

  /**
   * Build out this message from an InputStream.
   *
   * @param is An input stream from which the message bytes
   *           can be read.
   */
  public void decodeMsg(InputStream is) throws IOException {
    long duration;
    long elapsed;
    String addr;
    int port;
    // the mutable stuff
    DataInputStream dis = new DataInputStream(is);

    // read in the three header bytes
    for (int i = 0; i < signature.length; i++) {
      if (signature[i] != dis.readByte()) {
	throw new ProtocolException("Invalid Signature");
      }
    }

    // read in the poll version byte and decode
    m_pollVersion = -1;
    byte ver = dis.readByte();
    for (int i = 0; i < pollVersionByte.length; i++) {
      if (pollVersionByte[i] == ver)
	m_pollVersion = i + 1;
    }

    if (m_pollVersion <= 0) {
      throw new ProtocolException("Unsupported inbound poll version: " + ver);
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
    port = m_props.getInt("origPort", -1);
    String addr_str = m_props.getProperty("origIP");
    m_originatorID = m_idManager.stringToPeerIdentity(addr_str);
    m_hashAlgorithm = m_props.getProperty("hashAlgorithm");
    duration = m_props.getInt("duration", 0) * 1000;
    elapsed = m_props.getInt("elapsed", 0) * 1000;
    m_opcode = m_props.getInt("opcode", -1);
    m_archivalID = m_props.getProperty("au", "UNKNOWN");
    m_targetUrl = m_props.getProperty("url");
    m_challenge = m_props.getByteArray("challenge", EMPTY_BYTE_ARRAY);
    m_pollProof = m_props.getByteArray("effortproof", EMPTY_BYTE_ARRAY);
    m_pluginVersion = m_props.getProperty("plugVer");
    String nomineesString = m_props.getProperty("nominees");
    if (nomineesString != null) {
      m_nominees = StringUtil.breakAt(nomineesString, ';');
    }

    m_voteBlocks = new ArrayList();

    // Decode the list of vote blocks.
    // encodedVoteBlocks is a list of EncodedProperty objects, each one
    // representing a VoteBlock
    List encodedVoteBlocks = m_props.getEncodedPropertyList("voteblocks");
    if (encodedVoteBlocks != null) {
      for (Iterator ix = encodedVoteBlocks.iterator(); ix.hasNext(); ) {
	EncodedProperty vbProps = (EncodedProperty)ix.next();
	VoteBlock vb = new VoteBlock();
	vb.setFileName(vbProps.getProperty("fn"));
	vb.setFilteredLength(vbProps.getInt("fl", 0));
	vb.setUnfilteredLength(vbProps.getInt("ul", 0));
	vb.setFilteredOffset(vbProps.getInt("fo", 0));
	vb.setUnfilteredOffset(vbProps.getInt("uo", 0));
	vb.setPlainHash(vbProps.getByteArray("ph", EMPTY_BYTE_ARRAY));
	vb.setChallengeHash(vbProps.getByteArray("ch", EMPTY_BYTE_ARRAY));
	vb.setProof(vbProps.getByteArray("pr", EMPTY_BYTE_ARRAY));
	m_voteBlocks.add(vb);
      }
    }

    // calculate start and stop times
    long now = TimeBase.nowMs();
    m_startTime = now - elapsed;
    m_stopTime = now + duration;
  }

  /**
   * Build out this message from a byte array.
   *
   * @param encodedBytes The encoded byte array representing this message.
   */
  public void decodeMsg(byte[] encodedBytes) throws IOException {
    this.decodeMsg(new ByteArrayInputStream(encodedBytes));
  }

  public byte[] encodeMsg() throws IOException {
    storeProps();
    if (!isPollVersionSupported(m_pollVersion))
      throw new ProtocolException("Unsupported outbound poll version: "
				  + m_pollVersion);
    byte[] prop_bytes = m_props.encode();
    byte[] hash_bytes = computeHash(prop_bytes);
    int enc_len = prop_bytes.length + hash_bytes.length + 6; // msg header is 6 bytes
    ByteArrayOutputStream baos = new ByteArrayOutputStream(enc_len);
    DataOutputStream dos = new DataOutputStream(baos);
    dos.write(signature);
    dos.write(pollVersionByte[m_pollVersion - 1]);
    dos.writeShort(prop_bytes.length);
    dos.write(hash_bytes);
    dos.write(prop_bytes);
    return baos.toByteArray();
  }

  /**
   * Obtain an InputStream from which the bytes of this message can
   * be read.
   */
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(encodeMsg());
  }

  /**
   * Store all properties.
   */
  public void storeProps() throws IOException {
    // make sure the props table is up to date
    try {
      // PeerIdentity.getIdString() returns an IP:Port string.
      m_props.put("origIP", m_originatorID.getIdString());
      // m_props.put("origPort", m_originatorID.getIdString());
    } catch (NullPointerException npe) {
      throw new ProtocolException("encode - null origin host address.");
    }
    if (m_opcode == MSG_NO_OP) {
      m_props.putInt("opcode", m_opcode);
      m_props.putByteArray("challenge", m_challenge);
      return;
    }
    m_props.setProperty("hashAlgorithm", getHashAlgorithm());
    m_props.putInt("duration", (int) (getDuration() / 1000));
    m_props.putInt("elapsed", (int) (getElapsed() / 1000));
    m_props.putInt("opcode", m_opcode);
    m_props.setProperty("url", m_targetUrl);
    if (m_pluginVersion != null) {
      m_props.setProperty("plugVer", m_pluginVersion);
    }
    if (m_archivalID == null) {
      throw new ProtocolException("Null AU ID not allowed.");
    }
    m_props.setProperty("au", m_archivalID);
    m_props.putByteArray("challenge", m_challenge);
    if(m_pollProof != null) {
      m_props.putByteArray("effortproof", m_pollProof);
    }
    if (m_nominees != null) {
      m_props.setProperty("nominees",
			  StringUtil.separatedString(m_nominees, ";"));
    }

    // XXX: These should eventually be refactored out of the encoded
    // property object.  The large size of some AUs will quickly lead
    // to memory exhaustion if a lot of EncodedProperty objects full
    // of VoteBlocks are hanging around.

    // Store the vote block list
    ArrayList encodedVoteBlocks = new ArrayList();
    Iterator ix = getVoteBlockIterator();
    while(ix.hasNext()) {
      VoteBlock vb = (VoteBlock)ix.next();
      EncodedProperty vbProps = new EncodedProperty();
      vbProps.setProperty("fn", vb.getFileName());
      vbProps.putInt("fl", vb.getFilteredLength());
      vbProps.putInt("fo", vb.getFilteredOffset());
      vbProps.putInt("ul", vb.getUnfilteredLength());
      vbProps.putInt("uo", vb.getUnfilteredOffset());
      vbProps.putByteArray("ph", vb.getPlainHash());
      vbProps.putByteArray("ch", vb.getChallengeHash());
      vbProps.putByteArray("pr", vb.getProof());
      encodedVoteBlocks.add(vbProps);
    }
    if (log.isDebug3()) {
      log.debug3("[storeProps] Storing encodedVoteBlocks: " + encodedVoteBlocks);
    }
    m_props.putEncodedPropertyList("voteblocks", encodedVoteBlocks);
  }

  public String getKey() {
    if (m_key == null) {
      m_key = String.valueOf(B64Code.encode(m_challenge));
    }
    return m_key;
  }

  public byte[] getPollProof() {
    return m_pollProof;
  }

  public void setPollProof(byte[] b) {
    m_pollProof = b;
  }

  public boolean isNoOp() {
    return m_opcode == MSG_NO_OP;
  }

  public boolean isPollVersionSupported(int vers) {
    return (vers > 0 && vers <= pollVersionByte.length);
  }

  public String getOpcodeString() {
    return POLL_MESSAGES[m_opcode - POLL_MESSAGES_BASE];
  }

  public byte[] getChallenge() {
    return m_challenge;
  }

  public void setChallenge(byte[] b) {
    m_challenge = b;
  }

  public List getNominees() {
    return this.m_nominees;
  }

  public void setNominees(List nominees) {
    this.m_nominees = nominees;
  }

  // Vote Block accessors and iterator
  //
  // NOTE: For now, the list of vote blocks is implemented as an in-memory
  // array list.  It will be desirable to refactor this into on-disk storage
  // because of the size of this list
  //

  public void addVoteBlock(VoteBlock vb) {
    m_voteBlocks.add(vb);
  }

  public Iterator getVoteBlockIterator() {
    return m_voteBlocks.iterator();
  }

  public void sortVoteBlocks(Comparator c) {
    Collections.sort(m_voteBlocks, c);
  }

  //
  // Factory Methods
  //
  static public V3LcapMessage makeNoOpMsg(PeerIdentity originator,
					  byte[] challenge) throws IOException {
    V3LcapMessage msg = new V3LcapMessage();
    msg.m_originatorID = originator;
    msg.m_opcode = MSG_NO_OP;
    msg.m_challenge = challenge;
    msg.m_pollVersion = Configuration.getIntParam(PollSpec.PARAM_USE_V3_POLL_VERSION,
						  PollSpec.DEFAULT_USE_V3_POLL_VERSION);
    return msg;
  }

  /**
   * Make a NoOp message with randomly generated bytes.
   */
  static public V3LcapMessage makeNoOpMsg(PeerIdentity originator) throws IOException {
    return V3LcapMessage.makeNoOpMsg(originator, ByteArray.makeRandomBytes(20));
  }

  /**
   * make a message to request a poll using a pollspec.
   *
   * @param ps the pollspec specifying the url and bounds of interest
   * @param challenge the challange bytes
   * @param opcode the kind of poll being requested
   * @param timeRemaining  the time remaining for this poll
   * @param origin the identity of the requestor
   * @return message the new V3LcapMessage
   * @throws IOException if unable to create message
   */
  static public V3LcapMessage makeRequestMsg(PollSpec ps,
					     byte[] challenge,
					     int opcode,
					     long timeRemaining,
					     PeerIdentity origin)
      throws IOException {
    long now = TimeBase.nowMs();
    V3LcapMessage msg =
      new V3LcapMessage(opcode, origin, ps.getUrl(), now,
			now + timeRemaining, challenge);
    msg.setArchivalId(ps.getAuId());
    msg.setPollVersion(ps.getPollVersion());
    msg.setPluginVersion(ps.getPluginVersion());
    return msg;
  }

  /**
   * static method to make a reply message
   *
   * @param trigger the message which trggered the reply
   * @param opcode an opcode for this message
   * @param timeRemaining the time remaining on the poll
   * @param origin the identity of the requestor
   * @return a new Message object
   * @throws IOException if message construction failed
   */
  static public V3LcapMessage makeReplyMsg(V3LcapMessage trigger,
					   int opcode, long timeRemaining,
					   PeerIdentity origin)
      throws IOException {
    long now = TimeBase.nowMs();

    V3LcapMessage msg =
      new V3LcapMessage(opcode, origin,
			trigger.getTargetUrl(),
			now, now + timeRemaining,
			trigger.getChallenge());
    List voteBlocks = ListUtil.fromIterator(trigger.getVoteBlockIterator());
    for (Iterator ix = voteBlocks.iterator(); ix.hasNext(); ) {
      msg.addVoteBlock((VoteBlock)ix.next());
    }
    msg.setHashAlgorithm(trigger.getHashAlgorithm());
    msg.setArchivalId(trigger.getArchivalId());
    msg.setPollVersion(trigger.getPollVersion());
    msg.setPluginVersion(trigger.getPluginVersion());
    return msg;
  }


  // XXX: The implementation of getting the count of vote blocks
  //      will have to change when the underlying structure is
  //      refactored from an in-memory arraylist to a
  //      disk backed structure.
  //
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[V3LcapMessage: from ");
    sb.append(m_originatorID);
    sb.append(", ");
    if (isNoOp()) {
      sb.append(getOpcodeString());
    } else {
      sb.append(m_targetUrl);
      sb.append(" ");
      sb.append(getOpcodeString());
      sb.append(" C:");
      sb.append(String.valueOf(B64Code.encode(m_challenge)));
      sb.append(" B:");
      sb.append(String.valueOf(m_voteBlocks.size()));
      sb.append(" ver " + m_pollVersion);
    }
    sb.append("]");
    return sb.toString();
  }
}
