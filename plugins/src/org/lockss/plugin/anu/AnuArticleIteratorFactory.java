/*
 * $Id: $
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

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
