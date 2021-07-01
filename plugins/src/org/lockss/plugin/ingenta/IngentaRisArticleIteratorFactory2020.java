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

package org.lockss.plugin.ingenta;

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

public class IngentaRisArticleIteratorFactory2020 implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static Logger log = Logger.getLogger(IngentaRisArticleIteratorFactory2020.class);

  /*
   http://www.ingentaconnect.com/content/iuatld/ijtld/2015/00000019/00000001/art00002?format=ris
   http://www.ingentaconnect.com/content/iuatld/ijtld/2017/00000021/00000001/art00003?format=ris
   */

  protected static final String ROOT_TEMPLATE = "\"%scontent/%s/%s\", base_url, publisher_id, journal_id";
  private static final String PATTERN_TEMPLATE = "\"%scontent/%s/%s/([0-9]{4}/[^/]+/[^/]+/art[0-9]{5})\\?format=ris\", " +
          "base_url, publisher_id, journal_id";

  public static final Pattern HTML_PATTERN = Pattern.compile("(.*)content/([^/]+)/([^/]+)/([0-9]{4}/[^/]+/[^/]+/art[0-9]{5})\\?crawler=true&mimetype=text/html", Pattern.CASE_INSENSITIVE);
  public static final Pattern HTML_PATTERN_ALTERNATIVE = Pattern.compile("(.*)content/([^/]+)/([^/]+)/([0-9]{4}/[^/]+/[^/]+/art[0-9]{5})\\?crawler=true", Pattern.CASE_INSENSITIVE);
  public static final Pattern PDF_PATTERN = Pattern.compile("(.*)content/([^/]+)/([^/]+)/([0-9]{4}/[^/]+/[^/]+/art[0-9]{5})\\?crawler=true&mimetype=application/pdf", Pattern.CASE_INSENSITIVE);
  public static final Pattern RIS_PATTERN = Pattern.compile("(.*)content/([^/]+)/([^/]+)/([0-9]{4}/[^/]+/[^/]+/art[0-9]{5})\\??format=ris", Pattern.CASE_INSENSITIVE);
  public static final String HTML_REPLACEMENT = "$1content/$2/$3/$4?crawler=true&mimetype=text/html";
  public static final String HTML_REPLACEMENT_ALTERNATIVE = "$1content/$2/$3/$4?crawler=true";
  private static final String PDF_REPLACEMENT = "http://api.ingentaconnect.com/content/$2/$3/$4?crawler=true&mimetype=application/pdf";
  public static final String RIS_REPLACEMENT = "$1content/$2/$3/$4?format=ris";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
          throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

    builder.setSpec(target,
            ROOT_TEMPLATE,
            PATTERN_TEMPLATE,
            Pattern.CASE_INSENSITIVE);

    builder.addAspect(RIS_PATTERN,
            RIS_REPLACEMENT,
            ArticleFiles.ROLE_ARTICLE_METADATA);
    
    builder.addAspect(HTML_PATTERN,
            HTML_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_HTML);

    builder.addAspect(HTML_PATTERN_ALTERNATIVE,
            HTML_REPLACEMENT_ALTERNATIVE,
            ArticleFiles.ROLE_FULL_TEXT_PDF);


    builder.addAspect(PDF_PATTERN,
            PDF_REPLACEMENT,
            ArticleFiles.ROLE_FULL_TEXT_PDF);

    return builder.getSubTreeArticleIterator();
  }

  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
          throws PluginException {
    return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
  }
}
