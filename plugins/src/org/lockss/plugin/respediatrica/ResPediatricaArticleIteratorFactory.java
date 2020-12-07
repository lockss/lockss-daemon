/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.respediatrica;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

public class ResPediatricaArticleIteratorFactory
        implements ArticleIteratorFactory,
        ArticleMetadataExtractorFactory {

    protected static Logger log =
            Logger.getLogger(ResPediatricaArticleIteratorFactory.class);

    // It only have PDF and html page, no abstract/full-text
    // http://residenciapediatrica.com.br/detalhes/4
    // http://residenciapediatrica.com.br/exportar-pdf/4/v1n1a01.pdf   # Português version, default

    // http://residenciapediatrica.com.br/detalhes/323
    // http://residenciapediatrica.com.br/exportar-pdf/323/en_v8n2a08.pdf  # English or other language version
    // http://residenciapediatrica.com.br/exportar-pdf/323/v8n2a08.pdf     # Português version
    protected static final String ROOT_TEMPLATE = "\"%sdetalhes\", base_url";
    protected static final String PATTERN_TEMPLATE =
            "\"^%sdetalhes/\\d+$\", base_url";

    private static final Pattern HTML_PATTERN =
        Pattern.compile("/detalhes/(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final String HTML_REPLACEMENT = "/detalhes/$1";

//    private static final Pattern PDF_PATTERN =
//        Pattern.compile("/exportar-pdf/(\\d+)\\.pdf$", Pattern.CASE_INSENSITIVE);
//    private static final String PDF_REPLACEMENT = "/exportar-pdf/$1.pdf";
    
    private static final String RIS_REPLACEMENT = "/exportar-citacao/$1/ris";

    private static final String ENDNOTE_REPLACEMENT = "/exportar-citacao/$1/enl";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                        ROOT_TEMPLATE,
                        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(HTML_PATTERN,
                          HTML_REPLACEMENT,
                          ArticleFiles.ROLE_ABSTRACT,
                          ArticleFiles.ROLE_FULL_TEXT_HTML,
                          ArticleFiles.ROLE_ARTICLE_METADATA);

        builder.addAspect(RIS_REPLACEMENT,
                          ArticleFiles.ROLE_CITATION_RIS);
        
        builder.addAspect(ENDNOTE_REPLACEMENT,
                          ArticleFiles.ROLE_CITATION_ENDNOTE);

//        builder.addAspect(
//                PDF_PATTERN,
//                PDF_REPLACEMENT,
//                ArticleFiles.ROLE_FULL_TEXT_PDF);

        return builder.getSubTreeArticleIterator();
    }

    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }

}
