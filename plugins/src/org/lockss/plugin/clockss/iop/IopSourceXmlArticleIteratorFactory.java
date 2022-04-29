/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss.iop;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

//
// A  variation on the generic CLOCKSS source article iterator
// it iterates over tar.gz file and
// this one just excludes manifest.xml files - not the xml of the actual articles
// Also - since we're customizing anyway; pick up the pdf files with a 
// replacement.
// Also - with 2019 we now support a string for a year - eg 2019_1, 2019_B, etc. So can't use the "year" param
//
public class IopSourceXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

  private static final Logger log = Logger.getLogger(IopSourceXmlArticleIteratorFactory.class);
  
  // ROOT_TEMPLATE doesn't need to be defined as sub-tree is entire tree under base/year or base/directory
  // This pattern is specific to IOP - exclude the manifest.xml XML
  // and since we're customizing, we might as well also pick up the PDF from the
  // pattern
  // ...iop-released/2015/20-10-2015/0004-637X/805/1/18/apj_805_1_18.(pdf|xml)
  // end pattern with a number (they have article number as final portion of filename)
  // Shared with IopDeliveredSourcePlugin
  // ..iop-delivered/2018/HD1_1/0004-637X/805/1/18/apj_805_1_18.(pdf|xml)
  // to exclude manifest.xml since negative lookahead didn't work
  // With delivered source seeing some foo123.pdf without foo123.xml but WITH foo123.article
  // try using that ONLY if there is no foo.xml available
  // do not iterate on /file/dir/.article - only on /file/dir/foo123.article
  
  private static final String PATTERN_TEMPLATE = 
      "\"^%s[^/]+/[^/]+/[0-9-X]+\\.tar\\.gz!/.*[0-9]\\.(article|xml)$\", base_url";

  private static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.xml$", Pattern.CASE_INSENSITIVE);
  private static final String XML_REPLACEMENT = "/$1.xml";
  private static final Pattern ART_PATTERN = Pattern.compile("/(.*)\\.article$", Pattern.CASE_INSENSITIVE);
  private static final String ART_REPLACEMENT = "/$1.article";
  private static final String PDF_REPLACEMENT = "/$1.pdf";
  // I do not know what this is at the moment
  private static final String WEIRD_PDF_REPLACEMENT = "/$1o.pdf";
  private static final String MANUSCRIPT_REPLACEMENT = "/$1am.pdf";
  private static final String ROLE_MANUSCRIPT = "AuthorManuscriptPdf";
  private static final String ROLE_BACKUP_METADATA = "ArticleXml";
  private static final String ROLE_PRIMARY_METADATA = "JatsXml";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
                    .setTarget(target)
                    .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
                    .setVisitArchiveMembers(true));
    
    // NOTE - full_text_cu is set automatically to the url used for the articlefiles
    // ultimately the metadata extractor needs to set the entire facet map 

    // set up XML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(XML_PATTERN,
        XML_REPLACEMENT,
        ROLE_PRIMARY_METADATA,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    // set up article to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    // but only when a .xml isn't available
    builder.addAspect(ART_PATTERN,
        ART_REPLACEMENT,
        ROLE_BACKUP_METADATA);
    
    // While we can't identify articles that are *just* PDF which is why they
    // can't trigger an articlefiles by themselves, we can identify them
    // by replacement and they should be the full text CU.
    builder.addAspect(PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    builder.addAspect(MANUSCRIPT_REPLACEMENT,
        ROLE_MANUSCRIPT);
    
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA,
    		ROLE_PRIMARY_METADATA,
    		ROLE_BACKUP_METADATA);
    
    //Now set the order for the full text cu
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ROLE_MANUSCRIPT,
        ArticleFiles.ROLE_ARTICLE_METADATA); // though if it comes to this it won't emit

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
