/*
 * $Id: NewContentCrawler.java,v 1.2 2004-02-09 22:54:45 troberts Exp $
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
import java.io.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.state.*;

public class NewContentCrawler extends CrawlerImpl {

  private static Logger logger = Logger.getLogger("NewContentCrawler");
  private Set failedUrls = new HashSet();

  private static final String PARAM_RETRY_TIMES =
    Configuration.PREFIX + "CrawlerImpl.numCacheRetries";
  private static final int DEFAULT_RETRY_TIMES = 3;

  public static final String PARAM_RETRY_PAUSE =
    Configuration.PREFIX + "CrawlerImpl.retryPause";
  public static final long DEFAULT_RETRY_PAUSE = 10*Constants.SECOND;

  
  public NewContentCrawler(ArchivalUnit au, CrawlSpec spec, AuState aus) {
    super(au, spec, aus);
    crawlStatus = new Crawler.Status(au, spec.getStartingUrls(), getType());
  }

  public int getType() {
    return Crawler.NEW_CONTENT;
  }


  protected boolean doCrawl0(Deadline deadline) {
    boolean windowClosed = false;
    logger.info("Beginning crawl of "+au);
    crawlStatus.signalCrawlStarted();
    CachedUrlSet cus = au.getAuCachedUrlSet();
    Set parsedPages = new HashSet();

    Set extractedUrls = null;

    if (!deadline.expired() && !crawlPermission(cus)) {
      logger.debug("Crawling AU not permitted - aborting crawl!");
      return false;
    }

    int refetchDepth = spec.getRefetchDepth();
    Iterator it = getStartingUrls();
    for (int ix=0; ix<refetchDepth; ix++) {
      extractedUrls = new HashSet();
      while (it.hasNext() && !deadline.expired() && !crawlAborted) {
	String url = (String)it.next();
	//catch and warn if there's a url in the start urls
	//that we shouldn't cache
        // check crawl window during crawl
        if ((spec!=null) && (!spec.canCrawl())) {
          logger.debug("Crawl canceled: outside of crawl window");
          windowClosed = true;
          // break from while loop
          break;
        }
 	if (spec.isIncluded(url)) {
	  if (!doCrawlLoop(url, extractedUrls, parsedPages, cus, true, true)) {
	    if (crawlStatus.getCrawlError() == 0) {
	      crawlStatus.setCrawlError(Crawler.STATUS_ERROR);
	    }
	  }
	} else {
	  logger.warning("Called with a starting url we aren't suppose to "+
			 "cache: "+url);
	}
      }
      if (windowClosed) {
        // break from for loop
        break;
      }
      it = extractedUrls.iterator();
    }

    //we don't alter the crawl list from AuState until we've enumerated the
    //urls that need to be recrawled.

    Collection urlsToCrawl = aus.getCrawlUrls();
    while (!extractedUrls.isEmpty()) {
      String url = (String)extractedUrls.iterator().next();
      extractedUrls.remove(url);
      urlsToCrawl.add(url);
    }


    while (!urlsToCrawl.isEmpty() && !deadline.expired()
	   && !windowClosed && !crawlAborted) {
      String nextUrl = (String)CollectionUtil.removeElement(urlsToCrawl);
      // check crawl window during crawl
      if ((spec!=null) && (!spec.canCrawl())) {
        logger.debug("Crawl canceled: outside of crawl window");
        windowClosed = true;
        break;
      }
      if (!doCrawlLoop(nextUrl, urlsToCrawl, parsedPages, cus, false, false)) {
	if (crawlStatus.getCrawlError() == 0) {
	  crawlStatus.setCrawlError(Crawler.STATUS_ERROR);
	}
      }
      aus.updatedCrawlUrls(false);
    }
    // unsuccessful crawl if window closed
    if (windowClosed) {
      crawlStatus.setCrawlError(Crawler.STATUS_WINDOW_CLOSED);
    }
    if (crawlStatus.getCrawlError() != 0) {
      logger.info("Finished crawl (errors) of "+au);
    } else {
      logger.info("Finished crawl of "+au);
    }

    if (au instanceof BaseArchivalUnit) {
      BaseArchivalUnit bau = (BaseArchivalUnit)au;
      long cacheHits = bau.getCrawlSpecCacheHits();
      long cacheMisses = bau.getCrawlSpecCacheMisses();
      double per = ((float)cacheHits /
		    ((float)cacheHits + (float)cacheMisses));
      logger.info("Had "+cacheHits+" cache hits, with a percentage of "+per);
    }

    return (crawlStatus.getCrawlError() == 0);
  }



  protected Iterator getStartingUrls() {
    Collection startUrls = spec.getStartingUrls();
    return startUrls.iterator();
  }

  protected boolean doCrawlLoop(String url, Collection extractedUrls,
				Set parsedPages, CachedUrlSet cus,
				boolean overWrite, boolean reparse) {

    int error = 0;
    logger.debug2("Dequeued url from list: "+url);
    Plugin plugin = au.getPlugin();
    UrlCacher uc = plugin.makeUrlCacher(cus, url);

    // don't cache if already cached, unless overwriting
    if (overWrite || !uc.getCachedUrl().hasContent()) {
      try {
	if (failedUrls.contains(uc.getUrl())) {
	  //skip if it's already failed
	  logger.debug3("Already failed to cache "+uc+". Not retrying.");
	} else {
	  cacheWithRetries(uc, Configuration.getIntParam(PARAM_RETRY_TIMES,
							 DEFAULT_RETRY_TIMES),
			   overWrite);
	  numUrlsFetched++;
	}
      } catch (FileNotFoundException e) {
	logger.warning(uc+" not found on publisher's site");
      } catch (IOException ioe) {
	//XXX handle this better.  Requeue?
	logger.error("Problem caching "+uc+". Ignoring", ioe);
	error = Crawler.STATUS_FETCH_ERROR;
      }
    }
    else {
      if (!parsedPages.contains(uc.getUrl())) {
	logger.debug2(uc+" exists, not caching");
      }
      if (!reparse) {
	logger.debug2(uc+" exists, not reparsing");
	return true;
      }
    }
    try {
      if (!parsedPages.contains(uc.getUrl())) {
	logger.debug3("Parsing "+uc);
	CachedUrl cu = uc.getCachedUrl();

	//XXX quick fix; if statement should be removed when we rework
	//handling of error condition
	if (cu.hasContent()) {
	  ContentParser parser = getContentParser(cu);
	  if (parser != null) {
	    //IOException if the CU can't be read
	    parser.parseForUrls(cu, new MyFoundUrlCallback(parsedPages,
							   extractedUrls, au));
	  }

 	  crawlStatus.signalUrlParsed();
	  parsedPages.add(uc.getUrl());
	}
      }
    } catch (IOException ioe) {
      //XXX handle this better.  Requeue?
      logger.error("Problem parsing "+uc+". Ignoring", ioe);
      error = Crawler.STATUS_FETCH_ERROR;
    }
    logger.debug2("Removing from list: "+uc.getUrl());
    return (error == 0);
  }

  private void cacheWithRetries(UrlCacher uc, int maxRetries,
				boolean shouldForceCache)
      throws IOException {
    int numRetries = 0;
    while (true) {
      try {
	logger.debug("caching "+uc);
 	if (shouldForceCache) {
 	  uc.forceCache(); //IOException if there is a caching problem
 	} else {
	  uc.cache(); //IOException if there is a caching problem
 	}
	crawlStatus.signalUrlFetched();
	return; //cache didn't throw
      } catch (IOException e) {
	logger.debug("Exception when trying to cache "+uc+", retrying", e);
	Deadline pause =
	  Deadline.in(Configuration.getTimeIntervalParam(PARAM_RETRY_PAUSE,
							 DEFAULT_RETRY_PAUSE));
	while (!pause.expired()) {
	  logger.debug3("Sleeping for "+pause);
	  try {
	    pause.sleep();
	  } catch (InterruptedException ie) {
	    // no action
	  }
	}
	numRetries++;
	if (numRetries >= maxRetries) {
	  logger.warning("Failed to cache "+numRetries +" times.  Skipping "
			 + uc);
	  failedUrls.add(uc.getUrl());
	  throw e;
	}
	Plugin plugin = uc.getCachedUrlSet().getArchivalUnit().getPlugin();
	uc = plugin.makeUrlCacher(uc.getCachedUrlSet(), uc.getUrl());
      }
    }
  }

  private ContentParser getContentParser(CachedUrl cu) {
    Properties props = cu.getProperties();
    ArchivalUnit au = cu.getArchivalUnit();
    if (props != null) {
      String contentType = props.getProperty("content-type");
      return au.getContentParser(contentType);
    }
    return null;
  }

  private static class MyFoundUrlCallback
    implements ContentParser.FoundUrlCallback {
    Set parsedPages = null;
    Collection extractedUrls = null;
    ArchivalUnit au = null;

    public MyFoundUrlCallback(Set parsedPages, Collection extractedUrls,
			      ArchivalUnit au) {
      this.parsedPages = parsedPages;
      this.extractedUrls = extractedUrls;
      this.au = au;
    }

    /**
     * Check that we should cache this url and haven't already parsed it
     * @param url the url string
     */
    public void foundUrl(String url) {
      if (isSupportedUrlProtocol(url) && !parsedPages.contains(url)
	  && !extractedUrls.contains(url) && au.shouldBeCached(url)) {
	extractedUrls.add(url);
      }
    }
  }
}
