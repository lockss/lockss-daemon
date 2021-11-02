/*

Copyright (c) 2000-2020 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.pensoft.oai;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import org.dspace.xoai.serviceprovider.client.OAIClient;
import org.dspace.xoai.serviceprovider.exceptions.HttpException;
import org.dspace.xoai.serviceprovider.parameters.Parameters;
import org.lockss.crawler.CrawlUrlData;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcher.FetchResult;
import org.lockss.plugin.UrlFetcher.RedirectScheme;
import org.lockss.plugin.base.PassiveUrlConsumerFactory.PassiveUrlConsumer;
import org.lockss.util.Deadline;
import org.lockss.util.IOUtil;
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
