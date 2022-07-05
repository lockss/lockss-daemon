/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.atypon.practicalaction;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkExtractorFactory;
import org.lockss.util.Logger;


/*
 *  extend this class in order to suppress the showCitFormats link on TOC pages
 *  no way to easily crawlfilter based on html alone
 */
public class PracticalActionHtmlLinkExtractorFactory 
extends BaseAtyponHtmlLinkExtractorFactory {

  private static final String HREF_NAME = "href";

  @Override
  protected LinkExtractor createLinkTagExtractor(String attr) {
    return new PracticalActionLinkTagLinkExtractor(attr);
  }

  /*
   * Extend the BaseAtyponLinkTagLinkExtractor to suppress
   * showCitFormats from a table of contents url
   * The following html exists in the right column of tables-of-contents
   * There aren't any good identifiers to crawl filter out 
   * and don't want to add a general atypon crawl rule to exclude dois with "issue" in them
   * so instead, suppress the link extraction of a showCitFormats? link when on
   * a url with the form "<base>/toc/<jid>/vol/<issue>
   * eg: http://www.developmentbookshelf.com/toc/wl/34/2
   * TODO - think about putting this in to BaseAtypon if it shows up elsehwere
   *  
   *  <li class="downloadCitations">
   *            <a href="/action/showCitFormats?doi=10.3362%2F1756-3488.2015.34.issue-2">Send to Citation Mgr</a>
   *  </li>
   *  
   */
  public static class PracticalActionLinkTagLinkExtractor extends BaseAtyponLinkTagLinkExtractor {

    private static final Logger log = Logger.getLogger(PracticalActionLinkTagLinkExtractor.class);

    protected Pattern SHOW_CITATION_PATTERN = Pattern.compile("action/showCitFormats\\?doi=", Pattern.CASE_INSENSITIVE);
    protected Pattern TOC_URL_PATTERN = Pattern.compile("^https?://[^/]+/toc/[^/]+/[0-9]+/[^/]+$");

    // nothing needed in the constructor - just call the parent
    public PracticalActionLinkTagLinkExtractor(String attr) {
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
      Matcher tocMat = TOC_URL_PATTERN.matcher(srcUrl);
      // Are we on a page for which this would be pertinent?
      if ( (srcUrl != null) && tocMat.find()) {
        // now do we have a citation download href?
        if ((node.hasAttr(HREF_NAME)) ) {
          Matcher hrefMat = SHOW_CITATION_PATTERN.matcher(node.attr(HREF_NAME));
          if ( hrefMat.find() ) {
            //move along; you don't want this link
            log.debug3("ignoring show citation link on a TOC at page url: " + srcUrl);
            tagDone = true;
          }
        }
      }
      // If this wasn't the link we wanted to ignore, fall back to BaseAtypon and thence to Jsoup
      if (tagDone != true) {
        super.tagBegin(node, au, cb);
      }
    }
  }

}
