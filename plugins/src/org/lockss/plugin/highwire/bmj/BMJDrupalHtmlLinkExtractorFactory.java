/*
 * $Id: HighWireDrupalHtmlLinkExtractorFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
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

package org.lockss.plugin.highwire.bmj;

/*
 * This will require daemon 1.62 and later for JsoupHtmlLinkExtractor support
 * The vanilla JsoupHtmlLinkExtractor will generate URLs from tags that it finds on pages
 * without restrictions (inclusion/exclusion rules) and so long as those resulting URLs
 * satisfy the crawl rules they will be collected. 
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Node;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;

// an implementation of JsoupHtmlLinkExtractor
public class BMJDrupalHtmlLinkExtractorFactory implements LinkExtractorFactory {
  
  private static final Logger log = Logger.getLogger(BMJDrupalHtmlLinkExtractorFactory.class);
  
  private static final String LINKTAG = "link";
  private static final String HREF_NAME = "href";
  private static final String REL_NAME = "rel";
  private static final String SHORTLINK = "shortlink";
  private static final String DIVTAG = "div";
  
  // eg. http://www.bmj.com/content/348/bmj.f7003
  protected static Pattern URL_PATTERN = Pattern.compile(
      "^(https?://[^/]+/)content/[^/]+/bmj[.][^/?.]+$", Pattern.CASE_INSENSITIVE);
  protected static Pattern NODE_PATTERN = Pattern.compile(
      "^/node/([0-9]+)$", Pattern.CASE_INSENSITIVE);
  
  @Override
  public LinkExtractor createLinkExtractor(String mimeType)
      throws PluginException {
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    // we will do some additional processing for <link href="/node/25370" rel="shortlink">
    extractor.registerTagExtractor(LINKTAG, new HWSimpleTagLinkExtractor(HREF_NAME));
    extractor.registerTagExtractor(DIVTAG, new SimpleTagLinkExtractor(REL_NAME));
    return extractor;
  }
  
  protected static class HWSimpleTagLinkExtractor extends SimpleTagLinkExtractor {
    
    // nothing needed in the constructor - just call the parent
    public HWSimpleTagLinkExtractor(String attr) {
      super(attr);
    }
    
    /*
     * Extending the way links are extracted by the Jsoup link extractor in a specific case:
     *   - we are on an article page
     *   - we hit a link tag of the format:
     *         <link href="/node/<id>" rel="shortlink">
     * In this case we create a link to the RIS page by pulling the node id from the href
     * and generating a link like this:
     *     <base_url>highwire/citation/<id>/ris         
     * In any case other than this one, fall back to standard Jsoup implementation
     */
    @Override
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      String srcUrl = node.baseUri();
      Matcher urlMat = URL_PATTERN.matcher(srcUrl);
      // Are we on a page for which this would be pertinent? (html landing page)
      if ( (srcUrl != null) && urlMat.find()) {
        // if so, generate expected pdf landing page url
        String pdfUrl = srcUrl + ".full.pdf+html";
        log.debug3("Created/added pdf url for: " + srcUrl);
        cb.foundLink(pdfUrl);
        
        // now do we have a citation download href?
        if (node.hasAttr(HREF_NAME) && node.hasAttr(REL_NAME) &&
            SHORTLINK.equals(node.attr(REL_NAME))) {
          Matcher hrefMat = NODE_PATTERN.matcher(node.attr(HREF_NAME));
          if (hrefMat.find()) {
            String newUrl =  urlMat.group(1) + "highwire/citation/" + hrefMat.group(1) + "/ris";
            log.debug3("Created/added new url: " + newUrl);
            cb.foundLink(newUrl);
          }
        }
      }
      // for one reason or another, we didn't handle this. Fall back to standard Jsoup
      super.tagBegin(node, au, cb);
    }
  }
}
