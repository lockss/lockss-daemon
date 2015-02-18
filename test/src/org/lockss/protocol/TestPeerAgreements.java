/*
 * $Id$
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.test.*;
import org.lockss.util.*;

import java.util.*;


public class TestPeerAgreements extends LockssTestCase {

  public void testCreate() throws Exception {
    new PeerAgreements("id0");
    new PeerAgreements(new MockPeerIdentity("id0"));

    try {
      new PeerAgreements((String)null);
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testCreateFromIdentityAgreement() throws Exception {
    // Create pre-1.62 IdentityAgreement.
    IdentityManager.IdentityAgreement identityAgreement =
      new IdentityManager.IdentityAgreement("id0");
    identityAgreement.setLastAgree(1000);
    identityAgreement.setLastDisagree(2000);
    // call twice with a lower value than the first time, to make
    // the highest and the most recent different.
    identityAgreement.setPercentAgreement(0.5f);
    identityAgreement.setPercentAgreement(0.4f);
    identityAgreement.setPercentAgreementHint(0.3f);
    identityAgreement.setPercentAgreementHint(0.2f);

    PeerAgreements peerAgreements = PeerAgreements.from(identityAgreement);
    assertEquals("id0", peerAgreements.getId());
    PeerAgreement porAgreement = 
      peerAgreements.getPeerAgreement(AgreementType.POR);
    assertEquals(0.4f, porAgreement.getPercentAgreement());
    assertEquals(0.5f, porAgreement.getHighestPercentAgreement());
    assertEquals(2000, porAgreement.getPercentAgreementTime());
    assertEquals(0, porAgreement.getHighestPercentAgreementTime());

    PeerAgreement porHintAgreement = 
      peerAgreements.getPeerAgreement(AgreementType.POR_HINT);
    assertEquals(0.2f, porHintAgreement.getPercentAgreement());
    assertEquals(0.3f, porHintAgreement.getHighestPercentAgreement());
    assertEquals(0, porHintAgreement.getPercentAgreementTime());
    assertEquals(0, porHintAgreement.getHighestPercentAgreementTime());
  }

  public void testSignalValues() throws Exception {
    PeerAgreements peerAgreements = new PeerAgreements("id0");
    assertEquals("id0", peerAgreements.getId());

    peerAgreements.signalAgreement(AgreementType.POR, 0.5f, 1000);
    PeerAgreement porAgreement = 
      peerAgreements.getPeerAgreement(AgreementType.POR);
    assertEquals(0.5f, porAgreement.getPercentAgreement());
    assertEquals(0.5f, porAgreement.getHighestPercentAgreement());
    assertEquals(1000, porAgreement.getPercentAgreementTime());
    assertEquals(1000, porAgreement.getHighestPercentAgreementTime());

    peerAgreements.signalAgreement(AgreementType.POR, 0.4f, 2000);
    porAgreement = peerAgreements.getPeerAgreement(AgreementType.POR);
    assertEquals(0.4f, porAgreement.getPercentAgreement());
    assertEquals(0.5f, porAgreement.getHighestPercentAgreement());
    assertEquals(2000, porAgreement.getPercentAgreementTime());
    assertEquals(1000, porAgreement.getHighestPercentAgreementTime());
  }
}
