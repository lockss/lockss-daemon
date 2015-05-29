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

package org.lockss.plugin.acsess;

import java.util.Iterator;
import java.util.regex.Pattern;
import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;

/*
 Article files:
- abs: https://dl.sciencesocieties.org/publications/aj/abstracts/106/1/57
- preview pdf (abs 2): https://dl.sciencesocieties.org/publications/cns/abstracts/47/1/20/preview
- html full text: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57
- pdf: https://dl.sciencesocieties.org/publications/aj/pdfs/106/1/57
- tables only: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57?show-t-f=tables&wrapper=no
- figures only: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57?show-t-f=figures&wrapper=no
- EndNote: https://dl.sciencesocieties.org/publications/citation-manager/down/en/aj/106/5/1677
- ProCite Ris: https://dl.sciencesocieties.org/publications/citation-manager/down/pc/aj/106/5/1677
- Zotero Ris: https://dl.sciencesocieties.org/publications/citation-manager/down/zt/aj/106/5/1677
- MARC: https://dl.sciencesocieties.org/publications/citation-manager/down/marc/aj/106/5/1677
- RefWorks: https://dl.sciencesocieties.org/publications/citation-manager/down/refworks/aj/106/5/1677
 */
public class ACSESSJournalsArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
  
  public static final String ROLE_CITATION_REFWORKS = "CitationRefworks";
  // since both TABLES and FIGURES aspects exist, and only one if them
  // can get ArticleFiles.ROLE_FIGURES_TABLES
  public static final String ROLE_FIGURES = "Figures";
  public static final String ROLE_PREVIEW_PDF_ABS = "PreviewPdfAbstract";
  public static final String ROLE_ZOTERO_CITATION_RIS = "ZoteroCitationRis";
  public static final String ROLE_CITATION_MARC = "CitationMarc";

  private static final String ROOT_TEMPLATE = "\"%spublications/\", base_url";
  
  // https://dl.sciencesocieties.org/publications/citation-manager/down/en/aj/106/5/1677
  // https://dl.sciencesocieties.org/publications/aj/articles/106/1/57
  private static final String PATTERN_TEMPLATE = 
      "\"^%spublications/([^/]+)?/(abstracts|articles|pdfs|citation-manager)(/down/[^/]+/[^/]+)?/\\d+/\\d+/\\d+$\", base_url";  

  // https://dl.sciencesocieties.org/publications/aj/articles/106/1/57
  private Pattern HTML_PATTERN = Pattern.compile(      
      "/articles/(\\d+/\\d+/\\d+)$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/articles/$1";
  // https://dl.sciencesocieties.org/publications/aj/abstracts/106/1/57
  private static final String ABS_REPLACEMENT = "/abstracts/$1";
  // https://dl.sciencesocieties.org/publications/cns/abstracts/47/1/20/preview
  private static final String PREVIEW_PDF_ABS_REPLACEMENT = "/abstracts/$1/preview";
  // https://dl.sciencesocieties.org/publications/aj/pdfs/106/1/57
  private static final String PDF_REPLACEMENT = "/pdfs/$1";
  // https://dl.sciencesocieties.org/publications/aj/articles/106/1/57?show-t-f=tables&wrapper=no
  private static final String TABLES_REPLACEMENT = "/articles/$1?show-t-f=tables&wrapper=no";
  // https://dl.sciencesocieties.org/publications/aj/articles/106/1/57?show-t-f=figures&wrapper=no
  private static final String FIGURES_REPLACEMENT = "/articles/$1?show-t-f=figures&wrapper=no";
    
  // https://dl.sciencesocieties.org/publications/citation-manager/down/en/aj/106/5/1677
  private Pattern CITATION_PATTERN = Pattern.compile(
      "/citation-manager/down/[^/]/([^/])/(\\d+/\\d+/\\d+)$", Pattern.CASE_INSENSITIVE);      
  // https://dl.sciencesocieties.org/publications/citation-manager/down/en/aj/106/5/1677
  private static final String ENDNOTE_REPLACEMENT = "/citation-manager/down/en/$1/$2";
  // https://dl.sciencesocieties.org/publications/citation-manager/down/pc/aj/106/5/1677
  private static final String RIS_REPLACEMENT = "/citation-manager/down/pc/$1/$2";
  // https://dl.sciencesocieties.org/publications/citation-manager/down/zt/aj/106/5/1677
  private static final String ZOTERO_RIS_REPLACEMENT = "/citation-manager/down/zt/$1/$2";
  // https://dl.sciencesocieties.org/publications/citation-manager/down/marc/aj/106/5/1677
  private static final String MARC_REPLACEMENT = "/citation-manager/down/marc/$1/$2";
  // https://dl.sciencesocieties.org/publications/citation-manager/down/refworks/aj/106/5/1677
  private static final String REFWORKS_REPLACEMENT = "/citation-manager/down/refworks/$1/$2";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, 
      MetadataTarget target) throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = 
        new SubTreeArticleIteratorBuilder(au);  
    
    builder.setSpec(target,
                    ROOT_TEMPLATE, 
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    builder.addAspect(HTML_PATTERN, 
                      HTML_REPLACEMENT, 
                      ArticleFiles.ROLE_FULL_TEXT_HTML);
    builder.addAspect(ABS_REPLACEMENT,
                      ArticleFiles.ROLE_ABSTRACT);  
    builder.addAspect(PREVIEW_PDF_ABS_REPLACEMENT,
                      ROLE_PREVIEW_PDF_ABS);   
    builder.addAspect(PDF_REPLACEMENT, 
                      ArticleFiles.ROLE_FULL_TEXT_PDF); 
    builder.addAspect(TABLES_REPLACEMENT, 
                      ArticleFiles.ROLE_FIGURES_TABLES);  
    builder.addAspect(FIGURES_REPLACEMENT, 
                      ROLE_FIGURES);  
    builder.addAspect(CITATION_PATTERN,
                      ENDNOTE_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION_ENDNOTE);
    builder.addAspect(RIS_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION_RIS);
    builder.addAspect(ZOTERO_RIS_REPLACEMENT,
                      ROLE_ZOTERO_CITATION_RIS);
    builder.addAspect(MARC_REPLACEMENT,
                      ROLE_CITATION_MARC);
    builder.addAspect(REFWORKS_REPLACEMENT,
                      ROLE_CITATION_REFWORKS);
    
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                                  ArticleFiles.ROLE_CITATION_RIS,
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
