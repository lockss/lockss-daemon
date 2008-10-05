/*
 * $Id: TestPsmEvent.java,v 1.2 2005-06-04 21:37:12 tlipkis Exp $
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

import org.lockss.test.*;


public class TestPsmEvent extends LockssTestCase {

  class MsgEventInvitation extends PsmMsgEvent {
  }

  class MsgEventEngravedInvitation extends MsgEventInvitation {
  }

  class MsgEventVote extends PsmMsgEvent {
  }

  public void testUserVal() {
    PsmEvent e = new PsmEvent();
    assertEquals(0, e.getUserVal());
    PsmEvent e2 = e.withUserVal(12345);
    assertEquals(12345, e2.getUserVal());
    assertNotSame(e, e2);
    assertEquals(0, e.getUserVal());
  }

  public void testIsa() {
    PsmEvent vote = new MsgEventVote();
    PsmEvent invite = new MsgEventInvitation();
    PsmEvent engraved = new MsgEventEngravedInvitation();
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
