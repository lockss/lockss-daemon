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
in this Software without prior written authorization from Stanford University.
be used in advertising or otherwise to promote the sale, use or other dealings

*/

package org.lockss.plugin.silvafennica;

//import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.util.Logger;

public class FS2ArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(FS2ArticleIteratorFactory.class);
  
  // https://www.silvafennica.fi/pdf/article1313.pdf
  // https://www.silvafennica.fi/article/1588
  // https://www.silvafennica.fi/export/1588
  private static final String ROOT_TEMPLATE = "\"%s\", base_url";
  private static final String PATTERN_TEMPLATE = "\"^%sarticle/[0-9]+$\", base_url";
  
  private static final Pattern HTML_PATTERN = Pattern.compile("/article/([0-9]+)$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/article/$1";
  //private static final Pattern PDF_PATTERN = Pattern.compile("/pdf/article/([0-9]+)[.]pdf$", Pattern.CASE_INSENSITIVE);
  private static final String PDF_REPLACEMENT = "/pdf/article$1.pdf";
  //private static final Pattern ENW_PATTERN = Pattern.compile("/export/([0-9]+)$", Pattern.CASE_INSENSITIVE);
  private static final String ENW_REPLACEMENT = "/export/$1";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
          throws PluginException {
    // ArrayList<String> ary = new ArrayList<String>();
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    builder.setSpec(target, ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    builder.addAspect(HTML_PATTERN, HTML_REPLACEMENT, ArticleFiles.ROLE_FULL_TEXT_HTML);
    builder.addAspect(PDF_REPLACEMENT, ArticleFiles.ROLE_FULL_TEXT_PDF);
    builder.addAspect(ENW_REPLACEMENT, ArticleFiles.ROLE_CITATION_ENDNOTE);
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, ArticleFiles.ROLE_CITATION_ENDNOTE);
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target) throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
