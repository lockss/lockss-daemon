/*
 * $Id: TestPsmState.java,v 1.2 2005-02-24 04:25:59 tlipkis Exp $
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
import org.lockss.util.*;


public class TestPsmState extends LockssTestCase {

  public void testNullConstructorArgs() {
    try {
      new PsmState(null);
      fail("null name should throw");
    } catch (RuntimeException e) { }
    try {
      new PsmState(null, (PsmAction)null);
      fail("null name should throw");
    } catch (RuntimeException e) { }
    try {
      new PsmState("s1", (PsmAction)null, (PsmResponse)null);
      fail("null response should throw");
    } catch (RuntimeException e) { }
    try {
      new PsmState("s1", (PsmAction)null, (PsmResponse[])null);
      fail("null response array should throw");
    } catch (RuntimeException e) { }
  }

  private class Invitation extends PsmMsgEvent {}
  private class EngravedInvitation extends Invitation {}
  private class Vote extends PsmMsgEvent {}
  private class Solicit extends PsmMsgEvent {}
  PsmEvent Invitation = new Invitation();
  PsmEvent EngravedInvitation = new EngravedInvitation();
  PsmEvent Vote = new Vote();
  PsmEvent Solicit = new Solicit();

  PsmAction action = new PsmAction() {
      public PsmEvent run(PsmEvent event, PsmInterp interp) { return null; }};
  PsmResponse r1 = new PsmResponse(PsmEvents.Else, "s1");
  PsmResponse r2 = new PsmResponse(PsmEvents.MsgEvent, "sMsg");
  PsmResponse r2a = new PsmResponse(Invitation, "sInv");
  PsmResponse r2aa = new PsmResponse(EngravedInvitation, "sEng");
  PsmResponse r2b = new PsmResponse(Vote, "sVote");
  PsmResponse r2c = new PsmResponse(Solicit, "sSol");

  public void testGetResponse() {
    PsmState s1;
    s1 = new PsmState("s1", action, r1, r2);
    assertSame(r1, s1.getResponse(PsmEvents.Else));
    assertSame(r1, s1.getResponse(PsmEvents.MsgEvent));
    assertSame(r1, s1.getResponse(Vote));

    s1 = new PsmState("s1", action, r2, r1);
    assertSame(r1, s1.getResponse(PsmEvents.Else));
    assertSame(r2, s1.getResponse(PsmEvents.MsgEvent));
    assertSame(r2, s1.getResponse(Vote));

    s1 = new PsmState("s1", action, r2b, r2);
    assertSame(null, s1.getResponse(PsmEvents.Else));
    assertSame(r2, s1.getResponse(PsmEvents.MsgEvent));
    assertSame(r2b, s1.getResponse(Vote));

    s1 = new PsmState("s1", action, r2aa, r2a, r2b, r2, r1);
    assertSame(r1, s1.getResponse(PsmEvents.Else));
    assertSame(r2, s1.getResponse(PsmEvents.MsgEvent));
    assertSame(r2a, s1.getResponse(Invitation));
    assertSame(r2aa, s1.getResponse(EngravedInvitation));
    assertSame(r2b, s1.getResponse(Vote));
    assertSame(r2, s1.getResponse(Solicit));
    assertSame(r2, s1.getResponse(new PsmMsgEvent()));
  }

  public void testIsFinal() {
    PsmState s1 = new PsmState("s1");
    PsmState s2 = new PsmState("s1", action);
    PsmState s3 = new PsmState("s1", action,
			       new PsmResponse(PsmEvents.Else, "s1"));
    PsmState s4 = new PsmState("s1",
			       new PsmResponse(PsmEvents.Else, "s1"));
    assertTrue(s1.isFinal());
    assertTrue(s2.isFinal());
    assertFalse(s3.isFinal());
    assertFalse(s4.isFinal());
  }

  public void testIsSucceed() {
    PsmState s1 = new PsmState("s1");
    PsmState s2 = new PsmState("s1", action);
    PsmState s3 = new PsmState("s1", action).succeed();
    PsmState s4 = new PsmState("s1").fail();
    assertFalse(s1.isSucceed());
    assertFalse(s1.isFail());
    assertFalse(s2.isSucceed());
    assertFalse(s2.isFail());
    assertTrue(s3.isSucceed());
    assertFalse(s3.isFail());
    assertFalse(s4.isSucceed());
    assertTrue(s4.isFail());
    assertSame(s1, s1.succeed());
    assertSame(s2, s2.fail());
    assertTrue(s1.isSucceed());
    assertFalse(s1.isFail());
    assertSame(s1, s1.fail());
    assertFalse(s1.isSucceed());
    assertTrue(s1.isFail());
  }

  public void testConstructors() {
    PsmState s1;
    s1 = new PsmState("s1", action);
    assertEquals("s1", s1.getName());
    assertSame(action, s1.getEntryAction());
    assertEquals(0, s1.getResponses().length);

    s1 = new PsmState("s1", action, r1);
    assertEquals("s1", s1.getName());
    assertSame(action, s1.getEntryAction());
    assertIsomorphic(ListUtil.list(r1), s1.getResponses());

    s1 = new PsmState("s1", action, r1, r2);
    assertEquals("s1", s1.getName());
    assertSame(action, s1.getEntryAction());
    assertIsomorphic(ListUtil.list(r1, r2), s1.getResponses());

    s1 = new PsmState("s1", action, r1, r2, r2a);
    assertIsomorphic(ListUtil.list(r1, r2, r2a), s1.getResponses());

    s1 = new PsmState("s1", action, r1, r2, r2a, r2aa);
    assertIsomorphic(ListUtil.list(r1, r2, r2a, r2aa), s1.getResponses());

    s1 = new PsmState("s1", action, r1, r2, r2a, r2aa, r2b);
    assertIsomorphic(ListUtil.list(r1, r2, r2a, r2aa, r2b), s1.getResponses());

    s1 = new PsmState("s1", action, r1, r2, r2a, r2aa, r2b, r2c);
    assertIsomorphic(ListUtil.list(r1, r2, r2a, r2aa, r2b, r2c),
		     s1.getResponses());

    s1 = new PsmState("s1");
    assertEquals("s1", s1.getName());
    assertNull(s1.getEntryAction());
    assertEquals(0, s1.getResponses().length);

    s1 = new PsmState("s1", r1);
    assertEquals("s1", s1.getName());
    assertNull(s1.getEntryAction());
    assertIsomorphic(ListUtil.list(r1), s1.getResponses());

    s1 = new PsmState("s1", r1, r2);
    assertIsomorphic(ListUtil.list(r1, r2), s1.getResponses());

    s1 = new PsmState("s1", r1, r2, r2a);
    assertIsomorphic(ListUtil.list(r1, r2, r2a), s1.getResponses());

    s1 = new PsmState("s1", r1, r2, r2a, r2aa);
    assertIsomorphic(ListUtil.list(r1, r2, r2a, r2aa), s1.getResponses());

    s1 = new PsmState("s1", r1, r2, r2a, r2aa, r2b);
    assertIsomorphic(ListUtil.list(r1, r2, r2a, r2aa, r2b), s1.getResponses());

    s1 = new PsmState("s1", r1, r2, r2a, r2aa, r2b, r2c);
    assertIsomorphic(ListUtil.list(r1, r2, r2a, r2aa, r2b, r2c),
		     s1.getResponses());
  }

}
