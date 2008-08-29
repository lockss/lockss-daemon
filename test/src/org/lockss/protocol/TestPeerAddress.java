/*
 * $Id: TestPeerAddress.java,v 1.4.50.1 2008-08-29 09:19:41 tlipkis Exp $
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
import java.net.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import java.util.*;


/** Test case for class: org.lockss.protocol.PeerAddress */
public class TestPeerAddress extends LockssTestCase {
  static Logger log = Logger.getLogger("TestPeerAddress");

  private IdentityManager idmgr;

  String ipstr = "127.1.2.3";
  String ipstr2 = "127.255.3.2";
  int port = 42;
  IPAddr ipaddr, ipaddr2;

  protected void setUp() throws Exception {
    super.setUp();
    ipaddr = IPAddr.getByName(ipstr);
    ipaddr2 = IPAddr.getByName(ipstr2);
  }

  private PeerIdentity newPI(String key) throws Exception {
    return (PeerIdentity)PrivilegedAccessor.
      invokeConstructor(PeerIdentity.class.getName(), key);
  }

  // test constructor, accessors, equals
  public void testUDPAddr() throws Exception {
    PeerAddress.Udp pa1 = new PeerAddress.Udp(ipstr, ipaddr);
    assertTrue(pa1.equals(new PeerAddress.Udp(ipstr, ipaddr)));
    assertFalse(pa1.equals(new PeerAddress.Udp(ipstr, ipaddr2)));
    assertFalse(pa1.equals(new PeerAddress.Tcp(ipstr, ipaddr2, 0)));
    assertSame(ipaddr, pa1.getIPAddr());
  }

  boolean isMatch(PeerAddress pa, String ip) throws Exception {
    IpFilter filter = new IpFilter();
    filter.setFilters(ip, "");
    return pa.isAllowed(filter);
  }

  // test make from key
  public void testMakeUDPAddr() throws Exception {
    PeerAddress pa = PeerAddress.makePeerAddress(ipstr);
    assertTrue(pa instanceof PeerAddress.Udp);
    PeerAddress.Udp paUdp = (PeerAddress.Udp)pa;
    assertEquals(ipaddr, paUdp.getIPAddr());
    PeerIdentity pid = newPI(ipstr);
    assertEquals(pa, pid.getPeerAddress());

    assertTrue(isMatch(pa, ipstr));
    assertFalse(isMatch(pa, "111.211.33.44"));
  }

  // test constructor, accessors, equals
  public void testTCPAddr() throws Exception {
    PeerAddress.Tcp pa1 = new PeerAddress.Tcp(ipstr, ipaddr, port);
    assertTrue(pa1.equals(new PeerAddress.Tcp(ipstr, ipaddr, port)));
    assertFalse(pa1.equals(new PeerAddress.Tcp(ipstr, ipaddr, port+1)));
    assertFalse(pa1.equals(new PeerAddress.Tcp(ipstr, ipaddr2, port)));
    assertFalse(pa1.equals(new PeerAddress.Udp(ipstr, ipaddr)));
    assertSame(ipaddr, pa1.getIPAddr());
    assertEquals(port, pa1.getPort());
  }

  // test make from key
  public void testMakeTCPAddr() throws Exception {
    String key = IDUtil.ipAddrToKey(ipstr, port);
    PeerAddress pa = PeerAddress.makePeerAddress(key);
    assertTrue(pa instanceof PeerAddress.Tcp);
    PeerAddress.Tcp paTcp = (PeerAddress.Tcp)pa;
    assertEquals(ipaddr, paTcp.getIPAddr());
    assertEquals(port, paTcp.getPort());
    PeerIdentity pid = newPI(key);
    assertEquals(pa, pid.getPeerAddress());

    String key2 = "tcp:" + key;
    PeerAddress pa2 = PeerAddress.makePeerAddress(key2);
    assertEquals(pa2, pa);

    assertTrue(isMatch(pa, ipstr));
    assertFalse(isMatch(pa, "111.211.33.44"));
  }

  public void assertIllegal(String key) {
    try {
    PeerAddress pad = PeerAddress.makePeerAddress(key);
      fail("Should be illegal PeerAddress: " + key + ", was " + pad);
    } catch (IdentityManager.MalformedIdentityKeyException e) {
    }
  }

  public void assertLegal(String key) {
    try {
      PeerAddress.makePeerAddress(key);
    } catch (IdentityManager.MalformedIdentityKeyException e) {
      fail("Should be legal PeerAddress: " + key);
    }
  }

  public void lll(String key) throws Exception {
    log.info(key + ": " + PeerAddress.makePeerAddress(key));
  }

  public void testIllegalIdKey() throws Exception {
    assertIllegal(null);
    assertIllegal("");
    assertLegal("1.2.3.4");
    assertIllegal("1.2.3.4" + ":");
    assertLegal(IDUtil.ipAddrToKey("1.2.3.4", "65535"));
    assertIllegal(IDUtil.ipAddrToKey("1.2.3.4", "65536"));
    assertIllegal(IDUtil.ipAddrToKey("1.2.3.4", "X"));
    assertIllegal(IDUtil.ipAddrToKey("1.2.3.4", "-2"));
    assertIllegal(IDUtil.ipAddrToKey("1.2.3.4", "X"));

    // no abbreviated IP address
    assertIllegal("1");
    assertIllegal("1.2");
    // no IPV6 addresses for now
    assertIllegal("11:22:33:44:55:66:77:88");
    assertIllegal("::1");

    // no dns lookup
    assertIllegal("lockss.org");
    assertIllegal("::1");
  }
}
