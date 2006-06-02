package org.lockss.test;

import org.lockss.poller.v3.*;
/*
 * $Id: V3TestUtil.java,v 1.4 2006-06-02 20:27:16 smorabito Exp $
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

// Extend LockssTestCase just to get at the 'assertXXX' methods.
// I suppose there's probably a better way to do this.
public class V3TestUtil extends LockssTestCase {

  /**
   * Assert that two participant user data objects contain the same
   * state.
   *
   * @param a ParticipantUserData the first.
   * @param b ParticipantUserData the second.
   *
   */
  public static void assertEqualParticipantUserData(ParticipantUserData a,
                                                    ParticipantUserData b) {
    assertEquals(a.getHashAlgorithm(), b.getHashAlgorithm());
    assertEquals(a.getIntroEffortProof(), b.getIntroEffortProof());
    assertEquals(a.getPollAckEffortProof(), b.getPollAckEffortProof());
    assertEquals(a.getPollerNonce(), b.getPollerNonce());
    assertEquals(a.getReceiptEffortProof(), b.getReceiptEffortProof());
    assertEquals(a.getRemainingEffortProof(), b.getRemainingEffortProof());
    assertEquals(a.getRepairEffortProof(), b.getRepairEffortProof());
    assertEquals(a.getVoterNonce(), b.getVoterNonce());
    assertEquals(a.getNominees(), b.getNominees());
    if (a.getVoteBlocks() == null) {
      assertNull(b.getVoteBlocks());
    } else {
      assertEquals(a.getVoteBlocks().size(), b.getVoteBlocks().size());
    }
    assertEquals(a.isOuterCircle(), b.isOuterCircle());
    assertEquals(a.isVoteComplete(), b.isVoteComplete());
    if (a.getPsmInterpState() == null) {
      assertNull(b.getPsmInterpState());
    } else {
      assertEquals(a.getPsmInterpState().getLastResumableStateName(),
                   b.getPsmInterpState().getLastResumableStateName());
    }
    if (a.getPsmInterp() == null) {
      assertNull(b.getPsmInterp());
    } else {
      assertEquals(a.getPsmInterp().isWaiting(),
                   b.getPsmInterp().isWaiting());
      assertEquals(a.getPsmInterp().getCurrentState().getName(),
                   b.getPsmInterp().getCurrentState().getName());
    }
  }

  /**
   * Assert that two PollerStateBean objects are the same.
   *
   * @param b1
   * @param b2
   */
  public static void assertEqualPollerStateBeans(PollerStateBean b1,
                                                 PollerStateBean b2) {
    assertEquals(b1.getAuId(), b2.getAuId());
    assertEquals(b1.getHashAlgorithm(), b2.getHashAlgorithm());
    assertEquals(b1.getLastHashedBlock(), b2.getLastHashedBlock());
    assertEquals(b1.getPluginVersion(), b2.getPluginVersion());
    assertEquals(b1.getUrl(), b2.getUrl());
    assertEquals(b1.getPollDeadline(), b2.getPollDeadline());
    assertEquals(b1.getPollerId().getIdString(),
                 b2.getPollerId().getIdString());
  }

  /**
   * Assert that two VoterUserData objects are the same.
   *
   * @param d1
   * @param d2
   */
  public static void assertEqualVoterUserData(VoterUserData d1,
                                              VoterUserData d2) {
    assertEquals(d1.getAuId(), d2.getAuId());
    assertEquals(d1.getHashAlgorithm(), d2.getHashAlgorithm());
    assertEquals(d1.getIntroEffortProof(), d2.getIntroEffortProof());
    assertEquals(d1.getPluginVersion(), d2.getPluginVersion());
    assertEquals(d1.getPollAckEffortProof(), d2.getPollAckEffortProof());
    assertEquals(d1.getPollerNonce(), d2.getPollerNonce());
    assertEquals(d1.getPollKey(), d2.getPollKey());
    assertEquals(d1.getReceiptEffortProof(), d2.getReceiptEffortProof());
    assertEquals(d1.getRemainingEffortProof(), d2.getRemainingEffortProof());
    assertEquals(d1.getRepairEffortProof(), d2.getRepairEffortProof());
    assertEquals(d1.getRepairTarget(), d2.getRepairTarget());
    assertEquals(d1.getVoterNonce(), d2.getVoterNonce());
    assertEquals(d1.getDeadline(), d2.getDeadline());
    assertEquals(d1.getNominees(), d2.getNominees());
    assertEquals(d1.getPollerId().getIdString(),
                 d2.getPollerId().getIdString());
    assertEquals(d1.getPollVersion(), d2.getPollVersion());
    assertEquals(d1.getVoteBlocks().size(), d2.getVoteBlocks().size());
  }
}



