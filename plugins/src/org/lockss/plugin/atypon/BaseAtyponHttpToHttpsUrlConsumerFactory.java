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

package org.lockss.plugin.atypon;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A variant on 
 * {@link HttpToHttpsUrlConsumer}
 * for Atypon that allows a change from a "/" to a "%2F" within the doi
 * of showCitFormat urls
 * </p>
 * 
 * @author Alexandra Ohlson
 */
public class BaseAtyponHttpToHttpsUrlConsumerFactory implements UrlConsumerFactory {

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade,
                                       FetchedUrlData fud) {
    return new BaseAtyponHttpToHttpsUrlConsumer(crawlFacade, fud);
  }
  public class BaseAtyponHttpToHttpsUrlConsumer extends HttpToHttpsUrlConsumer {

  
    public BaseAtyponHttpToHttpsUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }


    /**
     * <p>
     * Determines if the URL is to be stored under its redirect chain's origin
     * URL. By default in this implementation, this is true when the AU is
     * HTTP-defined and there has been a single-hop redirect from an HTTP URL to
     * the exactly corresponding HTTPS URL.
     * With the additional exception that 
     * 
     * http://arc.aiaa.org/action/showCitFormats?doi=10.2514/1.C032918
     * redirects to:
     * https://arc.aiaa.org/action/showCitFormats?doi=10.2514%2F1.C032918
     * which will normalize to
     * http://arc.aiaa.org/action/showCitFormats?doi=10.2514%2F1.C032918
     * 
     * </p>
     * 
     */
    public boolean shouldStoreAtOrigUrl() {
      return AuUtil.isBaseUrlHttp(au)
          && fud.redirectUrls != null
          && fud.redirectUrls.size() == 1
          && fud.fetchUrl.equals(fud.redirectUrls.get(0))
          && UrlUtil.isHttpUrl(fud.origUrl)
          && UrlUtil.isHttpsUrl(fud.fetchUrl)
          && (UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(fud.fetchUrl)) ||
               UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(fud.fetchUrl.replace("%2F","/"))));
    }
 }
 
}
