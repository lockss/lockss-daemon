/*
 * $Id: CatalogueOrderComparator.java,v 1.1 2004-04-27 19:05:22 tlipkis Exp $
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
import java.io.*;
import gnu.regexp.*;
import org.lockss.plugin.*;
import org.lockss.remote.*;

/**
 * Comparator that implements a suitable ordering for titles in a library
 * catalogue.
 */

public class CatalogueOrderComparator implements Comparator {
  Map keyMap = new HashMap();

  public int compare(Object o1, Object o2) {
    if (!((o1 instanceof String)
	 && (o2 instanceof String))) {
      throw new IllegalArgumentException("CatalogueOrderComparator(" +
					 o1.getClass() + "," +
					 o2.getClass() + ")");
    }
    return compare((String)o1, (String)o2);
  }

  public int compare(String s1, String s2) {
    return getSortKey(s1).compareToIgnoreCase(getSortKey(s2));
  }

  String getSortKey(String s) {
    String key = (String)keyMap.get(s);
    if (key == null) {
      key = xlate(s);
      keyMap.put(s, key);
    }
    return key;
  }    

  String xlate(String s) {
    s = s.trim();
    s = deleteInitial(s, "a");
    s = deleteInitial(s, "an");
    s = deleteInitial(s, "the");
    s = deleteAll(s, ".,-:;\"\'");
    return s;
  }

  String deleteInitial(String s, String sub) {
    int sublen = sub.length();
    if (StringUtil.startsWithIgnoreCase(s, sub) &&
	s.length() > sublen &&
	Character.isWhitespace(s.charAt(sublen))) {
      s = s.substring(sublen + 1, s.length());
      s = s.trim();
    }
    return s;
  }

  String deleteAll(String s, String chars) {
    for (int ix = 0; ix < chars.length(); ix++) {
      String c = chars.substring(ix, ix + 1);
      s = StringUtil.replaceString(s, c, "");
    }
    return s;
  }
}
