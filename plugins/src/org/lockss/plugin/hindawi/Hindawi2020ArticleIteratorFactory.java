/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.hindawi;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class Hindawi2020ArticleIteratorFactory
    implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static final String ROOT_TEMPLATE_HTML = "\"%sjournals/%s/%d/\", base_url, journal_id, year";
  protected static final String ROOT_TEMPLATE_PDF = "\"%sjournals/%s/%d/\", download_url, journal_id, year";
  /* limit html art_id to  > 3 digits because article id's are generally ~6 digits and TOC 
   * uses same format but with low number (indicating page of articles)
   * Don't need to put digit limitation pdf version - will never be a TOC
   * article example: http://www.hindawi.com/journals/ijmms/1978/231678/
   * toc example: http://www.hindawi.com/journals/aaa/2013/ --> http://www.hindawi.com/journals/aaa/2013/14/ (1,373 articles)
   */
  protected static final String PATTERN_TEMPLATE = "\"^(%sjournals/%s/%d/\\d{4,}|%sjournals/%s/%d/\\d+\\.pdf)$\", base_url, journal_id, year, download_url, journal_id, year";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    String base_url = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String download_url = au.getConfiguration().get("download_url");
    
    /* Abstract at top of full text html */
    /* References at bottom of full text html */
    final Pattern HTML_PATTERN = Pattern.compile(String.format("^%sjournals/([^/]+)/([^/]+)/(\\d+)$", base_url), Pattern.CASE_INSENSITIVE);
    final String HTML_ABSTRACT_REFERENCE_REPLACEMENT = String.format("%sjournals/$1/$2/$3", base_url);

    final Pattern PDF_PATTERN = Pattern.compile(String.format("^%sjournals/([^/]+)/([^/]+)/(\\d+)\\.pdf$", download_url), Pattern.CASE_INSENSITIVE);
    final String PDF_REPLACEMENT = String.format("%sjournals/$1/$2/$3.pdf", download_url);

    /* Citation, XML, & ePub links found on right sidebar */
    final String RIS_REPLACEMENT = String.format("%sjournals/$1/$2/$3.ris", base_url);
    final String XML_REPLACEMENT = String.format("%sjournals/$1/$2/$3.xml", download_url);
    final String EPUB_REPLACEMENT = String.format("%sjournals/$1/$2/$3.epub", download_url);
    
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
                    Arrays.asList(ROOT_TEMPLATE_HTML, ROOT_TEMPLATE_PDF),
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    builder.addAspect(HTML_PATTERN,
                      HTML_ABSTRACT_REFERENCE_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_HTML,
                      ArticleFiles.ROLE_ARTICLE_METADATA);
    
    builder.addAspect(HTML_ABSTRACT_REFERENCE_REPLACEMENT,
                      ArticleFiles.ROLE_ABSTRACT);

    builder.addAspect(HTML_ABSTRACT_REFERENCE_REPLACEMENT,
                      ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(HTML_ABSTRACT_REFERENCE_REPLACEMENT,
                      ArticleFiles.ROLE_REFERENCES);
    
    builder.addAspect(PDF_PATTERN,
                      PDF_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    builder.addAspect(RIS_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION_RIS);
                      
    builder.addAspect(XML_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_XML);

    builder.addAspect(EPUB_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_EPUB);
    
    // Use the abstract preferentially to extract metadata
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
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
