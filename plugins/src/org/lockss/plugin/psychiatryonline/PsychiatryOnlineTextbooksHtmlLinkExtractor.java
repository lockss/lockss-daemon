/*
 * $Id: PsychiatryOnlineTextbooksHtmlLinkExtractor.java,v 1.4 2008-02-08 01:15:11 thib_gc Exp $
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
import org.lockss.config.Configuration.InvalidParam;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

public class PsychiatryOnlineTextbooksHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  public PsychiatryOnlineTextbooksHtmlLinkExtractor() {
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

      // Try javascript:windowReference('Reference', 'popup.aspx...')
      Pattern popupPattern = getPopupPattern();
      if (popupPattern != null && matcher.contains(href, popupPattern)) {
        String popupLink = interpretPopupMatch(matcher.getMatch());
        if (logger.isDebug3()) {
          logger.debug3("AU: "
                        + au.getName()
                        + ", match: "
                        + popupLink);
        }
        return popupLink;
      }

      // Try javascript:windowReference('', 'bookInfo.aspx...')
      Pattern bookInfoPattern = getBookInfoPattern(au);
      if (bookInfoPattern != null && matcher.contains(href, bookInfoPattern)) {
        String bookInfoLink = interpretBookInfoMatch(matcher.getMatch());
        if (logger.isDebug3()) {
          logger.debug3("AU: "
                        + au.getName()
                        + ", match: "
                        + bookInfoLink);
        }
        return bookInfoLink;
      }
    }

    return super.extractLinkFromTag(link, au, cb);
  }

  protected static Logger logger = Logger.getLogger("PsychiatryOnlineTextbooksHtmlLinkExtractor");

  protected static PatternCache patternCache = new PatternCacheLRU(4, new Perl5Compiler());

  public static Pattern getBookInfoPattern(ArchivalUnit au) {
    String regex = null;
    try {
      synchronized (patternCache) {
        final String regex1 = "javascript[^:]*:[^w]*windowReference[^(]*\\([^\"']*[\"'][\"'][^,]*,[^\"']*[\"'](bookInfo\\.aspx\\?file=\\w+";
        String regex2 = Integer.toString(au.getConfiguration().getInt("resource_id"));
        final String regex3 = "\\.html)[^\"']*[\"'][^)]*\\)[^;]*;";
        regex = regex1 + regex2 + regex3;
        return patternCache.getPattern(regex,
                                       Perl5Compiler.READ_ONLY_MASK);
      }
    }
    catch (InvalidParam ipe) {
      logger.error("InvalidParam exception for AU "
                   + au.getName()
                   + " ["
                   + au.getConfiguration().toString()
                   + "]",
                   ipe);
      return null;
    }
    catch (MalformedCachePatternException mcpe) {
      logger.error("Unexpected MalformedCachePatternException for AU "
                   + au.getName()
                   + ", pattern: "
                   + regex,
                   mcpe);
      return null;
    }
  }

  public static Pattern getPopupPattern() {
    synchronized (patternCache) {
      final String regex = "javascript[^:]*:[^w]*windowReference[^(]*\\([^\"']*[\"']Reference[\"'][^,]*,[^\"']*[\"'](popup\\.aspx\\?aID=)\\D*(\\d+)[^\"']*[\"'][^)]*\\)[^;]*;";
      return patternCache.getPattern(regex, Perl5Compiler.READ_ONLY_MASK);
    }
  }

  public static String interpretBookInfoMatch(MatchResult bookInfoMatch) {
    if (bookInfoMatch.groups() != 2) {
      logger.warning("Internal inconsistency: '"
                     + bookInfoMatch.toString()
                     + "' has "
                     + (bookInfoMatch.groups() - 1)
                     + " groups (expected 1)");
      if (bookInfoMatch.groups() < 2) {
        return null;
      }
    }
    return "/" + bookInfoMatch.group(1);
  }

  public static String interpretPopupMatch(MatchResult popupMatch) {
    if (popupMatch.groups() != 3) {
      logger.warning("Internal inconsistency: '"
                     + popupMatch.toString()
                     + "' has "
                     + (popupMatch.groups() - 1)
                     + " groups (expected 2)");
      if (popupMatch.groups() < 3) {
        return null;
      }
    }
    return "/" + popupMatch.group(1) + popupMatch.group(2);
  }

}
