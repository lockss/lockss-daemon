/*
 * $Id: PsmMethodAction.java,v 1.5 2008-01-27 06:47:10 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.lang.reflect.*;

/**
 * Action that uses reflection to invoke a method on the specified class.
 */
public class PsmMethodAction extends PsmAction {
  protected Method method;

  protected PsmMethodAction() {
  }

  public PsmMethodAction(Class c, String m) {
    this(c, m, new Class[] {PsmEvent.class, PsmInterp.class});
  }

  protected PsmMethodAction(Class c, String m, Class[] argArray) {
    // This prevents IllegalAccessExceptions when attempting to
    // invoke methods on a class that is in another package and not
    // defined 'public'.
    if (!Modifier.isPublic(c.getModifiers())) {
      throw new IllegalPsmMethodActionException("Action class must be public.");
    }

    try {
      method = c.getMethod(m, argArray);
    } catch (NoSuchMethodException ex) {
      throw new IllegalPsmMethodActionException(ex.toString() + ": method " + m);
    }

    // Check each exception this method declares thrown.  If it declares
    // exceptions, and any of them are not runtime exceptions, abort.
    Class[] exceptionTypes = method.getExceptionTypes();
    for (int i = 0; i < exceptionTypes.length; i++) {
      Class exceptionClass = exceptionTypes[i];
      if (!RuntimeException.class.isAssignableFrom(exceptionClass)) {
	throw new IllegalPsmMethodActionException("Method must not declare non-Runtime "+
						  "exceptions.");
      }
    }

    // Ensure that the method returns PsmEvent
    if (PsmEvent.class != method.getReturnType()) {
      throw new IllegalPsmMethodActionException("Method return type must be PsmEvent");
    }

    // Ensure that both the method is both public and static.
    if (!Modifier.isStatic(method.getModifiers()) ||
	!Modifier.isPublic(method.getModifiers())) {
      throw new IllegalPsmMethodActionException("Method " + m +
						" must be static and public.");
    }
  }

  public Method getMethod() {
    return method;
  }

  public PsmEvent run(PsmEvent event, PsmInterp interp) {
    return this.run(event, interp, new Object[]{event, interp});
  }

  protected PsmEvent run(PsmEvent event, PsmInterp interp, Object[] argArray) {
    try {
      return (PsmEvent)method.invoke(null, argArray);
    } catch (IllegalAccessException ex) {
      // This really should never happen, given the checks at
      // construction time.
      throw new PsmMethodActionException(ex.toString());
    } catch (InvocationTargetException ex) {
      // This may occur if the method throws a runtime exception.
      // Rather than wrap it in a PsmMethodActionException, let
      // it percolate up.  This cast should never fail, but if it
      // does, throw PsmMethodActionException.
      try {
	throw (RuntimeException)(ex.getTargetException());
      } catch (ClassCastException cce) {
	throw new PsmMethodActionException("Exception thrown from " +
					   "target method invocation " +
					   " is not a Runtime Exception: " +
					   cce.toString());
      }
    }
  }

  public static class PsmMethodActionException extends RuntimeException {
    public PsmMethodActionException() {
      super();
    }

    public PsmMethodActionException(String s) {
      super(s);
    }
  }

  /**
   * Runtime exception that will be thrown at construction time if the
   * PsmMethodAction is illegal.
   */
  public static class IllegalPsmMethodActionException
    extends PsmMethodActionException {
    public IllegalPsmMethodActionException() {
      super();
    }

    public IllegalPsmMethodActionException(String s) {
      super(s);
    }
  }
}
