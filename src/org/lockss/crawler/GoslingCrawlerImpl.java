/*
 * $Id: GoslingCrawlerImpl.java,v 1.45.2.1 2003-11-19 06:21:51 tlipkis Exp $
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

/*
 * Some portions of this code are:
 * Copyright (c) 2000-2003 Sun Microsystems. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduct the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems or the names of contributors may
 * be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 *   This software is provided "AS IS," without a warranty of any
 *   kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 *   WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 *   EXCLUDED. SUN MICROSYSTEMS AND ITS LICENSORS SHALL NOT BE LIABLE
 *   FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 *   MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO
 *   EVENT WILL SUN MICROSYSTEMS OR ITS LICENSORS BE LIABLE FOR ANY
 *   LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 *   CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 *   REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 *   OR INABILITY TO USE SOFTWARE, EVEN IF SUN MICROSYSTEMS
 *   HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed, licensed or intended for
 * use in the design, construction, operation or maintenance of any
 * nuclear facility.
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

/**
 * The crawler.
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */


public class GoslingCrawlerImpl implements Crawler {
  /**
   * TODO
   * 1) write state to harddrive using whatever system we come up for for the
   * rest of LOCKSS
   * 2) check deadline and die if we run too long
   */


  private static final String IMGTAG = "img";
  private static final String ATAG = "a";
  private static final String FRAMETAG = "frame";
  private static final String LINKTAG = "link";
  private static final String SCRIPTTAG = "script";
  private static final String SCRIPTTAGEND = "/script";
  private static final String BODYTAG = "body";
  private static final String TABLETAG = "table";
  private static final String TDTAG = "tc";

  private static final String ASRC = "href";
  private static final String SRC = "src";
  private static final String BACKGROUNDSRC = "background";

  private static Logger logger = Logger.getLogger("GoslingCrawlerImpl");

  private ArchivalUnit au;

  private long startTime = -1;
  private long endTime = -1;
  private int numUrlsFetched = 0;
  private int numUrlsParsed = 0;
  private int type;
  private Collection repairUrls = null;

  private CrawlSpec spec = null;
  
  private int crawlError = 0;
  private AuState aus = null;

  private static final String PARAM_RETRY_TIMES =
    Configuration.PREFIX + "GoslingCrawlerImpl.numCacheRetries";
  private static final int DEFAULT_RETRY_TIMES = 3;

  public static final String PARAM_RETRY_PAUSE =
    Configuration.PREFIX + "GoslingCrawlerImpl.retryPause";
  public static final long DEFAULT_RETRY_PAUSE = 10*Constants.SECOND; 


  /**
   * Construct a new content crawl object; does NOT start the crawl
   * @param au {@link ArchivalUnit} that this crawl will happen on
   * @param spec {@link CrawlSpec} that defines the crawl
   * @return new content crawl object
   */
  public static GoslingCrawlerImpl makeNewContentCrawler(ArchivalUnit au,
							 CrawlSpec spec,
							 AuState aus) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null AU");
    } else if (spec == null) {
      throw new IllegalArgumentException("Called with null spec");
    } else if (aus == null) {
      throw new IllegalArgumentException("Called with null AuState");
    }
    return new GoslingCrawlerImpl(au, spec, aus, Crawler.NEW_CONTENT);
  }

  /**
   * Construct a repair crawl object; does NOT start the crawl
   * @param au {@link ArchivalUnit} that this crawl will happen on
   * @param spec {@link CrawlSpec} that defines the crawl
   * @param repairUrls list of URLs to crawl for the repair
   * @return repair crawl object
   */
  public static GoslingCrawlerImpl makeRepairCrawler(ArchivalUnit au,
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
     return new GoslingCrawlerImpl(au, spec, aus, Crawler.REPAIR, repairUrls);
  }

  private GoslingCrawlerImpl(ArchivalUnit au, CrawlSpec spec,
			     AuState aus, int type) {
    this.au = au;
    this.spec = spec;
    this.type = type;
    this.aus = aus;
  }

  private GoslingCrawlerImpl(ArchivalUnit au, CrawlSpec spec,
			     AuState aus, int type, Collection repairUrls) {
    this(au, spec, aus, type);
    this.repairUrls = repairUrls;
  }

  public long getNumFetched() {
    return numUrlsFetched;
  }

  public long getNumParsed() {
    return numUrlsParsed;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public Collection getStartUrls() {
    return spec.getStartingUrls();
  }

  public int getType() {
    return type;
  }

  public int getStatus() {
    if (endTime == -1) {
      return Crawler.STATUS_INCOMPLETE;
    } else if (crawlError != 0) {
      return crawlError;
    }
    return Crawler.STATUS_SUCCESSFUL;
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
      endTime = TimeBase.nowMs();
    }
  }

  private boolean doCrawl0(Deadline deadline) {
    if (deadline == null) {
      throw new IllegalArgumentException("Called with a null Deadline");
    }
    boolean windowClosed = false;
    logger.info("Beginning crawl of "+au);
    startTime = TimeBase.nowMs();
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
	  if (!doCrawlLoop(url, extractedUrls, parsedPages, cus, true)) {
	    if (crawlError == 0) {
	      crawlError = Crawler.STATUS_ERROR;
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
      if (!doCrawlLoop(nextUrl, urlsToCrawl, parsedPages, cus, false)) {
	if (crawlError == 0) {
	  crawlError = Crawler.STATUS_ERROR;
	}
      }
      aus.updatedCrawlUrls(false);
    }
    // unsuccessful crawl if window closed
    if (windowClosed) {
      crawlError = Crawler.STATUS_WINDOW_CLOSED;
    }
    if (crawlError != 0) {
      logger.info("Finished crawl (errors) of "+au);
    } else {
      logger.info("Finished crawl of "+au);
    }      
    return (crawlError == 0);
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
          uc.cache();
          // check for the permission on the page
          CachedUrl cu = plugin.makeCachedUrl(ownerCus, manifest);
          InputStream is = cu.openForReading();
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
      crawlError = Crawler.STATUS_FETCH_ERROR;
    }
    if (!crawl_ok) {
      crawlError = err;
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
			     boolean overWrite) {
    int error = 0;
    logger.debug2("Dequeued url from list: "+url);
    Plugin plugin = au.getPlugin();
    UrlCacher uc = plugin.makeUrlCacher(cus, url);

    // don't cache if already cached, unless overwriting
    if (overWrite || !uc.getCachedUrl().hasContent()) {
      try {
	cacheWithRetries(uc, type,
			 Configuration.getIntParam(PARAM_RETRY_TIMES,
						   DEFAULT_RETRY_TIMES));
	numUrlsFetched++;
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
    }
    try {
      if (type == Crawler.NEW_CONTENT && !parsedPages.contains(uc.getUrl())) {
	logger.debug3("Parsing "+uc);
	CachedUrl cu = uc.getCachedUrl();

	//XXX quick fix; if statement should be removed when we rework
	//handling of error condition
	if (cu.hasContent()) {
	  addUrlsToSet(cu, extractedUrls, parsedPages);//IOException if the CU can't be read
	  parsedPages.add(uc.getUrl());
	  numUrlsParsed++;
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

  private void cacheWithRetries(UrlCacher uc, int type, int maxRetries)
   throws IOException {
    int numRetries = 0;
    while (true) {
      try {
	if (type == Crawler.NEW_CONTENT) {
	  logger.debug("caching "+uc);
	  uc.cache(); //IOException if there is a caching problem
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
	  logger.warning(uc+" threw "+numRetries
			 +" when trying to cache.  Skipping");
	  throw e;
	}
	Plugin plugin = uc.getCachedUrlSet().getArchivalUnit().getPlugin();
	uc = plugin.makeUrlCacher(uc.getCachedUrlSet(), uc.getUrl());
      }
    }
  }


  /**
   * Method which will parse the html file represented by cu and add all
   * urls in it which should be cached to set
   *
   * @param cu object representing a html file in the local file system
   * @param set set to which all the urs in cu should be added
   * @param urlsToIgnore urls which should not be added to set
   * @throws IOException
   */
  protected void addUrlsToSet(CachedUrl cu, Collection set, Set urlsToIgnore)
      throws IOException {
    if (shouldExtractLinksFromCachedUrl(cu)) {
      String cuStr = cu.getUrl();
      if (cuStr == null) {
	logger.error("CachedUrl has null getUrl() value: "+cu);
	return;
      }

      InputStream is = cu.openForReading();
      // set the reader to our default encoding
      //XXX try to extract encoding from source
      Reader reader = new InputStreamReader(is, Constants.DEFAULT_ENCODING); //should do this elsewhere
      URL srcUrl = new URL(cuStr);
      logger.debug2("Extracting urls from srcUrl");
      String nextUrl = null;
      while ((nextUrl = extractNextLink(reader, srcUrl)) != null) {
	logger.debug2("Extracted "+nextUrl);

	//should check if this is something we should cache first
 	if (!set.contains(nextUrl)
            && isSupportedUrlProtocol(srcUrl,nextUrl)
	    && !urlsToIgnore.contains(nextUrl)
	    && au.shouldBeCached(nextUrl)) {
	  set.add(nextUrl);
	}
      }
    }
  }

  /**
   * Determine if this is a CachedUrl that we can parse for new urls; currently
   * this is just done by verifying that the "content-type" property exists
   * and equals "text/html"
   *
   * @param cu CachedUrl representing the web page we may parse
   * @return true if cu has "content-type" set to "text/html", false otherwise
   */
  protected static boolean shouldExtractLinksFromCachedUrl(CachedUrl cu) {
    boolean returnVal = false;
    Properties props = cu.getProperties();
    if (props != null) {
      String contentType = props.getProperty("content-type");
      if (contentType != null) {
	//XXX check if the string starts with this
	returnVal = contentType.toLowerCase().startsWith("text/html");
      }
    }
    if (returnVal) {
      logger.debug2("I should try to extract links from "+cu);
    } else {
      logger.debug2("I shouldn't try to extract links from "+cu);
    }

    return returnVal;
  }

  protected static boolean isSupportedUrlProtocol(URL srcUrl, String url) {
    try {
      URL ur = new URL(srcUrl, url);
      // some 1.4 machines will allow this, so we explictly exclude it for now.
      if (StringUtil.getIndexIgnoringCase(ur.toString(), "https") != 0) {
        return true;
      }
    }
    catch (Exception ex) {
    }
    return false;
  }

  /**
   * Read through the reader stream, extract and return the next url found
   *
   * @param reader Reader object to extract the link from
   * @param srcUrl URL object representing the page we are looking at
   * (for resolving relative links)
   * @return String representing the next url in reader
   * @throws IOException
   * @throws MalformedURLException
   */
  protected static String extractNextLink(Reader reader, URL srcUrl)
      throws IOException, MalformedURLException {
    if (reader == null) {
      return null;
    }
    boolean inscript = false; //FIXME or I will break when we look at scripts
    String nextLink = null;
    int c = 0;
    StringBuffer lineBuf = new StringBuffer();

    while(nextLink == null && c >=0) {
      //skip to the next tag
      do {
	c = reader.read();
      } while (c >= 0 && c != '<');

      if (c == '<') {
	int pos = 0;
	c = reader.read();
	while (c >= 0 && c != '>') {
	  if(pos==2 && c=='-' && lineBuf.charAt(0)=='!'
	     && lineBuf.charAt(1)=='-') {
	    // we're in a HTML comment
	    pos = 0;
	    int lc1 = 0;
	    int lc2 = 0;
	    while((c = reader.read()) >= 0
		  && (c != '>' || lc1 != '-' || lc2 != '-')) {
	      lc1 = lc2;
	      lc2 = c;
	    }
	    break;
	  }
	  lineBuf.append((char)c);
	  pos++;
	  c = reader.read();
	}
	if (inscript) {
	  //FIXME when you deal with the script problems
	  //	  if(lookingAt(lineBuf, 0, pos, scripttagend)) {
	  inscript = false;
	  //}
	} else if (lineBuf.length() >= 5) { //see if the lineBuf has a link tag
	  nextLink = parseLink(lineBuf, srcUrl);
	}
	lineBuf = new StringBuffer();
      }
    }
    return nextLink;
  }


  private static boolean beginsWithTag(String s1, String tag) {
    if (StringUtil.getIndexIgnoringCase(s1, tag) == 0) {
      int len = tag.length();
      if (s1.length() > len && Character.isWhitespace(s1.charAt(len))) {
        return true;
      }
    }
    return false;
  }


  /**
   * Method to take a link tag, and parse out the URL it points to, returning
   * a string representation of the url (lifted and rewritten from the Gosling
   * crawler)
   *
   * @param link StringBuffer containing the text of a link tag (everything
   * between < and > (ie, "a href=http://www.test.org")
   * @param srcUrl URL object representing the page on which this
   * url was taken from (for resolving relative tags)
   * @return string representation of the url from the link tag
   * @throws MalformedURLException
   */
  protected static String parseLink(StringBuffer link, URL srcUrl)
      throws MalformedURLException {
    String returnStr = null;

    switch (link.charAt(0)) {
      case 'a': //<a href=http://www.yahoo.com>
      case 'A':
        if (beginsWithTag(link.toString(),ATAG)) {
          returnStr = getAttributeValue(ASRC, link.toString());
        }
        break;
      case 'f': //<frame src=frame1.html>
      case 'F':
        if (beginsWithTag(link.toString(),FRAMETAG)) {
          returnStr = getAttributeValue(SRC, link.toString());
        }
        break;
      case 'i': //<img src=image.gif>
      case 'I':
        if (beginsWithTag(link.toString(),IMGTAG)) {
          returnStr = getAttributeValue(SRC, link.toString());
        }
        break;
      case 'l': //<link href=blah.css>
      case 'L':
        if (beginsWithTag(link.toString(),LINKTAG)) {
          returnStr = getAttributeValue(ASRC, link.toString());
        }
        break;
      case 'b': //<body backgroung=background.gif>
      case 'B':
        if (beginsWithTag(link.toString(),BODYTAG)) {
          returnStr = getAttributeValue(BACKGROUNDSRC, link.toString());
        }
        break;
      case 's': //<script src=blah.js>
      case 'S':
        if (beginsWithTag(link.toString(),SCRIPTTAG)) {
          returnStr = getAttributeValue(SRC, link.toString());
        }
        break;
      case 't': //<tc background=back.gif> or <table background=back.gif>
      case 'T':
        if (beginsWithTag(link.toString(),TABLETAG) ||
          beginsWithTag(link.toString(),TDTAG)) {
          returnStr = getAttributeValue(BACKGROUNDSRC, link.toString());
        }
        break;
      default:
        return null;
    }
    if (returnStr != null) {
      returnStr = StringUtil.trimAfterChars(returnStr, " #\"");
      if (!isSupportedUrlProtocol(srcUrl, returnStr)) {
        logger.debug("skipping unsupported url " + returnStr);
      }
      else {
        logger.debug2("Generating url from: " + srcUrl + " and " + returnStr);
        URL retUrl = new URL(srcUrl, returnStr);
        returnStr = retUrl.toString();
      }
      logger.debug2("Parsed: " + returnStr);
     return returnStr;
    }
    return null;
  }

  private static String getAttributeValue(String attribute, String src) {
    logger.debug2("looking for "+attribute+" in "+src);
//  we need to allow for all whitespace in our tokenizer;
    StringTokenizer st = new StringTokenizer(src, "\n\t\r =\"", true);
    String lastToken = null;
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (!token.equals("=")) {
	if (!token.equals(" ") && !token.equals("\"")) {
	  lastToken = token;
	}
      } else {
        if (attribute.equalsIgnoreCase(lastToken))
          while (st.hasMoreTokens()) {
            token = st.nextToken();
            // we need to allow for arguments in the url which use '='
            if (!token.equals(" ") && !token.equals("\"")) {
              StringBuffer sb = new StringBuffer(token);
              while (st.hasMoreTokens() &&
                     !token.equals(" ") && !token.equals("\"")) {
                token = st.nextToken();
                sb.append(token);
              }
              return sb.toString();
            }
          }

//	if (attribute.equalsIgnoreCase(lastToken))
//	  while (st.hasMoreTokens()) {
//	    token = st.nextToken();
//	    if (!token.equals(" ") && !token.equals("\"")) {
//	      return token;
//	    }
//	  }
      }
    }
    return null;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[GoslingCrawlerImpl: ");
    sb.append(au.toString());
    sb.append("]");
    return sb.toString();
  }
}
