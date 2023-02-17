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

package org.lockss.plugin.ingenta;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

public class IngentaUrlNormalizer implements UrlNormalizer {

  /**
   * <p>The key for the non-standard configuration parameters
   * 'api_url', of type URL.</p>
   */
  protected static final String KEY_API_URL = "api_url";
  
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {

    /*
     * An Ingenta URL may have a jsessionid in it, which begins with
     * ";jesessionid=" and ends at the question mark for the URL query
     * or at the end of the URL if there is none.
     */
    
    if (url.contains(";jsessionid=")) {
      url = url.replaceFirst(";jsessionid=[^?]+", "");
    }
    
    /* 
     * It's not clear if this is a temporary (error) or if it's in transition
     * but across all of ingenta all article landing pages are now at 
     * contentone instead of content (though both pages still exist).
     * In order not to collect duplicates, we will normalize
     * /contentone/ to /content/
     * 
     */
    if (url.contains("/contentone/")) {
      url = url.replace("/contentone/", "/content/");
    }
    
    /*
     * The IngentaConnect platform is organized somewhat like an
     * interactive process, whereby one must click through to articles
     * and obtain one-time or short-lived URLs on the way. An
     * alternate access mechanism is in place for robots and
     * crawlers; see the HTML link extractor for that. Now the URL
     * normalizer needs to intercept requests for the gateway URLs
     * that lead to one-time or short-lived URLs and translate them
     * into the URL that is actually extracted and preserved.
     */

    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    final String INFOBIKE_PATH = "search/download?pub=infobike%3a%2f%2f";
    String baseUrlPrefix = baseUrl + INFOBIKE_PATH;
    if (StringUtils.containsIgnoreCase(url, INFOBIKE_PATH)) {
      // First remove the exitTargetId
      if (url.contains("&exitTargetId=")) {
        url = url.replaceFirst("&exitTargetId=[^&]+", "");
      }
      
      // Then massage the path to include slashes and "?crawler=true"
      // replace &mimetype=application/pdf (assumed default) see IngentaHtmlLinkRewriterFactory
      // with ?crawler=true, otherwise add ?crawler=true to mimetype param
      String path = url.substring(baseUrlPrefix.length());
      path = path.replaceAll("%2[Ff]", "/");
      path = path.replaceFirst("&mimetype=application/pdf", "?crawler=true");
      path = path.replaceFirst("&mimetype=", "?crawler=true&mimetype=");
      
      // Now construct the URL
      String apiUrl = au.getConfiguration().get(KEY_API_URL);
      url = apiUrl + "content/" + path;
    }
    
    return url;
  }

}
