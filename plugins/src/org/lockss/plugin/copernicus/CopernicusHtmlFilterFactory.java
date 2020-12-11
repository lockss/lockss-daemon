/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.copernicus;

import java.io.*;
import java.util.Vector;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.HtmlTagFilter;
import org.lockss.filter.StringFilter;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.HtmlTagFilter.TagPair;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class CopernicusHtmlFilterFactory implements FilterFactory {

        Logger log = Logger.getLogger(CopernicusHtmlFilterFactory.class);
        
  public static class FilteringException extends PluginException {
    public FilteringException() { super(); }
    public FilteringException(String msg, Throwable cause) { super(msg, cause); }
    public FilteringException(String msg) { super(msg); }
    public FilteringException(Throwable cause) { super(cause); }
  }

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    // going to keep/drop filtering model
    NodeFilter[] includeNodes = new NodeFilter[] {
        HtmlNodeFilters.tagWithAttribute("div","id", "page_content_container"),

    };
    // going to keep/drop filtering model
    NodeFilter[] excludeNodes = new NodeFilter[] {
        HtmlNodeFilters.tag("script"),
        HtmlNodeFilters.tag("noscript"),
        HtmlNodeFilters.comment(),
        // tabs just above article which are "article", "metrics", "related articles"
        HtmlNodeFilters.tagWithAttribute("td","id","tabnavfull"),
        //author affiliations may change over time
        HtmlNodeFilters.tagWithAttribute("span","class","pb_affiliations"),
        // the generic "host_url/index.html" is picked up because it's a permission page
        // exclude the majority of the contents (news/articles) which will change over time
        // news section
        HtmlNodeFilters.tagWithAttribute("div","id","news"),
        // recent articles listing
        HtmlNodeFilters.tagWithAttributeRegex("div","id","recent_paper"),
        // logos at the bottom
        HtmlNodeFilters.tagWithAttribute("div","id","essentential-logos-carousel"), 
        // date class id differs...
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "publishedDateAndMsType"),
        // list of received/revised/accepted dates sometimes present
        HtmlNodeFilters.tagWithAttribute("div", "class", "articleDates"),
        // The following exclude from the permission page, which we cannot crawl-exclude
        // but all the content is "current", so may differ from older volumes
        HtmlNodeFilters.tagWithAttribute("div","id","highlight_articles"),
        HtmlNodeFilters.tagWithAttribute("div","id","landing_page"),
        // base_url/volumes.html is included now for permission - ever increasing
        // actual issue article TOC is level2Toc
        // okay not to crawl filter as the volume is in the crawl rules
        HtmlNodeFilters.tagWithAttribute("div","class","level1Toc"),

    };
          
    
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              String tagName = tag.getTagName().toLowerCase();
              // addition of new <span style="white-space;nowrap"> but we need the content
              if ( ("span".equals(tagName))  && (tag.getAttribute("style") != null) ){
                Tag endTag = tag.getEndTag();
                // we need the contents in place, so rename the tags for subsequent text based removal
                tag.setAttributesEx(new Vector()); //empty attribs Vector. Even clears out tag name
                tag.setTagName("REMOVE");
                endTag.setTagName("REMOVE");
              } // remove the "class" tag on the div with id=page_content_container
              else if (("div".equals(tagName)) && (tag.getAttribute("id")!= null) && 
                          tag.getAttribute("id").equals("page_content_container")) {
                if (tag.getAttribute("class") != null) 
                  tag.removeAttribute("class");
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
    InputStream htmlFilter = new HtmlFilterInputStream(in,
        encoding,
      new HtmlCompoundTransform(
            HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
            HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)), xform));

    Reader reader = FilterUtil.getReader(htmlFilter, encoding);
    Reader tagFilter = HtmlTagFilter.makeNestedFilter(reader,
        ListUtil.list(   
            // <REMOVE> tags were created in place of specific span tags that needed removal
            new TagPair("<REMOVE", ">")
            ));   
    // do some replace on strings
    String[][] findAndReplace = new String[][] {
        // use of &nbsp; or " " inconsistent over time
        {"&nbsp;", " "}, 
        // out of sync - some versions have extraneous single spaces, 
        // so ADD a space between tags then use whitespace filter
        {"<", " <"},
 
    };
    Reader stringFilter = StringFilter.makeNestedFilter(tagFilter,
                                                          findAndReplace,
                                                          false);    
    
    return new ReaderInputStream(new WhiteSpaceFilter(stringFilter));
  }
}
