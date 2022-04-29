/*
 * $Id$
 */

/*

Copyright (c) 2013-2016 Board of Trustees of Leland Stanford Jr. University,
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


public class TestPeerAgreement extends LockssTestCase {

  public void testNoAgreementInstance() {
    assertEquals(-1.0f, PeerAgreement.NO_AGREEMENT.getPercentAgreement());
    assertEquals(0, PeerAgreement.NO_AGREEMENT.getPercentAgreementTime());
    assertEquals(-1.0f, PeerAgreement.NO_AGREEMENT.getHighestPercentAgreement());
    assertEquals(0, PeerAgreement.NO_AGREEMENT.getHighestPercentAgreementTime());
  }

  public void testSignalAgreement() {
    PeerAgreement agreement =
      PeerAgreement.NO_AGREEMENT.signalAgreement(0.5f, 100);
    assertEquals(0.5f, agreement.getPercentAgreement());
    assertEquals(100, agreement.getPercentAgreementTime());
    assertEquals(0.5f, agreement.getHighestPercentAgreement());
    assertEquals(100, agreement.getHighestPercentAgreementTime());
    // ensure that the NO_AGREEMENT instance wasn't changed.
    testNoAgreementInstance();

    // Make a new one with another signal.
    agreement = agreement.signalAgreement(0.4f, 200);
    assertEquals(0.4f, agreement.getPercentAgreement());
    assertEquals(200, agreement.getPercentAgreementTime());
    assertEquals(0.5f, agreement.getHighestPercentAgreement());
    assertEquals(100, agreement.getHighestPercentAgreementTime());

    // Make a new one with another signal, with a better percent
    agreement = agreement.signalAgreement(0.6f, 300);
    assertEquals(0.6f, agreement.getPercentAgreement());
    assertEquals(300, agreement.getPercentAgreementTime());
    assertEquals(0.6f, agreement.getHighestPercentAgreement());
    assertEquals(300, agreement.getHighestPercentAgreementTime());

    // Make a new one with another signal, with time going backwards;
    // perfectly ok.
    agreement = agreement.signalAgreement(0.3f, 50);
    assertEquals(0.3f, agreement.getPercentAgreement());
    assertEquals(50, agreement.getPercentAgreementTime());
    assertEquals(0.6f, agreement.getHighestPercentAgreement());
    assertEquals(300, agreement.getHighestPercentAgreementTime());
  }

  public void testSignalAgreementThrow() {
    PeerAgreement.NO_AGREEMENT.signalAgreement(1.0f, 100);
    PeerAgreement.NO_AGREEMENT.signalAgreement(0.0f, 100);

    try {
      PeerAgreement.NO_AGREEMENT.signalAgreement(1.00001f, 100);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      PeerAgreement.NO_AGREEMENT.signalAgreement(-0.00001f, 100);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }

    // Negative time? Sure!
    PeerAgreement.NO_AGREEMENT.signalAgreement(1.0f, -100);
  }

  public void testMerge() {
    assertEquals(PeerAgreement.NO_AGREEMENT,
		 PeerAgreement.NO_AGREEMENT.mergeWith(null));
    assertEquals(PeerAgreement.NO_AGREEMENT,
		 PeerAgreement.NO_AGREEMENT.mergeWith(PeerAgreement.NO_AGREEMENT));

    PeerAgreement a1 =
      PeerAgreement.NO_AGREEMENT.signalAgreement(0.5f, 100);
    assertEquals(a1, a1.mergeWith(a1));
    assertEquals(a1, a1.mergeWith(PeerAgreement.NO_AGREEMENT));
    assertEquals(a1, PeerAgreement.NO_AGREEMENT.mergeWith(a1));
    assertEquals(a1, a1.mergeWith(null));

    PeerAgreement a2 =
      PeerAgreement.NO_AGREEMENT.signalAgreement(0.75f, 200);
    assertEquals(a2, a2.mergeWith(a1));
    assertEquals(a2, a1.mergeWith(a2));
    assertEquals(a2, a2.mergeWith(a1));

    PeerAgreement a3 =
      PeerAgreement.NO_AGREEMENT.signalAgreement(0.6f, 50);
    PeerAgreement a13 =
      PeerAgreement.NO_AGREEMENT
      .signalAgreement(0.6f, 50)
      .signalAgreement(0.5f, 100);
    assertEquals(a3, a3.mergeWith(a3));
    assertEquals(a13, a1.mergeWith(a3));
    assertEquals(a13, a3.mergeWith(a1));
  }

  // Test the conversion from IdentityAgreement to PeerAgreement.
  public void testIdentityAgreement() {
    IdentityManager.IdentityAgreement idAgreement =
      new IdentityManager.IdentityAgreement("foo");
    idAgreement.setLastAgree(100);
    idAgreement.setLastDisagree(200);
    idAgreement.setPercentAgreement(0.5f);
    idAgreement.setPercentAgreement(0.4f);
    PeerAgreement agreement = PeerAgreement.porAgreement(idAgreement);

    assertEquals(0.4f, agreement.getPercentAgreement());
    assertEquals(200, agreement.getPercentAgreementTime());
    assertEquals(0.5f, agreement.getHighestPercentAgreement());
    assertEquals(0, agreement.getHighestPercentAgreementTime());

    idAgreement.setPercentAgreementHint(0.7f);
    idAgreement.setPercentAgreementHint(0.6f);
    agreement = PeerAgreement.porAgreementHint(idAgreement);

    assertEquals(0.6f, agreement.getPercentAgreement());
    assertEquals(0, agreement.getPercentAgreementTime());
    assertEquals(0.7f, agreement.getHighestPercentAgreement());
    assertEquals(0, agreement.getHighestPercentAgreementTime());
  }
}
