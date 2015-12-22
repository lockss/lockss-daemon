/* $Id$
 
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

package org.lockss.plugin.scielo;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class SciELOArticleIteratorFactory
      implements ArticleIteratorFactory, ArticleMetadataExtractorFactory
{

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
    "\"%sscielo.php[?]script=sci_arttext&pid=[^&]{0,5}%s%d[0-9]{4,16}&lng=en$\", base_url, journal_issn, year";

  // Groups:
  // 1. pid
  // 2. lng param (optional)
  protected Pattern FULLTEXT_PATTERN = 
      Pattern.compile("scielo.php[?]script=sci_arttext&pid=([^&-]{0,5}[0-9-]{9}[0-9]{8,20})&lng=en",
          Pattern.CASE_INSENSITIVE);
  
  protected static String FT_REPLACEMENT1i = "scielo.php?script=sci_arttext&pid=$1&lng=en";
  protected static String FT_REPLACEMENT1e = "scielo.php?script=sci_arttext&pid=$1&lng=es";
  protected static String FT_REPLACEMENT1p = "scielo.php?script=sci_arttext&pid=$1&lng=pt";
  protected static String FT_REPLACEMENT2i = "scielo.php?script=sci_arttext&pid=$1&lng=en&tlng=en";
  protected static String FT_REPLACEMENT2e = "scielo.php?script=sci_arttext&pid=$1&lng=en&tlng=es";
  protected static String FT_REPLACEMENT2p = "scielo.php?script=sci_arttext&pid=$1&lng=en&tlng=pt";
  protected static String FT_REPLACEMENT3i = "scielo.php?script=sci_arttext&pid=$1&lng=es&tlng=en";
  protected static String FT_REPLACEMENT3e = "scielo.php?script=sci_arttext&pid=$1&lng=es&tlng=es";
  protected static String FT_REPLACEMENT3p = "scielo.php?script=sci_arttext&pid=$1&lng=es&tlng=pt";
  protected static String FT_REPLACEMENT4i = "scielo.php?script=sci_arttext&pid=$1&lng=pt&tlng=en";
  protected static String FT_REPLACEMENT4e = "scielo.php?script=sci_arttext&pid=$1&lng=pt&tlng=es";
  protected static String FT_REPLACEMENT4p = "scielo.php?script=sci_arttext&pid=$1&lng=pt&tlng=pt";
  
  // these can be created from the FT url
  protected static String PDF_LANDING_REPLACEMENT1 = "scielo.php?script=sci_pdf&pid=$1&lng=en";
  protected static String PDF_LANDING_REPLACEMENT2 = "scielo.php?script=sci_pdf&pid=$1&lng=es";
  protected static String PDF_LANDING_REPLACEMENT3 = "scielo.php?script=sci_pdf&pid=$1&lng=pt";
  
  protected static String ABSTRACT_REPLACEMENT1i = "scielo.php?script=sci_abstract&pid=$1&lng=en";
  protected static String ABSTRACT_REPLACEMENT1e = "scielo.php?script=sci_abstract&pid=$1&lng=es";
  protected static String ABSTRACT_REPLACEMENT1p = "scielo.php?script=sci_abstract&pid=$1&lng=pt";
  protected static String ABSTRACT_REPLACEMENT2i = "scielo.php?script=sci_abstract&pid=$1&lng=en&tlng=en";
  protected static String ABSTRACT_REPLACEMENT2e = "scielo.php?script=sci_abstract&pid=$1&lng=en&tlng=es";
  protected static String ABSTRACT_REPLACEMENT2p = "scielo.php?script=sci_abstract&pid=$1&lng=en&tlng=pt";
  protected static String ABSTRACT_REPLACEMENT3i = "scielo.php?script=sci_abstract&pid=$1&lng=es&tlng=en";
  protected static String ABSTRACT_REPLACEMENT3e = "scielo.php?script=sci_abstract&pid=$1&lng=es&tlng=es";
  protected static String ABSTRACT_REPLACEMENT3p = "scielo.php?script=sci_abstract&pid=$1&lng=es&tlng=pt";
  protected static String ABSTRACT_REPLACEMENT4i = "scielo.php?script=sci_abstract&pid=$1&lng=pt&tlng=en";
  protected static String ABSTRACT_REPLACEMENT4e = "scielo.php?script=sci_abstract&pid=$1&lng=pt&tlng=es";
  protected static String ABSTRACT_REPLACEMENT4p = "scielo.php?script=sci_abstract&pid=$1&lng=pt&tlng=pt";
  
  // http://www.scielo.br/scieloOrg/php/articleXML.php?pid=S0102-67202014000400233&lang=en
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
    
    builder.addAspect(FULLTEXT_PATTERN,
        Arrays.asList(
            FT_REPLACEMENT4p,
            FT_REPLACEMENT3p,
            FT_REPLACEMENT2p,
            FT_REPLACEMENT1p,
            FT_REPLACEMENT3e,
            FT_REPLACEMENT4e,
            FT_REPLACEMENT2e,
            FT_REPLACEMENT1e,
            FT_REPLACEMENT1i,
            FT_REPLACEMENT2i,
            FT_REPLACEMENT3i,
            FT_REPLACEMENT4i
            ),
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        Arrays.asList(
            ABSTRACT_REPLACEMENT4p,
            ABSTRACT_REPLACEMENT3p,
            ABSTRACT_REPLACEMENT2p,
            ABSTRACT_REPLACEMENT1p,
            ABSTRACT_REPLACEMENT3e,
            ABSTRACT_REPLACEMENT4e,
            ABSTRACT_REPLACEMENT2e,
            ABSTRACT_REPLACEMENT1e,
            ABSTRACT_REPLACEMENT1i,
            ABSTRACT_REPLACEMENT2i,
            ABSTRACT_REPLACEMENT3i,
            ABSTRACT_REPLACEMENT4i
            ),
        ArticleFiles.ROLE_ABSTRACT);
    
    builder.addAspect(
        Arrays.asList(
            PDF_LANDING_REPLACEMENT1,
            PDF_LANDING_REPLACEMENT2,
            PDF_LANDING_REPLACEMENT3
            ),
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    builder.addAspect(
        XML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_XML);
    
    builder.addAspect(FT_REPLACEMENT2i, ArticleFiles.ROLE_FULL_TEXT_HTML + "_en");
    builder.addAspect(FT_REPLACEMENT3e, ArticleFiles.ROLE_FULL_TEXT_HTML + "_es");
    builder.addAspect(FT_REPLACEMENT4p, ArticleFiles.ROLE_FULL_TEXT_HTML + "_pt");
    
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
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