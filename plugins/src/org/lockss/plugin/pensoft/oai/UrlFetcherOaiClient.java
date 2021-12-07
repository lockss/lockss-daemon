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

package org.lockss.plugin.pensoft.oai;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import org.dspace.xoai.serviceprovider.client.OAIClient;
import org.dspace.xoai.serviceprovider.exceptions.HttpException;
import org.dspace.xoai.serviceprovider.parameters.Parameters;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlFetcher;
import org.lockss.util.Deadline;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.urlconn.CacheException;

public class UrlFetcherOaiClient implements OAIClient{
  private static final Logger log = Logger.getLogger(UrlFetcherOaiClient.class);
  protected CrawlerFacade facade;
  protected String baseUrl;
  private InputStream content;

  public UrlFetcherOaiClient(String baseUrl, CrawlerFacade cf) {
    this.baseUrl = baseUrl;
    facade = cf;
  }
  
  @Override
  public InputStream execute(Parameters parameters) throws HttpException {
	String url = parameters.toUrl(baseUrl);
    UrlFetcher uf = facade.makeUrlFetcher(url);
    BitSet permFetchFlags = uf.getFetchFlags();
    permFetchFlags.set(UrlCacher.REFETCH_FLAG);
    uf.setFetchFlags(permFetchFlags);
    
    facade.getCrawlerStatus().addPendingUrl(url);
    int retriesLeft = -1;
    int totalRetries = -1;
    while (true) {
        try {
          content = uf.getUncachedInputStream();
          if(content == null) {
        	  throw new HttpException("UrlFetcher returned null for an OAI response");
          }
          facade.getCrawlerStatus().removePendingUrl(url);
          facade.getCrawlerStatus().signalUrlFetched(url);
          return content;
        } catch (CacheException e) {
          if (!e.isAttributeSet(CacheException.ATTRIBUTE_RETRY)) {
            throw new HttpException(e);
          }
          if (retriesLeft < 0) {
            retriesLeft = facade.getRetryCount(e);
            totalRetries = retriesLeft;
          }
          if (log.isDebug2()) {
            log.debug("Retryable (" + retriesLeft + ") exception caching "
  		       + url, e);
          } else {
            log.debug("Retryable (" + retriesLeft + ") exception caching "
  		       + url + ": " + e.toString());
          }
          if (--retriesLeft > 0) {
            long delayTime = facade.getRetryDelay(e);
            Deadline wait = Deadline.in(delayTime);
            log.debug3("Waiting " +
  			StringUtil.timeIntervalToString(delayTime) +
  			" before retry");
            while (!wait.expired()) {
              try {
                wait.sleep();
              } catch (InterruptedException ie) {
                // no action
              }
            }
            uf.reset();
          } else {
            log.warning("Failed to cache (" + totalRetries + "), skipping: "
  			 + url);
            throw new HttpException(e);
          }
        } catch(IOException e) {
        	throw new HttpException(e);
        }
      }
  }
}
