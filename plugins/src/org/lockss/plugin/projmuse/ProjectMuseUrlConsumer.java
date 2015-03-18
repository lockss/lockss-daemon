/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.projmuse;

import java.io.IOException;
import java.util.List;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;

/**
 * @since 1.67.5 
 */
public class ProjectMuseUrlConsumer extends SimpleUrlConsumer {

  /**
   * 
   * @param facade
   * @param fud
   * @since 1.67.5
   */
  public ProjectMuseUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    super(facade, fud);
  }

  @Override
  public void consume() throws IOException {
    if (shouldStoreRedirectsAtOrigUrl(au, fud)) {
      // SimpleUrlConsumer stores at fud.origUrl, and processes the redirect
      // chain if (fud.redirectUrls != null && fud.fetchUrl != null)
      fud.redirectUrls = null;
      fud.fetchUrl = null;
    }
    super.consume();
  }
  
  /**
   * 
   * @param fud
   * @return
   * @since 1.67.5
   */
  protected static boolean shouldStoreRedirectsAtOrigUrl(ArchivalUnit au,
                                                         FetchedUrlData fud) {
    if (ProjectMuseUtil.isBaseUrlHttps(au)) {
      return false;
    }
    List<String> redirectUrls = fud.redirectUrls;
    if (redirectUrls != null && redirectUrls.size() == 1) {
      String redirectUrl = redirectUrls.get(0);
      String fetchUrl = fud.fetchUrl;
      if (redirectUrl.equals(fetchUrl)) {
        String origUrl = fud.origUrl;
        return origUrl.startsWith(ProjectMuseUtil.HTTP)
            && fetchUrl.startsWith(ProjectMuseUtil.HTTPS)
            && ProjectMuseUtil.baseUrlHttpsToHttp(au, fud.fetchUrl).equals(origUrl);
      }
    }
    return false;
  }
  
}
