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
in this Software without prior written authorization from Stanford University.
be used in advertising or otherwise to promote the sale, use or other dealings

*/

package org.lockss.plugin.silverchair;

import java.util.*;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class ScJournalsArticleIteratorFactory
    implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  public static final String ROLE_CITATION_REFWORKS = "CitationRefworks";
  public static final String ROLE_CITATION_MEDLARS = "CitationMedlars";
  
  private static final String ROOT_TEMPLATE = "\"%s\", base_url";
  private static final String PATTERN_TEMPLATE = "\"^%sarticle\\.aspx\\?articleid=\\d+$\", base_url";
  
  private static final Pattern HTML_PATTERN = Pattern.compile("/article\\.aspx\\?articleid=(\\d+)$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/article.aspx?articleid=$1";

  private static final String RIS_REPLACEMENT = "/downloadCitation.aspx?format=ris&articleid=$1";
  private static final String BIBTEX_REPLACEMENT_BIB = "/downloadCitation.aspx?format=bib&articleid=$1";
  private static final String BIBTEX_REPLACEMENT_BIBTEX = "/downloadCitation.aspx?format=bibtex&articleid=$1";
  private static final String MEDLARS_REPLACEMENT = "/downloadCitation.aspx?format=txt&articleid=$1";
  private static final String REFWORKS_REPLACEMENT = "/downloadCitation.aspx?articleid=$1";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    builder.setSpec(target,
                    ROOT_TEMPLATE,
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    builder.addAspect(HTML_PATTERN,
                      HTML_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_HTML);
    builder.addAspect(RIS_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION_RIS);
    builder.addAspect(Arrays.asList(BIBTEX_REPLACEMENT_BIB,
                                    BIBTEX_REPLACEMENT_BIBTEX),
                      ArticleFiles.ROLE_CITATION_BIBTEX);
    builder.addAspect(MEDLARS_REPLACEMENT, 
                      ROLE_CITATION_MEDLARS);
    builder.addAspect(REFWORKS_REPLACEMENT, 
                      ROLE_CITATION_REFWORKS);
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                                  ArticleFiles.ROLE_CITATION_RIS,
                                  ArticleFiles.ROLE_FULL_TEXT_HTML);
    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
