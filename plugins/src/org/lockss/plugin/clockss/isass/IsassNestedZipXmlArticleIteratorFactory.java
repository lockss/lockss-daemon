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

package org.lockss.plugin.clockss.isass;

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

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

public class IsassNestedZipXmlArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(IsassNestedZipXmlArticleIteratorFactory.class);

  // <base_url>/<year>/SPECIAL_DELIVERY_ijss_25_3_2022.zip!/ijss_11_2.pdf.zip/IJSS-11-14444-4009.pdf
  // <base_url>/<year>/SPECIAL_DELIVERY_ijss_25_3_2022.zip!/ijss_11_2.xml.zip/IJSS-11-14444-4009.xml
  // <base_url>/<year>/IJSS-1-01.zip!/IJSS-1-2006-0002-RR.pdf
  // <base_url>/<year>/IJSS-1-01.zip!/IJSS-1-2006-0002-RR.xml
  protected static final String ALL_ZIP_XML_PATTERN_TEMPLATE =
      "\"%s[^/]+/(?>[^/]+/)?.+\\.zip!?/.+\\.xml\", base_url";


  public static final Pattern XML_PATTERN = Pattern.compile("zip!?/(?>(.+)\\.xml\\.zip/)?([^/]+)\\.xml$", Pattern.CASE_INSENSITIVE);
  public static final String XML_REPLACEMENT_NEW = "zip!/$1.xml.zip/$2.xml";
  public static final String XML_REPLACEMENT_OLD = "zip!/$2.xml";
  // should always be a 1:1 relationship
  public static final String PDF_REPLACEMENT_NEW = "zip/$1.pdf.zip/$2.pdf";
  public static final String PDF_REPLACEMENT_OLD = "zip/$2.pdf";


  //
  // The non-archive source content structure looks like this:
  // <root_location>/<dir>/<possible subdirectories>/<STUFF>
  //     where STUFF is a series of files:  <name>.pdf, <name>.epub &
  //    as well as a some number of <othername(s)>.xml which provide the metadata
  //    for all the content.
  //
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
        .setTarget(target)
        .setPatternTemplate(ALL_ZIP_XML_PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE)
        .setVisitArchiveMembers(true));

    // NOTE - full_text_cu is set automatically to the url used for the articlefiles
    // ultimately the metadata extractor needs to set the entire facet map

    // set up XML to be an aspect that will trigger an ArticleFiles to feed the metadata extractor
    builder.addAspect(XML_PATTERN,
        Arrays.asList(
          XML_REPLACEMENT_OLD,
          XML_REPLACEMENT_NEW
        ),
        ArticleFiles.ROLE_ARTICLE_METADATA);

    // While we can't identify articles that are *just* PDF which is why they
    // can't trigger an articlefiles by themselves, we can identify them
    // by replacement and they should be the full text CU.
    builder.addAspect(
        Arrays.asList(
          PDF_REPLACEMENT_NEW,
          PDF_REPLACEMENT_OLD
        ),
        ArticleFiles.ROLE_FULL_TEXT_PDF);

    //Now set the order for the full text cu
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ARTICLE_METADATA); // though if it comes to this it won't emit

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
