/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.ama;

import org.jsoup.nodes.Node;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.extractor.JsoupHtmlLinkExtractor.SimpleTagLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class AmaScHtmlLinkExtractorFactory implements LinkExtractorFactory {
  
  private static final Logger logger = Logger.getLogger(AmaScHtmlLinkExtractorFactory.class);
  
  private static final String ANCHOR_TAG = "a";
  private static final String START_URL_STR = "LOCKSS/ListOfIssues.aspx?";
  protected static final String AUTH_SEARCH_STR = "searchresults?author=";
  
  @Override
  public LinkExtractor createLinkExtractor(String mimeType) throws PluginException {
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false, false, null, null);
    registerExtractors(extractor);
    return extractor;
  }
  
  /*
   *  For when it is insufficient to simply use a different link tag or script
   *  tag link extractor class, a child plugin can override this and register
   *  additional or alternate extractors
   */
  protected void registerExtractors(JsoupHtmlLinkExtractor extractor) {
    
    extractor.registerTagExtractor(ANCHOR_TAG,
                                   new ScAMAAnchorTagExtractor(new String[]{
                                       "href",
                                       "data-article-url",
                                       }));
  }
  
  public static class ScAMAAnchorTagExtractor extends SimpleTagLinkExtractor {
    
    public ScAMAAnchorTagExtractor(final String[] attrs) {
      super(attrs);
    }
    
    /**
     * Extract link(s) from this tag for attributes href, onclick, download.
     * We process each of the attributes in turn since more than one may be present.
     *
     * @param node the node containing the link
     * @param au Current archival unit to which this html document belongs.
     * @param cb A callback to record extracted links.
     */
    public void tagBegin(Node node, ArchivalUnit au, Callback cb) {
      //the <a href attribute handler
      String href;
      if (node.hasAttr("href") && ((href = node.attr("href")) != null)) {
        if (href.contains("/issue/")) {
          // <a href="http://jamanetwork.com/journals/jama/issue/315/2">12January - Volume 315, Issue 2</a>
          String srcUrl = node.baseUri();
          // ListOfIssues.aspx?resourceId=67&year=2016
          if (srcUrl.contains(START_URL_STR))
            JsoupHtmlLinkExtractor.checkLink(node, cb, "href");
        }
        // we look for searchresults?author= and exclude these
        else if (!href.contains(AUTH_SEARCH_STR)) {
          JsoupHtmlLinkExtractor.checkLink(node, cb, "href");
        }
      }   // end <a href
      
      if (node.hasAttr("data-article-url")) {
        String url = node.attr("data-article-url");
        if (!StringUtil.isNullString(url)) {
          cb.foundLink(url);
        }
      }
    }
  }
}
