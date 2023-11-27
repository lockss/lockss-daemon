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

package org.lockss.plugin.clockss.kluwerlaw;

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

public class KluwerLawJournalsArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(KluwerLawJournalsArticleIteratorFactory.class);

    protected static final String ROOT_TEMPLATE = "\"%s%s\",base_url,directory";
    private static final String PATTERN_TEMPLATE = "\"%s%s/.*\",base_url,directory";

    //https://clockss-test.lockss.org/sourcefiles/kluwerlawjnls-released/2023_01/AILA/204497.ox1		-		-	    -		-
    //https://clockss-test.lockss.org/sourcefiles/kluwerlawjnls-released/2023_01/AILA/204497.pdf		-		-

    protected static final Pattern PDF_PATTERN = Pattern.compile("/(.*)\\.pdf$");
    protected static final String PDF_REPLACEMENT = "/$1.pdf";

    protected static final Pattern METADATA_FILE_PATTERN = Pattern.compile("/(.*)\\.ox1$");
    protected static final String METADATA_FILE_REPLACEMENT = "/$1.ox1";


    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                ROOT_TEMPLATE,
                PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(METADATA_FILE_PATTERN,
                METADATA_FILE_REPLACEMENT,
                ArticleFiles.ROLE_FULL_TEXT_XML,
                ArticleFiles.ROLE_ARTICLE_METADATA);

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