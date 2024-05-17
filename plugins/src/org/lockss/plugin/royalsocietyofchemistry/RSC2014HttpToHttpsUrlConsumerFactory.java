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

package org.lockss.plugin.royalsocietyofchemistry;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A variant on 
 * {@link HttpToHttpsUrlConsumer}
 * for RSC that allows and xlink to http to https to be stored at the http version
 * </p>
 * 
 * @author Alexandra Ohlson
 */
public class RSC2014HttpToHttpsUrlConsumerFactory implements UrlConsumerFactory {
	private static final Logger log = Logger.getLogger(RSC2014HttpToHttpsUrlConsumerFactory.class);

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade,
                                       FetchedUrlData fud) {
    return new RSC2014HttpToHttpsUrlConsumer(crawlFacade, fud);
  }
  public class RSC2014HttpToHttpsUrlConsumer extends HttpToHttpsUrlConsumer {
    /* it would be more flexible to get this off the au:
     * au.getConfiguration().get("resolver_url")
     * but expensive. And it is always this. So leave it hard coded on the class for now
     */
      private static final String XLINK_HOST = "xlink.rsc.org";
  
    public RSC2014HttpToHttpsUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }
    
    @Override
    public void consume() throws IOException {
      checkXlinkStoreAtHttpUrl();
      // the regular http to https for other cases
      super.consume();
    }


    /**
     * base_url is http and 
     * http://xlink.rsc.org --> http --> https
     * we want to store this at BOTH xlink and http
     * but not at the https
     * 
     */
    private void checkXlinkStoreAtHttpUrl() {

      if (AuUtil.isBaseUrlHttp(au)
          && fud.redirectUrls != null
          && fud.redirectUrls.size() > 1
          && fud.origUrl.contains(XLINK_HOST)
          && UrlUtil.isHttpsUrl(fud.fetchUrl)
          && fud.origUrl.contains("?doi=")) {
        String code = StringUtils.substringAfter(fud.origUrl, "?doi=");
        if (fud.fetchUrl.endsWith("/" + code)) {
          if (log.isDebug2()) {
            log.debug2(String.format("Storing redirect chain %s (fetch URL %s) at first redirect URL and ignoring orig url %s",
                fud.redirectUrls.toString(),
                fud.fetchUrl,
                fud.origUrl));
          }

          fud.redirectUrls.set(0, fud.fetchUrl);
          for(int i=fud.redirectUrls.size()-1; i >= 1; i-- ){
            fud.redirectUrls.remove(i);
          }
          /*
           * We do still need to log this as a redirect, so leave that flag on
           * Set the content URL property to the URL under which the content is stored
           */
          fud.headers.put(CachedUrl.PROPERTY_CONTENT_URL, fud.fetchUrl);
        }
      }
    }
    
 }
 
}
