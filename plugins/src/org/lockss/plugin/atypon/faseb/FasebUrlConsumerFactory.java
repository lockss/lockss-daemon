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

package org.lockss.plugin.atypon.faseb;


import java.io.IOException;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A simple UrlConsumerFactory
 */
public class FasebUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(FasebUrlConsumerFactory.class);
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade,
      FetchedUrlData fud) {
    return new FasebUrlConsumer(crawlFacade, fud);
  }
  
  public class FasebUrlConsumer extends SimpleUrlConsumer {
    
    public FasebUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }
    
    @Override
    public void consume() throws IOException {
      if (shouldStoreAtOrigUrl()) {
        storeAtOrigUrl();
      }
      super.consume();
    }
    
    /**
     * <p>
     * Determines if the URL is to be stored under its redirect chain's origin
     * http://www.fasebj.org/doi/suppl/10.1096/fj.201700799R/suppl_file/fj.201700799R.sd1.docx
     * redirects to:
     * https://faseb-prod-cdn.literatumonline.com/journals/content/fasebj/2018/fasebj.2018.32.issue-2/fj.201700799r/20180116/suppl/fj.201700799r.sd1.docx?b9....
     * 
     * fj.201700799R.sd1.docx name common to both URLs
     * </p>
     * @throws IOException for unhandled redirection
     * 
     */
    public boolean shouldStoreAtOrigUrl() throws IOException {
      boolean should = false;

      // do Http to Https checks
      should = AuUtil.isBaseUrlHttp(au)
          && fud.redirectUrls != null
          && fud.redirectUrls.size() == 1
          && fud.fetchUrl.equals(fud.redirectUrls.get(0))
          && UrlUtil.isHttpUrl(fud.origUrl)
          && UrlUtil.isHttpsUrl(fud.fetchUrl)
          && UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(fud.fetchUrl));

      // do the additional 'should' check specific to Faseb
      if (fud.redirectUrls != null && (fud.redirectUrls.size() > 0)) {
        //the fetched = original or fetched is from cdn
        if (fud.fetchUrl.equals(fud.origUrl) ||
            fud.fetchUrl.contains("faseb-prod-cdn.literatumonline.com/journals/content/fasebj/")) {
          should = true;
        }
        log.debug3("Faseb redirect: " + fud.redirectUrls.size() + " " + fud.origUrl + " to " + fud.fetchUrl + " : " + should);
        if (!should && au.getConfiguration().containsKey(ConfigParamDescr.BASE_URL.getKey())) {
          String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
          if (fud.origUrl.startsWith(baseUrl)) {
            log.warning("myfud: " + fud.redirectUrls.size() + " " + fud.redirectUrls.toString());
//    XXX        throw new IOException("Redirection not handled: " + fud.fetchUrl);
          }
        }
      }
      return should;
    }
  }
}
