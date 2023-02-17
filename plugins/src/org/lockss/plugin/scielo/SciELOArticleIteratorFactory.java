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

package org.lockss.plugin.scielo;

import java.util.*;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class SciELOArticleIteratorFactory
      implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(SciELOArticleIteratorFactory.class);
  
  /*
   * The fulltext URL:
   *  http://www.scielo.br/scielo.php?script=sci_arttext&pid=S0102-67202014000400251&lng=en
   *  http://www.scielo.br/scielo.php?script=sci_arttext&pid=S0102-67202014000400251&lng=en&tlng=pt
   *  
   * The pdf landing page:
   *  http://www.scielo.br/scielo.php?script=sci_pdf&pid=S0102-67202014000400280&lng=..
   *  
   * The pdf URL:
   *  http://www.scielo.br/readcube/epdf.php?doi=10.1590/S0102-6720201PARAM_DEF4000100001&pid=S0102-67202014000100001&pdf_path=abcd/v27n1/0102-6720-abcd-27-01-00001.pdf&lang=en
   *  http://www.scielo.br/pdf/abcd/v27n1/0102-6720-abcd-27-01-00001.pdf
   *       
   */
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url";
  
  // scielo.php?script=sci_arttext&pid=S<journal_issn><year><volume><issue><identifier>&...
  protected static final String PATTERN_TEMPLATE = 
    "\"^%sscielo.php\\?script=sci_arttext&pid=[^&]{0,5}%s%d[0-9]{4,16}&lng=\", base_url, journal_issn, year";

  protected static final List<String> LANGUAGES = Arrays.asList("en", "es", "pt");
  
  // http://www.scielo.br/scieloOrg/php/articleXML.php?pid=S0102-67202014000400233&lang=en
  // Note 'lang' instead of 'lng'
  protected static String XML_REPLACEMENT = "scieloOrg/php/articleXML.php?pid=$1&lang=en";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
                                                          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au)
/*    {
      private SciELOSubTreeArticleIterator local = null;

      @Override
      public SubTreeArticleIterator getSubTreeArticleIterator() {
        // TODO Auto-generated method stub
        super.getSubTreeArticleIterator();
        return local;
      }

      @Override
      protected BuildableSubTreeArticleIterator instantiateBuildableIterator() {
        super.instantiateBuildableIterator();
        local = new SciELOSubTreeArticleIterator(super.au, super.spec);
        return local;
      }
    }*/;
    
    builder.setSpec(target,
                    ROOT_TEMPLATE,
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    // Full text HTML
    List<String> full_text_html_roles = new ArrayList<String>();
    for (String lng : LANGUAGES) {
      builder.addAspect(Pattern.compile("scielo\\.php\\?script=sci_arttext&pid=([^&]{0,5}[0-9X-]{9}[0-9]{8,20})&lng=" + lng + "$",
                                        Pattern.CASE_INSENSITIVE),
                        "scielo.php?script=sci_arttext&pid=$1&lng=" + lng,
                        ArticleFiles.ROLE_FULL_TEXT_HTML + lng);
      full_text_html_roles.add(ArticleFiles.ROLE_FULL_TEXT_HTML + lng);
    }
    for (String lng : LANGUAGES) {
      for (String tlng : LANGUAGES) {
        builder.addAspect(Pattern.compile("scielo\\.php\\?script=sci_arttext&pid=([^&]{0,5}[0-9X-]{9}[0-9]{8,20})&lng=" + lng + "&tlng=" + tlng + "$",
                                          Pattern.CASE_INSENSITIVE),
                          "scielo.php?script=sci_arttext&pid=$1&lng=" + lng + "&tlng=" + tlng,
                          ArticleFiles.ROLE_FULL_TEXT_HTML + lng + tlng);
        full_text_html_roles.add(ArticleFiles.ROLE_FULL_TEXT_HTML + lng + tlng);
      }
    }
    builder.setFullTextFromRoles(full_text_html_roles);
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_FULL_TEXT_HTML, full_text_html_roles);
    
    // Abstract
    List<String> abstract_roles = new ArrayList<String>();
    for (String lng : LANGUAGES) {
      builder.addAspect("scielo.php?script=sci_abstract&pid=$1&lng=" + lng,
                        ArticleFiles.ROLE_ABSTRACT + lng);
      abstract_roles.add(ArticleFiles.ROLE_ABSTRACT + lng);
    }
    for (String lng : LANGUAGES) {
      for (String tlng : LANGUAGES) {
        builder.addAspect("scielo.php?script=sci_abstract&pid=$1&lng=" + lng + "&tlng=" + tlng,
                          ArticleFiles.ROLE_ABSTRACT + lng + tlng);
        abstract_roles.add(ArticleFiles.ROLE_ABSTRACT + lng + tlng);
      }
    }
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ABSTRACT, abstract_roles);
    
    // PDF landing page
    List<String> pdf_landing_page_roles = new ArrayList<String>();
    for (String lng : LANGUAGES) {
      builder.addAspect("scielo.php?script=sci_pdf&pid=$1&lng=" + lng,
                        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE + lng);
      pdf_landing_page_roles.add(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE + lng);
    }
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE, pdf_landing_page_roles);
    
    builder.addAspect(XML_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_XML);
    
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
                                  ArticleFiles.ROLE_ABSTRACT,
                                  ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    return builder.getSubTreeArticleIterator();
  }
  
  // Create Article Metadata Extractor
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}