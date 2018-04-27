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

package org.lockss.plugin.atypon.hlthaff;


import java.io.IOException;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;

/**
 * <p>
 * A simple UrlConsumerFactory
 */
public class HealthAffairsUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(HealthAffairsUrlConsumerFactory.class);
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade,
      FetchedUrlData fud) {
    return new HealthAffairsUrlConsumer(crawlFacade, fud);
  }
  
  public class HealthAffairsUrlConsumer extends SimpleUrlConsumer {
    
    public HealthAffairsUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
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
     * </p>
     * @throws IOException for unhandled redirection
     * 
     */
    public boolean shouldStoreAtOrigUrl() throws IOException {
      boolean should = false;
      
      if (fud.redirectUrls != null && (fud.redirectUrls.size() > 0)) {
        //the fetched = original or fetched is from cdn
        if (fud.fetchUrl.equals(fud.origUrl) ||
            fud.fetchUrl.equals(fud.origUrl.concat("?cookieSet=1")) ||
            fud.fetchUrl.startsWith(fud.origUrl)) {
          should = true;
        }
        log.debug3("hlthaff redirect: " + fud.redirectUrls.size() + " " + fud.origUrl + " to " + fud.fetchUrl + " : " + should);
        if (!should && au.getConfiguration().containsKey(ConfigParamDescr.BASE_URL.getKey())) {
          String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
          if (fud.origUrl.startsWith(baseUrl)) {
            log.warning("myfud: " + fud.redirectUrls.size() + " " + fud.redirectUrls.toString());
          }
        }
      }
      return should;
    }
  }
}
