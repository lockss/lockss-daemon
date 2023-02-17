/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
       cannot assume issue is numeric - could be S1, Supplement_1, 5_Supplement, etc
       https://dl.sciencesocieties.org/publications/cs/articles/57/supplement1/S-73
- preview html landing: https://dl.sciencesocieties.org/publications/cns/abstracts/47/1/20/preview
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
  //public static final String ROLE_PREVIEW_PDF_ABSTRACT = "PreviewPdfAbstract";
  public static final String ROLE_PREVIEW_HTML_LANDING = "PreviewHtmlLanding";
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
  // path just after volume isn't just a number - could be supplement, etc
  private static final String PATTERN_TEMPLATE = 
      "\"^%spublications/%s/(abstracts|articles|pdfs)/%s/[^/]+/[^?/]+(/preview)?$\", base_url, journal_id, volume_name";
  
  // primary aspects need their own patterns
  // the PATTERN is already limiting to this one volume so don't require it to be any particular format
  private Pattern HTML_PATTERN = Pattern.compile(      
      "/publications/([^/]+)/articles/([^/]+)/([^/]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);
  private static final String HTML_REPLACEMENT = "/publications/$1/articles/$2/$3/$4";
  private Pattern ABSTRACT_PATTERN = Pattern.compile(      
      "/publications/([^/]+)/abstracts/([^/]+)/([^/]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);
  private static final String ABSTRACT_REPLACEMENT = "/publications/$1/abstracts/$2/$3/$4";    
  private Pattern PREVIEW_PDF_ABSTRACT_PATTERN = Pattern.compile(      
      "/publications/([^/]+)/abstracts/([^/]+)/([^/]+)/([^/]+)/preview$", Pattern.CASE_INSENSITIVE);
  private static final String PREVIEW_HTML_LANDING_REPLACEMENT = "/publications/$1/abstracts/$2/$3/$4/preview";
  private Pattern PDF_PATTERN = Pattern.compile(      
      "/publications/([^/]+)/pdfs/([^/]+)/([^/]+)/([^/]+)$", Pattern.CASE_INSENSITIVE);
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
                      PREVIEW_HTML_LANDING_REPLACEMENT,
                      ROLE_PREVIEW_HTML_LANDING);  
    
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
                                 ROLE_PREVIEW_HTML_LANDING);
    
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ABSTRACT,
                                  ROLE_HTML_ABSTRACT,
                                  ROLE_PREVIEW_HTML_LANDING);
   
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
