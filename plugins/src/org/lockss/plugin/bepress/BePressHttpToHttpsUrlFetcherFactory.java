/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bepress;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcherFactory;
import org.lockss.plugin.base.BaseUrlFetcher;
import org.lockss.util.*;

  public class BePressHttpToHttpsUrlFetcherFactory implements UrlFetcherFactory {
	  private static final Logger log = Logger.getLogger(BePressHttpToHttpsUrlFetcherFactory.class);

  @Override
  public UrlFetcher createUrlFetcher(CrawlerFacade crawlFacade, String url) {
    return new BePressHttpToHttpsUrlFetcher(crawlFacade, url);
  }

  public class BePressHttpToHttpsUrlFetcher extends BaseUrlFetcher {

    
    public BePressHttpToHttpsUrlFetcher(CrawlerFacade crawlFacade, String url) {
      super(crawlFacade, url);
    }

    /*
     * Differs from HttpToHttpsUrlFetcher because it allows the redirect and the 
     * normalized url to differ
     * fetch:http://scholarship.law.duke.edu/cgi/viewcontent.cgi?article=1530&context=alr
     * redir:https://scholarship.law.duke.edu/cgi/viewcontent.cgi?referer=http://scholarship.law.duke.edu/alr/vol34/iss2&httpsredir=1&article=1530&context=alr
     * norm: http://scholarship.law.duke.edu/cgi/viewcontent.cgi?article=1530&context=alr
	 * normalized ends up just like fetch, but redirect has additional referer arguments in addition to https 
     * 
     */
    @Override
    protected boolean isHttpToHttpsRedirect(String fetched,
                                            String redirect,
                                            String normalized) {
        log.debug3("checking redirect: f,r,n: " 
                + fetched.toString() + "," + redirect.toString() + "," + normalized.toString());    	
      return UrlUtil.isHttpUrl(fetched)
          && UrlUtil.isHttpsUrl(redirect)
          && UrlUtil.isHttpUrl(normalized)
//          && UrlUtil.stripProtocol(fetched).equals(UrlUtil.stripProtocol(redirect))
          && fetched.equals(normalized);
    }

    
  }

  
}
