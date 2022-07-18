/*
 * $Id$
 */

/*
 Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import org.mortbay.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.util.*;
import org.lockss.util.StringUtil;

/**
 * Class that encapsulates a V3 LCAP message that has been received or will be
 * sent over the wire.
 *
 * V3 LCAP Messages are not carried over UDP, so their encoded forms are not
 * required to fit in one UDP packet. They do not have Lower and Upper bounds
 * or remainders like V1 LCAP Messages.
 */
public class V3LcapMessage extends LcapMessage implements LockssSerializable {
  
  /** Maximum allowable size of repair data before storing on disk */
  public static final String PARAM_REPAIR_DATA_THRESHOLD =
    Configuration.PREFIX + "poll.v3.repairDataThreshold";
  public static final int DEFAULT_REPAIR_DATA_THRESHOLD = 1024*32; // 32K

  static final int EST_ENCODED_HEADER_LENGTH = 100;

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

  public static final int POLL_MESSAGES_BASE = 10;
  public static final String[] POLL_MESSAGES = { "Poll", "PollAck", "PollProof",
    "Nominate", "VoteRequest", "Vote", "RepairReq", "RepairRep",
    "EvaluationReceipt", "NoOp" };
  private static Logger log = Logger.getLogger("V3LcapMessage");
  
  
  // Poll rejection codes.
  public static enum PollNak { 
    NAK_GROUP_MISMATCH {
      public String toString() { return "Wrong Group"; }
    },
    NAK_VERSION_MISMATCH {
      public String toString() { return "Incompatible minor poll version"; }
    },
    NAK_NO_TIME {
      public String toString() { return "Too Busy"; }
    },
    NAK_UNKNOWN {
      public String toString() { return "Unknown"; }
    },
    NAK_NO_AU {
      public String toString() { return "No AU"; }
    },
    NAK_NO_SUBSTANCE {
      public String toString() {
	return "AU has no files containing substantial content"; }
    },
    NAK_PLUGIN_VERSION_MISMATCH {
      public String toString() { return "Plugin Version Mismatch"; }
    },
    NAK_DISABLED {
      public String toString() { return "Disabled"; }
    },
    NAK_NOT_CRAWLED {
      public String toString() { return "Not crawled"; }
    },
    NAK_TOO_MANY_VOTERS {
      public String toString() { return "Voter Limit"; }
    },
    NAK_HASH_TIMEOUT {
      public String toString() { return "Hash Timeout"; }
    },
    NAK_HASH_ERROR {
      public String toString() { return "Hash Error"; }
    },
    NAK_INSUFFICIENT_VOTE_DURATION {
      public String toString() { return "Insufficient Vote Duration"; }
    },
    NAK_HAVE_SUFFICIENT_REPAIRERS {
      public String toString() { return "AU is safe"; }
    },
    NAK_NOT_READY {
      public String toString() { return "Not ready"; }
    }
   };

  // V3 Protocol revision history:
  //  5 - Changed HtmlParser to scan the content of <script> tags
  //      permissively, as browsers do.  See
  //      HtmlFilterInputStream.setupHtmlParser()
  //  4 - URL included in hash (after challenge, before content)
  //  3 - The internal representation of VoteBlocks has changed.  Vote
  //      Blocks carry multiple versions, and we don't want older daemons
  //      to participate in any polls with versioned content.  Shipped with
  //      daemon 1.17.
  //  2 - Removed VoteBlocks from the encoded properties.  They are now
  //      encoded directly into the stream, like repair data.  Shipped with
  //      daemon 1.16.
  //  1 - Initial Release.  Shipped with daemon 1.12.
  public static final int V3_PROTOCOL_R1 = 1;
  public static final int V3_PROTOCOL_R2 = 2;
  public static final int V3_PROTOCOL_R3 = 3;
  public static final int V3_PROTOCOL_R4 = 4;
  public static final int V3_PROTOCOL_R5 = 5;
  // Don't use this directly, use getSupportedProtocolRev()
  private static final int V3_PROTOCOL_REV = V3_PROTOCOL_R5;

  /** Poll minor version from received message */
  private int m_minorVersion = V3_PROTOCOL_REV;

  /** The poller nonce bytes generated by the poller. */
  private byte[] m_pollerNonce;

  /** The voter nonce bytes generated by a poll participant. */
  private byte[] m_voterNonce;

  /** The voter nonce bytes generated by a participant in a symmetric poll. */
  private byte[] m_voterNonce2;

  /** The effort proof for this message (if any). */
  private byte[] m_effortProof;

  /** The modulus used to select URLs in a Proof of Possession poll */
  private int m_modulus;

  /** The nonce used to select URLs in a Proof of Possession poll */
  private byte[] m_sampleNonce;

  /** In Vote messages: A list of vote blocks for this vote. */
  VoteBlocks m_voteBlocks;
  
  /*
   * Note:  voteDeadline has been deprecated in favor of voteDuration.  
   * voteDeadline should be removed in a few releases (as of daemon release
   * 1.22)
   */
  
  /** For poll messages, the time by which a participant must have voted. 
   * @deprecated */
  transient long m_voteDeadline;

  /** For poll messages, the time left until a participant must have voted. */
  long m_voteDuration;
  
  /** NAK reason, if this is a NAK message. */
  private PollNak m_nak;

  /** The platform groups to which the sender of the message belongs. */
  private String m_groups;
  
  /** The time by which this message must be received in order to be
   * processed.  (E.g., the vote deadline, for a vote message.)  This field
   * is not part of the transmitted message; it's used by the sending
   * daemon to determine when the message should be discarded if it hasn't
   * yet been sent. */
  private long m_msgExpiration = 0;
  
  /** The maximum number of times to requeue this message if can't
   * establish a connection. */
  private int m_msgRetryMax = -1;
  
  /** The target interval after which to retry. */
  private long m_msgRetryInterval = 0;
  
  /**
   * In Nominate messages: The list of outer circle nominees, in the form of
   * peer identity strings.
   */
  private List m_nominees;

  /**
   * In Vote messages: True if all vote blocks have been sent, false otherwise.
   */
  private boolean m_voteComplete = false;

  /**
   * Repair Data.
   */
  private EncodedProperty m_repairProps;
  
  /**
   * A hint sent in the receipt for a poll indicating what level of agreement
   * the poller had with a participant.
   */
  private double m_agreementHint = -1.0;

  /**
   * A hint sent in the receipt for a poll indicating what level of
   * weightedAgreement the poller had with a participant.
   */
  private double m_weightedAgreementHint = -1.0;

  // InputStream from which to read repair data when encoding this message.
  private transient InputStream m_repairDataInputStream;
  private long m_repairDataLen = 0;  // Size of the repair data payload (if any)

  /** File used to store vote blocks, repair data, etc. */
  private transient File m_messageDir;

  private String m_repairDataFilePath; // If not null, repair data is on disk
  private byte[] m_repairDataByteArray; // If not null, repair data is in memory

  private int m_repairDataThreshold = DEFAULT_REPAIR_DATA_THRESHOLD;
 
  // Reference to the running daemon.  Must be restored post-serialization by
  // the postUnmarshal method.
  private transient LockssApp m_daemon; 

  /**
   * In Vote Request messages: the URL of the last vote block received. Null if
   * this is the first (or only) request.
   */
  private String m_lastVoteBlockURL;

  /*
   * Common to all versions:
   *
   *  bytes  0 -   2:  Signature ('lpm' in ASCII) (3 bytes)
   *  byte         3:  Protocol major version (1 byte)
   *
   * Fixed length V3 fields:
   *
   *  bytes        4:  Protocol minor version (1 byte)
   *  bytes  5  - 24:  SHA-1 hash of encoded properties (20 bytes)
   *  bytes  25 - 28:  LCAP Message Properties length (4 bytes)
   *  bytes  29 - 36:  VoteBlocks Properties length (8 bytes)
   *
   */

  /**
   * Construct a new V3LcapMessage.
   */
  public V3LcapMessage(File messageDir, LockssApp daemon) {
    m_daemon = daemon;
    m_props = new EncodedProperty();
    m_pollProtocol = Poll.V3_PROTOCOL;
    m_messageDir = messageDir;
    m_groups = ConfigManager.getPlatformGroups();
    m_repairDataThreshold =
      CurrentConfig.getIntParam(PARAM_REPAIR_DATA_THRESHOLD,
                                DEFAULT_REPAIR_DATA_THRESHOLD);
    m_modulus = 0;
    m_voterNonce2 = null;
    m_sampleNonce = null;
  }

  public V3LcapMessage(String auId, String pollKey, String pluginVersion,
                       byte[] pollerNonce, byte[] voterNonce,
		       byte[] voterNonce2, int opcode,
                       long deadline, PeerIdentity origin, File messageDir,
                       LockssApp daemon) {
    this(messageDir, daemon);
    m_archivalID = auId;
    m_startTime = TimeBase.nowMs();
    m_stopTime = deadline;
    m_key = pollKey;
    m_pluginVersion = pluginVersion;
    m_pollerNonce = pollerNonce;
    m_voterNonce = voterNonce;
    m_voterNonce2 = voterNonce2;
    m_opcode = opcode;
    m_modulus = 0;
    m_sampleNonce = null;
    m_originatorID = origin;
  }

  public V3LcapMessage(String auId, String pollKey, String pluginVersion,
                       byte[] pollerNonce, byte[] voterNonce, int opcode,
                       long deadline, PeerIdentity origin, File messageDir,
                       LockssApp daemon) {
    this(messageDir, daemon);
    m_archivalID = auId;
    m_startTime = TimeBase.nowMs();
    m_stopTime = deadline;
    m_key = pollKey;
    m_pluginVersion = pluginVersion;
    m_pollerNonce = pollerNonce;
    m_voterNonce = voterNonce;
    m_voterNonce2 = null;
    m_opcode = opcode;
    m_modulus = 0;
    m_sampleNonce = null;
    m_originatorID = origin;
  }

  public V3LcapMessage(String auId, String pollKey, String pluginVersion,
                       byte[] pollerNonce, byte[] voterNonce, int opcode,
		       int modulus, byte[] sampleNonce,
                       long deadline, PeerIdentity origin, File messageDir,
                       LockssApp daemon) {
    this(messageDir, daemon);
    m_archivalID = auId;
    m_startTime = TimeBase.nowMs();
    m_stopTime = deadline;
    m_key = pollKey;
    m_pluginVersion = pluginVersion;
    m_pollerNonce = pollerNonce;
    m_voterNonce = voterNonce;
    m_opcode = opcode;
    m_modulus = modulus;
    if (modulus != 0) {
      log.debug3("Message modulus: " + modulus);
    }
    m_sampleNonce = sampleNonce;
    m_originatorID = origin;
  }

  public V3LcapMessage(String auId, String pollKey, String pluginVersion,
                       byte[] pollerNonce, byte[] voterNonce,
		       byte[] voterNonce2, int opcode,
		       int modulus, byte[] sampleNonce,
                       long deadline, PeerIdentity origin, File messageDir,
                       LockssApp daemon) {
    this(messageDir, daemon);
    m_archivalID = auId;
    m_startTime = TimeBase.nowMs();
    m_stopTime = deadline;
    m_key = pollKey;
    m_pluginVersion = pluginVersion;
    m_pollerNonce = pollerNonce;
    m_voterNonce = voterNonce;
    m_voterNonce2 = voterNonce2;
    m_opcode = opcode;
    if (modulus != 0) {
      log.debug3("Message modulus: " + modulus);
    }
    m_sampleNonce = sampleNonce;
    m_originatorID = origin;
  }

  /**
   * Construct a V3LcapMessage from an encoded array of bytes.
   */
  public V3LcapMessage(byte[] encodedBytes, File messageDir, LockssApp daemon)
      throws IOException {
    this(new ByteArrayInputStream(encodedBytes), messageDir, daemon);
  }

  /**
   * Construct a V3LcapMessage from an encoded InputStream.
   */
  public V3LcapMessage(InputStream inputStream, File messageDir,
                       LockssApp daemon)
      throws IOException {
    this(messageDir, daemon);
    try {
      decodeMsg(inputStream);
    } catch (ProtocolException ex) {
      if (log.isDebug3()) {
	log.error("Unreadable Packet", ex);
      } else {
	log.error("Unreadable Packet: " + ex);
      }
      throw ex;
    } catch (IOException ex) {
      log.error("Unreadable Packet", ex);
      throw new ProtocolException("Unable to decode pkt.", ex);
    }
  }

  /** Method suitable for unit tests. */
  public int getSupportedProtocolRev() {
    return V3_PROTOCOL_REV;
  }

  /**
   * Build out this message from an InputStream.
   *
   * @param is An input stream from which the message bytes can be read.
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

    // Protocol major version
    int majorVersion = dis.readByte();
    // Protocol minor version
    m_minorVersion = dis.readByte();

    if (majorVersion != Poll.V3_POLL) {
      throw new ProtocolException("Unsupported inbound protocol: " +
                "major=" + majorVersion + ", minor=" + m_minorVersion);
    }

    // Encoded LCAP Properties

    // SHA-1 of the encoded LCAP Properties
    byte[] hash_bytes = new byte[SHA_LENGTH];
    dis.read(hash_bytes);
    // Length of the encoded LCAP Properties.
    int lcapPropLen = dis.readInt();
    // Encoded LCAP Properties.
    byte[] lcapProps = new byte[lcapPropLen];
    dis.read(lcapProps);
    // Verify the hash of the prop_bytes
    if (!verifyHash(hash_bytes, lcapProps)) {
      throw new ProtocolException("Hash verification failed.");
    }
    // Decode into the LCAP Properties data structure
    m_props.decode(lcapProps);

    // the immutable stuff
    m_key = m_props.getProperty("key");
    String addr_str = m_props.getProperty("origId");
    m_originatorID = m_idManager.stringToPeerIdentity(addr_str);
    m_hashAlgorithm = m_props.getProperty("hashAlgorithm");
    duration = m_props.getInt("duration", 0) * 1000L;
    elapsed = m_props.getInt("elapsed", 0) * 1000L;
    m_opcode = m_props.getInt("opcode", -1);
    m_archivalID = m_props.getProperty("au", "UNKNOWN");
    m_targetUrl = m_props.getProperty("url");
    m_pollerNonce = m_props.getByteArray("pollerNonce", ByteArray.EMPTY_BYTE_ARRAY);
    m_voterNonce = m_props.getByteArray("voterNonce", ByteArray.EMPTY_BYTE_ARRAY);
    m_voterNonce2 = m_props.getByteArray("voterNonce2", ByteArray.EMPTY_BYTE_ARRAY);
    m_effortProof = m_props.getByteArray("effortproof", ByteArray.EMPTY_BYTE_ARRAY);
    m_pluginVersion = m_props.getProperty("plugVer");

    // V3 specific message parameters
    String nomineesString = m_props.getProperty("nominees");
    if (nomineesString != null) {
      m_nominees = StringUtil.breakAt(nomineesString, ',');
    }
    m_voteDuration = m_props.getLong("voteduration", 0);
    m_lastVoteBlockURL = m_props.getProperty("lastvoteblockurl");
    m_voteComplete = m_props.getBoolean("votecomplete", false);
    m_repairProps = m_props.getEncodedProperty("repairProps");
    m_agreementHint = m_props.getDouble("agreementHint", -1.0);
    m_weightedAgreementHint = m_props.getDouble("weightedAgreementHint", -1.0);
    m_groups = m_props.getProperty("groups");
    m_modulus = m_props.getInt("modulus", 0);
    if (m_modulus != 0) {
      log.debug3("V3LcapMessage with modulus: " + m_modulus);
    }
    m_sampleNonce = m_props.getByteArray("sampleNonce", ByteArray.EMPTY_BYTE_ARRAY);
    String nakString = m_props.getProperty("nak");
    if (nakString != null) {
      try {
	m_nak = PollNak.valueOf(nakString);
      } catch (IllegalArgumentException e) {
	log.warning("Unknown nak: " + nakString);
	m_nak = PollNak.NAK_UNKNOWN;
      }
    }
    
    // If we have vote blocks, pass them to a VoteBlock object.
    int voteBlockCount = dis.readInt();
    m_repairDataLen = dis.readLong();

    if (voteBlockCount > 0) {
      // Find the directory associated with this poll's state.
      File stateDir =
        ((LockssDaemon)m_daemon).getPollManager().getStateDir(m_key);
      // If no state dir, this poll has ended; don't store voteblocks.
      // (They would get stored in the system tempdir, where they would
      // accumulate because nothing knows to delete them.)
      if (stateDir != null) {
	m_voteBlocks = new DiskVoteBlocks(voteBlockCount, dis, stateDir);
      }
    }

    // Read in the Repair Data, if any
    if (m_repairDataLen > 0) {
      if (m_repairDataLen > m_repairDataThreshold) {
        File repairDataFile;
        repairDataFile = FileUtil.createTempFile("v3lcapmessage_repair-",
                                                 ".data",
                                                 m_messageDir);
        log.debug2("Creating V3LcapMessage Repair Data File: " +
                   repairDataFile);
        FileOutputStream out = new FileOutputStream(repairDataFile);
        long copied = StreamUtil.copy(dis, out, m_repairDataLen);
        m_repairDataFilePath = repairDataFile.getAbsolutePath();
        IOUtil.safeClose(out);
      } else {
        // Read Into Memory
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long copied = StreamUtil.copy(dis, out, m_repairDataLen);
        m_repairDataByteArray = out.toByteArray();
        IOUtil.safeClose(out);
      }
    }

    // Safe to close the stream now.
    IOUtil.safeClose(dis);

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
    // This method is deprecated for V3LcapMessage
    throw new UnsupportedOperationException("Cannot encode as byte array.");
  }

  public InputStream getInputStream() throws IOException {
    storeProps();
    // This is a Vector only because SequenceInputStream requires an
    // Enumeration.  Old school!
    Vector inputStreams = new Vector();

    // Convert small-sized fields into in-memory byte arrays.
    byte[] lcapPropBytes = m_props.encode();
    byte[] hashBytes = computeHash(lcapPropBytes);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(bos);
    dos.write(signature);
    dos.writeByte(Poll.V3_POLL);
    dos.writeByte(getSupportedProtocolRev());
    // LCAP properties hash
    dos.write(hashBytes);
    // LCAP properties size
    dos.writeInt(lcapPropBytes.length);
    // LCAP properties
    dos.write(lcapPropBytes);
    // Number of encoded vote blocks.
    dos.writeInt(m_voteBlocks == null ? 0 : m_voteBlocks.size());
    // Size of repair data, if any.
    dos.writeLong(m_repairDataLen);

    // Now convert the header byte array into a ByteArrayInputStream.
    // Should all fit comfortably into memory.
    ByteArrayInputStream headerInputStream =
      new ByteArrayInputStream(bos.toByteArray());
    inputStreams.add(headerInputStream);

    if (m_voteBlocks != null && m_voteBlocks.size() > 0) {
      inputStreams.add(m_voteBlocks.getInputStream());
    }

    if (m_repairDataLen > 0) {
      inputStreams.add(m_repairDataInputStream);
    }

    return new SequenceInputStream(inputStreams.elements());
  }

  /**
   * Store all properties.
   */
  public void storeProps() throws IOException {
    // make sure the props table is up to date
    try {
      // PeerIdentity.getIdString() returns an IP:Port string.
      m_props.put("origId", m_originatorID.getIdString());
    } catch (NullPointerException npe) {
      throw new ProtocolException("encode - null origin host address.");
    }
    if (m_opcode == MSG_NO_OP) {
      m_props.putInt("opcode", m_opcode);
      if (m_pollerNonce != null) {
        m_props.putByteArray("pollerNonce", m_pollerNonce);
      }
      if (m_voterNonce != null) {
        m_props.putByteArray("voterNonce", m_voterNonce);
      }
      if (m_voterNonce2 != null && m_voterNonce2.length > 0) {
        m_props.putByteArray("voterNonce2", m_voterNonce2);
      }
      return;
    }
    m_props.setProperty("hashAlgorithm", getHashAlgorithm());
    m_props.putInt("duration", (int) (getDuration() / 1000));
    m_props.putInt("elapsed", (int) (getElapsed() / 1000));
    m_props.setProperty("key", m_key);
    m_props.putInt("opcode", m_opcode);
    m_props.putInt("modulus", m_modulus);
    if (m_modulus != 0) {
      log.debug3("V3LcapMessage sent modulus: " + m_modulus);
    }
    if (m_sampleNonce != null && m_sampleNonce.length > 0) {
      m_props.putByteArray("sampleNonce", m_sampleNonce);
    }
    if (m_targetUrl != null) {
      m_props.setProperty("url", m_targetUrl);
    }
    if (m_pluginVersion != null) {
      m_props.setProperty("plugVer", m_pluginVersion);
    }
    if (m_archivalID == null) {
      throw new ProtocolException("Null AU ID not allowed.");
    }
    m_props.setProperty("au", m_archivalID);
    if (m_pollerNonce != null) {
      m_props.putByteArray("pollerNonce", m_pollerNonce);
    }
    if (m_voterNonce != null) {
      m_props.putByteArray("voterNonce", m_voterNonce);
    }
    if (m_voterNonce2 != null && m_voterNonce2.length > 0) {
      m_props.putByteArray("voterNonce2", m_voterNonce2);
    }

    // V3 specific message parameters.

    m_props.putLong("votedeadline", m_voteDeadline);
    m_props.putLong("voteduration", m_voteDuration);
    m_props.setProperty("groups", m_groups);
    if (m_nak != null) {
      m_props.setProperty("nak", m_nak.name());
    }
    if (m_effortProof != null) {
      m_props.putByteArray("effortproof", m_effortProof);
    }
    if (m_nominees != null) {
      m_props.setProperty("nominees",
                          StringUtil.separatedString(m_nominees, ","));
    }
    if (m_lastVoteBlockURL != null) {
      m_props.setProperty("lastvoteblockurl", m_lastVoteBlockURL);
    }
    m_props.putBoolean("votecomplete", m_voteComplete);
    if (m_repairProps != null) {
      m_props.putEncodedProperty("repairProps", m_repairProps);
    }
    if (m_agreementHint >= 0.0) {
      m_props.putDouble("agreementHint", m_agreementHint);
    }
    if (m_weightedAgreementHint >= 0.0) {
      m_props.putDouble("weightedAgreementHint", m_weightedAgreementHint);
    }
  }

  /**
   * Return the unique identifying Poll Key for this poll.
   *
   * @return The unique poll identifier for this poll.
   */
  public String getKey() {
    return m_key;
  }

  public void setKey(String key) {
    m_key = key;
  }

  /**
   * Return an effort proof.
   *
   * @return The effort proof for this message.
   */
  public byte[] getEffortProof() {
    return m_effortProof;
  }

  public void setEffortProof(byte[] b) {
    m_effortProof = b;
  }

  public String getTargetUrl() {
    return m_targetUrl;
  }

  public void setTargetUrl(String url) {
    m_targetUrl = url;
  }

  public boolean isNoOp() {
    return m_opcode == MSG_NO_OP;
  }

  public String getOpcodeString() {
    return POLL_MESSAGES[m_opcode - POLL_MESSAGES_BASE];
  }

  public int getMinorVersion() {
    return m_minorVersion;
  }

  public byte[] getPollerNonce() {
    return m_pollerNonce;
  }

  public void setPollerNonce(byte[] b) {
    m_pollerNonce = b;
  }

  public byte[] getVoterNonce() {
    return m_voterNonce;
  }

  public void setVoterNonce(byte[] b) {
    this.m_voterNonce = b;
  }

  public byte[] getVoterNonce2() {
    if (m_voterNonce2 == ByteArray.EMPTY_BYTE_ARRAY) {
      return null;
    }
    return m_voterNonce2;
  }

  public void setVoterNonce2(byte[] b) {
    this.m_voterNonce2 = b;
  }

  public List getNominees() {
    return this.m_nominees;
  }

  public void setNominees(List nominees) {
    this.m_nominees = nominees;
  }

  public void setVoteComplete(boolean val) {
    this.m_voteComplete = val;
  }
  
  /** @deprecated */
  public long getVoteDeadline() {
    return m_voteDeadline;
  }
  
  /** @deprecated */
  public void setVoteDeadline(long l) {
    m_voteDeadline = l;
  }
  
  public long getVoteDuration() {
    return m_voteDuration;
  }
  
  public void setVoteDuration(long l) {
    m_voteDuration = l;
  }

  public int getModulus() {
    return m_modulus;
  }

  public void setModulus(int mod) {
    log.debug3("setModulus: " + mod);
    m_modulus = mod;
  }

  public byte[] getSampleNonce() {
    byte[] ret = ByteArray.EMPTY_BYTE_ARRAY;
    if (m_sampleNonce != null) {
      ret = m_sampleNonce;
    }
    return ret;
  }

  public void setSampleNonce(byte[] sampleNonce) {
    m_sampleNonce = sampleNonce;
  }
  
  public void setNak(PollNak nak) {
    m_nak = nak;
  }
  
  public PollNak getNak() {
    return m_nak;
  }

  public void setAgreementHint(double d) {
    m_agreementHint = d;
  }
  
  public void setWeightedAgreementHint(double d) {
    m_weightedAgreementHint = d;
  }
  
  public double getAgreementHint() {
    return m_agreementHint;
  }

  public double getWeightedAgreementHint() {
    return m_weightedAgreementHint;
  }

  public void setExpiration(long l) {
    m_msgExpiration = l;
  }
  
  public long getExpiration() {
    return m_msgExpiration;
  }
  
  public void setRetryMax(int l) {
    m_msgRetryMax = l;
  }
  
  public int getRetryMax() {
    return m_msgRetryMax;
  }
  
  public void setRetryInterval(long l) {
    m_msgRetryInterval = l;
  }
  
  public long getRetryInterval() {
    return m_msgRetryInterval;
  }
  
  /**
   * In Vote messages, determine whether more vote blocks are available.
   *
   * @return True if the vote is complete, false if more votes should be
   *         requested.
   */
  public boolean isVoteComplete() {
    return m_voteComplete;
  }

  /**
   * In Vote Request messages, return the URL of the last vote block received.
   * If this is the first vote request message, or the only one, this value will
   * be null.
   *
   * @return The URL of the last vote block received.
   */
  public String getLastVoteBlockURL() {
    return m_lastVoteBlockURL;
  }

  public void addVoteBlock(VoteBlock vb) throws IOException {
    // Do lazy initialization on our voteblocks -- only vote messages
    // have a voteBlocks field, this lets us save some memory on non-Vote
    // messages
    if (m_voteBlocks == null) {
      // Find the directory associated with this poll's state for our voteblocks
      File stateDir =
        ((LockssDaemon)m_daemon).getPollManager().getStateDir(m_key);
      if (stateDir != null) {
	m_voteBlocks = new DiskVoteBlocks(stateDir);
      } else if (!Boolean.getBoolean("org.lockss.unitTesting")) {
	// This happens in testing, shouldn't happen in a real poll
	log.warning("No state dir, key: " + m_key);
      }
    }
    if (m_voteBlocks != null) {
      m_voteBlocks.addVoteBlock(vb);
    }
  }

  /** Used for testing only */
  VoteBlocksIterator getVoteBlockIterator() throws FileNotFoundException {
    if (m_voteBlocks == null) {
      return VoteBlocksIterator.EMPTY_ITERATOR;
    } else {
      return m_voteBlocks.iterator();
    }
  }

  public VoteBlocks getVoteBlocks() {
    return m_voteBlocks;
  }

  public void setVoteBlocks(VoteBlocks voteBlocks) {
    m_voteBlocks = voteBlocks;
  }

  public void setRepairProps(CIProperties props) {
    if (props != null) {
      m_repairProps = EncodedProperty.fromProps(props);
    }
  }

  /**
   * Set the size of the repair data.
   *
   * @param len
   */
  public void setRepairDataLength(long len) {
    m_repairDataLen = len;
  }

  /**
   * Return the size of the repair data.
   */
  public long getRepairDataLength() {
    return m_repairDataLen;
  }

  /**
   * Return the extimated length of the encoded message.  This need not be
   * exact - it's used to decide whether to create a MemoryPeerMessage or
   * FilePeerMessage.
   */
  public long getEstimatedEncodedLength() {
    return EST_ENCODED_HEADER_LENGTH +
      getVoteBlocksLength() +
      getRepairDataLength();
  }

  long getVoteBlocksLength() {
    if (m_voteBlocks == null) {
      return 0;
    }
    return m_voteBlocks.getEstimatedEncodedLength();
  }

  /**
   *
   */
  public void setInputStream(InputStream is) {
    this.m_repairDataInputStream = is;
  }
  
  public String getGroups() {
    return m_groups;
  }
  
  public List getGroupList() {
    return StringUtil.breakAt(m_groups, ';');
  }
  
  public void setGroups(String groups) {
    this.m_groups = groups;
  }

  /**
   *
   * @return Input stream from which to read repair data.
   */
  public InputStream getRepairDataInputStream() throws IOException {
    if (m_repairDataFilePath != null) {
      File repairDataFile = new File(m_repairDataFilePath);
      if (repairDataFile.exists() && repairDataFile.canRead()) {
        return new FileInputStream(repairDataFile);
      }
    } else if (m_repairDataByteArray != null) {
      return new ByteArrayInputStream(m_repairDataByteArray);
    }
    return null;
  }

  public CIProperties getRepairProperties() {
    if (m_repairProps != null) {
      return CIProperties.fromProperties(m_repairProps);
    } else {
      return null;
    }
  }

  public void delete() {
    if (m_repairDataFilePath != null) {
      File repairDataFile = new File(m_repairDataFilePath);
      if (repairDataFile.exists() && repairDataFile.canRead()) {
        log.debug2("Deleting V3LcapMessage Data File: " + repairDataFile);
        repairDataFile.delete();
      }
      m_repairDataFilePath = null;
    }
  }

  //
  // Factory Methods
  //

  /**
   * Make a NoOp message.
   */
  static public V3LcapMessage makeNoOpMsg(PeerIdentity originator,
                                          byte[] pollerNonce,
                                          byte[] voterNonce,
                                          LockssApp daemon) {
    V3LcapMessage msg = new V3LcapMessage(null, daemon);
    msg.m_originatorID = originator;
    msg.m_opcode = MSG_NO_OP;
    msg.m_pollerNonce = pollerNonce;
    msg.m_voterNonce = voterNonce;
    msg.m_voterNonce2 = ByteArray.EMPTY_BYTE_ARRAY;
    msg.m_pollProtocol = Poll.V3_PROTOCOL;
    return msg;
  }

  static public V3LcapMessage makeNoOpMsg(PeerIdentity originator,
                                          byte[] pollerNonce,
                                          byte[] voterNonce,
                                          byte[] voterNonce2,
                                          LockssApp daemon) {
    V3LcapMessage msg = new V3LcapMessage(null, daemon);
    msg.m_originatorID = originator;
    msg.m_opcode = MSG_NO_OP;
    msg.m_pollerNonce = pollerNonce;
    msg.m_voterNonce = voterNonce;
    msg.m_voterNonce2 = voterNonce2;
    msg.m_pollProtocol = Poll.V1_PROTOCOL;
    msg.m_modulus = 0;
    return msg;
  }

  /**
   * Make a NoOp message.
   */
  static public V3LcapMessage makeNoOpMsg(PeerIdentity originator,
                                          byte[] pollerNonce,
                                          byte[] voterNonce,
					  int modulus,
                                          LockssApp daemon) {
    V3LcapMessage msg = new V3LcapMessage(null, daemon);
    msg.m_originatorID = originator;
    msg.m_opcode = MSG_NO_OP;
    msg.m_pollerNonce = pollerNonce;
    msg.m_voterNonce = voterNonce;
    msg.m_pollProtocol = Poll.V1_PROTOCOL;
    msg.m_modulus = modulus;
    return msg;
  }

  /**
   * Make a NoOp message with randomly generated bytes.
   */
  static public V3LcapMessage makeNoOpMsg(PeerIdentity originator,
                                          LockssApp daemon) {
    return V3LcapMessage.makeNoOpMsg(originator,
                                     ByteArray.makeRandomBytes(20),
                                     ByteArray.makeRandomBytes(20),
                                     daemon);
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[V3LcapMessage: from ");
    sb.append(m_originatorID);
    sb.append(", ");
    if (isNoOp()) {
      sb.append(getOpcodeString());
    } else {
      sb.append(getOpcodeString());
      if (getNak() != null) {
	sb.append(" Nak:");
	sb.append(getNak());
      }
      if (m_archivalID != null) {
	sb.append(" AUID: ");
	sb.append(m_archivalID);
      }
      sb.append(" Key:");
      sb.append(m_key);
      sb.append(" PN:");
      sb.append(ByteArray.toBase64(m_pollerNonce));
      if (m_voterNonce != null) {
        sb.append(" VN:");
        sb.append(ByteArray.toBase64(m_voterNonce));
      }
      if (m_voteBlocks != null) {
        sb.append(" B:");
        sb.append(String.valueOf(m_voteBlocks.size()));
      }
      if (m_repairDataLen > 0) {
        sb.append(" R:");
        sb.append(m_repairDataLen);
      }
      sb.append(" ver " + m_pollProtocol + " rev " + getSupportedProtocolRev());
    }
    sb.append("]");
    return sb.toString();
  }
  
  protected void postUnmarshal(LockssApp lockssContext) {
    m_daemon = lockssContext;
  }

  /**
   * Factory interface or creating V3LcapMessages.
   */
  public static interface Factory {
    public V3LcapMessage makeMessage(int opcode);
    public V3LcapMessage makeMessage(int opcode, long sizeEst);
  }
}
