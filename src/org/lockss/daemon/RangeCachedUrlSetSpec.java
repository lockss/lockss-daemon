/*
 * $Id: RangeCachedUrlSetSpec.java,v 1.1 2003-02-13 01:13:15 claire Exp $
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

import java.util.List;
import java.util.Collections;
import org.lockss.util.ListUtil;

/**
 * A CachedUrlSetSpec that matches based on a URL prefix and an optional
 * set of upper and lower bounds which are used to define the range
 * @author Claire Griffin
 * @version 1.0
 */

public class RangeCachedUrlSetSpec implements CachedUrlSetSpec{
  private String prefix;
  private List prefixList;
  private String upperBound;
  private String lowerBound;

  /**
   * Create a RangeCachedUrlSetSpec that matches URLs that start with the prefix
   * and is between the range of lower and upper bound.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param upperBound upper boundary of the prefix range inclusive
   * @param lowerBound lower boundary of the prefix range inclusive.
   * @throws NullPointerException if the prefix is null
   */
  public RangeCachedUrlSetSpec(String urlPrefix,
                                String upperBound, String lowerBound) {
    if (urlPrefix == null) {
      throw new NullPointerException("RangeCachedUrlSetSpec with null URL");
    }
    this.prefix = urlPrefix;
    this.prefixList = Collections.unmodifiableList(ListUtil.list(urlPrefix));
    this.upperBound = upperBound;
    this.lowerBound = lowerBound;
  }

  /**
   * Create a RangeCachedUrlSetSpec that matches URLs that start with the prefix
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @throws NullPointerException if the prefix is null
   */
  public RangeCachedUrlSetSpec(String urlPrefix) {
    this(urlPrefix, null, null);
  }

  /**
   * Return true if the URL begins with the prefix and is within
   * the ranges defined by upper and lower bounds if any.
   * @param url to match
   * @return true if it matches
   */
  public boolean matches(String url) {
    if (!url.startsWith(prefix)) {
      return false;
    }
    return inRange(url);
  }

  /**
   * return the Prefix list for this CachedUrlSetSpec
   * @return the prefix list
   */
  public List getPrefixList() {
    return prefixList;
  }

  /**
   * Overrides Object.equals().
   * Compares the lists and REs of the two specs.
   * @param obj the other spec
   * @return true if the lists and REs are equal
   */
  public boolean equals(Object obj) {
    if (obj instanceof RangeCachedUrlSetSpec) {
      RangeCachedUrlSetSpec spec = (RangeCachedUrlSetSpec)obj;
      if (!prefixList.equals(spec.getPrefixList())) {
        return false;
      }

      // check the lower boundary
      if (lowerBound == null) {
        if (spec.getLowerBound() != null) {
          return false;
        }
      }
      else if (!lowerBound.equals(spec.getLowerBound())) {
        return false;
      }

      // check the upper boundary
      if (upperBound == null) {
        if (spec.getUpperBound() != null) {
          return false;
        }
      }
      else if (!upperBound.equals(spec.getUpperBound())) {
        return false;
      }
    }
    else {
      // not the right kind of object
      return false;
    }
    return true;
  }

  /**
   * Returns the first url in the prefix list, or null if none.
   * @return the url
   */
  public String getPrimaryUrl() {
    return (prefixList.size()>0 ? (String)prefixList.get(0) : null);
  }

  /**
   * overrides Object.toString()
   * @return String representaion of this object
   */
  public String toString() {
    StringBuffer sb = new StringBuffer("[CUSS: ");
    sb.append(getPrimaryUrl());

    if(lowerBound != null) {
      sb.append(" lwr:");
      sb.append(lowerBound);
    }

    if(upperBound != null) {
      sb.append(" upr:");
      sb.append(upperBound);
    }
    return sb.toString();
  }

  /**
   * Overrides Object.hashCode().
   * Returns the hash of the strings
   * @return the hashcode
   */
  public int hashCode() {
    StringBuffer sb = new StringBuffer();
    sb.append(getPrimaryUrl());

    if(lowerBound != null) {
      sb.append(lowerBound);
    }

    if(upperBound != null) {
     sb.append(upperBound);
    }
    return sb.toString().hashCode();
  }


  /**
   * return the upper bounds of the range
   * @return the string representing the upper boundary
   */
  public String getUpperBound() {
    return upperBound;
  }

  /**
   * return the lower bounds of the range
   * @return the String representing the lower boundary
   */
  public String getLowerBound() {
    return lowerBound;
  }


  /**
   * return true if the url is in the bounded range
   * @param url the url to check
   * @return true if the url is between upper and lower bound
   */
  boolean inRange(String url) {
    boolean ret = true;

     if(upperBound != null) {
      String us = prefix + upperBound;
      if(us.compareTo(url) > 0) {
        return false;
      }
    }
    if(lowerBound != null) {
      String ls = prefix + lowerBound;
      if(ls.compareTo(url) < 0)  {
        return false;
      }
    }
    return ret;
  }
}