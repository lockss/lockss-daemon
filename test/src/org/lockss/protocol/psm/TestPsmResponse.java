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


public class TestPsmResponse extends LockssTestCase {

  PsmEvent event = new PsmEvent();

  PsmAction action = new PsmAction() {
      public PsmEvent run(PsmEvent event, PsmInterp interp) { return null; }};


  public void testNullConstructorArgs() {
    try {
      new PsmResponse(null, action);
      fail("null event should throw");
    } catch (PsmException.IllegalStateMachine e) { }
    try {
      new PsmResponse(PsmEvents.MsgEvent, (PsmAction)null);
      fail("null action should throw");
    } catch (PsmException.IllegalStateMachine e) { }
    try {
      new PsmResponse(PsmEvents.MsgEvent, (String)null);
      fail("null next state should throw");
    } catch (PsmException.IllegalStateMachine e) { }
    try {
      new PsmResponse(PsmEvents.MsgEvent, "");
      fail("null next state should throw");
    } catch (PsmException.IllegalStateMachine e) { }
  }

  public void testAccessors() {
    PsmResponse resp;
    resp = new PsmResponse(event, action);
    assertSame(event, resp.getEvent());
    assertSame(action, resp.getAction());
    assertTrue(resp.isAction());
    assertFalse(resp.isTransition());

    resp = new PsmResponse(event, "state_next");
    assertSame(event, resp.getEvent());
    assertEquals("state_next", resp.getNewState());
    assertTrue(resp.isTransition());
    assertFalse(resp.isAction());

    resp = new PsmResponse(event, PsmWait.FOREVER);
    assertSame(event, resp.getEvent());
    assertFalse(resp.isTransition());
    assertTrue(resp.isAction());
    assertTrue(resp.getAction().isWaitAction());
  }
}
