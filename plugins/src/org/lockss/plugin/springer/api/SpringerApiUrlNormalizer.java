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

package org.lockss.plugin.springer.api;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class SpringerApiUrlNormalizer implements UrlNormalizer {
  //Original: http://link.springer.com/content/pdf/10.1186%2F1472-6947-8-61.pdf
  //Norm this to Orig: http://download.springer.com/static/pdf/28/art%253A10.1186%252F1472-6947-8-61.pdf?auth66=1421354172_185780df15b847df3052ec9a904a3b12&ext=.pdf
  //
  protected static final Logger log = Logger.getLogger(SpringerApiUrlNormalizer.class);
  public static final String ENDS_WITH = "&ext=.pdf";
  
  
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    if (!url.startsWith(SpringerApiCrawlSeed.CDN_URL)
        || !url.endsWith(ENDS_WITH)) {
      if (log.isDebug3()) {
        log.debug3(String.format("Non-matching: %s in %s", url, au.getName()));
      }
      return url;
    }
    String doi = StringUtils.substringBetween(url, "/art%253A", ".pdf?auth66");
    String ret = SpringerApiCrawlSeed.CDN_URL + "content/pdf/" + doi + ".pdf";
    return ret;
  }
  
}
