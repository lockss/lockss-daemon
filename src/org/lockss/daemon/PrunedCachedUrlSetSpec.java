/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.*;

/**
 * A PrunedCachedUrlSetSpec either includes only subtrees that match a
 * pattern, or includes only subtrees that do not match a pattern.  In the
 * include case, paths that are ancestors of the subtree pattern are also
 * included (so that iterations will proceed down into the included
 * subtree).
 */
public class PrunedCachedUrlSetSpec extends RangeCachedUrlSetSpec {
  
  private static final Logger log = Logger.getLogger("PCUSS");

  Pattern includePat;
  Pattern excludePat;

  /**
   * Create a PrunedCachedUrlSetSpec that matches URLs that are within, or
   * are an ancestor of, the subtree(s) specified by the pattern.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param includePat Pattern specifying the subtree(s) to be included.
   */
  public static PrunedCachedUrlSetSpec
    includeMatchingSubTrees(String urlPrefix, Pattern includePat) {
    return new PrunedCachedUrlSetSpec(urlPrefix, includePat, null);
  }

  /**
   * Create a PrunedCachedUrlSetSpec that matches URLs that are not within
   * the subtree(s) specified by the pattern.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param excludePat Pattern specifying the subtree(s) to be excluded.
   */
  public static PrunedCachedUrlSetSpec
    excludeMatchingSubTrees(String urlPrefix, Pattern excludePat) {
    return new PrunedCachedUrlSetSpec(urlPrefix, null, excludePat);
  }

  /**
   * Create a PrunedCachedUrlSetSpec that matches URLs that are within, or
   * are an ancestor of, the subtree(s) specified by the pattern.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param includePattern Regexp specifying the subtree(s) to be included.
   * @param flags Compilation flags for the regexp.
   */
  public static PrunedCachedUrlSetSpec
    includeMatchingSubTrees(String urlPrefix,
			    String includePattern, int flags) {
    Pattern pat = Pattern.compile(includePattern, flags);
    return PrunedCachedUrlSetSpec.includeMatchingSubTrees(urlPrefix, pat);
  }

  /**
   * Create a PrunedCachedUrlSetSpec that matches URLs that are not within
   * the subtree(s) specified by the pattern.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param excludePattern Regexp specifying the subtree(s) to be excluded.
   * @param flags Compilation flags for the regexp.
   */
  public static PrunedCachedUrlSetSpec
    excludeMatchingSubTrees(String urlPrefix,
			    String excludePattern, int flags) {
    Pattern pat = Pattern.compile(excludePattern, flags);
    return PrunedCachedUrlSetSpec.excludeMatchingSubTrees(urlPrefix, pat);
  }

  /**
   * Create a PrunedCachedUrlSetSpec that matches URLs that are within, or
   * are an ancestor of, the subtree(s) specified by the pattern.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param includePattern Regexp specifying the subtree(s) to be included.
   */
  public static PrunedCachedUrlSetSpec
    includeMatchingSubTrees(String urlPrefix,
			    String includePattern) {
    return PrunedCachedUrlSetSpec.includeMatchingSubTrees(urlPrefix,
							  includePattern, 0);
  }

  /**
   * Create a PrunedCachedUrlSetSpec that matches URLs that are not within
   * the subtree(s) specified by the pattern.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param excludePattern Regexp specifying the subtree(s) to be excluded.
   */
  public static PrunedCachedUrlSetSpec
    excludeMatchingSubTrees(String urlPrefix,
			    String excludePattern) {
    return PrunedCachedUrlSetSpec.excludeMatchingSubTrees(urlPrefix,
							  excludePattern, 0);
  }

  /**
   * Create a PrunedCachedUrlSetSpec that matches URLs that start with the
   * prefix and either are or are not within one or mow subtrees specified
   * by a pattern.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param includePat If non-null, the set includes URLs that are "below"
   * the pattern (i.e., the pattern matches an initial substring of the
   * URL), or "above" the pattern (i.e., the URL doesn't match only because
   * it's shorter than the pattern).
   * @param excludePat If non-null, the set includes URLs that aren't
   * "below" the pattern (i.e., the pattern does not match an initial
   * substring of the URL).
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

  /** Not supported
   * @throws UnsupportedOperationException always
   */
  public boolean isDisjoint(CachedUrlSetSpec spec) {
    throw new UnsupportedOperationException("isDisjoint() not implemented for PrunedCachedUrlSetSpec");
  }

  /** Not supported
   * @throws UnsupportedOperationException always
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
