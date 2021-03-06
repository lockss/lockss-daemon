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

package org.lockss.plugin.usdocspln.gov.govinfo;

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
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class GovInfoCourtsSitemapsArticleIteratorFactory
        implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

  protected static Logger log =
          Logger.getLogger(GovInfoCourtsSitemapsArticleIteratorFactory.class);

  /*
  https://www.govinfo.gov/app/details/USCOURTS-akd-1_11-cv-00016/
  https://www.govinfo.gov/app/details/USCOURTS-akd-1_11-cv-00016/context
  https://www.govinfo.gov/content/pkg/USCOURTS-akd-1_11-cv-00016/pdf/USCOURTS-akd-1_11-cv-00016-0.pdf
  https://www.govinfo.gov/content/pkg/USCOURTS-akd-1_11-cv-00016/pdf/USCOURTS-akd-1_11-cv-00016-1.pdf
  https://www.govinfo.gov/content/pkg/USCOURTS-akd-1_11-cv-00016/pdf/USCOURTS-akd-1_11-cv-00016-2.pdf
   */

  protected static final String ROOT_TEMPLATE = "\"%sapp/details/\", base_url";
  protected static final String PATTERN_TEMPLATE = "\"%sapp/details/([^/]+)/([^/]+)$\", base_url";


  final Pattern HTML_PATTERN = Pattern.compile("([^/]+)/context$", Pattern.CASE_INSENSITIVE);
  final String HTML_REPLACEMENT = "$1";

  protected static final String PDF_FILE_LANDING_PAGE = "PDFFileLandingPage";


  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
          throws PluginException {


    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target, ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    builder.addAspect(
            HTML_PATTERN,
            HTML_REPLACEMENT,
            PDF_FILE_LANDING_PAGE);

    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, PDF_FILE_LANDING_PAGE);


    return builder.getSubTreeArticleIterator();
  }


  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}