/*
 * $Id: OJS2HtmlLinkExtractor.java,v 1.6 2015-01-30 22:22:00 etenbrink Exp $
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

package org.lockss.plugin.ojs2;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.oro.text.regex.*;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

public class OJS2HtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  protected static final Logger log = Logger.getLogger(OJS2HtmlLinkExtractor.class);
  
  protected static final String NAME = "name";
  protected static final String PDF_NAME = "citation_pdf_url";
  protected static final String FT_NAME = "citation_fulltext_html_url";
  protected static final String CONTENT = "content";

  protected static final org.apache.oro.text.regex.Pattern OPEN_RT_WINDOW_PATTERN = 
      RegexpUtil.uncheckedCompile("javascript:openRTWindow\\('([^']*)'\\);",
                                  Perl5Compiler.READ_ONLY_MASK);
  protected static final java.util.regex.Pattern JLA_ARTICLE_PATTERN = Pattern.compile(
      "(http://www[.]logicandanalysis[.]org/index.php/jla/article/view/[\\d]+)/[\\d]+$");
  
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
            Matcher mat = JLA_ARTICLE_PATTERN.matcher(href);
            if (mat.find()) {
              String url = mat.group(1);
              cb.foundLink(url);
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
          if (REFRESH.equalsIgnoreCase(getAttributeValue(HTTP_EQUIV, link))) {
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
          } else {
            if (PDF_NAME.equalsIgnoreCase(getAttributeValue(NAME, link)) ||
                FT_NAME.equalsIgnoreCase(getAttributeValue(NAME, link))) {
              
              String url = getAttributeValue(CONTENT, link);
              if (url != null) {
                cb.foundLink(url);
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

