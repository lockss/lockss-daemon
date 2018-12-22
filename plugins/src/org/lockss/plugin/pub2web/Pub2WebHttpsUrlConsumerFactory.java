/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * @since 1.68
 */
public class Pub2WebHttpsUrlConsumerFactory extends Pub2WebUrlConsumerFactory {

  private static final Logger log = Logger.getLogger(Pub2WebHttpsUrlConsumerFactory.class);

  @Override
  public Pub2WebHttpsUrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    return new Pub2WebHttpsUrlConsumer(facade, fud);
  }

  public class Pub2WebHttpsUrlConsumer extends HttpToHttpsUrlConsumer {
    
    Pub2WebUrlConsumer baseUC;

    public Pub2WebHttpsUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
      baseUC = new Pub2WebUrlConsumer(facade, fud);
    }

    @Override
    public boolean shouldStoreAtOrigUrl() {
      // handle vanilla redirect from http to https
      boolean should = shouldStoreAtOrigUrlVanilla();
      if (!should) {
        // a more complicated redirect, which *may* include the https redirection as well
        should = baseUC.shouldStoreRedirectsAtOrigUrl();
      }
      return should;
    }

    public boolean shouldStoreAtOrigUrlVanilla() {
      return AuUtil.isBaseUrlHttp(au)
             && fud.redirectUrls != null
             && fud.fetchUrl.equals(fud.redirectUrls.get(0))
             && UrlUtil.isHttpUrl(fud.origUrl)
             && UrlUtil.isHttpsUrl(fud.fetchUrl)
             && UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(fud.fetchUrl));
    }
  }

}
