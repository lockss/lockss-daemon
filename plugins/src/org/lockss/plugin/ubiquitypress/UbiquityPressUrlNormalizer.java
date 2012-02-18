/*
 * $Id: UbiquityPressUrlNormalizer.java,v 1.1 2012-02-18 00:57:50 akanshab01 Exp $
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
 * $Id: UbiquityPressUrlNormalizer.java,v 1.1 2012-02-18 00:57:50 akanshab01 Exp $
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

public class UbiquityPressUrlNormalizer implements UrlNormalizer {
 
  @Override
   public String normalizeUrl(String url,ArchivalUnit au)
      throws PluginException {
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL
                                          .getKey());
    String journalCode = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID
                                              .getKey());
    return normalizeProtectedUrl(url,baseUrl,journalCode) ; 
  }
  
  /**Describes a public normalizer method which makes call to the 
   *   normalizeProtectedUrl Method.
   * 
   * @param url
   * @param baseUrl
   * @param journalCode
   * @return normalizeUrl method which converts short urls to actual urls defined
   *    in the plugin xml file.
   * @throws PluginException
   */
  
  public String normalizeUrl(String url,String baseUrl,String journalCode)
      throws PluginException {
    return normalizeProtectedUrl(url,baseUrl,journalCode) ; 
  }
  
  
  /**Defines the protected normalize method  which will normalize the url into 
   *   the correct format.
   * 
   * @param url
   * @param baseUrl
   * @param journalCode
   * @return normalized url with the appended index.php and journal code.
   */
  
  protected String normalizeProtectedUrl(String url,String baseUrl,
      String journalCode){
    String returnVal = ""; 
    if(url.contains(baseUrl)) {
     // for eg. url refers to http://www.presentpasts.info/article/view/pp.52/92 
    String[] splitArr = url.split(baseUrl);
    // urlDetailInfo = article/view/pp.52/92 will be extracted from url
    String urlDetailInfo = splitArr[1];
    if(journalCode != null && urlDetailInfo != null ){
    StringBuilder buffer = new StringBuilder(baseUrl + "index.php");
    buffer.append("/");
    buffer.append(journalCode);
    buffer.append("/");
    buffer.append(urlDetailInfo);
    returnVal = buffer.toString(); 
    return returnVal;
    }
    else{
    return returnVal;
    }
    }
    return null;
    }
  }
