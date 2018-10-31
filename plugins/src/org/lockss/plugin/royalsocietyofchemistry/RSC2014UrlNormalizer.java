/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
