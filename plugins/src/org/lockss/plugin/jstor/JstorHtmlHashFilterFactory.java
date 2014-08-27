
/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

Except as contained in this notice, tMassachusettsMedicalSocietyHtmlFilterFactoryhe name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

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
