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

import org.lockss.test.*;

/**
 * @author edwardsb
 *
 * Does the Mock Peer Identity correctly gets its peer identity from 
 * the constructor?  These tests are very simple, but note that the
 * test constructs a Mock Peer Identity... but cast it as a Peer Identity.  
 */
public class TestMockPeerIdentity extends LockssTestCase {
  // Constants
  private final static String k_strPeerIdentity = "TCP:[192.168.0.1]:9723";
  
  // Instance variables
  private MockLockssDaemon m_theDaemon;
  private PeerIdentity m_peerIdentity;  /* Note: NOT MockPeerIdentity */
  
  /**
   * @see junit.framework.TestCase#setUp()
   * @throws java.lang.Exception
   */
  protected void setUp() throws Exception {
    super.setUp();
    
    m_peerIdentity = new MockPeerIdentity(k_strPeerIdentity);
    m_theDaemon = getMockLockssDaemon();
  }

  /**
   * @see junit.framework.TestCase#tearDown()
   * @throws java.lang.Exception
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test method for {@link org.lockss.protocol.MockPeerIdentity#toString()}.
   */
  public final void testToString() {
    String strPeerIdentity = m_peerIdentity.toString();    
    assertEquals("[MockPeer: " + k_strPeerIdentity + "]", strPeerIdentity);
  }

  /**
   * Test method for {@link org.lockss.protocol.MockPeerIdentity#getIdString()}.
   */
  public final void testGetIdString() {
    String strIdString = m_peerIdentity.getIdString();    
    assertEquals(k_strPeerIdentity, strIdString);
  }

  /**
   * Test method for {@link org.lockss.protocol.MockPeerIdentity#getPeerAddress()}.
   */
  public final void testGetPeerAddress() {
    try {
      m_peerIdentity.getPeerAddress();
      fail("getPeerAddress should have thrown an exception.");
    } catch (UnsupportedOperationException e) {
      // Pass.
    }
  }

  /**
   * Test method for {@link org.lockss.protocol.MockPeerIdentity#isLocalIdentity()}.
   */
  public final void testIsLocalIdentity() {
    boolean isLocalIdentity = m_peerIdentity.isLocalIdentity();
    assertEquals(false, isLocalIdentity);
  }

  
  /**
   * Test method for {@link org.lockss.protocol.PeerIdentity#getIPAddr()}.
   */
  public final void testGetIPAddr() {
    assertNull(m_peerIdentity.getIPAddr());
  }

  /**
   * Test method for {@link org.lockss.protocol.PeerIdentity#getUiUrlStem(int)}.
   */
  public final void testGetUiUrlStem() {
    try {
      m_peerIdentity.getUiUrlStem(8001);
      fail("getUiUrlStem should have thrown an exception.");
    } catch (UnsupportedOperationException e) {
      // Pass
    }
  }
}
