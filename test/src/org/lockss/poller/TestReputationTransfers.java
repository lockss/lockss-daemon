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

import java.io.File;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.config.ConfigManager;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.PeerIdentity;

import org.lockss.util.*;
import org.lockss.test.*;

/** JUnitTest case for class: org.lockss.poller.ReputationTransfers */
public class TestReputationTransfers extends LockssTestCase {

  private MockLockssDaemon daemon;
  protected IdentityManager idManager;

  final String p1 = "TCP:[127.0.0.1]:12";
  final String p2 = "TCP:[127.0.0.2]:12";
  final String p3 = "TCP:[127.0.0.3]:12";
  final String p4 = "TCP:[127.0.0.4]:12";
  final String p5 = "TCP:[127.0.0.5]:12";
  
  PeerIdentity peer1;
  PeerIdentity peer2;
  PeerIdentity peer3;
  PeerIdentity peer4;
  PeerIdentity peer5;

  List<PeerIdentity> list1;
  List<PeerIdentity> list2;
  List<PeerIdentity> list3;
  List<PeerIdentity> list4;
  List<PeerIdentity> list5;
  List<PeerIdentity> list12;
  List<PeerIdentity> list13;
  List<PeerIdentity> list34;
  List<PeerIdentity> list123;

  protected void setUp() throws Exception {
    super.setUp();

    File tempDir = getTempDir();
    String tempDirPath = tempDir.getAbsolutePath() + File.separator;
    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.1.2.3");
    //p.setProperty(LcapDatagramComm.PARAM_ENABLED, "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    initRequiredServices();
    peer1 = idManager.stringToPeerIdentity(p1);
    peer2 = idManager.stringToPeerIdentity(p2);
    peer3 = idManager.stringToPeerIdentity(p3);
    peer4 = idManager.stringToPeerIdentity(p4);
    peer5 = idManager.stringToPeerIdentity(p5);

    list1 = ListUtil.list(peer1);
    list2 = ListUtil.list(peer2);
    list3 = ListUtil.list(peer3);
    list4 = ListUtil.list(peer4);
    list5 = ListUtil.list(peer5);
    list12 = ListUtil.list(peer1, peer2);
    list13 = ListUtil.list(peer1, peer3);
    list123 = ListUtil.list(peer1, peer2, peer3);
    list34 = ListUtil.list(peer3, peer4);
  }


  public void tearDown() throws Exception {
    idManager.stopService();
    super.tearDown();
  }

  /** Test that a configuration change will be relayed to the instance
   * in use. */
  public void testConfigChange() throws Exception {
    setUpDiskSpace();
    MockLockssDaemon daemon = getMockLockssDaemon();
    daemon.getPollManager().startService();

    ReputationTransfers rpm = daemon.getPollManager().getRepairPolicy().
      getReputationTransfers();
    assertSameElements(list1, rpm.getAllReputationsTransferredFrom(peer1));
    String xfermap = p1 + "," + p2;
    ConfigurationUtil.addFromArgs(
      ReputationTransfers.PARAM_REPUTATION_TRANSFER_MAP, xfermap);
    assertSameElements(list12, rpm.getAllReputationsTransferredFrom(peer2));

    daemon.getPollManager().stopService();
  }

  public void testReputationTransfers() throws Exception {
    String xfermap = p1 + "," + p2 + ";" + p3 + "," + p4;
    ConfigurationUtil.addFromArgs(
      ReputationTransfers.PARAM_REPUTATION_TRANSFER_MAP, xfermap);

    ReputationTransfers rpm = new ReputationTransfers(idManager);
    assertSameElements(list1, rpm.getAllReputationsTransferredFrom(peer1));
    assertSameElements(list12, rpm.getAllReputationsTransferredFrom(peer2));
    assertSameElements(list3, rpm.getAllReputationsTransferredFrom(peer3));
    assertSameElements(list34, rpm.getAllReputationsTransferredFrom(peer4));
    assertSameElements(list5, rpm.getAllReputationsTransferredFrom(peer5));
  }

  public void testUnmodifiable() throws Exception {
    String xfermap = p1 + "," + p2;
    ConfigurationUtil.addFromArgs(
      ReputationTransfers.PARAM_REPUTATION_TRANSFER_MAP, xfermap);

    ReputationTransfers rpm = new ReputationTransfers(idManager);
    try {
      Collection<PeerIdentity> c = rpm.getAllReputationsTransferredFrom(peer2);
      assertSameElements(list12, c);
      c.add(peer3);
      fail("Collection should be unmodifiable");
    } catch (UnsupportedOperationException e) {
    }
    try {
      Collection<PeerIdentity> c = rpm.getAllReputationsTransferredFrom(peer5);
      assertSameElements(list5, c);
      c.add(peer3);
      fail("Collection should be unmodifiable");
    } catch (UnsupportedOperationException e) {
    }
  }

  public void testTransitive() throws Exception {
    String xfermap = p1 + "," + p2 + ";" + p2 + "," + p3;
    ConfigurationUtil.addFromArgs(
      ReputationTransfers.PARAM_REPUTATION_TRANSFER_MAP, xfermap);

    ReputationTransfers rpm = new ReputationTransfers(idManager);
    assertSameElements(list123,
		       rpm.getAllReputationsTransferredFrom(peer3));
    assertSameElements(list12,
		       rpm.getAllReputationsTransferredFrom(peer2));
    assertSameElements(list1,
		       rpm.getAllReputationsTransferredFrom(peer1));

    // No transfer in the param for this peer.
    assertSameElements(list4,
		       rpm.getAllReputationsTransferredFrom(peer4));
  }

  public void testEmptyMapping() throws Exception {
    ReputationTransfers rpm = new ReputationTransfers(idManager);
    assertSameElements(list1, rpm.getAllReputationsTransferredFrom(peer1));
    assertSameElements(list2, rpm.getAllReputationsTransferredFrom(peer2));
  }

  // todo(bhayes): Need to think about how to deal with this error case.
  /**
   * Have two peers each transfer their reputation to the same peer.
   */
  public void testMultipleSource() throws Exception {
    String xfermap = p1 + "," + p3 + ";" + p2 + "," + p3;
    ConfigurationUtil.addFromArgs(
      ReputationTransfers.PARAM_REPUTATION_TRANSFER_MAP, xfermap);

    ReputationTransfers rpm = new ReputationTransfers(idManager);
    // Should not be allowed?
    assertSameElements(list13, rpm.getAllReputationsTransferredFrom(peer3));
  }

  // todo(bhayes): Need to think about how to deal with this error case.
  /**
   * Have one peer transfer its reputation to two peers.
   */
  public void testMultipleDestinations() throws Exception {
    String xfermap = p1 + "," + p2 + ";" + p1 + "," + p3;
    ConfigurationUtil.addFromArgs(
      ReputationTransfers.PARAM_REPUTATION_TRANSFER_MAP, xfermap);

    ReputationTransfers rpm = new ReputationTransfers(idManager);
    // Should not be allowed?
    assertSameElements(list12, rpm.getAllReputationsTransferredFrom(peer2));
    assertSameElements(list3, rpm.getAllReputationsTransferredFrom(peer3));
  }

  // todo(bhayes): Need to think about how to deal with this error case.
  /**
   * Have a cycle of tranfers.
   */
  public void testCycle() throws Exception {
    String xfermap = p1 + "," + p2 + ";" + p2 + "," + p1;
    ConfigurationUtil.addFromArgs(
      ReputationTransfers.PARAM_REPUTATION_TRANSFER_MAP, xfermap);
    ReputationTransfers rpm = new ReputationTransfers(idManager);
    // Should not be allowed!
    assertSameElements(list12, rpm.getAllReputationsTransferredFrom(peer1));
    assertSameElements(list12, rpm.getAllReputationsTransferredFrom(peer2));
  }

  // todo(bhayes): Need to think about how to deal with this error case.
  /**
   * Have a transfer of a peer to itself.
   */
  public void testReflexive() throws Exception {
    String xfermap = p1 + "," + p1;
    ConfigurationUtil.addFromArgs(
      ReputationTransfers.PARAM_REPUTATION_TRANSFER_MAP, xfermap);
    ReputationTransfers rpm = new ReputationTransfers(idManager);
    assertSameElements(list1, rpm.getAllReputationsTransferredFrom(peer1));
  }

  private void initRequiredServices() {
    daemon = getMockLockssDaemon();
    idManager = daemon.getIdentityManager();

    idManager.startService();
  }

  /** Executes the test case
   * @param argv array of Strings containing command line arguments
   * */
  public static void main(String[] argv) {
    String[] testCaseList = {TestReputationTransfers.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
