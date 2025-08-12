/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.kare;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

/**
 Kare's abstract page has links to Full-Text pdf and also
 contains a handful of metatdata types.

 Article:
 https://agridergisi.com/jvi.aspx?un=AGRI-42800&volume=34&issue=1
 https://agridergisi.com/jvi.aspx?un=AGRI-60243&volume=33&supp=1

 Abstract:
 https://agridergisi.com/jvi.aspx?pdir=agri&plng=eng&un=AGRI-42800&look4= // not guaranteed to be there

 PDF:
 https://jag.journalagent.com/z4/download_fulltext.asp?pdir=agri&plng=eng&un=AGRI-42800

 Citation:
 https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=AGRI-42800&format=BibTeX
 https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=AGRI-42800&format=EndNote
 https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=AGRI-42800&format=Medlars
 https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=AGRI-42800&format=Procite
 https://jag.journalagent.com/z4/gencitation.asp?pdir=agri&article=AGRI-42800&format=RIS
 */

public class KareArticleIteratorFactory
        implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    private static final Logger log = Logger.getLogger(KareArticleIteratorFactory.class);


    private static final String ROOT_TEMPLATE = "\"%s\", base_url";

    private static final String PATTERN_TEMPLATE =
            "\"%sjvi\\.aspx\\?un=[^&]+&volume=[^&]+&(issue|supp)=[^&]+\", base_url";

    private static final Pattern ARTICLE_PATTERN = Pattern.compile(
            "jvi\\.aspx\\?un=([^&]+)&volume=([^&]+)&((issue|supp)=[^&]+)", Pattern.CASE_INSENSITIVE);


    private static final String ARTICLE_REPLACEMENT = "jvi.aspx?un=$1&volume=$2&$3";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(new SubTreeArticleIterator.Spec()
                .setTarget(target)
                .setRootTemplate(ROOT_TEMPLATE)
                .setPatternTemplate(PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE));

        builder.addAspect(ARTICLE_PATTERN,
                ARTICLE_REPLACEMENT,
                ArticleFiles.ROLE_ARTICLE_METADATA);


        return builder.getSubTreeArticleIterator();
    }


    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
}
