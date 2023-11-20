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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.BaseArticleMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.filter.html.HtmlTags.Article;
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
 * 
 * https://www.scielo.br/citation/export/37GVXdWqMdYMtNx7NcD86xF/?format=bib
 * https://www.scielo.br/citation/export/37GVXdWqMdYMtNx7NcD86xF/?format=ris
 */

public class SciELO2024ArticleIteratorFactory implements ArticleIteratorFactory, ArticleMetadataExtractorFactory{

    protected static Logger log =
        Logger.getLogger(OAPENBooksArticleIteratorFactory.class);

    private static final List<String> languageCodes = Arrays.asList("en", "pt", "es");

    private static final String ROOT_TEMPLATE = "\"%s\", base_url";
    private static final String PATTERN_TEMPLATE = "\"%sj/%s/a/[^/]+/(abstract/)?\\?(format=pdf&)?lang=(en|pt|es)\", base_url, journal_id";

    private static final List<Pattern> HTML_PATTERNS = new ArrayList<Pattern>(); 
    private static final List<Pattern> PDF_PATTERNS = new ArrayList<Pattern>(); 
    private static final List<Pattern> ABSTRACT_PATTERNS = new ArrayList<Pattern>(); 
    static {
        for(String code: languageCodes){
            HTML_PATTERNS.add(Pattern.compile("(/j/[^/]+/a/)([^/]+/)\\?lang=" + code, Pattern.CASE_INSENSITIVE));
            PDF_PATTERNS.add(Pattern.compile("(/j/[^/]+/a/)([^/]+/)\\?format=pdf&lang=" + code, Pattern.CASE_INSENSITIVE));
            ABSTRACT_PATTERNS.add(Pattern.compile("(/j/[^/]+/a/)([^/]+/)abstract/\\?lang=" + code, Pattern.CASE_INSENSITIVE));
        }
    }

    private static final List<String> HTML_REPLACEMENTS = new ArrayList<String>();
    private static final List<String> PDF_REPLACEMENTS = new ArrayList<String>();
    private static final List<String> ABSTRACTS_REPLACEMENTS = new ArrayList<String>();
    static{
        for(String code: languageCodes){
            HTML_REPLACEMENTS.add("$1$2?lang=" + code);
            PDF_REPLACEMENTS.add("$1$2?format=pdf&lang=" + code);
            ABSTRACTS_REPLACEMENTS.add("$1$2abstract/?lang=" + code);
        }
    }

    private static final String CITATION_BIBTEX_REPLACEMENT = "/citation/export/$2?format=bib";
    private static final String CITATION_RIS_REPLACEMENT = "/citation/export/$2?format=ris";
  
    @Override
    public Iterator<ArticleFiles> createArticleIterator(ArchivalUnit au, MetadataTarget target) throws PluginException {
        SubTreeArticleIteratorBuilder builder = new SubTreeArticleIteratorBuilder(au);

        builder.setSpec(target,
        ROOT_TEMPLATE,
        PATTERN_TEMPLATE, Pattern.CASE_INSENSITIVE);

        builder.addAspect(HTML_PATTERNS, HTML_REPLACEMENTS,
        ArticleFiles.ROLE_FULL_TEXT_HTML);

        builder.addAspect(PDF_PATTERNS,
        PDF_REPLACEMENTS,
        ArticleFiles.ROLE_FULL_TEXT_PDF);

        builder.addAspect(ABSTRACT_PATTERNS,
        ABSTRACTS_REPLACEMENTS,
        ArticleFiles.ROLE_ABSTRACT);

        builder.addAspect(CITATION_BIBTEX_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_BIBTEX);

        builder.addAspect(CITATION_RIS_REPLACEMENT,
        ArticleFiles.ROLE_CITATION_RIS);

        builder.setRoleFromOtherRoles(ArticleFiles.ROLE_ARTICLE_METADATA, ArticleFiles.ROLE_ABSTRACT, ArticleFiles.ROLE_FULL_TEXT_HTML);
        builder.setFullTextFromRoles(ArticleFiles.ROLE_FULL_TEXT_HTML, ArticleFiles.ROLE_FULL_TEXT_PDF, ArticleFiles.ROLE_ABSTRACT);

        return builder.getSubTreeArticleIterator();
    }


    @Override
    public ArticleMetadataExtractor createArticleMetadataExtractor(MetadataTarget target)
        throws PluginException {
        return new BaseArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);
    }
        
}
