/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.Logger;

/*
 * With the implementation of a books version of the Ingenta plugin, take the
 * opportunity to clean up the URL normalizer without destabilizing the existing
 * one.
 * 
 * IngentaUrlNormalizer.java
 *  * 1. Simple normalizations:
 *    unnecessary";jesessionid=" and ends at the question mark for the URL query
 *    or at the end of the URL if there is none.
 *   
 * 2. Complicated transformations  
 *    turn one-time expiring URLS in to crawler friendly stable urls
 * 
 * pdf link: 
 *    /search/download?pub=infobike%3a%2f%2fbkpub%2f2ouacs%2f2015%2f00000001%2f00000001%2fart00001
 *       &mimetype=application%2fpdf&exitTargetId=1463607913143
 *    becomes
 *    http://www.ingentaconnect.com/content/bkpub/2ouacs/2015/00000001/00000001/art00001?crawler=true&mimetype=application/pdf       
 *     
 *   NOTE - the official crawler stable version is at api.ingentaconnect.com but it is also available at
 *   www.ingentaconnect.com so we create that one instead.
 */

public class IngentaBooksUrlNormalizer implements UrlNormalizer {
  private static final Logger log = Logger.getLogger(IngentaBooksUrlNormalizer.class);

  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {

    /*
     * An Ingenta URL may have a jsessionid in it, 
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
      
      String path = url.substring(baseUrlPrefix.length());
      path = path.replaceAll("%2[Ff]", "/");
      // replace &mimetype= with ?crawler=true&mimetype=
      path = path.replaceFirst("&mimetype=", "?crawler=true&mimetype=");
      
      // Now construct the URL
      url = baseUrl + "content/" + path;
    }
    return url;
  }

}
