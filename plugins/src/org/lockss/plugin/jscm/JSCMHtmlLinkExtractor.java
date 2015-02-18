/*
 * $Id$
 */

/*

Copyright (c) 2010 Board of Trustees of Leland Stanford Jr. University,
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
