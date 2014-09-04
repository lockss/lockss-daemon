/*
 * $Id: SEGHtmlLinkExtractorFactory.java,v 1.1 2014-09-04 03:14:49 ldoan Exp $
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

package org.lockss.plugin.atypon.seg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkExtractorFactory;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/*
 * Looks for the javascript function 'popRefFull' in html 'a' tags, and
 * extracts the image id.  This image id, and the doi and article id extracted
 * from base uri are used to contruct the url for 'showFullPopup' page
 * where the large size images can be found.
 * 
 * html block to process:
 * <a href="javascript:popRefFull('f1')" class="ref">
 *   <img border="1" align="bottom" id="f1image" alt="" 
 *      src="<imghome>/literatum/publisher/seg/journals/content
 *           /journalid/2013/gabc.2013.99.issue-9/gabc2013-9999.9/20130101
 *           /images/small/figure1.gif">
 *   <br><strong>View larger version </strong>(64K)<br><br>
 * </a>
 * 
 * New url to add to crawl queue:
 *   <segbase>/action/showFullPopup?id=f1&doi=99.9999%2Fgeo2013-9999.9
 *                                              
 * The large size image wanted:
 * <segbase><imghome>/literatum/publisher/seg/journals/content
 *              /journalid/2013/gabc.2013.99.issue-9/gabc2013-9999.9/20130101
 *              /images/large/figure1.jpeg
 * 
 * BaseAtyponHtmlLinkExtractorFactory uses JsoupHtmlLinkExtractor
 */
public class SEGHtmlLinkExtractorFactory 
  extends BaseAtyponHtmlLinkExtractorFactory {
  
  private static final String ATAG_NAME = "a";
  private static final String HREF_NAME = "href";
  
  // <a href="javascript:popRefFull('f1')" class="ref">
  protected static final Pattern PATTERN_POPREFFULL =
      Pattern.compile("^javascript:popRefFull\\(\\'([^']+)\\'\\)$",
          Pattern.CASE_INSENSITIVE);
  
  // <segbase>/doi/full/99.9999/gabc2012-9999.9
  protected static final Pattern PATTERN_FULL_ARTICLE = 
      Pattern.compile("^(https?://[^/]+)/doi/full/([.0-9]+)/([^?&]+)$",
          Pattern.CASE_INSENSITIVE);
   
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) {
    // The parent (BaseAtypon) creates a Jsoup Link Extractor 
    // and sets default restrictions on it which we need
    JsoupHtmlLinkExtractor extractor = 
        (JsoupHtmlLinkExtractor)super.createLinkExtractor(mimeType);
    // register extractor for 'href' attribute of 'a' tag
    extractor.registerTagExtractor(
        ATAG_NAME, new SEGSimpleTagLinkExtractor(HREF_NAME));
    return extractor;
  }
  
  public static class SEGSimpleTagLinkExtractor 
    extends SimpleTagLinkExtractor {
    
    private static Logger log = 
        Logger.getLogger(SEGSimpleTagLinkExtractor.class);
    
    public SEGSimpleTagLinkExtractor(String attr) {
      super(attr);
    }
    
    // processes html a tag and create new url to add to crawl queue
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      if ("a".equals(node.nodeName())) {
        String popRefFull = node.attr(HREF_NAME);
        if (!StringUtil.isNullString(popRefFull)) {
          Matcher popRefFullMat = PATTERN_POPREFFULL.matcher(popRefFull);
          if (popRefFullMat.find()) {
            log.debug3("Found javascript:popRefFull function");
            String imgId = popRefFullMat.group(1);
            // base uri: <segbase>/doi/full/99.9999/gabc2012-9999.9
            String srcUrl = node.baseUri();
            log.debug3("srcUrl from baseUri: " + srcUrl);
            if (!StringUtil.isNullString(srcUrl)) {
              Matcher fullArticleMat = PATTERN_FULL_ARTICLE.matcher(srcUrl);
              if (fullArticleMat.find()) {
                String doi = fullArticleMat.group(2);
                String articleId = fullArticleMat.group(3);
                // constructs showFullPopup page:
                // <segbase>/action/showFullPopup?id=f1
                //                              &doi=99.9999%2Fgeo2013-9999.9
                if ((!StringUtil.isNullString(imgId)
                    && !StringUtil.isNullString(doi)
                    && !StringUtil.isNullString(articleId))) {
                  String newUrl =  "/action/showFullPopup?id=" + imgId
                                     + "&doi=" + doi + "%2F" + articleId;
                  log.debug3("Created/added new url: " + newUrl);
                  cb.foundLink(newUrl);
                  return;
                }
                log.debug3("imgId, doi or articleId is null");
                log.debug3("imgId:" + imgId + " doi:" + 
                                              " articleId:" + articleId);
              }
            }
          }
        }
      }
      super.tagBegin(node, au, cb);
    }   
  }
      
}