/*
 * $Id: RECachedUrlSetSpec.java,v 1.2 2002-11-14 22:00:24 tal Exp $
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
import gnu.regexp.*;
import org.lockss.util.*;

/**
 * A CachedUrlSetSpec that matches based on a URL prefix and an optional
 * regular expression
 */
public class RECachedUrlSetSpec implements CachedUrlSetSpec {
  private String prefix;
  private List prefixList;
  private RE re;

  /**
   * Create a CachedUrlSetSpec that matches URLs that start with the prefix
   * and match the regexp.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param regexp Regular expression that URLs must also match.
   * @throws NullPointerException if the prefix is null
   */
  public RECachedUrlSetSpec(String urlPrefix, RE regexp) {
    if (urlPrefix == null) {
      throw new NullPointerException("RECachedUrlSetSpec with null URL");
    }
    this.prefix = urlPrefix;
    this.prefixList = Collections.unmodifiableList(ListUtil.list(urlPrefix));
    this.re = regexp;
  }
  
  /**
   * Create a CachedUrlSetSpec that matches URLs that start with the prefix
   * and match the regexp.
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @param regexp Regular expression that URLs must also match.
   * @throws NullPointerException if the prefix is null
   * @throws REException if the regexp is invalid
   */
  public RECachedUrlSetSpec(String urlPrefix, String regexp)
      throws REException {
    this(urlPrefix, regexp != null ? new RE(regexp) : null);
  }
  
  /**
   * Create a CachedUrlSetSpec that matches URLs that start with the prefix
   * @param urlPrefix Common prefix of URLs in the CachedUrlSetSpec.
   * @throws NullPointerException if the prefix is null
   */
  public RECachedUrlSetSpec(String urlPrefix) {
    this(urlPrefix, (RE)null);
  }
  
  /**
   * Return true if the URL begins with the prefix and matches the regexp,
   * if any.
   */
  public boolean matches(String url) {
    // tk - should be case-independent, at least for protocol & host part.
    if (!url.startsWith(prefix)) {
      return false;
    }
    return (re != null) ? (null != re.getMatch(url)) : true;
  }

  /**
   * Return a list containing the URL prefix
   */
  public List getPrefixList() {
    return prefixList;
  }

  /**
   * Return the RE, or null if none
   */
  public RE getRE() {
    return re;
  }

  public boolean equals(Object o) {
    if (! (o instanceof RECachedUrlSetSpec)) {
      return false;
    }
    RECachedUrlSetSpec c = (RECachedUrlSetSpec)o;
    RE cre = c.getRE();
    return prefixList.equals(c.getPrefixList()) &&
      (re == null ? cre == null : re.toString().equals(cre.toString()));
  }

  public String toString() {
    return "[CUSS: " + prefixList.get(0) +
      ((re == null) ? "]" : (", " + re + "]"));
  }
}
