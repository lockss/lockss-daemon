/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.internationalunionofcrystallography.oai;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.UrlUtil;

/*
 * 
 * change  http://
 * to this http://
 * 
 * If and only if the AU is iucrdata
 */

public class IUCrOaiUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  
  private static final String TARGET = "http://journals.iucr.org/";
  private static final String IUCRDATA = "iucrdata";

  public static final String SCRIPT_URL = "script_url";
  
  /*  Note: Normalizes iucrdata urls with journals.iucr.org
   * 
   */
  
  public String additionalNormalization(String url, ArchivalUnit au)
      throws PluginException {
    
//    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
//    String oaiSet = au.getConfiguration().get(BaseOaiPmhCrawlSeed.KEY_AU_OAI_SET);

    // Same thing as BaseUrlHttpHttpsUrlNormalizer but for script_url
    if (UrlUtil.isSameHost(au.getConfiguration().get(SCRIPT_URL), url)) {
      url = AuUtil.normalizeHttpHttpsFromParamUrl(au, SCRIPT_URL, url);
    }
    
//    if (oaiSet.equalsIgnoreCase(IUCRDATA) && url.startsWith(TARGET)) {
//      url = url.replace(TARGET, baseUrl);
//    }
    return url;
  }

}
