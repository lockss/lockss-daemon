/*
 * $Id: V3LcapMessageFactory.java,v 1.1 2005-07-13 07:53:06 smorabito Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;
import org.mortbay.util.B64Code;

/**
 * Factory methods used by the V3 Polling system to create instances of
 * V3LcapMessages.
 */
public class V3LcapMessageFactory {

  /**
   * Make a Poll message.
   */
  public static V3LcapMessage makePollMsg(V3VoterState vs) {
    V3LcapMessage msg =
      V3LcapMessage.makeRequestMsg(vs.getPollSpec(),
				   vs.getChallenge(),
				   V3LcapMessage.MSG_POLL,
				   vs.getDeadline(),
				   vs.getPollerId());
    msg.setEffortProof(vs.getIntroEffortProof());
    return msg;
  }

  /**
   * Make a Poll Proof message.
   */
  public static V3LcapMessage makePollProofMsg(V3VoterState vs) {
    V3LcapMessage msg =
      V3LcapMessage.makeRequestMsg(vs.getPollSpec(),
				   vs.getChallenge(),
				   V3LcapMessage.MSG_POLL_PROOF,
				   vs.getDeadline(),
				   vs.getPollerId());
    msg.setEffortProof(vs.getRemainingEffortProof());
    return msg;
  }

  /**
   * Make a Vote Request message.
   */
  public static V3LcapMessage makeVoteRequestMsg(V3VoterState vs) {
    return V3LcapMessage.makeRequestMsg(vs.getPollSpec(),
					vs.getChallenge(),
					V3LcapMessage.MSG_VOTE_REQ,
					vs.getDeadline(),
					vs.getPollerId());
  }

  /**
   * Make a Repair Request message.
   */
  public static V3LcapMessage makeRepairRequestMsg(V3VoterState vs) {
    V3LcapMessage msg =
      V3LcapMessage.makeRequestMsg(vs.getPollSpec(),
				   vs.getChallenge(),
				   V3LcapMessage.MSG_REPAIR_REQ,
				   vs.getDeadline(),
				   vs.getPollerId());
    msg.setTarget(vs.getTarget());
    msg.setEffortProof(vs.getRepairEffortProof());
    return msg;
  }

  /**
   * Make an Evaluation Receipt message.
   */
  public static V3LcapMessage makeEvaluationReceiptMsg(V3VoterState vs) {
    V3LcapMessage msg =
      V3LcapMessage.makeRequestMsg(vs.getPollSpec(),
				   vs.getChallenge(),
				   V3LcapMessage.MSG_EVALUATION_RECEIPT,
				   vs.getDeadline(),
				   vs.getPollerId());
    msg.setEffortProof(vs.getReceiptEffortProof());
    return msg;
  }

}
