/*
 * $Id: LcapIdentity.java,v 1.12 2003-02-20 00:57:28 claire Exp $
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
import org.lockss.util.*;
import org.lockss.daemon.*;
import java.io.Serializable;

/**
 * quick and dirty wrapper class for a network identity.
 * Should this class implement <code>PeerIdentity</code>?
 * @author Claire Griffin
 * @version 1.0
 */
public class LcapIdentity implements Serializable {
  /*
       PACKET HISTOGRAM SUPPORT - CURRENTLY NOT BEING USED
  */
  transient long m_lastActiveTime = 0;
  transient long m_lastOpTime = 0;
  transient long m_incrPackets = 0;   // Total packets arrived this interval
  transient long m_origPackets = 0;   // Unique pkts from this indentity this interval
  transient long m_forwPackets = 0;   // Unique pkts forwarded by this identity this interval
  transient long m_duplPackets = 0;   // Duplicate packets originated by this identity
  transient long m_totalPackets = 0;
  transient long m_lastTimeZeroed = 0;
  transient HashMap m_pktsThisInterval = new HashMap();
  transient HashMap m_pktsLastInterval = new HashMap();
  /*
    END PACKET HISTOGRAM SUPPORT - CURRENTLY NOT BEING USED
  */

  transient InetAddress m_address = null;

  int m_reputation;
  String m_idKey;

  static Logger theLog=Logger.getLogger("Identity");

  LcapIdentity(String idKey, int reputation) throws UnknownHostException {
    m_idKey = idKey;
    m_reputation = reputation;
    m_address = stringToAddr(idKey);
  }

  /**
   * construct a new Identity from an address
   * @param addr the InetAddress
   */
  LcapIdentity(InetAddress addr) {
    m_idKey = makeIdKey(addr);
    m_reputation = IdentityManager.INITIAL_REPUTATION;
    m_address = addr;
  }


  // accessor methods

  /**
   * return the address of the Identity
   * @return the <code>InetAddress<\code> for this Identity
   * @throws UnknownHostException
   */
  public InetAddress getAddress() throws UnknownHostException {
    if(m_address == null) {
      m_address = stringToAddr(m_idKey);
    }
    return m_address;
  }

  /**
   * return the current value of this Identity's reputation
   * @return the int value of reputation
   */
  public int getReputation() {
    return m_reputation;
  }

  public String getIdKey() {
    return m_idKey;
  }


  // methods which may need to be overridden
  /**
   * return true if two Identity are found to be the same
   * @param id the Identity to compare with this one
   * @return true if the id keys are the same
   */
  public boolean isEqual(LcapIdentity id) {
    String idKey = id.m_idKey;

    return idKey.equals(m_idKey);
  }

  /**
   * return the identity of the Identity
   * @return the String representation of the Identity
   */
  public String toString() {
    return m_idKey;
  }

  /**
   * return the name of the host as a string
   * @return the String representation of the Host
   */
  protected String toHost() {
    return m_idKey;
  }


  /**
   * update the active packet counter
   * @param NoOp boolean true if this is a no-op message
   * @param msg the active message
   */
  public void rememberActive(boolean NoOp, LcapMessage msg) {
    m_lastActiveTime = TimeBase.nowMs();
    if (!NoOp) {
      m_lastOpTime = m_lastActiveTime;
    }
    m_incrPackets++;
    m_totalPackets++;
    if (msg.getOriginAddr().equals(this.m_address)) {
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


  /**
   * update the reputation value for this Identity
   * @param delta the change in reputation
   */
  void changeReputation(int delta) {
    m_reputation += delta;
  }

  static String makeIdKey(InetAddress addr) {
    return addrToString(addr);
  }

  /**
   * turn and InetAddress into a dotted quartet string since
   * get host address doesn't necessarily return an address
   * @param addr the address to turn into a string
   * @return the address as dotted quartet sting
   */
  public static String addrToString(InetAddress addr)  {
    String ret = addr.getHostAddress();
    return ret;
  }

  public static InetAddress stringToAddr(String addr) throws UnknownHostException {
    InetAddress ret = InetAddress.getByName(addr);
    return ret;
  }

}
