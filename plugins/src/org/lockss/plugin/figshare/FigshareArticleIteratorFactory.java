/*
 * $Id$
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

package org.lockss.plugin.figshare;

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.ListUtil;
import org.lockss.util.Constants;
import org.lockss.util.Logger;

/*
 * No PDF; Full Text HTML: http://figshare.com/article/full-text-pdf/56564
 * not collecting the above file - too many parts will change too often
 * manifest page (for 2011): 
 * http://api.figshare.com/v1/clockss/articles/listing?from=2011-01-01&to=2011-12-31
 * HTML file on manifest page: http://api.figshare.com/v1/articles/16?format=html
 *   requesting Figshare put traditional metadata info in the html (as they do for
 *   their full-text-html and their informational links in href format, so we can
 *   collect more easily.  Otherwise, need to write a specialized metadataformat 
 *   collector and link extractor.
 */

public class FigshareArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger("FigshareArticleIteratorFactory");
  // example: http://api.figshare.com/v1/articles/229?format=html
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url, ";   // for figshare.com
  protected static final String API_ROOT_TEMPLATE = "\"%s\", api_url, ";        // for api.figshare.com
  protected static final String PATTERN_TEMPLATE = "\"v1/articles/(\\d)+\\?format=html$\", base_url";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    // example: http://api.figshare.com/v1/articles/229?format=html

    final Pattern HTML_PATTERN = Pattern.compile("(v1/articles/[\\d]+\\?format=html)$");
    final String HTML_REPL = "$1";
    builder.setSpec(target, API_ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    // only meaningful substance on this page is pdfs
    builder.addAspect(HTML_PATTERN, HTML_REPL, ArticleFiles.ROLE_FULL_TEXT_HTML, 
        ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
