/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.highwire;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class HighWirePressH20UrlNormalizer extends BaseUrlHttpHttpsUrlNormalizer {
  
  protected static final Logger log = Logger.getLogger(HighWirePressH20UrlNormalizer.class);
  protected static final String SID_PARAM = "?sid=";
  protected static final String SID_PATTERN_STR = "[?]sid=.+$";
  protected static final String SEARCH_STR = "search?submit=yes&";
  
  protected static final String SORT_PARAM = "sortspec";
  protected static final String TOC_SEC_ID_PARAM = "tocsectionid";
  protected static final String VOL_PARAM = "volume";
  protected static final String ISSUE_PARAM = "issue";
  protected static final String FIRSTINDEX_PARAM = "FIRSTINDEX";
  
  protected static final Set<String> KNOWN_PARAMS = new HashSet<String>(Arrays.asList(
      SORT_PARAM,
      TOC_SEC_ID_PARAM,
      VOL_PARAM,
      ISSUE_PARAM,
      FIRSTINDEX_PARAM
    ));
  
  @Override
  public String additionalNormalization(String url, ArchivalUnit au)
      throws PluginException {
    
    if (url.contains(SID_PARAM)) {
      url = url.replaceFirst(SID_PATTERN_STR, "");
    }
    
    if (url.contains(SEARCH_STR)) {
      // We need to specify order of arguments, as the site can change the order
      // We need to make sure we don't get redundant URLs due to "found" links that are
      // the same but for ordering. Note: at minimum must have TOC_SEC_ID_PARAM
      
      int smark = url.indexOf("&");
      
      StringBuilder newUrl = new StringBuilder(url.substring(0, smark));
      Map<String, String> query = parseQueryString(url.substring(smark + 1));
      String val;
      
      val = query.get(SORT_PARAM);
      if (val != null) {
        newUrl.append("&" + SORT_PARAM + "=" + val);
      }
      
      val = query.get(TOC_SEC_ID_PARAM);
      if (val == null) return url;
      newUrl.append("&" + TOC_SEC_ID_PARAM + "=" + val);
      
      val = query.get(VOL_PARAM);
      if (val != null) {
        newUrl.append("&" + VOL_PARAM + "=" + val);
      }
      
      val = query.get(ISSUE_PARAM);
      if (val != null) {
        newUrl.append("&" + ISSUE_PARAM + "=" + val);
      }
      
      val = query.get(FIRSTINDEX_PARAM);
      if (val != null && !val.equals("0")) {
        newUrl.append("&" + FIRSTINDEX_PARAM + "=" + val);
      }
      
      url = newUrl.toString();
      log.debug3(url);
      return url;
      
    }
    return url;
  }
  
  protected static Map<String, String> parseQueryString(String queryString) {
    queryString = queryString.replace("%26", "&");
    Map<String, String> ret = new HashMap<String, String>();
    
    for (String pair : queryString.split("&")) {
      log.debug3("pair: " + pair);
      int eq = pair.indexOf("=");
      String key;
      String val;
      if (eq < 0 || eq == pair.length() - 1) {
        key = pair;
        val = null;
      }
      else {
        key = pair.substring(0, eq);
        val = pair.substring(eq + 1);
      }
      if (!KNOWN_PARAMS.contains(key)) {
        log.debug(key + " not in known params");
      }
      ret.put(key, val);
    }
    
    return ret;
  }
  
}
