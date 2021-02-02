/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.asco;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.tags.Div;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;

public class AscoHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  static NodeFilter[] filters = new NodeFilter[] {

    //article page - center column with tools, most-read, etc
    HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-tools"),
          HtmlNodeFilters.tagWithAttributeRegex(
                 "a", "href", "/action/showCitFormats\\?")),
    //toc center column
    HtmlNodeFilters.tagWithAttributeRegex("div", "class", "toc-tools"),
    HtmlNodeFilters.tagWithAttribute("table", "class", "references"),
    
    // ASCO has a number of articles with in-line unlabelled links to other
    // volumes. The WORST is here:
    // http://ascopubs.org/doi/full/10.1200/JCO.2016.68.2146
    // JCO-34 alone (a huge volume of 7000+ articles) had 136
    // do not follow "doi/(abs|full)/" href when found within either full or abstract body
    // div class of hlFld-Fulltext or hlFld-Abstrct, 
    new AndFilter(
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "doi/(abs|full)/"),
        HtmlNodeFilters.ancestor(HtmlNodeFilters.tagWithAttributeRegex("div", "class", "^hlFld-(Fulltext|Abstract)"))
    ),

      // there is a 'box' embedded in article for asco (may be others) that includes only a few aarticles outside the AU
      // and is difficult to pin down as the id, and class name are not unique
      // it seems that div.id = 'b1' is always a 'The Bottom Line' section, if it is present. But ...
      // this box is not always present. So we need to filter on the wording title itself 'Related ASCO Guidelines'
      // on this page is an example: https://ascopubs.org/doi/full/10.1200/JCO.20.02672
      //  <div id="b2" class="boxed-text-anchor">
      //    <div class="NLM_sec NLM_sec_level_2">
      //      <div class="head-b">Related ASCO Guidelines</div>
      // This page uses id=b1 for the box, as it is missing a 'The Bottom Line' section
      // https://ascopubs.org/doi/full/10.1200/JCO.20.00611
      // these filters would be nice, but would not catch all instances
      // HtmlNodeFilters.tagWithAttributeRegex("div","id", "b2"), // this is not guaranteed
      // <div class="boxed-text-float" >
      // <div class="boxed-text-anchor">   is used in older articles
      // e.g.   https://ascopubs.org/doi/10.1200/JCO.2016.70.1474
      // so lets put it all together
      new NodeFilter() {
        @Override public boolean accept(Node node) {
          if (HtmlNodeFilters.tagWithAttributeRegex("div", "class", "boxed-text-(float|anchor)").accept(node)) {
            log.debug2("found tag btfa: " + node.toPlainTextString());
            Node firstChild = node.getFirstChild();
            if (firstChild != null && firstChild instanceof Div) {
              // case the firstChild to a Div type
              Div firstDiv = (Div)firstChild;
              Node secondChild = firstDiv.getFirstChild();
              if (secondChild != null && secondChild instanceof Div) {
                Div secondDiv = (Div)secondChild;
                // this title seems regular, however, the capitalization is not. additionally, there is sometimes a ':' at the
                // end, i don't think this matters, just noting the inconsistency
                Pattern titlePattern = Pattern.compile("Related ASCO (Guideline|Standard)", Pattern.CASE_INSENSITIVE);
                // this method returns the inner text, e.g. <div class="head-b">Related ASCO Guidelines</div>
                // becomes 'Related ASCO Guidelines'
                String tagText = secondDiv.toPlainTextString();
                return titlePattern.matcher(tagText).find();
              }
            }
          }
          return false;
        }
      },
  };


  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return super.createFilteredInputStream(au, in, encoding, filters);
  }

}
