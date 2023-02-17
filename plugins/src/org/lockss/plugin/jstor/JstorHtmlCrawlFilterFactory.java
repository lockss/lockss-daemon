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
