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

package org.lockss.plugin.resiliencealliance;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcherFactory;
import org.lockss.plugin.base.BaseUrlFetcher;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.net.MalformedURLException;
import java.util.*;

public class ResilienceAllianceUrlFetcherFactory implements UrlFetcherFactory {
  private static final Logger log = Logger.getLogger(ResilienceAllianceUrlFetcherFactory.class);

  ResilienceAllianceUrlHostHelper rauhh = new ResilienceAllianceUrlHostHelper();

  @Override
  public UrlFetcher createUrlFetcher(CrawlerFacade crawlFacade, String url) {
    return new ResilienceAllianceUrlFetcher(crawlFacade, url);
  }

  public class ResilienceAllianceUrlFetcher extends BaseUrlFetcher {
    public ResilienceAllianceUrlFetcher(CrawlerFacade crawlFacade, String url) {
      super(crawlFacade, url);
      List<String> startAndPermissionUrls = rauhh.getBaseAndPermissionUrls(au);
      if (!startAndPermissionUrls.contains(url)) {
        setFetchUrlToNonWww();
      }
    }

    public void setFetchUrlToNonWww() {
      String baseUrlHost = rauhh.getBaseUrlHost(au);
      try {
        String urlHost = UrlUtil.getHost(this.fetchUrl);
        if (urlHost.startsWith("www.") && urlHost.contains(UrlUtil.delSubDomain(baseUrlHost, "www"))) {
          log.info("changing fetch url from: " + this.fetchUrl);
          this.fetchUrl = UrlUtil.delSubDomain(this.fetchUrl, "www");
          log.info("to: " + this.fetchUrl);
        } else {
          log.info("didnt change: " + this.fetchUrl);
        }
      }
      catch (MalformedURLException mue) {
        log.debug2("Malformed URL", mue);
      }
    }
  }
}
