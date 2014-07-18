
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

package org.lockss.plugin.jstor;

import java.io.InputStream;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;

public class JstorHtmlCrawlFilterFactory implements FilterFactory {
  protected static final Pattern corrections = Pattern.compile("Original Article|Corrigendum|Correction|Errata|Erratum", Pattern.CASE_INSENSITIVE);

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
    // Articles referencing this article
    // TODO - find an example of this
    HtmlNodeFilters.tagWithAttribute("div", "id", "itemsCiting"),
    //right column
    // any full article or TOC
    HtmlNodeFilters.tagWithAttribute("div", "class", "rightCol myYahoo"),
    // next/prev can redirect if a "full" format points to one without "full" option
    //ex. http://www.jstor.org/stable/full/10.5325/chaucerrev.47.3.0223 (prev) 
    HtmlNodeFilters.tagWithAttribute("div", "id", "issueNav"),
    // References
    //TODO - find an example of this
    HtmlNodeFilters.tagWithAttribute("div", "id", "references"),
    //  list of references - crawl filter only 
    HtmlNodeFilters.tagWithAttributeRegex("ul", "class", "citeList"),

    // In limited cases we go to  html pages - watch for corrigendum, errata which 
    // can cross over issues.eg
    // http://www.jstor.org/stable/10.1525/ncm.2011.34.issue-3
    // Not all Atypon plugins necessarily need this but MANY do and it is
    // an insidious source of over crawling
    new NodeFilter() {
      @Override public boolean accept(Node node) {
        if (!(node instanceof LinkTag)) return false;
        String allText = ((CompositeTag)node).toPlainTextString();
        return corrections.matcher(allText).find();
      }
    },
    
    };
    return new HtmlFilterInputStream(in,
                                     encoding,
                                     HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
  }

}
