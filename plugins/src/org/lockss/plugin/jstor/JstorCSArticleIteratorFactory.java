/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * JSTOR Current Scholarship version
 * Because they force redirection to PDF for the crawler
 * we only pick up TOC and pdf and the XML and the RIS citation file
 * which we'll use for metadata
 * We need to iterate on the RIS file because in the legacy volumes the PDF doesn't
 * contain the needed first part of the doi
 * Newer volumes use a more consistent doi-based url
 *   http://www.jstor.org/stable/10.2972/hesperia.84.3.0515
 *   http://www.jstor.org/stable/pdf/10.2972/hesperia.84.3.0515.pdf
 *   http://www.jstor.org/citation/ris/10.2972/hesperia.84.3.0515
 *   http://www.jstor.org/doi/xml/10.2972/hesperia.84.3.0515
 *       issue: http://www.jstor.org/stable/10.2972/hesperia.84.issue-3
 * Legacy volumes use an article identifier only - hard to distinguish issues
 *   http://www.jstor.org/stable/40981057
 *   http://www.jstor.org/stable/pdf/40981057.pdf
 *   http://www.jstor.org/citation/ris/10.2307/40981057
 *   http://www.jstor.org/doi/xml/10.2307/40981057
 *       issue: http://www.jstor.org/stable/10.2972/i40044030
 */

public class JstorCSArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(JstorCSArticleIteratorFactory.class);

  // don't set the ROOT_TEMPLATE - it is just base_url

  private static final String PATTERN_TEMPLATE = 
      "\"^%s(citation/ris/|doi/xml/)\", base_url";

  // various aspects of an article - PDF or STABLE may not have the first part of the doi
  // 2nd part of doi do not seem to have "/" in them on JSTOR
  private static final Pattern RIS_PATTERN = Pattern.compile("/citation/ris/([.0-9]+)/([^?&/]+)$", Pattern.CASE_INSENSITIVE);               
  private static final Pattern XML_PATTERN = Pattern.compile("/doi/xml/([.0-9]+)/([^?&/]+)$", Pattern.CASE_INSENSITIVE);               
  // how to get from one of the above to the other
  private final String RIS_REPLACEMENT = "/citation/ris/$1/$2"; //no replacement...just the one
  private final String PDF_REPLACEMENT1 = "/stable/pdf/$1/$2.pdf"; // newer volumes
  private final String PDF_REPLACEMENT2 = "/stable/pdf/$2.pdf"; //legacy
  private final String XML_REPLACEMENT = "/doi/xml/$1/$2"; //consistent across old and new volumes
  private final String STABLE_REPLACEMENT1 = "/stable/$1/$2"; //would be abstract, but redirects to pdf - newer
  private final String STABLE_REPLACEMENT2 = "/stable/$2"; //would be abstract, but redirects to pdf - legacy

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(new SubTreeArticleIterator.Spec()
    .setTarget(target)
    .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    // set up RIS to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(RIS_PATTERN,
        RIS_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_RIS,
        ArticleFiles.ROLE_ARTICLE_METADATA);
    // this should exist and could be an alternative for metadata
    builder.addAspect(XML_PATTERN,
        XML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_XML);
    

    // only one of these two will exists at a time
    builder.addAspect(
        PDF_REPLACEMENT1,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    builder.addAspect(
        PDF_REPLACEMENT2,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    // only one of these two will exist at a time
    builder.addAspect(
        STABLE_REPLACEMENT1,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    builder.addAspect(
        STABLE_REPLACEMENT2,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    // The order in which we want to define full_text_cu.  
    // First one that exists will get the job
    // Leave the CITATION_RIS in because if just doing iterator, it's the only one set
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_XML,
        ArticleFiles.ROLE_CITATION_RIS);

    // set the ROLE_ARTICLE_METADATA to the first one that exists 
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_CITATION_RIS,
        ArticleFiles.ROLE_FULL_TEXT_XML);
    
    return builder.getSubTreeArticleIterator();
  }


  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}

