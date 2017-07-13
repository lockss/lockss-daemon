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

package org.lockss.plugin.atypon;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.UrlFetcher;
import org.lockss.plugin.UrlFetcherFactory;
import org.lockss.plugin.base.HttpToHttpsUrlFetcher;
import org.lockss.util.*;

  public class BaseAtyponHttpToHttpsUrlFetcherFactory implements UrlFetcherFactory {

  @Override
  public UrlFetcher createUrlFetcher(CrawlerFacade crawlFacade, String url) {
    return new BaseAtyponHttpToHttpsUrlFetcher(crawlFacade, url);
  }

  public class BaseAtyponHttpToHttpsUrlFetcher extends HttpToHttpsUrlFetcher {

  protected static final String SHOWCITFORMAT = "action/showCitFormats";

  public BaseAtyponHttpToHttpsUrlFetcher(CrawlerFacade crawlFacade, String url) {
    super(crawlFacade, url);
  }

    /*
     * Differs from HttpToHttpsUrlFetcher
     * 
     * For the action/... URLs, it allows the encoding of arguments
     * For example:
     * http://arc.aiaa.org/action/showCitFormats?doi=10.2514/1.C032918
     * redirects to:
     * https://arc.aiaa.org/action/showCitFormats?doi=10.2514%2F1.C032918
     * which will normalize to
     * http://arc.aiaa.org/action/showCitFormats?doi=10.2514%2F1.C032918
      * 
     */
    @Override
    protected boolean isHttpToHttpsRedirect(String fetched,
                                            String redirect,
                                            String normalized) {
      return UrlUtil.isHttpUrl(fetched)
          && UrlUtil.isHttpsUrl(redirect)
          && UrlUtil.isHttpUrl(normalized)
          && (UrlUtil.stripProtocol(fetched).equals(UrlUtil.stripProtocol(redirect)) ||
              UrlUtil.stripProtocol(fetched).equals(UrlUtil.stripProtocol(redirect.replace("%2F", "/"))))
          && (fetched.equals(normalized) ||
              fetched.equals(normalized.replace("%2F","/")));
    }
  }

  
}
