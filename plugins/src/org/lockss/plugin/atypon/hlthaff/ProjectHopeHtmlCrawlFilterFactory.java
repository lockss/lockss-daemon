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

package org.lockss.plugin.atypon.hlthaff;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.Bullet;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class ProjectHopeHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {

  protected static final Pattern corrections = Pattern.compile("^( |&nbsp;)*(original article):?", Pattern.CASE_INSENSITIVE);

  NodeFilter[] filters = new NodeFilter[] {
      HtmlNodeFilters.tag("header"),
      HtmlNodeFilters.tag("footer"),
      // HtmlNodeFilters.tag("nav"),
      HtmlNodeFilters.allExceptSubtree(
          HtmlNodeFilters.tag("nav"),
          new OrFilter( // HtmlCompoundTransform(
              HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/doi/pdf/"),
              HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/action/showCitFormats[?]"))),

      // Article landing - ajax tabs
      HtmlNodeFilters.tagWithAttribute("li", "id", "pane-pcw-references"),
      HtmlNodeFilters.tagWithAttribute("li", "id", "pane-pcw-related"),
      // minor
      HtmlNodeFilters.tagWithAttribute("section", "class", "article__metrics"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "toc-header__top"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "sidebar-region"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "article__topic"),
      HtmlNodeFilters.tagWithAttribute("ul", "class", "social-links"),
      // in case there are links in the preview text
      HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-item__abstract"),
      // References
      HtmlNodeFilters.tagWithAttributeRegex("li", "class", "references__item"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "disqus_thread"),
      // never want these links, excluded lists was too long
      HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "/servlet/linkout[?]type="),
      HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/author/"),
      
      // an insidious source of over crawling
      // 5/1/18 - adding check for pattern in the <li>
      new NodeFilter() {
        @Override public boolean accept(Node node) {
          if (!(node instanceof LinkTag)) return false;
          Node parent = node.getParent();
          if (!(parent instanceof Bullet)) return false;
          String allText = ((CompositeTag)parent).toPlainTextString();
          return corrections.matcher(allText).find();
        }
      },

  };
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return super.createFilteredInputStream(au, in, encoding, filters);
  }
  
  /*public static void main(String[] args) throws Exception {
    String file1 = "/home/etenbrink/workspace/data/ha1.html";
    String file2 = "/home/etenbrink/workspace/data/ha2.html";
    String file3 = "/home/etenbrink/workspace/data/ha3.html";
    String file4 = "/home/etenbrink/workspace/data/ha5.html";
    IOUtils.copy(new HealthAffairsHtmlCrawlFilterFactory().createFilteredInputStream(null, 
        new FileInputStream(file1), Constants.DEFAULT_ENCODING), 
        new FileOutputStream(file1 + ".out"));
    IOUtils.copy(new HealthAffairsHtmlCrawlFilterFactory().createFilteredInputStream(null,
        new FileInputStream(file2), Constants.DEFAULT_ENCODING),
        new FileOutputStream(file2 + ".out"));
    IOUtils.copy(new HealthAffairsHtmlCrawlFilterFactory().createFilteredInputStream(null,
        new FileInputStream(file3), Constants.DEFAULT_ENCODING),
        new FileOutputStream(file3 + ".out"));
    IOUtils.copy(new HealthAffairsHtmlCrawlFilterFactory().createFilteredInputStream(null,
        new FileInputStream(file4), Constants.DEFAULT_ENCODING),
        new FileOutputStream(file4 + ".out"));
  }*/
  
}
