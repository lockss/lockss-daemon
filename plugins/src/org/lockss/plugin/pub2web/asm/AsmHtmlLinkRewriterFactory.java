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
package org.lockss.plugin.pub2web.asm;

import java.io.InputStream;
import java.io.IOException;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.*;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

/**
 * This custom link rewriter performs Pub2Web  specific rewriting 
 * to make the HTML article files available through Serve Content
 * 
 */
public class AsmHtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  protected static final String FULL_TEXT_ATTR = "data-fullTexturl";
  // XXX must match value used in AsmHtmlLinkExtractorFactory
  protected static final String ART_LISTING_CLASS = "tocheadingarticlelisting";
  
  private static final Logger log =
    Logger.getLogger(AsmHtmlLinkRewriterFactory.class);
  
  /**
   * This link rewriter adds special processing for article HTML links, 
   * The standard ServeContent rewriting in NodeFilterHtmlLinkRewriterFactory is used 
   * with the addition of addAttrToRewrite, addPreXform and addPostXform
   * 
   * See MsHtmlLinkExtractorFactory.java for additional background on the data-* attributes
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
    
    fact.addAttrToRewrite(FULL_TEXT_ATTR);
    fact.addPostXform(new PostFilter());
    fact.addPreXform(new PreFilter());
    
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
  }
  
  
  static class PreFilter implements NodeFilter {
    
    public PreFilter() {
      super();
    }
    
    public boolean accept(Node node) {
      try {
        if (node instanceof Div) {
          Attribute classAttr = ((TagNode)node).getAttributeEx("class");
          if (classAttr != null && classAttr.getValue().contains(ART_LISTING_CLASS)) {
            String url = ((Div)node).toPlainTextString();
            NodeList children = new NodeList(); // removing previous node.getChildren();
            if (url != null) {
              ((TagNode)node).removeAttribute("class");
              ((Div)node).setAttribute("display", "block");
              TagNode ptag = new TagNode();
              ptag.setTagName("p");
              ptag.setParent(node);
              ptag.setStartPosition(node.getStartPosition());
              ptag.setEndPosition(node.getEndPosition());
              children.add(ptag);
              
              TagNode endTag = new TagNode();
              endTag.setTagName("/a");
              endTag.setParent(node);
              endTag.setStartPosition(node.getEndPosition());
              LinkTag link = new LinkTag();
              link.setTagName("a");
              link.setLink(url);
              link.setAttribute("target", "_blank");
              link.setParent(node);
              link.setStartPosition(node.getStartPosition());
              link.setChildren(new NodeList(new TextNode("List of articles")));
              link.setEndTag(endTag);
              children.add(link);
            }
            node.setChildren(children);
          }
        }
      } catch (Throwable ex) {
        log.error(node.toHtml(), ex);
      }
      return false;
    }
  }
  
  private static class PostFilter implements NodeFilter {
    
    public boolean accept(Node node) {
      try {
        /*
         * This filter was not able to rewrite the fulltext tab as a link
         * Rather we display the existing html link near the PDF link
         * also, figure and table links open in a new window/tab
         */
        if (node instanceof LinkTag) {
          Attribute classAttr = ((TagNode)node).getAttributeEx("class");
          if (classAttr != null && classAttr.getValue().contains("media-link")) {
            ((LinkTag)node).setAttribute("target", "_blank");
          }
        }
        else if (node instanceof Bullet) {
          Attribute classAttr = ((TagNode)node).getAttributeEx("class");
          if (classAttr != null && classAttr.getValue().contains("html")) {
            ((Bullet) node).setAttribute("style", "display: block");
          }
        }
      } catch (Throwable ex) {
        log.error(node.toHtml(), ex);
      }
      return false;
    }
  }
}
