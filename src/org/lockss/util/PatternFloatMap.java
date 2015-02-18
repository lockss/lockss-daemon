/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

/** "Map" strings to floats, where the keys are patterns against which the
 * strings are matched.  The patterns are ordered; the value associated
 * with the first one that matches is returned.  */

// Should genericize (along with PatternIntMap) but need to find a way to
// do type-depending string parsing.

public class PatternFloatMap {
  static Logger log = Logger.getLogger("PatternFloatMap");

  /** An empty PatternFloatMap, which always returns the default value. */
  public final static PatternFloatMap EMPTY =
    new PatternFloatMap(Collections.EMPTY_LIST);

  private Map<Pattern,Float> patternMap;

  /** Create a PatternFloatMap from a list of strings of the form
   * <code><i>RE</i>,<i>int</i></code> */
  public PatternFloatMap(List<String> patternPairs)
      throws IllegalArgumentException {
    makePatternMap(patternPairs);
  }

  /** Create a PatternFloatMap from a string of the form
   * <code><i>RE</i>,<i>int</i>[;<i>RE</i>,<i>int</i> ...]</code> */
  public PatternFloatMap(String spec)
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
	  throw new IllegalArgumentException("Marformed pattern,int pair; no comma: "
					     + pair);
	}
	String regexp = pair.substring(0, pos);
	String pristr = pair.substring(pos + 1);
	float pri;
	Pattern pat;
	try {
	  pri = Float.parseFloat(pristr);
	  int flags = Perl5Compiler.READ_ONLY_MASK;
	  pat = RegexpUtil.getCompiler().compile(regexp, flags);
	  patternMap.put(pat, pri);
	} catch (NumberFormatException e) {
	  throw new IllegalArgumentException("Illegal priority: " + pristr);
	} catch (MalformedPatternException e) {
	  throw new IllegalArgumentException("Illegal regexp: " + regexp);
	}
      }
    }
  }

  /** Return the value associated with the first pattern that the string
   * matches, or zero if none */
  public float getMatch(String str) {
    return getMatch(str, 0);
  }

  /** Return the value associated with the first pattern that the string
   * matches, or the specified default value if none */
  public float getMatch(String str, float dfault) {
    return getMatch(str, dfault, Float.MAX_VALUE);
  }

  /** Return the value associated with the first pattern that the string
   * matches, or the specified default value if none, considering only
   * patterns whose associated value is less than or equal to maxPri. */
  public float getMatch(String str, float dfault, float maxPri) {
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    for (Map.Entry<Pattern,Float> ent : patternMap.entrySet()) {
      if (ent.getValue() <= maxPri) {
	Pattern pat = ent.getKey();
	if (matcher.contains(str, pat)) {
	  log.debug2("getMatch(" + str + "): " + ent.getValue());
	  return ent.getValue();
	}
      }
    }
    log.debug2("getMatch(" + str + "): default: " + dfault);
    return dfault;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[pm: ");
    if (patternMap.isEmpty()) {
      sb.append("EMPTY");
    } else {
      for (Iterator<Map.Entry<Pattern,Float>> iter = patternMap.entrySet().iterator(); iter.hasNext(); ) {
	Map.Entry<Pattern,Float> ent = iter.next();
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