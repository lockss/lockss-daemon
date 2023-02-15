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

package gov.gpo.access.permanent.plugin.monthlylaborreview;

import java.util.*;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

public class MonhtlyLaborReview2022ArticleIteratorFactory
    implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

  protected static final String ROOT_TEMPLATE_1 = "\"%sopub/mlr/%d/\", base_url, year";
  protected static final String ROOT_TEMPLATE_2 = "\"%sopub/mlr/cwc/\", base_url, year";
  protected static final String PATTERN_TEMPLATE = "\"^%sopub/mlr/(%d|cwc)/.*\\.(htm|pdf)$\", base_url, year";

  protected static final Pattern HTML_PATTERN = Pattern.compile("/(\\d+/article)/([^/]+)\\.htm$", Pattern.CASE_INSENSITIVE);
  protected static final String HTML_REPLACEMENT = "/$1/$2.htm";
  
  protected static final Pattern PDF_PATTERN_1 = Pattern.compile("/(\\d+/article/pdf)/([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  protected static final String PDF_REPLACEMENT_1 = "/$1/$2.pdf";
  protected static final Pattern PDF_PATTERN_2 = Pattern.compile("/(\\d+/\\d+)/([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  protected static final String PDF_REPLACEMENT_2 = "/$1/$2.pdf";
  protected static final Pattern PDF_PATTERN_3 = Pattern.compile("/(cwc)/([^/]+)\\.pdf$", Pattern.CASE_INSENSITIVE);
  protected static final String PDF_REPLACEMENT_3 = "/cwc/$2.pdf";

  @Override
  public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                      MetadataTarget target)
  
      throws PluginException {
    SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);
    
    builder.setSpec(target,
                    Arrays.asList(ROOT_TEMPLATE_1, ROOT_TEMPLATE_2),
                    PATTERN_TEMPLATE);
    
    builder.addAspect(HTML_PATTERN,
                      HTML_REPLACEMENT,
                      ArticleFiles.ROLE_FULL_TEXT_HTML);
    
    builder.addAspect(Arrays.asList(PDF_PATTERN_1, PDF_PATTERN_2, PDF_PATTERN_3),
                      Arrays.asList(PDF_REPLACEMENT_1, PDF_REPLACEMENT_2, PDF_REPLACEMENT_3),
                      ArticleFiles.ROLE_FULL_TEXT_PDF);

    return builder.getSubTreeArticleIterator();
  }
  
  @Override
  public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target) throws PluginException {
    return new BaseArticleMetadataExtractor();
  }
  
}
