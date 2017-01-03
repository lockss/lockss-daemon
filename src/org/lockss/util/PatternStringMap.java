/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import java.text.*;
import org.apache.commons.collections.map.*;
import org.apache.oro.text.regex.*;

/** "Map" strings to string, where the keys are patterns against which
 * the strings are matched.  The patterns are ordered; the value associated
 * with the first one that matches is returned.  */
public class PatternStringMap {
  static Logger log = Logger.getLogger("PatternStringMap");

  /** An empty PatternStringMap, which always returns the default
   * value. */
  public final static PatternStringMap EMPTY =
    new PatternStringMap(Collections.EMPTY_LIST);

  private Map<Pattern,String> patternMap;

  /** Create a PatternStringMap from a list of strings of the form
   * <code><i>RE</i>,<i>string</i></code> */
  public PatternStringMap(List<String> patternPairs)
      throws IllegalArgumentException {
    makePatternMap(patternPairs);
  }

  /** Create a PatternStringMap from a string of the form
   * <code><i>RE</i>,<i>string</i>[;<i>RE</i>,<i>string</i> ...]</code> */
  public PatternStringMap(String spec)
      throws IllegalArgumentException {
    makePatternMap(StringUtil.breakAt(spec, ";", -1, true, true));
  }

  private void makePatternMap(List<String> patternPairs)
      throws IllegalArgumentException {
    if (patternPairs != null) {
      patternMap = new LinkedMap();
      for (String pair : patternPairs) {
	// Find the last occurrence of comma to avoid regexp quoting
	int pos = pair.lastIndexOf(',');
	if (pos < 0) {
	  throw new IllegalArgumentException("Marformed pattern,string pair; no comma: "
					     + pair);
	}
	String regexp = pair.substring(0, pos);
	String val = pair.substring(pos + 1);
	try {
	  int flags = Perl5Compiler.READ_ONLY_MASK;
	  Pattern pat = RegexpUtil.getCompiler().compile(regexp, flags);
	  patternMap.put(pat, val);
	} catch (MalformedPatternException e) {
	  throw new IllegalArgumentException("Illegal regexp: " + regexp);
	}
      }
    }
  }

  /** Return the value associated with the first pattern that the string
   * matches, or null if none */
  public String getMatch(String str) {
    return getMatch(str, null);
  }

  /** Return the value associated with the first pattern that the string
   * matches, or the specified default value if none. */
  public String getMatch(String str, String dfault) {
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    for (Map.Entry<Pattern,String> ent : patternMap.entrySet()) {
      Pattern pat = ent.getKey();
      if (matcher.contains(str, pat)) {
	log.debug2("getMatch(" + str + "): " + ent.getValue());
	return ent.getValue();
      }
    }
    log.debug2("getMatch(" + str + "): default: " + dfault);
    return dfault;
  }

  public boolean isEmpty() {
    return patternMap.isEmpty();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[pm: ");
    if (patternMap.isEmpty()) {
      sb.append("EMPTY");
    } else {
      for (Iterator<Map.Entry<Pattern,String>> iter = patternMap.entrySet().iterator(); iter.hasNext(); ) {
	Map.Entry<Pattern,String> ent = iter.next();
	sb.append("[");
	sb.append(ent.getKey().getPattern());
	sb.append(": ");
	sb.append(ent.getValue());
	sb.append("]");
	if (iter.hasNext()) {
	  sb.append(", ");
	}
      }
    }
    sb.append("]");
    return sb.toString();
  }
}
