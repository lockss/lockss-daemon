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

//public class RSCBooksUrlNormalizer implements UrlNormalizer {
public class RSCBooksUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {

  private static final Logger log = Logger.getLogger(RSCBooksUrlNormalizer.class);
  
  /*  Note: this assumes that all AUs have same params, this way we set the urls once
   *       param[base_url] = http://pubs.rsc.org/
   *       param[resolver_url] = http://xlink.rsc.org/
   */
  private static String content_url = "";
  
  //public String normalizeUrl(String url, ArchivalUnit au)
  public String additionalNormalization(String url, ArchivalUnit au)
      throws PluginException {
    
    if (content_url.isEmpty()) {
      content_url = au.getConfiguration().get("base_url") + "en/content/";
    }
    if (StringUtil.startsWithIgnoreCase(url, content_url)) {
      url = StringUtils.lowerCase(url);
    }
    return url;
  }

}
