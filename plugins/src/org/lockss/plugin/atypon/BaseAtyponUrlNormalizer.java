/*
 * $Id: BaseAtyponUrlNormalizer.java,v 1.2 2013-07-01 22:18:05 alexandraohlson Exp $
 */
/*
 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, destroy, sublicense, and/or sell
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
package org.lockss.plugin.atypon;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;


public class BaseAtyponUrlNormalizer implements UrlNormalizer {

  protected static final String SUFFIX = "?cookieSet=1";
  
  // IN PROGRESS - In order to support the needs of all the Atypon children, this needs to change
  // to an implementation with some abstract methods so that the child can set a list or set
  // of parameters that need to be included or excluded for action/downloadCitation
  // We want to end up with .../action/downloadCitation?doi=blah%2Fblah&format=ris&include=cit
  // but different child plugins have variations on what their forms produce, such as:  

  //downloaded Citations have extra things we want to get rid of
  protected static final String CITATION_DOWNLOAD_URL = "action/downloadCitation"; 
  protected static final String DOWNLOAD_NAME = "&downloadFileName=";
  // take off even the include=cit because we may have to add &format=ris on first
  protected static final String CIT_SIAM_SUFFIX = "&include=cit&submit=Download+publication+citation+data";
  protected static final String CIT_FS_SUFFIX = "&include=cit&submit=Download+article+metadata";
  

  /* 
   *  Several Atypon plugins do this
   * A child could choose to avoid this entirely by setting it to org.lockss.util.Default
   * or they could write their own child implementation.
   */
  public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {
    
    // The following is only important f
    if (url.contains(CITATION_DOWNLOAD_URL)) {
      if (url.contains(DOWNLOAD_NAME)) {
        url = url.replaceFirst("&downloadFileName=[^&]+", "");
      }
      url = StringUtils.chomp(url, CIT_SIAM_SUFFIX);
      url = StringUtils.chomp(url,CIT_FS_SUFFIX);
      // NOW, if we don't already have &format=, then add it on as a RIS
      if (!(url.contains("&format="))) {
        url= url + "&format=ris";       
      }
      if (!(url.contains("&include=cit"))) {
        url = url + "&include=cit";
      }
      
    }
    return StringUtils.chomp(url, SUFFIX);
  }

}
