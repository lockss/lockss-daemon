/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer;

import java.io.IOException;

import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

/**
 * <p>
 * The purpose of this link extractor is simply to log specific links at debug3
 * so that they can be cataloged for debugging purposes.
 * </p>
 * @author Thib Guicherd-Callin
 */
public class SpringerLinkHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  protected static Logger logger = Logger.getLogger(SpringerLinkHtmlLinkExtractor.class);

  @Override
  protected String extractLinkFromTag(StringBuffer link,
                                       ArchivalUnit au,
                                       Callback cb)
      throws IOException {
    if (logger.isDebug3()) {
      String candidate = null;
      char ch = link.charAt(0);
      if ((ch == 'l' || ch == 'L') && beginsWithTag(link, LINKTAG)) {
        candidate = getAttributeValue("href", link);
      }
      else if ((ch == 's' || ch == 'S') && beginsWithTag(link, SCRIPTTAG)) {
        candidate = getAttributeValue("src", link);
      }
      if (candidate != null && candidate.contains("/dynamic-file.axd?id=")) {
        logger.debug3(String.format("Found: AU %s, source URL %s, target link %s", au.getName(), srcUrl, candidate));
      }
    }
    return super.extractLinkFromTag(link, au, cb);
  }

  
  
}
