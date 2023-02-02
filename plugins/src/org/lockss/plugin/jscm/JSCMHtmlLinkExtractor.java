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

package org.lockss.plugin.jscm;

import java.io.IOException;

import org.apache.oro.text.regex.*;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

public class JSCMHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  public JSCMHtmlLinkExtractor() {
    super();
  }
  
  @Override
  protected String extractLinkFromTag(StringBuffer link,
                                      ArchivalUnit au,
                                      Callback cb)
      throws IOException {
    char ch = link.charAt(0);
    if ((ch == 'a' || ch == 'A') && Character.isWhitespace(link.charAt(1))) {
      // <a onClick="...">
      String onClick = getAttributeValue("onClick", link);
      if (onClick != null) {
      
	PatternMatcher matcher = RegexpUtil.getMatcher();

	// MM_openBrWindow('url' ...
	Pattern openBrWindowPattern = getBrOpenWindowPattern();
	if (openBrWindowPattern != null && matcher.contains(onClick, openBrWindowPattern)) {
	  String openWindowLink = interpretOpenWindowMatch(matcher.getMatch());
	  if (logger.isDebug3()) {
	    logger.debug3("AU: "
			  + au.getName()
			  + ", MM_openBrWindow match: "
			  + openWindowLink);
	  }
	  return openWindowLink;
	}
      }
    }

    return super.extractLinkFromTag(link, au, cb);
  }
  
  protected static Logger logger = Logger.getLogger("JSCMHtmlLinkExtractor");

  protected static final int GROUP_COUNT = 1;
  protected static final Pattern OPEN_BR_WINDOW_PATTERN = RegexpUtil.uncheckedCompile("MM_openBrWindow\\([^']*'([^']*)'",
                                                                                   Perl5Compiler.READ_ONLY_MASK);

  public static Pattern getBrOpenWindowPattern() {
    return OPEN_BR_WINDOW_PATTERN;
  }

  public static String interpretOpenWindowMatch(MatchResult openWindowMatch) {
    if ((openWindowMatch.groups() - 1) != GROUP_COUNT) {
      logger.warning("Internal inconsistency: MM_openBrWindow match '"
          + openWindowMatch.toString()
          + "' has "
          + (openWindowMatch.groups() - 1)
          + " proper subgroups; expected "
	  + GROUP_COUNT);
      if ((openWindowMatch.groups() - 1) < GROUP_COUNT) {
        return null;
      }
    }
    return openWindowMatch.group(1);
  }

}
