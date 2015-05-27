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

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.ArchivalUnit;

/**
 * @since 1.67.5
 */
public class ProjectMuseUtil {

  /**
   * @since 1.67.5
   */
  public static final String HTTP = "http://";

  /**
   * @since 1.67.5
   */
  public static final String HTTPS = "https://";

  /**
   * @since 1.67.5
   */
  public static boolean isBaseUrlHttp(ArchivalUnit au) {
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    return baseUrl != null && baseUrl.startsWith(HTTP);
  }

  /**
   * @since 1.67.5
   */
  public static boolean isBaseUrlHttps(ArchivalUnit au) {
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    return baseUrl != null && baseUrl.startsWith(HTTPS);
  }

  public static boolean isBaseUrlHost(ArchivalUnit au, String url) {
    if (url != null) {
      String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
      baseUrl = baseUrl.replace(HTTPS, HTTP);
      url = url.replace(HTTPS, HTTP);
      return url.startsWith(baseUrl);
    }
    return false;
  }

  /**
   * <p>
   * If the AU is HTTP-based and the given URL is HTTPS, converts it to an
   * equivalent HTTP URL and
   * If the AU is HTTPS-based and the given URL is HTTP, converts it to an
   * equivalent HTTPS URL, otherwise returns the URL unchanged.
   * </p>
   * 
   * @since 1.67.5
   */
  public static String baseUrlHostCheck(ArchivalUnit au, String url) {
    if (isBaseUrlHost(au, url)) {
      if (isBaseUrlHttp(au) && url.startsWith(HTTPS)) {
        url = HTTP + url.substring(HTTPS.length());
      } else if (isBaseUrlHttps(au) && url.startsWith(HTTP)) {
        url = HTTPS + url.substring(HTTP.length());
      }
    }
    return url;
  }
  
  /**
   * <p>
   * Prevents instantiation.
   * </p>
   * 
   * @since 1.67.5
   */
  private ProjectMuseUtil() {
    // Prevent instantiation
  }
  
}
