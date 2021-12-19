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

package org.lockss.plugin.emerald;

import java.io.*;
import java.util.List;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.*;
import org.htmlparser.util.*;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.filter.*;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class EmeraldHtmlFilterFactory implements FilterFactory {

  protected static class EmeraldHtmlTransformWithVisitor implements HtmlTransform {

    protected String charset = null;
    
    @Override
    public NodeList transform(NodeList nodeList) throws IOException {
      try {
        nodeList.visitAllNodesWith(new NodeVisitor() {
          @Override
          public void visitTag(Tag tag) {
            String tagName = tag.getTagName().toLowerCase();
            
            /*
             * <abbr>
             */
            if ("abbr".equals(tagName)) {
              // <... title="..."> vs. <... title= "..."> 
              String title = tag.getAttribute("title");
              if (title != null) {
                tag.removeAttribute("title");
                tag.setAttribute("title", title, '"');
              }
            }
            /*
             * <a> 
             */
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
              // <... target="..."> vs. <... target= "..."> 
              String target = tag.getAttribute("target");
              if (target != null) {
                tag.removeAttribute("target");
                tag.setAttribute("target", target, '"');
              }
            }
            /*
             * <meta>
             */
            else if (tag instanceof MetaTag) {
              if (charset != null) {
                // Only the first one counts
                return;
              }
              MetaTag meta = (MetaTag)tag;
              String httpEquiv = meta.getHttpEquiv();
              if (httpEquiv != null && "content-type".equals(httpEquiv.toLowerCase())) {
                String contentType = meta.getMetaContent();
                if (contentType != null) {
                  charset = HeaderUtil.getCharsetFromContentType(contentType);
                }
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
  }

  protected static final Logger logger = Logger.getLogger(EmeraldHtmlFilterFactory.class);
  
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding) {
    NodeFilter[] filters = new NodeFilter[] {
        // Contains changing <meta> tags
        new TagNameFilter("head"),
        // Changing scripts
        new TagNameFilter("script"),
        new TagNameFilter("noscript"),
        // <br> vs. <br />
        new TagNameFilter("br"),
        // <img> vs. <img />
        new TagNameFilter("img"),
        // Some bars were removed over time
        new TagNameFilter("hr"),
        // Has number of article download in row
        HtmlNodeFilters.tagWithAttribute("td", "headers", "tocopy"),
        // Has related articles download list
        HtmlNodeFilters.tagWithAttribute("td", "headers", "releatedlist"), // Typo?
        HtmlNodeFilters.tagWithAttribute("td", "headers", "relatedlist"),
        // Alternate related articles download list
        HtmlNodeFilters.tagWithAttribute("table", "class", "articlePrintTable"),
        // Added later
        HtmlNodeFilters.tagWithAttribute("div", "id", "printJournalHead"),
        // This heading was later removed
        HtmlNodeFilters.tagWithAttribute("h2", "class", "article"),
        // Changed Infotrieve provider
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^https?://www4\\.infotrieve\\.com/"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^https?://www\\.contentscm\\.com/"),
    };
                    
    EmeraldHtmlTransformWithVisitor xform = new EmeraldHtmlTransformWithVisitor();

    // First filter with HtmlParser
    InputStream filteredStream = new HtmlFilterInputStream(in,
                                                           encoding,
                                                           new HtmlCompoundTransform(xform,
                                                                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters))));
        
    try {
      filteredStream.available(); // Force parse
      if (xform.charset != null) {
        logger.debug2(String.format("Http-Equiv changes charset from %s to %s", encoding, xform.charset));
        encoding = xform.charset;
      }
    }
    catch (IOException ioe) {
      logger.debug2("IOException on available()", ioe);
    }
    
    Reader reader = FilterUtil.getReader(filteredStream, encoding);
    Reader filtReader = makeFilteredReader(reader);
    return new ReaderInputStream(filtReader, encoding);
  }

  static Reader makeFilteredReader(Reader reader) {
    List tagList = ListUtil.list(
        //new TagPair("<p>Printed from:", "Emerald Group Publishing Limited</p>", false, false),
        // Variable Javascript between intended </body> and a redundant </body>
        new TagPair("</body>", "</html>", false, false)
    );
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader, tagList);
    return new WhiteSpaceFilter(tagFilter);
  }
  
}
