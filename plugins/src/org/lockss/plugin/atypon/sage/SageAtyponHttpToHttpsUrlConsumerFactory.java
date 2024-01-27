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
package org.lockss.plugin.atypon.sage;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.atypon.BaseAtyponHttpToHttpsUrlConsumerFactory;
import org.lockss.util.Logger;

/* For Sage publications, there are URLs that have /reader/ in its structure but
 * it redirect to an /epdf/ link and then redirects to a /pdf/ link. 
 * Example: http://journals.sagepub.com/doi/reader/10.5301/GTND.2016.15860
 * Therefore, we need to override the default atypon URL consumer factory 
 * which is currently storing the pdf link at the reader link and 
 * follow the redirects to get the pdf link which is what we use for article metadata. 
 */

public class SageAtyponHttpToHttpsUrlConsumerFactory extends BaseAtyponHttpToHttpsUrlConsumerFactory {
    protected static Logger log = Logger.getLogger(SageAtyponHttpToHttpsUrlConsumerFactory.class);

    @Override
    public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade, FetchedUrlData fud) {
        return new SageAtyponHttpToHttpsUrlConsumer(crawlFacade, fud);
    }
    public class SageAtyponHttpToHttpsUrlConsumer extends BaseAtyponHttpToHttpsUrlConsumer {
    
        public SageAtyponHttpToHttpsUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
            super(facade, fud);
        }

        @Override
        public boolean shouldStoreAtOrigUrl() {
            //log.debug3(fud.origUrl);
            if(fud.origUrl.contains("doi/reader/")){
                //log.debug3("This is a reader link.");
                return false;
            }else{
                //log.debug3("This is not a reader link");
                return super.shouldStoreAtOrigUrl();
            }
        }
    }
}
