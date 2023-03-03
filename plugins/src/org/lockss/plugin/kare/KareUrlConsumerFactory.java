/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.kare;

import org.lockss.daemon.Crawler;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;

public class KareUrlConsumerFactory implements UrlConsumerFactory {
    private static final Logger log = Logger.getLogger(KareUrlConsumerFactory.class);
    public static final String PDF_ORIG = "https://jag.journalagent.com/z4/download_fulltext.asp";
    public static final String PDF_REDIRECT = "https://jag.journalagent.com/agri/pdfs/";

    @Override
    public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade crawlFacade,
                                         FetchedUrlData fud) {
        return new KareUrlConsumer(crawlFacade, fud);
    }
    public class KareUrlConsumer extends HttpToHttpsUrlConsumer {


        public KareUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
            super(facade, fud);
        }

        /**
         * <p>
         * Determines if the URL is to be stored under its redirect chain's origin
         * URL.
         * Specifically,
         * </p>
         *
         */
        public boolean shouldStoreAtOrigUrl() {
            boolean should;
            should = fud.origUrl.contains(PDF_ORIG);
            if (fud.redirectUrls != null
                    && fud.redirectUrls.size() >= 1
                    && should) {
                should = (fud.origUrl.contains(PDF_ORIG) && fud.fetchUrl.contains(PDF_REDIRECT));
            }
            return should;
        }
    }

}