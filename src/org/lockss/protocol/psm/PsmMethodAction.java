/*
* $Id: PsmMethodAction.java,v 1.2 2005-05-04 22:45:20 smorabito Exp $
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
      throw new PsmMethodActionException("Action class must be public.");
    }
					
    try {
      method = c.getMethod(m, argArray);
    } catch (NoSuchMethodException ex) {
      throw new PsmMethodActionException(ex.toString() + ": method " + m);
    }

    if (PsmEvent.class != method.getReturnType()) {
      throw new PsmMethodActionException("Method return type must be PsmEvent");
    }

    if (!Modifier.isStatic(method.getModifiers()) ||
	!Modifier.isPublic(method.getModifiers())) {
      throw new PsmMethodActionException("Method " + m +
					 " must be static and public.");
    }
  }

  public PsmEvent run(PsmEvent event, PsmInterp interp) {
    return this.run(event, interp, new Object[]{event, interp});
  }

  protected PsmEvent run(PsmEvent event, PsmInterp interp, Object[] argArray) {
    try {
      return (PsmEvent)method.invoke(null, argArray);
    } catch (IllegalAccessException ex) {
      throw new PsmMethodActionException(ex.toString());
    } catch (InvocationTargetException ex) {
      throw new PsmMethodActionException(ex.toString());
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
}
