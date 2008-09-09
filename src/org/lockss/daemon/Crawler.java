/*
 * $Id: Crawler.java,v 1.52.14.1 2008-09-09 08:00:57 tlipkis Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.io.BufferedInputStream;
import java.util.*;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.collections.map.LinkedMap;
import org.lockss.util.*;
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

  public static final int NEW_CONTENT = 0;
  public static final int REPAIR = 1;
  public static final int BACKGROUND = 2;
  public static final int OAI = 3;
  public static final int ARC = 4;

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
  public int getType();

  /**
   * Return true iff the crawl tries to collect the entire AU content.
   */
  public boolean isWholeAU();

  /**
   * aborts the running crawl
   */
  public void abortCrawl();


  /**
   * Set a watchdog that should be poked periodically by the crawl
   * @param wdog the watchdog
   */
  public void setWatchdog(LockssWatchdog wdog);

  /**
   * Returns an int representing the status of this crawler
   */
  public CrawlerStatus getStatus();

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
     * Generate a URL cacher  for the given URL
     * @param url
     * @return UrlCacher for the given URL
     */
    public UrlCacher makeUrlCacher(String url);

    public BufferedInputStream resetInputStream(BufferedInputStream is,
						String url)
	throws IOException;

    public void refetchPermissionPage(String url) throws IOException;

    public CrawlerStatus getCrawlerStatus();

  }
}
