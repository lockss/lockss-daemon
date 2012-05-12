/*
 * $Id: TaylorAndFrancisHtmlHashFilterFactory.java,v 1.3 2012-05-12 00:22:28 mellen22 Exp $
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.taylorandfrancis;

import java.io.*;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.lf5.util.StreamUtils;
import org.htmlparser.*;
import org.htmlparser.filters.*;
import org.htmlparser.tags.*;
import org.htmlparser.util.*;
import org.htmlparser.visitors.NodeVisitor;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class TaylorAndFrancisHtmlHashFilterFactory implements FilterFactory {

  Logger log = Logger.getLogger("TaylorAndFrancisHtmlHashFilterFactory");
  
  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        /*
         * From the crawl filter
         */
        // News articles
        HtmlNodeFilters.tagWithAttribute("div", "id", "newsArticles"),
        // Related and most read articles
        HtmlNodeFilters.tagWithAttribute("div", "id", "relatedArticles"),
        //Ad module
        HtmlNodeFilters.tagWithAttribute("div", "class", "ad module"),
        /*
         * Proper to the hash filter
         */
        // Contains site-specific SFX code
        new TagNameFilter("script"),
        // Contains site-specific SFX markup
        HtmlNodeFilters.tagWithAttribute("a", "class", "sfxLink"),
        // Counterpart of the previous clause when there is no integrated SFX
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "^/servlet/linkout\\?"),
        // Contains institution-specific markup
        HtmlNodeFilters.tagWithAttribute("div", "id", "branding"),
        // Contains a cookie or session ID
        HtmlNodeFilters.tagWithAttribute("link", "type", "application/rss+xml"),
        // Contains the current year in a copyright statement
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "credits"),
        // Contains a cookie or session ID
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "&feed=rss"),
        // Contains a variant phrase "Full access" or "Free access"
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "accessIconWrapper"),
    };
    
    HtmlTransform xform = new HtmlTransform() {
      @Override
      public NodeList transform(NodeList nodeList) throws IOException {
        try {
          nodeList.visitAllNodesWith(new NodeVisitor() {
            @Override
            public void visitTag(Tag tag) {
              try {
                if ("span".equalsIgnoreCase(tag.getTagName()) && tag.getAttribute("id") != null) {
                  tag.removeAttribute("id");
                }
                else if ("div".equalsIgnoreCase(tag.getTagName()) && tag.getAttribute("class") != null && tag.getAttribute("class").startsWith("access ")) {
                  tag.removeAttribute("class");
                }
                else {
                  super.visitTag(tag);
                }
              }
              catch (Exception exc) {
                log.debug2("Internal error (visitor)", exc); // Ignore this tag and move on
              }
            }
          });
        }
        catch (ParserException pe) {
          log.debug2("Internal error (parser)", pe); // Bail
        }
        return nodeList;
      }
    };
    
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     new HtmlCompoundTransform(HtmlNodeFilterTransform.exclude(new OrFilter(filters)),
                                                               xform));
  }

  public static void main(String[] args) throws Exception {
    FilterFactory fact = new TaylorAndFrancisHtmlHashFilterFactory();
    InputStream in = fact.createFilteredInputStream(null, new FileInputStream("/tmp/h0"), "UTF-8");
    IOUtils.copy(in, new FileOutputStream("/tmp/h0.out"));
  }
}
