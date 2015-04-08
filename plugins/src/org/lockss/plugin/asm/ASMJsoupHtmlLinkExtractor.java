/*
 * $Id:$
 */

/*

Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.asm;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ASMJsoupHtmlLinkExtractor extends JsoupHtmlLinkExtractor {
  
  // the url that returns a one-time expiring full-text url, eg
  // http://www.asmscience.org/deliver/fulltext/microbiolspec/2/1/AID-0004-2012.html?itemId=/content/journal/microbiolspec/10.1128/microbiolspec.AID-0004-2012&mimeType=html&fmt=ahah
  protected static final Pattern PATTERN_FULL_ARTICLE_AHAH_URL = Pattern.compile("^(https?://[^/]+)/deliver/fulltext/[^/]+/[0-9]+/[0-9]+/[^/]+\\.html\\?itemId=[^&]+&mimeType=html&fmt=ahah$", Pattern.CASE_INSENSITIVE);
  // A one-time full-text article URL - eg
  //http://www.asmscience.org/docserver/ahah/fulltext/microbiolspec/2/1/AID-0004-2012.html?expires=1427956275&id=id&accname=4398&checksum=BF7F91C9BFCC756A09C4FF5D59B4EE58
  protected static final Pattern PATTERN_ARTICLE_TEMPORARY_URL = Pattern.compile("^(https?://[^/]+)/docserver/ahah/fulltext/[^/]+/[0-9]+/[0-9]+/[^/]+\\.html\\?expires=[^&]+&id=id&accname=[^&]+&checksum=.+$", Pattern.CASE_INSENSITIVE);

  /**
   * the theLog for this class
   */
  private static final Logger log =
      Logger.getLogger(ASMJsoupHtmlLinkExtractor.class);
  @Override
  public void extractUrls(final ArchivalUnit au, final InputStream in,
                          final String encoding, final String srcUrl,
                          final Callback cb)
      throws IOException, PluginException {
    // If this matches the AHAH URL pattern, it is a text/html file that actually 
    // contains ONLY a line of text that is a link to a one-time authorized URL
    // We need to extract out this URL but cannot use the default extractor because the 
    // html is malformed. There are no tags at all.
    if (srcUrl.contains("&mimeType=html&fmt=ahah")) {
      log.info("WE SHOULD BE PULLING URL");
    }
    Matcher urlGenMatch = PATTERN_FULL_ARTICLE_AHAH_URL.matcher(srcUrl);
    if ( (srcUrl != null) && urlGenMatch.matches()) {
      // Parse our file
      Document doc = Jsoup.parse(in, encoding, srcUrl);
      String tempUrl =  doc.body().text();
      Matcher oneTimeMatch = PATTERN_ARTICLE_TEMPORARY_URL.matcher(tempUrl);
      if (oneTimeMatch.matches()) {
        log.debug3("we have a one-time URL!!");
        log.debug3(tempUrl);
        cb.foundLink(tempUrl);
      } else {
        log.debug3(tempUrl);
        log.warning("The returned full-text generation URL didn't return a one-time content URL");
      }
    } else {
      super.extractUrls(au, in, encoding, srcUrl, cb);
    }
  }

}