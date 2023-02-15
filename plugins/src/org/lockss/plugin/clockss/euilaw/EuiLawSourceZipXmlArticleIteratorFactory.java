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

package org.lockss.plugin.clockss.euilaw;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.plugin.clockss.SourceZipXmlArticleIteratorFactory;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class EuiLawSourceZipXmlArticleIteratorFactory extends SourceZipXmlArticleIteratorFactory {

  protected static Logger log = Logger.getLogger(EuiLawSourceZipXmlArticleIteratorFactory.class);

  protected static final String ALL_PATTERN_TEMPLATE = "\"^%s[^/]+/.*\\.zip!/.*\\.(xml|pdf)$\",base_url";

  // Old way. they may go back to it.
  // <base_url>/<dir>/lawandworld_XML_Volume_8_Issue_1_2022.zip!/First_Last.xml
  // <base_url>/<dir>/lawandworld_Volume_8_Issue_1_2022/First_Last.pdf
  //public static final Pattern XML_PATTERN = Pattern.compile("/([^/]*)XML_([^/]*)\\.zip!/([^/]*)\\.xml$", Pattern.CASE_INSENSITIVE);
  //  public static final String XML_REPLACEMENT = "/$1XML_$2.zip!/$3.xml";
  //  private static final String PDF_REPLACEMENT = "/$1$2/$3.pdf";

  // New way. still need work from them.
  // <base_url>/<dir>/Volume_1_Issue_2_2015.zip!/Volume_1_Issue_2_2015/Volume_1_Issue_2_2015_PDF/First_Last.pdf
  // <base_url>/<dir>/Volume_1_Issue_2_2015.zip!/Volume_1_Issue_2_2015/Volume_1_Issue_2_2015_XML/First_Last.xml
  public static final Pattern XML_PATTERN = Pattern.compile("/([^/]*\\.zip!)/(.*)_XML/([^/]*)\\.xml$", Pattern.CASE_INSENSITIVE);
  private static final String XML_REPLACEMENT = "/$1/$2_XML/$3.xml";
  private static final String PDF_REPLACEMENT = "/$1/$2_PDF/$3.pdf";
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

    builder.addAspect(XML_PATTERN,
        XML_REPLACEMENT,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ARTICLE_METADATA);

    return builder.getSubTreeArticleIterator();
  }

  protected String getIncludePatternTemplate() {
    return ALL_PATTERN_TEMPLATE;
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
