/*
 * $Id: WrappedMap.java,v 1.1 2003-09-04 23:11:17 tyronen Exp $
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

package org.lockss.plugin;

import java.util.*;

/**
 * <p>Title: WrappedMap</p>
 * <p>Description: Like a HashMap, but unwraps keys if necessary.  It cannot
 * be instantiated directly; use the <code>
 * WrapperState.createWrappedMapIfAvailable<code> method.  </p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

class WrappedMap extends HashMap implements Map {

  /* Override default constructors with package-protected versions
     to force use of factory method in WrapperState */
  WrappedMap() {
    super();
  }

  WrappedMap(int initialCapacity) {
    super(initialCapacity);
  }

  WrappedMap(int initialCapacity, float loadFactor) {
    super(initialCapacity,loadFactor);
  }

  WrappedMap(Map t) {
    super(t.size());
    Iterator it = t.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry)it.next();
      put(entry.getKey(),entry.getValue());
    }
  }

  Object unwrap(Object obj) {
    if (obj instanceof Wrapped) {
      return ((Wrapped)obj).getOriginal();
    } else {
      return obj;
    }
  }

  public boolean containsKey(Object key) {
    return super.containsKey(unwrap(key));
  }

  public Object get(Object key) {
    return super.get(unwrap(key));
  }

  public Object put(Object key, Object value) {
    return super.put(unwrap(key),value);
  }

}