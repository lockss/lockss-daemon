/*
 * $Id: AUCachedUrlSetSpec.java,v 1.2 2003-06-03 01:52:50 tal Exp $
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
   * Returns "LOCKSSAU:"
   * @return the url
   */
  public String getUrl() {
    return URL;
  }

  /**
   * Always returns true; all URLs fall within this spec
   * @return true
   */
  public boolean matches(String url) {
    return true;
  }

  public boolean isAU() {
    return true;
  }

  public boolean isSingleNode() {
    return false;
  }

  public boolean isRangeRestricted() {
    return false;
  }

  public boolean isDisjoint(CachedUrlSetSpec spec) {
    return false;
  }

  public boolean subsumes(CachedUrlSetSpec spec) {
    return true;
  }

  /**
   * overrides Object.toString()
   * @return String representaion of this object
   */
  public String toString() {
    return "[AUCUSS]";
  }

  /**
   * Overrides Object.equals().
   * Compares the lists and REs of the two specs.
   * @param obj the other spec
   * @return true if the lists and REs are equal
   */
  public boolean equals(Object obj) {
    return (obj instanceof AUCachedUrlSetSpec);
  }

  /**
   * Overrides Object.hashCode().
   * Returns the hash of the strings
   * @return the hashcode
   */
  public int hashCode() {
    return URL.hashCode();
  }
}
