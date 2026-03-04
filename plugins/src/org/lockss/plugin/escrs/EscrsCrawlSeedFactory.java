/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.escrs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;

public class EscrsCrawlSeedFactory implements CrawlSeedFactory {
    private static final Logger log = Logger.getLogger(EscrsCrawlSeedFactory.class);

    @Override
    public CrawlSeed createCrawlSeed(CrawlerFacade facade) {
        return new EscrsCrawlSeed(facade);
    }

    public static class EscrsCrawlSeed extends BaseCrawlSeed {
        
        private CrawlerFacade cf;
        public EscrsCrawlSeed(CrawlerFacade facade) {
            super(facade);
            cf = facade;
        }
        
        @Override
        public Collection<String> doGetStartUrls() throws ConfigurationException, PluginException, IOException {
            Collection<String> sUrls = super.doGetStartUrls();
            Collection<String> uUrls = new ArrayList<String>(sUrls.size());

            ArchivalUnit au = cf.getAu();
            String tdbYear = au.getConfiguration().get("year");
            
            for (Iterator<String> iter = sUrls.iterator(); iter.hasNext();) {
                String sUrl = iter.next();
                CachedUrl cu = au.makeCachedUrl(sUrl);
                InputStream in = cu.getUnfilteredInputStream();
                if (in != null) {
                    try {
                        String year = null;
                        CachedUrl issueUrl;
                        Elements articles; 
                        Elements small_element_date;
                        String url = cu.getUrl();
                        try {
                            Document doc = Jsoup.parse(in, cu.getEncoding(), url); 
                            articles = doc.select("article.archived-issue");
                            for(Element article : articles){
                                issueUrl = au.makeCachedUrl(article.select("a.issue-summary__link").attr("href").trim());
                                log.debug3("Current issue url is: " + issueUrl);
                                small_element_date = article.select("p[class=\"archived-issue__date\"]>small");
                                year = checkElement(small_element_date);
                                log.debug3("TDB year is " + tdbYear + " and issue date is " + year);
                                if(year.contains(tdbYear)){
                                    log.debug3(issueUrl.getUrl() + " added to list of start URLs.");
                                    uUrls.add(issueUrl.getUrl());
                                }
                            }
                            return uUrls;
                        } catch (IOException e) {
                            log.debug3("Escrs: Error getting correct AU manifest", e);
                        }
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            return super.doGetStartUrls();
        }

        protected String checkElement(Elements element) {
            String cleanedUpElement = null;
            if ( element != null){
                cleanedUpElement = element.text().trim();
                log.debug3("Escrs: Element is " + element);
                if (cleanedUpElement != null) {
                    log.debug3("Escrs: Element cleaned is " + cleanedUpElement);
                } else {
                    log.debug3("Escrs: Element is null");
                }
            }
            return cleanedUpElement;
        }
    }
}
