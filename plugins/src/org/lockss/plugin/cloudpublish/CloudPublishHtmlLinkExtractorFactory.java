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

package org.lockss.plugin.cloudpublish;

import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkExtractorFactory;
import org.lockss.util.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*
 *  extend this class in order to use the Liverpool books specific link tag 
 *  link extractor 
 */
public class CloudPublishHtmlLinkExtractorFactory
extends BaseAtyponHtmlLinkExtractorFactory {

  private static final String HREF_NAME = "href";

  @Override
  protected LinkExtractor createLinkTagExtractor(String attr) {
    return new LUPBooksLinkTagLinkExtractor(attr);
  }

  /*
   *  Extend the BaseAtyponLinkTagLinkExtractor to handle the additional
   *  Liverpool Books (only) need to generate the RIS metadata URL from the 
   *  full book PDF url
   *  <a href="/doi/pdf/10.3828/....."
   *      
   *  all other link tag cases are handled as per BaseAtypon and Jsoup      
   */
  public static class LUPBooksLinkTagLinkExtractor extends BaseAtyponLinkTagLinkExtractor {

    private static Logger log = Logger.getLogger(LUPBooksLinkTagLinkExtractor.class);

    protected Pattern BOOK_LANDING_PATTERN = Pattern.compile("^(https?://.*/)doi/book/([.0-9]+)/([^/]+)$");
    protected Pattern PDF_URL_PATTERN = Pattern.compile("/doi/pdf/([.0-9]+)/([^/]+)$");
    // note - we can assume the "no slash in 2nd part of doi" because for books liverpool
    // uses the isbn as the second part of the doi.  This is not true across 
    // Atypon, but does hold for Liverpool
    private static final String DOWNLOAD_RIS_ACTION = "action/downloadCitation";
    private static final String DOWNLOAD_RIS_TAIL = "&format=ris&include=cit";

    // nothing needed in the constructor - just call the parent
    public LUPBooksLinkTagLinkExtractor(String attr) {
      super(attr);
    }

    /*
     * Extending the way links are extracted by the Jsoup link extractor in a specific case:
     *   - we are on an book landing page 
     *   - we hit an anchor tag of the format:
     *         <a href="doi/pdf/10.xxxx/foo"...
     * In this case we create a link to the citations download page by pulling the DOI from the srcUrl
     * and generating a link like this:
     *     <base_url>action/downloadCitation?doi=...&format=ris&include=cit
     * after creating this link, fall back to standard atypon/jsoup implementations
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();
      Matcher bookMat = BOOK_LANDING_PATTERN.matcher(srcUrl);
      // Are we on a page for which this would be pertinent?
      if ( (srcUrl != null) && bookMat.find()) {
        // is this the PDF link?
        if ((node.hasAttr(HREF_NAME)) ) {
          Matcher hrefMat = PDF_URL_PATTERN.matcher(node.attr(HREF_NAME));
          if ( (hrefMat.find()) ){
            // the "/" gets normalized to %2F 
            String newUrl =  bookMat.group(1) + DOWNLOAD_RIS_ACTION + "?doi=" + hrefMat.group(1) + "/" + hrefMat.group(2) + DOWNLOAD_RIS_TAIL;
            log.debug3("Generated ris citation url: " + newUrl);
            cb.foundLink(newUrl);
          }
        }
      }
      // allow the parent to get the actual pdf link
      super.tagBegin(node, au, cb);
    }
  }

}
