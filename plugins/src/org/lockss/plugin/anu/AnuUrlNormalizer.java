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

package org.lockss.plugin.anu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

public class AnuUrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  
  protected static final Logger log = Logger.getLogger(AnuUrlNormalizer.class);
  
  protected static final String REPL_STR = "[?].+$";
  protected static final String CSS_SUFFIX = ".css?";
  protected static final String JS_SUFFIX = ".js?";
  
  protected static final String FID_PARAM = "?field_id_value=";
  protected static final String ITOK_PARAM = "?itok=";
  protected static final String REFR_PARAM = "?referer=";
  
  protected static final String PAGE_PARAM = "page=";
  protected static final Pattern PAGE_PAT = Pattern.compile("(?<!xhtml)[?].*(page=[0-9]+)", Pattern.CASE_INSENSITIVE);
  
  
  @Override
  public String additionalNormalization(String url, ArchivalUnit au)
      throws PluginException {
    if (url.contains(PAGE_PARAM)) {
      Matcher mat = PAGE_PAT.matcher(url);
      if (mat.find()) {
        url = url.replaceFirst(REPL_STR, "?" + mat.group(1));
      }
    }
    if (url.contains(CSS_SUFFIX) ||
        url.contains(JS_SUFFIX) ||
        url.contains(FID_PARAM) ||
        url.contains(ITOK_PARAM) ||
        url.contains(REFR_PARAM)) {
      url = url.replaceFirst(REPL_STR, "");
    }
    
    return(url);
  }


  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    /* NOTE: 
     * Adding special handling to normalize http://press-files.anu.edu.au/ 
     * as well as https://press.anu.edu.au/
     * Check for same host or "press-files.anu.edu.au", then normalize
     * This should be safe as wget of any https://press-files.anu.edu.au files did not redirect, just returned content with 200
     * Also, if the press-files.anu.edu.au site changes protocol, we will not collect both versions of the files
     */
    if (UrlUtil.isSameHost(au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()), url) ||
        url.contains("://press-files.anu.edu.au")) {
      url = AuUtil.normalizeHttpHttpsFromBaseUrl(au, url);
    }
    return additionalNormalization(url, au);
  }
}
