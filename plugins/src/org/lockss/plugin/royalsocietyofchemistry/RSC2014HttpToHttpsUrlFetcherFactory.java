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

package org.lockss.plugin.royalsocietyofchemistry;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcherFactory;
import org.lockss.plugin.base.HttpToHttpsUrlFetcher;
import org.lockss.util.*;

  public class RSC2014HttpToHttpsUrlFetcherFactory implements UrlFetcherFactory {
    private static final Logger log = Logger.getLogger(RSC2014HttpToHttpsUrlFetcherFactory.class);

  @Override
  public UrlFetcher createUrlFetcher(CrawlerFacade crawlFacade, String url) {
    return new RSC2014HttpToHttpsUrlFetcher(crawlFacade, url);
  }

  public class RSC2014HttpToHttpsUrlFetcher extends HttpToHttpsUrlFetcher {

    /* it would be more flexible to get this like this before use:
     * au.getConfiguration().get("resolver_url")
     * but expensive. So leave it hard coded on the class for now
     */
      private static final String XLINK_HOST = "xlink.rsc.org";

  public RSC2014HttpToHttpsUrlFetcher(CrawlerFacade crawlFacade, String url) {
    super(crawlFacade, url);
  }

 
  /*  The resolver_url version of an article redirects to the HTTP which 
   * redirects to the HTTPS which is normalized back to HTTP
   * and we need this process to stop there
   * The normalizer also makes the entire url lower case so it might not be a case-match
   * 
   * fetched:
   * http://xlink.rsc.org/?doi=c005531j
   * redirect:
   * http://pubs.rsc.org/en/content/articlelanding/2010/fo/c005531j
   * to
   * https://pubs.rsc.org/en/content/articlelanding/2010/fo/c005531j
   * normalized:
   * http://pubs.rsc.org/en/content/articlelanding/2010/fo/c005531j
   * 
   * This is called for each step in the redirect chain, so we only need to identify the final http to https as a redirect
   */
  
  @Override
  protected boolean isHttpToHttpsRedirect(String fetched,
                                          String redirect,
                                          String normalized) {
    boolean isRedirect = false;
     /* this is just like super but with case insensitivity because we normalize to lower */
    log.debug3("f: " + fetched + " r: " + redirect + " n: " + normalized);
     isRedirect = UrlUtil.isHttpUrl(fetched)
         && UrlUtil.isHttpsUrl(redirect)
         && UrlUtil.isHttpUrl(normalized)
         && UrlUtil.stripProtocol(fetched).equalsIgnoreCase(UrlUtil.stripProtocol(redirect))
         && fetched.equalsIgnoreCase(normalized);

     log.debug3("isRedirect = " + isRedirect);
     return isRedirect;
  }

  }
  
}
