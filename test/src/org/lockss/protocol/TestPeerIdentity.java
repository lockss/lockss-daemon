/*
 * $Id: TestPeerIdentity.java,v 1.1 2004-09-28 08:47:25 tlipkis Exp $
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


/** Test case for class: org.lockss.protocol.PeerIdentity */
public class TestPeerIdentity extends LockssTestCase {

  private IdentityManager idmgr;

  protected void setUp() throws Exception {
    super.setUp();
  }

  private PeerIdentity newPI(String key) throws Exception {
    return (PeerIdentity)PrivilegedAccessor.
      invokeConstructor(PeerIdentity.class.getName(), key);
  }

  // ensure that equals() and hashCode() have not been overridden to behave
  // differently from Object default (identity)
  public void testEqualsHash() throws Exception {
    PeerIdentity id1 = newPI("key1");
    PeerIdentity id2 = newPI("key1");
    PeerIdentity id3 = newPI("key2");
    assertNotEquals(id1, id2);
    assertNotEquals(id1, id3);
    assertNotEquals(id2, id3);
    assertEquals(System.identityHashCode(id1), id1.hashCode());
    assertEquals(System.identityHashCode(id2), id2.hashCode());
    assertEquals(System.identityHashCode(id3), id3.hashCode());
  }

  public void testFromString() throws Exception {
    PeerIdentity id1 = PeerIdentity.stringToIdentity("k1");
    PeerIdentity id2 = PeerIdentity.stringToIdentity("k1");
    PeerIdentity id3 = PeerIdentity.stringToIdentity("k3");
    assertSame(id1, id2);
    assertEquals("k1", id1.getIdString());
    assertEquals("k3", id3.getIdString());
  }

  public void testLocal() throws Exception {
    PeerIdentity id1 = PeerIdentity.stringToLocalIdentity("k88");
    PeerIdentity id2 = PeerIdentity.stringToIdentity("k2");
    assertTrue("isLocal() s.b. true", id1.isLocalIdentity());
    assertFalse("isLocal() s.b. false", id2.isLocalIdentity());
  }

  public void testIsLocal() throws Exception {
    PeerIdentity id1 = newPI("key1");
    PeerIdentity id2 = (PeerIdentity)PrivilegedAccessor.
      invokeConstructor(PeerIdentity.LocalPeerIdentity.class.getName(), "lkey");

  }

}
