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
