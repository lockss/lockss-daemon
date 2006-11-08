/*
 * $Id: TestPsmMsgEvent.java,v 1.6 2006-11-08 16:42:59 smorabito Exp $
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

package org.lockss.protocol.psm;

import java.io.File;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.ByteArray;
import org.lockss.protocol.*;


public class TestPsmMsgEvent extends LockssTestCase {

  File tempDir;

  public void setUp() throws Exception {
    super.setUp();
    tempDir = getTempDir();
  }

  class MsgEventInvitation extends PsmMsgEvent {
  }

  class MsgEventEngravedInvitation extends MsgEventInvitation {
  }

  class MsgEventVote extends PsmMsgEvent {
  }


  public void testMessage() {
    LcapMessage msg1 = new V3LcapMessage(null, getMockLockssDaemon());
    PsmMsgEvent e = new PsmMsgEvent();
    assertEquals(null, e.getMessage());
    PsmMsgEvent e2 = e.withMessage(msg1);
    assertSame(msg1, e2.getMessage());
    assertNotSame(e, e2);
    assertEquals(null, e.getMessage());
  }

  V3LcapMessage makeMsg(int opcode) {
    return new V3LcapMessage("ArchivalID_2", "key", "Plug42",
                             ByteArray.makeRandomBytes(20),
                             ByteArray.makeRandomBytes(20),
                             opcode, 987654321, null, tempDir,
                             getMockLockssDaemon());
  }

  public void testFromMessage() {
    Map msgEvents = new HashMap();
    msgEvents.put(new Integer(10), new MsgEventInvitation());
    msgEvents.put(new Integer(11), new MsgEventEngravedInvitation());
    V3LcapMessage msg1 = makeMsg(10);
    PsmMsgEvent e1 = PsmMsgEvent.fromMessage(msg1, msgEvents);
    assertEquals(MsgEventInvitation.class, e1.getClass());
    assertEquals(msg1, e1.getMessage());
    V3LcapMessage msg2 = makeMsg(11);
    PsmMsgEvent e2 = PsmMsgEvent.fromMessage(msg2, msgEvents);
    assertEquals(MsgEventEngravedInvitation.class, e2.getClass());
    assertEquals(msg2, e2.getMessage());
    V3LcapMessage msg3 = makeMsg(12);
    PsmMsgEvent e3 = PsmMsgEvent.fromMessage(msg3, msgEvents);
    assertEquals(PsmMsgEvent.class, e3.getClass());
    assertEquals(msg3, e3.getMessage());
  }

  public void testIsa() {
    PsmMsgEvent vote = new MsgEventVote();
    PsmMsgEvent invite = new MsgEventInvitation();
    PsmMsgEvent engraved = new MsgEventEngravedInvitation();
    assertTrue(invite.isa(PsmEvents.Event));
    assertTrue(invite.isa(PsmEvents.MsgEvent));
    assertTrue(invite.isa(invite));
    assertTrue(engraved.isa(invite));
    assertTrue(engraved.isa(PsmEvents.MsgEvent));
    assertFalse(invite.isa(engraved));
    assertFalse(invite.isa(vote));
    assertFalse(vote.isa(invite));
  }
}
