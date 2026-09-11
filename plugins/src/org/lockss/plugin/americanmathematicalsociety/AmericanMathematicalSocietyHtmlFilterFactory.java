/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.americanmathematicalsociety;

import java.io.InputStream;
import java.io.Reader;

import org.htmlparser.NodeFilter;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.TagNameFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.filter.WhiteSpaceFilter;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.ReaderInputStream;

public class AmericanMathematicalSocietyHtmlFilterFactory implements FilterFactory {
  Logger log = Logger.getLogger(AmericanMathematicalSocietyHtmlFilterFactory.class);

  public InputStream createFilteredInputStream(
      ArchivalUnit au, InputStream in, String encoding)
          throws PluginException {
    NodeFilter[] filters = new NodeFilter[] {
        // Aggressive filtering of non-content tags
        new TagNameFilter("script"),
        // Ribbon is at top of page with society links and icons
        HtmlNodeFilters.tagWithAttribute("table", "id", "ribbon"),
        // Journal link block does not have a another useful attribute, hope this text
        // remains constant: <table summary="Table that holds logos and navigation">
        HtmlNodeFilters.tagWithAttributeRegex("table", "summary", "logos and navigation"),
        // issue and article navigation links that can change
        HtmlNodeFilters.tagWithAttribute("td", "id", "navCell"),
        // not sure what this contains but in case the number changes, removing XXX
        HtmlNodeFilters.tagWithAttribute("div", "class", "altmetric-embed"),
        // Changeable copyright and links
        HtmlNodeFilters.tagWithAttribute("table", "id", "footer"),
        //remove generated verification tokens
        HtmlNodeFilters.tagWithAttribute("input", "name", "__RequestVerificationToken"),
        //remove generated email links
        HtmlNodeFilters.tagWithAttributeRegex("a", "class", "__cf_email__"),
        HtmlNodeFilters.tagWithAttributeRegex("a", "href", "email-protection"),
        HtmlNodeFilters.tagWithAttributeRegex("span", "class", "__cf_email__"),
        
    };
    
    // Do the initial html filtering
    InputStream filteredStream = new HtmlFilterInputStream(in,encoding,
        HtmlNodeFilterTransform.exclude(new OrFilter(filters)));
    // add whitespace filtering
    Reader filteredReader = FilterUtil.getReader(filteredStream, encoding);
    return new ReaderInputStream(new WhiteSpaceFilter(filteredReader));
  }
  
}

