/*
 * $Id: OJS2HtmlLinkExtractor.java,v 1.1 2008-10-28 09:19:07 thib_gc Exp $
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

  public OJS2HtmlLinkExtractor() {
    super();
  }
  
  @Override
  protected String extractLinkFromTag(StringBuffer link,
                                      ArchivalUnit au,
                                      Callback cb)
      throws IOException {
    char ch = link.charAt(0);
    if ((ch == 'a' || ch == 'A') && Character.isWhitespace(link.charAt(1))) {
      // <a href="...">
      String href = getAttributeValue(HREF, link);
      if (href == null) {
        return null;
      }
      
      PatternMatcher matcher = RegexpUtil.getMatcher();

      // javascript:openRTWindow(url);
      Pattern openRTWindowPattern = getOpenRTWindowPattern();
      if (openRTWindowPattern != null && matcher.contains(href, openRTWindowPattern)) {
        String openRTWindowLink = interpretRTOpenWindowMatch(matcher.getMatch());
        if (logger.isDebug3()) {
          logger.debug3("AU: "
                        + au.getName()
                        + ", openRTWindow match: "
                        + openRTWindowLink);
        }
        return openRTWindowLink;
      }

    }

    return super.extractLinkFromTag(link, au, cb);
  }
  
  protected static Logger logger = Logger.getLogger("OJS2HtmlLinkExtractor");

  protected static final Pattern OPEN_RT_WINDOW_PATTERN = RegexpUtil.uncheckedCompile("javascript:openRTWindow\\('([^']*)'\\);",
                                                                                      Perl5Compiler.READ_ONLY_MASK);

  public static Pattern getOpenRTWindowPattern() {
    return OPEN_RT_WINDOW_PATTERN;
  }

  public static String interpretRTOpenWindowMatch(MatchResult openRTWindowMatch) {
    if ((openRTWindowMatch.groups() - 1) != 1) {
      logger.warning("Internal inconsistency: openRTWindow match '"
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
