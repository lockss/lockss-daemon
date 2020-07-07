/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ingenta;

import java.io.IOException;
import java.net.URL;
import java.util.regex.*;

import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

public class IngentaHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  private static final Logger logger = Logger.getLogger(IngentaHtmlLinkExtractor.class);
  
  protected static final Pattern popupPattern = 
      Pattern.compile("^javascript:popupImage\\(([\"']|%22)(.*)\\1\\)$", 
          Pattern.CASE_INSENSITIVE);

  protected static final Pattern articlePattern =
          Pattern.compile("^https?://(.*)/[0-9]{4}/\\d+/\\d+/art[0-9]{5}$",
                  Pattern.CASE_INSENSITIVE);
  
  @Override
  protected String extractLinkFromTag(StringBuffer link,
                                      ArchivalUnit au,
                                      Callback cb)
      throws IOException {

    char ch = link.charAt(0);
    if ((ch == 'a' || ch == 'A') && Character.isWhitespace(link.charAt(1))) {
      String href = getAttributeValue(HREF, link);
      if (href == null) {
        return null;
      }
      Matcher mat = popupPattern.matcher(href);
      if (mat.find()) {
        logger.debug3("Found a suitable <a> tag");
        if (baseUrl == null) { baseUrl = new URL(srcUrl); } // Copycat of parseLink()
        return resolveUri(baseUrl, mat.group(2));
      }
    }

    /*
    Some of them have both, some of them only have "CRAWLER.fullTextLink"
    <meta name="CRAWLER.fullTextHtmlLink" content="https://api.ingentaconnect.com/content/aspt/sb/2019/00000044/00000001?crawler=true&mimetype=text/html"/>
    <meta name="CRAWLER.fullTextLink" content="https://api.ingentaconnect.com/content/aspt/sb/2019/00000044/00000001?crawler=true"/>
     */

    else if ((ch == 'm' || ch == 'M') && beginsWithTag(link, METATAG)) {
      String key = getAttributeValue("name", link);
      String content =  "";

      Matcher articleMat = articlePattern.matcher(this.srcUrl);

      if (articleMat.find()) {
        if (key != null && key.startsWith("CRAWLER.fullTextHtmlLink")) {
          logger.debug3("Found a suitable <meta> tag");
          content = getAttributeValue("content", link);
        } else if (key != null && key.startsWith("CRAWLER.fullTextLink")) {
          content = getAttributeValue("content", link);
        }

        if (content != null) {
          String articleUrl = this.srcUrl;
          String pdf = articleUrl;

          if (!articleUrl.contains("?crawler=true&mimetype=text/html")) {
            content = articleUrl + "?crawler=true&mimetype=text/html";
            pdf = articleUrl + "?crawler=true&mimetype=application/pdf";
          }

          // Replace "base_url" with "api_url"
          String api_based_article = content.replace(au.getConfiguration().get("base_url"), au.getConfiguration().get("api_url"));
          String api_based_pdf = pdf.replace(au.getConfiguration().get("base_url"), au.getConfiguration().get("api_url"));

          cb.foundLink(api_based_article);
          cb.foundLink(api_based_pdf);

          return content;
        }
      }
    }
    
    logger.debug3("No suitable <a> or <meta> tag");
    return super.extractLinkFromTag(link, au, cb);
  }
  
}
