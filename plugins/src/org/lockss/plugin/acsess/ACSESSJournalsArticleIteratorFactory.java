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
       https://dl.sciencesocieties.org/publications/jeq/abstracts/27/5/JEQ0270051094
- preview pdf (abs 2): https://dl.sciencesocieties.org/publications/cns/abstracts/47/1/20/preview
- html full text: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57
                  https://dl.sciencesocieties.org/publications/aj/articles/106/3/1070a
- pdf: https://dl.sciencesocieties.org/publications/aj/pdfs/106/1/57
- tables only: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57?show-t-f=tables&wrapper=no
- figures only: https://dl.sciencesocieties.org/publications/aj/articles/106/1/57?show-t-f=figures&wrapper=no
- supplement: https://dl.sciencesocieties.org/publications/jeq/supplements/43/177-supplement.pdf
                https://dl.sciencesocieties.org/publications/aj/supplements/106/645-supplement1.xlsx
                https://dl.sciencesocieties.org/publications/aj/supplements/106/645-supplement2.pdf
- EndNote: https://dl.sciencesocieties.org/publications/citation-manager/down/en/aj/106/5/1677
- ProCite Ris: https://dl.sciencesocieties.org/publications/citation-manager/down/pc/aj/106/5/1677
- MARC: https://dl.sciencesocieties.org/publications/citation-manager/down/marc/aj/106/5/1677
- RefWorks: https://dl.sciencesocieties.org/publications/citation-manager/down/refworks/aj/106/5/1677
 */
public class ACSESSJournalsArticleIteratorFactory
  implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
  
  public static final String ROLE_HTML_ABSTRACT = "HtmlAbstract";
  public static final String ROLE_PREVIEW_PDF_ABSTRACT = "PreviewPdfAbstract";
  public static final String ROLE_TABLES_ONLY = "TablesOnly";
  public static final String ROLE_FIGURES_ONLY = "FiguresOnly";
  public static final String ROLE_PDF_SUPPLEMENT = "PdfSupplement";
  public static final String ROLE_PDF_SUPPLEMENT_1 = "PdfSupplement1";
  public static final String ROLE_PDF_SUPPLEMENT_2 = "PdfSupplement2";
  public static final String ROLE_XLSX_SUPPLEMENT = "XlsxSupplement";
  public static final String ROLE_XLSX_SUPPLEMENT_1 = "XlsxSupplement1";
  public static final String ROLE_XLSX_SUPPLEMENT_2 = "XlsxSupplement2";
  public static final String ROLE_CITATION_MARC = "CitationMarc";
  public static final String ROLE_CITATION_REFWORKS = "CitationRefworks";

  private static final String ROOT_TEMPLATE = "\"%spublications/\", base_url";
  
  // pattern template must include all primary aspects
  // abstracts, preview pdf abstracts, html full text, and pdf full text  
  private static final String PATTERN_TEMPLATE = 
      "\"^%spublications/%s/(abstracts|articles|pdfs)/\\d+/\\d+/[^?/]+(/preview)?$\", base_url, journal_id";
  
  // primary aspects need their own patterns
  private Pattern HTML_PATTERN = Pattern.compile(      
      "/publications/([^/]+)/articles/(\\d+)/(\\d+)/([^/]+)$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/publications/$1/articles/$2/$3/$4";
  private Pattern ABSTRACT_PATTERN = Pattern.compile(      
      "/publications/([^/]+)/abstracts/(\\d+)/(\\d+)/([^/]+)$", Pattern.CASE_INSENSITIVE);
  private static final String ABSTRACT_REPLACEMENT = "/publications/$1/abstracts/$2/$3/$4";    
  private Pattern PREVIEW_PDF_ABSTRACT_PATTERN = Pattern.compile(      
      "/publications/([^/]+)/abstracts/(\\d+)/(\\d+)/([^/]+)/preview$", Pattern.CASE_INSENSITIVE);
  private static final String PREVIEW_PDF_ABSTRACT_REPLACEMENT = "/publications/$1/abstracts/$2/$3/$4/preview";
  private Pattern PDF_PATTERN = Pattern.compile(      
      "/publications/([^/]+)/pdfs/(\\d+)/(\\d+)/([^/]+)$", Pattern.CASE_INSENSITIVE);
  private static final String PDF_REPLACEMENT = "/publications/$1/pdfs/$2/$3/$4";

  private static final String TABLES_REPLACEMENT = "/publications/$1/articles/$2/$3/$4?show-t-f=tables&wrapper=no";
  private static final String FIGURES_REPLACEMENT = "/publications/$1/articles/$2/$3/$4?show-t-f=figures&wrapper=no";
  private static final String PDF_SUPPLEMENT_REPLACEMENT = "/publications/$1/supplements/$2/$4-supplement.pdf";
  private static final String PDF_SUPPLEMENT_1_REPLACEMENT = "/publications/$1/supplements/$2/$4-supplement1.pdf";
  private static final String PDF_SUPPLEMENT_2_REPLACEMENT = "/publications/$1/supplements/$2/$4-supplement2.pdf";
  private static final String XLSX_SUPPLEMENT_REPLACEMENT = "/publications/$1/supplements/$2/$4-supplement.xlsx";
  private static final String XLSX_SUPPLEMENT_1_REPLACEMENT = "/publications/$1/supplements/$2/$4-supplement1.xlsx";
  private static final String XLSX_SUPPLEMENT_2_REPLACEMENT = "/publications/$1/supplements/$2/$4-supplement2.xlsx";
    
  private static final String ENDNOTE_REPLACEMENT = "/publications/citation-manager/down/en/$1/$2/$3/$4";
  private static final String PROCITE_RIS_REPLACEMENT = "/publications/citation-manager/down/pc/$1/$2/$3/$4";
  private static final String MARC_REPLACEMENT = "/publications/citation-manager/down/marc/$1/$2/$3/$4";
  private static final String REFWORKS_REPLACEMENT = "/publications/citation-manager/down/refworks/$1/$2/$3/$4";
  
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
    builder.addAspect(PDF_PATTERN,
                      PDF_REPLACEMENT, 
                      ArticleFiles.ROLE_FULL_TEXT_PDF);     
    builder.addAspect(ABSTRACT_PATTERN,
                      ABSTRACT_REPLACEMENT,
                      ROLE_HTML_ABSTRACT);
    builder.addAspect(PREVIEW_PDF_ABSTRACT_PATTERN,
                      PREVIEW_PDF_ABSTRACT_REPLACEMENT,
                      ROLE_PREVIEW_PDF_ABSTRACT);  
    
    builder.addAspect(TABLES_REPLACEMENT, 
                      ROLE_TABLES_ONLY);  
    builder.addAspect(FIGURES_REPLACEMENT, 
                      ROLE_FIGURES_ONLY);
    builder.addAspect(PDF_SUPPLEMENT_REPLACEMENT, 
                      ROLE_PDF_SUPPLEMENT);
    builder.addAspect(PDF_SUPPLEMENT_1_REPLACEMENT, 
                      ROLE_PDF_SUPPLEMENT_1);
    builder.addAspect(PDF_SUPPLEMENT_2_REPLACEMENT, 
                      ROLE_PDF_SUPPLEMENT_2);
    builder.addAspect(XLSX_SUPPLEMENT_REPLACEMENT, 
                      ROLE_XLSX_SUPPLEMENT);
    builder.addAspect(XLSX_SUPPLEMENT_1_REPLACEMENT, 
                      ROLE_XLSX_SUPPLEMENT_1);
    builder.addAspect(XLSX_SUPPLEMENT_2_REPLACEMENT, 
                      ROLE_XLSX_SUPPLEMENT_2);
    
    builder.addAspect(ENDNOTE_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION_ENDNOTE);
    builder.addAspect(PROCITE_RIS_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION_RIS);
    builder.addAspect(MARC_REPLACEMENT,
                      ROLE_CITATION_MARC);
    builder.addAspect(REFWORKS_REPLACEMENT,
                      ROLE_CITATION_REFWORKS);
    
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML,
                                 ArticleFiles.ROLE_FULL_TEXT_PDF,
                                 ROLE_HTML_ABSTRACT,
                                 ROLE_PREVIEW_PDF_ABSTRACT);
    
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ABSTRACT,
                                  ROLE_HTML_ABSTRACT,
                                  ROLE_PREVIEW_PDF_ABSTRACT);
   
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
