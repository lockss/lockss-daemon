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

package org.lockss.plugin.clockss.nature;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.*;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/*
 *  Although this uses the SourceXmlMetadataExtractor framework, it is a harvest plugin,
 *  so the ArticleIterator will find the ".html" files if they exist
 *  Not all articles have .pdf
 *  BoneKEy Reports was more consistent
 *      https://bonekey.stanford.clockss.org/2015/bonekeyreports_2015_bonekey2014115_fulltext/
 *      all article directories were bonekeyreports_year_bonekey#_fulltext/
 *      and the html, pdf and xml were all siblings int that directory
 *  Knowledge Environment got less consistent. Most were
 *    https://knowledgeenvironment.stanford.clockss.org/2015/bonekey_2015_bonekey201514_xml_pdf/
 *      with the article directory as bonekey_year_bonekey#_xml_pdf
 *         but two ended with "fulltext" and not "xml_pdf"
 *      The xml files are either in the article directory or under "/xml" or "/xml_temp"
 *      The pdf files are either in the article directory or under "/pdf_temp"
 *  There are some spurious xml files, but the ones we want have the name bonekey[0-9]+.xml only    
 *      
 */
public class BkTriggeredArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static final Logger log = Logger.getLogger(BkTriggeredArticleIteratorFactory.class);
  // xml file is only ever in the article dir, under xml_temp or under xml (in the last case, there is no pdf)
  //group1 = article directory
  //group2,3,4 are optional - don't use
  //group5 is the base filename for the xml/pdf
  public static final Pattern XML_PATTERN = Pattern.compile("/(bonekey(reports)?_[0-9]+_[^/]+)/(xml(_temp)?/)?(bonekey[0-9]+)\\.xml$", Pattern.CASE_INSENSITIVE);
  public static final String XML_REPLACEMENT1 = "/$1/$5.xml";
  public static final String XML_REPLACEMENT2 = "/$1/xml_temp/$5.xml";
  public static final String XML_REPLACEMENT3 = "/$1/xml/$5.xml";
  // pdf is only ever in the article directory or under pdf_temp, but keep the groupings the same
  public static final Pattern PDF_PATTERN = Pattern.compile("/(bonekey(reports)?_[0-9]+_[^/]+)/(pdf(_temp)/)?(bonekey[0-9]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  public static final String PDF_REPLACEMENT1 = "/$1/$5.pdf";
  public static final String PDF_REPLACEMENT2 = "/$1/pdf_temp/$5.pdf";
  // html is always just in the articles directory
  public static final String ABS_REPLACEMENT = "/$1/$5.html";
  
  // all files found under article subdirector or one level deeper that contain bonekey(reports)?_<year> in the name
//  protected static final String PATTERN_TEMPLATE = "\"%s%d/bonekey(reports)?_%d_[^/]+/([^/]+/)?(bonekey[0-9]+)\\.(xml|pdf)$\",base_url,year,year";
  // it's automatically already limited to this AU so no need to put in base_url or year via string substitution
  protected static final String PATTERN_TEMPLATE = "/bonekey(reports)?_[0-9]+_[^/]+/([^/]+/)?(bonekey[0-9]+)\\.(xml|pdf)$";
  
  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    // no need to limit to ROOT_TEMPLATE
    builder.setSpec(builder.newSpec()
                    .setTarget(target)
                    .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));
    
    // Because we created the interim website, we KNOW that there will always
    // be an XML and an HTML landing page for each article. There will sometimes
    // be a PDF, unless one was never provided to us.
    
    // Not all volumes have PDF, but for those that do
    builder.addAspect(PDF_PATTERN,
        Arrays.asList(PDF_REPLACEMENT1, PDF_REPLACEMENT2),
                      ArticleFiles.ROLE_FULL_TEXT_PDF);
    // set up XML to be an aspect that will trigger an ArticleFiles and feed the metadata extractor
    builder.addAspect(XML_PATTERN,
        Arrays.asList(XML_REPLACEMENT1, XML_REPLACEMENT2, XML_REPLACEMENT3),
                      ArticleFiles.ROLE_FULL_TEXT_XML,
                      ArticleFiles.ROLE_ARTICLE_METADATA);

    // set up ABSTRACT to be a secondary aspect - otherwise you'd try to create
    // an article out of the issue TOC
    builder.addAspect(ABS_REPLACEMENT,
                      ArticleFiles.ROLE_ABSTRACT);
    
    
    // The order in which we want to define full_text_cu.  
    // First one that exists will get the job
    // When there is no PDF, the "abstract" is actually complete content
    builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF,
        ArticleFiles.ROLE_ABSTRACT,
        ArticleFiles.ROLE_FULL_TEXT_XML);  

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}