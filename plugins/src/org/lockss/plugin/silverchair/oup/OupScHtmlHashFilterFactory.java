/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.silverchair.oup;

import java.io.*;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.silverchair.BaseScHtmlHashFilterFactory;
import org.lockss.util.Logger;

public class OupScHtmlHashFilterFactory extends BaseScHtmlHashFilterFactory {

  private static final Logger log = Logger.getLogger(OupScHtmlHashFilterFactory.class);
  
  @Override
  protected boolean doExtraSpecialFilter() {
    return false;
  }

  @Override
  public InputStream createFilteredInputStream(final ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    
    NodeFilter[] includeFilters = new NodeFilter[] {
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-list-resources"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "resourceTypeList-OUP_Issue"),
        // We were including 2 copies of the article-body, when contained within the ContentColumn
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-body"),
//        HtmlNodeFilters.allExceptSubtree(
        HtmlNodeFilters.tagWithAttributeRegex("div", "id", "ContentColumn"),
//            HtmlNodeFilters.tagWithAttributeRegex("div", "class", "article-body")),
        HtmlNodeFilters.tagWithAttributeRegex("span", "class", "content-inner-wrap"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "OUP_Issues_List"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "IssuesAndVolumeListManifest"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-instance-OUP_(Figure)?ViewLarge"),
    };
    
    NodeFilter[] moreExcludeFilters = new NodeFilter[] {
        HtmlNodeFilters.tag("script"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "navbar-search"),
        HtmlNodeFilters.tagWithAttributeRegex("div", "class", "widget-(Related|[^ ]+Metadata)"),
        HtmlNodeFilters.allExceptSubtree(
            HtmlNodeFilters.tagWithAttribute("div", "id", "authorInfo_OUP_ArticleTop_Info_Widget"),
            HtmlNodeFilters.tagWithAttribute("div", "class", "name-role-wrap")),
        HtmlNodeFilters.tagWithAttribute("div", "class", "issue-browse__supplement-list-wrap"),
        // HtmlNodeFilters.tagWithAttributeRegex("img", "class", "content-image"),
    };
    
    return createFilteredInputStream(au, in, encoding, includeFilters, moreExcludeFilters);
  }
  
}
