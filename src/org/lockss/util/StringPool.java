/*
 * $Id: StringPool.java,v 1.2 2013-05-08 09:10:43 tlipkis Exp $
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
 * Named intern() pools for Strings.  Similer to String.intern(), but use
 * of context-dependent pools should allow for smaller maps with less
 * waste.
 */
public class StringPool {

  public static StringPool AU_CONFIG_PROPS = new StringPool("AU config props");
  public static StringPool HTTP_HEADERS = new StringPool("HTTP headers");
  public static StringPool PLUGIN_IDS = new StringPool("Plugin IDs");

  private String name;
  private Map<String,String> map;

  public StringPool(String name) {
    this(name, 20);
  }

  /** Create a StringPool with a name and initial size */
  public StringPool(String name, int initialSize) {
    this.name = name;
    map = new HashMap<String,String>(initialSize);
  }

  /** Return the instance of the string already in the pool, if any, else
   * add this instance and return it. */
  public synchronized String intern(String str) {
    if (true) {
      String res = map.get(str);
      if (res == null) {
	map.put(str, str);
	res = str;
      }
      return res;
    } else {
      return str;
    }
  }

  private int sumStringChars() {
    int res = 0;
    for (String val : map.values()) {
      res += val.length();
    }
    return res;
  }

  public String toString() {
    return "[StringPool " + name + ", " + map.size() + " entries]";
  }

  public String toStats() {
    return "[StringPool " + name + ", " + map.size() + " entries, " +
      sumStringChars() + " total chars]";
  }

}
