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

package org.lockss.plugin.scielo;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.SubTreeArticleIteratorBuilder;
import org.lockss.plugin.oapen.OAPENBooksArticleIteratorFactory;
import org.lockss.util.Logger;

/**
 * full-text HTML URL:
 * https://www.scielo.br/j/abcd/a/V83s5FPhKPnx9JGzDmkJRJn/?lang=en
 * 
 * PDF URL:
 * https://www.scielo.br/j/abcd/a/V83s5FPhKPnx9JGzDmkJRJn/?format=pdf&lang=en
 * 
 * Abstract URL:
 * https://www.scielo.br/j/abcd/a/V83s5FPhKPnx9JGzDmkJRJn/abstract/?lang=en
 */

public class SciELO2024ArticleIteratorFactory implements ArticleIteratorFactory{

    protected static Logger log =
        Logger.getLogger(OAPENBooksArticleIteratorFactory.class);

    private static final String ROOT_TEMPLATE = "\"%s\", base_url";
    private static final String PATTERN_TEMPLATE = "\"%sj/%s/a/[^/]+/(abstract/)?\\?(format=pdf&)?lang=en\", base_url, journal_id";

    private static final Pattern HTML_PATTERN = Pattern.compile("(/a/[^/]+/)(\\?lang=en)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PDF_PATTERN = Pattern.compile("(/a/[^/]+/)(\\?format=pdf&lang=en)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ABSTRACT_PATTERN = Pattern.compile("(/a/[^/]+/)(abstract/\\?lang=en)", Pattern.CASE_INSENSITIVE);

    private static final String HTML_REPLACEMENT = "$1?lang=en"; 
    private static final String PDF_REPLACEMENT = "$1?format=pdf&lang=en";
    private static final String ABSTRACT_REPLACEMENT = "$1abstract/?lang=en";

    
  
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
        ROOT_TEMPLATE,
        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(HTML_PATTERN, HTML_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_HTML);

        builder.addAspect(PDF_PATTERN,
        PDF_REPLACEMENT,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(ABSTRACT_PATTERN,
        ABSTRACT_REPLACEMENT,
        ArticleFiles.ROLE_ABSTRACT);

        return builder.getSubTreeArticleIterator();
    }
    
}
