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
package org.lockss.plugin.pub2web.ms;

import java.io.InputStream;
import java.io.IOException;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
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
public class MsHtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  protected static final String FULL_TEXT_ATTR = "data-fullTexturl";
  protected static final String AJAX_ATTR = "data-ajaxurl";
  protected static final String ID_ATTR = "id";
  protected static final String FULLTEXT_ID_VAL = "itemFullTextId";
  
  private static final Logger log =
    Logger.getLogger(MsHtmlLinkRewriterFactory.class);
  
  /**
   * This link rewriter adds special processing for article HTML links, 
   * it finds and preserves URLs and tab nodes for fulltext, figures, and supplementary content
   * When both URL and tab node link for each type is known, the link is rewritten.
   * The standard ServeContent rewriting in NodeFilterHtmlLinkRewriterFactory is used 
   * with the addition of addAttrToRewrite and addPostXform
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
      new NodeFilterHtmlLinkRewriterFactory() {
      
    };
    
    fact.addAttrToRewrite(AJAX_ATTR);
    fact.addAttrToRewrite(FULL_TEXT_ATTR);
    fact.addPostXform(new PostFilter());
    
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
  }
  
  
  private static class PostFilter implements NodeFilter {
    
    protected static String htmlUrl;
    protected static Node htmlLinkNode;
    protected static Node htmlTabNode;
    protected static String mediaUrl;
    protected static Node mediaTabNode;
    protected static String supplUrl;
    protected static Node supplTabNode;
    
    public PostFilter() {
      super();
      htmlUrl = null;
      htmlLinkNode = null;
      mediaUrl = null;
      mediaTabNode = null;
      supplUrl = null;
      supplTabNode = null;
    }
    
    public boolean accept(Node node) {
      try {
        if (node instanceof Div) {
          Attribute ftAttr = ((TagNode)node).getAttributeEx(FULL_TEXT_ATTR);
          if (ftAttr != null) {
            htmlUrl = ftAttr.getValue();
            if (htmlLinkNode != null) {
              ((TagNode)htmlLinkNode).setAttribute("href", htmlUrl);
              ((TagNode)htmlLinkNode).setAttribute("target", "_blank");
            }
            if (htmlTabNode != null) {
              ((TagNode)htmlTabNode).setAttribute("href", htmlUrl);
              ((TagNode)htmlTabNode).setAttribute("target", "_blank");
            }
          }
          // <div id="tab3" class="dataandmedia hidden-js-div tabbedsection tab-pane" 
          //        data-ajaxurl="/content/journal/jgv/10.1099/vir.0.069286-0/figures?fmt=ahah">
          Attribute ajaxAttr = ((TagNode)node).getAttributeEx(AJAX_ATTR);
          if (ajaxAttr != null) {
            Attribute idAttr = ((TagNode)node).getAttributeEx("id");
            String idVal = (idAttr != null) ? idAttr.getValue() : null;
            Attribute classAttr = ((TagNode)node).getAttributeEx("class");
            if (classAttr != null) {
              String cl = classAttr.getValue();
              if (cl.contains("dataandmedia")) {
                mediaUrl = ajaxAttr.getValue();
                if (mediaTabNode != null) {
                  ((TagNode)mediaTabNode).setAttribute("href", mediaUrl);
                  ((TagNode)mediaTabNode).setAttribute("target", "_blank");
                }
              } else if (cl.contains("supplement")) {
                supplUrl = ajaxAttr.getValue();
                if (supplTabNode != null) {
                  ((TagNode)supplTabNode).setAttribute("href", supplUrl);
                  ((TagNode)supplTabNode).setAttribute("target", "_blank");
                }
              }
            }
          }
        }
        else if (node instanceof LinkTag) {
          String href = ((LinkTag)node).getLink();
          Attribute hrefAttr = ((TagNode)node).getAttributeEx("href");
          if (hrefAttr != null && hrefAttr.getValue().contains("#tab")) {
            Attribute classAttr = ((TagNode)node).getAttributeEx("class");
            Attribute titleAttr = ((TagNode)node).getAttributeEx("title");
            if (classAttr != null && classAttr.getValue().contains("html")) {
              if (htmlLinkNode != null) {
                log.warning("non-null linkNode " + htmlLinkNode.toString());
              }
              htmlLinkNode = node;
              if (htmlUrl != null) {
                ((TagNode)htmlLinkNode).setAttribute("href", htmlUrl);
                ((TagNode)htmlLinkNode).setAttribute("target", "_blank");
              }
            } else if (titleAttr != null) {
              switch (titleAttr.getValue().substring(0, 4)) {
              case "Full":
                htmlTabNode = node;
                if (htmlUrl != null) {
                  ((TagNode)htmlTabNode).setAttribute("href", htmlUrl);
                  ((TagNode)htmlTabNode).setAttribute("target", "_blank");
                }
                break;
              case "Figs":
                mediaTabNode = node;
                if (mediaUrl != null) {
                  ((TagNode)mediaTabNode).setAttribute("href", mediaUrl);
                  ((TagNode)mediaTabNode).setAttribute("target", "_blank");
                }
                break;
              case "Supp":
                supplTabNode = node;
                if (supplUrl != null) {
                  ((TagNode)supplTabNode).setAttribute("href", supplUrl);
                  ((TagNode)supplTabNode).setAttribute("target", "_blank");
                }
                break;
              default:
                break;
              }
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
