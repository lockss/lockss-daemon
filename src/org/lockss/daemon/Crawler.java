/*
 * $Id: Crawler.java,v 1.26 2005-01-14 01:37:39 troberts Exp $
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

import java.io.IOException;
import java.util.*;
import org.lockss.util.*;
import org.lockss.state.*;
import org.lockss.plugin.*;

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

  public static final String STATUS_INCOMPLETE = "Active";
  public static final String STATUS_SUCCESSFUL = "Successful";
  public static final String STATUS_ERROR = "Error";
  public static final String STATUS_WINDOW_CLOSED = "Crawl window closed";
  public static final String STATUS_FETCH_ERROR = "Fetch error";
  public static final String STATUS_PUB_PERMISSION = "No permission from publisher";
  //public static final String STATUS_UNKNOWN = "Unknown";

  /**
   * Initiate a crawl starting with all the urls in urls
   * @return true if the crawl was successful
   * @param deadline maximum time to spend on this crawl
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
  public Crawler.Status getStatus();


  public static class Status {
    protected long startTime = -1;
    protected long endTime = -1;
    protected String crawlError = null;
    protected Collection startUrls = null;
    protected ArchivalUnit au = null;
    protected int type = -1;

    protected Map urlsWithErrors = new HashMap();
    protected Set urlsFetched = new HashSet();
    protected Set urlsNotModified = new HashSet();
    protected Set urlsParsed = new HashSet();

    public Status(ArchivalUnit au, Collection startUrls, int type) {
      this.au = au;
      this.startUrls = startUrls;
      this.type = type;
    }

    /**
     * Return the time at which this crawl began
     * @return time at which this crawl began or -1 if it hadn't yet
     */
    public long getStartTime() {
      return startTime;
    }

    public void signalCrawlStarted() {
      startTime = TimeBase.nowMs();
    }

    /**
     * Return the time at which this crawl ended
     * @return time at which this crawl ended or -1 if it hadn't yet
     */
    public long getEndTime() {
      return endTime;
    }

    public void signalCrawlEnded() {
      endTime = TimeBase.nowMs();
    }

    /**
     * Return the number of urls that have been fetched by this crawler
     * @return number of urls that have been fetched by this crawler
     */
    public long getNumFetched() {
      return urlsFetched.size();
    }

    /**
     * Return the number of urls whose GETs returned 304 not modified
     * @return number of urls whose contents were not modified
     */
    public long getNumNotModified() {
      return urlsNotModified.size();
    }

    public void signalUrlFetched(String url) {
      urlsFetched.add(url);
    }

    public void signalUrlNotModified(String url) {
      urlsNotModified.add(url);
    }

    /**
     * @return hash of the urls that couldn't be fetched due to errors and the
     * error they got
     */
    public Map getUrlsWithErrors() {
      return urlsWithErrors;
    }

    public long getNumUrlsWithErrors() {
      return urlsWithErrors.size();
    }

    public Set getUrlsFetched() {
      return urlsFetched;
    }

    public Set getUrlsNotModified() {
      return urlsNotModified;
    }

    public Set getUrlsParsed() {
      return urlsParsed;
    }

    /**
     * Return the number of urls that have been parsed by this crawler
     * @return number of urls that have been parsed by this crawler
     */
    public long getNumParsed() {
      return urlsParsed.size();
    }

     public void signalUrlParsed(String url) {
       urlsParsed.add(url);
     }
    
    public Collection getStartUrls() {
      return startUrls;
    }

    public String getCrawlStatus() {
      if (endTime == -1) {
	return Crawler.STATUS_INCOMPLETE;
      } else if (crawlError != null) {
	return crawlError;
      }
      return Crawler.STATUS_SUCCESSFUL;
    }

    public void setCrawlError(String crawlError) {
      this.crawlError = crawlError;
    }

    public void signalErrorForUrl(String url, String error) {
      urlsWithErrors.put(url, error);
    }

    public String getCrawlError() {
      return crawlError;
    }

    public int getType() {
      return type;
    }

    public ArchivalUnit getAu() {
      return au;
    }
  }
}
