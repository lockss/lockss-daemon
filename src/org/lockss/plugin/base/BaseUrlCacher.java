/*
 * $Id: BaseUrlCacher.java,v 1.2 2003-05-06 20:05:28 aalto Exp $
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

package org.lockss.plugin.base;

import java.io.*;
import java.util.Properties;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * Abstract base class for UrlCachers.
 * Plugins may extend this to get some common UrlCacher functionality.
 */
public abstract class BaseUrlCacher implements UrlCacher {
  protected CachedUrlSet cus;
  protected String url;
  protected static Logger logger = Logger.getLogger("UrlCacher");

  /**
   * Must invoke this constructor in plugin subclass.
   * @param owner the CachedUrlSet which ownes the url
   * @param url the url string
   */
  protected BaseUrlCacher(CachedUrlSet owner, String url) {
    this.cus = owner;
    this.url = url;
  }

  /**
   * Return the URL in string form
   * @return the url string
   */
  public String getUrl() {
    return url;
  }

  /**
   * Overrides normal <code>toString()</code> to return a string like
   * "BUC: <url>"
   * @return the class-url string
   */
  public String toString() {
    return "[BUC: "+url+"]";
  }

  /**
   * Return the CachedUrlSet to which this CachedUrl belongs.
   * @return the owner CachedUrlSet
   */
  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  /**
   * Return the ArchivalUnit to which this CachedUrl belongs.
   * @return the owner ArchivalUnit
   */
  public ArchivalUnit getArchivalUnit() {
    CachedUrlSet cus = getCachedUrlSet();
    return cus.getArchivalUnit();
  }

  /**
   * Return <code>true</code> if the underlying url is one that
   * the plug-in believes should be preserved.
   * @return a boolean indicating if it should be cached
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

  /**
   * Copy the content and properties from the source into the cache
   * @throws IOException
   */
  public void cache() throws IOException {
    InputStream input = getUncachedInputStream();
    Properties headers = getUncachedProperties();
    if (input==null) {
      logger.error("Received null inputstream for url '"+url+"'.");
      throw new CachingException("Received null inputstream.");
    }
    if (headers==null) {
      logger.error("Received null headers for url '"+url+"'.");
      throw new CachingException("Received null headers.");
    }
    storeContent(input, headers);
  }

  protected abstract void storeContent(InputStream input,
				       Properties props)
      throws IOException;
  protected abstract InputStream getUncachedInputStream() throws IOException;
  protected abstract Properties getUncachedProperties() throws IOException;

  public static class CachingException extends IOException {
    public CachingException(String msg) {
      super(msg);
    }
  }
}
