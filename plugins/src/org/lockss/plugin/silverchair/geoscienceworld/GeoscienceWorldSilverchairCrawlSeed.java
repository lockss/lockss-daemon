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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.lockss.config.Configuration;
import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlData;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.gigascience.GigaScienceDoiLinkExtractor;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;

public class GeoscienceWorldSilverchairCrawlSeed extends BaseCrawlSeed{
    private static final Logger log = Logger.getLogger(GeoscienceWorldSilverchairCrawlSeed.class);

    //https://pubs.geoscienceworld.org/gsa/books/edited-volume/2377/Recent-Advancement-in-Geoinformatics-and-Data

    protected Crawler.CrawlerFacade facade;

    protected String startUrl;
    protected String redirectUrl;

    public GeoscienceWorldSilverchairCrawlSeed(Crawler.CrawlerFacade facade) {
        super(facade);
        if (au == null) {
            throw new IllegalArgumentException("Valid archival unit required for crawl seed");
        }
        this.facade = facade;
    }

    
    //should only be one redirect url, store in fake html page
    protected void storeStartUrls(String redirectUrl, String url) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n");
        log.debug3("SINGLE_node_API_URL is :"  + redirectUrl);
        sb.append("<a href=\"" + redirectUrl + "\">" + redirectUrl + "</a><br/>\n");
        sb.append("</html>");
        CIProperties headers = new CIProperties();
        //Should use a constant here
        headers.setProperty("content-type", "text/html; charset=utf-8");
        UrlData ud = new UrlData(new ByteArrayInputStream(sb.toString().getBytes(Constants.ENCODING_UTF_8)), headers, url);
        UrlCacher cacher = facade.makeUrlCacher(ud);
        cacher.storeContent();
    }

}
