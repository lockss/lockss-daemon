/*
 * $Id: 
 */

/*
Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

Note: This class is almost exactly the same as PsychiatryOnlineHtmlLinkExtractor.
*/

package org.lockss.plugin.edinburgh;

import java.io.*;

import org.apache.oro.text.regex.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class EdinburghUniversityPressHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  public EdinburghUniversityPressHtmlLinkExtractor() {
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

  @Override
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
      throws IOException {
    try {
      FilterFactory filter = new EdinburghUniversityPressHtmlFilterFactory();
      InputStream filtered = filter.createFilteredInputStream(au, in, encoding);
      super.extractUrls(au, filtered, encoding, srcUrl, cb);
    }
    catch (PluginException pe) {
      // Use new IOException constructor in Java 6
      IOException ioe = new IOException();
      ioe.initCause(pe);
      throw ioe;
    }
  }

  protected static Logger logger = Logger.getLogger("PsychiatryOnlineHtmlLinkExtractor");

//  protected static final Pattern WINDOW_REFERENCE_PATTERN = RegexpUtil.uncheckedCompile("javascript.*:.*windowReference.*\\([^']*'(?:\\.|[^'\\\\])*'.*,[^']*'(\\.|[^'\\\\])*'.*\\).*;",
//      Perl5Compiler.READ_ONLY_MASK);

  protected static final Pattern WINDOW_REFERENCE_PATTERN = RegexpUtil.uncheckedCompile("javascript[^:]*:.*windowReference[^(]*\\([^']*'(?:[^']*)'[^,]*,[^']*'([^']*)'[^)]*\\)[^;]*;",
                                                                                        Perl5Compiler.READ_ONLY_MASK);
  
  public static Pattern getWindowReferencePattern() {
    return WINDOW_REFERENCE_PATTERN;
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
