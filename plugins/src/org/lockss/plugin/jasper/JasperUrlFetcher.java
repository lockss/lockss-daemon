/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.jasper;

import java.io.IOException;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.base.BaseUrlFetcher;
import org.lockss.util.urlconn.CacheException;

public class JasperUrlFetcher extends BaseUrlFetcher {

  protected JasperUrlFetcherHelper helper;
  
  public JasperUrlFetcher(CrawlerFacade crawlerFacade,
                          String url,
                          JasperUrlFetcherHelper helper) {
    super(crawlerFacade, url);
    this.helper = helper;
  }
  
  @Override
  protected void addRequestHeaders() {
    super.addRequestHeaders();
    setRequestProperty("Authorization", helper.getAuthorizationString());
  }

  @Override
  public FetchResult fetch() throws CacheException {
    try {
      helper.ensureExecuted(connectionPool);
    }
    catch (IOException ioe) {
      CacheException ce = new CacheException("URL fetcher helper error");
      ce.initCause(ioe);
      throw ce;
    }
    return super.fetch();
  }

}
