/*
 * $Id: WrapperState.java,v 1.1 2003-07-25 00:29:05 tyronen Exp $
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

package org.lockss.util;

/**
 * <p>Title: WrapperState </p>
 * <p>Description: Used to maintain mappings of original to wrapped objects
 * in classes created by the WrapperGenerator. </p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

import java.util.*;
import java.lang.reflect.*;

public class WrapperState {

  public static final String PREFIX = "Wrapped";

  /** Master container; key=Class objects, value=map of instances,
   *  where key=instance, value=wrapped instance */
  static Map classMap = new HashMap();

  /** Given a regular object, returns a wrapped version of it */
  public static Object getWrapper(Object obj) {
    try {
      Class cl = obj.getClass();
      Map instanceMap;
      if (classMap.containsKey(cl)) {
        instanceMap = (Map) classMap.get(cl);
      }
      else {
        instanceMap = new WeakHashMap();
        classMap.put(cl, instanceMap);
      }
      if (instanceMap.containsKey(obj)) {
        return instanceMap.get(obj);
      }
      else {
        Class wrapped = Class.forName(makeWrappedName(cl));
        if (wrapped.equals(obj.getClass())) {
          return obj;
        } else {
          Class[] classarray = new Class[1];
          classarray[0] = Class.forName(cl.getName());
          Constructor con = wrapped.getConstructor(classarray);
          Object[] objarray = new Object[1];
          objarray[0] = obj;
          Object wrappedObj = con.newInstance(objarray);
          instanceMap.put(obj, wrappedObj);
          return wrappedObj;
        }
      }
    } catch (Exception e) {
      // return the original object if a failure occurs
      return obj;
    }
  }

  // Given a wrapped object, get the original
  public static Object getOriginal(Object obj) {
    String classname = ClassUtil.getClassNameWithoutPackage(obj.getClass());
    if (!classname.startsWith(PREFIX)) {
      return obj;
    } else {
      String origclass = classname.substring(PREFIX.length()+1);
      if (!classMap.containsKey(origclass)) {
        return obj;
      } else {
        Map instanceMap = (Map)classMap.get(origclass);
        Iterator it = instanceMap.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry entry = (Map.Entry)it.next();
          if (obj == entry.getValue()) {
            return entry.getKey();
          }
        }
        throw new NoSuchElementException();
      }
    }
  }

  private static String makeWrappedName(Class cl) {
    String classname = ClassUtil.getClassNameWithoutPackage(cl);
    return (classname.startsWith(PREFIX)) ? classname : PREFIX + classname;
  }


}