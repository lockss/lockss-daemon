/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.plugin.*;

/** "Map" AUs to floats.  The keys are XPath predicates ({@link
 * AuXpathMatcher} against which the AUs are matched.  The XPaths are
 * ordered; the value associated with the first one that matches is
 * returned.  */
public class AuXpathFloatMap {
  static Logger log = Logger.getLogger("AuXpathFloatMap");

  /** An empty AuXpathFloatMap, which always returns the default value. */
  public final static AuXpathFloatMap EMPTY =
    new AuXpathFloatMap(Collections.EMPTY_LIST);

  private Map<AuXpathMatcher,Float> patternMap;

  /** Create an AuXpathFloatMap from a list of strings of the form
   * <code>[<i>xpath</i>],<i>float</i></code> */
  public AuXpathFloatMap(List<String> patternPairs)
      throws IllegalArgumentException {
    makeAuXpathMatcherMap(patternPairs);
  }

  /** Create an AuXpathFloatMap from a string of the form
   * <code>[<i>xpath</i>],<i>float</i>;[<i>xpath</i>],<i>float</i>;</code>... */
  public AuXpathFloatMap(String spec)
      throws IllegalArgumentException {
    makeAuXpathMatcherMap(StringUtil.breakAt(spec, ";", -1, true, true));
  }

  private void makeAuXpathMatcherMap(List<String> patternPairs)
      throws IllegalArgumentException {
    if (patternPairs != null) {
      patternMap = new LinkedMap();
      for (String pair : patternPairs) {
    	// Find the last occurrence of comma to avoid xpath quoting
    	int pos = pair.lastIndexOf(',');
    	if (pos < 0) {
    	  throw new IllegalArgumentException("Malformed xpath,float pair; no comma: "
    					     + pair);
    	}
    	String xpath = pair.substring(0, pos);
    	String pristr = pair.substring(pos + 1);
    	float pri;
	AuXpathMatcher matcher;
    	try {
    	  pri = Float.parseFloat(pristr);
	  matcher = AuXpathMatcher.create(xpath);
	  patternMap.put(matcher, pri);
    	} catch (NumberFormatException e) {
    	  throw new IllegalArgumentException("Illegal priority: " + pristr);
    	}
      }
    }
  }

  /** Return the value associated with the first pattern that the string
   * matches, or zero if none */
  public float getMatch(ArchivalUnit au) {
    return getMatch(au, 0);
  }

  /** Return the value associated with the first pattern that the string
   * matches, or the specified default value if none */
  public float getMatch(ArchivalUnit au, float dfault) {
    return getMatch(au, dfault, Float.MAX_VALUE);
  }

  /** Return the value associated with the first pattern that the string
   * matches, or the specified default value if none, considering only
   * patterns whose associated value is less than or equal to maxPri. */
  public float getMatch(ArchivalUnit au, float dfault, float maxPri) {
    for (Map.Entry<AuXpathMatcher,Float> ent : patternMap.entrySet()) {
      if (ent.getValue() <= maxPri) {
    	AuXpathMatcher matcher = ent.getKey();
    	if (matcher.isMatch(au)) {
    	  log.debug2("getMatch(" + au + "): " + ent.getValue());
    	  return ent.getValue();
    	}
      }
    }
    log.debug2("getMatch(" + au + "): default: " + dfault);
    return dfault;
  }

  public boolean isEmpty() {
    return patternMap.isEmpty();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[auxpathmap: ");
    if (patternMap.isEmpty()) {
      sb.append("EMPTY");
    } else {
      for (Iterator<Map.Entry<AuXpathMatcher,Float>> iter = patternMap.entrySet().iterator(); iter.hasNext(); ) {
	Map.Entry<AuXpathMatcher,Float> ent = iter.next();
	sb.append("[");
	sb.append(ent.getKey().getXpath());
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


