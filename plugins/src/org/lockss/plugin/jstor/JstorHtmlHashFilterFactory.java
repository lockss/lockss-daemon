/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.jstor;

import java.io.*;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class JstorHtmlHashFilterFactory implements FilterFactory {

	Logger log = Logger.getLogger(JstorHtmlHashFilterFactory.class);
	
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
	  
    // First filter with HtmlParser
    NodeFilter[] filters = new NodeFilter[] {
	
      /*
       * Crawl filter
       */
      // Articles referencing this article
      HtmlNodeFilters.tagWithAttribute("div", "id", "itemsCiting"),
      // Right column containing related article and ads
      HtmlNodeFilters.tagWithAttribute("div", "class", "rightCol myYahoo"),
      // these links look acceptable but can redirect (eg, if "full" doesn't exist of all articles)
      HtmlNodeFilters.tagWithAttribute("div", "id", "issueNav"),
     
      /*
       * Hash filter
       */
      // Contains ad-specific cookies
      new TagNameFilter("script"),
      new TagNameFilter("noscript"),
      //filter out comments
      HtmlNodeFilters.comment(),      
      // Document header
      new TagNameFilter("head"),
      // Header of toc page
      HtmlNodeFilters.tagWithAttribute("div", "class", "head globalContainer"),
      // Containing copyright
      HtmlNodeFilters.tagWithAttribute("div", "class", "footer"),
      // Containing copyright
      HtmlNodeFilters.tagWithAttribute("div", "class", "foot"),
      // Containing name information
      HtmlNodeFilters.tagWithAttribute("div", "class", "banner"),
      HtmlNodeFilters.tagWithAttribute("div",  "class", "infoBox"),
      // author info may get global update after the article
      HtmlNodeFilters.tagWithAttribute("div",  "class", "articleBody_author"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "articleFoot"),
      
      HtmlNodeFilters.tagWithAttribute("div", "class", "marketingLinks"),
      HtmlNodeFilters.tagWithAttribute("div", "id", "journalLinks"),
      HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "issueTools"),
      HtmlNodeFilters.tagWithAttribute("div", "class", "subCite"),
      //<div id="marketing-survey" class="hide">
      HtmlNodeFilters.tagWithAttribute("div", "id", "marketing-survey"),
      //<div id="SCDataSiteWide" data-institution="Stanford University" ...
      //information about who/how connecting to site for scripts
      HtmlNodeFilters.tagWithAttribute("div", "id", "SCDataSiteWide"),
      
    };
    
    HtmlTransform xform = new HtmlTransform() {
        @Override
        public NodeList transform(NodeList nodeList) throws IOException {
          try {
            nodeList.visitAllNodesWith(new NodeVisitor() {
              @Override
              //the "rel" attribute on link tags are using variable named values
              public void visitTag(Tag tag) {
                if (tag instanceof LinkTag && tag.getAttribute("rel") != null) {
                  tag.removeAttribute("rel");
                }
              }
            });
          } catch (ParserException pe) {
            IOException ioe = new IOException();
            ioe.initCause(pe);
            throw ioe;
          }
          return nodeList;
        }
    };   
    
    //return new HtmlFilterInputStream(in, HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    return new HtmlFilterInputStream(in, encoding,
        new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)), xform));
  }
  
}
