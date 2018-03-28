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

package org.lockss.plugin.atypon.faseb;


import java.io.IOException;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.Logger;

/**
 * <p>
 * A simple UrlConsumerFactory
 */
public class FedAmSocExpBiolUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(FedAmSocExpBiolUrlConsumerFactory.class);
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade,
      FetchedUrlData fud) {
    return new FedAmSocExpBiolUrlConsumer(crawlFacade, fud);
  }
  
  public class FedAmSocExpBiolUrlConsumer extends SimpleUrlConsumer {
    
    public FedAmSocExpBiolUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
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
     * http://www.fasebj.org/doi/suppl/10.1096/fj.201700799R/suppl_file/fj.201700799R.sd1.docx
     * redirects to:
     * https://faseb-prod-cdn.literatumonline.com/journals/content/fasebj/2018/fasebj.2018.32.issue-2/fj.201700799r/20180116/suppl/fj.201700799r.sd1.docx?b9....
     * 
     * fj.201700799R.sd1.docx name common to both URLs
     * </p>
     * 
     */
    public boolean shouldStoreAtOrigUrl() {
      boolean should = false;
      
      if (fud.redirectUrls != null && (fud.redirectUrls.size() > 0)) {
        //the fetched = original or fetched = original with ?sso-checked=true
        if (fud.fetchUrl.equals(fud.origUrl) ||
            fud.fetchUrl.contains("faseb-prod-cdn.literatumonline.com/journals/content/fasebj/")) {
          should = true;
        }
        log.debug3("SSO redirect: " + fud.redirectUrls.size() + " " + fud.origUrl + " to " + fud.fetchUrl + " : " + should);
        if (!should) {
          log.debug3("myfud: " + fud.redirectUrls.size() + " " + fud.redirectUrls.toString());
        }
      }
      return should;
    }
  }
}
