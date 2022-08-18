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

/** "Map" strings to integers, where the keys are patterns against which
 * the strings are matched.  The patterns are ordered; the value associated
 * with the first one that matches is returned.  */
public class PatternIntMap extends AbstractPatternMap<Integer> {
  static Logger log = Logger.getLogger("PatternIntMap");

  /** An empty PatternIntMap, which always returns the default value. */
  public final static PatternIntMap EMPTY =
    new PatternIntMap(Collections.emptyList());

  private PatternIntMap() {
    super();
  }

  /** Create a PatternIntMap from a list of strings of the form
   * <code><i>RE</i>,<i>int</i></code>
   * @deprecated use {@link #fromSpec()}
   */
  @Deprecated
  public PatternIntMap(List<String> patternPairs)
      throws IllegalArgumentException {
    super(patternPairs);
  }

  /** Create a PatternIntMap from a string of the form
   * <code><i>RE</i>,<i>int</i>[;<i>RE</i>,<i>int</i> ...]</code>
   * @deprecated use {@link #fromSpec()}
   */
  @Deprecated
  public PatternIntMap(String spec)
      throws IllegalArgumentException {
    super(spec);
  }

  /** Create a PatternIntMap from a list of strings of the form
   * <code><i>RE</i>,<i>int</i></code>
   */
  public static PatternIntMap fromSpec(List<String> patternPairs)
      throws IllegalArgumentException {
    return (PatternIntMap)new PatternIntMap().parseSpec(patternPairs);
  }

  /** Create a PatternIntMap from a string of the form
   * <code><i>RE</i>,<i>string</i>[;<i>RE</i>,<i>string</i> ...]</code> */
  public static PatternIntMap fromSpec(String spec)
      throws IllegalArgumentException {
    return (PatternIntMap)new PatternIntMap().parseSpec(specList(spec));
  }

  protected Integer parseRhs(String rhs) throws NumberFormatException {
    return Integer.parseInt(rhs);
  }

  /** Return the value associated with the first pattern that the string
   * matches, or 0.0 if none match.
   * @param str the string to match against the LHS patterns.
   * @return the value associated with the first pattern that matches
   * str, or 0.0 if none match.
   */
  public Integer getMatch(String str) {
    return super.getMatch(str, 0);
  }

  /** Return the value associated with the first pattern that the string
   * matches, or the specified default value if none.
   * @param str the string to match against the LHS patterns.
   * @param dfault the value to return if no pattern matches str.
   * @return the value associated with the first pattern that matches
   * str, or dfault if none match.
   */
  public Integer getMatch(String str, int dfault) {
    return super.getMatch(str, dfault);
  }

  /** Return the value associated with the first pattern that the string
   * matches, or the specified default value if none, considering only
   * patterns whose associated value is no greater than maxPri
   * @param str the string to match against the LHS patterns.
   * @param dfault the value to return if no pattern (whose value
   * is less than or equal to maxPri) matches str.
   * @param maxPri the largest acceptable result value
   * @return the first value LE maxPri associated with a pattern that
   * matches str.
   */
  /** Return the value associated with the first pattern that the string
   * matches, or the specified default value if none, considering only
   * patterns whose associated value is less than or equal to maxPri. */
  public Integer getMatch(String str, int dfault, int maxPri) {
    return super.getMatch(str, dfault, x -> x <= maxPri);
  }
}
