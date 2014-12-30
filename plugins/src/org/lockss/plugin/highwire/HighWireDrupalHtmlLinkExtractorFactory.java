/*
 * $Id: HighWireDrupalHtmlLinkExtractorFactory.java,v 1.5 2014-12-30 21:53:34 etenbrink Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

/*
 * This will require daemon 1.62 and later for JsoupHtmlLinkExtractor support
 * The vanilla JsoupHtmlLinkExtractor will generate URLs from tags that it finds on pages
 * without restrictions (inclusion/exclusion rules) and so long as those resulting URLs
 * satisfy the crawl rules they will be collected. 
 */

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
public class HighWireDrupalHtmlLinkExtractorFactory implements LinkExtractorFactory {
  
  private static final Logger log = Logger.getLogger(HighWireDrupalHtmlLinkExtractorFactory.class);
  
  private static final String DIVTAG = "div";
  private static final String RELTAG = "rel";
  
  @Override
  public LinkExtractor createLinkExtractor(String mimeType)
      throws PluginException {
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    // we will do some additional processing for <link href="/node/25370" rel="shortlink"> not needed
    // <div rel"/stuff/more">....</div>  ????
    extractor.registerTagExtractor(DIVTAG, new HWSimpleTagLinkExtractor(RELTAG));
    return extractor;
  }
  
  public static class HWSimpleTagLinkExtractor extends SimpleTagLinkExtractor {
    
    // nothing needed in the constructor - just call the parent
    public HWSimpleTagLinkExtractor(String attr) {
      super(attr);
    }
    
    @Override
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      // now do we have a citation download href?
      if (node.hasAttr(RELTAG)) {
        String newUrl =  node.attr(RELTAG);
        log.debug3("found url: " + newUrl);
      }
      // for one reason or another, we didn't handle this. Fall back to standard Jsoup
      super.tagBegin(node, au, cb);
    }
  }
 //  public static class HWSimpleTagLinkExtractor extends SimpleTagLinkExtractor {
//    
//    
//    // nothing needed in the constructor - just call the parent
//    public HWSimpleTagLinkExtractor(String attr) {
//      super(attr);
//    }
//    
//    /*
//     * Extending the way links are extracted by the Jsoup link extractor in a specific case:
//     *   - we are on an article page
//     *   - we hit a link tag of the format:
//     *         <link href="/node/<id>" rel="shortlink">
//     * In this case we create a link to the RIS page by pulling the node id from the href
//     * and generating a link like this:
//     *     <base_url>highwire/citation/<id>/ris         
//     * In any case other than this one, fall back to standard Jsoup implementation
//     */
//    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
//      String srcUrl = node.baseUri();
//      Matcher urlMat = URL_PATTERN.matcher(srcUrl);
//      // Are we on a page for which this would be pertinent? (html landing page)
//      if ( (srcUrl != null) && urlMat.find()) {
//        // now do we have a citation download href?
//        if (node.hasAttr(HREF) && node.hasAttr(RELTAG) &&
//            SHORTLINK.equals(node.attr(RELTAG))) {
//          Matcher hrefMat = NODE_PATTERN.matcher(node.attr(HREF));
//          if (hrefMat.find()) {
//            String newUrl =  urlMat.group(1) + "highwire/citation/" + hrefMat.group(1) + "/ris";
//            log.debug3("Created/added new url: " + newUrl);
//            cb.foundLink(newUrl);
//          }
//        }
//      }
//      // for one reason or another, we didn't handle this. Fall back to standard Jsoup
//      super.tagBegin(node, au, cb);
//    }
//  }
//  
}