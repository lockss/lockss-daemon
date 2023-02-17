/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
    @Override
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
      // return super.shouldStoreAtOrigUrl();
    }
 }
 
}
