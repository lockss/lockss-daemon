/*
Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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
package org.lockss.plugin.resiliencealliance;

import org.lockss.daemon.Crawler;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;

import java.io.IOException;

public class ResilienceAllianceUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(ResilienceAllianceUrlConsumerFactory.class);

  ResilienceAllianceUrlHostHelper rauhh = new ResilienceAllianceUrlHostHelper();

  @Override
  public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade crawlFacade,
                                       FetchedUrlData fud) {
    return new ResilienceAllianceUrlConsumer(crawlFacade, fud);
  }

  public class ResilienceAllianceUrlConsumer extends SimpleUrlConsumer {

    public ResilienceAllianceUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }

    @Override
    public void consume() throws IOException {
      if (shouldStoreAtOrigUrl()) {
        storeAtOrigUrl();
      }
      super.consume();
    }

    public boolean shouldStoreAtOrigUrl() throws IOException {
      boolean should = false;
      String base_url =  au.getConfiguration().get("base_url");;
      String baseurl_w_or_without_www = rauhh.getWwwComplementFromUrl(base_url);
    	if (fud.redirectUrls != null && (fud.redirectUrls.size() > 0)) {
    		// if redirected from our base url to the other one
    		if (fud.fetchUrl.contains(base_url) & fud.redirectUrls.get(0).contains(baseurl_w_or_without_www)) {
    			should = true;
    		}
    	}
      return should;
    }
  }
}
