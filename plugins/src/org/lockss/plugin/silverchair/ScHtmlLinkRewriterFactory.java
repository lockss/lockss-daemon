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

package org.lockss.plugin.silverchair;

import java.io.InputStream;
import java.io.IOException;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.*;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

/**
 * This custom link rewriter performs Silverchair specific rewriting 
 * to make the PDF article files available through Serve Content
 * 
 */
public class ScHtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  // Should match value in ScHtmlLinkExtractorFactory
  protected static final String DATA_ARTICLE_URL_ATTR = "data-article-url";
  protected static final String DATA_AJAX_ATTR = "data-ajax-url";
  
  private static final Logger log =
    Logger.getLogger(ScHtmlLinkRewriterFactory.class);
  
  /**
   * This link rewriter adds special processing for article PDF links, 
   * it finds and rewrites PDF links
   * 
   * See ScHtmlLinkExtractorFactory.java for background on the data-article-* attributes
   * 
   */
  @Override
  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xfm)
      throws PluginException, IOException {
    
    NodeFilterHtmlLinkRewriterFactory fact =
      new NodeFilterHtmlLinkRewriterFactory();
    
    fact.addAttrToRewrite(DATA_ARTICLE_URL_ATTR);
    fact.addAttrToRewrite("data-original");
    fact.addPostXform(new PostFilter());
    
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
  }
  
  
  private static class PostFilter implements NodeFilter {
    
    public boolean accept(Node node) {
      try {
        if (node instanceof TagNode) {
          
          Attribute idAttr = ((TagNode)node).getAttributeEx("id");
          Attribute classAttr = ((TagNode)node).getAttributeEx("class");
          
          if (node instanceof LinkTag) {
            
            Attribute urlAttr = ((TagNode)node).getAttributeEx(DATA_ARTICLE_URL_ATTR);
            // id="item_MultimediaUrl"
            if (urlAttr != null && classAttr != null) {
              String classVal = classAttr.getValue();
              if (!classVal.contains("readcube-epdf")) {
                ((TagNode)node).removeAttribute(DATA_AJAX_ATTR);
                String pdfUrl = urlAttr.getValue();
                if (pdfUrl != null && !pdfUrl.isEmpty()) {
                  ((TagNode)node).setAttribute("class", "rewritten pdf link");
                  ((LinkTag)node).setAttribute("href", pdfUrl);
                  ((LinkTag)node).setAttribute("target", "_blank");
                }
              }
            } else if (idAttr != null && idAttr.getValue().matches("item_MultimediaUrl")) {
              ((LinkTag)node).setAttribute("target", "_blank");
            }
          }
          else if (node instanceof Div) {
            if (idAttr != null && idAttr.getValue().matches("(article|figure|table|supplemental|multimedia)Tab")) {
              ((TagNode)node).removeAttribute("style");
              ((Div) node).setAttribute("style", "display: block");
            }
          }
          else if (node instanceof ImageTag) {
            Attribute attr = ((ImageTag)node).getAttributeEx("data-original");
            if (attr != null) {
              String orig = attr.getValue();
              ((ImageTag)node).setImageURL(orig);
            }
          }
        }
      } catch (Throwable ex) {
        log.error(node.toHtml(), ex);
      }
      return false;
    }
  }
}
