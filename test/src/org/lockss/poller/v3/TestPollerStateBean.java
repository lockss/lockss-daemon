/*
 * $Id: TestPollerStateBean.java 39864 2015-02-18 09:10:24Z thib_gc $
 */

/*

 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;

import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.test.*;

import static org.lockss.util.Constants.*;

public class TestPollerStateBean extends LockssTestCase {

  private MockLockssDaemon theDaemon;

  private PeerIdentity pollerId;

  private IdentityManager idMgr;
  MockPlugin mplug;
  private MockArchivalUnit mau;
  PollerStateBean pollerState;
  
  private String localPeerKey = "TCP:[127.0.0.1]:9729";
  
  private File tempDir;

  public void setUp() throws Exception {
    super.setUp();
    ConfigurationUtil.addFromArgs(IdentityManager.PARAM_LOCAL_V3_IDENTITY,
				  localPeerKey);
    theDaemon = getMockLockssDaemon();
    idMgr = theDaemon.getIdentityManager();
    TimeBase.setSimulated();
    this.tempDir = getTempDir();
    mau = setupAu();
    pollerId = findPeerIdentity(localPeerKey);
    PollSpec pspec = new PollSpec(mau.getAuCachedUrlSet(), Poll.V3_POLL);
    pollerState = new PollerStateBean(pspec, pollerId, "key",
				      100, 200,
                                      0, 3, 50, "sha-1", 1,
				      V3Poller.PollVariant.PoR,
				      10);
  }

  private MockArchivalUnit setupAu() {
    mau = new MockArchivalUnit();
    mau.setAuId("mock");
    mplug = new MockPlugin(theDaemon);
    mau.setPlugin(mplug);
    return mau;
  }

  PeerIdentity findPeerIdentity(String key) throws Exception {
    PeerIdentity pid = idMgr.findPeerIdentity(key);
    // hack to ensure it's created
    idMgr.findLcapIdentity(pid, pid.getIdString());
    return pid;
  }

  public void testWeights() {
    PatternFloatMap pfm = PatternFloatMap.fromSpec(".*half.*,0.5;quarter,0.25");
    mau.setUrlPsllResultMap(pfm);
    PollerStateBean.TallyStatus ts = pollerState.getTallyStatus();
    pollerState.setUrlResultWeightMap(pfm);
    ts.addAgreedUrl("http://x.y/1");
    ts.addAgreedUrl("http://x.y/2");
    ts.addAgreedUrl("http://x.y/half1");
    ts.addAgreedUrl("http://x.y/quarter2");

    ts.addDisagreedUrl("http://x.y/half3");

    ts.addTooCloseUrl("http://x.y/full");
    ts.addTooCloseUrl("http://x.y/zyx/quarter/1");

    ts.addNoQuorumUrl("http://x.y/zyx/quarter/2");
    ts.addNoQuorumUrl("http://x.y/zyx/half/z");

    assertEquals(2.75F, ts.getWeightedAgreedCount());
    assertEquals(0.5F, ts.getWeightedDisagreedCount());
    assertEquals(1.25F, ts.getWeightedTooCloseCount());
    assertEquals(0.75F, ts.getWeightedNoQuorumCount());

    // Adding existing one to another set should decrease former set
    ts.addAgreedUrl("http://x.y/half3");
    assertEquals(3.25F, ts.getWeightedAgreedCount());
    assertEquals(0.0F, ts.getWeightedDisagreedCount());
    assertEquals(1.25F, ts.getWeightedTooCloseCount());
    assertEquals(0.75F, ts.getWeightedNoQuorumCount());
  }

}
