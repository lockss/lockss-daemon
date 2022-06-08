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

package org.lockss.plugin.medknow;

import org.lockss.daemon.Crawler;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

public class MedknowHttpHttpsUrlConsumerFactory extends MedknowUrlConsumerFactory {
  private static final Logger log = Logger.getLogger(MedknowHttpHttpsUrlConsumerFactory.class);


  public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
    return new MedknowHttpHttpsUrlConsumerFactory.MedknowHttpHttpsUrlConsumer(facade, fud);
  }

  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain (e.g. to support collecting and repairing
   * redirect chains that begin with fixed URLs but go through showCaptcha).
   * @since 1.67.5
   */
  public class MedknowHttpHttpsUrlConsumer extends MedknowUrlConsumer {

    public MedknowHttpHttpsUrlConsumer(Crawler.CrawlerFacade facade,
                                       FetchedUrlData fud) {
      super(facade, fud);
    }

    @Override
    protected boolean shouldStoreRedirectsAtOrigUrl() {
      boolean should = false;
      should = (AuUtil.isBaseUrlHttp(au)
        && fud.redirectUrls != null
        && fud.redirectUrls.size() == 1
        && fud.fetchUrl.equals(fud.redirectUrls.get(0))
        && UrlUtil.isHttpUrl(fud.origUrl)
        && UrlUtil.isHttpsUrl(fud.fetchUrl)
        && UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(fud.fetchUrl))
        ) || super.shouldStoreRedirectsAtOrigUrl();
      log.debug2("in http https medknow");
      return should;
    }
  }

}
