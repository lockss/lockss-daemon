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

package org.lockss.plugin.internationalunionofcrystallography.oai;

import java.io.IOException;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.UrlUtil;

/**
 * @see org.lockss.plugin.base.HttpToHttpsUrlConsumer
 */
public class IUCrOaiHttpHttpsUrlConsumer extends SimpleUrlConsumer {

  public static final String SCRIPT_URL = "script_url";
  
  public IUCrOaiHttpHttpsUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
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
   * URL.
   * </p>
   *
   * @see org.lockss.plugin.base.HttpToHttpsUrlConsumer#shouldStoreAtOrigUrl()
   */
  public boolean shouldStoreAtOrigUrl() {
    boolean isFromBaseUrl = UrlUtil.isSameHost(fud.origUrl, au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()));
    boolean baseUrlIsHttp = AuUtil.isBaseUrlHttp(au);
    boolean isFromScriptUrl = UrlUtil.isSameHost(fud.origUrl, au.getConfiguration().get(SCRIPT_URL));
    boolean scriptUrlIsHttp = UrlUtil.isHttpUrl(au.getConfiguration().get(SCRIPT_URL));
    return    ((isFromBaseUrl && baseUrlIsHttp) || (isFromScriptUrl && scriptUrlIsHttp))
           && fud.redirectUrls != null
           && fud.redirectUrls.size() == 1
           && fud.fetchUrl.equals(fud.redirectUrls.get(0))
           && UrlUtil.isHttpUrl(fud.origUrl)
           && UrlUtil.isHttpsUrl(fud.fetchUrl)
           && UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(fud.fetchUrl));
  }
  
}
