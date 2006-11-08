/*
 * $Id: TestV3Events.java,v 1.5 2006-11-08 16:42:58 smorabito Exp $
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

package org.lockss.poller.v3;

import java.util.*;
import org.lockss.test.*;
import org.lockss.protocol.*;
import org.lockss.protocol.psm.*;


public class TestV3Events extends LockssTestCase {

  V3LcapMessage makeMsg(int opcode) {
    return new V3LcapMessage("auid", "key", "1", null, null, opcode,
                             987654321, null, null, null);
  }

  void assertMsgClass(int opcode, PsmMsgEvent prototypeEvent) {
    PsmMsgEvent e = V3Events.fromMessage(makeMsg(opcode));
    assertEquals(prototypeEvent.getClass(), e.getClass());
  }

  public void testFromMessage() {
    assertMsgClass(V3LcapMessage.MSG_POLL_ACK, V3Events.msgPollAck);
    assertMsgClass(V3LcapMessage.MSG_POLL_PROOF, V3Events.msgPollProof);
    assertMsgClass(V3LcapMessage.MSG_VOTE, V3Events.msgVote);
    assertMsgClass(V3LcapMessage.MSG_REPAIR_REQ, V3Events.msgRepairRequest);
    assertMsgClass(V3LcapMessage.MSG_REPAIR_REP, V3Events.msgRepair);
    assertMsgClass(V3LcapMessage.MSG_EVALUATION_RECEIPT, V3Events.msgReceipt);
  }
}
