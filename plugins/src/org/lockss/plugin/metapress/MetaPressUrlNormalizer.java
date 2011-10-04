/*
 * $Id: MetaPressUrlNormalizer.java,v 1.1 2011-10-04 10:43:59 thib_gc Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;


public class MetaPressUrlNormalizer implements UrlNormalizer {

  protected static final String[] QUERY_DISQUALIFY = new String[] {
    "sortorder=",
  };
  
  protected static final String[] QUERY_REMOVE = new String[] {
    "p=",
    "pi=",
    "p_o=",
    "mark=",
    "sw=",
  };
  
  public static String normalizeQuery(String query) {
    // Empty query string: empty result
    if (query == null || query.length() == 0) {
      return "";
    }
    
    // Disqualifying prefix: empty result
    for (String prefix : QUERY_DISQUALIFY) {
      if (query.startsWith(prefix)) {
        return "";
      }
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
