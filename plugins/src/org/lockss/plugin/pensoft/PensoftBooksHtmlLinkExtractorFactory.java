/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pensoft;

import org.jsoup.nodes.Node;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.GoslingHtmlLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PensoftBooksHtmlLinkExtractorFactory implements LinkExtractorFactory {

  private static final Logger log = Logger.getLogger(PensoftBooksHtmlLinkExtractorFactory.class);


  public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
    return new PensoftBooksHtmlLinkExtractor();
  }

  public static class PensoftBooksHtmlLinkExtractor extends GoslingHtmlLinkExtractor {

    @Override
    protected String extractLinkFromTag(StringBuffer link,
                                        ArchivalUnit au,
                                        Callback cb)
            throws IOException {

        String content =  "";

        String base_url = au.getConfiguration().get("base_url");
        String book_id =  au.getConfiguration().get("book_id");

        log.debug3("base_url = " + base_url + ", book_id = " + book_id + ", this srcUrl = " + this.srcUrl);


        if (this.srcUrl.contains(base_url) && this.srcUrl.contains(book_id)) {

          String ris_url = base_url + "article/" + book_id + "/download/ris";

          log.debug3("base_url = " + base_url + ", book_id = " + book_id + ", ris_url  = " + ris_url);

          String article_preview = base_url + "article_preview.php?id=" + book_id ;

            log.debug3("base_url = " + base_url + ", book_id = " + book_id + ", article_preview  = " + article_preview);

          cb.foundLink(ris_url);
          cb.foundLink(article_preview);
        }

      return super.extractLinkFromTag(link, au, cb);
    }
  }
}
