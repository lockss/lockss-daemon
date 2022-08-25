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
import org.apache.commons.lang3.tuple.*;

/** "Map" strings to arbitrary Objects, where the keys are patterns
 * against which the strings are matched.  The patterns are ordered;
 * the value associated with the first one that matches is
 * returned.  */
public class PatternMap<T> extends AbstractPatternMap<T> {

  /** An empty PatternMap, which always returns the default
   * value. */
  public final static PatternMap EMPTY =
    (PatternMap)new PatternMap().compilePairs(Collections.emptyList());

  protected PatternMap() {
    super();
  }

  /** Create a PatternMap from a list of strings of the form
   * <code><i>RE</i>,<i>string</i></code>
   */
  public static <T> PatternMap<T> fromPairs(List<Pair<String,T>> patternPairs)
      throws IllegalArgumentException {
    return (PatternMap)new PatternMap().compilePairs(patternPairs);
  }

  protected T parseRhs(String rhs) {
    throw new UnsupportedOperationException("Shouldn't happen");
  }
}
