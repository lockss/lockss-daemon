/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.oup;

import java.io.*;

import org.apache.commons.io.IOUtils;
import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;
import org.lockss.plugin.silverchair.BaseScHtmlHashFilterFactory;
import org.lockss.util.Constants;
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
  
  public static void main(String[] args) throws Exception {
    String file1 = "/tmp/data/oup1.html";
    String file2 = "/tmp/data/oup2.html";
    String file3 = "/tmp/data/oup3.html";
    String file4 = "/tmp/data/oup4.html";
    IOUtils.copy(new OupScHtmlHashFilterFactory().createFilteredInputStream(null, 
        new FileInputStream(file1), Constants.ENCODING_UTF_8), 
        new FileOutputStream(file1 + ".hout"));
    IOUtils.copy(new OupScHtmlHashFilterFactory().createFilteredInputStream(null,
        new FileInputStream(file2), Constants.ENCODING_UTF_8),
        new FileOutputStream(file2 + ".hout"));
    IOUtils.copy(new OupScHtmlHashFilterFactory().createFilteredInputStream(null,
        new FileInputStream(file3), Constants.ENCODING_UTF_8),
        new FileOutputStream(file3 + ".hout"));
    IOUtils.copy(new OupScHtmlHashFilterFactory().createFilteredInputStream(null,
        new FileInputStream(file4), Constants.ENCODING_UTF_8),
        new FileOutputStream(file4 + ".hout"));
  }
}
