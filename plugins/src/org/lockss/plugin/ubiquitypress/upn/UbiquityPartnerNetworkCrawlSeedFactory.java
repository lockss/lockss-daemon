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

package org.lockss.plugin.ubiquitypress.upn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.crawler.CrawlSeed;
import org.lockss.crawler.CrawlSeedFactory;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

public class UbiquityPartnerNetworkCrawlSeedFactory implements CrawlSeedFactory {

    private static final Logger log = Logger.getLogger(UbiquityPartnerNetworkCrawlSeedFactory.class);

    @Override
    public CrawlSeed createCrawlSeed(CrawlerFacade crawlFacade) {
        return new UbiquityPartnerNetworkCrawlSeed(crawlFacade);
    }

    public static class UbiquityPartnerNetworkCrawlSeed extends BaseCrawlSeed {
    
            public static Set<String> deceasedAUs = new HashSet<>(Arrays.asList("org|lockss|plugin|ubiquitypress|upn|ClockssUbiquityPartnerNetworkPlugin&base_url~https%3A%2F%2Faccount%2Eestetikajournal%2Eorg%2F&year~2022"));
            private String year;
            private String baseUrl;
    
            public UbiquityPartnerNetworkCrawlSeed(CrawlerFacade crawlerFacade) {
                super(crawlerFacade);
            }
    
            @Override
            public Collection<String> doGetStartUrls() throws ConfigurationException,
            PluginException, IOException {
                if(deceasedAUs.contains(au.getAuId())){
                    Collection<String> uUrls = new ArrayList<String>(2);
                    baseUrl = au.getConfiguration().get("base_url2");
                    year = au.getConfiguration().get("year");
                    String s = baseUrl + "lockss/year/" + year;
                    uUrls.add(s);
                    uUrls.add(UrlUtil.replaceScheme(s, "https", "http"));
                    log.debug3("The start url getting changed is " + uUrls.toString());
                    return uUrls;
                }else{
                    return super.doGetStartUrls();
                }
            }
        }
}
