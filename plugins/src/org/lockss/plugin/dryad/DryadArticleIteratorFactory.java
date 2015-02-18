/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.dryad;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class DryadArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {

  protected static Logger log = 
      Logger.getLogger(DryadArticleIteratorFactory.class);

  // params from tdb file corresponding to AU
  protected static final String ROOT_TEMPLATE = "\"%s\", base_url"; 

  protected static final String PATTERN_TEMPLATE =
      "\"^%sresource/doi:10.5061/dryad[.][a-z0-9.]+$\", base_url";
  
  // Dryad publisher, article content may look like this but you do not know precisely
  //
  //  datadryad.org/resource/doi:10.5061/dryad.<doi_uid>[.<ver#>] (landing page)
  //  datadryad.org/resource/doi:10.5061/dryad.<doi_uid>[.<ver#>]?show=full
  //  datadryad.org/resource/doi:10.5061/dryad.<doi_uid>[.<ver#>]/citation/bib
  //  datadryad.org/resource/doi:10.5061/dryad.<doi_uid>[.<ver#>]/citation/ris
  //  datadryad.org/resource/doi:10.5061/dryad.<doi_uid>[.<ver#>]/<subpage#>
  //  datadryad.org/resource/doi:10.5061/dryad.<doi_uid>[.<ver#>]/<subpage#>?show=full
  //  datadryad.org/bitstream/handle/* (data files)
  //  datadryad.org/handle/* (duplicate pages)
  //

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) 
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // various aspects of an article

    final Pattern ABSTRACT_PATTERN = Pattern.compile(
        "(/doi:10.5061/dryad[.][a-z0-9.]+)$",
        Pattern.CASE_INSENSITIVE);

    // how to change from one form (aspect) of article to another
    final String ABSTRACT_REPLACEMENT = "$1";
    final String FULL_REPLACEMENT = "$1?show=full";
    final String BIB_REPLACEMENT = "$1/citation/bib";
    final String RIS_REPLACEMENT = "$1/citation/ris";

    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    // set up Abstract to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means an abstract could be considered a 
    // FULL_TEXT_CU until this is deprecated
    // though the ordered list for role full text will mean if any of the others 
    // are there, they will become the FTCU
    builder.addAspect(Arrays.asList(ABSTRACT_PATTERN),
        ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT, 
        ArticleFiles.ROLE_ARTICLE_METADATA, 
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE);

    // set up show=full to be an aspect
    builder.addAspect(
        FULL_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);

    // set up /citation/(bib|ris) to be aspects
    builder.addAspect(
        BIB_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_BIBTEX);
    builder.addAspect(
        RIS_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_RIS);

    // The order in which we want to define full_text_cu, just abstract
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE,
        ArticleFiles.ROLE_ABSTRACT);

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
