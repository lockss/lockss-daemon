/*
 * $Id: OJS2HtmlLinkExtractor.java,v 1.3 2013-04-22 22:27:41 pgust Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs2;

import java.io.IOException;

import org.apache.oro.text.regex.*;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

public class OJS2HtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  protected static Logger log = Logger.getLogger(OJS2HtmlLinkExtractor.class);

  protected static final Pattern OPEN_RT_WINDOW_PATTERN = 
      RegexpUtil.uncheckedCompile("javascript:openRTWindow\\('([^']*)'\\);",
                                  Perl5Compiler.READ_ONLY_MASK);
  
  @Override
  protected String extractLinkFromTag(
      StringBuffer link, ArchivalUnit au, Callback cb) throws IOException {
    switch (link.charAt(0)) {
      case 'a':
      case 'A':
        if (beginsWithTag(link,"a")) {
          // <a href="...">
          String href = getAttributeValue(HREF, link);
          if (href != null) {
            // javascript:openRTWindow(url);
            PatternMatcher matcher = RegexpUtil.getMatcher();
            if (   OPEN_RT_WINDOW_PATTERN != null 
                && matcher.contains(href, OPEN_RT_WINDOW_PATTERN)) {
              String openRTWindowLink = 
                  interpretRTOpenWindowMatch(matcher.getMatch());
              return openRTWindowLink;
            }
          }
        }
        break;
        
      case 'f':
      case 'F':
        if (beginsWithTag(link,"form")) {
          // <form action="...">
          String action = getAttributeValue("action", link);
          if (action != null) {
            // javascript:openRTWindow(url);
            PatternMatcher matcher = RegexpUtil.getMatcher();
            if (   OPEN_RT_WINDOW_PATTERN != null 
                && matcher.contains(action, OPEN_RT_WINDOW_PATTERN)) {
              String openRTWindowLink = 
                  interpretRTOpenWindowMatch(matcher.getMatch());
              return openRTWindowLink;
            }
          }
        }
        break;
        
      case 'm':
      case 'M':
        if (beginsWithTag(link,"meta")) {
          if ("refresh".equalsIgnoreCase(getAttributeValue(HTTP_EQUIV, link))) {
            String value = getAttributeValue(HTTP_EQUIV_CONTENT, link);
            if (value != null) {
              // <meta http-equiv="refresh" content="...">
              int i = value.indexOf(";url=");
              if (i >= 0) {
                String url = value.substring(i+5);
  
                // javascript:openRTWindow(url);
                PatternMatcher matcher = RegexpUtil.getMatcher();
                if (   OPEN_RT_WINDOW_PATTERN != null 
                    && matcher.contains(url, OPEN_RT_WINDOW_PATTERN)) {
                  String openRTWindowLink = 
                      interpretRTOpenWindowMatch(matcher.getMatch());
                  return openRTWindowLink;
                }
              }
            }
          }
        }
        break;
    }
      
    return super.extractLinkFromTag(link, au, cb);
  }
  
  private static String interpretRTOpenWindowMatch(MatchResult openRTWindowMatch) {
    if ((openRTWindowMatch.groups() - 1) != 1) {
      log.warning("Internal inconsistency: openRTWindow match '"
          + openRTWindowMatch.toString()
          + "' has "
          + (openRTWindowMatch.groups() - 1)
          + " proper subgroups; expected 1");
      if ((openRTWindowMatch.groups() - 1) < 1) {
        return null;
      }
    }
    return openRTWindowMatch.group(1);
  }

}

