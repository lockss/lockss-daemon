/*
 * $Id:$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web.ms;



import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.*;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

/* an implementation of JsoupHtmlLinkExtractor  */
/* 
 * MS uses ajax in order to dynamically generate portions of the html
 * TOC 
 *  Unlike for ASM, we don't need to do an extraction for these as they are straight-up
 *  <a href=<url for portion here>

 * META_TAG - 
 *     on an article landing page: pull the crawler version of the full-text HTML from the article landing page
 *     
 *     on an article landing page: may have to do ajax interpretation to get full-sized figures     
 *     
 *  TODO: should we decide to pick up the dynamic full-text HTML instead of the crawler version
 *    we will need to add back in some functionality here to extract the original URL    
 */
public class MsHtmlLinkExtractorFactory 
implements LinkExtractorFactory {

  private static final Logger log = 
      Logger.getLogger(MsHtmlLinkExtractorFactory.class);

  private static final String DIV_TAG = "div";
  private static final String ID_ATTR = "id";
  private static final String CLASS_ATTR = "class";
  private static final String META_TAG = "meta";
  private static final String NAME_ATTR = "name";
  private static final String CONTENT_ATTR = "content";


  //main journal article landing page - extract other information from tags here
  //http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.069286-0
  protected static final Pattern PATTERN_ARTICLE_LANDING_URL = Pattern.compile("^(https?://[^/]+)/content/journal/[^/]+/[0-9]{2}\\.[0-9]{4}/[^/?&]+$", Pattern.CASE_INSENSITIVE); 
  
  /*
   * Tab controlled display of full-text HTML is handled via
   * <div id="itemFullTextId" 
   * class="itemFullTextHtml 
   * retrieveFullTextHtml 
   * hidden-js-div" data-fullTexturl="foo-dee-foo">foo-dee-foo</div>
   * We are going to normalize this to point to the crawler version of the full-text html that we have extracted from the meta tags
   * so that clicking on the link works....
   */
  protected static final String FULLTEXT_ID_VAL = "itemFullTextId";
  protected static final String FULL_TEXT_ATTR = "data-fullTexturl";
  protected static final String AJAX_ATTR = "data-ajaxurl";
  
  protected static final String CRAWLERTEXT_NAME_VAL = "CRAWLER.fullTextLink";
  
  protected static final String FIGURES_URL_SNIPPET = "figures?fmt=ahah"; 
  
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) {
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    registerExtractors(extractor);
    return extractor;
  }


  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {
    extractor.registerTagExtractor(DIV_TAG, new MsDivTagLinkExtractor(ID_ATTR));
    //extractor.registerTagExtractor(META_TAG, new MsMetaTagLinkExtractor(NAME_ATTR));
  }

  /*

   * this one is already picked up
   * <img src="/docserver/fulltext/microbiolspec/2/1/AID-0004-2012-fig1.gif"
   */
  public static class MsDivTagLinkExtractor extends SimpleTagLinkExtractor {

    // nothing needed in the constructor - just call the parent
    public MsDivTagLinkExtractor(String attr) {
      super(attr);
    }

    //
    // This will get called when a DIV tag exists with an "id" value
    // but we don't want the JSOUP super class to also create a link using
    // any "id" value of a div tag, so when we are here, do not call super,
    // just return.
    //
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();
      Matcher landingMat = PATTERN_ARTICLE_LANDING_URL.matcher(srcUrl);

      // Are we an article landing page?
      if ( (srcUrl != null) && landingMat.matches()) {
        log.debug3("...it's an article landing page");
        String base_url = landingMat.group(1);
        if (DIV_TAG.equals(node.nodeName())) {
          String idVal = node.attr(ID_ATTR);
          String ftVal = node.attr(FULL_TEXT_ATTR);
          String ajVal = node.attr(AJAX_ATTR);
          if ( ajVal != null) {
            String newUrl = base_url + ajVal;
            log.debug3("create ajax URL: " + newUrl);
            cb.foundLink(newUrl);
            // if it was this, no need to do anything further with this div
            return;
          } else if ( idVal!= null && FULLTEXT_ID_VAL.equals(idVal) && ftVal != null) {
            String newUrl = base_url + ftVal;
            log.debug3("create fulltext URL: " + newUrl);
            cb.foundLink(newUrl);
            // if it was this, no need to do anything further with this div
            return;
            }
            /*
            if ( val != null && val.contains(FIGURES_URL_SNIPPET)) {
              log.debug3("...and we matched that expected URL for toc article generator");
              String newUrl = base_url + val;
              log.debug3("create new URL: " + newUrl);
              cb.foundLink(newUrl);
              // if it was this, no need to do anything further with this link
              return;
            }
            */
        } //are we a div
      } // are we on an article landing page?               
      // Do NOT call super. 
      // Only in these very specific cases will a <DIV> tag generate a URL
      // Sometimes a <DIV> tag is just a <DIV> tag.
    }
      
 
      private boolean isAttrValMatches(Node node, String attr_name,
          Pattern valPattern) {
        boolean retVal = false;
        
        if (valPattern != null) {
          String tempVal = node.attr(attr_name);         
          Matcher mat = valPattern.matcher(tempVal);
          if (mat.find()) {
            retVal = true;
          }
        } 
        return retVal;
      }
      
  }
  
  /*
   * META tag on an article landing page to get the crawler version of the full-text article
   */
  
  public static class MsMetaTagLinkExtractor extends SimpleTagLinkExtractor {

    // nothing needed in the constructor - just call the parent
    public MsMetaTagLinkExtractor(String attr) {
      super(attr);
    }

    //
    // This will get called when 
    // <meta name="CRAWLER.fullTextUrl"
    // but we don't want the JSOUP super class to also create a link using
    // any "name value of a meta tag
    // just return.
    //
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();
      Matcher landingMat = PATTERN_ARTICLE_LANDING_URL.matcher(srcUrl);

      // Are we a page for which this would be pertinent?
      if ( (srcUrl != null) && landingMat.matches()) {
        log.debug3("...it's an article landing page");
        if (META_TAG.equals(node.nodeName())) {
          String nameAttr = node.attr(NAME_ATTR);
          if (CRAWLERTEXT_NAME_VAL.equals(nameAttr)) {
            String contentUrl = node.attr(CONTENT_ATTR);
            log.debug3("crawler_fullTextUrl: " + contentUrl);
            if (contentUrl != null) {
              //TESTING
              //cb.foundLink(contentUrl);
              return;
            }
          }
        }
      } // end of check for full text html page
      // Do NOT call super. 
      // Only in these very specific cases will a <Meta> tag generate a URL
    }
  }
}
