/*
 * $Id: HindawiArticleIteratorFactory.java,v 1.2 2013-06-20 00:05:48 thib_gc Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.hindawi;

import java.util.*;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class HindawiArticleIteratorFactory
    implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static final String ROOT_TEMPLATE_HTML = "\"%sjournals/%s/%s/\", base_url, journal_id, volume_name";
  protected static final String ROOT_TEMPLATE_PDF = "\"%sjournals/%s/%s/\", download_url, journal_id, volume_name";
  protected static final String PATTERN_TEMPLATE = "\"^(%sjournals/%s/%s/\\d+|%sjournals/%s/%s/\\d+\\.pdf)$\", base_url, journal_id, volume_name, download_url, journal_id, volume_name";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
    String base_url = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String download_url = au.getConfiguration().get("download_url");
    
    final Pattern HTML_PATTERN = Pattern.compile(String.format("^%sjournals/([^/]+)/([^/]+)/(\\d+)$", base_url), Pattern.CASE_INSENSITIVE);
    final String HTML_REPLACEMENT = String.format("%sjournals/$1/$2/$3", base_url);

    final Pattern PDF_PATTERN = Pattern.compile(String.format("^%sjournals/([^/]+)/([^/]+)/(\\d+)\\.pdf$", download_url), Pattern.CASE_INSENSITIVE);
    final String PDF_REPLACEMENT = String.format("%sjournals/$1/$2/$3.pdf", download_url);

    final String ABSTRACT_REPLACEMENT = String.format("%sjournals/$1/$2/$3/abs", base_url);
    final String CITATION_REPLACEMENT = String.format("%sjournals/$1/$2/$3/cta", base_url);
    final String REFERENCES_REPLACEMENT = String.format("%sjournals/$1/$2/$3/ref", base_url);
    final String EPUB_REPLACEMENT = String.format("%sjournals/$1/$2/$3.epub", download_url);
    
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
                    Arrays.asList(ROOT_TEMPLATE_HTML, ROOT_TEMPLATE_PDF),
                    PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

    builder.addAspect(HTML_PATTERN,
                      HTML_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_HTML,
                      ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(PDF_PATTERN,
                      PDF_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_PDF);
    
    builder.addAspect(ABSTRACT_REPLACEMENT,
                      ArticleFiles.ROLE_ABSTRACT,
                      ArticleFiles.ROLE_ARTICLE_METADATA);

    builder.addAspect(EPUB_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_EPUB);

    builder.addAspect(CITATION_REPLACEMENT,
                      ArticleFiles.ROLE_CITATION);

    builder.addAspect(REFERENCES_REPLACEMENT,
                      ArticleFiles.ROLE_REFERENCES);
    
    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
      throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
  
}
