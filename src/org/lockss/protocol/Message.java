/*
 * $Id: Message.java,v 1.1 2002-10-02 15:41:43 claire Exp $
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

import java.net.DatagramPacket;
import java.io.IOException;
import java.io.DataInputStream;
import org.lockss.util.EncodedProperty;
import org.lockss.util.Logger;
import java.net.InetAddress;
import org.lockss.daemon.PeerIdentity;
import java.net.DatagramSocket;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.daemon.CachedUrlSet;
import java.net.*;
import java.util.StringTokenizer;


/**
 * <p>Description: used to encapsulate a message which has been received
 * or will be sent over the wire. </p>
 * @author Claire Griffin
 * @version 1.0
 */

public class Message {
  public static final int NAME_POLL_REQ = 0;
  public static final int NAME_POLL_REP = 1;
  public static final int CONTENT_POLL_REQ = 2;
  public static final int CONTENT_POLL_REP = 3;
  public static final int VERIFY_POLL_REQ = 4;
  public static final int VERIFY_POLL_REP = 5;

  InetAddress m_group;    // The group address
  InetAddress m_sender;   // The sender address
  InetAddress m_originIP;
  Identity m_originID;
  Identity m_senderID;
  DatagramSocket m_socket;

  byte m_ttl;             // The time to live it packet was sent
  int m_quorum;           // The quorum factor
  int m_opcode;          // the kind of packet
  int m_port;
  long m_startTime;
  long m_stopTime;
  boolean m_multicast;
  int m_originPort;
  byte m_hopCount;

  protected byte[] m_challenge;
  protected byte[] m_verifier;
  protected byte[] m_hashed;

  protected String[] m_entries;
  protected CachedUrlSetSpec m_UrlSetSpec;

  private static Logger log = Logger.getLogger("Packet");
  private byte[] m_bytes;
  private EncodedProperty m_props;

  protected Message() throws IOException {
    m_props = new EncodedProperty();
    m_bytes = null;
  }

  protected Message(byte[] encodedBytes) throws IOException {
    m_props = new EncodedProperty();
    m_bytes = encodedBytes;

    try {
      m_props.decode(encodedBytes);
    }
    catch (IOException ex) {
      log.error("Unreadable Packet",ex);
      throw new ProtocolException("Unable to decode pkt.");
    }
  }

  protected Message(CachedUrlSetSpec urlspec,
                    InetAddress group,
                    int port,
                    byte ttl,
                    int quorum,
                       byte[] challenge,
                       byte[] verifier,
                       byte[] hashedData,
                       int opcode) throws IOException {
    this();
    // extract data from packet
    m_group = group;
    m_port = port;
    m_ttl = ttl;
    m_quorum = quorum;
    m_sender = null;
    m_senderID = null;
    m_opcode = opcode;
    m_challenge = challenge;
    m_verifier = verifier;
    m_hashed = hashedData;
    m_startTime = 0;
    m_stopTime = 0;
    m_socket = null;
    m_multicast = false;
    m_originPort = 0;
    m_originIP = null;
    m_originID = null;
    m_hopCount = 0;
  }

  protected Message(Message trigger,
                       byte[] verifier,
                       byte[] hashedContent,
                       int opcode) throws IOException {

    // copy the essential information from the trigger packet
    m_group = trigger.getGroupAddress();
    m_port = trigger.m_port;  // XXX should be originatorPort?
    m_ttl = trigger.getTimeToLive();
    m_quorum = trigger.getQuorum();
    m_sender = null;
    m_senderID = null;
    m_opcode = opcode;
    m_challenge = trigger.getChallenge();
    m_verifier = verifier;
    m_hashed = hashedContent;

    m_UrlSetSpec = trigger.getUrlSetSpec();
    m_startTime = 0;
    m_stopTime = 0;
    m_socket = trigger.getSocket();
    m_multicast = false;
    m_originPort = m_socket.getLocalPort();
    m_originIP = m_socket.getLocalAddress();
    m_originID = Identity.getLocalIdentity(m_socket);
    m_hopCount =trigger.getHopCount();
   }

  /**
   * get a property that was decoded and stored for this packet
   * @param key - the property name under which the it is stored
   * @return a string representation of the property
   */
  public String getPacketProperty(String key) {
    return m_props.getProperty(key);
  }

  /**
   * set or add a new property into the packet.
   * @param key the key under which to store the property
   * @param value the value to store
   */
  protected void setMsgProperty(String key, String value) {
    m_props.setProperty(key, value);
  }

  /**
   * static method to make a message request packet
   * @param ps
   * @param challenge a byte array containing the challange
   * @param verifier a byte array containing the verifier
   * @param opcode the opcode for this message
   * @param timeRemaining the time remaining for the poll
   * @param s the socket
   * @return a new Message object
   * @throws IOException
   */
  static public Message makeRequestMsg(CachedUrlSetSpec urlspec,
                                       InetAddress group,
                                       int port,
                                       byte ttl,
                                       int quorum,
                                       byte[] challenge,
                                       byte[] verifier,
                                       int opcode,
                                       long timeRemaining,
                                       DatagramSocket s) throws IOException {

    Message msg = new Message(urlspec,group,port,ttl,quorum,
                              challenge,verifier,new byte[0],opcode);
    if (msg != null) {
      msg.m_startTime = System.currentTimeMillis();
      msg.m_stopTime = msg.m_startTime + timeRemaining;
      msg.m_socket = s;
      msg.m_originPort = s.getLocalPort();
      msg.m_originIP = s.getLocalAddress();
      msg.m_originID = Identity.getLocalIdentity(msg.m_socket);
      // XXX - is this correct
      msg.m_hopCount = ttl;
    }
    return msg;

  }

  /**
   * static method to make a reply message
   * @param trigger the message which trggered the reply
   * @param hashedContent the hashed content bytes
   * @param verifier the veerifier bytes
   * @param opcode an opcode for this message
   * @param timeRemaining the time remaining on the poll
   * @param s the socket
   * @return a new Message object
   * @throws IOException
   */
  static public Message makeReplyMsg(Message trigger,
                                byte[] hashedContent,
                                byte[] verifier,
                                int opcode,
                                long timeRemaining,
				DatagramSocket s) throws IOException {
    Message msg = new Message(trigger, verifier, hashedContent, opcode);
    if (msg != null) {
      msg.m_socket = s;
      msg.m_startTime = System.currentTimeMillis();
      msg.m_stopTime = msg.m_startTime + timeRemaining;
      msg.m_UrlSetSpec = trigger.getUrlSetSpec();
      msg.m_originPort = s.getLocalPort();
      msg.m_originIP = s.getLocalAddress();
      msg.m_originID = Identity.getLocalIdentity(s);
      msg.m_hopCount = trigger.getHopCount();
    }
    return msg;
  }

  /**
   * a static method to create a new message from a Datagram Packet
   * @param p the datagram packet
   * @param s the datagram socket
   * @param mcast true if packet was from a multicast socket
   * @return a new Message object
   * @throws IOException
   */
  static Message decodeToMsg(DatagramPacket p,
                            DatagramSocket s,
                            boolean mcast) throws IOException {
    Message msg = new Message(p.getData());
    if(msg != null) {
      msg.decodeMsg();
      msg.m_socket = s;
      msg.m_multicast = mcast;
    }
    return msg;
  }

  /**
   * decode the raw packet data into a property table
   * TODO: this needs to have the data broken into a variable portion
   * and a invariable portion and hashed
   */
  public void decodeMsg() {
    long duration;
    long elapsed;

    m_multicast = m_props.getBoolean("mcast", false);
    m_hopCount = (byte) m_props.getInt("hops", 0);
    m_ttl = (byte) m_props.getInt("ttl", 0);
    m_opcode = m_props.getInt("opcode", -1);
    duration = m_props.getInt("duration", 0) * 1000;
    elapsed = m_props.getInt("elapsed", 0)* 1000;

    try {
      m_originIP = InetAddress.getByName(m_props.getProperty("orig"));
      m_group =InetAddress.getByName(m_props.getProperty("group"));
    }
    catch (UnknownHostException ex) {
      log.error("invalid origin ip and/or group");
    }

    // XXX we need a real CachedUrlSetSpec to reconstitute these
    String prefix = m_props.getProperty("prefix");
    String regexp = m_props.getProperty("regexp");

    m_challenge = m_props.getByteArray("challenge", new byte[0]);
    m_verifier = m_props.getByteArray("verifier", new byte[0]);
    m_hashed = m_props.getByteArray("hashed", new byte[0]);
    long now = System.currentTimeMillis();
    m_startTime = now - elapsed;
    m_stopTime = now + duration;
    m_originID = Identity.getIdentity(m_originIP, m_originPort);
  }

  /**
   * encode the message from a props table into a stream of bytes
   * @return the encoded message as bytes
   */
  public byte[] encodeMsg() throws IOException {
    // make sure the props table is up to date
   m_props.putBoolean("mcast",m_multicast);
   m_props.putInt("hops",m_hopCount);
   m_props.putInt("ttl",m_ttl);
   m_props.putInt("opcode",m_opcode);
   m_props.putInt("duration",(int)(getDuration()/1000));
   m_props.putInt("elapsed",(int)(getElapsed()/1000));
   m_props.put("orig",m_originIP.getHostAddress());
   m_props.put("group",m_group.getHostAddress());
   m_props.setProperty("prefix", m_UrlSetSpec.urlPrefix());
   m_props.setProperty("regexp",m_UrlSetSpec.regExp());
   m_props.putByteArray("challenge", m_challenge);
   m_props.putByteArray("verifier", m_verifier);
   m_props.putByteArray("hashed", m_hashed);

    return m_props.encode();
  }

  public long getDuration() {
    long now = System.currentTimeMillis();
    long ret = m_stopTime - now;
    if (ret < 0)
      ret = 0;
    return ret;
  }

  public long getElapsed() {
    long now = System.currentTimeMillis();
    long ret = now - m_startTime;
    if (now > m_stopTime)
      ret = m_stopTime - m_startTime;
    return ret;
  }

  public boolean isReply() {
    return (m_opcode % 2 == 1) ? true : false;
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

  public InetAddress getGroupAddress() {
    return m_group;    // The group address
  }

  public InetAddress getSenderAddress() {
    return m_sender;   // The sender address
  }

  public InetAddress getOriginIP() {
    return m_originIP;
  }

  public Identity getOriginID(){
    return m_originID;
  }

  public Identity getSenderID() {
      return m_senderID;
  }

  public DatagramSocket getSocket() {
    return m_socket;
  }

  public int getQuorum() {
    return m_quorum;
  }

  public int getOpcode() {
    return m_opcode;
  }

  public int getPort() {
    return m_port;
  }

  public boolean isMulticast() {
    return m_multicast;
  }

  public int getOriginPort() {
    return m_originPort;
  }

  public String[] getEntries() {
    return m_entries;
  }

  public byte getHopCount() {
    return m_hopCount;
  }

  public byte[] getChallenge() {
    return m_challenge;
  }

  public byte[] getVerifier() {
    return m_verifier;
  }

  public byte[] getHashed() {
    return m_hashed;
  }


  public CachedUrlSetSpec getUrlSetSpec() {
    return m_UrlSetSpec;
  }

  String entriesToString() {
    StringBuffer buf = new StringBuffer(80);
    for(int i= 0; i< m_entries.length; i++) {
      buf.append(m_entries[i]);
      buf.append("\n");
    }
    return buf.toString();
  }

  String[] stringToEntries(String estr) {
    StringTokenizer tokenizer = new StringTokenizer(estr,"\n");
    String[] ret = new String[tokenizer.countTokens()];
    int i = 0;

    while(tokenizer.hasMoreTokens()) {
      ret[i++] = tokenizer.nextToken();
    }
    return ret;
  }

  /** Exception thrown if packet is unparseable. */
  public static class ProtocolException extends IOException {
    public ProtocolException(String msg) {
      super(msg);
    }
  }
}

