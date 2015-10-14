/* $Id:$
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

package org.lockss.plugin.atypon.liverpool;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkExtractorFactory;
import org.lockss.util.Logger;


/*
 *  extend this class in order to use the Liverpool books specific link tag 
 *  link extractor 
 */
public class LiverpoolBooksHtmlLinkExtractorFactory 
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
