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


public class TestPsmMachine extends LockssTestCase {

  PsmState[] states1 = {
    new PsmState("Start", PsmWait.FOREVER,
		 new PsmResponse(PsmEvents.Else, "Foo")),
    new PsmState("Foo"),
  };

  public void testNullConstructorArgs() {
    try {
      new PsmMachine(null, states1, "Start");
      fail("null name should throw");
    } catch (PsmException.IllegalStateMachine e) { }
    try {
      new PsmMachine("Test1", null, "Start");
      fail("null states should throw");
    } catch (PsmException.IllegalStateMachine e) { }
    try {
      new PsmMachine("Test1", states1, (String)null);
      fail("null initial state should throw");
    } catch (PsmException.IllegalStateMachine e) { }
    try {
      new PsmMachine("Test1", states1, (PsmState)null);
      fail("null initial state should throw");
    } catch (PsmException.IllegalStateMachine e) { }
  }

  public void testIllMachine() {
    try {
      new PsmMachine("Test1", states1, "NoState");
      fail("Bad initial state name should throw");
    } catch (PsmException.IllegalStateMachine e) { }
    try {
      new PsmMachine("Test1", states1, new PsmState("X"));
      fail("Bad initial state");
    } catch (PsmException.IllegalStateMachine e) { }
    try {
      new PsmMachine("Test1", states1, new PsmState("Foo"));
      fail("Bad initial state");
    } catch (PsmException.IllegalStateMachine e) { }
  }

  public void testIllTrans() {
    PsmState[] statesOk = {
      new PsmState("Start", PsmWait.FOREVER,
		   new PsmResponse(PsmEvents.Else, "Foo")),
      new PsmState("Foo"),
    };
    PsmState[] statesBad = {
      new PsmState("Start", PsmWait.FOREVER,
		   new PsmResponse(PsmEvents.Else, "Bar")),
      new PsmState("Foo"),
    };
    new PsmMachine("Test1", statesOk, "Start");
    try {
      new PsmMachine("Test1", statesBad, "Start");
      fail("Tansition to nonexistent state");
    } catch (PsmException.IllegalStateMachine e) { }
  }

  public void testLegalMachine() {
    PsmMachine m1 = new PsmMachine("Test1", states1, "Start");
    assertEquals("Test1", m1.getName());
    assertEquals(states1, m1.getStates());
    assertSame(states1[0], m1.getInitialState());

    PsmMachine m2 = new PsmMachine("Test2", states1, states1[0]);
    assertEquals("Test2", m2.getName());
    assertEquals(states1, m2.getStates());
    assertSame(states1[0], m2.getInitialState());
  }

  public void testStateMap() {
    PsmState[] states = {
      new PsmState("Start", PsmWait.FOREVER,
		   new PsmResponse(PsmEvents.Else, "foo")),
      new PsmState("s1"),
      new PsmState("foo"),
    };
    PsmMachine m = new PsmMachine("Test1", states, "s1");
    assertEquals("Test1", m.getName());
    assertEquals(states, m.getStates());
    assertSame(states[0], m.getState("Start"));
    assertSame(states[1], m.getState("s1"));
    assertSame(states[2], m.getState("foo"));
    assertSame(states[1], m.getInitialState());
  }

  public void testDupState() {
    PsmState[] states = {
      new PsmState("Start", PsmWait.FOREVER,
		   new PsmResponse(PsmEvents.Else, "foo")),
      new PsmState("s1"),
      new PsmState("s2"),
      new PsmState("s1"),
    };
    try {
      new PsmMachine("Test1", states, "Start");
      fail("Dup state name name should throw");
    } catch (PsmException.IllegalStateMachine e) { }
  }
}
