/*
 * $Id$
 *

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

package org.lockss.util;

import java.util.*;

/**
 * Utilities for dealing with classes.
 */

public class ClassUtil {

  /** Returns true of the classname passed is a Java primitive type; false if
   * it is an object
   */
  public static boolean isPrimitive(String name) {
    if (name.equals("double") || name.equals("float") ||
        name.equals("int") || name.equals("long") ||
        name.equals("short") || name.equals("byte") ||
        name.equals("char") || name.equals("boolean")) {
      return true;
    }
    else {
      return false;
    }
  }

  /** Returns the name of a class without the package prefix */
  public static String getClassNameWithoutPackage(Class cl) {
    String fullname = cl.getName();
    int pos = fullname.lastIndexOf('.');
    return (pos==-1) ? fullname : fullname.substring(pos + 1);
  }

  public static String objectTypeName(String name) {
    if (!isPrimitive(name)) {
      return name;
    }
    else if (name.equals("int")) {
      return "Integer";
    }
    else if (name.equals("char")) {
      return "Character";
    }
    else {
      return StringUtil.titleCase(name);
    }
  }

  /**
   * instantiate a class handle any exceptions
   *
   * @param className the name of the class to instantiate
   * @param type the type or Class to instantiate
   *
   * @return an instance of the class cast as a object of type T
   *
   * @throws java.lang.IllegalStateException if we unable to make the class
   */
  public static <T> T instantiate(final String className, final Class<T> type)
      throws IllegalStateException {
    try {
      return type.cast(Class.forName(className).newInstance());
    } catch (final InstantiationException e) {
      throw new IllegalStateException(e);
    } catch (final IllegalAccessException e) {
      throw new IllegalStateException(e);
    } catch (final ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}
