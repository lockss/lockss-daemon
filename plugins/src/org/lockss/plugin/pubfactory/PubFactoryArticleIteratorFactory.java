/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pubfactory;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

/*
 * https://www.berghahnjournals.com/downloadpdf/journals/boyhood-studies/10/1/bhs100105.pdf
 * https://www.berghahnjournals.com/downloadpdf/journals/boyhood-studies/10/1/bhs100105.xml
 * https://www.berghahnjournals.com/view/journals/boyhood-studies/10/1/bhs100101.xml
 * https://www.berghahnjournals.com/view/journals/boyhood-studies/10/1/bhs100101.xml?pdfVersion=true (frameset)
 *
 * Also - this look like an error. Ignore it. It's the link associated with the pdf tab when you're
 * already on the html of the pdf tab active
 * https://www.berghahnjournals.com/view/journals/boyhood-studies/10/1/bhs100101.xml?&pdfVersion=true (WHAAA?)
 */

public class PubFactoryArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  private static final Logger log = Logger.getLogger(PubFactoryArticleIteratorFactory.class);

  // don't set the ROOT_TEMPLATE - it is just base_url

  private static String PATTERN_TEMPLATE;

  private static final String PATTERN_TEMPLATE_W_1_BASE_URL =
      "\"^%s(downloadpdf|view)/journals/%s/%s/\", base_url, journal_id, volume_name";
  // to accomodate plugins that have base_url2 param, which is where many articles can be found
  private static final String PATTERN_TEMPLATE_W_2_BASE_URL =
      "\"^(%s|%s)(downloadpdf|view)/journals/%s/%s/\", base_url, base_url2, journal_id, volume_name";

  // (?![^/]+issue[^/]+) is a negative lookahead to exclude issue TOC pages but to allow articles through
  // it must come before the bit that picks up the filename when it's not an issue
  // https://www.berghahnjournals.com/view/journals/boyhood-studies/10/1/boyhood-studies.10.issue-1.xml
  private static final Pattern ART_LANDING_PATTERN = Pattern.compile("/view/(journals/.+/(?![^/]+issue[^/]+)[^/]+)\\.xml$", Pattern.CASE_INSENSITIVE);               
  private static final Pattern ART_PDF_PATTERN = Pattern.compile("/downloadpdf/(journals/.+/[^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);               
  private static final Pattern ART_PDF_XML_PATTERN = Pattern.compile("/downloadpdf/(journals/.+/[^/]+)\\.xml$", Pattern.CASE_INSENSITIVE);               
  
  // how to get from one of the above to the other
  private final String PDF_REPLACEMENT = "/downloadpdf/$1.pdf"; 
  private final String PDF_XML_REPLACEMENT = "/downloadpdf/$1.xml"; 
  private final String PDF_FRAME_REPLACEMENT = "/view/$1.xml?pdfVersion=true"; //legacy
  private final String LANDING_REPLACEMENT = "/view/$1.xml";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {

    // see if base_url2 is a parameter for the AU. if it is, get the pattern that allows for it
    String base_url2 = au.getConfiguration().get(ConfigParamDescr.BASE_URL2.getKey());
    if (base_url2 != null) {
      log.info("baseurl2 is : " + base_url2);
      PATTERN_TEMPLATE = PATTERN_TEMPLATE_W_2_BASE_URL;
    } else {
      PATTERN_TEMPLATE = PATTERN_TEMPLATE_W_1_BASE_URL;
    }

    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(new SubTreeArticleIterator.Spec()
    .setTarget(target)
    .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    builder.addAspect(ART_LANDING_PATTERN,
        LANDING_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    // make this one primary by defining it first
    builder.addAspect(ART_PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

    // another version
    builder.addAspect(ART_PDF_XML_PATTERN,
            PDF_XML_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(
        PDF_FRAME_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE);


    // The order in which we want to define full_text_cu.  
    // First one that exists will get the job
    // Leave the CITATION_RIS in because if just doing iterator, it's the only one set
    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML,
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

