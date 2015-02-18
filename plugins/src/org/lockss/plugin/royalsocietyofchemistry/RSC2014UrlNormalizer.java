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
import org.lockss.util.StringUtil;

/*
 * Lower-case the url
 * change  http://pubs.rsc.org/en/Content/ArticleLanding/2009/GC/B822924D
 * to this http://pubs.rsc.org/en/content/articlelanding/2009/gc/b822924d
 * 
 * and http://xlink.rsc.org/?DOI=B712109A
 * to http://xlink.rsc.org/?doi=b712109a
 */

public class RSC2014UrlNormalizer implements UrlNormalizer {
  
  private static final Logger log = Logger.getLogger(RSC2014PdfFilterFactory.class);
  
  /*  Note: this assumes that all AUs have same params, this way we set the urls once
   *       param[base_url] = http://pubs.rsc.org/
   *       param[resolver_url] = http://xlink.rsc.org/
   */
  private static String content_url = "";
  private static String resolver_url = "";
  
  public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {
    
    if (content_url.isEmpty()) {
      content_url = au.getConfiguration().get("base_url") + "en/content/";
      resolver_url = au.getConfiguration().get("resolver_url");
    }
    if (StringUtil.startsWithIgnoreCase(url, content_url) || 
        StringUtil.startsWithIgnoreCase(url, resolver_url)) {
      url = StringUtils.lowerCase(url);
    }
    return url;
  }

}
