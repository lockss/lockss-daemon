/*
 * $Id: V3LcapMessage.java,v 1.2 2005-03-18 09:09:18 smorabito Exp $
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
  public static final int MSG_POLL = 0;
  public static final int MSG_POLL_ACK = 1;
  public static final int MSG_POLL_PROOF = 2;
  public static final int MSG_VOTE = 3;
  public static final int MSG_REPAIR_REQ = 4;
  public static final int MSG_REPAIR_REP = 5;
  public static final int MSG_EVALUATION_RECEIPT = 6;
  public static final int MSG_NO_OP = 7;

  public static final String[] POLL_MESSAGES = {
    "Poll", "PollAck", "PollProof", "Vote", "RepairReq",
    "RepairRep", "EvaluationReceipt", "NoOp"
  };

  public static final byte[] pollVersionByte = { '1', '2', '3'};

  private static Logger log = Logger.getLogger("V3LcapMessage");

  private ArrayList m_voteBlocks; // List<V3VoteBlock> of vote blocks.

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
    m_hashAlgorithm = getDefaultHashAlgorithm();
    m_pollVersion = Configuration.getIntParam(PollSpec.PARAM_USE_POLL_VERSION,
					      PollSpec.DEFAULT_USE_POLL_VERSION);
  }

  public V3LcapMessage(int opcode, PeerIdentity origin, String url, long start,
		       long stop, byte ttl, byte[] challenge, byte[] verifier,
		       byte[] hashed) {
    this();
    m_opcode = opcode;
    m_originatorID = origin;
    m_targetUrl = url;
    m_startTime = start;
    m_stopTime = stop;
    m_ttl = ttl;
    m_challenge = challenge;
    m_verifier = verifier;
    m_hashed = hashed;
  }

  public V3LcapMessage(int opcode, PeerIdentity origin, String url, long start,
		       long stop, byte ttl, byte[] challenge, byte[] verifier,
		       byte[] hashed, Collection voteblocks) {
    this(opcode, origin, url, start, stop, ttl, challenge, verifier, hashed);
    if (voteblocks != null) {
      for (Iterator ix = voteblocks.iterator(); ix.hasNext(); ) {
	m_voteBlocks.add((V3LcapMessage.VoteBlock)ix.next());
      }
    }
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
    m_ttl = (byte) m_props.getInt("ttl", 0);
    duration = m_props.getInt("duration", 0) * 1000;
    elapsed = m_props.getInt("elapsed", 0) * 1000;
    m_opcode = m_props.getInt("opcode", -1);
    m_archivalID = m_props.getProperty("au", "UNKNOWN");
    m_targetUrl = m_props.getProperty("url");
    m_challenge = m_props.getByteArray("challenge", EMPTY_BYTE_ARRAY);
    m_verifier = m_props.getByteArray("verifier", EMPTY_BYTE_ARRAY);
    m_hashed = m_props.getByteArray("hashed", EMPTY_BYTE_ARRAY);
    m_pluginVersion = m_props.getProperty("plugVer");

    m_voteBlocks = new ArrayList();

    // Decode the list of vote blocks.
    // encodedVoteBlocks is a list of EncodedProperty objects, each one
    // representing a VoteBlock
    ArrayList encodedVoteBlocks = m_props.getEncodedPropertyList("voteblocks");
    if (encodedVoteBlocks != null) {
      // XXX: Should this ever be allowed?
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
    return getOutputStream().toByteArray();
  }

  /**
   * Obtain an OutputStream representing the encoded bytes of this
   * message.
   */
  public ByteArrayOutputStream getOutputStream() throws IOException {
    storeProps();
    if (!supportedPollVersion(m_pollVersion))
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
    return baos;
  }

  /**
   * Store all properties.
   */
  public void storeProps() throws IOException {
    // make sure the props table is up to date
    try {
      // use port 0 here - it will be ignored in the string
      m_props.put("origIP", m_originatorID.getIdString());
    } catch (NullPointerException npe) {
      throw new ProtocolException("encode - null origin host address.");
    }
    if (m_opcode == MSG_NO_OP) {
      m_props.putInt("opcode", m_opcode);
      m_props.putByteArray("verifier", m_verifier);
      return;
    }
    m_props.setProperty("hashAlgorithm", m_hashAlgorithm);
    m_props.putInt("ttl", m_ttl);
    m_props.putInt("duration", (int) (getDuration() / 1000));
    m_props.putInt("elapsed", (int) (getElapsed() / 1000));
    m_props.putInt("opcode", m_opcode);
    m_props.setProperty("url", m_targetUrl);
    if (m_pluginVersion != null) {
      m_props.setProperty("plugVer", m_pluginVersion);
    }
    if (m_archivalID == null) {
      m_archivalID = "UNKNOWN";
    }
    m_props.setProperty("au", m_archivalID);
    m_props.putByteArray("challenge", m_challenge);
    m_props.putByteArray("verifier", m_verifier);
    if (m_hashed != null) {
      m_props.putByteArray("hashed", m_hashed);
    } else if (isReply()) { // if we're a reply we'd better have a hash
      throw new ProtocolException("encode - missing hash in reply packet.");
    }

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
    if (log.isDebug2()) {
      log.debug2("[storeProps] Storing encodedVoteBlocks: " + encodedVoteBlocks);
    }
    m_props.putEncodedPropertyList("voteblocks", encodedVoteBlocks);
  }

  public String getKey() {
    if (m_key == null) {
      m_key = String.valueOf(B64Code.encode(m_challenge));
    }
    return m_key;
  }

  public boolean isReply() {
    return ((m_opcode == MSG_POLL_ACK) ||
	    (m_opcode == MSG_VOTE) ||
	    (m_opcode == MSG_REPAIR_REP));
  }

  public boolean isNoOp() {
    return m_opcode == MSG_NO_OP;
  }

  public boolean supportedPollVersion(int vers) {
    return vers == 3;
  }

  public String getOpcodeString() {
    return POLL_MESSAGES[m_opcode];
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
                                          byte[] verifier) throws IOException {
    V3LcapMessage msg = new V3LcapMessage();
    msg.m_originatorID = originator;
    msg.m_opcode = MSG_NO_OP;
    msg.m_verifier = verifier;
    msg.m_pollVersion = Configuration.getIntParam(PollSpec.PARAM_USE_POLL_VERSION,
						  PollSpec.DEFAULT_USE_POLL_VERSION);
    return msg;
  }

  /**
   * make a message to request a poll using a pollspec.
   * 
   * @param pollspec the pollspec specifying the url and bounds of interest
   * @param voteblocks the voteblocks found in the message
   * @param challenge the challange bytes
   * @param verifier the verifier bytes
   * @param opcode the kind of poll being requested
   * @param timeRemaining  the time remaining for this poll
   * @param localID the identity of the requestor
   * @return message the new V3LcapMessage
   * @throws IOException if unable to create message
   */
  static public V3LcapMessage makeRequestMsg(PollSpec ps,
					     Collection voteBlocks,
                                             byte[] challenge,
					     byte[] verifier,
                                             int opcode,
					     long timeRemaining,
                                             PeerIdentity origin) 
      throws IOException {
    long now = TimeBase.nowMs();
    V3LcapMessage msg =
      new V3LcapMessage(opcode, origin, ps.getUrl(), now, now + timeRemaining,
			(byte)0, challenge, verifier, null, voteBlocks);
    msg.setArchivalId(ps.getAuId());
    msg.setPollVersion(ps.getPollVersion());
    msg.setPluginVersion(ps.getPluginVersion());
    return msg;
  }

  /**
   * static method to make a reply message
   * 
   * @param trigger the message which trggered the reply
   * @param hashedContent the hashed content bytes
   * @param verifier the veerifier bytes
   * @param opcode an opcode for this message
   * @param timeRemaining the time remaining on the poll
   * @param localID the identity of the requestor
   * @return a new Message object
   * @throws IOException if message construction failed
   */
  static public V3LcapMessage makeReplyMsg(V3LcapMessage trigger,
                                           byte[] hashedContent, byte[] verifier,
                                           int opcode, long timeRemaining,
                                           PeerIdentity origin)
      throws IOException {
    if (hashedContent == null) {
      log.error("Making a reply message with null hashed content");
    }

    long now = TimeBase.nowMs();

    V3LcapMessage msg = 
      new V3LcapMessage(opcode, origin, trigger.getTargetUrl(),
			now, now + timeRemaining,
			trigger.getTimeToLive(), trigger.getChallenge(),
			hashedContent, verifier,
			ListUtil.fromIterator(trigger.getVoteBlockIterator()));
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
      sb.append(" V:");
      sb.append(String.valueOf(B64Code.encode(m_verifier)));
      if (m_hashed != null) { //can be null for a request message
	sb.append(" H:");
	sb.append(String.valueOf(B64Code.encode(m_hashed)));
      }
      sb.append(" B:");
      sb.append(String.valueOf(m_voteBlocks.size()));
      if (m_pollVersion > 1)
	sb.append(" ver " + m_pollVersion);
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * A simple bean representing a V3 vote block -- a file, or part of a file.
   */
  public static class VoteBlock {
    private String m_fileName;
    private int m_filteredLength;
    private int m_filteredOffset;
    private int m_unfilteredLength;
    private int m_unfilteredOffset;
    private byte[] m_plainHash;
    private byte[] m_challengeHash;
    private byte[] m_proof;

    public VoteBlock() { }

    // Convenience constructor for testing.
    public VoteBlock(String fileName, int fLength, int fOffset,
		     int uLength, int uOffset, byte[] plHash,
		     byte[] chHash, byte[] proof) {
      m_fileName = fileName;
      m_filteredLength = fLength;
      m_filteredOffset = fOffset;
      m_unfilteredLength = uLength;
      m_unfilteredOffset = uOffset;
      m_plainHash = plHash;
      m_challengeHash = chHash;
      m_proof = proof;
    }
    

    public String getFileName() {
      return m_fileName;
    }

    public void setFileName(String s) {
      m_fileName = s;
    }

    public int getFilteredLength() {
      return m_filteredLength;
    }

    public void setFilteredLength(int i) {
      m_filteredLength = i;
    }

    public int getFilteredOffset() {
      return m_filteredOffset;
    }

    public void setFilteredOffset(int i) {
      m_filteredOffset = i;
    }

    public int getUnfilteredLength() {
      return m_unfilteredLength;
    }

    public void setUnfilteredLength(int i) {
      m_unfilteredLength = i;
    }

    public int getUnfilteredOffset() {
      return m_unfilteredOffset;
    }

    public void setUnfilteredOffset(int i) {
      m_unfilteredOffset = i;
    }

    public byte[] getPlainHash() {
      return m_plainHash;
    }

    public void setPlainHash(byte[] b) {
      m_plainHash = b;
    }

    public byte[] getChallengeHash() {
      return m_challengeHash;
    }

    public void setChallengeHash(byte[] b) {
      m_challengeHash = b;
    }

    public byte[] getProof() {
      return m_proof;
    }

    public void setProof(byte[] b) {
      m_proof = b;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer("[VoteBlock: ");
      sb.append("fn = " + m_fileName + ", ");
      sb.append("fl = " + m_filteredLength + ", ");
      sb.append("fo = " + m_filteredOffset + ", ");
      sb.append("ul = " + m_unfilteredLength + ", ");
      sb.append("uo = " + m_unfilteredOffset + ", ");
      sb.append("ph = " + 
		m_plainHash == null ? "null" : new String(B64Code.encode(m_plainHash))
		+ ", ");
      sb.append("ch = " +
		m_challengeHash == null ? "null" : new String(B64Code.encode(m_challengeHash))
		+ ", ");
      sb.append("pr = " + 
		m_proof == null ? "null" : new String(B64Code.encode(m_proof))
		+ " ]");
      return sb.toString();
    }

    public boolean equals(Object o) {
      if (o == this) {
	return true;
      }

      if (!(o instanceof VoteBlock)) {
	return false;
      }

      VoteBlock vb = (VoteBlock)o;
      return vb.m_fileName.equals(m_fileName) &&
	vb.m_filteredLength == m_filteredLength &&
	vb.m_filteredOffset == m_filteredOffset &&
	vb.m_unfilteredLength == m_unfilteredLength &&
	vb.m_unfilteredOffset == m_unfilteredOffset &&
	Arrays.equals(vb.m_plainHash, m_plainHash) &&
	Arrays.equals(vb.m_challengeHash, m_challengeHash) &&
	Arrays.equals(vb.m_proof, m_proof);
    }

    public int hashCode() {
      int result = 17;
      result = 37 * result + m_fileName.hashCode();
      result = 37 * result + m_filteredLength;
      result = 37 * result + m_filteredOffset;
      result = 37 * result + m_unfilteredLength;
      result = 37 * result + m_unfilteredOffset;
      for (int i = 0; i < m_plainHash.length; i++) {
	result = 37 * result + m_plainHash[i];
      }
      for (int i = 0; i < m_challengeHash.length; i++) {
	result = 37 * result + m_challengeHash[i];
      }
      for (int i = 0; i < m_proof.length; i++) {
	result = 37 * result + m_proof[i];
      }
      return result;
    }
  }

  /**
   * A Comparator class for V3 VoteBlocks.
   */
  public static class VoteBlockComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      int filteredLength1 = ((V3LcapMessage.VoteBlock)o1).getFilteredLength();
      int filteredLength2 = ((V3LcapMessage.VoteBlock)o2).getFilteredLength();

      if (filteredLength1 > filteredLength2) {
	return 1;
      } else if (filteredLength2 > filteredLength1) {
	return -1;
      }

      // Filtered lengths are equal, compare unfiltered length

      int unfilteredLength1 = ((V3LcapMessage.VoteBlock)o1).getUnfilteredLength();
      int unfilteredLength2 = ((V3LcapMessage.VoteBlock)o2).getUnfilteredLength();

      if (unfilteredLength1 > unfilteredLength2) {
	return 1;
      } else if (unfilteredLength2 > unfilteredLength1) {
	return -1;
      }

      // Unfiltered lengths are equal, compare file names
      
      String fn1 = ((V3LcapMessage.VoteBlock)o1).getFileName();
      String fn2 = ((V3LcapMessage.VoteBlock)o2).getFileName();

      return fn1.compareTo(fn2);
    }
  }
}
