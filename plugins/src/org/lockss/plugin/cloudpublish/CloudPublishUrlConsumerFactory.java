/*
 Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.cloudpublish;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.Crawler;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

public class CloudPublishUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(CloudPublishUrlConsumerFactory.class);

  @Override
  public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade crawlFacade,
                                       FetchedUrlData fud) {
    return new CloudPublishUrlConsumer(crawlFacade, fud);
  }
  public class CloudPublishUrlConsumer extends HttpToHttpsUrlConsumer {


    public CloudPublishUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }

    /**
     * <p>
     * Determines if the URL is to be stored under its redirect chain's origin
     * URL.
     *
     * http://dev-liverpoolup.cloudpublish.co.uk/read/?item_type=journal_article&item_id=20082
     *
     * http://dev-liverpoolup.cloudpublish.co.uk/read/?item_type=journal_article&item_id=20194&mode=download
     *  -->
     *    https://liverpoolup.cloudpublish.co.uk/read/?id=20082&type=journal_article&cref=LUP0923&peref=&drm=soft&acs=1&exit=http%3A%2F%2Fdev-liverpoolup.cloudpublish.co.uk%2Fjournals%2Farticle%2F20082%2F&p=6&uid=LUP&t=1639011561&h=fece739719aad172763a1aa10664d1c2
     * </p>
     *
     */
    public boolean shouldStoreAtOrigUrl() {
      boolean should = false;
      should = fud.origUrl.contains("read/?item_type=journal_article&item_id=");;
      if (AuUtil.isBaseUrlHttp(au)
          && fud.redirectUrls != null
          && fud.redirectUrls.size() >= 1
          && UrlUtil.isHttpUrl(fud.origUrl)
          && UrlUtil.isHttpsUrl(fud.fetchUrl)
          && !should) {
        String origBase = StringUtils.substringBefore(UrlUtil.stripProtocol(fud.origUrl),"?");
        String fetchBase = StringUtils.substringBefore(UrlUtil.stripProtocol(fud.fetchUrl),"?");
        should = (
            origBase.equals(fetchBase) ||
                origBase.equals(fetchBase.replaceFirst("/doi/[^/]+/", "/doi/")) ||
                origBase.replaceFirst("/doi/[^/]+/", "/doi/").equals(fetchBase.replaceFirst("/doi/[^/]+/", "/doi/")) ||
                origBase.equals(fetchBase.replace("%2F","/")));
        if (fud.redirectUrls != null) {
          log.debug3("BA redirect " + fud.redirectUrls.size() + ": " + fud.redirectUrls.toString());
          log.debug3("BA redirect: " + " " + fud.origUrl + " to " + fud.fetchUrl + " should consume?: " + should);
        }
      }
      return should;
    }
  }

}
