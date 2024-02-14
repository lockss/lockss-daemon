/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.silverchair.geoscienceworld;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.plugin.base.BaseUrlFetcher;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.plugin.gigascience.GigaScienceDoiLinkExtractor;
import org.lockss.util.CharsetUtil;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;

public class GeoscienceWorldSilverchairCrawlSeed extends BaseCrawlSeed{
    private static final Logger log = Logger.getLogger(GeoscienceWorldSilverchairCrawlSeed.class);

    //https://pubs.geoscienceworld.org/gsa/books/edited-volume/2377/Recent-Advancement-in-Geoinformatics-and-Data

    protected Crawler.CrawlerFacade facade;

    protected List<String> urlList;

    protected String startUrl;


    public GeoscienceWorldSilverchairCrawlSeed(Crawler.CrawlerFacade facade) {
        super(facade);
        if (au == null) {
            throw new IllegalArgumentException("Valid archival unit required for crawl seed");
        }
        this.facade = facade;
    }

    @Override
    public Collection<String> doGetStartUrls() throws PluginException, IOException {
        Collection<String> startUrls = au.getStartUrls();
        if (startUrls == null || startUrls.isEmpty()) {
            throw new PluginException.InvalidDefinition("CrawlSeed expects the Plugin to define a non-null start URL list");
        }
        startUrl = startUrls.iterator().next();
        UrlFetcher uf = makeUrlFetcher(startUrl);
        // Make request
        UrlFetcher.FetchResult fr = null;
        try {
            fr = uf.fetch();
        }catch (CacheException ce){
            if(ce.getCause() != null && ce.getCause().getMessage().contains("LOCKSS")) {
                log.debug("OAI result errored due to LOCKSS audit proxy. Trying alternate start Url", ce);
            } else {
                log.debug2("Stopping due to fatal CacheException", ce);
                Throwable cause = ce.getCause();
                if (cause != null && IOException.class.equals(cause.getClass())) {
                    throw (IOException)cause; // Unwrap IOException
                }
                else {
                    throw ce;
                }
            }
        }
        if (fr == UrlFetcher.FetchResult.FETCHED) {
            facade.getCrawlerStatus().removePendingUrl(startUrl);
            facade.getCrawlerStatus().signalUrlFetched(startUrl);
        }
        log.debug3("the start url is:" + startUrl);
        log.debug3("the fetch result is:" + startUrl);
        return startUrls;
    }

    protected UrlFetcher makeUrlFetcher( final String url) {
        // Make a URL fetcher
        BaseUrlFetcher uf = (BaseUrlFetcher)facade.makeUrlFetcher(url);

        // Set custom URL consumer factory
        uf.setUrlConsumerFactory(new UrlConsumerFactory() {
            @Override
            public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade ucfFacade,
                                                 FetchedUrlData ucfFud) {
                // Make custom URL consumer
                return new SimpleUrlConsumer(ucfFacade, ucfFud) {
                };
            }
        });
        return uf;
    }
}
