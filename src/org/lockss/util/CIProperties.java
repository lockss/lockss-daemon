/*
 * $Id$
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
import java.util.*;

import org.mortbay.util.StringMap;

import java.io.Externalizable;
// import java.util.AbstractMap;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.HashSet;
// import java.util.Map;
// import java.util.Set;

/** Properties with case-independent string keys. Canonicalizes all keys
 * by lowercasing them.
*/
public class CIProperties extends Properties {
  /** Create a case-independent Properties.
   */
  public CIProperties() {
  }

  /** Factory to create from a Properties */
  public static CIProperties fromProperties(Properties props) {
    CIProperties res = new CIProperties();
    for (Iterator iter = props.keySet().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      res.setProperty(key, props.getProperty(key));
    }
    return res;
  }

  private String keyObj(Object obj) {
    return obj.toString().toLowerCase();
  }

  private String keyObj(String obj) {
    return obj.toLowerCase();
  }

  public Object put(Object key, Object value) {
    return super.put(keyObj(key), value);
  }

  public Object put(String key, Object value) {
    return super.put(keyObj(key), value);
  }

  public Object get(Object key) {
    return super.get(keyObj(key));
  }

  public Object get(String key) {
    return super.get(keyObj(key));
  }

  public String getProperty(String key) {
    return super.getProperty(keyObj(key));
  }

  public Object remove(Object key) {
    return super.remove(keyObj(key));
  }

  public Object remove(String key) {
    return super.remove(keyObj(key));
  }

  public boolean containsKey(String key) {
    return super.containsKey(keyObj(key));
  }

}
