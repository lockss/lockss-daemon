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

package org.lockss.plugin.clockss.eastview;

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


public class EastviewDirSourceXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

  private static final Logger log = Logger.getLogger(EastviewDirSourceXmlArticleIteratorFactory.class);

  // Xml and PDF match from 2007, 2006 is a mixed year
  //https://clockss-test.lockss.org/sourcefiles/eastview-released/2021/Eastview%20Journal%20Content/Digital%20Archives/Military%20Thought%20(DA-MLT)%201990-2019/DA-MLT.zip!/DA-MLT/MTH/2015/03/001_31/46295500_MTH_2015_0024_0001_0001.pdf
  //https://clockss-test.lockss.org/sourcefiles/eastview-released/2021/Eastview%20Journal%20Content/Digital%20Archives/Military%20Thought%20(DA-MLT)%201990-2019/DA-MLT.zip!/DA-MLT/MTH/2015/03/001_31/46295500_MTH_2015_0024_0001_0001.xml

  // Only xml exits for year 2002,2003,2004, 2005
  //https://clockss-test.lockss.org/sourcefiles/eastview-released/2021/Eastview%20Journal%20Content/Digital%20Archives/Military%20Thought%20(DA-MLT)%201990-2019/DA-MLT.zip!/DA-MLT/MTH/2003/03/001_31/01mth150.xml

  // Only xhtml exists, before year 2002
  //https://clockss-test.lockss.org/sourcefiles/eastview-released/2021/Eastview%20Journal%20Content/Digital%20Archives/Military%20Thought%20(DA-MLT)%201990-2019/DA-MLT.zip!/DA-MLT/MTH/1990/04/004_01/0040002.xhtml
  protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
          "\"%s[^/]+/.*\\.zip!/(.*)\\.(xml|xhtml)$\", base_url";

  // Be sure to exclude all nested archives in case supplemental data is provided this way
  protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
          Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                  Pattern.CASE_INSENSITIVE);

  protected Pattern getExcludeSubTreePattern() {
    return SUB_NESTED_ARCHIVE_PATTERN;
  }

  protected String getIncludePatternTemplate() {
    return ALL_ZIP_XML_PATTERN_TEMPLATE;
  }

  private static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.(xml)$", Pattern.CASE_INSENSITIVE);
  private static final String XML_REPLACEMENT = "/$1.xml";
  private static final String PDF_REPLACEMENT = "/$1.pdf";


  //For early years, like before 2002, the delivered content only have "xhtml"
  private static final Pattern XHTML_PATTERN = Pattern.compile("/(.*)\\.(xhtml)$", Pattern.CASE_INSENSITIVE);
  private static final String XHTML_REPLACEMENT = "/$1.xhtml";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(builder.newSpec()
            .setPatternTemplate(getIncludePatternTemplate(), Pattern.CASE_INSENSITIVE)
            .setExcludeSubTreePattern(getExcludeSubTreePattern())
            .setVisitArchiveMembers(true)
            .setVisitArchiveMembers(getIsArchive()));

    builder.addAspect(XHTML_PATTERN,
            XHTML_REPLACEMENT,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(XML_PATTERN,
        XML_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);


    builder.addAspect(XML_PATTERN,
            PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ARTICLE_METADATA); // emit even without PDF

    return builder.getSubTreeArticleIterator();
  }

  protected boolean getIsArchive() {
    return true;
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
