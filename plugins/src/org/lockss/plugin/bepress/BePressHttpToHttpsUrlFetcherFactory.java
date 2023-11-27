/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
