/*
 * $Id: AnyCachedUrlSetSpec.java,v 1.5 2003-02-20 02:23:40 aalto Exp $
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
 * A CachedUrlSetSpec that matches any of a list of CachedUrlSetSpecs
 */
public class AnyCachedUrlSetSpec implements CachedUrlSetSpec {
  private static Logger logger = Logger.getLogger("CachedUrlSetSpec");
  private Set set;

  /**
   * Create a CachedUrlSetSpec that matches if any of the CachedUrlSetSpecs
   * in the list match.
   * @param specSet list of CachedUrlSetSpecs
   * @throws NullPointerException if the list or any element is null.
   * @throws ClassCastException if any element isn't a CachedUrlSetSpec
   */
  public AnyCachedUrlSetSpec(Set specSet) throws ClassCastException {
    this.set = SetUtil.immutableSetOfType(specSet, CachedUrlSetSpec.class);
  }

  /**
   * Return true if any of the contained CachedUrlSetSpecs matches the URL.
   * @param url the url to test
   * @return true if it matches any of the sub-specs
   */
  public boolean matches(String url) {
    for (Iterator iter = set.iterator(); iter.hasNext(); ) {
      CachedUrlSetSpec cuss = (CachedUrlSetSpec)iter.next();
      if (cuss.matches(url)) {
	return true;
      }
    }
    return false;
  }

  /**
   * Return a list of all URL prefixes in the contained CachedUrlSetSpecs.
   * Duplicates are not removed.
   * @return the prefix list
   */
  public List getPrefixList() {
    List res = new LinkedList();
    for (Iterator iter = set.iterator(); iter.hasNext(); ) {
      CachedUrlSetSpec cuss = (CachedUrlSetSpec)iter.next();
      if (cuss instanceof AnyCachedUrlSetSpec) {
        res.addAll(((AnyCachedUrlSetSpec)cuss).getPrefixList());
      } else {
        res.add(cuss.getUrl());
      }
    }
    return res;
  }

  /**
   * Overrides Object.hashCode().
   * Returns the sum of the sub-spec hashcodes.
   * @return the hashcode
   */
  public int hashCode() {
    int hashSum = 0;
    for (Iterator iter = set.iterator(); iter.hasNext(); ) {
      CachedUrlSetSpec cuss = (CachedUrlSetSpec)iter.next();
      hashSum += cuss.hashCode();
    }
    return hashSum;
  }

  /**
   * Overrides Object.equals().
   * Compares the CUSS sets.
   * @param obj the object to compare to
   * @return true if the sets are equal
   */
  public boolean equals(Object obj) {
    if (obj instanceof AnyCachedUrlSetSpec) {
      AnyCachedUrlSetSpec spec = (AnyCachedUrlSetSpec)obj;
      return set.equals(spec.set);
    } else {
      return false;
    }
  }

  /**
   * Returns a CSV list of the sub-spec id strings.
   * @return the id
   */
  public String toString() {
    return "[AnyCUSS: "+StringUtil.separatedString(set, ", ")+"]";
  }

  /**
   * Returns the primary url of the first sub-spec, or null if none.
   * @return the url
   */
  public String getUrl() {
    if (set.size()>0) {
      return ((CachedUrlSetSpec)set.iterator().next()).getUrl();
    } else {
      return null;
    }
  }
}
