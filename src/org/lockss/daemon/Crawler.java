/*
 * $Id: Crawler.java,v 1.18 2004-01-13 02:36:27 troberts Exp $
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

  public static final int STATUS_INCOMPLETE = 1;
  public static final int STATUS_SUCCESSFUL = 2;
  public static final int STATUS_ERROR = 3;
  public static final int STATUS_WINDOW_CLOSED = 4;
  public static final int STATUS_FETCH_ERROR = 5;
  public static final int STATUS_PUB_PERMISSION = 6;

  /**
   * Initiate a crawl starting with all the urls in urls
   * @return true if the crawl was successful
   * @param deadline maximum time to spend on this crawl
   */
  public boolean doCrawl(Deadline deadline);


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
   * Returns the starting urls for this crawler
   * @return starting urls for this crawler
   */
//   public Collection getStartUrls();

  /**
   * aborts the running crawl
   */
  public void abortCrawl();


  /**
   * Returns an int representing the status of this crawler
   */
  public Crawler.Status getStatus();


  public static class Status {
    protected long startTime = -1;
    protected long endTime = -1;
    protected long numFetched = 0;
    protected long numParsed = 0;
    protected int crawlError = 0;
    protected Collection startUrls = null;
    protected ArchivalUnit au = null;
    protected int type = -1;

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
      return numFetched;
    }

    public void signalUrlFetched() {
      numFetched++;
    }

    /**
     * Return the number of urls that have been parsed by this crawler
     * @return number of urls that have been parsed by this crawler
     */
    public long getNumParsed() {
      return numParsed;
    }

    public void signalUrlParsed() {
      numParsed++;
    }
    
    public Collection getStartUrls() {
      return startUrls;
    }


    public int getCrawlStatus() {
      if (endTime == -1) {
	return Crawler.STATUS_INCOMPLETE;
      } else if (crawlError != 0) {
	return crawlError;
      }
      return Crawler.STATUS_SUCCESSFUL;
    }

    public void setCrawlError(int crawlError) {
      this.crawlError = crawlError;
    }

    public int getCrawlError() {
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
