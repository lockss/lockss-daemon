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

package org.lockss.plugin.highwire;

import java.util.*;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class HighWirePressArticleIteratorFactory
    implements ArticleIteratorFactory,
               ArticleMetadataExtractorFactory {
  
  protected static Logger log =
    Logger.getLogger(HighWirePressArticleIteratorFactory.class);
  
  protected static final String ROOT_TEMPLATE_HTML =
    "\"%scgi/content/full/%s/\", base_url, volume_name";
  
  protected static final String OLD_ROOT_TEMPLATE_HTML =
    "\"%scgi/content/full/%d/\", base_url, volume";
  
  protected static final String ROOT_TEMPLATE_PDF =
    "\"%scgi/reprint/%s/\", base_url, volume_name";
  
  protected static final String OLD_ROOT_TEMPLATE_PDF =
    "\"%scgi/reprint/%d/\", base_url, volume";
  
  protected static final String PATTERN_TEMPLATE =
    "\"^%scgi/(?:content/full/(?:[^/]+;)?%s/[^/]+/[^/]+|reprint/(?:[^/]+;)?%s/[^/]+/[^/]+" +
    "[.]pdf)$\", base_url, volume_name, volume_name";
  
  protected static final String OLD_PATTERN_TEMPLATE =
    "\"^%scgi/(content/full/([^/]+;)?%d/[^/]+/[^/]+|reprint/([^/]+;)?%d/[^/]+/[^/]+" +
    "([.]pdf)?)$\", base_url, volume, volume";
  
  protected static final Pattern HTML_PATTERN = Pattern.compile(
      "/cgi/content/full/([^/]+;)?([^/]+/[^/]+/[^/]+)$",
      Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "/cgi/reprint/([^/]+;)?([^/]+/[^/]+/[^/]+)[.]pdf$",
      Pattern.CASE_INSENSITIVE);
  
  protected static final Pattern ABSTRACT_PATTERN = Pattern.compile(
      "/cgi/content/(?:abstract|summary)/([^/]+;)?([^/]+/[^/]+/[^/]+)$",
      Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String HTML_REPLACEMENT1 = "/cgi/content/full/$2";
  protected static final String HTML_REPLACEMENT2 = "/cgi/content/full/$1$2";
  protected static final String PDF_REPLACEMENT1 = "/cgi/reprint/$2.pdf";
  protected static final String PDF_REPLACEMENT2 = "/cgi/reprint/$1$2.pdf";
  protected static final String PDF_LANDING_REPLACEMENT1 = "/cgi/reprint/$2";
  protected static final String PDF_LANDING_REPLACEMENT2 = "/cgi/reprint/$1$2";
  protected static final String PDF_LANDING_REPLACEMENT3 = "/cgi/reprintframed/$2";
  protected static final String PDF_LANDING_REPLACEMENT4 = "/cgi/reprintframed/$1$2";
  protected static final String PDF_LANDING_REPLACEMENT5 = "/cgi/framedreprint/$2";
  protected static final String PDF_LANDING_REPLACEMENT6 = "/cgi/framedreprint/$1$2";
  protected static final String ABSTRACT_REPLACEMENT1 = "/cgi/content/abstract/$2";
  protected static final String ABSTRACT_REPLACEMENT2 = "/cgi/content/abstract/$1$2";
  protected static final String SUMMARY_REPLACEMENT = "/cgi/content/summary/$2";
  protected static final String REFERENCES_REPLACEMENT = "/cgi/content/refs/$2";
  
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    
    List<String> rootTemplates = new ArrayList<String>(2);
    String patternTemplate = null;
    String pluginId = au.getPluginId();
    if ("org.lockss.plugin.highwire.HighWirePlugin".equals(pluginId)) {
      // H10a plugin uses integer volume
      rootTemplates.add(OLD_ROOT_TEMPLATE_HTML);
      rootTemplates.add(OLD_ROOT_TEMPLATE_PDF);
      patternTemplate = OLD_PATTERN_TEMPLATE;
    }
    else {
      rootTemplates.add(ROOT_TEMPLATE_HTML);
      rootTemplates.add(ROOT_TEMPLATE_PDF);
      patternTemplate = PATTERN_TEMPLATE;
    }
    
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        rootTemplates, patternTemplate, Pattern.CASE_INSENSITIVE);
    
    // set up html or pdf to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means full is considered a FULL_TEXT_CU 
    // until this is deprecated
    builder.addAspect(HTML_PATTERN, Arrays.asList(
        HTML_REPLACEMENT1, HTML_REPLACEMENT2),
        ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(PDF_PATTERN, Arrays.asList(
        PDF_REPLACEMENT1, PDF_REPLACEMENT2),
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up abstract/summary to be an aspect
    builder.addAspect(ABSTRACT_PATTERN, Arrays.asList(
        ABSTRACT_REPLACEMENT1, ABSTRACT_REPLACEMENT2, SUMMARY_REPLACEMENT),
        ArticleFiles.ROLE_ABSTRACT);
    
    // set up pdf landing page to be an aspect
    builder.addAspect(Arrays.asList(
        PDF_LANDING_REPLACEMENT1,
        PDF_LANDING_REPLACEMENT2,
        PDF_LANDING_REPLACEMENT3,
        PDF_LANDING_REPLACEMENT4,
        PDF_LANDING_REPLACEMENT5,
        PDF_LANDING_REPLACEMENT6),
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);
    
    // set up references to be an aspect
    builder.addAspect(
        REFERENCES_REPLACEMENT,
        ArticleFiles.ROLE_REFERENCES);
    
    // add metadata role from abstract, html, or pdf landing page
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, Arrays.asList(
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE));
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    builder.setFullTextFromRoles(Arrays.asList(
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE,
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ABSTRACT));
    
    return builder.getSubTreeArticleIterator();
  }
  
  public ArticleMetadataExtractor
    createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
