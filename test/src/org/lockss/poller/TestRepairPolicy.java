/*
 * $Id: TestRepairPolicy.java,v 1.1 2012-08-13 20:47:28 barry409 Exp $
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

  protected PeerIdentity pid;
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
    pid = idManager.stringToPeerIdentity(peer);

    highAgreeAu =
      (MockArchivalUnit)PollTestPlugin.PTArchivalUnit.
      createFromListOfRootUrls(rooturls);
    setUpAu(highAgreeAu);
    idManager.signalPartialAgreement(pid, highAgreeAu, (float)0.95);

    lowAgreeAu =
      (MockArchivalUnit)PollTestPlugin.PTArchivalUnit.
      createFromListOfRootUrls(rooturls);
    setUpAu(lowAgreeAu);
    idManager.signalPartialAgreement(pid, lowAgreeAu, (float)0.05);

    daemon.getSchedService().startService();
    daemon.getHashService().startService();
    daemon.getDatagramRouterManager().startService();
    daemon.getRouterManager().startService();
    pollManager.startService();
    idManager.startService();
  }

  public void testSetup() throws Exception {
    assertEquals(0.95, idManager.getHighestPercentAgreement(pid, highAgreeAu),
		 0.01);
    assertEquals(0.05, idManager.getHighestPercentAgreement(pid, lowAgreeAu),
		 0.01);
  }

  /** Check the base method for serving a repair.  */
  public void testServeAuRepair() throws Exception {
    assertTrue(pollManager.getRepairPolicy().serveAuRepair(pid, highAgreeAu));
    assertFalse(pollManager.getRepairPolicy().serveAuRepair(pid, lowAgreeAu));
  }

  /** Check disabling all V3 repairs.  */
  public void testDisallowV3Repair() throws Exception {
    ConfigurationUtil.addFromArgs(RepairPolicy.PARAM_ALLOW_V3_REPAIRS, "false");
    RepairPolicy rp = pollManager.getRepairPolicy();
    assertFalse(rp.serveRepair(pid, highAgreeAu, rooturls[0]));
    assertFalse(rp.serveRepair(pid, lowAgreeAu, rooturls[0]));
  }

  /** Check open access override. */
  public void testAllowOpenAccessRepair() throws Exception {
    ConfigurationUtil.addFromArgs(RepairPolicy.PARAM_OPEN_ACCESS_REPAIR_NEEDS_AGREEMENT, "false");

    AuState aus = AuUtil.getAuState(lowAgreeAu);
    aus.setAccessType(AuState.AccessType.OpenAccess);

    RepairPolicy rp = pollManager.getRepairPolicy();
    assertTrue(rp.serveRepair(pid, highAgreeAu, rooturls[0]));
    assertTrue(rp.serveRepair(pid, lowAgreeAu, rooturls[0]));
  }

  /** Check open access override. */
  public void testTrustedNetworks() throws Exception {
    ConfigurationUtil.addFromArgs(BlockingStreamComm.PARAM_USE_V3_OVER_SSL, "true");
    ConfigurationUtil.addFromArgs(BlockingStreamComm.PARAM_USE_SSL_CLIENT_AUTH, "true");
    ConfigurationUtil.addFromArgs(RepairPolicy.PARAM_REPAIR_ANY_TRUSTED_PEER, "true");

    // LcapStreamComm doesn't subscribe to changes in the config, so
    // just tell it to be trusted.
    scomm.setTrustedNetwork(true);

    RepairPolicy rp = pollManager.getRepairPolicy();
    assertTrue(rp.serveRepair(pid, highAgreeAu, rooturls[0]));
    assertTrue(rp.serveRepair(pid, lowAgreeAu, rooturls[0]));
  }

  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {TestRepairPolicy.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
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
