/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
    return UrlUtil.isHttpUrl(fetched)
        && UrlUtil.isHttpsUrl(redirect)
        && UrlUtil.isHttpUrl(normalized)
        && UrlUtil.stripProtocol(fetched).equals(UrlUtil.stripProtocol(redirect))
        && fetched.equals(normalized);
  }
  
}
