/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.base;

import java.io.IOException;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * This URL consumer is suitable for a site that used to be HTTP-only and is now
 * HTTP-to-HTTPS, meaning that all HTTP URL requests are now redirected to the
 * corresponding HTTPS URL. For HTTPS-defined AUs, it does nothing special.
 * For HTTP-defined AUs, it stores content under the origin (HTTP) URL when it
 * encounters an HTTP-to-HTTPS redirect chain, as determined by
 * {@link #shouldStoreAtOrigUrl()} (which can be overridden).
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.70
 * @see HttpToHttpsUrlConsumerFactory
 */
public class HttpToHttpsUrlConsumer extends SimpleUrlConsumer {
  private static final Logger log = Logger.getLogger(HttpToHttpsUrlConsumer.class);
  /**
   * @param facade
   * @param fud
   * @since 1.70
   */
  public HttpToHttpsUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
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
   * URL. By default in this implementation, this is true when the AU is
   * HTTP-defined and there has been a single-hop redirect from an HTTP URL to
   * the exactly corresponding HTTPS URL.
   * </p>
   * 
   * @return true if and only if the URL fetch matches the above criteria
   * @since 1.70
   * @see AuUtil#isBaseUrlHttp(ArchivalUnit)
   */
  public boolean shouldStoreAtOrigUrl() {
    log.info("inside shouldStore at Orig Url");
    boolean should = AuUtil.isBaseUrlHttp(au)
           && fud.redirectUrls != null
           && fud.redirectUrls.size() == 1
           && fud.fetchUrl.equals(fud.redirectUrls.get(0))
           && UrlUtil.isHttpUrl(fud.origUrl)
           && UrlUtil.isHttpsUrl(fud.fetchUrl)
           && UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(fud.fetchUrl));
    log.info("should store at Orig? : " + should);
    return should;
  }
  
}
