/*
 * $Id: BaseUrlCacher.java,v 1.2 2002-11-02 01:51:27 troberts Exp $
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Properties;

/** Abstract base class for UrlCachers.
 * Plugins may extend this to get some common UrlCacher functionality.
 */
public abstract class BaseUrlCacher implements UrlCacher {
  protected CachedUrlSet cus;
  protected String url;

  /** Must invoke this constructor in plugin subclass. */
  protected BaseUrlCacher(CachedUrlSet owner, String url) {
    this.cus = owner;
    this.url = url;
  }

  /** Return the URL */
  public String getUrl() {
    return url;
  }

  /**
   * Overrides normal <code>toString()</code> to return the url for this cacher
   */
  public String toString(){
    return url;
  }

  /**
   * Return the CachedUrlSet to which this CachedUrl belongs.
   */
  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  /**
   * Return the ArchivalUnit to which this CachedUrl belongs.
   */
  public ArchivalUnit getArchivalUnit() {
    CachedUrlSet cus = getCachedUrlSet();
    return cus.getArchivalUnit();
  }

  /** Return <code>true</code> if the underlying url is one that
   * the plug-in believes should be preserved.
   */
  public boolean shouldBeCached() {
    return getArchivalUnit().shouldBeCached(getUrl());
  }

  /**
   * Return a CachedUrl for the content stored.  May be
   * called only after the content is completely written.
   * @return CachedUrl for the content stored.
   */
  public CachedUrl getCachedUrl() {
    return getCachedUrlSet().makeCachedUrl(getUrl());
  }

  /** Copy the content and properties from the source into the cache */
  public void cache() throws IOException {
    storeContent(getUncachedInputStream(),
		 getUncachedProperties());
  }
}
