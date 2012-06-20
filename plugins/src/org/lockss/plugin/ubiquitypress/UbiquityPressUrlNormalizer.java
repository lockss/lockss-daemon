/*
 * $Id: UbiquityPressUrlNormalizer.java,v 1.2.8.2 2012-06-20 00:02:56 nchondros Exp $
 */

/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

 *//*
 * $Id: UbiquityPressUrlNormalizer.java,v 1.2.8.2 2012-06-20 00:02:56 nchondros Exp $
 */

/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.plugin.ubiquitypress;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.util.StringUtil;

public class UbiquityPressUrlNormalizer implements UrlNormalizer {
 
  @Override
   public String normalizeUrl(String url,ArchivalUnit au)
      throws PluginException {
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL
                                          .getKey());
    String journalCode = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID
                                              .getKey());
    return normalizeUrl(url,baseUrl,journalCode) ; 
  }
  
  /**Defines the protected normalize method  which will normalize the url into 
   *   the correct format.
   * 
   * @param url
   * @param baseUrl
   * @param journalCode
   * @return normalized url with the appended index.php and journal code.
   */
  protected String normalizeUrl(String url,String baseUrl, String journalCode) {
    if (   StringUtil.isNullString(journalCode) 
        || StringUtil.isNullString(baseUrl)
        || !StringUtil.startsWithIgnoreCase(url, baseUrl)) {
      return url;
    }
    
     // for eg. url refers to http://www.presentpasts.info/article/view/pp.52/92 
    // urlDetailInfo = article/view/pp.52/92 will be extracted from url
    String urlDetailInfo = url.substring(baseUrl.length());
    StringBuilder buffer = new StringBuilder();
    buffer.append(baseUrl);
    buffer.append("index.php");
    buffer.append("/");
    buffer.append(journalCode);
    buffer.append("/");
    buffer.append(urlDetailInfo);
    return buffer.toString();
  }
}
