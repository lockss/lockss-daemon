/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.highwire.ers;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.httpclient.CircularRedirectException;

import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;

public class ERSScolarisCrawlSeedFactory implements CrawlSeedFactory{
    private static final Logger log = Logger.getLogger(ERSScolarisCrawlSeedFactory.class);
    @Override
    public CrawlSeed createCrawlSeed(CrawlerFacade crawlFacade) {
        return new ERSScolarisCrawlSeed(crawlFacade);
    }

    public static class ERSScolarisCrawlSeed extends BaseCrawlSeed {
    
    protected CrawlerFacade cf;

    public ERSScolarisCrawlSeed(CrawlerFacade cf) {
        super(cf);
        this.cf = cf;
    };

    @Override
    protected void initialize() throws ConfigurationException, IOException, PluginException{
        super.initialize();
        Collection<String> startUrls = au.getStartUrls();
        String startUrl = startUrls.iterator().next();
        log.debug2("Prefetching permission URL: " + startUrl);
        UrlFetcher uf = cf.makeUrlFetcher(startUrl);
        uf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_FOLLOW);
        FetchResult fr;
        try {
          fr = uf.fetch();
          log.critical("Uh oh! " + fr);
        }
        catch (CacheException.UnknownExceptionException uee) {
          log.critical("UEE", uee);
          log.critical("UEE CAUSE", uee.getCause());
          Throwable cause = uee.getCause();
          if (cause == null || !(cause instanceof CircularRedirectException)) {
            log.debug3("Rethrowing an UnknownExceptionException", uee);
            throw uee;
          }
          // FIXME: add a check that it ended up in the right place, but alas, the error message has port 443 in it: Circular redirect to 'https://publications.ersnet.org:443/content/erjor/lockss-manifest/vol_10_manifest.html'
          log.debug2("Prefetching permission URL succeeded: " + startUrl);
        }
        catch (CacheException ce) {
          log.debug("Prefetching permission URL failed: " + startUrl, ce);
          throw ce;
        }

    }

}

}

