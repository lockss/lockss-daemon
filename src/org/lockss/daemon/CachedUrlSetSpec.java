/*
 * $Id: CachedUrlSetSpec.java,v 1.5 2003-01-28 00:32:11 aalto Exp $
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

/**
 * Specifies a set of URLs.  Used by <code>CachedUrlSet</code> to determine
 * which URLs are included, and to enumerate starting points.
 */
public interface CachedUrlSetSpec {
  /**
   * Determine whether a url is part of this <code>CachedUrlSetSpec</code>.
   * @param url The url.
   * @return true iff the url matches the spec
   */
  public boolean matches(String url);

  /**
   * Get the URLPrefix list.
   * @return the list of URL prefixes, as Strings
   */
  public List getPrefixList();

  /**
   * This needs to be overridden to hash CachedUrlSets properly.
   * @return the hashcode
   */
  public int hashCode();

  /**
   * This needs to be overridden to hash CachedUrlSets properly.
   * @param obj the object to compare to
   * @return true if the objects are equal
   */
  public boolean equals(Object obj);

  /**
   * Returns a unique id string.
   * @return the unique id
   */
  public String getIdString();

  /**
   * Returns the primary url referenced by the spec.
   * @return the url
   */
  public String getPrimaryUrl();
}
