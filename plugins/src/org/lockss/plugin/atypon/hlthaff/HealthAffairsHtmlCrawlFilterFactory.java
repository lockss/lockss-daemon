/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

public class HealthAffairsHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {

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
