/*
 * $Id:$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pub2web.iet;



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

/* 
 * IET uses ajax in order to dynamically generate portions of the html
 * 
 * Identify the div that contains the text that is the Full Text link
 * and extract that text
 */
public class IetHtmlLinkExtractorFactory 
implements LinkExtractorFactory {

  private static final Logger log = 
      Logger.getLogger(IetHtmlLinkExtractorFactory.class);

  private static final String DIV_TAG = "div";
  private static final String ID_ATTR = "id";

  protected static final Pattern PATTERN_ARTICLE_LANDING_URL = Pattern.compile("^(https?://[^/]+)/content/journals?/([^/]+/)?[0-9]{2}\\.[0-9]{4}/[^/?&]+$", Pattern.CASE_INSENSITIVE); 
  protected static final String FULLTEXT_ID_VAL = "itemFullTextId";
  protected static final String AJAX_ATTR = "data-ajaxurl";
      
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) {
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    registerExtractors(extractor);
    return extractor;
  }


  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {
    extractor.registerTagExtractor(DIV_TAG, new MsDivTagLinkExtractor(ID_ATTR));
    
  }

  public static class MsDivTagLinkExtractor extends SimpleTagLinkExtractor {

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
          if ( idVal!= null && FULLTEXT_ID_VAL.equals(idVal)) {
            for(Node childNode : node.childNodes()) {
              //find children of the identified div that are plain text. That's our link
              if(childNode instanceof TextNode) {
                String newUrl = base_url + ((TextNode)childNode).text();
                log.debug3("create fulltext URL: " + newUrl);
                cb.foundLink(newUrl);
                return;
              }
            }
          }
        } 
      }
    }
      
  }
  
}