/*
 * $Id: TestPsmMethodMsgAction.java,v 1.1 2005-05-04 19:07:02 smorabito Exp $
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
import org.lockss.protocol.*;


/**
 */
public class TestPsmMethodMsgAction extends LockssTestCase {

  private MyActionHandlers handlers = new MyActionHandlers();

  private static final LcapMessage msg = new V3LcapMessage();

  // To be used as an argument to PsmMethodActions
  private static PsmMsgEvent argEvent =
    new PsmMsgEvent(msg);

  // Expected returns from invocations.
  private static PsmEvent fooEvent = new PsmEvent();
  private static PsmEvent barEvent = new PsmEvent();
  private static PsmEvent bazEvent = new PsmEvent();

  /**
   * Ensure that valid PsmMethodActions can be constructed and run.
   */
  public void testValidMethodInvocation() {
    PsmMethodMsgAction fooAction =
      new PsmMethodMsgAction(MyActionHandlers.class, "handleFoo");
    PsmMethodMsgAction barAction =
      new PsmMethodMsgAction(MyActionHandlers.class, "handleBar");

    PsmEvent fooReturn = fooAction.run(argEvent, null);
    PsmEvent barReturn = barAction.run(argEvent, null);

    assertEquals(fooReturn, fooEvent);
    assertEquals(barReturn, barEvent);
  }

  /**
   * Ensure that a PsmMethodMsgAction cannot be constructed with a
   * non-static method.
   */
  public void testNonStaticMethodInvocationThrows() {
    try {
      PsmMethodMsgAction bazAction =
	new PsmMethodMsgAction(MyActionHandlers.class, "handleBaz");
      fail("Should have thrown PsmMethodMsgActionException");
    } catch (PsmMethodAction.PsmMethodActionException ex) {
      ; // This is expected.
    }
  }

  /**
   * Ensure that a PsmMethodMsgAction cannot be constructed with a
   * non-existant method.
   */
  public void testNoSuchMethodThrows() {
    try {
      PsmMethodMsgAction noSuchAction =
	new PsmMethodMsgAction(MyActionHandlers.class, "noSuchMethod");
      fail("Should have thrown PsmMethodActionException");
    } catch (PsmMethodAction.PsmMethodActionException ex) {
      ; // This is expected.
    }
  }

  /**
   * Ensure that constructing with a non-public Actions handler class
   * throws appropriately.
   */
  public void testNonPublicClassConstructionThrows() {
    PsmMethodMsgAction action = null;
    try {
      action = new PsmMethodMsgAction(PrivateActionHandlers.class,
				      "handleFoo");
      fail("Should have thrown PsmMethodActionException");
    } catch (PsmMethodAction.PsmMethodActionException ex) {
      ; // This is expected.
    }
    try {
      action = new PsmMethodMsgAction(PackageActionHandlers.class,
				      "handleFoo");
      fail("Should have thrown PsmMethodActionException");
    } catch (PsmMethodAction.PsmMethodActionException ex) {
      ; // This is expected.
    }
    try {
      action = new PsmMethodMsgAction(ProtectedActionHandlers.class,
				      "handleFoo");
      fail("Should have thrown PsmMethodActionException");
    } catch (PsmMethodAction.PsmMethodActionException ex) {
      ; // This is expected.
    }
  }

  /**
   * Class that defines a number of handler methods.
   */
  public static class MyActionHandlers {
    public static PsmEvent handleFoo(PsmMsgEvent event, PsmInterp interp) {
      return fooEvent;
    }

    public static PsmEvent handleBar(PsmMsgEvent event, PsmInterp interp) {
      return barEvent;
    }

    /** Non-static methods should cause PsmMethodActionExceptions. */
    public PsmEvent handleBaz(PsmMsgEvent evt, PsmInterp interp) {
      return bazEvent;
    }
  }

  /**
   * Classes used by testNonPublicClassConstructorsThrow()
   */
  private static class PrivateActionHandlers {
    public PsmEvent handleFoo(PsmEvent evt, PsmInterp interp) {
      return null;
    }
  }

  static class PackageActionHandlers {
    public PsmEvent handleFoo(PsmEvent evt, PsmInterp interp) {
      return null;
    }
  }

  protected class ProtectedActionHandlers {
    public PsmEvent handleFoo(PsmEvent evt, PsmInterp interp) {
      return null;
    }
  }
}
