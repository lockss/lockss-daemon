/*
 * $Id: SilverchairHtmlLinkExtractor.java,v 1.1 2014-04-29 15:07:05 thib_gc Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.*;

import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class SilverchairHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  private static final Logger logger = Logger.getLogger(SilverchairHtmlLinkExtractor.class);
  
  protected static final Pattern PATTERN_SRCURL =
      Pattern.compile("/article\\.aspx\\?articleid=([^&]+)$", Pattern.CASE_INSENSITIVE);
  
  @Override
  protected String extractLinkFromTag(StringBuffer link,
                                      ArchivalUnit au,
                                      Callback cb)
      throws IOException {
    char ch = link.charAt(0);
    if ((ch == 'a' || ch == 'A') && beginsWithTag(link, ATAG) && srcUrl.contains("article.aspx?articleid=")) {
      String href = getAttributeValue(HREF, link);
      if (href != null && href.endsWith("/downloadCitation.aspx?format=ris")) {
        logger.debug3("Found target URL");
        // Derive the article ID from srcUrl
        Matcher srcMat = PATTERN_SRCURL.matcher(srcUrl);
        if (srcMat.find()) {
          String articleId = srcMat.group(1);
          // Generate the correct URLs of all citations (including RIS)
          if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
          for (String str : Arrays.asList("format=ris&",
                                          "format=bibtex&",
                                          "format=txt&",
              "")) {
            String url = String.format("/downloadCitation.aspx?%sarticleid=%s", str, articleId);
            logger.debug3(String.format("Generated %s", url));
            emit(cb, resolveUri(baseUrl, url));
          }
        }
      }
    }
    // Defer to the parent no matter what
    return super.extractLinkFromTag(link, au, cb);
  }

}
