/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.royalsocietyofchemistry;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A variant on 
 * {@link HttpToHttpsUrlConsumer}
 * for RSC that allows and xlink to http to https to be stored at the http version
 * </p>
 * 
 * @author Alexandra Ohlson
 */
public class RSC2014HttpToHttpsUrlConsumerFactory implements UrlConsumerFactory {
	private static final Logger log = Logger.getLogger(RSC2014HttpToHttpsUrlConsumerFactory.class);

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade,
                                       FetchedUrlData fud) {
    return new RSC2014HttpToHttpsUrlConsumer(crawlFacade, fud);
  }
  public class RSC2014HttpToHttpsUrlConsumer extends HttpToHttpsUrlConsumer {
    /* it would be more flexible to get this off the au:
     * au.getConfiguration().get("resolver_url")
     * but expensive. And it is always this. So leave it hard coded on the class for now
     */
      private static final String XLINK_HOST = "xlink.rsc.org";
  
    public RSC2014HttpToHttpsUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }
    
    
    /* We need to add in another test in addition to shouldStoreAtOrigUrl
     * we have some cases where the xlink --> http --> https and 
     * we want to store at the http which is the first of the redirects.
     */
    @Override
    public void consume() throws IOException {
      checkXlinkStoreAtHttpUrl();
      // the regular http to https for other cases
      super.consume();
    }


    /**
     * base_url is http and 
     * http://xlink.rsc.org --> http --> https
     * we want to store this at BOTH xlink and http
     * but not at the https
     * 
     */
    private void checkXlinkStoreAtHttpUrl() {

      if (AuUtil.isBaseUrlHttp(au)
          && fud.redirectUrls != null
          && fud.redirectUrls.size() == 2
          && fud.origUrl.contains(XLINK_HOST)
          && UrlUtil.isHttpsUrl(fud.fetchUrl)) {

        if (UrlUtil.stripProtocol(fud.redirectUrls.get(0)).equalsIgnoreCase(UrlUtil.stripProtocol(fud.redirectUrls.get(1)))) {
          if (log.isDebug2()) {
            log.debug2(String.format("Storing redirect chain %s (fetch URL %s) at first redirect URL and ignoring orig url %s",
                fud.redirectUrls.toString(),
                fud.fetchUrl,
                fud.origUrl));
          }
          // we know this works based on the opening if statement
          fud.redirectUrls.remove(1);
          fud.fetchUrl = fud.redirectUrls.get(0);
          /*
           * We do still need to log this as a redirect, so leave that flag on
           * Set the content URL property to the URL under which the content is stored
           */
          fud.headers.put(CachedUrl.PROPERTY_CONTENT_URL, fud.redirectUrls.get(0));
        }
      }
    }
    
    
 }
 
}
