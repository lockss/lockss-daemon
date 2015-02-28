/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
  
}
