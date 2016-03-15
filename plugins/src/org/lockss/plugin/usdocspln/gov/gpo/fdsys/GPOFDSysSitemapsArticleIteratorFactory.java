/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
    "\"^%sfdsys/pkg/([^/]+)/(html?|pdf|xml)/([^/]+)\\.(html?|pdf|xml)$\", base_url";
  
  protected static final Pattern HTML_PATTERN = 
      Pattern.compile("([^/]+)/html/([^/]+)\\.htm$",
          Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern PDF_PATTERN = 
      Pattern.compile("([^/]+)/pdf/([^/]+)\\.pdf$",
          Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern XML_PATTERN = 
      Pattern.compile("([^/]+)/xml/([^/]+)\\.xml$",
          Pattern.CASE_INSENSITIVE);
  
  protected static final String HTML_REPLACEMENT = "$1/html/$2.htm";
  protected static final String PDF_REPLACEMENT = "$1/pdf/$2.pdf";
  protected static final String XML_REPLACEMENT = "$1/xml/$2.xml";

  
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target, ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up html, pdf, xml to be an aspects that will trigger an ArticleFiles
    builder.addAspect(
        HTML_PATTERN, HTML_REPLACEMENT, 
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(
        PDF_PATTERN, PDF_REPLACEMENT, 
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    builder.addAspect(
        XML_PATTERN, XML_REPLACEMENT, 
        ArticleFiles.ROLE_FULL_TEXT_XML);
    
    // add metadata role from html, xml, or pdf (NOTE: pdf metadata is the access url)
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, Arrays.asList(
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_FULL_TEXT_XML,
        ArticleFiles.ROLE_FULL_TEXT_PDF));
    
    return builder.getSubTreeArticleIterator();
  }
  
  
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
