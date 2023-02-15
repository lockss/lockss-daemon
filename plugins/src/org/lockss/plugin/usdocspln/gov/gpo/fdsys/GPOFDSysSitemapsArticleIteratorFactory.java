/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.extractor.*;
import org.lockss.daemon.PluginException;

public class GPOFDSysSitemapsArticleIteratorFactory
  implements ArticleIteratorFactory,
             ArticleMetadataExtractorFactory {
  
  protected static Logger log = 
    Logger.getLogger(GPOFDSysSitemapsArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE =
    "\"%sfdsys/pkg/\", base_url";

  protected static final String PATTERN_TEMPLATE =
    "\"^%sfdsys/pkg/([^/]+)/(html?|mp3|pdf|xml)/([^/]+)\\.(html?|mp3|pdf|xml)$\", base_url";
  
  protected static final Pattern HTML_PATTERN = 
      Pattern.compile("([^/]+)/html/([^/]+)\\.htm$",
          Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern MP3_PATTERN = 
      Pattern.compile("([^/]+)/mp3/([^/]+)\\.mp3$",
          Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern PDF_PATTERN = 
      Pattern.compile("([^/]+)/pdf/([^/]+)\\.pdf$",
          Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern XML_PATTERN = 
      Pattern.compile("([^/]+)/xml/([^/]+)\\.xml$",
          Pattern.CASE_INSENSITIVE);
  
  protected static final String HTML_REPLACEMENT1 = "$1/html/$2.htm";
  protected static final String HTML_REPLACEMENT2 = "$1/html/$2.html";
  protected static final String MP3_REPLACEMENT = "$1/mp3/$2.mp3";
  protected static final String PDF_REPLACEMENT = "$1/pdf/$2.pdf";
  protected static final String XML_REPLACEMENT = "$1/xml/$2.xml";
  
  protected static final String ROLE_AUDIO_FILE = "AudioFile";
  
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target, ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up html, pdf, xml to be an aspects that will trigger an ArticleFiles
    builder.addAspect(
        HTML_PATTERN, Arrays.asList(HTML_REPLACEMENT1, HTML_REPLACEMENT2),
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT, 
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    builder.addAspect(
        XML_PATTERN, XML_REPLACEMENT, 
        ArticleFiles.ROLE_FULL_TEXT_XML);
    
    builder.addAspect(
        MP3_PATTERN, MP3_REPLACEMENT, 
        ROLE_AUDIO_FILE);
    
    // add metadata role from html, xml, or pdf (NOTE: pdf metadata is the access url)
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, Arrays.asList(
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_FULL_TEXT_XML,
        ROLE_AUDIO_FILE));
    
    return builder.getSubTreeArticleIterator();
  }
  
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
