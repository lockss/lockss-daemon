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

package org.lockss.plugin.silverchair;

import org.jsoup.nodes.Node;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SilverchairVideoHtmlLinkExtractorFactory implements LinkExtractorFactory {

  private static final Logger logger = Logger.getLogger(SilverchairVideoHtmlLinkExtractorFactory.class);

  private static final String META_TAG = "meta";

  protected static final Pattern PATTERN_DOI =
          Pattern.compile("/(article|proceeding)\\.aspx\\?(articleid=[^&]+)$",
                  Pattern.CASE_INSENSITIVE);
  

  @Override
  public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false,false,null,null);
    registerExtractors(extractor);
    return extractor;
  }

  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {

    extractor.registerTagExtractor(META_TAG,
                                   new MetaTagExtractor(new String[]{"name"}));

  }

  public static class MetaTagExtractor extends SimpleTagLinkExtractor {

    public MetaTagExtractor(final String[] attrs) {
      super(attrs);
  }

    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();

      //the <a href attribute handler
      if (node.hasAttr("name")) {
        String metaValue = node.attr("name");

        logger.debug3("Metavalue = " + metaValue);

        if (metaValue.contains("citation_doi")) {

           String doiValue = node.attr("content");

            logger.debug3("Metavalue citation_doi = " + doiValue);

            //For example :  http://movie-usa.glencoesoftware.com/metadata/10.1083/jcb.202111095
            String videoUrl = "http://movie-usa.glencoesoftware.com/metadata/" + doiValue;

            logger.debug3("Metavalue videoUrl = " + videoUrl);

            if (!StringUtil.isNullString(videoUrl)) {

              logger.debug3("Metavalue videoUrl added to foundLink = " + videoUrl);

              cb.foundLink(AuUtil.normalizeHttpHttpsFromBaseUrl(au, videoUrl));
            }
        }
      }
    }
  }
}
