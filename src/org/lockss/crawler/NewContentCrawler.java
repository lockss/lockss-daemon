/*
 * $Id: NewContentCrawler.java,v 1.54 2006-11-14 22:15:48 tlipkis Exp $
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

package org.lockss.crawler;

import java.util.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.*;

public class NewContentCrawler extends FollowLinkCrawler {

  private static Logger logger = Logger.getLogger("NewContentCrawler");

  private SpiderCrawlSpec spec;

  public NewContentCrawler(ArchivalUnit au, CrawlSpec crawlSpec, AuState aus) {
    super(au, crawlSpec, aus);
    spec = (SpiderCrawlSpec) crawlSpec;
    crawlStatus = new CrawlerStatus(au, spec.getStartingUrls(),
				    getTypeString());
  }

  public int getType() {
    return Crawler.NEW_CONTENT;
  }

  public String getTypeString() {
    return "New Content";
  }

  public boolean isWholeAU() {
    return true;
  }

  /**
   * Keeps crawling from the baseUrl til it hits the refetchDepth
   * to extract url for newly added pages since last crawl.
   *
   * @return a set of urls that contains updated content.
   */
  protected Set getUrlsToFollow(){
    Set extractedUrls = newSet();
    int refetchDepth0 = spec.getRefetchDepth();
    String key = StringUtil.replaceString(PARAM_REFETCH_DEPTH,
					  "<auid>", au.getAuId());
    int refetchDepth = CurrentConfig.getIntParam(key, refetchDepth0);
    if (refetchDepth != refetchDepth0) {
      logger.info("Crawl spec refetch depth (" + refetchDepth0 +
		  ") overridden by parameter (" + refetchDepth + ")");
    }

    //maxDepth should be greater than refetchDepth
    if (refetchDepth > maxDepth){ //it should not happen
      logger.error("Max. depth is set smaller than refetchDepth." +
		   " Abort Crawl of " + au);
      crawlStatus.setCrawlError("Max. Crawl depth too small");
      abortCrawl();
      //return null;
    }

    Collection startUrls = spec.getStartingUrls();
    for (Iterator iter = SetUtil.theSet(startUrls).iterator();
	 iter.hasNext(); ) {
      crawlStatus.addPendingUrl((String)iter.next());
    }
    cachingStartUrls = true; //added to report error when fail to fetch startUrl
    logger.debug3("refetchDepth: "+refetchDepth);

    // Important to put list we're iterating over in urlsToCrawl, so
    // FollowLinkCrawler.foundUrl() can see them
    urlsToCrawl = SetUtil.theSet(startUrls);
    for (int ix = 0; ix < refetchDepth && !urlsToCrawl.isEmpty(); ix++) {
      logger.debug3("Refetching " + urlsToCrawl.size() + " URLs at level "+ix);

      extractedUrls = newSet();

      for (Iterator it = urlsToCrawl.iterator();
	   it.hasNext() && !crawlAborted; ) {
	String url = (String)it.next();
	//XXX should we add code here to check if the URL is in the protocol
	//we are supporting right now ? or the check is done in plugin already ?
	//same apply to check if the url is Malformed

	logger.debug2("Trying to process " +url);

        // check crawl window during crawl
	if (!withinCrawlWindow()) {
	  crawlStatus.setCrawlError(Crawler.STATUS_WINDOW_CLOSED);
	  abortCrawl();
	  //return null;
	}

        crawlStatus.removePendingUrl(url);
	if (parsedPages.contains(url)) {
	  continue;
	}

	//catch and warn if there's a url in the start urls
	//that we shouldn't cache
 	if (spec.isIncluded(url)) {
	  if (!fetchAndParse(url, extractedUrls, parsedPages, true, true)) {
	    if (crawlStatus.getCrawlError() == null) {
	      crawlStatus.setCrawlError(Crawler.STATUS_ERROR);
	    }
	  }
	} else if (ix == 0) {
	  logger.warning("Starting url not in crawl spec: " + url);
	  crawlStatus.setCrawlError(Crawler.STATUS_PLUGIN_ERROR);
	}
      } // end while loop
      lvlCnt++;
      urlsToCrawl = extractedUrls;
    } // end for loop
    cachingStartUrls = false;

    return extractedUrls;
  } // end of getUrlsToFollow()

  protected boolean shouldFollowLink(){
    return true;
  }

}
