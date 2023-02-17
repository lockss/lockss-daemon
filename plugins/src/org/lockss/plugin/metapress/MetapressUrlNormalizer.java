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

package org.lockss.plugin.metapress;

import java.util.*;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;

public class MetapressUrlNormalizer implements UrlNormalizer {
  
  /*
   * keys we want to remove from the query
   * p: some random number/letter - seems to be date/time marker, not necessary to view item
   * pi: related to issue numbering to track previous/next. Not necessary to view
   * For example of above:
   * http://inderscience.metapress.com/content/m1804w66tn802h54/?p=9bc3f3743d4f48d5ac0793c7ab0d2ccf&pi=3
   * http://inderscience.metapress.com/content/m1804w66tn802h54
   * mark: action to add this item to list of marked items, not a unique page
   * sw: ??
   *
   * arguments we DON'T want to remove
   *    a=# (which language to present article text in, 2011+)
   *    k=# (which language to present keywords in, 2011+)
   *    example: http://liverpool.metapress.com/content/5quu123154411492/?k=9&a=12
   *  Turns out we need
   *    p_o: related to pages of articles in TOC, where p_o != 0
   */
  protected static final String[] REMOVED_KEYS = {
    "mark", "p", "pi", "p_o", "sw", "sortorder"
  };
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    if (au.getStartUrls().contains(url)) {
      // DO NOT NORMALIZE THE START URL (same as the crawler's behavior)
      return url;
    }

    int questionMark = url.indexOf('?');
    if (questionMark < 0) {
      return url;
    }
    
    String query = url.substring(questionMark + 1);
    String normalizedQuery = normalizeQuery(query);
    if (normalizedQuery == null || normalizedQuery.length() == 0) {
      return url.substring(0, questionMark);
    }
    return url.substring(0, questionMark + 1) + normalizedQuery;
  }
  
  public String normalizeQuery(String query) {
    Map<String, String> map = parseQuery(query);
    for (String removedKey : REMOVED_KEYS) {
      if ("p_o".equals(removedKey) && !"0".equals(map.get(removedKey))) {
        continue; // keep non-zero p_0 values
      }
      map.remove(removedKey);
    }
    return outputQuery(map);
  }

  public static Map<String, String> parseQuery(String query) {
    // Empty query string: empty result
    if (query == null || query.length() == 0) {
      return Collections.emptyMap();
    }

    Map<String, String> map = new HashMap<String, String>();
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      if (pair.length() == 0) {
        continue;
      }
      int e = pair.indexOf('=');
      if (e < 0) {
        map.put(pair, "");
      }
      else {
        map.put(pair.substring(0, e), pair.substring(e + 1));
      }
    }
    return map;
  }

  public static String outputQuery(Map<String, String> map) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String key : new TreeSet<String>(map.keySet())) {
      if (first) {
        first = false;
      }
      else {
        sb.append('&');
      }
      sb.append(key);
      sb.append('=');
      sb.append(map.get(key));
    }
    return sb.toString();
  }
  
  public static void main(String[] args) throws Exception {
    UrlNormalizer norm = new MetapressUrlNormalizer();
    for (String u : Arrays.asList("http://liverpool.metapress.com/openurl.asp?genre=volume&eissn=1478-3398&volume=83",
                                  "http://liverpool.metapress.com/openurl.asp?eissn=1478-3398&genre=volume&volume=83")) {
      System.out.format("from: %s%n  to: %s%n%n", u, norm.normalizeUrl(u, null));
    }
  }
  
}
