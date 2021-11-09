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

package org.lockss.plugin.atypon.asm;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.tags.Div;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.filter.html.HtmlTags;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponHtmlHashFilterFactory;
import org.lockss.util.Logger;

import java.io.InputStream;

public class AsmHtmlHashFilterFactory extends BaseAtyponHtmlHashFilterFactory {

  private static final Logger log = Logger.getLogger(AsmHtmlHashFilterFactory.class);

  /**
   * <p>
   * This node filter determines if a node meets any of a number of
   * criteria (characterized by a list of sub tree node filters) and
   * filters it out if it is in a target tree (characterized by a
   * target root node filter),
   * </p>
   */
  public static class onlyTheseSubtrees implements NodeFilter {

    protected NodeFilter rootNodeFilter;
    protected NodeFilter[] subTreeNodeFilters;

    public onlyTheseSubtrees(NodeFilter rootNodeFilter,
                             NodeFilter[] subTreeNodeFilters) {
      this.rootNodeFilter = rootNodeFilter;
      this.subTreeNodeFilters = subTreeNodeFilters;
    }

    @Override
    public boolean accept(Node node) {
      // Determine if the node meets any of the subTreeNodeFilters
      for (NodeFilter filter : subTreeNodeFilters) { if (filter.accept(node)) {
        // Inspect the node's ancestors
        for (Node current = node; current != null; current = current.getParent()) {
          if (rootNodeFilter.accept(current)) { return true; }
        }
      }}
      return false;
    }
  }

  public static Node GetFirstChildTag(Node parent) {
    Node child = parent.getFirstChild();
    while (child != null) {
      if (child instanceof Tag) {
        return child;
      }
      child = child.getNextSibling();
    }
    return null;
  }

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {

    NodeFilter[] asmFilters = new NodeFilter[] {
      // remove the external links in the citation section. sometimes the various links are missing
      HtmlNodeFilters.tagWithAttribute("div", "class", "external-links"),
      // sometimes missing tags with the publish history section, remove only them
      new onlyTheseSubtrees(
        HtmlNodeFilters.tagWithAttribute("section", "class", "core-history"),
        new NodeFilter[]{
          HtmlNodeFilters.tagWithAttribute("div", "class", "pubmed"),
          HtmlNodeFilters.tag("br")
        }
      ),
      // sometimes missing tags within div role=doc-footnote, remove only them
      new onlyTheseSubtrees(
          HtmlNodeFilters.tagWithAttribute("div", "role", "doc-footnote"),
          new NodeFilter[]{
              HtmlNodeFilters.tagWithAttribute("div", "class", "label"),
          }
      ),
      // Check if the img src is a dynamically generated path. remove the src link if so.
        // e.g.
        // <img src="/cms/10.1128/ecosalplus.ESP-0008-2016/asset/887afaf0-ba50-4a7d-ac07-8af515c6489e/assets/graphic/esp-0008-2016_fig_002.gif" >
        // ASM  <img src="/cms/asset/4c00858f-aabd-495c-a0b1-59c46ac3afe0/jb.2021.203.issue-10.cover.gif" alt="Journal of Bacteriology cover image"/>
      HtmlNodeFilters.tagWithAttributeRegex("img", "src", "/cms/.*assets?/"),

        // <div class="issue-item__header">

      HtmlNodeFilters.tagWithAttribute("div", "class", "issue-item__header"),

      // the We recommend section at the bottom of the page can differ, or not be present at all.
      // remove the entire parent div.
      new NodeFilter() {
        @Override
        public boolean accept(Node node) {
          if (node instanceof Div) {
            String className = ((Div) node).getAttribute("class");
            if (className != null && !className.isEmpty() && className.equals("container")) {
              Node child = GetFirstChildTag(node);
              if (child != null) {
                Node grandChild = GetFirstChildTag(child);
                if (grandChild != null) {
                  Node greatGrandChild = GetFirstChildTag(grandChild);
                  if (greatGrandChild instanceof HtmlTags.Section) {
                    String sectionClass = ((HtmlTags.Section) greatGrandChild).getAttribute("class");
                    if (sectionClass != null && sectionClass.equals("we-recommend")) {
                      return true;
                    }
                  }
                }
              }
            }
          }
          return false;
        }
      }

    };
    // add the various filters to BaseAtyponHashFilters
    return super.createFilteredInputStream(au, in, encoding, asmFilters);
  }
  // enable this filter since h3 id on the toc pages is dynamically generated.
  // e.g. <h3 id="h_d101805e5959" class="to-section">
  @Override
  public boolean doTagIDFiltering() { return true; }

}
