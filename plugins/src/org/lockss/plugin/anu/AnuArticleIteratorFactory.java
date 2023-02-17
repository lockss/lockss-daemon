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

package org.lockss.plugin.anu;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.SubTreeArticleIterator.Spec;
import org.lockss.util.Logger;

public class AnuArticleIteratorFactory
implements ArticleIteratorFactory,
           ArticleMetadataExtractorFactory {
  
  protected static Logger log = Logger.getLogger(AnuArticleIteratorFactory.class);

  Spec AnuSpec = new Spec();

  // relevant files are all hosted on another domain (rather than the base_url)
  // so it is hardcoded for now
  protected static final String ROOT_TEMPLATE = "https://press-files.anu.edu.au/downloads/";
  // pattern can include either the pdf or html aspects
  protected static final String INCLUDE_PATTERN = "^https://press-files.anu.edu.au/downloads/press/[^/]+/(pdf|html)/";
  // exclude a number of urls,
  // pdfs that are informational/generic to the journal,
  // https://press-files.anu.edu.au/downloads/press/p74151/pdf/contributors26.pdf
  // https://press-files.anu.edu.au/downloads/press/p10321/pdf/2_prelim_hr1_1998.pdf
  // https://press-files.anu.edu.au/downloads/press/n8684/html/font/AGaramondPro-Bold.otf * and other static files
  // title pages or separators like this
  // https://press-files.anu.edu.au/downloads/press/n8444/html/part01.xhtml
  protected static final String EXCLUDE_PATTERN =
      "^https://press-files.anu.edu.au/downloads/press/[^/]+"+
      "/(pdf|html)/"+
      "(" +
          "(\\d\\d?_)?" + /* 2_prelim_hr1_1998.pdf ugly, but this is the only way i can think of */
          "(" + /* match any of these strings that are html and/or pdf pages that are not articles or article like */
            "authors|author_profiles|bibliography|contents|c?over|contributors|" +
            "images|inside_cover|journal_information|"+
            "part|prelim(s|inary)?|upfront"+
          ")" +
          "(_...?\\d?\\d?_\\d)?" + /* again, ugly, but what else is there? */
          "\\d?\\d?\\d?" + /* sometimes there is up to 3 digits e.g. part01, contributors26 */
          "\\.(x?html|pdf)" +
        "|css|jpg|ttc|otf|png|jpg" +
      ")$";

  protected static final Pattern PDF_PATTERN = Pattern.compile(
      "([^/]+)/pdf/([^/.]+)\\.pdf",
      Pattern.CASE_INSENSITIVE);

  protected static final Pattern HTML_PATTERN = Pattern.compile(
      "([^/]+)/html/([^/.]+)\\.x?html",
      Pattern.CASE_INSENSITIVE);

  private static final String PDF_REPLACEMENT = "$1/pdf/$2.pdf";
  private static final String XHTML_REPLACEMENT = "$1/html/$2.xhtml";
  // haven't seen this, but it is probable to exist, so include it for now.
  private static final String HTML_REPLACEMENT = "$1/html/$2.html";

  // https://press-files.anu.edu.au/downloads/press/p332783/pdf/chp01.pdf
  // https://press-files.anu.edu.au/downloads/press/n8684/html/03_black.xhtml

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) 
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    AnuSpec.setTarget(target);
    AnuSpec.setRootTemplate(ROOT_TEMPLATE);
    //AnuSpec.setPatternTemplate(INCLUDE_PATTERN, Pattern.CASE_INSENSITIVE);
    AnuSpec.setExcludeSubTreePatternTemplate(EXCLUDE_PATTERN, Pattern.CASE_INSENSITIVE);

    builder.setSpec(AnuSpec);

    //builder.setSpec(
    //    target,
    //    ROOT_TEMPLATE,
    //    INCLUDE_PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    builder.addAspect(
        PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    builder.addAspect(
        HTML_PATTERN,
        Arrays.asList(
          XHTML_REPLACEMENT,
          HTML_REPLACEMENT
        ),
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_ARTICLE_METADATA,
        ArticleFiles.ROLE_FULL_TEXT_HTML_LANDING_PAGE);

    builder.setFullTextFromRoles(
        ArticleFiles.ROLE_FULL_TEXT_HTML,
        ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
    throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }

}
