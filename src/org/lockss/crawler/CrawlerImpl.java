/*
 * $Id: CrawlerImpl.java,v 1.2 2004-01-13 02:36:27 troberts Exp $
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

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;

/**
 * The crawler.
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */


public class CrawlerImpl implements Crawler {
  /**
   * TODO
   * 1) write state to harddrive using whatever system we come up for for the
   * rest of LOCKSS
   * 2) check deadline and die if we run too long
   */


  private static Logger logger = Logger.getLogger("CrawlerImpl");

  private ArchivalUnit au;

  private Crawler.Status crawlStatus = null;

  private long startTime = -1;
  private long endTime = -1;
  private int numUrlsFetched = 0;
  private int numUrlsParsed = 0;
  private int type;
  private Collection repairUrls = null;

  private CrawlSpec spec = null;

  private int crawlError = 0;
  private AuState aus = null;

  private Set failedUrls = new HashSet();

  private GoslingHtmlParser htmlParser = null;

  private static final String PARAM_RETRY_TIMES =
    Configuration.PREFIX + "CrawlerImpl.numCacheRetries";
  private static final int DEFAULT_RETRY_TIMES = 3;

  public static final String PARAM_RETRY_PAUSE =
    Configuration.PREFIX + "CrawlerImpl.retryPause";
  public static final long DEFAULT_RETRY_PAUSE = 10*Constants.SECOND;


  /**
   * Construct a new content crawl object; does NOT start the crawl
   * @param au {@link ArchivalUnit} that this crawl will happen on
   * @param spec {@link CrawlSpec} that defines the crawl
   * @param aus {@link AuState} for the AU
   * @return new content crawl object
   */
  public static CrawlerImpl makeNewContentCrawler(ArchivalUnit au,
							 CrawlSpec spec,
							 AuState aus) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    } else if (spec == null) {
      throw new IllegalArgumentException("Called with null spec");
    } else if (aus == null) {
      throw new IllegalArgumentException("Called with null AuState");
    }
    return new CrawlerImpl(au, spec, aus, Crawler.NEW_CONTENT);
  }

  /**
   * Construct a repair crawl object; does NOT start the crawl
   * @param au {@link ArchivalUnit} that this crawl will happen on
   * @param spec {@link CrawlSpec} that defines the crawl
   * @param aus {@link AuState} for the AU
   * @param repairUrls list of URLs to crawl for the repair
   * @return repair crawl object
   */
  public static CrawlerImpl makeRepairCrawler(ArchivalUnit au,
						     CrawlSpec spec,
						     AuState aus,
						     Collection repairUrls) {
     if (au == null) {
       throw new IllegalArgumentException("Called with null AU");
     } else if (spec == null) {
       throw new IllegalArgumentException("Called with null spec");
     } else if (repairUrls == null) {
       throw new IllegalArgumentException("Called with null repair coll");
     } else if (repairUrls.size() == 0) {
       throw new IllegalArgumentException("Called with empty repair list");
     }
     return new CrawlerImpl(au, spec, aus, Crawler.REPAIR, repairUrls);
  }

  private CrawlerImpl(ArchivalUnit au, CrawlSpec spec,
			     AuState aus, int type) {
    this.au = au;
    this.spec = spec;
    this.type = type;
    this.aus = aus;
    crawlStatus = new Crawler.Status(au, spec.getStartingUrls(), type);
    htmlParser = new GoslingHtmlParser(au);
  }

  private CrawlerImpl(ArchivalUnit au, CrawlSpec spec,
			     AuState aus, int type, Collection repairUrls) {
    this(au, spec, aus, type);
    this.repairUrls = repairUrls;

    //XXX hack, since crawlStatus will get set twice
    crawlStatus = new Crawler.Status(au, repairUrls, type);
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public int getType() {
    return type;
  }

  public Crawler.Status getStatus() {
    return crawlStatus;
  }


  public void abortCrawl() {
//     throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Main method of the crawler; it loops through crawling and caching
   * urls.
   *
   * @param deadline when to terminate by
   * @return true if no errors
   */
  public boolean doCrawl(Deadline deadline) {
    try {
      return doCrawl0(deadline);
    } finally {
      crawlStatus.signalCrawlEnded();
    }
  }

  private boolean doCrawl0(Deadline deadline) {
    if (deadline == null) {
      throw new IllegalArgumentException("Called with a null Deadline");
    }
    boolean windowClosed = false;
    logger.info("Beginning crawl of "+au);
    crawlStatus.signalCrawlStarted();
    CachedUrlSet cus = au.getAuCachedUrlSet();
    Set parsedPages = new HashSet();

    Set extractedUrls = null;

    if (!deadline.expired() &&
        (type == Crawler.NEW_CONTENT)  && !crawlPermission(cus)) {
      logger.debug("Crawling AU not permitted - aborting crawl!");
      return false;
    }

    int refetchDepth = spec.getRefetchDepth();
    Iterator it = null;
    if (type == Crawler.NEW_CONTENT) {
      Collection startUrls = spec.getStartingUrls();
      it = startUrls.iterator();
    } else {
      it = repairUrls.iterator();
    }
    for (int ix=0; ix<refetchDepth; ix++) {
      extractedUrls = new HashSet();
      while (it.hasNext() && !deadline.expired()) {
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


    while (!urlsToCrawl.isEmpty() && !deadline.expired() && !windowClosed) {
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

  boolean crawlPermission(CachedUrlSet ownerCus) {
    boolean crawl_ok = false;
    int err = Crawler.STATUS_PUB_PERMISSION;

    // fetch and cache the manifest page
    String manifest = au.getManifestPage();
    Plugin plugin = au.getPlugin();
    UrlCacher uc = plugin.makeUrlCacher(ownerCus, manifest);
    try {
      if (au.shouldBeCached(manifest)) {
        // check for proper crawl window
        if ((au.getCrawlSpec()==null) || (au.getCrawlSpec().canCrawl())) {
          // check for the permission on the page without caching it
          InputStream is = uc.getUncachedInputStream();
          // set the reader to our default encoding
          //XXX try to extract encoding from source
          Reader reader = new InputStreamReader(is, Constants.DEFAULT_ENCODING);
          crawl_ok = au.checkCrawlPermission(reader);
          if (!crawl_ok) {
            logger.error("Couldn't start crawl due to missing permission.");
          }
        } else {
          logger.debug("Couldn't start crawl due to crawl window restrictions.");
	  err = Crawler.STATUS_WINDOW_CLOSED;
        }
      } else {
	logger.debug("Manifest not within CrawlSpec");
      }
    } catch (IOException ex) {
      logger.warning("Exception reading manifest: "+ex);
      crawlStatus.setCrawlError(Crawler.STATUS_FETCH_ERROR);
    }
    if (!crawl_ok) {
      crawlStatus.setCrawlError(Crawler.STATUS_FETCH_ERROR);
    }
    return crawl_ok;
  }


  /**
   * This is the meat of the crawl.  Fetches the specified url and adds
   * any urls it harvests from it to extractedUrls
   * @param url url to fetch
   * @param extractedUrls set to write harvested urls to
   * @param parsedPages set containing all the pages that have already
   * been parsed (to make sure we don't loop)
   * @param cus cached url set that the url belongs to
   * @param overWrite true if overwriting is desired
   * @return true if there were no errors
   */
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
	  cacheWithRetries(uc, type,
			   Configuration.getIntParam(PARAM_RETRY_TIMES,
						     DEFAULT_RETRY_TIMES));
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
      if (type == Crawler.NEW_CONTENT && !parsedPages.contains(uc.getUrl())) {
	logger.debug3("Parsing "+uc);
	CachedUrl cu = uc.getCachedUrl();

	//XXX quick fix; if statement should be removed when we rework
	//handling of error condition
	if (cu.hasContent()) {
	  ContentParser parser = getContentParser(cu);
	  if (parser != null) {
	    //IOException if the CU can't be read
	    parser.parseForUrls(cu, extractedUrls,
				new MyUrlCheckCallback(parsedPages, au));
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

  private ContentParser getContentParser(CachedUrl cu) {
    Properties props = cu.getProperties();
    if (props != null) {
      String contentType = props.getProperty("content-type");
      if (contentType == null) {
	return null;
      } 
      if (contentType.toLowerCase().startsWith("text/html")) {
	return htmlParser;
      }
    }
    return null;
  }

  protected static boolean isSupportedUrlProtocol(String url) {
    try {
//       URL ur = new URL(srcUrl, url);
      URL ur = new URL(url);
      // some 1.4 machines will allow this, so we explictly exclude it for now.
      if (StringUtil.getIndexIgnoringCase(ur.toString(), "https") != 0) {
        return true;
      }
    }
    catch (Exception ex) {
    }
    return false;
  }

  private void cacheWithRetries(UrlCacher uc, int type, int maxRetries)
      throws IOException {
    int numRetries = 0;
    while (true) {
      try {
	if (type == Crawler.NEW_CONTENT) {
	  logger.debug("caching "+uc);
	  uc.cache(); //IOException if there is a caching problem
	  crawlStatus.signalUrlFetched();
	} else {
	  logger.debug("forced caching "+uc);
	  uc.forceCache();
	}
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

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[CrawlerImpl: ");
    sb.append(au.toString());
    sb.append("]");
    return sb.toString();
  }

  private static class MyUrlCheckCallback
    implements ContentParser.UrlCheckCallback {
    Set parsedPages = null;
    ArchivalUnit au = null;

    public MyUrlCheckCallback(Set parsedPages, ArchivalUnit au) {
      this.parsedPages = parsedPages;
      this.au = au;
    }
    
    /**
     * Check that we should cache this url and haven't already parsed it
     */
    public boolean shouldCacheUrl(String url) {
      return (isSupportedUrlProtocol(url)
	      && !parsedPages.contains(url)
	      && au.shouldBeCached(url));
    }
  }
}
