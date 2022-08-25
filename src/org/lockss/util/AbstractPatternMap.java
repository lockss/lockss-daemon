/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.*;
import org.apache.commons.collections4.map.*;
import org.apache.commons.lang3.tuple.*;

/** A "Map" whose element's LHS's are Patterns against which stringa
 * are matched, returning the RHS of the first element that
 * matches.  */
public abstract class AbstractPatternMap<T> {
  static Logger log = Logger.getLogger("AbstractPatternMap");

  protected Map<Pattern,T> patternMap;

  /** Parse the RHS string into the type appropriate for this map */
  protected abstract T parseRhs(String rhs);

  protected AbstractPatternMap() {
  }

  /** Create an AbstractPatternMap from a list of strings of the form
   * <code><i>RE</i>,<i>int</i></code> */
  public AbstractPatternMap(List<String> patternPairs)
      throws IllegalArgumentException {
    parseSpec(patternPairs);
  }

  /** Create a AbstractPatternMap from a string of the form
   * <code><i>RE</i>,<i>int</i>[;<i>RE</i>,<i>int</i> ...]</code> */
  public AbstractPatternMap(String spec)
      throws IllegalArgumentException {
    parseSpec(StringUtil.breakAt(spec, ";", -1, true, true));
  }

  protected static List<String> specList(String spec) {
    return (StringUtil.breakAt(spec, ";", -1, true, true));
  }

  /** Return a copy of the Pattern,T pairs in the map.  Provides
   * visibility into the map without allowing modification */
  public List<Pair<Pattern,T>> getPairs() {
    List<Pair<Pattern,T>> res = new ArrayList<>();
    for (Map.Entry<Pattern,T> ent : patternMap.entrySet()) {
      res.add(Pair.of(ent.getKey(), ent.getValue()));
    }
    return res;
  }

  protected AbstractPatternMap parseSpec(List<String> patternPairs)
      throws IllegalArgumentException {
    List<Pair<String,T>> pairList = new ArrayList<>();
    if (patternPairs != null) {
      for (String pair : patternPairs) {
	// Find the last occurrence of comma as regexp may contain them
	int pos = pair.lastIndexOf(',');
	if (pos < 0) {
	  throw new IllegalArgumentException("Marformed pattern,val pair; no comma: "
					     + pair);
	}
	String regexp = pair.substring(0, pos);
	String rhsStr = pair.substring(pos + 1);
        try {
          T rhs = parseRhs(rhsStr);
          pairList.add(Pair.of(regexp, rhs));
	} catch (Exception e) {
	  throw new IllegalArgumentException("Illegal RHS: " + rhsStr +
                                             ": " + e.toString());
        }
      }
    }
    compilePairs(pairList);
    return this;
  }

  protected AbstractPatternMap compilePairs(List<Pair<String,T>> patternPairs)
      throws IllegalArgumentException {
    patternMap = new LinkedMap<>();
    if (patternPairs != null) {
      for (Pair<String,T> pair : patternPairs) {
	String regexp = pair.getLeft();
	Pattern pat;
	try {
	  pat = Pattern.compile(regexp);
	  patternMap.put(pat, pair.getRight());
	} catch (PatternSyntaxException e) {
	  throw new IllegalArgumentException("Illegal regexp: " + regexp, e);
	}
      }
    }
    return this;
  }

  /** Return the value associated with the first pattern that the string
   * matches, or zero if none */
  public T getMatch(String str) {
    return getMatch(str, null);
  }

  /** Return the value associated with the first pattern that the string
   * matches, or the specified default value if none */
  public T getMatch(String str, T dfault) {
    return getMatch(str, dfault, x -> true);
  }

  /** Return the value associated with the first pattern that the string
   * matches, or the specified default value if none, considering only
   * patterns whose associated value satisfies the filter.
   * @param str the string to match against the LHS patterns.
   * @param dfault the value to return if no pattern (whose value
   * satisfies the predicate) matches str.
   */
  public T getMatch(String str, T dfault, Predicate<T> valueFilter) {
    for (Map.Entry<Pattern,T> ent : patternMap.entrySet()) {
      Matcher m = ent.getKey().matcher(str);
      if (valueFilter.test(ent.getValue()) && m.find()) {
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
      for (Iterator<Map.Entry<Pattern,T>> iter = patternMap.entrySet().iterator(); iter.hasNext(); ) {
	Map.Entry<Pattern,T> ent = iter.next();
	sb.append("[");
	sb.append(ent.getKey().pattern());
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
