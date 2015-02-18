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

package org.lockss.plugin.mathematicalsciencespublishers;

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

public class MathematicalSciencesPublishersArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {
  
  protected static Logger log = 
      Logger.getLogger(MathematicalSciencesPublishersArticleIteratorFactory.class);
  
  // params from tdb file corresponding to AU
  protected static final String ROOT_TEMPLATE =
      "\"%s%s/%d/\", base_url, journal_id, year"; 
  
  protected static final String PATTERN_TEMPLATE =
      "\"^%s%s/%d/[0-9-]+/p.+[.]xhtml$\", base_url, journal_id, year";
  
  // various article aspects
  // note that the base url remains the same within a journal site
  // http://msp.org/involve/2013/6-1/p01.xhtml
  // http://msp.org/camcos/2012/7-2/p01-s.pdf
  // http://msp.org/camcos/2012/7-2/camcos-v7-n2-p01-p.pdf
  // http://www.msp.warwick.ac.uk/gt/2006/10/gt-2006-10-025p.pdf
  // http://msp.org/ant/2011/5-2/pC1.xhtml
  // http://msp.org/ant/2011/5-2/ant-v5-n2-pC1-s.pdf
  
  protected static final Pattern ABSTRACT_PATTERN = Pattern.compile(
      "([^/]+)/([0-9]+)/([0-9]+)(-?)([0-9]*)/p([c0-9]+)[.]xhtml$",
      Pattern.CASE_INSENSITIVE);
  
  // how to change from one form (aspect) of article to another
  protected static final String ABSTRACT_REPLACEMENT = "$1/$2/$3$4$5/p$6.xhtml";
  protected static final String SPDF_REPLACEMENT = "$1/$2/$3$4$5/$1-v$3$4n$5-p$6-s.pdf";
  protected static final String PPDF_REPLACEMENT = "$1/$2/$3$4$5/$1-v$3$4n$5-p$6-p.pdf";
  protected static final String ALT_SPDF_REPLACEMENT = "$1/$2/$3$4$5/$1-$2-$3-$6s.pdf";
  protected static final String ALT_PPDF_REPLACEMENT = "$1/$2/$3$4$5/$1-$2-$3-$6p.pdf";
  protected static final String REFERENCE_REPLACEMENT = "$1/$2/$3$4$5/b$6.xhtml";
  
  // MSP publisher, article content may look like this but you do not know
  // how many of the aspects will exist for a particular journal
  //
  //  msp.org/<journal_id>/<year>/<voliss>/p<page>.xhtml (abstract)
  //  msp.org/<journal_id>/<year>/<voliss>/<page_id>s.pdf (screen pdf)
  //  msp.org/<journal_id>/<year>/<voliss>/<page_id>p.pdf (printer pdf)
  //  msp.org/<journal_id>/<year>/<voliss>/b<page>xhtml (references)
  //  Not collected:
  //  msp.org/<journal_id>/<year>/<voliss>/f<page>xhtml (forward citations)
  //

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) 
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
        ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);
    
    // set up Abstract to be an aspect that will trigger an ArticleFiles
    // NOTE - for the moment this also means an abstract could be considered a 
    // FULL_TEXT_CU until this is deprecated
    // though the ordered list for role full text will mean if any of the others 
    // are there, they will become the FTCU
    builder.addAspect(
        ABSTRACT_PATTERN, ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT, ArticleFiles.ROLE_ARTICLE_METADATA);
    
    // set up PDF to be an aspect
    builder.addAspect(
        Arrays.asList(SPDF_REPLACEMENT, PPDF_REPLACEMENT, 
            ALT_SPDF_REPLACEMENT, ALT_PPDF_REPLACEMENT),
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    // set up minor reference aspect
    builder.addAspect(
        REFERENCE_REPLACEMENT,
        ArticleFiles.ROLE_REFERENCES);
    
    // The order in which we want to define full_text_cu.
    // First one that exists will get the job
    // In this case, there are two jobs, one for counting articles (abstract is 
    // good) and the other for metadata (PDF is correct)
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF, 
        ArticleFiles.ROLE_ABSTRACT);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
