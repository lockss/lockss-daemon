/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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
import org.lockss.plugin.AuUtil;
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
    fact.addPostXform(new PostFilter(au));
    
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
    protected static ArchivalUnit thisau;
    
    public PostFilter(ArchivalUnit au) {
      super();
      thisau = au;
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
            htmlUrl = AuUtil.normalizeHttpHttpsFromBaseUrl(thisau, htmlUrl);
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
                mediaUrl = AuUtil.normalizeHttpHttpsFromBaseUrl(thisau, mediaUrl);
                if (mediaTabNode != null) {
                  ((TagNode)mediaTabNode).setAttribute("href", mediaUrl);
                  ((TagNode)mediaTabNode).setAttribute("target", "_blank");
                }
              } else if (cl.contains("supplement")) {
                supplUrl = ajaxAttr.getValue();
                supplUrl = AuUtil.normalizeHttpHttpsFromBaseUrl(thisau, supplUrl);
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
