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

package org.lockss.plugin.clockss.wiley;

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

public class WileyBooksSourceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(WileyBooksSourceArticleIteratorFactory.class);

  /*
  Each book is a zip file. But there is no PDF file for the whole book, it is chapter based + additional stuff
  We will use  "fmatter.xml" to extract metadata from, since each book only report as one item in report.

  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/ch7/ch7.pdf
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/ch7/ch7.xml
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/ch8/ch8.pdf
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/ch8/ch8.xml
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/ch9/ch9.pdf
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/ch9/ch9.xml
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/cover.gif
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/fmatter/fmatter.pdf
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/fmatter/fmatter.xml
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/gloss/gloss.pdf
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/gloss/gloss.xml
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/index/index.pdf
  https://clockss-test.lockss.org/sourcefiles/wileybooks-released/2022_01/9780470007259.zip!/index/index.xml

  Besides "fmatter.xml", also need to try "fmatter1.xml"/"fmatter_indsub.xml"

   */

  protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
          "\"%s[^/]+/.*\\.zip!/(.*)\\.(xml|pdf)$\", base_url";

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

  public static final Pattern XML_PATTERN = Pattern.compile("/fmatter/(fmatter|fmatter1|fmatter_indsub)\\.xml$", Pattern.CASE_INSENSITIVE);
  public static final Pattern PDF_PATTERN = Pattern.compile("/fmatter/(fmatter|fmatter1|fmatter_indsub)\\.pdf$", Pattern.CASE_INSENSITIVE);
  public static final String XML_REPLACEMENT = "/fmatter/$1.xml";
  private static final String PDF_REPLACEMENT = "/fmatter/$1.pdf";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
            .setTarget(target)
            .setPatternTemplate(getIncludePatternTemplate(), Pattern.CASE_INSENSITIVE)
            .setExcludeSubTreePattern(getExcludeSubTreePattern())
            .setVisitArchiveMembers(true)
            .setVisitArchiveMembers(getIsArchive()));

    builder.addAspect(PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(XML_PATTERN,
            XML_REPLACEMENT,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }

  // NOTE - for a child to create their own version of this
  // indicates if the iterator should descend in to archives (for tar/zip deliveries)
  protected boolean getIsArchive() {
    return true;
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
