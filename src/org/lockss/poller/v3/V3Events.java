/*
 * $Id: V3Events.java,v 1.10 2008-01-27 06:46:04 tlipkis Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;

public class V3Events {
  // Definitions
  public static class Ok extends PsmEvent {}
  public static class VoteIncomplete extends PsmEvent {}
  public static class VoteComplete extends PsmEvent {}
  public static class ReceiptOk extends PsmEvent {}
  public static class RepairOk extends PsmEvent {}
  public static class NeedRepair extends PsmEvent {}
  public static class RepairNotNeeded extends PsmEvent {}
  public static class RepairRequestOk extends PsmEvent {}
  public static class ReadyToVote extends PsmEvent {}
  public static class Wait extends PsmEvent {}
  public static class NoSuchRepair extends PsmEvent {}
  public static class DeclinePoll extends PsmEvent {}
  public static class HashingDone extends PsmEvent {}
  public static class Finalize extends PsmEvent {}

  // Each msg event here probably wants a corresponsing instance below, and
  // an entry in msgEvents mapping opcode to instance
  public static class Poll extends PsmMsgEvent {}
  public static class PollProof extends PsmMsgEvent {}
  public static class RepairRequest extends PsmMsgEvent {}
  public static class Repair extends PsmMsgEvent {}
  public static class Receipt extends PsmMsgEvent {}
  public static class PollAck extends PsmMsgEvent {}
  public static class VoteRequest extends PsmMsgEvent {}
  public static class Vote extends PsmMsgEvent {}
  public static class Nominate extends PsmMsgEvent {}


  // Instances
  public static PsmEvent evtElse = PsmEvents.Else;
  public static PsmEvent evtError = PsmEvents.Error;
  public static PsmEvent evtOk = new Ok();
  public static PsmEvent evtVoteIncomplete = new VoteIncomplete();
  public static PsmEvent evtVoteComplete = new VoteComplete();
  public static PsmEvent evtReceiptOk = new ReceiptOk();
  public static PsmEvent evtRepairOk = new RepairOk();
  public static PsmEvent evtRepairRequestOk = new RepairRequestOk();
  public static PsmEvent evtNeedRepair = new NeedRepair();
  public static PsmEvent evtRepairNotNeeded = new RepairNotNeeded();
  public static PsmEvent evtReadyToVote = new ReadyToVote();
  public static PsmEvent evtWait = new Wait();
  public static PsmEvent evtNoSuchRepair = new NoSuchRepair();
  public static PsmEvent evtDeclinePoll = new DeclinePoll();
  public static PsmEvent evtHashingDone = new HashingDone();
  public static PsmEvent evtFinalize = new Finalize();

  public static Poll msgPoll = new Poll();
  public static PollAck msgPollAck = new PollAck();
  public static Nominate msgNominate = new Nominate();
  public static VoteRequest msgVoteRequest = new VoteRequest();
  public static Vote msgVote = new Vote();
  public static PollProof msgPollProof = new PollProof();
  public static RepairRequest msgRepairRequest = new RepairRequest();
  public static Repair msgRepair = new Repair();
  public static Receipt msgReceipt = new Receipt();

  // Mapping of message opcode to event class (prototype)
  private static final Map msgEvents = new HashMap();
  static {
    msgEvents.put(new Integer(V3LcapMessage.MSG_POLL), msgPoll);
    msgEvents.put(new Integer(V3LcapMessage.MSG_POLL_ACK), msgPollAck);
    msgEvents.put(new Integer(V3LcapMessage.MSG_POLL_PROOF), msgPollProof);
    msgEvents.put(new Integer(V3LcapMessage.MSG_NOMINATE), msgNominate);
    msgEvents.put(new Integer(V3LcapMessage.MSG_VOTE_REQ), msgVoteRequest);
    msgEvents.put(new Integer(V3LcapMessage.MSG_VOTE), msgVote);
    msgEvents.put(new Integer(V3LcapMessage.MSG_REPAIR_REQ), msgRepairRequest);
    msgEvents.put(new Integer(V3LcapMessage.MSG_REPAIR_REP), msgRepair);
    msgEvents.put(new Integer(V3LcapMessage.MSG_EVALUATION_RECEIPT), msgReceipt);
  }

  /** Return an opcode-specific message event, with the message as its
   * payload */
  public static PsmMsgEvent fromMessage(V3LcapMessage msg) {
    return PsmMsgEvent.fromMessage(msg, msgEvents);
  }

}
