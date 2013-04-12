/*
 * $Id: OJS2HtmlLinkExtractor.java,v 1.2 2013-04-12 23:35:59 pgust Exp $
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

import org.apache.oro.text.regex.*;
import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

public class OJS2HtmlLinkExtractor extends JsoupHtmlLinkExtractor {

  protected static Logger logger = Logger.getLogger("OJS2HtmlLinkExtractor");

  protected static final Pattern OPEN_RT_WINDOW_PATTERN = 
      RegexpUtil.uncheckedCompile("javascript:openRTWindow\\('([^']*)'\\);",
                                  Perl5Compiler.READ_ONLY_MASK);

  static class ATagLinkExtractor extends BaseLinkExtractor {
    public void tagBegin(final Node node, final ArchivalUnit au, final Callback cb) {
      if (node.hasAttr("href")) {
        String url = node.attr("href");
        String newUrl = getLink(url);
        cb.foundLink(getLink(newUrl));
      }
    }
  }
  
  static class FormTagLinkExtractor extends BaseLinkExtractor {
    public void tagBegin(final Node node, final ArchivalUnit au, final Callback cb) {
      if (node.hasAttr("action")) {
        String url = node.attr("action");
        String newUrl = getLink(url);
        cb.foundLink(getLink(newUrl));
      }
    }
  }
  
  static class MetaTagLinkExtractor extends BaseLinkExtractor {
    public void tagBegin(final Node node, final ArchivalUnit au, final Callback cb) {
      if ("refresh".equalsIgnoreCase(node.attr("http-equiv"))) {
        if (node.hasAttr("content")) {
          String value = node.attr("content");
          int i = value.indexOf(";url=");
          if (i >= 0) {
            String tagPrefix = value.substring(0, i + 5);
            String url = value.substring(tagPrefix.length());
            String newUrl = getLink(url);
            cb.foundLink(getLink(newUrl));
          }
        }
      }
    }
  }
  
  public OJS2HtmlLinkExtractor() {
    super();
    registerTagExtractor("a", new ATagLinkExtractor());
    registerTagExtractor("form", new FormTagLinkExtractor());
    registerTagExtractor("meta", new MetaTagLinkExtractor());
  }
  
  public static String getLink(String link) {
    PatternMatcher matcher = RegexpUtil.getMatcher();

    // javascript:openRTWindow(url);
    if (OPEN_RT_WINDOW_PATTERN != null && matcher.contains(link, OPEN_RT_WINDOW_PATTERN)) {
      MatchResult openRTWindowMatch = matcher.getMatch();
      if (openRTWindowMatch.groups() == 2) {
        link = openRTWindowMatch.group(1);
      }
    }
    return link;
  }
}
