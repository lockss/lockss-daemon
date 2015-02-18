/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.protocol.AgreementType;
import org.lockss.protocol.IdentityManagerImpl;
import org.lockss.protocol.MockPeerIdentity;
import org.lockss.protocol.PeerAgreement;
import org.lockss.protocol.PeerIdentity;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.ws.entities.AgreementTypeWsResult;
import org.lockss.ws.entities.PeerAgreementWsResult;
import org.lockss.ws.entities.PeerAgreementsWsResult;

/**
 * Test class for org.lockss.ws.status.AuWsSource
 * 
 * @author Fernando Garcia-Loygorri
 */
public class TestAuWsSource extends LockssTestCase {

  public void setUp() throws Exception {
    super.setUp();

    setUpDiskSpace();

    MockLockssDaemon theDaemon = getMockLockssDaemon();

    MyIdentityManager idMgr = new MyIdentityManager();
    theDaemon.setIdentityManager(idMgr);
  }

  public void testGetPeerAgreements() throws Exception {
    AuWsSource source = new AuWsSource(null);
    List<PeerAgreementsWsResult> peerAgreements = source.getPeerAgreements();
    assertEquals(5, peerAgreements.size());

    for (PeerAgreementsWsResult result : peerAgreements) {
      validate(result);
    }
  }

  private void validate(PeerAgreementsWsResult result) {
    String peerId = result.getPeerId();
    Map<AgreementTypeWsResult, PeerAgreementWsResult> agreements =
	result.getAgreements();

    if ("tcp:[1.2.3.4]:12".equals(peerId)) {
      assertEquals(3, agreements.size());
      assertTrue(agreements.keySet().contains(AgreementTypeWsResult.POR));
      assertTrue(agreements.keySet().contains(AgreementTypeWsResult.POP));
      assertTrue(agreements.keySet().contains(AgreementTypeWsResult
	  .SYMMETRIC_POR_HINT));

      PeerAgreementWsResult pa = agreements.get(AgreementTypeWsResult.POR);
      assertEquals(0.2f, pa.getPercentAgreement());
      assertEquals(2L, pa.getPercentAgreementTimestamp().longValue());
      assertEquals(0.2f, pa.getHighestPercentAgreement());
      assertEquals(2L, pa.getHighestPercentAgreementTimestamp().longValue());

      pa = agreements.get(AgreementTypeWsResult.POP);
      assertEquals(0.22f, pa.getPercentAgreement());
      assertEquals(22L, pa.getPercentAgreementTimestamp().longValue());
      assertEquals(0.22f, pa.getHighestPercentAgreement());
      assertEquals(22L, pa.getHighestPercentAgreementTimestamp().longValue());

      pa = agreements.get(AgreementTypeWsResult.SYMMETRIC_POR_HINT);
      assertEquals(0.222f, pa.getPercentAgreement());
      assertEquals(222L, pa.getPercentAgreementTimestamp().longValue());
      assertEquals(0.222f, pa.getHighestPercentAgreement());
      assertEquals(222L, pa.getHighestPercentAgreementTimestamp().longValue());
    } else if ("tcp:[2.3.4.5]:23".equals(peerId)) {
      assertEquals(1, agreements.size());
      assertTrue(agreements.keySet().contains(AgreementTypeWsResult
	  .SYMMETRIC_POP_HINT));

      PeerAgreementWsResult pa =
	  agreements.get(AgreementTypeWsResult.SYMMETRIC_POP_HINT);
      assertEquals(0.23f, pa.getPercentAgreement());
      assertEquals(2345L, pa.getPercentAgreementTimestamp().longValue());
      assertEquals(0.234f, pa.getHighestPercentAgreement());
      assertEquals(234L, pa.getHighestPercentAgreementTimestamp().longValue());
    } else if ("tcp:[7.6.5.4]:76".equals(peerId)) {
      assertEquals(1, agreements.size());
      assertTrue(agreements.keySet().contains(AgreementTypeWsResult
	  .SYMMETRIC_POP_HINT));

      PeerAgreementWsResult pa =
	  agreements.get(AgreementTypeWsResult.SYMMETRIC_POP_HINT);
      assertEquals(0.54f, pa.getPercentAgreement());
      assertEquals(5454L, pa.getPercentAgreementTimestamp().longValue());
      assertEquals(0.76f, pa.getHighestPercentAgreement());
      assertEquals(76L, pa.getHighestPercentAgreementTimestamp().longValue());
    } else if ("tcp:[8.9.0.1]:89".equals(peerId)) {
      assertEquals(1, agreements.size());
      assertTrue(agreements.keySet().contains(AgreementTypeWsResult.POR));

      PeerAgreementWsResult pa = agreements.get(AgreementTypeWsResult.POR);
      assertEquals(0.9f, pa.getPercentAgreement());
      assertEquals(9L, pa.getPercentAgreementTimestamp().longValue());
      assertEquals(0.9f, pa.getHighestPercentAgreement());
      assertEquals(9L, pa.getHighestPercentAgreementTimestamp().longValue());
    } else if ("tcp:[9.8.7.6]:98".equals(peerId)) {
      assertEquals(2, agreements.size());
      assertTrue(agreements.keySet().contains(AgreementTypeWsResult.POP));

      PeerAgreementWsResult pa = agreements.get(AgreementTypeWsResult.POP);
      assertEquals(0.888f, pa.getPercentAgreement());
      assertEquals(888L, pa.getPercentAgreementTimestamp().longValue());
      assertEquals(0.99f, pa.getHighestPercentAgreement());
      assertEquals(99L, pa.getHighestPercentAgreementTimestamp().longValue());

      pa = agreements.get(AgreementTypeWsResult.SYMMETRIC_POR_HINT);
      assertEquals(0.8888f, pa.getPercentAgreement());
      assertEquals(8888L, pa.getPercentAgreementTimestamp().longValue());
      assertEquals(0.999f, pa.getHighestPercentAgreement());
      assertEquals(999L, pa.getHighestPercentAgreementTimestamp().longValue());
    }
  }

  public static class MyIdentityManager extends IdentityManagerImpl {
    public Map<PeerIdentity, PeerAgreement> getAgreements(ArchivalUnit au,
	AgreementType type) {
      Map<PeerIdentity, PeerAgreement> agreements =
	  new HashMap<PeerIdentity, PeerAgreement>();

      if (AgreementType.POR.equals(type)) {
	PeerIdentity pi1 = new MockPeerIdentity("tcp:[1.2.3.4]:12");
	PeerAgreement pa1 = PeerAgreement.NO_AGREEMENT;
	pa1 = pa1.signalAgreement(0.1f, 1L);
	pa1 = pa1.signalAgreement(0.2f, 2L);
	agreements.put(pi1, pa1);

	PeerIdentity pi2 = new MockPeerIdentity("tcp:[8.9.0.1]:89");
	PeerAgreement pa2 = PeerAgreement.NO_AGREEMENT;
	pa2 = pa2.signalAgreement(0.8f, 8L);
	pa2 = pa2.signalAgreement(0.9f, 9L);
	agreements.put(pi2, pa2);
      } else if (AgreementType.POP.equals(type)) {
	PeerIdentity pi1 = new MockPeerIdentity("tcp:[1.2.3.4]:12");
	PeerAgreement pa1 = PeerAgreement.NO_AGREEMENT;
	pa1 = pa1.signalAgreement(0.11f, 11L);
	pa1 = pa1.signalAgreement(0.22f, 22L);
	agreements.put(pi1, pa1);

	PeerIdentity pi2 = new MockPeerIdentity("tcp:[9.8.7.6]:98");
	PeerAgreement pa2 = PeerAgreement.NO_AGREEMENT;
	pa2 = pa2.signalAgreement(0.99f, 99L);
	pa2 = pa2.signalAgreement(0.888f, 888L);
	agreements.put(pi2, pa2);
      }	if (AgreementType.SYMMETRIC_POR_HINT.equals(type)) {
	PeerIdentity pi1 = new MockPeerIdentity("tcp:[1.2.3.4]:12");
	PeerAgreement pa1 = PeerAgreement.NO_AGREEMENT;
	pa1 = pa1.signalAgreement(0.111f, 111L);
	pa1 = pa1.signalAgreement(0.222f, 222L);
	agreements.put(pi1, pa1);

	PeerIdentity pi2 = new MockPeerIdentity("tcp:[9.8.7.6]:98");
	PeerAgreement pa2 = PeerAgreement.NO_AGREEMENT;
	pa2 = pa2.signalAgreement(0.999f, 999L);
	pa2 = pa2.signalAgreement(0.8888f, 8888L);
	agreements.put(pi2, pa2);
      } else if (AgreementType.SYMMETRIC_POP_HINT.equals(type)) {
	PeerIdentity pi1 = new MockPeerIdentity("tcp:[2.3.4.5]:23");
	PeerAgreement pa1 = PeerAgreement.NO_AGREEMENT;
	pa1 = pa1.signalAgreement(0.234f, 234L);
	pa1 = pa1.signalAgreement(0.23f, 2345L);
	agreements.put(pi1, pa1);

	PeerIdentity pi2 = new MockPeerIdentity("tcp:[7.6.5.4]:76");
	PeerAgreement pa2 = PeerAgreement.NO_AGREEMENT;
	pa2 = pa2.signalAgreement(0.76f, 76L);
	pa2 = pa2.signalAgreement(0.54f, 5454L);
	agreements.put(pi2, pa2);
      }

      return agreements;
    }
  }
}
