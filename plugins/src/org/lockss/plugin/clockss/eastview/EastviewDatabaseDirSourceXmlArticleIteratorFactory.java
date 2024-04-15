/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.eastview;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;


public class EastviewDatabaseDirSourceXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

  private static final Logger log = Logger.getLogger(EastviewDatabaseDirSourceXmlArticleIteratorFactory.class);

  /*
  /sourcefiles/eastviewudbcom-released/2024_01/eastview/UDB-COM/LGA/1997.zip
  /sourcefiles/eastviewudbcom-released/2024_01/eastview/UDB-COM/LGA/1998.zip
  /sourcefiles/eastviewudbcom-released/2024_01/eastview/UDB-COM/LGA/1999.zip
  /sourcefiles/eastviewudbcom-released/2024_01/eastview/UDB-COM/LGA/2000.zip
  /sourcefiles/eastviewudbcom-released/2024_01/eastview/UDB-COM/LGA/2001.zip
   */

  private static final String PATTERN_TEMPLATE =
          "\"^%s%s/.*\\.zip\", base_url, directory";

  // Be sure to exclude all nested archives in case supplemental data is provided this way
  protected static final Pattern NESTED_ARCHIVE_PATTERN =
          Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$",
                  Pattern.CASE_INSENSITIVE);
  private static final Pattern ZIP_PATTERN = Pattern.compile("/(.*.zip)$", Pattern.CASE_INSENSITIVE);
  private static final String 	ZIP_REPLACEMENT = "/$1.zip";

  private static final String ROLE_FILE_ITEM = "FileItem";


  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
            .setTarget(target)
            .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
            .setExcludeSubTreePattern(NESTED_ARCHIVE_PATTERN));


    builder.addAspect(ZIP_PATTERN,
            ZIP_REPLACEMENT,
            ROLE_FILE_ITEM,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, ROLE_FILE_ITEM);

    return builder.getSubTreeArticleIterator();
  }

  protected boolean getIsArchive() {
    return true;
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    return new EastviewDatabaseDirSourceMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
