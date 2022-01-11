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

package org.lockss.plugin.atypon.massachusettsmedicalsociety;

import org.lockss.daemon.Crawler;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.atypon.BaseAtyponHttpToHttpsUrlFetcherFactory;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

public class MassachusetssMedicalSocietyUrlFetcherFactory extends BaseAtyponHttpToHttpsUrlFetcherFactory {

  private static final Logger log = Logger.getLogger(MassachusetssMedicalSocietyUrlFetcherFactory.class);
  protected static final Pattern POLISH_CACHE_PATTERN =
      Pattern.compile("(/digital-objects/|(/images/(img_)?(medium|large)/)).*\\.(jpe?g|png|gif)$",
          Pattern.CASE_INSENSITIVE);

  @Override
  public UrlFetcher createUrlFetcher(Crawler.CrawlerFacade crawlFacade, String url) {
    return new PolishUrlFetcher(crawlFacade, url);
  }

  public class PolishUrlFetcher extends BaseAtyponHttpToHttpsUrlFetcher {

    public PolishUrlFetcher(Crawler.CrawlerFacade crawlFacade, String url) {
      super(crawlFacade, url);
    }

    @Override
    protected String getRequestUrl() throws IOException {
      String url = super.getRequestUrl();
      Matcher postMatch = POLISH_CACHE_PATTERN.matcher(url);
      if (postMatch.find()) {
        url += "?polish-cache-bypass-randomness=" + randomAlphanumeric(8);
      }
      return url;
    }

  }
}
