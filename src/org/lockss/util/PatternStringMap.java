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

/** "Map" strings to string, where the keys are patterns against which
 * the strings are matched.  The patterns are ordered; the value associated
 * with the first one that matches is returned.  */
public class PatternStringMap extends AbstractPatternMap<String> {

  /** An empty PatternStringMap, which always returns the default
   * value. */
  public final static PatternStringMap EMPTY =
    new PatternStringMap(Collections.emptyList());

  private PatternStringMap() {
    super();
  }

  /** Create a PatternStringMap from a list of strings of the form
   * <code><i>RE</i>,<i>string</i></code>
   * @deprecated use {@link #fromSpec()}
   */
  @Deprecated
  public PatternStringMap(List<String> patternPairs)
      throws IllegalArgumentException {
    super(patternPairs);
  }

  /** Create a PatternStringMap from a string of the form
   * <code><i>RE</i>,<i>string</i>[;<i>RE</i>,<i>string</i> ...]</code>
   * @deprecated use {@link #fromSpec()}
   */
  @Deprecated
  public PatternStringMap(String spec)
      throws IllegalArgumentException {
    super(spec);
  }

  /** Create a PatternStringMap from a list of strings of the form
   * <code><i>RE</i>,<i>string</i></code> */
  public static PatternStringMap fromSpec(List<String> patternPairs)
      throws IllegalArgumentException {
    return (PatternStringMap)new PatternStringMap().parseSpec(patternPairs);
  }

  /** Create a PatternStringMap from a string of the form
   * <code><i>RE</i>,<i>string</i>[;<i>RE</i>,<i>string</i> ...]</code> */
  public static PatternStringMap fromSpec(String spec)
      throws IllegalArgumentException {
    return (PatternStringMap)new PatternStringMap().parseSpec(specList(spec));
  }

  protected String parseRhs(String rhs) {
    return rhs;
  }
}
