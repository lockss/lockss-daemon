/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.base;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.util.*;

/**
 * 
 * @author Thib Guicherd-Callin
 * @since 1.70
 */
public class HttpToHttpsUrlFetcher extends BaseUrlFetcher {

  private static final Logger log = Logger.getLogger(HttpToHttpsUrlFetcher.class);
  
  public HttpToHttpsUrlFetcher(CrawlerFacade crawlFacade, String url) {
    super(crawlFacade, url);
  }

  /**
   * <p>
   * Determines if the triple of a fetch URL, its redirect URL, and the
   * normalized redirect URL is an HTTP-to-HTTPS redirect that is then
   * normalized back to the HTTP URL. In {@link BaseUrlFetcher}, this is always
   * false. In this class ({@link HttpToHttpsUrlFetcher}), this is true if the
   * redirect is otherwise exact and the fetch URL and normalized redirect URL
   * are identical. (If some site slightly alters the redirect URL, for example
   * in case, or if the URL normalizer does, then this method needs to be
   * overridden to allow for more flexibility.)
   * </p>
   * 
   * @param fetched
   *          The fetch URL
   * @param redirect
   *          The redirect URL (the URL the fetch redirected to)
   * @param normalized
   *          The normalized redirect URL
   * @return True if and only if the given triple represents an exact
   *         HTTP-HTTPS-HTTP loop
   * @since 1.70
   * @see HttpToHttpsUrlFetcher#isHttpToHttpsRedirect(String, String, String)
   */
  @Override
  protected boolean isHttpToHttpsRedirect(String fetched,
                                          String redirect,
                                          String normalized) {
    boolean ret =    UrlUtil.isHttpUrl(fetched)
                  && UrlUtil.isHttpsUrl(redirect)
                  && UrlUtil.isHttpUrl(normalized)
                  && UrlUtil.stripProtocol(fetched).equals(UrlUtil.stripProtocol(redirect))
                  && fetched.equals(normalized);
    if (log.isDebug3()) {
      log.debug3(String.format("fetched=%s, redirect=%s, normalized=%s, result: %s",
                               fetched,
                               redirect,
                               normalized,
                               Boolean.toString(ret)));
    }
    return ret;
  }
  
}
