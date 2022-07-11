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

package org.lockss.plugin.clockss.casalini;

import org.lockss.config.CurrentConfig;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

import java.util.Iterator;
import java.util.regex.Pattern;

public class CasaliniLibriMarcXmlSourceArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory {

    protected static Logger log = Logger.getLogger(CasaliniLibriMarcXmlSourceArticleIteratorFactory.class);

    /*
     * This is only used in two AUs: casalini2012-released/2016 and casalini-released/2019
     * The only three XML URLs are:
     *   http://clockss-ingest.lockss.org/sourcefiles/casalini2012-released/2016/monographs.xml
     *   http://clockss-ingest.lockss.org/sourcefiles/casalini-released/2019/Psicoterapia/psicoterapia_scienze_umane_articles_20191014.xml
     *   http://clockss-ingest.lockss.org/sourcefiles/casalini-released/2019/Psicoterapia/psicoterapia_scienze_umane_issues_20191014.xml
     * The first contains both articles and issues, the other two contain only the corresponding type.
     * Enumerate all URLs ending in .xml but only select those not having "issues" in the basename.
     */
    protected static final String ROOT_TEMPLATE = "\"%s%d\", base_url, year";
    protected static final String PATTERN_TEMPLATE = "\"^%s%d.*/[^/]+\\.xml$\", base_url, year";
    protected static final Pattern XML_PATTERN = Pattern.compile("/((?!.*issues)[^/]+\\.xml)$", Pattern.CASE_INSENSITIVE);
    protected static final String XML_REPLACEMENT = "/$1.xml";

    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au,
                                                        MetadataTarget target)
            throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
                        ROOT_TEMPLATE,
                        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(XML_PATTERN,
                          XML_REPLACEMENT,
                          ArticleFiles.ROLE_ARTICLE_METADATA);

        return builder.getSubTreeArticleIterator();
    }


    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
            throws PluginException {
        //Do this on purpose, since it has one-xml-many-PDFs
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA) {
            @Override
            protected boolean isCheckAccessUrl() {
                return false;
            }
        };
    }
}
