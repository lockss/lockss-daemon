/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.psychiatryonline;

import java.io.*;

import org.apache.oro.text.regex.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.*;
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

  @Override
  public void extractUrls(ArchivalUnit au,
                          InputStream in,
                          String encoding,
                          String srcUrl,
                          Callback cb)
      throws IOException {
    try {
      FilterFactory filter = new PsychiatryOnlineHtmlFilterFactory();
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
