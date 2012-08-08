/*
 * $Id: 
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
  }


  public void tearDown() throws Exception {
    idManager.stopService();
    super.tearDown();
  }

  public void testReputationTransfers() throws Exception {
    String xfermap = p1 + "," + p2 + ";" + p3 + "," + p4;
    ConfigurationUtil.addFromArgs(
      ReputationTransfers.PARAM_REPUTATION_TRANSFER_MAP, xfermap);

    ReputationTransfers rpm = new ReputationTransfers(idManager);
    assertEquals(peer1, rpm.getReputationTransferredFrom(peer2));
    assertEquals(peer3, rpm.getReputationTransferredFrom(peer4));
    assertNull(rpm.getReputationTransferredFrom(peer1));
    assertNull(rpm.getReputationTransferredFrom(peer5));
  }

  public void testTransitive() throws Exception {
    String xfermap = p1 + "," + p2 + ";" + p2 + "," + p3;
    ConfigurationUtil.addFromArgs(
      ReputationTransfers.PARAM_REPUTATION_TRANSFER_MAP, xfermap);

    ReputationTransfers rpm = new ReputationTransfers(idManager);
    // This relies on the order being maintained by
    // ReputationTransfers, which is not part of its contract.
    assertEquals(ListUtil.list(peer3, peer2, peer1),
		 ListUtil.fromIterator(
		   rpm.getAllReputationsTransferredFrom(peer3).iterator()));
    assertEquals(ListUtil.list(peer2, peer1),
		 ListUtil.fromIterator(
		   rpm.getAllReputationsTransferredFrom(peer2).iterator()));
    assertEquals(ListUtil.list(peer1),
		 ListUtil.fromIterator(
		   rpm.getAllReputationsTransferredFrom(peer1).iterator()));

    // No transfer in the param for this peer.
    assertEquals(ListUtil.list(peer4),
		 ListUtil.fromIterator(
		   rpm.getAllReputationsTransferredFrom(peer4).iterator()));
  }

  public void testEmptyMapping() throws Exception {
    ReputationTransfers rpm = new ReputationTransfers(idManager);
    assertNull(rpm.getReputationTransferredFrom(peer1));
    assertNull(rpm.getReputationTransferredFrom(peer2));
  }

  public void testConfigChanged() throws Exception {
    ReputationTransfers rpm = new ReputationTransfers(idManager);
    assertNull(rpm.getReputationTransferredFrom(peer1));
    assertNull(rpm.getReputationTransferredFrom(peer2));

    Configuration oldConfig = ConfigManager.getCurrentConfig();
    String xfermap = p1 + "," + p2;
    ConfigurationUtil.addFromArgs(
      ReputationTransfers.PARAM_REPUTATION_TRANSFER_MAP, xfermap);
    assertEquals(peer1, rpm.getReputationTransferredFrom(peer2));
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
    assertEquals(peer1, rpm.getReputationTransferredFrom(peer3));
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
    assertEquals(peer1, rpm.getReputationTransferredFrom(peer2));
    assertNull(rpm.getReputationTransferredFrom(peer3));
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
    assertEquals(peer1, rpm.getReputationTransferredFrom(peer2));
    assertEquals(peer2, rpm.getReputationTransferredFrom(peer1));
    assertEquals(ListUtil.list(peer1, peer2),
		 ListUtil.fromIterator(
		   rpm.getAllReputationsTransferredFrom(peer1).iterator()));
    assertEquals(ListUtil.list(peer2, peer1),
		 ListUtil.fromIterator(
		   rpm.getAllReputationsTransferredFrom(peer2).iterator()));
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
    assertEquals(null, rpm.getReputationTransferredFrom(peer1));
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
