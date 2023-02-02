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

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * Lower-case the url
 * change  http://pubs.rsc.org/en/Content/ArticleLanding/2009/GC/B822924D
 * to this http://pubs.rsc.org/en/content/articlelanding/2009/gc/b822924d
 * 
 * and http://xlink.rsc.org/?DOI=B712109A
 * to http://xlink.rsc.org/?doi=b712109a
 * 
 * Note that the vanilla BaseUrlHttpHttpsUrlNormalizer is fine because
 * only the base_url has changed to https
 * The resolver_url is still at http and the base_url2 and graphics_url are
 * more like CDN support
 */

public class RSC2014UrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  
  private static final Logger log = Logger.getLogger(RSC2014UrlNormalizer.class);
  
  /* 
   * Since this happens after the Http to Https normalization we can assume the 
   * protocol has already been made to match the params, but to be safe use host only
   * resolver_url_host
   *       param[base_url] = http://pubs.rsc.org/
   *       param[resolver_url] = http://xlink.rsc.org/
   */
  private static String content_url_host = "";
  private static String resolver_url_host = "";
  
  public String additionalNormalization(String url, ArchivalUnit au)
      throws PluginException {
    
    /*
     * Since this happens after the HttpToHttps normalization, we know that we match our 
     * base_url/resolver_url protocol 
     */
    log.debug3("url in :" + url);
    if (content_url_host.isEmpty()) {
      content_url_host = StringUtils.substringAfter(au.getConfiguration().get("base_url"),"://") + "en/content/";
      resolver_url_host = StringUtils.substringAfter(au.getConfiguration().get("resolver_url"), "://");
    }
    // if the url is either a content url or a redirect url make sure it's lower case
    String testurl = StringUtils.lowerCase(url);
    if (testurl.contains(content_url_host) || 
        testurl.contains(resolver_url_host)) {
      url = testurl;
    }
    log.debug3("url out :" + url);
    return url;
  }

}
