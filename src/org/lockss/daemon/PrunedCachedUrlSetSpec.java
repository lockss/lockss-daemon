/*
 * $Id: PrunedCachedUrlSetSpec.java,v 1.3.6.2 2012-06-20 00:02:58 nchondros Exp $
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

package org.lockss.daemon;

import java.util.regex.*;
import org.apache.commons.lang.*;

import org.lockss.util.*;

/**
 * A RangeCachedUrlSetSpec that includes or excludes subtrees that match a
 * pattern
 */
public class PrunedCachedUrlSetSpec extends RangeCachedUrlSetSpec {
  static Logger log = Logger.getLogger("PCUSS");

  Pattern includePat;
  Pattern excludePat;

  public static PrunedCachedUrlSetSpec
    includeMatchingSubTrees(String urlPrefix, Pattern includePat) {
    return new PrunedCachedUrlSetSpec(urlPrefix, includePat, null);
  }

  public static PrunedCachedUrlSetSpec
    excludeMatchingSubTrees(String urlPrefix, Pattern excludePat) {
    return new PrunedCachedUrlSetSpec(urlPrefix, null, excludePat);
  }

  public static PrunedCachedUrlSetSpec
    includeMatchingSubTrees(String urlPrefix,
			    String includePattern, int flags) {
    Pattern pat = Pattern.compile(includePattern, flags);
    return PrunedCachedUrlSetSpec.includeMatchingSubTrees(urlPrefix, pat);
  }

  public static PrunedCachedUrlSetSpec
    excludeMatchingSubTrees(String urlPrefix,
			    String includePattern, int flags) {
    Pattern pat = Pattern.compile(includePattern, flags);
    return PrunedCachedUrlSetSpec.excludeMatchingSubTrees(urlPrefix, pat);
  }

  public static PrunedCachedUrlSetSpec
    includeMatchingSubTrees(String urlPrefix,
			    String includePattern) {
    return PrunedCachedUrlSetSpec.includeMatchingSubTrees(urlPrefix,
							  includePattern, 0);
  }

  public static PrunedCachedUrlSetSpec
    excludeMatchingSubTrees(String urlPrefix,
			    String excludePattern) {
    return PrunedCachedUrlSetSpec.excludeMatchingSubTrees(urlPrefix,
							  excludePattern, 0);
  }

  /**
   * Create a PrunedCachedUrlSetSpec that matches URLs that start with the
   * prefix and are within the specified range.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param lowerBound lower boundary of the prefix range, inclusive.  If
   * null, the range is unbounded at the bottom.
   * @param upperBound upper boundary of the prefix range, inclusive.  If
   * null, the range is unbounded at the top.
   * @throws NullPointerException if the prefix is null.
   * @throws IllegalArgumentException if lower bound is greater than upper.
   */
  PrunedCachedUrlSetSpec(String urlPrefix,
			 Pattern includePat, Pattern excludePat) {
    super(urlPrefix, null, null);
    this.includePat = includePat;
    this.excludePat = excludePat;
  }

  public boolean matches(String url) {
    boolean res = matches0(url);
    return res;
  }

  boolean matches0(String url) {
    if (!super.matches(url)) {
      return false;
    }
    if (includePat != null) {
      Matcher mat = includePat.matcher(url);
      return mat.lookingAt() || mat.hitEnd();
    }	
    if (excludePat != null) {
      Matcher mat = excludePat.matcher(url);
       return !mat.lookingAt();
    }	
    return true;
  }

  public boolean isDisjoint(CachedUrlSetSpec spec) {
    throw new UnsupportedOperationException("isDisjoint() not implemented for PrunedCachedUrlSetSpec");
  }

  /**
   * @param spec the set to test subsumption of
   * @return true if spec is entirely contained in this one
   */
  public boolean subsumes(CachedUrlSetSpec spec) {
    throw new UnsupportedOperationException("subsumes() not implemented for PrunedCachedUrlSetSpec");
  }

  /**
   * @param obj the other spec
   * @return true if the prefix and include/exclude patterns are equal.
   */
  public boolean equals(Object obj) {
    if (obj instanceof PrunedCachedUrlSetSpec) {
      PrunedCachedUrlSetSpec spec = (PrunedCachedUrlSetSpec)obj;
      return super.equals(obj) &&
	RegexpUtil.patEquals(includePat, spec.includePat) &&
	RegexpUtil.patEquals(excludePat, spec.excludePat);
    } else {
      // not a PrunedCachedUrlSetSpec
      return false;
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("[PCUSS: ");
    appendPrefRange(sb);
    if (includePat != null) {
      sb.append(" incl: ");
      sb.append(includePat.pattern());
      sb.append("(");
      sb.append(includePat.flags());
      sb.append(")");
    }      
    if (excludePat != null) {
      sb.append(" excl: ");
      sb.append(excludePat.pattern());
      sb.append("(");
      sb.append(excludePat.flags());
      sb.append(")");
    }      
    sb.append("]");
    return sb.toString();
  }

  /**
   * @return a hash made from the prefix and include/exclude patterns.
   */
  public int hashCode() {
    int hash = super.hashCode();
    if (includePat != null) {
      hash += 7 * includePat.hashCode();
    }
    if (excludePat != null) {
      hash += 11 * excludePat.hashCode();
    }
    return hash;
  }

}
