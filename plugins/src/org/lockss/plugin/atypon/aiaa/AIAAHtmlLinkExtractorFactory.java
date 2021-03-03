/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.atypon.aiaa;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkExtractorFactory;
import org.lockss.util.Logger;


/*
 *  extend this class in order to use the AIAA specific link tag 
 *  link extractor 
 */
public class AIAAHtmlLinkExtractorFactory 
extends BaseAtyponHtmlLinkExtractorFactory {

  private static final String HREF_NAME = "href";

  @Override
  protected LinkExtractor createLinkTagExtractor(String attr) {
    return new AIAALinkTagLinkExtractor(attr);
  }

  /*
   *  Extend the BaseAtyponLinkTagLinkExtractor to handle the additional
   *  AIAA-unique case of
   *  <a href="javascript:submitArticles"
   *      in order to generate the citation download URL
   *  all other link tag cases are handled as per BaseAtypon and Jsoup      
   */
  public static class AIAALinkTagLinkExtractor extends BaseAtyponLinkTagLinkExtractor {

    private static Logger log = Logger.getLogger(AIAALinkTagLinkExtractor.class);

    protected Pattern SUBMIT_ARTICLES_PATTERN = Pattern.compile("javascript:submitArticles\\(([^,]+),([^,]+),*", Pattern.CASE_INSENSITIVE);
    protected Pattern DOI_URL_PATTERN = Pattern.compile("^(https?://.*/)doi/(abs|full|book)/([.0-9]+)/([^/]+)$");
    private static final String CIT_FORMATS_ACTION = "action/showCitFormats";

    // nothing needed in the constructor - just call the parent
    public AIAALinkTagLinkExtractor(String attr) {
      super(attr);
    }

    /*
     * Extending the way links are extracted by the Jsoup link extractor in a specific case:
     *   - we are on an article page (full or abstract) or a book landing page
     *   - we hit an anchor tag of the format:
     *         <a href="javascript:submitArticles(document.frmAs, 'action/showCitFormats', .....> </a>
     * In this case we create a link to the citations download page by pulling the DOI from the srcUrl
     * and generating a link like this:
     *     <base_url>action/showCitFormats?doi=xx.xxx/yyyyyy         
     * In any case other than this one, fall back to standard JSoup implementation
     *  which is the "super" of the tagBegin definition    
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      Boolean tagDone = false;
      String srcUrl = node.baseUri();
      Matcher doiMat = DOI_URL_PATTERN.matcher(srcUrl);
      // Are we on a page for which this would be pertinent?
      if ( (srcUrl != null) && doiMat.find()) {
        // now do we have a citation download href?
        if ((node.hasAttr(HREF_NAME)) ) {
          Matcher hrefMat = SUBMIT_ARTICLES_PATTERN.matcher(node.attr(HREF_NAME));
          if ( (hrefMat.find() && hrefMat.group(2).contains(CIT_FORMATS_ACTION))) {
            String newUrl =  doiMat.group(1) + CIT_FORMATS_ACTION + "?doi=" + doiMat.group(3) + "/" + doiMat.group(4);
            newUrl = AuUtil.normalizeHttpHttpsFromBaseUrl(au, newUrl);
            log.debug3("Generated citation download url: " + newUrl);
            cb.foundLink(newUrl);
            tagDone = true;
          }
        }
      }
      // for one reason or another, we didn't handle this. Fall back to BaseAtypon and thence to Jsoup
      if (tagDone != true) {
        super.tagBegin(node, au, cb);
      }
    }
  }

}
