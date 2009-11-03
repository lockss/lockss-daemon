/*
 * $Id: IngentaUrlNormalizer.java,v 1.2.2.2 2009-11-03 23:52:02 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
      String path = url.substring(baseUrlPrefix.length());
      path = path.replaceAll("%2[Ff]", "/");
      path = path.replaceFirst("&mimetype=", "?crawler=true&mimetype=");
      
      // Now construct the URL
      String apiUrl = au.getConfiguration().get(KEY_API_URL);
      url = apiUrl + "content/" + path;
    }
    
    return url;
  }

}
