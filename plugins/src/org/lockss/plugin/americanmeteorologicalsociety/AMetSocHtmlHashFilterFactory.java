/*
 * $Id: AMetSocHtmlHashFilterFactory.java,v 1.2 2013-05-02 20:33:08 alexandraohlson Exp $
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

package org.lockss.plugin.americanmeteorologicalsociety;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Vector;

import org.htmlparser.NodeFilter;
import org.htmlparser.Tag;
import org.htmlparser.filters.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class AMetSocHtmlHashFilterFactory implements FilterFactory {
  
  Logger log = Logger.getLogger("AMetSocHtmlHashFilterFactoryy");

  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Variable identifiers - institution doing the crawl
        new TagNameFilter("script"),
        // Contains name and logo of institution
        HtmlNodeFilters.tagWithAttribute("div", "id", "identity-bar"),
        // Contains copyright year
        HtmlNodeFilters.tagWithAttribute("div", "id", "footer"),
        // Contains "current issue" link which will change over time
        HtmlNodeFilters.tagWithAttribute("div", "id", "journalNavPanel"),
        // Contains the changeable list of citations
        HtmlNodeFilters.tagWithAttribute("div", "class", "citedBySection"),
        // Contains the linbrary specific "find it" button
        HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
    };
        HtmlTransform xform = new HtmlTransform() {
          @Override
          public NodeList transform(NodeList nodeList) throws IOException {
            try {
              nodeList.visitAllNodesWith(new NodeVisitor() {
                @Override
                public void visitTag(Tag tag) {
                  String tagName = tag.getTagName().toLowerCase();
                  // Need to remove <span class="titleX" id="xxxxx"> because id is variable
                  // cannot remove span tag pair because contents are content. Just remove the id value
                  if ( ("span".equals(tagName))  && (tag.getAttribute("id") != null) ){
                    tag.setAttribute("id", "0");
                  }
                }
              });
            }
            catch (Exception exc) {
              log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
            }
            return nodeList;
          }
        };   
        
        // Also need white space filter to condense multiple white spaces down to 1
        InputStream filtered = new HtmlFilterInputStream(in,
            encoding,
            new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xform));
        
        Reader filteredReader = FilterUtil.getReader(filtered, encoding);
        return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));

  }

}
