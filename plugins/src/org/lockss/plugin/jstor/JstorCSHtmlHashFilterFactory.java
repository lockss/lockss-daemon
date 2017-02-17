
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

Except as contained in this notice, tMassachusettsMedicalSocietyHtmlFilterFactoryhe name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.jstor;

import java.io.*;
import java.util.regex.Pattern;

import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.*;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class JstorCSHtmlHashFilterFactory implements FilterFactory {
  
  private static final Logger log = Logger.getLogger(JstorCSHtmlHashFilterFactory.class);
	
   private static final Pattern MANIFEST_TITLE_PATTERN =
	      Pattern.compile("(Cl|L)ockss App Manifest", Pattern.CASE_INSENSITIVE);

  
  /*
   * Some AUs only get TOC and pdf
   * But some provide full text (see chaucer review)
   * 
   */	  
    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in, 
                                                 String encoding) {
      NodeFilter[] includeNodes = new NodeFilter[] {
          //manifest page - very basic page
          //<h1> with manifest page title followed by a 
          //<ul> of links
          //Find the UL that has a parent that is an H1 with the correct pattern
          new NodeFilter() {
            @Override
            public boolean accept(Node node) {
              if (!(node instanceof BulletList)) return false;
              Node prev_sib = node.getPreviousSibling();
              while (prev_sib != null && (prev_sib instanceof TextNode)) {
                prev_sib = prev_sib.getPreviousSibling();
              }
              if (prev_sib != null && 
                   prev_sib instanceof HeadingTag) {
                String allText = ((CompositeTag)prev_sib).toPlainTextString();
                return MANIFEST_TITLE_PATTERN.matcher(allText).find();
              }
              return false;
            }
          },          
 
          // toc - contents only
          HtmlNodeFilters.tagWithAttribute("div", "class", "toc-view"),
          // citation/info
          HtmlNodeFilters.tagWithAttribute("div", "id", "citationBody"),
          // for those papers that have full text html
          HtmlNodeFilters.tagWithAttribute("div", "id", "full_text_tab_contents"),
          // must pick small portion of journal_info or it will hash0 - parts of it are ever changing
          // http://www.jstor.org/stable/10.5325/pennhistory.82.1.0001?item_view=journal_info
          HtmlNodeFilters.tagWithAttributeRegex("div", "class", "journal_description"),
      };
      
      NodeFilter[] excludeNodes = new NodeFilter[] {       
          HtmlNodeFilters.tagWithAttribute("div","id","citation-tools"),
          // the span will change over time
          HtmlNodeFilters.tagWithAttribute("div","id","journal_info_drop"),
          //might change offerings and not pertinent content
          HtmlNodeFilters.tagWithAttribute("ul","id","export-bulk-drop"),
          
          // the value of data-issue-key is variable - just remove the associated tag
          HtmlNodeFilters.tagWithAttributeRegex("div", "data-issue-key", ".*"),

      };
      return getFilteredInputStream(au, in, encoding,
          includeNodes, excludeNodes);
    }
    
    
    
    public InputStream getFilteredInputStream(ArchivalUnit au, InputStream in,
        String encoding, NodeFilter[] includeNodes, NodeFilter[] excludeNodes) {
      if (excludeNodes == null) {
        throw new NullPointerException("excludeNodes array is null");
      }  
      if (includeNodes == null) {
        throw new NullPointerException("includeNodes array is null!");
      }   
      InputStream filtered;
      filtered = new HtmlFilterInputStream(in, encoding,
                   new HtmlCompoundTransform(
                       HtmlNodeFilterTransform.include(new OrFilter(includeNodes)),
                       HtmlNodeFilterTransform.exclude(new OrFilter(excludeNodes)))
                 );
      
      Reader reader = FilterUtil.getReader(filtered, encoding);
      return new ReaderInputStream(reader); 
    }
  
}
