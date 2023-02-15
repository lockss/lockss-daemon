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

package org.lockss.plugin.clockss.aippublishing;

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


public class AIPPublishingArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

  private static final Logger log = Logger.getLogger(AIPPublishingArticleIteratorFactory.class);

  // They have multiple kinds of xml at book level, and publisher confirmed they have the same content
  // and all the formats are guaranteed to be delivered for each book
  /*
  9780735421547.marc.xml
  9780735421547.mrc
  9780735421547.onix.xml
  9780735421547.pdf
  9780735421547.xml  -- BITS format

  All chapter level PDF has something like "_001/_002/_003"
  ./9780735421547_011:
  9780735421547_011.epub
  9780735421547_011.pdf
  9780735421547_011.xml
  */

  protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
          "\"%s%s/.*\\.zip!/([^/]+)(\\.zip)?/[^_]+\\.pdf$\", base_url, directory";

  // Be sure to exclude all nested archives in case supplemental data is provided this way
  protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN =
          Pattern.compile(".*/[^/]+\\.zip!/[^/.]+\\.zip/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                  Pattern.CASE_INSENSITIVE);

  private static final Pattern PDF_PATTERN = Pattern.compile("/(.*)\\.pdf$", Pattern.CASE_INSENSITIVE);
  private static final Pattern XML_PATTERN = Pattern.compile("/(.*)\\.onix\\.xml$", Pattern.CASE_INSENSITIVE);
  private static final String XML_REPLACEMENT = "/$1.onix.xml";
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
