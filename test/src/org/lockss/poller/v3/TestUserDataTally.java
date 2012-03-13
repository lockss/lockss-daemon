/*
 * $Id: TestUserDataTally.java,v 1.1 2012-03-13 18:29:17 barry409 Exp $
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import org.lockss.test.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import java.util.*;
import java.io.*;

public class TestUserDataTally extends LockssTestCase {
  private IdentityManager idmgr;
  private LockssDaemon theDaemon;
  private PeerIdentity[] testPeers;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    theDaemon = getMockLockssDaemon();
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    p.setProperty(V3Poller.PARAM_V3_VOTE_MARGIN, "73");
    p.setProperty(V3Poller.PARAM_V3_TRUSTED_WEIGHT, "300");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    idmgr = theDaemon.getIdentityManager();
    idmgr.startService();
    setupPeers();
  }
  
  private void setupPeers() throws Exception {
    testPeers = new PeerIdentity[10];
    testPeers[0] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9900");
    testPeers[1] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9901");
    testPeers[2] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9902");
    testPeers[3] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9903");
    testPeers[4] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9904");
    testPeers[5] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9905");
    testPeers[6] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9906");
    testPeers[7] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9907");
    testPeers[8] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9908");
    testPeers[9] = idmgr.stringToPeerIdentity("TCP:[192.168.0.1]:9909");
  }

  public void tearDown() throws Exception {
    idmgr.stopService();
    super.tearDown();
  }
  
  public void testTalliedVoters() {
    UserDataTally tally;
    Collection<PeerIdentity> talliedVoters;
    Collection<PeerIdentity> talliedAgreeVoters;

    tally = new UserDataTally();
    tally.addTalliedAgreeVoter(testPeers[0]);
    tally.addTalliedDisagreeVoter(testPeers[1]);
    talliedVoters = tally.talliedVoters;
    assertEquals(2, talliedVoters.size());
    assertContains(talliedVoters, testPeers[0]);
    assertContains(talliedVoters, testPeers[1]);
    talliedAgreeVoters = tally.talliedAgreeVoters;
    assertEquals(1, talliedAgreeVoters.size());
    assertContains(talliedAgreeVoters, testPeers[0]);
  }
}
