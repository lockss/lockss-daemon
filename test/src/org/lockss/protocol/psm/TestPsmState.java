/*
 * $Id$
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

  public void testIllResponse() {
    PsmResponse rok = new PsmResponse(new PsmEvent(), "Start");
    PsmResponse rerr = new PsmResponse(new PsmWaitEvent(), "Start");
    new PsmState("start", action, rok);
    try {
      new PsmState("start", action, rerr);
      fail("Response for wait event should be illegal");
    } catch (PsmException.IllegalStateMachine e) { }
  }

  public void testUnmatchableResponse() {
    String msg = "Impossible-to-match response should throw";
    assertTrue(r2.getEvent().isa(r1.getEvent()));
    new PsmState("s1", action, r2aa, r2a, r2b, r2, r1);
    try {
      new PsmState("s1", action, r1, r2);
      fail(msg);
    } catch (PsmException.IllegalStateMachine e) { }
    new PsmState("s1", action, r2aa, r2a, r2b, r2, r1);
    try {
      new PsmState("s1", action, r2a, r2b, r2, r1, r2aa);
      fail(msg);
    } catch (RuntimeException e) { }
  }

  public void testGetResponse() {
    PsmState s1;
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

  public void testResumable() {
    PsmState s1 = new PsmState("s1");
    PsmState s2 = new PsmState("s2", action);
    assertFalse(s1.isResumable());
    assertSame(s1, s1.setResumable(true));
    assertTrue(s1.isResumable());
    assertSame(s1, s1.setResumable(false));
    assertFalse(s1.isResumable());
  }

  public void testIsFinal() {
    PsmState s1 = new PsmState("s1");
    PsmState s2 = new PsmState("s1", action);
    PsmState s3 = new PsmState("s1", action,
			       new PsmResponse(PsmEvents.Else, "s1"));
    PsmState s4 = new PsmState("s1", PsmWait.FOREVER,
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

    s1 = new PsmState("s1", action, r2, r1);
    assertEquals("s1", s1.getName());
    assertSame(action, s1.getEntryAction());
    assertIsomorphic(ListUtil.list(r2, r1), s1.getResponses());

    s1 = new PsmState("s1", action, r2a, r2, r1);
    assertIsomorphic(ListUtil.list(r2a, r2, r1), s1.getResponses());

    s1 = new PsmState("s1", action, r2aa, r2a, r2, r1);
    assertIsomorphic(ListUtil.list(r2aa, r2a, r2, r1), s1.getResponses());

    s1 = new PsmState("s1", action, r2b, r2aa, r2a, r2, r1);
    assertIsomorphic(ListUtil.list(r2b, r2aa, r2a, r2, r1), s1.getResponses());

    s1 = new PsmState("s1", action, r2c, r2b, r2aa, r2a, r2, r1);
    assertIsomorphic(ListUtil.list(r2c, r2b, r2aa, r2a, r2, r1),
		     s1.getResponses());

    s1 = new PsmState("s1");
    assertEquals("s1", s1.getName());
    assertTrue(s1.getEntryAction().isWaitAction());
    assertEquals(0, s1.getResponses().length);

    s1 = new PsmState("s1", PsmWait.FOREVER, r1);
    assertEquals("s1", s1.getName());
    assertTrue(s1.getEntryAction().isWaitAction());
    assertIsomorphic(ListUtil.list(r1), s1.getResponses());

    s1 = new PsmState("s1", PsmWait.FOREVER, r2, r1);
    assertIsomorphic(ListUtil.list(r2, r1), s1.getResponses());

    s1 = new PsmState("s1", PsmWait.FOREVER, r2a, r2, r1);
    assertIsomorphic(ListUtil.list(r2a, r2, r1), s1.getResponses());

    s1 = new PsmState("s1", PsmWait.FOREVER, r2aa, r2a, r2, r1);
    assertIsomorphic(ListUtil.list(r2aa, r2a, r2, r1), s1.getResponses());

    s1 = new PsmState("s1", PsmWait.FOREVER, r2b, r2aa, r2a, r2, r1);
    assertIsomorphic(ListUtil.list(r2b, r2aa, r2a, r2, r1), s1.getResponses());

    s1 = new PsmState("s1", PsmWait.FOREVER, r2c, r2b, r2aa, r2a, r2, r1);
    assertIsomorphic(ListUtil.list(r2c, r2b, r2aa, r2a, r2, r1),
		     s1.getResponses());
  }

}
