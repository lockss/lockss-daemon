/*
 * $Id: MapUtil.java,v 1.2 2005-08-02 22:54:36 troberts Exp $
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
 * Create a list of Object from a call list. */
public class MapUtil {
  /**
   * Don't construct. */
  private MapUtil() {
  }

  /**
   * Create map from arg map. */
  public static Map map() {
    return new HashMap();
  }

  public static Map map(Object key1, Object val1) {
    Map m = map();
    m.put(key1, val1);
    return m;
  }

  public static Map map(Object key1, Object val1,
			Object key2, Object val2) {
    Map m = map(key1, val1);
    m.put(key2, val2);
    return m;
  }

  public static Map map(Object key1, Object val1,
			Object key2, Object val2,
			Object key3, Object val3) {
    Map m = map(key1, val1, key2, val2);
    m.put(key3, val3);
    return m;
  }

  public static Map map(Object key1, Object val1,
			Object key2, Object val2,
			Object key3, Object val3,
			Object key4, Object val4) {
    Map m = map(key1, val1, key2, val2, key3, val3);
    m.put(key4, val4);
    return m;
  }

}
