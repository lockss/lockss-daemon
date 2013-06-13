/*
 * $Id: FutureScienceHtmlHashFilterFactory.java,v 1.4 2013-06-13 21:45:46 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.futurescience;

import java.io.InputStream;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Remark;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.util.SimpleNodeIterator;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.HtmlFilterInputStream;
import org.lockss.filter.html.HtmlNodeFilterTransform;
import org.lockss.filter.html.HtmlNodeFilters;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;

public class FutureScienceHtmlHashFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
          throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Variable identifiers - institution doing the crawl  - including genSfxLinks() which is the institution button
        new TagNameFilter("script"),
        // contains the institution banner on both TOC and article pages
        HtmlNodeFilters.tagWithAttribute("div", "class", "institutionBanner"),
        // welcome and login
        HtmlNodeFilters.tagWithAttribute("span", "class", "identitiesName"),
        // footer at the bottom with copyright, etc.
        HtmlNodeFilters.tagWithAttribute("div", "class", "bottomContactInfo"),
        // article page - right side column has "related articles found in:" in the Quick Links box
        HtmlNodeFilters.tagWithAttribute("td", "class", "quickLinks_content"),
        // Left side columns has list of Journals (might change over time) and current year's catalog
        //<table class="sideMenu mceItemTable" cellpadding="2" width="165">
        HtmlNodeFilters.tagWithAttribute("table", "class", "sideMenu mceItemTable"),
        
        // article pages (abstract, reference, full) have a "cited by" section which will change over time
        HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
        
        // articles have a section "Users who read this also read..." which is tricky to isolate
        // It's a little scary, but <div class="full_text"> seems only to be used for this section (not to be confused with fulltext)
        // though I could verify that it is followed by <div class="header_divide"><h3>Users who read this article also read:</h3></div>
        HtmlNodeFilters.tagWithAttribute("div", "class", "full_text"),

        // TOC has ad placeholders in various places - tricky to isolate
        // The comment is always there, the ad may or may not follow
        // The placeholder comments appear to always be in <td> </td> chunks, often with other stuff as well
        // Look for <!-- placeholder id=null...--> comment and if there is one, remove the <a .....> .... </a> chunk after it
        // This is most easily done by removing the entire <td> </td> tag element
       new NodeFilter() {      
          // look for a <td> that has a comment <!-- placeholder id=null....--> child somewhere in it. If it's there remove it.
          @Override public boolean accept(Node node) {
            if (!(node instanceof TableColumn)) return false;
              Node childNode = node.getFirstChild();
              while (childNode != null) {
                if (childNode instanceof Remark) {
                  String remarkText = childNode.getText();
                  if ( (remarkText != null) && remarkText.contains("placeholder id=null") ) return true;
                }
                childNode = childNode.getNextSibling();
              }
              return false;
          }
        }
        
        // Some article listings have a "free" glif. Might that change status over time?

    };
    return new HtmlFilterInputStream(in,
        encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));

  }
}

