/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.oecd;

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

public class OcedWorkingPaperArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(OcedWorkingPaperArticleIteratorFactory.class);

  /*

  https://www.oecd-ilibrary.org/economics/a-multi-gas-assessment-of-the-kyoto-protocol_540631321683
  https://www.oecd-ilibrary.org/economics/a-multi-gas-assessment-of-the-kyoto-protocol_540631321683/cite
  https://www.oecd-ilibrary.org/economics/a-multi-gas-assessment-of-the-kyoto-protocol_540631321683/cite/bib
  https://www.oecd-ilibrary.org/economics/a-multi-gas-assessment-of-the-kyoto-protocol_540631321683/cite/endnote
  https://www.oecd-ilibrary.org/economics/a-multi-gas-assessment-of-the-kyoto-protocol_540631321683/cite/ris
  https://www.oecd-ilibrary.org/economics/a-multi-gas-assessment-of-the-kyoto-protocol_540631321683/cite/txt

  https://www.oecd-ilibrary.org/a-multi-gas-assessment-of-the-kyoto-protocol_5lgsjhvj8247.pdf?itemId=%2Fcontent%2Fpaper%2F540631321683&mimeType=pdf
   */

  protected static final String PATTERN_TEMPLATE = "\"%s([^/]+)/([^/]+)_([^/]+)$\", base_url";

  public static final Pattern HTML_PATTERN = Pattern.compile("/([^/]+)/([^/]+)_([^/]+)", Pattern.CASE_INSENSITIVE);
  public static final String HTML_REPLACEMENT = "/$1/$2_$3";

  String RIS_REPLACEMENT = "/$1/$2_$3/cite/ris";
  String BIB_REPLACEMENT = "/$1/$2_$3/cite/bib";
  String ENDNOTE_REPLACEMENT = "/$1/$2_$3/cite/endnote";


  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
          throws PluginException {

    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(new SubTreeArticleIterator.Spec()
            .setTarget(target)
            .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

    builder.addAspect(
            HTML_PATTERN,
            HTML_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_HTML,
            ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(
            RIS_REPLACEMENT,
            ArticleFiles.ROLE_CITATION_RIS);
    builder.addAspect(
            BIB_REPLACEMENT,
            ArticleFiles.ROLE_CITATION_BIBTEX);
    builder.addAspect(
            ENDNOTE_REPLACEMENT,
            ArticleFiles.ROLE_CITATION_ENDNOTE);

    builder.setRoleFromOtherRoles(
            ArticleFiles.ROLE_CITATION,
            Arrays.asList(
                    ArticleFiles.ROLE_CITATION_RIS,
                    ArticleFiles.ROLE_CITATION_BIBTEX,
                    ArticleFiles.ROLE_CITATION_ENDNOTE
            )
    );

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}


