/*
 * $Id: PsychiatryOnlineHtmlLinkExtractor.java,v 1.1 2008-02-12 06:52:47 thib_gc Exp $
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

package org.lockss.plugin.psychiatryonline;

import java.io.IOException;

import org.apache.oro.text.*;
import org.apache.oro.text.regex.*;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

public class PsychiatryOnlineHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  public PsychiatryOnlineHtmlLinkExtractor() {
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

      // javascript:windowReference(str1, str2)
      Pattern windowReferencePattern = getWindowReferencePattern();
      if (windowReferencePattern != null && matcher.contains(href, windowReferencePattern)) {
        String windowReferenceLink = interpretWindowReferenceMatch(matcher.getMatch());
        if (logger.isDebug3()) {
          logger.debug3("AU: "
                        + au.getName()
                        + ", windowReference match: "
                        + windowReferenceLink);
        }
        return windowReferenceLink;
      }

    }

    return super.extractLinkFromTag(link, au, cb);
  }

  protected static Logger logger = Logger.getLogger("PsychiatryOnlineHtmlLinkExtractor");

  protected static PatternCache patternCache = new PatternCacheLRU(4, new Perl5Compiler());

  public static Pattern getWindowReferencePattern() {
    synchronized (patternCache) {
      final String regex = "javascript.*:.*windowReference.*\\([^']*'(?:\\.|[^'\\])*'.*,[^']*'(\\.|[^'\\])*'.*\\).*;";
      return patternCache.getPattern(regex, Perl5Compiler.READ_ONLY_MASK);
    }
  }

  public static String interpretWindowReferenceMatch(MatchResult windowReferenceMatch) {
    if ((windowReferenceMatch.groups() - 1) != 1) {
      logger.warning("Internal inconsistency: windowReference match '"
                     + windowReferenceMatch.toString()
                     + "' has "
                     + (windowReferenceMatch.groups() - 1)
                     + " proper subgroups; expected 1");
      if ((windowReferenceMatch.groups() - 1) < 1) {
        return null;
      }
    }
    return windowReferenceMatch.group(1);
  }

}
