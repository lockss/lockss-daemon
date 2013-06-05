/*
 * $Id: HindawiPublishingCorporationHtmlFilterFactory.java,v 1.13 2013-06-05 21:47:43 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.hindawi;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Vector;

import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.*;
import org.lockss.filter.StringFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class HindawiPublishingCorporationHtmlFilterFactory implements FilterFactory {
  
  protected static final Logger logger = Logger.getLogger(HindawiPublishingCorporationHtmlFilterFactory.class);

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)                                     
                                               
      throws PluginException {
        NodeFilter[] filters = new NodeFilter[] {
            // Contains changing <meta> tags
            new TagNameFilter("head"),
            // Filter out <script> tags that seem to be edited often
            new TagNameFilter("script"),
            // Filter out <div id="left_column">...</div>
            HtmlNodeFilters.tagWithAttribute("div", "id", "left_column"),
            // ASP cookies; once without '__', now with  
            HtmlNodeFilters.tagWithAttribute("input", "id", "VIEWSTATE"),
            HtmlNodeFilters.tagWithAttribute("input", "id", "__VIEWSTATE"),
            // ASP cookies; once without '__', now with  
            HtmlNodeFilters.tagWithAttribute("input", "id", "EVENTVALIDATION"),
            HtmlNodeFilters.tagWithAttribute("input", "id", "__EVENTVALIDATION"),
            // not sure if this will work because the <svg> goop is embedded between the <span></span>
            HtmlNodeFilters.tagWithAttributeRegex("span", "style", "^width"),
            // right hand actions column may or may not have "citations to this article" which changes over time
            HtmlNodeFilters.tagWithAttribute("div", "class", "right_column_actions"),
    };
        HtmlTransform xform = new HtmlTransform() {
          @Override
          public NodeList transform(NodeList nodeList) throws IOException {
            try {
              nodeList.visitAllNodesWith(new NodeVisitor() {
                @Override
                public void visitTag(Tag tag) {
                  String tagName = tag.getTagName().toLowerCase();
                  // An issue with variable amounts of attributes following the <html ....> 
                  if ("html".equals(tagName)) {
                    tag.setAttributesEx(new Vector()); //empty attribs Vector. Even clears out tag name
                    tag.setTagName("html");
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

    InputStream htmlFilter = new HtmlFilterInputStream(in,
                                                       encoding,
                                                       new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),
                                                           xform));

    Reader reader = FilterUtil.getReader(htmlFilter, encoding);
    // consolidate white space before doing tagfilter stuff     
    Reader WSReader = new WhiteSpaceFilter(reader);
    Reader filtReader = makeFilteredReader(WSReader);
    return new ReaderInputStream(filtReader);
  }
  
  // Noisy whitespace has already been removed
  static Reader makeFilteredReader(Reader reader) {
    List tagList = ListUtil.list(
        // Remove DOCTYPE declaration which seems to vary but is not a node in the DOM
        new TagPair("<!DOCTYPE", ">", false, false),
        new TagPair("<svg", "/svg>", true, false)
    );
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader, tagList);
    String[][] findAndReplace = new String[][] { 
        // out of sync - some versions have extraneous single spaces, so remove between tags
        {"> <", "><"},
        // remove leading space after tags (extra spaces will already have been consolidated down to one 
        {"> ", ">"},
    };
    Reader stringFilter = StringFilter.makeNestedFilter(tagFilter,
        findAndReplace, false);
    return stringFilter;
   }

}

/*
    // so some replace on strings
    String[][] findAndReplace = new String[][] {
        // use of &nbsp; or " " inconsistent over time
        {"&nbsp;", " "}, 
        // out of sync - some versions have extraneous single spaces, so remove between tags
        {"> <", "><"},
    };
    Reader stringFilter = StringFilter.makeNestedFilter(tagFilter,
                                                          findAndReplace,
                                                          false);

    return new ReaderInputStream(new WhiteSpaceFilter(stringFilter));   
*/