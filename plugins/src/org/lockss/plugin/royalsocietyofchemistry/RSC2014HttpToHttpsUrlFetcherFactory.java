/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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
