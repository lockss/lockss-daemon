/*
 * $Id$
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A variant on 
 * {@link HttpToHttpsUrlConsumer}
 * for Atypon that allows a change from a "/" to a "%2F" within the doi
 * of showCitFormat urls
 * </p>
 * 
 * @author Alexandra Ohlson
 */
public class BaseAtyponHttpToHttpsUrlConsumerFactory implements UrlConsumerFactory {
	private static final Logger log = Logger.getLogger(BaseAtyponHttpToHttpsUrlConsumerFactory.class);

  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade,
                                       FetchedUrlData fud) {
    return new BaseAtyponHttpToHttpsUrlConsumer(crawlFacade, fud);
  }
  public class BaseAtyponHttpToHttpsUrlConsumer extends HttpToHttpsUrlConsumer {

  
    public BaseAtyponHttpToHttpsUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }


    /**
     * <p>
     * Determines if the URL is to be stored under its redirect chain's origin
     * URL. By default in this implementation, this is true when the AU is
     * HTTP-defined and there has been a single-hop redirect from an HTTP URL to
     * the exactly corresponding HTTPS URL.
     * With the additional exception that 
     * https://www.emeraldinsight.com/doi/10.1108/SAMPJ-02-2016-0006
     * redirects to
     * https://www.emeraldinsight.com/doi/full/10.1108/SAMPJ-02-2016-0006
     * and 
     * http://www.emeraldinsight.com/doi/abs/10.1108/SAMPJ-02-2016-0006
     * redirects to:
     * https://www.emeraldinsight.com/doi/full/10.1108/SAMPJ-02-2016-0006
     * 
     * http://arc.aiaa.org/action/showCitFormats?doi=10.2514/1.C032918
     * redirects to:
     * https://arc.aiaa.org/action/showCitFormats?doi=10.2514%2F1.C032918
     * which will normalize to
     * http://arc.aiaa.org/action/showCitFormats?doi=10.2514%2F1.C032918
     * 
     * Found another odd change -
     * http://ajph.aphapublications.org/action/showPopup?citid=citart1&id=fd2%20fd4&doi=10.2105%2FAJPH.2011.300237
     * became
     * https://ajph.aphapublications.org/action/showPopup?citid=citart1&id=fd2+fd4&doi=10.2105%2FAJPH.2011.300237
     * Note the encoding of the "+"
     * I think it's safe to generalize this as the first part of the url up to the ? is the same and not worry
     * about the args might or might not be encoded
     *    http://foo.com/showCitFormats?doi=<anything> 
     *    http://foo.com/action/showPopup?citid=<anything> 
     * finally - allow for the / to %2F in the doi portion of a "doi/full/10.1111/blah url
     * </p>
     * 
     */
    public boolean shouldStoreAtOrigUrl() {
      boolean should = false;
      if (AuUtil.isBaseUrlHttp(au)
          && fud.redirectUrls != null
          && fud.redirectUrls.size() >= 1
          && UrlUtil.isHttpUrl(fud.origUrl)
          && UrlUtil.isHttpsUrl(fud.fetchUrl)) {
        String origBase = StringUtils.substringBefore(UrlUtil.stripProtocol(fud.origUrl),"?");
        String fetchBase = StringUtils.substringBefore(UrlUtil.stripProtocol(fud.fetchUrl),"?");
        should = (
            origBase.equals(fetchBase) ||
            origBase.equals(fetchBase.replaceFirst("/doi/[^/]+/", "/doi/")) ||
            origBase.replaceFirst("/doi/[^/]+/", "/doi/").equals(fetchBase.replaceFirst("/doi/[^/]+/", "/doi/")) ||
            origBase.equals(fetchBase.replace("%2F","/")));
        if (fud.redirectUrls != null) {
          log.debug3("BA redirect " + fud.redirectUrls.size() + ": " + fud.redirectUrls.toString());
          log.debug3("BA redirect: " + " " + fud.origUrl + " to " + fud.fetchUrl + " should consume?: " + should);
        }
      }
      return should;
    }
 }
 
}
