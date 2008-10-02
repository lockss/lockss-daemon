/*
 * $Id: TestPeerIdentity.java,v 1.6 2008-10-02 06:49:22 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.File;

import org.lockss.test.*;
import org.lockss.util.*;

/** Test case for class: org.lockss.protocol.PeerIdentity */
public class TestPeerIdentity extends LockssTestCase {

  static String KEY1 = "tcp:[1.1.1.1]:111";
  static String KEY2 = "tcp:[1.1.1.1]:112";

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
    PeerIdentity id1 = newPI(KEY1);
    PeerIdentity id2 = newPI(KEY1);
    PeerIdentity id3 = newPI(KEY2);
    assertNotEquals(id1, id2);
    assertNotEquals(id1, id3);
    assertNotEquals(id2, id3);
    assertEquals(System.identityHashCode(id1), id1.hashCode());
    assertEquals(System.identityHashCode(id2), id2.hashCode());
    assertEquals(System.identityHashCode(id3), id3.hashCode());
  }

  public void testIsLocal() throws Exception {
    PeerIdentity id1 = new PeerIdentity(KEY1);
    PeerIdentity id2 = new PeerIdentity.LocalIdentity(KEY2);
    assertFalse(id1.isLocalIdentity());
    assertTrue(id2.isLocalIdentity());
  }

  public void testSerializationRoundtrip() throws Exception {
    MockLockssDaemon daemon = getMockLockssDaemon();
    String host = "1.2.3.4";
    String prop = "org.lockss.localIPAddress="+host;
    ConfigurationUtil.
      setCurrentConfigFromUrlList(ListUtil.list(FileTestUtil.urlOfString(prop)));
    idmgr = daemon.getIdentityManager();

    ObjectSerializer serializer = new XStreamSerializer();
    ObjectSerializer deserializer = new XStreamSerializer(daemon);

    File temp1 = File.createTempFile("tmp", ".xml");
    temp1.deleteOnExit();
    PeerIdentity pidv1 = new PeerIdentity("12.34.56.78");
    serializer.serialize(temp1, pidv1);
    PeerIdentity back1 = (PeerIdentity)deserializer.deserialize(temp1);
    assertEquals(pidv1.getIdString(), back1.getIdString());
    assertEquals(pidv1.getPeerAddress(), back1.getPeerAddress());

    File temp3 = File.createTempFile("tmp", ".xml");
    temp3.deleteOnExit();
    PeerIdentity pidv3 =
      new PeerIdentity(IDUtil.ipAddrToKey("87.65.43.21", "999"));
    serializer.serialize(temp3, pidv3);
    PeerIdentity back3 = (PeerIdentity)deserializer.deserialize(temp3);
    assertEquals(pidv3.getIdString(), back3.getIdString());
    assertEquals(pidv3.getPeerAddress(), back3.getPeerAddress());
  }
}
