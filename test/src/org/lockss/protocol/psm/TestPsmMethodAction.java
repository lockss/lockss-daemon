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
import java.io.IOException;

/**
 */
public class TestPsmMethodAction extends LockssTestCase {

  private MyActionHandlers handlers = new MyActionHandlers();

  // To be used as an argument to PsmMethodActions
  private static PsmEvent argEvent = new PsmEvent();

  // Expected returns from invocations.
  private static PsmEvent fooEvent = new PsmEvent();
  private static PsmEvent barEvent = new PsmEvent();
  private static PsmEvent bazEvent = new PsmEvent();

  /**
   * Ensure that valid PsmMethodActions can be constructed and run.
   */
  public void testValidMethodInvocation() {
    PsmMethodAction fooAction =
      new PsmMethodAction(MyActionHandlers.class, "handleFoo");
    PsmMethodAction barAction =
      new PsmMethodAction(MyActionHandlers.class, "handleBar");

    PsmEvent fooReturn = fooAction.run(argEvent, null);
    PsmEvent barReturn = barAction.run(argEvent, null);

    assertSame(fooReturn, fooEvent);
    assertSame(barReturn, barEvent);
  }

  /**
   * Ensure that a PsmMethodAction cannot be constructed with a
   * non-static method.
   */
  public void testNonStaticMethodInvocationThrows() {
    try {
      PsmMethodAction bazAction =
	new PsmMethodAction(MyActionHandlers.class, "handleBaz");
      fail("Should have thrown IllegalPsmMethodActionException");
    } catch (PsmMethodAction.IllegalPsmMethodActionException ex) {
      // This is expected.
    }
  }

  /**
   * Ensure that a PsmMethodAction cannot be constructed with a
   * non-existant method.
   */
  public void testNoSuchMethodThrows() {
    try {
      PsmMethodAction noSuchAction =
	new PsmMethodAction(MyActionHandlers.class, "noSuchMethod");
      fail("Should have thrown IllegalPsmMethodActionException");
    } catch (PsmMethodAction.IllegalPsmMethodActionException ex) {
      // This is expected.
    }
  }

  /**
   * Ensure that constructing with a method that has the wrong
   * return type throws.
   */
  public void testWrongReturnTypeThrows() {
    try {
      PsmMethodAction wrongReturnType =
	new PsmMethodAction(MyActionHandlers.class, "wrongReturnType");
      fail("Should have thrown IllegalPsmMethodActionException");
    } catch (PsmMethodAction.IllegalPsmMethodActionException ex) {
      //expected
    }
  }

  /**
   * Ensure that constructing with a method that has the
   * wrong argument type throws.
   */
  public void testWrongArgTypeThrows() {
    try {
      PsmMethodAction wrongArgumentType =
	new PsmMethodAction(MyActionHandlers.class, "wrongArgumentType");
      fail("Should have thrown IllegalPsmMethodActionException");
    } catch (PsmMethodAction.IllegalPsmMethodActionException ex) {
      //expected
    }
  }

  /**
   * Ensure that constructing with methods that declare
   * RuntimeExceptions succeeds.
   */
  public void testRuntimeExceptionDeclarationSucceeds() {
    PsmMethodAction action1 =
      new PsmMethodAction(MyActionHandlers.class,
			  "throwRuntimeException");

    PsmMethodAction action2 =
      new PsmMethodAction(MyActionHandlers.class,
			  "throwPsmMethodActionException");
  }

  /**
   * Ensure that constructing with methods that declare
   * non-RuntimeExceptions fails.
   */
  public void testNonRuntimeExceptionDeclarationThrows() {
    try {
      PsmMethodAction action1 =
	new PsmMethodAction(MyActionHandlers.class,
			    "throwIoException");
    } catch (PsmMethodAction.IllegalPsmMethodActionException ex) {
      //expected
    }
  }

  /**
   * Ensure that constructing with non-public Actions handler classes
   * or methods throw appropriately.
   */
  public void testNonPublicClassConstructionThrows() {
    PsmMethodAction action = null;
    // Methods
    try {
      action = new PsmMethodAction(MyActionHandlers.class,
				   "privateMethod");
      fail("Should have thrown IllegalPsmMethodActionException");
    } catch (PsmMethodAction.IllegalPsmMethodActionException ex) {
      // This is expected
    }
    try {
      action = new PsmMethodAction(MyActionHandlers.class,
				   "packageMethod");
      fail("Should have thrown IllegalPsmMethodActionException");
    } catch (PsmMethodAction.IllegalPsmMethodActionException ex) {
      // This is expected
    }
    try {
      action = new PsmMethodAction(MyActionHandlers.class,
				   "protectedMethod");
      fail("Should have thrown IllegalPsmMethodActionException");
    } catch (PsmMethodAction.IllegalPsmMethodActionException ex) {
      // This is expected
    }
    // Classes
    try {
      action = new PsmMethodAction(PrivateActionHandlers.class,
				   "handleFoo");
      fail("Should have thrown IllegalPsmMethodActionException");
    } catch (PsmMethodAction.IllegalPsmMethodActionException ex) {
      // This is expected.
    }
    try {
      action = new PsmMethodAction(PackageActionHandlers.class,
				   "handleFoo");
      fail("Should have thrown IllegalPsmMethodActionException");
    } catch (PsmMethodAction.IllegalPsmMethodActionException ex) {
      // This is expected.
    }
    try {
      action = new PsmMethodAction(ProtectedActionHandlers.class,
				   "handleFoo");
      fail("Should have thrown IllegalPsmMethodActionException");
    } catch (PsmMethodAction.IllegalPsmMethodActionException ex) {
      // This is expected.
    }
  }


  /**
   * Class that defines a number of handler methods.
   */
  public static class MyActionHandlers {
    public static PsmEvent handleFoo(PsmEvent event, PsmInterp interp) {
      return fooEvent;
    }

    public static PsmEvent handleBar(PsmEvent event, PsmInterp interp) {
      return barEvent;
    }

    /** Non-static methods should cause IllegalPsmMethodActionExceptions. */
    public PsmEvent handleBaz(PsmEvent evt, PsmInterp interp) {
      return bazEvent;
    }

    /** Incorrect argument types. */
    public static Object wrongArgumentType(PsmMsgEvent evt, PsmInterp interp) {
      return null;
    }

    /** Invalid return type. */
    public static Object wrongReturnType(PsmEvent evt, PsmInterp interp) {
      return null;
    }

    /** Non-public methods. */
    private static PsmEvent privateMethod(PsmEvent evt, PsmInterp interp) {
      return null;
    }

    static PsmEvent packageMethod(PsmEvent evt, PsmInterp interp) {
      return null;
    }

    protected static PsmEvent protectedMethod(PsmEvent evt, PsmInterp interp) {
      return null;
    }

    /** Methods that declare exceptions. */
    public static PsmEvent throwRuntimeException(PsmEvent evt, PsmInterp interp)
	throws RuntimeException {
      throw new RuntimeException();
    }

    // Subclasses of RuntimeException should be OK.
    public static PsmEvent throwPsmMethodActionException(PsmEvent evt, PsmInterp interp)
	throws PsmMethodAction.PsmMethodActionException {
      throw new PsmMethodAction.PsmMethodActionException();
    }

    // IOException should not be OK.
    public static PsmEvent throwIoException(PsmEvent evt, PsmInterp interp)
	throws IOException {
      throw new IOException();
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
