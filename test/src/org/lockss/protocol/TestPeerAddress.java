/*
 * $Id: TestPeerAddress.java,v 1.1 2005-05-18 05:52:17 tlipkis Exp $
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
    PeerIdentity pid = newPI(ipstr);
    PeerAddress.Udp pa1 = new PeerAddress.Udp(pid, ipaddr);
    PeerAddress.Udp pa1a = new PeerAddress.Udp(pid, ipaddr);
    PeerAddress.Udp pa2 = new PeerAddress.Udp(pid, ipaddr2);
    assertTrue(pa1.equals(pa1a));
    assertFalse(pa1.equals(pa2));
    assertEquals(ipaddr, pa1.getIPAddr());
  }

  // test make from key
  public void testMakeUDPAddr() throws Exception {
    PeerIdentity pid = newPI(ipstr);
    PeerAddress pa = PeerAddress.makePeerAddress(pid, ipstr);
    assertTrue(pa instanceof PeerAddress.Udp);
    PeerAddress.Udp paUdp = (PeerAddress.Udp)pa;
    assertEquals(ipaddr, paUdp.getIPAddr());
    assertEquals(pa, pid.getPeerAddress());
  }

  // test constructor, accessors, equals
  public void testTCPAddr() throws Exception {
    PeerIdentity pid = newPI(ipstr);
    PeerAddress.Tcp pa1 = new PeerAddress.Tcp(pid, ipaddr, port);
    PeerAddress.Tcp pa1a = new PeerAddress.Tcp(pid, ipaddr, port);
    PeerAddress.Tcp pa2 = new PeerAddress.Tcp(pid, ipaddr, port+1);
    PeerAddress.Tcp pa3 = new PeerAddress.Tcp(pid, ipaddr2, port);
    assertTrue(pa1.equals(pa1a));
    assertFalse(pa1.equals(pa2));
    assertFalse(pa1.equals(pa3));
    assertEquals(ipaddr, pa1.getIPAddr());
    assertEquals(port, pa1.getPort());
  }

  // test make from key
  public void testMakeTCPAddr() throws Exception {
    String key = ipstr + IdentityManager.V3_ID_SEPARATOR + port;
    PeerIdentity pid = newPI(key);
    PeerAddress pa = PeerAddress.makePeerAddress(pid, key);
    assertTrue(pa instanceof PeerAddress.Tcp);
    PeerAddress.Tcp paTcp = (PeerAddress.Tcp)pa;
    assertEquals(ipaddr, paTcp.getIPAddr());
    assertEquals(port, paTcp.getPort());
    assertEquals(pa, pid.getPeerAddress());
  }
}
