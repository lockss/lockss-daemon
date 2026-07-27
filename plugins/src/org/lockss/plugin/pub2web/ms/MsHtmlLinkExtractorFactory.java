/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.pub2web.ms;



import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Node;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.util.Logger;

/* an implementation of JsoupHtmlLinkExtractor  */
/* 
 * MS uses ajax in order to dynamically generate portions of the html
 * TOC 
 *  Unlike for ASM, we don't need to do an extraction for these as they are straight-up
 *  <a href=<url for portion here>
 *
 * Full-Text html link is extracted from the javascript so that button click will work
 *    it is then normalized to be the crawler-friendly url
 * PDF link is available as an href but is also normalized to a crawler friendly version   
 */
public class MsHtmlLinkExtractorFactory 
implements LinkExtractorFactory {

  private static final Logger log = 
      Logger.getLogger(MsHtmlLinkExtractorFactory.class);

  private static final String DIV_TAG = "div";
  private static final String ID_ATTR = "id";

  private static final String FORM_TAG = "form";
  private static final String ACTION_ATTR = "action";

  //main journal article landing page - extract other information from tags here
  //http://jgv.microbiologyresearch.org/content/journal/jgv/10.1099/vir.0.069286-0
  protected static final Pattern PATTERN_ARTICLE_LANDING_URL = Pattern.compile("^(https?://[^/]+)/content/journal/[^/]+/[0-9]{2}\\.[0-9]{4}/[^/?&]+$", Pattern.CASE_INSENSITIVE);
  /*
  PDF link used to be a href link
  <a href="/deliver/fulltext/jmmcr/3/2/jmmcr000086.pdf?itemId=/content/journal/jmmcr/10.1099/jmmcr.0.000086&amp;mimeType=pdf&amp;isFastTrackArticle=" title="" rel="external" class="externallink pdf
  list-group-item list-group-item-info" ><div class="fa fa-file-pdf-o full-text-icon"></div>PDF
  <div class="fulltextsize ">
  539.07
  Kb
  </div></a>


  PDF link embedded into form action since 3/2020

  <form action="/deliver/fulltext/micro/165/3/254_micro000750.pdf?itemId=%2Fcontent%2Fjournal%2Fmicro%2F10.1099%2Fmic.0.000750&mimeType=pdf&containerItemId=content/journal/micro"
  target="/content/journal/micro/10.1099/mic.0.000750-pdf"
  data-title="Download"
  data-itemId="http://instance.metastore.ingenta.com/content/journal/micro/10.1099/mic.0.000750"
  class="ft-download-content__form ft-download-content__form--pdf js-ft-download-form " >
  <input type="hidden" name="pending" value="false" >
  <i class="fa fa-file-pdf-o
  access-options-icon" aria-hidden="true"></i>
  <span class="hidden-xxs">PDF</span>
  </form>
   */

  protected static final Pattern FORM_ACTION_PDF_URL = Pattern.compile("^/deliver/fulltext/[^/]+/[^.]+\\.pdf\\?itemId=.*", Pattern.CASE_INSENSITIVE);

  /*
  
  /*
   * FUL-TEXT HTML:
   * <div 
   *   id="itemFullTextId" 
   *   class="itemFullTextHtml retrieveFullTextHtml hidden-js-div" 
   *   data-fullTexturl="/deliver/fulltext/jgv/96/5/956.html?itemId=/content/journal/jgv/10.1099/jgv.0.000014&mimeType=html&fmt=ahah">
   * 
   * SUPPLEMENTARY DATA:
   * <div 
   *     id="tab5" (or something) 
   *     class="supplements hidden-js-div tabbedsection tab-pane" 
   *     data-ajaxurl="/content/journal/jgv/10.1099/jgv.0.000014/supp-data"
   *     
   * FIGURES TAB:
   * <div 
   *     id="tab3" 
   *     class="dataandmedia hidden-js-div tabbedsection tab-pane" 
   *     data-ajaxurl="/content/journal/jgv/10.1099/jgv.0.000014/figures?fmt=ahah">     
   */
  protected static final String FULLTEXT_ID_VAL = "itemFullTextId";
  protected static final String FULL_TEXT_ATTR = "data-fullTexturl";
  protected static final String AJAX_ATTR = "data-ajaxurl";
      
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) {
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    registerExtractors(extractor);
    return extractor;
  }


  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {
    extractor.registerTagExtractor(DIV_TAG, new MsDivTagLinkExtractor(ID_ATTR));
    extractor.registerTagExtractor(FORM_TAG, new MsFormActionExtractor(ACTION_ATTR));
  }

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

      // This only gets called for <div> tags with "id" attrs
      // Are we an article landing page?
      if ( (srcUrl != null) && landingMat.matches()) {
        String base_url = landingMat.group(1);
        if (DIV_TAG.equals(node.nodeName())) {
          String idVal = node.attr(ID_ATTR);
          String ftVal = node.attr(FULL_TEXT_ATTR);
          String ajVal = node.attr(AJAX_ATTR);
          if ( ajVal != null & !(StringUtils.isEmpty(ajVal))) {
            String newUrl = base_url + ajVal;
            newUrl = AuUtil.normalizeHttpHttpsFromBaseUrl(au, newUrl);
            log.debug3("create ajax URL: " + newUrl);
            cb.foundLink(newUrl);
            // if it was this, no need to do anything further with this div
            return;
          } else if ( idVal!= null && FULLTEXT_ID_VAL.equals(idVal) && ftVal != null & !(StringUtils.isEmpty(ftVal))) {
            String newUrl = base_url + ftVal;
            newUrl = AuUtil.normalizeHttpHttpsFromBaseUrl(au, newUrl);
            log.debug3("create fulltext URL: " + newUrl);
            cb.foundLink(newUrl);
            // if it was this, no need to do anything further with this div
            return;
            }
        } //are we a div
      } // are we on an article landing page?               
      // Do NOT call super, only create a link for these <DIV> tags
      // Sometimes a <DIV> tag is just a <DIV> tag.
    }
      
  }

  // Extract PDF link from form action since the website change of 3/2020
  public static class MsFormActionExtractor extends SimpleTagLinkExtractor {

    // nothing needed in the constructor - just call the parent
    public MsFormActionExtractor(String attr) {
      super(attr);
    }

    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();

      if ((srcUrl != null)) {
        if (FORM_TAG.equals(node.nodeName())) {
          String actionLink = node.attr(ACTION_ATTR);

          Matcher pdfLinkMat = FORM_ACTION_PDF_URL.matcher(actionLink);

          if (pdfLinkMat.matches()) {

            String base = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
            String newUrl = base + actionLink;

            log.debug3("MsHtmlFormExtractor PDF link: " + newUrl);
            
            cb.foundLink(newUrl);
          }
        }
        log.debug3("now calling the super tagBegin " + srcUrl + ", node name for " + node.nodeName());
        super.tagBegin(node, au, cb);
      }
    }

  }

}