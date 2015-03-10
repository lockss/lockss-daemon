/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.nature;

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

public class NaturePublishingGroupArticleIteratorFactory
implements ArticleIteratorFactory,
ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(NaturePublishingGroupArticleIteratorFactory.class);

  protected static final String ROOT_TEMPLATE = "\"%s%s/journal/v%s/\", base_url, journal_id, volume_name";

  // we trap both full html AND pdfs as primary aspects
  protected static final String PATTERN_TEMPLATE = "\"^%s%s/journal/v[^/]+/n[^/]+/(full/[^/]+\\.html|pdf/[^/]+\\.pdf)$\", base_url, journal_id, volume_name";

  // Primary aspects of article
  // note that exlusion syntax (?!foo) doesn't count as a matching group
  // Do not pick up the <vol>/<issue>/full/index.html which is a 404 served as 200
  private static final Pattern HTML_PATTERN = Pattern.compile("/full/(?!index)([^/]+)\\.html$", Pattern.CASE_INSENSITIVE);
  // Do not pick up the <vol>/<issue>/pdf/toc.pdf or masthead.pdf as articles there is one each
  // per issue and they aren't really articles and they don't have metadata
  private static final Pattern PDF_PATTERN = Pattern.compile("/pdf/(?!toc|masthead)([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);

  // how to change from one form (aspect) of article to another
  private static final String HTML_REPLACEMENT = "/full/$1.html";
  private static final String PDF_REPLACEMENT = "/pdf/$1.pdf";

  // Things not an "article" but in support of an article
  private static final String ABSTRACT_REPLACEMENT = "/abs/$1.html";
  private static final String RIS_REPLACEMENT = "/ris/$1.ris";
  private static final String FIG_REPLACEMENT = "/fig_tab/$1_ft.html";
  private static final String SUPPL_REPLACEMENT = "/suppinfo/$1.html";

  /*
   * The Nature URL structure means that the HTML for an article is
   * at a URL like http://www.nature.com/gt/journal/v16/n5/full/gt200929a.html
   * ie <base_url>/<journal_id>/journal/v<volume> is the subtree we want.
   */
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target,
        ROOT_TEMPLATE,
        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);


    // The order in which these aspects are added is important. They determine which will trigger
    // the ArticleFiles and if you are only counting articles (not pulling metadata) then the 
    // lower aspects aren't looked for, once you get a match.

    // set up PDF to be an aspect that will trigger an ArticleFiles
    builder.addAspect(PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    // set up full text html to be an aspect that will trigger an ArticleFiles
    builder.addAspect(HTML_PATTERN,
        HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA); // use for metadata if abstract doesn't exist

    // an abstract alone should not be enough to trigger an ArticleFiles
    builder.addAspect(ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    // set a role, but it isn't sufficient to trigger an ArticleFiles
    // First choice is &include=cit; second choice is &include=abs (AMetSoc)
    builder.addAspect(RIS_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_RIS);
    // set a role, but it isn't sufficient to trigger an ArticleFiles
    builder.addAspect(SUPPL_REPLACEMENT,
        ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);
    // set a role, but it isn't sufficient to trigger an ArticleFiles
    builder.addAspect(FIG_REPLACEMENT,
        ArticleFiles.ROLE_FIGURES_TABLES);

    // Now prioritize various items

    // The order in which we want to define full_text_cu.  
    // First one that exists will get the job
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_HTML);  

    // set the ROLE_ARTICLE_METADATA to the first one that exists
    // 3/9/2015 - don't use RIS for metadata - it is very limited
    // whereas the html metadata seems comprehensive
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
        //ArticleFiles.ROLE_CITATION_RIS,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
