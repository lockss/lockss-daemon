/*
 * $Id: BaseUrlCacher.java,v 1.10 2003-09-19 22:34:02 eaalto Exp $
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

package org.lockss.plugin.base;

import java.io.*;
import java.net.*;
import java.util.Properties;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.repository.*;

/**
 * Base class for UrlCachers.  Utilizes the LockssRepository for caching, and
 * URL connections for fetching.
 * Plugins may extend this to get some common UrlCacher functionality.
 */
public class BaseUrlCacher implements UrlCacher {
  protected CachedUrlSet cus;
  protected String url;
  private URLConnection conn;
  protected static Logger logger = Logger.getLogger("UrlCacher");
  private LockssRepository repository;

  public BaseUrlCacher(CachedUrlSet owner, String url) {
    this.cus = owner;
    this.url = url;
    ArchivalUnit au = owner.getArchivalUnit();
    repository = au.getPlugin().getDaemon().getLockssRepository(au);
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
    return getArchivalUnit().getPlugin().makeCachedUrl(cus, url);
  }

  public void cache() throws IOException {
    long lastCached = 0;
    Plugin plugin = getArchivalUnit().getPlugin();
    CachedUrl cachedVersion = plugin.makeCachedUrl(cus, url);
    // if it's been cached, get the last caching time and use that
    if ((cachedVersion!=null) && cachedVersion.hasContent()) {
      Properties cachedProps = cachedVersion.getProperties();
      try {
        lastCached = Long.parseLong(cachedProps.getProperty("date"));
      } catch (NumberFormatException nfe) { }
    }
    cache(lastCached);
  }

  public void forceCache() throws IOException {
    // forces the cache
    cache(0);
  }

  private void cache(long lastCached) throws IOException {
    logger.debug3("Pausing before fetching content");
    getArchivalUnit().pauseBeforeFetch();
    logger.debug3("Done pausing");
    InputStream input = getUncachedInputStream(lastCached);
    if (input!=null) {
      // null input indicates unmodified content, so skip caching
      Properties headers = getUncachedProperties();
      if (headers == null) {
        logger.error("Received null headers for url '" + url + "'.");
        throw new CachingException("Received null headers.");
      }
      storeContent(input, headers);
    }
  }

  protected void storeContent(InputStream input, Properties headers)
      throws IOException {
    logger.debug3("Caching url '"+url+"'");
    RepositoryNode leaf = repository.createNewNode(url);
    leaf.makeNewVersion();

    OutputStream os = leaf.getNewOutputStream();
    StreamUtil.copy(input, os);
    os.close();
    input.close();

    leaf.setNewProperties(headers);

    leaf.sealNewVersion();
  }

  /**
   * Gets an InputStream for this URL, using the 'lastCached' time as
   * 'if-modified-since'.  If a 304 is generated (not modified), it returns
   * null.
   * @param lastCached the last cached time
   * @return the InputStream, or null
   * @throws IOException
   */
  protected InputStream getUncachedInputStream(long lastCached)
      throws IOException {
    if (conn==null) {
      URL urlObj = new URL(url);
      conn = urlObj.openConnection();
    }
    conn.setIfModifiedSince(lastCached);
    InputStream input = conn.getInputStream();
    if (conn instanceof HttpURLConnection) {
      // http connection; check response code
      int code = ((HttpURLConnection)conn).getResponseCode();
      if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
        logger.debug2("Unmodified content not cached for url '"+url+"'");
        return null;
      }
    }
    return input;
  }

  protected Properties getUncachedProperties() throws IOException {
    Properties props = new Properties();
    if (conn==null) {
      URL urlO = new URL(url);
      conn = urlO.openConnection();
    }
    // set header properties in which we have interest
    props.setProperty("content-type", conn.getContentType());
    props.setProperty("content-url", url);
    props.setProperty("date", ""+conn.getDate());
    return props;
  }

  public static class CachingException extends IOException {
    public CachingException(String msg) {
      super(msg);
    }
  }
}
