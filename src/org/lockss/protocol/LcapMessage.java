/*
 * $Id: LcapMessage.java,v 1.52.2.4 2004-11-18 15:45:06 dshr Exp $
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

import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.StringTokenizer;
import org.lockss.util.*;
import java.io.*;
import org.mortbay.util.B64Code;
import org.lockss.config.Configuration;

import java.security.*;
import org.lockss.app.LockssDaemon;
import org.lockss.poller.*;
import java.util.*;

public interface LcapMessage {
  /**
   * get a property that was decoded and stored for this packet
   * @param key - the property name under which the it is stored
   * @return a string representation of the property
   */
    public String getPacketProperty(String key);

  /**
   * set or add a new property into the packet.
   * @param key the key under which to store the property
   * @param value the value to store
   */
    public void setMsgProperty(String key, String value);

  /**
   * decode the raw packet data into a property table
   * @param encodedBytes the array of encoded bytes
   * @throws IOException
   */
    public void decodeMsg(byte[] encodedBytes) throws IOException;

  /**
   * encode the message from a props table into a stream of bytes
   * @return the encoded message as bytes
   * @throws IOException if the packet can not be encoded
   */
    public byte[] encodeMsg() throws IOException;

  /**
   * store the local variables in the property table
   * @throws IOException if the packet can not be stored
   */
    void storeProps() throws IOException;


    public long getDuration();

    public long getElapsed();

    public boolean isReply();

    public boolean isNamePoll();

    public boolean isContentPoll();

    public boolean isVerifyPoll();

    public boolean isNoOp();

    public String getHashAlgorithm();

  /* methods to support data access */
    public long getStartTime();

    public long getStopTime();

    public byte getTimeToLive();

    public PeerIdentity getOriginatorID();

    public int getOpcode();

    public String getOpcodeString();

    public String getArchivalId();

    public String getPluginVersion();

    public boolean getMulticast();

    public void setMulticast(boolean multicast);

    public int getPollVersion();

    public void setPollVersion(int vers);

    public boolean supportedPollVersion(int vers);


    public ArrayList getEntries();

    public String getLwrRemain();

    public String getUprRemain();

    public String getLwrBound();

    public String getUprBound();

    public byte getHopCount();

    public void setHopCount(int hopCount);

    public byte[] getChallenge();

    public byte[] getVerifier();

    public byte[] getHashed();

    public String getTargetUrl();

    public String getKey();

    String entriesToString(int maxBufSize);

    ArrayList stringToEntries(String estr);

    public String toString();
}
