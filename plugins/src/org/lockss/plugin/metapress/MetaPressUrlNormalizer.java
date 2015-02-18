/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.util.Logger;


public class MetaPressUrlNormalizer implements UrlNormalizer {
  
  protected static Logger logger = Logger.getLogger(MetaPressUrlNormalizer.class);
  
  // Should not disqualify query where like  &p_o=10 or &p_o=20 (these are pages on TOC)
  protected static Pattern PAT_DISQUALIFY =
      Pattern.compile("^sortorder=(?!.*[&amp;]p_o=[1-9])");
  
  /*
   * arguments we want to remove
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
  protected static final String[] QUERY_REMOVE = new String[] {
    "p=",
    "pi=",
    "p_o=0",
    "mark=",
    "sw=",
    "sortorder="
  };
  
  public static String normalizeQuery(String query) {
    // Empty query string: empty result
    if (query == null || query.length() == 0) {
      return "";
    }
    
    // Disqualifying prefix: empty result
    Matcher mat = PAT_DISQUALIFY.matcher(query);
    if (mat.find()) {
      return "";
    }
    
    // Potentially non-empty result
    StringBuilder sb = new StringBuilder(query.length());
    int begin = 0;
    
    while (begin < query.length()) {
      // Move past an ampersand
      if (query.charAt(begin) == '&') {
        ++begin;
        continue;
      }
      
      // Isolate the query component query.substring(begin, end)
      int end = query.indexOf('&', begin);
      if (end < 0) {
        end = query.length();
      }
      
      // Signal removable argument by moving 'begin' past 'end'
      for (String key : QUERY_REMOVE) {
        if (query.startsWith(key, begin)) {
          begin = end + 1;
          break;
        }
      }
      
      // If 'begin' still before 'end', output non-removable query component
      if (begin < end) {
        if (sb.length() != 0) {
          sb.append('&');
        }
        sb.append(query.substring(begin, end));
        begin = end + 1;
      }
    }
    
    return sb.toString();
  }
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    int questionMark = url.indexOf('?');
    if (questionMark < 0) {
      return url;
    }
    
    String query = url.substring(questionMark + 1);
    String simplifiedQuery = normalizeQuery(query);
    if (simplifiedQuery == null || simplifiedQuery.length() == 0) {
      return url.substring(0, questionMark);
    }
    
    return url.substring(0, questionMark + 1) + simplifiedQuery;
  }

}
