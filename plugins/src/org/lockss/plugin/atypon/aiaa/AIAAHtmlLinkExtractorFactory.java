/* $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.aiaa;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.extractor.JsoupHtmlLinkExtractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
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
    protected Pattern DOI_URL_PATTERN = Pattern.compile("^(https?://.*/)doi/(abs|full)/([.0-9]+)/([^/]+)$");
    private static final String CIT_FORMATS_ACTION = "action/showCitFormats";

    // nothing needed in the constructor - just call the parent
    public AIAALinkTagLinkExtractor(String attr) {
      super(attr);
    }

    /*
     * Extending the way links are extracted by the Jsoup link extractor in a specific case:
     *   - we are on an article page (full or abstract)
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
