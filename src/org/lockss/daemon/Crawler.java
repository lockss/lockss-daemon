/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
nto use, copy, modify, merge, publish, distribute, sublicense, and/or sell
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

import org.lockss.util.urlconn.CacheException;
import org.lockss.plugin.*;
import org.lockss.crawler.*;

/**
 * This interface is implemented by the generic LOCKSS daemon.
 * The plug-ins use it to call the crawler to actually fetch
 * content.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */
public interface Crawler {

  public enum Type {
    NEW_CONTENT("New Content"),
    REPAIR("Repair"),
    AJAX("AJAX");

    final String printString;
    Type(String printString) {
      this.printString = printString;
    }

    public String toString() {
      return printString;
    }
  }

  public static final int STATUS_UNKNOWN = 0;
  public static final int STATUS_QUEUED = 1;
  public static final int STATUS_ACTIVE = 2;
  public static final int STATUS_SUCCESSFUL = 3;
  public static final int STATUS_ERROR = 4;
  public static final int STATUS_ABORTED = 5;
  public static final int STATUS_WINDOW_CLOSED = 6;
  public static final int STATUS_FETCH_ERROR = 7;
  public static final int STATUS_NO_PUB_PERMISSION = 8;
  public static final int STATUS_PLUGIN_ERROR = 9;
  public static final int STATUS_REPO_ERR = 10;
  public static final int STATUS_RUNNING_AT_CRASH = 11;
  public static final int STATUS_EXTRACTOR_ERROR = 12;
  public static final int STATUS_CRAWL_TEST_SUCCESSFUL = 13;
  public static final int STATUS_CRAWL_TEST_FAIL = 14;
  public static final int STATUS_LAST = 14;

  /**
   * Initiate a crawl starting with all the urls in urls
   * @return true if the crawl was successful
   */
  public boolean doCrawl();

  /**
   * Return the AU that this crawler is crawling within
   * @return the AU that this crawler is crawling within
   */
  public ArchivalUnit getAu();

  /**
   * Returns the type of crawl
   * @return crawl type
   */
  public Type getType();

  /**
   * Return true iff the crawl tries to collect the entire AU content.
   */
  public boolean isWholeAU();

  /**
   * aborts the running crawl
   */
  public void abortCrawl();


  /**
   * Set the CrawlReq that caused this crawl, if any.  Communicates
   * request-specific args such as refetch depth
   */
  public void setCrawlReq(CrawlReq req);

  /**
   * Set a watchdog that should be poked periodically by the crawl
   * @param wdog the watchdog
   */
  public void setWatchdog(LockssWatchdog wdog);

  /**
   * Returns an int representing the status of this crawler
   */
  public CrawlerStatus getCrawlerStatus();

  /** Store the crawl pool key */
  public void setCrawlPool(String key);

  /** Return the previously stored crawl pool key */
  public String getCrawlPool();

  /**
   * Encapsulation for the methods that the PermissionMap needs from a
   * crawler
   *
   * @author troberts
   *
   */
  public static interface PermissionHelper {

    public ArchivalUnit getAu();

    /**
     * Generate a URL cacher for the given URL, suitable for fetching and
     * possibly storing a permission page.  See
     * {@link BaseCrawler#PARAM_STORE_PERMISSION_SCHEME}.
     * @param url
     * @return UrlCacher for the given URL
     */
    public UrlFetcher makePermissionUrlFetcher(String url);

    public void setPreviousContentType(String previousContentType);

    public CrawlerStatus getCrawlerStatus();

  }
  
  public static interface CrawlerFacade extends PermissionHelper {
    
    public void addToFailedUrls(String url);
    
    public void addToFetchQueue(CrawlUrlData curl);
    
    public void addToParseQueue(CrawlUrlData curl);
    
    public void addToPermissionProbeQueue(String probeUrl,
					  String referrerUrl);

    public void setPreviousContentType(String previousContentType);
    
    public CrawlerStatus getCrawlerStatus();
    
    public ArchivalUnit getAu();

    public boolean isAborted();

    /**
     * @since 1.67.5
     */
    public UrlFetcher makeUrlFetcher(String url);

    public UrlFetcher makePermissionUrlFetcher(String url);

    public UrlCacher makeUrlCacher(UrlData ud);
    
    public boolean hasPermission(String url);
    
    public long getRetryDelay(CacheException ce);
    
    public int getRetryCount(CacheException ce);
    
    public int permissonStreamResetMax();
    
    public boolean isGloballyPermittedHost(String host);

    public boolean isAllowedPluginPermittedHost(String host);

    public void updateCdnStems(String url);

    public CrawlUrl addChild(CrawlUrl curl, String url);

    public Object putStateObj(String key, Object val);

    public Object getStateObj(String key);
  }

}
