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

package org.lockss.plugin.ingenta.hbku;

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

import java.util.Iterator;
import java.util.regex.Pattern;

public class HBKUArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(HBKUArticleIteratorFactory.class);


  /*
    https://www.qscience.com/content/journals/10.5339/ahcsps.2011.10
    https://www.qscience.com/content/journals/10.5339/ahcsps.2011.10/cite/bibtex
    https://www.qscience.com/content/journals/10.5339/ahcsps.2011.10?crawler=true
    https://www.qscience.com/content/journals/10.5339/ahcsps.2011.10?crawler=true&mimetype=text/html
    https://www.qscience.com/content/journals/10.5339/ahcsps.2011.10?crawler=true&mimetype=text/pdf
   */

  protected static final String ROOT_TEMPLATE =
          "\"%scontent/journals/\", base_url";

  protected static final String PATTERN_TEMPLATE =
          "\"%scontent/journals/([^/]+/[^.]+\\.[0-9]{4}\\.\\d+)(\\?crawler=true.*)?\", base_url, journal_id";

  // Groups:
  // 1. unique identifier for this chapter/book
  // don't need to be so explicit about doi syntax because PATTERN_TEMPLATE is restrictive
  protected Pattern PDF_PATTERN =
          Pattern.compile("/([^/]+/[^.]+\\.[0-9]{4}\\.\\d+)\\?crawler=true&mimetype=application/pdf", Pattern.CASE_INSENSITIVE);
  protected static String PDF_REPLACEMENT = "/$1?crawler=true&mimetype=application/pdf";

  protected Pattern HTML_PATTERN =
          Pattern.compile("/([^/]+/[^.]+\\.[0-9]{4}\\.\\d+)\\?crawler=true&mimetype=text/html", Pattern.CASE_INSENSITIVE);
  protected static String HTML_REPLACEMENT = "/$1?crawler=true&mimetype=html";

  protected Pattern LANDING_PATTERN =
          Pattern.compile("/([^/]+/[^.]+\\.[0-9]{4}\\.\\d+)$", Pattern.CASE_INSENSITIVE);
  protected static String LANDING_REPLACEMENT = "/$1";

  // secondary aspects, get from the others $1 = journal or book; $2 is the identifying inf
  protected static String CITATION_BIB_REPLACEMENT = "/$1/cite/bibtex";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target,
            ROOT_TEMPLATE,
            PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    builder.addAspect(PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    builder.addAspect(LANDING_PATTERN,
            LANDING_REPLACEMENT,
            ArticleFiles.ROLE_ABSTRACT,
            ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE);

    builder.addAspect(CITATION_BIB_REPLACEMENT,
            ArticleFiles.ROLE_CITATION_BIBTEX);

    builder.addAspect(HTML_PATTERN,
            HTML_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_HTML);

    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
            ArticleFiles.ROLE_FULL_TEXT_HTML,
            ArticleFiles.ROLE_ABSTRACT);

    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
            ArticleFiles.ROLE_ABSTRACT);
    
    return builder.getSubTreeArticleIterator();

  }

  // Create Article Metadata Extractor
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}