/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
