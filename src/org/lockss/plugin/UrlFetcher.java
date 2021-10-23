/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

import org.lockss.config.Configuration;
import org.lockss.crawler.CrawlRateLimiter;
import org.lockss.crawler.CrawlUrl;
import org.lockss.daemon.LockssWatchdog;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.IPAddr;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.LockssUrlConnectionPool;

public interface UrlFetcher {
  
  /** Don't follow redirects; throw CacheException.RetryNewUrlException if
   * redirect response received */
  public static final RedirectScheme REDIRECT_SCHEME_DONT_FOLLOW =
    new RedirectScheme(0);
  /** Automatically follow all redirects */
  public static final RedirectScheme REDIRECT_SCHEME_FOLLOW =
    new RedirectScheme(RedirectScheme.REDIRECT_OPTION_FOLLOW_AUTO);
  /** Follow redirects only in crawl spec */
  public static final RedirectScheme REDIRECT_SCHEME_FOLLOW_IN_SPEC =
    new RedirectScheme(RedirectScheme.REDIRECT_OPTION_IF_CRAWL_SPEC);
  /** Follow redirects only on same host */
  public static final RedirectScheme REDIRECT_SCHEME_FOLLOW_ON_HOST =
    new RedirectScheme(RedirectScheme.REDIRECT_OPTION_ON_HOST_ONLY);
  /** Follow redirects only in crawl spec and on host */
  public static final RedirectScheme REDIRECT_SCHEME_FOLLOW_IN_SPEC_ON_HOST =
    new RedirectScheme(RedirectScheme.REDIRECT_OPTION_IF_CRAWL_SPEC +
        RedirectScheme.REDIRECT_OPTION_ON_HOST_ONLY);
  /** Follow redirects if within the crawl spec, store under all names */
  public static final RedirectScheme REDIRECT_SCHEME_STORE_ALL_IN_SPEC =
    new RedirectScheme(RedirectScheme.REDIRECT_OPTION_IF_CRAWL_SPEC +
        RedirectScheme.REDIRECT_OPTION_STORE_ALL);
	
  public enum FetchResult {
    FETCHED, FETCHED_NOT_MODIFIED, NOT_FETCHED
  }
  
  public FetchResult fetch() throws CacheException;
  
  /**
   * Return the ArchivalUnit to which this UrlCacher belongs.
   * @return the ArchivalUnit
   */
  public ArchivalUnit getArchivalUnit();

  /**
   * Return the url being represented
   * @return the {@link String} url being represented.
   */
  public String getUrl();
  
  public InputStream resetInputStream(InputStream input, String lastModified) throws IOException;
  
  public void setUrlConsumerFactory(UrlConsumerFactory consumerFactory);
  
  /** Set the shared connection pool object to be used by this UrlCacher */
  public void setConnectionPool(LockssUrlConnectionPool connectionPool);
  
  /** For multihomed machines, determines which local address will be the
   * sorce of outgoing URL connections */
  public void setLocalAddress(IPAddr localAddr);

  /** Set the host and port the UrlCacher should proxy through */
  public void setProxy(String proxyHost, int proxyPort);
  
  /**
   * Provides the crawl url for UrlFetchers created within the crawl 
   * @param curl
   */
  public void setCrawlUrl(CrawlUrl curl);
  
  
  /**
   * Sets various attributes of the fetch operation
   * Currently these are:
   * refetch - refetch the content even if it's already present and up to date
   * don't close input stream - needed for archives
   * @param fetchFlags BitSet encapsulating the fetch flags
   */
  public void setFetchFlags(BitSet fetchFlags);

  /**
   * Gets the fetch flags
   */
  public BitSet getFetchFlags();
  
  /** Set a request header, overwriting any previous value */
  public void setRequestProperty(String key, String value);

  /** Determines the behavior if a redirect response is received. */
  public void setRedirectScheme(RedirectScheme scheme);

  /** Set a CrawlRateLimiter, which should be consulted before every fetch
   * request */
  public void setCrawlRateLimiter(CrawlRateLimiter crl);

  /** Set the content type just fetched, for MIME-type dependent rate
   * limiters */
  public void setPreviousContentType(String prevContentType);
  
  /**
   * Gets an InputStream for this URL, issuing a request if not already
   * done.  This may only be called once!
   * @return the InputStream
   * @throws IOException
   */
  public InputStream getUncachedInputStream() throws IOException;

  /**
   * Gets the header properties in the server response.  Must be called
   * only after getUncachedInputStream() has succeeded.
   * @return the {@link CIProperties}
   * @throws UnsupportedOperationException if called before
   * getUncachedInputStream() or cache()
   */
  public CIProperties getUncachedProperties();
  
  /**
   * Reset the UrlCacher to its pre-opened state, so that it can be
   * reopened.
   */
  public void reset();
  
  public void setWatchdog(LockssWatchdog wdog);
  
  public LockssWatchdog getWatchdog();
  
  public static class RedirectScheme {
    /** Follow redirects */
    public static final int REDIRECT_OPTION_FOLLOW_AUTO = 1;
    /** Follow redirects only if match crawl spec */
    public static final int REDIRECT_OPTION_IF_CRAWL_SPEC = 2;
    /** Store content under all redirected names */
    public static final int REDIRECT_OPTION_STORE_ALL = 4;
    /** Follow redirects only within same host */
    public static final int REDIRECT_OPTION_ON_HOST_ONLY = 8;
    
    private int options = 0;
    private RedirectScheme(int options) {
      this.options = options;
    }
    public int getOptions() {
      return options;
    }
    
    /** Return true if there are options in common between the argument and
     * redirectOptions */
    public boolean isRedirectOption(int option) {
      return (options & option) != 0;
    }

    public String toString() {
      return Integer.toString(options);
    }
  }

}
