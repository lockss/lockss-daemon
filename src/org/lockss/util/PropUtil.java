/*
 * $Id: PropUtil.java,v 1.3 2003-04-15 01:20:27 troberts Exp $
 */

package org.lockss.util;

import java.util.*;
import java.net.URLEncoder;
import org.mortbay.tools.*;

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

/** Utilities for Properties
 */
public class PropUtil {

  private static final String noProp = new String(); // guaranteed unique obj

  private static boolean isKeySame(String key, Properties p1, Properties p2) {
    Object o1 = p1.getProperty(key);
    Object o2 = p2.getProperty(key, noProp);
    return (o1 == o2) ||
      // o2 == noProp means p2 didn't have key that p1 has.
      // we know o1 != o2, so if o1 == null, o2 != null.
      !((o2 == noProp || o1 == null || !o1.equals(o2)));
  }

  /**
   * Compare two Properties for equality (same set of properties with
   * same (.equals) values.
   * @param p1 first Properties
   * @param p2 second Properties
   * @return true iff Properties are equal
   */
  public static boolean equalProps(Properties p1, Properties p2) {
    if (p1 == null || p2 == null || p1.size() != p2.size()) {
      return false;
    }
    Enumeration en = p1.keys();
    while (en.hasMoreElements()) {
      String k1 = (String)en.nextElement();
      if (! isKeySame(k1, p1, p2)) {
	return false;
      }
    }
    return true;
  }

  /**
   * Compare two Properties, return the set of keys whose values are not
   * equal.  (The set may contain keys that don't exist in one or the other
   * Properties).
   * @param p1 first Properties
   * @param p2 second Properties
   * @return Set of keys whose values differ, or null if
   * there are no differences.
   */
  public static Set differentKeys(Properties p1, Properties p2) {
    if (p1 == null) {
      if (p2 == null) {
	return null;
      } else {
	return p2.keySet();
      }
    } else if (p2 == null) {
      return p1.keySet();
    }
    Set res = new HashSet();
    Set keys1 = p1.keySet();
    for (Iterator iter = keys1.iterator(); iter.hasNext();) {
      String k1 = (String)iter.next();
      if (! isKeySame(k1, p1, p2)) {
	res.add(k1);
      }
    }
    // add all the keys in p2 that don't appear in p1
    Set p2Only = new HashSet(p2.keySet());
    p2Only.removeAll(keys1);
    res.addAll(p2Only);
    return res;
  }

  public static String propsToEncodedString(Properties props) {
    if (props == null || props.isEmpty()) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    for (Iterator it=props.keySet().iterator(); it.hasNext();) {
      String key = (String)it.next();
      sb.append(PropKeyEncoder.encode(key));
      sb.append("~");
      sb.append(URLEncoder.encode(props.getProperty(key)));
      if (it.hasNext()) {
	sb.append("&");
      }
    }
    return sb.toString();
  }
}
