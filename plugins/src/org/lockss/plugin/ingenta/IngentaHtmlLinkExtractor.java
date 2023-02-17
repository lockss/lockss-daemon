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
          Pattern.compile("^https?://(.*)/[0-9]{4}/[^/]+/[^/]+/art[0-9]{5}$",
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
