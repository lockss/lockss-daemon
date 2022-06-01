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

import org.lockss.crawler.BaseCrawlSeed;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class MedknowCrawlSeed extends BaseCrawlSeed {

  private static final Logger log = Logger.getLogger(MedknowCrawlSeed.class);

  public boolean isFailOnStartUrlError() {
    return false;
  }
  protected Crawler.CrawlerFacade facade;

  public MedknowCrawlSeed(Crawler.CrawlerFacade crawlerFacade) {
    super(crawlerFacade);
    this.facade = crawlerFacade;
  }

  @Override
  public Collection<String> doGetStartUrls()
      throws ArchivalUnit.ConfigurationException, PluginException, IOException {
    Collection<String> startUrls = super.doGetStartUrls();
    return checkUrls(startUrls);
  }

  @Override
  public Collection<String> doGetPermissionUrls()
      throws ArchivalUnit.ConfigurationException, PluginException, IOException {
    Collection<String> permUrls = super.doGetPermissionUrls();
    return checkUrls(permUrls);
  }

  private Collection<String> checkUrls(Collection<String> urls)
      throws IOException {
    Collection<String> okUrls = new ArrayList<>();
    for (String url : urls) {
      // Make a URL fetcher
      UrlFetcher uf = facade.makeUrlFetcher(url);

      UrlFetcher.FetchResult fr;
      try {
        fr = uf.fetch();
      }
      catch (CacheException ce) {
        log.debug2("Stopping due to fatal CacheException", ce);
        Throwable cause = ce.getCause();
        if (cause != null && IOException.class.equals(cause.getClass())) {
          throw (IOException)cause; // Unwrap IOException
        }
        else {
          throw ce;
        }
      }
      if (fr != UrlFetcher.FetchResult.NOT_FETCHED) {
        facade.getCrawlerStatus().removePendingUrl(url);
        facade.getCrawlerStatus().signalUrlFetched(url);
        okUrls.add(url);
      }
    }
    return okUrls;
  }
}
