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

package org.lockss.plugin.ubiquitypress;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.util.StringUtil;

public class UbiquityPressUrlNormalizer implements UrlNormalizer {

  protected String baseUrl;
  
  protected String journalId;
  
  @Override
   public String normalizeUrl(String url,ArchivalUnit au)
      throws PluginException {
    if (this.baseUrl == null) {
      this.baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
      this.journalId = au.getConfiguration().get(ConfigParamDescr.JOURNAL_ID.getKey());
    }
    return doNormalize(url, baseUrl, journalId) ;
  }
  
  /**Defines the protected normalize method  which will normalize the url into 
   *   the correct format.
   * 
   * @param url
   * @param baseUrl
   * @param journalId
   * @return normalized url with the appended index.php and journal code.
   */
  protected String doNormalize(String url,
                               String baseUrl,
                               String journalId) {
    if (   StringUtil.isNullString(journalId) 
        || StringUtil.isNullString(baseUrl)
        || !StringUtil.startsWithIgnoreCase(url, baseUrl)) {
      return url;
    }
    
    // e.g. url is "http://www.presentpasts.info/article/view/pp.52/92" 
    // path is "article/view/pp.52/92"
    return String.format("%sindex.php/%s/%s",
                         baseUrl,
                         journalId,
                         url.substring(baseUrl.length()));
  }
}
