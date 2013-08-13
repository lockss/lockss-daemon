/*
 * $Id: TaylorAndFrancisHtmlCrawlFilterFactory.java,v 1.5 2013-08-13 21:39:25 alexandraohlson Exp $
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

package org.lockss.plugin.taylorandfrancis;

import java.io.InputStream;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;


public class TaylorAndFrancisHtmlCrawlFilterFactory implements FilterFactory {

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // News articles
        HtmlNodeFilters.tagWithAttribute("div", "id", "newsArticles"),
        // Related and most read articles
        HtmlNodeFilters.tagWithAttribute("div", "id", "relatedArticles"),
        //Ads from the publisher
        HtmlNodeFilters.tagWithAttribute("div", "class", "ad module"),
        // links to T&F articles go directly to other article
        HtmlNodeFilters.tagWithAttribute("div",  "id", "referencesPanel"),
        // If cited by other T&F could go directly to other article 
        HtmlNodeFilters.tagWithAttribute("div",  "id", "citationsPanel"),     
        
        // on a Corrigendum abstract or full text page, there will be a link to "Original Article"
        // and on the Original Article page there will be a link back to the "Corrigendum"
        // No obvious attributes, so just look for the text
        new NodeFilter() {
          @Override public boolean accept(Node node) {
            if (!(node instanceof LinkTag)) return false;
            String allText = ((CompositeTag)node).toPlainTextString();
            //using regex - the "i" is for case insensitivity; the "s" is for accepting newlines
            return (allText.matches("(?is).*Original Article.*") || allText.matches("(?is).*Corrigendum.*") 
                || allText.matches("(?is).*Correction.*"));
          }
        },
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
