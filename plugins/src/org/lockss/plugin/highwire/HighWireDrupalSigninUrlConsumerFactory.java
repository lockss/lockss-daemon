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

package org.lockss.plugin.highwire;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * @since 1.68.0 with storeAtOrigUrl()
 */
public class HighWireDrupalSigninUrlConsumerFactory extends HighWireDrupalUrlConsumerFactory {
  private static final Logger log = Logger.getLogger(HighWireDrupalSigninUrlConsumerFactory.class);
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a UrlConsumer");
    return new HighWireDrupalSigninUrlConsumer(facade, fud);
  }
  
  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain 
   * 
   * </p>
   * 
   * @since 1.68.0
   */
  public class HighWireDrupalSigninUrlConsumer extends HighWireDrupalUrlConsumer {
    
    
    public HighWireDrupalSigninUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }
    
    /**
     * <p>
     * Determines if a particular redirect chain should cause content to be stored
     * only at the origin URL ({@link FetchedUrlData#origUrl}).
     * </p>
     * 
     * @return True if and only if the fetched URL data represents a particular
     *         redirect chain that should cause content to be stored only at the
     *         origin URL.
     */
    @Override
    public boolean shouldStoreAtOrigUrl() {
      boolean should = super.shouldStoreAtOrigUrl();
      if (!should) {
        //special extra case while trying ASH signon redirect
        should = (fud.redirectUrls != null
            && (fud.redirectUrls.size() == 2 || fud.redirectUrls.size() == 3)
            && fud.redirectUrls.get(0).equals(fud.fetchUrl)
            && fud.origUrl.equals(fud.fetchUrl));
        if (fud.redirectUrls != null && fud.redirectUrls.size() > 1) {
          log.debug3("big redirect: " + fud.origUrl + " to " + fud.fetchUrl);
        }
      }
      return should;
    }
    
  }
  
}
