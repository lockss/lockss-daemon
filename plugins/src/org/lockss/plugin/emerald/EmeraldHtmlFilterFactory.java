/*
 * $Id: EmeraldHtmlFilterFactory.java,v 1.10 2012-11-12 20:27:36 thib_gc Exp $ 
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.emerald;

import java.io.*;
import java.util.List;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.*;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class EmeraldHtmlFilterFactory implements FilterFactory {

  protected static final Logger logger = Logger.getLogger(EmeraldHtmlFilterFactory.class);
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
        // Contains changing <meta> tags
        new TagNameFilter("head"),
        // Changing scripts
        new TagNameFilter("script"),
        // <br> vs. <br />
        new TagNameFilter("br"),
        // <img> vs. <img />
        new TagNameFilter("img"),
        // Has number of article download in row
        HtmlNodeFilters.tagWithAttribute("td", "headers", "tocopy"),
        // Has related articles download list
        HtmlNodeFilters.tagWithAttribute("td", "headers", "releatedlist"),
        // Alternate related articles download list
        HtmlNodeFilters.tagWithAttribute("table", "class", "articlePrintTable"),
        // Added later
        HtmlNodeFilters.tagWithAttribute("div", "id", "printJournalHead"),
    };
                    
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              String tagName = tag.getTagName().toLowerCase();
              if ("abbr".equals(tagName)) {
                // <... title="..."> vs. <... title= "..."> 
                String title = tag.getAttribute("title");
                if (title != null) {
                  tag.removeAttribute("title");
                  tag.setAttribute("title", title, '"');
                }
              }
              else if (tag instanceof LinkTag) {
                String href = tag.getAttribute("href");
                if (href != null) {
                  // Some wrong internal links got fixed
                  tag.removeAttribute("href");
                  if (!href.startsWith("#")) {
                    // <... href="..."> vs. <... href= "...">
                    tag.setAttribute("href", href, '"');
                  }
                }
                // <... title="..."> vs. <... title= "..."> 
                String title = tag.getAttribute("title");
                if (title != null) {
                  tag.removeAttribute("title");
                  tag.setAttribute("title", title, '"');
                }
              }
            }
          });
        }
        catch (Exception exc) {
          logger.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
        }
        return nodeList;
      }
    };
          

    // First filter with HtmlParser
    InputStream filteredStream = new HtmlFilterInputStream(in,
                                                           encoding,
                                                           new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),
                                                                                     xform));
        

    
    Reader reader = FilterUtil.getReader(filteredStream, encoding);
    Reader filtReader = makeFilteredReader(reader);
    return new ReaderInputStream(filtReader, encoding);
  }

  static Reader makeFilteredReader(Reader reader) {
    List tagList = ListUtil.list(
        new TagPair("<p>Printed from:", "Emerald Group Publishing Limited</p>", false, false),
        // Variable Javascript between intended </body> and a redundant </body>
        new TagPair("</body>", "</html>", false, false)
    );
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader, tagList);
    return new WhiteSpaceFilter(tagFilter);
  }
  
}
