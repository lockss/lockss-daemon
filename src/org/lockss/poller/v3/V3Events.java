/*
 * $Id: V3Events.java,v 1.1 2005-05-04 19:04:11 smorabito Exp $
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

import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;

public class V3Events {
  // Definitions
  public static class Ok extends PsmEvent {}
  public static class ReceiptOk extends PsmEvent {}
  public static class RepairOk extends PsmEvent {}
  public static class RepairNeeded extends PsmEvent {}
  public static class RepairNotNeeded extends PsmEvent{}

  public static class PollProof extends PsmMsgEvent {
    PollProof() { super(); }
    PollProof(LcapMessage msg) { super(msg); }
  }
  public static class RepairRequest extends PsmMsgEvent {
    RepairRequest() { super(); }
    RepairRequest(LcapMessage msg) { super(msg); }
  }
  public static class Repair extends PsmMsgEvent {
    Repair() { super(); }
    Repair(LcapMessage msg) { super(msg); }
  }
  public static class Receipt extends PsmMsgEvent {
    Receipt() { super(); }
    Receipt(LcapMessage msg) { super(msg); }
  }
  public static class PollAck extends PsmMsgEvent {
    PollAck() { super(); }
    PollAck(LcapMessage msg) { super(msg); }
  }
  public static class Vote extends PsmMsgEvent {
    Vote() { super(); }
    Vote(LcapMessage msg) { super(msg); }
  }

  // Instances
  public static PsmEvent evtElse = PsmEvents.Else;
  public static PsmEvent evtError = PsmEvents.Error;
  public static PsmEvent evtOk = new Ok();
  public static PsmEvent evtReceiptOk = new ReceiptOk();
  public static PsmEvent evtRepairOk = new RepairOk();
  public static PsmEvent evtRepairNeeded = new RepairNeeded();
  public static PsmEvent evtRepairNotNeeded = new RepairNotNeeded();
  
  public static PollAck msgPollAck = new PollAck();
  public static Vote msgVote = new Vote();
  public static PollProof msgPollProof = new PollProof();
  public static RepairRequest msgRepairRequest = new RepairRequest();
  public static Repair msgRepair = new Repair();
  public static Receipt msgReceipt = new Receipt();
}
