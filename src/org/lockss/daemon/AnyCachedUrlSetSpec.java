/*
 * $Id: AnyCachedUrlSetSpec.java,v 1.3 2003-01-28 00:32:11 aalto Exp $
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
  private List list;

  /**
   * Create a CachedUrlSetSpec that matches if any of the CachedUrlSetSpecs
   * in the list match.
   * @param specList list of CachedUrlSetSpecs
   * @throws NullPointerException if the list or any element is null.
   * @throws ClassCastException if any element isn't a CachedUrlSetSpec
   */
  public AnyCachedUrlSetSpec(List specList) throws ClassCastException {
    this.list = ListUtil.immutableListOfType(specList, CachedUrlSetSpec.class);
  }

  /**
   * Return true if any of the contained CachedUrlSetSpecs matches the URL.
   * @param url the url to test
   * @return true if it matches any of the sub-specs
   */
  public boolean matches(String url) {
    for (Iterator iter = list.iterator(); iter.hasNext(); ) {
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
    for (Iterator iter = list.iterator(); iter.hasNext(); ) {
      CachedUrlSetSpec cuss = (CachedUrlSetSpec)iter.next();
      res.addAll(cuss.getPrefixList());
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
    for (Iterator iter = list.iterator(); iter.hasNext(); ) {
      CachedUrlSetSpec cuss = (CachedUrlSetSpec)iter.next();
      hashSum += cuss.hashCode();
    }
    return hashSum;
  }

  /**
   * Overrides Object.equals().
   * Returns the id strings.
   * @param obj the object to compare to
   * @return the hashcode
   */
  public boolean equals(Object obj) {
    if (obj instanceof CachedUrlSetSpec) {
      CachedUrlSetSpec spec = (CachedUrlSetSpec)obj;
      return getIdString().equals(spec.getIdString());
    } else {
      logger.error("Trying to compare a spec and a non-spec.");
      throw new IllegalArgumentException("Trying to compare spec to non-spec.");
    }
  }

  /**
   * Returns a CSV list of the sub-spec id strings.
   * @return the id
   */
  public String getIdString() {
    StringBuffer buffer = new StringBuffer();
    for (Iterator iter = list.iterator(); iter.hasNext(); ) {
      CachedUrlSetSpec cuss = (CachedUrlSetSpec)iter.next();
      if (buffer.length()>0) {
        buffer.append(",");
      }
      buffer.append(cuss.getIdString());
    }
    return buffer.toString();
  }

  /**
   * Returns the primary url of the first sub-spec, or null if none.
   * @return the url
   */
  public String getPrimaryUrl() {
    if (list.size()>0) {
      return ((CachedUrlSetSpec)list.get(0)).getPrimaryUrl();
    } else {
      return null;
    }
  }
}
