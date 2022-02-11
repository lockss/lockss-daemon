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
package org.lockss.plugin.oecd;

import org.lockss.daemon.Crawler;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;

import java.io.IOException;

public class OecdUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(OecdUrlConsumerFactory.class);
  /*
  https://www.oecd-ilibrary.org/frequency-based-co-movement-of-inflation-in-selected-euro-area-countries_5jm26ttlxdd1.pdf?itemId=%2Fcontent%2Fpaper%2Fjbcma-2015-5jm26ttlxdd1&mimeType=pdf
  https://www.oecd-ilibrary.org/docserver/jbcma-2015-5jm26ttlxdd1.pdf?expires=1643909821&id=id&accname=ocid194777&checksum=DD36836B974F98914B836656A0A287E4
   */

  private static final String DOC_SERVER_REDIRECT = "/docserver/";
  private static final String KXCDN_DOMAIN = "kxcdn.com/";

  @Override
  public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade crawlFacade,
                                       FetchedUrlData fud) {
    return new OecdUrlConsumer(crawlFacade, fud);
  }

  public class OecdUrlConsumer extends SimpleUrlConsumer {

    public OecdUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
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
    	if (fud.redirectUrls != null && (fud.redirectUrls.size() > 0)) {
    		// if redirected to the docserver or kxcdn domain (zip files) we dont need both
    		if (fud.fetchUrl.contains(DOC_SERVER_REDIRECT) ||
            fud.fetchUrl.contains(KXCDN_DOMAIN)
        ) {
    			should = true;
    		}
    	}
      return should;
    }
  }
}
