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

package org.lockss.plugin.universityofleeds;

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

public class UniversityOfLeedsBooksArticleIteratorFactory implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

  protected static Logger log =
          Logger.getLogger(UniversityOfLeedsBooksArticleIteratorFactory.class);


      /*
      https://leeds.pressbooks.pub/educ5272m
      https://leeds.pressbooks.pub/educ5272m/open/download?type=epub
      https://leeds.pressbooks.pub/educ5272m/open/download?type=pdf
       */


    // Matches the base URL: https://leeds.pressbooks.pub/educ5272m
    protected static final String ROOT_TEMPLATE = "\"%s\", base_url";

    protected static final String PATTERN_TEMPLATE =
            "\"%s%s(/open/download\\?type=(pdf|print_pdf|epub))?$\", base_url, resource_id";


    // 1. PDF Pattern (Matches pdf or print_pdf types)
    public static final Pattern PDF_PATTERN = Pattern.compile("/open/download\\?type=pdf$", Pattern.CASE_INSENSITIVE);
    public static final String PDF_REPLACEMENT = "/open/download?type=pdf";

    // 2. EPUB Pattern
    public static final Pattern EPUB_PATTERN = Pattern.compile("/open/download\\?type=epub$", Pattern.CASE_INSENSITIVE);
    public static final String EPUB_REPLACEMENT = "/open/download?type=epub";

    // 3. HTML/Metadata Pattern (The landing page itself)
    // This matches the resource_id with nothing following it
    public static final Pattern HTML_PATTERN = Pattern.compile("$", Pattern.CASE_INSENSITIVE);
    public static final String HTML_REPLACEMENT = "";


    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        // Set the strict pattern
        builder.setSpec(target, ROOT_TEMPLATE, PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        // 1. Map standard PDF
        builder.addAspect(
                Pattern.compile("/open/download\\?type=pdf$", Pattern.CASE_INSENSITIVE),
                "/open/download?type=pdf",
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        // 2. Map Print PDF as an alternative for the same role
        builder.addAspect(
                Pattern.compile("/open/download\\?type=print_pdf$", Pattern.CASE_INSENSITIVE),
                "/open/download?type=print_pdf",
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        // 3. Map EPUB
        builder.addAspect(
                EPUB_PATTERN, // (/open/download\?type=epub)
                "/open/download?type=epub",
                ArticleFiles.ROLE_FULL_TEXT_EPUB);

        // 4. Map the Landing Page (Root) as HTML and Metadata
        builder.addAspect(
                Pattern.compile("$"),
                "",
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

        // This ensures only ONE article is created per book,
        // and it identifies it by the HTML landing page.
        builder.setFullTextFromRoles(
                ArticleFiles.ROLE_FULL_TEXT_HTML,
                ArticleFiles.ROLE_FULL_TEXT_PDF);

        return builder.getSubTreeArticleIterator();
    }

    protected SubTreeArticleIteratorBuilder localBuilderCreator(ArchivalUnit au) {
    return new SubTreeArticleIteratorBuilder(au);
  }
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
