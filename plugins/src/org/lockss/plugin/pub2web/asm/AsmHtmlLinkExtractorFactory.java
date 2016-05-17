/*
 * $Id:$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web.asm;



import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.lockss.extractor.*;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

/* an implementation of JsoupHtmlLinkExtractor  */
/* 
 * ASM and MS differ slightly in how they label links
 * TOC (journals only)
 *    pull the URL that hold article list portions from the "tocheadingarticlelisting" 
 *    books TOC (that is full-book landing) have the chapter URLs as straight href
 * On the article/chapter landing pages, get figures/tables/data from the identifed
 * div tags.
 */
public class AsmHtmlLinkExtractorFactory 
implements LinkExtractorFactory {

  private static final Logger log = 
      Logger.getLogger(AsmHtmlLinkExtractorFactory.class);

  private static final String DIV_TAG = "div";
  private static final String ID_ATTR = "id";
  private static final String CLASS_ATTR = "class";


  // and for table of contents article listing (journals only)
  //<div class="tocheadingarticlelisting retrieveTocheadingArticle hiddenjsdiv">/content/journal/microbiolspec/2/4/articles?fmt=ahah&tocHeading=http://asm.metastore.ingenta.com/content/journal/microbiolspec/reviewarticle</div>
  // Identify an article landing page from which all article aspect links originate
  protected static final Pattern PATTERN_TOC_LANDING_URL = Pattern.compile("^(https?://[^/]+)/content/journal/[^/]+/[0-9]+/[0-9]+$", Pattern.CASE_INSENSITIVE);
  protected static final String ART_LISTING_CLASS = "tocheadingarticlelisting";
  protected static final String ARTICLES_URL_SNIPPET = "articles?fmt=ahah"; 
  

  //main book chapter or journal article landing page - extract other information from tags here
  // www.asmscience.org/content/journal/microbiolspec/10.1128/microbiolspec.PLAS-0022-2014
  // www.asmscience.org/content/book/10.1128/9781555818289.chap4
  protected static final Pattern PATTERN_ARTICLE_CHAP_LANDING_URL = Pattern.compile("^(https?://[^/]+)/content/(book|journal/[^/]+)/[0-9]{2}\\.[0-9]{4}/[^/?&]+$", Pattern.CASE_INSENSITIVE); 
  protected static final Pattern FIGURE_TABLE_PATTERN = Pattern.compile("metadata_(webId|thumbnailImage|fullSizeImage)", Pattern.CASE_INSENSITIVE);
  protected static final String FULLTEXT_ID_VAL = "itemFullTextId";
  protected static final String FULL_TEXT_ATTR = "data-fullTexturl";
  
  
  
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) {
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    registerExtractors(extractor);
    return extractor;
  }


  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {
    extractor.registerTagExtractor(DIV_TAG, new ASMDivTagLinkExtractor(ID_ATTR));
  }

  /*
   * This is the same as MS
   * pick up the click-url for the full-text html (which is normalized to the crawler version)
   * <div .... data-fullTexturl="/deliver/fulltext/10.1128/9781555817992/ch..."   *
   *  
   * This is unique to ASM (books and journals)
   * <div class="metadata_webId">/content/microbiolspec/10.1128/microbiolspec.AID-0004-2012.fig1</div>
   * <div class="metadata_thumbnailImage">microbiolspec/2/1/AID-0004-2012-fig1_thmb.gif</div>
   * <div class="metadata_fullSizeImage">microbiolspec/2/1/AID-0004-2012-fig1.gif</div>
   * this one is already picked up
   * <img src="/docserver/fulltext/microbiolspec/2/1/AID-0004-2012-fig1.gif"   * 
   */
  public static class ASMDivTagLinkExtractor extends SimpleTagLinkExtractor {

    // nothing needed in the constructor - just call the parent
    public ASMDivTagLinkExtractor(String attr) {
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
      // we should only proceed if basic conditions are met
      if ( (srcUrl == null)  || !(DIV_TAG.equals(node.nodeName())) ) {
        return;
      }
      
      Matcher landingMat = PATTERN_ARTICLE_CHAP_LANDING_URL.matcher(srcUrl);
      Matcher tocMat = PATTERN_TOC_LANDING_URL.matcher(srcUrl);
      // A. an article/book/chapter landing page
      if (landingMat.matches()) {
        log.debug3("...it's an article, book or chapter landing page");
        // build up a DOI value with html encoding of the slash"
        String base_url = landingMat.group(1);

        String idVal = node.attr(ID_ATTR); // what the "id" attri is set to
        String ftVal = node.attr(FULL_TEXT_ATTR); // what the data-fullTextUrl is set to
        if ( idVal!= null && FULLTEXT_ID_VAL.equals(idVal) && ftVal != null & !(StringUtils.isEmpty(ftVal))) {
          String newUrl = base_url + ftVal;
          log.debug3("create fulltext URL: " + newUrl);
          cb.foundLink(newUrl);
          // if it was this, no need to do anything further with this div
          return;
        } else {
          // if class atrribute exists and the value matches 
          // "metadata_webId", "metadata_thubnailImage", or "metadata_fullSizeImage" 
          String classVal = node.attr(CLASS_ATTR); // what the "id" attri is set to
          if ( classVal != null && FIGURE_TABLE_PATTERN.matcher(classVal).find()) {  
            log.debug3("div with figure/table metadata_* link");
            String figure_table_val = null; 
            // the value is the text of this node
            for (Node tnode : node.childNodes()) {
              if (tnode instanceof TextNode) {
                String full_val = ((TextNode)tnode).text();
                figure_table_val = (full_val != null) ? full_val.trim() : null;
                break;
              }
            }
            if (figure_table_val != null) {
              // we picked up a link to a figure or table - thumbnail, landing page, or full-size
              // either starts with "/content/..." or needs "/docserver/fulltext" prepended
              log.debug3("picked up url snippet: " + figure_table_val);
              String newUrl;
              if (!figure_table_val.startsWith("/content")) {
                newUrl = base_url + "/docserver/fulltext/" + figure_table_val;
              } else {
                newUrl = base_url + figure_table_val;
              }
              log.debug3("create new URL: " + newUrl);
              cb.foundLink(newUrl);
              // if it was this, no need to do anything further with this link
              return;
            }
          } 
        } 
        return; // no need to look further on this page
      } 
      // b. a TOC page 
      if(tocMat.matches()) {
        log.debug3("on a TOC page");
        String base_url = tocMat.group(1);
 
        String classVal = node.attr(CLASS_ATTR);
        if ( classVal != null && classVal.contains(ART_LISTING_CLASS)) {
          String tVal = null;
          for (Node tnode : node.childNodes()) {
            if (tnode instanceof TextNode) {
              tVal = ((TextNode)tnode).text();
              break;
            }
          }
          log.debug3("toc div val: " + tVal);
          if ( tVal != null && tVal.contains(ARTICLES_URL_SNIPPET)) {
            log.debug3("...and we matched that expected URL for toc article generator");
            String newUrl = base_url + tVal;
            log.debug3("create new URL: " + newUrl);
            cb.foundLink(newUrl);
            // if it was this, no need to do anything further with this link
            return;
          }
        }
      } 
      // Do NOT call super, only create a link for these <DIV> tags
      // Sometimes a <DIV> tag is just a <DIV> tag.
    }               
  }
  
}