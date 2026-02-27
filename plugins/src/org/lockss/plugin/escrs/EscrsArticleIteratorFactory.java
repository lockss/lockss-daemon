/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.escrs;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.util.Logger;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.extractor.MetadataTarget;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;


public class EscrsArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory{

    protected static Logger log = Logger.getLogger(EscrsArticleIteratorFactory.class);

    /*
        Example Abstract: https://emiratesscholar.com/directory/index.php/ejbess/article/view/1084
        Example PDF: https://emiratesscholar.com/directory/index.php/ejbess/article/view/1084/570 
    */

    private static final String ROOT_TEMPLATE = "\"%s\", base_url";
    private static final String PATTERN_TEMPLATE = "\"%sdirectory/index.php/%s/article/view\", base_url, journal_id";

    private static final Pattern ABSTRACT_PATTERN = Pattern.compile("/article/view/([0-9]+)$",Pattern.CASE_INSENSITIVE);
    private static final Pattern PDF_PATTERN = Pattern.compile("/article/view/([0-9]+)/([0-9]+)",Pattern.CASE_INSENSITIVE);

    private static final String ABSTRACT_REPLACEMENT = "/article/view/$1";
    private static final String PDF_REPLACEMENT = "/article/view/$1/$2";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
        ROOT_TEMPLATE,
        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(ABSTRACT_PATTERN, ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);

        builder.addAspect(PDF_PATTERN, PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, ArticleFiles.ROLE_ABSTRACT);
        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_PDF);

        return builder.getSubTreeArticleIterator();
    }


    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
        throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
    
}
