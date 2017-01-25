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

package org.lockss.plugin.silverchair.ama;

import org.jsoup.nodes.Node;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

//import java.io.UnsupportedEncodingException;
//import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScAMAHtmlLinkExtractorFactory implements LinkExtractorFactory {

  private static final Logger logger = Logger.getLogger(ScAMAHtmlLinkExtractorFactory.class);

  private static final String ANCHOR_TAG = "a";
//  public static final String IMG_TAG = "img";

  protected static final Pattern PATTERN_ARTICLE_PDF =
      Pattern.compile("/journals/([^/]+)/data/journals/([^/]+/[^/]+/[^/.]+[.]pdf)$",
          Pattern.CASE_INSENSITIVE);
  protected static final String ACCESS_ARTICLE_PDF =
      "/pdfaccess.ashx[?]url=/data/journals/%s&routename=%s";
  protected static final String AUTH_SEARCH_STR = "searchresults?author=";
  
  // http://jamanetwork.com/journals/jamainternalmedicine/data/journals/intemed/934800/iii160001.pdf
  // http://jamanetwork.com/pdfaccess.ashx?url=/data/journals/intemed/934800/iii160001.pdf&routename=jamainternalmedicine
//  protected static final Pattern PATTERN_ARTICLE =
//    Pattern.compile("/(article|proceeding)\\.aspx\\?(articleid=[^&]+)$",
//                    Pattern.CASE_INSENSITIVE);
//
//  protected static final Pattern PATTERN_CITATION =
//    Pattern.compile("/downloadCitation\\.aspx\\?(format=[^&]+)?$",
//                    Pattern.CASE_INSENSITIVE);
//
//  protected static final Pattern PATTERN_DOWNLOAD_FILE =
//    Pattern.compile("javascript:downloadFile\\('([^']+)'\\)",
//                    Pattern.CASE_INSENSITIVE);
//
//  protected static final Pattern PATTERN_OPENPDF =
//    Pattern.compile("openPDFWindow\\('([^']+)'\\)",
//                    Pattern.CASE_INSENSITIVE);

  @Override
  public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false, false, null, null);
    registerExtractors(extractor);
    return extractor;
  }

  /*
 *  For when it is insufficient to simply use a different link tag or script
 *  tag link extractor class, a child plugin can override this and register
 *  additional or alternate extractors
 */
  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {

    extractor.registerTagExtractor(ANCHOR_TAG,
                                   new ScAMAAnchorTagExtractor(new String[]{
                                       "href",
                                       "data-article-url",
//                                       "onclick",
//                                       "download"
                                       }));
//    extractor.registerTagExtractor(IMG_TAG,
//                                   new SimpleTagLinkExtractor(new String[]{"src", "longdesc",
//                                                                           "data-original"
//                                   }));

  }

  public static class ScAMAAnchorTagExtractor extends SimpleTagLinkExtractor {

    public ScAMAAnchorTagExtractor(final String[] attrs) {
      super(attrs);
  }

    /**
     * Extract link(s) from this tag for attributes href, onclick, download.
     * We process each of the attributes in turn since more than one may be present.
     *
     * @param node the node containing the link
     * @param au Current archival unit to which this html document belongs.
     * @param cb A callback to record extracted links.
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();

      //the <a href attribute handler
      if (node.hasAttr("href")) {
        String href = node.attr("href");
//        Matcher hrefMat;
        // we look for searchresults?author= and exclude these
        if ((href != null) && !href.contains(AUTH_SEARCH_STR)) {
          // the standard method for <a href>
          JsoupHtmlLinkExtractor.checkLink(node, cb, "href");
        }
//        hrefMat = PATTERN_CITATION.matcher(href);
//        if (hrefMat.find()) {
//          logger.debug3("Found target citation URL");
//          // Derive citation format; can be null
//          String formatPair = hrefMat.group(1);
//          Matcher srcUrlMat = PATTERN_ARTICLE.matcher(srcUrl);
//          if (srcUrlMat.find()) {
//            // Derive article ID
//            String articleIdPair = srcUrlMat.group(2);
//            // Generate correct citation URL
//            String url;
//            if (formatPair == null) {
//              url = String.format("/downloadCitation.aspx?%s", articleIdPair);
//            }
//            else {
//              url = String.format("/downloadCitation.aspx?%s&%s", formatPair,
//                                  articleIdPair);
//            }
//            logger.debug3(String.format("Generated %s", url));
//            if (!StringUtil.isNullString(url)) {
//              cb.foundLink(url);
//            }
//          }
//        }
//        else {        }
      }   // end <a href
      
      if (node.hasAttr("data-article-url")) {
        String url = node.attr("data-article-url");
        if (!StringUtil.isNullString(url)) {
          cb.foundLink(url);
        }
      }
      
      // the 'download' attribute handler - the standard method for <a download
      if (node.hasAttr("download")) {
        JsoupHtmlLinkExtractor.checkLink(node, cb, "download");
      }
    }

} }
