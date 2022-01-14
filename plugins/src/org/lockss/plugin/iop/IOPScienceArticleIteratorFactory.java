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

package org.lockss.plugin.iop;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;


public class IOPScienceArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(IOPScienceArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
      "\"%s%s/%s\", base_url, journal_issn, volume_name";
  
  protected static final String PATTERN_TEMPLATE =
      "\"^%s%s/%s/[^/]+/[^/.?]+(?:/(?:article|fulltext|pdf/[^/]+[.]pdf))?$\"," +
      " base_url, journal_issn, volume_name";
  
  // various aspects of an article
  // http://iopscience.iop.org/1478-3975/8/1/015001
  // http://iopscience.iop.org/1478-3975/8/1/015001/refs
  // http://iopscience.iop.org/1478-3975/8/1/015001/cites
  // http://iopscience.iop.org/1478-3975/8/1/015001/article
  // http://iopscience.iop.org/1478-3975/8/1/015001/fulltext
  // http://iopscience.iop.org/1478-3975/8/1/015001/pdf/1478-3975_8_1_015001.pdf
  // http://iopscience.iop.org/1478-3975/8/1/015001?foo (exclude)
  // http://iopscience.iop.org/1478-3975/8/1/015001.foo (exclude)
  
  protected static final Pattern ABSTRACT_PATTERN =
      Pattern.compile("/([0-9]{4}-[0-9]{3}[0-9X])/([^/]+)/([^/]+)/([^/]+)$",
                      Pattern.CASE_INSENSITIVE);
                                               
  protected static final Pattern HTML_PATTERN =
      Pattern.compile("/([0-9]{4}-[0-9]{3}[0-9X])/([^/]+)/([^/]+)/([^/]+)(?:/fulltext|/article)$",
                      Pattern.CASE_INSENSITIVE);
                                               
  protected static final Pattern PDF_PATTERN =
      Pattern.compile("/([0-9]{4}-[0-9]{3}[0-9X])/([^/]+)/([^/]+)/([^/]+)/pdf/\\1_\\2_\\3_\\4[.]pdf$",
                      Pattern.CASE_INSENSITIVE);
                                               
  // Identify groups in the pattern "/(<jissn>)/(<volnum>)/(<issnum>)/(<articlenum>).*
  // The format of the ISSN is an eight digit number, 
  // divided by a hyphen into two four-digit numbers.
  // The last digit, which may be 0–9 or an X, is a check digit. 
  // how to change from one form (aspect) of article to another
  protected static final String ABSTRACT_REPLACEMENT = "/$1/$2/$3/$4";
  protected static final String HTML_REPLACEMENT = "/$1/$2/$3/$4/fulltext";
  protected static final String HTML_REPLACEMENT2 = "/$1/$2/$3/$4/article";
  protected static final String PDF_REPLACEMENT = "/$1/$2/$3/$4/pdf/$1_$2_$3_$4.pdf";
  protected static final String REFS_REPLACEMENT = "/$1/$2/$3/$4/refs";
  protected static final String SUPPL_REPLACEMENT = "/$1/$2/$3/$4/media";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(
      ArchivalUnit au, MetadataTarget target)
          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up fulltext to be an aspect that will trigger an ArticleFiles
    builder.addAspect(
        HTML_PATTERN, Arrays.asList(HTML_REPLACEMENT, HTML_REPLACEMENT2),
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up abstract to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means it is considered a FULL_TEXT_CU
    // until this fulltext concept is deprecated
    builder.addAspect(
        ABSTRACT_PATTERN, ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);
    
    builder.addAspect(
        REFS_REPLACEMENT,
        ArticleFiles.ROLE_REFERENCES);
    
    builder.addAspect(
        SUPPL_REPLACEMENT,
        ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML, 
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // The order in which to find ROLE_ARTICLE_METADATA
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, 
        ArticleFiles.ROLE_ABSTRACT, ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
