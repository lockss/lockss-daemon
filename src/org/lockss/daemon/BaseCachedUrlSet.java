/*
 * $Id: BaseCachedUrlSet.java,v 1.5 2003-01-25 02:21:11 aalto Exp $
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
import java.io.File;

/**
 * Abstract base class for CachedUrlSets.
 * Plugins may extend this to get some common CachedUrlSet functionality.
 */
public abstract class BaseCachedUrlSet implements CachedUrlSet {
  protected ArchivalUnit au;
  protected CachedUrlSetSpec spec;

  /**
   * Must invoke this constructor in plugin subclass.
   * @param owner the AU to which it belongs
   * @param spec the CachedUrlSet's spec
   */
  public BaseCachedUrlSet(ArchivalUnit owner, CachedUrlSetSpec spec) {
    this.spec = spec;
    this.au = owner;
  }

  /**
   * Return the CachedUrlSetSpec
   * @return the spec
   */
  public CachedUrlSetSpec getSpec() {
    return spec;
  }

  /**
   * Return the enclosing ArchivalUnit
   * @return the AU
   */
  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  /**
   * Return true if content for the url is present in the CachedUrlSet
   * @param url the url to test
   * @return true if it is already cached
   */
  public boolean isCached(String url) {
    CachedUrl cu = makeCachedUrl(url);
    return cu == null ? false : cu.exists();
  }

  /**
   * Return true if the url falls within the scope of this CachedUrlSet,
   * whether it is present in the cache or not
   * @param url the url to test
   * @return true if is within the scope
   */
  public boolean containsUrl(String url) {
    return (null != makeCachedUrl(url));
  }

  /**
   * Overridden to return the toString() method of the CachedUrlSetSpec.
   * @return the spec string
   */
  public String toString() {
    return "[BCUS: "+spec+"]";
  }

  /**
   * Returns the first url in the prefix list.  Strips the trailing seperator
   * character ('/'), if any.
   * @return the unique id
   */
  public String getIdString() {
    return spec.getIdString();
  }

  /**
   * Returns the main url of the spec.
   * @return the url
   */
  public String getPrimaryUrl() {
    return spec.getPrimaryUrl();
  }

  /**
   * Overrides Object.hashCode();
   * Returns the hashcode of the spec.
   * @return the hashcode
   */
  public int hashCode() {
    return spec.hashCode();
  }

}
