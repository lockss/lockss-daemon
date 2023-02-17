/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.codeocean;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 * A  variation on the generic CLOCKSS source article iterator
 * it iterates over the delivered zip files and allows the metadata extractor to count items.
 * code-ocean-released/2019/nature/4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9/v1.0/capsule.zip
 * code-ocean-released/2019/nature/4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9/v1.0/extract.sh
 * code-ocean-released/2019/nature/4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9/v1.0/image.tar.xz
 * code-ocean-released/2019/nature/4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9/v1.0/preservation.yml
 * code-ocean-released/2019/nature/4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9/v1.0/results.zip
 */
public class CodeOceanArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory  {

  private static final Logger log = Logger.getLogger(CodeOceanArticleIteratorFactory.class);
  
  /*
   * .../code-ocean-released/2019/nature/4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9/v1.0
  *     capsule.zip  - code capsule, data, etc a
  *     results.zip - 
  *     image.tar.xz - compressed docker image
  *     extract.sh - extraction script should anything be needed
  *     preservation.yml - use this for preservation metadata rather than capsule.zip!/metadata.yml
  *  and there can be multiple versions under the same UUID
  *  
  *  Use the capsule.zip as the metadata object and then use file-substitution to find the 
  *  preservation.yml for details.
  *     
   */
  private static final String PATTERN_TEMPLATE = 
      "\"^%s%s/.*/capsule\\.zip\", base_url, directory";
  
  // Be sure to exclude all nested archives in case we ever explode the zip to look at the yaml
  protected static final Pattern NESTED_ARCHIVE_PATTERN = 
      Pattern.compile(".*/[^/]+\\.zip!/.+\\.(zip|tar|gz|tgz|tar\\.gz)$", 
          Pattern.CASE_INSENSITIVE);  

  private static final Pattern ZIP_PATTERN = Pattern.compile("/([^/]+)/(v[^/]+)/capsule\\.zip$", Pattern.CASE_INSENSITIVE);
  private static final String 	ZIP_REPLACEMENT = "/$1/$2/capsule.zip";
  private static final String MD_REPLACEMENT = "/$1/$2/preservation.yml";
  // might exist, might not
  private static final String TAR_REPLACEMENT = "/$1/$2/image.tar.xz";
  private static final String RES_REPLACEMENT = "/$1/$2/results.zip";
  
  private static final String ROLE_PRESERVATION_FILE = "PresevationMetadata";
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
    


    // set up ZIP to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    // set up ZIP to be the aspect that is the access_url "full text CU"
    builder.addAspect(ZIP_PATTERN,
    		ZIP_REPLACEMENT,
        ROLE_FILE_ITEM, // this is the url of the item being counted    		
        ArticleFiles.ROLE_ARTICLE_METADATA); //if there wasn't a preservation.yml, we'd get what we could from this

    // this should be secondary ROLE_ARTICLE_METADATA
    builder.addAspect(
            MD_REPLACEMENT,
            ROLE_PRESERVATION_FILE,
            ArticleFiles.ROLE_ARTICLE_METADATA); //use this to pull in additional details
    
    // reset the ordering of the ROLE_ARTICLE_METADATA
    // make the file object a fallback in case a preservation.yml isn't included
    builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, ROLE_PRESERVATION_FILE, ROLE_FILE_ITEM);
    
    // tar files represent the docker config of the equivalent zip code modul
    // associate them but they aren't themselves a code module
    builder.addAspect(TAR_REPLACEMENT,
        ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);

    // results
    // associate them but they aren't themselves a code module
    builder.addAspect(RES_REPLACEMENT,
        "ExperimentalResults");



    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
	  return new CodeOceanArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);	  
  }
  
  
}
