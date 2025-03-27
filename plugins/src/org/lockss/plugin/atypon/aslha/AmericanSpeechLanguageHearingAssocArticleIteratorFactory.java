/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.atypon.aslha;

import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.plugin.atypon.BaseAtyponArticleIteratorFactory;
import org.lockss.util.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class AmericanSpeechLanguageHearingAssocArticleIteratorFactory extends BaseAtyponArticleIteratorFactory {

  private static final Logger log = Logger.getLogger(AmericanSpeechLanguageHearingAssocArticleIteratorFactory.class);


  // https://pubs.asha.org/doi/10.1044/0161-1461%282007/001%29
  // https://pubs.asha.org/doi/abs/10.1044/0161-1461%282007/001%29
  // https://pubs.asha.org/doi/epdf/10.1044/0161-1461%282007/001%29
  // https://pubs.asha.org/doi/full/10.1044/0161-1461%282007/001%29
  // https://pubs.asha.org/doi/pdf/10.1044/0161-1461%282007/001%29

  private static final String ROOT_TEMPLATE = "\"%sdoi/\", base_url";

  private static final Pattern PDF_PATTERN = Pattern.compile("/doi/pdf/([.0-9]+)/([^?&/]+)/([-.0-9a-zA-Z()/]+)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern EPDF_PATTERN = Pattern.compile("/doi/epdf/([.0-9]+)/([^?&/]+)/([-.0-9a-zA-Z()/]+)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern ABSTRACT_PATTERN = Pattern.compile("/doi/abs/([.0-9]+)/([^?&/]+)/([-.0-9a-zA-Z()/]+)$", Pattern.CASE_INSENSITIVE);

  private static final Pattern HTML_PATTERN = Pattern.compile("/doi/full/([.0-9]+)/([^?&/]+)/([-.0-9a-zA-Z()/]+)$", Pattern.CASE_INSENSITIVE);
  private static final String ABSTRACT_REPLACEMENT = "/doi/abs/$1/$2/$3";
  private static final String HTML_REPLACEMENT = "/doi/full/$1/$2/$3";

  private static final String EPDF_REPLACEMENT = "/doi/epdf/$1/$2/$3";
  private static final String PDF_REPLACEMENT = "/doi/pdf/$1/$2/$3";


  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    SubTreeArticleIteratorBuilder builder = localBuilderCreator(au);

    builder.setSpec(target,
            ROOT_TEMPLATE,
            getPatternTemplate(), Pattern.CASE_INSENSITIVE);

    builder.addAspect(PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(EPDF_PATTERN,
            EPDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(ABSTRACT_PATTERN,
            ABSTRACT_REPLACEMENT,
            ArticleFiles.ROLE_ABSTRACT,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(HTML_PATTERN,
            HTML_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_HTML,
            ArticleFiles.ROLE_ARTICLE_METADATA); // use for metadata if abstract doesn't exist

    return builder.getSubTreeArticleIterator();
  }
  
}