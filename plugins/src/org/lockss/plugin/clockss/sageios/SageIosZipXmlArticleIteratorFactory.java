/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.sageios;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class SageIosZipXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {
        
  protected static Logger log = Logger.getLogger(SageIosZipXmlArticleIteratorFactory.class);

  //Most books have a fulltext metadata file. Unfortunately, there are a few that don't so instead, we'll pull the metadata
  //from the front matter instead for these cases. 

  protected static final String ALL_PATTERN_TEMPLATE = "\"%s%s/[^/]+\\.zip!/(fulltext|[A-Z0-9-]+fm(-i)?)/[^/]+\\.(xml|pdf)$\", base_url, directory";

    protected static final Pattern SUB_NESTED_ARCHIVE_PATTERN = 
      Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$", 
          Pattern.CASE_INSENSITIVE);
  protected static final Pattern NESTED_ARCHIVE_PATTERN = 
      Pattern.compile(".*/.+\\.(zip|tar|gz|tgz|tar\\.gz)$", 
          Pattern.CASE_INSENSITIVE);
      
  public static final Pattern BOOK_METADATA_PATTERN = Pattern.compile("/fulltext/([^/]+)\\.xml$", Pattern.CASE_INSENSITIVE);
  public static final Pattern FRONTMATTER_METADATA_PATTERN = Pattern.compile("/([A-Z0-9-]+(?:-fm(?:-i)?)?)/\\1\\.xml$", Pattern.CASE_INSENSITIVE);
  public static final Pattern PDF_PATTERN = Pattern.compile("/fulltext/([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  public static final String BOOK_METADATA_REPLACEMENT = "/fulltext/$1.xml";
  public static final String FRONTMATTER_METADATA_REPLACEMENT = "/$1/$1.xml";
  public static final String PDF_REPLACEMENT = "/fulltext/$1.pdf";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(builder.newSpec()
                    .setTarget(target)
                    .setPatternTemplate(getIncludePatternTemplate(), Pattern.CASE_INSENSITIVE)
                    .setExcludeSubTreePattern(getExcludeSubTreePattern())
                    .setVisitArchiveMembers(true)); 

    builder.addAspect(PDF_PATTERN, 
                      PDF_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(BOOK_METADATA_PATTERN,
                      BOOK_METADATA_REPLACEMENT,
                      "ROLE_BOOK_METADATA");
                      //TODO: Create constant

    builder.addAspect(FRONTMATTER_METADATA_PATTERN,
                      FRONTMATTER_METADATA_REPLACEMENT,
                      "ROLE_FRONTMATTER_METADATA");
 
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, "ROLE_BOOK_METDATA", "ROLE_FRONTMATTER_METADATA"); 
  

    return builder.getSubTreeArticleIterator();
  }
  
  protected Pattern getExcludeSubTreePattern() {
    return SUB_NESTED_ARCHIVE_PATTERN;
  }

  protected String getIncludePatternTemplate() {
    return ALL_PATTERN_TEMPLATE;
  }

  // iterator should descend in to archives (for tar/zip deliveries)
  protected boolean getIsArchive() {
    return true;
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
