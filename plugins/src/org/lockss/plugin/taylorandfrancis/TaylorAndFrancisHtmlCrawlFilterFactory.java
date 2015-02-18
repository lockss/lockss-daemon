/*
 * $Id$
 */

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

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.plugin.taylorandfrancis;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.*;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

/*STANDALONE - DOES NOT INHERIT FROM BASE ATYPON */
public class TaylorAndFrancisHtmlCrawlFilterFactory implements FilterFactory {

  protected static final Pattern corrections = Pattern.compile("Original Article|Corrigendum|Correction", Pattern.CASE_INSENSITIVE);
  
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
        //full article has in-line references with direct links 
        //example: http://www.tandfonline.com/doi/full/10.1080/09064702.2012.670665#.U0weNlXC02c
        // reference #20
        HtmlNodeFilters.tagWithAttribute("span",  "class", "referenceDiv"),     
        // full page with references in a list at the bottom - some with direct links, see
        //example: http://www.tandfonline.com/doi/full/10.1080/09064702.2012.670665#.U0weNlXC02c
        // reference #20
        HtmlNodeFilters.tagWithAttribute("ul",  "class", "references"),     
        // if has "doi/mlt" will crawl filter out - but remove in case it doesn't
        HtmlNodeFilters.tagWithAttribute("li",  "class", "relatedArticleLink"),     
        
        //do not follow breadcrumb back to TOC in case of overcrawl to article
        HtmlNodeFilters.tagWithAttribute("div", "id", "breadcrumb"),
        //and if you get to TOC, don't follow links in header (next/prev)
        HtmlNodeFilters.tagWithAttribute("div", "class", "hd"),
        
        // on a Corrigendum abstract or full text page, there will be a link to "Original Article"
        // and on the Original Article page there will be a link back to the "Corrigendum"
        // No obvious attributes, so just look for the text
        new NodeFilter() {
          @Override public boolean accept(Node node) {
            if (!(node instanceof LinkTag)) return false;
            String allText = ((CompositeTag)node).toPlainTextString();
            return corrections.matcher(allText).find();
//            //using regex - the "i" is for case insensitivity; the "s" is for accepting newlines
//            return (allText.matches("(?is).*Original Article.*") || allText.matches("(?is).*Corrigendum.*") 
//                || allText.matches("(?is).*Correction.*"));
          }
        },
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
