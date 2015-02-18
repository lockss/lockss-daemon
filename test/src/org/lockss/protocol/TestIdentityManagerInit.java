/*
 * $Id$
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
import java.util.*;
import java.net.UnknownHostException;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.test.*;

/** Test cases for org.lockss.protocol.IdentityManager that test
 * initialization and startup.  See TestIdentityManager for more
 * IdentityManager tests. */
public class TestIdentityManagerInit extends LockssTestCase {
  static int TEST_V3_PORT = 4456;
  static String TEST_LOCAL_IP = "127.1.2.3";

  Object testIdKey;

  private MockLockssDaemon theDaemon;
  private IdentityManager idmgr;
  IPAddr testIpAddr;

  public void setUp() throws Exception {
    super.setUp();
    testIpAddr = IPAddr.getByName(TEST_LOCAL_IP);
    theDaemon = getMockLockssDaemon();
  }

  public void configInit(boolean v3, boolean iddb) throws Exception {
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, TEST_LOCAL_IP);
    if (v3) {
      p.setProperty(IdentityManager.PARAM_LOCAL_V3_PORT, "" + TEST_V3_PORT);
    }
    if (iddb) {
      String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
      p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    }
    ConfigurationUtil.setCurrentConfigFromProps(p);

    idmgr = theDaemon.getIdentityManager();
  }

  public void tearDown() throws Exception {
    if (idmgr != null) {
      idmgr.stopService();
      idmgr = null;
    }
    super.tearDown();
  }

  public void testMissingLocalV1Identity() throws Exception {
    try {
      idmgr = theDaemon.getIdentityManager();
      fail("No local ip addr, IdentityManager.initService() should throw");
    } catch (LockssAppException e) {
    }
  }

  public void testLocalV1Identity() throws Exception {
    configInit(false, false);
    PeerIdentity p1 = idmgr.ipAddrToPeerIdentity(testIpAddr, 0);
    assertNotNull(p1);
    assertTrue(p1.isLocalIdentity());
    assertTrue(idmgr.isLocalIdentity(p1));

    //    LcapIdentity i1 = idmgr.

    PeerIdentity p2 = idmgr.ipAddrToPeerIdentity(IPAddr.getByName("4.22.66.78"),
						 0);
    assertNotNull(p2);
    assertFalse(p2.isLocalIdentity());
    assertFalse(idmgr.isLocalIdentity(p2));
  }

  public void testLocalV3Identity() throws Exception {
    configInit(true, false);
    PeerIdentity p1 = idmgr.ipAddrToPeerIdentity(testIpAddr, 0);
    assertNotNull(p1);
    assertTrue(p1.isLocalIdentity());

    PeerIdentity p2 = idmgr.ipAddrToPeerIdentity(testIpAddr, TEST_V3_PORT);
    assertNotNull(p2);
    assertTrue(p2.isLocalIdentity());
    assertTrue(idmgr.isLocalIdentity(p2));

    assertNotSame(p1, p2);
    assertNotEquals(p1, p2);
  }

  // XXX
  // needs iddb reading tests

}
