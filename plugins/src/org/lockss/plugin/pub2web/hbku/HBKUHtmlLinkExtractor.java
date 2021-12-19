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

package org.lockss.plugin.pub2web.hbku;

import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HBKUHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

  private static final Logger logger = Logger.getLogger(HBKUHtmlLinkExtractor.class);

  //https://www.qscience.com/content/journals/10.5339/ahcsps.2011.1
  //https://www.qscience.com/content/journals/10.5339/rels.2009.commonground.1
  protected static final Pattern articlePattern =
          Pattern.compile("^https?://(.*)/content/journals/[^.]+\\.[0-9]{4}/(.*)",
                  Pattern.CASE_INSENSITIVE);
  
  @Override
  protected String extractLinkFromTag(StringBuffer link,
                                      ArchivalUnit au,
                                      Callback cb)
      throws IOException {

    char ch = link.charAt(0);

    /*
    <meta name="CRAWLER.fullTextLink" content="https://www.qscience.com/content/journals/10.5339/ahcsps.2011.1?crawler=true"/>
     */

    if ((ch == 'm' || ch == 'M') && beginsWithTag(link, METATAG)) {

      String key = getAttributeValue("name", link);
      String fullTextUrl =  "";

      Matcher articleMat = articlePattern.matcher(this.srcUrl);

      if (articleMat.find()) {
        if (key != null && key.startsWith("CRAWLER.fullTextHtmlLink")) {
          logger.debug3("Found a suitable <meta> tag - CRAWLER.fullTextHtmlLink\"");
          fullTextUrl = getAttributeValue("content", link);
        } else if (key != null && key.startsWith("CRAWLER.fullTextLink")) {
          logger.debug3("Found a suitable <meta> tag - CRAWLER.fullTextLink");
          fullTextUrl = getAttributeValue("content", link);
        }

        if (fullTextUrl != null) {
          String articleUrl = this.srcUrl;

          String pdf = fullTextUrl;

          if (articleUrl.contains("?crawler=true&mimetype=text/html")) {
            fullTextUrl = articleUrl;
            pdf = articleUrl.replace("?crawler=true&mimetype=text/html", "&mimetype=application/pdf");

          } else if (articleUrl.contains("?crawler=true")) {
            fullTextUrl = articleUrl + "&mimetype=text/html";
            pdf = articleUrl + "&mimetype=application/pdf";
          } else {
            fullTextUrl = articleUrl + "?crawler=true&mimetype=text/html";
            pdf = articleUrl + "?crawler=true&mimetype=application/pdf";
          }

          cb.foundLink(fullTextUrl);
          cb.foundLink(pdf);

          return fullTextUrl;
        }
      } else {
        logger.debug3("None match articleUrl = " + this.srcUrl);
      }
    }
    
    logger.debug3("No suitable <meta> tag");
    return super.extractLinkFromTag(link, au, cb);
  }
  
}
