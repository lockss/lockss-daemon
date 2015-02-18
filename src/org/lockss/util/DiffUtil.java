/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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
import java.text.*;


/**
 * Utilities for computing text diffs; convenience methods for
 * DiffMatchPatch
 */
public class DiffUtil {

  static Logger log = Logger.getLogger("DiffUtil");

  private static DiffMatchPatch newDmp() {
    DiffMatchPatch dmp = new DiffMatchPatch();
    return dmp;
  }

  public static List<DiffMatchPatch.Diff> diffLines(String str1, String str2) {
    DiffMatchPatch dmp = newDmp();
    DiffMatchPatch.LinesToCharsResult lcr = dmp.diff_linesToChars(str1, str2);
    LinkedList<DiffMatchPatch.Diff> diffs =
      dmp.diff_main(lcr.chars1, lcr.chars2, false);
    dmp.diff_charsToLines(diffs, lcr.lineArray);
    if (log.isDebug3()) log.debug3("diffs: " + diffs);
    return diffs;
  }

  public static String diff_u0(String str1, String str2) {
    StringBuilder sb = new StringBuilder();
    for (DiffMatchPatch.Diff d : diffLines(str1, str2)) {
      switch (d.operation) {
      case INSERT:
	append(sb, "+", d.text);
	break;
      case DELETE:
	append(sb, "-", d.text);
	break;
      case EQUAL:
	break;
      }
    }
    return sb.toString();
  }

  public static String diff_configText(String str1, String str2) {
    String res =
      diff_u0(StringUtil.normalizeEols(str1),
	      StringUtil.normalizeEols(str2));
    return (res.length() < 3) ? "(none)\n" : res;
  }

  private static void append(StringBuilder sb, String pref, String text) {
    if (text.endsWith("\n")) {
      text = text.substring(0, text.length() - 1);
    }
    for (String s : StringUtil.breakAt(text, "\n")) {
      sb.append(pref);
      sb.append(" ");
      sb.append(s);
      sb.append("\n");
    }
  }
}
