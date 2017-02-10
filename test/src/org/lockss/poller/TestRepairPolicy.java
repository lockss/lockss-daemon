/*
 * $Id$
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

package org.lockss.poller;

import java.io.*;
import java.security.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.v3.*;
import org.lockss.protocol.*;
import org.lockss.protocol.BlockingStreamComm;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.test.*;
import org.lockss.repository.*;

/** JUnitTest case for class: org.lockss.poller.RepairPolicy */
public class TestRepairPolicy extends LockssTestCase {

  private static String[] rooturls = {"http://www.test.org",
				      "http://www.test1.org",
				      "http://www.test2.org"};
  private static String peer = "TCP:[127.0.0.1]:12";

  protected MockArchivalUnit highAgreeAu;
  protected MockArchivalUnit lowAgreeAu;
  private MockLockssDaemon daemon;

  protected PeerIdentity reqPid;
  protected MockPollManager pollManager;
  protected MyBlockingStreamComm scomm;
  protected IdentityManager idManager;
  private File tempDir;

  protected void setUp() throws Exception {
    super.setUp();

    File tempDir = getTempDir();
    String tempDirPath = tempDir.getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.1.2.3");
    p.setProperty(LcapDatagramComm.PARAM_ENABLED, "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    initRequiredServices();
  }


  public void tearDown() throws Exception {
    pollManager.stopService();
    idManager.stopService();
    daemon.getLockssRepository(highAgreeAu).stopService();
    daemon.getLockssRepository(lowAgreeAu).stopService();
    daemon.getHashService().stopService();
    daemon.getDatagramRouterManager().stopService();
    daemon.getRouterManager().stopService();
    super.tearDown();
  }

  private void setUpAu(MockArchivalUnit au) {
    au.setPlugin(new MockPlugin(daemon));
    MockNodeManager nodeManager = new MockNodeManager();
    daemon.setNodeManager(nodeManager, au);
    nodeManager.setAuState(new MockAuState());
  }

  private void initRequiredServices() throws Exception {
    daemon = getMockLockssDaemon();
    pollManager = new MockPollManager();
    pollManager.initService(daemon);
    daemon.setPollManager(pollManager);

    scomm = new MyBlockingStreamComm();
    scomm.initService(daemon);
    daemon.setStreamCommManager(scomm);

    idManager = daemon.getIdentityManager();
    reqPid = idManager.stringToPeerIdentity(peer);

    highAgreeAu =
      (MockArchivalUnit)PollTestPlugin.PTArchivalUnit.
      createFromListOfRootUrls(rooturls);
    setUpAu(highAgreeAu);
    idManager.signalPartialAgreement(reqPid, highAgreeAu, (float)0.95);

    lowAgreeAu =
      (MockArchivalUnit)PollTestPlugin.PTArchivalUnit.
      createFromListOfRootUrls(rooturls);
    setUpAu(lowAgreeAu);
    idManager.signalPartialAgreement(reqPid, lowAgreeAu, (float)0.05);

    daemon.getSchedService().startService();
    daemon.getHashService().startService();
    daemon.getDatagramRouterManager().startService();
    daemon.getRouterManager().startService();
    pollManager.startService();
    idManager.startService();
  }

  public void testSetup() throws Exception {
    assertEquals(0.95, idManager.getHighestPercentAgreement(reqPid, highAgreeAu),
		 0.01);
    assertEquals(0.05, idManager.getHighestPercentAgreement(reqPid, lowAgreeAu),
		 0.01);
  }

  public void testServeAuRepair() throws Exception {
    RepairPolicy rp = pollManager.getRepairPolicy();
    assertTrue(rp.shouldServeAuRepair(reqPid, highAgreeAu));
    assertFalse(rp.shouldServeAuRepair(reqPid, lowAgreeAu));
  }

  public void testAgreementType() throws Exception {
    RepairPolicy rp = pollManager.getRepairPolicy();
    MockArchivalUnit au;
    EnumSet<AgreementType> permitRepairAgreements =
      EnumSet.of(AgreementType.POR, AgreementType.SYMMETRIC_POR,
		 AgreementType.POP, AgreementType.SYMMETRIC_POP);
    
    for (AgreementType type: AgreementType.values()) {
      au = (MockArchivalUnit)PollTestPlugin.PTArchivalUnit.
	createFromListOfRootUrls(rooturls);
      setUpAu(au);
      assertFalse(rp.shouldServeAuRepair(reqPid, au));
      idManager.signalPartialAgreement(type, reqPid, au, (float)0.05);
      assertFalse(rp.shouldServeAuRepair(reqPid, au));
      // shouldServeRepair if and only if the type is one of those
      // listed above.
      idManager.signalPartialAgreement(type, reqPid, au, (float)0.95);
      assertEquals(permitRepairAgreements.contains(type),
		   rp.shouldServeAuRepair(reqPid, au));
    }
  }

  public void testDisallowV3Repair() throws Exception {
    ConfigurationUtil.addFromArgs(RepairPolicy.PARAM_ALLOW_V3_REPAIRS, "false");
    RepairPolicy rp = pollManager.getRepairPolicy();
    assertFalse(rp.shouldServeRepair(reqPid, highAgreeAu, rooturls[0]));
    assertFalse(rp.shouldServeRepair(reqPid, lowAgreeAu, rooturls[0]));
  }

  public void testAllowOpenAccessRepair() throws Exception {
    ConfigurationUtil.addFromArgs(RepairPolicy.PARAM_OPEN_ACCESS_REPAIR_NEEDS_AGREEMENT, "false");

    AuState aus = AuUtil.getAuState(lowAgreeAu);
    aus.setAccessType(AuState.AccessType.OpenAccess);

    RepairPolicy rp = pollManager.getRepairPolicy();
    assertTrue(rp.shouldServeRepair(reqPid, highAgreeAu, rooturls[0]));
    assertTrue(rp.shouldServeRepair(reqPid, lowAgreeAu, rooturls[0]));
  }

  public void testTrustedNetworks() throws Exception {
    ConfigurationUtil.addFromArgs(BlockingStreamComm.PARAM_USE_V3_OVER_SSL, "true");
    ConfigurationUtil.addFromArgs(BlockingStreamComm.PARAM_USE_SSL_CLIENT_AUTH, "true");
    ConfigurationUtil.addFromArgs(RepairPolicy.PARAM_REPAIR_ANY_TRUSTED_PEER, "true");

    // LcapStreamComm doesn't subscribe to changes in the config, so
    // just tell it to be trusted.
    scomm.setTrustedNetwork(true);

    RepairPolicy rp = pollManager.getRepairPolicy();
    assertTrue(rp.shouldServeRepair(reqPid, highAgreeAu, rooturls[0]));
    assertTrue(rp.shouldServeRepair(reqPid, lowAgreeAu, rooturls[0]));
  }

  static class MyBlockingStreamComm extends BlockingStreamComm {
    boolean setTrustedCalled = false;
    boolean trusted;
    
    void setTrustedNetwork(boolean trusted) {
      this.setTrustedCalled = true;
      this.trusted = trusted;
    }

    @Override public boolean isTrustedNetwork() {
      if (setTrustedCalled) {
	return trusted;
      }
      return super.isTrustedNetwork();
    }
  }
}
