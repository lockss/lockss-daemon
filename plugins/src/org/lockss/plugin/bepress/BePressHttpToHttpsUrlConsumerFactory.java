/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bepress;

import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.plugin.base.HttpToHttpsUrlConsumerFactory;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A variant on 
 * {@link HttpToHttpsUrlConsumer}
 * for Atypon that allows the redirect url to have additional args
 * </p>
 * 
 * @author Alexandra Ohlson
 */
public class BePressHttpToHttpsUrlConsumerFactory extends HttpToHttpsUrlConsumerFactory {
	private static final Logger log = Logger.getLogger(BePressHttpToHttpsUrlConsumerFactory.class);
	
	  protected static final Pattern REF_ARG_PATTERN = Pattern.compile("referer=[^&]+&httpsredir=1(&)?");
	  protected static final Pattern TRAIL_SLASH_PATTERN = Pattern.compile("/$");

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade,
                                       FetchedUrlData fud) {
    return new BePressHttpToHttpsUrlConsumer(crawlFacade, fud);
  }
  public class BePressHttpToHttpsUrlConsumer extends HttpToHttpsUrlConsumer {

  
    public BePressHttpToHttpsUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }


    //
    // bepress does a couple non-standard things on their https redirection
    // 1. they add a "referrer=<something>&httpsredir=1" argument in to the new url for articles
    //    which we normalize out so the redirect url doesn't necessarily equal the fetch url 
    // 2. for TOC they're currently going from http to https and then back to http so allow for any number of 
    //    redirection hops so long as the end = the start after normalization.
    //
    public boolean shouldStoreAtOrigUrl() {
    	log.debug3("FUD: " + fud.toString());
    	log.debug3("FUD fetch: " + fud.fetchUrl);
    	// In order to compare the original and fetched, remove the referer arg as well
    	// for redirected TOC, check against a version that didn't redirect to a version with a trailing slash
    	String normFetch = REF_ARG_PATTERN.matcher(fud.fetchUrl).replaceFirst("");
    	String noSlashFetch = TRAIL_SLASH_PATTERN.matcher(fud.fetchUrl).replaceFirst("");
      return AuUtil.isBaseUrlHttp(au)
          && fud.redirectUrls != null
          && fud.redirectUrls.size() >= 1
          //&& fud.fetchUrl.equals(fud.redirectUrls.get(0))
          && UrlUtil.isHttpUrl(fud.origUrl)
          && UrlUtil.isHttpsUrl(fud.fetchUrl)
          && (UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(fud.fetchUrl)) ||
        		  UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(normFetch)) ||
        		  UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(noSlashFetch)));
    }
 }
 
}
