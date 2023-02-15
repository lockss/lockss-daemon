/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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
