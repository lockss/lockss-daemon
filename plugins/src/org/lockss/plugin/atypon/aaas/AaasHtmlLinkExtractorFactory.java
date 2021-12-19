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

package org.lockss.plugin.atypon.aaas;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkExtractorFactory;
import org.lockss.util.Logger;

public class AaasHtmlLinkExtractorFactory
    extends BaseAtyponHtmlLinkExtractorFactory {

  /*
   *  AaasHtmlLinkExtractorFactory creates a url to a metadata file (.ris) for every article DOI_HTML page it
   *  encounters.
   */

  private static final Pattern DOI_URL_PATTERN = Pattern.compile("^(https?://.*/)doi/([.0-9]+)/([^/]+)$");
  private static final String ACTION_DOWNLOAD_CITATION = "action/downloadCitation";
  private static final String PILL_CITATIONS_ANCHOR = "#pill-citations";
  private static final String DOWNLOAD_RIS_TAIL = "&format=ris&include=cit";

  @Override
  protected LinkExtractor createLinkTagExtractor(String attr) {
    return new AaasLinkTagLinkExtractor(attr);
  }

  /*
   *  Extend the BaseAtyponLinkTagLinkExtractor to create the
   *  RIS metadata URL from the #pill-citations anchor (which links to a form)
   *  all other link tag cases are handled as per BaseAtypon and Jsoup
   */
  public static class AaasLinkTagLinkExtractor extends BaseAtyponLinkTagLinkExtractor {

    private static final Logger logger = Logger.getLogger(AaasLinkTagLinkExtractor.class);

    // nothing needed in the constructor - just call the parent
    public AaasLinkTagLinkExtractor(String attr) {
      super(attr);
    }

    /*
     * Extending the way links are extracted by the Jsoup link extractor in a specific case:
     * In this case we create a link to the citations download page by pulling the DOI from the srcUrl
     * and generating a link like this:
     *     <base_url>action/downloadCitation?doi=...&format=ris&include=cit
     * after creating this link, fall back to standard atypon/jsoup implementations
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();
      Matcher doiMat = DOI_URL_PATTERN.matcher(srcUrl);
      // Are we on a page for which this would be pertinent?
      if ( (srcUrl != null) && doiMat.find()) {
        // now do we have a link to the citations export

        if ((node.hasAttr("href")) ) {
          String hrefval = node.attr("href");
          if (hrefval.contains(PILL_CITATIONS_ANCHOR)) {
            logger.info("found #pill-citations, rewriting");
            // the "/" gets normalized to %2F
            String urlPath = ACTION_DOWNLOAD_CITATION +
                "?doi=" + doiMat.group(2) + "/" + doiMat.group(3) +
                DOWNLOAD_RIS_TAIL;
            // set the href (doesnt work)
            //node.attr("href", "/" + urlPath);
            String newUrl = doiMat.group(1) + urlPath;
            logger.debug3("Generated ris citation url: " + newUrl);
            cb.foundLink(newUrl);
          }
        }
      }
      // allow the parent to get the actual pdf link
      super.tagBegin(node, au, cb);
    }
  }
}
