/*
 * $Id: AUCachedUrlSetSpec.java,v 1.4 2003-06-20 22:34:50 claire Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.plugin.*;

/**
 * A CachedUrlSetSpec that represents an entire ArchivalUnit.
 */

public class AUCachedUrlSetSpec implements CachedUrlSetSpec {

  public static final String URL = AuUrl.PROTOCOL_COLON;

  /**
   * Create an AUCachedUrlSetSpec
   */
  public AUCachedUrlSetSpec() {
  }

  /**
   * @return "LOCKSSAU:"
   */
  public String getUrl() {
    return URL;
  }

  /**
   * @param url The url.
   * @return true - all URLs match this spec
   */
  public boolean matches(String url) {
    return true;
  }

  /**
   * @return true
   */
  public boolean isAU() {
    return true;
  }

  /**
   * @return false
   */
  public boolean isSingleNode() {
    return false;
  }

  /**
   * @return false
   */
  public boolean isRangeRestricted() {
    return false;
  }

  /**
   * @arg spec the set to test disjointness with
   * @return false - this overlaps any other CUSS in the same AU
   */
  public boolean isDisjoint(CachedUrlSetSpec spec) {
    return false;
  }

  /**
   * @arg spec the set to test subsumption of
   * @return true - this subsumes any other CUSS in the same AU
   */
  public boolean subsumes(CachedUrlSetSpec spec) {
    return true;
  }

  public String toString() {
    return "[AUCUSS]";
  }

  /**
   * @param obj the object to compare to
   * @return true iff the argument is also an AUCachedUrlSetSpec
   */
  public boolean equals(Object obj) {
    return (obj instanceof AUCachedUrlSetSpec);
  }

  /**
   * @return the URL's hashcode
   */
  public int hashCode() {
    return URL.hashCode();
  }
}
