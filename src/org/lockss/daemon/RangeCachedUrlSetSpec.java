/*
 * $Id: RangeCachedUrlSetSpec.java,v 1.10.2.1 2003-06-09 20:14:23 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.lockss.util.*;

/**
 * A CachedUrlSetSpec that specifies all or part of a subtree, rooted at
 * the prefix URL.  If neither lower nor upper bound is specified, the set
 * is the entire subtree, including the node named by the URL.  If either
 * bound is specified, the set is the union of the subtrees rooted at all
 * of this node's children whose names fall within the bounds, inclusive.
 * The node named by the URL is not considered part of the set in this case.
 * @author Claire Griffin
 * @version 1.0
 */
public class RangeCachedUrlSetSpec implements CachedUrlSetSpec {
  private String prefix;
  private String upperBound;
  private String lowerBound;

  /**
   * Create a RangeCachedUrlSetSpec that matches URLs that start with the
   * prefix and are within the specified range.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param lowerBound lower boundary of the prefix range, inclusive.  If
   * null, the range is unbounded at the bottom.
   * @param upperBound upper boundary of the prefix range, inclusive.  If
   * null, the range is unbounded at the top.
   * @throws NullPointerException if the prefix is null
   */
  public RangeCachedUrlSetSpec(String urlPrefix,
			       String lowerBound, String upperBound) {
    if (urlPrefix == null) {
      throw new NullPointerException("RangeCachedUrlSetSpec with null URL");
    }
    this.prefix = urlPrefix;
    this.upperBound = upperBound;
    this.lowerBound = lowerBound;
  }

  /**
   * Create a RangeCachedUrlSetSpec that matches URLs that start with the
   * prefix, including that URL.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @throws NullPointerException if the prefix is null
   */
  public RangeCachedUrlSetSpec(String urlPrefix) {
    this(urlPrefix, null, null);
  }

  /**
   * @param url the url to match
   * @return true if the URL's prefix matches ours, and the remaining
   * substring is within the range defined by upper and lower bounds, if
   * any.  The URL of a node does not match the range-restricted CUSS at
   * that node.
   */
  public boolean matches(String url) {
    if (!url.startsWith(prefix)) {
      return false;
    }
    // if url length is same, it's this node
    if (url.length() == prefix.length()) {
      return !isRangeRestricted();
    }
    return inRange(url);
  }

  private boolean isInitialPathComponent(String path, String ofPath) {
    // is initial path if starts with
    if (!ofPath.startsWith(path)) {
      return false;
    } else {
      // and next char is '/' (so 'foo' doesn't start 'foobar' but does 'foo/bar')
      return (ofPath.charAt(path.length()) == UrlUtil.URL_PATH_SEPARATOR_CHAR);
    }
  }

  /** @return false */
  public boolean isAU() {
    return false;
  }

  /** @return false */
  public boolean isSingleNode() {
    return false;
  }

  /* @return true iff either the lower or upper bound, or both, is
   * non-null. */
  public boolean isRangeRestricted() {
    return lowerBound != null || upperBound != null;
  }

  /**
   * @param spec the set to test disjointness with
   * @return true if the two sets are disjoint
   */
  public boolean isDisjoint(CachedUrlSetSpec spec) {
    if (spec.isSingleNode()) {
      return !matches(spec.getUrl());
    }
    if (spec.isAU()) {
      return false;
    }
    if (spec instanceof RangeCachedUrlSetSpec) {
      RangeCachedUrlSetSpec rspec = (RangeCachedUrlSetSpec)spec;
      if (prefix.equals(rspec.getUrl())) {
	// same node, check ranges disjoint
	String l1 = lowerBound;
	String u1 = upperBound;
	String l2 = rspec.getLowerBound();
	String u2 = rspec.getUpperBound();
	return (l1 != null && u2 != null && l1.compareTo(u2) > 0) ||
	  (l2 != null && u1 != null && l2.compareTo(u1) > 0);
      } else {
	// different node, disjoint if neither root is included in other's set
	return !(matches(rspec.getUrl()) || rspec.matches(prefix));
      }
    }
    throw new RuntimeException("Unknown CUSS type: " + spec);
  }

  /**
   * @param spec the set to test subsumption of
   * @return true if spec is entirely contained in this one
   */
  public boolean subsumes(CachedUrlSetSpec spec) {
    if (spec.isSingleNode()) {
      return matches(spec.getUrl());
    }
    if (spec.isAU()) {
      return false;
    }
    if (spec instanceof RangeCachedUrlSetSpec) {
      RangeCachedUrlSetSpec rspec = (RangeCachedUrlSetSpec)spec;
      if (prefix.equals(rspec.getUrl())) {
	// same node, check range1 includes range2
	String l1 = lowerBound;
	String u1 = upperBound;
	String l2 = rspec.getLowerBound();
	String u2 = rspec.getUpperBound();
	return (l1 == null || (l2 != null && l1.compareTo(l2) <= 0)) &&
	  (u1 == null || (u2 != null && u1.compareTo(u2) >= 0));
      } else {
	// different node, subsumed if its root is in our set
	return matches(rspec.getUrl());
      }
    }
    throw new RuntimeException("Unknown CUSS type: " + spec);
  }

  /**
   * @param obj the other spec
   * @return true if the prefix and ranges are equal.
   */
  public boolean equals(Object obj) {
    if (obj instanceof RangeCachedUrlSetSpec) {
      RangeCachedUrlSetSpec spec = (RangeCachedUrlSetSpec)obj;
      return prefix.equals(spec.getUrl()) &&
	StringUtil.equalStrings(lowerBound, spec.getLowerBound()) &&
	StringUtil.equalStrings(upperBound, spec.getUpperBound());
    } else {
      // not a RangeCachedUrlSetSpec
      return false;
    }
  }

  /**
   * Returns the prefix URL.
   * @return the url
   */
  public String getUrl() {
    return prefix;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("[CUSS: ");
    sb.append(prefix);

    if (lowerBound != null || upperBound != null) {
      sb.append(" [");

      if (lowerBound != null) {
	sb.append(lowerBound);
      }
      sb.append(" - ");
      if (upperBound != null) {
	sb.append(upperBound);
      }
      sb.append("]");
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * @return a hash made from the prefix and bounds.
   */
  public int hashCode() {
    int hash = 0x40700704;
    hash += prefix.hashCode();
    if (lowerBound != null) {
      hash += 3 * lowerBound.hashCode();
    }
    if (upperBound != null) {
      hash += 5 * upperBound.hashCode();
    }
    return hash;
  }

  /**
   * Return the upper bound of the range
   * @return the String representing the upper boundary
   */
  public String getUpperBound() {
    return upperBound;
  }

  /**
   * Return the lower bound of the range
   * @return the String representing the lower boundary
   */
  public String getLowerBound() {
    return lowerBound;
  }

  // Cache the concatenated bounds strings used by inRange()
  String us;
  String ls;

  private String getUs() {
    if (us == null) {
      us = prefix + upperBound;
    }
    return us;
  }

  private String getLs() {
    if (ls == null) {
      ls = prefix + lowerBound;
    }
    return ls;
  }

  /**
   * Return true if the url is in the bounded range.
   * @param url the url to check
   * @return true if the url is between upper and lower bound
   */
  boolean inRange(String url) {
    if (upperBound != null) {
      if (getUs().compareTo(url) < 0) { //url is lexographically greater
	return false;
      }
    }
    if (lowerBound != null) {
      if (getLs().compareTo(url) > 0)  {
	return false;
      }
    }
    return true;
  }
}
