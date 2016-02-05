/* $Id$
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
