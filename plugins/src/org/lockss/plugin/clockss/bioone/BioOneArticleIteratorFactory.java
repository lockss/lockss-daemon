/*
 * $Id:
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.bioone;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;


public class BioOneArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

  private static final Logger log = Logger.getLogger(BioOneArticleIteratorFactory.class);

  // They have two kinds of xml, the one with NUMBER only is for issue only, so no matching PDF file
  //https://clockss-test.lockss.org/sourcefiles/bioone-released/2020/entn_129_3_SendAll.zip!/3.zip/021.129.0300.xml
  //https://clockss-test.lockss.org/sourcefiles/bioone-released/2020/jche_38_1_SendAll.zip!/1.zip/018.029.0200.xml

  // The following is for article
  //https://clockss-test.lockss.org/sourcefiles/bioone-released/2020/entn_129_3_SendAll.zip!/021.129.0301_SendAll.zip/021.129.0301.pdf	application/pdf	787505
  //https://clockss-test.lockss.org/sourcefiles/bioone-released/2020/entn_129_3_SendAll.zip!/021.129.0301_SendAll.zip/021.129.0301.xml	application/xml	40853


  protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
          "\"%s%s/.*\\.zip!/([^/]+)(\\.zip)?/.*\\.pdf$\", base_url, directory";

  // Be sure to exclude all nested archives in case supplemental data is provided this way
  protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
          Pattern.compile(".*/[^/]+\\.zip!/[^/.]+\\.zip/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                  Pattern.CASE_INSENSITIVE);

  private static final Pattern PDF_PATTERN = Pattern.compile("/(.*)\\.pdf$", Pattern.CASE_INSENSITIVE);
  private static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.xml$", Pattern.CASE_INSENSITIVE);
  private static final String XML_REPLACEMENT = "/$1.xml";
  private static final String PDF_REPLACEMENT = "/$1.pdf";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(builder.newSpec()
        .setTarget(target)
        .setPatternTemplate(ALL_ZIP_XML_PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
        .setExcludeSubTreePattern(SUB_NESTED_ARCHIVE_PATTERN)
        .setVisitArchiveMembers(true));

    builder.addAspect(PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(XML_PATTERN,
            XML_REPLACEMENT,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
